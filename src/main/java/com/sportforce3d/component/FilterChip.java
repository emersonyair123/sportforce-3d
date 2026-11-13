package com.sportforce3d.component;

import javafx.scene.control.ToggleButton;

public class FilterChip extends ToggleButton {

    public FilterChip(String text) {
        super(text);
        getStyleClass().setAll("sf-filter-chip");
        setMnemonicParsing(false);
    }
}
