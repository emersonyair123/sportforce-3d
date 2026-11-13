package com.sportforce3d.design;

import com.sportforce3d.design.PgeyGarmentCut.PgeyShortsLength;
import com.sportforce3d.design.PgeyGarmentCut.PgeySleeve;
import com.sportforce3d.design.PgeyGarmentCut.PgeySocksLength;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyZone;
import com.sportforce3d.viewmodel.PgeyDesignerViewModel;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class ViewSyncTest {

    private static PgeyDesignDocument kit3d() {
        PgeyDesignDocument d = PgeyViewSync.pgeyNormalize(PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE))
                .pgeyWithColor(PgeyZone.JERSEY_BODY, "#D7192A").pgeyWithColor(PgeyZone.JERSEY_SLEEVES, "#D7192A")
                .pgeyWithColor(PgeyZone.JERSEY_SIDES, "#0B0E13").pgeyWithColor(PgeyZone.SHORTS_BASE, "#0B2A6B")
                .pgeyWithColor(PgeyZone.SOCKS_LEG, "#F2F5F8")
                .pgeyWithColor(PgeyZone.JERSEY_COLLAR, "#D7192A").pgeyWithColor(PgeyZone.JERSEY_CUFFS, "#D7192A")
                .pgeyWithColor(PgeyZone.SHORTS_STRIPE, "#0B2A6B").pgeyWithColor(PgeyZone.SHORTS_WAIST, "#0B2A6B")
                .pgeyWithColor(PgeyZone.SOCKS_BAND, "#F2F5F8")
                .pgeyWithPattern(HepyGarment.JERSEY, new PgeyPatternSpec(PgeyPatternType.FRANJAS_VERTICALES, "#F2F5F8", 0.045));
        d = d.pgeyAddElement(PgeyTextSlot.BACK_NAME.pgeyCreate(d).pgeyWithText("QUISPE"))
                .pgeyAddElement(PgeyTextSlot.BACK_NUMBER.pgeyCreate(d).pgeyWithText("9"))
                .pgeyAddElement(PgeyTextSlot.CHEST_NUMBER.pgeyCreate(d).pgeyWithText("9"))
                .pgeyAddElement(PgeyDesignDefaults.pgeyEmblem("escudo-clasico", "#0B0E13", "#F2F5F8"));
        return PgeyViewSync.pgeyNormalize(d);
    }

    @Test
    void the3dDesignIsWhatTheRealisticViewShows() {
        PgeyDesignDocument d = kit3d();
        PgeyRealisticView v = PgeyViewSync.pgeyView(d);
        PgeyRealisticSpec s = v.pgeySpec();
        assertThat(s.pgeyModelKey()).isEqualTo("futbol_hombre_frente");
        assertThat(s.pgeyColor("camiseta")).isEqualTo("#D7192A");
        assertThat(s.pgeyColor("paneles")).isEqualTo("#0B0E13");
        assertThat(s.pgeyColor("short")).isEqualTo("#0B2A6B");
        assertThat(s.pgeyColor("medias")).isEqualTo("#F2F5F8");
        assertThat(s.pgeyPattern()).isEqualTo("rayas");
        assertThat(s.pgeyPatternColor()).isEqualTo("#F2F5F8");
        assertThat(s.pgeyName()).isEqualTo("QUISPE");
        assertThat(s.pgeyNumber()).isEqualTo(9);
        PgeyDesignElement nameEl = PgeyTextSlot.BACK_NAME.pgeyFind(d);
        assertThat(v.pgeyName().pgeyFill()).isEqualTo(nameEl.pgeyColor());
        assertThat(v.pgeyName().pgeyOutline()).isEqualTo(nameEl.pgeyAccentColor());
        assertThat(v.pgeyChest().pgeyValue()).isEqualTo("9");
        assertThat(v.pgeyCrest().pgeyEmblem()).isEqualTo("escudo-clasico");
        assertThat(v.pgeyNotices()).isEmpty();
        assertThat(d.pgeyRealistic()).as("lo guardado es lo que se ve").isEqualTo(s);
        assertThat(d.pgeyRealisticPhotosMatch()).isTrue();
    }

    @Test
    void realisticChangesGoBackTo3d() {
        PgeyDesignDocument d = kit3d();
        PgeyRealisticSpec before = PgeyViewSync.pgeyView(d).pgeySpec();
        PgeyRealisticSpec after = before.pgeyWithColor("camiseta", "#1F5FFF").pgeyWithColor("short", "#FFFFFF")
                .pgeyWithColor("paneles", "#FFD400").pgeyWithColor("medias", "#0B0E13")
                .pgeyWithPattern("franjas").pgeyWithPatternColor("#FFD400").pgeyWithName("ROJAS").pgeyWithNumber(7);
        PgeyDesignDocument out = PgeyViewSync.pgeyApplyRealistic(d, before, after);

        assertThat(out.pgeyColor(PgeyZone.JERSEY_BODY)).isEqualTo("#1F5FFF");
        assertThat(out.pgeyColor(PgeyZone.JERSEY_SLEEVES)).as("las mangas de la foto son la camiseta").isEqualTo("#1F5FFF");
        assertThat(out.pgeyColor(PgeyZone.JERSEY_SIDES)).isEqualTo("#FFD400");
        assertThat(out.pgeyColor(PgeyZone.SHORTS_BASE)).isEqualTo("#FFFFFF");
        assertThat(out.pgeyColor(PgeyZone.SOCKS_LEG)).isEqualTo("#0B0E13");
        assertThat(out.pgeyPattern(HepyGarment.JERSEY).pgeyType()).isEqualTo(PgeyPatternType.FRANJAS_HORIZONTALES);
        assertThat(out.pgeyPattern(HepyGarment.JERSEY).pgeyColor()).isEqualTo("#FFD400");
        assertThat(PgeyTextSlot.BACK_NAME.pgeyFind(out).pgeyText()).isEqualTo("ROJAS");
        assertThat(PgeyTextSlot.BACK_NUMBER.pgeyFind(out).pgeyText()).isEqualTo("7");
        assertThat(PgeyTextSlot.CHEST_NUMBER.pgeyFind(out).pgeyText()).isEqualTo("7");
        assertThat(PgeyViewSync.pgeyView(out).pgeySpec()).isEqualTo(after.pgeyWithColor("principal", "#1F5FFF"));

        PgeyRealisticSpec cleared = PgeyViewSync.pgeyView(out).pgeySpec().pgeyWithName(null).pgeyWithNumber(null);
        PgeyDesignDocument none = PgeyViewSync.pgeyApplyRealistic(out, PgeyViewSync.pgeyView(out).pgeySpec(), cleared);
        assertThat(PgeyTextSlot.BACK_NAME.pgeyFind(none)).isNull();
        assertThat(PgeyTextSlot.BACK_NUMBER.pgeyFind(none)).isNull();
        assertThat(PgeyTextSlot.CHEST_NUMBER.pgeyFind(none)).isNull();
    }

    @Test
    void cutEquivalences() {
        PgeyDesignDocument d = kit3d();
        PgeyDesignDocument longSleeve = PgeyViewSync.pgeyNormalize(d.pgeyWithCut(d.pgeyCut().pgeyWithSleeve(PgeySleeve.LARGA)));
        assertThat(PgeyViewSync.pgeyView(longSleeve).pgeyModelKey()).isEqualTo("mangalarga_hombre_frente");
        assertThat(PgeyViewSync.pgeyView(longSleeve).pgeyNotices()).isEmpty();
        PgeyDesignDocument longShorts = PgeyViewSync.pgeyNormalize(d.pgeyWithCut(d.pgeyCut().pgeyWithShorts(PgeyShortsLength.LARGO)));
        assertThat(PgeyViewSync.pgeyView(longShorts).pgeyModelKey()).isEqualTo("shortlargo_hombre_frente");
        PgeyRealisticSpec s = PgeyViewSync.pgeyView(d).pgeySpec();
        PgeyDesignDocument fromPhoto = PgeyViewSync.pgeyApplyRealistic(d, s, s.pgeyWithModel("mangalarga_hombre_frente"));
        assertThat(fromPhoto.pgeyCut().pgeySleeve()).isEqualTo(PgeySleeve.LARGA);
        PgeyDesignDocument fromShortPhoto = PgeyViewSync.pgeyApplyRealistic(d, s, s.pgeyWithModel("shortlargo_hombre_frente"));
        assertThat(fromShortPhoto.pgeyCut().pgeyShorts()).isEqualTo(PgeyShortsLength.LARGO);
        PgeyDesignDocument back = PgeyViewSync.pgeyApplyRealistic(fromPhoto, PgeyViewSync.pgeyView(fromPhoto).pgeySpec(),
                PgeyViewSync.pgeyView(fromPhoto).pgeySpec().pgeyWithModel("futbol_hombre_frente"));
        assertThat(back.pgeyCut().pgeySleeve()).isEqualTo(PgeySleeve.CORTA);

        PgeyDesignDocument woman = PgeyViewSync.pgeyApplyRealistic(d, s, s.pgeyWithModel("futbol_mujer_frente"));
        woman = PgeyViewSync.pgeyNormalize(woman.pgeyWithCut(new PgeyGarmentCut(PgeySleeve.LARGA, PgeyShortsLength.CORTO, PgeySocksLength.TOBILLERA)));
        PgeyRealisticView wv = PgeyViewSync.pgeyView(woman);
        assertThat(wv.pgeyModelKey()).isEqualTo("futbol_mujer_frente");
        assertThat(wv.pgeyNotices()).contains("Manga larga: ver en Vista 3D", "Short corto: ver en Vista 3D",
                "Medias tobilleras: ver en Vista 3D");
        assertThat(woman.pgeyRealisticPhotosMatch()).as("las fotos del pedido salen del 3D").isFalse();
        assertThat(wv.pgeySpec().pgeyColor("camiseta")).isEqualTo("#D7192A");
    }

    @Test
    void photoOnlyCutsShowTheClosest3dCutWithANotice() {
        PgeyDesignDocument d = kit3d();
        PgeyRealisticSpec s = PgeyViewSync.pgeyView(d).pgeySpec();
        PgeyDesignDocument polo = PgeyViewSync.pgeyApplyRealistic(d, s, s.pgeyWithModel("polo_hombre_frente"));
        assertThat(PgeyViewSync.pgeyView(polo).pgeyModelKey()).isEqualTo("polo_hombre_frente");
        assertThat(PgeyViewSync.pgeyNotices3d(polo)).anyMatch(n -> n.startsWith("Polo: la Vista 3D muestra"));
        assertThat(polo.pgeyCut()).isEqualTo(PgeyGarmentCut.PGEY_DEFAULT);
        PgeyDesignDocument buzo = PgeyViewSync.pgeyApplyRealistic(d, s, s.pgeyWithModel("buzo_hombre_frente"));
        assertThat(buzo.pgeyCut().pgeySleeve()).isEqualTo(PgeySleeve.LARGA);
        assertThat(buzo.pgeyCut().pgeyShorts()).isEqualTo(PgeyShortsLength.LARGO);
        assertThat(PgeyViewSync.pgeyNotices3d(buzo)).anyMatch(n -> n.startsWith("Buzo:"));
        assertThat(PgeyViewSync.pgeyNotices3d(d)).isEmpty();
    }

    @Test
    void whatThePhotoCannotShowGivesANoticeNeverOtherColors() {
        PgeyDesignDocument d = kit3d();
        PgeyRealisticSpec s = PgeyViewSync.pgeyView(d).pgeySpec();
        PgeyDesignDocument dyn = PgeyViewSync.pgeyApplyRealistic(d, s, s.pgeyWithPattern("dinamico"));
        assertThat(dyn.pgeyPattern(HepyGarment.JERSEY).pgeyType()).isEqualTo(PgeyPatternType.LISO);
        assertThat(PgeyViewSync.pgeyView(dyn).pgeySpec().pgeyPattern()).isEqualTo("dinamico");
        assertThat(PgeyViewSync.pgeyNotices3d(dyn)).anyMatch(n -> n.contains("dinámico"));
        PgeyDesignDocument striped = PgeyViewSync.pgeyNormalize(dyn.pgeyWithPattern(HepyGarment.JERSEY,
                dyn.pgeyPattern(HepyGarment.JERSEY).pgeyWithType(PgeyPatternType.FRANJAS_VERTICALES)));
        assertThat(PgeyViewSync.pgeyView(striped).pgeySpec().pgeyPattern()).isEqualTo("rayas");

        PgeyDesignDocument checks = PgeyViewSync.pgeyNormalize(d.pgeyWithPattern(HepyGarment.JERSEY,
                d.pgeyPattern(HepyGarment.JERSEY).pgeyWithType(PgeyPatternType.CUADROS)));
        assertThat(PgeyViewSync.pgeyView(checks).pgeySpec().pgeyPattern()).isEqualTo("liso");
        assertThat(PgeyViewSync.pgeyView(checks).pgeyNotices()).contains("Diseño cuadros: ver en Vista 3D");

        PgeyDesignDocument other = PgeyViewSync.pgeyNormalize(d.pgeyWithMaterial(PgeyMaterialSlot.JERSEY, "MALLA")
                .pgeyWithColor(PgeyZone.JERSEY_SLEEVES, "#FFFFFF")
                .pgeyReplaceElement(PgeyTextSlot.BACK_NAME.pgeyFind(d).pgeyAt(0.5, 0.6)));
        assertThat(PgeyViewSync.pgeyView(other).pgeyNotices()).contains("Tela malla deportiva: ver en Vista 3D",
                "Mangas de otro color: ver en Vista 3D", "Posición del nombre: ver en Vista 3D");
        PgeyDesignDocument collar = PgeyViewSync.pgeyNormalize(d.pgeyWithColor(PgeyZone.JERSEY_COLLAR, "#39FF88"));
        assertThat(PgeyViewSync.pgeyView(collar).pgeyNotices()).contains("Color del cuello y los puños: ver en Vista 3D");
        assertThat(collar.pgeyRealisticPhotosMatch()).isFalse();
    }

    @Test
    void oneObservableDocumentForBothViewsInTheViewModel() {
        PgeyDesignerViewModel vm = new PgeyDesignerViewModel(null, null, Clock.systemUTC());
        vm.pgeyEdit("Color", d -> d.pgeyWithColor(PgeyZone.JERSEY_BODY, "#D7192A"), null);
        vm.pgeyEdit("Manga larga", d -> d.pgeyWithCut(d.pgeyCut().pgeyWithSleeve(PgeySleeve.LARGA)), null);
        assertThat(vm.pgeyRealistic().pgeyColor("camiseta")).isEqualTo("#D7192A");
        assertThat(vm.pgeyRealistic().pgeyModelKey()).isEqualTo("mangalarga_hombre_frente");
        assertThat(vm.pgeyDoc().pgeyRealistic()).isEqualTo(vm.pgeyRealistic());
        vm.pgeyEditRealistic("Nombre", s -> s.pgeyWithName("GUERRERO").pgeyWithNumber(10), null);
        assertThat(PgeyTextSlot.BACK_NAME.pgeyFind(vm.pgeyDoc()).pgeyText()).isEqualTo("GUERRERO");
        assertThat(PgeyTextSlot.CHEST_NUMBER.pgeyFind(vm.pgeyDoc()).pgeyText()).isEqualTo("10");
        vm.pgeyEditRealistic("Categoría", s -> s.pgeyWithModel("futbol_mujer_frente"), null);
        assertThat(vm.personaProperty().get()).hasToString("Mujer");
        assertThat(vm.pgeyRealisticView().pgeyNotices()).contains("Manga larga: ver en Vista 3D");
        vm.pgeyUndo();
        vm.pgeyUndo();
        assertThat(PgeyTextSlot.BACK_NAME.pgeyFind(vm.pgeyDoc())).isNull();
        assertThat(vm.pgeyRealistic().pgeyName()).isNull();
    }

    @Test
    void designsSavedBeforeStage11OpenWithBothViewsInSync() {
        PgeyDesignDocument old = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE).pgeyWithRealistic(new PgeyRealisticSpec(
                "futbol_hombre_frente", java.util.Map.of("camiseta", "#D7141E", "short", "#0B0E13",
                "paneles", "#0B0E13", "medias", "#D7141E"), "rayas", "#F2F5F8", "QUISPE", 10));
        String v3 = PgeyDesignJson.pgeyWrite(old).replace("\"schemaVersion\":4", "\"schemaVersion\":3");
        PgeyDesignDocument up = PgeyDesignJson.pgeyRead(v3);
        assertThat(up.pgeyColor(PgeyZone.JERSEY_BODY)).as("gana la vista con que se hicieron las fotos").isEqualTo("#D7141E");
        assertThat(up.pgeyPattern(HepyGarment.JERSEY).pgeyType()).isEqualTo(PgeyPatternType.FRANJAS_VERTICALES);
        assertThat(PgeyTextSlot.BACK_NAME.pgeyFind(up).pgeyText()).isEqualTo("QUISPE");
        assertThat(PgeyViewSync.pgeyView(up).pgeySpec()).isEqualTo(up.pgeyRealistic());
    }
}
