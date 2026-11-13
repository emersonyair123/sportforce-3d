package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhConfirmDialog;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.HepyGarmentArt;
import com.sportforce3d.component.PpqhSfBadge;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.HepyAdminProductRow;
import com.sportforce3d.model.HepyLowStockVariant;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
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
import javafx.scene.control.Tooltip;
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
import java.util.stream.Collectors;

public class HepyAdminProductsController {

    private final PpqhAppContext hepyContext;

    @FXML private PpqhSfTextField hepyFilterField;
    @FXML private CheckBox hepyDeletedCheck;
    @FXML private PpqhSfButton hepyNewButton;
    @FXML private TableView<HepyAdminProductRow> hepyTable;

    public HepyAdminProductsController(PpqhAppContext hepyContext) {
        this.hepyContext = hepyContext;
    }

    @FXML
    private void initialize() {
        hepyNewButton.ppqhWithIcon(Feather.PLUS);
        hepyNewButton.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_PRODUCT_EDIT, null));

        PauseTransition hepyDebounce = new PauseTransition(Duration.millis(250));
        hepyDebounce.setOnFinished(hepyE -> hepyLoad());
        hepyFilterField.textProperty().addListener((hepyObs, hepyOld, hepyV) -> hepyDebounce.playFromStart());
        hepyDeletedCheck.setOnAction(hepyE -> hepyLoad());

        hepyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        hepyTable.setPlaceholder(new PpqhEmptyState(Feather.TAG, "Sin productos", "No hay productos que coincidan con la búsqueda."));
        hepyTable.getColumns().setAll(List.of(
                hepyColumn("PRODUCTO", 250, this::hepyProductCell),
                hepyColumn("CATEGORÍA", 170, hepyR -> hepyText(hepyR.hepyCategoryName(), "body-s")),
                hepyColumn("PRECIO", 90, hepyR -> hepyText(HepyMoney.hepyFormat(hepyR.hepyPriceCents()), "body-s")),
                hepyColumn("STOCK", 170, this::hepyStockCell),
                hepyColumn("ESTADO", 110, this::hepyStatusCell),
                hepyColumn("ACCIONES", 180, this::hepyActionsCell)));
        hepyTable.setFixedCellSize(72);
        hepyLoad();
    }

    private void hepyLoad() {
        String hepyText = hepyFilterField.getText();
        boolean hepyDeleted = hepyDeletedCheck.isSelected();
        PpqhAsync.ppqhRun(() -> hepyContext.ppqhAdminProductService().hepyList(hepyText, hepyDeleted),
                hepyRows -> hepyTable.getItems().setAll(hepyRows),
                hepyE -> hepyTable.setPlaceholder(new PpqhEmptyState(Feather.LOCK, "No se pudo cargar la lista", PpqhErrorMessages.ppqhOf(hepyE))));
    }

    private TableColumn<HepyAdminProductRow, HepyAdminProductRow> hepyColumn(String hepyTitle, double hepyWidth,
                                                                 Function<HepyAdminProductRow, Node> hepyRenderer) {
        TableColumn<HepyAdminProductRow, HepyAdminProductRow> hepyCol = new TableColumn<>(hepyTitle);
        hepyCol.setPrefWidth(hepyWidth);
        hepyCol.setMinWidth(hepyWidth);
        hepyCol.setSortable(false);
        hepyCol.setReorderable(false);
        hepyCol.setCellValueFactory(hepyData -> new ReadOnlyObjectWrapper<>(hepyData.getValue()));
        hepyCol.setCellFactory(hepyC -> new TableCell<>() {
            @Override
            protected void updateItem(HepyAdminProductRow hepyRow, boolean hepyEmpty) {
                super.updateItem(hepyRow, hepyEmpty);
                setText(null);
                setGraphic(hepyEmpty || hepyRow == null ? null : hepyRenderer.apply(hepyRow));
            }
        });
        return hepyCol;
    }

    private Node hepyProductCell(HepyAdminProductRow hepyR) {
        StackPane hepyThumb = new StackPane();
        hepyThumb.getStyleClass().add("sf-thumb");
        hepyThumb.setMinSize(44, 55);
        hepyThumb.setMaxSize(44, 55);
        Image hepyImage = HepyProductImages.hepyLoad(hepyContext.ppqhStorage(), hepyR.hepyImagePath(), 44, 55);
        if (hepyImage != null) {
            ImageView hepyView = new ImageView(hepyImage);
            hepyView.setFitWidth(44);
            hepyView.setFitHeight(55);
            hepyView.setPreserveRatio(true);
            hepyThumb.getChildren().add(hepyView);
        } else {
            hepyThumb.getChildren().add(new HepyGarmentArt(hepyR.hepyGarment(), hepyR.hepyColorHex(), 40));
        }
        Label hepyName = hepyText(hepyR.hepyName(), "sf-strong");
        Label hepySku = hepyText(hepyR.hepySku(), "code");
        hepySku.getStyleClass().add("ink-muted");
        HBox hepyBox = new HBox(12, hepyThumb, new VBox(2, hepyName, hepySku));
        hepyBox.setAlignment(Pos.CENTER_LEFT);
        return hepyBox;
    }

    private Node hepyStockCell(HepyAdminProductRow hepyR) {
        Label hepyUnits = hepyText(hepyR.hepyStock() + " u.", "body-s");
        hepyUnits.setTooltip(new Tooltip(hepyR.hepyVariantCount() + " variantes activas · " + hepyR.hepyStock() + " unidades disponibles"));
        if (!hepyR.hepyLowStock() || hepyR.hepyDeleted()) {
            return hepyUnits;
        }
        PpqhSfBadge hepyBadge = new PpqhSfBadge("Bajo", PpqhSfBadge.PpqhKind.WARNING).ppqhWithIcon(Feather.ALERT_TRIANGLE);
        HBox hepyTop = new HBox(6, hepyUnits, hepyBadge);
        hepyTop.setAlignment(Pos.CENTER_LEFT);

        List<HepyLowStockVariant> hepyLow = hepyR.hepyLowStockVariants();
        String hepyDetail = hepyLow.get(0).hepyLabel() + (hepyLow.size() > 1 ? "  +" + (hepyLow.size() - 1) + " más" : "");
        Label hepyFirst = new Label(hepyDetail);
        hepyFirst.getStyleClass().addAll("body-s", "warning-text");
        String hepyAll = hepyLow.stream().map(HepyLowStockVariant::hepyLabel).collect(Collectors.joining("\n"));
        Tooltip hepyTip = new Tooltip("Stock bajo (umbral de aviso: " + hepyLow.get(0).hepyThreshold() + " u.)\n" + hepyAll);
        hepyBadge.setTooltip(hepyTip);
        hepyFirst.setTooltip(hepyTip);
        VBox hepyBox = new VBox(2, hepyTop, hepyFirst);
        hepyBox.setAlignment(Pos.CENTER_LEFT);
        hepyBox.setAccessibleText(hepyR.hepyStock() + " unidades. Stock bajo: " + hepyAll.replace("\n", "; "));
        return hepyBox;
    }

    private Node hepyStatusCell(HepyAdminProductRow hepyR) {
        if (hepyR.hepyDeleted()) {
            return new PpqhSfBadge("Eliminado", PpqhSfBadge.PpqhKind.DANGER).ppqhWithIcon(Feather.TRASH_2);
        }
        return hepyR.hepyActive() ? new PpqhSfBadge("Activo", PpqhSfBadge.PpqhKind.SUCCESS).ppqhWithIcon(Feather.CHECK)
                : new PpqhSfBadge("Inactivo").ppqhWithIcon(Feather.EYE_OFF);
    }

    private Node hepyActionsCell(HepyAdminProductRow hepyR) {
        PpqhSfButton hepyEdit = new PpqhSfButton("Editar", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
        hepyEdit.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_PRODUCT_EDIT, hepyR.hepyId()));
        hepyEdit.setMinWidth(Region.USE_PREF_SIZE);
        PpqhSfButton hepySecond;
        if (hepyR.hepyDeleted()) {
            hepySecond = new PpqhSfButton("Restaurar", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
            hepySecond.setOnAction(hepyE -> hepyRun(() -> hepyContext.ppqhAdminProductService().hepyRestore(hepyR.hepyId()),
                    "Producto restaurado (queda inactivo hasta que lo actives)."));
        } else {
            hepySecond = new PpqhSfButton("Eliminar", PpqhSfButton.PpqhVariant.DANGER, PpqhSfButton.PpqhSize.SM);
            hepySecond.setOnAction(hepyE -> PpqhConfirmDialog.ppqhShow(hepyTable, "¿Eliminar “" + hepyR.hepyName() + "”?",
                    "Dejará de venderse y en los carritos aparecerá como no disponible. Los pedidos existentes lo conservan. Puedes restaurarlo después.",
                    "Eliminar", true,
                    () -> hepyRun(() -> hepyContext.ppqhAdminProductService().hepyDelete(hepyR.hepyId()), "Producto eliminado.")));
        }
        hepySecond.setMinWidth(Region.USE_PREF_SIZE);
        HBox hepyBox = new HBox(8, hepyEdit, hepySecond);
        hepyBox.setAlignment(Pos.CENTER_LEFT);
        return hepyBox;
    }

    private void hepyRun(Runnable hepyAction, String hepySuccess) {
        PpqhAsync.ppqhRun(() -> {
                    hepyAction.run();
                    return Boolean.TRUE;
                },
                hepyOk -> {
                    PpqhToast.ppqhSuccess(hepyTable, hepySuccess);
                    hepyLoad();
                    hepyContext.ppqhRefreshCartCount();
                },
                hepyE -> PpqhToast.ppqhError(hepyTable, PpqhErrorMessages.ppqhOf(hepyE)));
    }

    private static Label hepyText(String hepyValue, String hepyStyle) {
        Label hepyLabel = new Label(hepyValue);
        hepyLabel.getStyleClass().addAll(hepyStyle, "ink");
        return hepyLabel;
    }
}
