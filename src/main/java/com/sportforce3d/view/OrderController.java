package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.app.Router;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.ConfirmDialog;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.OrderStatusBadge;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfLink;
import com.sportforce3d.component.StatusTimeline;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.OrderItem;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.StatusChange;
import com.sportforce3d.util.Money;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;

import java.time.Instant;
import java.util.Set;

public class OrderController implements Router.Routable {

    private static final Set<OrderStatus> CANCELLABLE = Set.of(OrderStatus.PENDIENTE_PAGO, OrderStatus.PAGO_EN_VERIFICACION);

    private final AppContext context;

    @FXML private SfLink backLink;
    @FXML private VBox content;

    public OrderController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        backLink.setGraphic(Icons.of(Feather.ARROW_LEFT, Icons.SM));
        backLink.setOnAction(e -> context.router().go(Routes.ORDERS));
    }

    @Override
    public void onRoute(Object param) {
        if (param instanceof Long id) {
            load(id);
        } else {
            context.router().go(Routes.ORDERS);
        }
    }

    private void load(long id) {
        Async.run(() -> context.orderService().order(id), this::render,
                e -> content.getChildren().setAll(new EmptyState(Feather.ALERT_TRIANGLE, "No se pudo abrir el pedido",
                        ErrorMessages.of(e))));
    }

    private void render(OrderDetail o) {
        Label number = new Label(o.orderNumber());
        number.getStyleClass().addAll("display-l", "ink");
        Label date = new Label("Realizado el " + OrdersController.DATE.format(Instant.parse(o.placedAt())));
        date.getStyleClass().addAll("body-s", "ink-muted");
        HBox header = new HBox(16, new VBox(4, number, date), OrderStatusBadge.of(o.status()));
        header.setAlignment(Pos.CENTER_LEFT);

        StatusTimeline timeline = new StatusTimeline();
        if (o.status() == OrderStatus.CANCELADO) {
            timeline.setCancelled(o.lastStatusBeforeCancel());
        } else {
            timeline.setStatus(o.status());
        }
        VBox timelineCard = card("Estado del pedido", timeline);

        VBox items = new VBox(10);
        for (OrderItem i : o.items()) {
            Rectangle color = new Rectangle(14, 14, parse(i.colorHex()));
            color.setArcWidth(4);
            color.setArcHeight(4);
            Label name = new Label(i.quantity() + " × " + i.productName());
            name.getStyleClass().addAll("body", "ink");
            name.setWrapText(true);
            Label meta = new Label("Talla " + i.sizeCode() + " · " + i.sku() + " · " + Money.format(i.unitPriceCents()) + " c/u");
            meta.getStyleClass().addAll("body-s", "ink-muted");
            VBox text = new VBox(2, name, meta);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label total = new Label(Money.format(i.lineTotalCents()));
            total.getStyleClass().addAll("body", "ink");
            total.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = new HBox(12, color, text, total);
            row.setAlignment(Pos.CENTER_LEFT);
            items.getChildren().add(row);
        }
        Region divider = new Region();
        divider.getStyleClass().add("sf-divider");
        Label totalLabel = new Label("Total");
        totalLabel.getStyleClass().addAll("title-s", "ink");
        Label total = new Label(Money.format(o.totalCents()));
        total.getStyleClass().addAll("price", "ink");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox totalRow = new HBox(totalLabel, spacer, total);
        totalRow.setAlignment(Pos.BASELINE_LEFT);
        VBox itemsCard = card("Productos", new VBox(12, items, divider,
                CartController.row("Subtotal", Money.format(o.subtotalCents())),
                CartController.row("Recargos por materiales", Money.format(o.surchargeCents())),
                CartController.row("Envío", o.shippingCents() == 0 ? "Gratis" : Money.format(o.shippingCents())),
                totalRow));
        itemsCard.setPrefWidth(600);
        itemsCard.setMinWidth(460);

        Label ship = new Label(o.shipRecipient() + " · " + o.shipPhone() + "\n" + o.shippingOneLine()
                + (o.shipReference() == null ? "" : "\nRef.: " + o.shipReference()));
        ship.getStyleClass().addAll("body-s", "ink");
        ship.setWrapText(true);
        Label pay = new Label(o.paymentMethod().label());
        pay.getStyleClass().addAll("body-s", "ink");
        VBox side = new VBox(16, card("Envío", ship), card("Pago", new VBox(8, pay, paymentStatus(o))));
        if (o.notes() != null) {
            Label notes = new Label(o.notes());
            notes.getStyleClass().addAll("body-s", "ink-muted");
            notes.setWrapText(true);
            side.getChildren().add(card("Notas", notes));
        }
        if (CANCELLABLE.contains(o.status())) {
            SfButton cancel = new SfButton("Cancelar pedido", SfButton.Variant.DANGER).withIcon(Feather.X_CIRCLE);
            cancel.setMaxWidth(Double.MAX_VALUE);
            cancel.setOnAction(e -> ConfirmDialog.show(content, "¿Cancelar el pedido " + o.orderNumber() + "?",
                    "Se liberará el stock reservado. Si ya pagaste, escribe a la tienda antes de cancelar.",
                    "Cancelar pedido", true, () -> cancel(o)));
            side.getChildren().add(cancel);
        }
        side.setPrefWidth(340);
        side.setMinWidth(300);

        FlowPane columns = new FlowPane(24, 24, itemsCard, side);
        content.getChildren().setAll(header, timelineCard, columns, historyCard(o));
    }

    private Node paymentStatus(OrderDetail o) {
        Label text = new Label(switch (o.status()) {
            case PENDIENTE_PAGO -> "Pendiente de pago. Pronto podrás registrar tu código de operación desde aquí.";
            case PAGO_EN_VERIFICACION -> "Pago en verificación: un administrador lo está revisando.";
            case CANCELADO -> "Pedido cancelado.";
            default -> "Pago confirmado.";
        });
        text.getStyleClass().addAll("body-s", "ink-muted");
        text.setWrapText(true);
        VBox box = new VBox(8, text);
        if (o.status() == OrderStatus.PENDIENTE_PAGO) {
            // El registro del pago (código de operación y comprobante) llega en la Etapa 5.
            box.getChildren().add(new SfButton("Registrar pago", SfButton.Variant.PRIMARY, SfButton.Size.SM).comingSoon());
        }
        return box;
    }

    private VBox historyCard(OrderDetail o) {
        VBox rows = new VBox(8);
        for (StatusChange change : o.history()) {
            Label when = new Label(OrdersController.DATE.format(Instant.parse(change.createdAt())));
            when.getStyleClass().addAll("code", "ink-muted");
            when.setMinWidth(150);
            Label what = new Label(change.to().label() + (change.note() == null ? "" : " — " + change.note())
                    + (change.changedBy() == null ? "" : " (" + change.changedBy() + ")"));
            what.getStyleClass().addAll("body-s", "ink");
            what.setWrapText(true);
            rows.getChildren().add(new HBox(12, when, what));
        }
        return card("Historial", rows);
    }

    private void cancel(OrderDetail o) {
        Async.run(() -> context.orderService().cancel(o.id(), null),
                updated -> {
                    render(updated);
                    Toast.success(content, "Pedido " + o.orderNumber() + " cancelado.");
                },
                e -> Toast.error(content, ErrorMessages.of(e)));
    }

    private static VBox card(String title, Node body) {
        Label label = new Label(title);
        label.getStyleClass().addAll("title-s", "ink");
        VBox card = new VBox(12, label, body);
        card.getStyleClass().add("sf-card");
        card.setPadding(new Insets(20));
        return card;
    }

    private static Color parse(String hex) {
        try {
            return Color.web(hex);
        } catch (RuntimeException e) {
            return Color.GRAY;
        }
    }
}
