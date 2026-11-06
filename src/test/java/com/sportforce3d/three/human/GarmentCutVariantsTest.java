package com.sportforce3d.three.human;

import com.sportforce3d.design.GarmentCut;
import com.sportforce3d.design.GarmentCut.ShortsLength;
import com.sportforce3d.design.GarmentCut.Sleeve;
import com.sportforce3d.design.GarmentCut.SocksLength;
import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.UvRect;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.model.ModelProviders;
import com.sportforce3d.three.model.UvLayout;
import com.sportforce3d.three.model.Zone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Etapa 10C: manga larga, short corto y largo y medias tobilleras y de media pierna simulados en Blender para cada
 * persona (tools/blender, {@code --variantes}): las mismas zonas e islas UV (más la del antebrazo), tela por fuera de
 * la piel, cada largo donde corresponde y la piel que queda a la vista según el largo.
 */
class GarmentCutVariantsTest {

    private static final GarmentCut LONG = GarmentCut.DEFAULT.withSleeve(Sleeve.LARGA);
    private static final GarmentCut SHORT_SHORTS = GarmentCut.DEFAULT.withShorts(ShortsLength.CORTO);
    private static final GarmentCut LONG_SHORTS = GarmentCut.DEFAULT.withShorts(ShortsLength.LARGO);
    private static final GarmentCut ANKLE = GarmentCut.DEFAULT.withSocks(SocksLength.TOBILLERA);
    private static final GarmentCut MID = GarmentCut.DEFAULT.withSocks(SocksLength.MEDIA);
    private static final List<GarmentCut> VARIANTS = List.of(LONG, SHORT_SHORTS, LONG_SHORTS, ANKLE, MID);

    @ParameterizedTest
    @EnumSource(Persona.class)
    void cadaVarianteEstaEmpaquetadaConSusZonasYUvEnSusIslas(Persona p) {
        for (GarmentCut cut : VARIANTS) {
            for (Garment g : List.of(Garment.JERSEY, Garment.SHORTS, Garment.SOCKS)) {
                assertThat(SimulatedGarments.available(p, g, cut)).as("%s %s %s", p, g, cut).isTrue();
            }
            assertThat(SimulatedGarments.folder(p, Garment.JERSEY, cut)).isEqualTo(
                    cut == LONG ? "/models/prendas/" + p.key() + "/camiseta_futbol_v_larga/"
                            : SimulatedGarments.folder(p, Garment.JERSEY));
        }
        for (GarmentCut cut : VARIANTS) {
            Map<Garment, SimulatedGarments.Piece> pieces = SimulatedGarments.load(p, cut);
            assertThat(pieces).containsOnlyKeys(Garment.JERSEY, Garment.SHORTS, Garment.SOCKS);
            pieces.values().forEach(piece -> piece.zones().forEach((zone, parts) -> {
                List<UvRect> islands = UvLayout.BY_ZONE.get(zone);
                for (ModelData.Part part : parts) {
                    float[] tex = part.mesh().texCoords();
                    for (int i = 0; i < tex.length; i += 2) {
                        double u = tex[i];
                        double v = tex[i + 1];
                        assertThat(islands.stream().anyMatch(r -> u >= r.u0() - 1e-3 && u <= r.u1() + 1e-3
                                && v >= r.v0() - 1e-3 && v <= r.v1() + 1e-3))
                                .as("%s %s %s: uv (%.4f, %.4f) fuera de sus islas", p, cut, zone, u, v).isTrue();
                    }
                }
            }));
        }
        // El antebrazo de la manga larga usa su isla; la manga corta, nunca.
        assertThat(usesIsland(SimulatedGarments.load(p, LONG).get(Garment.JERSEY), UvLayout.JERSEY_FOREARM_LEFT)).isTrue();
        assertThat(usesIsland(SimulatedGarments.load(p).get(Garment.JERSEY), UvLayout.JERSEY_FOREARM_LEFT)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void laTelaDeCadaVarianteQuedaPorFueraDeLaPiel(Persona p) {
        HumanModel human = OutfitFitterTest.geometryOnly(p);
        TriangleIndex.Query skin = TriangleIndex.of(List.of(OutfitFitter.skinOf(human).mesh()), 0.02).query();
        // El pie de las medias queda dentro de la zapatilla (la app lo recorta): no cuenta, así la proporción de la
        // tobillera (poca caña) es comparable con la de la media larga.
        double ankle = human.minY() + 0.08 * human.height();
        Map<GarmentCut, Garment> changed = Map.of(LONG, Garment.JERSEY, SHORT_SHORTS, Garment.SHORTS,
                LONG_SHORTS, Garment.SHORTS, ANKLE, Garment.SOCKS, MID, Garment.SOCKS);
        changed.forEach((cut, g) -> {
            int n = 0;
            int outside = 0;
            for (List<ModelData.Part> parts : SimulatedGarments.load(p, cut).get(g).zones().values()) {
                for (ModelData.Part part : parts) {
                    float[] pts = part.mesh().points();
                    for (int i = 0; i < pts.length; i += 3) {
                        Vec3 q = new Vec3(pts[i], -pts[i + 1], -pts[i + 2]);
                        if (g == Garment.SOCKS && q.y() < ankle) {
                            continue;
                        }
                        TriangleIndex.Hit hit = skin.nearest(q, 0.12);
                        n++;
                        if (hit == null || q.sub(hit.point()).dot(hit.normal()) > -0.0005
                                || !SimulatedGarmentsTest.inside(skin, q)) {
                            outside++;
                        }
                    }
                }
            }
            assertThat((double) outside / n).as("%s %s fuera de la piel", p, cut).isGreaterThan(0.995);
        });
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void cadaLargoLlegaDondeDebe(Persona p) {
        HumanModel human = OutfitFitterTest.geometryOnly(p);
        double h = human.height();
        // Manga larga: llega a la muñeca. En pose A el brazo baja a ~45°, así que los ~30 cm más de manga se alejan
        // del cuerpo en x algo más del 7 % del alto.
        double shortReach = maxAbsX(SimulatedGarments.load(p).get(Garment.JERSEY), Zone.JERSEY_CUFFS);
        double longReach = maxAbsX(SimulatedGarments.load(p, LONG).get(Garment.JERSEY), Zone.JERSEY_CUFFS);
        assertThat(longReach - shortReach).as("%s: la manga larga llega más lejos", p).isGreaterThan(0.07 * h);
        // Short: corto (medio muslo) < medio (sobre la rodilla) < largo (a la rodilla), con al menos 3 % del alto
        // entre uno y otro.
        double corto = minY(SimulatedGarments.load(p, SHORT_SHORTS).get(Garment.SHORTS), Zone.SHORTS_BASE);
        double medio = minY(SimulatedGarments.load(p).get(Garment.SHORTS), Zone.SHORTS_BASE);
        double largo = minY(SimulatedGarments.load(p, LONG_SHORTS).get(Garment.SHORTS), Zone.SHORTS_BASE);
        assertThat(corto - medio).as("%s: short corto sobre el medio", p).isGreaterThan(0.03 * h);
        assertThat(medio - largo).as("%s: short largo bajo el medio", p).isGreaterThan(0.03 * h);
        // Medias: tobillera < media pierna < larga (el borde de la banda sube).
        double tob = maxY(SimulatedGarments.load(p, ANKLE).get(Garment.SOCKS), Zone.SOCKS_BAND);
        double mid = maxY(SimulatedGarments.load(p, MID).get(Garment.SOCKS), Zone.SOCKS_BAND);
        double larga = maxY(SimulatedGarments.load(p).get(Garment.SOCKS), Zone.SOCKS_BAND);
        assertThat(tob).isLessThan(mid - 0.04 * h);
        assertThat(mid).isLessThan(larga - 0.04 * h);
        assertThat(tob - human.minY()).as("%s: la tobillera queda sobre el tobillo", p).isGreaterThan(0.09 * h);
    }

    @ParameterizedTest
    @EnumSource(Persona.class)
    void laPielQueTapaCadaLargoSeOcultaYLaDemasSeVe(Persona p) {
        HumanModel human = OutfitFitterTest.geometryOnly(p);
        var source = ModelProviders.load(p.fallback());
        int base = skinFaces(OutfitFitter.fit(human, source, SimulatedGarments.load(p)));
        int longSleeve = skinFaces(OutfitFitter.fit(human, source, SimulatedGarments.load(p, LONG)));
        int ankle = skinFaces(OutfitFitter.fit(human, source, SimulatedGarments.load(p, ANKLE)));
        int shortShorts = skinFaces(OutfitFitter.fit(human, source, SimulatedGarments.load(p, SHORT_SHORTS)));
        assertThat(longSleeve).as("%s: la manga larga tapa el antebrazo", p).isLessThan(base);
        // La piel bajo las medias nunca se oculta (van pegadas a la pierna): con tobilleras no se oculta más.
        assertThat(ankle).as("%s: con tobilleras se ve la pantorrilla", p).isGreaterThanOrEqualTo(base);
        assertThat(shortShorts).as("%s: con short corto se ve más muslo", p).isGreaterThan(base);
    }

    /**
     * El número del short (por defecto a media altura de lo que se ve, según el largo) queda entre el ruedo de la
     * camiseta y la basta, con su media altura (3,5 cm) libre por arriba y por abajo, en las cuatro personas.
     */
    @ParameterizedTest
    @EnumSource(Persona.class)
    void elNumeroDelShortQuedaALaVistaEnCadaLargo(Persona p) {
        SimulatedGarments.Piece jersey = SimulatedGarments.load(p).get(Garment.JERSEY);
        for (ShortsLength length : ShortsLength.values()) {
            SimulatedGarments.Piece shorts = SimulatedGarments.load(p, GarmentCut.DEFAULT.withShorts(length))
                    .get(Garment.SHORTS);
            UvRect r = UvLayout.SHORTS_LEFT_FRONT;
            double tu = r.u0() + (r.u1() - r.u0()) * length.numberU();
            double tv = r.v0() + (r.v1() - r.v0()) * length.numberV();
            double[] best = null;
            double bestD = Double.MAX_VALUE;
            for (ModelData.Part part : shorts.zones().get(Zone.SHORTS_BASE)) {
                float[] pts = part.mesh().points();
                float[] tex = part.mesh().texCoords();
                int[] f = part.mesh().faces();
                for (int t = 0; t < part.mesh().faceCount() * 3; t++) {
                    int pi = f[t * 3];
                    int ti = f[t * 3 + 2];
                    double d = Math.hypot(tex[ti * 2] - tu, tex[ti * 2 + 1] - tv);
                    if (d < bestD) {
                        bestD = d;
                        best = new double[]{pts[pi * 3], -pts[pi * 3 + 1], -pts[pi * 3 + 2]};
                    }
                }
            }
            assertThat(bestD).as("%s %s: hay tela en la posición del número", p, length).isLessThan(0.01);
            double x = best[0];
            double y = best[1];
            double hemJersey = Double.MAX_VALUE;
            for (ModelData.Part part : jersey.zones().get(Zone.JERSEY_BODY)) {
                float[] pts = part.mesh().points();
                for (int i = 0; i < pts.length; i += 3) {
                    if (Math.abs(pts[i] - x) < 0.04 && -pts[i + 2] > 0) {
                        hemJersey = Math.min(hemJersey, -pts[i + 1]);
                    }
                }
            }
            double hemShorts = Double.MAX_VALUE;
            for (ModelData.Part part : shorts.zones().get(Zone.SHORTS_BASE)) {
                float[] pts = part.mesh().points();
                for (int i = 0; i < pts.length; i += 3) {
                    if (Math.abs(pts[i] - x) < 0.04 && -pts[i + 2] > 0) {
                        hemShorts = Math.min(hemShorts, -pts[i + 1]);
                    }
                }
            }
            assertThat(y + 0.035).as("%s %s: el número no queda bajo la camiseta", p, length).isLessThan(hemJersey);
            assertThat(y - 0.035).as("%s %s: el número no se sale por la basta", p, length).isGreaterThan(hemShorts);
            assertThat(Math.abs((y - hemShorts) - (hemJersey - y))).as("%s %s: a media altura", p, length)
                    .isLessThan(0.05);
            // Sobre el muslo izquierdo (+x), no en la bragueta.
            assertThat(x).as("%s %s: en la pierna izquierda, lejos de la línea media", p, length)
                    .isGreaterThan(0.035 * OutfitFitterTest.geometryOnly(p).height());
        }
    }

    private static int skinFaces(OutfitFitter.DressedPerson d) {
        return OutfitFitter.skinOf(d.body()).mesh().faceCount();
    }

    private static boolean usesIsland(SimulatedGarments.Piece piece, UvRect r) {
        for (ModelData.Part part : piece.zones().get(Zone.JERSEY_SLEEVES)) {
            float[] tex = part.mesh().texCoords();
            for (int i = 0; i < tex.length; i += 2) {
                if (tex[i] > r.u0() && tex[i] < r.u1() && tex[i + 1] > r.v0() + 0.01 && tex[i + 1] < r.v1()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Coordenadas del visor (JavaFX): y hacia abajo; se devuelven en el espacio del modelo (y arriba). */
    private static double maxAbsX(SimulatedGarments.Piece piece, Zone zone) {
        double m = 0;
        for (ModelData.Part part : piece.zones().get(zone)) {
            float[] pts = part.mesh().points();
            for (int i = 0; i < pts.length; i += 3) {
                m = Math.max(m, Math.abs(pts[i]));
            }
        }
        return m;
    }

    private static double minY(SimulatedGarments.Piece piece, Zone zone) {
        double m = Double.MAX_VALUE;
        for (ModelData.Part part : piece.zones().get(zone)) {
            float[] pts = part.mesh().points();
            for (int i = 1; i < pts.length; i += 3) {
                m = Math.min(m, -pts[i]);
            }
        }
        return m;
    }

    private static double maxY(SimulatedGarments.Piece piece, Zone zone) {
        double m = -Double.MAX_VALUE;
        for (ModelData.Part part : piece.zones().get(zone)) {
            float[] pts = part.mesh().points();
            for (int i = 1; i < pts.length; i += 3) {
                m = Math.max(m, -pts[i]);
            }
        }
        return m;
    }
}
