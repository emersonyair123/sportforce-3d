package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.app.Router;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.FilterChip;
import com.sportforce3d.component.OrderStatusBadge;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.OrderSummary;
import com.sportforce3d.util.Money;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AdminOrdersController implements Router.Routable {

    private final AppContext context;
    private final ToggleGroup chips = new ToggleGroup();
    private OrderStatus status;

    @FXML private SfTextField filterField;
    @FXML private Label countLabel;
    @FXML private FlowPane statusChips;
    @FXML private TableView<OrderSummary> table;

    public AdminOrdersController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        PauseTransition debounce = new PauseTransition(Duration.millis(250));
        debounce.setOnFinished(e -> load());
        filterField.textProperty().addListener((obs, old, v) -> debounce.playFromStart());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new EmptyState(Feather.CLIPBOARD, "Sin pedidos", "No hay pedidos con estos filtros."));
        table.getColumns().setAll(List.of(
                column("PEDIDO", 190, this::numberCell),
                column("CLIENTE", 200, o -> text(o.customerName(), "body-s")),
                column("ESTADO", 190, o -> OrderStatusBadge.of(o.status())),
                column("PAGO", 150, o -> text(o.paymentMethod().label(), "body-s")),
                column("ÍTEMS", 70, o -> text(String.valueOf(o.itemCount()), "body-s")),
                column("TOTAL", 110, o -> text(Money.format(o.totalCents()), "sf-strong")),
                column("", 110, this::openCell)));
        table.setFixedCellSize(60);
        table.setRowFactory(t -> {
            TableRow<OrderSummary> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && row.getItem() != null) {
                    open(row.getItem());
                }
            });
            return row;
        });
    }

    @Override
    public void onRoute(Object param) {
        status = param instanceof OrderStatus s ? s : null;
        loadCounts();
        load();
    }

    private void loadCounts() {
        Async.run(() -> context.adminOrderService().counts(), this::renderChips, e -> { });
    }

    private void renderChips(Map<OrderStatus, Integer> counts) {
        statusChips.getChildren().clear();
        chips.getToggles().clear();
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        statusChips.getChildren().add(chip("Todos (" + total + ")", null));
        for (OrderStatus s : OrderStatus.values()) {
            statusChips.getChildren().add(chip(s.label() + " (" + counts.getOrDefault(s, 0) + ")", s));
        }
    }

    private FilterChip chip(String label, OrderStatus value) {
        FilterChip chip = new FilterChip(label);
        chip.setToggleGroup(chips);
        chip.setSelected(value == status);
        chip.setOnAction(e -> {
            chip.setSelected(true);
            status = value;
            load();
        });
        return chip;
    }

    private void load() {
        OrderStatus s = status;
        String text = filterField.getText();
        Async.run(() -> context.adminOrderService().list(s, text), rows -> {
            table.getItems().setAll(rows);
            countLabel.setText(rows.size() == 1 ? "1 pedido" : rows.size() + " pedidos");
        }, e -> table.setPlaceholder(new EmptyState(Feather.LOCK, "No se pudo cargar la lista", ErrorMessages.of(e))));
    }

    private void open(OrderSummary o) {
        context.router().go(Routes.ADMIN_ORDER, o.id());
    }

    private Node numberCell(OrderSummary o) {
        Label number = text(o.orderNumber(), "code");
        Label date = text(OrdersController.DATE.format(Instant.parse(o.placedAt())), "body-s");
        date.getStyleClass().add("ink-muted");
        VBox box = new VBox(2, number, date);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Node openCell(OrderSummary o) {
        SfButton open = new SfButton("Abrir", SfButton.Variant.GHOST, SfButton.Size.SM);
        open.setOnAction(e -> open(o));
        return open;
    }

    private static Label text(String value, String style) {
        Label l = new Label(value);
        l.getStyleClass().addAll(style, "ink");
        return l;
    }

    private static TableColumn<OrderSummary, OrderSummary> column(String title, double width,
                                                                  Function<OrderSummary, Node> renderer) {
        TableColumn<OrderSummary, OrderSummary> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setMinWidth(width);
        col.setSortable(false);
        col.setReorderable(false);
        col.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(OrderSummary row, boolean empty) {
                super.updateItem(row, empty);
                setText(null);
                setGraphic(empty || row == null ? null : renderer.apply(row));
            }
        });
        return col;
    }
}
