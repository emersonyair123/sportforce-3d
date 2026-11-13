package com.sportforce3d.viewmodel;

import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeyDesignJson;
import com.sportforce3d.design.PgeyRealisticSpec;
import com.sportforce3d.model.HepyRealisticLook;
import com.sportforce3d.three.human.PgeyPersona;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DesignerViewModelRealisticTest {
    private final PgeyDesignerViewModel vm = new PgeyDesignerViewModel(null, null, Clock.systemUTC());

    @Test
    void editsAreUndoableAndMarkTheDesignDirty() {
        PgeyRealisticSpec start = vm.pgeyRealistic();
        vm.pgeyEditRealistic("Corte · Polo", s -> s.pgeyWithModel("polo_hombre_frente"), null);
        vm.pgeyEditRealistic("Color · Camiseta", s -> s.pgeyWithColor("camiseta", "#d7141e"), null);
        assertThat(vm.pgeyRealistic().pgeyModelKey()).isEqualTo("polo_hombre_frente");
        assertThat(vm.pgeyRealistic().pgeyColor("camiseta")).isEqualTo("#D7141E");
        assertThat(vm.dirtyProperty().get()).isTrue();

        vm.pgeyUndo();
        assertThat(vm.pgeyRealistic().pgeyColor("camiseta")).isEqualTo(start.pgeyColor("camiseta"));
        assertThat(vm.pgeyRealistic().pgeyModelKey()).isEqualTo("polo_hombre_frente");
        vm.pgeyUndo();
        assertThat(vm.pgeyRealistic()).isEqualTo(start);
        vm.pgeyRedo();
        assertThat(vm.pgeyRealistic().pgeyModelKey()).isEqualTo("polo_hombre_frente");
    }

    @Test
    void the3dPersonFollowsTheRealisticCategory() {
        assertThat(vm.personaProperty().get()).isEqualTo(PgeyPersona.HOMBRE);
        vm.pgeyEditRealistic("Categoría · Niña", s -> s.pgeyWithModel("futbol_nina_frente"), null);
        assertThat(vm.personaProperty().get()).isEqualTo(PgeyPersona.NINA);
        vm.pgeyEditRealistic("Categoría · Mujer", s -> s.pgeyWithModel("polo_mujer_frente"), null);
        assertThat(vm.personaProperty().get()).isEqualTo(PgeyPersona.MUJER);
        vm.pgeyEditRealistic("Producto", s -> s.pgeyWithModel("producto_medias"), null);
        assertThat(vm.personaProperty().get()).isEqualTo(PgeyPersona.MUJER);
        vm.pgeyUndo();
        vm.pgeyUndo();
        assertThat(vm.personaProperty().get()).isEqualTo(PgeyPersona.NINA);
    }

    @Test
    void typingIsOneUndoStep() {
        for (String partial : new String[]{"Q", "QU", "QUI", "QUISPE"}) {
            vm.pgeyEditRealistic("Nombre", s -> s.pgeyWithName(partial), "realistic-name");
        }
        vm.pgeySeal();
        assertThat(vm.pgeyRealistic().pgeyName()).isEqualTo("QUISPE");
        vm.pgeyUndo();
        assertThat(vm.pgeyRealistic().pgeyName()).isNull();
    }

    @Test
    void realisticViewIsSavedInTheDocumentJson() {
        vm.pgeyEditRealistic("Diseño", s -> s.pgeyWithModel("voley_mujer_frente").pgeyWithPattern("banda")
                .pgeyWithPatternColor("#d7141e").pgeyWithNumber(3), null);
        PgeyDesignDocument back = PgeyDesignJson.pgeyRead(PgeyDesignJson.pgeyWrite(vm.pgeyDoc()));
        assertThat(back.pgeyRealistic()).isEqualTo(vm.pgeyRealistic());
        assertThat(PgeyDesignJson.pgeyWrite(vm.pgeyDoc())).contains("\"modelKey\":\"voley_mujer_frente\"", "\"pattern\":\"banda\"");
    }

    @Test
    void aProductLookStartsTheDesignWithItsModelColorsAndPattern() {
        PgeyRealisticSpec spec = PgeyDesignerViewModel.pgeyFromLook(new HepyRealisticLook("futbol_mujer_espalda",
                Map.of("camiseta", "#0b2a6b", "paneles", "#ffd400"), "franjas", "#ffd400"));
        assertThat(spec.pgeyModelKey()).as("siempre el modelo de frente").isEqualTo("futbol_mujer_frente");
        assertThat(spec.pgeyColor("camiseta")).isEqualTo("#0B2A6B");
        assertThat(spec.pgeyColor("medias")).as("las zonas que el producto no trae quedan como antes")
                .isEqualTo(PgeyRealisticSpec.pgeyDefaults().pgeyColor("medias"));
        assertThat(spec.pgeyPattern()).isEqualTo("franjas");
        assertThat(spec.pgeyPatternColor()).isEqualTo("#FFD400");
        assertThat(spec.pgeyProblems()).isEmpty();

        PgeyRealisticSpec plain = PgeyDesignerViewModel.pgeyFromLook(new HepyRealisticLook("buzo_hombre_frente",
                Map.of("principal", "#0b2a6b"), null, null));
        assertThat(plain.pgeyPlain()).isTrue();
        assertThat(plain.pgeyPatternColor()).isNotNull();
    }
}
