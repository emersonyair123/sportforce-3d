package com.sportforce3d.component;

import com.sportforce3d.model.OrderStatus;
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

/**
 * Línea de tiempo horizontal de un pedido. Cada paso dice su estado con texto e icono
 * (hecho ✓, actual •, pendiente vacío), no solo con color. CANCELADO marca el paso donde se detuvo.
 */
public class StatusTimeline extends HBox {

    private static final PseudoClass DONE = PseudoClass.getPseudoClass("done");
    private static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");
    private static final PseudoClass CANCELLED = PseudoClass.getPseudoClass("cancelled");

    private final ObjectProperty<OrderStatus> status = new SimpleObjectProperty<>(this, "status");
    /** Último estado alcanzado antes de cancelar (para dibujar dónde se detuvo). */
    private OrderStatus cancelledAt = OrderStatus.PENDIENTE_PAGO;

    public StatusTimeline() {
        getStyleClass().add("sf-timeline");
        setAlignment(Pos.TOP_LEFT);
        status.addListener((obs, old, s) -> rebuild());
        setStatus(OrderStatus.PENDIENTE_PAGO);
    }

    public void setCancelled(OrderStatus lastReached) {
        this.cancelledAt = lastReached == null ? OrderStatus.PENDIENTE_PAGO : lastReached;
        status.set(null);
        status.set(OrderStatus.CANCELADO);
    }

    private void rebuild() {
        getChildren().clear();
        OrderStatus current = status.get();
        if (current == null) {
            return;
        }
        boolean cancelled = current == OrderStatus.CANCELADO;
        int currentIndex = OrderStatus.FLOW.indexOf(cancelled ? cancelledAt : current);

        for (int i = 0; i < OrderStatus.FLOW.size(); i++) {
            OrderStatus step = OrderStatus.FLOW.get(i);
            boolean done = i < currentIndex || (!cancelled && current == OrderStatus.ENTREGADO && i == currentIndex);
            boolean isCurrent = i == currentIndex && !done;

            StackPane dot = new StackPane();
            dot.getStyleClass().add("sf-timeline-dot");
            if (done) {
                dot.getChildren().add(Icons.of(Feather.CHECK, 12));
            } else if (isCurrent) {
                dot.getChildren().add(Icons.of(cancelled ? Feather.X : Feather.CIRCLE, cancelled ? 12 : 8));
            }

            String text = isCurrent && cancelled ? OrderStatus.CANCELADO.label() : step.label();
            Label label = new Label(text);
            label.getStyleClass().add("sf-timeline-label");
            label.setWrapText(true);
            label.setMaxWidth(96);

            VBox node = new VBox(8, dot, label);
            node.getStyleClass().add("sf-timeline-step");
            node.setAlignment(Pos.TOP_CENTER);
            node.setMinWidth(80);
            node.pseudoClassStateChanged(DONE, done);
            node.pseudoClassStateChanged(CURRENT, isCurrent && !cancelled);
            node.pseudoClassStateChanged(CANCELLED, isCurrent && cancelled);
            node.setAccessibleText(text + (done ? ", completado" : isCurrent ? ", estado actual" : ", pendiente"));
            getChildren().add(node);

            if (i < OrderStatus.FLOW.size() - 1) {
                Region line = new Region();
                line.getStyleClass().add("sf-timeline-line");
                line.pseudoClassStateChanged(DONE, i < currentIndex);
                line.setMinWidth(16);
                HBox.setHgrow(line, Priority.ALWAYS);
                line.setTranslateY(10);
                getChildren().add(line);
            }
        }
    }

    public ObjectProperty<OrderStatus> statusProperty() {
        return status;
    }

    public OrderStatus getStatus() {
        return status.get();
    }

    public void setStatus(OrderStatus value) {
        status.set(value);
    }
}
