package com.sportforce3d.component;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.feather.Feather;

/** Selector de cantidad en píldora (− 1 +). Flechas arriba/abajo también cambian el valor. */
public class QuantityStepper extends HBox {

    private final IntegerProperty value = new SimpleIntegerProperty(this, "value", 1);
    private final IntegerProperty min = new SimpleIntegerProperty(this, "min", 1);
    private final IntegerProperty max = new SimpleIntegerProperty(this, "max", 99);

    public QuantityStepper() {
        getStyleClass().add("sf-stepper");
        setMaxWidth(USE_PREF_SIZE);

        SfButton minus = SfButton.icon(Feather.MINUS, "Disminuir cantidad");
        SfButton plus = SfButton.icon(Feather.PLUS, "Aumentar cantidad");
        Label label = new Label();
        label.getStyleClass().add("sf-stepper-value");
        label.textProperty().bind(value.asString());

        minus.setOnAction(e -> setValue(getValue() - 1));
        plus.setOnAction(e -> setValue(getValue() + 1));
        minus.disableProperty().bind(value.lessThanOrEqualTo(min));
        plus.disableProperty().bind(value.greaterThanOrEqualTo(max));

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.RIGHT) {
                setValue(getValue() + 1);
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.LEFT) {
                setValue(getValue() - 1);
                e.consume();
            }
        });

        getChildren().addAll(minus, label, plus);
        setAccessibleText("Cantidad");
    }

    public IntegerProperty valueProperty() {
        return value;
    }

    public int getValue() {
        return value.get();
    }

    /** Ajusta el valor al rango [min, max]. */
    public void setValue(int v) {
        value.set(Math.max(min.get(), Math.min(max.get(), v)));
    }

    public IntegerProperty minProperty() {
        return min;
    }

    public IntegerProperty maxProperty() {
        return max;
    }

    public void setMax(int v) {
        max.set(v);
        setValue(getValue());
    }

    public void setMin(int v) {
        min.set(v);
        setValue(getValue());
    }
}
