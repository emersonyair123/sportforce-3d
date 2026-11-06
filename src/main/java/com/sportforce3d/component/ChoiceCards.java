package com.sportforce3d.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Grupo de tarjetas de opción única (direcciones, métodos de pago): indicador de radio, título, detalle y
 * un nodo opcional a la derecha. Las opciones no disponibles quedan deshabilitadas con texto "Próximamente".
 */
public class ChoiceCards<T> extends VBox {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final ObjectProperty<T> selected = new SimpleObjectProperty<>(this, "selected");
    private final List<Card> cards = new ArrayList<>();

    private final class Card extends HBox {
        final T value;

        Card(T value, String title, String detail, Node trailing, boolean available) {
            this.value = value;
            getStyleClass().add("sf-choice");
            setSpacing(12);
            setAlignment(Pos.CENTER_LEFT);
            setFocusTraversable(available);
            setDisable(!available);
            setAccessibleRole(AccessibleRole.RADIO_BUTTON);
            setAccessibleText(title + (detail == null ? "" : ", " + detail) + (available ? "" : ", próximamente"));

            Region dot = new Region();
            dot.getStyleClass().add("sf-choice-dot");
            StackPane radio = new StackPane(dot);
            radio.getStyleClass().add("sf-choice-radio");

            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("sf-choice-title");
            VBox text = new VBox(2, titleLabel);
            if (detail != null && !detail.isBlank()) {
                Label detailLabel = new Label(detail);
                detailLabel.getStyleClass().addAll("body-s", "ink-muted");
                detailLabel.setWrapText(true);
                text.getChildren().add(detailLabel);
            }
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);
            getChildren().addAll(radio, text);
            if (!available) {
                getChildren().add(new SfBadge("Próximamente"));
            } else if (trailing != null) {
                getChildren().add(trailing);
            }

            setOnMouseClicked(e -> {
                if (e.getTarget() instanceof Node n && trailing != null && isInside(n, trailing)) {
                    return;
                }
                requestFocus();
                selected.set(value);
            });
            setOnKeyPressed(e -> {
                int index = cards.indexOf(this);
                if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                    selected.set(value);
                } else if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.RIGHT) {
                    move(index, +1);
                } else if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.LEFT) {
                    move(index, -1);
                } else {
                    return;
                }
                e.consume();
            });
        }

        void refresh() {
            pseudoClassStateChanged(SELECTED, Objects.equals(selected.get(), value));
            notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTED);
        }

        @Override
        public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
            if (attribute == AccessibleAttribute.SELECTED) {
                return Objects.equals(selected.get(), value);
            }
            return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    public ChoiceCards() {
        getStyleClass().add("sf-choices");
        setSpacing(8);
        selected.addListener((obs, old, value) -> cards.forEach(Card::refresh));
    }

    public void add(T value, String title, String detail, Node trailing, boolean available) {
        Card card = new Card(value, title, detail, trailing, available);
        cards.add(card);
        getChildren().add(card);
        card.refresh();
    }

    public void clear() {
        cards.clear();
        getChildren().clear();
    }

    private void move(int from, int step) {
        for (int i = from + step; i >= 0 && i < cards.size(); i += step) {
            Card c = cards.get(i);
            if (!c.isDisabled()) {
                c.requestFocus();
                selected.set(c.value);
                return;
            }
        }
    }

    private static boolean isInside(Node node, Node container) {
        for (Node n = node; n != null; n = n.getParent()) {
            if (n == container) {
                return true;
            }
        }
        return false;
    }

    public ObjectProperty<T> selectedProperty() {
        return selected;
    }

    public T getSelected() {
        return selected.get();
    }

    public void setSelected(T value) {
        selected.set(value);
    }
}
