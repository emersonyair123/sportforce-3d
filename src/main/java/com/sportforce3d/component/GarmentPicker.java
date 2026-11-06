package com.sportforce3d.component;

import com.sportforce3d.model.Garment;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.util.List;

/** Control segmentado: Camiseta · Short · Medias · Zapatillas · Accesorios. Siempre hay una prenda elegida. */
public class GarmentPicker extends HBox {

    private final ObjectProperty<Garment> selected = new SimpleObjectProperty<>(this, "selected");
    private final ToggleGroup group = new ToggleGroup();

    public GarmentPicker() {
        this(Garment.DESIGNER);
    }

    public GarmentPicker(List<Garment> garments) {
        getStyleClass().add("sf-seg");
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(USE_PREF_SIZE);

        for (Garment garment : garments) {
            ToggleButton item = new ToggleButton(garment.label());
            item.getStyleClass().setAll("sf-seg-item");
            item.setUserData(garment);
            item.setToggleGroup(group);
            item.setMnemonicParsing(false);
            getChildren().add(item);
        }

        group.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (toggle == null) {
                // No se permite quedar sin prenda: se revierte la deselección.
                group.selectToggle(old);
            } else {
                selected.set((Garment) toggle.getUserData());
            }
        });
        selected.addListener((obs, old, garment) -> group.getToggles().stream()
                .filter(t -> t.getUserData() == garment)
                .findFirst()
                .ifPresent(group::selectToggle));

        if (!garments.isEmpty()) {
            selected.set(garments.get(0));
        }
    }

    public ObjectProperty<Garment> selectedProperty() {
        return selected;
    }

    public Garment getSelected() {
        return selected.get();
    }

    public void setSelected(Garment garment) {
        selected.set(garment);
    }
}
