package com.sportforce3d.three.model;

public enum PgeyBodyType {
    ADULTO_HOMBRE("adulto-hombre", "Adulto hombre"),
    ADULTO_MUJER("adulto-mujer", "Adulto mujer"),
    JUVENIL("juvenil", "Juvenil");

    private final String pgeyKey;
    private final String pgeyLabel;

    PgeyBodyType(String pgeyKey, String pgeyLabel) {
        this.pgeyKey = pgeyKey;
        this.pgeyLabel = pgeyLabel;
    }

    public String pgeyKey() {
        return pgeyKey;
    }

    public String pgeyLabel() {
        return pgeyLabel;
    }

    @Override
    public String toString() {
        return pgeyLabel;
    }
}
