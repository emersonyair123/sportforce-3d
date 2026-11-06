package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.OrderStatusBadge;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.model.OrderSummary;
import com.sportforce3d.util.Money;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrdersController {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Lima"));

    private final AppContext context;

    @FXML private VBox list;

    public OrdersController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        Async.run(context.orderService()::myOrders, this::render,
                e -> list.getChildren().setAll(new EmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar tus pedidos",
                        ErrorMessages.of(e))));
    }

    private void render(List<OrderSummary> orders) {
        if (orders.isEmpty()) {
            SfButton browse = new SfButton("Ver catálogo", SfButton.Variant.PRIMARY);
            browse.setOnAction(e -> context.router().go(Routes.CATALOG));
            list.getChildren().setAll(new EmptyState(Feather.PACKAGE, "Aún no tienes pedidos",
                    "Cuando compres algo, aquí verás su estado.", browse));
            return;
        }
        list.getChildren().clear();
        for (OrderSummary o : orders) {
            Label number = new Label(o.orderNumber());
            number.getStyleClass().addAll("code", "ink");
            Label date = new Label(DATE.format(Instant.parse(o.placedAt())) + " · "
                    + (o.itemCount() == 1 ? "1 producto" : o.itemCount() + " productos") + " · " + o.paymentMethod().label());
            date.getStyleClass().addAll("body-s", "ink-muted");
            VBox text = new VBox(4, number, date);
            HBox.setHgrow(text, Priority.ALWAYS);

            Label total = new Label(Money.format(o.totalCents()));
            total.getStyleClass().addAll("price", "ink");
            total.setMinWidth(Region.USE_PREF_SIZE);

            HBox row = new HBox(16, text, OrderStatusBadge.of(o.status()), total, Icons.of(Feather.CHEVRON_RIGHT, Icons.MD));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().addAll("sf-card", "sf-row-link");
            row.setPadding(new Insets(16, 20, 16, 20));
            row.setCursor(Cursor.HAND);
            row.setFocusTraversable(true);
            row.setAccessibleText("Pedido " + o.orderNumber() + ", " + o.status().label() + ", " + Money.format(o.totalCents()));
            row.setOnMouseClicked(e -> context.router().go(Routes.ORDER, o.id()));
            row.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    context.router().go(Routes.ORDER, o.id());
                }
            });
            list.getChildren().add(row);
        }
    }
}
