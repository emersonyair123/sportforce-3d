package com.sportforce3d.design;

public record DesignElement(String id, ElementType type, Area area, double u, double v, double scale,
                            double rotation, boolean visible, boolean locked, double height,
                            String text, String font, String color, String accentColor, double outline,
                            String emblem, Long assetId, String assetPath) {

    public static final double MIN_SCALE = 0.2;
    public static final double MAX_SCALE = 2.0;

    public DesignElement {
        u = clamp01(u);
        v = clamp01(v);
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        rotation = normalizeAngle(rotation);
    }

    public DesignElement at(double nu, double nv) {
        return new DesignElement(id, type, area, nu, nv, scale, rotation, visible, locked, height, text, font,
                color, accentColor, outline, emblem, assetId, assetPath);
    }

    public DesignElement withScale(double s) {
        return new DesignElement(id, type, area, u, v, s, rotation, visible, locked, height, text, font,
                color, accentColor, outline, emblem, assetId, assetPath);
    }

    public DesignElement withRotation(double r) {
        return new DesignElement(id, type, area, u, v, scale, r, visible, locked, height, text, font,
                color, accentColor, outline, emblem, assetId, assetPath);
    }

    public DesignElement withVisible(boolean b) {
        return new DesignElement(id, type, area, u, v, scale, rotation, b, locked, height, text, font,
                color, accentColor, outline, emblem, assetId, assetPath);
    }

    public DesignElement withLocked(boolean b) {
        return new DesignElement(id, type, area, u, v, scale, rotation, visible, b, height, text, font,
                color, accentColor, outline, emblem, assetId, assetPath);
    }

    public DesignElement withText(String t) {
        return new DesignElement(id, type, area, u, v, scale, rotation, visible, locked, height, t, font,
                color, accentColor, outline, emblem, assetId, assetPath);
    }

    public DesignElement withFont(String f) {
        return new DesignElement(id, type, area, u, v, scale, rotation, visible, locked, height, text, f,
                color, accentColor, outline, emblem, assetId, assetPath);
    }

    public DesignElement withColor(String c) {
        return new DesignElement(id, type, area, u, v, scale, rotation, visible, locked, height, text, font,
                c, accentColor, outline, emblem, assetId, assetPath);
    }

    public DesignElement withAccent(String c) {
        return new DesignElement(id, type, area, u, v, scale, rotation, visible, locked, height, text, font,
                color, c, outline, emblem, assetId, assetPath);
    }

    public DesignElement withOutline(double o) {
        return new DesignElement(id, type, area, u, v, scale, rotation, visible, locked, height, text, font,
                color, accentColor, Math.max(0, Math.min(0.012, o)), emblem, assetId, assetPath);
    }

    public DesignElement withArea(Area a) {
        return new DesignElement(id, type, a, u, v, scale, rotation, visible, locked, height, text, font,
                color, accentColor, outline, emblem, assetId, assetPath);
    }

    public DesignElement withId(String newId) {
        return new DesignElement(newId, type, area, u, v, scale, rotation, visible, locked, height, text, font,
                color, accentColor, outline, emblem, assetId, assetPath);
    }

    public String displayName() {
        return switch (type) {
            case NAME -> "Nombre · " + (text == null || text.isBlank() ? "(vacío)" : text);
            case NUMBER -> "Número · " + (text == null ? "" : text);
            case EMBLEM -> "Emblema · " + EmblemCatalog.label(emblem);
            case LOGO -> "Logo subido";
        };
    }

    static double clamp01(double x) {
        return Double.isNaN(x) ? 0.5 : Math.max(0, Math.min(1, x));
    }

    static double normalizeAngle(double deg) {
        double a = deg % 360;
        if (a > 180) {
            a -= 360;
        } else if (a <= -180) {
            a += 360;
        }
        return a;
    }
}
