package com.sportforce3d.component;

import com.sportforce3d.model.PpqhOrderStatus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

public class PpqhStatusTimeline extends HBox {

    private static final PseudoClass PPQH_DONE = PseudoClass.getPseudoClass("done");
    private static final PseudoClass PPQH_CURRENT = PseudoClass.getPseudoClass("current");
    private static final PseudoClass PPQH_CANCELLED = PseudoClass.getPseudoClass("cancelled");

    private final ObjectProperty<PpqhOrderStatus> ppqhStatus = new SimpleObjectProperty<>(this, "status");
    private PpqhOrderStatus ppqhCancelledAt = PpqhOrderStatus.PENDIENTE_PAGO;

    public PpqhStatusTimeline() {
        getStyleClass().add("sf-timeline");
        setAlignment(Pos.TOP_LEFT);
        ppqhStatus.addListener((ppqhObs, ppqhOld, ppqhS) -> ppqhRebuild());
        setStatus(PpqhOrderStatus.PENDIENTE_PAGO);
    }

    public void setCancelled(PpqhOrderStatus ppqhLastReached) {
        this.ppqhCancelledAt = ppqhLastReached == null ? PpqhOrderStatus.PENDIENTE_PAGO : ppqhLastReached;
        ppqhStatus.set(null);
        ppqhStatus.set(PpqhOrderStatus.CANCELADO);
    }

    private void ppqhRebuild() {
        getChildren().clear();
        PpqhOrderStatus ppqhCurrent = ppqhStatus.get();
        if (ppqhCurrent == null) {
            return;
        }
        boolean ppqhCancelled = ppqhCurrent == PpqhOrderStatus.CANCELADO;
        int ppqhCurrentIndex = PpqhOrderStatus.PPQH_FLOW.indexOf(ppqhCancelled ? ppqhCancelledAt : ppqhCurrent);

        for (int ppqhI = 0; ppqhI < PpqhOrderStatus.PPQH_FLOW.size(); ppqhI++) {
            PpqhOrderStatus ppqhStep = PpqhOrderStatus.PPQH_FLOW.get(ppqhI);
            boolean ppqhDone = ppqhI < ppqhCurrentIndex || (!ppqhCancelled && ppqhCurrent == PpqhOrderStatus.ENTREGADO && ppqhI == ppqhCurrentIndex);
            boolean ppqhIsCurrent = ppqhI == ppqhCurrentIndex && !ppqhDone;

            StackPane ppqhDot = new StackPane();
            ppqhDot.getStyleClass().add("sf-timeline-dot");
            if (ppqhDone) {
                ppqhDot.getChildren().add(PpqhIcons.ppqhOf(Feather.CHECK, 12));
            } else if (ppqhIsCurrent) {
                ppqhDot.getChildren().add(PpqhIcons.ppqhOf(ppqhCancelled ? Feather.X : Feather.CIRCLE, ppqhCancelled ? 12 : 8));
            }

            String ppqhText = ppqhIsCurrent && ppqhCancelled ? PpqhOrderStatus.CANCELADO.ppqhLabel() : ppqhStep.ppqhLabel();
            Label ppqhLabel = new Label(ppqhText);
            ppqhLabel.getStyleClass().add("sf-timeline-label");
            ppqhLabel.setWrapText(true);
            ppqhLabel.setMaxWidth(96);

            VBox ppqhNode = new VBox(8, ppqhDot, ppqhLabel);
            ppqhNode.getStyleClass().add("sf-timeline-step");
            ppqhNode.setAlignment(Pos.TOP_CENTER);
            ppqhNode.setMinWidth(80);
            ppqhNode.pseudoClassStateChanged(PPQH_DONE, ppqhDone);
            ppqhNode.pseudoClassStateChanged(PPQH_CURRENT, ppqhIsCurrent && !ppqhCancelled);
            ppqhNode.pseudoClassStateChanged(PPQH_CANCELLED, ppqhIsCurrent && ppqhCancelled);
            ppqhNode.setAccessibleText(ppqhText + (ppqhDone ? ", completado" : ppqhIsCurrent ? ", estado actual" : ", pendiente"));
            getChildren().add(ppqhNode);

            if (ppqhI < PpqhOrderStatus.PPQH_FLOW.size() - 1) {
                Region ppqhLine = new Region();
                ppqhLine.getStyleClass().add("sf-timeline-line");
                ppqhLine.pseudoClassStateChanged(PPQH_DONE, ppqhI < ppqhCurrentIndex);
                ppqhLine.setMinWidth(16);
                HBox.setHgrow(ppqhLine, Priority.ALWAYS);
                ppqhLine.setTranslateY(10);
                getChildren().add(ppqhLine);
            }
        }
    }

    public ObjectProperty<PpqhOrderStatus> statusProperty() {
        return ppqhStatus;
    }

    public PpqhOrderStatus getStatus() {
        return ppqhStatus.get();
    }

    public void setStatus(PpqhOrderStatus ppqhValue) {
        ppqhStatus.set(ppqhValue);
    }
}
