package com.sportforce3d.design;

public enum PgeyPatternType {
    LISO("Liso"),
    FRANJAS_HORIZONTALES("Franjas horizontales"),
    FRANJAS_VERTICALES("Franjas verticales"),
    DIAGONALES("Diagonales"),
    DEGRADADO("Degradado"),
    CUADROS("Cuadros"),
    SUBLIMADO("Sublimado geométrico");

    private final String pgeyLabel;

    PgeyPatternType(String pgeyLabel) {
        this.pgeyLabel = pgeyLabel;
    }

    public String pgeyLabel() {
        return pgeyLabel;
    }
}
