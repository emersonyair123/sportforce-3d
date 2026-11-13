package com.sportforce3d.render;

import java.util.List;

public record PgeyRealisticModel(String pgeyKey, PgeyView pgeyView, List<String> pgeyZones, PgeyShirtGeometry pgeyShirt, int pgeyWidth, int pgeyHeight) {

    public static final String PGEY_CAMISETA = "camiseta";
    public static final String PGEY_SHORT = "short";
    public static final String PGEY_PANELES = "paneles";
    public static final String PGEY_MEDIAS = "medias";
    public static final String PGEY_PRINCIPAL = "principal";

    public enum PgeyView {
        FRENTE, ESPALDA, PRODUCTO
    }

    public record PgeyShirtGeometry(int pgeyCenterX, int pgeyNeckY, int pgeyHemY) {

        public PgeyShirtGeometry {
            if (pgeyHemY <= pgeyNeckY) {
                throw new IllegalArgumentException("La basta debe estar debajo del cuello");
            }
        }

        public int pgeyHeight() {
            return pgeyHemY - pgeyNeckY;
        }
    }

    public PgeyRealisticModel {
        pgeyZones = List.copyOf(pgeyZones);
    }

    public boolean pgeyHasZone(String pgeyZone) {
        return pgeyZones.contains(pgeyZone);
    }

    public String pgeyCut() {
        int pgeyI = pgeyKey.indexOf('_');
        return pgeyI < 0 ? pgeyKey : pgeyKey.substring(0, pgeyI);
    }

    public String pgeyAudience() {
        if (pgeyView == PgeyView.PRODUCTO) {
            return null;
        }
        String[] pgeyParts = pgeyKey.split("_");
        return pgeyParts.length >= 3 ? pgeyParts[1] : null;
    }

    public String pgeyOtherViewKey() {
        return switch (pgeyView) {
            case FRENTE -> pgeyKey.replaceFirst("_frente$", "_espalda");
            case ESPALDA -> pgeyKey.replaceFirst("_espalda$", "_frente");
            case PRODUCTO -> null;
        };
    }
}
