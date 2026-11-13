package com.sportforce3d.render;

import java.util.Map;

public final class PgeyRecolor {
    static final float PGEY_SHADOW = 0.85f;
    static final float PGEY_DETAIL = 0.85f;
    private static final float[] PGEY_UNIT = pgeyUnit();

    private PgeyRecolor() {
    }

    private static float[] pgeyUnit() {
        float[] pgeyT = new float[256];
        for (int pgeyI = 0; pgeyI < 256; pgeyI++) {
            pgeyT[pgeyI] = pgeyI / 255f;
        }
        return pgeyT;
    }

    public static PgeyArgbImage pgeyCompose(PgeyModelLayers pgeyLayers, Map<String, Integer> pgeyColors, PgeyArgbImage pgeyOverlay) {
        int pgeyW = pgeyLayers.pgeyWidth();
        int pgeyH = pgeyLayers.pgeyHeight();
        int pgeyN = pgeyW * pgeyH;
        int[] pgeyBase = pgeyLayers.pgeyBase().pgeyPixels();
        float[] pgeyR = new float[pgeyN];
        float[] pgeyG = new float[pgeyN];
        float[] pgeyB = new float[pgeyN];
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            int pgeyC = pgeyBase[pgeyI];
            pgeyR[pgeyI] = PGEY_UNIT[(pgeyC >> 16) & 0xFF];
            pgeyG[pgeyI] = PGEY_UNIT[(pgeyC >> 8) & 0xFF];
            pgeyB[pgeyI] = PGEY_UNIT[pgeyC & 0xFF];
        }

        for (Map.Entry<String, Integer> pgeyE : pgeyColors.entrySet()) {
            PgeyMask pgeyM = pgeyLayers.pgeyZones().get(pgeyE.getKey());
            if (pgeyM == null || pgeyM.pgeyIsEmpty()) {
                continue;
            }
            int pgeyC = pgeyE.getValue();
            float pgeyKr = 1 - PGEY_UNIT[(pgeyC >> 16) & 0xFF];
            float pgeyKg = 1 - PGEY_UNIT[(pgeyC >> 8) & 0xFF];
            float pgeyKb = 1 - PGEY_UNIT[pgeyC & 0xFF];
            byte[] pgeyAlpha = pgeyM.pgeyAlpha();
            for (int pgeyY = 0; pgeyY < pgeyM.pgeyHeight(); pgeyY++) {
                int pgeySrc = pgeyY * pgeyM.pgeyWidth();
                int pgeyDst = (pgeyM.pgeyY() + pgeyY) * pgeyW + pgeyM.pgeyX();
                for (int pgeyX = 0; pgeyX < pgeyM.pgeyWidth(); pgeyX++) {
                    int pgeyAv = pgeyAlpha[pgeySrc + pgeyX] & 0xFF;
                    if (pgeyAv == 0) {
                        continue;
                    }
                    float pgeyA = PGEY_UNIT[pgeyAv];
                    int pgeyI = pgeyDst + pgeyX;
                    pgeyR[pgeyI] *= 1 - pgeyA * pgeyKr;
                    pgeyG[pgeyI] *= 1 - pgeyA * pgeyKg;
                    pgeyB[pgeyI] *= 1 - pgeyA * pgeyKb;
                }
            }
        }

        PgeyMask pgeyCam = pgeyLayers.pgeyZone(PgeyRealisticModel.PGEY_CAMISETA);
        if (pgeyOverlay != null && !pgeyCam.pgeyIsEmpty()) {
            if (pgeyOverlay.pgeyWidth() != pgeyW || pgeyOverlay.pgeyHeight() != pgeyH) {
                throw new IllegalArgumentException("El diseño debe medir lo mismo que la foto");
            }
            int[] pgeyOver = pgeyOverlay.pgeyPixels();
            byte[] pgeyAlpha = pgeyCam.pgeyAlpha();
            for (int pgeyY = 0; pgeyY < pgeyCam.pgeyHeight(); pgeyY++) {
                int pgeySrc = pgeyY * pgeyCam.pgeyWidth();
                int pgeyDst = (pgeyCam.pgeyY() + pgeyY) * pgeyW + pgeyCam.pgeyX();
                for (int pgeyX = 0; pgeyX < pgeyCam.pgeyWidth(); pgeyX++) {
                    int pgeyAv = pgeyAlpha[pgeySrc + pgeyX] & 0xFF;
                    if (pgeyAv == 0) {
                        continue;
                    }
                    float pgeyACam = PGEY_UNIT[pgeyAv];
                    int pgeyI = pgeyDst + pgeyX;
                    int pgeyO = pgeyOver[pgeyI];
                    float pgeyAo = PGEY_UNIT[pgeyO >>> 24] * pgeyACam;
                    if (pgeyAo > 0) {
                        pgeyR[pgeyI] = pgeyR[pgeyI] * (1 - pgeyAo) + PGEY_UNIT[(pgeyO >> 16) & 0xFF] * pgeyAo;
                        pgeyG[pgeyI] = pgeyG[pgeyI] * (1 - pgeyAo) + PGEY_UNIT[(pgeyO >> 8) & 0xFF] * pgeyAo;
                        pgeyB[pgeyI] = pgeyB[pgeyI] * (1 - pgeyAo) + PGEY_UNIT[pgeyO & 0xFF] * pgeyAo;
                    }
                    float pgeyK = pgeyACam * PGEY_SHADOW;
                    int pgeyC = pgeyBase[pgeyI];
                    pgeyR[pgeyI] *= 1 - pgeyK * (1 - PGEY_UNIT[(pgeyC >> 16) & 0xFF]);
                    pgeyG[pgeyI] *= 1 - pgeyK * (1 - PGEY_UNIT[(pgeyC >> 8) & 0xFF]);
                    pgeyB[pgeyI] *= 1 - pgeyK * (1 - PGEY_UNIT[pgeyC & 0xFF]);
                }
            }
        }

        PgeyMask pgeyDg = pgeyLayers.pgeyDetailGray();
        PgeyMask pgeyDa = pgeyLayers.pgeyDetailAlpha();
        if (!pgeyDa.pgeyIsEmpty()) {
            byte[] pgeyGray = pgeyDg.pgeyAlpha();
            byte[] pgeyAlpha = pgeyDa.pgeyAlpha();
            for (int pgeyY = 0; pgeyY < pgeyDa.pgeyHeight(); pgeyY++) {
                int pgeySrc = pgeyY * pgeyDa.pgeyWidth();
                int pgeyDst = (pgeyDa.pgeyY() + pgeyY) * pgeyW + pgeyDa.pgeyX();
                for (int pgeyX = 0; pgeyX < pgeyDa.pgeyWidth(); pgeyX++) {
                    int pgeyAv = pgeyAlpha[pgeySrc + pgeyX] & 0xFF;
                    if (pgeyAv == 0) {
                        continue;
                    }
                    float pgeyAd = PGEY_UNIT[pgeyAv] * PGEY_DETAIL;
                    float pgeyD = PGEY_UNIT[pgeyGray[pgeySrc + pgeyX] & 0xFF];
                    int pgeyI = pgeyDst + pgeyX;
                    pgeyR[pgeyI] = pgeyOverlayBlend(pgeyR[pgeyI], pgeyD, pgeyAd);
                    pgeyG[pgeyI] = pgeyOverlayBlend(pgeyG[pgeyI], pgeyD, pgeyAd);
                    pgeyB[pgeyI] = pgeyOverlayBlend(pgeyB[pgeyI], pgeyD, pgeyAd);
                }
            }
        }

        int[] pgeyOut = new int[pgeyN];
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            pgeyOut[pgeyI] = (pgeyBase[pgeyI] & 0xFF000000) | (pgeyTo255(pgeyR[pgeyI]) << 16) | (pgeyTo255(pgeyG[pgeyI]) << 8) | pgeyTo255(pgeyB[pgeyI]);
        }
        return new PgeyArgbImage(pgeyW, pgeyH, pgeyOut);
    }

    private static float pgeyOverlayBlend(float pgeyRes, float pgeyD, float pgeyAd) {
        float pgeyOv = pgeyRes < 0.5f ? 2 * pgeyRes * pgeyD : 1 - 2 * (1 - pgeyRes) * (1 - pgeyD);
        return pgeyRes * (1 - pgeyAd) + pgeyOv * pgeyAd;
    }

    private static int pgeyTo255(float pgeyV) {
        int pgeyI = (int) (pgeyV * 255f + 0.5f);
        return pgeyI < 0 ? 0 : Math.min(pgeyI, 255);
    }

    public static PgeyArgbImage pgeyDownscale(PgeyArgbImage pgeySrc, int pgeyDw, int pgeyDh) {
        int pgeySw = pgeySrc.pgeyWidth();
        int pgeySh = pgeySrc.pgeyHeight();
        if (pgeyDw <= 0 || pgeyDh <= 0 || pgeyDw > pgeySw || pgeyDh > pgeySh) {
            throw new IllegalArgumentException("Solo reduce: " + pgeySw + "×" + pgeySh + " → " + pgeyDw + "×" + pgeyDh);
        }
        int[] pgeyP = pgeySrc.pgeyPixels();
        int[] pgeyOut = new int[pgeyDw * pgeyDh];
        for (int pgeyDy = 0; pgeyDy < pgeyDh; pgeyDy++) {
            int pgeyY0 = (int) ((long) pgeyDy * pgeySh / pgeyDh);
            int pgeyY1 = Math.max(pgeyY0 + 1, (int) ((long) (pgeyDy + 1) * pgeySh / pgeyDh));
            for (int pgeyDx = 0; pgeyDx < pgeyDw; pgeyDx++) {
                int pgeyX0 = (int) ((long) pgeyDx * pgeySw / pgeyDw);
                int pgeyX1 = Math.max(pgeyX0 + 1, (int) ((long) (pgeyDx + 1) * pgeySw / pgeyDw));
                long pgeySa = 0;
                long pgeySr = 0;
                long pgeySg = 0;
                long pgeySb = 0;
                for (int pgeyY = pgeyY0; pgeyY < pgeyY1; pgeyY++) {
                    int pgeyRow = pgeyY * pgeySw;
                    for (int pgeyX = pgeyX0; pgeyX < pgeyX1; pgeyX++) {
                        int pgeyC = pgeyP[pgeyRow + pgeyX];
                        int pgeyA = pgeyC >>> 24;
                        pgeySa += pgeyA;
                        pgeySr += (long) ((pgeyC >> 16) & 0xFF) * pgeyA;
                        pgeySg += (long) ((pgeyC >> 8) & 0xFF) * pgeyA;
                        pgeySb += (long) (pgeyC & 0xFF) * pgeyA;
                    }
                }
                int pgeyCount = (pgeyY1 - pgeyY0) * (pgeyX1 - pgeyX0);
                int pgeyArgb = 0;
                if (pgeySa > 0) {
                    int pgeyA = (int) ((pgeySa + pgeyCount / 2) / pgeyCount);
                    pgeyArgb = (pgeyA << 24)
                            | (int) ((pgeySr + pgeySa / 2) / pgeySa) << 16
                            | (int) ((pgeySg + pgeySa / 2) / pgeySa) << 8
                            | (int) ((pgeySb + pgeySa / 2) / pgeySa);
                }
                pgeyOut[pgeyDy * pgeyDw + pgeyDx] = pgeyArgb;
            }
        }
        return new PgeyArgbImage(pgeyDw, pgeyDh, pgeyOut);
    }
}
