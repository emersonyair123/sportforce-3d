package com.sportforce3d.service;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeyDesignElement;
import com.sportforce3d.design.PgeyDesignJson;
import com.sportforce3d.design.PgeyTechSheet;
import com.sportforce3d.model.PgeyDesignAsset;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhOrderItem;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.PpqhOrderSummary;
import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyDesignRepository;
import com.sportforce3d.repository.HepyOrderRepository;
import com.sportforce3d.storage.HepyStorageService;
import com.sportforce3d.util.PpqhValidators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PpqhAdminOrderService {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhAdminOrderService.class);
    public static final int PPQH_NOTE_MAX = 300;

    private final HepyDatabase ppqhDb;
    private final HepyOrderRepository ppqhOrders;
    private final PpqhOrderService ppqhOrderService;
    private final HepyDesignRepository ppqhDesigns;
    private final HepyCatalogService ppqhCatalog;
    private final HepyStorageService ppqhStorage;
    private final PpqhSession ppqhSession;
    private final Clock ppqhClock;

    public record PpqhOrderDesign(long ppqhOrderId, String ppqhOrderNumber, PpqhOrderItem ppqhItem, PgeyDesignDocument ppqhDocument,
                              PgeyTechSheet ppqhSheet, Map<String, HepyMaterial> ppqhMaterials, List<PgeyDesignAsset> ppqhAssets) {
    }

    public PpqhAdminOrderService(HepyDatabase ppqhDb, HepyOrderRepository ppqhOrders, PpqhOrderService ppqhOrderService, HepyDesignRepository ppqhDesigns,
                             HepyCatalogService ppqhCatalog, HepyStorageService ppqhStorage, PpqhSession ppqhSession, Clock ppqhClock) {
        this.ppqhDb = ppqhDb;
        this.ppqhOrders = ppqhOrders;
        this.ppqhOrderService = ppqhOrderService;
        this.ppqhDesigns = ppqhDesigns;
        this.ppqhCatalog = ppqhCatalog;
        this.ppqhStorage = ppqhStorage;
        this.ppqhSession = ppqhSession;
        this.ppqhClock = ppqhClock;
    }

    public List<PpqhOrderSummary> ppqhList(PpqhOrderStatus ppqhStatus, String ppqhText) {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        return ppqhOrders.hepyFindForAdmin(ppqhStatus, ppqhText, 500);
    }

    public Map<PpqhOrderStatus, Integer> ppqhCounts() {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        return ppqhOrders.hepyCountByStatus();
    }

    public PpqhOrderDetail ppqhOrder(long ppqhOrderId) {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        return ppqhOrders.hepyFindDetail(ppqhOrderId).orElseThrow(() -> new PpqhServiceException("No encontramos ese pedido."));
    }

    public static List<PpqhOrderStatus> ppqhNextStates(PpqhOrderStatus ppqhCurrent) {
        return switch (ppqhCurrent) {
            case PENDIENTE_PAGO, PAGO_EN_VERIFICACION -> List.of(PpqhOrderStatus.CANCELADO);
            case PAGADO -> List.of(PpqhOrderStatus.EN_PRODUCCION);
            case EN_PRODUCCION -> List.of(PpqhOrderStatus.ENVIADO);
            case ENVIADO -> List.of(PpqhOrderStatus.ENTREGADO);
            case ENTREGADO, CANCELADO -> List.of();
        };
    }

    public PpqhOrderDetail ppqhChangeStatus(long ppqhOrderId, PpqhOrderStatus ppqhNext, String ppqhNote) {
        PpqhUser ppqhAdmin = ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        PpqhOrderDetail ppqhOrder = ppqhOrder(ppqhOrderId);
        String ppqhCleanNote = PpqhValidators.ppqhBlankToNull(ppqhNote);
        if (ppqhCleanNote != null && ppqhCleanNote.length() > PPQH_NOTE_MAX) {
            throw PpqhValidationException.ppqhOf("note", "La nota admite hasta " + PPQH_NOTE_MAX + " caracteres.");
        }
        if (PpqhValidators.ppqhPlainText(ppqhCleanNote, true) != null) {
            throw PpqhValidationException.ppqhOf("note", PpqhValidators.ppqhPlainText(ppqhCleanNote, true));
        }
        if (ppqhNext == PpqhOrderStatus.PAGADO) {
            throw new PpqhServiceException("Un pedido pasa a Pagado solo al aprobar su pago en Administración → Pagos.");
        }
        if (!ppqhNextStates(ppqhOrder.ppqhStatus()).contains(ppqhNext)) {
            throw new PpqhServiceException("No se puede pasar de " + ppqhOrder.ppqhStatus().ppqhLabel() + " a " + ppqhNext.ppqhLabel() + ".");
        }
        if (ppqhNext == PpqhOrderStatus.CANCELADO) {
            if (ppqhCleanNote == null) {
                throw PpqhValidationException.ppqhOf("note", "Escribe el motivo de la cancelación: el cliente lo verá.");
            }
            return ppqhOrderService.ppqhCancel(ppqhOrderId, ppqhCleanNote);
        }
        String ppqhNow = ppqhClock.instant().toString();
        ppqhDb.hepyInTransaction(ppqhC -> {
            if (!ppqhOrders.hepyUpdateStatus(ppqhC, ppqhOrderId, ppqhOrder.ppqhStatus(), ppqhNext)) {
                throw new PpqhServiceException("El pedido cambió de estado. Actualiza la pantalla e inténtalo de nuevo.");
            }
            ppqhOrders.hepyInsertHistory(ppqhC, ppqhOrderId, ppqhOrder.ppqhStatus(), ppqhNext, ppqhAdmin.ppqhId(), ppqhCleanNote, ppqhNow);
            return null;
        });
        ppqhLog.info("Pedido {}: {} → {} por el ADMIN {}", ppqhOrder.ppqhOrderNumber(), ppqhOrder.ppqhStatus(), ppqhNext, ppqhAdmin.ppqhId());
        return ppqhOrders.hepyFindDetail(ppqhOrderId).orElseThrow();
    }

    public PpqhOrderDesign ppqhDesign(long ppqhOrderItemId) {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        HepyOrderRepository.HepyItemWithOrder ppqhFound = ppqhOrders.hepyFindItem(ppqhOrderItemId)
                .filter(ppqhI -> ppqhI.hepyItem().ppqhHasDesign())
                .orElseThrow(() -> new PpqhServiceException("Esa línea no tiene un diseño personalizado."));
        PgeyDesignDocument ppqhDoc = PgeyDesignJson.pgeyRead(ppqhFound.hepyItem().ppqhDesignSnapshotJson());
        Map<Long, PgeyDesignAsset> ppqhAssets = new LinkedHashMap<>();
        for (PgeyDesignElement ppqhE : ppqhDoc.pgeyElements()) {
            if (ppqhE.pgeyAssetId() != null) {
                ppqhDesigns.hepyFindAsset(ppqhE.pgeyAssetId()).ifPresent(ppqhA -> ppqhAssets.put(ppqhA.hepyAsset().pgeyId(), ppqhA.hepyAsset()));
            }
        }
        Map<Long, String> ppqhNames = new LinkedHashMap<>();
        ppqhAssets.forEach((ppqhId, ppqhA) -> ppqhNames.put(ppqhId, ppqhA.pgeyOriginalName()));
        Map<String, HepyMaterial> ppqhMaterials = ppqhCatalog.hepyMaterialsByCode();
        return new PpqhOrderDesign(ppqhFound.hepyOrderId(), ppqhFound.hepyOrderNumber(), ppqhFound.hepyItem(), ppqhDoc,
                PgeyTechSheet.pgeyOf(ppqhDoc, ppqhMaterials, ppqhNames), ppqhMaterials, List.copyOf(ppqhAssets.values()));
    }

    public Path ppqhExportDesign(long ppqhOrderItemId, Path ppqhParent) {
        PpqhOrderDesign ppqhD = ppqhDesign(ppqhOrderItemId);
        Path ppqhDir = ppqhParent.resolve(ppqhD.ppqhOrderNumber() + "-linea-" + ppqhD.ppqhItem().ppqhId());
        try {
            Files.createDirectories(ppqhDir);
            Files.writeString(ppqhDir.resolve("diseno.json"), ppqhD.ppqhItem().ppqhDesignSnapshotJson(), StandardCharsets.UTF_8);
            Files.writeString(ppqhDir.resolve("ficha-tecnica.csv"),
                    ppqhD.ppqhSheet().pgeyToCsv(ppqhD.ppqhOrderNumber() + " · " + ppqhD.ppqhItem().ppqhProductName() + " · " + ppqhD.ppqhItem().ppqhDesignName()),
                    StandardCharsets.UTF_8);
            ppqhCopy(ppqhD.ppqhItem().ppqhDesignFrontPath(), ppqhDir.resolve("captura-frente.png"));
            ppqhCopy(ppqhD.ppqhItem().ppqhDesignBackPath(), ppqhDir.resolve("captura-espalda.png"));
            List<String> ppqhUsed = new ArrayList<>();
            for (PgeyDesignAsset ppqhA : ppqhD.ppqhAssets()) {
                String ppqhName = "logo-" + ppqhA.pgeyId() + "-" + ppqhSafe(ppqhA.pgeyOriginalName());
                ppqhCopy(ppqhA.pgeyPath(), ppqhDir.resolve(ppqhName));
                ppqhUsed.add(ppqhName);
            }
            ppqhLog.info("Diseño de {} línea {} exportado a {} ({} logos)", ppqhD.ppqhOrderNumber(), ppqhD.ppqhItem().ppqhId(), ppqhDir, ppqhUsed.size());
            return ppqhDir;
        } catch (IOException ppqhE) {
            throw new UncheckedIOException("No se pudo guardar los archivos en " + ppqhDir, ppqhE);
        }
    }

    private void ppqhCopy(String ppqhStoredPath, Path ppqhTarget) throws IOException {
        if (ppqhStoredPath == null) {
            return;
        }
        Path ppqhSource = ppqhStorage.hepyResolve(ppqhStoredPath);
        if (Files.exists(ppqhSource)) {
            Files.copy(ppqhSource, ppqhTarget, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String ppqhSafe(String ppqhName) {
        String ppqhS = ppqhName == null ? "logo" : ppqhName.replaceAll("[^A-Za-z0-9._-]", "_");
        return ppqhS.length() > 60 ? ppqhS.substring(ppqhS.length() - 60) : ppqhS;
    }
}
