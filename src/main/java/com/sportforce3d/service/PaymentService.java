package com.sportforce3d.service;

import com.sportforce3d.db.Database;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.Payment;
import com.sportforce3d.model.PaymentInstructions;
import com.sportforce3d.model.PaymentStatus;
import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.OrderRepository;
import com.sportforce3d.repository.PaymentRepository;
import com.sportforce3d.storage.StorageService;
import com.sportforce3d.util.Validators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pagos con verificación manual (Yape, Plin, transferencia):
 * <ol>
 *     <li>el cliente registra el código de operación y sube el comprobante → pedido "Pago en verificación";</li>
 *     <li>un ADMIN lo compara con su app o estado de cuenta y lo <b>aprueba</b> (pedido "Pagado"; la reserva de stock
 *     se vuelve venta) o lo <b>rechaza</b> con motivo (pedido vuelve a "Pendiente de pago" y se abre un pago nuevo
 *     para que el cliente lo registre otra vez).</li>
 * </ol>
 * Nada aquí marca un pedido como pagado sin la aprobación de un ADMIN.
 */
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    /** Letras, números y guiones: los códigos de Yape/Plin son numéricos; los bancarios pueden llevar letras. */
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9-]{4,40}$");
    public static final int REASON_MIN = 5;
    public static final int REASON_MAX = 300;

    private final Database db;
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final StorageService storage;
    private final Session session;
    private final Clock clock;

    public PaymentService(Database db, PaymentRepository payments, OrderRepository orders, StorageService storage,
                          Session session, Clock clock) {
        this.db = db;
        this.payments = payments;
        this.orders = orders;
        this.storage = storage;
        this.session = session;
        this.clock = clock;
    }

    public PaymentInstructions instructions() {
        session.requireUser();
        return payments.instructions();
    }

    // ------------------------------------------------------------------ Cliente

    /** El dueño del pedido registra su pago: código de operación + comprobante (PNG, JPG o PDF, máx. 5 MB). */
    public OrderDetail register(long orderId, String operationCode, Path proof) {
        User me = session.requireUser();
        OrderDetail order = orders.findDetail(orderId)
                .filter(o -> o.userId() == me.id())
                .orElseThrow(() -> new ServiceException("No encontramos ese pedido."));
        if (order.status() != OrderStatus.PENDIENTE_PAGO) {
            throw new ServiceException("Este pedido no espera un pago (" + order.status().label().toLowerCase() + ").");
        }
        Payment payment = order.currentPayment();
        if (payment == null || payment.status() != PaymentStatus.PENDIENTE) {
            throw new ServiceException("Este pedido no tiene un pago pendiente. Actualiza la pantalla.");
        }
        String code = Validators.blankToNull(operationCode) == null ? "" : operationCode.trim().replace(" ", "");
        if (!CODE.matcher(code).matches()) {
            throw ValidationException.of("code", "Escribe el código de operación tal como aparece en tu app (4 a 40 letras o números).");
        }
        if (proof == null || !Files.isRegularFile(proof)) {
            throw ValidationException.of("proof", "Sube la captura o el PDF del comprobante.");
        }
        StorageService.StoredFile stored = storage.store(proof, "payments",
                Set.of(StorageService.Kind.PNG, StorageService.Kind.JPEG, StorageService.Kind.PDF));
        String now = clock.instant().toString();
        try {
            db.inTransaction(c -> {
                if (payments.operationCodeInUse(c, payment.method(), code)) {
                    throw ValidationException.of("code", "Ese código de operación ya se registró en otro pago.");
                }
                if (!payments.submit(c, payment.id(), code, now)
                        || !orders.updateStatus(c, orderId, OrderStatus.PENDIENTE_PAGO, OrderStatus.PAGO_EN_VERIFICACION)) {
                    throw new ServiceException("El pedido cambió de estado. Actualiza la pantalla e inténtalo de nuevo.");
                }
                payments.insertProof(c, payment.id(), stored.path(), stored.kind().mimeType(), stored.sizeBytes(), now);
                orders.insertHistory(c, orderId, OrderStatus.PENDIENTE_PAGO, OrderStatus.PAGO_EN_VERIFICACION, me.id(),
                        "Pago registrado (" + payment.method().label() + ", operación " + code + ")", now);
                return null;
            });
        } catch (RuntimeException e) {
            storage.delete(stored.path());
            throw e;
        }
        log.info("Pago {} del pedido {} registrado por el usuario {}", payment.id(), order.orderNumber(), me.id());
        return orders.findDetail(orderId).orElseThrow();
    }

    // ------------------------------------------------------------------ ADMIN

    public List<Payment> queue(PaymentStatus status) {
        session.requireRole(Role.ADMIN);
        return payments.findByStatus(status == null ? PaymentStatus.EN_VERIFICACION : status, 200);
    }

    public int pendingCount() {
        session.requireRole(Role.ADMIN);
        return payments.countByStatus(PaymentStatus.EN_VERIFICACION);
    }

    public Payment payment(long paymentId) {
        session.requireRole(Role.ADMIN);
        return payments.find(paymentId).orElseThrow(() -> new ServiceException("No encontramos ese pago."));
    }

    /**
     * Aprueba un pago en verificación: pago APROBADO (con quién y cuándo), pedido PAGADO y, por cada línea, la
     * reserva se descuenta del stock ({@code quantity} y {@code reserved} bajan juntos). Todo o nada.
     */
    public OrderDetail approve(long paymentId, String note) {
        User admin = session.requireRole(Role.ADMIN);
        Payment payment = payment(paymentId);
        String now = clock.instant().toString();
        String cleanNote = Validators.blankToNull(note);
        db.inTransaction(c -> {
            if (!payments.approve(c, paymentId, admin.id(), now)) {
                throw new ServiceException("Este pago ya no está en verificación. Actualiza la pantalla.");
            }
            if (!orders.updateStatus(c, payment.orderId(), OrderStatus.PAGO_EN_VERIFICACION, OrderStatus.PAGADO)) {
                throw new ServiceException("El pedido cambió de estado. Actualiza la pantalla e inténtalo de nuevo.");
            }
            OrderDetail order = orders.findDetail(c, payment.orderId()).orElseThrow();
            for (OrderRepository.Reservation r : orders.reservations(c, order.id())) {
                if (!orders.commitStock(c, r.variantId(), r.quantity())) {
                    throw new ServiceException("El inventario de una variante del pedido no coincide con lo reservado. "
                            + "Revísalo en Administración → Inventario antes de aprobar.");
                }
            }
            orders.insertHistory(c, order.id(), OrderStatus.PAGO_EN_VERIFICACION, OrderStatus.PAGADO, admin.id(),
                    "Pago aprobado (" + payment.method().label() + ", operación " + payment.operationCode() + ")"
                            + (cleanNote == null ? "" : ". " + cleanNote), now);
            return null;
        });
        log.info("Pago {} del pedido {} aprobado por el ADMIN {}", paymentId, payment.orderNumber(), admin.id());
        return orders.findDetail(payment.orderId()).orElseThrow();
    }

    /**
     * Rechaza un pago en verificación con motivo: el pedido vuelve a "Pendiente de pago" con un pago nuevo para que
     * el cliente registre otro código. El stock sigue reservado.
     */
    public OrderDetail reject(long paymentId, String reason) {
        User admin = session.requireRole(Role.ADMIN);
        String why = Validators.blankToNull(reason) == null ? "" : reason.trim();
        if (why.length() < REASON_MIN || why.length() > REASON_MAX) {
            throw ValidationException.of("reason", "Escribe el motivo del rechazo (" + REASON_MIN + " a " + REASON_MAX
                    + " caracteres): el cliente lo verá.");
        }
        if (Validators.plainText(why, true) != null) {
            throw ValidationException.of("reason", Validators.plainText(why, true));
        }
        Payment payment = payment(paymentId);
        String now = clock.instant().toString();
        db.inTransaction(c -> {
            if (!payments.reject(c, paymentId, admin.id(), why, now)) {
                throw new ServiceException("Este pago ya no está en verificación. Actualiza la pantalla.");
            }
            if (!orders.updateStatus(c, payment.orderId(), OrderStatus.PAGO_EN_VERIFICACION, OrderStatus.PENDIENTE_PAGO)) {
                throw new ServiceException("El pedido cambió de estado. Actualiza la pantalla e inténtalo de nuevo.");
            }
            orders.insertPendingPayment(c, payment.orderId(), payment.method(), payment.amountCents());
            orders.insertHistory(c, payment.orderId(), OrderStatus.PAGO_EN_VERIFICACION, OrderStatus.PENDIENTE_PAGO,
                    admin.id(), "Pago rechazado: " + why, now);
            return null;
        });
        log.info("Pago {} del pedido {} rechazado por el ADMIN {}", paymentId, payment.orderNumber(), admin.id());
        return orders.findDetail(payment.orderId()).orElseThrow();
    }
}