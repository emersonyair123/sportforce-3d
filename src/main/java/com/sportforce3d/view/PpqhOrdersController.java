package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhOrderStatusBadge;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.model.PpqhOrderSummary;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
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

public class PpqhOrdersController {

    static final DateTimeFormatter PPQH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Lima"));

    private final PpqhAppContext ppqhContext;

    @FXML private VBox ppqhList;

    public PpqhOrdersController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
    }

    @FXML
    private void initialize() {
        PpqhAsync.ppqhRun(ppqhContext.ppqhOrderService()::ppqhMyOrders, this::ppqhRender,
                ppqhE -> ppqhList.getChildren().setAll(new PpqhEmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar tus pedidos",
                        PpqhErrorMessages.ppqhOf(ppqhE))));
    }

    private void ppqhRender(List<PpqhOrderSummary> ppqhOrders) {
        if (ppqhOrders.isEmpty()) {
            PpqhSfButton ppqhBrowse = new PpqhSfButton("Ver catálogo", PpqhSfButton.PpqhVariant.PRIMARY);
            ppqhBrowse.setOnAction(ppqhE -> ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CATALOG));
            ppqhList.getChildren().setAll(new PpqhEmptyState(Feather.PACKAGE, "Aún no tienes pedidos",
                    "Cuando compres algo, aquí verás su estado.", ppqhBrowse));
            return;
        }
        ppqhList.getChildren().clear();
        for (PpqhOrderSummary ppqhO : ppqhOrders) {
            Label ppqhNumber = new Label(ppqhO.ppqhOrderNumber());
            ppqhNumber.getStyleClass().addAll("code", "ink");
            Label ppqhDate = new Label(PPQH_DATE.format(Instant.parse(ppqhO.ppqhPlacedAt())) + " · "
                    + (ppqhO.ppqhItemCount() == 1 ? "1 producto" : ppqhO.ppqhItemCount() + " productos") + " · " + ppqhO.ppqhPaymentMethod().ppqhLabel());
            ppqhDate.getStyleClass().addAll("body-s", "ink-muted");
            VBox ppqhText = new VBox(4, ppqhNumber, ppqhDate);
            HBox.setHgrow(ppqhText, Priority.ALWAYS);

            Label ppqhTotal = new Label(HepyMoney.hepyFormat(ppqhO.ppqhTotalCents()));
            ppqhTotal.getStyleClass().addAll("price", "ink");
            ppqhTotal.setMinWidth(Region.USE_PREF_SIZE);

            HBox ppqhRow = new HBox(16, ppqhText, PpqhOrderStatusBadge.ppqhOf(ppqhO.ppqhStatus()), ppqhTotal, PpqhIcons.ppqhOf(Feather.CHEVRON_RIGHT, PpqhIcons.PPQH_MD));
            ppqhRow.setAlignment(Pos.CENTER_LEFT);
            ppqhRow.getStyleClass().addAll("sf-card", "sf-row-link");
            ppqhRow.setPadding(new Insets(16, 20, 16, 20));
            ppqhRow.setCursor(Cursor.HAND);
            ppqhRow.setFocusTraversable(true);
            ppqhRow.setAccessibleText("Pedido " + ppqhO.ppqhOrderNumber() + ", " + ppqhO.ppqhStatus().ppqhLabel() + ", " + HepyMoney.hepyFormat(ppqhO.ppqhTotalCents()));
            ppqhRow.setOnMouseClicked(ppqhE -> ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ORDER, ppqhO.ppqhId()));
            ppqhRow.setOnKeyPressed(ppqhE -> {
                if (ppqhE.getCode() == KeyCode.ENTER) {
                    ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ORDER, ppqhO.ppqhId());
                }
            });
            ppqhList.getChildren().add(ppqhRow);
        }
    }
}
