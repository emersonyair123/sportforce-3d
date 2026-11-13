package com.sportforce3d.render;

import java.util.Map;

public record PgeyModelLayers(PgeyArgbImage pgeyBase, Map<String, PgeyMask> pgeyZones, PgeyMask pgeyDetailGray, PgeyMask pgeyDetailAlpha) {

    public PgeyModelLayers {
        pgeyZones = Map.copyOf(pgeyZones);
        if (pgeyDetailGray.pgeyX() != pgeyDetailAlpha.pgeyX() || pgeyDetailGray.pgeyY() != pgeyDetailAlpha.pgeyY()
                || pgeyDetailGray.pgeyWidth() != pgeyDetailAlpha.pgeyWidth() || pgeyDetailGray.pgeyHeight() != pgeyDetailAlpha.pgeyHeight()) {
            throw new IllegalArgumentException("El gris y el alfa del detalle deben tener la misma caja");
        }
    }

    public int pgeyWidth() {
        return pgeyBase.pgeyWidth();
    }

    public int pgeyHeight() {
        return pgeyBase.pgeyHeight();
    }

    public PgeyMask pgeyZone(String pgeyName) {
        return pgeyZones.getOrDefault(pgeyName, PgeyMask.PGEY_EMPTY);
    }

    public static PgeyMask[] pgeyDetail(PgeyArgbImage pgeyDetail) {
        int[] pgeyBox = PgeyMask.pgeyBounds(pgeyDetail);
        return new PgeyMask[]{PgeyMask.pgeyCrop(pgeyDetail, false, pgeyBox), PgeyMask.pgeyCrop(pgeyDetail, true, pgeyBox)};
    }
}
