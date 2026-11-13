package com.sportforce3d.component;

import javafx.scene.control.ToggleButton;

public class HepyFilterChip extends ToggleButton {

    public HepyFilterChip(String hepyText) {
        super(hepyText);
        getStyleClass().setAll("sf-filter-chip");
        setMnemonicParsing(false);
    }
}
