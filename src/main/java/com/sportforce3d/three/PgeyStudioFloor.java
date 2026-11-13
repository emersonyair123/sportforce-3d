package com.sportforce3d.three;

import com.sportforce3d.three.geom.PgeyGridSurface;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyVec3;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;

import java.util.List;

public final class PgeyStudioFloor {

    static final double PGEY_RADIUS = 1.6;
    private static final int PGEY_TEX = 1024;

    private PgeyStudioFloor() {
    }

    public static MeshView pgeyCreate(List<float[]> pgeyContactPoints, double pgeyFigureCenterX, double pgeyFigureCenterZ,
                                  Color pgeyBackground, Color pgeyFloorCenter) {
        MeshView pgeyView = new MeshView(pgeyDisc().pgeyToTriangleMesh());
        PhongMaterial pgeyMaterial = new PhongMaterial(Color.BLACK);
        pgeyMaterial.setSpecularColor(Color.BLACK);
        pgeyMaterial.setSelfIlluminationMap(pgeyTexture(pgeyContactPoints, pgeyFigureCenterX, pgeyFigureCenterZ, pgeyBackground, pgeyFloorCenter));
        pgeyView.setMaterial(pgeyMaterial);
        return pgeyView;
    }

    private static PgeyMeshData pgeyDisc() {
        int pgeyRows = 40;
        int pgeyCols = 128;
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
        for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
            double pgeyRad = PGEY_RADIUS * (pgeyR == 0 ? 0.0005 : (double) pgeyR / (pgeyRows - 1));
            for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                double pgeyA = 2 * Math.PI * pgeyC / pgeyCols;
                pgeyG.pgeySet(pgeyR, pgeyC, new PgeyVec3(pgeyRad * Math.sin(pgeyA), 0, pgeyRad * Math.cos(pgeyA)), PgeyVec3.PGEY_UP);
            }
        }
        return pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
            double pgeyRad = (double) pgeyR / (pgeyRows - 1);
            double pgeyA = 2 * Math.PI * pgeyC / pgeyCols;
            pgeyOut[0] = (float) (0.5 + 0.5 * pgeyRad * Math.sin(pgeyA));
            pgeyOut[1] = (float) (0.5 + 0.5 * pgeyRad * -Math.cos(pgeyA));
        });
    }

    private static WritableImage pgeyTexture(List<float[]> pgeyPts, double pgeyCx, double pgeyCz, Color pgeyBg, Color pgeyCenter) {
        double[] pgeyOcc = new double[PGEY_TEX * PGEY_TEX];
        for (float[] pgeyP : pgeyPts) {
            double pgeyHeight = -pgeyP[1];
            if (pgeyHeight > 0.03) {
                continue;
            }
            double pgeyWeight = 1 - pgeyHeight / 0.03;
            int pgeyPx = (int) Math.round((0.5 + pgeyP[0] / (2 * PGEY_RADIUS)) * (PGEY_TEX - 1));
            int pgeyPz = (int) Math.round((0.5 + pgeyP[2] / (2 * PGEY_RADIUS)) * (PGEY_TEX - 1));
            if (pgeyPx >= 0 && pgeyPx < PGEY_TEX && pgeyPz >= 0 && pgeyPz < PGEY_TEX) {
                pgeyOcc[pgeyPz * PGEY_TEX + pgeyPx] += pgeyWeight;
            }
        }
        double[] pgeyContact = pgeyBlur(pgeyOcc, 7);
        double[] pgeyContactWide = pgeyBlur(pgeyOcc, 22);
        double pgeyMaxC = 1e-9;
        double pgeyMaxW = 1e-9;
        for (int pgeyI = 0; pgeyI < pgeyOcc.length; pgeyI++) {
            pgeyMaxC = Math.max(pgeyMaxC, pgeyContact[pgeyI]);
            pgeyMaxW = Math.max(pgeyMaxW, pgeyContactWide[pgeyI]);
        }

        WritableImage pgeyImg = new WritableImage(PGEY_TEX, PGEY_TEX);
        PixelWriter pgeyW = pgeyImg.getPixelWriter();
        for (int pgeyY = 0; pgeyY < PGEY_TEX; pgeyY++) {
            for (int pgeyX = 0; pgeyX < PGEY_TEX; pgeyX++) {
                double pgeyU = (double) pgeyX / (PGEY_TEX - 1) - 0.5;
                double pgeyV = (double) pgeyY / (PGEY_TEX - 1) - 0.5;
                double pgeyR = Math.sqrt(pgeyU * pgeyU + pgeyV * pgeyV) * 2;
                double pgeyGlow = Math.pow(Math.max(0, 1 - pgeyR / 0.82), 1.6);
                Color pgeyBase = pgeyBg.interpolate(pgeyCenter, pgeyGlow);
                double pgeySx = (pgeyU * 2 * PGEY_RADIUS - pgeyCx) / 0.34;
                double pgeySz = (pgeyV * 2 * PGEY_RADIUS - pgeyCz) / 0.24;
                double pgeyAmbientShadow = 0.38 * Math.exp(-(pgeySx * pgeySx + pgeySz * pgeySz) * 1.4);
                double pgeyC = Math.min(1, pgeyContact[pgeyY * PGEY_TEX + pgeyX] / pgeyMaxC * 1.25);
                double pgeyCw = Math.min(1, pgeyContactWide[pgeyY * PGEY_TEX + pgeyX] / pgeyMaxW * 1.1);
                double pgeyShadow = Math.min(0.92, pgeyAmbientShadow + 0.75 * pgeyC + 0.35 * pgeyCw);
                Color pgeyOut = pgeyBase.interpolate(Color.BLACK, pgeyShadow);
                pgeyW.setColor(pgeyX, pgeyY, pgeyOut);
            }
        }
        return pgeyImg;
    }

    static double[] pgeyBlur(double[] pgeySrc, int pgeyRadius) {
        double pgeySigma = pgeyRadius / 2.0;
        double[] pgeyK = new double[pgeyRadius * 2 + 1];
        double pgeySum = 0;
        for (int pgeyI = -pgeyRadius; pgeyI <= pgeyRadius; pgeyI++) {
            pgeyK[pgeyI + pgeyRadius] = Math.exp(-(pgeyI * pgeyI) / (2 * pgeySigma * pgeySigma));
            pgeySum += pgeyK[pgeyI + pgeyRadius];
        }
        for (int pgeyI = 0; pgeyI < pgeyK.length; pgeyI++) {
            pgeyK[pgeyI] /= pgeySum;
        }
        double[] pgeyTmp = new double[pgeySrc.length];
        double[] pgeyOut = new double[pgeySrc.length];
        for (int pgeyY = 0; pgeyY < PGEY_TEX; pgeyY++) {
            for (int pgeyX = 0; pgeyX < PGEY_TEX; pgeyX++) {
                double pgeyAcc = 0;
                for (int pgeyI = -pgeyRadius; pgeyI <= pgeyRadius; pgeyI++) {
                    int pgeyXx = Math.min(PGEY_TEX - 1, Math.max(0, pgeyX + pgeyI));
                    pgeyAcc += pgeySrc[pgeyY * PGEY_TEX + pgeyXx] * pgeyK[pgeyI + pgeyRadius];
                }
                pgeyTmp[pgeyY * PGEY_TEX + pgeyX] = pgeyAcc;
            }
        }
        for (int pgeyY = 0; pgeyY < PGEY_TEX; pgeyY++) {
            for (int pgeyX = 0; pgeyX < PGEY_TEX; pgeyX++) {
                double pgeyAcc = 0;
                for (int pgeyI = -pgeyRadius; pgeyI <= pgeyRadius; pgeyI++) {
                    int pgeyYy = Math.min(PGEY_TEX - 1, Math.max(0, pgeyY + pgeyI));
                    pgeyAcc += pgeyTmp[pgeyYy * PGEY_TEX + pgeyX] * pgeyK[pgeyI + pgeyRadius];
                }
                pgeyOut[pgeyY * PGEY_TEX + pgeyX] = pgeyAcc;
            }
        }
        return pgeyOut;
    }
}
