package com.sportforce3d.three.procedural;

import com.sportforce3d.three.geom.GridSurface;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Tube;
import com.sportforce3d.three.geom.UvRect;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.UvLayout;
import com.sportforce3d.three.procedural.BodyShape.Side;

import java.util.ArrayList;
import java.util.List;

import static com.sportforce3d.three.geom.Curve.angleDiff;
import static com.sportforce3d.three.geom.Curve.angularBell;
import static com.sportforce3d.three.geom.Curve.bell;
import static com.sportforce3d.three.geom.Curve.smoothstep;

/**
 * Camiseta (cuerpo, mangas, cuello en V, puños, paneles laterales), short (base por pierna, pretina, franjas)
 * y medias (caña y banda). Todo en espacio de diseño de {@link BodyShape}; la holgura se aplica sobre la
 * normal real de la superficie base, así que también es uniforme en la pendiente de los hombros.
 */
final class GarmentGenerator {

    // ---------------------------------------------------------------- Camiseta
    static final double HEM_FRONT = 0.990;
    static final double HEM_BACK = 0.972;
    static final double NECK_SIDE = 1.533;
    static final double NECK_BACK = 1.538;
    static final double V_DEPTH = 0.104;
    static final double V_HALF = 0.62;
    static final double JERSEY_V_TOP = 1.545;
    static final double JERSEY_V_BOTTOM = 0.965;
    static final double ARMPIT = 1.352;
    static final double SLEEVE_END = 0.235;
    static final double CUFF_WIDTH = 0.026;
    /** La camiseta cae recta desde el pecho (DRAPE_TOP) hasta la cadera (DRAPE_BOTTOM). */
    static final double DRAPE_TOP = 1.29;
    static final double DRAPE_BOTTOM = 1.02;

    // ---------------------------------------------------------------- Short
    static final double SHORTS_TOP = 0.962;
    static final double SHORTS_HEM = 0.615;
    static final double CROTCH = 0.825;
    /** Debajo de esta altura la "D" de la pelvis conserva su forma (no se angosta hacia la entrepierna). */
    static final double HIP_FREEZE = 0.905;
    static final double WAIST_BOTTOM = 0.948;
    static final double WAIST_TOP = 0.992;

    // ---------------------------------------------------------------- Medias
    static final double SOCK_TOP = 0.458;
    static final double SOCK_BAND_BOTTOM = 0.405;

    /** Superficie paramétrica (u, v) → punto en espacio de diseño. */
    @FunctionalInterface
    interface Surface {
        Vec3 at(double u, double v);
    }

    private GarmentGenerator() {
    }

    /** Normal (espacio de diseño) por diferencias centrales, orientada según {@code hint}. */
    static Vec3 normal(Surface s, double u, double v, double du, double dv, Vec3 hint) {
        Vec3 pu = s.at(u + du, v).sub(s.at(u - du, v));
        Vec3 pv = s.at(u, v + dv).sub(s.at(u, v - dv));
        Vec3 n = pu.cross(pv).normalize();
        if (n.length() == 0) {
            return hint.normalize();
        }
        return n.dot(hint) < 0 ? n.mul(-1) : n;
    }

    // ================================================================ Camiseta

    static double hemY(double phi) {
        return HEM_BACK + (HEM_FRONT - HEM_BACK) * (1 + Math.cos(phi)) / 2;
    }

    /** Escote en V adelante, cuello redondo atrás. */
    static double necklineY(double phi) {
        double base = NECK_SIDE + (NECK_BACK - NECK_SIDE) * (1 - Math.cos(phi)) / 2;
        double k = Math.max(0, 1 - Math.abs(angleDiff(phi, 0)) / V_HALF);
        return base - V_DEPTH * Math.pow(k, 1.18);
    }

    /** Superficie base de la camiseta: cae recta desde el pecho y descansa en la cadera. */
    static Vec3 jerseyBase(BodyShape b, double phi, double y) {
        double rb = b.torsoRadius(phi, y);
        double rChest = b.torsoRadius(phi, DRAPE_TOP) * 0.985;
        double k = 0.62 * smoothstep(DRAPE_TOP, DRAPE_BOTTOM, y);
        double r = Math.max(rb, rb + (rChest - rb) * k);
        return new Vec3(r * Math.sin(phi), y, b.torsoCenterZ(y) + r * Math.cos(phi));
    }

    /** Holgura de la camiseta (más suelta abajo para cubrir la pretina del short) más pliegues sutiles. */
    static double jerseyEase(double phi, double y) {
        double ease = 0.0065 + 0.0115 * smoothstep(1.10, 0.99, y) + 0.003 * smoothstep(1.47, 1.53, y);
        double hemFolds = (0.0030 * smoothstep(1.17, 0.99, y) + 0.0006)
                * (0.6 * Math.sin(5 * phi + 0.7) + 0.4 * Math.sin(11 * phi + 2.1));
        double sides = angularBell(phi, Math.PI / 2, 0.25) + angularBell(phi, -Math.PI / 2, 0.25);
        double waistWrinkles = 0.0012 * Math.sin(y * 110) * sides * bell(y, 1.12, 0.05);
        double backDrape = 0.0015 * Math.sin(7 * phi) * angularBell(phi, Math.PI, 0.9) * smoothstep(1.20, 1.0, y);
        return ease + hemFolds + waistWrinkles + backDrape;
    }

    static Vec3 jerseyPoint(BodyShape b, double phi, double y, double extra) {
        Surface base = (u, v) -> jerseyBase(b, u, v);
        Vec3 p = base.at(phi, y);
        Vec3 n = normal(base, phi, y, 0.002, 0.002, p.sub(b.torsoAxis(y)));
        return p.addScaled(n, jerseyEase(phi, y) + extra);
    }

    static MeshData jerseyBody(BodyShape b) {
        int rows = 130;
        int cols = 160; // costuras de las islas en φ = ±π/2 (columnas 40 y 120)
        GridSurface g = new GridSurface(rows, cols, true);
        double[] ys = new double[rows * cols];
        for (int c = 0; c < cols; c++) {
            double phi = 2 * Math.PI * c / cols;
            double hem = hemY(phi);
            double top = necklineY(phi);
            for (int r = 0; r < rows; r++) {
                double t = (double) r / (rows - 1);
                double y = hem + (top - hem) * (t * t * (3 - 2 * t) * 0.35 + t * 0.65);
                ys[r * cols + c] = y;
                Vec3 p = jerseyPoint(b, phi, y, 0);
                g.set(r, c, b.out(p), p.sub(b.torsoAxis(y)));
            }
        }
        int back0 = cols / 4;
        int back1 = 3 * cols / 4;
        return g.build(2,
                (r, c) -> c >= back0 && c < back1 ? 1 : 0,
                (r, c, island, out) -> {
                    double phi = 2 * Math.PI * c / cols;
                    double y = ys[r * cols + (c % cols)];
                    double v = (JERSEY_V_TOP - y) / (JERSEY_V_TOP - JERSEY_V_BOTTOM);
                    if (island == 0) {
                        double a = angleDiff(phi, 0); // [-π/2, π/2]
                        out[0] = UvLayout.JERSEY_FRONT.u(0.5 + a / Math.PI);
                        out[1] = UvLayout.JERSEY_FRONT.v(v);
                    } else {
                        out[0] = UvLayout.JERSEY_BACK.u((phi - Math.PI / 2) / Math.PI);
                        out[1] = UvLayout.JERSEY_BACK.v(v);
                    }
                });
    }

    /** Cuello en V: tira con borde enrollado que sigue el escote. */
    static MeshData collar(BodyShape b) {
        int cols = 240;
        double[][] rowsDef = {
                // {desplazamiento vertical respecto del escote, espesor extra}
                {-0.0155, 0.0022},
                {-0.0137, 0.0036},
                {-0.003, 0.0040},
                {0.0025, 0.0024},
                {0.0030, -0.0010},
                {0.0010, -0.0045},
        };
        GridSurface g = new GridSurface(rowsDef.length, cols, true);
        for (int c = 0; c < cols; c++) {
            double phi = 2 * Math.PI * c / cols;
            double top = necklineY(phi);
            for (int r = 0; r < rowsDef.length; r++) {
                // El ancho se mide sobre la tela (longitud de arco), no en altura: en la pendiente del
                // trapecio una tira medida en altura se ensancharía como un cuello de polo.
                double dy = rowsDef[r][0];
                double y = dy < 0 ? yBelowByArc(b, phi, top, -dy) : top + dy;
                Vec3 p = jerseyPoint(b, phi, y, rowsDef[r][1]);
                // Filas 0-2: cara exterior; 3: borde superior; 4-5: cara interior (mira hacia el cuello).
                Vec3 radial = p.sub(b.torsoAxis(y)).normalize();
                Vec3 hint = r <= 2 ? radial : r == 3 ? radial.add(new Vec3(0, 1.5, 0)) : radial.mul(-1).add(new Vec3(0, 0.3, 0));
                g.set(r, c, b.out(p), hint);
            }
        }
        UvRect rect = UvLayout.JERSEY_COLLAR;
        return g.build(1, (r, c) -> 0, (r, c, island, out) -> {
            out[0] = rect.u((double) c / cols);
            out[1] = rect.v((double) r / (rowsDef.length - 1));
        });
    }

    /** Altura a la que se llega bajando {@code dist} metros sobre la superficie de la camiseta desde {@code top}. */
    static double yBelowByArc(BodyShape b, double phi, double top, double dist) {
        Vec3 start = jerseyPoint(b, phi, top, 0);
        double lo = top - dist;
        double hi = top;
        for (int i = 0; i < 30; i++) {
            double mid = (lo + hi) / 2;
            if (jerseyPoint(b, phi, mid, 0).sub(start).length() > dist) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2;
    }

    /** Paneles laterales desde el ruedo hasta la axila, un poco por encima de la tela del cuerpo. */
    static MeshData sides(BodyShape b) {
        List<MeshData> parts = new ArrayList<>();
        for (Side side : Side.values()) {
            int rows = 80;
            int cols = 12;
            double center = side.sign * Math.PI / 2;
            GridSurface g = new GridSurface(rows, cols, false);
            for (int r = 0; r < rows; r++) {
                double t = (double) r / (rows - 1);
                for (int c = 0; c < cols; c++) {
                    double half = 0.105 * (1 - 0.55 * smoothstep(0.82, 1.0, t));
                    double phi = center + side.sign * half * (2.0 * c / (cols - 1) - 1);
                    double y = hemY(phi) + 0.002 + (ARMPIT - hemY(phi)) * t;
                    Vec3 p = jerseyPoint(b, phi, y, 0.0018);
                    g.set(r, c, b.out(p), p.sub(b.torsoAxis(y)));
                }
            }
            UvRect rect = side == Side.LEFT ? UvLayout.JERSEY_SIDE_LEFT : UvLayout.JERSEY_SIDE_RIGHT;
            parts.add(g.build(1, (r, c) -> 0, (r, c, island, out) -> {
                out[0] = rect.u((double) c / (cols - 1));
                out[1] = rect.v(1 - (double) r / (rows - 1));
            }));
        }
        return MeshData.merge(parts);
    }

    static double sleeveEase(double f, double angle, int sign) {
        double flare = 0.0085 + 0.0100 * smoothstep(0.06, SLEEVE_END, f);
        double folds = 0.0018 * Math.sin(4 * angle + sign * 0.9) * smoothstep(0.10, SLEEVE_END, f);
        double drape = 0.0025 * Math.max(0, -Math.cos(angle)) * smoothstep(0.12, SLEEVE_END, f);
        return flare + folds + drape;
    }

    static MeshData sleeves(BodyShape b) {
        List<MeshData> parts = new ArrayList<>();
        for (Side side : Side.values()) {
            Tube arm = b.arm(side);
            int rows = 70;
            int cols = 64;
            GridSurface g = new GridSurface(rows, cols, true);
            for (int r = 0; r < rows; r++) {
                double f = SLEEVE_END * r / (rows - 1);
                Vec3 center = arm.center(f);
                for (int c = 0; c < cols; c++) {
                    double angle = 2 * Math.PI * c / cols;
                    Vec3 p = arm.point(f, angle, sleeveEase(f, angle, side.sign));
                    g.set(r, c, b.out(p), p.sub(center));
                }
            }
            UvRect rect = side == Side.LEFT ? UvLayout.JERSEY_SLEEVE_LEFT : UvLayout.JERSEY_SLEEVE_RIGHT;
            parts.add(g.build(1, (r, c) -> 0, (r, c, island, out) -> {
                out[0] = rect.u((double) c / cols);
                out[1] = rect.v((double) r / (rows - 1));
            }));
        }
        return MeshData.merge(parts);
    }

    static MeshData cuffs(BodyShape b) {
        List<MeshData> parts = new ArrayList<>();
        double f1 = SLEEVE_END + 0.002;
        double f0 = f1 - CUFF_WIDTH / b.arm(Side.LEFT).length();
        double[][] rowsDef = {
                // {fracción 0..1 dentro del puño, espesor extra sobre la manga}
                {0.00, 0.0006}, {0.06, 0.0034}, {0.50, 0.0040}, {0.94, 0.0034}, {1.00, 0.0006}, {0.985, -0.0030},
        };
        for (Side side : Side.values()) {
            Tube arm = b.arm(side);
            int cols = 64;
            GridSurface g = new GridSurface(rowsDef.length, cols, true);
            for (int r = 0; r < rowsDef.length; r++) {
                double f = f0 + (f1 - f0) * rowsDef[r][0];
                Vec3 center = arm.center(f);
                for (int c = 0; c < cols; c++) {
                    double angle = 2 * Math.PI * c / cols;
                    double e = sleeveEase(Math.min(f, SLEEVE_END), angle, side.sign) + rowsDef[r][1];
                    Vec3 p = arm.point(f, angle, e);
                    Vec3 radial = p.sub(center).normalize();
                    // La última fila es el labio interior del puño: su cara mira hacia el brazo.
                    Vec3 hint = r == rowsDef.length - 1 ? radial.mul(-1).add(arm.tangent(f).mul(0.3))
                            : r == rowsDef.length - 2 ? radial.add(arm.tangent(f).mul(1.5)) : radial;
                    g.set(r, c, b.out(p), hint);
                }
            }
            UvRect rect = side == Side.LEFT ? UvLayout.JERSEY_CUFF_LEFT : UvLayout.JERSEY_CUFF_RIGHT;
            parts.add(g.build(1, (r, c) -> 0, (r, c, island, out) -> {
                out[0] = rect.u((double) c / cols);
                out[1] = rect.v((double) r / (rowsDef.length - 1));
            }));
        }
        return MeshData.merge(parts);
    }

    // ================================================================ Short

    static double shortsEase(double y) {
        return 0.013 + 0.009 * smoothstep(0.96, CROTCH, y) + 0.011 * smoothstep(CROTCH, SHORTS_HEM, y);
    }

    /**
     * Punto del short para una pierna. {@code q} recorre la sección: [0, 0.5] = de adelante hacia atrás por el
     * lado exterior; [0.5, 1) = de atrás hacia adelante por el lado interior. Arriba de la entrepierna la sección
     * es media pelvis (una "D"); abajo, un óvalo alrededor del muslo; en medio se mezclan suavemente.
     */
    static Vec3 shortsPoint(BodyShape b, Side side, double q, double y, double extra) {
        double ease = shortsEase(y) + extra;
        double folds = 0.0028 * Math.sin(2 * Math.PI * q * 7 + side.sign * 1.3) * smoothstep(CROTCH, SHORTS_HEM, y);
        double yd = Math.max(y, HIP_FREEZE);
        Vec3 d;
        if (q <= 0.5) {
            d = b.torsoPoint(side.sign * 2 * Math.PI * q, yd, ease);
        } else {
            Vec3 back = b.torsoPoint(side.sign * Math.PI, yd, ease);
            Vec3 front = b.torsoPoint(0, yd, ease);
            d = back.lerp(front, (q - 0.5) / 0.5);
        }
        d = new Vec3(d.x(), y, d.z());
        double blend = smoothstep(CROTCH + 0.03, CROTCH - 0.07, y);
        Tube leg = b.leg(side);
        double f = b.legFractionAt(side, y);
        Vec3 p = d;
        if (blend > 0) {
            Vec3 o = leg.point(f, 2 * Math.PI * q, ease + folds);
            // El óvalo de la pierna se toma a la misma altura para que las filas queden horizontales.
            p = d.lerp(new Vec3(o.x(), y, o.z()), blend);
        }
        return outsideLeg(leg, f, p, ease);
    }

    /**
     * Garantiza que el punto quede por fuera del muslo (más la holgura). Mezclar la "D" de la cadera con el
     * óvalo de la pierna no conserva la correspondencia de ángulos, y sin esto el muslo atraviesa la tela.
     */
    static Vec3 outsideLeg(Tube leg, double f, Vec3 p, double ease) {
        Vec3 c = leg.center(f);
        Vec3 d = new Vec3(p.x() - c.x(), 0, p.z() - c.z());
        double dist = d.length();
        if (dist < 1e-9) {
            return p;
        }
        double angle = Math.atan2(d.dot(leg.sideAxis(f)), d.dot(leg.frontAxis(f)));
        Vec3 surface = leg.point(f, angle, ease);
        double need = new Vec3(surface.x() - c.x(), 0, surface.z() - c.z()).length();
        if (dist >= need) {
            return p;
        }
        Vec3 pushed = c.addScaled(d.normalize(), need);
        return new Vec3(pushed.x(), p.y(), pushed.z());
    }

    static MeshData shorts(BodyShape b) {
        List<MeshData> parts = new ArrayList<>();
        for (Side side : Side.values()) {
            int rows = 100;
            int cols = 96; // costuras de islas en q = 0.25 (exterior) y 0.75 (interior)
            GridSurface g = new GridSurface(rows, cols, true);
            for (int r = 0; r < rows; r++) {
                double t = (double) r / (rows - 1);
                double y = SHORTS_TOP + (SHORTS_HEM - SHORTS_TOP) * t;
                for (int c = 0; c < cols; c++) {
                    double q = (double) c / cols;
                    Vec3 p = shortsPoint(b, side, q, y, 0);
                    Vec3 axis = y > CROTCH ? b.torsoAxis(y).add(new Vec3(side.sign * 0.05, 0, 0))
                            : b.leg(side).center(b.legFractionAt(side, y));
                    g.set(r, c, b.out(p), p.sub(new Vec3(axis.x(), y, axis.z())));
                }
            }
            UvRect front = side == Side.LEFT ? UvLayout.SHORTS_LEFT_FRONT : UvLayout.SHORTS_RIGHT_FRONT;
            UvRect back = side == Side.LEFT ? UvLayout.SHORTS_LEFT_BACK : UvLayout.SHORTS_RIGHT_BACK;
            int q25 = cols / 4;
            int q75 = 3 * cols / 4;
            parts.add(g.build(2,
                    (r, c) -> c >= q25 && c < q75 ? 1 : 0,
                    (r, c, island, out) -> {
                        double psi = 2 * Math.PI * c / cols;
                        double v = (double) r / (rows - 1);
                        if (island == 0) {
                            double a = angleDiff(psi, 0);
                            out[0] = front.u(0.5 + side.sign * a / Math.PI);
                            out[1] = front.v(v);
                        } else {
                            out[0] = back.u(0.5 + side.sign * (psi - Math.PI) / Math.PI);
                            out[1] = back.v(v);
                        }
                    }));
        }
        return MeshData.merge(parts);
    }

    static MeshData waistband(BodyShape b) {
        double[][] rowsDef = {
                {WAIST_BOTTOM, 0.0145}, {WAIST_BOTTOM + 0.002, 0.0168}, {WAIST_TOP - 0.003, 0.0168},
                {WAIST_TOP, 0.0150}, {WAIST_TOP - 0.001, 0.0110},
        };
        int cols = 160;
        GridSurface g = new GridSurface(rowsDef.length, cols, true);
        for (int r = 0; r < rowsDef.length; r++) {
            double y = rowsDef[r][0];
            for (int c = 0; c < cols; c++) {
                double phi = 2 * Math.PI * c / cols;
                Vec3 p = b.torsoPoint(phi, y, rowsDef[r][1]);
                Vec3 radial = p.sub(b.torsoAxis(y)).normalize();
                Vec3 hint = r == rowsDef.length - 1 ? radial.mul(-1).add(new Vec3(0, 0.3, 0))
                        : r == rowsDef.length - 2 ? radial.add(new Vec3(0, 1.5, 0)) : radial;
                g.set(r, c, b.out(p), hint);
            }
        }
        UvRect rect = UvLayout.SHORTS_WAIST;
        return g.build(1, (r, c) -> 0, (r, c, island, out) -> {
            out[0] = rect.u((double) c / cols);
            out[1] = rect.v((double) r / (rowsDef.length - 1));
        });
    }

    static MeshData stripes(BodyShape b) {
        List<MeshData> parts = new ArrayList<>();
        for (Side side : Side.values()) {
            int rows = 90;
            int cols = 8;
            double halfWidth = 0.019;
            GridSurface g = new GridSurface(rows, cols, false);
            for (int r = 0; r < rows; r++) {
                double y = WAIST_BOTTOM - 0.002 + (SHORTS_HEM + 0.003 - WAIST_BOTTOM + 0.002) * r / (rows - 1);
                for (int c = 0; c < cols; c++) {
                    double q = 0.25 + halfWidth * (2.0 * c / (cols - 1) - 1);
                    Vec3 p = shortsPoint(b, side, q, y, 0.0026);
                    Vec3 axis = y > CROTCH ? b.torsoAxis(y) : b.leg(side).center(b.legFractionAt(side, y));
                    g.set(r, c, b.out(p), p.sub(new Vec3(axis.x(), y, axis.z())));
                }
            }
            UvRect rect = side == Side.LEFT ? UvLayout.SHORTS_STRIPE_LEFT : UvLayout.SHORTS_STRIPE_RIGHT;
            parts.add(g.build(1, (r, c) -> 0, (r, c, island, out) -> {
                out[0] = rect.u((double) r / (rows - 1));
                out[1] = rect.v((double) c / (cols - 1));
            }));
        }
        return MeshData.merge(parts);
    }

    // ================================================================ Medias

    static MeshData socks(BodyShape b) {
        List<MeshData> parts = new ArrayList<>();
        for (Side side : Side.values()) {
            Tube leg = b.leg(side);
            double f0 = b.legFractionAt(side, SOCK_BAND_BOTTOM + 0.004);
            int rows = 90;
            int cols = 56;
            GridSurface g = new GridSurface(rows, cols, true);
            for (int r = 0; r < rows; r++) {
                double f = f0 + (1 - f0) * r / (rows - 1);
                Vec3 center = leg.center(f);
                for (int c = 0; c < cols; c++) {
                    double angle = 2 * Math.PI * c / cols;
                    double ribs = 0.00045 * Math.cos(angle * 28);
                    Vec3 p = leg.point(f, angle, 0.0035 + ribs);
                    g.set(r, c, b.out(p), p.sub(center));
                }
            }
            UvRect rect = side == Side.LEFT ? UvLayout.SOCK_LEFT : UvLayout.SOCK_RIGHT;
            parts.add(g.build(1, (r, c) -> 0, (r, c, island, out) -> {
                out[0] = rect.u((double) c / cols);
                out[1] = rect.v((double) r / (rows - 1));
            }));
        }
        return MeshData.merge(parts);
    }

    static MeshData sockBands(BodyShape b) {
        List<MeshData> parts = new ArrayList<>();
        double[][] rowsDef = {
                // {y, espesor}
                {SOCK_BAND_BOTTOM, 0.0038}, {SOCK_BAND_BOTTOM + 0.002, 0.0056}, {SOCK_TOP - 0.004, 0.0060},
                {SOCK_TOP, 0.0050}, {SOCK_TOP - 0.0015, 0.0012},
        };
        for (Side side : Side.values()) {
            Tube leg = b.leg(side);
            int cols = 56;
            GridSurface g = new GridSurface(rowsDef.length, cols, true);
            for (int r = 0; r < rowsDef.length; r++) {
                double f = b.legFractionAt(side, rowsDef[r][0]);
                Vec3 center = leg.center(f);
                for (int c = 0; c < cols; c++) {
                    double angle = 2 * Math.PI * c / cols;
                    double ribs = 0.0004 * Math.cos(angle * 28);
                    Vec3 p = leg.point(f, angle, rowsDef[r][1] + ribs);
                    Vec3 radial = p.sub(center).normalize();
                    Vec3 hint = r == rowsDef.length - 1 ? radial.mul(-1).add(new Vec3(0, 0.3, 0))
                            : r == rowsDef.length - 2 ? radial.add(new Vec3(0, 1.5, 0)) : radial;
                    g.set(r, c, b.out(p), hint);
                }
            }
            UvRect rect = side == Side.LEFT ? UvLayout.SOCK_BAND_LEFT : UvLayout.SOCK_BAND_RIGHT;
            parts.add(g.build(1, (r, c) -> 0, (r, c, island, out) -> {
                out[0] = rect.u((double) c / cols);
                out[1] = rect.v((double) r / (rowsDef.length - 1));
            }));
        }
        return MeshData.merge(parts);
    }
}
