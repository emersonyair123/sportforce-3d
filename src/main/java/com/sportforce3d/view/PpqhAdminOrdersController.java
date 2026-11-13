package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.app.PpqhRouter;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.HepyFilterChip;
import com.sportforce3d.component.PpqhOrderStatusBadge;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.PpqhOrderSummary;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
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

public class PpqhAdminOrdersController implements PpqhRouter.PpqhRoutable {

    private final PpqhAppContext ppqhContext;
    private final ToggleGroup ppqhChips = new ToggleGroup();
    private PpqhOrderStatus ppqhStatus;

    @FXML private PpqhSfTextField ppqhFilterField;
    @FXML private Label ppqhCountLabel;
    @FXML private FlowPane ppqhStatusChips;
    @FXML private TableView<PpqhOrderSummary> ppqhTable;

    public PpqhAdminOrdersController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
    }

    @FXML
    private void initialize() {
        PauseTransition ppqhDebounce = new PauseTransition(Duration.millis(250));
        ppqhDebounce.setOnFinished(ppqhE -> ppqhLoad());
        ppqhFilterField.textProperty().addListener((ppqhObs, ppqhOld, ppqhV) -> ppqhDebounce.playFromStart());

        ppqhTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ppqhTable.setPlaceholder(new PpqhEmptyState(Feather.CLIPBOARD, "Sin pedidos", "No hay pedidos con estos filtros."));
        ppqhTable.getColumns().setAll(List.of(
                ppqhColumn("PEDIDO", 190, this::ppqhNumberCell),
                ppqhColumn("CLIENTE", 200, ppqhO -> ppqhText(ppqhO.ppqhCustomerName(), "body-s")),
                ppqhColumn("ESTADO", 190, ppqhO -> PpqhOrderStatusBadge.ppqhOf(ppqhO.ppqhStatus())),
                ppqhColumn("PAGO", 150, ppqhO -> ppqhText(ppqhO.ppqhPaymentMethod().ppqhLabel(), "body-s")),
                ppqhColumn("ÍTEMS", 70, ppqhO -> ppqhText(String.valueOf(ppqhO.ppqhItemCount()), "body-s")),
                ppqhColumn("TOTAL", 110, ppqhO -> ppqhText(HepyMoney.hepyFormat(ppqhO.ppqhTotalCents()), "sf-strong")),
                ppqhColumn("", 110, this::ppqhOpenCell)));
        ppqhTable.setFixedCellSize(60);
        ppqhTable.setRowFactory(ppqhT -> {
            TableRow<PpqhOrderSummary> ppqhRow = new TableRow<>();
            ppqhRow.setOnMouseClicked(ppqhE -> {
                if (ppqhE.getClickCount() == 2 && ppqhRow.getItem() != null) {
                    ppqhOpen(ppqhRow.getItem());
                }
            });
            return ppqhRow;
        });
    }

    @Override
    public void ppqhOnRoute(Object ppqhParam) {
        ppqhStatus = ppqhParam instanceof PpqhOrderStatus ppqhS ? ppqhS : null;
        ppqhLoadCounts();
        ppqhLoad();
    }

    private void ppqhLoadCounts() {
        PpqhAsync.ppqhRun(() -> ppqhContext.ppqhAdminOrderService().ppqhCounts(), this::ppqhRenderChips, ppqhE -> { });
    }

    private void ppqhRenderChips(Map<PpqhOrderStatus, Integer> ppqhCounts) {
        ppqhStatusChips.getChildren().clear();
        ppqhChips.getToggles().clear();
        int ppqhTotal = ppqhCounts.values().stream().mapToInt(Integer::intValue).sum();
        ppqhStatusChips.getChildren().add(ppqhChip("Todos (" + ppqhTotal + ")", null));
        for (PpqhOrderStatus ppqhS : PpqhOrderStatus.values()) {
            ppqhStatusChips.getChildren().add(ppqhChip(ppqhS.ppqhLabel() + " (" + ppqhCounts.getOrDefault(ppqhS, 0) + ")", ppqhS));
        }
    }

    private HepyFilterChip ppqhChip(String ppqhLabel, PpqhOrderStatus ppqhValue) {
        HepyFilterChip ppqhChip = new HepyFilterChip(ppqhLabel);
        ppqhChip.setToggleGroup(ppqhChips);
        ppqhChip.setSelected(ppqhValue == ppqhStatus);
        ppqhChip.setOnAction(ppqhE -> {
            ppqhChip.setSelected(true);
            ppqhStatus = ppqhValue;
            ppqhLoad();
        });
        return ppqhChip;
    }

    private void ppqhLoad() {
        PpqhOrderStatus ppqhS = ppqhStatus;
        String ppqhText = ppqhFilterField.getText();
        PpqhAsync.ppqhRun(() -> ppqhContext.ppqhAdminOrderService().ppqhList(ppqhS, ppqhText), ppqhRows -> {
            ppqhTable.getItems().setAll(ppqhRows);
            ppqhCountLabel.setText(ppqhRows.size() == 1 ? "1 pedido" : ppqhRows.size() + " pedidos");
        }, ppqhE -> ppqhTable.setPlaceholder(new PpqhEmptyState(Feather.LOCK, "No se pudo cargar la lista", PpqhErrorMessages.ppqhOf(ppqhE))));
    }

    private void ppqhOpen(PpqhOrderSummary ppqhO) {
        ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_ORDER, ppqhO.ppqhId());
    }

    private Node ppqhNumberCell(PpqhOrderSummary ppqhO) {
        Label ppqhNumber = ppqhText(ppqhO.ppqhOrderNumber(), "code");
        Label ppqhDate = ppqhText(PpqhOrdersController.PPQH_DATE.format(Instant.parse(ppqhO.ppqhPlacedAt())), "body-s");
        ppqhDate.getStyleClass().add("ink-muted");
        VBox ppqhBox = new VBox(2, ppqhNumber, ppqhDate);
        ppqhBox.setAlignment(Pos.CENTER_LEFT);
        return ppqhBox;
    }

    private Node ppqhOpenCell(PpqhOrderSummary ppqhO) {
        PpqhSfButton ppqhOpen = new PpqhSfButton("Abrir", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
        ppqhOpen.setOnAction(ppqhE -> ppqhOpen(ppqhO));
        return ppqhOpen;
    }

    private static Label ppqhText(String ppqhValue, String ppqhStyle) {
        Label ppqhL = new Label(ppqhValue);
        ppqhL.getStyleClass().addAll(ppqhStyle, "ink");
        return ppqhL;
    }

    private static TableColumn<PpqhOrderSummary, PpqhOrderSummary> ppqhColumn(String ppqhTitle, double ppqhWidth,
                                                                  Function<PpqhOrderSummary, Node> ppqhRenderer) {
        TableColumn<PpqhOrderSummary, PpqhOrderSummary> ppqhCol = new TableColumn<>(ppqhTitle);
        ppqhCol.setPrefWidth(ppqhWidth);
        ppqhCol.setMinWidth(ppqhWidth);
        ppqhCol.setSortable(false);
        ppqhCol.setReorderable(false);
        ppqhCol.setCellValueFactory(ppqhData -> new ReadOnlyObjectWrapper<>(ppqhData.getValue()));
        ppqhCol.setCellFactory(ppqhC -> new TableCell<>() {
            @Override
            protected void updateItem(PpqhOrderSummary ppqhRow, boolean ppqhEmpty) {
                super.updateItem(ppqhRow, ppqhEmpty);
                setText(null);
                setGraphic(ppqhEmpty || ppqhRow == null ? null : ppqhRenderer.apply(ppqhRow));
            }
        });
        return ppqhCol;
    }
}
