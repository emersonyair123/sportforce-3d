package com.sportforce3d.three.human;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Las cuatro personas empaquetadas se leen completas: geometría, materiales y texturas existentes. */
class HumanModelsTest {

    @ParameterizedTest
    @EnumSource(Persona.class)
    void cadaPersonaTieneSuModeloConPielOjosYPelo(Persona persona) throws IOException {
        Modelos3d.requierePersonas();
        assertThat(HumanModels.available(persona)).isTrue();
        ObjMtlReader.Result r = HumanModels.readGeometry(persona);

        assertThat(r.groups()).hasSize(3);
        assertThat(r.groups()).allSatisfy(g -> {
            assertThat(g.mesh().faceCount()).isPositive();
            assertThat(r.materials()).as("material %s en el MTL", g.material()).containsKey(g.material());
        });
        assertThat(r.groups()).anySatisfy(g -> assertThat(g.material()).isEqualTo("Eye_brown"));
        // Los pies tocan el piso y el frente mira a +Z (la nariz está más adelante que los talones).
        assertThat(r.min()[1]).isBetween(-0.01, 0.01);
        assertThat(r.max()[2]).isGreaterThan(-r.min()[2]);
        double expected = switch (persona) {
            case HOMBRE -> 1.88;
            case MUJER -> 1.59;
            case NINO -> 1.28;
            case NINA -> 1.19;
        };
        assertThat(r.height()).isBetween(expected - 0.02, expected + 0.02);

        for (ObjMtlReader.Group g : r.groups()) {
            String map = r.material(g.material()).diffuseMap();
            assertThat(map).startsWith("textures/");
            try (InputStream in = HumanModels.class.getResourceAsStream(persona.folder() + map)) {
                assertThat(in).as("textura %s", map).isNotNull();
            }
        }
    }

    @Test
    void laPielEsOpacaYElPeloYLosOjosTienenAlfa() throws IOException {
        Modelos3d.requierePersonas();
        assertThat(HumanModels.pngHasAlpha(bytes("hombre/textures/young_lightskinned_male_diffuse.png"))).isFalse();
        assertThat(HumanModels.pngHasAlpha(bytes("hombre/textures/short02_diffuse.png"))).isTrue();
        assertThat(HumanModels.pngHasAlpha(bytes("hombre/textures/brown_eye.png"))).isTrue();
        assertThat(HumanModels.pngHasAlpha(new byte[]{1, 2, 3})).isFalse();
    }

    @Test
    void laPersonaSaleDeLaCategoriaRealista() {
        assertThat(Persona.fromModelKey("futbol_hombre_frente")).isEqualTo(Persona.HOMBRE);
        assertThat(Persona.fromModelKey("polo_mujer_frente")).isEqualTo(Persona.MUJER);
        assertThat(Persona.fromModelKey("futbol_nina_espalda")).isEqualTo(Persona.NINA);
        assertThat(Persona.fromModelKey("buzo_juvenil_frente")).isEqualTo(Persona.NINO);
        assertThat(Persona.fromModelKey("producto_medias")).isNull();
        assertThat(Persona.fromModelKey(null)).isNull();
    }

    private static byte[] bytes(String path) throws IOException {
        try (InputStream in = HumanModels.class.getResourceAsStream("/models/humanos/" + path)) {
            return in.readAllBytes();
        }
    }
}
