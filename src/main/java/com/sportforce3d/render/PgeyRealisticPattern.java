package com.sportforce3d.render;

import java.util.Locale;

public enum PgeyRealisticPattern {
    LISO("liso", "Liso"),
    RAYAS("rayas", "Rayas"),
    FRANJAS("franjas", "Franjas horizontales"),
    DINAMICO("dinamico", "Dinámico"),
    GEOMETRICO("geometrico", "Geométrico"),
    MINIMAL("minimal", "Minimal (hombros)"),
    BANDA("banda", "Banda diagonal");

    private final String pgeyCode;
    private final String pgeyLabel;

    PgeyRealisticPattern(String pgeyCode, String pgeyLabel) {
        this.pgeyCode = pgeyCode;
        this.pgeyLabel = pgeyLabel;
    }

    public String pgeyCode() {
        return pgeyCode;
    }

    public String pgeyLabel() {
        return pgeyLabel;
    }

    public static PgeyRealisticPattern pgeyFromCode(String pgeyCode) {
        if (pgeyCode == null || pgeyCode.isBlank()) {
            return LISO;
        }
        String pgeyC = pgeyCode.trim().toLowerCase(Locale.ROOT);
        for (PgeyRealisticPattern pgeyP : values()) {
            if (pgeyP.pgeyCode.equals(pgeyC)) {
                return pgeyP;
            }
        }
        throw new IllegalArgumentException("Diseño desconocido: " + pgeyCode);
    }
}
