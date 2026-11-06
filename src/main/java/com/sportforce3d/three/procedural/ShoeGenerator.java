package com.sportforce3d.three.procedural;

import com.sportforce3d.three.geom.Curve;
import com.sportforce3d.three.geom.GridSurface;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Tube;
import com.sportforce3d.three.geom.UvRect;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.UvLayout;
import com.sportforce3d.three.model.Zone;
import com.sportforce3d.three.procedural.BodyShape.Side;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.sportforce3d.three.geom.Curve.smoothstep;

/**
 * Chimpunes de fútbol: capellada, puntera, contrafuerte con collarín, lengüeta, cordones, suela y 10 tacos.
 * Coordenadas locales del pie: {@code t} de 0 (talón) a 1 (punta) a lo largo, {@code x} lateral, {@code y}
 * altura sobre el piso. Sin marcas ni franjas de terceros.
 */
final class ShoeGenerator {

    static final double LENGTH = 0.285;
    static final double HEEL_BACK = -0.072;
    static final double SOLE_BOTTOM = 0.0125;
    static final double SOLE_TOP = 0.027;
    static final double TOE_OUT_DEG = 6;

    /** Medio ancho de la capellada a lo largo del pie. */
    static final Curve WIDTH = Curve.of(0, .000, .012, .022, .035, .034, .09, .041, .22, .044, .42, .045,
            .58, .048, .71, .0500, .83, .0465, .92, .0375, .965, .026, .99, .013, 1.0, .000);
    /** Alto de la capellada sobre la suela. */
    static final Curve HEIGHT = Curve.of(0, .070, .03, .076, .10, .080, .20, .080, .30, .078, .42, .068,
            .55, .055, .68, .044, .80, .036, .90, .029, 1.0, .018);
    /** Medio ancho de la suela (sobresale un poco). */
    static final Curve SOLE_WIDTH = Curve.of(-.022, .000, -.010, .022, .010, .036, .06, .043, .22, .048, .42, .049,
            .58, .052, .71, .0545, .83, .0510, .92, .0420, .97, .0300, 1.0, .018, 1.022, .000);

    private ShoeGenerator() {
    }

    /** Elevación de la punta (toe spring). */
    static double toeSpring(double t) {
        return 0.013 * Math.pow(smoothstep(0.74, 1.04, t), 1.4);
    }

    /** Marco del pie: origen en el piso bajo el tobillo, punta un poco hacia afuera. */
    record Foot(Vec3 origin, Vec3 forward, Vec3 lateral, int sign) {

        Vec3 at(double t, double x, double y) {
            double z = HEEL_BACK + t * LENGTH;
            return origin.addScaled(forward, z).addScaled(lateral, x).add(new Vec3(0, y, 0));
        }
    }

    static Foot foot(BodyShape b, Side side) {
        Vec3 ankle = b.ankle(side);
        double a = Math.toRadians(TOE_OUT_DEG);
        Vec3 forward = new Vec3(side.sign * Math.sin(a), 0, Math.cos(a));
        Vec3 lateral = new Vec3(Math.cos(a) * side.sign, 0, -Math.sin(a));
        return new Foot(new Vec3(ankle.x(), 0, ankle.z()), forward, lateral, side.sign);
    }

    /** Punto de la sección de la capellada; {@code a} ∈ [0, 2π), arriba en π/2. */
    static Vec3 upperPoint(Foot foot, double t, double a, double extra, double heightCap) {
        // Alrededor de la abertura del tobillo el tope es plano y más cuadrado: la pierna lo corta en un
        // óvalo limpio que tapa el collarín. Hacia el empeine vuelve a ser redondeado.
        double opening = 1 - smoothstep(0.30, 0.46, t);
        double w = WIDTH.at(t) + extra;
        double h = Math.min(HEIGHT.at(t), OPENING_HEIGHT + 0.004 * (1 - opening)) + extra;
        double c = Math.cos(a);
        double s = Math.sin(a);
        double topExp = 2.0 / (2.25 + 2.6 * opening);
        double sideExp = 2.0 / (2.4 + 0.8 * opening);
        double x = w * Math.signum(c) * Math.pow(Math.abs(c), sideExp);
        double y = s >= 0 ? Math.min(h * Math.pow(s, topExp), heightCap) : -0.003 * Math.abs(s);
        return foot.at(t, x, SOLE_TOP + y + toeSpring(t));
    }

    /** Altura del tope plano alrededor del tobillo (sobre la suela); el collarín se apoya ahí. */
    static final double OPENING_HEIGHT = 0.072;

    static Map<Zone, MeshData> build(BodyShape b) {
        Map<Zone, List<MeshData>> parts = new EnumMap<>(Zone.class);
        for (Side side : Side.values()) {
            Foot foot = foot(b, side);
            add(parts, Zone.SHOE_UPPER, upper(b, foot));
            add(parts, Zone.SHOE_UPPER, tongue(b, foot));
            add(parts, Zone.SHOE_TOE, overlay(b, foot, 0.755, 1.0, 0.0018, 9, UvLayout.SHOE_TOE));
            add(parts, Zone.SHOE_HEEL, overlay(b, foot, 0.0, 0.245, 0.0018, 0.058, UvLayout.SHOE_HEEL));
            add(parts, Zone.SHOE_UPPER, collar(b, foot, side));
            add(parts, Zone.SHOE_LACES, laces(b, foot));
            add(parts, Zone.SHOE_SOLE, sole(b, foot));
            add(parts, Zone.SHOE_SOLE, studs(b, foot));
        }
        Map<Zone, MeshData> merged = new EnumMap<>(Zone.class);
        parts.forEach((zone, list) -> merged.put(zone, MeshData.merge(list)));
        return merged;
    }

    private static void add(Map<Zone, List<MeshData>> map, Zone zone, MeshData mesh) {
        map.computeIfAbsent(zone, z -> new ArrayList<>()).add(mesh);
    }

    private static Vec3 axisPoint(Foot foot, double t) {
        return foot.at(t, 0, SOLE_TOP + HEIGHT.at(t) * 0.35 + toeSpring(t));
    }

    private static MeshData upper(BodyShape b, Foot foot) {
        int rows = 130;
        int cols = 72;
        GridSurface g = new GridSurface(rows, cols, true);
        for (int r = 0; r < rows; r++) {
            double u = (double) r / (rows - 1);
            double t = 0.5 - 0.5 * Math.cos(Math.PI * u); // más filas en talón y punta
            for (int c = 0; c < cols; c++) {
                double a = 2 * Math.PI * c / cols;
                Vec3 p = upperPoint(foot, t, a, 0, 1);
                Vec3 hint = p.sub(axisPoint(foot, t)).add(foot.forward().mul(t < 0.02 ? -0.05 : t > 0.98 ? 0.05 : 0));
                g.set(r, c, b.out(p), hint);
            }
        }
        UvRect rect = UvLayout.SHOE_UPPER;
        return g.build(1, (r, c) -> 0, (r, c, island, out) -> {
            out[0] = rect.u((double) r / (rows - 1));
            out[1] = rect.v((double) c / cols);
        });
    }

    /** Puntera o contrafuerte: un parche de la parte superior de la capellada, un poco por encima. */
    private static MeshData overlay(BodyShape b, Foot foot, double t0, double t1, double extra, double heightCap,
                                    UvRect rect) {
        int rows = 46;
        int cols = 44;
        double a0 = -0.20;
        double a1 = Math.PI + 0.20;
        GridSurface g = new GridSurface(rows, cols, false);
        for (int r = 0; r < rows; r++) {
            double t = t0 + (t1 - t0) * r / (rows - 1);
            for (int c = 0; c < cols; c++) {
                double a = a0 + (a1 - a0) * c / (cols - 1);
                double cap = heightCap * (t0 == 0 ? 1 - 0.35 * smoothstep(0.14, 0.245, t) : 1);
                Vec3 p = upperPoint(foot, t, a, extra, cap);
                Vec3 hint = p.sub(axisPoint(foot, t)).add(foot.forward().mul(t < 0.02 ? -0.05 : t > 0.98 ? 0.05 : 0));
                g.set(r, c, b.out(p), hint);
            }
        }
        return g.build(1, (r, c) -> 0, (r, c, island, out) -> {
            out[0] = rect.u((double) r / (rows - 1));
            out[1] = rect.v((double) c / (cols - 1));
        });
    }

    /** Altura de diseño donde la pierna entra en la capellada (borde del collarín). */
    static final double COLLAR_Y = SOLE_TOP + OPENING_HEIGHT;

    /**
     * Collarín acolchado: un anillo alrededor de la pierna justo donde entra en la capellada. Tapa la línea de
     * intersección entre la media y la zapatilla, que de otro modo se vería dentada.
     */
    private static MeshData collar(BodyShape b, Foot foot, Side side) {
        Tube leg = b.leg(side);
        double f = 1.0;
        Vec3 c0 = leg.center(f);
        Vec3 center = new Vec3(c0.x(), COLLAR_Y, c0.z());
        Vec3 sideAxis = leg.sideAxis(f);
        Vec3 frontAxis = leg.frontAxis(f);
        double rx = leg.radiusSide(f) + 0.0085;
        double rz = leg.radiusFront(f) + 0.0085;
        int ring = 80;
        int section = 16;
        double tube = 0.0068;
        GridSurface g = new GridSurface(section + 1, ring, true);
        for (int r = 0; r <= section; r++) {
            double s = 2 * Math.PI * r / section;
            for (int c = 0; c < ring; c++) {
                double a = 2 * Math.PI * c / ring;
                Vec3 radial = sideAxis.mul(Math.sin(a) * rx).add(frontAxis.mul(Math.cos(a) * rz));
                Vec3 ringPoint = center.add(radial);
                Vec3 out = new Vec3(radial.x(), 0, radial.z()).normalize();
                Vec3 p = ringPoint.addScaled(out, Math.cos(s) * tube).add(new Vec3(0, Math.sin(s) * tube * 0.9, 0));
                g.set(r, c, b.out(p), p.sub(ringPoint));
            }
        }
        UvRect rect = UvLayout.SHOE_UPPER;
        return g.build(1, (r, c) -> 0, (r, c, island, out) -> {
            out[0] = rect.u(0.10 + 0.20 * c / ring);
            out[1] = rect.v(0.90 + 0.10 * r / section);
        });
    }

    /** Lengüeta que asoma sobre el empeine, delante del tobillo. */
    private static MeshData tongue(BodyShape b, Foot foot) {
        int rows = 12;
        int cols = 14;
        GridSurface g = new GridSurface(rows, cols, false);
        for (int r = 0; r < rows; r++) {
            double v = (double) r / (rows - 1);
            double t = 0.315 - 0.055 * v;
            for (int c = 0; c < cols; c++) {
                double x = 0.021 * (2.0 * c / (cols - 1) - 1);
                double curve = 1 - Math.pow(Math.abs(x) / 0.024, 2) * 0.35;
                double y = SOLE_TOP + HEIGHT.at(0.315) * 0.92 + 0.030 * v * curve + toeSpring(t);
                Vec3 p = foot.at(t, x, y);
                g.set(r, c, b.out(p), foot.forward().add(new Vec3(0, 0.8, 0)));
            }
        }
        UvRect rect = UvLayout.SHOE_UPPER;
        return g.build(1, (r, c) -> 0, (r, c, island, out) -> {
            out[0] = rect.u(0.30 + 0.06 * c / (cols - 1));
            out[1] = rect.v(0.20 * r / (rows - 1));
        });
    }

    /** Cinco cordones cruzados en diagonal sobre el empeine. */
    private static MeshData laces(BodyShape b, Foot foot) {
        List<MeshData> list = new ArrayList<>();
        double[] ts = {0.345, 0.395, 0.445, 0.495, 0.545};
        for (int i = 0; i < ts.length; i++) {
            double skew = (i % 2 == 0 ? 1 : -1) * 0.012;
            List<Vec3> path = new ArrayList<>();
            int samples = 9;
            for (int k = 0; k < samples; k++) {
                double a = Math.PI / 2 + 0.40 * (2.0 * k / (samples - 1) - 1);
                double tt = ts[i] + skew * (2.0 * k / (samples - 1) - 1);
                path.add(upperPoint(foot, tt, a, 0.0022, 1));
            }
            Tube lace = new Tube(path, Curve.of(0, .0024, 1, .0024), Curve.of(0, .0031, 1, .0031),
                    Curve.of(0, 0, 1, 0), new Vec3(0, 1, 0));
            int rows = 22;
            int cols = 12;
            GridSurface g = new GridSurface(rows, cols, true);
            for (int r = 0; r < rows; r++) {
                double f = (double) r / (rows - 1);
                double endTaper = 1 - 0.6 * Math.pow(Math.abs(2 * f - 1), 8);
                for (int c = 0; c < cols; c++) {
                    double ang = 2 * Math.PI * c / cols;
                    // Cordón plano: ancho sobre la capellada, delgado hacia arriba.
                    Vec3 up = lace.sideAxis(f);
                    Vec3 across = lace.frontAxis(f);
                    Vec3 p = lace.center(f).addScaled(across, 0.0034 * endTaper * Math.sin(ang))
                            .addScaled(up, 0.0016 * endTaper * Math.cos(ang));
                    g.set(r, c, b.out(p), p.sub(lace.center(f)));
                }
            }
            UvRect rect = UvLayout.SHOE_LACES;
            final int idx = i;
            list.add(g.build(1, (r, c) -> 0, (r, c, island, out) -> {
                out[0] = rect.u((idx + (double) r / (rows - 1)) / ts.length);
                out[1] = rect.v((double) c / cols);
            }));
        }
        return MeshData.merge(list);
    }

    /** Suela: perfil de rectángulo redondeado (superelipse n = 6) con la punta levantada. */
    private static MeshData sole(BodyShape b, Foot foot) {
        int rows = 120;
        int cols = 64;
        double t0 = SOLE_WIDTH.minX();
        double t1 = SOLE_WIDTH.maxX();
        GridSurface g = new GridSurface(rows, cols, true);
        for (int r = 0; r < rows; r++) {
            double u = (double) r / (rows - 1);
            double t = t0 + (t1 - t0) * (0.5 - 0.5 * Math.cos(Math.PI * u));
            double w = SOLE_WIDTH.at(t);
            double mid = (SOLE_BOTTOM + SOLE_TOP + 0.002) / 2;
            double half = (SOLE_TOP + 0.002 - SOLE_BOTTOM) / 2;
            double lift = toeSpring(Math.min(1, Math.max(0, t)));
            for (int c = 0; c < cols; c++) {
                double a = 2 * Math.PI * c / cols;
                double co = Math.cos(a);
                double s = Math.sin(a);
                double x = w * Math.signum(co) * Math.pow(Math.abs(co), 2.0 / 6);
                double y = mid + half * Math.signum(s) * Math.pow(Math.abs(s), 2.0 / 6) + lift;
                Vec3 p = foot.at(t, x, y);
                Vec3 hint = p.sub(foot.at(t, 0, mid + lift)).add(foot.forward().mul(t < 0 ? -0.05 : t > 1 ? 0.05 : 0));
                g.set(r, c, b.out(p), hint);
            }
        }
        UvRect rect = UvLayout.SHOE_SOLE;
        return g.build(1, (r, c) -> 0, (r, c, island, out) -> {
            out[0] = rect.u((double) r / (rows - 1));
            out[1] = rect.v((double) c / cols);
        });
    }

    /** Tacos cónicos (FG): 6 en la parte delantera y 4 en el talón. */
    private static MeshData studs(BodyShape b, Foot foot) {
        double[][] positions = {
                {0.585, 0.60}, {0.585, -0.60}, {0.745, 0.62}, {0.745, -0.62}, {0.885, 0.36}, {0.885, -0.36},
                {0.085, 0.52}, {0.085, -0.52}, {0.225, 0.57}, {0.225, -0.57},
        };
        List<MeshData> list = new ArrayList<>();
        for (double[] pos : positions) {
            double t = pos[0];
            double x = SOLE_WIDTH.at(t) * pos[1];
            double top = SOLE_BOTTOM + toeSpring(t) + 0.001;
            double height = top - 0.0004;
            int rows = 8;
            int cols = 20;
            GridSurface g = new GridSurface(rows, cols, true);
            for (int r = 0; r < rows; r++) {
                double v = (double) r / (rows - 1);
                // Cono truncado con la base redondeada y la punta cerrada.
                double radius = r == rows - 1 ? 0.0002 : 0.0068 - 0.0026 * v - (r == rows - 2 ? 0.0012 : 0);
                double y = top - height * Math.min(1, v * 1.0);
                for (int c = 0; c < cols; c++) {
                    double a = 2 * Math.PI * c / cols;
                    Vec3 center = foot.at(t, x, y);
                    Vec3 p = center.addScaled(foot.lateral(), radius * Math.sin(a)).addScaled(foot.forward(), radius * Math.cos(a));
                    Vec3 hint = p.sub(center).add(new Vec3(0, r >= rows - 2 ? -0.004 : 0, 0));
                    g.set(r, c, b.out(p), hint);
                }
            }
            UvRect rect = UvLayout.SHOE_SOLE;
            list.add(g.build(1, (r, c) -> 0, (r, c, island, out) -> {
                out[0] = rect.u(0.9 + 0.1 * c / cols);
                out[1] = rect.v((double) r / (rows - 1));
            }));
        }
        return MeshData.merge(list);
    }
}
