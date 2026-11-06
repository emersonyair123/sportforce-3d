package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.app.Router;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.OrderStatusBadge;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfLink;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.StatusTimeline;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.OrderItem;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.Payment;
import com.sportforce3d.model.PaymentProof;
import com.sportforce3d.model.PaymentStatus;
import com.sportforce3d.model.StatusChange;
import com.sportforce3d.service.AdminOrderService;
import com.sportforce3d.util.Money;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;

import java.time.Instant;
import java.util.List;

/** Administración → Pedidos → detalle: líneas (con uniforme 3D), cliente y envío, pagos, estado e historial. */
public class AdminOrderController implements Router.Routable {

    private final AppContext context;

    @FXML private SfLink backLink;
    @FXML private VBox content;

    public AdminOrderController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        backLink.setGraphic(Icons.of(Feather.ARROW_LEFT, Icons.SM));
        backLink.setOnAction(e -> context.router().go(Routes.ADMIN_ORDERS));
    }

    @Override
    public void onRoute(Object param) {
        if (param instanceof Long id) {
            load(id);
        } else {
            context.router().go(Routes.ADMIN_ORDERS);
        }
    }

    private void load(long id) {
        Async.run(() -> context.adminOrderService().order(id), this::render,
                e -> content.getChildren().setAll(new EmptyState(Feather.ALERT_TRIANGLE, "No se pudo abrir el pedido",
                        ErrorMessages.of(e))));
    }

    private void render(OrderDetail o) {
        Label number = new Label(o.orderNumber());
        number.getStyleClass().addAll("display-l", "ink");
        Label date = new Label("Realizado el " + OrdersController.DATE.format(Instant.parse(o.placedAt()))
                + " · " + o.customerName() + " · " + o.customerEmail());
        date.getStyleClass().addAll("body-s", "ink-muted");
        HBox header = new HBox(16, new VBox(4, number, date), OrderStatusBadge.of(o.status()));
        header.setAlignment(Pos.CENTER_LEFT);

        StatusTimeline timeline = new StatusTimeline();
        if (o.status() == OrderStatus.CANCELADO) {
            timeline.setCancelled(o.lastStatusBeforeCancel());
        } else {
            timeline.setStatus(o.status());
        }

        VBox left = new VBox(24, card("Productos", itemsBox(o)), card("Pagos", paymentsBox(o)));
        left.setPrefWidth(660);
        left.setMinWidth(520);
        HBox.setHgrow(left, Priority.ALWAYS);
        VBox right = new VBox(24, card("Estado", statusBox(o)), card("Envío", shippingBox(o)), card("Historial", historyBox(o)));
        right.setPrefWidth(400);
        right.setMinWidth(340);
        FlowPane columns = new FlowPane(24, 24, left, right);
        content.getChildren().setAll(header, card("Recorrido", timeline), columns);
    }

    // ---------------------------------------------------------------- Productos

    private Node itemsBox(OrderDetail o) {
        VBox items = new VBox(12);
        for (OrderItem i : o.items()) {
            Node marker;
            Image img = i.hasDesign() ? ProductImages.load(context.storage(), i.designFrontPath(), 72, 96) : null;
            if (img != null) {
                ImageView view = new ImageView(img);
                view.setFitWidth(72);
                view.setFitHeight(96);
                view.setPreserveRatio(true);
                StackPane thumb = new StackPane(view);
                thumb.getStyleClass().add("sf-thumb");
                thumb.setMinSize(72, 96);
                thumb.setMaxSize(72, 96);
                marker = thumb;
            } else {
                Rectangle c = new Rectangle(14, 14, color(i.colorHex()));
                c.setArcWidth(4);
                c.setArcHeight(4);
                marker = c;
            }
            Label name = new Label(i.quantity() + " × " + i.productName());
            name.getStyleClass().addAll("body", "ink", "sf-strong");
            name.setWrapText(true);
            Label meta = new Label("Talla " + i.sizeCode() + " · " + i.sku() + " · " + Money.format(i.unitPriceCents())
                    + " c/u" + (i.surchargeCents() == 0 ? "" : " + " + Money.format(i.surchargeCents()) + " materiales"));
            meta.getStyleClass().addAll("body-s", "ink-muted");
            meta.setWrapText(true);
            VBox text = new VBox(4, name, meta);
            if (i.hasDesign()) {
                Label design = new Label("Uniforme personalizado: " + i.designName() + " (versión " + i.designVersion() + ")");
                design.getStyleClass().addAll("body-s", "brand-text");
                SfButton view = new SfButton("Ver uniforme 3D y ficha técnica", SfButton.Variant.GHOST, SfButton.Size.SM)
                        .withIcon(Feather.BOX);
                view.setOnAction(e -> context.router().go(Routes.ADMIN_DESIGN, i.id()));
                text.getChildren().addAll(design, view);
            }
            HBox.setHgrow(text, Priority.ALWAYS);
            text.setMinWidth(0);
            Label total = new Label(Money.format(i.lineTotalCents()));
            total.getStyleClass().addAll("body", "ink");
            total.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = new HBox(14, marker, text, total);
            row.setAlignment(Pos.TOP_LEFT);
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
        return new VBox(12, items, divider,
                CartController.row("Subtotal", Money.format(o.subtotalCents())),
                CartController.row("Recargos por materiales", Money.format(o.surchargeCents())),
                CartController.row("Envío", o.shippingCents() == 0 ? "Gratis" : Money.format(o.shippingCents())),
                totalRow);
    }

    // ---------------------------------------------------------------- Pagos

    private Node paymentsBox(OrderDetail o) {
        VBox box = new VBox(16);
        Label method = new Label(o.paymentMethod().label() + " · verificación manual");
        method.getStyleClass().addAll("body-s", "ink-muted");
        box.getChildren().add(method);
        List<Payment> payments = o.payments();
        for (int idx = payments.size() - 1; idx >= 0; idx--) {
            box.getChildren().add(paymentRow(payments.get(idx)));
        }
        return box;
    }

    private Node paymentRow(Payment p) {
        HBox top = new HBox(10, PaymentActions.badge(p.status()), label(Money.format(p.amountCents()), "body", "ink"));
        top.setAlignment(Pos.CENTER_LEFT);
        VBox info = new VBox(6, top);
        if (p.operationCode() != null) {
            Label code = new Label("Operación " + p.operationCode());
            code.getStyleClass().addAll("code", "ink");
            info.getChildren().add(code);
        }
        switch (p.status()) {
            case PENDIENTE -> info.getChildren().add(label("El cliente aún no registra este pago.", "body-s", "ink-muted"));
            case EN_VERIFICACION -> info.getChildren().add(label("Registrado el "
                    + OrdersController.DATE.format(Instant.parse(p.updatedAt())), "body-s", "ink-muted"));
            case APROBADO -> info.getChildren().add(label("Aprobado por " + p.verifiedBy() + " el "
                    + OrdersController.DATE.format(Instant.parse(p.verifiedAt())), "body-s", "ink-muted"));
            case RECHAZADO -> info.getChildren().add(label("Rechazado"
                    + (p.verifiedBy() == null ? "" : " por " + p.verifiedBy()) + ": " + p.rejectionReason(),
                    "body-s", "danger-text"));
        }
        if (p.status() == PaymentStatus.EN_VERIFICACION) {
            SfButton approve = new SfButton("Aprobar", SfButton.Variant.PRIMARY, SfButton.Size.SM).withIcon(Feather.CHECK);
            approve.setOnAction(e -> PaymentActions.approve(context, content, p, this::render));
            SfButton reject = new SfButton("Rechazar", SfButton.Variant.DANGER, SfButton.Size.SM).withIcon(Feather.X);
            reject.setOnAction(e -> PaymentActions.reject(context, content, p, this::render));
            info.getChildren().add(new HBox(8, approve, reject));
        }
        HBox.setHgrow(info, Priority.ALWAYS);
        HBox row = new HBox(14);
        for (PaymentProof proof : p.proofs()) {
            row.getChildren().add(PaymentActions.proofThumb(context, proof, 72, 96));
        }
        row.getChildren().add(info);
        row.getStyleClass().add("sf-payment-row");
        return row;
    }

    // ---------------------------------------------------------------- Estado

    private Node statusBox(OrderDetail o) {
        List<OrderStatus> next = AdminOrderService.nextStates(o.status());
        VBox box = new VBox(12);
        Label current = label("Estado actual: " + o.status().label(), "body-s", "ink");
        box.getChildren().add(current);
        if (o.status() == OrderStatus.PAGO_EN_VERIFICACION) {
            box.getChildren().add(label("Para pasar a Pagado, aprueba el pago en la sección Pagos de este pedido.",
                    "body-s", "ink-muted"));
        } else if (o.status() == OrderStatus.PENDIENTE_PAGO) {
            box.getChildren().add(label("Esperando que el cliente registre su pago.", "body-s", "ink-muted"));
        }
        if (next.isEmpty()) {
            box.getChildren().add(label("Este pedido ya no cambia de estado.", "body-s", "ink-muted"));
            return box;
        }
        ComboBox<OrderStatus> combo = new ComboBox<>();
        combo.getItems().setAll(next);
        combo.setValue(next.get(0));
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(OrderStatus s) {
                return s == null ? "" : s.label();
            }

            @Override
            public OrderStatus fromString(String s) {
                return null;
            }
        });
        SfTextField note = new SfTextField("Nota para el historial");
        note.setPromptText("Ej. Enviado por Olva Courier, guía 123456");
        SfButton apply = new SfButton("Cambiar estado", SfButton.Variant.PRIMARY, SfButton.Size.SM).withIcon(Feather.ARROW_RIGHT);
        apply.setOnAction(e -> {
            OrderStatus target = combo.getValue();
            Runnable run = () -> {
                apply.setDisable(true);
                Async.run(() -> context.adminOrderService().changeStatus(o.id(), target, note.getText()), updated -> {
                    render(updated);
                    Toast.success(content, o.orderNumber() + ": " + target.label() + ".");
                }, ex -> {
                    apply.setDisable(false);
                    Toast.error(content, ErrorMessages.of(ex));
                });
            };
            if (target == OrderStatus.CANCELADO) {
                com.sportforce3d.component.ConfirmDialog.show(content, "¿Cancelar " + o.orderNumber() + "?",
                        "Se libera el stock reservado y el cliente verá la nota como motivo.", "Cancelar pedido", true, run);
            } else {
                run.run();
            }
        });
        HBox.setHgrow(combo, Priority.ALWAYS);
        box.getChildren().addAll(labeledBox("NUEVO ESTADO", combo), note, apply);
        return box;
    }

    private Node shippingBox(OrderDetail o) {
        Label ship = new Label(o.shipRecipient() + " · " + o.shipPhone() + "\n" + o.shippingOneLine()
                + (o.shipReference() == null ? "" : "\nRef.: " + o.shipReference())
                + (o.notes() == null ? "" : "\n\nNotas del cliente: " + o.notes()));
        ship.getStyleClass().addAll("body-s", "ink");
        ship.setWrapText(true);
        return ship;
    }

    private Node historyBox(OrderDetail o) {
        VBox rows = new VBox(10);
        for (int i = o.history().size() - 1; i >= 0; i--) {
            StatusChange c = o.history().get(i);
            Label when = label(OrdersController.DATE.format(Instant.parse(c.createdAt()))
                    + (c.changedBy() == null ? "" : " · " + c.changedBy()), "body-s", "ink-muted");
            Label what = label(c.to().label() + (c.note() == null ? "" : " — " + c.note()), "body-s", "ink");
            rows.getChildren().add(new VBox(2, what, when));
        }
        return rows;
    }

    // ---------------------------------------------------------------- Utilidades

    private static Label label(String text, String... styles) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styles);
        l.setWrapText(true);
        return l;
    }

    private static VBox labeledBox(String title, Node node) {
        Label l = new Label(title);
        l.getStyleClass().addAll("label-text", "ink-muted");
        return new VBox(6, l, node);
    }

    private static VBox card(String title, Node body) {
        Label label = new Label(title);
        label.getStyleClass().addAll("title-s", "ink");
        VBox card = new VBox(14, label, body);
        card.getStyleClass().add("sf-card");
        card.setPadding(new Insets(20));
        return card;
    }

    private static Color color(String hex) {
        try {
            return Color.web(hex);
        } catch (RuntimeException e) {
            return Color.GRAY;
        }
    }
}
