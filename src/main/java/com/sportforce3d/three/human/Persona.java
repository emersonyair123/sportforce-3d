package com.sportforce3d.three.human;

import com.sportforce3d.three.model.BodyType;

public enum Persona {
    HOMBRE("hombre", "Hombre", BodyType.ADULTO_HOMBRE),
    MUJER("mujer", "Mujer", BodyType.ADULTO_MUJER),
    NINO("nino", "Niño", BodyType.JUVENIL),
    NINA("nina", "Niña", BodyType.JUVENIL);

    private final String key;
    private final String label;
    private final BodyType fallback;

    Persona(String key, String label, BodyType fallback) {
        this.key = key;
        this.label = label;
        this.fallback = fallback;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public BodyType fallback() {
        return fallback;
    }

    public String folder() {
        return "/models/humanos/" + key + "/";
    }

    public String objResource() {
        return folder() + key + ".obj";
    }

    public static Persona fromAudience(String audience) {
        if (audience == null) {
            return null;
        }
        return switch (audience) {
            case "hombre" -> HOMBRE;
            case "mujer" -> MUJER;
            case "nino", "juvenil" -> NINO;
            case "nina" -> NINA;
            default -> null;
        };
    }

    public static Persona fromModelKey(String modelKey) {
        if (modelKey == null) {
            return null;
        }
        String[] parts = modelKey.split("_");
        return parts.length >= 3 ? fromAudience(parts[1]) : null;
    }

    public static Persona forDesign(String modelKey, BodyType body) {
        Persona p = fromModelKey(modelKey);
        if (p != null) {
            return p;
        }
        return body == BodyType.ADULTO_MUJER ? MUJER : body == BodyType.JUVENIL ? NINO : HOMBRE;
    }

    @Override
    public String toString() {
        return label;
    }
}
