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

public final class OutfitFitter {
    public record DressedPerson(HumanModel body, ModelData outfit, Map<Garment, ClothShading> shading) {
    }

    private static final double CELL = 0.02;
    private static final double MIN_EASE = 0.002;
    private static final double MAX_EASE = 0.035;
    private static final double SHOULDER_MAX_EASE = 0.012;
    private static final double SLEEVE_TUBE = 0.02;
    private static final double SLEEVE_FLARE = 0.10;
    private static final double DRAPE_SLOPE = 0.10;
    private static final double COLLAR_THICKNESS = 0.55;
    private static final double COLLAR_HEIGHT = 0.6;
    private static final double COLLAR_LOWER = 0.003;
    private static final double GAP = 0.004;
    private static final double SOCK_GAP = 0.0025;
    private static final double FOLLOWER_GAP = 0.002;
    private static final int SMOOTH_PASSES = 4;
    private static final double BRIDGE = Math.toRadians(9);

    private enum Region { JERSEY, TORSO, LEG, ARM, BLEND, SHOE }

    private record Cloth(double ease, int taubin) {
    }

    private static final Map<Zone, Cloth> BASES = Map.of(
            Zone.JERSEY_BODY, new Cloth(0.011, 60),
            Zone.JERSEY_SLEEVES, new Cloth(0.008, 15),
            Zone.SHORTS_BASE, new Cloth(0.010, 30),
            Zone.SOCKS_LEG, new Cloth(SOCK_GAP, 0));

    private static final Map<Zone, Zone> FOLLOWS = Map.of(
            Zone.JERSEY_COLLAR, Zone.JERSEY_BODY,
            Zone.JERSEY_SIDES, Zone.JERSEY_BODY,
            Zone.JERSEY_CUFFS, Zone.JERSEY_SLEEVES,
            Zone.SHORTS_WAIST, Zone.SHORTS_BASE,
            Zone.SHORTS_STRIPE, Zone.SHORTS_BASE,
            Zone.SOCKS_BAND, Zone.SOCKS_LEG);

    private final BodyFrame from;
    private final BodyFrame to;
    private final TriangleIndex skin;
    private final double ratio;
    private final double blendTop;
    private final double blendBottom;
    private final double hipFreeze;
    private final double drapeTop;
    private final double drapeBottom;
    private final ThreadLocal<TriangleIndex.Query> skinQ;

    private OutfitFitter(BodyFrame from, BodyFrame to, TriangleIndex skin, BodyShape shape) {
        this.from = from;
        this.to = to;
        this.skin = skin;
        this.ratio = to.height() / from.height();
        double scale = shape.height() / 1.812;
        blendTop = (ProceduralModelProvider.SHORTS_CROTCH + 0.03) * scale;
        blendBottom = (ProceduralModelProvider.SHORTS_CROTCH - 0.07) * scale;
        hipFreeze = ProceduralModelProvider.SHORTS_HIP_FREEZE * scale;
        drapeTop = ProceduralModelProvider.JERSEY_DRAPE_TOP * scale;
        drapeBottom = ProceduralModelProvider.JERSEY_DRAPE_BOTTOM * scale;
        skinQ = ThreadLocal.withInitial(this.skin::query);
    }

    public static DressedPerson fit(HumanModel human, ModelData source) {
        return fit(human, source, Map.of());
    }

    public static DressedPerson fit(HumanModel human, ModelData source,
                                    Map<Garment, SimulatedGarments.Piece> simulated) {
        BodyShape shape = BodyShape.of(source.bodyType());
        HumanModel.Part skinPart = skinOf(human);
        TriangleIndex skin = TriangleIndex.of(List.of(skinPart.mesh()), CELL);
        TriangleIndex mannequin = TriangleIndex.of(source.mannequin().stream().map(Part::mesh).toList(), CELL);
        BodyFrame from = BodyFrame.of(shape, mannequin.vertices());
        BodyFrame to = BodyFrame.measure(skin.vertices(), skin.triangles(), human.minY(), human.height());
        return new OutfitFitter(from, to, skin, shape).dress(human, skinPart, source, simulated);
    }

    static HumanModel.Part skinOf(HumanModel human) {
        HumanModel.Part best = null;
        for (HumanModel.Part p : human.parts()) {
            if (!p.transparent() && (best == null || p.mesh().faceCount() > best.mesh().faceCount())) {
                best = p;
            }
        }
        return best != null ? best : human.parts().get(0);
    }

    private record Fitted(MeshData mesh, int[] canon, double[] src, double[] dst, boolean[] inside) {
    }

    private DressedPerson dress(HumanModel human, HumanModel.Part skinPart, ModelData source,
                                Map<Garment, SimulatedGarments.Piece> simulated) {
        Shoes shoes = new Shoes(source);
        Map<Zone, List<Fitted>> base = new EnumMap<>(Zone.class);
        BASES.forEach((zone, cloth) -> {
            if (!simulated.containsKey(zone.garment())) {
                base.put(zone, source.zones().get(zone).stream().map(p -> fitBase(zone, cloth, p.mesh())).toList());
            }
        });
        if (!simulated.containsKey(Garment.JERSEY)) {
            hideSleeveTops(base.get(Zone.JERSEY_SLEEVES), base.get(Zone.JERSEY_BODY));
        }

        Map<Zone, List<Part>> zones = new EnumMap<>(Zone.class);
        simulated.forEach((g, piece) -> piece.zones().forEach((zone, parts) -> zones.put(zone, g != Garment.SOCKS
                ? parts : parts.stream().map(p -> new Part(p.name(), removeBelow(p.mesh(), shoes, 0.55))).toList())));
        double h = human.height();
        source.zones().forEach((zone, parts) -> {
            if (simulated.containsKey(zone.garment())) {
                return;
            }
            List<Part> out = new ArrayList<>();
            for (int i = 0; i < parts.size(); i++) {
                Part part = parts.get(i);
                MeshData mesh;
                if (zone.garment() == Garment.SHOES) {
                    mesh = shoes.place(part.mesh());
                } else if (base.containsKey(zone)) {
                    mesh = toMesh(base.get(zone).get(i));
                } else if (zone == Zone.JERSEY_COLLAR) {
                    mesh = followNeckline(part.mesh(), base.get(Zone.JERSEY_BODY));
                } else {
                    mesh = follow(part.mesh(), base.get(FOLLOWS.get(zone)));
                }
                if (zone.garment() == Garment.SOCKS) {
                    mesh = removeBelow(mesh, shoes, 0.55);
                }
                if (zone == Zone.JERSEY_BODY) {
                    mesh = ClothOps.hemFold(mesh, lowest(mesh, 0.06 * h), 0.005, 0.006);
                } else if (zone == Zone.SHORTS_BASE) {
                    mesh = ClothOps.hemFold(mesh, lowest(mesh, 0.03 * h), 0.005, 0.006);
                }
                out.add(new Part(part.name(), mesh));
            }
            zones.put(zone, out);
        });

        List<Vec3> openings = new ArrayList<>();
        simulated.values().forEach(piece -> openings.addAll(piece.openings()));
        List<HumanModel.Part> parts = new ArrayList<>();
        for (HumanModel.Part p : human.parts()) {
            parts.add(p == skinPart ? new HumanModel.Part(p.name(),
                    hideCovered(removeBelow(p.mesh(), shoes, 0.75), zones, openings, simulated.keySet()),
                    p.diffuse(), p.specular(), p.specularPower(), p.texture(), p.transparent()) : p);
        }
        HumanModel body = new HumanModel(human.persona(), parts, human.minY(), human.height());
        ModelData outfit = new ModelData(source.bodyType(), List.of(), zones, focus(zones, human), human.height());
        Map<Garment, ClothShading> shading = shading(zones, OutfitFitter.skinOf(body).mesh(), simulated.keySet());
        simulated.forEach((g, piece) -> shading.put(g, piece.shading()));
        return new DressedPerson(body, outfit, shading);
    }

    private static final Map<Garment, Integer> SHADING_SIZE = Map.of(Garment.JERSEY, 1024, Garment.SHORTS, 1024,
            Garment.SOCKS, 512);
    private static final int AO_RAYS = 14;
    private static final double AO_DISTANCE = 0.12;

    private Map<Garment, ClothShading> shading(Map<Zone, List<Part>> zones, MeshData visibleSkin,
                                               java.util.Set<Garment> baked) {
        List<MeshData> all = new ArrayList<>();
        all.add(visibleSkin);
        zones.values().forEach(parts -> parts.forEach(pt -> all.add(pt.mesh())));
        TriangleIndex occluders = TriangleIndex.of(all, CELL);
        ThreadLocal<TriangleIndex.Query> q = ThreadLocal.withInitial(occluders::query);
        Vec3[] hemisphere = hemisphere(AO_RAYS);
        Map<Garment, ClothShading> out = new EnumMap<>(Garment.class);
        SHADING_SIZE.forEach((garment, size) -> {
            if (baked.contains(garment)) {
                return;
            }
            List<MeshData> meshes = new ArrayList<>();
            List<ClothShading.PointData> data = new ArrayList<>();
            zones.forEach((zone, parts) -> {
                if (zone.garment() != garment) {
                    return;
                }
                for (Part pt : parts) {
                    meshes.add(pt.mesh());
                    data.add(pointData(zone, pt.mesh(), q, hemisphere));
                }
            });
            if (!meshes.isEmpty()) {
                out.put(garment, ClothShading.bake(meshes, data, size));
            }
        });
        return out;
    }

    private static Vec3[] hemisphere(int n) {
        Vec3[] d = new Vec3[n];
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < n; i++) {
            double z = 1 - (i + 0.5) / n * 0.85;
            double r = Math.sqrt(1 - z * z);
            double a = golden * i;
            d[i] = new Vec3(Math.cos(a) * r, Math.sin(a) * r, z);
        }
        return d;
    }

    private ClothShading.PointData pointData(Zone zone, MeshData mesh, ThreadLocal<TriangleIndex.Query> q,
                                             Vec3[] hemisphere) {
        int n = mesh.pointCount();
        float[] open = new float[n];
        float[] wrinkle = new float[n];
        float[] angle = new float[n];
        float[] pts = mesh.points();
        float[] nrm = mesh.normals();
        int[] normalOf = new int[n];
        int[] f = mesh.faces();
        for (int k = 0; k < f.length; k += 3) {
            normalOf[f[k]] = f[k + 1];
        }
        double h = to.height();
        IntStream.range(0, n).parallel().forEach(i -> {
            Vec3 p = new Vec3(pts[i * 3], -pts[i * 3 + 1], -pts[i * 3 + 2]);
            int ni = normalOf[i] * 3;
            Vec3 nv = new Vec3(nrm[ni], -nrm[ni + 1], -nrm[ni + 2]).normalize();
            Vec3 t = Math.abs(nv.y()) < 0.9 ? Vec3.UP.cross(nv).normalize() : new Vec3(1, 0, 0).cross(nv).normalize();
            Vec3 b = nv.cross(t);
            Vec3 o = p.addScaled(nv, 0.003);
            double occ = 0;
            for (Vec3 d : hemisphere) {
                Vec3 dir = t.mul(d.x()).add(b.mul(d.y())).add(nv.mul(d.z()));
                double hit = q.get().ray(o, dir, AO_DISTANCE);
                if (!Double.isNaN(hit)) {
                    occ += 1 - hit / AO_DISTANCE;
                }
            }
            open[i] = (float) Math.max(0, 1 - 1.4 * occ / hemisphere.length);

            double y = p.y();
            int sign = p.x() >= 0 ? 1 : -1;
            double side = Math.abs(nv.x());
            double inner = Math.max(0, -nv.x() * sign);
            double w = 0;
            double a = Math.PI / 2;
            switch (zone.garment()) {
                case JERSEY -> {
                    if (zone == Zone.JERSEY_SLEEVES || zone == Zone.JERSEY_CUFFS) {
                        w = 0.65 * inner * (0.4 + 0.6 * smoothstep(to.armpit - 0.02 * h, to.armpit - 0.10 * h, y));
                    } else if (zone != Zone.JERSEY_COLLAR) {
                        double armpit = bell(y, to.armpit - 0.04 * h, 0.045 * h) * smoothstep(0.35, 0.85, side);
                        double waist = 0.5 * bell(y, to.floor + 0.585 * h, 0.04 * h) * (0.3 + 0.7 * side);
                        if (armpit > waist) {
                            a = (p.x() > 0) == (p.z() > 0) ? Math.toRadians(55) : Math.toRadians(125);
                        }
                        w = Math.max(w, Math.max(armpit, waist));
                    } else {
                        w = 0;
                    }
                }
                case SHORTS -> {
                    double crotch = bell(y, to.crotch - 0.03 * h, 0.045 * h) * (0.3 + 0.7 * inner);
                    double hip = 0.5 * bell(y, to.crotch + 0.04 * h, 0.04 * h) * smoothstep(0.2, 0.7, nv.z());
                    if (crotch > 0.2) {
                        a = sign > 0 ? Math.toRadians(60) : Math.toRadians(120);
                    }
                    w = Math.max(crotch, hip);
                }
                case SOCKS -> {
                    double knee = bell(y, to.floor + 0.285 * h, 0.035 * h) * smoothstep(0.1, 0.7, -nv.z());
                    double ankle = 0.45 * bell(y, to.floor + 0.075 * h, 0.03 * h);
                    w = Math.max(knee, ankle);
                }
                default -> w = 0;
            }
            wrinkle[i] = (float) w;
            angle[i] = (float) a;
        });
        return new ClothShading.PointData(open, wrinkle, angle);
    }

    private static double bell(double x, double center, double width) {
        double d = (x - center) / width;
        return Math.exp(-d * d);
    }

    private static java.util.function.IntPredicate lowest(MeshData mesh, double band) {
        float[] p = mesh.points();
        double min = Double.MAX_VALUE;
        for (int i = 1; i < p.length; i += 3) {
            min = Math.min(min, -p[i]);
        }
        double limit = min + band;
        return i -> -p[i * 3 + 1] < limit;
    }

    private static Region region(Zone zone) {
        return switch (zone) {
            case JERSEY_BODY -> Region.JERSEY;
            case JERSEY_SLEEVES -> Region.ARM;
            case SHORTS_BASE -> Region.BLEND;
            case SOCKS_LEG -> Region.LEG;
            default -> Region.TORSO;
        };
    }

    private Fitted fitBase(Zone zone, Cloth cloth, MeshData mesh) {
        Region region = region(zone);
        float[] pts = mesh.points();
        int[] canon = new int[mesh.pointCount()];
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
        int passes = region == Region.BLEND ? SMOOTH_PASSES * 3 : SMOOTH_PASSES;
        smoothDisplacement(mesh, canon, src, dst, inside, m, passes);
        IntStream.range(0, m).parallel().filter(c -> !inside[c]).forEach(c -> pushOut(dst, c, cloth.ease()));
        if (cloth.taubin() > 0) {
            int[][] nb = ClothOps.neighbors(mesh, canon, m);
            ClothOps.taubin(dst, nb, ClothOps.boundary(mesh, canon, m), inside, cloth.taubin());
        } else {
            double gap = zone.garment() == Garment.SOCKS ? SOCK_GAP : GAP;
            IntStream.range(0, m).parallel().filter(c -> !inside[c]).forEach(c -> pushOut(dst, c, gap));
        }
        return new Fitted(mesh, canon, src, dst, inside);
    }

    private static MeshData toMesh(Fitted f) {
        float[] pts = f.mesh().points();
        float[] out = new float[pts.length];
        for (int i = 0; i < f.canon().length; i++) {
            int c = f.canon()[i];
            out[i * 3] = (float) f.dst()[c * 3];
            out[i * 3 + 1] = (float) -f.dst()[c * 3 + 1];
            out[i * 3 + 2] = (float) -f.dst()[c * 3 + 2];
        }
        return withNormals(f.mesh(), out);
    }

    private MeshData follow(MeshData mesh, List<Fitted> base) {
        int total = base.stream().mapToInt(b -> b.src().length).sum();
        double[] src = new double[total];
        double[] dst = new double[total];
        int at = 0;
        for (Fitted b : base) {
            System.arraycopy(b.src(), 0, src, at, b.src().length);
            System.arraycopy(b.dst(), 0, dst, at, b.dst().length);
            at += b.src().length;
        }
        float[] pts = mesh.points();
        double[] p = new double[pts.length];
        for (int i = 0; i < pts.length; i += 3) {
            p[i] = pts[i];
            p[i + 1] = -pts[i + 1];
            p[i + 2] = -pts[i + 2];
        }
        double[] moved = ClothOps.follow(src, dst, p);
        IntStream.range(0, moved.length / 3).parallel().forEach(c -> pushOut(moved, c, FOLLOWER_GAP));
        float[] out = new float[pts.length];
        for (int i = 0; i < pts.length; i += 3) {
            out[i] = (float) moved[i];
            out[i + 1] = (float) -moved[i + 1];
            out[i + 2] = (float) -moved[i + 2];
        }
        return withNormals(mesh, out);
    }

    private MeshData followNeckline(MeshData collar, List<Fitted> body) {
        Fitted b = body.get(0);
        MeshData fittedBody = toMesh(b);
        double top = Double.NEGATIVE_INFINITY;
        double bottom = Double.POSITIVE_INFINITY;
        for (int i = 1; i < b.src().length; i += 3) {
            top = Math.max(top, b.src()[i]);
            bottom = Math.min(bottom, b.src()[i]);
        }
        double middle = (top + bottom) / 2;
        float[] out = ClothOps.followEdge(b.mesh(), fittedBody, i -> -b.mesh().points()[i * 3 + 1] > middle,
                collar.points(), COLLAR_THICKNESS, COLLAR_HEIGHT, COLLAR_LOWER);
        double[] model = new double[out.length];
        for (int i = 0; i < out.length; i += 3) {
            model[i] = out[i];
            model[i + 1] = -out[i + 1];
            model[i + 2] = -out[i + 2];
        }
        IntStream.range(0, model.length / 3).parallel().forEach(c -> pushOut(model, c, FOLLOWER_GAP));
        for (int i = 0; i < out.length; i += 3) {
            out[i] = (float) model[i];
            out[i + 1] = (float) -model[i + 1];
            out[i + 2] = (float) -model[i + 2];
        }
        return withNormals(collar, out);
    }

    private void hideSleeveTops(List<Fitted> sleeves, List<Fitted> body) {
        TriangleIndex index = TriangleIndex.of(body.stream().map(OutfitFitter::toMesh).toList(), CELL);
        ThreadLocal<TriangleIndex.Query> q = ThreadLocal.withInitial(index::query);
        for (Fitted s : sleeves) {
            double[] src = s.src();
            double[] d = s.dst();
            IntStream.range(0, d.length / 3).parallel().forEach(c -> {
                Vec3 o = new Vec3(src[c * 3], src[c * 3 + 1], src[c * 3 + 2]);
                if (o.y() <= from.armpit || !from.insideTorso(o)) {
                    return;
                }
                Vec3 p = new Vec3(d[c * 3], d[c * 3 + 1], d[c * 3 + 2]);
                TriangleIndex.Hit hit = q.get().nearest(p, 0.04);
                if (hit == null) {
                    return;
                }
                double sd = p.sub(hit.point()).dot(hit.normal());
                if (sd > -0.004) {
                    Vec3 in = p.addScaled(hit.normal(), -(sd + 0.004));
                    d[c * 3] = in.x();
                    d[c * 3 + 1] = in.y();
                    d[c * 3 + 2] = in.z();
                }
            });
        }
    }

    private record Placed(Vec3 p, boolean inside) {
    }

    private Placed transfer(Vec3 p, Region region) {
        return switch (region) {
            case JERSEY -> torso(p, p.y(), true);
            case TORSO -> torso(p, p.y(), false);
            case LEG -> leg(p);
            case ARM -> arm(p);
            case BLEND -> {
                double w = smoothstep(blendTop, blendBottom, p.y());
                double surfaceY = Math.max(p.y(), hipFreeze);
                if (w <= 0) {
                    yield torso(p, surfaceY, false);
                }
                if (w >= 1) {
                    yield leg(p);
                }
                Placed a = torso(p, surfaceY, false);
                Placed b = leg(p);
                Vec3 mid = a.p().lerp(b.p(), w);
                Vec3 c = to.legCenter(p.x() >= 0 ? 1 : -1, mid.y());
                double ra = Math.hypot(a.p().x() - c.x(), a.p().z() - c.z());
                double rb = Math.hypot(b.p().x() - c.x(), b.p().z() - c.z());
                Vec3 h = new Vec3(mid.x() - c.x(), 0, mid.z() - c.z());
                double len = h.length();
                Vec3 q = len < 1e-9 ? mid
                        : new Vec3(c.x() + h.x() / len * (ra + (rb - ra) * w), mid.y(), c.z() + h.z() / len * (ra + (rb - ra) * w));
                yield new Placed(q, w < 0.5 ? a.inside() : b.inside());
            }
            case SHOE -> new Placed(p, false);
        };
    }

    private Placed torso(Vec3 p, double surfaceY, boolean drape) {
        Vec3 axis = from.torsoCenter(p.y());
        double w = smoothstep(from.armpit - 0.03 * from.height(), from.armpit + 0.03 * from.height(), p.y());
        Placed flat = null;
        if (w < 1) {
            double y = mapY(p.y());
            Vec3 axisTo = to.torsoCenter(Math.max(y, to.crotch + 0.01));
            Placed f = drape
                    ? vertical(p, axis, axisTo, dir -> drapedFrom(p.y(), dir), dir -> drapedTo(p.y(), axisTo, dir))
                    : vertical(p, axis, axisTo, dir -> from.torsoRadius(surfaceY, dir), dir -> bridged(axisTo, dir));
            flat = new Placed(new Vec3(f.p().x(), y, f.p().z()), f.inside());
        }
        if (w <= 0) {
            return flat;
        }
        Placed up = shoulder(p);
        if (flat == null) {
            return up;
        }
        Vec3 mid = flat.p().lerp(up.p(), w);
        Vec3 c = to.torsoCenter(mid.y());
        Vec3 v = mid.sub(c);
        double len = v.length();
        if (len < 1e-9) {
            return new Placed(mid, w < 0.5 ? flat.inside() : up.inside());
        }
        double r = flat.p().sub(c).length() * (1 - w) + up.p().sub(c).length() * w;
        return new Placed(c.addScaled(v.mul(1 / len), r), w < 0.5 ? flat.inside() : up.inside());
    }

    private Placed shoulder(Vec3 p) {
        Vec3 c = from.torsoCenter(from.armpit - 0.15 * from.height());
        Vec3 ct = to.torsoCenter(to.armpit - 0.15 * to.height());
        Vec3 v = p.sub(c);
        double d = v.length();
        if (d < 1e-6) {
            return new Placed(ct, true);
        }
        Vec3 dir = v.mul(1 / d);
        double side = Math.abs(dir.x()) / Math.max(1e-6, Math.hypot(dir.x(), dir.z()));
        double tolerance = side > 0.6 ? 1.35 : 2.2;
        double exit = from.torsoExit(c, dir);
        double[] r = radius(Double.isNaN(exit) ? exit : Math.min(exit, d), d, skinQ.get().ray(ct, dir, 0.8), tolerance,
                side > 0.6 ? SHOULDER_MAX_EASE : MAX_EASE);
        Vec3 q = ct.addScaled(dir, r[0]);
        double cap = mapY(p.y()) + 0.025;
        double t = smoothstep(cap - 0.01, cap + 0.01, q.y());
        if (t <= 0) {
            return new Placed(q, false);
        }
        Vec3 axisTo = to.torsoCenter(cap);
        Placed flat = vertical(p, from.torsoCenter(p.y()), axisTo, dirH -> from.torsoRadius(p.y(), dirH),
                dirH -> bridged(axisTo, dirH));
        Vec3 f = new Vec3(flat.p().x(), Math.min(cap, q.y()), flat.p().z());
        Vec3 mid = q.lerp(f, t);
        Vec3 c0 = to.torsoCenter(mid.y());
        Vec3 h = new Vec3(mid.x() - c0.x(), 0, mid.z() - c0.z());
        double len = h.length();
        if (len < 1e-9) {
            return new Placed(mid, false);
        }
        double rq = Math.hypot(q.x() - c0.x(), q.z() - c0.z());
        double rf = Math.hypot(f.x() - c0.x(), f.z() - c0.z());
        double rr = rq + (rf - rq) * t;
        return new Placed(new Vec3(c0.x() + h.x() / len * rr, mid.y(), c0.z() + h.z() / len * rr), false);
    }

    private Placed leg(Vec3 p) {
        int sign = p.x() >= 0 ? 1 : -1;
        Vec3 axisTo = to.legCenter(sign, mapY(p.y()));
        return vertical(p, from.legCenter(sign, p.y()), axisTo, dir -> from.legRadius(sign, p.y(), dir),
                dir -> bridged(axisTo, dir));
    }

    private double drapedFrom(double y, Vec3 dir) {
        double rb = from.torsoRadius(y, dir);
        double rc = from.torsoRadius(drapeTop, dir) * 0.985;
        double k = 0.62 * smoothstep(drapeTop, drapeBottom, y);
        return Math.max(rb, rb + (rc - rb) * k);
    }

    private double drapedTo(double yFrom, Vec3 axisTo, Vec3 dir) {
        double rb = bridged(axisTo, dir);
        if (Double.isNaN(rb) || yFrom >= drapeTop + 0.08 * from.height()) {
            return rb;
        }
        double hang = rb;
        double top = to.armpit - 0.02 * to.height();
        for (double y = Math.ceil(axisTo.y() * 100) / 100; y <= top + 1e-9; y += 0.01) {
            double r = bridged(to.torsoCenter(y), dir);
            if (!Double.isNaN(r)) {
                hang = Math.max(hang, r * 0.985 - DRAPE_SLOPE * (y - axisTo.y()));
            }
        }
        return hang;
    }

    private double bridged(Vec3 origin, Vec3 dir) {
        double[][] hull = sections.computeIfAbsent(List.of(Math.round(origin.x() * 1000), Math.round(origin.y() * 1000),
                Math.round(origin.z() * 1000)), k -> section(origin));
        if (hull.length < 3) {
            return skinQ.get().ray(origin, dir, 0.6);
        }
        double dx = dir.x();
        double dz = dir.z();
        double best = Double.NaN;
        for (int i = 0; i < hull.length; i++) {
            double[] a = hull[i];
            double[] b = hull[(i + 1) % hull.length];
            double ex = b[0] - a[0];
            double ez = b[1] - a[1];
            double den = dx * ez - dz * ex;
            if (Math.abs(den) < 1e-12) {
                continue;
            }
            double t = (a[0] * ez - a[1] * ex) / den;
            double u = (a[0] * dz - a[1] * dx) / den;
            if (t > 0 && u >= -1e-9 && u <= 1 + 1e-9 && (Double.isNaN(best) || t > best)) {
                best = t;
            }
        }
        return Double.isNaN(best) ? skinQ.get().ray(origin, dir, 0.6) : best;
    }

    private final java.util.concurrent.ConcurrentHashMap<List<Long>, double[][]> sections =
            new java.util.concurrent.ConcurrentHashMap<>();

    private double[][] section(Vec3 origin) {
        List<double[]> pts = new ArrayList<>();
        TriangleIndex.Query q = skinQ.get();
        for (int k = 0; k < 72; k++) {
            double a = 2 * Math.PI * k / 72;
            double r = q.ray(origin, new Vec3(Math.sin(a), 0, Math.cos(a)), 0.6);
            if (!Double.isNaN(r)) {
                pts.add(new double[]{Math.sin(a) * r, Math.cos(a) * r});
            }
        }
        if (pts.size() < 3) {
            return new double[0][];
        }
        pts.sort((p1, p2) -> p1[0] != p2[0] ? Double.compare(p1[0], p2[0]) : Double.compare(p1[1], p2[1]));
        double[][] h = new double[pts.size() * 2][];
        int n = 0;
        for (int pass = 0; pass < 2; pass++) {
            int start = n;
            for (int idx = 0; idx < pts.size(); idx++) {
                double[] pt = pts.get(pass == 0 ? idx : pts.size() - 1 - idx);
                while (n >= start + 2 && cross(h[n - 2], h[n - 1], pt) <= 0) {
                    n--;
                }
                h[n++] = pt;
            }
            n--;
        }
        return Arrays.copyOf(h, n);
    }

    private static double cross(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    private Placed vertical(Vec3 p, Vec3 axisFrom, Vec3 axisTo, java.util.function.ToDoubleFunction<Vec3> surfaceFrom,
                            java.util.function.ToDoubleFunction<Vec3> surfaceTo) {
        Vec3 h = new Vec3(p.x() - axisFrom.x(), 0, p.z() - axisFrom.z());
        double d = h.length();
        double y = axisTo.y();
        if (d < 1e-6) {
            return new Placed(new Vec3(axisTo.x(), y, axisTo.z()), true);
        }
        Vec3 dir = h.mul(1 / d);
        double[] r = radius(surfaceFrom.applyAsDouble(dir), d, surfaceTo.applyAsDouble(dir), 2.2, MAX_EASE);
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
        double tube = SLEEVE_TUBE * to.height();
        double measureAt = Math.max(sh, tube);
        Vec3 cm = to.armPoint(sign, measureAt);
        Vec3[] fm = frame(to.armDirection(sign, measureAt));
        Vec3 dirM = fm[0].mul(Math.cos(angle)).add(fm[1].mul(Math.sin(angle)));
        double sFrom = sh >= tube ? s : tube / ratio;
        double[] r = radius(from.armRadius(sign, sFrom, dirFrom), d, skinQ.get().ray(cm, dirM, 0.3), 1.35, MAX_EASE);
        double flare = SLEEVE_FLARE * Math.max(0, sh - tube);
        return new Placed(ch.addScaled(dirTo, r[0] + (r[1] > 0 ? 0 : flare)), r[1] > 0);
    }

    private double[] radius(double rFrom, double d, double measured, double tolerance, double maxEase) {
        boolean inside = !Double.isNaN(rFrom) && d < rFrom - 0.001;
        double e = Double.isNaN(rFrom) ? 0.008 : clamp(d - rFrom, MIN_EASE, maxEase);
        double expected = (Double.isNaN(rFrom) ? d - e : rFrom) * ratio;
        double rTo = measured;
        if (Double.isNaN(rTo) || rTo < 0.4 * expected || rTo > tolerance * expected + 0.03) {
            rTo = expected;
        }
        if (inside) {
            return new double[]{rTo * Math.max(0, d) / rFrom, 1};
        }
        return new double[]{rTo + e, 0};
    }

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

    private static Vec3[] frame(Vec3 axis) {
        Vec3 f = Vec3.FRONT.sub(axis.mul(Vec3.FRONT.dot(axis)));
        if (f.length() < 1e-6) {
            f = new Vec3(1, 0, 0).sub(axis.mul(axis.x()));
        }
        f = f.normalize();
        return new Vec3[]{f, axis.cross(f).normalize()};
    }

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
            if (acc[i] * old[i] + acc[i + 1] * old[i + 1] + acc[i + 2] * old[i + 2] < 0) {
                len = -len;
            }
            if (Math.abs(len) < 1e-12) {
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
