package com.sportforce3d.view;

import com.sportforce3d.design.PgeyRealisticSpec;
import com.sportforce3d.render.PgeyRealisticModel;
import com.sportforce3d.render.PgeyRealisticModelRepository;
import com.sportforce3d.render.PgeyRealisticPattern;
import com.sportforce3d.render.PgeyRecolorRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RealisticDesignerTest {

    private final PgeyRealisticModelRepository models = new PgeyRealisticModelRepository();

    private List<String> cuts(String audience) {
        return PgeyRealisticDesigner.PGEY_CUTS.keySet().stream()
                .filter(cut -> models.pgeyFind(PgeyRealisticDesigner.pgeyKey(cut, audience)).isPresent())
                .collect(Collectors.toList());
    }

    @Test
    void eachCategoryShowsOnlyTheCutsThatHavePhotos() {
        assertThat(PgeyRealisticDesigner.PGEY_CATEGORIES.values()).containsExactly("Hombre", "Mujer", "Niño", "Niña");
        assertThat(PgeyRealisticDesigner.PGEY_CUTS.values()).containsExactly("Fútbol cuello V", "Polo", "Cuello redondo",
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
        for (String audience : PgeyRealisticDesigner.PGEY_CATEGORIES.keySet()) {
            for (String cut : cuts(audience)) {
                PgeyRealisticModel front = models.pgeyRequire(PgeyRealisticDesigner.pgeyKey(cut, audience));
                assertThat(models.pgeyOtherView(front).isPresent()).as(front.pgeyKey()).isEqualTo(!frontOnly.contains(cut));
            }
        }
    }

    @Test
    void requestCarriesColorsPatternAndTexts() {
        PgeyRealisticSpec spec = PgeyRealisticSpec.pgeyDefaults().pgeyWithPattern("rayas").pgeyWithPatternColor("#0B2A6B")
                .pgeyWithName("QUISPE").pgeyWithNumber(9);
        PgeyRecolorRequest req = PgeyRealisticPreview.pgeyRequest("futbol_hombre_espalda", spec);
        assertThat(req.pgeyModelKey()).isEqualTo("futbol_hombre_espalda");
        assertThat(req.pgeyPattern()).isEqualTo(PgeyRealisticPattern.RAYAS);
        assertThat(req.pgeyPatternColor()).isEqualTo("#0b2a6b");
        assertThat(req.pgeyName()).isEqualTo("QUISPE");
        assertThat(req.pgeyNumber()).isEqualTo(9);
        assertThat(req.pgeyZoneColors()).containsEntry("camiseta", "#1f5fff");

        PgeyRecolorRequest plain = PgeyRealisticPreview.pgeyRequest("futbol_hombre_frente", PgeyRealisticSpec.pgeyDefaults());
        assertThat(plain.pgeyPattern()).isEqualTo(PgeyRealisticPattern.LISO);
        assertThat(plain.pgeyPatternColor()).isNull();
        assertThat(plain.pgeyHasOverlay()).isFalse();
    }

    @Test
    void zoneLabelsFollowTheCut() {
        assertThat(PgeyRealisticDesigner.pgeyZoneLabel(models.pgeyRequire("raglan_mujer_frente"), "paneles")).isEqualTo("Mangas");
        assertThat(PgeyRealisticDesigner.pgeyZoneLabel(models.pgeyRequire("futbol_hombre_frente"), "paneles")).isEqualTo("Paneles");
        assertThat(PgeyRealisticDesigner.pgeyZoneLabel(models.pgeyRequire("buzo_hombre_frente"), "principal")).isEqualTo("Buzo");
    }
}
