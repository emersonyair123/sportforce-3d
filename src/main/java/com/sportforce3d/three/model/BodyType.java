package com.sportforce3d.three.model;

/** Tipos de maniquí del diseñador. {@code key} se usa para buscar modelos OBJ (resources/models/{key}.obj). */
public enum BodyType {
    ADULTO_HOMBRE("adulto-hombre", "Adulto hombre"),
    ADULTO_MUJER("adulto-mujer", "Adulto mujer"),
    JUVENIL("juvenil", "Juvenil");

    private final String key;
    private final String label;

    BodyType(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
