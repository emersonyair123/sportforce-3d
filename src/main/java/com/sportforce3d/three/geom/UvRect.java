package com.sportforce3d.three.geom;

/** Rectángulo de una isla UV dentro de la textura de una prenda (0..1). V crece hacia abajo, como en una imagen. */
public record UvRect(double u0, double v0, double u1, double v1) {

    public UvRect {
        if (!(u0 >= 0 && v0 >= 0 && u1 <= 1 && v1 <= 1 && u0 < u1 && v0 < v1)) {
            throw new IllegalArgumentException("Isla UV inválida: " + u0 + "," + v0 + " → " + u1 + "," + v1);
        }
    }

    /** Punto de la isla para coordenadas locales (0..1, 0..1). */
    public float u(double local) {
        return (float) (u0 + (u1 - u0) * clamp(local));
    }

    public float v(double local) {
        return (float) (v0 + (v1 - v0) * clamp(local));
    }

    public boolean overlaps(UvRect o) {
        return u0 < o.u1 && o.u0 < u1 && v0 < o.v1 && o.v0 < v1;
    }

    private static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }
}
