package com.sportforce3d.three.geom;

import java.util.Arrays;

public final class Curve {

    private final double[] xs;
    private final double[] ys;
    private final double[] ms;

    private Curve(double[] xs, double[] ys) {
        this.xs = xs;
        this.ys = ys;
        this.ms = slopes(xs, ys);
    }

    public static Curve of(double... pairs) {
        if (pairs.length < 4 || pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Se necesitan al menos 2 pares (x, y)");
        }
        int n = pairs.length / 2;
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = pairs[2 * i];
            ys[i] = pairs[2 * i + 1];
            if (i > 0 && xs[i] <= xs[i - 1]) {
                throw new IllegalArgumentException("Las x deben ser crecientes: " + Arrays.toString(xs));
            }
        }
        return new Curve(xs, ys);
    }

    public Curve scaled(double k) {
        double[] y2 = new double[ys.length];
        for (int i = 0; i < ys.length; i++) {
            y2[i] = ys[i] * k;
        }
        return new Curve(xs.clone(), y2);
    }

    public double minX() {
        return xs[0];
    }

    public double maxX() {
        return xs[xs.length - 1];
    }

    public double at(double x) {
        int n = xs.length;
        if (x <= xs[0]) {
            return ys[0];
        }
        if (x >= xs[n - 1]) {
            return ys[n - 1];
        }
        int k = Arrays.binarySearch(xs, x);
        if (k >= 0) {
            return ys[k];
        }
        k = -k - 2;
        double h = xs[k + 1] - xs[k];
        double t = (x - xs[k]) / h;
        double t2 = t * t;
        double t3 = t2 * t;
        double h00 = 2 * t3 - 3 * t2 + 1;
        double h10 = t3 - 2 * t2 + t;
        double h01 = -2 * t3 + 3 * t2;
        double h11 = t3 - t2;
        return h00 * ys[k] + h10 * h * ms[k] + h01 * ys[k + 1] + h11 * h * ms[k + 1];
    }

    private static double[] slopes(double[] xs, double[] ys) {
        int n = xs.length;
        double[] d = new double[n - 1];
        double[] h = new double[n - 1];
        for (int i = 0; i < n - 1; i++) {
            h[i] = xs[i + 1] - xs[i];
            d[i] = (ys[i + 1] - ys[i]) / h[i];
        }
        double[] m = new double[n];
        m[0] = d[0];
        m[n - 1] = d[n - 2];
        for (int i = 1; i < n - 1; i++) {
            if (d[i - 1] * d[i] <= 0) {
                m[i] = 0;
            } else {
                double w1 = 2 * h[i] + h[i - 1];
                double w2 = h[i] + 2 * h[i - 1];
                m[i] = (w1 + w2) / (w1 / d[i - 1] + w2 / d[i]);
            }
        }
        if (n > 2) {
            m[0] = endSlope(h[0], h[1], d[0], d[1]);
            m[n - 1] = endSlope(h[n - 2], h[n - 3], d[n - 2], d[n - 3]);
        }
        return m;
    }

    private static double endSlope(double h0, double h1, double d0, double d1) {
        double m = ((2 * h0 + h1) * d0 - h0 * d1) / (h0 + h1);
        if (Math.signum(m) != Math.signum(d0)) {
            return 0;
        }
        if (Math.signum(d0) != Math.signum(d1) && Math.abs(m) > Math.abs(3 * d0)) {
            return 3 * d0;
        }
        return m;
    }

    public static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    public static double bell(double x, double center, double sigma) {
        double d = (x - center) / sigma;
        return Math.exp(-0.5 * d * d);
    }

    public static double angleDiff(double a, double b) {
        double d = (a - b) % (2 * Math.PI);
        if (d > Math.PI) {
            d -= 2 * Math.PI;
        } else if (d <= -Math.PI) {
            d += 2 * Math.PI;
        }
        return d;
    }

    public static double angularBell(double angle, double center, double sigma) {
        return bell(angleDiff(angle, center), 0, sigma);
    }
}
