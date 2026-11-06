package com.sportforce3d.three.human;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.model.ModelData.Focus;
import com.sportforce3d.three.model.ModelData.Part;
import com.sportforce3d.three.model.Zone;
import com.sportforce3d.three.procedural.BodyShape;
import com.sportforce3d.three.procedural.ProceduralModelProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Viste a una persona con el uniforme del maniquí procedural: las mismas mallas, con sus islas UV, cuello, puños y
 * ruedos, se trasladan al cuerpo de la persona. Así el compositor de texturas, el relieve de tela y el arrastre de
 * elementos funcionan igual que en el maniquí.
 *
 * <p>Cada vértice se ubica respecto del eje de su parte (torso, pierna o brazo) en el maniquí: altura (o posición a
 * lo largo del brazo), ángulo y holgura sobre la piel, medida con un rayo desde el eje. En la persona se busca el
 * mismo punto del eje (alturas proporcionales entre entrepierna, axila y cuello), se lanza el rayo con el mismo
 * ángulo y se suma la misma holgura. Después se suaviza el desplazamiento (quita picos donde un rayo tocó otra
 * parte), se empuja hacia afuera lo que quede a menos de unos milímetros de la piel y se recalculan las normales.
 * Las zapatillas se escalan y giran como un sólido sobre cada pie, y los pies y la punta de las medias (que quedan
 * dentro del calzado) se ocultan para que no atraviesen la zapatilla.
 */
public final class OutfitFitter {

    /** Persona (con los pies ocultos dentro del calzado) y uniforme ajustado a ella. */
    public record DressedPerson(HumanModel body, ModelData outfit) {
    }

    private static final double CELL = 0.02;
    private static final double MIN_EASE = 0.002;
    private static final double MAX_EASE = 0.045;
    /** Distancia mínima entre la tela y la piel después de ajustar. */
    private static final double GAP = 0.004;
    private static final double SOCK_GAP = 0.0025;
    private static final int SMOOTH_PASSES = 4;

    private enum Region { TORSO, LEG, ARM, BLEND, SHOE }

    private final BodyFrame from;
    private final BodyFrame to;
    private final TriangleIndex mannequin;
    private final TriangleIndex skin;
    private final double ratio;
    private final double blendTop;
    private final double blendBottom;
    private final ThreadLocal<TriangleIndex.Query> mannequinQ;
    private final ThreadLocal<TriangleIndex.Query> skinQ;

    private OutfitFitter(BodyFrame from, BodyFrame to, TriangleIndex mannequin, TriangleIndex skin, BodyShape shape) {
        this.from = from;
        this.to = to;
        this.mannequin = mannequin;
        this.skin = skin;
        this.ratio = to.height() / from.height();
        double scale = shape.height() / 1.812;
        blendTop = (ProceduralModelProvider.SHORTS_CROTCH + 0.03) * scale;
        blendBottom = (ProceduralModelProvider.SHORTS_CROTCH - 0.07) * scale;
        mannequinQ = ThreadLocal.withInitial(this.mannequin::query);
        skinQ = ThreadLocal.withInitial(this.skin::query);
    }

    /** Ajusta el uniforme de {@code source} (modelo procedural) a la persona. Lento (~1–3 s): segundo plano. */
    public static DressedPerson fit(HumanModel human, ModelData source) {
        BodyShape shape = BodyShape.of(source.bodyType());
        HumanModel.Part skinPart = skinOf(human);
        TriangleIndex skin = TriangleIndex.of(List.of(skinPart.mesh()), CELL);
        TriangleIndex mannequin = TriangleIndex.of(source.mannequin().stream().map(Part::mesh).toList(), CELL);
        BodyFrame from = BodyFrame.of(shape, mannequin.vertices());
        BodyFrame to = BodyFrame.measure(skin.vertices(), human.minY(), human.height());
        return new OutfitFitter(from, to, mannequin, skin, shape).dress(human, skinPart, source);
    }

    /** Diagnóstico temporal. */
    static String debug(HumanModel human, ModelData source, Vec3 p) {
        BodyShape shape = BodyShape.of(source.bodyType());
        TriangleIndex skin = TriangleIndex.of(List.of(skinOf(human).mesh()), CELL);
        TriangleIndex mannequin = TriangleIndex.of(source.mannequin().stream().map(Part::mesh).toList(), CELL);
        OutfitFitter f = new OutfitFitter(BodyFrame.of(shape, mannequin.vertices()),
                BodyFrame.measure(skin.vertices(), skin.triangles(), human.minY(), human.height()), mannequin, skin, shape);
        Vec3 c = f.from.torsoCenter(p.y());
        Vec3 h = new Vec3(p.x() - c.x(), 0, p.z() - c.z());
        Vec3 dir = h.normalize();
        double rFrom = f.mannequinQ.get().ray(c, dir, 0.6);
        Vec3 ct = f.to.torsoCenter(f.mapY(p.y()));
        double rTo = f.skinQ.get().ray(ct, dir, 0.6);
        return String.format("p=%s c=%s d=%.3f rFrom=%.3f yTo=%.3f cTo=%s rTo=%.3f placed=%s", p, c, h.length(), rFrom,
                f.mapY(p.y()), ct, rTo, f.transfer(p, Region.BLEND));
    }

    /** La piel: la parte opaca con más caras. */
    static HumanModel.Part skinOf(HumanModel human) {
        HumanModel.Part best = null;
        for (HumanModel.Part p : human.parts()) {
            if (!p.transparent() && (best == null || p.mesh().faceCount() > best.mesh().faceCount())) {
                best = p;
            }
        }
        return best != null ? best : human.parts().get(0);
    }

    private DressedPerson dress(HumanModel human, HumanModel.Part skinPart, ModelData source) {
        Map<Zone, List<Part>> zones = new EnumMap<>(Zone.class);
        Shoes shoes = new Shoes(source);
        source.zones().forEach((zone, parts) -> {
            List<Part> fitted = new ArrayList<>();
            for (Part part : parts) {
                MeshData mesh = zone.garment() == Garment.SHOES ? shoes.place(part.mesh()) : fitGarment(zone, part.mesh());
                if (zone.garment() == Garment.SOCKS) {
                    mesh = removeBelow(mesh, shoes, 0.55);
                }
                fitted.add(new Part(part.name(), mesh));
            }
            zones.put(zone, fitted);
        });

        List<HumanModel.Part> parts = new ArrayList<>();
        for (HumanModel.Part p : human.parts()) {
            parts.add(p == skinPart ? new HumanModel.Part(p.name(), hideCovered(removeBelow(p.mesh(), shoes, 0.75), zones), p.diffuse(),
                    p.specular(), p.specularPower(), p.texture(), p.transparent()) : p);
        }
        HumanModel body = new HumanModel(human.persona(), parts, human.minY(), human.height());
        ModelData outfit = new ModelData(source.bodyType(), List.of(), zones, focus(zones, human), human.height());
        return new DressedPerson(body, outfit);
    }

    // ---------------------------------------------------------------- Prendas de tela

    private static Region region(Zone zone) {
        return switch (zone) {
            case JERSEY_BODY, JERSEY_COLLAR, JERSEY_SIDES, SHORTS_WAIST -> Region.TORSO;
            case JERSEY_SLEEVES, JERSEY_CUFFS -> Region.ARM;
            case SHORTS_BASE, SHORTS_STRIPE -> Region.BLEND;
            case SOCKS_LEG, SOCKS_BAND -> Region.LEG;
            default -> Region.SHOE;
        };
    }

    private MeshData fitGarment(Zone zone, MeshData mesh) {
        Region region = region(zone);
        float[] pts = mesh.points();
        int n = mesh.pointCount();
        // Puntos repetidos en la misma posición (costuras entre piezas) se tratan como uno solo.
        int[] canon = new int[n];
        int[] firstOf = weld(pts, canon);
        int m = firstOf.length;
        double[] src = new double[m * 3];
        double[] dst = new double[m * 3];
        boolean[] inside = new boolean[m];
        IntStream.range(0, m).parallel().forEach(c -> {
            int i = firstOf[c] * 3;
            Vec3 p = new Vec3(pts[i], -pts[i + 1], -pts[i + 2]);
            Placed placed = transfer(p, region);
            Vec3 q = placed.p();
            inside[c] = placed.inside();
            src[c * 3] = p.x();
            src[c * 3 + 1] = p.y();
            src[c * 3 + 2] = p.z();
            dst[c * 3] = q.x();
            dst[c * 3 + 1] = q.y();
            dst[c * 3 + 2] = q.z();
        });
        // El short cambia de eje (pelvis → pierna) sobre la entrepierna: más pasadas para que no quede un escalón.
        int passes = region == Region.BLEND ? SMOOTH_PASSES * 3 : SMOOTH_PASSES;
        smoothDisplacement(mesh, canon, src, dst, inside, m, passes);
        double gap = zone.garment() == Garment.SOCKS ? SOCK_GAP : GAP;
        // Lo que en el maniquí iba dentro del cuerpo (la cara interior de la pelvis del short) sigue oculto.
        IntStream.range(0, m).parallel().filter(c -> !inside[c]).forEach(c -> pushOut(dst, c, gap));

        float[] out = new float[pts.length];
        for (int i = 0; i < n; i++) {
            int c = canon[i];
            out[i * 3] = (float) dst[c * 3];
            out[i * 3 + 1] = (float) -dst[c * 3 + 1];
            out[i * 3 + 2] = (float) -dst[c * 3 + 2];
        }
        return withNormals(mesh, out);
    }

    /** Punto trasladado; {@code inside} = en el maniquí estaba dentro del cuerpo (no se ve). */
    private record Placed(Vec3 p, boolean inside) {
    }

    /** Traslada un punto del uniforme del maniquí (espacio del modelo) a la persona. */
    private Placed transfer(Vec3 p, Region region) {
        return switch (region) {
            case TORSO -> torso(p);
            case LEG -> leg(p);
            case ARM -> arm(p);
            case BLEND -> {
                double w = smoothstep(blendTop, blendBottom, p.y());
                if (w <= 0) {
                    yield torso(p);
                }
                if (w >= 1) {
                    yield leg(p);
                }
                Placed a = torso(p);
                Placed b = leg(p);
                yield new Placed(a.p().lerp(b.p(), w), w < 0.5 ? a.inside() : b.inside());
            }
            case SHOE -> new Placed(p, false);
        };
    }

    private Placed torso(Vec3 p) {
        return vertical(p, from.torsoCenter(p.y()), to.torsoCenter(mapY(p.y())));
    }

    private Placed leg(Vec3 p) {
        int sign = p.x() >= 0 ? 1 : -1;
        return vertical(p, from.legCenter(sign, p.y()), to.legCenter(sign, mapY(p.y())));
    }

    /** Rayo horizontal desde el eje de una parte vertical (torso o pierna). */
    private Placed vertical(Vec3 p, Vec3 axisFrom, Vec3 axisTo) {
        Vec3 h = new Vec3(p.x() - axisFrom.x(), 0, p.z() - axisFrom.z());
        double d = h.length();
        double y = axisTo.y();
        if (d < 1e-6) {
            return new Placed(new Vec3(axisTo.x(), y, axisTo.z()), true);
        }
        Vec3 dir = h.mul(1 / d);
        double[] r = radius(axisFrom, dir, d, axisTo, dir, 0.6);
        return new Placed(new Vec3(axisTo.x() + dir.x() * r[0], y, axisTo.z() + dir.z() * r[0]), r[1] > 0);
    }

    private Placed arm(Vec3 p) {
        int sign = p.x() >= 0 ? 1 : -1;
        double s = from.armProject(sign, p);
        Vec3 c = from.armPoint(sign, s);
        Vec3 axis = from.armDirection(sign, s);
        Vec3 v = p.sub(c);
        v = v.sub(axis.mul(v.dot(axis)));
        double d = v.length();
        // Sobre la axila se conserva el desnivel vertical: en la persona el brazo va inclinado (pose A) y, con la
        // misma longitud sobre el eje, la manga no llegaría al hombro.
        double sh = s >= 0 ? s * ratio
                : s * ratio * Math.abs(axis.y()) / Math.max(0.3, Math.abs(to.armDirection(sign, 0).y()));
        Vec3 ch = to.armPoint(sign, sh);
        if (d < 1e-6) {
            return new Placed(ch, true);
        }
        Vec3[] fFrom = frame(axis);
        double angle = Math.atan2(v.dot(fFrom[1]), v.dot(fFrom[0]));
        Vec3[] fTo = frame(to.armDirection(sign, sh));
        Vec3 dirFrom = v.mul(1 / d);
        Vec3 dirTo = fTo[0].mul(Math.cos(angle)).add(fTo[1].mul(Math.sin(angle)));
        double[] r = radius(c, dirFrom, d, ch, dirTo, 0.3);
        return new Placed(ch.addScaled(dirTo, r[0]), r[1] > 0);
    }

    /**
     * Distancia desde el eje de la persona a la que va la tela: radio de su piel en esa dirección más la holgura
     * que la prenda tenía sobre el maniquí. Si el rayo no da con la piel o da algo absurdo (otra parte del cuerpo),
     * se usa el radio del maniquí escalado. Un punto que en el maniquí estaba dentro del cuerpo queda dentro, a la
     * misma fracción del radio. Devuelve {radio, 1 si quedó dentro}.
     */
    private double[] radius(Vec3 originFrom, Vec3 dirFrom, double d, Vec3 originTo, Vec3 dirTo, double max) {
        double rFrom = mannequinQ.get().ray(originFrom, dirFrom, max);
        boolean inside = !Double.isNaN(rFrom) && d < rFrom - 0.001;
        double e = Double.isNaN(rFrom) ? 0.008 : clamp(d - rFrom, MIN_EASE, MAX_EASE);
        double expected = (Double.isNaN(rFrom) ? d - e : rFrom) * ratio;
        double rTo = skinQ.get().ray(originTo, dirTo, max);
        if (Double.isNaN(rTo) || rTo < 0.4 * expected || rTo > 2.2 * expected + 0.03) {
            rTo = expected;
        }
        if (inside) {
            return new double[]{rTo * Math.max(0, d) / rFrom, 1};
        }
        return new double[]{rTo + e, 0};
    }

    /**
     * Alturas proporcionales por tramos: piso, entrepierna, axila y cuello. Sobre la base del cuello (escote,
     * ribete) se usa la escala de altura general: el tramo cuello–coronilla incluye la cabeza y subiría el escote.
     */
    double mapY(double y) {
        double[] a = {from.floor, from.crotch, from.armpit, from.neck};
        double[] b = {to.floor, to.crotch, to.armpit, to.neck};
        if (y <= a[0]) {
            return b[0] + (y - a[0]) * ratio;
        }
        for (int i = 1; i < a.length; i++) {
            if (y <= a[i]) {
                double t = (y - a[i - 1]) / (a[i] - a[i - 1]);
                return b[i - 1] + t * (b[i] - b[i - 1]);
            }
        }
        return b[b.length - 1] + (y - a[a.length - 1]) * ratio;
    }

    /** Base ortonormal perpendicular a {@code axis}: [frente, lado]. */
    private static Vec3[] frame(Vec3 axis) {
        Vec3 f = Vec3.FRONT.sub(axis.mul(Vec3.FRONT.dot(axis)));
        if (f.length() < 1e-6) {
            f = new Vec3(1, 0, 0).sub(axis.mul(axis.x()));
        }
        f = f.normalize();
        return new Vec3[]{f, axis.cross(f).normalize()};
    }

    /**
     * Suaviza el desplazamiento (destino − origen) con sus vecinos: quita picos sin borrar los pliegues. Solo se
     * mezclan vecinos del mismo lado de la piel: lo oculto dentro del cuerpo no arrastra a la tela visible.
     */
    private static void smoothDisplacement(MeshData mesh, int[] canon, double[] src, double[] dst, boolean[] inside,
                                           int m, int passes) {
        List<int[]> edges = new ArrayList<>();
        int[] f = mesh.faces();
        int[] degree = new int[m];
        for (int k = 0; k < mesh.faceCount(); k++) {
            int a = canon[f[k * MeshData.FACE_STRIDE]];
            int b = canon[f[k * MeshData.FACE_STRIDE + 3]];
            int c = canon[f[k * MeshData.FACE_STRIDE + 6]];
            for (int[] e : new int[][]{{a, b}, {b, c}, {c, a}}) {
                if (inside[e[0]] == inside[e[1]]) {
                    edges.add(e);
                    degree[e[0]]++;
                    degree[e[1]]++;
                }
            }
        }
        double[] delta = new double[m * 3];
        for (int i = 0; i < m * 3; i++) {
            delta[i] = dst[i] - src[i];
        }
        for (int pass = 0; pass < passes; pass++) {
            double[] sum = new double[m * 3];
            for (int[] e : edges) {
                for (int k = 0; k < 3; k++) {
                    sum[e[0] * 3 + k] += delta[e[1] * 3 + k];
                    sum[e[1] * 3 + k] += delta[e[0] * 3 + k];
                }
            }
            for (int c = 0; c < m; c++) {
                if (degree[c] == 0) {
                    continue;
                }
                for (int k = 0; k < 3; k++) {
                    delta[c * 3 + k] = 0.5 * delta[c * 3 + k] + 0.5 * sum[c * 3 + k] / degree[c];
                }
            }
        }
        for (int i = 0; i < m * 3; i++) {
            dst[i] = src[i] + delta[i];
        }
    }

    /** Si el punto quedó dentro de la piel o a menos de {@code gap}, lo saca por la normal de la cara más cercana. */
    private void pushOut(double[] dst, int c, double gap) {
        TriangleIndex.Query q = skinQ.get();
        for (int it = 0; it < 3; it++) {
            Vec3 p = new Vec3(dst[c * 3], dst[c * 3 + 1], dst[c * 3 + 2]);
            TriangleIndex.Hit hit = q.nearest(p, 0.03);
            if (hit == null) {
                hit = q.nearest(p, 0.12);
            }
            if (hit == null) {
                return;
            }
            double sd = p.sub(hit.point()).dot(hit.normal());
            if (sd >= gap) {
                return;
            }
            Vec3 moved = p.addScaled(hit.normal(), gap - sd);
            dst[c * 3] = moved.x();
            dst[c * 3 + 1] = moved.y();
            dst[c * 3 + 2] = moved.z();
        }
    }

    // ---------------------------------------------------------------- Zapatillas

    /** Transformación rígida (giro + escala por eje del pie) de cada zapatilla sobre el pie de la persona. */
    private final class Shoes {
        private final Foot[] shoe = new Foot[2];
        private final Foot[] foot = new Foot[2];

        Shoes(ModelData source) {
            List<double[]> shoePts = new ArrayList<>();
            source.zones().forEach((zone, parts) -> {
                if (zone.garment() == Garment.SHOES) {
                    for (Part p : parts) {
                        float[] pts = p.mesh().points();
                        for (int i = 0; i < pts.length; i += 3) {
                            shoePts.add(new double[]{pts[i], -pts[i + 1], -pts[i + 2]});
                        }
                    }
                }
            });
            List<double[]> footPts = new ArrayList<>();
            double[] v = skin.vertices();
            double footTop = to.floor + 0.045 * to.height();
            for (int i = 0; i < v.length; i += 3) {
                if (v[i + 1] < footTop && Math.abs(v[i]) < 0.2 * to.height()) {
                    footPts.add(new double[]{v[i], v[i + 1], v[i + 2]});
                }
            }
            for (int k = 0; k < 2; k++) {
                int sign = k == 0 ? -1 : 1;
                shoe[k] = Foot.of(shoePts, sign);
                foot[k] = Foot.of(footPts, sign);
            }
        }

        MeshData place(MeshData mesh) {
            float[] pts = mesh.points();
            float[] out = new float[pts.length];
            for (int i = 0; i < pts.length; i += 3) {
                double x = pts[i];
                double y = -pts[i + 1];
                double z = -pts[i + 2];
                int k = x >= 0 ? 1 : 0;
                Foot s = shoe[k];
                Foot f = foot[k];
                if (s == null || f == null) {
                    out[i] = pts[i];
                    out[i + 1] = pts[i + 1];
                    out[i + 2] = pts[i + 2];
                    continue;
                }
                double qx = x - s.cx;
                double qz = z - s.cz;
                double along = qx * s.dx + qz * s.dz;
                double across = qx * -s.dz + qz * s.dx;
                // El calzado cubre el pie con 2.5 cm de largo y 1.6 cm de ancho de holgura.
                double sl = (f.length + 0.025) / s.length;
                double sw = (f.width + 0.016) / s.width;
                double sy = clamp((sl + sw) / 2, 0.6, 1.25);
                double nx = f.cx + f.dx * along * sl - f.dz * across * sw;
                double nz = f.cz + f.dz * along * sl + f.dx * across * sw;
                out[i] = (float) nx;
                out[i + 1] = (float) -(y * sy + to.floor);
                out[i + 2] = (float) -nz;
            }
            return withNormals(mesh, out);
        }

        /** Altura del borde del calzado a la que se corta lo que queda dentro (fracción {@code k} de su alto). */
        double cutHeight(double x, double z, double k) {
            Foot f = foot[x >= 0 ? 1 : 0];
            Foot s = shoe[x >= 0 ? 1 : 0];
            if (f == null || s == null) {
                return Double.NEGATIVE_INFINITY;
            }
            double qx = x - f.cx;
            double qz = z - f.cz;
            double along = qx * f.dx + qz * f.dz;
            double across = qx * -f.dz + qz * f.dx;
            double halfL = (f.length + 0.025) / 2;
            double halfW = (f.width + 0.016) / 2;
            if (Math.abs(along) > halfL || Math.abs(across) > halfW) {
                return Double.NEGATIVE_INFINITY;
            }
            double sy = clamp(((f.length + 0.025) / s.length + (f.width + 0.016) / s.width) / 2, 0.6, 1.25);
            return to.floor + s.height * sy * k;
        }
    }

    /** Huella de un pie o zapatilla: centro, dirección (hacia la punta), largo, ancho y alto. */
    private record Foot(double cx, double cz, double dx, double dz, double length, double width, double height) {

        static Foot of(List<double[]> pts, int sign) {
            double sx = 0;
            double sz = 0;
            int n = 0;
            for (double[] p : pts) {
                if (p[0] * sign > 0) {
                    sx += p[0];
                    sz += p[2];
                    n++;
                }
            }
            if (n < 10) {
                return null;
            }
            double mx = sx / n;
            double mz = sz / n;
            double cxx = 0;
            double cxz = 0;
            double czz = 0;
            for (double[] p : pts) {
                if (p[0] * sign > 0) {
                    double a = p[0] - mx;
                    double b = p[2] - mz;
                    cxx += a * a;
                    cxz += a * b;
                    czz += b * b;
                }
            }
            // Eje principal en el plano del piso (el pie es más largo que ancho).
            double angle = 0.5 * Math.atan2(2 * cxz, czz - cxx);
            double dx = Math.sin(angle);
            double dz = Math.cos(angle);
            if (dz < 0) {
                dx = -dx;
                dz = -dz;
            }
            double minA = Double.MAX_VALUE;
            double maxA = -Double.MAX_VALUE;
            double minC = Double.MAX_VALUE;
            double maxC = -Double.MAX_VALUE;
            double maxY = 0;
            for (double[] p : pts) {
                if (p[0] * sign > 0) {
                    double a = (p[0] - mx) * dx + (p[2] - mz) * dz;
                    double c = (p[0] - mx) * -dz + (p[2] - mz) * dx;
                    minA = Math.min(minA, a);
                    maxA = Math.max(maxA, a);
                    minC = Math.min(minC, c);
                    maxC = Math.max(maxC, c);
                    maxY = Math.max(maxY, p[1]);
                }
            }
            double midA = (minA + maxA) / 2;
            double midC = (minC + maxC) / 2;
            return new Foot(mx + dx * midA - dz * midC, mz + dz * midA + dx * midC, dx, dz, maxA - minA,
                    maxC - minC, maxY);
        }
    }

    /** Quita las caras que quedan completas dentro del calzado (por debajo de la fracción {@code k} de su alto). */
    private static MeshData removeBelow(MeshData mesh, Shoes shoes, double k) {
        float[] p = mesh.points();
        int[] f = mesh.faces();
        int[] kept = new int[f.length];
        int n = 0;
        for (int t = 0; t < mesh.faceCount(); t++) {
            boolean inside = true;
            for (int c = 0; c < 3 && inside; c++) {
                int i = f[t * MeshData.FACE_STRIDE + c * 3] * 3;
                double y = -p[i + 1];
                inside = y < shoes.cutHeight(p[i], -p[i + 2], k);
            }
            if (!inside) {
                System.arraycopy(f, t * MeshData.FACE_STRIDE, kept, n, MeshData.FACE_STRIDE);
                n += MeshData.FACE_STRIDE;
            }
        }
        return n == f.length ? mesh : new MeshData(mesh.points(), mesh.normals(), mesh.texCoords(), Arrays.copyOf(kept, n));
    }

    /**
     * Oculta la piel que queda bajo la ropa, como "hide faces under clothes" de MakeHuman: un punto de piel se oculta
     * si un rayo por su normal choca con tela a menos de 8 cm y está a más de 3.5 cm de una abertura (escote, basta,
     * puños, perneras), por donde se podría ver. Se oculta también la piel que ya asoma por la tela (tela detrás a
     * menos de 2 cm), esté donde esté. Las prendas simuladas tienen grosor (sus bordes están cerrados) y cada zona es
     * una malla aparte, así que el borde de sus mallas son costuras, no aberturas: sus aberturas reales llegan en
     * {@code extraOpenings} y sus zonas se saltan ({@code simulated}).
     */
    private static MeshData hideCovered(MeshData mesh, Map<Zone, List<Part>> zones, List<Vec3> extraOpenings,
                                        java.util.Set<Garment> simulated) {
        List<MeshData> cloth = new ArrayList<>();
        for (Zone z : List.of(Zone.JERSEY_BODY, Zone.JERSEY_SIDES, Zone.JERSEY_SLEEVES, Zone.JERSEY_COLLAR,
                Zone.JERSEY_CUFFS, Zone.SHORTS_BASE, Zone.SHORTS_WAIST, Zone.SHORTS_STRIPE)) {
            zones.getOrDefault(z, List.of()).forEach(p -> cloth.add(p.mesh()));
        }
        if (cloth.isEmpty()) {
            return mesh;
        }
        Openings openings = new Openings();
        for (Zone z : List.of(Zone.JERSEY_BODY, Zone.JERSEY_COLLAR, Zone.JERSEY_CUFFS, Zone.SHORTS_BASE)) {
            if (!simulated.contains(z.garment())) {
                zones.getOrDefault(z, List.of()).forEach(part -> openings.addBoundary(part.mesh()));
            }
        }
        extraOpenings.forEach(openings::add);
        TriangleIndex index = TriangleIndex.of(cloth, CELL);
        ThreadLocal<TriangleIndex.Query> q = ThreadLocal.withInitial(index::query);
        float[] p = mesh.points();
        float[] nrm = mesh.normals();
        int[] f = mesh.faces();
        boolean[] covered = new boolean[mesh.pointCount()];
        IntStream.range(0, mesh.pointCount()).parallel().forEach(i -> {
            Vec3 o = new Vec3(p[i * 3], -p[i * 3 + 1], -p[i * 3 + 2]);
            Vec3 n = new Vec3(nrm[i * 3], -nrm[i * 3 + 1], -nrm[i * 3 + 2]).normalize();
            TriangleIndex.Query query = q.get();
            boolean under = !Double.isNaN(query.ray(o, n, 0.08)) && !openings.near(o, 0.035);
            covered[i] = under || !Double.isNaN(query.ray(o, n.mul(-1), 0.02));
        });
        int[] kept = new int[f.length];
        int k = 0;
        for (int t = 0; t < mesh.faceCount(); t++) {
            int b = t * MeshData.FACE_STRIDE;
            if (!(covered[f[b]] && covered[f[b + 3]] && covered[f[b + 6]])) {
                System.arraycopy(f, b, kept, k, MeshData.FACE_STRIDE);
                k += MeshData.FACE_STRIDE;
            }
        }
        return k == f.length ? mesh : new MeshData(p, nrm, mesh.texCoords(), Arrays.copyOf(kept, k));
    }

    /** Puntos de borde de las prendas (aberturas), en una rejilla para buscar si hay uno cerca. */
    private static final class Openings {
        private static final double CELL_SIZE = 0.035;
        private final Map<List<Integer>, List<Vec3>> grid = new HashMap<>();

        void addBoundary(MeshData mesh) {
            float[] pts = mesh.points();
            int[] canon = new int[mesh.pointCount()];
            int[] first = weld(pts, canon);
            boolean[] edge = ClothOps.boundary(mesh, canon, first.length);
            for (int c = 0; c < first.length; c++) {
                if (edge[c]) {
                    int i = first[c] * 3;
                    add(new Vec3(pts[i], -pts[i + 1], -pts[i + 2]));
                }
            }
        }

        void add(Vec3 v) {
            grid.computeIfAbsent(cell(v), k -> new ArrayList<>()).add(v);
        }

        boolean near(Vec3 v, double r) {
            List<Integer> c = cell(v);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        List<Vec3> list = grid.get(List.of(c.get(0) + dx, c.get(1) + dy, c.get(2) + dz));
                        if (list != null) {
                            for (Vec3 o : list) {
                                if (o.sub(v).length() < r) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }

        private static List<Integer> cell(Vec3 v) {
            return List.of((int) Math.floor(v.x() / CELL_SIZE), (int) Math.floor(v.y() / CELL_SIZE),
                    (int) Math.floor(v.z() / CELL_SIZE));
        }
    }

    // ---------------------------------------------------------------- Ayudas

    /** Normales suaves recalculadas tras deformar, con el mismo sentido que las originales. */
    private static MeshData withNormals(MeshData mesh, float[] points) {
        float[] old = mesh.normals();
        double[] acc = new double[old.length];
        int[] f = mesh.faces();
        for (int t = 0; t < mesh.faceCount(); t++) {
            int b = t * MeshData.FACE_STRIDE;
            int a = f[b] * 3;
            int c = f[b + 3] * 3;
            int d = f[b + 6] * 3;
            double ux = points[c] - points[a];
            double uy = points[c + 1] - points[a + 1];
            double uz = points[c + 2] - points[a + 2];
            double wx = points[d] - points[a];
            double wy = points[d + 1] - points[a + 1];
            double wz = points[d + 2] - points[a + 2];
            double nx = uy * wz - uz * wy;
            double ny = uz * wx - ux * wz;
            double nz = ux * wy - uy * wx;
            // El sentido de la cara puede no coincidir con la normal guardada: se orienta con la original.
            int n0 = f[b + 1] * 3;
            if (nx * old[n0] + ny * old[n0 + 1] + nz * old[n0 + 2] < 0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }
            for (int k = 0; k < 3; k++) {
                int ni = f[b + k * 3 + 1] * 3;
                acc[ni] += nx;
                acc[ni + 1] += ny;
                acc[ni + 2] += nz;
            }
        }
        float[] normals = new float[old.length];
        for (int i = 0; i < old.length; i += 3) {
            double len = Math.sqrt(acc[i] * acc[i] + acc[i + 1] * acc[i + 1] + acc[i + 2] * acc[i + 2]);
            if (len < 1e-12) {
                normals[i] = old[i];
                normals[i + 1] = old[i + 1];
                normals[i + 2] = old[i + 2];
            } else {
                normals[i] = (float) (acc[i] / len);
                normals[i + 1] = (float) (acc[i + 1] / len);
                normals[i + 2] = (float) (acc[i + 2] / len);
            }
        }
        return new MeshData(points, normals, mesh.texCoords(), mesh.faces());
    }

    /** Agrupa puntos en la misma posición (0.01 mm). Devuelve el primer índice de cada grupo. */
    private static int[] weld(float[] pts, int[] canon) {
        Map<List<Integer>, Integer> ids = new HashMap<>();
        List<Integer> first = new ArrayList<>();
        for (int i = 0; i < canon.length; i++) {
            List<Integer> key = List.of(Math.round(pts[i * 3] * 1e5f), Math.round(pts[i * 3 + 1] * 1e5f),
                    Math.round(pts[i * 3 + 2] * 1e5f));
            Integer id = ids.get(key);
            if (id == null) {
                id = first.size();
                ids.put(key, id);
                first.add(i);
            }
            canon[i] = id;
        }
        return first.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Encuadres por prenda sobre la persona. */
    private static Map<Garment, Focus> focus(Map<Zone, List<Part>> zones, HumanModel human) {
        double h = human.height();
        double k = h / 1.812;
        Map<Garment, double[]> boxes = new EnumMap<>(Garment.class);
        zones.forEach((zone, parts) -> {
            double[] box = boxes.computeIfAbsent(zone.garment(),
                    g -> new double[]{Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE});
            for (Part p : parts) {
                float[] pts = p.mesh().points();
                for (int i = 0; i < pts.length; i += 3) {
                    box[0] = Math.min(box[0], -pts[i + 1]);
                    box[1] = Math.max(box[1], -pts[i + 1]);
                    box[2] = Math.min(box[2], -pts[i + 2]);
                    box[3] = Math.max(box[3], -pts[i + 2]);
                }
            }
        });
        Map<Garment, Focus> focus = new EnumMap<>(Garment.class);
        Map<Garment, double[]> look = Map.of(Garment.JERSEY, new double[]{1.85, 4}, Garment.SHORTS, new double[]{1.50, 3},
                Garment.SOCKS, new double[]{1.40, 7}, Garment.SHOES, new double[]{0.95, 18});
        boxes.forEach((g, box) -> {
            double[] l = look.getOrDefault(g, new double[]{1.6, 5});
            focus.put(g, new Focus(new Vec3(0, (box[0] + box[1]) / 2, (box[2] + box[3]) / 2), l[0] * k, l[1]));
        });
        Focus full = fullBody(human.minY(), h);
        for (Garment g : Garment.values()) {
            focus.putIfAbsent(g, full);
        }
        focus.put(Garment.KIT, full);
        return focus;
    }

    /**
     * Encuadre de la persona completa, de la cabeza a los pies: un poco más lejos que el maniquí y apuntando algo por
     * encima del centro del cuerpo, así queda centrada en la parte del visor que no tapa la barra flotante del
     * diseñador (arriba). Lo usan el encuadre inicial y "Reiniciar".
     */
    public static Focus fullBody(double minY, double height) {
        return new Focus(new Vec3(0, minY + 0.535 * height, 0), 4.6 * height / 1.812, 5);
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0, 1);
        return t * t * (3 - 2 * t);
    }
}
