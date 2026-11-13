package com.sportforce3d.three;

public enum ViewPreset {
    FRONT("Frente", 0),
    BACK("Espalda", 180),
    LEFT("Izquierda", -90),
    RIGHT("Derecha", 90);

    private final String label;
    private final double yaw;

    ViewPreset(String label, double yaw) {
        this.label = label;
        this.yaw = yaw;
    }

    public String label() {
        return label;
    }

    public double yaw() {
        return yaw;
    }
}
