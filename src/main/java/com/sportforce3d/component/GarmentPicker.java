package com.sportforce3d.component;

import com.sportforce3d.model.Garment;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;

import java.util.List;

public class GarmentPicker extends FlowPane {

    private final ObjectProperty<Garment> selected = new SimpleObjectProperty<>(this, "selected");
    private final ToggleGroup group = new ToggleGroup();

    public GarmentPicker() {
        this(Garment.DESIGNER);
    }

    public GarmentPicker(List<Garment> garments) {
        getStyleClass().add("sf-seg");
        setAlignment(Pos.CENTER_LEFT);
        setHgap(4);
        setVgap(4);

        for (Garment garment : garments) {
            ToggleButton item = new ToggleButton(garment.label());
            item.getStyleClass().setAll("sf-seg-item");
            item.setUserData(garment);
            item.setToggleGroup(group);
            item.setMnemonicParsing(false);
            item.setMinWidth(USE_PREF_SIZE);
            getChildren().add(item);
        }

        group.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (toggle == null) {
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
