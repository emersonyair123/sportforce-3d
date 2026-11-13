package com.sportforce3d.view;

import com.sportforce3d.design.RealisticSpec;
import com.sportforce3d.render.RealisticModel;
import com.sportforce3d.render.RealisticModelRepository;
import com.sportforce3d.render.RealisticPattern;
import com.sportforce3d.render.RecolorRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RealisticDesignerTest {

    private final RealisticModelRepository models = new RealisticModelRepository();

    private List<String> cuts(String audience) {
        return RealisticDesigner.CUTS.keySet().stream()
                .filter(cut -> models.find(RealisticDesigner.key(cut, audience)).isPresent())
                .collect(Collectors.toList());
    }

    @Test
    void eachCategoryShowsOnlyTheCutsThatHavePhotos() {
        assertThat(RealisticDesigner.CATEGORIES.values()).containsExactly("Hombre", "Mujer", "Niño", "Niña");
        assertThat(RealisticDesigner.CUTS.values()).containsExactly("Fútbol cuello V", "Polo", "Cuello redondo",
                "Raglán", "Manga larga", "Vóley", "Básquet", "Buzo", "Short largo", "Licra", "Arquero", "Compresión", "Casaca");
        assertThat(cuts("hombre")).containsExactly("futbol", "polo", "redondo", "mangalarga", "voley", "basquet",
                "buzo", "shortlargo", "arquero", "compresion", "casaca");
        assertThat(cuts("mujer")).containsExactly("futbol", "raglan", "voley", "buzo", "licra");
        assertThat(cuts("nino")).containsExactly("futbol", "buzo");
        assertThat(cuts("nina")).containsExactly("futbol", "buzo");
    }

    @Test
    void shirtsHaveFrontAndBackAndHoodiesOnlyFront() {
        java.util.Set<String> frontOnly = java.util.Set.of("buzo", "arquero", "compresion", "casaca");
        for (String audience : RealisticDesigner.CATEGORIES.keySet()) {
            for (String cut : cuts(audience)) {
                RealisticModel front = models.require(RealisticDesigner.key(cut, audience));
                assertThat(models.otherView(front).isPresent()).as(front.key()).isEqualTo(!frontOnly.contains(cut));
            }
        }
    }

    @Test
    void requestCarriesColorsPatternAndTexts() {
        RealisticSpec spec = RealisticSpec.defaults().withPattern("rayas").withPatternColor("#0B2A6B")
                .withName("QUISPE").withNumber(9);
        RecolorRequest req = RealisticPreview.request("futbol_hombre_espalda", spec);
        assertThat(req.modelKey()).isEqualTo("futbol_hombre_espalda");
        assertThat(req.pattern()).isEqualTo(RealisticPattern.RAYAS);
        assertThat(req.patternColor()).isEqualTo("#0b2a6b");
        assertThat(req.name()).isEqualTo("QUISPE");
        assertThat(req.number()).isEqualTo(9);
        assertThat(req.zoneColors()).containsEntry("camiseta", "#1f5fff");

        RecolorRequest plain = RealisticPreview.request("futbol_hombre_frente", RealisticSpec.defaults());
        assertThat(plain.pattern()).isEqualTo(RealisticPattern.LISO);
        assertThat(plain.patternColor()).isNull();
        assertThat(plain.hasOverlay()).isFalse();
    }

    @Test
    void zoneLabelsFollowTheCut() {
        assertThat(RealisticDesigner.zoneLabel(models.require("raglan_mujer_frente"), "paneles")).isEqualTo("Mangas");
        assertThat(RealisticDesigner.zoneLabel(models.require("futbol_hombre_frente"), "paneles")).isEqualTo("Paneles");
        assertThat(RealisticDesigner.zoneLabel(models.require("buzo_hombre_frente"), "principal")).isEqualTo("Buzo");
    }
}
