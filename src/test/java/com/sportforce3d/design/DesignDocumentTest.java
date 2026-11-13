package com.sportforce3d.design;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyZone;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DesignDocumentTest {

    @Test
    void jsonRoundTripKeepsEverything() {
        PgeyDesignDocument doc = PgeySampleDesigns.pgeyHalcones()
                .pgeyWithBodyType(PgeyBodyType.ADULTO_MUJER)
                .pgeyWithProduct(4L)
                .pgeyAddElement(PgeyDesignDefaults.pgeyLogo(7, "uploads/abc.png").pgeyWithRotation(30).pgeyWithScale(1.4));

        String json = PgeyDesignJson.pgeyWrite(doc);
        PgeyDesignDocument back = PgeyDesignJson.pgeyRead(json);

        assertThat(back).isEqualTo(doc);
        assertThat(json).contains("\"schemaVersion\":4", "\"JERSEY_BODY\":\"#D7192A\"", "FRANJAS_VERTICALES", "QUISPE");
        assertThat(back.pgeyElements()).extracting(PgeyDesignElement::pgeyId)
                .containsExactlyElementsOf(doc.pgeyElements().stream().map(PgeyDesignElement::pgeyId).toList());
    }

    @Test
    void cutRoundTripsAndDesignsBeforeStage10OpenWithTheUsualCut() {
        PgeyGarmentCut cut = new PgeyGarmentCut(PgeyGarmentCut.PgeySleeve.LARGA, PgeyGarmentCut.PgeyShortsLength.CORTO,
                PgeyGarmentCut.PgeySocksLength.TOBILLERA);
        PgeyDesignDocument doc = PgeySampleDesigns.pgeyHalcones().pgeyWithCut(cut);
        String json = PgeyDesignJson.pgeyWrite(doc);
        assertThat(json).contains("\"sleeve\":\"LARGA\"", "\"shorts\":\"CORTO\"", "\"socks\":\"TOBILLERA\"");
        assertThat(PgeyDesignJson.pgeyRead(json).pgeyCut()).isEqualTo(cut);
        assertThat(doc.pgeyWithColor(PgeyZone.JERSEY_BODY, "#000000").pgeyCut()).isEqualTo(cut);

        String v2 = PgeyDesignJson.pgeyWrite(PgeySampleDesigns.pgeyHalcones()).replace("\"schemaVersion\":4", "\"schemaVersion\":2")
                .replaceAll(",?\"cut\":\\{[^}]*\\}", "");
        assertThat(v2).doesNotContain("\"cut\"");
        PgeyDesignDocument old = PgeyDesignJson.pgeyRead(v2);
        assertThat(old.pgeySchemaVersion()).isEqualTo(PgeyDesignDocument.PGEY_SCHEMA_VERSION);
        assertThat(old.pgeyCut()).isEqualTo(PgeyGarmentCut.PGEY_DEFAULT);
        assertThat(old.pgeyWithCut(null).pgeyCut()).isEqualTo(PgeyGarmentCut.PGEY_DEFAULT);
    }

    @Test
    void shortsNumberFollowsTheShortsLengthUnlessTheClientMovedIt() {
        PgeyDesignElement auto = PgeyDesignDefaults.pgeyShortsNumber("7", "#FFFFFF", "#000000");
        PgeyDesignElement moved = PgeyDesignDefaults.pgeyShortsNumber("8", "#FFFFFF", "#000000").pgeyAt(0.4, 0.8);
        assertThat(auto.pgeyU()).isEqualTo(PgeyGarmentCut.PgeyShortsLength.MEDIO.pgeyNumberU());
        PgeyDesignDocument doc = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE).pgeyAddElement(auto).pgeyAddElement(moved);
        assertThat(auto.pgeyV()).isEqualTo(PgeyGarmentCut.PgeyShortsLength.MEDIO.pgeyNumberV());

        PgeyDesignDocument corto = doc.pgeyWithCut(doc.pgeyCut().pgeyWithShorts(PgeyGarmentCut.PgeyShortsLength.CORTO));
        assertThat(corto.pgeyElement(auto.pgeyId()).orElseThrow().pgeyV()).isEqualTo(PgeyGarmentCut.PgeyShortsLength.CORTO.pgeyNumberV());
        assertThat(corto.pgeyElement(auto.pgeyId()).orElseThrow().pgeyU()).isEqualTo(PgeyGarmentCut.PgeyShortsLength.CORTO.pgeyNumberU());
        assertThat(corto.pgeyElement(moved.pgeyId()).orElseThrow().pgeyV()).isEqualTo(0.8);
        PgeyDesignDocument largo = corto.pgeyWithCut(corto.pgeyCut().pgeyWithShorts(PgeyGarmentCut.PgeyShortsLength.LARGO));
        assertThat(largo.pgeyElement(auto.pgeyId()).orElseThrow().pgeyV()).isEqualTo(PgeyGarmentCut.PgeyShortsLength.LARGO.pgeyNumberV());
        assertThat(largo.pgeyWithCut(largo.pgeyCut().pgeyWithSleeve(PgeyGarmentCut.PgeySleeve.LARGA)).pgeyElements()).isEqualTo(largo.pgeyElements());
        assertThat(PgeyDesignDefaults.pgeyShortsNumber("9", "#FFFFFF", "#000000", PgeyGarmentCut.PgeyShortsLength.CORTO).pgeyV())
                .isEqualTo(PgeyGarmentCut.PgeyShortsLength.CORTO.pgeyNumberV());
    }

    @Test
    void rejectsNewerOrForeignJson() {
        String newer = PgeyDesignJson.pgeyWrite(PgeyDesignDefaults.pgeyDocument(PgeyBodyType.JUVENIL)).replace("\"schemaVersion\":4", "\"schemaVersion\":9");
        assertThatThrownBy(() -> PgeyDesignJson.pgeyRead(newer)).hasMessageContaining("versión más nueva");
        assertThatThrownBy(() -> PgeyDesignJson.pgeyRead("{\"hola\":1}")).hasMessageContaining("no es un diseño");
        assertThatThrownBy(() -> PgeyDesignJson.pgeyRead("{no es json")).hasMessageContaining("dañado");
    }

    @Test
    void realisticViewRoundTripsAndOldDesignsStillOpen() {
        PgeyDesignDocument doc = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_MUJER).pgeyWithRealistic(PgeyRealisticSpec.pgeyDefaults()
                .pgeyWithModel("voley_mujer_frente").pgeyWithColor("camiseta", "#d7141e").pgeyWithPattern("banda")
                .pgeyWithPatternColor("#f2f5f8").pgeyWithName("QUISPE").pgeyWithNumber(7));
        PgeyDesignDocument back = PgeyDesignJson.pgeyRead(PgeyDesignJson.pgeyWrite(doc));
        assertThat(back).isEqualTo(doc);
        assertThat(back.pgeyRealistic().pgeyColor("camiseta")).isEqualTo("#D7141E");
        assertThat(back.pgeyRealistic().pgeyModelKey()).isEqualTo("voley_mujer_frente");

        String v1 = "{\"schemaVersion\":1,\"bodyType\":\"JUVENIL\",\"productId\":4,"
                + "\"colors\":{\"JERSEY_BODY\":\"#D7192A\"},\"patterns\":{},\"materials\":{},\"elements\":[]}";
        PgeyDesignDocument old = PgeyDesignJson.pgeyRead(v1);
        assertThat(old.pgeySchemaVersion()).isEqualTo(PgeyDesignDocument.PGEY_SCHEMA_VERSION);
        assertThat(old.pgeyBodyType()).isEqualTo(PgeyBodyType.JUVENIL);
        assertThat(old.pgeyColor(PgeyZone.JERSEY_BODY)).isEqualTo("#D7192A");
        assertThat(old.pgeyRealistic()).isNull();
        assertThat(old.pgeyRealisticOrDefault()).isEqualTo(PgeyRealisticSpec.pgeyDefaults());
        assertThat(PgeyDesignRules.pgeyProblems(old)).isEmpty();
    }

    @Test
    void realisticRulesRejectLongNamesBadNumbersAndColors() {
        PgeyRealisticSpec ok = PgeyRealisticSpec.pgeyDefaults().pgeyWithName("ABCDEFGHIJKL").pgeyWithNumber(99);
        assertThat(ok.pgeyProblems()).isEmpty();
        assertThat(ok.pgeyWithName("ABCDEFGHIJKLM").pgeyProblems()).isNotEmpty();
        assertThat(ok.pgeyWithNumber(100).pgeyProblems()).isNotEmpty();
        assertThat(ok.pgeyWithColor("short", "rojo").pgeyProblems()).isNotEmpty();
        assertThat(ok.pgeyWithPattern("cuadros").pgeyProblems()).isNotEmpty();
        assertThat(ok.pgeyWithPattern("rayas").pgeyWithPatternColor(null).pgeyProblems()).isNotEmpty();
        assertThat(PgeyDesignRules.pgeyProblems(PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE)
                .pgeyWithRealistic(ok.pgeyWithNumber(-1)))).isNotEmpty();
    }

    @Test
    void ignoresUnknownFieldsFromLaterMinorChanges() {
        String json = PgeyDesignJson.pgeyWrite(PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE)).replaceFirst("\\{", "{\"campoNuevo\":true,");
        assertThat(PgeyDesignJson.pgeyRead(json).pgeyBodyType()).isEqualTo(PgeyBodyType.ADULTO_HOMBRE);
    }

    @Test
    void documentsAreImmutableAndChangesReturnCopies() {
        PgeyDesignDocument a = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE);
        PgeyDesignDocument b = a.pgeyWithColor(PgeyZone.JERSEY_BODY, "#ff7a00");
        assertThat(a.pgeyColor(PgeyZone.JERSEY_BODY)).isEqualTo("#1F5FFF");
        assertThat(b.pgeyColor(PgeyZone.JERSEY_BODY)).isEqualTo("#FF7A00");
        assertThat(a.pgeyPattern(HepyGarment.JERSEY).pgeyType()).isEqualTo(PgeyPatternType.LISO);
        assertThat(a.pgeyMaterial(PgeyMaterialSlot.SOCKS)).isEqualTo("LICRA");
    }

    @Test
    void elementsClampScaleRotationAndPosition() {
        PgeyDesignElement e = PgeyDesignDefaults.pgeyEmblem("rayo", null, null).pgeyWithScale(5).pgeyWithRotation(270).pgeyAt(1.7, -3);
        assertThat(e.pgeyScale()).isEqualTo(PgeyDesignElement.PGEY_MAX_SCALE);
        assertThat(e.pgeyRotation()).isEqualTo(-90);
        assertThat(e.pgeyU()).isEqualTo(1);
        assertThat(e.pgeyV()).isEqualTo(0);
        assertThat(PgeyDesignDefaults.pgeyEmblem("rayo", null, null).pgeyWithScale(0.01).pgeyScale()).isEqualTo(PgeyDesignElement.PGEY_MIN_SCALE);
    }

    @Test
    void layersMoveUpAndDown() {
        PgeyDesignElement a = PgeyDesignDefaults.pgeyName("A", "#FFFFFF", "#000000");
        PgeyDesignElement b = PgeyDesignDefaults.pgeyBackNumber("9", "#FFFFFF", "#000000");
        PgeyDesignDocument d = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE).pgeyAddElement(a).pgeyAddElement(b);
        assertThat(d.pgeyMoveLayer(a.pgeyId(), 1).pgeyElements()).extracting(PgeyDesignElement::pgeyId).containsExactly(b.pgeyId(), a.pgeyId());
        assertThat(d.pgeyMoveLayer(a.pgeyId(), -1).pgeyElements()).extracting(PgeyDesignElement::pgeyId).containsExactly(a.pgeyId(), b.pgeyId());
        assertThat(d.pgeyRemoveElement(a.pgeyId()).pgeyElements()).containsExactly(b);
    }

    @Test
    void rulesValidateNamesNumbersColorsAndEmblems() {
        assertThat(PgeyDesignRules.pgeyCleanName("guerrero-pérez 2026!")).isEqualTo("GUERRERO-PÉR").hasSize(PgeyDesignRules.PGEY_NAME_MAX);
        assertThat(PgeyDesignRules.pgeyCleanNumber("007")).isEqualTo("0");
        assertThat(PgeyDesignRules.pgeyCleanNumber("99")).isEqualTo("99");
        assertThat(PgeyDesignRules.pgeyCleanNumber("abc")).isNull();

        assertThat(PgeyDesignRules.pgeyProblems(PgeySampleDesigns.pgeyHalcones())).isEmpty();
        PgeyDesignDocument bad = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE)
                .pgeyWithColor(PgeyZone.JERSEY_BODY, "azul")
                .pgeyAddElement(PgeyDesignDefaults.pgeyBackNumber("100", "#FFFFFF", "#000000"))
                .pgeyAddElement(PgeyDesignDefaults.pgeyName("NOMBREDEMASIADOLARGO", "#FFFFFF", "#000000"))
                .pgeyAddElement(PgeyDesignDefaults.pgeyEmblem("marca-famosa", null, null));
        assertThat(PgeyDesignRules.pgeyProblems(bad)).hasSize(4);
    }
}
