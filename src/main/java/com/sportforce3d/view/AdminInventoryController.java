package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.SfBadge;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.InventoryRow;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;
import java.util.function.Function;

public class AdminInventoryController {

    private final AppContext context;

    @FXML private SfTextField filterField;
    @FXML private CheckBox lowOnlyCheck;
    @FXML private Label summaryLabel;
    @FXML private TableView<InventoryRow> table;

    public AdminInventoryController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        PauseTransition debounce = new PauseTransition(Duration.millis(250));
        debounce.setOnFinished(e -> load());
        filterField.textProperty().addListener((obs, old, v) -> debounce.playFromStart());
        lowOnlyCheck.setOnAction(e -> load());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new EmptyState(Feather.ARCHIVE, "Sin variantes", "No hay variantes que coincidan con la búsqueda."));
        table.setFixedCellSize(60);
        table.getColumns().setAll(List.of(
                column("PRODUCTO", 300, this::productCell),
                column("TALLA", 90, r -> label(r.sizeName(), "body-s")),
                column("COLOR", 170, this::colorCell),
                column("DISPONIBLE", 160, this::availableCell),
                column("RESERVADO", 100, r -> label(String.valueOf(r.reserved()), "body-s")),
                column("STOCK · AVISO", 300, this::editCell)));
        load();
    }

    private void load() {
        String text = filterField.getText();
        boolean low = lowOnlyCheck.isSelected();
        Async.run(() -> context.adminProductService().inventory(text, low),
                rows -> {
                    table.getItems().setAll(rows);
                    long lowCount = rows.stream().filter(InventoryRow::lowStock).count();
                    summaryLabel.setText(rows.size() + " variantes · " + lowCount + " con stock bajo");
                },
                e -> table.setPlaceholder(new EmptyState(Feather.LOCK, "No se pudo cargar el inventario", ErrorMessages.of(e))));
    }

    private TableColumn<InventoryRow, InventoryRow> column(String title, double width, Function<InventoryRow, Node> renderer) {
        TableColumn<InventoryRow, InventoryRow> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setMinWidth(width);
        col.setSortable(false);
        col.setReorderable(false);
        col.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(InventoryRow row, boolean empty) {
                super.updateItem(row, empty);
                setText(null);
                setGraphic(empty || row == null ? null : renderer.apply(row));
            }
        });
        return col;
    }

    private Node productCell(InventoryRow r) {
        Label name = label(r.productName(), "sf-strong");
        Label sku = label(r.sku(), "code");
        sku.getStyleClass().add("ink-muted");
        VBox box = new VBox(2, name, sku);
        if (!r.productActive()) {
            name.setText(r.productName() + " (inactivo)");
        }
        return box;
    }

    private Node colorCell(InventoryRow r) {
        Rectangle swatch = new Rectangle(14, 14);
        swatch.setArcWidth(4);
        swatch.setArcHeight(4);
        try {
            swatch.setFill(Color.web(r.colorHex()));
        } catch (RuntimeException e) {
            swatch.setFill(Color.TRANSPARENT);
        }
        HBox box = new HBox(8, swatch, label(r.colorName(), "body-s"));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Node availableCell(InventoryRow r) {
        Label units = label(String.valueOf(r.available()), "body-s");
        HBox box = new HBox(8, units);
        box.setAlignment(Pos.CENTER_LEFT);
        if (r.available() == 0) {
            box.getChildren().add(new SfBadge("Agotado", SfBadge.Kind.DANGER).withIcon(Feather.SLASH));
        } else if (r.lowStock()) {
            box.getChildren().add(new SfBadge("Stock bajo", SfBadge.Kind.WARNING).withIcon(Feather.ALERT_TRIANGLE));
        }
        return box;
    }

    private Node editCell(InventoryRow r) {
        TextField stock = new TextField(String.valueOf(r.quantity()));
        stock.getStyleClass().addAll("sf-input-sm", "code");
        stock.setPrefWidth(80);
        stock.setAccessibleText("Stock de " + r.sku());
        TextField threshold = new TextField(String.valueOf(r.lowStockThreshold()));
        threshold.getStyleClass().addAll("sf-input-sm", "code");
        threshold.setPrefWidth(64);
        threshold.setAccessibleText("Aviso de stock bajo de " + r.sku());
        SfButton save = new SfButton("Guardar", SfButton.Variant.GHOST, SfButton.Size.SM);
        save.setDisable(true);
        Runnable dirty = () -> save.setDisable(stock.getText().equals(String.valueOf(r.quantity()))
                && threshold.getText().equals(String.valueOf(r.lowStockThreshold())));
        stock.textProperty().addListener((obs, old, v) -> dirty.run());
        threshold.textProperty().addListener((obs, old, v) -> dirty.run());
        save.setOnAction(e -> {
            int q;
            int t;
            try {
                q = Integer.parseInt(stock.getText().trim());
                t = Integer.parseInt(threshold.getText().trim());
            } catch (NumberFormatException ex) {
                Toast.error(table, "Escribe números enteros en stock y aviso.");
                return;
            }
            Async.run(() -> context.adminProductService().updateStock(r.variantId(), q, t),
                    updated -> {
                        Toast.success(table, "Stock de " + r.sku() + " actualizado.");
                        load();
                    },
                    ex -> Toast.error(table, ErrorMessages.of(ex)));
        });
        stock.setOnAction(e -> save.fire());
        threshold.setOnAction(e -> save.fire());
        HBox box = new HBox(8, stock, threshold, save);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Label label(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().addAll(style, "ink");
        return l;
    }
}
