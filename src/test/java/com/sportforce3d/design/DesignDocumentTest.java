package com.sportforce3d.design;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.Zone;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DesignDocumentTest {

    @Test
    void jsonRoundTripKeepsEverything() {
        DesignDocument doc = SampleDesigns.halcones()
                .withBodyType(BodyType.ADULTO_MUJER)
                .withProduct(4L)
                .addElement(DesignDefaults.logo(7, "uploads/abc.png").withRotation(30).withScale(1.4));

        String json = DesignJson.write(doc);
        DesignDocument back = DesignJson.read(json);

        assertThat(back).isEqualTo(doc);
        assertThat(json).contains("\"schemaVersion\":4", "\"JERSEY_BODY\":\"#D7192A\"", "FRANJAS_VERTICALES", "QUISPE");
        assertThat(back.elements()).extracting(DesignElement::id)
                .containsExactlyElementsOf(doc.elements().stream().map(DesignElement::id).toList());
    }

    @Test
    void cutRoundTripsAndDesignsBeforeStage10OpenWithTheUsualCut() {
        GarmentCut cut = new GarmentCut(GarmentCut.Sleeve.LARGA, GarmentCut.ShortsLength.CORTO,
                GarmentCut.SocksLength.TOBILLERA);
        DesignDocument doc = SampleDesigns.halcones().withCut(cut);
        String json = DesignJson.write(doc);
        assertThat(json).contains("\"sleeve\":\"LARGA\"", "\"shorts\":\"CORTO\"", "\"socks\":\"TOBILLERA\"");
        assertThat(DesignJson.read(json).cut()).isEqualTo(cut);
        assertThat(doc.withColor(Zone.JERSEY_BODY, "#000000").cut()).isEqualTo(cut);

        String v2 = DesignJson.write(SampleDesigns.halcones()).replace("\"schemaVersion\":4", "\"schemaVersion\":2")
                .replaceAll(",?\"cut\":\\{[^}]*\\}", "");
        assertThat(v2).doesNotContain("\"cut\"");
        DesignDocument old = DesignJson.read(v2);
        assertThat(old.schemaVersion()).isEqualTo(DesignDocument.SCHEMA_VERSION);
        assertThat(old.cut()).isEqualTo(GarmentCut.DEFAULT);
        assertThat(old.withCut(null).cut()).isEqualTo(GarmentCut.DEFAULT);
    }

    @Test
    void shortsNumberFollowsTheShortsLengthUnlessTheClientMovedIt() {
        DesignElement auto = DesignDefaults.shortsNumber("7", "#FFFFFF", "#000000");
        DesignElement moved = DesignDefaults.shortsNumber("8", "#FFFFFF", "#000000").at(0.4, 0.8);
        assertThat(auto.u()).isEqualTo(GarmentCut.ShortsLength.MEDIO.numberU());
        DesignDocument doc = DesignDefaults.document(BodyType.ADULTO_HOMBRE).addElement(auto).addElement(moved);
        assertThat(auto.v()).isEqualTo(GarmentCut.ShortsLength.MEDIO.numberV());

        DesignDocument corto = doc.withCut(doc.cut().withShorts(GarmentCut.ShortsLength.CORTO));
        assertThat(corto.element(auto.id()).orElseThrow().v()).isEqualTo(GarmentCut.ShortsLength.CORTO.numberV());
        assertThat(corto.element(auto.id()).orElseThrow().u()).isEqualTo(GarmentCut.ShortsLength.CORTO.numberU());
        assertThat(corto.element(moved.id()).orElseThrow().v()).isEqualTo(0.8);
        DesignDocument largo = corto.withCut(corto.cut().withShorts(GarmentCut.ShortsLength.LARGO));
        assertThat(largo.element(auto.id()).orElseThrow().v()).isEqualTo(GarmentCut.ShortsLength.LARGO.numberV());
        assertThat(largo.withCut(largo.cut().withSleeve(GarmentCut.Sleeve.LARGA)).elements()).isEqualTo(largo.elements());
        assertThat(DesignDefaults.shortsNumber("9", "#FFFFFF", "#000000", GarmentCut.ShortsLength.CORTO).v())
                .isEqualTo(GarmentCut.ShortsLength.CORTO.numberV());
    }

    @Test
    void rejectsNewerOrForeignJson() {
        String newer = DesignJson.write(DesignDefaults.document(BodyType.JUVENIL)).replace("\"schemaVersion\":4", "\"schemaVersion\":9");
        assertThatThrownBy(() -> DesignJson.read(newer)).hasMessageContaining("versión más nueva");
        assertThatThrownBy(() -> DesignJson.read("{\"hola\":1}")).hasMessageContaining("no es un diseño");
        assertThatThrownBy(() -> DesignJson.read("{no es json")).hasMessageContaining("dañado");
    }

    @Test
    void realisticViewRoundTripsAndOldDesignsStillOpen() {
        DesignDocument doc = DesignDefaults.document(BodyType.ADULTO_MUJER).withRealistic(RealisticSpec.defaults()
                .withModel("voley_mujer_frente").withColor("camiseta", "#d7141e").withPattern("banda")
                .withPatternColor("#f2f5f8").withName("QUISPE").withNumber(7));
        DesignDocument back = DesignJson.read(DesignJson.write(doc));
        assertThat(back).isEqualTo(doc);
        assertThat(back.realistic().color("camiseta")).isEqualTo("#D7141E");
        assertThat(back.realistic().modelKey()).isEqualTo("voley_mujer_frente");

        String v1 = "{\"schemaVersion\":1,\"bodyType\":\"JUVENIL\",\"productId\":4,"
                + "\"colors\":{\"JERSEY_BODY\":\"#D7192A\"},\"patterns\":{},\"materials\":{},\"elements\":[]}";
        DesignDocument old = DesignJson.read(v1);
        assertThat(old.schemaVersion()).isEqualTo(DesignDocument.SCHEMA_VERSION);
        assertThat(old.bodyType()).isEqualTo(BodyType.JUVENIL);
        assertThat(old.color(Zone.JERSEY_BODY)).isEqualTo("#D7192A");
        assertThat(old.realistic()).isNull();
        assertThat(old.realisticOrDefault()).isEqualTo(RealisticSpec.defaults());
        assertThat(DesignRules.problems(old)).isEmpty();
    }

    @Test
    void realisticRulesRejectLongNamesBadNumbersAndColors() {
        RealisticSpec ok = RealisticSpec.defaults().withName("ABCDEFGHIJKL").withNumber(99);
        assertThat(ok.problems()).isEmpty();
        assertThat(ok.withName("ABCDEFGHIJKLM").problems()).isNotEmpty();
        assertThat(ok.withNumber(100).problems()).isNotEmpty();
        assertThat(ok.withColor("short", "rojo").problems()).isNotEmpty();
        assertThat(ok.withPattern("cuadros").problems()).isNotEmpty();
        assertThat(ok.withPattern("rayas").withPatternColor(null).problems()).isNotEmpty();
        assertThat(DesignRules.problems(DesignDefaults.document(BodyType.ADULTO_HOMBRE)
                .withRealistic(ok.withNumber(-1)))).isNotEmpty();
    }

    @Test
    void ignoresUnknownFieldsFromLaterMinorChanges() {
        String json = DesignJson.write(DesignDefaults.document(BodyType.ADULTO_HOMBRE)).replaceFirst("\\{", "{\"campoNuevo\":true,");
        assertThat(DesignJson.read(json).bodyType()).isEqualTo(BodyType.ADULTO_HOMBRE);
    }

    @Test
    void documentsAreImmutableAndChangesReturnCopies() {
        DesignDocument a = DesignDefaults.document(BodyType.ADULTO_HOMBRE);
        DesignDocument b = a.withColor(Zone.JERSEY_BODY, "#ff7a00");
        assertThat(a.color(Zone.JERSEY_BODY)).isEqualTo("#1F5FFF");
        assertThat(b.color(Zone.JERSEY_BODY)).isEqualTo("#FF7A00");
        assertThat(a.pattern(Garment.JERSEY).type()).isEqualTo(PatternType.LISO);
        assertThat(a.material(MaterialSlot.SOCKS)).isEqualTo("LICRA");
    }

    @Test
    void elementsClampScaleRotationAndPosition() {
        DesignElement e = DesignDefaults.emblem("rayo", null, null).withScale(5).withRotation(270).at(1.7, -3);
        assertThat(e.scale()).isEqualTo(DesignElement.MAX_SCALE);
        assertThat(e.rotation()).isEqualTo(-90);
        assertThat(e.u()).isEqualTo(1);
        assertThat(e.v()).isEqualTo(0);
        assertThat(DesignDefaults.emblem("rayo", null, null).withScale(0.01).scale()).isEqualTo(DesignElement.MIN_SCALE);
    }

    @Test
    void layersMoveUpAndDown() {
        DesignElement a = DesignDefaults.name("A", "#FFFFFF", "#000000");
        DesignElement b = DesignDefaults.backNumber("9", "#FFFFFF", "#000000");
        DesignDocument d = DesignDefaults.document(BodyType.ADULTO_HOMBRE).addElement(a).addElement(b);
        assertThat(d.moveLayer(a.id(), 1).elements()).extracting(DesignElement::id).containsExactly(b.id(), a.id());
        assertThat(d.moveLayer(a.id(), -1).elements()).extracting(DesignElement::id).containsExactly(a.id(), b.id());
        assertThat(d.removeElement(a.id()).elements()).containsExactly(b);
    }

    @Test
    void rulesValidateNamesNumbersColorsAndEmblems() {
        assertThat(DesignRules.cleanName("guerrero-pérez 2026!")).isEqualTo("GUERRERO-PÉR").hasSize(DesignRules.NAME_MAX);
        assertThat(DesignRules.cleanNumber("007")).isEqualTo("0");
        assertThat(DesignRules.cleanNumber("99")).isEqualTo("99");
        assertThat(DesignRules.cleanNumber("abc")).isNull();

        assertThat(DesignRules.problems(SampleDesigns.halcones())).isEmpty();
        DesignDocument bad = DesignDefaults.document(BodyType.ADULTO_HOMBRE)
                .withColor(Zone.JERSEY_BODY, "azul")
                .addElement(DesignDefaults.backNumber("100", "#FFFFFF", "#000000"))
                .addElement(DesignDefaults.name("NOMBREDEMASIADOLARGO", "#FFFFFF", "#000000"))
                .addElement(DesignDefaults.emblem("marca-famosa", null, null));
        assertThat(DesignRules.problems(bad)).hasSize(4);
    }
}
