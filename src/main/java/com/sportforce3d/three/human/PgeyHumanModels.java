package com.sportforce3d.three.human;

import com.sportforce3d.design.PgeyGarmentCut;
import com.sportforce3d.three.model.PgeyModelProviders;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PgeyHumanModels {

    private static final Logger pgeyLog = LoggerFactory.getLogger(PgeyHumanModels.class);
    private static final int PGEY_CACHE_SIZE = 2;
    private static final double PGEY_MIN_SPECULAR = 0.08;

    private static final Map<PgeyPersona, PgeyHumanModel> PGEY_CACHE = new LinkedHashMap<>(4, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<PgeyPersona, PgeyHumanModel> pgeyEldest) {
            return size() > PGEY_CACHE_SIZE;
        }
    };

    private record PgeyDressKey(PgeyPersona pgeyPersona, PgeyGarmentCut pgeyCut) {
    }

    private static final Map<PgeyDressKey, PgeyOutfitFitter.PgeyDressedPerson> PGEY_DRESSED = new LinkedHashMap<>(4, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<PgeyDressKey, PgeyOutfitFitter.PgeyDressedPerson> pgeyEldest) {
            return size() > PGEY_CACHE_SIZE + 1;
        }
    };

    private PgeyHumanModels() {
    }

    public static PgeyOutfitFitter.PgeyDressedPerson pgeyDress(PgeyPersona pgeyPersona) {
        return pgeyDress(pgeyPersona, PgeyGarmentCut.PGEY_DEFAULT);
    }

    public static PgeyOutfitFitter.PgeyDressedPerson pgeyDress(PgeyPersona pgeyPersona, PgeyGarmentCut pgeyCut) {
        PgeyDressKey pgeyKey = new PgeyDressKey(pgeyPersona, pgeyCut);
        synchronized (PGEY_DRESSED) {
            PgeyOutfitFitter.PgeyDressedPerson pgeyCached = PGEY_DRESSED.get(pgeyKey);
            if (pgeyCached != null) {
                return pgeyCached;
            }
        }
        PgeyHumanModel pgeyHuman = pgeyLoad(pgeyPersona);
        long pgeyStart = System.nanoTime();
        var pgeySimulated = PgeySimulatedGarments.pgeyLoad(pgeyPersona, pgeyCut);
        PgeyOutfitFitter.PgeyDressedPerson pgeyDressed = PgeyOutfitFitter.pgeyFit(pgeyHuman, PgeyModelProviders.pgeyLoad(pgeyPersona.pgeyFallback()), pgeySimulated);
        pgeyLog.info("Uniforme ajustado a {} en {} ms (tela simulada: {}; {})", pgeyPersona.pgeyKey(),
                (System.nanoTime() - pgeyStart) / 1_000_000, pgeySimulated.keySet(), pgeyCut.pgeySummary());
        synchronized (PGEY_DRESSED) {
            PGEY_DRESSED.put(pgeyKey, pgeyDressed);
        }
        return pgeyDressed;
    }

    public static boolean pgeyAvailable(PgeyPersona pgeyPersona) {
        return pgeyPersona != null && PgeyHumanModels.class.getResource(pgeyPersona.pgeyObjResource()) != null;
    }

    public static PgeyObjMtlReader.PgeyResult pgeyReadGeometry(PgeyPersona pgeyPersona) {
        try (InputStream pgeyIn = PgeyHumanModels.class.getResourceAsStream(pgeyPersona.pgeyObjResource())) {
            if (pgeyIn == null) {
                throw new IllegalStateException("No existe " + pgeyPersona.pgeyObjResource());
            }
            return PgeyObjMtlReader.pgeyRead(new InputStreamReader(pgeyIn, StandardCharsets.UTF_8), pgeyPath -> pgeyOpen(pgeyPersona, pgeyPath));
        } catch (IOException pgeyE) {
            throw new UncheckedIOException(pgeyE);
        }
    }

    public static PgeyHumanModel pgeyLoad(PgeyPersona pgeyPersona) {
        synchronized (PGEY_CACHE) {
            PgeyHumanModel pgeyCached = PGEY_CACHE.get(pgeyPersona);
            if (pgeyCached != null) {
                return pgeyCached;
            }
        }
        long pgeyStart = System.nanoTime();
        PgeyObjMtlReader.PgeyResult pgeyGeometry = pgeyReadGeometry(pgeyPersona);
        Map<String, PgeyTexture> pgeyTextures = new HashMap<>();
        List<PgeyHumanModel.PgeyPart> pgeyOpaque = new ArrayList<>();
        List<PgeyHumanModel.PgeyPart> pgeyTransparent = new ArrayList<>();
        for (PgeyObjMtlReader.PgeyGroup pgeyG : pgeyGeometry.pgeyGroups()) {
            PgeyObjMtlReader.PgeyMaterial pgeyM = pgeyGeometry.pgeyMaterial(pgeyG.pgeyMaterial());
            PgeyTexture pgeyTex = pgeyM.pgeyDiffuseMap() == null ? null
                    : pgeyTextures.computeIfAbsent(pgeyM.pgeyDiffuseMap(), pgeyP -> pgeyTexture(pgeyPersona, pgeyP));
            boolean pgeyAlpha = pgeyTex != null && pgeyTex.pgeyAlpha();
            double pgeyKs = Math.max(PGEY_MIN_SPECULAR, pgeyAvg(pgeyM.pgeySpecular()));
            PgeyHumanModel.PgeyPart pgeyPart = new PgeyHumanModel.PgeyPart(pgeyG.pgeyMaterial(), pgeyG.pgeyMesh(),
                    Color.color(pgeyM.pgeyDiffuse()[0], pgeyM.pgeyDiffuse()[1], pgeyM.pgeyDiffuse()[2]), Color.gray(Math.min(1, pgeyKs)),
                    pgeyM.pgeyShininess() > 0 ? pgeyM.pgeyShininess() : 24, pgeyTex == null ? null : pgeyTex.pgeyImage(), pgeyAlpha);
            (pgeyAlpha ? pgeyTransparent : pgeyOpaque).add(pgeyPart);
        }
        List<PgeyHumanModel.PgeyPart> pgeyParts = new ArrayList<>(pgeyOpaque);
        pgeyParts.addAll(pgeyTransparent);
        PgeyHumanModel pgeyModel = new PgeyHumanModel(pgeyPersona, pgeyParts, pgeyGeometry.pgeyMin()[1], pgeyGeometry.pgeyHeight());
        pgeyLog.info("Persona 3D {} cargada en {} ms ({} materiales)", pgeyPersona.pgeyKey(),
                (System.nanoTime() - pgeyStart) / 1_000_000, pgeyParts.size());
        synchronized (PGEY_CACHE) {
            PGEY_CACHE.put(pgeyPersona, pgeyModel);
        }
        return pgeyModel;
    }

    private record PgeyTexture(Image pgeyImage, boolean pgeyAlpha) {
    }

    private static PgeyTexture pgeyTexture(PgeyPersona pgeyPersona, String pgeyPath) {
        try (InputStream pgeyIn = PgeyHumanModels.class.getResourceAsStream(pgeyPersona.pgeyFolder() + pgeyPath)) {
            if (pgeyIn == null) {
                pgeyLog.warn("Falta la textura {}{}", pgeyPersona.pgeyFolder(), pgeyPath);
                return null;
            }
            byte[] pgeyBytes = pgeyIn.readAllBytes();
            Image pgeyImage = new Image(new ByteArrayInputStream(pgeyBytes));
            if (pgeyImage.isError()) {
                pgeyLog.warn("No se pudo leer la textura {}{}", pgeyPersona.pgeyFolder(), pgeyPath, pgeyImage.getException());
                return null;
            }
            return new PgeyTexture(pgeyImage, pgeyPngHasAlpha(pgeyBytes));
        } catch (IOException pgeyE) {
            throw new UncheckedIOException(pgeyE);
        }
    }

    static boolean pgeyPngHasAlpha(byte[] pgeyPng) {
        if (pgeyPng.length < 33 || (pgeyPng[0] & 0xFF) != 0x89 || pgeyPng[1] != 'P' || pgeyPng[2] != 'N' || pgeyPng[3] != 'G') {
            return false;
        }
        int pgeyColorType = pgeyPng[25] & 0xFF;
        if (pgeyColorType == 4 || pgeyColorType == 6) {
            return true;
        }
        int pgeyPos = 8;
        while (pgeyPos + 8 <= pgeyPng.length) {
            int pgeyLen = ((pgeyPng[pgeyPos] & 0xFF) << 24) | ((pgeyPng[pgeyPos + 1] & 0xFF) << 16) | ((pgeyPng[pgeyPos + 2] & 0xFF) << 8)
                    | (pgeyPng[pgeyPos + 3] & 0xFF);
            String pgeyType = new String(pgeyPng, pgeyPos + 4, 4, StandardCharsets.US_ASCII);
            if (pgeyType.equals("tRNS")) {
                return true;
            }
            if (pgeyType.equals("IDAT") || pgeyLen < 0) {
                return false;
            }
            pgeyPos += 12 + pgeyLen;
        }
        return false;
    }

    private static Reader pgeyOpen(PgeyPersona pgeyPersona, String pgeyPath) {
        InputStream pgeyIn = PgeyHumanModels.class.getResourceAsStream(pgeyPersona.pgeyFolder() + pgeyPath);
        return pgeyIn == null ? null : new InputStreamReader(pgeyIn, StandardCharsets.UTF_8);
    }

    private static double pgeyAvg(double[] pgeyRgb) {
        return (pgeyRgb[0] + pgeyRgb[1] + pgeyRgb[2]) / 3;
    }
}
