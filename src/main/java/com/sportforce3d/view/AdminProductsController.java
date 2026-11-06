package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.ConfirmDialog;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.GarmentArt;
import com.sportforce3d.component.SfBadge;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.AdminProductRow;
import com.sportforce3d.util.Money;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;
import java.util.function.Function;

public class AdminProductsController {

    private final AppContext context;

    @FXML private SfTextField filterField;
    @FXML private CheckBox deletedCheck;
    @FXML private SfButton newButton;
    @FXML private TableView<AdminProductRow> table;

    public AdminProductsController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        newButton.withIcon(Feather.PLUS);
        newButton.setOnAction(e -> context.router().go(Routes.ADMIN_PRODUCT_EDIT, null));

        PauseTransition debounce = new PauseTransition(Duration.millis(250));
        debounce.setOnFinished(e -> load());
        filterField.textProperty().addListener((obs, old, v) -> debounce.playFromStart());
        deletedCheck.setOnAction(e -> load());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new EmptyState(Feather.TAG, "Sin productos", "No hay productos que coincidan con la búsqueda."));
        table.getColumns().setAll(List.of(
                column("PRODUCTO", 280, this::productCell),
                column("CATEGORÍA", 150, r -> text(r.categoryName(), "body-s")),
                column("PRECIO", 100, r -> text(Money.format(r.priceCents()), "body-s")),
                column("STOCK", 140, this::stockCell),
                column("ESTADO", 110, this::statusCell),
                column("ACCIONES", 190, this::actionsCell)));
        table.setFixedCellSize(72);
        load();
    }

    private void load() {
        String text = filterField.getText();
        boolean deleted = deletedCheck.isSelected();
        Async.run(() -> context.adminProductService().list(text, deleted),
                rows -> table.getItems().setAll(rows),
                e -> table.setPlaceholder(new EmptyState(Feather.LOCK, "No se pudo cargar la lista", ErrorMessages.of(e))));
    }

    private TableColumn<AdminProductRow, AdminProductRow> column(String title, double width,
                                                                 Function<AdminProductRow, Node> renderer) {
        TableColumn<AdminProductRow, AdminProductRow> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setMinWidth(width);
        col.setSortable(false);
        col.setReorderable(false);
        col.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(AdminProductRow row, boolean empty) {
                super.updateItem(row, empty);
                setText(null);
                setGraphic(empty || row == null ? null : renderer.apply(row));
            }
        });
        return col;
    }

    private Node productCell(AdminProductRow r) {
        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("sf-thumb");
        thumb.setMinSize(44, 55);
        thumb.setMaxSize(44, 55);
        Image image = ProductImages.load(context.storage(), r.imagePath(), 44, 55);
        if (image != null) {
            ImageView view = new ImageView(image);
            view.setFitWidth(44);
            view.setFitHeight(55);
            view.setPreserveRatio(true);
            thumb.getChildren().add(view);
        } else {
            thumb.getChildren().add(new GarmentArt(r.garment(), r.colorHex(), 40));
        }
        Label name = text(r.name(), "sf-strong");
        Label sku = text(r.sku(), "code");
        sku.getStyleClass().add("ink-muted");
        HBox box = new HBox(12, thumb, new VBox(2, name, sku));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Node stockCell(AdminProductRow r) {
        // Total disponible de las variantes activas; el aviso dice qué variante lo provoca.
        Label units = text(r.stock() + " u.", "body-s");
        units.setTooltip(new Tooltip(r.variantCount() + " variantes activas · " + r.stock() + " unidades disponibles"));
        if (!r.lowStock() || r.deleted()) {
            return units;
        }
        SfBadge badge = new SfBadge("Bajo", SfBadge.Kind.WARNING).withIcon(Feather.ALERT_TRIANGLE);
        HBox top = new HBox(6, units, badge);
        top.setAlignment(Pos.CENTER_LEFT);

        List<LowStockVariant> low = r.lowStockVariants();
        String detail = low.get(0).label() + (low.size() > 1 ? "  +" + (low.size() - 1) + " más" : "");
        Label first = new Label(detail);
        first.getStyleClass().addAll("body-s", "warning-text");
        String all = low.stream().map(LowStockVariant::label).collect(Collectors.joining("\n"));
        Tooltip tip = new Tooltip("Stock bajo (umbral de aviso: " + low.get(0).threshold() + " u.)\n" + all);
        badge.setTooltip(tip);
        first.setTooltip(tip);
        VBox box = new VBox(2, top, first);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setAccessibleText(r.stock() + " unidades. Stock bajo: " + all.replace("\n", "; "));
        return box;
    }

    private Node statusCell(AdminProductRow r) {
        if (r.deleted()) {
            return new SfBadge("Eliminado", SfBadge.Kind.DANGER).withIcon(Feather.TRASH_2);
        }
        return r.active() ? new SfBadge("Activo", SfBadge.Kind.SUCCESS).withIcon(Feather.CHECK)
                : new SfBadge("Inactivo").withIcon(Feather.EYE_OFF);
    }

    private Node actionsCell(AdminProductRow r) {
        SfButton edit = new SfButton("Editar", SfButton.Variant.GHOST, SfButton.Size.SM);
        edit.setOnAction(e -> context.router().go(Routes.ADMIN_PRODUCT_EDIT, r.id()));
        edit.setMinWidth(Region.USE_PREF_SIZE);
        SfButton second;
        if (r.deleted()) {
            second = new SfButton("Restaurar", SfButton.Variant.GHOST, SfButton.Size.SM);
            second.setOnAction(e -> run(() -> context.adminProductService().restore(r.id()),
                    "Producto restaurado (queda inactivo hasta que lo actives)."));
        } else {
            second = new SfButton("Eliminar", SfButton.Variant.DANGER, SfButton.Size.SM);
            second.setOnAction(e -> ConfirmDialog.show(table, "¿Eliminar “" + r.name() + "”?",
                    "Dejará de venderse y en los carritos aparecerá como no disponible. Los pedidos existentes lo conservan. Puedes restaurarlo después.",
                    "Eliminar", true,
                    () -> run(() -> context.adminProductService().delete(r.id()), "Producto eliminado.")));
        }
        second.setMinWidth(Region.USE_PREF_SIZE);
        HBox box = new HBox(8, edit, second);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void run(Runnable action, String success) {
        Async.run(() -> {
                    action.run();
                    return Boolean.TRUE;
                },
                ok -> {
                    Toast.success(table, success);
                    load();
                    context.refreshCartCount();
                },
                e -> Toast.error(table, ErrorMessages.of(e)));
    }

    private static Label text(String value, String style) {
        Label label = new Label(value);
        label.getStyleClass().addAll(style, "ink");
        return label;
    }
}
