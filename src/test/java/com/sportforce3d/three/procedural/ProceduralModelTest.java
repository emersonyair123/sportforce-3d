package com.sportforce3d.three.procedural;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.GeometryTest;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.UvRect;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.model.UvLayout;
import com.sportforce3d.three.model.Zone;
import com.sportforce3d.three.procedural.BodyShape.Side;
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

    private static final Map<BodyType, ModelData> MODELS = new EnumMap<>(BodyType.class);

    @BeforeAll
    static void build() {
        for (BodyType type : BodyType.values()) {
            MODELS.put(type, new ProceduralModelProvider().load(type));
        }
    }

    @ParameterizedTest
    @EnumSource(BodyType.class)
    void everyZoneExistsWithItsFixedNameAndValidGeometry(BodyType type) {
        ModelData model = MODELS.get(type);
        assertThat(model.zones().keySet()).containsExactlyInAnyOrder(Zone.values());
        model.zones().forEach((zone, parts) -> {
            assertThat(parts).isNotEmpty();
            assertThat(parts.get(0).name()).isEqualTo(zone.meshName());
            parts.forEach(p -> assertFinite(p.mesh()));
        });
        assertThat(model.mannequin()).extracting(ModelData.Part::name)
                .contains("mannequin_torso", "mannequin_head", "mannequin_arm_l", "mannequin_leg_r");
        // Suficientes segmentos para normales suaves (no "cilindros básicos").
        assertThat(model.totalFaces()).isGreaterThan(250_000);
        assertThat(model.focus().keySet()).contains(Garment.values());
    }

    @Test
    void facesPointOutwardOnEveryPart() {
        ModelData model = MODELS.get(BodyType.ADULTO_HOMBRE);
        model.mannequin().forEach(p -> GeometryTest.assertOutward(p.mesh()));
        for (Zone zone : List.of(Zone.JERSEY_BODY, Zone.JERSEY_SLEEVES, Zone.SHORTS_BASE, Zone.SOCKS_LEG,
                Zone.SHOE_UPPER, Zone.SHOE_SOLE)) {
            model.zones().get(zone).forEach(p -> GeometryTest.assertOutward(p.mesh()));
        }
    }

    @Test
    void heightsMatchTheBodyTypes() {
        assertThat(MODELS.get(BodyType.ADULTO_HOMBRE).height()).isBetween(1.78, 1.84);
        assertThat(MODELS.get(BodyType.ADULTO_MUJER).height()).isBetween(1.70, 1.76);
        assertThat(MODELS.get(BodyType.JUVENIL).height()).isBetween(1.45, 1.55);
    }

    @Test
    void texCoordsStayInsideTheirZoneIslands() {
        ModelData model = MODELS.get(BodyType.ADULTO_HOMBRE);
        model.zones().forEach((zone, parts) -> {
            List<UvRect> islands = UvLayout.BY_ZONE.get(zone);
            for (ModelData.Part part : parts) {
                float[] t = part.mesh().texCoords();
                int[] f = part.mesh().faces();
                for (int i = 2; i < f.length; i += 3) {
                    double u = t[f[i] * 2];
                    double v = t[f[i] * 2 + 1];
                    assertThat(islands.stream().anyMatch(r -> u >= r.u0() - 1e-4 && u <= r.u1() + 1e-4
                            && v >= r.v0() - 1e-4 && v <= r.v1() + 1e-4))
                            .as("%s: UV (%.3f, %.3f) fuera de sus islas", zone, u, v).isTrue();
                }
            }
        });
    }

    @Test
    void jerseyFrontAndBackUseSeparateIslands() {
        MeshData jersey = MODELS.get(BodyType.ADULTO_HOMBRE).zones().get(Zone.JERSEY_BODY).get(0).mesh();
        float[] p = jersey.points();
        float[] t = jersey.texCoords();
        int[] f = jersey.faces();
        int front = 0;
        int back = 0;
        for (int i = 0; i < f.length; i += MeshData.FACE_STRIDE) {
            // Centro de la cara en coordenadas del modelo (+Z = frente; JavaFX invierte Z).
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
        for (Garment g : Garment.values()) {
            List<UvRect> rects = new ArrayList<>();
            UvLayout.BY_ZONE.forEach((zone, list) -> {
                if (zone.garment() == g) {
                    rects.addAll(list);
                }
            });
            for (int i = 0; i < rects.size(); i++) {
                for (int j = i + 1; j < rects.size(); j++) {
                    assertThat(rects.get(i).overlaps(rects.get(j))).as("%s: %s y %s", g, rects.get(i), rects.get(j)).isFalse();
                }
            }
        }
    }

    @Test
    void jerseyAndShortsNeverCutIntoTheBody() {
        BodyShape b = BodyShape.of(BodyType.ADULTO_HOMBRE);
        for (int c = 0; c < 72; c++) {
            double phi = 2 * Math.PI * c / 72;
            for (double y = GarmentGenerator.hemY(phi); y < GarmentGenerator.necklineY(phi) - 0.01; y += 0.01) {
                Vec3 p = GarmentGenerator.jerseyPoint(b, phi, y, 0);
                double r = Math.hypot(p.x(), p.z() - b.torsoCenterZ(y));
                assertThat(r).as("camiseta en φ=%.2f y=%.2f", phi, y).isGreaterThan(b.torsoRadius(phi, y));
            }
        }
        for (Side side : Side.values()) {
            for (double y = GarmentGenerator.SHORTS_HEM; y < GarmentGenerator.CROTCH; y += 0.01) {
                double f = b.legFractionAt(side, y);
                Vec3 center = b.leg(side).center(f);
                for (int c = 0; c < 48; c++) {
                    double q = (double) c / 48;
                    Vec3 p = GarmentGenerator.shortsPoint(b, side, q, y, 0);
                    double dist = Math.hypot(p.x() - center.x(), p.z() - center.z());
                    double angle = Math.atan2(p.sub(center).dot(b.leg(side).sideAxis(f)),
                            p.sub(center).dot(b.leg(side).frontAxis(f)));
                    Vec3 skin = b.leg(side).point(f, angle, 0);
                    double skinDist = Math.hypot(skin.x() - center.x(), skin.z() - center.z());
                    assertThat(dist).as("short %s q=%.2f y=%.2f", side, q, y).isGreaterThan(skinDist);
                }
            }
        }
    }

    @Test
    void studsReachTheFloorAndShoesStayAboveIt() {
        MeshData sole = MODELS.get(BodyType.ADULTO_HOMBRE).zones().get(Zone.SHOE_SOLE).get(0).mesh();
        double minHeight = Double.MAX_VALUE;
        float[] p = sole.points();
        for (int i = 1; i < p.length; i += 3) {
            minHeight = Math.min(minHeight, -p[i]);
        }
        assertThat(minHeight).isBetween(-0.001, 0.003);
    }

    private static void assertFinite(MeshData m) {
        for (float v : m.points()) {
            assertThat(Float.isFinite(v)).isTrue();
        }
        for (float v : m.normals()) {
            assertThat(Float.isFinite(v)).isTrue();
        }
        for (float v : m.texCoords()) {
            assertThat(v).isBetween(0f, 1f);
        }
    }
}
