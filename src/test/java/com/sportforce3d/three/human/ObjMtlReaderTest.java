package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.PgeyMeshData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ObjMtlReaderTest {

    private static final String MTL = """
            # MakeHuman exported MTL
            newmtl piel
            Kd 1 0.95 0.85
            Ks 0.02745 0.02745 0.02745
            d 0
            map_Kd textures/piel.png
            map_D textures/piel.png

            newmtl ojos
            Kd 1 1 1
            Ks 1 1 1
            Ns 80
            map_Kd -s 1 1 1 textures\\ojo.png
            """;

    private static final String OBJ = """
            mtllib modelo.mtl
            v 0 0 0
            v 1 0 0
            v 1 2 0
            v 0 2 0
            v 0 0 1
            vt 0 0
            vt 1 0
            vt 1 1
            vt 0 1
            usemtl piel
            g base.obj
            f 1/1 2/2 3/3 4/4
            usemtl ojos
            g high-poly.obj
            f -5/1 -4/2 -1/4
            """;

    private static PgeyObjMtlReader.PgeyResult read(String obj, Map<String, String> files) throws IOException {
        return PgeyObjMtlReader.pgeyRead(new StringReader(obj), path -> {
            String text = files.get(path);
            return text == null ? null : new StringReader(text);
        });
    }

    @Test
    void creaUnaMallaPorMaterialEnElOrdenDelObj() throws IOException {
        PgeyObjMtlReader.PgeyResult r = read(OBJ, Map.of("modelo.mtl", MTL));

        assertThat(r.pgeyGroups()).extracting(PgeyObjMtlReader.PgeyGroup::pgeyMaterial).containsExactly("piel", "ojos");
        PgeyMeshData piel = r.pgeyGroups().get(0).pgeyMesh();
        assertThat(piel.pgeyFaceCount()).as("el cuadro se divide en dos triángulos").isEqualTo(2);
        assertThat(piel.pgeyPointCount()).as("solo los vértices que usa el material").isEqualTo(4);
        PgeyMeshData ojos = r.pgeyGroups().get(1).pgeyMesh();
        assertThat(ojos.pgeyFaceCount()).isEqualTo(1);
        assertThat(ojos.pgeyPointCount()).isEqualTo(3);
    }

    @Test
    void convierteACoordenadasDeJavaFxEInvierteV() throws IOException {
        PgeyObjMtlReader.PgeyResult r = read(OBJ, Map.of("modelo.mtl", MTL));
        PgeyMeshData piel = r.pgeyGroups().get(0).pgeyMesh();
        int corner = 2;
        int p = piel.pgeyFaces()[corner * 3];
        int t = piel.pgeyFaces()[corner * 3 + 2];
        assertThat(piel.pgeyPoints()[p * 3]).isEqualTo(1f);
        assertThat(piel.pgeyPoints()[p * 3 + 1]).isEqualTo(-2f);
        assertThat(piel.pgeyTexCoords()[t * 2]).isEqualTo(1f);
        assertThat(piel.pgeyTexCoords()[t * 2 + 1]).isEqualTo(0f);

        assertThat(r.pgeyMin()).containsExactly(0, 0, 0);
        assertThat(r.pgeyMax()).containsExactly(1, 2, 1);
        assertThat(r.pgeyHeight()).isEqualTo(2);
    }

    @Test
    void calculaNormalesSuavesUnitariasCuandoElObjNoTrae() throws IOException {
        PgeyMeshData piel = read(OBJ, Map.of("modelo.mtl", MTL)).pgeyGroups().get(0).pgeyMesh();
        for (int c = 0; c < 6; c++) {
            int n = piel.pgeyFaces()[c * 3 + 1];
            assertThat(piel.pgeyNormals()[n * 3]).isCloseTo(0f, within(1e-6f));
            assertThat(piel.pgeyNormals()[n * 3 + 1]).isCloseTo(0f, within(1e-6f));
            assertThat(piel.pgeyNormals()[n * 3 + 2]).isCloseTo(-1f, within(1e-6f));
        }
    }

    @Test
    void lasNormalesSePromedianEntreCarasVecinas() throws IOException {
        String obj = """
                v 0 0 0
                v 0 1 0
                v 1 0 0
                v 0 0 -1
                f 1 3 2
                f 1 2 4
                """;
        PgeyObjMtlReader.PgeyResult result = read(obj, Map.of());
        assertThat(result.pgeyGroups().get(0).pgeyMaterial()).isEqualTo(PgeyObjMtlReader.PGEY_DEFAULT_MATERIAL);
        PgeyMeshData m = result.pgeyGroups().get(0).pgeyMesh();
        int shared = m.pgeyFaces()[0];
        int n = m.pgeyFaces()[1];
        assertThat(m.pgeyFaces()[1]).as("normal indexada por vértice").isEqualTo(shared);
        float[] nn = m.pgeyNormals();
        double len = Math.sqrt(nn[n * 3] * nn[n * 3] + nn[n * 3 + 1] * nn[n * 3 + 1] + nn[n * 3 + 2] * nn[n * 3 + 2]);
        assertThat(len).isCloseTo(1, within(1e-6));
        assertThat(nn[n * 3]).isCloseTo(-0.7071f, within(1e-3f));
        assertThat(nn[n * 3 + 1]).isCloseTo(0f, within(1e-6f));
        assertThat(nn[n * 3 + 2]).isCloseTo(-0.7071f, within(1e-3f));
    }

    @Test
    void usaLasNormalesDelObjSiLasTrae() throws IOException {
        String obj = """
                v 0 0 0
                v 1 0 0
                v 0 1 0
                vn 0 1 0
                f 1//1 2//1 3//1
                """;
        PgeyMeshData m = read(obj, Map.of()).pgeyGroups().get(0).pgeyMesh();
        int n = m.pgeyFaces()[1];
        assertThat(m.pgeyNormals()[n * 3 + 1]).as("Y arriba → Y abajo en JavaFX").isEqualTo(-1f);
        assertThat(m.pgeyTexCoords()).as("sin vt: una coordenada (0, 0) compartida").containsExactly(0f, 0f);
    }

    @Test
    void poligonosDeMasLadosSeTriangulanEnAbanico() throws IOException {
        String obj = """
                v 0 0 0
                v 1 0 0
                v 2 1 0
                v 1 2 0
                v 0 1 0
                f 1 2 3 4 5
                """;
        assertThat(read(obj, Map.of()).pgeyGroups().get(0).pgeyMesh().pgeyFaceCount()).isEqualTo(3);
    }

    @Test
    void leeElMtlIgnorandoDyConRutasRelativasALaCarpeta() throws IOException {
        Map<String, PgeyObjMtlReader.PgeyMaterial> m = PgeyObjMtlReader.pgeyReadMtl(new StringReader(MTL), "humanos/");
        PgeyObjMtlReader.PgeyMaterial piel = m.get("piel");
        assertThat(piel.pgeyDiffuse()).containsExactly(1, 0.95, 0.85);
        assertThat(piel.pgeySpecular()[0]).isEqualTo(0.02745);
        assertThat(piel.pgeyDiffuseMap()).isEqualTo("humanos/textures/piel.png");
        assertThat(piel.pgeyAlphaMap()).isEqualTo("humanos/textures/piel.png");
        PgeyObjMtlReader.PgeyMaterial ojos = m.get("ojos");
        assertThat(ojos.pgeyShininess()).isEqualTo(80);
        assertThat(ojos.pgeyDiffuseMap()).as("opciones antes de la ruta y barra invertida").isEqualTo("humanos/textures/ojo.png");
    }

    @Test
    void elMtlSeBuscaRelativoAlObjYLosMaterialesFaltantesTienenValoresNeutros() throws IOException {
        PgeyObjMtlReader.PgeyResult r = read(OBJ.replace("mtllib modelo.mtl", "mtllib sub/modelo.mtl"),
                Map.of("sub/modelo.mtl", MTL));
        assertThat(r.pgeyMaterial("piel").pgeyDiffuseMap()).isEqualTo("sub/textures/piel.png");
        PgeyObjMtlReader.PgeyMaterial missing = r.pgeyMaterial("no-existe");
        assertThat(missing.pgeyDiffuse()).containsExactly(1, 1, 1);
        assertThat(missing.pgeyDiffuseMap()).isNull();
    }

    @Test
    void sinMtlLaGeometriaIgualSeLee() throws IOException {
        PgeyObjMtlReader.PgeyResult r = read(OBJ, Map.of());
        assertThat(r.pgeyGroups()).hasSize(2);
        assertThat(r.pgeyMaterials()).isEmpty();
    }

    @Test
    void rechazaRutasQueSalenDeLaCarpetaDelModelo() throws IOException {
        assertThat(PgeyObjMtlReader.pgeySafePath("../secreto.png")).isNull();
        assertThat(PgeyObjMtlReader.pgeySafePath("textures/../../x.png")).isNull();
        assertThat(PgeyObjMtlReader.pgeySafePath("/etc/passwd")).isNull();
        assertThat(PgeyObjMtlReader.pgeySafePath("C:\\Windows\\x.png")).isNull();
        assertThat(PgeyObjMtlReader.pgeySafePath("./textures/a.png")).isEqualTo("textures/a.png");

        String evil = "newmtl x\nmap_Kd ../../fuera.png\n";
        assertThat(PgeyObjMtlReader.pgeyReadMtl(new StringReader(evil), "").get("x").pgeyDiffuseMap()).isNull();
        List<String> opened = new ArrayList<>();
        PgeyObjMtlReader.pgeyRead(new StringReader("mtllib ../fuera.mtl\nv 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n"), p -> {
            opened.add(p);
            return (Reader) null;
        });
        assertThat(opened).isEmpty();
    }

    @Test
    void erroresClarosConIndicesFueraDeRangoONumerosInvalidos() {
        assertThatThrownBy(() -> read("v 0 0 0\nf 1 2 3\n", Map.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("línea 2");
        assertThatThrownBy(() -> read("v 0 x 0\n", Map.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("línea 1");
        assertThatThrownBy(() -> read("v 0 0 0\nf 0 1 1\n", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> read("v 0 0 0\n", Map.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no tiene caras");
    }
}
