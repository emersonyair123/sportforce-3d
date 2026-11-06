package com.sportforce3d.three.geom;

import java.util.List;

/**
 * Tubo de sección elíptica variable a lo largo de una espina suave (Catmull-Rom por puntos de control).
 * Sirve para brazos, piernas y cuello. Los radios y el desplazamiento frontal se dan como curvas sobre la
 * fracción de longitud (0 = inicio, 1 = final).
 *
 * <p>Ángulo de la sección: 0 = frente (+Z), π/2 = lado exterior (hacia {@code outward}), π = atrás.
 */
public final class Tube {

    private static final int SAMPLES = 800;

    private final Vec3[] pts = new Vec3[SAMPLES + 1];
    private final double[] len = new double[SAMPLES + 1];
    private final Curve rx;
    private final Curve rz;
    private final Curve frontShift;
    private final Vec3 outward;

    /**
     * @param control    puntos de control de la espina (≥ 2)
     * @param rx         radio hacia el lado exterior según la fracción de longitud
     * @param rz         radio hacia el frente según la fracción de longitud
     * @param frontShift desplazamiento del centro hacia el frente (positivo) o atrás
     * @param outward    dirección "exterior" aproximada (p. ej. +X para el brazo izquierdo)
     */
    public Tube(List<Vec3> control, Curve rx, Curve rz, Curve frontShift, Vec3 outward) {
        this.rx = rx;
        this.rz = rz;
        this.frontShift = frontShift;
        this.outward = outward.normalize();
        int n = control.size();
        double acc = 0;
        for (int i = 0; i <= SAMPLES; i++) {
            double t = (double) i / SAMPLES * (n - 1);
            int k = Math.min((int) Math.floor(t), n - 2);
            double u = t - k;
            Vec3 p0 = control.get(Math.max(0, k - 1));
            Vec3 p1 = control.get(k);
            Vec3 p2 = control.get(k + 1);
            Vec3 p3 = control.get(Math.min(n - 1, k + 2));
            pts[i] = catmullRom(p0, p1, p2, p3, u);
            if (i > 0) {
                acc += pts[i].sub(pts[i - 1]).length();
            }
            len[i] = acc;
        }
    }

    public double length() {
        return len[SAMPLES];
    }

    /** Centro de la espina (sin desplazamiento frontal) en la fracción {@code f}. */
    public Vec3 spine(double f) {
        double target = Math.max(0, Math.min(1, f)) * length();
        int lo = 0;
        int hi = SAMPLES;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (len[mid] < target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double seg = len[hi] - len[lo];
        double t = seg < 1e-12 ? 0 : (target - len[lo]) / seg;
        return pts[lo].lerp(pts[hi], t);
    }

    public Vec3 tangent(double f) {
        double d = 0.002;
        Vec3 a = spine(Math.max(0, f - d));
        Vec3 b = spine(Math.min(1, f + d));
        return b.sub(a).normalize();
    }

    /** Eje frontal de la sección: +Z proyectado perpendicular a la espina. */
    public Vec3 frontAxis(double f) {
        Vec3 t = tangent(f);
        Vec3 front = Vec3.FRONT.sub(t.mul(t.dot(Vec3.FRONT)));
        if (front.length() < 1e-6) {
            front = Vec3.UP.sub(t.mul(t.dot(Vec3.UP)));
        }
        return front.normalize();
    }

    /** Eje lateral (hacia afuera) de la sección. */
    public Vec3 sideAxis(double f) {
        Vec3 t = tangent(f);
        Vec3 front = frontAxis(f);
        Vec3 side = t.cross(front).normalize();
        return side.dot(outward) < 0 ? side.mul(-1) : side;
    }

    public Vec3 center(double f) {
        return spine(f).addScaled(frontAxis(f), frontShift.at(f));
    }

    public double radiusSide(double f) {
        return rx.at(f);
    }

    public double radiusFront(double f) {
        return rz.at(f);
    }

    /** Punto de la superficie con un espesor extra {@code extra} (para prendas sobre el cuerpo). */
    public Vec3 point(double f, double angle, double extra) {
        Vec3 c = center(f);
        double s = Math.sin(angle);
        double co = Math.cos(angle);
        return c.addScaled(sideAxis(f), (rx.at(f) + extra) * s)
                .addScaled(frontAxis(f), (rz.at(f) + extra) * co);
    }

    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return new Vec3(
                cr(p0.x(), p1.x(), p2.x(), p3.x(), t, t2, t3),
                cr(p0.y(), p1.y(), p2.y(), p3.y(), t, t2, t3),
                cr(p0.z(), p1.z(), p2.z(), p3.z(), t, t2, t3));
    }

    private static double cr(double a, double b, double c, double d, double t, double t2, double t3) {
        return 0.5 * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3);
    }
}
