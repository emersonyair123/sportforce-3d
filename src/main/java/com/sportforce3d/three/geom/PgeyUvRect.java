package com.sportforce3d.three.geom;

public record PgeyUvRect(double pgeyU0, double pgeyV0, double pgeyU1, double pgeyV1) {

    public PgeyUvRect {
        if (!(pgeyU0 >= 0 && pgeyV0 >= 0 && pgeyU1 <= 1 && pgeyV1 <= 1 && pgeyU0 < pgeyU1 && pgeyV0 < pgeyV1)) {
            throw new IllegalArgumentException("Isla UV inválida: " + pgeyU0 + "," + pgeyV0 + " → " + pgeyU1 + "," + pgeyV1);
        }
    }

    public float pgeyU(double pgeyLocal) {
        return (float) (pgeyU0 + (pgeyU1 - pgeyU0) * pgeyClamp(pgeyLocal));
    }

    public float pgeyV(double pgeyLocal) {
        return (float) (pgeyV0 + (pgeyV1 - pgeyV0) * pgeyClamp(pgeyLocal));
    }

    public boolean pgeyOverlaps(PgeyUvRect pgeyO) {
        return pgeyU0 < pgeyO.pgeyU1 && pgeyO.pgeyU0 < pgeyU1 && pgeyV0 < pgeyO.pgeyV1 && pgeyO.pgeyV0 < pgeyV1;
    }

    private static double pgeyClamp(double pgeyX) {
        return Math.max(0, Math.min(1, pgeyX));
    }
}
