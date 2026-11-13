package com.sportforce3d.component;

import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.util.HepyMoney;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class PgeyMaterialPicker extends VBox {

    private static final PseudoClass PGEY_SELECTED = PseudoClass.getPseudoClass("selected");

    private final ObjectProperty<HepyMaterial> pgeySelected = new SimpleObjectProperty<>(this, "selected");
    private final ObjectProperty<Color> pgeySampleColor = new SimpleObjectProperty<>(this, "sampleColor", Color.web("#1F5FFF"));
    private final List<PgeyCard> pgeyCards = new ArrayList<>();

    private final class PgeyCard extends HBox {
        final HepyMaterial pgeyMaterial;
        final PgeyMaterialSample pgeySample;

        PgeyCard(HepyMaterial pgeyMaterial) {
            this.pgeyMaterial = pgeyMaterial;
            getStyleClass().add("sf-mat");
            setSpacing(12);
            setAlignment(Pos.CENTER_LEFT);
            setFocusTraversable(true);
            setAccessibleRole(AccessibleRole.RADIO_BUTTON);
            setAccessibleText(pgeyMaterial.hepyName() + ", " + pgeyMaterial.hepyUsage() + ", " + HepyMoney.hepySurcharge(pgeyMaterial.hepySurchargeCents()));

            pgeySample = new PgeyMaterialSample(pgeyMaterial.hepyPreviewPattern(), pgeySampleColor.get(), 40);
            Label pgeyName = new Label(pgeyMaterial.hepyName());
            pgeyName.getStyleClass().add("sf-mat-name");
            Label pgeyNote = new Label(pgeyMaterial.hepyUsage());
            pgeyNote.getStyleClass().add("sf-mat-note");
            VBox pgeyText = new VBox(pgeyName, pgeyNote);
            pgeyText.setMinWidth(0);
            HBox.setHgrow(pgeyText, Priority.ALWAYS);
            Label pgeyPrice = new Label(HepyMoney.hepySurcharge(pgeyMaterial.hepySurchargeCents()));
            pgeyPrice.getStyleClass().add("sf-mat-price");
            pgeyPrice.setMinWidth(USE_PREF_SIZE);
            getChildren().addAll(pgeySample, pgeyText, pgeyPrice);

            setOnMouseClicked(pgeyE -> {
                requestFocus();
                pgeySelected.set(pgeyMaterial);
            });
            setOnKeyPressed(pgeyE -> {
                int pgeyIndex = pgeyCards.indexOf(this);
                if (pgeyE.getCode() == KeyCode.SPACE || pgeyE.getCode() == KeyCode.ENTER) {
                    pgeySelected.set(pgeyMaterial);
                } else if ((pgeyE.getCode() == KeyCode.DOWN || pgeyE.getCode() == KeyCode.RIGHT) && pgeyIndex < pgeyCards.size() - 1) {
                    PgeyCard pgeyNext = pgeyCards.get(pgeyIndex + 1);
                    pgeyNext.requestFocus();
                    pgeySelected.set(pgeyNext.pgeyMaterial);
                } else if ((pgeyE.getCode() == KeyCode.UP || pgeyE.getCode() == KeyCode.LEFT) && pgeyIndex > 0) {
                    PgeyCard pgeyPrev = pgeyCards.get(pgeyIndex - 1);
                    pgeyPrev.requestFocus();
                    pgeySelected.set(pgeyPrev.pgeyMaterial);
                } else {
                    return;
                }
                pgeyE.consume();
            });
        }

        void setSelected(boolean pgeyValue) {
            pseudoClassStateChanged(PGEY_SELECTED, pgeyValue);
            notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTED);
        }

        @Override
        public Object queryAccessibleAttribute(AccessibleAttribute pgeyAttribute, Object... pgeyParameters) {
            if (pgeyAttribute == AccessibleAttribute.SELECTED) {
                return pgeySelected.get() == pgeyMaterial;
            }
            return super.queryAccessibleAttribute(pgeyAttribute, pgeyParameters);
        }
    }

    public PgeyMaterialPicker() {
        getStyleClass().add("sf-mats");
        pgeySelected.addListener((pgeyObs, pgeyOld, pgeyMaterial) ->
                pgeyCards.forEach(pgeyC -> pgeyC.setSelected(pgeyMaterial != null && pgeyC.pgeyMaterial.hepyId() == pgeyMaterial.hepyId())));
        pgeySampleColor.addListener((pgeyObs, pgeyOld, pgeyColor) -> pgeyCards.forEach(pgeyC -> pgeyC.pgeySample.pgeyPaint(pgeyColor)));
    }

    public void setMaterials(List<HepyMaterial> pgeyMaterials) {
        pgeyCards.clear();
        for (HepyMaterial pgeyMaterial : pgeyMaterials) {
            pgeyCards.add(new PgeyCard(pgeyMaterial));
        }
        getChildren().setAll(pgeyCards);
        pgeySelected.set(null);
        if (!pgeyMaterials.isEmpty()) {
            pgeySelected.set(pgeyMaterials.get(0));
        }
    }

    public ObjectProperty<HepyMaterial> selectedProperty() {
        return pgeySelected;
    }

    public void setSelected(HepyMaterial pgeyMaterial) {
        pgeySelected.set(pgeyMaterial == null ? null
                : pgeyCards.stream().map(pgeyC -> pgeyC.pgeyMaterial).filter(pgeyM -> pgeyM.hepyId() == pgeyMaterial.hepyId()).findFirst().orElse(null));
    }

    public HepyMaterial getSelected() {
        return pgeySelected.get();
    }

    public ObjectProperty<Color> sampleColorProperty() {
        return pgeySampleColor;
    }
}
