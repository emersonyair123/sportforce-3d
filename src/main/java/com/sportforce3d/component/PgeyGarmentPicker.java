package com.sportforce3d.component;

import com.sportforce3d.model.HepyGarment;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;

import java.util.List;

public class PgeyGarmentPicker extends FlowPane {

    private final ObjectProperty<HepyGarment> pgeySelected = new SimpleObjectProperty<>(this, "selected");
    private final ToggleGroup pgeyGroup = new ToggleGroup();

    public PgeyGarmentPicker() {
        this(HepyGarment.HEPY_DESIGNER);
    }

    public PgeyGarmentPicker(List<HepyGarment> pgeyGarments) {
        getStyleClass().add("sf-seg");
        setAlignment(Pos.CENTER_LEFT);
        setHgap(4);
        setVgap(4);

        for (HepyGarment pgeyGarment : pgeyGarments) {
            ToggleButton pgeyItem = new ToggleButton(pgeyGarment.hepyLabel());
            pgeyItem.getStyleClass().setAll("sf-seg-item");
            pgeyItem.setUserData(pgeyGarment);
            pgeyItem.setToggleGroup(pgeyGroup);
            pgeyItem.setMnemonicParsing(false);
            pgeyItem.setMinWidth(USE_PREF_SIZE);
            getChildren().add(pgeyItem);
        }

        pgeyGroup.selectedToggleProperty().addListener((pgeyObs, pgeyOld, pgeyToggle) -> {
            if (pgeyToggle == null) {
                pgeyGroup.selectToggle(pgeyOld);
            } else {
                pgeySelected.set((HepyGarment) pgeyToggle.getUserData());
            }
        });
        pgeySelected.addListener((pgeyObs, pgeyOld, pgeyGarment) -> pgeyGroup.getToggles().stream()
                .filter(pgeyT -> pgeyT.getUserData() == pgeyGarment)
                .findFirst()
                .ifPresent(pgeyGroup::selectToggle));

        if (!pgeyGarments.isEmpty()) {
            pgeySelected.set(pgeyGarments.get(0));
        }
    }

    public ObjectProperty<HepyGarment> selectedProperty() {
        return pgeySelected;
    }

    public HepyGarment getSelected() {
        return pgeySelected.get();
    }

    public void setSelected(HepyGarment pgeyGarment) {
        pgeySelected.set(pgeyGarment);
    }
}
