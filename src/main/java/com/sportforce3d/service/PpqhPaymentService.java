package com.sportforce3d.service;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.PpqhPayment;
import com.sportforce3d.model.PpqhPaymentInstructions;
import com.sportforce3d.model.PpqhPaymentStatus;
import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyOrderRepository;
import com.sportforce3d.repository.HepyPaymentRepository;
import com.sportforce3d.storage.HepyStorageService;
import com.sportforce3d.util.PpqhValidators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class PpqhPaymentService {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhPaymentService.class);
    private static final Pattern PPQH_CODE = Pattern.compile("^[A-Za-z0-9-]{4,40}$");
    public static final int PPQH_REASON_MIN = 5;
    public static final int PPQH_REASON_MAX = 300;

    private final HepyDatabase ppqhDb;
    private final HepyPaymentRepository ppqhPayments;
    private final HepyOrderRepository ppqhOrders;
    private final HepyStorageService ppqhStorage;
    private final PpqhSession ppqhSession;
    private final Clock ppqhClock;

    public PpqhPaymentService(HepyDatabase ppqhDb, HepyPaymentRepository ppqhPayments, HepyOrderRepository ppqhOrders, HepyStorageService ppqhStorage,
                          PpqhSession ppqhSession, Clock ppqhClock) {
        this.ppqhDb = ppqhDb;
        this.ppqhPayments = ppqhPayments;
        this.ppqhOrders = ppqhOrders;
        this.ppqhStorage = ppqhStorage;
        this.ppqhSession = ppqhSession;
        this.ppqhClock = ppqhClock;
    }

    public PpqhPaymentInstructions ppqhInstructions() {
        ppqhSession.ppqhRequireUser();
        return ppqhPayments.hepyInstructions();
    }

    public PpqhOrderDetail ppqhRegister(long ppqhOrderId, String ppqhOperationCode, Path ppqhProof) {
        PpqhUser ppqhMe = ppqhSession.ppqhRequireUser();
        PpqhOrderDetail ppqhOrder = ppqhOrders.hepyFindDetail(ppqhOrderId)
                .filter(ppqhO -> ppqhO.ppqhUserId() == ppqhMe.ppqhId())
                .orElseThrow(() -> new PpqhServiceException("No encontramos ese pedido."));
        if (ppqhOrder.ppqhStatus() != PpqhOrderStatus.PENDIENTE_PAGO) {
            throw new PpqhServiceException("Este pedido no espera un pago (" + ppqhOrder.ppqhStatus().ppqhLabel().toLowerCase() + ").");
        }
        PpqhPayment ppqhPayment = ppqhOrder.ppqhCurrentPayment();
        if (ppqhPayment == null || ppqhPayment.ppqhStatus() != PpqhPaymentStatus.PENDIENTE) {
            throw new PpqhServiceException("Este pedido no tiene un pago pendiente. Actualiza la pantalla.");
        }
        String ppqhCode = PpqhValidators.ppqhBlankToNull(ppqhOperationCode) == null ? "" : ppqhOperationCode.trim().replace(" ", "");
        if (!PPQH_CODE.matcher(ppqhCode).matches()) {
            throw PpqhValidationException.ppqhOf("code", "Escribe el código de operación tal como aparece en tu app (4 a 40 letras o números).");
        }
        if (ppqhProof == null || !Files.isRegularFile(ppqhProof)) {
            throw PpqhValidationException.ppqhOf("proof", "Sube la captura o el PDF del comprobante.");
        }
        HepyStorageService.HepyStoredFile ppqhStored = ppqhStorage.hepyStore(ppqhProof, "payments",
                Set.of(HepyStorageService.HepyKind.PNG, HepyStorageService.HepyKind.JPEG, HepyStorageService.HepyKind.PDF));
        String ppqhNow = ppqhClock.instant().toString();
        try {
            ppqhDb.hepyInTransaction(ppqhC -> {
                if (ppqhPayments.hepyOperationCodeInUse(ppqhC, ppqhPayment.ppqhMethod(), ppqhCode)) {
                    throw PpqhValidationException.ppqhOf("code", "Ese código de operación ya se registró en otro pago.");
                }
                if (!ppqhPayments.hepySubmit(ppqhC, ppqhPayment.ppqhId(), ppqhCode, ppqhNow)
                        || !ppqhOrders.hepyUpdateStatus(ppqhC, ppqhOrderId, PpqhOrderStatus.PENDIENTE_PAGO, PpqhOrderStatus.PAGO_EN_VERIFICACION)) {
                    throw new PpqhServiceException("El pedido cambió de estado. Actualiza la pantalla e inténtalo de nuevo.");
                }
                ppqhPayments.hepyInsertProof(ppqhC, ppqhPayment.ppqhId(), ppqhStored.hepyPath(), ppqhStored.hepyKind().hepyMimeType(), ppqhStored.hepySizeBytes(), ppqhNow);
                ppqhOrders.hepyInsertHistory(ppqhC, ppqhOrderId, PpqhOrderStatus.PENDIENTE_PAGO, PpqhOrderStatus.PAGO_EN_VERIFICACION, ppqhMe.ppqhId(),
                        "Pago registrado (" + ppqhPayment.ppqhMethod().ppqhLabel() + ", operación " + ppqhCode + ")", ppqhNow);
                return null;
            });
        } catch (RuntimeException ppqhE) {
            ppqhStorage.hepyDelete(ppqhStored.hepyPath());
            throw ppqhE;
        }
        ppqhLog.info("Pago {} del pedido {} registrado por el usuario {}", ppqhPayment.ppqhId(), ppqhOrder.ppqhOrderNumber(), ppqhMe.ppqhId());
        return ppqhOrders.hepyFindDetail(ppqhOrderId).orElseThrow();
    }

    public List<PpqhPayment> ppqhQueue(PpqhPaymentStatus ppqhStatus) {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        return ppqhPayments.hepyFindByStatus(ppqhStatus == null ? PpqhPaymentStatus.EN_VERIFICACION : ppqhStatus, 200);
    }

    public int ppqhPendingCount() {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        return ppqhPayments.hepyCountByStatus(PpqhPaymentStatus.EN_VERIFICACION);
    }

    public PpqhPayment ppqhPayment(long ppqhPaymentId) {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        return ppqhPayments.hepyFind(ppqhPaymentId).orElseThrow(() -> new PpqhServiceException("No encontramos ese pago."));
    }

    public PpqhOrderDetail ppqhApprove(long ppqhPaymentId, String ppqhNote) {
        PpqhUser ppqhAdmin = ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        PpqhPayment ppqhPayment = ppqhPayment(ppqhPaymentId);
        String ppqhNow = ppqhClock.instant().toString();
        String ppqhCleanNote = PpqhValidators.ppqhBlankToNull(ppqhNote);
        ppqhDb.hepyInTransaction(ppqhC -> {
            if (!ppqhPayments.hepyApprove(ppqhC, ppqhPaymentId, ppqhAdmin.ppqhId(), ppqhNow)) {
                throw new PpqhServiceException("Este pago ya no está en verificación. Actualiza la pantalla.");
            }
            if (!ppqhOrders.hepyUpdateStatus(ppqhC, ppqhPayment.ppqhOrderId(), PpqhOrderStatus.PAGO_EN_VERIFICACION, PpqhOrderStatus.PAGADO)) {
                throw new PpqhServiceException("El pedido cambió de estado. Actualiza la pantalla e inténtalo de nuevo.");
            }
            PpqhOrderDetail ppqhOrder = ppqhOrders.hepyFindDetail(ppqhC, ppqhPayment.ppqhOrderId()).orElseThrow();
            for (HepyOrderRepository.HepyReservation ppqhR : ppqhOrders.hepyReservations(ppqhC, ppqhOrder.ppqhId())) {
                if (!ppqhOrders.hepyCommitStock(ppqhC, ppqhR.hepyVariantId(), ppqhR.hepyQuantity())) {
                    throw new PpqhServiceException("El inventario de una variante del pedido no coincide con lo reservado. "
                            + "Revísalo en Administración → Inventario antes de aprobar.");
                }
            }
            ppqhOrders.hepyInsertHistory(ppqhC, ppqhOrder.ppqhId(), PpqhOrderStatus.PAGO_EN_VERIFICACION, PpqhOrderStatus.PAGADO, ppqhAdmin.ppqhId(),
                    "Pago aprobado (" + ppqhPayment.ppqhMethod().ppqhLabel() + ", operación " + ppqhPayment.ppqhOperationCode() + ")"
                            + (ppqhCleanNote == null ? "" : ". " + ppqhCleanNote), ppqhNow);
            return null;
        });
        ppqhLog.info("Pago {} del pedido {} aprobado por el ADMIN {}", ppqhPaymentId, ppqhPayment.ppqhOrderNumber(), ppqhAdmin.ppqhId());
        return ppqhOrders.hepyFindDetail(ppqhPayment.ppqhOrderId()).orElseThrow();
    }

    public PpqhOrderDetail ppqhReject(long ppqhPaymentId, String ppqhReason) {
        PpqhUser ppqhAdmin = ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        String ppqhWhy = PpqhValidators.ppqhBlankToNull(ppqhReason) == null ? "" : ppqhReason.trim();
        if (ppqhWhy.length() < PPQH_REASON_MIN || ppqhWhy.length() > PPQH_REASON_MAX) {
            throw PpqhValidationException.ppqhOf("reason", "Escribe el motivo del rechazo (" + PPQH_REASON_MIN + " a " + PPQH_REASON_MAX
                    + " caracteres): el cliente lo verá.");
        }
        if (PpqhValidators.ppqhPlainText(ppqhWhy, true) != null) {
            throw PpqhValidationException.ppqhOf("reason", PpqhValidators.ppqhPlainText(ppqhWhy, true));
        }
        PpqhPayment ppqhPayment = ppqhPayment(ppqhPaymentId);
        String ppqhNow = ppqhClock.instant().toString();
        ppqhDb.hepyInTransaction(ppqhC -> {
            if (!ppqhPayments.hepyReject(ppqhC, ppqhPaymentId, ppqhAdmin.ppqhId(), ppqhWhy, ppqhNow)) {
                throw new PpqhServiceException("Este pago ya no está en verificación. Actualiza la pantalla.");
            }
            if (!ppqhOrders.hepyUpdateStatus(ppqhC, ppqhPayment.ppqhOrderId(), PpqhOrderStatus.PAGO_EN_VERIFICACION, PpqhOrderStatus.PENDIENTE_PAGO)) {
                throw new PpqhServiceException("El pedido cambió de estado. Actualiza la pantalla e inténtalo de nuevo.");
            }
            ppqhOrders.hepyInsertPendingPayment(ppqhC, ppqhPayment.ppqhOrderId(), ppqhPayment.ppqhMethod(), ppqhPayment.ppqhAmountCents());
            ppqhOrders.hepyInsertHistory(ppqhC, ppqhPayment.ppqhOrderId(), PpqhOrderStatus.PAGO_EN_VERIFICACION, PpqhOrderStatus.PENDIENTE_PAGO,
                    ppqhAdmin.ppqhId(), "Pago rechazado: " + ppqhWhy, ppqhNow);
            return null;
        });
        ppqhLog.info("Pago {} del pedido {} rechazado por el ADMIN {}", ppqhPaymentId, ppqhPayment.ppqhOrderNumber(), ppqhAdmin.ppqhId());
        return ppqhOrders.hepyFindDetail(ppqhPayment.ppqhOrderId()).orElseThrow();
    }
}
