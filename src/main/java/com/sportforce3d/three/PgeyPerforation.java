package com.sportforce3d.three;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyUvRect;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class PgeyPerforation {
    static final double PGEY_CELL_PX = 6.0;
    private static final double PGEY_DEPTH = 0.62;

    private final Map<String, float[]> pgeyMasks = new HashMap<>();
    private final Map<HepyGarment, WritableImage[]> pgeyOutputs = new EnumMap<>(HepyGarment.class);
    private final Map<HepyGarment, Integer> pgeyNext = new EnumMap<>(HepyGarment.class);
    private int[] pgeyBuf = new int[0];

    static double pgeyHole(double pgeyU, double pgeyV) {
        double pgeyRow = Math.floor(pgeyV);
        double pgeyFu = pgeyU + (((long) pgeyRow & 1) == 0 ? 0 : 0.5);
        double pgeyDu = pgeyFu - Math.floor(pgeyFu) - 0.5;
        double pgeyDv = pgeyV - pgeyRow - 0.5;
        double pgeyR = Math.sqrt(pgeyDu * pgeyDu + pgeyDv * pgeyDv);
        return Math.max(0, Math.min(1, (0.27 - pgeyR) / 0.08));
    }

    Image pgeyApply(HepyGarment pgeyG, Image pgeyTexture, List<PgeyUvRect> pgeyRects) {
        int pgeyW = (int) pgeyTexture.getWidth();
        int pgeyH = (int) pgeyTexture.getHeight();
        float[] pgeyMask = pgeyMasks.computeIfAbsent(pgeyG + ":" + pgeyW + "x" + pgeyH + ":" + pgeyRects, pgeyK -> pgeyMask(pgeyW, pgeyH, pgeyRects));
        if (pgeyBuf.length != pgeyW * pgeyH) {
            pgeyBuf = new int[pgeyW * pgeyH];
        }
        pgeyTexture.getPixelReader().getPixels(0, 0, pgeyW, pgeyH, PixelFormat.getIntArgbInstance(), pgeyBuf, 0, pgeyW);
        for (int pgeyI = 0; pgeyI < pgeyBuf.length; pgeyI++) {
            float pgeyM = pgeyMask[pgeyI];
            if (pgeyM > 0) {
                int pgeyC = pgeyBuf[pgeyI];
                double pgeyF = 1 - PGEY_DEPTH * pgeyM;
                int pgeyR = (int) (((pgeyC >> 16) & 0xFF) * pgeyF);
                int pgeyGr = (int) (((pgeyC >> 8) & 0xFF) * pgeyF);
                int pgeyB = (int) ((pgeyC & 0xFF) * pgeyF);
                pgeyBuf[pgeyI] = (pgeyC & 0xFF000000) | (pgeyR << 16) | (pgeyGr << 8) | pgeyB;
            }
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

    static float[] pgeyMask(int pgeyW, int pgeyH, List<PgeyUvRect> pgeyRects) {
        float[] pgeyMask = new float[pgeyW * pgeyH];
        double pgeyCell = PGEY_CELL_PX * pgeyW / 2048.0;
        for (PgeyUvRect pgeyR : pgeyRects) {
            int pgeyX0 = (int) Math.floor(pgeyR.pgeyU0() * pgeyW);
            int pgeyX1 = (int) Math.ceil(pgeyR.pgeyU1() * pgeyW);
            int pgeyY0 = (int) Math.floor(pgeyR.pgeyV0() * pgeyH);
            int pgeyY1 = (int) Math.ceil(pgeyR.pgeyV1() * pgeyH);
            for (int pgeyY = Math.max(0, pgeyY0); pgeyY < Math.min(pgeyH, pgeyY1); pgeyY++) {
                for (int pgeyX = Math.max(0, pgeyX0); pgeyX < Math.min(pgeyW, pgeyX1); pgeyX++) {
                    pgeyMask[pgeyY * pgeyW + pgeyX] = (float) pgeyHole((pgeyX + 0.5) / pgeyCell, (pgeyY + 0.5) / pgeyCell);
                }
            }
        }
        return pgeyMask;
    }
}
