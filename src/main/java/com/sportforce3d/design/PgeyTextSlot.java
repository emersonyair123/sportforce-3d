package com.sportforce3d.design;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.model.PgeyZone;

public enum PgeyTextSlot {
    BACK_NAME("Nombre en la espalda"),
    BACK_NUMBER("Número en la espalda"),
    CHEST_NUMBER("Número en el pecho"),
    SHORTS_NUMBER("Número en el short");

    private final String pgeyLabel;

    PgeyTextSlot(String pgeyLabel) {
        this.pgeyLabel = pgeyLabel;
    }

    public String pgeyLabel() {
        return pgeyLabel;
    }

    public boolean pgeyMatches(PgeyDesignElement pgeyE) {
        return switch (this) {
            case BACK_NAME -> pgeyE.pgeyType() == PgeyElementType.NAME && pgeyE.pgeyArea() == PgeyArea.JERSEY_BACK;
            case BACK_NUMBER -> pgeyE.pgeyType() == PgeyElementType.NUMBER && pgeyE.pgeyArea() == PgeyArea.JERSEY_BACK;
            case CHEST_NUMBER -> pgeyE.pgeyType() == PgeyElementType.NUMBER && pgeyE.pgeyArea() == PgeyArea.JERSEY_FRONT;
            case SHORTS_NUMBER -> pgeyE.pgeyType() == PgeyElementType.NUMBER && pgeyE.pgeyArea().pgeyGarment() == HepyGarment.SHORTS;
        };
    }

    public PgeyDesignElement pgeyFind(PgeyDesignDocument pgeyDoc) {
        return pgeyDoc.pgeyElements().stream().filter(this::pgeyMatches).findFirst().orElse(null);
    }

    public static PgeyTextSlot pgeyOf(PgeyDesignElement pgeyE) {
        for (PgeyTextSlot pgeyS : values()) {
            if (pgeyS.pgeyMatches(pgeyE)) {
                return pgeyS;
            }
        }
        return null;
    }

    public PgeyDesignElement pgeyCreate(PgeyDesignDocument pgeyDoc) {
        String pgeyFill = pgeyContrast(pgeyDoc.pgeyColor(PgeyZone.JERSEY_BODY));
        String pgeyEdge = pgeyFill.equals("#FFFFFF") ? "#0B0E13" : "#FFFFFF";
        String pgeyShortsFill = pgeyContrast(pgeyDoc.pgeyColor(PgeyZone.SHORTS_BASE));
        return switch (this) {
            case BACK_NAME -> PgeyDesignDefaults.pgeyName("TU NOMBRE", pgeyFill, pgeyEdge).pgeyWithOutline(0.004);
            case BACK_NUMBER -> PgeyDesignDefaults.pgeyBackNumber("10", pgeyFill, pgeyEdge).pgeyWithOutline(0.006);
            case CHEST_NUMBER -> PgeyDesignDefaults.pgeyChestNumber("10", pgeyFill, pgeyEdge).pgeyWithOutline(0.003);
            case SHORTS_NUMBER -> PgeyDesignDefaults.pgeyShortsNumber("10", pgeyShortsFill,
                    pgeyShortsFill.equals("#FFFFFF") ? "#0B0E13" : "#FFFFFF", pgeyDoc.pgeyCut().pgeyShorts()).pgeyWithOutline(0.003);
        };
    }

    public boolean pgeyAtDefault(PgeyDesignElement pgeyE, PgeyDesignDocument pgeyDoc) {
        PgeyDesignElement pgeyD = pgeyCreate(pgeyDoc);
        return Math.abs(pgeyE.pgeyU() - pgeyD.pgeyU()) < 0.02 && Math.abs(pgeyE.pgeyV() - pgeyD.pgeyV()) < 0.02
                && Math.abs(pgeyE.pgeyScale() - 1) < 0.05 && Math.abs(pgeyE.pgeyRotation()) < 2;
    }

    public static String pgeyContrast(String pgeyHex) {
        int pgeyRgb;
        try {
            pgeyRgb = Integer.parseInt(pgeyHex.substring(1), 16);
        } catch (RuntimeException pgeyE) {
            return "#FFFFFF";
        }
        double pgeyLum = (0.2126 * ((pgeyRgb >> 16) & 0xFF) + 0.7152 * ((pgeyRgb >> 8) & 0xFF) + 0.0722 * (pgeyRgb & 0xFF)) / 255;
        return pgeyLum > 0.55 ? "#0B0E13" : "#FFFFFF";
    }
}
