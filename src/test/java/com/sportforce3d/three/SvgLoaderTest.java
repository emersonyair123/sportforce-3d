package com.sportforce3d.three;

import com.sportforce3d.design.EmblemCatalog;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SvgLoaderTest {

    @Test
    void everyLibraryEmblemParses() throws Exception {
        for (String key : EmblemCatalog.EMBLEMS.keySet()) {
            try (InputStream in = SvgLoaderTest.class.getResourceAsStream(EmblemCatalog.resource(key))) {
                assertThat(in).as(key).isNotNull();
                SvgLoader.SvgDoc doc = SvgLoader.parse(in);
                assertThat(doc.width()).isEqualTo(120);
                assertThat(doc.shapes()).as(key).isNotEmpty();
            }
        }
    }

    @Test
    void resolvesInheritedStylesAndStyleAttribute() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 20">
                  <g fill="#1f5fff" opacity="0.5">
                    <rect x="0" y="0" width="10" height="10"/>
                    <circle cx="20" cy="5" r="4" style="fill:#39ff88;stroke:#000;stroke-width:2"/>
                  </g>
                  <polygon points="0,0 10,0 5,8"/>
                </svg>""";
        SvgLoader.SvgDoc doc = SvgLoader.parse(stream(svg));
        assertThat(doc.shapes()).hasSize(3);
        assertThat(doc.shapes().get(0).fill()).isEqualTo("#1f5fff");
        assertThat(doc.shapes().get(0).opacity()).isEqualTo(0.5);
        assertThat(doc.shapes().get(1).fill()).isEqualTo("#39ff88");
        assertThat(doc.shapes().get(1).strokeWidth()).isEqualTo(2);
        assertThat(doc.shapes().get(2).fill()).as("SVG pinta negro por defecto").isEqualTo("#000000");
    }

    @Test
    void rejectsDoctypesAndNonSvg() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE svg [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\"><text>&x;</text></svg>";
        assertThatThrownBy(() -> SvgLoader.parse(stream(xxe))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SvgLoader.parse(stream("<html/>"))).hasMessageContaining("no es un SVG");
        assertThatThrownBy(() -> SvgLoader.parse(stream("<svg xmlns=\"http://www.w3.org/2000/svg\"/>")))
                .hasMessageContaining("PNG");
    }

    @Test
    void recolorReplacesExactColors() {
        assertThat(SvgLoader.paint("#1f5fff", java.util.Map.of("#1F5FFF", "#D7192A")).toString()).isEqualTo("0xd7192aff");
        assertThat(SvgLoader.paint("none", java.util.Map.of())).isNull();
    }

    private static InputStream stream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
