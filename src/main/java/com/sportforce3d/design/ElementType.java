package com.sportforce3d.design;

public enum ElementType {
    NAME("Nombre", "TEXT"),
    NUMBER("Número", "NUMBER"),
    EMBLEM("Emblema", "EMBLEM"),
    LOGO("Logo", "UPLOAD");

    private final String label;
    private final String dbType;

    ElementType(String label, String dbType) {
        this.label = label;
        this.dbType = dbType;
    }

    public String label() {
        return label;
    }

    public String dbType() {
        return dbType;
    }

    public boolean isText() {
        return this == NAME || this == NUMBER;
    }
}
