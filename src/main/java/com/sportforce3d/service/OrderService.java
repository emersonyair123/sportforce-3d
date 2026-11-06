package com.sportforce3d.service;

import com.sportforce3d.db.Database;
import com.sportforce3d.design.DesignJson;
import com.sportforce3d.design.DesignPricing;
import com.sportforce3d.model.Material;
import com.sportforce3d.repository.DesignRepository;
import com.sportforce3d.repository.DesignRepository.StoredDesign;
import com.sportforce3d.storage.StorageService;
import com.sportforce3d.model.Address;
import com.sportforce3d.model.CartLine;
import com.sportforce3d.model.CartSummary;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.OrderSummary;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.AddressRepository;
import com.sportforce3d.repository.CartRepository;
import com.sportforce3d.repository.OrderRepository;
import com.sportforce3d.repository.OrderRepository.NewItem;
import com.sportforce3d.repository.OrderRepository.NewOrder;
import com.sportforce3d.repository.OrderRepository.Reservation;
import com.sportforce3d.util.Validators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Convierte el carrito en un pedido: congela precios y dirección, reserva stock y crea el pago pendiente,
 * todo en una sola transacción. Nunca marca un pedido como pagado: eso lo hace un ADMIN al verificar el pago.
 * <p>
 * Las líneas personalizadas guardan una copia inmutable del diseño: el JSON de la versión actual y sus capturas
 * copiadas a {@code storage/orders/}. Así el pedido no cambia aunque el cliente edite o borre el diseño después.
 */
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Set<OrderStatus> CANCELLABLE = Set.of(OrderStatus.PENDIENTE_PAGO, OrderStatus.PAGO_EN_VERIFICACION);

    public static final int NOTES_MAX = 300;

    private final Database db;
    private final OrderRepository orders;
    private final CartRepository carts;
    private final AddressRepository addresses;
    private final CartService cartService;
    private final DesignRepository designs;
    private final CatalogService catalog;
    private final StorageService storage;
    private final Session session;
    private final Clock clock;

    /** Copia del diseño preparada antes de la transacción. */
    private record DesignCopy(String name, int version, String json, String front, String back) {
    }

    public OrderService(Database db, OrderRepository orders, CartRepository carts, AddressRepository addresses,
                        CartService cartService, DesignRepository designs, CatalogService catalog,
                        StorageService storage, Session session, Clock clock) {
        this.db = db;
        this.orders = orders;
        this.carts = carts;
        this.addresses = addresses;
        this.cartService = cartService;
        this.designs = designs;
        this.catalog = catalog;
        this.storage = storage;
        this.session = session;
        this.clock = clock;
    }

    public OrderDetail placeOrder(long addressId, PaymentMethod method, String notes) {
        User me = session.requireUser();
        if (method == null) {
            throw ValidationException.of("method", "Elige un método de pago.");
        }
        if (!method.available()) {
            throw ValidationException.of("method", "El pago con tarjeta estará disponible próximamente.");
        }
        String cleanNotes = Validators.blankToNull(notes);
        if (cleanNotes != null && cleanNotes.length() > NOTES_MAX) {
            throw ValidationException.of("notes", "Las notas admiten hasta " + NOTES_MAX + " caracteres.");
        }
        String now = clock.instant().toString();
        int year = clock.instant().atZone(LIMA).getYear();

        // Copias de los diseños (archivos) fuera de la transacción; si algo falla se borran.
        List<String> created = new ArrayList<>();
        Map<Long, DesignCopy> copies = new HashMap<>();
        try {
            for (CartLine line : carts.findLines(me.id())) {
                if (line.hasDesign() && !copies.containsKey(line.designId())) {
                    copies.put(line.designId(), copyDesign(me, line, created));
                }
            }
            long orderId = insertOrder(me, addressId, method, cleanNotes, now, year, copies);
            return orders.findDetail(orderId).orElseThrow();
        } catch (RuntimeException e) {
            created.forEach(storage::delete);
            throw e;
        }
    }

    private DesignCopy copyDesign(User me, CartLine line, List<String> created) {
        StoredDesign d = designs.find(line.designId())
                .filter(x -> x.userId() == me.id() && !x.deleted())
                .orElseThrow(() -> new ServiceException("El diseño de " + line.productName()
                        + " ya no existe. Quítalo del carrito para continuar."));
        DesignJson.read(d.documentJson()); // valida antes de congelarlo
        return new DesignCopy(d.summary().name(), d.summary().version(), d.documentJson(),
                copyFile(d.summary().thumbnailFront(), created), copyFile(d.summary().thumbnailBack(), created));
    }

    private String copyFile(String path, List<String> created) {
        if (path == null) {
            return null;
        }
        Path source = storage.resolve(path);
        if (!Files.exists(source)) {
            return null;
        }
        String copy = storage.store(source, "orders", Set.of(StorageService.Kind.PNG)).path();
        created.add(copy);
        return copy;
    }

    private long insertOrder(User me, long addressId, PaymentMethod method, String cleanNotes, String now, int year,
                             Map<Long, DesignCopy> copies) {
        Map<String, Material> materials = copies.isEmpty() ? Map.of() : catalog.materialsByCode();
        return db.inTransaction(c -> {
            Address address = addresses.findById(c, me.id(), addressId)
                    .orElseThrow(() -> ValidationException.of("address", "Elige una dirección de envío."));
            List<CartLine> lines = new ArrayList<>();
            for (CartLine line : carts.findLines(c, me.id())) {
                if (line.hasDesign()) {
                    DesignCopy copy = copies.get(line.designId());
                    if (copy == null) {
                        throw new ServiceException("Tu carrito cambió mientras confirmabas. Revísalo e inténtalo de nuevo.");
                    }
                    // El recargo se congela con la versión que se copia.
                    line = line.withSurcharge(DesignPricing.surchargeCents(DesignJson.read(copy.json()), materials,
                            line.garment()));
                }
                lines.add(line);
            }
            if (lines.isEmpty()) {
                throw new ServiceException("Tu carrito está vacío.");
            }
            for (CartLine line : lines) {
                if (!line.purchasable()) {
                    throw new ServiceException(line.productName() + " ya no está disponible. Quítalo del carrito para continuar.");
                }
            }
            CartSummary summary = cartService.summarize(lines);

            String number = "PED-%d-%06d".formatted(year, orders.nextSequence(c, year));
            long id = orders.insertOrder(c, number, new NewOrder(me.id(), method, summary.subtotalCents(),
                    summary.surchargeCents(), summary.shippingCents(), address, cleanNotes, now));

            for (CartLine line : lines) {
                if (!orders.reserveStock(c, line.variantId(), line.quantity())) {
                    // Cualquier excepción revierte la transacción completa: sin pedido ni reservas a medias.
                    CartService.checkStock(line.productName() + " (talla " + line.sizeName() + ")",
                            line.quantity(), line.available());
                    throw new ServiceException("No hay stock suficiente de " + line.productName() + ".");
                }
                DesignCopy copy = line.hasDesign() ? copies.get(line.designId()) : null;
                orders.insertItem(c, id, new NewItem(line.productId(), line.variantId(), line.productName(),
                        line.sku(), line.sizeName(), line.colorHex(), line.quantity(), line.unitPriceCents(),
                        line.surchargeCents(), line.designId(), copy == null ? null : copy.name(),
                        copy == null ? null : copy.version(), copy == null ? null : copy.json(),
                        copy == null ? null : copy.front(), copy == null ? null : copy.back()));
            }
            orders.insertHistory(c, id, null, OrderStatus.PENDIENTE_PAGO, me.id(), "Pedido creado", now);
            orders.insertPendingPayment(c, id, method, summary.totalCents());
            carts.clear(c, me.id());
            log.info("Pedido {} creado por el usuario {} ({} líneas, total {} céntimos)",
                    number, me.id(), lines.size(), summary.totalCents());
            return id;
        });
    }

    public List<OrderSummary> myOrders() {
        return orders.findByUser(session.requireUser().id());
    }

    /** Detalle visible para el dueño del pedido o un ADMIN. A otros se les responde como si no existiera. */
    public OrderDetail order(long orderId) {
        User me = session.requireUser();
        return orders.findDetail(orderId)
                .filter(o -> o.userId() == me.id() || me.hasRole(Role.ADMIN))
                .orElseThrow(() -> new ServiceException("No encontramos ese pedido."));
    }

    /** Cancela un pedido aún no pagado y libera el stock reservado. */
    public OrderDetail cancel(long orderId, String reason) {
        User me = session.requireUser();
        OrderDetail order = order(orderId);
        if (!CANCELLABLE.contains(order.status())) {
            throw new ServiceException("Este pedido ya no se puede cancelar (" + order.status().label().toLowerCase() + ").");
        }
        String note = Validators.blankToNull(reason) == null
                ? (order.userId() == me.id() ? "Cancelado por el cliente" : "Cancelado por la tienda")
                : reason.trim();
        String now = clock.instant().toString();
        db.inTransaction(c -> {
            if (!orders.updateStatus(c, orderId, order.status(), OrderStatus.CANCELADO)) {
                throw new ServiceException("El pedido cambió de estado. Actualiza la pantalla e inténtalo de nuevo.");
            }
            for (Reservation r : orders.reservations(c, orderId)) {
                orders.releaseStock(c, r.variantId(), r.quantity());
            }
            orders.rejectOpenPayments(c, orderId, note);
            orders.insertHistory(c, orderId, order.status(), OrderStatus.CANCELADO, me.id(), note, now);
            return null;
        });
        log.info("Pedido {} cancelado por el usuario {}", order.orderNumber(), me.id());
        return orders.findDetail(orderId).orElseThrow();
    }
}
