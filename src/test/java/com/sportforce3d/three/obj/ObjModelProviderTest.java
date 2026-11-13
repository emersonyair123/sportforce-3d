package com.sportforce3d.three.obj;

import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.model.PgeyZone;
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
        PgeyModelData model = PgeyObjModelProvider.pgeyParse(new StringReader(OBJ), PgeyBodyType.ADULTO_HOMBRE);

        assertThat(model.pgeyMannequin()).singleElement().satisfies(p -> {
            assertThat(p.pgeyName()).isEqualTo("mannequin_body");
            assertThat(p.pgeyMesh().pgeyFaceCount()).as("cuadrilátero → 2 triángulos").isEqualTo(2);
            assertThat(p.pgeyMesh().pgeyPoints()[1 + 3 * 2]).isEqualTo(-1.8f);
            assertThat(p.pgeyMesh().pgeyTexCoords()[1]).isEqualTo(1f);
        });
        assertThat(model.pgeyZones()).containsOnlyKeys(PgeyZone.JERSEY_BODY, PgeyZone.SHOE_SOLE);
        float[] n = model.pgeyZones().get(PgeyZone.JERSEY_BODY).get(0).pgeyMesh().pgeyNormals();
        assertThat(Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2])).isCloseTo(1.0, org.assertj.core.api.Assertions.within(1e-6));
        assertThat(model.pgeyHeight()).isEqualTo(1.8);
        assertThat(model.pgeyFocus()).isNotEmpty();
    }

    @Test
    void reportsBrokenIndices() {
        assertThatThrownBy(() -> PgeyObjModelProvider.pgeyParse(new StringReader("o jersey_body\nv 0 0 0\nf 1 2 3\n"),
                PgeyBodyType.ADULTO_HOMBRE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("línea 3");
    }

    @Test
    void proceduralIsUsedWhenNoObjIsPackaged() {
        assertThat(PgeyObjModelProvider.pgeyAvailable(PgeyBodyType.ADULTO_HOMBRE)).isFalse();
    }
}
