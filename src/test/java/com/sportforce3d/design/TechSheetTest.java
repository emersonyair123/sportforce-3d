package com.sportforce3d.design;

import com.sportforce3d.three.model.PgeyBodyType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TechSheetTest {

    @Test
    void realisticViewGoesToTheSheetAndTheCsv() {
        PgeyDesignDocument doc = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE).pgeyWithRealistic(PgeyRealisticSpec.pgeyDefaults()
                .pgeyWithModel("futbol_hombre_frente").pgeyWithColor("camiseta", "#f2f5f8").pgeyWithColor("short", "#0b0e13")
                .pgeyWithPattern("banda").pgeyWithPatternColor("#d7141e").pgeyWithName("GUERRERO").pgeyWithNumber(9));
        PgeyTechSheet sheet = PgeyTechSheet.pgeyOf(doc, Map.of(), Map.of());

        PgeyTechSheet.PgeyRealistic r = sheet.pgeyRealistic();
        assertThat(r.pgeyModel()).isEqualTo("futbol_hombre_frente");
        assertThat(r.pgeyZones()).containsEntry("camiseta", "#F2F5F8").containsEntry("short", "#0B0E13");
        assertThat(r.pgeyPattern()).isEqualTo("banda");
        assertThat(r.pgeyPatternColor()).isEqualTo("#D7141E");

        String csv = sheet.pgeyToCsv("PED-2026-000001");
        assertThat(csv).contains("VISTA REALISTA;Modelo;futbol_hombre_frente", ";Zona camiseta;#F2F5F8",
                ";Diseño;banda;#D7141E", ";Nombre;GUERRERO", ";Número;9");
    }

    @Test
    void cutGoesToTheSheetAndTheCsv() {
        PgeyDesignDocument doc = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE)
                .pgeyWithCut(PgeyGarmentCut.PGEY_DEFAULT.pgeyWithSleeve(PgeyGarmentCut.PgeySleeve.LARGA).pgeyWithSocks(PgeyGarmentCut.PgeySocksLength.MEDIA));
        PgeyTechSheet sheet = PgeyTechSheet.pgeyOf(doc, Map.of(), Map.of());
        assertThat(sheet.pgeyCut().pgeySleeve()).isEqualTo(PgeyGarmentCut.PgeySleeve.LARGA);
        assertThat(sheet.pgeyToCsv("x")).contains("CORTE;Manga;Larga", ";Largo del short;Medio (sobre la rodilla)",
                ";Largo de las medias;Media pierna (a media pantorrilla)");
        assertThat(PgeyTechSheet.pgeyOf(PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE), Map.of(), Map.of()).pgeyToCsv("x"))
                .contains("CORTE;Manga;Corta", ";Largo de las medias;Larga (bajo la rodilla)");
    }

    @Test
    void plainDesignsHaveNoPatternColorAndOldDesignsNoRealisticView() {
        PgeyDesignDocument plain = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE);
        assertThat(PgeyTechSheet.pgeyOf(plain, Map.of(), Map.of()).pgeyRealistic().pgeyPatternColor()).isNull();

        PgeyDesignDocument old = plain.pgeyWithRealistic(null);
        PgeyTechSheet sheet = PgeyTechSheet.pgeyOf(old, Map.of(), Map.of());
        assertThat(sheet.pgeyRealistic()).isNull();
        assertThat(sheet.pgeyToCsv("x")).doesNotContain("VISTA REALISTA");
    }
}
