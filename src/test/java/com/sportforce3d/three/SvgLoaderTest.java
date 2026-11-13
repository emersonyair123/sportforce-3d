package com.sportforce3d.three;

import com.sportforce3d.design.PgeyEmblemCatalog;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SvgLoaderTest {

    @Test
    void everyLibraryEmblemParses() throws Exception {
        for (String key : PgeyEmblemCatalog.PGEY_EMBLEMS.keySet()) {
            try (InputStream in = SvgLoaderTest.class.getResourceAsStream(PgeyEmblemCatalog.pgeyResource(key))) {
                assertThat(in).as(key).isNotNull();
                PgeySvgLoader.PgeySvgDoc doc = PgeySvgLoader.pgeyParse(in);
                assertThat(doc.pgeyWidth()).isEqualTo(120);
                assertThat(doc.pgeyShapes()).as(key).isNotEmpty();
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
        PgeySvgLoader.PgeySvgDoc doc = PgeySvgLoader.pgeyParse(stream(svg));
        assertThat(doc.pgeyShapes()).hasSize(3);
        assertThat(doc.pgeyShapes().get(0).pgeyFill()).isEqualTo("#1f5fff");
        assertThat(doc.pgeyShapes().get(0).pgeyOpacity()).isEqualTo(0.5);
        assertThat(doc.pgeyShapes().get(1).pgeyFill()).isEqualTo("#39ff88");
        assertThat(doc.pgeyShapes().get(1).pgeyStrokeWidth()).isEqualTo(2);
        assertThat(doc.pgeyShapes().get(2).pgeyFill()).as("SVG pinta negro por defecto").isEqualTo("#000000");
    }

    @Test
    void rejectsDoctypesAndNonSvg() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE svg [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\"><text>&x;</text></svg>";
        assertThatThrownBy(() -> PgeySvgLoader.pgeyParse(stream(xxe))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PgeySvgLoader.pgeyParse(stream("<html/>"))).hasMessageContaining("no es un SVG");
        assertThatThrownBy(() -> PgeySvgLoader.pgeyParse(stream("<svg xmlns=\"http://www.w3.org/2000/svg\"/>")))
                .hasMessageContaining("PNG");
    }

    @Test
    void recolorReplacesExactColors() {
        assertThat(PgeySvgLoader.pgeyPaint("#1f5fff", java.util.Map.of("#1F5FFF", "#D7192A")).toString()).isEqualTo("0xd7192aff");
        assertThat(PgeySvgLoader.pgeyPaint("none", java.util.Map.of())).isNull();
    }

    private static InputStream stream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
