package com.sportforce3d.three.human;

import com.sportforce3d.design.PgeyGarmentCut;
import com.sportforce3d.design.PgeyGarmentCut.PgeyShortsLength;
import com.sportforce3d.design.PgeyGarmentCut.PgeySleeve;
import com.sportforce3d.design.PgeyGarmentCut.PgeySocksLength;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyUvRect;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.model.PgeyModelProviders;
import com.sportforce3d.three.model.PgeyUvLayout;
import com.sportforce3d.three.model.PgeyZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GarmentCutVariantsTest {

    private static final PgeyGarmentCut LONG = PgeyGarmentCut.PGEY_DEFAULT.pgeyWithSleeve(PgeySleeve.LARGA);
    private static final PgeyGarmentCut SHORT_SHORTS = PgeyGarmentCut.PGEY_DEFAULT.pgeyWithShorts(PgeyShortsLength.CORTO);
    private static final PgeyGarmentCut LONG_SHORTS = PgeyGarmentCut.PGEY_DEFAULT.pgeyWithShorts(PgeyShortsLength.LARGO);
    private static final PgeyGarmentCut ANKLE = PgeyGarmentCut.PGEY_DEFAULT.pgeyWithSocks(PgeySocksLength.TOBILLERA);
    private static final PgeyGarmentCut MID = PgeyGarmentCut.PGEY_DEFAULT.pgeyWithSocks(PgeySocksLength.MEDIA);
    private static final List<PgeyGarmentCut> VARIANTS = List.of(LONG, SHORT_SHORTS, LONG_SHORTS, ANKLE, MID);

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void cadaVarianteEstaEmpaquetadaConSusZonasYUvEnSusIslas(PgeyPersona p) {
        for (PgeyGarmentCut cut : VARIANTS) {
            for (HepyGarment g : List.of(HepyGarment.JERSEY, HepyGarment.SHORTS, HepyGarment.SOCKS)) {
                assertThat(PgeySimulatedGarments.pgeyAvailable(p, g, cut)).as("%s %s %s", p, g, cut).isTrue();
            }
            assertThat(PgeySimulatedGarments.pgeyFolder(p, HepyGarment.JERSEY, cut)).isEqualTo(
                    cut == LONG ? "/models/prendas/" + p.pgeyKey() + "/camiseta_futbol_v_larga/"
                            : PgeySimulatedGarments.pgeyFolder(p, HepyGarment.JERSEY));
        }
        for (PgeyGarmentCut cut : VARIANTS) {
            Map<HepyGarment, PgeySimulatedGarments.PgeyPiece> pieces = PgeySimulatedGarments.pgeyLoad(p, cut);
            assertThat(pieces).containsOnlyKeys(HepyGarment.JERSEY, HepyGarment.SHORTS, HepyGarment.SOCKS);
            pieces.values().forEach(piece -> piece.pgeyZones().forEach((zone, parts) -> {
                List<PgeyUvRect> islands = PgeyUvLayout.PGEY_BY_ZONE.get(zone);
                for (PgeyModelData.PgeyPart part : parts) {
                    float[] tex = part.pgeyMesh().pgeyTexCoords();
                    for (int i = 0; i < tex.length; i += 2) {
                        double u = tex[i];
                        double v = tex[i + 1];
                        assertThat(islands.stream().anyMatch(r -> u >= r.pgeyU0() - 1e-3 && u <= r.pgeyU1() + 1e-3
                                && v >= r.pgeyV0() - 1e-3 && v <= r.pgeyV1() + 1e-3))
                                .as("%s %s %s: uv (%.4f, %.4f) fuera de sus islas", p, cut, zone, u, v).isTrue();
                    }
                }
            }));
        }
        assertThat(usesIsland(PgeySimulatedGarments.pgeyLoad(p, LONG).get(HepyGarment.JERSEY), PgeyUvLayout.PGEY_JERSEY_FOREARM_LEFT)).isTrue();
        assertThat(usesIsland(PgeySimulatedGarments.pgeyLoad(p).get(HepyGarment.JERSEY), PgeyUvLayout.PGEY_JERSEY_FOREARM_LEFT)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void laTelaDeCadaVarianteQuedaPorFueraDeLaPiel(PgeyPersona p) {
        PgeyHumanModel human = OutfitFitterTest.geometryOnly(p);
        PgeyTriangleIndex.PgeyQuery skin = PgeyTriangleIndex.pgeyOf(List.of(PgeyOutfitFitter.pgeySkinOf(human).pgeyMesh()), 0.02).pgeyQuery();
        double ankle = human.pgeyMinY() + 0.08 * human.pgeyHeight();
        Map<PgeyGarmentCut, HepyGarment> changed = Map.of(LONG, HepyGarment.JERSEY, SHORT_SHORTS, HepyGarment.SHORTS,
                LONG_SHORTS, HepyGarment.SHORTS, ANKLE, HepyGarment.SOCKS, MID, HepyGarment.SOCKS);
        changed.forEach((cut, g) -> {
            int n = 0;
            int outside = 0;
            for (List<PgeyModelData.PgeyPart> parts : PgeySimulatedGarments.pgeyLoad(p, cut).get(g).pgeyZones().values()) {
                for (PgeyModelData.PgeyPart part : parts) {
                    float[] pts = part.pgeyMesh().pgeyPoints();
                    for (int i = 0; i < pts.length; i += 3) {
                        PgeyVec3 q = new PgeyVec3(pts[i], -pts[i + 1], -pts[i + 2]);
                        if (g == HepyGarment.SOCKS && q.pgeyY() < ankle) {
                            continue;
                        }
                        PgeyTriangleIndex.PgeyHit hit = skin.pgeyNearest(q, 0.12);
                        n++;
                        if (hit == null || q.pgeySub(hit.pgeyPoint()).pgeyDot(hit.pgeyNormal()) > -0.0005
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
    @EnumSource(PgeyPersona.class)
    void cadaLargoLlegaDondeDebe(PgeyPersona p) {
        PgeyHumanModel human = OutfitFitterTest.geometryOnly(p);
        double h = human.pgeyHeight();
        double shortReach = maxAbsX(PgeySimulatedGarments.pgeyLoad(p).get(HepyGarment.JERSEY), PgeyZone.JERSEY_CUFFS);
        double longReach = maxAbsX(PgeySimulatedGarments.pgeyLoad(p, LONG).get(HepyGarment.JERSEY), PgeyZone.JERSEY_CUFFS);
        assertThat(longReach - shortReach).as("%s: la manga larga llega más lejos", p).isGreaterThan(0.07 * h);
        double corto = minY(PgeySimulatedGarments.pgeyLoad(p, SHORT_SHORTS).get(HepyGarment.SHORTS), PgeyZone.SHORTS_BASE);
        double medio = minY(PgeySimulatedGarments.pgeyLoad(p).get(HepyGarment.SHORTS), PgeyZone.SHORTS_BASE);
        double largo = minY(PgeySimulatedGarments.pgeyLoad(p, LONG_SHORTS).get(HepyGarment.SHORTS), PgeyZone.SHORTS_BASE);
        assertThat(corto - medio).as("%s: short corto sobre el medio", p).isGreaterThan(0.03 * h);
        assertThat(medio - largo).as("%s: short largo bajo el medio", p).isGreaterThan(0.03 * h);
        double tob = maxY(PgeySimulatedGarments.pgeyLoad(p, ANKLE).get(HepyGarment.SOCKS), PgeyZone.SOCKS_BAND);
        double mid = maxY(PgeySimulatedGarments.pgeyLoad(p, MID).get(HepyGarment.SOCKS), PgeyZone.SOCKS_BAND);
        double larga = maxY(PgeySimulatedGarments.pgeyLoad(p).get(HepyGarment.SOCKS), PgeyZone.SOCKS_BAND);
        assertThat(tob).isLessThan(mid - 0.04 * h);
        assertThat(mid).isLessThan(larga - 0.04 * h);
        assertThat(tob - human.pgeyMinY()).as("%s: la tobillera queda sobre el tobillo", p).isGreaterThan(0.09 * h);
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void laPielQueTapaCadaLargoSeOcultaYLaDemasSeVe(PgeyPersona p) {
        PgeyHumanModel human = OutfitFitterTest.geometryOnly(p);
        var source = PgeyModelProviders.pgeyLoad(p.pgeyFallback());
        int base = skinFaces(PgeyOutfitFitter.pgeyFit(human, source, PgeySimulatedGarments.pgeyLoad(p)));
        int longSleeve = skinFaces(PgeyOutfitFitter.pgeyFit(human, source, PgeySimulatedGarments.pgeyLoad(p, LONG)));
        int ankle = skinFaces(PgeyOutfitFitter.pgeyFit(human, source, PgeySimulatedGarments.pgeyLoad(p, ANKLE)));
        int shortShorts = skinFaces(PgeyOutfitFitter.pgeyFit(human, source, PgeySimulatedGarments.pgeyLoad(p, SHORT_SHORTS)));
        assertThat(longSleeve).as("%s: la manga larga tapa el antebrazo", p).isLessThan(base);
        assertThat(ankle).as("%s: con tobilleras se ve la pantorrilla", p).isGreaterThanOrEqualTo(base);
        assertThat(shortShorts).as("%s: con short corto se ve más muslo", p).isGreaterThan(base);
    }

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void elNumeroDelShortQuedaALaVistaEnCadaLargo(PgeyPersona p) {
        PgeySimulatedGarments.PgeyPiece jersey = PgeySimulatedGarments.pgeyLoad(p).get(HepyGarment.JERSEY);
        for (PgeyShortsLength length : PgeyShortsLength.values()) {
            PgeySimulatedGarments.PgeyPiece shorts = PgeySimulatedGarments.pgeyLoad(p, PgeyGarmentCut.PGEY_DEFAULT.pgeyWithShorts(length))
                    .get(HepyGarment.SHORTS);
            PgeyUvRect r = PgeyUvLayout.PGEY_SHORTS_LEFT_FRONT;
            double tu = r.pgeyU0() + (r.pgeyU1() - r.pgeyU0()) * length.pgeyNumberU();
            double tv = r.pgeyV0() + (r.pgeyV1() - r.pgeyV0()) * length.pgeyNumberV();
            double[] best = null;
            double bestD = Double.MAX_VALUE;
            for (PgeyModelData.PgeyPart part : shorts.pgeyZones().get(PgeyZone.SHORTS_BASE)) {
                float[] pts = part.pgeyMesh().pgeyPoints();
                float[] tex = part.pgeyMesh().pgeyTexCoords();
                int[] f = part.pgeyMesh().pgeyFaces();
                for (int t = 0; t < part.pgeyMesh().pgeyFaceCount() * 3; t++) {
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
            for (PgeyModelData.PgeyPart part : jersey.pgeyZones().get(PgeyZone.JERSEY_BODY)) {
                float[] pts = part.pgeyMesh().pgeyPoints();
                for (int i = 0; i < pts.length; i += 3) {
                    if (Math.abs(pts[i] - x) < 0.04 && -pts[i + 2] > 0) {
                        hemJersey = Math.min(hemJersey, -pts[i + 1]);
                    }
                }
            }
            double hemShorts = Double.MAX_VALUE;
            for (PgeyModelData.PgeyPart part : shorts.pgeyZones().get(PgeyZone.SHORTS_BASE)) {
                float[] pts = part.pgeyMesh().pgeyPoints();
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
            assertThat(x).as("%s %s: en la pierna izquierda, lejos de la línea media", p, length)
                    .isGreaterThan(0.035 * OutfitFitterTest.geometryOnly(p).pgeyHeight());
        }
    }

    private static int skinFaces(PgeyOutfitFitter.PgeyDressedPerson d) {
        return PgeyOutfitFitter.pgeySkinOf(d.pgeyBody()).pgeyMesh().pgeyFaceCount();
    }

    private static boolean usesIsland(PgeySimulatedGarments.PgeyPiece piece, PgeyUvRect r) {
        for (PgeyModelData.PgeyPart part : piece.pgeyZones().get(PgeyZone.JERSEY_SLEEVES)) {
            float[] tex = part.pgeyMesh().pgeyTexCoords();
            for (int i = 0; i < tex.length; i += 2) {
                if (tex[i] > r.pgeyU0() && tex[i] < r.pgeyU1() && tex[i + 1] > r.pgeyV0() + 0.01 && tex[i + 1] < r.pgeyV1()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double maxAbsX(PgeySimulatedGarments.PgeyPiece piece, PgeyZone zone) {
        double m = 0;
        for (PgeyModelData.PgeyPart part : piece.pgeyZones().get(zone)) {
            float[] pts = part.pgeyMesh().pgeyPoints();
            for (int i = 0; i < pts.length; i += 3) {
                m = Math.max(m, Math.abs(pts[i]));
            }
        }
        return m;
    }

    private static double minY(PgeySimulatedGarments.PgeyPiece piece, PgeyZone zone) {
        double m = Double.MAX_VALUE;
        for (PgeyModelData.PgeyPart part : piece.pgeyZones().get(zone)) {
            float[] pts = part.pgeyMesh().pgeyPoints();
            for (int i = 1; i < pts.length; i += 3) {
                m = Math.min(m, -pts[i]);
            }
        }
        return m;
    }

    private static double maxY(PgeySimulatedGarments.PgeyPiece piece, PgeyZone zone) {
        double m = -Double.MAX_VALUE;
        for (PgeyModelData.PgeyPart part : piece.pgeyZones().get(zone)) {
            float[] pts = part.pgeyMesh().pgeyPoints();
            for (int i = 1; i < pts.length; i += 3) {
                m = Math.max(m, -pts[i]);
            }
        }
        return m;
    }
}
