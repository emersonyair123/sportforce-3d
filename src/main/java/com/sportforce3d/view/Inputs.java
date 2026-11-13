package com.sportforce3d.view;

import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

final class Inputs {

    private Inputs() {
    }

    static void limit(TextInputControl input, int max) {
        input.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= max ? change : null));
    }
}
