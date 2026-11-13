package com.sportforce3d.three;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.human.PgeyClothShading;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

final class PgeyClothLook {
    static final Color PGEY_POLYESTER_SPECULAR = Color.gray(0.24);
    static final double PGEY_POLYESTER_POWER = 20;
    private static final double PGEY_DESATURATE = 0.45;
    private static final double PGEY_WRINKLE_RELIEF = 0.55;
    private static final double PGEY_FABRIC_ON_SIMULATED = 0.35;
    private static final int PGEY_SMOOTH_RADIUS = 12;

    private final Map<HepyGarment, PgeyClothShading> pgeyShading;
    private final Map<HepyGarment, float[]> pgeyShadeFull = new EnumMap<>(HepyGarment.class);
    private final Map<HepyGarment, int[]> pgeyPixels = new EnumMap<>(HepyGarment.class);
    private final Map<HepyGarment, WritableImage[]> pgeyOutputs = new EnumMap<>(HepyGarment.class);
    private final Map<HepyGarment, Integer> pgeyNext = new EnumMap<>(HepyGarment.class);
    private final Map<HepyGarment, Map<Image, Image>> pgeyBumps = new EnumMap<>(HepyGarment.class);
    private final Map<HepyGarment, Image> pgeyFlatBumps = new EnumMap<>(HepyGarment.class);

    PgeyClothLook(Map<HepyGarment, PgeyClothShading> pgeyShading) {
        this.pgeyShading = pgeyShading;
    }

    boolean pgeyCovers(HepyGarment pgeyG) {
        return pgeyShading.containsKey(pgeyG);
    }

    Image pgeyShade(HepyGarment pgeyG, Image pgeyTexture) {
        PgeyClothShading pgeySh = pgeyShading.get(pgeyG);
        if (pgeySh == null || pgeyTexture == null) {
            return pgeyTexture;
        }
        int pgeyW = (int) pgeyTexture.getWidth();
        int pgeyH = (int) pgeyTexture.getHeight();
        float[] pgeyS = pgeyShadeFull.computeIfAbsent(pgeyG, pgeyK -> pgeyUpsample(pgeySh, pgeyW, pgeyH));
        if (pgeyS.length != pgeyW * pgeyH) {
            pgeyS = pgeyUpsample(pgeySh, pgeyW, pgeyH);
            pgeyShadeFull.put(pgeyG, pgeyS);
        }
        int[] pgeyBuf = pgeyPixels.computeIfAbsent(pgeyG, pgeyK -> new int[pgeyW * pgeyH]);
        if (pgeyBuf.length != pgeyW * pgeyH) {
            pgeyBuf = new int[pgeyW * pgeyH];
            pgeyPixels.put(pgeyG, pgeyBuf);
        }
        pgeyTexture.getPixelReader().getPixels(0, 0, pgeyW, pgeyH, PixelFormat.getIntArgbInstance(), pgeyBuf, 0, pgeyW);
        for (int pgeyI = 0; pgeyI < pgeyBuf.length; pgeyI++) {
            int pgeyC = pgeyBuf[pgeyI];
            double pgeyF = pgeyS[pgeyI];
            double pgeyR = (pgeyC >> 16) & 0xFF;
            double pgeyGr = (pgeyC >> 8) & 0xFF;
            double pgeyB = pgeyC & 0xFF;
            double pgeyLum = 0.30 * pgeyR + 0.59 * pgeyGr + 0.11 * pgeyB;
            double pgeyK = Math.max(0, 1 - pgeyF) * PGEY_DESATURATE;
            pgeyR = (pgeyR + (pgeyLum - pgeyR) * pgeyK) * pgeyF;
            pgeyGr = (pgeyGr + (pgeyLum - pgeyGr) * pgeyK) * pgeyF;
            pgeyB = (pgeyB + (pgeyLum - pgeyB) * pgeyK) * pgeyF;
            pgeyBuf[pgeyI] = (pgeyC & 0xFF000000) | (pgeyClamp(pgeyR) << 16) | (pgeyClamp(pgeyGr) << 8) | pgeyClamp(pgeyB);
        }
        WritableImage[] pgeyPair = pgeyOutputs.computeIfAbsent(pgeyG, pgeyK -> new WritableImage[2]);
        int pgeySlot = pgeyNext.getOrDefault(pgeyG, 0);
        if (pgeyPair[pgeySlot] == null || pgeyPair[pgeySlot].getWidth() != pgeyW || pgeyPair[pgeySlot].getHeight() != pgeyH) {
            pgeyPair[pgeySlot] = new WritableImage(pgeyW, pgeyH);
        }
        pgeyPair[pgeySlot].getPixelWriter().setPixels(0, 0, pgeyW, pgeyH, PixelFormat.getIntArgbInstance(), pgeyBuf, 0, pgeyW);
        pgeyNext.put(pgeyG, 1 - pgeySlot);
        return pgeyPair[pgeySlot];
    }

    Image pgeyBump(HepyGarment pgeyG, Image pgeyBase) {
        PgeyClothShading pgeySh = pgeyShading.get(pgeyG);
        if (pgeySh == null) {
            return pgeyBase;
        }
        if (pgeyBase == null) {
            return pgeyFlatBumps.computeIfAbsent(pgeyG, pgeyK -> pgeyCombine(pgeySh.pgeyWithSmoothedNormals(PGEY_SMOOTH_RADIUS), null, 2048));
        }
        return pgeyBumps.computeIfAbsent(pgeyG, pgeyK -> new IdentityHashMap<>())
                .computeIfAbsent(pgeyBase, pgeyB -> pgeyCombine(pgeySh, pgeyB, (int) pgeyB.getWidth()));
    }

    private static WritableImage pgeyCombine(PgeyClothShading pgeySh, Image pgeyBase, int pgeySize) {
        int[] pgeySrc = new int[pgeySize * pgeySize];
        if (pgeyBase != null) {
            pgeyBase.getPixelReader().getPixels(0, 0, pgeySize, pgeySize, PixelFormat.getIntArgbInstance(), pgeySrc, 0, pgeySize);
        }
        int[] pgeyOut = new int[pgeySize * pgeySize];
        double pgeyE = 1.0 / pgeySh.pgeySize();
        for (int pgeyY = 0; pgeyY < pgeySize; pgeyY++) {
            double pgeyV = (pgeyY + 0.5) / pgeySize;
            for (int pgeyX = 0; pgeyX < pgeySize; pgeyX++) {
                double pgeyU = (pgeyX + 0.5) / pgeySize;
                double pgeyNx;
                double pgeyNy;
                double pgeyNz;
                if (pgeyBase != null) {
                    int pgeyC = pgeySrc[pgeyY * pgeySize + pgeyX];
                    pgeyNx = ((pgeyC >> 16) & 0xFF) / 255.0 * 2 - 1;
                    pgeyNy = ((pgeyC >> 8) & 0xFF) / 255.0 * 2 - 1;
                    pgeyNz = (pgeyC & 0xFF) / 255.0 * 2 - 1;
                } else {
                    pgeyNx = 0;
                    pgeyNy = 0;
                    pgeyNz = 1;
                }
                double[] pgeyBaked = pgeySh.pgeyNormalAt(pgeyU, pgeyV);
                if (pgeyBaked != null) {
                    pgeyNx = pgeyNx * PGEY_FABRIC_ON_SIMULATED + pgeyBaked[0];
                    pgeyNy = pgeyNy * PGEY_FABRIC_ON_SIMULATED + pgeyBaked[1];
                    pgeyNz = pgeyNz * pgeyBaked[2];
                } else {
                    double pgeyDhdu = (pgeySh.pgeyHeightAt(pgeyU + pgeyE, pgeyV) - pgeySh.pgeyHeightAt(pgeyU - pgeyE, pgeyV)) / 2;
                    double pgeyDhdv = (pgeySh.pgeyHeightAt(pgeyU, pgeyV + pgeyE) - pgeySh.pgeyHeightAt(pgeyU, pgeyV - pgeyE)) / 2;
                    pgeyNx -= pgeyDhdu * PGEY_WRINKLE_RELIEF;
                    pgeyNy -= pgeyDhdv * PGEY_WRINKLE_RELIEF;
                }
                double pgeyLen = Math.sqrt(pgeyNx * pgeyNx + pgeyNy * pgeyNy + pgeyNz * pgeyNz);
                int pgeyR = pgeyClamp((pgeyNx / pgeyLen * 0.5 + 0.5) * 255);
                int pgeyGg = pgeyClamp((pgeyNy / pgeyLen * 0.5 + 0.5) * 255);
                int pgeyB = pgeyClamp((pgeyNz / pgeyLen * 0.5 + 0.5) * 255);
                pgeyOut[pgeyY * pgeySize + pgeyX] = 0xFF000000 | (pgeyR << 16) | (pgeyGg << 8) | pgeyB;
            }
        }
        WritableImage pgeyImg = new WritableImage(pgeySize, pgeySize);
        pgeyImg.getPixelWriter().setPixels(0, 0, pgeySize, pgeySize, PixelFormat.getIntArgbInstance(), pgeyOut, 0, pgeySize);
        return pgeyImg;
    }

    private static float[] pgeyUpsample(PgeyClothShading pgeySh, int pgeyW, int pgeyH) {
        float[] pgeyOut = new float[pgeyW * pgeyH];
        for (int pgeyY = 0; pgeyY < pgeyH; pgeyY++) {
            double pgeyV = (pgeyY + 0.5) / pgeyH;
            for (int pgeyX = 0; pgeyX < pgeyW; pgeyX++) {
                pgeyOut[pgeyY * pgeyW + pgeyX] = (float) pgeySh.pgeyShadeAt((pgeyX + 0.5) / pgeyW, pgeyV);
            }
        }
        return pgeyOut;
    }

    private static int pgeyClamp(double pgeyV) {
        return (int) Math.max(0, Math.min(255, Math.round(pgeyV)));
    }
}
