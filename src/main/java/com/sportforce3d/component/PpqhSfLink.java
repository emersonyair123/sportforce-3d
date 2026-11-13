package com.sportforce3d.component;

import javafx.beans.NamedArg;
import javafx.scene.control.Button;

public class PpqhSfLink extends Button {

    public PpqhSfLink(@NamedArg("text") String ppqhText) {
        super(ppqhText);
        getStyleClass().setAll("sf-link");
        setMnemonicParsing(false);
    }

    public PpqhSfLink() {
        this("");
    }
}
