package com.sportforce3d.component;

import javafx.scene.control.ToggleButton;

/** Chip de filtro en píldora; seleccionado lleva texto brand-text y borde brand. */
public class FilterChip extends ToggleButton {

    public FilterChip(String text) {
        super(text);
        getStyleClass().setAll("sf-filter-chip");
        setMnemonicParsing(false);
    }
}
