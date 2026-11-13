package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.HepyFilterChip;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfLink;
import com.sportforce3d.model.PpqhPayment;
import com.sportforce3d.model.PpqhPaymentProof;
import com.sportforce3d.model.PpqhPaymentStatus;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.time.Instant;
import java.util.List;

public class PpqhAdminPaymentsController {

    private final PpqhAppContext ppqhContext;
    private final ToggleGroup ppqhGroup = new ToggleGroup();
    private PpqhPaymentStatus ppqhStatus = PpqhPaymentStatus.EN_VERIFICACION;

    @FXML private FlowPane ppqhChips;
    @FXML private VBox ppqhList;

    public PpqhAdminPaymentsController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
    }

    @FXML
    private void initialize() {
        for (PpqhPaymentStatus ppqhS : List.of(PpqhPaymentStatus.EN_VERIFICACION, PpqhPaymentStatus.APROBADO, PpqhPaymentStatus.RECHAZADO)) {
            HepyFilterChip ppqhChip = new HepyFilterChip(ppqhS == PpqhPaymentStatus.EN_VERIFICACION ? "Por verificar" : ppqhS.ppqhLabel() + "s");
            ppqhChip.setToggleGroup(ppqhGroup);
            ppqhChip.setSelected(ppqhS == ppqhStatus);
            ppqhChip.setOnAction(ppqhE -> {
                ppqhChip.setSelected(true);
                ppqhStatus = ppqhS;
                ppqhLoad();
            });
            ppqhChips.getChildren().add(ppqhChip);
        }
        ppqhLoad();
    }

    private void ppqhLoad() {
        PpqhPaymentStatus ppqhS = ppqhStatus;
        PpqhAsync.ppqhRun(() -> ppqhContext.ppqhPaymentService().ppqhQueue(ppqhS), this::ppqhRender,
                ppqhE -> ppqhList.getChildren().setAll(new PpqhEmptyState(Feather.LOCK, "No se pudo cargar los pagos", PpqhErrorMessages.ppqhOf(ppqhE))));
    }

    private void ppqhRender(List<PpqhPayment> ppqhPayments) {
        if (ppqhPayments.isEmpty()) {
            ppqhList.getChildren().setAll(ppqhStatus == PpqhPaymentStatus.EN_VERIFICACION
                    ? new PpqhEmptyState(Feather.CHECK_CIRCLE, "No hay pagos por verificar",
                    "Cuando un cliente registre su código de operación, aparecerá aquí.")
                    : new PpqhEmptyState(Feather.CREDIT_CARD, "Sin pagos", "No hay pagos en este estado."));
            return;
        }
        ppqhList.getChildren().clear();
        for (PpqhPayment ppqhP : ppqhPayments) {
            ppqhList.getChildren().add(ppqhCard(ppqhP));
        }
    }

    private Node ppqhCard(PpqhPayment ppqhP) {
        HBox ppqhRow = new HBox(20);
        ppqhRow.getStyleClass().add("sf-card");
        ppqhRow.setPadding(new Insets(16));

        VBox ppqhProofs = new VBox(8);
        for (PpqhPaymentProof ppqhProof : ppqhP.ppqhProofs()) {
            ppqhProofs.getChildren().add(PpqhPaymentActions.ppqhProofThumb(ppqhContext, ppqhProof, 120, 160));
        }
        if (ppqhP.ppqhProofs().isEmpty()) {
            ppqhProofs.getChildren().add(ppqhLabel("Sin comprobante", "body-s", "ink-subtle"));
        }

        PpqhSfLink ppqhNumber = new PpqhSfLink(ppqhP.ppqhOrderNumber());
        ppqhNumber.setOnAction(ppqhE -> ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_ORDER, ppqhP.ppqhOrderId()));
        HBox ppqhHead = new HBox(12, ppqhNumber, PpqhPaymentActions.ppqhBadge(ppqhP.ppqhStatus()));
        ppqhHead.setAlignment(Pos.CENTER_LEFT);
        Label ppqhAmount = ppqhLabel(HepyMoney.hepyFormat(ppqhP.ppqhAmountCents()), "price", "ink");
        Label ppqhWho = ppqhLabel(ppqhP.ppqhCustomerName() + " · " + ppqhP.ppqhMethod().ppqhLabel(), "body-s", "ink");
        Label ppqhCode = ppqhLabel("Operación " + (ppqhP.ppqhOperationCode() == null ? "—" : ppqhP.ppqhOperationCode()), "code", "ink");
        Label ppqhWhen = ppqhLabel("Registrado el " + PpqhOrdersController.PPQH_DATE.format(Instant.parse(ppqhP.ppqhUpdatedAt())), "body-s", "ink-muted");
        VBox ppqhInfo = new VBox(6, ppqhHead, ppqhAmount, ppqhWho, ppqhCode, ppqhWhen);
        if (ppqhP.ppqhAmountCents() != ppqhP.ppqhOrderTotalCents()) {
            ppqhInfo.getChildren().add(ppqhLabel("El pedido suma " + HepyMoney.hepyFormat(ppqhP.ppqhOrderTotalCents()) + ".", "body-s", "warning-text"));
        }
        if (ppqhP.ppqhStatus() == PpqhPaymentStatus.APROBADO) {
            ppqhInfo.getChildren().add(ppqhLabel("Aprobado por " + ppqhP.ppqhVerifiedBy() + " el "
                    + PpqhOrdersController.PPQH_DATE.format(Instant.parse(ppqhP.ppqhVerifiedAt())), "body-s", "ink-muted"));
        } else if (ppqhP.ppqhStatus() == PpqhPaymentStatus.RECHAZADO) {
            ppqhInfo.getChildren().add(ppqhLabel("Motivo: " + ppqhP.ppqhRejectionReason(), "body-s", "danger-text"));
        }
        HBox.setHgrow(ppqhInfo, Priority.ALWAYS);
        ppqhInfo.setMinWidth(0);

        VBox ppqhActions = new VBox(8);
        ppqhActions.setAlignment(Pos.TOP_RIGHT);
        if (ppqhP.ppqhStatus() == PpqhPaymentStatus.EN_VERIFICACION) {
            PpqhSfButton ppqhApprove = new PpqhSfButton("Aprobar", PpqhSfButton.PpqhVariant.PRIMARY, PpqhSfButton.PpqhSize.SM).ppqhWithIcon(Feather.CHECK);
            ppqhApprove.setOnAction(ppqhE -> PpqhPaymentActions.ppqhApprove(ppqhContext, ppqhList, ppqhP, ppqhO -> ppqhLoad()));
            PpqhSfButton ppqhReject = new PpqhSfButton("Rechazar", PpqhSfButton.PpqhVariant.DANGER, PpqhSfButton.PpqhSize.SM).ppqhWithIcon(Feather.X);
            ppqhReject.setOnAction(ppqhE -> PpqhPaymentActions.ppqhReject(ppqhContext, ppqhList, ppqhP, ppqhO -> ppqhLoad()));
            ppqhApprove.setMaxWidth(Double.MAX_VALUE);
            ppqhReject.setMaxWidth(Double.MAX_VALUE);
            ppqhActions.getChildren().addAll(ppqhApprove, ppqhReject);
        }
        if (!ppqhP.ppqhProofs().isEmpty()) {
            PpqhSfButton ppqhDownload = new PpqhSfButton("Descargar comprobante", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM)
                    .ppqhWithIcon(Feather.DOWNLOAD);
            ppqhDownload.setOnAction(ppqhE -> PpqhPaymentActions.ppqhDownload(ppqhContext, ppqhList, ppqhP.ppqhProofs().get(0)));
            ppqhActions.getChildren().add(ppqhDownload);
        }
        ppqhActions.setMinWidth(190);
        ppqhRow.getChildren().addAll(ppqhProofs, ppqhInfo, ppqhActions);
        return ppqhRow;
    }

    private static Label ppqhLabel(String ppqhText, String... ppqhStyles) {
        Label ppqhL = new Label(ppqhText);
        ppqhL.getStyleClass().addAll(ppqhStyles);
        ppqhL.setWrapText(true);
        return ppqhL;
    }
}
