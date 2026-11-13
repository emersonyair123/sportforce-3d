package com.sportforce3d.design;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PgeyDesignElement(@JsonProperty("id") String pgeyId, @JsonProperty("type") PgeyElementType pgeyType, @JsonProperty("area") PgeyArea pgeyArea, @JsonProperty("u") double pgeyU, @JsonProperty("v") double pgeyV, @JsonProperty("scale") double pgeyScale,
                            @JsonProperty("rotation") double pgeyRotation, @JsonProperty("visible") boolean pgeyVisible, @JsonProperty("locked") boolean pgeyLocked, @JsonProperty("height") double pgeyHeight,
                            @JsonProperty("text") String pgeyText, @JsonProperty("font") String pgeyFont, @JsonProperty("color") String pgeyColor, @JsonProperty("accentColor") String pgeyAccentColor, @JsonProperty("outline") double pgeyOutline,
                            @JsonProperty("emblem") String pgeyEmblem, @JsonProperty("assetId") Long pgeyAssetId, @JsonProperty("assetPath") String pgeyAssetPath) {

    public static final double PGEY_MIN_SCALE = 0.2;
    public static final double PGEY_MAX_SCALE = 2.0;

    public PgeyDesignElement {
        pgeyU = pgeyClamp01(pgeyU);
        pgeyV = pgeyClamp01(pgeyV);
        pgeyScale = Math.max(PGEY_MIN_SCALE, Math.min(PGEY_MAX_SCALE, pgeyScale));
        pgeyRotation = pgeyNormalizeAngle(pgeyRotation);
    }

    public PgeyDesignElement pgeyAt(double pgeyNu, double pgeyNv) {
        return new PgeyDesignElement(pgeyId, pgeyType, pgeyArea, pgeyNu, pgeyNv, pgeyScale, pgeyRotation, pgeyVisible, pgeyLocked, pgeyHeight, pgeyText, pgeyFont,
                pgeyColor, pgeyAccentColor, pgeyOutline, pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public PgeyDesignElement pgeyWithScale(double pgeyS) {
        return new PgeyDesignElement(pgeyId, pgeyType, pgeyArea, pgeyU, pgeyV, pgeyS, pgeyRotation, pgeyVisible, pgeyLocked, pgeyHeight, pgeyText, pgeyFont,
                pgeyColor, pgeyAccentColor, pgeyOutline, pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public PgeyDesignElement pgeyWithRotation(double pgeyR) {
        return new PgeyDesignElement(pgeyId, pgeyType, pgeyArea, pgeyU, pgeyV, pgeyScale, pgeyR, pgeyVisible, pgeyLocked, pgeyHeight, pgeyText, pgeyFont,
                pgeyColor, pgeyAccentColor, pgeyOutline, pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public PgeyDesignElement pgeyWithVisible(boolean pgeyB) {
        return new PgeyDesignElement(pgeyId, pgeyType, pgeyArea, pgeyU, pgeyV, pgeyScale, pgeyRotation, pgeyB, pgeyLocked, pgeyHeight, pgeyText, pgeyFont,
                pgeyColor, pgeyAccentColor, pgeyOutline, pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public PgeyDesignElement pgeyWithLocked(boolean pgeyB) {
        return new PgeyDesignElement(pgeyId, pgeyType, pgeyArea, pgeyU, pgeyV, pgeyScale, pgeyRotation, pgeyVisible, pgeyB, pgeyHeight, pgeyText, pgeyFont,
                pgeyColor, pgeyAccentColor, pgeyOutline, pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public PgeyDesignElement pgeyWithText(String pgeyT) {
        return new PgeyDesignElement(pgeyId, pgeyType, pgeyArea, pgeyU, pgeyV, pgeyScale, pgeyRotation, pgeyVisible, pgeyLocked, pgeyHeight, pgeyT, pgeyFont,
                pgeyColor, pgeyAccentColor, pgeyOutline, pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public PgeyDesignElement pgeyWithFont(String pgeyF) {
        return new PgeyDesignElement(pgeyId, pgeyType, pgeyArea, pgeyU, pgeyV, pgeyScale, pgeyRotation, pgeyVisible, pgeyLocked, pgeyHeight, pgeyText, pgeyF,
                pgeyColor, pgeyAccentColor, pgeyOutline, pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public PgeyDesignElement pgeyWithColor(String pgeyC) {
        return new PgeyDesignElement(pgeyId, pgeyType, pgeyArea, pgeyU, pgeyV, pgeyScale, pgeyRotation, pgeyVisible, pgeyLocked, pgeyHeight, pgeyText, pgeyFont,
                pgeyC, pgeyAccentColor, pgeyOutline, pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public PgeyDesignElement pgeyWithAccent(String pgeyC) {
        return new PgeyDesignElement(pgeyId, pgeyType, pgeyArea, pgeyU, pgeyV, pgeyScale, pgeyRotation, pgeyVisible, pgeyLocked, pgeyHeight, pgeyText, pgeyFont,
                pgeyColor, pgeyC, pgeyOutline, pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public PgeyDesignElement pgeyWithOutline(double pgeyO) {
        return new PgeyDesignElement(pgeyId, pgeyType, pgeyArea, pgeyU, pgeyV, pgeyScale, pgeyRotation, pgeyVisible, pgeyLocked, pgeyHeight, pgeyText, pgeyFont,
                pgeyColor, pgeyAccentColor, Math.max(0, Math.min(0.012, pgeyO)), pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public PgeyDesignElement pgeyWithArea(PgeyArea pgeyA) {
        return new PgeyDesignElement(pgeyId, pgeyType, pgeyA, pgeyU, pgeyV, pgeyScale, pgeyRotation, pgeyVisible, pgeyLocked, pgeyHeight, pgeyText, pgeyFont,
                pgeyColor, pgeyAccentColor, pgeyOutline, pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public PgeyDesignElement pgeyWithId(String pgeyNewId) {
        return new PgeyDesignElement(pgeyNewId, pgeyType, pgeyArea, pgeyU, pgeyV, pgeyScale, pgeyRotation, pgeyVisible, pgeyLocked, pgeyHeight, pgeyText, pgeyFont,
                pgeyColor, pgeyAccentColor, pgeyOutline, pgeyEmblem, pgeyAssetId, pgeyAssetPath);
    }

    public String pgeyDisplayName() {
        return switch (pgeyType) {
            case NAME -> "Nombre · " + (pgeyText == null || pgeyText.isBlank() ? "(vacío)" : pgeyText);
            case NUMBER -> "Número · " + (pgeyText == null ? "" : pgeyText);
            case EMBLEM -> "Emblema · " + PgeyEmblemCatalog.pgeyLabel(pgeyEmblem);
            case LOGO -> "Logo subido";
        };
    }

    static double pgeyClamp01(double pgeyX) {
        return Double.isNaN(pgeyX) ? 0.5 : Math.max(0, Math.min(1, pgeyX));
    }

    static double pgeyNormalizeAngle(double pgeyDeg) {
        double pgeyA = pgeyDeg % 360;
        if (pgeyA > 180) {
            pgeyA -= 360;
        } else if (pgeyA <= -180) {
            pgeyA += 360;
        }
        return pgeyA;
    }
}
