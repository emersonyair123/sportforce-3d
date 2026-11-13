package com.sportforce3d.design;

import com.sportforce3d.three.Materials;
import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.Zone;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class DesignDefaults {

    private DesignDefaults() {
    }

    public static DesignDocument document(BodyType type) {
        Map<Zone, String> colors = new EnumMap<>(Zone.class);
        Materials.DEFAULT_COLORS.forEach((zone, color) -> colors.put(zone, hex(color)));
        Map<MaterialSlot, String> materials = new EnumMap<>(MaterialSlot.class);
        for (MaterialSlot slot : MaterialSlot.values()) {
            materials.put(slot, slot.defaultCode());
        }
        return new DesignDocument(DesignDocument.SCHEMA_VERSION, type, null, colors, Map.of(), materials, java.util.List.of(),
                RealisticSpec.defaults(), GarmentCut.DEFAULT);
    }

    public static DesignElement name(String text, String color, String outlineColor) {
        return new DesignElement(newId(), ElementType.NAME, Area.JERSEY_BACK, 0.5, 0.20, 1.0, 0, true, false,
                0.060, text, DesignFonts.DEFAULT, color, outlineColor, 0, null, null, null);
    }

    public static DesignElement backNumber(String number, String color, String outlineColor) {
        return new DesignElement(newId(), ElementType.NUMBER, Area.JERSEY_BACK, 0.5, 0.47, 1.0, 0, true, false,
                0.240, number, DesignFonts.DEFAULT, color, outlineColor, 0, null, null, null);
    }

    public static DesignElement chestNumber(String number, String color, String outlineColor) {
        return new DesignElement(newId(), ElementType.NUMBER, Area.JERSEY_FRONT, 0.30, 0.28, 1.0, 0, true, false,
                0.090, number, DesignFonts.DEFAULT, color, outlineColor, 0, null, null, null);
    }

    public static DesignElement shortsNumber(String number, String color, String outlineColor) {
        return shortsNumber(number, color, outlineColor, GarmentCut.ShortsLength.MEDIO);
    }

    public static DesignElement shortsNumber(String number, String color, String outlineColor,
                                             GarmentCut.ShortsLength length) {
        return new DesignElement(newId(), ElementType.NUMBER, Area.SHORTS_LEFT, length.numberU(), length.numberV(),
                1.0, 0, true, false, 0.070, number, DesignFonts.DEFAULT, color, outlineColor, 0, null, null, null);
    }

    public static DesignElement emblem(String key, String primary, String accent) {
        return new DesignElement(newId(), ElementType.EMBLEM, Area.JERSEY_FRONT, 0.70, 0.27, 1.0, 0, true, false,
                0.085, null, null, primary, accent, 0, key, null, null);
    }

    public static DesignElement logo(long assetId, String path) {
        return new DesignElement(newId(), ElementType.LOGO, Area.JERSEY_FRONT, 0.70, 0.27, 1.0, 0, true, false,
                0.085, null, null, null, null, 0, null, assetId, path);
    }

    public static String newId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String hex(Color c) {
        return String.format("#%02X%02X%02X", (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
    }
}
