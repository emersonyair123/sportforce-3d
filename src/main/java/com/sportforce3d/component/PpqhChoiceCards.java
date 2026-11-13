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

public class PpqhChoiceCards<T> extends VBox {

    private static final PseudoClass PPQH_SELECTED = PseudoClass.getPseudoClass("selected");

    private final ObjectProperty<T> ppqhSelected = new SimpleObjectProperty<>(this, "selected");
    private final List<PpqhCard> ppqhCards = new ArrayList<>();

    private final class PpqhCard extends HBox {
        final T ppqhValue;

        PpqhCard(T ppqhValue, String ppqhTitle, String ppqhDetail, Node ppqhTrailing, boolean ppqhAvailable) {
            this.ppqhValue = ppqhValue;
            getStyleClass().add("sf-choice");
            setSpacing(12);
            setAlignment(Pos.CENTER_LEFT);
            setFocusTraversable(ppqhAvailable);
            setDisable(!ppqhAvailable);
            setAccessibleRole(AccessibleRole.RADIO_BUTTON);
            setAccessibleText(ppqhTitle + (ppqhDetail == null ? "" : ", " + ppqhDetail) + (ppqhAvailable ? "" : ", próximamente"));

            Region ppqhDot = new Region();
            ppqhDot.getStyleClass().add("sf-choice-dot");
            StackPane ppqhRadio = new StackPane(ppqhDot);
            ppqhRadio.getStyleClass().add("sf-choice-radio");

            Label ppqhTitleLabel = new Label(ppqhTitle);
            ppqhTitleLabel.getStyleClass().add("sf-choice-title");
            VBox ppqhText = new VBox(2, ppqhTitleLabel);
            if (ppqhDetail != null && !ppqhDetail.isBlank()) {
                Label ppqhDetailLabel = new Label(ppqhDetail);
                ppqhDetailLabel.getStyleClass().addAll("body-s", "ink-muted");
                ppqhDetailLabel.setWrapText(true);
                ppqhText.getChildren().add(ppqhDetailLabel);
            }
            ppqhText.setMinWidth(0);
            HBox.setHgrow(ppqhText, Priority.ALWAYS);
            getChildren().addAll(ppqhRadio, ppqhText);
            if (!ppqhAvailable) {
                getChildren().add(new PpqhSfBadge("Próximamente"));
            } else if (ppqhTrailing != null) {
                getChildren().add(ppqhTrailing);
            }

            setOnMouseClicked(ppqhE -> {
                if (ppqhE.getTarget() instanceof Node ppqhN && ppqhTrailing != null && ppqhIsInside(ppqhN, ppqhTrailing)) {
                    return;
                }
                requestFocus();
                ppqhSelected.set(ppqhValue);
            });
            setOnKeyPressed(ppqhE -> {
                int ppqhIndex = ppqhCards.indexOf(this);
                if (ppqhE.getCode() == KeyCode.SPACE || ppqhE.getCode() == KeyCode.ENTER) {
                    ppqhSelected.set(ppqhValue);
                } else if (ppqhE.getCode() == KeyCode.DOWN || ppqhE.getCode() == KeyCode.RIGHT) {
                    ppqhMove(ppqhIndex, +1);
                } else if (ppqhE.getCode() == KeyCode.UP || ppqhE.getCode() == KeyCode.LEFT) {
                    ppqhMove(ppqhIndex, -1);
                } else {
                    return;
                }
                ppqhE.consume();
            });
        }

        void ppqhRefresh() {
            pseudoClassStateChanged(PPQH_SELECTED, Objects.equals(ppqhSelected.get(), ppqhValue));
            notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTED);
        }

        @Override
        public Object queryAccessibleAttribute(AccessibleAttribute ppqhAttribute, Object... ppqhParameters) {
            if (ppqhAttribute == AccessibleAttribute.SELECTED) {
                return Objects.equals(ppqhSelected.get(), ppqhValue);
            }
            return super.queryAccessibleAttribute(ppqhAttribute, ppqhParameters);
        }
    }

    public PpqhChoiceCards() {
        getStyleClass().add("sf-choices");
        setSpacing(8);
        ppqhSelected.addListener((ppqhObs, ppqhOld, ppqhValue) -> ppqhCards.forEach(PpqhCard::ppqhRefresh));
    }

    public void ppqhAdd(T ppqhValue, String ppqhTitle, String ppqhDetail, Node ppqhTrailing, boolean ppqhAvailable) {
        PpqhCard ppqhCard = new PpqhCard(ppqhValue, ppqhTitle, ppqhDetail, ppqhTrailing, ppqhAvailable);
        ppqhCards.add(ppqhCard);
        getChildren().add(ppqhCard);
        ppqhCard.ppqhRefresh();
    }

    public void ppqhClear() {
        ppqhCards.clear();
        getChildren().clear();
    }

    private void ppqhMove(int ppqhFrom, int ppqhStep) {
        for (int ppqhI = ppqhFrom + ppqhStep; ppqhI >= 0 && ppqhI < ppqhCards.size(); ppqhI += ppqhStep) {
            PpqhCard ppqhC = ppqhCards.get(ppqhI);
            if (!ppqhC.isDisabled()) {
                ppqhC.requestFocus();
                ppqhSelected.set(ppqhC.ppqhValue);
                return;
            }
        }
    }

    private static boolean ppqhIsInside(Node ppqhNode, Node ppqhContainer) {
        for (Node ppqhN = ppqhNode; ppqhN != null; ppqhN = ppqhN.getParent()) {
            if (ppqhN == ppqhContainer) {
                return true;
            }
        }
        return false;
    }

    public ObjectProperty<T> selectedProperty() {
        return ppqhSelected;
    }

    public T getSelected() {
        return ppqhSelected.get();
    }

    public void setSelected(T ppqhValue) {
        ppqhSelected.set(ppqhValue);
    }
}
