package com.sportforce3d.three.human;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.model.ModelProviders;
import com.sportforce3d.three.model.Zone;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OutfitFitterTest {

    private static final Map<Persona, Fitted> FITS = new EnumMap<>(Persona.class);

    private record Fitted(HumanModel human, ModelData source, OutfitFitter.DressedPerson dressed, TriangleIndex skin) {
    }

    @BeforeAll
    static void fitAll() {
        for (Persona p : Persona.values()) {
            HumanModel human = geometryOnly(p);
            ModelData source = ModelProviders.load(p.fallback());
            OutfitFitter.DressedPerson dressed = OutfitFitter.fit(human, source);
            FITS.put(p, new Fitted(human, source, dressed,
                    TriangleIndex.of(List.of(OutfitFitter.skinOf(human).mesh()), 0.02)));
        }
    }

    static HumanModel geometryOnly(Persona p) {
        ObjMtlReader.Result r = HumanModels.readGeometry(p);
        List<HumanModel.Part> parts = new ArrayList<>();
        int skin = 0;
        for (int i = 1; i < r.groups().size(); i++) {
            if (r.groups().get(i).mesh().faceCount() > r.groups().get(skin).mesh().faceCount()) {
                skin = i;
            }
        }
        for (int i = 0; i < r.groups().size(); i++) {
            ObjMtlReader.Group g = r.groups().get(i);
            parts.add(new HumanModel.Part(g.material(), g.mesh(), Color.WHITE, Color.GRAY, 20, null, i != skin));
        }
        return new HumanModel(p, parts, r.min()[1], r.height());
    }

    private static double[] stats(Fitted f, Zone zone) {
        TriangleIndex.Query q = f.skin().query();
        TriangleIndex.Query body = TriangleIndex.of(f.dressed().outfit().zones().get(Zone.JERSEY_BODY).stream()
                .map(ModelData.Part::mesh).toList(), 0.02).query();
        int outside = 0;
        int n = 0;
        double far = 0;
        for (var part : f.dressed().outfit().zones().get(zone)) {
            float[] p = part.mesh().points();
            for (int i = 0; i < p.length; i += 3) {
                Vec3 v = new Vec3(p[i], -p[i + 1], -p[i + 2]);
                TriangleIndex.Hit hit = q.nearest(v, 0.12);
                n++;
                if (hit == null) {
                    far = 0.12;
                    outside++;
                } else if (v.sub(hit.point()).dot(hit.normal()) > -0.0005) {
                    outside++;
                    far = Math.max(far, hit.distance());
                } else if (zone == Zone.JERSEY_SLEEVES) {
                    TriangleIndex.Hit b = body.nearest(v, 0.03);
                    if (b != null && v.sub(b.point()).dot(b.normal()) < 0) {
                        outside++;
                    }
                }
            }
        }
        return new double[]{(double) outside / n, far};
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void conservaLasMallasYLasIslasUv(Persona p) {
        Fitted f = FITS.get(p);
        assertThat(f.dressed().outfit().zones().keySet()).isEqualTo(f.source().zones().keySet());
        f.source().zones().forEach((zone, parts) -> {
            for (int i = 0; i < parts.size(); i++) {
                MeshData a = parts.get(i).mesh();
                MeshData b = f.dressed().outfit().zones().get(zone).get(i).mesh();
                assertThat(b.pointCount()).as("%s puntos", zone).isGreaterThanOrEqualTo(a.pointCount());
                assertThat(b.texCoords()).as("%s UV", zone).isEqualTo(a.texCoords());
                if (zone == Zone.JERSEY_BODY || zone == Zone.SHORTS_BASE) {
                    assertThat(b.pointCount()).as("%s con basta", zone).isGreaterThan(a.pointCount());
                    assertThat(java.util.Arrays.copyOf(b.faces(), a.faces().length)).as("%s caras", zone)
                            .isEqualTo(a.faces());
                } else if (zone.garment() == Garment.SOCKS) {
                    assertThat(b.faceCount()).isLessThanOrEqualTo(a.faceCount()).isGreaterThan(a.faceCount() / 2);
                } else {
                    assertThat(b.faces()).as("%s caras", zone).isEqualTo(a.faces());
                }
            }
        });
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void laTelaQuedaPorFueraDeLaPielYSinPicos(Persona p) {
        Fitted f = FITS.get(p);
        for (Zone z : Zone.values()) {
            if (z.garment() == Garment.SHOES) {
                continue;
            }
            double[] s = stats(f, z);
            double min = z == Zone.JERSEY_BODY ? 0.99 : z == Zone.JERSEY_SLEEVES ? 0.95 : 0.995;
            if (z != Zone.SHORTS_BASE) {
                assertThat(s[0]).as("%s %s: vértices por fuera de la piel", p, z).isGreaterThanOrEqualTo(min);
            }
            assertThat(s[1]).as("%s %s: distancia máxima a la piel (m)", p, z).isLessThan(0.07);
        }
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void laPielNoAsomaPorLaRopa(Persona p) {
        Fitted f = FITS.get(p);
        Map<Zone, List<ModelData.Part>> z = f.dressed().outfit().zones();
        List<MeshData> cloth = new ArrayList<>();
        for (Zone zone : List.of(Zone.JERSEY_BODY, Zone.JERSEY_SIDES, Zone.SHORTS_BASE, Zone.SHORTS_STRIPE,
                Zone.SHORTS_WAIST)) {
            z.get(zone).forEach(part -> cloth.add(part.mesh()));
        }
        TriangleIndex.Query clothQ = TriangleIndex.of(cloth, 0.02).query();
        TriangleIndex skinIndex = TriangleIndex.of(List.of(OutfitFitter.skinOf(f.dressed().body()).mesh()), 0.02);
        TriangleIndex.Query skinQ = skinIndex.query();
        double[] range = heightRange(z.get(Zone.SHORTS_BASE), z.get(Zone.JERSEY_BODY));
        int rays = 0;
        int skinFirst = 0;
        for (double y = range[0] + 0.015; y < range[1] - 0.03; y += 0.01) {
            for (int a = -50; a <= 50; a += 5) {
                for (int back = 0; back < 2; back++) {
                    double ang = Math.toRadians(a + back * 180);
                    Vec3 dir = new Vec3(-Math.sin(ang), 0, -Math.cos(ang));
                    Vec3 origin = new Vec3(0, y, 0).addScaled(dir, -1.0);
                    double c = clothQ.ray(origin, dir, 1.0);
                    double sk = skinQ.ray(origin, dir, 1.0);
                    if (Double.isNaN(c) && Double.isNaN(sk)) {
                        continue;
                    }
                    rays++;
                    if (!Double.isNaN(sk) && (Double.isNaN(c) || sk < c - 0.001)) {
                        skinFirst++;
                    }
                }
            }
        }
        assertThat(rays).isGreaterThan(500);
        assertThat((double) skinFirst / rays).as("%s: rayos que ven piel antes que tela", p).isLessThan(0.01);
    }

    private static double[] heightRange(List<ModelData.Part> shorts, List<ModelData.Part> jersey) {
        double lo = -Double.MAX_VALUE;
        for (var part : shorts) {
            float[] pts = part.mesh().points();
            double min = Double.MAX_VALUE;
            for (int i = 0; i < pts.length; i += 3) {
                min = Math.min(min, -pts[i + 1]);
            }
            lo = Math.max(lo, min);
        }
        double hi = 0;
        for (var part : jersey) {
            float[] pts = part.mesh().points();
            for (int i = 0; i < pts.length; i += 3) {
                hi = Math.max(hi, -pts[i + 1]);
            }
        }
        return new double[]{lo, lo + (hi - lo) * 0.8};
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void lasZapatillasCubrenLosPiesYLaPielTapadaSeOculta(Persona p) {
        Fitted f = FITS.get(p);
        double[] v = f.skin().vertices();
        double footTop = f.human().minY() + 0.025 * f.human().height();
        for (int sign : new int[]{-1, 1}) {
            double[] shoe = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
            for (Zone z : List.of(Zone.SHOE_UPPER, Zone.SHOE_SOLE, Zone.SHOE_TOE, Zone.SHOE_HEEL)) {
                for (var part : f.dressed().outfit().zones().get(z)) {
                    float[] pts = part.mesh().points();
                    for (int i = 0; i < pts.length; i += 3) {
                        if (pts[i] * sign > 0) {
                            shoe[0] = Math.min(shoe[0], pts[i]);
                            shoe[1] = Math.max(shoe[1], pts[i]);
                            shoe[2] = Math.min(shoe[2], -pts[i + 2]);
                            shoe[3] = Math.max(shoe[3], -pts[i + 2]);
                        }
                    }
                }
            }
            for (int i = 0; i < v.length; i += 3) {
                if (v[i + 1] < footTop && v[i] * sign > 0 && Math.abs(v[i]) < 0.25) {
                    assertThat(v[i]).as("%s: pie dentro del ancho de la zapatilla", p).isBetween(shoe[0], shoe[1]);
                    assertThat(v[i + 2]).as("%s: pie dentro del largo de la zapatilla", p).isBetween(shoe[2], shoe[3]);
                }
            }
        }
        int before = OutfitFitter.skinOf(f.human()).mesh().faceCount();
        int after = OutfitFitter.skinOf(f.dressed().body()).mesh().faceCount();
        assertThat(after).as("piel oculta bajo la ropa y en los pies").isLessThan(before).isGreaterThan(before / 2);
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void lasMedidasDelCuerpoSonPlausibles(Persona p) {
        Fitted f = FITS.get(p);
        BodyFrame b = BodyFrame.measure(f.skin().vertices(), f.skin().triangles(), f.human().minY(), f.human().height());
        double h = b.height();
        assertThat((b.crotch - b.floor) / h).isBetween(0.40, 0.52);
        assertThat((b.armpit - b.floor) / h).isBetween(0.68, 0.80);
        assertThat(b.neck).isBetween(b.armpit + 0.03 * h, b.top - 0.12 * h);
        assertThat(b.armDirection(1, 0).x()).isGreaterThan(0.3);
        assertThat(b.armDirection(-1, 0).x()).isLessThan(-0.3);
        assertThat(b.armDirection(1, 0).y()).isLessThan(-0.3);
    }
}
