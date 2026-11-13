package com.sportforce3d.service;

import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeyDesignElement;
import com.sportforce3d.design.PgeyDesignJson;
import com.sportforce3d.design.PgeyDesignRules;
import com.sportforce3d.design.PgeyDraftStore;
import com.sportforce3d.design.PgeyElementType;
import com.sportforce3d.model.PgeyDesignAsset;
import com.sportforce3d.model.PgeyDesignSummary;
import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyDesignRepository;
import com.sportforce3d.repository.HepyDesignRepository.HepyStoredDesign;
import com.sportforce3d.storage.HepyStorageService;
import com.sportforce3d.util.PpqhValidators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PgeyDesignService {

    private static final Logger pgeyLog = LoggerFactory.getLogger(PgeyDesignService.class);

    public record PgeyOpenedDesign(PgeyDesignSummary pgeySummary, PgeyDesignDocument pgeyDocument) {
    }

    private final HepyDesignRepository pgeyDesigns;
    private final HepyStorageService pgeyStorage;
    private final PgeyDraftStore pgeyDrafts;
    private final PpqhSession pgeySession;
    private final Clock pgeyClock;

    public PgeyDesignService(HepyDesignRepository pgeyDesigns, HepyStorageService pgeyStorage, PgeyDraftStore pgeyDrafts, PpqhSession pgeySession,
                         Clock pgeyClock) {
        this.pgeyDesigns = pgeyDesigns;
        this.pgeyStorage = pgeyStorage;
        this.pgeyDrafts = pgeyDrafts;
        this.pgeySession = pgeySession;
        this.pgeyClock = pgeyClock;
    }

    public PgeyDesignSummary pgeySave(Long pgeyDesignId, String pgeyName, PgeyDesignDocument pgeyDoc, Path pgeyFrontPng, Path pgeyBackPng) {
        PpqhUser pgeyMe = pgeySession.ppqhRequireUser();
        String pgeyCleanName = pgeyName == null ? "" : pgeyName.trim();
        if (pgeyCleanName.isEmpty() || pgeyCleanName.length() > 80) {
            throw PpqhValidationException.ppqhOf("name", "Ponle un nombre al diseño (hasta 80 caracteres).");
        }
        List<String> pgeyProblems = PgeyDesignRules.pgeyProblems(pgeyDoc);
        if (!pgeyProblems.isEmpty()) {
            throw PpqhValidationException.ppqhOf("design", pgeyProblems.get(0));
        }
        pgeyCheckAssets(pgeyMe, pgeyDoc);
        HepyStoredDesign pgeyExisting = pgeyDesignId == null ? null : pgeyOwned(pgeyMe, pgeyDesignId, false);

        String pgeyFront = pgeyFrontPng == null ? null : pgeyStorage.hepyStore(pgeyFrontPng, "designs", Set.of(HepyStorageService.HepyKind.PNG)).hepyPath();
        String pgeyBack = pgeyBackPng == null ? null : pgeyStorage.hepyStore(pgeyBackPng, "designs", Set.of(HepyStorageService.HepyKind.PNG)).hepyPath();
        String pgeyJson = PgeyDesignJson.pgeyWrite(pgeyDoc);
        long pgeyId;
        try {
            if (pgeyExisting == null) {
                pgeyId = pgeyDesigns.hepyCreate(pgeyMe.ppqhId(), pgeyDoc.pgeyProductId(), pgeyCleanName, pgeyDoc.pgeySchemaVersion(), pgeyJson, pgeyDoc.pgeyElements(), pgeyFront, pgeyBack);
            } else {
                pgeyId = pgeyDesignId;
                pgeyDesigns.hepyAddVersion(pgeyDesignId, pgeyCleanName, pgeyDoc.pgeyProductId(), pgeyDoc.pgeySchemaVersion(), pgeyJson, pgeyDoc.pgeyElements(), pgeyFront, pgeyBack);
                pgeyDeleteFile(pgeyExisting.hepySummary().pgeyThumbnailFront());
                pgeyDeleteFile(pgeyExisting.hepySummary().pgeyThumbnailBack());
            }
        } catch (RuntimeException pgeyE) {
            pgeyDeleteFile(pgeyFront);
            pgeyDeleteFile(pgeyBack);
            throw pgeyE;
        }
        pgeyDrafts.pgeyDelete(pgeyMe.ppqhId());
        pgeyLog.info("Usuario {} guardó el diseño {} ({} elementos)", pgeyMe.ppqhId(), pgeyId, pgeyDoc.pgeyElements().size());
        return pgeyDesigns.hepyFind(pgeyId).orElseThrow().hepySummary();
    }

    public List<PgeyDesignSummary> pgeyMyDesigns() {
        return pgeyDesigns.hepyFindByUser(pgeySession.ppqhRequireUser().ppqhId());
    }

    public PgeyOpenedDesign pgeyOpen(long pgeyDesignId) {
        PpqhUser pgeyMe = pgeySession.ppqhRequireUser();
        HepyStoredDesign pgeyD = pgeyOwned(pgeyMe, pgeyDesignId, true);
        return new PgeyOpenedDesign(pgeyD.hepySummary(), PgeyDesignJson.pgeyRead(pgeyD.hepyDocumentJson()));
    }

    public PgeyDesignSummary pgeyDuplicate(long pgeyDesignId) {
        PpqhUser pgeyMe = pgeySession.ppqhRequireUser();
        HepyStoredDesign pgeyD = pgeyOwned(pgeyMe, pgeyDesignId, false);
        String pgeyFront = pgeyCopy(pgeyD.hepySummary().pgeyThumbnailFront());
        String pgeyBack = pgeyCopy(pgeyD.hepySummary().pgeyThumbnailBack());
        PgeyDesignDocument pgeyDoc = PgeyDesignJson.pgeyRead(pgeyD.hepyDocumentJson());
        String pgeyName = ("Copia de " + pgeyD.hepySummary().pgeyName());
        long pgeyId = pgeyDesigns.hepyCreate(pgeyMe.ppqhId(), pgeyD.hepySummary().pgeyProductId(), pgeyName.length() > 80 ? pgeyName.substring(0, 80) : pgeyName,
                pgeyDoc.pgeySchemaVersion(), pgeyD.hepyDocumentJson(), pgeyDoc.pgeyElements(), pgeyFront, pgeyBack);
        return pgeyDesigns.hepyFind(pgeyId).orElseThrow().hepySummary();
    }

    public void pgeyDelete(long pgeyDesignId) {
        PpqhUser pgeyMe = pgeySession.ppqhRequireUser();
        pgeyOwned(pgeyMe, pgeyDesignId, false);
        pgeyDesigns.hepySoftDelete(pgeyDesignId, pgeyClock.instant());
    }

    public PgeyDesignAsset pgeyUploadLogo(Path pgeyFile, boolean pgeyRightsDeclared) {
        PpqhUser pgeyMe = pgeySession.ppqhRequireUser();
        if (pgeyFile == null || !Files.isRegularFile(pgeyFile)) {
            throw PpqhValidationException.ppqhOf("file", "No se encontró el archivo.");
        }
        if (!pgeyRightsDeclared) {
            throw PpqhValidationException.ppqhOf("rights", "Debes declarar que tienes derecho a usar este logo.");
        }
        String pgeySha = pgeySha256(pgeyFile);
        Optional<PgeyDesignAsset> pgeySame = pgeyDesigns.hepyFindAssetBySha(pgeyMe.ppqhId(), pgeySha);
        if (pgeySame.isPresent()) {
            return pgeySame.get();
        }
        HepyStorageService.HepyStoredFile pgeyStored = pgeyStorage.hepyStore(pgeyFile, "uploads",
                Set.of(HepyStorageService.HepyKind.PNG, HepyStorageService.HepyKind.SVG));
        if (pgeyStored.hepyKind() == HepyStorageService.HepyKind.SVG) {
            try (InputStream pgeyIn = Files.newInputStream(pgeyStorage.hepyResolve(pgeyStored.hepyPath()))) {
                com.sportforce3d.three.PgeySvgLoader.pgeyParse(pgeyIn);
            } catch (IllegalArgumentException | IOException pgeyE) {
                pgeyStorage.hepyDelete(pgeyStored.hepyPath());
                throw PpqhValidationException.ppqhOf("file", "Ese SVG usa funciones no compatibles. Súbelo como PNG.");
            }
        }
        String pgeyOriginal = pgeyFile.getFileName().toString();
        return pgeyDesigns.hepyInsertAsset(pgeyMe.ppqhId(), pgeyOriginal.length() > 120 ? pgeyOriginal.substring(0, 120) : pgeyOriginal,
                pgeyStored.hepyPath(), pgeyStored.hepyKind().hepyMimeType(), pgeyStored.hepySizeBytes(), pgeySha);
    }

    public List<PgeyDesignAsset> pgeyMyLogos() {
        return pgeyDesigns.hepyFindAssets(pgeySession.ppqhRequireUser().ppqhId());
    }

    public void pgeySaveDraft(Long pgeyDesignId, String pgeyName, PgeyDesignDocument pgeyDoc) {
        PpqhUser pgeyMe = pgeySession.ppqhRequireUser();
        pgeyDrafts.pgeySave(pgeyMe.ppqhId(), new PgeyDraftStore.PgeyDraft(pgeyDesignId, pgeyName, PgeyDesignJson.pgeyWrite(pgeyDoc), pgeyClock.instant().toString()));
    }

    public Optional<PgeyDraftStore.PgeyDraft> pgeyDraft() {
        return pgeySession.ppqhCurrent().flatMap(pgeyU -> pgeyDrafts.pgeyLoad(pgeyU.ppqhId()));
    }

    public void pgeyDiscardDraft() {
        pgeySession.ppqhCurrent().ifPresent(pgeyU -> pgeyDrafts.pgeyDelete(pgeyU.ppqhId()));
    }

    private HepyStoredDesign pgeyOwned(PpqhUser pgeyMe, long pgeyDesignId, boolean pgeyAdminCanRead) {
        HepyStoredDesign pgeyD = pgeyDesigns.hepyFind(pgeyDesignId).filter(pgeyX -> !pgeyX.hepyDeleted())
                .orElseThrow(() -> new PpqhServiceException("No encontramos ese diseño."));
        boolean pgeyAdmin = pgeyAdminCanRead && pgeyMe.ppqhHasRole(PpqhRole.ADMIN);
        if (pgeyD.hepyUserId() != pgeyMe.ppqhId() && !pgeyAdmin) {
            throw new PpqhServiceException("No encontramos ese diseño.");
        }
        return pgeyD;
    }

    private void pgeyCheckAssets(PpqhUser pgeyMe, PgeyDesignDocument pgeyDoc) {
        for (PgeyDesignElement pgeyE : pgeyDoc.pgeyElements()) {
            if (pgeyE.pgeyType() == PgeyElementType.LOGO) {
                var pgeyAsset = pgeyDesigns.hepyFindAsset(pgeyE.pgeyAssetId())
                        .orElseThrow(() -> PpqhValidationException.ppqhOf("design", "Un logo del diseño ya no existe."));
                if (pgeyAsset.hepyUserId() != pgeyMe.ppqhId() || !pgeyAsset.hepyAsset().pgeyPath().equals(pgeyE.pgeyAssetPath())) {
                    throw PpqhValidationException.ppqhOf("design", "Un logo del diseño no es tuyo.");
                }
            }
        }
    }

    private String pgeyCopy(String pgeyPath) {
        if (pgeyPath == null) {
            return null;
        }
        Path pgeySrc = pgeyStorage.hepyResolve(pgeyPath);
        if (!Files.isRegularFile(pgeySrc)) {
            return null;
        }
        return pgeyStorage.hepyStore(pgeySrc, "designs", Set.of(HepyStorageService.HepyKind.PNG)).hepyPath();
    }

    private void pgeyDeleteFile(String pgeyPath) {
        if (pgeyPath != null) {
            pgeyStorage.hepyDelete(pgeyPath);
        }
    }

    static String pgeySha256(Path pgeyFile) {
        try (InputStream pgeyIn = Files.newInputStream(pgeyFile)) {
            MessageDigest pgeyMd = MessageDigest.getInstance("SHA-256");
            byte[] pgeyBuf = new byte[8192];
            int pgeyN;
            while ((pgeyN = pgeyIn.read(pgeyBuf)) > 0) {
                pgeyMd.update(pgeyBuf, 0, pgeyN);
            }
            return HexFormat.of().formatHex(pgeyMd.digest());
        } catch (IOException pgeyE) {
            throw new UncheckedIOException("No se pudo leer el archivo", pgeyE);
        } catch (NoSuchAlgorithmException pgeyE) {
            throw new IllegalStateException(pgeyE);
        }
    }
}
