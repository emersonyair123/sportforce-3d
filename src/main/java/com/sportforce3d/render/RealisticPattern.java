package com.sportforce3d.render;

import java.util.Locale;

public enum RealisticPattern {
    LISO("liso", "Liso"),
    RAYAS("rayas", "Rayas"),
    FRANJAS("franjas", "Franjas horizontales"),
    DINAMICO("dinamico", "Dinámico"),
    GEOMETRICO("geometrico", "Geométrico"),
    MINIMAL("minimal", "Minimal (hombros)"),
    BANDA("banda", "Banda diagonal");

    private final String code;
    private final String label;

    RealisticPattern(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static RealisticPattern fromCode(String code) {
        if (code == null || code.isBlank()) {
            return LISO;
        }
        String c = code.trim().toLowerCase(Locale.ROOT);
        for (RealisticPattern p : values()) {
            if (p.code.equals(c)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Diseño desconocido: " + code);
    }
}
