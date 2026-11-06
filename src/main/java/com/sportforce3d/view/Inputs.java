package com.sportforce3d.view;

import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

/** Límites de escritura en la vista: aviso inmediato; el service vuelve a validar. */
final class Inputs {

    private Inputs() {
    }

    /** No deja escribir ni pegar más de {@code max} caracteres. */
    static void limit(TextInputControl input, int max) {
        input.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= max ? change : null));
    }
}
