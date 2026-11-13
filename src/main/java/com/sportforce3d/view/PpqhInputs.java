package com.sportforce3d.view;

import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

final class PpqhInputs {

    private PpqhInputs() {
    }

    static void ppqhLimit(TextInputControl ppqhInput, int ppqhMax) {
        ppqhInput.setTextFormatter(new TextFormatter<String>(ppqhChange ->
                ppqhChange.getControlNewText().length() <= ppqhMax ? ppqhChange : null));
    }
}
