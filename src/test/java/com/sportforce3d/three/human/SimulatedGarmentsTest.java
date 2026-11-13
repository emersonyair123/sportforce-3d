package com.sportforce3d.three.human;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyUvRect;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.model.PgeyModelProviders;
import com.sportforce3d.three.model.PgeyUvLayout;
import com.sportforce3d.three.model.PgeyZone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedGarmentsTest {

    private record Dressed(Map<HepyGarment, PgeySimulatedGarments.PgeyPiece> pieces, PgeyHumanModel human,
                           PgeyOutfitFitter.PgeyDressedPerson dressed) {
    }

    private static final Map<PgeyPersona, Dressed> ALL = new EnumMap<>(PgeyPersona.class);

    @BeforeAll
    static void load() {
        for (PgeyPersona p : PgeyPersona.values()) {
            var pieces = PgeySimulatedGarments.pgeyLoad(p);
            PgeyHumanModel human = OutfitFitterTest.geometryOnly(p);
            ALL.put(p, new Dressed(pieces, human,
                    PgeyOutfitFitter.pgeyFit(human, PgeyModelProviders.pgeyLoad(p.pgeyFallback()), pieces)));
        }
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void cadaPersonaTieneCamisetaShortYMediasSimulados(PgeyPersona p) {
        var pieces = ALL.get(p).pieces();
        for (HepyGarment g : List.of(HepyGarment.JERSEY, HepyGarment.SHORTS, HepyGarment.SOCKS)) {
            assertThat(PgeySimulatedGarments.pgeyAvailable(p, g)).as("%s %s", p, g).isTrue();
        }
        assertThat(pieces).containsOnlyKeys(HepyGarment.JERSEY, HepyGarment.SHORTS, HepyGarment.SOCKS);
        assertThat(pieces.get(HepyGarment.JERSEY).pgeyZones()).containsOnlyKeys(PgeyZone.JERSEY_BODY, PgeyZone.JERSEY_SLEEVES,
                PgeyZone.JERSEY_COLLAR, PgeyZone.JERSEY_CUFFS, PgeyZone.JERSEY_SIDES);
        assertThat(pieces.get(HepyGarment.SHORTS).pgeyZones()).containsOnlyKeys(PgeyZone.SHORTS_BASE, PgeyZone.SHORTS_STRIPE,
                PgeyZone.SHORTS_WAIST);
        assertThat(pieces.get(HepyGarment.SOCKS).pgeyZones()).containsOnlyKeys(PgeyZone.SOCKS_LEG, PgeyZone.SOCKS_BAND);
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void lasUvDeCadaZonaQuedanEnSusIslas(PgeyPersona p) {
        ALL.get(p).pieces().values().forEach(piece -> piece.pgeyZones().forEach((zone, parts) -> {
            List<PgeyUvRect> islands = PgeyUvLayout.PGEY_BY_ZONE.get(zone);
            for (PgeyModelData.PgeyPart part : parts) {
                float[] tex = part.pgeyMesh().pgeyTexCoords();
                for (int i = 0; i < tex.length; i += 2) {
                    double u = tex[i];
                    double v = tex[i + 1];
                    boolean inside = islands.stream().anyMatch(r -> u >= r.pgeyU0() - 1e-3 && u <= r.pgeyU1() + 1e-3
                            && v >= r.pgeyV0() - 1e-3 && v <= r.pgeyV1() + 1e-3);
                    assertThat(inside).as("%s %s: uv (%.4f, %.4f) fuera de sus islas", p, zone, u, v).isTrue();
                }
            }
        }));
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void elSombreadoHorneadoTieneOclusionYNormalMap(PgeyPersona p) {
        for (PgeySimulatedGarments.PgeyPiece piece : ALL.get(p).pieces().values()) {
            PgeyClothShading s = piece.pgeyShading();
            assertThat(s.pgeyNormals()).isNotNull().hasSize(s.pgeySize() * s.pgeySize());
            double min = 1;
            double max = 0;
            for (float v : s.pgeyShade()) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            assertThat(min).isGreaterThanOrEqualTo(0.39);
            assertThat(max).isLessThanOrEqualTo(1.0001).isGreaterThan(0.95);
        }
        PgeyClothShading jersey = ALL.get(p).pieces().get(HepyGarment.JERSEY).pgeyShading();
        double u = PgeyUvLayout.PGEY_JERSEY_FRONT.pgeyU(0.5);
        double v = PgeyUvLayout.PGEY_JERSEY_FRONT.pgeyV(0.6);
        assertThat(jersey.pgeyShadeAt(u, v)).isGreaterThan(0.85);
        assertThat(jersey.pgeyNormalAt(u, v)[2]).isGreaterThan(0.8);
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void losBordesDeLaTelaSonAberturas(PgeyPersona p) {
        Dressed d = ALL.get(p);
        List<PgeyVec3> openings = d.pieces().get(HepyGarment.JERSEY).pgeyOpenings();
        double floor = d.human().pgeyMinY();
        double h = d.human().pgeyHeight();
        assertThat(openings).anyMatch(q -> q.pgeyY() > floor + 0.80 * h);
        assertThat(openings).anyMatch(q -> q.pgeyY() < floor + 0.56 * h && Math.abs(q.pgeyX()) < 0.05 * h);
        assertThat(openings).anyMatch(q -> Math.abs(q.pgeyX()) > 0.16 * h);
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void laTelaQuedaPorFueraDeLaPiel(PgeyPersona p) {
        Dressed d = ALL.get(p);
        PgeyTriangleIndex.PgeyQuery skin = PgeyTriangleIndex.pgeyOf(List.of(PgeyOutfitFitter.pgeySkinOf(d.human()).pgeyMesh()), 0.02).pgeyQuery();
        d.pieces().forEach((g, piece) -> {
            int n = 0;
            int outside = 0;
            for (List<PgeyModelData.PgeyPart> parts : piece.pgeyZones().values()) {
                for (PgeyModelData.PgeyPart part : parts) {
                    float[] pts = part.pgeyMesh().pgeyPoints();
                    for (int i = 0; i < pts.length; i += 3) {
                        PgeyVec3 q = new PgeyVec3(pts[i], -pts[i + 1], -pts[i + 2]);
                        PgeyTriangleIndex.PgeyHit hit = skin.pgeyNearest(q, 0.12);
                        n++;
                        if (hit == null || q.pgeySub(hit.pgeyPoint()).pgeyDot(hit.pgeyNormal()) > -0.0005 || !inside(skin, q)) {
                            outside++;
                        }
                    }
                }
            }
            assertThat((double) outside / n).as("%s %s fuera de la piel", p, g).isGreaterThan(0.995);
        });
    }

    static boolean inside(PgeyTriangleIndex.PgeyQuery skin, PgeyVec3 p) {
        int votes = 0;
        for (PgeyVec3 dir : List.of(new PgeyVec3(1, 0, 0), new PgeyVec3(-1, 0, 0), new PgeyVec3(0, 0, 1))) {
            PgeyVec3 o = p;
            int crossings = 0;
            double left = 0.6;
            while (left > 0) {
                double d = skin.pgeyRay(o, dir, left);
                if (Double.isNaN(d)) {
                    break;
                }
                crossings++;
                o = o.pgeyAddScaled(dir, d + 1e-5);
                left -= d + 1e-5;
            }
            votes += crossings % 2;
        }
        return votes >= 2;
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void alVestirseUsanLasPrendasSimuladasYLaPielTapadaSeOculta(PgeyPersona p) {
        Dressed d = ALL.get(p);
        Map<PgeyZone, List<PgeyModelData.PgeyPart>> zones = d.dressed().pgeyOutfit().pgeyZones();
        assertThat(zones.get(PgeyZone.JERSEY_BODY)).isSameAs(d.pieces().get(HepyGarment.JERSEY).pgeyZones().get(PgeyZone.JERSEY_BODY));
        assertThat(zones).containsKeys(PgeyZone.SOCKS_LEG, PgeyZone.SHOE_UPPER);
        assertThat(d.dressed().pgeyShading().get(HepyGarment.JERSEY)).isSameAs(d.pieces().get(HepyGarment.JERSEY).pgeyShading());
        assertThat(d.dressed().pgeyShading().get(HepyGarment.SOCKS)).isSameAs(d.pieces().get(HepyGarment.SOCKS).pgeyShading());
        int socks = d.pieces().get(HepyGarment.SOCKS).pgeyZones().get(PgeyZone.SOCKS_LEG).stream()
                .mapToInt(part -> part.pgeyMesh().pgeyFaceCount()).sum();
        assertThat(zones.get(PgeyZone.SOCKS_LEG).stream().mapToInt(part -> part.pgeyMesh().pgeyFaceCount()).sum())
                .isLessThan(socks);

        PgeyMeshData before = PgeyOutfitFitter.pgeySkinOf(d.human()).pgeyMesh();
        PgeyMeshData after = PgeyOutfitFitter.pgeySkinOf(d.dressed().pgeyBody()).pgeyMesh();
        assertThat(after.pgeyFaceCount()).isLessThan((int) (before.pgeyFaceCount() * 0.85));
        List<PgeyVec3> openings = d.pieces().values().stream().flatMap(piece -> piece.pgeyOpenings().stream()).toList();
        double floor = d.human().pgeyMinY();
        double h = d.human().pgeyHeight();
        float[] pts = after.pgeyPoints();
        int[] f = after.pgeyFaces();
        int visibles = 0;
        for (int t = 0; t < after.pgeyFaceCount(); t++) {
            int a = f[t * PgeyMeshData.PGEY_FACE_STRIDE] * 3;
            PgeyVec3 v = new PgeyVec3(pts[a], -pts[a + 1], -pts[a + 2]);
            if (Math.abs(v.pgeyX()) < 0.054 * h && v.pgeyY() > floor + 0.60 * h && v.pgeyY() < floor + 0.72 * h && v.pgeyZ() > 0
                    && openings.stream().noneMatch(o -> o.pgeySub(v).pgeyLength() < 0.035)) {
                visibles++;
            }
        }
        assertThat(visibles).as("%s: piel visible bajo la camiseta", p).isZero();
    }
}
