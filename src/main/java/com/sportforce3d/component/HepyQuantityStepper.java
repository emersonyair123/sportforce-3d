package com.sportforce3d.component;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.feather.Feather;

public class HepyQuantityStepper extends HBox {

    private final IntegerProperty hepyValue = new SimpleIntegerProperty(this, "value", 1);
    private final IntegerProperty hepyMin = new SimpleIntegerProperty(this, "min", 1);
    private final IntegerProperty hepyMax = new SimpleIntegerProperty(this, "max", 99);

    public HepyQuantityStepper() {
        getStyleClass().add("sf-stepper");
        setMaxWidth(USE_PREF_SIZE);

        PpqhSfButton hepyMinus = PpqhSfButton.ppqhIcon(Feather.MINUS, "Disminuir cantidad");
        PpqhSfButton hepyPlus = PpqhSfButton.ppqhIcon(Feather.PLUS, "Aumentar cantidad");
        Label hepyLabel = new Label();
        hepyLabel.getStyleClass().add("sf-stepper-value");
        hepyLabel.textProperty().bind(hepyValue.asString());

        hepyMinus.setOnAction(hepyE -> setValue(getValue() - 1));
        hepyPlus.setOnAction(hepyE -> setValue(getValue() + 1));
        hepyMinus.disableProperty().bind(hepyValue.lessThanOrEqualTo(hepyMin));
        hepyPlus.disableProperty().bind(hepyValue.greaterThanOrEqualTo(hepyMax));

        setOnKeyPressed(hepyE -> {
            if (hepyE.getCode() == KeyCode.UP || hepyE.getCode() == KeyCode.RIGHT) {
                setValue(getValue() + 1);
                hepyE.consume();
            } else if (hepyE.getCode() == KeyCode.DOWN || hepyE.getCode() == KeyCode.LEFT) {
                setValue(getValue() - 1);
                hepyE.consume();
            }
        });

        getChildren().addAll(hepyMinus, hepyLabel, hepyPlus);
        setAccessibleText("Cantidad");
    }

    public IntegerProperty valueProperty() {
        return hepyValue;
    }

    public int getValue() {
        return hepyValue.get();
    }

    public void setValue(int hepyV) {
        hepyValue.set(Math.max(hepyMin.get(), Math.min(hepyMax.get(), hepyV)));
    }

    public IntegerProperty minProperty() {
        return hepyMin;
    }

    public IntegerProperty maxProperty() {
        return hepyMax;
    }

    public void setMax(int hepyV) {
        hepyMax.set(hepyV);
        setValue(getValue());
    }

    public void setMin(int hepyV) {
        hepyMin.set(hepyV);
        setValue(getValue());
    }
}
