package com.sportforce3d.three.human;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.model.PgeyModelProviders;
import com.sportforce3d.three.model.PgeyZone;
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

    private static final Map<PgeyPersona, Fitted> FITS = new EnumMap<>(PgeyPersona.class);

    private record Fitted(PgeyHumanModel human, PgeyModelData source, PgeyOutfitFitter.PgeyDressedPerson dressed, PgeyTriangleIndex skin) {
    }

    @BeforeAll
    static void fitAll() {
        for (PgeyPersona p : PgeyPersona.values()) {
            PgeyHumanModel human = geometryOnly(p);
            PgeyModelData source = PgeyModelProviders.pgeyLoad(p.pgeyFallback());
            PgeyOutfitFitter.PgeyDressedPerson dressed = PgeyOutfitFitter.pgeyFit(human, source);
            FITS.put(p, new Fitted(human, source, dressed,
                    PgeyTriangleIndex.pgeyOf(List.of(PgeyOutfitFitter.pgeySkinOf(human).pgeyMesh()), 0.02)));
        }
    }

    static PgeyHumanModel geometryOnly(PgeyPersona p) {
        PgeyObjMtlReader.PgeyResult r = PgeyHumanModels.pgeyReadGeometry(p);
        List<PgeyHumanModel.PgeyPart> parts = new ArrayList<>();
        int skin = 0;
        for (int i = 1; i < r.pgeyGroups().size(); i++) {
            if (r.pgeyGroups().get(i).pgeyMesh().pgeyFaceCount() > r.pgeyGroups().get(skin).pgeyMesh().pgeyFaceCount()) {
                skin = i;
            }
        }
        for (int i = 0; i < r.pgeyGroups().size(); i++) {
            PgeyObjMtlReader.PgeyGroup g = r.pgeyGroups().get(i);
            parts.add(new PgeyHumanModel.PgeyPart(g.pgeyMaterial(), g.pgeyMesh(), Color.WHITE, Color.GRAY, 20, null, i != skin));
        }
        return new PgeyHumanModel(p, parts, r.pgeyMin()[1], r.pgeyHeight());
    }

    private static double[] stats(Fitted f, PgeyZone zone) {
        PgeyTriangleIndex.PgeyQuery q = f.skin().pgeyQuery();
        PgeyTriangleIndex.PgeyQuery body = PgeyTriangleIndex.pgeyOf(f.dressed().pgeyOutfit().pgeyZones().get(PgeyZone.JERSEY_BODY).stream()
                .map(PgeyModelData.PgeyPart::pgeyMesh).toList(), 0.02).pgeyQuery();
        int outside = 0;
        int n = 0;
        double far = 0;
        for (var part : f.dressed().pgeyOutfit().pgeyZones().get(zone)) {
            float[] p = part.pgeyMesh().pgeyPoints();
            for (int i = 0; i < p.length; i += 3) {
                PgeyVec3 v = new PgeyVec3(p[i], -p[i + 1], -p[i + 2]);
                PgeyTriangleIndex.PgeyHit hit = q.pgeyNearest(v, 0.12);
                n++;
                if (hit == null) {
                    far = 0.12;
                    outside++;
                } else if (v.pgeySub(hit.pgeyPoint()).pgeyDot(hit.pgeyNormal()) > -0.0005) {
                    outside++;
                    far = Math.max(far, hit.pgeyDistance());
                } else if (zone == PgeyZone.JERSEY_SLEEVES) {
                    PgeyTriangleIndex.PgeyHit b = body.pgeyNearest(v, 0.03);
                    if (b != null && v.pgeySub(b.pgeyPoint()).pgeyDot(b.pgeyNormal()) < 0) {
                        outside++;
                    }
                }
            }
        }
        return new double[]{(double) outside / n, far};
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void conservaLasMallasYLasIslasUv(PgeyPersona p) {
        Fitted f = FITS.get(p);
        assertThat(f.dressed().pgeyOutfit().pgeyZones().keySet()).isEqualTo(f.source().pgeyZones().keySet());
        f.source().pgeyZones().forEach((zone, parts) -> {
            for (int i = 0; i < parts.size(); i++) {
                PgeyMeshData a = parts.get(i).pgeyMesh();
                PgeyMeshData b = f.dressed().pgeyOutfit().pgeyZones().get(zone).get(i).pgeyMesh();
                assertThat(b.pgeyPointCount()).as("%s puntos", zone).isGreaterThanOrEqualTo(a.pgeyPointCount());
                assertThat(b.pgeyTexCoords()).as("%s UV", zone).isEqualTo(a.pgeyTexCoords());
                if (zone == PgeyZone.JERSEY_BODY || zone == PgeyZone.SHORTS_BASE) {
                    assertThat(b.pgeyPointCount()).as("%s con basta", zone).isGreaterThan(a.pgeyPointCount());
                    assertThat(java.util.Arrays.copyOf(b.pgeyFaces(), a.pgeyFaces().length)).as("%s caras", zone)
                            .isEqualTo(a.pgeyFaces());
                } else if (zone.pgeyGarment() == HepyGarment.SOCKS) {
                    assertThat(b.pgeyFaceCount()).isLessThanOrEqualTo(a.pgeyFaceCount()).isGreaterThan(a.pgeyFaceCount() / 2);
                } else {
                    assertThat(b.pgeyFaces()).as("%s caras", zone).isEqualTo(a.pgeyFaces());
                }
            }
        });
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void laTelaQuedaPorFueraDeLaPielYSinPicos(PgeyPersona p) {
        Fitted f = FITS.get(p);
        for (PgeyZone z : PgeyZone.values()) {
            if (z.pgeyGarment() == HepyGarment.SHOES) {
                continue;
            }
            double[] s = stats(f, z);
            double min = z == PgeyZone.JERSEY_BODY ? 0.99 : z == PgeyZone.JERSEY_SLEEVES ? 0.95 : 0.995;
            if (z != PgeyZone.SHORTS_BASE) {
                assertThat(s[0]).as("%s %s: vértices por fuera de la piel", p, z).isGreaterThanOrEqualTo(min);
            }
            assertThat(s[1]).as("%s %s: distancia máxima a la piel (m)", p, z).isLessThan(0.07);
        }
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void laPielNoAsomaPorLaRopa(PgeyPersona p) {
        Fitted f = FITS.get(p);
        Map<PgeyZone, List<PgeyModelData.PgeyPart>> z = f.dressed().pgeyOutfit().pgeyZones();
        List<PgeyMeshData> cloth = new ArrayList<>();
        for (PgeyZone zone : List.of(PgeyZone.JERSEY_BODY, PgeyZone.JERSEY_SIDES, PgeyZone.SHORTS_BASE, PgeyZone.SHORTS_STRIPE,
                PgeyZone.SHORTS_WAIST)) {
            z.get(zone).forEach(part -> cloth.add(part.pgeyMesh()));
        }
        PgeyTriangleIndex.PgeyQuery clothQ = PgeyTriangleIndex.pgeyOf(cloth, 0.02).pgeyQuery();
        PgeyTriangleIndex skinIndex = PgeyTriangleIndex.pgeyOf(List.of(PgeyOutfitFitter.pgeySkinOf(f.dressed().pgeyBody()).pgeyMesh()), 0.02);
        PgeyTriangleIndex.PgeyQuery skinQ = skinIndex.pgeyQuery();
        double[] range = heightRange(z.get(PgeyZone.SHORTS_BASE), z.get(PgeyZone.JERSEY_BODY));
        int rays = 0;
        int skinFirst = 0;
        for (double y = range[0] + 0.015; y < range[1] - 0.03; y += 0.01) {
            for (int a = -50; a <= 50; a += 5) {
                for (int back = 0; back < 2; back++) {
                    double ang = Math.toRadians(a + back * 180);
                    PgeyVec3 dir = new PgeyVec3(-Math.sin(ang), 0, -Math.cos(ang));
                    PgeyVec3 origin = new PgeyVec3(0, y, 0).pgeyAddScaled(dir, -1.0);
                    double c = clothQ.pgeyRay(origin, dir, 1.0);
                    double sk = skinQ.pgeyRay(origin, dir, 1.0);
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

    private static double[] heightRange(List<PgeyModelData.PgeyPart> shorts, List<PgeyModelData.PgeyPart> jersey) {
        double lo = -Double.MAX_VALUE;
        for (var part : shorts) {
            float[] pts = part.pgeyMesh().pgeyPoints();
            double min = Double.MAX_VALUE;
            for (int i = 0; i < pts.length; i += 3) {
                min = Math.min(min, -pts[i + 1]);
            }
            lo = Math.max(lo, min);
        }
        double hi = 0;
        for (var part : jersey) {
            float[] pts = part.pgeyMesh().pgeyPoints();
            for (int i = 0; i < pts.length; i += 3) {
                hi = Math.max(hi, -pts[i + 1]);
            }
        }
        return new double[]{lo, lo + (hi - lo) * 0.8};
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void lasZapatillasCubrenLosPiesYLaPielTapadaSeOculta(PgeyPersona p) {
        Fitted f = FITS.get(p);
        double[] v = f.skin().pgeyVertices();
        double footTop = f.human().pgeyMinY() + 0.025 * f.human().pgeyHeight();
        for (int sign : new int[]{-1, 1}) {
            double[] shoe = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
            for (PgeyZone z : List.of(PgeyZone.SHOE_UPPER, PgeyZone.SHOE_SOLE, PgeyZone.SHOE_TOE, PgeyZone.SHOE_HEEL)) {
                for (var part : f.dressed().pgeyOutfit().pgeyZones().get(z)) {
                    float[] pts = part.pgeyMesh().pgeyPoints();
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
        int before = PgeyOutfitFitter.pgeySkinOf(f.human()).pgeyMesh().pgeyFaceCount();
        int after = PgeyOutfitFitter.pgeySkinOf(f.dressed().pgeyBody()).pgeyMesh().pgeyFaceCount();
        assertThat(after).as("piel oculta bajo la ropa y en los pies").isLessThan(before).isGreaterThan(before / 2);
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void lasMedidasDelCuerpoSonPlausibles(PgeyPersona p) {
        Fitted f = FITS.get(p);
        PgeyBodyFrame b = PgeyBodyFrame.pgeyMeasure(f.skin().pgeyVertices(), f.skin().pgeyTriangles(), f.human().pgeyMinY(), f.human().pgeyHeight());
        double h = b.pgeyHeight();
        assertThat((b.pgeyCrotch - b.pgeyFloor) / h).isBetween(0.40, 0.52);
        assertThat((b.pgeyArmpit - b.pgeyFloor) / h).isBetween(0.68, 0.80);
        assertThat(b.pgeyNeck).isBetween(b.pgeyArmpit + 0.03 * h, b.pgeyTop - 0.12 * h);
        assertThat(b.pgeyArmDirection(1, 0).pgeyX()).isGreaterThan(0.3);
        assertThat(b.pgeyArmDirection(-1, 0).pgeyX()).isLessThan(-0.3);
        assertThat(b.pgeyArmDirection(1, 0).pgeyY()).isLessThan(-0.3);
    }
}
