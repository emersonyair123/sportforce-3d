package com.sportforce3d.viewmodel;

import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.DesignJson;
import com.sportforce3d.design.RealisticSpec;
import com.sportforce3d.model.RealisticLook;
import com.sportforce3d.three.human.Persona;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DesignerViewModelRealisticTest {
    private final DesignerViewModel vm = new DesignerViewModel(null, null, Clock.systemUTC());

    @Test
    void editsAreUndoableAndMarkTheDesignDirty() {
        RealisticSpec start = vm.realistic();
        vm.editRealistic("Corte · Polo", s -> s.withModel("polo_hombre_frente"), null);
        vm.editRealistic("Color · Camiseta", s -> s.withColor("camiseta", "#d7141e"), null);
        assertThat(vm.realistic().modelKey()).isEqualTo("polo_hombre_frente");
        assertThat(vm.realistic().color("camiseta")).isEqualTo("#D7141E");
        assertThat(vm.dirtyProperty().get()).isTrue();

        vm.undo();
        assertThat(vm.realistic().color("camiseta")).isEqualTo(start.color("camiseta"));
        assertThat(vm.realistic().modelKey()).isEqualTo("polo_hombre_frente");
        vm.undo();
        assertThat(vm.realistic()).isEqualTo(start);
        vm.redo();
        assertThat(vm.realistic().modelKey()).isEqualTo("polo_hombre_frente");
    }

    @Test
    void the3dPersonFollowsTheRealisticCategory() {
        assertThat(vm.personaProperty().get()).isEqualTo(Persona.HOMBRE);
        vm.editRealistic("Categoría · Niña", s -> s.withModel("futbol_nina_frente"), null);
        assertThat(vm.personaProperty().get()).isEqualTo(Persona.NINA);
        vm.editRealistic("Categoría · Mujer", s -> s.withModel("polo_mujer_frente"), null);
        assertThat(vm.personaProperty().get()).isEqualTo(Persona.MUJER);
        vm.editRealistic("Producto", s -> s.withModel("producto_medias"), null);
        assertThat(vm.personaProperty().get()).isEqualTo(Persona.MUJER);
        vm.undo();
        vm.undo();
        assertThat(vm.personaProperty().get()).isEqualTo(Persona.NINA);
    }

    @Test
    void typingIsOneUndoStep() {
        for (String partial : new String[]{"Q", "QU", "QUI", "QUISPE"}) {
            vm.editRealistic("Nombre", s -> s.withName(partial), "realistic-name");
        }
        vm.seal();
        assertThat(vm.realistic().name()).isEqualTo("QUISPE");
        vm.undo();
        assertThat(vm.realistic().name()).isNull();
    }

    @Test
    void realisticViewIsSavedInTheDocumentJson() {
        vm.editRealistic("Diseño", s -> s.withModel("voley_mujer_frente").withPattern("banda")
                .withPatternColor("#d7141e").withNumber(3), null);
        DesignDocument back = DesignJson.read(DesignJson.write(vm.doc()));
        assertThat(back.realistic()).isEqualTo(vm.realistic());
        assertThat(DesignJson.write(vm.doc())).contains("\"modelKey\":\"voley_mujer_frente\"", "\"pattern\":\"banda\"");
    }

    @Test
    void aProductLookStartsTheDesignWithItsModelColorsAndPattern() {
        RealisticSpec spec = DesignerViewModel.fromLook(new RealisticLook("futbol_mujer_espalda",
                Map.of("camiseta", "#0b2a6b", "paneles", "#ffd400"), "franjas", "#ffd400"));
        assertThat(spec.modelKey()).as("siempre el modelo de frente").isEqualTo("futbol_mujer_frente");
        assertThat(spec.color("camiseta")).isEqualTo("#0B2A6B");
        assertThat(spec.color("medias")).as("las zonas que el producto no trae quedan como antes")
                .isEqualTo(RealisticSpec.defaults().color("medias"));
        assertThat(spec.pattern()).isEqualTo("franjas");
        assertThat(spec.patternColor()).isEqualTo("#FFD400");
        assertThat(spec.problems()).isEmpty();

        RealisticSpec plain = DesignerViewModel.fromLook(new RealisticLook("buzo_hombre_frente",
                Map.of("principal", "#0b2a6b"), null, null));
        assertThat(plain.plain()).isTrue();
        assertThat(plain.patternColor()).isNotNull();
    }
}
