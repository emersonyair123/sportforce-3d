package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhSfBadge;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.HepyInventoryRow;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;
import java.util.function.Function;

public class HepyAdminInventoryController {

    private final PpqhAppContext hepyContext;

    @FXML private PpqhSfTextField hepyFilterField;
    @FXML private CheckBox hepyLowOnlyCheck;
    @FXML private Label hepySummaryLabel;
    @FXML private TableView<HepyInventoryRow> hepyTable;

    public HepyAdminInventoryController(PpqhAppContext hepyContext) {
        this.hepyContext = hepyContext;
    }

    @FXML
    private void initialize() {
        PauseTransition hepyDebounce = new PauseTransition(Duration.millis(250));
        hepyDebounce.setOnFinished(hepyE -> hepyLoad());
        hepyFilterField.textProperty().addListener((hepyObs, hepyOld, hepyV) -> hepyDebounce.playFromStart());
        hepyLowOnlyCheck.setOnAction(hepyE -> hepyLoad());

        hepyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        hepyTable.setPlaceholder(new PpqhEmptyState(Feather.ARCHIVE, "Sin variantes", "No hay variantes que coincidan con la búsqueda."));
        hepyTable.setFixedCellSize(60);
        hepyTable.getColumns().setAll(List.of(
                hepyColumn("PRODUCTO", 260, this::hepyProductCell),
                hepyColumn("TALLA", 70, hepyR -> hepyLabel(hepyR.hepySizeName(), "body-s")),
                hepyColumn("COLOR", 150, this::hepyColorCell),
                hepyColumn("DISPONIBLE", 160, this::hepyAvailableCell),
                hepyColumn("RESERVADO", 90, hepyR -> hepyLabel(String.valueOf(hepyR.hepyReserved()), "body-s")),
                hepyColumn("STOCK · AVISO", 250, this::hepyEditCell)));
        hepyLoad();
    }

    private void hepyLoad() {
        String hepyText = hepyFilterField.getText();
        boolean hepyLow = hepyLowOnlyCheck.isSelected();
        PpqhAsync.ppqhRun(() -> hepyContext.ppqhAdminProductService().hepyInventory(hepyText, hepyLow),
                hepyRows -> {
                    hepyTable.getItems().setAll(hepyRows);
                    long hepyLowCount = hepyRows.stream().filter(HepyInventoryRow::hepyLowStock).count();
                    hepySummaryLabel.setText(hepyRows.size() + " variantes · " + hepyLowCount + " con stock bajo");
                },
                hepyE -> hepyTable.setPlaceholder(new PpqhEmptyState(Feather.LOCK, "No se pudo cargar el inventario", PpqhErrorMessages.ppqhOf(hepyE))));
    }

    private TableColumn<HepyInventoryRow, HepyInventoryRow> hepyColumn(String hepyTitle, double hepyWidth, Function<HepyInventoryRow, Node> hepyRenderer) {
        TableColumn<HepyInventoryRow, HepyInventoryRow> hepyCol = new TableColumn<>(hepyTitle);
        hepyCol.setPrefWidth(hepyWidth);
        hepyCol.setMinWidth(hepyWidth);
        hepyCol.setSortable(false);
        hepyCol.setReorderable(false);
        hepyCol.setCellValueFactory(hepyData -> new ReadOnlyObjectWrapper<>(hepyData.getValue()));
        hepyCol.setCellFactory(hepyC -> new TableCell<>() {
            @Override
            protected void updateItem(HepyInventoryRow hepyRow, boolean hepyEmpty) {
                super.updateItem(hepyRow, hepyEmpty);
                setText(null);
                setGraphic(hepyEmpty || hepyRow == null ? null : hepyRenderer.apply(hepyRow));
            }
        });
        return hepyCol;
    }

    private Node hepyProductCell(HepyInventoryRow hepyR) {
        Label hepyName = hepyLabel(hepyR.hepyProductName(), "sf-strong");
        Label hepySku = hepyLabel(hepyR.hepySku(), "code");
        hepySku.getStyleClass().add("ink-muted");
        VBox hepyBox = new VBox(2, hepyName, hepySku);
        if (!hepyR.hepyProductActive()) {
            hepyName.setText(hepyR.hepyProductName() + " (inactivo)");
        }
        return hepyBox;
    }

    private Node hepyColorCell(HepyInventoryRow hepyR) {
        Rectangle hepySwatch = new Rectangle(14, 14);
        hepySwatch.setArcWidth(4);
        hepySwatch.setArcHeight(4);
        try {
            hepySwatch.setFill(Color.web(hepyR.hepyColorHex()));
        } catch (RuntimeException hepyE) {
            hepySwatch.setFill(Color.TRANSPARENT);
        }
        HBox hepyBox = new HBox(8, hepySwatch, hepyLabel(hepyR.hepyColorName(), "body-s"));
        hepyBox.setAlignment(Pos.CENTER_LEFT);
        return hepyBox;
    }

    private Node hepyAvailableCell(HepyInventoryRow hepyR) {
        Label hepyUnits = hepyLabel(String.valueOf(hepyR.hepyAvailable()), "body-s");
        HBox hepyBox = new HBox(8, hepyUnits);
        hepyBox.setAlignment(Pos.CENTER_LEFT);
        if (hepyR.hepyAvailable() == 0) {
            hepyBox.getChildren().add(new PpqhSfBadge("Agotado", PpqhSfBadge.PpqhKind.DANGER).ppqhWithIcon(Feather.SLASH));
        } else if (hepyR.hepyLowStock()) {
            hepyBox.getChildren().add(new PpqhSfBadge("Stock bajo", PpqhSfBadge.PpqhKind.WARNING).ppqhWithIcon(Feather.ALERT_TRIANGLE));
        }
        return hepyBox;
    }

    private Node hepyEditCell(HepyInventoryRow hepyR) {
        TextField hepyStock = new TextField(String.valueOf(hepyR.hepyQuantity()));
        hepyStock.getStyleClass().addAll("sf-input-sm", "code");
        hepyStock.setPrefWidth(80);
        hepyStock.setAccessibleText("Stock de " + hepyR.hepySku());
        TextField hepyThreshold = new TextField(String.valueOf(hepyR.hepyLowStockThreshold()));
        hepyThreshold.getStyleClass().addAll("sf-input-sm", "code");
        hepyThreshold.setPrefWidth(64);
        hepyThreshold.setAccessibleText("Aviso de stock bajo de " + hepyR.hepySku());
        PpqhSfButton hepySave = new PpqhSfButton("Guardar", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
        hepySave.setDisable(true);
        Runnable hepyDirty = () -> hepySave.setDisable(hepyStock.getText().equals(String.valueOf(hepyR.hepyQuantity()))
                && hepyThreshold.getText().equals(String.valueOf(hepyR.hepyLowStockThreshold())));
        hepyStock.textProperty().addListener((hepyObs, hepyOld, hepyV) -> hepyDirty.run());
        hepyThreshold.textProperty().addListener((hepyObs, hepyOld, hepyV) -> hepyDirty.run());
        hepySave.setOnAction(hepyE -> {
            int hepyQ;
            int hepyT;
            try {
                hepyQ = Integer.parseInt(hepyStock.getText().trim());
                hepyT = Integer.parseInt(hepyThreshold.getText().trim());
            } catch (NumberFormatException hepyEx) {
                PpqhToast.ppqhError(hepyTable, "Escribe números enteros en stock y aviso.");
                return;
            }
            PpqhAsync.ppqhRun(() -> hepyContext.ppqhAdminProductService().hepyUpdateStock(hepyR.hepyVariantId(), hepyQ, hepyT),
                    hepyUpdated -> {
                        PpqhToast.ppqhSuccess(hepyTable, "Stock de " + hepyR.hepySku() + " actualizado.");
                        hepyLoad();
                    },
                    hepyEx -> PpqhToast.ppqhError(hepyTable, PpqhErrorMessages.ppqhOf(hepyEx)));
        });
        hepyStock.setOnAction(hepyE -> hepySave.fire());
        hepyThreshold.setOnAction(hepyE -> hepySave.fire());
        HBox hepyBox = new HBox(8, hepyStock, hepyThreshold, hepySave);
        hepyBox.setAlignment(Pos.CENTER_LEFT);
        return hepyBox;
    }

    private static Label hepyLabel(String hepyText, String hepyStyle) {
        Label hepyL = new Label(hepyText);
        hepyL.getStyleClass().addAll(hepyStyle, "ink");
        return hepyL;
    }
}
