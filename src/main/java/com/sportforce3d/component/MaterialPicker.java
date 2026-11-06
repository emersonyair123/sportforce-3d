package com.sportforce3d.component;

import com.sportforce3d.model.Material;
import com.sportforce3d.util.Money;
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

/**
 * Lista de tarjetas de material: muestra de textura, nombre (title-s), uso (body-s, ink-muted)
 * y recargo ("+ S/ 15.00" o "Incluido"). La elegida lleva borde brand y fondo bg-300.
 * Se navega con Tab y se elige con Espacio, Enter o las flechas.
 */
public class MaterialPicker extends VBox {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final ObjectProperty<Material> selected = new SimpleObjectProperty<>(this, "selected");
    private final ObjectProperty<Color> sampleColor = new SimpleObjectProperty<>(this, "sampleColor", Color.web("#1F5FFF"));
    private final List<Card> cards = new ArrayList<>();

    private final class Card extends HBox {
        final Material material;
        final MaterialSample sample;

        Card(Material material) {
            this.material = material;
            getStyleClass().add("sf-mat");
            setSpacing(12);
            setAlignment(Pos.CENTER_LEFT);
            setFocusTraversable(true);
            setAccessibleRole(AccessibleRole.RADIO_BUTTON);
            setAccessibleText(material.name() + ", " + material.usage() + ", " + Money.surcharge(material.surchargeCents()));

            sample = new MaterialSample(material.previewPattern(), sampleColor.get(), 40);
            Label name = new Label(material.name());
            name.getStyleClass().add("sf-mat-name");
            Label note = new Label(material.usage());
            note.getStyleClass().add("sf-mat-note");
            VBox text = new VBox(name, note);
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label price = new Label(Money.surcharge(material.surchargeCents()));
            price.getStyleClass().add("sf-mat-price");
            price.setMinWidth(USE_PREF_SIZE);
            getChildren().addAll(sample, text, price);

            setOnMouseClicked(e -> {
                requestFocus();
                selected.set(material);
            });
            setOnKeyPressed(e -> {
                int index = cards.indexOf(this);
                if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                    selected.set(material);
                } else if ((e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.RIGHT) && index < cards.size() - 1) {
                    Card next = cards.get(index + 1);
                    next.requestFocus();
                    selected.set(next.material);
                } else if ((e.getCode() == KeyCode.UP || e.getCode() == KeyCode.LEFT) && index > 0) {
                    Card prev = cards.get(index - 1);
                    prev.requestFocus();
                    selected.set(prev.material);
                } else {
                    return;
                }
                e.consume();
            });
        }

        void setSelected(boolean value) {
            pseudoClassStateChanged(SELECTED, value);
            notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTED);
        }

        @Override
        public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
            if (attribute == AccessibleAttribute.SELECTED) {
                return selected.get() == material;
            }
            return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    public MaterialPicker() {
        getStyleClass().add("sf-mats");
        selected.addListener((obs, old, material) ->
                cards.forEach(c -> c.setSelected(material != null && c.material.id() == material.id())));
        sampleColor.addListener((obs, old, color) -> cards.forEach(c -> c.sample.paint(color)));
    }

    /** Reemplaza los materiales mostrados (solo los válidos para la prenda activa) y elige el primero. */
    public void setMaterials(List<Material> materials) {
        cards.clear();
        for (Material material : materials) {
            cards.add(new Card(material));
        }
        getChildren().setAll(cards);
        selected.set(null);
        if (!materials.isEmpty()) {
            selected.set(materials.get(0));
        }
    }

    public ObjectProperty<Material> selectedProperty() {
        return selected;
    }

    public Material getSelected() {
        return selected.get();
    }

    /** Color de la prenda con el que se dibujan las muestras. */
    public ObjectProperty<Color> sampleColorProperty() {
        return sampleColor;
    }
}
