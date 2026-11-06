package com.sportforce3d.design;

import com.sportforce3d.three.model.BodyType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** La ficha técnica incluye la vista realista con los colores por zona en HEX. */
class TechSheetTest {

    @Test
    void realisticViewGoesToTheSheetAndTheCsv() {
        DesignDocument doc = DesignDefaults.document(BodyType.ADULTO_HOMBRE).withRealistic(RealisticSpec.defaults()
                .withModel("futbol_hombre_frente").withColor("camiseta", "#f2f5f8").withColor("short", "#0b0e13")
                .withPattern("banda").withPatternColor("#d7141e").withName("GUERRERO").withNumber(9));
        TechSheet sheet = TechSheet.of(doc, Map.of(), Map.of());

        TechSheet.Realistic r = sheet.realistic();
        assertThat(r.model()).isEqualTo("futbol_hombre_frente");
        assertThat(r.zones()).containsEntry("camiseta", "#F2F5F8").containsEntry("short", "#0B0E13");
        assertThat(r.pattern()).isEqualTo("banda");
        assertThat(r.patternColor()).isEqualTo("#D7141E");

        String csv = sheet.toCsv("PED-2026-000001");
        assertThat(csv).contains("VISTA REALISTA;Modelo;futbol_hombre_frente", ";Zona camiseta;#F2F5F8",
                ";Diseño;banda;#D7141E", ";Nombre;GUERRERO", ";Número;9");
    }

    @Test
    void cutGoesToTheSheetAndTheCsv() {
        DesignDocument doc = DesignDefaults.document(BodyType.ADULTO_HOMBRE)
                .withCut(GarmentCut.DEFAULT.withSleeve(GarmentCut.Sleeve.LARGA).withSocks(GarmentCut.SocksLength.MEDIA));
        TechSheet sheet = TechSheet.of(doc, Map.of(), Map.of());
        assertThat(sheet.cut().sleeve()).isEqualTo(GarmentCut.Sleeve.LARGA);
        assertThat(sheet.toCsv("x")).contains("CORTE;Manga;Larga", ";Largo del short;Medio (sobre la rodilla)",
                ";Largo de las medias;Media pierna (a media pantorrilla)");
        // Un diseño anterior a la Etapa 10 lleva el corte de siempre.
        assertThat(TechSheet.of(DesignDefaults.document(BodyType.ADULTO_HOMBRE), Map.of(), Map.of()).toCsv("x"))
                .contains("CORTE;Manga;Corta", ";Largo de las medias;Larga (bajo la rodilla)");
    }

    @Test
    void plainDesignsHaveNoPatternColorAndOldDesignsNoRealisticView() {
        DesignDocument plain = DesignDefaults.document(BodyType.ADULTO_HOMBRE);
        assertThat(TechSheet.of(plain, Map.of(), Map.of()).realistic().patternColor()).isNull();

        DesignDocument old = plain.withRealistic(null);
        TechSheet sheet = TechSheet.of(old, Map.of(), Map.of());
        assertThat(sheet.realistic()).isNull();
        assertThat(sheet.toCsv("x")).doesNotContain("VISTA REALISTA");
    }
}
