package com.sportforce3d.three;

public enum PgeyViewPreset {
    FRONT("Frente", 0),
    BACK("Espalda", 180),
    LEFT("Izquierda", -90),
    RIGHT("Derecha", 90);

    private final String pgeyLabel;
    private final double pgeyYaw;

    PgeyViewPreset(String pgeyLabel, double pgeyYaw) {
        this.pgeyLabel = pgeyLabel;
        this.pgeyYaw = pgeyYaw;
    }

    public String pgeyLabel() {
        return pgeyLabel;
    }

    public double pgeyYaw() {
        return pgeyYaw;
    }
}
