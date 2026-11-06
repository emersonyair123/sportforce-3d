package com.sportforce3d.render;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecolorRequestTest {

    @Test
    void normalizesColorsAndName() {
        RecolorRequest r = new RecolorRequest("futbol_hombre_espalda", Map.of("short", "#0B2A6B", "camiseta", "#FFFFFF"),
                RealisticPattern.RAYAS, "#D7141E", "  pérez ", 9, null);
        assertThat(r.zoneColors()).containsExactly(Map.entry("camiseta", "#ffffff"), Map.entry("short", "#0b2a6b"));
        assertThat(r.patternColor()).isEqualTo("#d7141e");
        assertThat(r.name()).isEqualTo("PÉREZ");
        assertThat(r.withModel("futbol_hombre_frente").cacheKey()).startsWith("futbol_hombre_frente|");
    }

    @Test
    void plainPatternIgnoresItsColorAndBlankTextIsNull() {
        RecolorRequest r = new RecolorRequest("buzo_hombre_frente", Map.of(), RealisticPattern.LISO, "nada", " ", null, null);
        assertThat(r.patternColor()).isNull();
        assertThat(r.name()).isNull();
        assertThat(r.hasOverlay()).isFalse();
    }

    @Test
    void rejectsInvalidValues() {
        assertThatThrownBy(() -> RecolorRequest.of("x", Map.of("camiseta", "rojo"))).hasMessageContaining("Color");
        assertThatThrownBy(() -> new RecolorRequest("x", Map.of(), RealisticPattern.BANDA, null, null, null, null))
                .hasMessageContaining("Color");
        assertThatThrownBy(() -> new RecolorRequest("x", Map.of(), null, null, "ABCDEFGHIJKLM", null, null))
                .hasMessageContaining("12");
        assertThatThrownBy(() -> new RecolorRequest("x", Map.of(), null, null, null, 100, null))
                .hasMessageContaining("99");
        assertThatThrownBy(() -> new RecolorRequest("x", Map.of(), null, null, "A\u0007B", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RecolorRequest.of(" ", Map.of())).hasMessageContaining("modelo");
    }

    @Test
    void patternCodesMatchTheCatalog() {
        assertThat(RealisticPattern.fromCode(null)).isEqualTo(RealisticPattern.LISO);
        assertThat(RealisticPattern.fromCode("dinamico")).isEqualTo(RealisticPattern.DINAMICO);
        assertThat(RealisticPattern.fromCode("Banda")).isEqualTo(RealisticPattern.BANDA);
        assertThatThrownBy(() -> RealisticPattern.fromCode("cuadros")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void automaticTextColorContrastsWithTheShirt() {
        assertThat(DesignOverlay.textColor(RecolorRequest.of("x", Map.of("camiseta", "#f2f5f8"))))
                .isEqualTo(DesignOverlay.DARK_TEXT);
        assertThat(DesignOverlay.textColor(RecolorRequest.of("x", Map.of("camiseta", "#0b2a6b"))))
                .isEqualTo(DesignOverlay.LIGHT_TEXT);
    }
}
