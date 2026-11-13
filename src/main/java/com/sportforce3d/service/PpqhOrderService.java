package com.sportforce3d.service;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.design.PgeyDesignJson;
import com.sportforce3d.design.PgeyDesignPricing;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.repository.HepyDesignRepository;
import com.sportforce3d.repository.HepyDesignRepository.HepyStoredDesign;
import com.sportforce3d.storage.HepyStorageService;
import com.sportforce3d.model.PpqhAddress;
import com.sportforce3d.model.HepyCartLine;
import com.sportforce3d.model.HepyCartSummary;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.PpqhOrderSummary;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyAddressRepository;
import com.sportforce3d.repository.HepyCartRepository;
import com.sportforce3d.repository.HepyOrderRepository;
import com.sportforce3d.repository.HepyOrderRepository.HepyNewItem;
import com.sportforce3d.repository.HepyOrderRepository.HepyNewOrder;
import com.sportforce3d.repository.HepyOrderRepository.HepyReservation;
import com.sportforce3d.util.PpqhValidators;
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

public class PpqhOrderService {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhOrderService.class);
    private static final ZoneId PPQH_LIMA = ZoneId.of("America/Lima");
    private static final Set<PpqhOrderStatus> PPQH_CANCELLABLE = Set.of(PpqhOrderStatus.PENDIENTE_PAGO, PpqhOrderStatus.PAGO_EN_VERIFICACION);

    public static final int PPQH_NOTES_MAX = 300;

    private final HepyDatabase ppqhDb;
    private final HepyOrderRepository ppqhOrders;
    private final HepyCartRepository ppqhCarts;
    private final HepyAddressRepository ppqhAddresses;
    private final HepyCartService ppqhCartService;
    private final HepyDesignRepository ppqhDesigns;
    private final HepyCatalogService ppqhCatalog;
    private final HepyStorageService ppqhStorage;
    private final PpqhSession ppqhSession;
    private final Clock ppqhClock;

    private record PpqhDesignCopy(String ppqhName, int ppqhVersion, String ppqhJson, String ppqhFront, String ppqhBack) {
    }

    public PpqhOrderService(HepyDatabase ppqhDb, HepyOrderRepository ppqhOrders, HepyCartRepository ppqhCarts, HepyAddressRepository ppqhAddresses,
                        HepyCartService ppqhCartService, HepyDesignRepository ppqhDesigns, HepyCatalogService ppqhCatalog,
                        HepyStorageService ppqhStorage, PpqhSession ppqhSession, Clock ppqhClock) {
        this.ppqhDb = ppqhDb;
        this.ppqhOrders = ppqhOrders;
        this.ppqhCarts = ppqhCarts;
        this.ppqhAddresses = ppqhAddresses;
        this.ppqhCartService = ppqhCartService;
        this.ppqhDesigns = ppqhDesigns;
        this.ppqhCatalog = ppqhCatalog;
        this.ppqhStorage = ppqhStorage;
        this.ppqhSession = ppqhSession;
        this.ppqhClock = ppqhClock;
    }

    public PpqhOrderDetail ppqhPlaceOrder(long ppqhAddressId, PpqhPaymentMethod ppqhMethod, String ppqhNotes) {
        PpqhUser ppqhMe = ppqhSession.ppqhRequireUser();
        if (ppqhMethod == null) {
            throw PpqhValidationException.ppqhOf("method", "Elige un método de pago.");
        }
        if (!ppqhMethod.ppqhAvailable()) {
            throw PpqhValidationException.ppqhOf("method", "El pago con tarjeta estará disponible próximamente.");
        }
        String ppqhCleanNotes = PpqhValidators.ppqhBlankToNull(ppqhNotes);
        if (ppqhCleanNotes != null && ppqhCleanNotes.length() > PPQH_NOTES_MAX) {
            throw PpqhValidationException.ppqhOf("notes", "Las notas admiten hasta " + PPQH_NOTES_MAX + " caracteres.");
        }
        if (PpqhValidators.ppqhPlainText(ppqhCleanNotes, true) != null) {
            throw PpqhValidationException.ppqhOf("notes", PpqhValidators.ppqhPlainText(ppqhCleanNotes, true));
        }
        String ppqhNow = ppqhClock.instant().toString();
        int ppqhYear = ppqhClock.instant().atZone(PPQH_LIMA).getYear();

        List<String> ppqhCreated = new ArrayList<>();
        Map<Long, PpqhDesignCopy> ppqhCopies = new HashMap<>();
        try {
            for (HepyCartLine ppqhLine : ppqhCarts.hepyFindLines(ppqhMe.ppqhId())) {
                if (ppqhLine.hepyHasDesign() && !ppqhCopies.containsKey(ppqhLine.hepyDesignId())) {
                    ppqhCopies.put(ppqhLine.hepyDesignId(), ppqhCopyDesign(ppqhMe, ppqhLine, ppqhCreated));
                }
            }
            long ppqhOrderId = ppqhInsertOrder(ppqhMe, ppqhAddressId, ppqhMethod, ppqhCleanNotes, ppqhNow, ppqhYear, ppqhCopies);
            return ppqhOrders.hepyFindDetail(ppqhOrderId).orElseThrow();
        } catch (RuntimeException ppqhE) {
            ppqhCreated.forEach(ppqhStorage::hepyDelete);
            throw ppqhE;
        }
    }

    private PpqhDesignCopy ppqhCopyDesign(PpqhUser ppqhMe, HepyCartLine ppqhLine, List<String> ppqhCreated) {
        HepyStoredDesign ppqhD = ppqhDesigns.hepyFind(ppqhLine.hepyDesignId())
                .filter(ppqhX -> ppqhX.hepyUserId() == ppqhMe.ppqhId() && !ppqhX.hepyDeleted())
                .orElseThrow(() -> new PpqhServiceException("El diseño de " + ppqhLine.hepyProductName()
                        + " ya no existe. Quítalo del carrito para continuar."));
        PgeyDesignJson.pgeyRead(ppqhD.hepyDocumentJson());
        return new PpqhDesignCopy(ppqhD.hepySummary().pgeyName(), ppqhD.hepySummary().pgeyVersion(), ppqhD.hepyDocumentJson(),
                ppqhCopyFile(ppqhD.hepySummary().pgeyThumbnailFront(), ppqhCreated), ppqhCopyFile(ppqhD.hepySummary().pgeyThumbnailBack(), ppqhCreated));
    }

    private String ppqhCopyFile(String ppqhPath, List<String> ppqhCreated) {
        if (ppqhPath == null) {
            return null;
        }
        Path ppqhSource = ppqhStorage.hepyResolve(ppqhPath);
        if (!Files.exists(ppqhSource)) {
            return null;
        }
        String ppqhCopy = ppqhStorage.hepyStore(ppqhSource, "orders", Set.of(HepyStorageService.HepyKind.PNG)).hepyPath();
        ppqhCreated.add(ppqhCopy);
        return ppqhCopy;
    }

    private long ppqhInsertOrder(PpqhUser ppqhMe, long ppqhAddressId, PpqhPaymentMethod ppqhMethod, String ppqhCleanNotes, String ppqhNow, int ppqhYear,
                             Map<Long, PpqhDesignCopy> ppqhCopies) {
        Map<String, HepyMaterial> ppqhMaterials = ppqhCopies.isEmpty() ? Map.of() : ppqhCatalog.hepyMaterialsByCode();
        return ppqhDb.hepyInTransaction(ppqhC -> {
            PpqhAddress ppqhAddress = ppqhAddresses.hepyFindById(ppqhC, ppqhMe.ppqhId(), ppqhAddressId)
                    .orElseThrow(() -> PpqhValidationException.ppqhOf("address", "Elige una dirección de envío."));
            List<HepyCartLine> ppqhLines = new ArrayList<>();
            for (HepyCartLine ppqhLine : ppqhCarts.hepyFindLines(ppqhC, ppqhMe.ppqhId())) {
                if (ppqhLine.hepyHasDesign()) {
                    PpqhDesignCopy ppqhCopy = ppqhCopies.get(ppqhLine.hepyDesignId());
                    if (ppqhCopy == null) {
                        throw new PpqhServiceException("Tu carrito cambió mientras confirmabas. Revísalo e inténtalo de nuevo.");
                    }
                    ppqhLine = ppqhLine.hepyWithSurcharge(PgeyDesignPricing.pgeySurchargeCents(PgeyDesignJson.pgeyRead(ppqhCopy.ppqhJson()), ppqhMaterials,
                            ppqhLine.hepyGarment()));
                }
                ppqhLines.add(ppqhLine);
            }
            if (ppqhLines.isEmpty()) {
                throw new PpqhServiceException("Tu carrito está vacío.");
            }
            for (HepyCartLine ppqhLine : ppqhLines) {
                if (!ppqhLine.hepyPurchasable()) {
                    throw new PpqhServiceException(ppqhLine.hepyProductName() + " ya no está disponible. Quítalo del carrito para continuar.");
                }
            }
            HepyCartSummary ppqhSummary = ppqhCartService.hepySummarize(ppqhLines);

            String ppqhNumber = "PED-%d-%06d".formatted(ppqhYear, ppqhOrders.hepyNextSequence(ppqhC, ppqhYear));
            long ppqhId = ppqhOrders.hepyInsertOrder(ppqhC, ppqhNumber, new HepyNewOrder(ppqhMe.ppqhId(), ppqhMethod, ppqhSummary.hepySubtotalCents(),
                    ppqhSummary.hepySurchargeCents(), ppqhSummary.hepyShippingCents(), ppqhAddress, ppqhCleanNotes, ppqhNow));

            for (HepyCartLine ppqhLine : ppqhLines) {
                if (!ppqhOrders.hepyReserveStock(ppqhC, ppqhLine.hepyVariantId(), ppqhLine.hepyQuantity())) {
                    HepyCartService.hepyCheckStock(ppqhLine.hepyProductName() + " (talla " + ppqhLine.hepySizeName() + ")",
                            ppqhLine.hepyQuantity(), ppqhLine.hepyAvailable());
                    throw new PpqhServiceException("No hay stock suficiente de " + ppqhLine.hepyProductName() + ".");
                }
                PpqhDesignCopy ppqhCopy = ppqhLine.hepyHasDesign() ? ppqhCopies.get(ppqhLine.hepyDesignId()) : null;
                ppqhOrders.hepyInsertItem(ppqhC, ppqhId, new HepyNewItem(ppqhLine.hepyProductId(), ppqhLine.hepyVariantId(), ppqhLine.hepyProductName(),
                        ppqhLine.hepySku(), ppqhLine.hepySizeName(), ppqhLine.hepyColorHex(), ppqhLine.hepyQuantity(), ppqhLine.hepyUnitPriceCents(),
                        ppqhLine.hepySurchargeCents(), ppqhLine.hepyDesignId(), ppqhCopy == null ? null : ppqhCopy.ppqhName(),
                        ppqhCopy == null ? null : ppqhCopy.ppqhVersion(), ppqhCopy == null ? null : ppqhCopy.ppqhJson(),
                        ppqhCopy == null ? null : ppqhCopy.ppqhFront(), ppqhCopy == null ? null : ppqhCopy.ppqhBack()));
            }
            ppqhOrders.hepyInsertHistory(ppqhC, ppqhId, null, PpqhOrderStatus.PENDIENTE_PAGO, ppqhMe.ppqhId(), "Pedido creado", ppqhNow);
            ppqhOrders.hepyInsertPendingPayment(ppqhC, ppqhId, ppqhMethod, ppqhSummary.hepyTotalCents());
            ppqhCarts.hepyClear(ppqhC, ppqhMe.ppqhId());
            ppqhLog.info("Pedido {} creado por el usuario {} ({} líneas, total {} céntimos)",
                    ppqhNumber, ppqhMe.ppqhId(), ppqhLines.size(), ppqhSummary.hepyTotalCents());
            return ppqhId;
        });
    }

    public List<PpqhOrderSummary> ppqhMyOrders() {
        return ppqhOrders.hepyFindByUser(ppqhSession.ppqhRequireUser().ppqhId());
    }

    public PpqhOrderDetail ppqhOrder(long ppqhOrderId) {
        PpqhUser ppqhMe = ppqhSession.ppqhRequireUser();
        return ppqhOrders.hepyFindDetail(ppqhOrderId)
                .filter(ppqhO -> ppqhO.ppqhUserId() == ppqhMe.ppqhId() || ppqhMe.ppqhHasRole(PpqhRole.ADMIN))
                .orElseThrow(() -> new PpqhServiceException("No encontramos ese pedido."));
    }

    public PpqhOrderDetail ppqhCancel(long ppqhOrderId, String ppqhReason) {
        PpqhUser ppqhMe = ppqhSession.ppqhRequireUser();
        PpqhOrderDetail ppqhOrder = ppqhOrder(ppqhOrderId);
        if (!PPQH_CANCELLABLE.contains(ppqhOrder.ppqhStatus())) {
            throw new PpqhServiceException("Este pedido ya no se puede cancelar (" + ppqhOrder.ppqhStatus().ppqhLabel().toLowerCase() + ").");
        }
        String ppqhCleanReason = PpqhValidators.ppqhBlankToNull(ppqhReason);
        if (ppqhCleanReason != null && ppqhCleanReason.length() > PPQH_NOTES_MAX) {
            throw PpqhValidationException.ppqhOf("reason", "El motivo admite hasta " + PPQH_NOTES_MAX + " caracteres.");
        }
        if (PpqhValidators.ppqhPlainText(ppqhCleanReason, true) != null) {
            throw PpqhValidationException.ppqhOf("reason", PpqhValidators.ppqhPlainText(ppqhCleanReason, true));
        }
        String ppqhNote = ppqhCleanReason == null
                ? (ppqhOrder.ppqhUserId() == ppqhMe.ppqhId() ? "Cancelado por el cliente" : "Cancelado por la tienda")
                : ppqhReason.trim();
        String ppqhNow = ppqhClock.instant().toString();
        ppqhDb.hepyInTransaction(ppqhC -> {
            if (!ppqhOrders.hepyUpdateStatus(ppqhC, ppqhOrderId, ppqhOrder.ppqhStatus(), PpqhOrderStatus.CANCELADO)) {
                throw new PpqhServiceException("El pedido cambió de estado. Actualiza la pantalla e inténtalo de nuevo.");
            }
            for (HepyReservation ppqhR : ppqhOrders.hepyReservations(ppqhC, ppqhOrderId)) {
                ppqhOrders.hepyReleaseStock(ppqhC, ppqhR.hepyVariantId(), ppqhR.hepyQuantity());
            }
            ppqhOrders.hepyRejectOpenPayments(ppqhC, ppqhOrderId, ppqhNote);
            ppqhOrders.hepyInsertHistory(ppqhC, ppqhOrderId, ppqhOrder.ppqhStatus(), PpqhOrderStatus.CANCELADO, ppqhMe.ppqhId(), ppqhNote, ppqhNow);
            return null;
        });
        ppqhLog.info("Pedido {} cancelado por el usuario {}", ppqhOrder.ppqhOrderNumber(), ppqhMe.ppqhId());
        return ppqhOrders.hepyFindDetail(ppqhOrderId).orElseThrow();
    }
}
