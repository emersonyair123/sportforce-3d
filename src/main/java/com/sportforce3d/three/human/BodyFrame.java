package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.Tube;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.procedural.BodyShape;
import com.sportforce3d.three.procedural.ProceduralModelProvider;

import java.util.Arrays;

/**
 * "Esqueleto" de un cuerpo para trasladar prendas de uno a otro, en espacio del modelo (metros, Y arriba, +Z
 * frente, +X = izquierda): alturas de referencia (entrepierna, axila, cuello, coronilla), eje del torso y de cada
 * pierna por altura, y eje de cada brazo. {@link #measure} lo mide en una malla cualquiera (las personas de
 * MakeHuman, en pose A); {@link #of(BodyShape, double[])} lo toma del maniquí procedural, que lo conoce exacto.
 */
abstract class BodyFrame {

    final double floor;
    final double crotch;
    final double armpit;
    final double neck;
    final double top;

    BodyFrame(double floor, double crotch, double armpit, double neck, double top) {
        this.floor = floor;
        this.crotch = crotch;
        this.armpit = armpit;
        this.neck = neck;
        this.top = top;
    }

    double height() {
        return top - floor;
    }

    /** Centro del torso a la altura y (x = 0). */
    abstract Vec3 torsoCenter(double y);

    /** Centro de la pierna ({@code sign} −1 derecha, +1 izquierda) a la altura y. */
    abstract Vec3 legCenter(int sign, double y);

    /** Punto del eje del brazo a {@code s} metros desde donde cruza la altura de la axila (positivo = hacia la mano). */
    abstract Vec3 armPoint(int sign, double s);

    /** Dirección del eje del brazo (hacia la mano) en {@code s}. */
    abstract Vec3 armDirection(int sign, double s);

    /** Parámetro {@code s} del punto del eje del brazo más cercano a p. */
    abstract double armProject(int sign, Vec3 p);

    /**
     * Radio exacto de la piel desde el eje en la dirección horizontal {@code dir}: solo lo sabe el maniquí
     * procedural (sus piezas se solapan, así que un rayo contra su malla choca con paredes internas). NaN = medirlo.
     */
    double torsoRadius(double y, Vec3 dir) {
        return Double.NaN;
    }

    double legRadius(int sign, double y, Vec3 dir) {
        return Double.NaN;
    }

    /**
     * Distancia desde {@code origin} (dentro del torso) hasta donde sale el rayo de la superficie del torso, en
     * cualquier dirección (hombros y cuello). NaN = medirlo con la malla.
     */
    double torsoExit(Vec3 origin, Vec3 dir) {
        return Double.NaN;
    }

    /** Radio del brazo en {@code s} en la dirección {@code dir} (perpendicular al eje). */
    double armRadius(int sign, double s, Vec3 dir) {
        return Double.NaN;
    }

    /** true si el punto está dentro del torso (solo lo sabe el maniquí procedural). */
    boolean insideTorso(Vec3 p) {
        return false;
    }

    // ================================================================ Maniquí procedural

    static BodyFrame of(BodyShape shape, double[] mannequinVertices) {
        return new Procedural(shape, mannequinVertices);
    }

    private static final class Procedural extends BodyFrame {
        private final BodyShape b;
        private final double scale;
        /** Escala horizontal (x, z) de diseño a modelo. */
        private final double girth;
        /** Eje de cada brazo muestreado: [lado][muestra] y su longitud acumulada. */
        private final Vec3[][] arm = new Vec3[2][];
        private final double[][] armLen = new double[2][];
        private final double[] armpitS = new double[2];

        Procedural(BodyShape b, double[] v) {
            // Entrepierna = la del short (su costura de entrepierna se lleva a la de la persona).
            super(0, ProceduralModelProvider.SHORTS_CROTCH * b.height() / 1.812, ProceduralModelProvider.ARMPIT * b.height() / 1.812,
                    neckBase(v, ProceduralModelProvider.ARMPIT * b.height() / 1.812, b.height()), b.height());
            this.b = b;
            this.scale = b.height() / 1.812;
            this.girth = b.out(new Vec3(1, 0, 0)).x();
            for (BodyShape.Side side : BodyShape.Side.values()) {
                Tube t = b.arm(side);
                int n = 200;
                Vec3[] pts = new Vec3[n + 1];
                double[] len = new double[n + 1];
                for (int i = 0; i <= n; i++) {
                    pts[i] = b.out(t.spine((double) i / n));
                    len[i] = i == 0 ? 0 : len[i - 1] + pts[i].sub(pts[i - 1]).length();
                }
                int k = side.sign > 0 ? 1 : 0;
                arm[k] = pts;
                armLen[k] = len;
                armpitS[k] = arcAtHeight(pts, len, armpit);
            }
        }

        @Override
        Vec3 torsoCenter(double y) {
            Vec3 a = b.out(b.torsoAxis(y / scale));
            return new Vec3(0, y, a.z());
        }

        @Override
        Vec3 legCenter(int sign, double y) {
            BodyShape.Side side = sign > 0 ? BodyShape.Side.LEFT : BodyShape.Side.RIGHT;
            Tube leg = b.leg(side);
            Vec3 c = b.out(leg.center(b.legFractionAt(side, y / scale)));
            return new Vec3(c.x(), y, c.z());
        }

        @Override
        Vec3 armPoint(int sign, double s) {
            int k = sign > 0 ? 1 : 0;
            return at(arm[k], armLen[k], armpitS[k] + s);
        }

        @Override
        Vec3 armDirection(int sign, double s) {
            int k = sign > 0 ? 1 : 0;
            double a = armpitS[k] + s;
            return at(arm[k], armLen[k], a + 0.01).sub(at(arm[k], armLen[k], a - 0.01)).normalize();
        }

        @Override
        double armProject(int sign, Vec3 p) {
            int k = sign > 0 ? 1 : 0;
            Vec3[] pts = arm[k];
            double[] len = armLen[k];
            double best = Double.MAX_VALUE;
            double bestS = 0;
            for (int i = 0; i < pts.length - 1; i++) {
                Vec3 ab = pts[i + 1].sub(pts[i]);
                double l2 = ab.dot(ab);
                double t = l2 < 1e-12 ? 0 : Math.max(0, Math.min(1, p.sub(pts[i]).dot(ab) / l2));
                double d = pts[i].addScaled(ab, t).sub(p).length();
                if (d < best) {
                    best = d;
                    bestS = len[i] + t * Math.sqrt(l2);
                }
            }
            return bestS - armpitS[k];
        }

        @Override
        double torsoRadius(double y, Vec3 dir) {
            return b.torsoRadius(Math.atan2(dir.x(), dir.z()), y / scale) * girth;
        }

        @Override
        double legRadius(int sign, double y, Vec3 dir) {
            BodyShape.Side side = sign > 0 ? BodyShape.Side.LEFT : BodyShape.Side.RIGHT;
            return ellipse(b.leg(side), b.legFractionAt(side, y / scale), dir) * girth;
        }

        @Override
        double armRadius(int sign, double s, Vec3 dir) {
            int k = sign > 0 ? 1 : 0;
            double total = armLen[k][armLen[k].length - 1];
            double f = Math.max(0, Math.min(1, (armpitS[k] + s) / total));
            return ellipse(b.arm(sign > 0 ? BodyShape.Side.LEFT : BodyShape.Side.RIGHT), f, dir) * girth;
        }

        @Override
        double torsoExit(Vec3 origin, Vec3 dir) {
            double step = 0.006;
            double t = 0;
            while (t < 0.6 && insideTorso(origin.addScaled(dir, t))) {
                t += step;
            }
            if (t >= 0.6) {
                return Double.NaN;
            }
            double lo = Math.max(0, t - step);
            double hi = t;
            for (int i = 0; i < 20; i++) {
                double mid = (lo + hi) / 2;
                if (insideTorso(origin.addScaled(dir, mid))) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            return (lo + hi) / 2;
        }

        @Override
        boolean insideTorso(Vec3 q) {
            double yd = q.y() / scale;
            if (yd > BodyShape.TORSO_TOP || yd < BodyShape.TORSO_BOTTOM) {
                return false;
            }
            Vec3 c = torsoCenter(q.y());
            double dx = q.x() - c.x();
            double dz = q.z() - c.z();
            double r = Math.sqrt(dx * dx + dz * dz);
            return r < b.torsoRadius(Math.atan2(dx, dz), yd) * girth;
        }

        /** Radio de la sección elíptica del tubo en la dirección {@code dir}. */
        private static double ellipse(Tube t, double f, Vec3 dir) {
            double a = dir.dot(t.frontAxis(f));
            double c = dir.dot(t.sideAxis(f));
            double rz = t.radiusFront(f);
            double rx = t.radiusSide(f);
            return 1 / Math.sqrt((a / rz) * (a / rz) + (c / rx) * (c / rx));
        }

        private static Vec3 at(Vec3[] pts, double[] len, double s) {
            if (s <= 0) {
                return pts[0].addScaled(pts[1].sub(pts[0]).normalize(), s);
            }
            for (int i = 1; i < pts.length; i++) {
                if (len[i] >= s) {
                    double seg = len[i] - len[i - 1];
                    return pts[i - 1].lerp(pts[i], seg < 1e-12 ? 0 : (s - len[i - 1]) / seg);
                }
            }
            return pts[pts.length - 1];
        }

        private static double arcAtHeight(Vec3[] pts, double[] len, double y) {
            for (int i = 1; i < pts.length; i++) {
                if (pts[i].y() <= y) {
                    double dy = pts[i - 1].y() - pts[i].y();
                    double t = dy < 1e-12 ? 0 : (pts[i - 1].y() - y) / dy;
                    return len[i - 1] + t * (len[i] - len[i - 1]);
                }
            }
            return 0;
        }
    }

    // ================================================================ Malla medida (MakeHuman)

    /** Mide una persona en pose A sobre los vértices de su piel (espacio del modelo). */
    static BodyFrame measure(double[] v, double floor, double height) {
        return new Measured(v, floor, height);
    }

    private static final class Measured extends BodyFrame {
        private static final double STEP = 0.004;
        private final double[] torsoZ;
        private final double[][] legX = new double[2][];
        private final double[][] legZ = new double[2][];
        private final Vec3[] armOrigin = new Vec3[2];
        private final Vec3[] armDir = new Vec3[2];

        Measured(double[] v, double floor, double h) {
            this(new Slices(v), floor, h);
        }

        private Measured(Slices s, double floor, double h) {
            super(floor, lowestCenter(s.v, h + floor), s.armpit(floor, h), Double.NaN, floor + h);
            // neck depende de la axila medida: se recalcula abajo con el valor final.
            this.neckMeasured = neckBase(s.v, armpit, top);
            int n = (int) Math.ceil(h / STEP) + 2;
            torsoZ = new double[n];
            for (int i = 0; i < n; i++) {
                double y = floor + i * STEP;
                double[] box = s.contour(y, -0.03, 0.03);
                torsoZ[i] = box == null ? Double.NaN : (box[2] + box[3]) / 2;
            }
            fillGaps(torsoZ);
            double legMax = 0.135 * h;
            for (int k = 0; k < 2; k++) {
                int sign = k == 0 ? -1 : 1;
                legX[k] = new double[n];
                legZ[k] = new double[n];
                for (int i = 0; i < n; i++) {
                    double y = floor + i * STEP;
                    double[] box = y < crotch - 0.01 ? s.contour(y, sign < 0 ? -legMax : 0.0, sign < 0 ? 0.0 : legMax) : null;
                    legX[k][i] = box == null ? Double.NaN : (box[0] + box[1]) / 2;
                    legZ[k][i] = box == null ? Double.NaN : (box[2] + box[3]) / 2;
                }
                fillGaps(legX[k]);
                fillGaps(legZ[k]);
                fitArm(s, k, sign, h);
            }
        }

        private final double neckMeasured;

        private void fitArm(Slices s, int k, int sign, double h) {
            // Brazo (sin el torso) entre la axila y el codo: componente principal = dirección del brazo.
            double y0 = armpit - 0.11 * h;
            double y1 = armpit - 0.015 * h;
            double[] sum = new double[3];
            int count = 0;
            double[][] pts = s.armVertices(sign, y0, y1);
            for (double[] p : pts) {
                sum[0] += p[0];
                sum[1] += p[1];
                sum[2] += p[2];
                count++;
            }
            if (count < 20) {
                // Sin brazo separado del torso: dirección del maniquí (casi vertical, un poco hacia afuera).
                armOrigin[k] = new Vec3(sign * 0.11 * h, armpit, 0);
                armDir[k] = new Vec3(sign * 0.25, -1, 0).normalize();
                return;
            }
            Vec3 m = new Vec3(sum[0] / count, sum[1] / count, sum[2] / count);
            double[][] cov = new double[3][3];
            for (double[] p : pts) {
                double[] d = {p[0] - m.x(), p[1] - m.y(), p[2] - m.z()};
                for (int a = 0; a < 3; a++) {
                    for (int b = 0; b < 3; b++) {
                        cov[a][b] += d[a] * d[b];
                    }
                }
            }
            Vec3 dir = new Vec3(sign * 0.6, -0.8, 0);
            for (int it = 0; it < 60; it++) {
                dir = new Vec3(cov[0][0] * dir.x() + cov[0][1] * dir.y() + cov[0][2] * dir.z(),
                        cov[1][0] * dir.x() + cov[1][1] * dir.y() + cov[1][2] * dir.z(),
                        cov[2][0] * dir.x() + cov[2][1] * dir.y() + cov[2][2] * dir.z()).normalize();
            }
            if (dir.y() > 0) {
                dir = dir.mul(-1);
            }
            // Origen: donde el eje cruza la altura de la axila.
            double t = (armpit - m.y()) / dir.y();
            armOrigin[k] = m.addScaled(dir, t);
            armDir[k] = dir;
        }

        @Override
        Vec3 torsoCenter(double y) {
            return new Vec3(0, y, sample(torsoZ, y));
        }

        @Override
        Vec3 legCenter(int sign, double y) {
            int k = sign > 0 ? 1 : 0;
            double yy = Math.min(y, crotch - 0.02);
            return new Vec3(sample(legX[k], yy), y, sample(legZ[k], yy));
        }

        @Override
        Vec3 armPoint(int sign, double s) {
            int k = sign > 0 ? 1 : 0;
            return armOrigin[k].addScaled(armDir[k], s);
        }

        @Override
        Vec3 armDirection(int sign, double s) {
            return armDir[sign > 0 ? 1 : 0];
        }

        @Override
        double armProject(int sign, Vec3 p) {
            int k = sign > 0 ? 1 : 0;
            return p.sub(armOrigin[k]).dot(armDir[k]);
        }

        double neckMeasured() {
            return neckMeasured;
        }

        private double sample(double[] table, double y) {
            double f = (y - floor) / STEP;
            int i = (int) Math.floor(f);
            if (i < 0) {
                return table[0];
            }
            if (i >= table.length - 1) {
                return table[table.length - 1];
            }
            double t = f - i;
            return table[i] * (1 - t) + table[i + 1] * t;
        }
    }

    /** Copia del cuerpo medido con el cuello medido (el constructor no lo puede pasar a super). */
    static BodyFrame measureWithNeck(double[] v, double floor, double height) {
        Measured m = new Measured(v, floor, height);
        return new Delegate(m, m.neckMeasured());
    }

    private static final class Delegate extends BodyFrame {
        private final BodyFrame d;

        Delegate(BodyFrame d, double neck) {
            super(d.floor, d.crotch, d.armpit, neck, d.top);
            this.d = d;
        }

        @Override
        Vec3 torsoCenter(double y) {
            return d.torsoCenter(y);
        }

        @Override
        Vec3 legCenter(int sign, double y) {
            return d.legCenter(sign, y);
        }

        @Override
        Vec3 armPoint(int sign, double s) {
            return d.armPoint(sign, s);
        }

        @Override
        Vec3 armDirection(int sign, double s) {
            return d.armDirection(sign, s);
        }

        @Override
        double armProject(int sign, Vec3 p) {
            return d.armProject(sign, p);
        }
    }

    // ================================================================ Mediciones

    /** Vértices ordenados por altura para consultar franjas horizontales. */
    private static final class Slices {
        final double[] v;
        final int[] order;
        final double[] ys;

        Slices(double[] v) {
            this.v = v;
            int n = v.length / 3;
            Integer[] idx = new Integer[n];
            for (int i = 0; i < n; i++) {
                idx[i] = i;
            }
            Arrays.sort(idx, (a, b) -> Double.compare(v[a * 3 + 1], v[b * 3 + 1]));
            order = new int[n];
            ys = new double[n];
            for (int i = 0; i < n; i++) {
                order[i] = idx[i];
                ys[i] = v[idx[i] * 3 + 1];
            }
        }

        private int lower(double y) {
            int i = Arrays.binarySearch(ys, y);
            if (i < 0) {
                return -i - 1;
            }
            while (i > 0 && ys[i - 1] == y) {
                i--;
            }
            return i;
        }

        /**
         * Caja [minX, maxX, minZ, maxZ] del contorno de la malla en el plano y (puntos donde las aristas lo cruzan)
         * con x en [x0, x1]; null si no hay. A diferencia de los vértices de una franja, el contorno no tiene
         * huecos aunque la malla sea gruesa.
         */
        double[] contour(double y, double x0, double x1) {
            double[] b = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
            boolean any = false;
            for (int t = 0; t < tri.length; t += 3) {
                for (int e = 0; e < 3; e++) {
                    int a = tri[t + e] * 3;
                    int c = tri[t + (e + 1) % 3] * 3;
                    double ya = v[a + 1];
                    double yc = v[c + 1];
                    if ((ya - y) * (yc - y) > 0 || ya == yc) {
                        continue;
                    }
                    double k = (y - ya) / (yc - ya);
                    double x = v[a] + k * (v[c] - v[a]);
                    if (x < x0 || x > x1) {
                        continue;
                    }
                    double z = v[a + 2] + k * (v[c + 2] - v[a + 2]);
                    b[0] = Math.min(b[0], x);
                    b[1] = Math.max(b[1], x);
                    b[2] = Math.min(b[2], z);
                    b[3] = Math.max(b[3], z);
                    any = true;
                }
            }
            return any ? b : null;
        }

        /** Caja [minX, maxX, minZ, maxZ] de los vértices de la franja y ± half con x en [x0, x1]; null si vacía. */
        double[] box(double y, double half, double x0, double x1, double z0, double z1) {
            double[] b = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
            boolean any = false;
            for (int i = lower(y - half); i < ys.length && ys[i] <= y + half; i++) {
                int p = order[i] * 3;
                double x = v[p];
                double z = v[p + 2];
                if (x >= x0 && x <= x1 && z >= z0 && z <= z1) {
                    b[0] = Math.min(b[0], x);
                    b[1] = Math.max(b[1], x);
                    b[2] = Math.min(b[2], z);
                    b[3] = Math.max(b[3], z);
                    any = true;
                }
            }
            return any ? b : null;
        }

        /**
         * Medio ancho del torso en la franja: el último |x| antes del primer hueco de más de 1 cm entre el torso y
         * el brazo; NaN si no hay hueco (el brazo está unido o no hay brazo a esa altura).
         */
        double torsoHalfWidth(double y, int sign) {
            double half = 0.004;
            int from = lower(y - half);
            int to = from;
            while (to < ys.length && ys[to] <= y + half) {
                to++;
            }
            double[] u = new double[to - from];
            int n = 0;
            for (int i = from; i < to; i++) {
                double x = v[order[i] * 3] * sign;
                if (x > 0.02) {
                    u[n++] = x;
                }
            }
            Arrays.sort(u, 0, n);
            for (int i = 1; i < n; i++) {
                if (u[i] - u[i - 1] > 0.01 && u[i - 1] > 0.05 && u[n - 1] - u[i - 1] > 0.03) {
                    return u[i - 1];
                }
            }
            return Double.NaN;
        }

        /**
         * Altura de la axila: bajando desde el cuello, la primera franja donde ambos brazos se separan del torso y
         * siguen separados al menos 6 % de la altura más abajo (cerca de los hombros hay huecos cortos que no son
         * la axila).
         */
        double armpit(double floor, double h) {
            double step = 0.003;
            int need = (int) Math.ceil(0.06 * h / step);
            int run = 0;
            double top = Double.NaN;
            for (double y = floor + 0.86 * h; y > floor + 0.5 * h; y -= step) {
                if (!Double.isNaN(torsoHalfWidth(y, -1)) && !Double.isNaN(torsoHalfWidth(y, 1))) {
                    if (run == 0) {
                        top = y;
                    }
                    if (++run >= need) {
                        return top;
                    }
                } else {
                    run = 0;
                }
            }
            return floor + 0.73 * h;
        }

        /** Vértices del brazo (más allá del hueco con el torso) entre y0 e y1. */
        double[][] armVertices(int sign, double y0, double y1) {
            java.util.List<double[]> out = new java.util.ArrayList<>();
            double step = 0.008;
            for (double y = y0; y < y1; y += step) {
                double w = torsoHalfWidth(y + step / 2, sign);
                if (Double.isNaN(w)) {
                    continue;
                }
                for (int i = lower(y); i < ys.length && ys[i] < y + step; i++) {
                    int p = order[i] * 3;
                    if (v[p] * sign > w + 0.004) {
                        out.add(new double[]{v[p], v[p + 1], v[p + 2]});
                    }
                }
            }
            return out.toArray(new double[0][]);
        }
    }

    /** Punto más bajo del cuerpo en el centro (|x| < 1.2 cm) por encima de los pies: la entrepierna. */
    static double lowestCenter(double[] v, double top) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < v.length; i += 3) {
            double y = v[i + 1];
            if (Math.abs(v[i]) < 0.012 && y > 0.3 * top && y < 0.65 * top) {
                best = Math.min(best, y);
            }
        }
        return best == Double.MAX_VALUE ? 0.47 * top : best;
    }

    /** Base del cuello: subiendo desde la axila, donde el ancho cae por debajo del 45 % del de los hombros. */
    static double neckBase(double[] v, double armpit, double top) {
        double h = top;
        double step = 0.004;
        int n = (int) Math.ceil((top - armpit) / step) + 1;
        double[] width = new double[n];
        for (int i = 0; i < v.length; i += 3) {
            double y = v[i + 1];
            if (y >= armpit && y < top) {
                int k = (int) ((y - armpit) / step);
                width[k] = Math.max(width[k], Math.abs(v[i]));
            }
        }
        double shoulders = 0;
        for (int k = 0; k < n && k * step < 0.08 * h; k++) {
            shoulders = Math.max(shoulders, width[k]);
        }
        for (int k = 0; k < n; k++) {
            if (k * step > 0.02 && width[k] > 0 && width[k] < 0.45 * shoulders) {
                return armpit + k * step;
            }
        }
        return armpit + 0.1 * h;
    }

    private static void fillGaps(double[] t) {
        int first = -1;
        for (int i = 0; i < t.length; i++) {
            if (!Double.isNaN(t[i])) {
                first = i;
                break;
            }
        }
        if (first < 0) {
            Arrays.fill(t, 0);
            return;
        }
        for (int i = 0; i < first; i++) {
            t[i] = t[first];
        }
        for (int i = first + 1; i < t.length; i++) {
            if (Double.isNaN(t[i])) {
                t[i] = t[i - 1];
            }
        }
    }
}
