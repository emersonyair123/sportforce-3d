package com.sportforce3d.render;

import java.util.Map;

/**
 * Píxeles de un modelo listos para recolorear (se cargan una vez y no cambian).
 *
 * @param base        foto con la prenda en blanco normalizado
 * @param zones       alfa de cada {@code zona_<nombre>.png}
 * @param detailGray  gris de {@code detalle.png} (128 = neutro), con la misma caja que {@code detailAlpha}
 * @param detailAlpha alfa de {@code detalle.png}
 */
public record ModelLayers(ArgbImage base, Map<String, Mask> zones, Mask detailGray, Mask detailAlpha) {

    public ModelLayers {
        zones = Map.copyOf(zones);
        if (detailGray.x() != detailAlpha.x() || detailGray.y() != detailAlpha.y()
                || detailGray.width() != detailAlpha.width() || detailGray.height() != detailAlpha.height()) {
            throw new IllegalArgumentException("El gris y el alfa del detalle deben tener la misma caja");
        }
    }

    public int width() {
        return base.width();
    }

    public int height() {
        return base.height();
    }

    public Mask zone(String name) {
        return zones.getOrDefault(name, Mask.EMPTY);
    }

    /** Separa el gris y el alfa de una imagen de detalle. */
    public static Mask[] detail(ArgbImage detail) {
        int[] box = Mask.bounds(detail);
        return new Mask[]{Mask.crop(detail, false, box), Mask.crop(detail, true, box)};
    }
}
