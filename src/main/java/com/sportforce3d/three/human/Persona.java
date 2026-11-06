package com.sportforce3d.three.human;

import com.sportforce3d.three.model.BodyType;

/**
 * Personas 3D de la Vista 3D (exportadas con MakeHuman, CC0) en {@code resources/models/humanos/{key}/{key}.obj}.
 * {@code key} coincide con la categoría de la vista realista ({@code futbol_<key>_frente}), así las dos vistas
 * muestran a la misma persona.
 */
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

    /** Maniquí procedural que se muestra si falta el modelo de la persona. */
    public BodyType fallback() {
        return fallback;
    }

    /** Carpeta del recurso, con barra final. */
    public String folder() {
        return "/models/humanos/" + key + "/";
    }

    public String objResource() {
        return folder() + key + ".obj";
    }

    /**
     * Persona de una categoría de la vista realista ({@code hombre}, {@code mujer}, {@code nino}, {@code nina};
     * {@code juvenil} → niño). null si la categoría no es de una persona (productos, zapatillas).
     */
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

    /** Persona de una clave de modelo realista ({@code corte_categoria_vista}); null si no tiene. */
    public static Persona fromModelKey(String modelKey) {
        if (modelKey == null) {
            return null;
        }
        String[] parts = modelKey.split("_");
        return parts.length >= 3 ? fromAudience(parts[1]) : null;
    }

    /**
     * Persona de un diseño: la de su vista realista o, si esa no es de una persona (productos sin modelo), la del
     * maniquí elegido (juvenil → niño).
     */
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
