package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.MeshData;
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

    /** Un cuadrado (dos materiales: un cuadro de piel y un triángulo de ojos), sin normales como MakeHuman. */
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

    private static ObjMtlReader.Result read(String obj, Map<String, String> files) throws IOException {
        return ObjMtlReader.read(new StringReader(obj), path -> {
            String text = files.get(path);
            return text == null ? null : new StringReader(text);
        });
    }

    @Test
    void creaUnaMallaPorMaterialEnElOrdenDelObj() throws IOException {
        ObjMtlReader.Result r = read(OBJ, Map.of("modelo.mtl", MTL));

        assertThat(r.groups()).extracting(ObjMtlReader.Group::material).containsExactly("piel", "ojos");
        MeshData piel = r.groups().get(0).mesh();
        assertThat(piel.faceCount()).as("el cuadro se divide en dos triángulos").isEqualTo(2);
        assertThat(piel.pointCount()).as("solo los vértices que usa el material").isEqualTo(4);
        MeshData ojos = r.groups().get(1).mesh();
        assertThat(ojos.faceCount()).isEqualTo(1);
        assertThat(ojos.pointCount()).isEqualTo(3);
    }

    @Test
    void convierteACoordenadasDeJavaFxEInvierteV() throws IOException {
        ObjMtlReader.Result r = read(OBJ, Map.of("modelo.mtl", MTL));
        MeshData piel = r.groups().get(0).mesh();
        // Vértice 3 (1, 2, 0) → (1, -2, -0); su vt (1, 1) → (1, 0).
        int corner = 2;
        int p = piel.faces()[corner * 3];
        int t = piel.faces()[corner * 3 + 2];
        assertThat(piel.points()[p * 3]).isEqualTo(1f);
        assertThat(piel.points()[p * 3 + 1]).isEqualTo(-2f);
        assertThat(piel.texCoords()[t * 2]).isEqualTo(1f);
        assertThat(piel.texCoords()[t * 2 + 1]).isEqualTo(0f);

        assertThat(r.min()).containsExactly(0, 0, 0);
        assertThat(r.max()).containsExactly(1, 2, 1);
        assertThat(r.height()).isEqualTo(2);
    }

    @Test
    void calculaNormalesSuavesUnitariasCuandoElObjNoTrae() throws IOException {
        MeshData piel = read(OBJ, Map.of("modelo.mtl", MTL)).groups().get(0).mesh();
        // Cuadro plano en z = 0 con caras antihorarias vistas desde +Z (el frente): normal (0, 0, 1) → JavaFX (0, 0, -1).
        for (int c = 0; c < 6; c++) {
            int n = piel.faces()[c * 3 + 1];
            assertThat(piel.normals()[n * 3]).isCloseTo(0f, within(1e-6f));
            assertThat(piel.normals()[n * 3 + 1]).isCloseTo(0f, within(1e-6f));
            assertThat(piel.normals()[n * 3 + 2]).isCloseTo(-1f, within(1e-6f));
        }
    }

    @Test
    void lasNormalesSePromedianEntreCarasVecinas() throws IOException {
        // Dos caras en ángulo recto que comparten la arista 1–2: la normal suave del vértice compartido queda a 45°.
        String obj = """
                v 0 0 0
                v 0 1 0
                v 1 0 0
                v 0 0 -1
                f 1 3 2
                f 1 2 4
                """;
        ObjMtlReader.Result result = read(obj, Map.of());
        assertThat(result.groups().get(0).material()).isEqualTo(ObjMtlReader.DEFAULT_MATERIAL);
        MeshData m = result.groups().get(0).mesh();
        int shared = m.faces()[0];
        int n = m.faces()[1];
        assertThat(m.faces()[1]).as("normal indexada por vértice").isEqualTo(shared);
        float[] nn = m.normals();
        double len = Math.sqrt(nn[n * 3] * nn[n * 3] + nn[n * 3 + 1] * nn[n * 3 + 1] + nn[n * 3 + 2] * nn[n * 3 + 2]);
        assertThat(len).isCloseTo(1, within(1e-6));
        // OBJ: (0,0,1) y (-1,0,0) → promedio (-0.707, 0, 0.707) → JavaFX (-0.707, 0, -0.707).
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
        MeshData m = read(obj, Map.of()).groups().get(0).mesh();
        int n = m.faces()[1];
        assertThat(m.normals()[n * 3 + 1]).as("Y arriba → Y abajo en JavaFX").isEqualTo(-1f);
        assertThat(m.texCoords()).as("sin vt: una coordenada (0, 0) compartida").containsExactly(0f, 0f);
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
        assertThat(read(obj, Map.of()).groups().get(0).mesh().faceCount()).isEqualTo(3);
    }

    @Test
    void leeElMtlIgnorandoDyConRutasRelativasALaCarpeta() throws IOException {
        Map<String, ObjMtlReader.Material> m = ObjMtlReader.readMtl(new StringReader(MTL), "humanos/");
        ObjMtlReader.Material piel = m.get("piel");
        assertThat(piel.diffuse()).containsExactly(1, 0.95, 0.85);
        assertThat(piel.specular()[0]).isEqualTo(0.02745);
        assertThat(piel.diffuseMap()).isEqualTo("humanos/textures/piel.png");
        assertThat(piel.alphaMap()).isEqualTo("humanos/textures/piel.png");
        ObjMtlReader.Material ojos = m.get("ojos");
        assertThat(ojos.shininess()).isEqualTo(80);
        assertThat(ojos.diffuseMap()).as("opciones antes de la ruta y barra invertida").isEqualTo("humanos/textures/ojo.png");
    }

    @Test
    void elMtlSeBuscaRelativoAlObjYLosMaterialesFaltantesTienenValoresNeutros() throws IOException {
        ObjMtlReader.Result r = read(OBJ.replace("mtllib modelo.mtl", "mtllib sub/modelo.mtl"),
                Map.of("sub/modelo.mtl", MTL));
        assertThat(r.material("piel").diffuseMap()).isEqualTo("sub/textures/piel.png");
        ObjMtlReader.Material missing = r.material("no-existe");
        assertThat(missing.diffuse()).containsExactly(1, 1, 1);
        assertThat(missing.diffuseMap()).isNull();
    }

    @Test
    void sinMtlLaGeometriaIgualSeLee() throws IOException {
        ObjMtlReader.Result r = read(OBJ, Map.of());
        assertThat(r.groups()).hasSize(2);
        assertThat(r.materials()).isEmpty();
    }

    @Test
    void rechazaRutasQueSalenDeLaCarpetaDelModelo() throws IOException {
        assertThat(ObjMtlReader.safePath("../secreto.png")).isNull();
        assertThat(ObjMtlReader.safePath("textures/../../x.png")).isNull();
        assertThat(ObjMtlReader.safePath("/etc/passwd")).isNull();
        assertThat(ObjMtlReader.safePath("C:\\Windows\\x.png")).isNull();
        assertThat(ObjMtlReader.safePath("./textures/a.png")).isEqualTo("textures/a.png");

        String evil = "newmtl x\nmap_Kd ../../fuera.png\n";
        assertThat(ObjMtlReader.readMtl(new StringReader(evil), "").get("x").diffuseMap()).isNull();
        List<String> opened = new ArrayList<>();
        ObjMtlReader.read(new StringReader("mtllib ../fuera.mtl\nv 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n"), p -> {
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
