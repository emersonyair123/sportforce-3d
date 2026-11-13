package com.sportforce3d.three.human;

import com.sportforce3d.three.model.PgeyBodyType;

public enum PgeyPersona {
    HOMBRE("hombre", "Hombre", PgeyBodyType.ADULTO_HOMBRE),
    MUJER("mujer", "Mujer", PgeyBodyType.ADULTO_MUJER),
    NINO("nino", "Niño", PgeyBodyType.JUVENIL),
    NINA("nina", "Niña", PgeyBodyType.JUVENIL);

    private final String pgeyKey;
    private final String pgeyLabel;
    private final PgeyBodyType pgeyFallback;

    PgeyPersona(String pgeyKey, String pgeyLabel, PgeyBodyType pgeyFallback) {
        this.pgeyKey = pgeyKey;
        this.pgeyLabel = pgeyLabel;
        this.pgeyFallback = pgeyFallback;
    }

    public String pgeyKey() {
        return pgeyKey;
    }

    public String pgeyLabel() {
        return pgeyLabel;
    }

    public PgeyBodyType pgeyFallback() {
        return pgeyFallback;
    }

    public String pgeyFolder() {
        return "/models/humanos/" + pgeyKey + "/";
    }

    public String pgeyObjResource() {
        return pgeyFolder() + pgeyKey + ".obj";
    }

    public static PgeyPersona pgeyFromAudience(String pgeyAudience) {
        if (pgeyAudience == null) {
            return null;
        }
        return switch (pgeyAudience) {
            case "hombre" -> HOMBRE;
            case "mujer" -> MUJER;
            case "nino", "juvenil" -> NINO;
            case "nina" -> NINA;
            default -> null;
        };
    }

    public static PgeyPersona pgeyFromModelKey(String pgeyModelKey) {
        if (pgeyModelKey == null) {
            return null;
        }
        String[] pgeyParts = pgeyModelKey.split("_");
        return pgeyParts.length >= 3 ? pgeyFromAudience(pgeyParts[1]) : null;
    }

    public static PgeyPersona pgeyForDesign(String pgeyModelKey, PgeyBodyType pgeyBody) {
        PgeyPersona pgeyP = pgeyFromModelKey(pgeyModelKey);
        if (pgeyP != null) {
            return pgeyP;
        }
        return pgeyBody == PgeyBodyType.ADULTO_MUJER ? MUJER : pgeyBody == PgeyBodyType.JUVENIL ? NINO : HOMBRE;
    }

    @Override
    public String toString() {
        return pgeyLabel;
    }
}
