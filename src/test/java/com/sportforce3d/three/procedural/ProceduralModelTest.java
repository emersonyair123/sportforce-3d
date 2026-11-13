package com.sportforce3d.three.procedural;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.GeometryTest;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyUvRect;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.model.PgeyUvLayout;
import com.sportforce3d.three.model.PgeyZone;
import com.sportforce3d.three.procedural.PgeyBodyShape.PgeySide;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProceduralModelTest {

    private static final Map<PgeyBodyType, PgeyModelData> MODELS = new EnumMap<>(PgeyBodyType.class);

    @BeforeAll
    static void build() {
        for (PgeyBodyType type : PgeyBodyType.values()) {
            MODELS.put(type, new PgeyProceduralModelProvider().pgeyLoad(type));
        }
    }

    @ParameterizedTest
    @EnumSource(PgeyBodyType.class)
    void everyZoneExistsWithItsFixedNameAndValidGeometry(PgeyBodyType type) {
        PgeyModelData model = MODELS.get(type);
        assertThat(model.pgeyZones().keySet()).containsExactlyInAnyOrder(PgeyZone.values());
        model.pgeyZones().forEach((zone, parts) -> {
            assertThat(parts).isNotEmpty();
            assertThat(parts.get(0).pgeyName()).isEqualTo(zone.pgeyMeshName());
            parts.forEach(p -> assertFinite(p.pgeyMesh()));
        });
        assertThat(model.pgeyMannequin()).extracting(PgeyModelData.PgeyPart::pgeyName)
                .contains("mannequin_torso", "mannequin_head", "mannequin_arm_l", "mannequin_leg_r");
        assertThat(model.pgeyTotalFaces()).isGreaterThan(250_000);
        assertThat(model.pgeyFocus().keySet()).contains(HepyGarment.values());
    }

    @Test
    void facesPointOutwardOnEveryPart() {
        PgeyModelData model = MODELS.get(PgeyBodyType.ADULTO_HOMBRE);
        model.pgeyMannequin().forEach(p -> GeometryTest.assertOutward(p.pgeyMesh()));
        for (PgeyZone zone : List.of(PgeyZone.JERSEY_BODY, PgeyZone.JERSEY_SLEEVES, PgeyZone.SHORTS_BASE, PgeyZone.SOCKS_LEG,
                PgeyZone.SHOE_UPPER, PgeyZone.SHOE_SOLE)) {
            model.pgeyZones().get(zone).forEach(p -> GeometryTest.assertOutward(p.pgeyMesh()));
        }
    }

    @Test
    void heightsMatchTheBodyTypes() {
        assertThat(MODELS.get(PgeyBodyType.ADULTO_HOMBRE).pgeyHeight()).isBetween(1.78, 1.84);
        assertThat(MODELS.get(PgeyBodyType.ADULTO_MUJER).pgeyHeight()).isBetween(1.70, 1.76);
        assertThat(MODELS.get(PgeyBodyType.JUVENIL).pgeyHeight()).isBetween(1.45, 1.55);
    }

    @Test
    void texCoordsStayInsideTheirZoneIslands() {
        PgeyModelData model = MODELS.get(PgeyBodyType.ADULTO_HOMBRE);
        model.pgeyZones().forEach((zone, parts) -> {
            List<PgeyUvRect> islands = PgeyUvLayout.PGEY_BY_ZONE.get(zone);
            for (PgeyModelData.PgeyPart part : parts) {
                float[] t = part.pgeyMesh().pgeyTexCoords();
                int[] f = part.pgeyMesh().pgeyFaces();
                for (int i = 2; i < f.length; i += 3) {
                    double u = t[f[i] * 2];
                    double v = t[f[i] * 2 + 1];
                    assertThat(islands.stream().anyMatch(r -> u >= r.pgeyU0() - 1e-4 && u <= r.pgeyU1() + 1e-4
                            && v >= r.pgeyV0() - 1e-4 && v <= r.pgeyV1() + 1e-4))
                            .as("%s: UV (%.3f, %.3f) fuera de sus islas", zone, u, v).isTrue();
                }
            }
        });
    }

    @Test
    void jerseyFrontAndBackUseSeparateIslands() {
        PgeyMeshData jersey = MODELS.get(PgeyBodyType.ADULTO_HOMBRE).pgeyZones().get(PgeyZone.JERSEY_BODY).get(0).pgeyMesh();
        float[] p = jersey.pgeyPoints();
        float[] t = jersey.pgeyTexCoords();
        int[] f = jersey.pgeyFaces();
        int front = 0;
        int back = 0;
        for (int i = 0; i < f.length; i += PgeyMeshData.PGEY_FACE_STRIDE) {
            double z = -(p[f[i] * 3 + 2] + p[f[i + 3] * 3 + 2] + p[f[i + 6] * 3 + 2]) / 3;
            double u = (t[f[i + 2] * 2] + t[f[i + 5] * 2] + t[f[i + 8] * 2]) / 3;
            if (z > 0.03) {
                assertThat(u).isLessThan(0.5);
                front++;
            } else if (z < -0.03) {
                assertThat(u).isGreaterThan(0.5);
                back++;
            }
        }
        assertThat(front).isPositive();
        assertThat(back).isPositive();
    }

    @Test
    void islandsOfTheSameGarmentDoNotOverlap() {
        for (HepyGarment g : HepyGarment.values()) {
            List<PgeyUvRect> rects = new ArrayList<>();
            PgeyUvLayout.PGEY_BY_ZONE.forEach((zone, list) -> {
                if (zone.pgeyGarment() == g) {
                    rects.addAll(list);
                }
            });
            for (int i = 0; i < rects.size(); i++) {
                for (int j = i + 1; j < rects.size(); j++) {
                    assertThat(rects.get(i).pgeyOverlaps(rects.get(j))).as("%s: %s y %s", g, rects.get(i), rects.get(j)).isFalse();
                }
            }
        }
    }

    @Test
    void jerseyAndShortsNeverCutIntoTheBody() {
        PgeyBodyShape b = PgeyBodyShape.pgeyOf(PgeyBodyType.ADULTO_HOMBRE);
        for (int c = 0; c < 72; c++) {
            double phi = 2 * Math.PI * c / 72;
            for (double y = PgeyGarmentGenerator.pgeyHemY(phi); y < PgeyGarmentGenerator.pgeyNecklineY(phi) - 0.01; y += 0.01) {
                PgeyVec3 p = PgeyGarmentGenerator.pgeyJerseyPoint(b, phi, y, 0);
                double r = Math.hypot(p.pgeyX(), p.pgeyZ() - b.pgeyTorsoCenterZ(y));
                assertThat(r).as("camiseta en φ=%.2f y=%.2f", phi, y).isGreaterThan(b.pgeyTorsoRadius(phi, y));
            }
        }
        for (PgeySide side : PgeySide.values()) {
            for (double y = PgeyGarmentGenerator.PGEY_SHORTS_HEM; y < PgeyGarmentGenerator.PGEY_CROTCH; y += 0.01) {
                double f = b.pgeyLegFractionAt(side, y);
                PgeyVec3 center = b.pgeyLeg(side).pgeyCenter(f);
                for (int c = 0; c < 48; c++) {
                    double q = (double) c / 48;
                    PgeyVec3 p = PgeyGarmentGenerator.pgeyShortsPoint(b, side, q, y, 0);
                    double dist = Math.hypot(p.pgeyX() - center.pgeyX(), p.pgeyZ() - center.pgeyZ());
                    double angle = Math.atan2(p.pgeySub(center).pgeyDot(b.pgeyLeg(side).pgeySideAxis(f)),
                            p.pgeySub(center).pgeyDot(b.pgeyLeg(side).pgeyFrontAxis(f)));
                    PgeyVec3 skin = b.pgeyLeg(side).pgeyPoint(f, angle, 0);
                    double skinDist = Math.hypot(skin.pgeyX() - center.pgeyX(), skin.pgeyZ() - center.pgeyZ());
                    assertThat(dist).as("short %s q=%.2f y=%.2f", side, q, y).isGreaterThan(skinDist);
                }
            }
        }
    }

    @Test
    void studsReachTheFloorAndShoesStayAboveIt() {
        PgeyMeshData sole = MODELS.get(PgeyBodyType.ADULTO_HOMBRE).pgeyZones().get(PgeyZone.SHOE_SOLE).get(0).pgeyMesh();
        double minHeight = Double.MAX_VALUE;
        float[] p = sole.pgeyPoints();
        for (int i = 1; i < p.length; i += 3) {
            minHeight = Math.min(minHeight, -p[i]);
        }
        assertThat(minHeight).isBetween(-0.001, 0.003);
    }

    private static void assertFinite(PgeyMeshData m) {
        for (float v : m.pgeyPoints()) {
            assertThat(Float.isFinite(v)).isTrue();
        }
        for (float v : m.pgeyNormals()) {
            assertThat(Float.isFinite(v)).isTrue();
        }
        for (float v : m.pgeyTexCoords()) {
            assertThat(v).isBetween(0f, 1f);
        }
    }
}
