package com.sportforce3d.design;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.model.Zone;

/**
 * Las cuatro opciones de texto del diseño (pestaña Texto del 3D): nombre y número en la espalda, número en el pecho
 * y número en el short. La Vista realista lee y escribe estas mismas opciones ({@link ViewSync}).
 */
public enum TextSlot {
    BACK_NAME("Nombre en la espalda"),
    BACK_NUMBER("Número en la espalda"),
    CHEST_NUMBER("Número en el pecho"),
    SHORTS_NUMBER("Número en el short");

    private final String label;

    TextSlot(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean matches(DesignElement e) {
        return switch (this) {
            case BACK_NAME -> e.type() == ElementType.NAME && e.area() == Area.JERSEY_BACK;
            case BACK_NUMBER -> e.type() == ElementType.NUMBER && e.area() == Area.JERSEY_BACK;
            case CHEST_NUMBER -> e.type() == ElementType.NUMBER && e.area() == Area.JERSEY_FRONT;
            case SHORTS_NUMBER -> e.type() == ElementType.NUMBER && e.area().garment() == Garment.SHORTS;
        };
    }

    /** El primer elemento del documento que corresponde a esta opción (o null). */
    public DesignElement find(DesignDocument doc) {
        return doc.elements().stream().filter(this::matches).findFirst().orElse(null);
    }

    public static TextSlot of(DesignElement e) {
        for (TextSlot s : values()) {
            if (s.matches(e)) {
                return s;
            }
        }
        return null;
    }

    /** Elemento nuevo de esta opción con los colores que contrastan con la prenda y contorno desde el inicio. */
    public DesignElement create(DesignDocument doc) {
        String fill = contrast(doc.color(Zone.JERSEY_BODY));
        String edge = fill.equals("#FFFFFF") ? "#0B0E13" : "#FFFFFF";
        String shortsFill = contrast(doc.color(Zone.SHORTS_BASE));
        // Con contorno desde el inicio: sobre franjas o colores medios el texto sin borde se pierde.
        return switch (this) {
            case BACK_NAME -> DesignDefaults.name("TU NOMBRE", fill, edge).withOutline(0.004);
            case BACK_NUMBER -> DesignDefaults.backNumber("10", fill, edge).withOutline(0.006);
            case CHEST_NUMBER -> DesignDefaults.chestNumber("10", fill, edge).withOutline(0.003);
            case SHORTS_NUMBER -> DesignDefaults.shortsNumber("10", shortsFill,
                    shortsFill.equals("#FFFFFF") ? "#0B0E13" : "#FFFFFF", doc.cut().shorts()).withOutline(0.003);
        };
    }

    /** true si el elemento sigue en la posición, tamaño y giro con que se crea (la foto realista lo dibuja ahí). */
    public boolean atDefault(DesignElement e, DesignDocument doc) {
        DesignElement d = create(doc);
        return Math.abs(e.u() - d.u()) < 0.02 && Math.abs(e.v() - d.v()) < 0.02
                && Math.abs(e.scale() - 1) < 0.05 && Math.abs(e.rotation()) < 2;
    }

    /** Blanco o negro, lo que más contraste con el color de fondo. */
    public static String contrast(String hex) {
        int rgb;
        try {
            rgb = Integer.parseInt(hex.substring(1), 16);
        } catch (RuntimeException e) {
            return "#FFFFFF";
        }
        double lum = (0.2126 * ((rgb >> 16) & 0xFF) + 0.7152 * ((rgb >> 8) & 0xFF) + 0.0722 * (rgb & 0xFF)) / 255;
        return lum > 0.55 ? "#0B0E13" : "#FFFFFF";
    }
}
