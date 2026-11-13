package com.sportforce3d.three.human;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class HumanModelsTest {

    @ParameterizedTest
    @EnumSource(PgeyPersona.class)
    void cadaPersonaTieneSuModeloConPielOjosYPelo(PgeyPersona persona) throws IOException {
        assertThat(PgeyHumanModels.pgeyAvailable(persona)).isTrue();
        PgeyObjMtlReader.PgeyResult r = PgeyHumanModels.pgeyReadGeometry(persona);

        assertThat(r.pgeyGroups()).hasSize(3);
        assertThat(r.pgeyGroups()).allSatisfy(g -> {
            assertThat(g.pgeyMesh().pgeyFaceCount()).isPositive();
            assertThat(r.pgeyMaterials()).as("material %s en el MTL", g.pgeyMaterial()).containsKey(g.pgeyMaterial());
        });
        assertThat(r.pgeyGroups()).anySatisfy(g -> assertThat(g.pgeyMaterial()).isEqualTo("Eye_brown"));
        assertThat(r.pgeyMin()[1]).isBetween(-0.01, 0.01);
        assertThat(r.pgeyMax()[2]).isGreaterThan(-r.pgeyMin()[2]);
        double expected = switch (persona) {
            case HOMBRE -> 1.88;
            case MUJER -> 1.59;
            case NINO -> 1.28;
            case NINA -> 1.19;
        };
        assertThat(r.pgeyHeight()).isBetween(expected - 0.02, expected + 0.02);

        for (PgeyObjMtlReader.PgeyGroup g : r.pgeyGroups()) {
            String map = r.pgeyMaterial(g.pgeyMaterial()).pgeyDiffuseMap();
            assertThat(map).startsWith("textures/");
            try (InputStream in = PgeyHumanModels.class.getResourceAsStream(persona.pgeyFolder() + map)) {
                assertThat(in).as("textura %s", map).isNotNull();
            }
        }
    }

    @Test
    void laPielEsOpacaYElPeloYLosOjosTienenAlfa() throws IOException {
        assertThat(PgeyHumanModels.pgeyPngHasAlpha(bytes("hombre/textures/young_lightskinned_male_diffuse.png"))).isFalse();
        assertThat(PgeyHumanModels.pgeyPngHasAlpha(bytes("hombre/textures/short02_diffuse.png"))).isTrue();
        assertThat(PgeyHumanModels.pgeyPngHasAlpha(bytes("hombre/textures/brown_eye.png"))).isTrue();
        assertThat(PgeyHumanModels.pgeyPngHasAlpha(new byte[]{1, 2, 3})).isFalse();
    }

    @Test
    void laPersonaSaleDeLaCategoriaRealista() {
        assertThat(PgeyPersona.pgeyFromModelKey("futbol_hombre_frente")).isEqualTo(PgeyPersona.HOMBRE);
        assertThat(PgeyPersona.pgeyFromModelKey("polo_mujer_frente")).isEqualTo(PgeyPersona.MUJER);
        assertThat(PgeyPersona.pgeyFromModelKey("futbol_nina_espalda")).isEqualTo(PgeyPersona.NINA);
        assertThat(PgeyPersona.pgeyFromModelKey("buzo_juvenil_frente")).isEqualTo(PgeyPersona.NINO);
        assertThat(PgeyPersona.pgeyFromModelKey("producto_medias")).isNull();
        assertThat(PgeyPersona.pgeyFromModelKey(null)).isNull();
    }

    private static byte[] bytes(String path) throws IOException {
        try (InputStream in = PgeyHumanModels.class.getResourceAsStream("/models/humanos/" + path)) {
            return in.readAllBytes();
        }
    }
}
