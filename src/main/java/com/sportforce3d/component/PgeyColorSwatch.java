package com.sportforce3d.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PgeyColorSwatch extends Button {

    private static final PseudoClass PGEY_SELECTED = PseudoClass.getPseudoClass("selected");

    private final Color pgeyColor;
    private final BooleanProperty pgeySelected = new SimpleBooleanProperty(this, "selected", false) {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(PGEY_SELECTED, get());
        }
    };

    public PgeyColorSwatch(Color pgeyColor, String pgeyName) {
        this.pgeyColor = pgeyColor;
        getStyleClass().setAll("sf-swatch");
        Rectangle pgeyFill = new Rectangle(30, 30, pgeyColor);
        pgeyFill.setArcWidth(10);
        pgeyFill.setArcHeight(10);
        setGraphic(pgeyFill);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        String pgeyHex = pgeyToHex(pgeyColor);
        setAccessibleText(pgeyName + " " + pgeyHex);
        setTooltip(new Tooltip(pgeyName + " · " + pgeyHex));
    }

    public Color getColor() {
        return pgeyColor;
    }

    public BooleanProperty selectedProperty() {
        return pgeySelected;
    }

    public boolean isSelected() {
        return pgeySelected.get();
    }

    public void setSelected(boolean pgeyValue) {
        pgeySelected.set(pgeyValue);
    }

    public static String pgeyToHex(Color pgeyC) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(pgeyC.getRed() * 255),
                (int) Math.round(pgeyC.getGreen() * 255),
                (int) Math.round(pgeyC.getBlue() * 255));
    }
}
