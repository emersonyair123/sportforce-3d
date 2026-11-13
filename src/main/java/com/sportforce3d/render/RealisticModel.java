package com.sportforce3d.render;

import java.util.List;

public record RealisticModel(String key, View view, List<String> zones, ShirtGeometry shirt, int width, int height) {

    public static final String CAMISETA = "camiseta";
    public static final String SHORT = "short";
    public static final String PANELES = "paneles";
    public static final String MEDIAS = "medias";
    public static final String PRINCIPAL = "principal";

    public enum View {
        FRENTE, ESPALDA, PRODUCTO
    }

    public record ShirtGeometry(int centerX, int neckY, int hemY) {

        public ShirtGeometry {
            if (hemY <= neckY) {
                throw new IllegalArgumentException("La basta debe estar debajo del cuello");
            }
        }

        public int height() {
            return hemY - neckY;
        }
    }

    public RealisticModel {
        zones = List.copyOf(zones);
    }

    public boolean hasZone(String zone) {
        return zones.contains(zone);
    }

    public String cut() {
        int i = key.indexOf('_');
        return i < 0 ? key : key.substring(0, i);
    }

    public String audience() {
        if (view == View.PRODUCTO) {
            return null;
        }
        String[] parts = key.split("_");
        return parts.length >= 3 ? parts[1] : null;
    }

    public String otherViewKey() {
        return switch (view) {
            case FRENTE -> key.replaceFirst("_frente$", "_espalda");
            case ESPALDA -> key.replaceFirst("_espalda$", "_frente");
            case PRODUCTO -> null;
        };
    }
}
