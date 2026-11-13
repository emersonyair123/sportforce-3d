package com.sportforce3d.design;

public enum PgeyElementType {
    NAME("Nombre", "TEXT"),
    NUMBER("Número", "NUMBER"),
    EMBLEM("Emblema", "EMBLEM"),
    LOGO("Logo", "UPLOAD");

    private final String pgeyLabel;
    private final String pgeyDbType;

    PgeyElementType(String pgeyLabel, String pgeyDbType) {
        this.pgeyLabel = pgeyLabel;
        this.pgeyDbType = pgeyDbType;
    }

    public String pgeyLabel() {
        return pgeyLabel;
    }

    public String pgeyDbType() {
        return pgeyDbType;
    }

    public boolean pgeyIsText() {
        return this == NAME || this == NUMBER;
    }
}
