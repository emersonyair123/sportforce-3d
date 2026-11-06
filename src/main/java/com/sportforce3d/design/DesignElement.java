package com.sportforce3d.design;

/**
 * Elemento colocado sobre una prenda: nombre, número, emblema de la biblioteca o logo subido.
 *
 * @param u, v         posición del centro dentro de su zona (0..1)
 * @param scale        tamaño relativo (0.2 = 20 % … 2.0 = 200 %)
 * @param rotation     giro en grados (-180..180)
 * @param height       alto base en metros sobre la prenda (antes de {@code scale})
 * @param text         texto del nombre (máx. 12) o número (0–99)
 * @param font         familia tipográfica (ver {@link DesignFonts})
 * @param color        color de relleno del texto, o color principal del emblema
 * @param accentColor  contorno del texto, o color de acento del emblema
 * @param outline      grosor del contorno en metros (0 = sin contorno)
 * @param emblem       clave del emblema de la biblioteca (p. ej. "escudo-clasico")
 * @param assetId      id de design_assets del logo subido
 * @param assetPath    ruta del logo en el almacenamiento
 */
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

    /** Nombre corto para la lista de capas. */
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
