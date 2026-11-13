package com.sportforce3d.render;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecolorRequestTest {

    @Test
    void normalizesColorsAndName() {
        PgeyRecolorRequest r = new PgeyRecolorRequest("futbol_hombre_espalda", Map.of("short", "#0B2A6B", "camiseta", "#FFFFFF"),
                PgeyRealisticPattern.RAYAS, "#D7141E", "  pérez ", 9, null);
        assertThat(r.pgeyZoneColors()).containsExactly(Map.entry("camiseta", "#ffffff"), Map.entry("short", "#0b2a6b"));
        assertThat(r.pgeyPatternColor()).isEqualTo("#d7141e");
        assertThat(r.pgeyName()).isEqualTo("PÉREZ");
        assertThat(r.pgeyWithModel("futbol_hombre_frente").pgeyCacheKey()).startsWith("futbol_hombre_frente|");
    }

    @Test
    void plainPatternIgnoresItsColorAndBlankTextIsNull() {
        PgeyRecolorRequest r = new PgeyRecolorRequest("buzo_hombre_frente", Map.of(), PgeyRealisticPattern.LISO, "nada", " ", null, null);
        assertThat(r.pgeyPatternColor()).isNull();
        assertThat(r.pgeyName()).isNull();
        assertThat(r.pgeyHasOverlay()).isFalse();
    }

    @Test
    void rejectsInvalidValues() {
        assertThatThrownBy(() -> PgeyRecolorRequest.pgeyOf("x", Map.of("camiseta", "rojo"))).hasMessageContaining("Color");
        assertThatThrownBy(() -> new PgeyRecolorRequest("x", Map.of(), PgeyRealisticPattern.BANDA, null, null, null, null))
                .hasMessageContaining("Color");
        assertThatThrownBy(() -> new PgeyRecolorRequest("x", Map.of(), null, null, "ABCDEFGHIJKLM", null, null))
                .hasMessageContaining("12");
        assertThatThrownBy(() -> new PgeyRecolorRequest("x", Map.of(), null, null, null, 100, null))
                .hasMessageContaining("99");
        assertThatThrownBy(() -> new PgeyRecolorRequest("x", Map.of(), null, null, "A\u0007B", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PgeyRecolorRequest.pgeyOf(" ", Map.of())).hasMessageContaining("modelo");
    }

    @Test
    void patternCodesMatchTheCatalog() {
        assertThat(PgeyRealisticPattern.pgeyFromCode(null)).isEqualTo(PgeyRealisticPattern.LISO);
        assertThat(PgeyRealisticPattern.pgeyFromCode("dinamico")).isEqualTo(PgeyRealisticPattern.DINAMICO);
        assertThat(PgeyRealisticPattern.pgeyFromCode("Banda")).isEqualTo(PgeyRealisticPattern.BANDA);
        assertThatThrownBy(() -> PgeyRealisticPattern.pgeyFromCode("cuadros")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void automaticTextColorContrastsWithTheShirt() {
        assertThat(PgeyDesignOverlay.pgeyTextColor(PgeyRecolorRequest.pgeyOf("x", Map.of("camiseta", "#f2f5f8"))))
                .isEqualTo(PgeyDesignOverlay.PGEY_DARK_TEXT);
        assertThat(PgeyDesignOverlay.pgeyTextColor(PgeyRecolorRequest.pgeyOf("x", Map.of("camiseta", "#0b2a6b"))))
                .isEqualTo(PgeyDesignOverlay.PGEY_LIGHT_TEXT);
    }
}
