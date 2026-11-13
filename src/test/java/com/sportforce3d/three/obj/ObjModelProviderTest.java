package com.sportforce3d.three.obj;

import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.model.Zone;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjModelProviderTest {

    private static final String OBJ = """
            # Exportado de Blender
            mtllib uniforme.mtl
            o mannequin_body
            v 0 0 0
            v 1 0 0
            v 1 1.8 0
            v 0 1.8 0
            vt 0 0
            vt 1 0
            vt 1 1
            vt 0 1
            vn 0 0 1
            f 1/1/1 2/2/1 3/3/1 4/4/1
            o jersey_body.001
            v 0 1.2 0.1
            v 0.5 1.2 0.1
            v 0.5 1.5 0.1
            usemtl Camiseta
            f -3 -2 -1
            g shoe_sole_L
            v 0 0 0.2
            v 0.1 0 0.2
            v 0.1 0 0.3
            f 8//1 9//1 10//1
            o luz_de_estudio
            v 0 0 0
            v 1 0 0
            v 0 1 0
            f 11 12 13
            """;

    @Test
    void mapsObjectsToZonesAndTriangulatesPolygons() throws Exception {
        ModelData model = ObjModelProvider.parse(new StringReader(OBJ), BodyType.ADULTO_HOMBRE);

        assertThat(model.mannequin()).singleElement().satisfies(p -> {
            assertThat(p.name()).isEqualTo("mannequin_body");
            assertThat(p.mesh().faceCount()).as("cuadrilátero → 2 triángulos").isEqualTo(2);
            assertThat(p.mesh().points()[1 + 3 * 2]).isEqualTo(-1.8f);
            assertThat(p.mesh().texCoords()[1]).isEqualTo(1f);
        });
        assertThat(model.zones()).containsOnlyKeys(Zone.JERSEY_BODY, Zone.SHOE_SOLE);
        float[] n = model.zones().get(Zone.JERSEY_BODY).get(0).mesh().normals();
        assertThat(Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2])).isCloseTo(1.0, org.assertj.core.api.Assertions.within(1e-6));
        assertThat(model.height()).isEqualTo(1.8);
        assertThat(model.focus()).isNotEmpty();
    }

    @Test
    void reportsBrokenIndices() {
        assertThatThrownBy(() -> ObjModelProvider.parse(new StringReader("o jersey_body\nv 0 0 0\nf 1 2 3\n"),
                BodyType.ADULTO_HOMBRE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("línea 3");
    }

    @Test
    void proceduralIsUsedWhenNoObjIsPackaged() {
        assertThat(ObjModelProvider.available(BodyType.ADULTO_HOMBRE)).isFalse();
    }
}
