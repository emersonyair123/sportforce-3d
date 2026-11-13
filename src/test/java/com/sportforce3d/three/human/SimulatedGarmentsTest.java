package com.sportforce3d.three.human;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.UvRect;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.model.ModelProviders;
import com.sportforce3d.three.model.UvLayout;
import com.sportforce3d.three.model.Zone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedGarmentsTest {

    private record Dressed(Map<Garment, SimulatedGarments.Piece> pieces, HumanModel human,
                           OutfitFitter.DressedPerson dressed) {
    }

    private static final Map<Persona, Dressed> ALL = new EnumMap<>(Persona.class);

    @BeforeAll
    static void load() {
        for (Persona p : Persona.values()) {
            var pieces = SimulatedGarments.load(p);
            HumanModel human = OutfitFitterTest.geometryOnly(p);
            ALL.put(p, new Dressed(pieces, human,
                    OutfitFitter.fit(human, ModelProviders.load(p.fallback()), pieces)));
        }
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void cadaPersonaTieneCamisetaShortYMediasSimulados(Persona p) {
        var pieces = ALL.get(p).pieces();
        for (Garment g : List.of(Garment.JERSEY, Garment.SHORTS, Garment.SOCKS)) {
            assertThat(SimulatedGarments.available(p, g)).as("%s %s", p, g).isTrue();
        }
        assertThat(pieces).containsOnlyKeys(Garment.JERSEY, Garment.SHORTS, Garment.SOCKS);
        assertThat(pieces.get(Garment.JERSEY).zones()).containsOnlyKeys(Zone.JERSEY_BODY, Zone.JERSEY_SLEEVES,
                Zone.JERSEY_COLLAR, Zone.JERSEY_CUFFS, Zone.JERSEY_SIDES);
        assertThat(pieces.get(Garment.SHORTS).zones()).containsOnlyKeys(Zone.SHORTS_BASE, Zone.SHORTS_STRIPE,
                Zone.SHORTS_WAIST);
        assertThat(pieces.get(Garment.SOCKS).zones()).containsOnlyKeys(Zone.SOCKS_LEG, Zone.SOCKS_BAND);
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void lasUvDeCadaZonaQuedanEnSusIslas(Persona p) {
        ALL.get(p).pieces().values().forEach(piece -> piece.zones().forEach((zone, parts) -> {
            List<UvRect> islands = UvLayout.BY_ZONE.get(zone);
            for (ModelData.Part part : parts) {
                float[] tex = part.mesh().texCoords();
                for (int i = 0; i < tex.length; i += 2) {
                    double u = tex[i];
                    double v = tex[i + 1];
                    boolean inside = islands.stream().anyMatch(r -> u >= r.u0() - 1e-3 && u <= r.u1() + 1e-3
                            && v >= r.v0() - 1e-3 && v <= r.v1() + 1e-3);
                    assertThat(inside).as("%s %s: uv (%.4f, %.4f) fuera de sus islas", p, zone, u, v).isTrue();
                }
            }
        }));
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void elSombreadoHorneadoTieneOclusionYNormalMap(Persona p) {
        for (SimulatedGarments.Piece piece : ALL.get(p).pieces().values()) {
            ClothShading s = piece.shading();
            assertThat(s.normals()).isNotNull().hasSize(s.size() * s.size());
            double min = 1;
            double max = 0;
            for (float v : s.shade()) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            assertThat(min).isGreaterThanOrEqualTo(0.39);
            assertThat(max).isLessThanOrEqualTo(1.0001).isGreaterThan(0.95);
        }
        ClothShading jersey = ALL.get(p).pieces().get(Garment.JERSEY).shading();
        double u = UvLayout.JERSEY_FRONT.u(0.5);
        double v = UvLayout.JERSEY_FRONT.v(0.6);
        assertThat(jersey.shadeAt(u, v)).isGreaterThan(0.85);
        assertThat(jersey.normalAt(u, v)[2]).isGreaterThan(0.8);
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void losBordesDeLaTelaSonAberturas(Persona p) {
        Dressed d = ALL.get(p);
        List<Vec3> openings = d.pieces().get(Garment.JERSEY).openings();
        double floor = d.human().minY();
        double h = d.human().height();
        assertThat(openings).anyMatch(q -> q.y() > floor + 0.80 * h);
        assertThat(openings).anyMatch(q -> q.y() < floor + 0.56 * h && Math.abs(q.x()) < 0.05 * h);
        assertThat(openings).anyMatch(q -> Math.abs(q.x()) > 0.16 * h);
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void laTelaQuedaPorFueraDeLaPiel(Persona p) {
        Dressed d = ALL.get(p);
        TriangleIndex.Query skin = TriangleIndex.of(List.of(OutfitFitter.skinOf(d.human()).mesh()), 0.02).query();
        d.pieces().forEach((g, piece) -> {
            int n = 0;
            int outside = 0;
            for (List<ModelData.Part> parts : piece.zones().values()) {
                for (ModelData.Part part : parts) {
                    float[] pts = part.mesh().points();
                    for (int i = 0; i < pts.length; i += 3) {
                        Vec3 q = new Vec3(pts[i], -pts[i + 1], -pts[i + 2]);
                        TriangleIndex.Hit hit = skin.nearest(q, 0.12);
                        n++;
                        if (hit == null || q.sub(hit.point()).dot(hit.normal()) > -0.0005 || !inside(skin, q)) {
                            outside++;
                        }
                    }
                }
            }
            assertThat((double) outside / n).as("%s %s fuera de la piel", p, g).isGreaterThan(0.995);
        });
    }

    static boolean inside(TriangleIndex.Query skin, Vec3 p) {
        int votes = 0;
        for (Vec3 dir : List.of(new Vec3(1, 0, 0), new Vec3(-1, 0, 0), new Vec3(0, 0, 1))) {
            Vec3 o = p;
            int crossings = 0;
            double left = 0.6;
            while (left > 0) {
                double d = skin.ray(o, dir, left);
                if (Double.isNaN(d)) {
                    break;
                }
                crossings++;
                o = o.addScaled(dir, d + 1e-5);
                left -= d + 1e-5;
            }
            votes += crossings % 2;
        }
        return votes >= 2;
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void alVestirseUsanLasPrendasSimuladasYLaPielTapadaSeOculta(Persona p) {
        Dressed d = ALL.get(p);
        Map<Zone, List<ModelData.Part>> zones = d.dressed().outfit().zones();
        assertThat(zones.get(Zone.JERSEY_BODY)).isSameAs(d.pieces().get(Garment.JERSEY).zones().get(Zone.JERSEY_BODY));
        assertThat(zones).containsKeys(Zone.SOCKS_LEG, Zone.SHOE_UPPER);
        assertThat(d.dressed().shading().get(Garment.JERSEY)).isSameAs(d.pieces().get(Garment.JERSEY).shading());
        assertThat(d.dressed().shading().get(Garment.SOCKS)).isSameAs(d.pieces().get(Garment.SOCKS).shading());
        int socks = d.pieces().get(Garment.SOCKS).zones().get(Zone.SOCKS_LEG).stream()
                .mapToInt(part -> part.mesh().faceCount()).sum();
        assertThat(zones.get(Zone.SOCKS_LEG).stream().mapToInt(part -> part.mesh().faceCount()).sum())
                .isLessThan(socks);

        MeshData before = OutfitFitter.skinOf(d.human()).mesh();
        MeshData after = OutfitFitter.skinOf(d.dressed().body()).mesh();
        assertThat(after.faceCount()).isLessThan((int) (before.faceCount() * 0.85));
        List<Vec3> openings = d.pieces().values().stream().flatMap(piece -> piece.openings().stream()).toList();
        double floor = d.human().minY();
        double h = d.human().height();
        float[] pts = after.points();
        int[] f = after.faces();
        int visibles = 0;
        for (int t = 0; t < after.faceCount(); t++) {
            int a = f[t * MeshData.FACE_STRIDE] * 3;
            Vec3 v = new Vec3(pts[a], -pts[a + 1], -pts[a + 2]);
            if (Math.abs(v.x()) < 0.054 * h && v.y() > floor + 0.60 * h && v.y() < floor + 0.72 * h && v.z() > 0
                    && openings.stream().noneMatch(o -> o.sub(v).length() < 0.035)) {
                visibles++;
            }
        }
        assertThat(visibles).as("%s: piel visible bajo la camiseta", p).isZero();
    }
}
