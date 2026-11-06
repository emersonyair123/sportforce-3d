package com.sportforce3d.three.geom;

/** Vector 3D inmutable en espacio del modelo: metros, Y hacia arriba, +Z hacia el frente del maniquí. */
public record Vec3(double x, double y, double z) {

    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public static final Vec3 UP = new Vec3(0, 1, 0);
    public static final Vec3 FRONT = new Vec3(0, 0, 1);

    public Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    public Vec3 sub(Vec3 o) {
        return new Vec3(x - o.x, y - o.y, z - o.z);
    }

    public Vec3 mul(double k) {
        return new Vec3(x * k, y * k, z * k);
    }

    /** this + o·k */
    public Vec3 addScaled(Vec3 o, double k) {
        return new Vec3(x + o.x * k, y + o.y * k, z + o.z * k);
    }

    public double dot(Vec3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public Vec3 cross(Vec3 o) {
        return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /** Vector unitario; si la longitud es casi cero devuelve ZERO. */
    public Vec3 normalize() {
        double len = length();
        return len < 1e-12 ? ZERO : new Vec3(x / len, y / len, z / len);
    }

    public Vec3 lerp(Vec3 o, double t) {
        return new Vec3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t);
    }

    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }
}
