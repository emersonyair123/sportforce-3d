package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.PgeyMeshData;

import java.util.List;

public record PgeyClothShading(int pgeySize, float[] pgeyShade, float[] pgeyHeight, int[] pgeyNormals) {
    private static final double PGEY_AO_FLOOR = 0.40;
    private static final double PGEY_AO_GAMMA = 1.25;

    public PgeyClothShading(int pgeySize, float[] pgeyShade, float[] pgeyHeight) {
        this(pgeySize, pgeyShade, pgeyHeight, null);
    }

    record PgeyPointData(float[] pgeyOpenness, float[] pgeyWrinkle, float[] pgeyAngle) {
    }

    static PgeyClothShading pgeyFromMaps(java.awt.image.BufferedImage pgeyAo, java.awt.image.BufferedImage pgeyNormal) {
        int pgeySize = pgeyAo.getWidth();
        if (pgeyAo.getHeight() != pgeySize || pgeyNormal.getWidth() != pgeySize || pgeyNormal.getHeight() != pgeySize) {
            throw new IllegalArgumentException("ao.png y normal.png deben ser cuadradas y del mismo tamaño");
        }
        int[] pgeyA = pgeyAo.getRGB(0, 0, pgeySize, pgeySize, null, 0, pgeySize);
        int[] pgeyN = pgeyNormal.getRGB(0, 0, pgeySize, pgeySize, null, 0, pgeySize);
        float[] pgeyShade = new float[pgeySize * pgeySize];
        for (int pgeyI = 0; pgeyI < pgeyA.length; pgeyI++) {
            double pgeyOpen = ((pgeyA[pgeyI] >> 8) & 0xFF) / 255.0;
            pgeyShade[pgeyI] = (float) (PGEY_AO_FLOOR + (1 - PGEY_AO_FLOOR) * Math.pow(pgeyOpen, PGEY_AO_GAMMA));
        }
        return new PgeyClothShading(pgeySize, pgeyShade, new float[pgeySize * pgeySize], pgeyN);
    }

    public PgeyClothShading pgeyWithSmoothedNormals(int pgeyRadius) {
        if (pgeyNormals == null || pgeyRadius <= 0) {
            return this;
        }
        int pgeyN = pgeySize * pgeySize;
        float[][] pgeyC = new float[3][pgeyN];
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            int pgeyP = pgeyNormals[pgeyI];
            pgeyC[0][pgeyI] = ((pgeyP >> 16) & 0xFF) / 255f * 2 - 1;
            pgeyC[1][pgeyI] = ((pgeyP >> 8) & 0xFF) / 255f * 2 - 1;
            pgeyC[2][pgeyI] = (pgeyP & 0xFF) / 255f * 2 - 1;
        }
        float[] pgeyTmp = new float[pgeyN];
        for (float[] pgeyCh : pgeyC) {
            for (int pgeyPass = 0; pgeyPass < 2; pgeyPass++) {
                pgeyBoxBlur(pgeyCh, pgeyTmp, pgeyRadius, true);
                pgeyBoxBlur(pgeyTmp, pgeyCh, pgeyRadius, false);
            }
        }
        int[] pgeyOut = new int[pgeyN];
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            double pgeyX = pgeyC[0][pgeyI];
            double pgeyY = pgeyC[1][pgeyI];
            double pgeyZ = pgeyC[2][pgeyI];
            double pgeyLen = Math.max(1e-6, Math.sqrt(pgeyX * pgeyX + pgeyY * pgeyY + pgeyZ * pgeyZ));
            pgeyOut[pgeyI] = 0xFF000000 | (pgeyChannel(pgeyX / pgeyLen) << 16) | (pgeyChannel(pgeyY / pgeyLen) << 8) | pgeyChannel(pgeyZ / pgeyLen);
        }
        return new PgeyClothShading(pgeySize, pgeyShade, pgeyHeight, pgeyOut);
    }

    private static int pgeyChannel(double pgeyV) {
        return (int) Math.max(0, Math.min(255, Math.round((pgeyV * 0.5 + 0.5) * 255)));
    }

    private void pgeyBoxBlur(float[] pgeySrc, float[] pgeyDst, int pgeyR, boolean pgeyRows) {
        float pgeyK = 1f / (2 * pgeyR + 1);
        for (int pgeyLine = 0; pgeyLine < pgeySize; pgeyLine++) {
            float pgeyAcc = 0;
            for (int pgeyT = -pgeyR; pgeyT <= pgeyR; pgeyT++) {
                pgeyAcc += pgeySrc[pgeyAt(pgeyLine, Math.max(0, Math.min(pgeySize - 1, pgeyT)), pgeyRows)];
            }
            for (int pgeyT = 0; pgeyT < pgeySize; pgeyT++) {
                pgeyDst[pgeyAt(pgeyLine, pgeyT, pgeyRows)] = pgeyAcc * pgeyK;
                int pgeyAdd = Math.min(pgeySize - 1, pgeyT + pgeyR + 1);
                int pgeyDrop = Math.max(0, pgeyT - pgeyR);
                pgeyAcc += pgeySrc[pgeyAt(pgeyLine, pgeyAdd, pgeyRows)] - pgeySrc[pgeyAt(pgeyLine, pgeyDrop, pgeyRows)];
            }
        }
    }

    private int pgeyAt(int pgeyLine, int pgeyT, boolean pgeyRows) {
        return pgeyRows ? pgeyLine * pgeySize + pgeyT : pgeyT * pgeySize + pgeyLine;
    }

    public double[] pgeyNormalAt(double pgeyU, double pgeyV) {
        if (pgeyNormals == null) {
            return null;
        }
        double pgeyX = Math.max(0, Math.min(pgeySize - 1.001, pgeyU * pgeySize - 0.5));
        double pgeyY = Math.max(0, Math.min(pgeySize - 1.001, pgeyV * pgeySize - 0.5));
        int pgeyX0 = (int) pgeyX;
        int pgeyY0 = (int) pgeyY;
        double pgeyFx = pgeyX - pgeyX0;
        double pgeyFy = pgeyY - pgeyY0;
        double[] pgeyOut = new double[3];
        int[] pgeyIdx = {pgeyY0 * pgeySize + pgeyX0, pgeyY0 * pgeySize + pgeyX0 + 1, (pgeyY0 + 1) * pgeySize + pgeyX0, (pgeyY0 + 1) * pgeySize + pgeyX0 + 1};
        double[] pgeyW = {(1 - pgeyFx) * (1 - pgeyFy), pgeyFx * (1 - pgeyFy), (1 - pgeyFx) * pgeyFy, pgeyFx * pgeyFy};
        for (int pgeyK = 0; pgeyK < 4; pgeyK++) {
            int pgeyC = pgeyNormals[pgeyIdx[pgeyK]];
            pgeyOut[0] += pgeyW[pgeyK] * (((pgeyC >> 16) & 0xFF) / 255.0 * 2 - 1);
            pgeyOut[1] += pgeyW[pgeyK] * (((pgeyC >> 8) & 0xFF) / 255.0 * 2 - 1);
            pgeyOut[2] += pgeyW[pgeyK] * ((pgeyC & 0xFF) / 255.0 * 2 - 1);
        }
        return pgeyOut;
    }

    public double pgeyShadeAt(double pgeyU, double pgeyV) {
        return pgeySample(pgeyShade, pgeyU, pgeyV);
    }

    public double pgeyHeightAt(double pgeyU, double pgeyV) {
        return pgeySample(pgeyHeight, pgeyU, pgeyV);
    }

    private double pgeySample(float[] pgeyMap, double pgeyU, double pgeyV) {
        double pgeyX = Math.max(0, Math.min(pgeySize - 1.001, pgeyU * pgeySize - 0.5));
        double pgeyY = Math.max(0, Math.min(pgeySize - 1.001, pgeyV * pgeySize - 0.5));
        int pgeyX0 = (int) pgeyX;
        int pgeyY0 = (int) pgeyY;
        double pgeyFx = pgeyX - pgeyX0;
        double pgeyFy = pgeyY - pgeyY0;
        int pgeyI = pgeyY0 * pgeySize + pgeyX0;
        double pgeyA = pgeyMap[pgeyI] * (1 - pgeyFx) + pgeyMap[pgeyI + 1] * pgeyFx;
        double pgeyB = pgeyMap[pgeyI + pgeySize] * (1 - pgeyFx) + pgeyMap[pgeyI + pgeySize + 1] * pgeyFx;
        return pgeyA * (1 - pgeyFy) + pgeyB * pgeyFy;
    }

    static PgeyClothShading pgeyBake(List<PgeyMeshData> pgeyMeshes, List<PgeyPointData> pgeyData, int pgeySize) {
        int pgeyN = pgeySize * pgeySize;
        float[] pgeyOpen = new float[pgeyN];
        float[] pgeyW = new float[pgeyN];
        float[] pgeyCa = new float[pgeyN];
        float[] pgeySa = new float[pgeyN];
        boolean[] pgeyCovered = new boolean[pgeyN];
        for (int pgeyK = 0; pgeyK < pgeyMeshes.size(); pgeyK++) {
            PgeyMeshData pgeyM = pgeyMeshes.get(pgeyK);
            PgeyPointData pgeyD = pgeyData.get(pgeyK);
            float[] pgeyTex = pgeyM.pgeyTexCoords();
            int[] pgeyF = pgeyM.pgeyFaces();
            for (int pgeyT = 0; pgeyT < pgeyM.pgeyFaceCount(); pgeyT++) {
                int pgeyB = pgeyT * PgeyMeshData.PGEY_FACE_STRIDE;
                int[] pgeyP = {pgeyF[pgeyB], pgeyF[pgeyB + 3], pgeyF[pgeyB + 6]};
                int[] pgeyTc = {pgeyF[pgeyB + 2], pgeyF[pgeyB + 5], pgeyF[pgeyB + 8]};
                if (pgeyP[0] >= pgeyD.pgeyOpenness().length || pgeyP[1] >= pgeyD.pgeyOpenness().length || pgeyP[2] >= pgeyD.pgeyOpenness().length) {
                    continue;
                }
                double[] pgeyX = new double[3];
                double[] pgeyY = new double[3];
                for (int pgeyC = 0; pgeyC < 3; pgeyC++) {
                    pgeyX[pgeyC] = pgeyTex[pgeyTc[pgeyC] * 2] * pgeySize - 0.5;
                    pgeyY[pgeyC] = pgeyTex[pgeyTc[pgeyC] * 2 + 1] * pgeySize - 0.5;
                }
                pgeyRaster(pgeyX, pgeyY, pgeySize, (pgeyI, pgeyL0, pgeyL1, pgeyL2) -> {
                    pgeyOpen[pgeyI] = (float) (pgeyL0 * pgeyD.pgeyOpenness()[pgeyP[0]] + pgeyL1 * pgeyD.pgeyOpenness()[pgeyP[1]] + pgeyL2 * pgeyD.pgeyOpenness()[pgeyP[2]]);
                    pgeyW[pgeyI] = (float) (pgeyL0 * pgeyD.pgeyWrinkle()[pgeyP[0]] + pgeyL1 * pgeyD.pgeyWrinkle()[pgeyP[1]] + pgeyL2 * pgeyD.pgeyWrinkle()[pgeyP[2]]);
                    pgeyCa[pgeyI] = (float) (pgeyL0 * Math.cos(pgeyD.pgeyAngle()[pgeyP[0]]) + pgeyL1 * Math.cos(pgeyD.pgeyAngle()[pgeyP[1]])
                            + pgeyL2 * Math.cos(pgeyD.pgeyAngle()[pgeyP[2]]));
                    pgeySa[pgeyI] = (float) (pgeyL0 * Math.sin(pgeyD.pgeyAngle()[pgeyP[0]]) + pgeyL1 * Math.sin(pgeyD.pgeyAngle()[pgeyP[1]])
                            + pgeyL2 * Math.sin(pgeyD.pgeyAngle()[pgeyP[2]]));
                    pgeyCovered[pgeyI] = true;
                });
            }
        }
        pgeyDilate(pgeyOpen, pgeyCovered.clone(), pgeySize, 4, 1);
        pgeyDilate(pgeyW, pgeyCovered.clone(), pgeySize, 4, 0);
        float[] pgeyShade = new float[pgeyN];
        float[] pgeyHeight = new float[pgeyN];
        double pgeyPeriod = pgeySize / 34.0;
        for (int pgeyY = 0; pgeyY < pgeySize; pgeyY++) {
            for (int pgeyX = 0; pgeyX < pgeySize; pgeyX++) {
                int pgeyI = pgeyY * pgeySize + pgeyX;
                double pgeyLen = Math.hypot(pgeyCa[pgeyI], pgeySa[pgeyI]);
                double pgeyC = pgeyLen < 1e-6 ? 0 : pgeyCa[pgeyI] / pgeyLen;
                double pgeyS = pgeyLen < 1e-6 ? 1 : pgeySa[pgeyI] / pgeyLen;
                double pgeyAlong = (pgeyX * pgeyC + pgeyY * pgeyS) / pgeyPeriod;
                double pgeyAcross = (-pgeyX * pgeyS + pgeyY * pgeyC) / (pgeyPeriod * 3.2);
                double pgeyWobble = 1.1 * pgeyNoise(pgeyAcross * 0.8, pgeyAlong * 0.15);
                double pgeyRidge = Math.sin(2 * Math.PI * (pgeyAlong + pgeyWobble));
                double pgeyRuido = pgeyNoise(pgeyAcross * 0.6 + 7.1, pgeyAlong * 0.45 + 3.3);
                double pgeyFade = pgeyRuido * pgeyRuido * (3 - 2 * pgeyRuido);
                pgeyFade = Math.max(0, Math.min(1, (pgeyFade - 0.25) / 0.5));
                double pgeyH = pgeyW[pgeyI] * pgeyRidge * pgeyFade;
                pgeyHeight[pgeyI] = (float) pgeyH;
                double pgeyAo = 0.42 + 0.58 * Math.pow(Math.max(0, Math.min(1, pgeyOpen[pgeyI])), 1.6);
                double pgeyFold = pgeyH < 0 ? 1 + 0.32 * pgeyH : 1 + 0.05 * pgeyH;
                pgeyShade[pgeyI] = (float) Math.max(0.35, Math.min(1.05, pgeyAo * pgeyFold));
            }
        }
        return new PgeyClothShading(pgeySize, pgeyShade, pgeyHeight);
    }

    @FunctionalInterface
    private interface PgeyPixelSink {
        void pgeyPut(int pgeyIndex, double pgeyL0, double pgeyL1, double pgeyL2);
    }

    private static void pgeyRaster(double[] pgeyX, double[] pgeyY, int pgeySize, PgeyPixelSink pgeySink) {
        int pgeyX0 = Math.max(0, (int) Math.floor(Math.min(pgeyX[0], Math.min(pgeyX[1], pgeyX[2]))));
        int pgeyX1 = Math.min(pgeySize - 1, (int) Math.ceil(Math.max(pgeyX[0], Math.max(pgeyX[1], pgeyX[2]))));
        int pgeyY0 = Math.max(0, (int) Math.floor(Math.min(pgeyY[0], Math.min(pgeyY[1], pgeyY[2]))));
        int pgeyY1 = Math.min(pgeySize - 1, (int) Math.ceil(Math.max(pgeyY[0], Math.max(pgeyY[1], pgeyY[2]))));
        double pgeyDen = (pgeyY[1] - pgeyY[2]) * (pgeyX[0] - pgeyX[2]) + (pgeyX[2] - pgeyX[1]) * (pgeyY[0] - pgeyY[2]);
        if (Math.abs(pgeyDen) < 1e-12) {
            return;
        }
        for (int pgeyPy = pgeyY0; pgeyPy <= pgeyY1; pgeyPy++) {
            for (int pgeyPx = pgeyX0; pgeyPx <= pgeyX1; pgeyPx++) {
                double pgeyL0 = ((pgeyY[1] - pgeyY[2]) * (pgeyPx - pgeyX[2]) + (pgeyX[2] - pgeyX[1]) * (pgeyPy - pgeyY[2])) / pgeyDen;
                double pgeyL1 = ((pgeyY[2] - pgeyY[0]) * (pgeyPx - pgeyX[2]) + (pgeyX[0] - pgeyX[2]) * (pgeyPy - pgeyY[2])) / pgeyDen;
                double pgeyL2 = 1 - pgeyL0 - pgeyL1;
                if (pgeyL0 >= -1e-6 && pgeyL1 >= -1e-6 && pgeyL2 >= -1e-6) {
                    pgeySink.pgeyPut(pgeyPy * pgeySize + pgeyPx, pgeyL0, pgeyL1, pgeyL2);
                }
            }
        }
    }

    private static void pgeyDilate(float[] pgeyMap, boolean[] pgeyCovered, int pgeySize, int pgeyPasses, float pgeyFill) {
        for (int pgeyPass = 0; pgeyPass < pgeyPasses; pgeyPass++) {
            boolean[] pgeyNext = pgeyCovered.clone();
            for (int pgeyY = 0; pgeyY < pgeySize; pgeyY++) {
                for (int pgeyX = 0; pgeyX < pgeySize; pgeyX++) {
                    int pgeyI = pgeyY * pgeySize + pgeyX;
                    if (pgeyCovered[pgeyI]) {
                        continue;
                    }
                    double pgeySum = 0;
                    int pgeyC = 0;
                    for (int pgeyDy = -1; pgeyDy <= 1; pgeyDy++) {
                        for (int pgeyDx = -1; pgeyDx <= 1; pgeyDx++) {
                            int pgeyXx = pgeyX + pgeyDx;
                            int pgeyYy = pgeyY + pgeyDy;
                            if (pgeyXx >= 0 && pgeyYy >= 0 && pgeyXx < pgeySize && pgeyYy < pgeySize && pgeyCovered[pgeyYy * pgeySize + pgeyXx]) {
                                pgeySum += pgeyMap[pgeyYy * pgeySize + pgeyXx];
                                pgeyC++;
                            }
                        }
                    }
                    if (pgeyC > 0) {
                        pgeyMap[pgeyI] = (float) (pgeySum / pgeyC);
                        pgeyNext[pgeyI] = true;
                    }
                }
            }
            System.arraycopy(pgeyNext, 0, pgeyCovered, 0, pgeyCovered.length);
        }
        for (int pgeyI = 0; pgeyI < pgeyMap.length; pgeyI++) {
            if (!pgeyCovered[pgeyI]) {
                pgeyMap[pgeyI] = pgeyFill;
            }
        }
    }

    static double pgeyNoise(double pgeyX, double pgeyY) {
        int pgeyXi = (int) Math.floor(pgeyX);
        int pgeyYi = (int) Math.floor(pgeyY);
        double pgeyFx = pgeyX - pgeyXi;
        double pgeyFy = pgeyY - pgeyYi;
        double pgeySx = pgeyFx * pgeyFx * (3 - 2 * pgeyFx);
        double pgeySy = pgeyFy * pgeyFy * (3 - 2 * pgeyFy);
        double pgeyA = pgeyHash(pgeyXi, pgeyYi);
        double pgeyB = pgeyHash(pgeyXi + 1, pgeyYi);
        double pgeyC = pgeyHash(pgeyXi, pgeyYi + 1);
        double pgeyD = pgeyHash(pgeyXi + 1, pgeyYi + 1);
        return (pgeyA + (pgeyB - pgeyA) * pgeySx) * (1 - pgeySy) + (pgeyC + (pgeyD - pgeyC) * pgeySx) * pgeySy;
    }

    private static double pgeyHash(int pgeyX, int pgeyY) {
        long pgeyH = pgeyX * 374761393L + pgeyY * 668265263L;
        pgeyH = (pgeyH ^ (pgeyH >>> 13)) * 1274126177L;
        return ((pgeyH ^ (pgeyH >>> 16)) & 0xFFFF) / 65535.0;
    }
}
