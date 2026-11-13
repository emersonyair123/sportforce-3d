package com.sportforce3d.three;

import com.sportforce3d.model.HepyMaterial;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.EnumMap;
import java.util.Map;

public final class PgeyFabricMaps {

    private static final Map<HepyMaterial.HepyPattern, WritableImage> PGEY_CACHE = new EnumMap<>(HepyMaterial.HepyPattern.class);

    private PgeyFabricMaps() {
    }

    public static WritableImage pgeyDryFit() {
        return pgeyForPattern(HepyMaterial.HepyPattern.DOTS);
    }

    public static synchronized WritableImage pgeyForPattern(HepyMaterial.HepyPattern pgeyPattern) {
        if (PGEY_CACHE.containsKey(pgeyPattern)) {
            return PGEY_CACHE.get(pgeyPattern);
        }
        WritableImage pgeyImg = switch (pgeyPattern) {
            case DOTS -> pgeyNormalMap(2048, 2.0, 0.035, (pgeyU, pgeyV) -> {
                double pgeyRow = Math.floor(pgeyV);
                double pgeyFu = pgeyU + (pgeyRow % 2 == 0 ? 0 : 0.5);
                double pgeyDu = pgeyFu - Math.floor(pgeyFu) - 0.5;
                double pgeyDv = pgeyV - pgeyRow - 0.5;
                return -Math.exp(-(pgeyDu * pgeyDu + pgeyDv * pgeyDv * 1.3) * 18);
            });
            case MESH -> pgeyNormalMap(2048, PgeyPerforation.PGEY_CELL_PX, 0.12, (pgeyU, pgeyV) -> -PgeyPerforation.pgeyHole(pgeyU, pgeyV));
            case PLAIN -> pgeyNormalMap(2048, 2.5, 0.14, (pgeyU, pgeyV) -> Math.abs(pgeyU - Math.floor(pgeyU) - 0.5)
                    + Math.sin(pgeyV * Math.PI * 2) * 0.25 + pgeyNoise(pgeyU, pgeyV) * 0.15);
            case DIAGONAL -> pgeyNormalMap(2048, 2.5, 0.035, (pgeyU, pgeyV) -> Math.sin((pgeyU + pgeyV) * Math.PI * 2) * 0.5);
            case LINES -> pgeyNormalMap(2048, 1.75, 0.04, (pgeyU, pgeyV) -> Math.sin(pgeyV * Math.PI * 2) * 0.5);
            case GRID -> pgeyNormalMap(2048, 1.5, 0.03, (pgeyU, pgeyV) -> Math.sin(pgeyU * Math.PI * 2) * Math.sin(pgeyV * Math.PI * 2));
            case KNIT -> pgeyNormalMap(1024, 6.0, 0.18, (pgeyU, pgeyV) -> Math.abs(((pgeyU % 1) + 1) % 1 - 0.5)
                    + Math.sin(pgeyV * Math.PI * 2) * 0.25);
            case GRAIN -> pgeyNormalMap(1024, 5.0, 0.12, PgeyFabricMaps::pgeyNoise);
            case RUBBER -> pgeyNormalMap(1024, 8.0, 0.14, (pgeyU, pgeyV) -> Math.sin(pgeyU * Math.PI * 2) * Math.cos(pgeyV * Math.PI));
            case SMOOTH, STUDS, REFLECTIVE -> null;
        };
        PGEY_CACHE.put(pgeyPattern, pgeyImg);
        return pgeyImg;
    }

    private static double pgeyNoise(double pgeyU, double pgeyV) {
        double pgeyS = Math.sin(pgeyU * 12.9898 + pgeyV * 78.233) * 43758.5453;
        double pgeyBase = pgeyS - Math.floor(pgeyS);
        return (pgeyBase + Math.sin(pgeyU * 2.1) * Math.cos(pgeyV * 1.7)) * 0.4;
    }

    interface PgeyHeight {
        double pgeyAt(double pgeyU, double pgeyV);
    }

    static WritableImage pgeyNormalMap(int pgeySize, double pgeyCellPx, double pgeyStrength, PgeyHeight pgeyH) {
        WritableImage pgeyImg = new WritableImage(pgeySize, pgeySize);
        PixelWriter pgeyW = pgeyImg.getPixelWriter();
        double pgeyCells = pgeySize / pgeyCellPx;
        double pgeyE = 1.0 / pgeySize;
        for (int pgeyY = 0; pgeyY < pgeySize; pgeyY++) {
            for (int pgeyX = 0; pgeyX < pgeySize; pgeyX++) {
                double pgeyU = (double) pgeyX / pgeySize * pgeyCells;
                double pgeyV = (double) pgeyY / pgeySize * pgeyCells;
                double pgeyDhdu = (pgeyH.pgeyAt(pgeyU + pgeyE * pgeyCells, pgeyV) - pgeyH.pgeyAt(pgeyU - pgeyE * pgeyCells, pgeyV)) / 2;
                double pgeyDhdv = (pgeyH.pgeyAt(pgeyU, pgeyV + pgeyE * pgeyCells) - pgeyH.pgeyAt(pgeyU, pgeyV - pgeyE * pgeyCells)) / 2;
                double pgeyNx = -pgeyDhdu * pgeyStrength * 6;
                double pgeyNy = -pgeyDhdv * pgeyStrength * 6;
                double pgeyLen = Math.sqrt(pgeyNx * pgeyNx + pgeyNy * pgeyNy + 1);
                int pgeyR = (int) Math.round((pgeyNx / pgeyLen * 0.5 + 0.5) * 255);
                int pgeyG = (int) Math.round((pgeyNy / pgeyLen * 0.5 + 0.5) * 255);
                int pgeyB = (int) Math.round((1 / pgeyLen * 0.5 + 0.5) * 255);
                pgeyW.setArgb(pgeyX, pgeyY, 0xFF000000 | (pgeyR << 16) | (pgeyG << 8) | pgeyB);
            }
        }
        return pgeyImg;
    }
}
