package com.sportforce3d.design;

import com.sportforce3d.three.PgeyMaterials;
import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyZone;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class PgeyDesignDefaults {

    private PgeyDesignDefaults() {
    }

    public static PgeyDesignDocument pgeyDocument(PgeyBodyType pgeyType) {
        Map<PgeyZone, String> pgeyColors = new EnumMap<>(PgeyZone.class);
        PgeyMaterials.PGEY_DEFAULT_COLORS.forEach((pgeyZone, pgeyColor) -> pgeyColors.put(pgeyZone, pgeyHex(pgeyColor)));
        Map<PgeyMaterialSlot, String> pgeyMaterials = new EnumMap<>(PgeyMaterialSlot.class);
        for (PgeyMaterialSlot pgeySlot : PgeyMaterialSlot.values()) {
            pgeyMaterials.put(pgeySlot, pgeySlot.pgeyDefaultCode());
        }
        return new PgeyDesignDocument(PgeyDesignDocument.PGEY_SCHEMA_VERSION, pgeyType, null, pgeyColors, Map.of(), pgeyMaterials, java.util.List.of(),
                PgeyRealisticSpec.pgeyDefaults(), PgeyGarmentCut.PGEY_DEFAULT);
    }

    public static PgeyDesignElement pgeyName(String pgeyText, String pgeyColor, String pgeyOutlineColor) {
        return new PgeyDesignElement(pgeyNewId(), PgeyElementType.NAME, PgeyArea.JERSEY_BACK, 0.5, 0.20, 1.0, 0, true, false,
                0.060, pgeyText, PgeyDesignFonts.PGEY_DEFAULT, pgeyColor, pgeyOutlineColor, 0, null, null, null);
    }

    public static PgeyDesignElement pgeyBackNumber(String pgeyNumber, String pgeyColor, String pgeyOutlineColor) {
        return new PgeyDesignElement(pgeyNewId(), PgeyElementType.NUMBER, PgeyArea.JERSEY_BACK, 0.5, 0.47, 1.0, 0, true, false,
                0.240, pgeyNumber, PgeyDesignFonts.PGEY_DEFAULT, pgeyColor, pgeyOutlineColor, 0, null, null, null);
    }

    public static PgeyDesignElement pgeyChestNumber(String pgeyNumber, String pgeyColor, String pgeyOutlineColor) {
        return new PgeyDesignElement(pgeyNewId(), PgeyElementType.NUMBER, PgeyArea.JERSEY_FRONT, 0.30, 0.28, 1.0, 0, true, false,
                0.090, pgeyNumber, PgeyDesignFonts.PGEY_DEFAULT, pgeyColor, pgeyOutlineColor, 0, null, null, null);
    }

    public static PgeyDesignElement pgeyShortsNumber(String pgeyNumber, String pgeyColor, String pgeyOutlineColor) {
        return pgeyShortsNumber(pgeyNumber, pgeyColor, pgeyOutlineColor, PgeyGarmentCut.PgeyShortsLength.MEDIO);
    }

    public static PgeyDesignElement pgeyShortsNumber(String pgeyNumber, String pgeyColor, String pgeyOutlineColor,
                                             PgeyGarmentCut.PgeyShortsLength pgeyLength) {
        return new PgeyDesignElement(pgeyNewId(), PgeyElementType.NUMBER, PgeyArea.SHORTS_LEFT, pgeyLength.pgeyNumberU(), pgeyLength.pgeyNumberV(),
                1.0, 0, true, false, 0.070, pgeyNumber, PgeyDesignFonts.PGEY_DEFAULT, pgeyColor, pgeyOutlineColor, 0, null, null, null);
    }

    public static PgeyDesignElement pgeyEmblem(String pgeyKey, String pgeyPrimary, String pgeyAccent) {
        return new PgeyDesignElement(pgeyNewId(), PgeyElementType.EMBLEM, PgeyArea.JERSEY_FRONT, 0.70, 0.27, 1.0, 0, true, false,
                0.085, null, null, pgeyPrimary, pgeyAccent, 0, pgeyKey, null, null);
    }

    public static PgeyDesignElement pgeyLogo(long pgeyAssetId, String pgeyPath) {
        return new PgeyDesignElement(pgeyNewId(), PgeyElementType.LOGO, PgeyArea.JERSEY_FRONT, 0.70, 0.27, 1.0, 0, true, false,
                0.085, null, null, null, null, 0, null, pgeyAssetId, pgeyPath);
    }

    public static String pgeyNewId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String pgeyHex(Color pgeyC) {
        return String.format("#%02X%02X%02X", (int) Math.round(pgeyC.getRed() * 255),
                (int) Math.round(pgeyC.getGreen() * 255), (int) Math.round(pgeyC.getBlue() * 255));
    }
}
