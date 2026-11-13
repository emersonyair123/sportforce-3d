package com.sportforce3d.design;

import com.sportforce3d.design.GarmentCut.ShortsLength;
import com.sportforce3d.design.GarmentCut.Sleeve;
import com.sportforce3d.design.GarmentCut.SocksLength;
import com.sportforce3d.model.Garment;
import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.Zone;
import com.sportforce3d.viewmodel.DesignerViewModel;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class ViewSyncTest {

    private static DesignDocument kit3d() {
        DesignDocument d = ViewSync.normalize(DesignDefaults.document(BodyType.ADULTO_HOMBRE))
                .withColor(Zone.JERSEY_BODY, "#D7192A").withColor(Zone.JERSEY_SLEEVES, "#D7192A")
                .withColor(Zone.JERSEY_SIDES, "#0B0E13").withColor(Zone.SHORTS_BASE, "#0B2A6B")
                .withColor(Zone.SOCKS_LEG, "#F2F5F8")
                .withColor(Zone.JERSEY_COLLAR, "#D7192A").withColor(Zone.JERSEY_CUFFS, "#D7192A")
                .withColor(Zone.SHORTS_STRIPE, "#0B2A6B").withColor(Zone.SHORTS_WAIST, "#0B2A6B")
                .withColor(Zone.SOCKS_BAND, "#F2F5F8")
                .withPattern(Garment.JERSEY, new PatternSpec(PatternType.FRANJAS_VERTICALES, "#F2F5F8", 0.045));
        d = d.addElement(TextSlot.BACK_NAME.create(d).withText("QUISPE"))
                .addElement(TextSlot.BACK_NUMBER.create(d).withText("9"))
                .addElement(TextSlot.CHEST_NUMBER.create(d).withText("9"))
                .addElement(DesignDefaults.emblem("escudo-clasico", "#0B0E13", "#F2F5F8"));
        return ViewSync.normalize(d);
    }

    @Test
    void the3dDesignIsWhatTheRealisticViewShows() {
        DesignDocument d = kit3d();
        RealisticView v = ViewSync.view(d);
        RealisticSpec s = v.spec();
        assertThat(s.modelKey()).isEqualTo("futbol_hombre_frente");
        assertThat(s.color("camiseta")).isEqualTo("#D7192A");
        assertThat(s.color("paneles")).isEqualTo("#0B0E13");
        assertThat(s.color("short")).isEqualTo("#0B2A6B");
        assertThat(s.color("medias")).isEqualTo("#F2F5F8");
        assertThat(s.pattern()).isEqualTo("rayas");
        assertThat(s.patternColor()).isEqualTo("#F2F5F8");
        assertThat(s.name()).isEqualTo("QUISPE");
        assertThat(s.number()).isEqualTo(9);
        DesignElement nameEl = TextSlot.BACK_NAME.find(d);
        assertThat(v.name().fill()).isEqualTo(nameEl.color());
        assertThat(v.name().outline()).isEqualTo(nameEl.accentColor());
        assertThat(v.chest().value()).isEqualTo("9");
        assertThat(v.crest().emblem()).isEqualTo("escudo-clasico");
        assertThat(v.notices()).isEmpty();
        assertThat(d.realistic()).as("lo guardado es lo que se ve").isEqualTo(s);
        assertThat(d.realisticPhotosMatch()).isTrue();
    }

    @Test
    void realisticChangesGoBackTo3d() {
        DesignDocument d = kit3d();
        RealisticSpec before = ViewSync.view(d).spec();
        RealisticSpec after = before.withColor("camiseta", "#1F5FFF").withColor("short", "#FFFFFF")
                .withColor("paneles", "#FFD400").withColor("medias", "#0B0E13")
                .withPattern("franjas").withPatternColor("#FFD400").withName("ROJAS").withNumber(7);
        DesignDocument out = ViewSync.applyRealistic(d, before, after);

        assertThat(out.color(Zone.JERSEY_BODY)).isEqualTo("#1F5FFF");
        assertThat(out.color(Zone.JERSEY_SLEEVES)).as("las mangas de la foto son la camiseta").isEqualTo("#1F5FFF");
        assertThat(out.color(Zone.JERSEY_SIDES)).isEqualTo("#FFD400");
        assertThat(out.color(Zone.SHORTS_BASE)).isEqualTo("#FFFFFF");
        assertThat(out.color(Zone.SOCKS_LEG)).isEqualTo("#0B0E13");
        assertThat(out.pattern(Garment.JERSEY).type()).isEqualTo(PatternType.FRANJAS_HORIZONTALES);
        assertThat(out.pattern(Garment.JERSEY).color()).isEqualTo("#FFD400");
        assertThat(TextSlot.BACK_NAME.find(out).text()).isEqualTo("ROJAS");
        assertThat(TextSlot.BACK_NUMBER.find(out).text()).isEqualTo("7");
        assertThat(TextSlot.CHEST_NUMBER.find(out).text()).isEqualTo("7");
        assertThat(ViewSync.view(out).spec()).isEqualTo(after.withColor("principal", "#1F5FFF"));

        RealisticSpec cleared = ViewSync.view(out).spec().withName(null).withNumber(null);
        DesignDocument none = ViewSync.applyRealistic(out, ViewSync.view(out).spec(), cleared);
        assertThat(TextSlot.BACK_NAME.find(none)).isNull();
        assertThat(TextSlot.BACK_NUMBER.find(none)).isNull();
        assertThat(TextSlot.CHEST_NUMBER.find(none)).isNull();
    }

    @Test
    void cutEquivalences() {
        DesignDocument d = kit3d();
        DesignDocument longSleeve = ViewSync.normalize(d.withCut(d.cut().withSleeve(Sleeve.LARGA)));
        assertThat(ViewSync.view(longSleeve).modelKey()).isEqualTo("mangalarga_hombre_frente");
        assertThat(ViewSync.view(longSleeve).notices()).isEmpty();
        DesignDocument longShorts = ViewSync.normalize(d.withCut(d.cut().withShorts(ShortsLength.LARGO)));
        assertThat(ViewSync.view(longShorts).modelKey()).isEqualTo("shortlargo_hombre_frente");
        RealisticSpec s = ViewSync.view(d).spec();
        DesignDocument fromPhoto = ViewSync.applyRealistic(d, s, s.withModel("mangalarga_hombre_frente"));
        assertThat(fromPhoto.cut().sleeve()).isEqualTo(Sleeve.LARGA);
        DesignDocument fromShortPhoto = ViewSync.applyRealistic(d, s, s.withModel("shortlargo_hombre_frente"));
        assertThat(fromShortPhoto.cut().shorts()).isEqualTo(ShortsLength.LARGO);
        DesignDocument back = ViewSync.applyRealistic(fromPhoto, ViewSync.view(fromPhoto).spec(),
                ViewSync.view(fromPhoto).spec().withModel("futbol_hombre_frente"));
        assertThat(back.cut().sleeve()).isEqualTo(Sleeve.CORTA);

        DesignDocument woman = ViewSync.applyRealistic(d, s, s.withModel("futbol_mujer_frente"));
        woman = ViewSync.normalize(woman.withCut(new GarmentCut(Sleeve.LARGA, ShortsLength.CORTO, SocksLength.TOBILLERA)));
        RealisticView wv = ViewSync.view(woman);
        assertThat(wv.modelKey()).isEqualTo("futbol_mujer_frente");
        assertThat(wv.notices()).contains("Manga larga: ver en Vista 3D", "Short corto: ver en Vista 3D",
                "Medias tobilleras: ver en Vista 3D");
        assertThat(woman.realisticPhotosMatch()).as("las fotos del pedido salen del 3D").isFalse();
        assertThat(wv.spec().color("camiseta")).isEqualTo("#D7192A");
    }

    @Test
    void photoOnlyCutsShowTheClosest3dCutWithANotice() {
        DesignDocument d = kit3d();
        RealisticSpec s = ViewSync.view(d).spec();
        DesignDocument polo = ViewSync.applyRealistic(d, s, s.withModel("polo_hombre_frente"));
        assertThat(ViewSync.view(polo).modelKey()).isEqualTo("polo_hombre_frente");
        assertThat(ViewSync.notices3d(polo)).anyMatch(n -> n.startsWith("Polo: la Vista 3D muestra"));
        assertThat(polo.cut()).isEqualTo(GarmentCut.DEFAULT);
        DesignDocument buzo = ViewSync.applyRealistic(d, s, s.withModel("buzo_hombre_frente"));
        assertThat(buzo.cut().sleeve()).isEqualTo(Sleeve.LARGA);
        assertThat(buzo.cut().shorts()).isEqualTo(ShortsLength.LARGO);
        assertThat(ViewSync.notices3d(buzo)).anyMatch(n -> n.startsWith("Buzo:"));
        assertThat(ViewSync.notices3d(d)).isEmpty();
    }

    @Test
    void whatThePhotoCannotShowGivesANoticeNeverOtherColors() {
        DesignDocument d = kit3d();
        RealisticSpec s = ViewSync.view(d).spec();
        DesignDocument dyn = ViewSync.applyRealistic(d, s, s.withPattern("dinamico"));
        assertThat(dyn.pattern(Garment.JERSEY).type()).isEqualTo(PatternType.LISO);
        assertThat(ViewSync.view(dyn).spec().pattern()).isEqualTo("dinamico");
        assertThat(ViewSync.notices3d(dyn)).anyMatch(n -> n.contains("dinámico"));
        DesignDocument striped = ViewSync.normalize(dyn.withPattern(Garment.JERSEY,
                dyn.pattern(Garment.JERSEY).withType(PatternType.FRANJAS_VERTICALES)));
        assertThat(ViewSync.view(striped).spec().pattern()).isEqualTo("rayas");

        DesignDocument checks = ViewSync.normalize(d.withPattern(Garment.JERSEY,
                d.pattern(Garment.JERSEY).withType(PatternType.CUADROS)));
        assertThat(ViewSync.view(checks).spec().pattern()).isEqualTo("liso");
        assertThat(ViewSync.view(checks).notices()).contains("Diseño cuadros: ver en Vista 3D");

        DesignDocument other = ViewSync.normalize(d.withMaterial(MaterialSlot.JERSEY, "MALLA")
                .withColor(Zone.JERSEY_SLEEVES, "#FFFFFF")
                .replaceElement(TextSlot.BACK_NAME.find(d).at(0.5, 0.6)));
        assertThat(ViewSync.view(other).notices()).contains("Tela malla deportiva: ver en Vista 3D",
                "Mangas de otro color: ver en Vista 3D", "Posición del nombre: ver en Vista 3D");
        DesignDocument collar = ViewSync.normalize(d.withColor(Zone.JERSEY_COLLAR, "#39FF88"));
        assertThat(ViewSync.view(collar).notices()).contains("Color del cuello y los puños: ver en Vista 3D");
        assertThat(collar.realisticPhotosMatch()).isFalse();
    }

    @Test
    void oneObservableDocumentForBothViewsInTheViewModel() {
        DesignerViewModel vm = new DesignerViewModel(null, null, Clock.systemUTC());
        vm.edit("Color", d -> d.withColor(Zone.JERSEY_BODY, "#D7192A"), null);
        vm.edit("Manga larga", d -> d.withCut(d.cut().withSleeve(Sleeve.LARGA)), null);
        assertThat(vm.realistic().color("camiseta")).isEqualTo("#D7192A");
        assertThat(vm.realistic().modelKey()).isEqualTo("mangalarga_hombre_frente");
        assertThat(vm.doc().realistic()).isEqualTo(vm.realistic());
        vm.editRealistic("Nombre", s -> s.withName("GUERRERO").withNumber(10), null);
        assertThat(TextSlot.BACK_NAME.find(vm.doc()).text()).isEqualTo("GUERRERO");
        assertThat(TextSlot.CHEST_NUMBER.find(vm.doc()).text()).isEqualTo("10");
        vm.editRealistic("Categoría", s -> s.withModel("futbol_mujer_frente"), null);
        assertThat(vm.personaProperty().get()).hasToString("Mujer");
        assertThat(vm.realisticView().notices()).contains("Manga larga: ver en Vista 3D");
        vm.undo();
        vm.undo();
        assertThat(TextSlot.BACK_NAME.find(vm.doc())).isNull();
        assertThat(vm.realistic().name()).isNull();
    }

    @Test
    void designsSavedBeforeStage11OpenWithBothViewsInSync() {
        DesignDocument old = DesignDefaults.document(BodyType.ADULTO_HOMBRE).withRealistic(new RealisticSpec(
                "futbol_hombre_frente", java.util.Map.of("camiseta", "#D7141E", "short", "#0B0E13",
                "paneles", "#0B0E13", "medias", "#D7141E"), "rayas", "#F2F5F8", "QUISPE", 10));
        String v3 = DesignJson.write(old).replace("\"schemaVersion\":4", "\"schemaVersion\":3");
        DesignDocument up = DesignJson.read(v3);
        assertThat(up.color(Zone.JERSEY_BODY)).as("gana la vista con que se hicieron las fotos").isEqualTo("#D7141E");
        assertThat(up.pattern(Garment.JERSEY).type()).isEqualTo(PatternType.FRANJAS_VERTICALES);
        assertThat(TextSlot.BACK_NAME.find(up).text()).isEqualTo("QUISPE");
        assertThat(ViewSync.view(up).spec()).isEqualTo(up.realistic());
    }
}
