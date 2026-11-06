package com.sportforce3d.design;

public enum PatternType {
    LISO("Liso"),
    FRANJAS_HORIZONTALES("Franjas horizontales"),
    FRANJAS_VERTICALES("Franjas verticales"),
    DIAGONALES("Diagonales"),
    DEGRADADO("Degradado"),
    CUADROS("Cuadros"),
    SUBLIMADO("Sublimado geométrico");

    private final String label;

    PatternType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
