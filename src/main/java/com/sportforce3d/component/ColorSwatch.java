package com.sportforce3d.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** Muestra de color de 32 px con borde line-strong; seleccionada lleva el anillo de foco. */
public class ColorSwatch extends Button {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final Color color;
    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected", false) {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(SELECTED, get());
        }
    };

    public ColorSwatch(Color color, String name) {
        this.color = color;
        getStyleClass().setAll("sf-swatch");
        // El color es dato del cliente, no un token: se pinta por código dentro del borde del CSS.
        Rectangle fill = new Rectangle(30, 30, color);
        fill.setArcWidth(10);
        fill.setArcHeight(10);
        setGraphic(fill);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        String hex = toHex(color);
        setAccessibleText(name + " " + hex);
        setTooltip(new Tooltip(name + " · " + hex));
    }

    public Color getColor() {
        return color;
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }

    public static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }
}
