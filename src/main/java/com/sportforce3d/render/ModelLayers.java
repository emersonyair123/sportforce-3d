package com.sportforce3d.render;

import java.util.Map;

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

    public static Mask[] detail(ArgbImage detail) {
        int[] box = Mask.bounds(detail);
        return new Mask[]{Mask.crop(detail, false, box), Mask.crop(detail, true, box)};
    }
}
