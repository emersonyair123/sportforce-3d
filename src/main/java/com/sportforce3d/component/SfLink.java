package com.sportforce3d.component;

import javafx.beans.NamedArg;
import javafx.scene.control.Button;

/** Enlace de texto en brand-text que actúa como botón ("Regístrate", "Inicia sesión"). */
public class SfLink extends Button {

    public SfLink(@NamedArg("text") String text) {
        super(text);
        getStyleClass().setAll("sf-link");
        setMnemonicParsing(false);
    }

    public SfLink() {
        this("");
    }
}
