package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.app.PpqhRouter;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhOrderStatusBadge;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfLink;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhStatusTimeline;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhOrderItem;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.PpqhPayment;
import com.sportforce3d.model.PpqhPaymentProof;
import com.sportforce3d.model.PpqhPaymentStatus;
import com.sportforce3d.model.PpqhStatusChange;
import com.sportforce3d.service.PpqhAdminOrderService;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
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

public class PpqhAdminOrderController implements PpqhRouter.PpqhRoutable {

    private final PpqhAppContext ppqhContext;

    @FXML private PpqhSfLink ppqhBackLink;
    @FXML private VBox ppqhContent;

    public PpqhAdminOrderController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
    }

    @FXML
    private void initialize() {
        ppqhBackLink.setGraphic(PpqhIcons.ppqhOf(Feather.ARROW_LEFT, PpqhIcons.PPQH_SM));
        ppqhBackLink.setOnAction(ppqhE -> ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_ORDERS));
    }

    @Override
    public void ppqhOnRoute(Object ppqhParam) {
        if (ppqhParam instanceof Long ppqhId) {
            ppqhLoad(ppqhId);
        } else {
            ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_ORDERS);
        }
    }

    private void ppqhLoad(long ppqhId) {
        PpqhAsync.ppqhRun(() -> ppqhContext.ppqhAdminOrderService().ppqhOrder(ppqhId), this::ppqhRender,
                ppqhE -> ppqhContent.getChildren().setAll(new PpqhEmptyState(Feather.ALERT_TRIANGLE, "No se pudo abrir el pedido",
                        PpqhErrorMessages.ppqhOf(ppqhE))));
    }

    private void ppqhRender(PpqhOrderDetail ppqhO) {
        Label ppqhNumber = new Label(ppqhO.ppqhOrderNumber());
        ppqhNumber.getStyleClass().addAll("display-l", "ink");
        Label ppqhDate = new Label("Realizado el " + PpqhOrdersController.PPQH_DATE.format(Instant.parse(ppqhO.ppqhPlacedAt()))
                + " · " + ppqhO.ppqhCustomerName() + " · " + ppqhO.ppqhCustomerEmail());
        ppqhDate.getStyleClass().addAll("body-s", "ink-muted");
        HBox ppqhTitleRow = new HBox(16, ppqhNumber, PpqhOrderStatusBadge.ppqhOf(ppqhO.ppqhStatus()));
        ppqhTitleRow.setAlignment(Pos.CENTER_LEFT);
        VBox ppqhHeader = new VBox(4, ppqhTitleRow, ppqhDate);

        PpqhStatusTimeline ppqhTimeline = new PpqhStatusTimeline();
        if (ppqhO.ppqhStatus() == PpqhOrderStatus.CANCELADO) {
            ppqhTimeline.setCancelled(ppqhO.ppqhLastStatusBeforeCancel());
        } else {
            ppqhTimeline.setStatus(ppqhO.ppqhStatus());
        }

        VBox ppqhLeft = new VBox(24, ppqhCard("Productos", ppqhItemsBox(ppqhO)), ppqhCard("Pagos", ppqhPaymentsBox(ppqhO)));
        ppqhLeft.setPrefWidth(660);
        ppqhLeft.setMinWidth(520);
        HBox.setHgrow(ppqhLeft, Priority.ALWAYS);
        VBox ppqhRight = new VBox(24, ppqhCard("Estado", ppqhStatusBox(ppqhO)), ppqhCard("Envío", ppqhShippingBox(ppqhO)), ppqhCard("Historial", ppqhHistoryBox(ppqhO)));
        ppqhRight.setPrefWidth(400);
        ppqhRight.setMinWidth(340);
        FlowPane ppqhColumns = new FlowPane(24, 24, ppqhLeft, ppqhRight);
        ppqhContent.getChildren().setAll(ppqhHeader, ppqhCard("Recorrido", ppqhTimeline), ppqhColumns);
    }

    private Node ppqhItemsBox(PpqhOrderDetail ppqhO) {
        VBox ppqhItems = new VBox(12);
        for (PpqhOrderItem ppqhI : ppqhO.ppqhItems()) {
            Node ppqhMarker;
            ImageView ppqhView = ppqhI.ppqhHasDesign() ? HepyProductImages.hepyDesignCapture(ppqhContext.ppqhStorage(), ppqhI.ppqhDesignFrontPath(), 72, 96) : null;
            if (ppqhView != null) {
                StackPane ppqhThumb = new StackPane(ppqhView);
                ppqhThumb.getStyleClass().add("sf-thumb");
                ppqhThumb.setMinSize(72, 96);
                ppqhThumb.setMaxSize(72, 96);
                ppqhMarker = ppqhThumb;
            } else {
                Rectangle ppqhC = new Rectangle(14, 14, ppqhColor(ppqhI.ppqhColorHex()));
                ppqhC.setArcWidth(4);
                ppqhC.setArcHeight(4);
                ppqhMarker = ppqhC;
            }
            Label ppqhName = new Label(ppqhI.ppqhQuantity() + " × " + ppqhI.ppqhProductName());
            ppqhName.getStyleClass().addAll("body", "ink", "sf-strong");
            ppqhName.setWrapText(true);
            Label ppqhMeta = new Label("Talla " + ppqhI.ppqhSizeCode() + " · " + ppqhI.ppqhSku() + " · " + HepyMoney.hepyFormat(ppqhI.ppqhUnitPriceCents())
                    + " c/u" + (ppqhI.ppqhSurchargeCents() == 0 ? "" : " + " + HepyMoney.hepyFormat(ppqhI.ppqhSurchargeCents()) + " materiales"));
            ppqhMeta.getStyleClass().addAll("body-s", "ink-muted");
            ppqhMeta.setWrapText(true);
            VBox ppqhText = new VBox(4, ppqhName, ppqhMeta);
            if (ppqhI.ppqhHasDesign()) {
                Label ppqhDesign = new Label("Uniforme personalizado: " + ppqhI.ppqhDesignName() + " (versión " + ppqhI.ppqhDesignVersion() + ")");
                ppqhDesign.getStyleClass().addAll("body-s", "brand-text");
                PpqhSfButton ppqhOpen3d = new PpqhSfButton("Ver uniforme 3D y ficha técnica", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM)
                        .ppqhWithIcon(Feather.BOX);
                ppqhOpen3d.setOnAction(ppqhE -> ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_DESIGN, ppqhI.ppqhId()));
                ppqhText.getChildren().addAll(ppqhDesign, ppqhOpen3d);
            }
            HBox.setHgrow(ppqhText, Priority.ALWAYS);
            ppqhText.setMinWidth(0);
            Label ppqhTotal = new Label(HepyMoney.hepyFormat(ppqhI.ppqhLineTotalCents()));
            ppqhTotal.getStyleClass().addAll("body", "ink");
            ppqhTotal.setMinWidth(Region.USE_PREF_SIZE);
            HBox ppqhRow = new HBox(14, ppqhMarker, ppqhText, ppqhTotal);
            ppqhRow.setAlignment(Pos.TOP_LEFT);
            ppqhItems.getChildren().add(ppqhRow);
        }
        Region ppqhDivider = new Region();
        ppqhDivider.getStyleClass().add("sf-divider");
        Label ppqhTotalLabel = new Label("Total");
        ppqhTotalLabel.getStyleClass().addAll("title-s", "ink");
        Label ppqhTotal = new Label(HepyMoney.hepyFormat(ppqhO.ppqhTotalCents()));
        ppqhTotal.getStyleClass().addAll("price", "ink");
        Region ppqhSpacer = new Region();
        HBox.setHgrow(ppqhSpacer, Priority.ALWAYS);
        HBox ppqhTotalRow = new HBox(ppqhTotalLabel, ppqhSpacer, ppqhTotal);
        ppqhTotalRow.setAlignment(Pos.BASELINE_LEFT);
        return new VBox(12, ppqhItems, ppqhDivider,
                HepyCartController.hepyRow("Subtotal", HepyMoney.hepyFormat(ppqhO.ppqhSubtotalCents())),
                HepyCartController.hepyRow("Recargos por materiales", HepyMoney.hepyFormat(ppqhO.ppqhSurchargeCents())),
                HepyCartController.hepyRow("Envío", ppqhO.ppqhShippingCents() == 0 ? "Gratis" : HepyMoney.hepyFormat(ppqhO.ppqhShippingCents())),
                ppqhTotalRow);
    }

    private Node ppqhPaymentsBox(PpqhOrderDetail ppqhO) {
        VBox ppqhBox = new VBox(16);
        Label ppqhMethod = new Label(ppqhO.ppqhPaymentMethod().ppqhLabel() + " · verificación manual");
        ppqhMethod.getStyleClass().addAll("body-s", "ink-muted");
        ppqhBox.getChildren().add(ppqhMethod);
        List<PpqhPayment> ppqhPayments = ppqhO.ppqhPayments();
        for (int ppqhIdx = ppqhPayments.size() - 1; ppqhIdx >= 0; ppqhIdx--) {
            ppqhBox.getChildren().add(ppqhPaymentRow(ppqhPayments.get(ppqhIdx)));
        }
        return ppqhBox;
    }

    private Node ppqhPaymentRow(PpqhPayment ppqhP) {
        HBox ppqhTop = new HBox(10, PpqhPaymentActions.ppqhBadge(ppqhP.ppqhStatus()), ppqhLabel(HepyMoney.hepyFormat(ppqhP.ppqhAmountCents()), "body", "ink"));
        ppqhTop.setAlignment(Pos.CENTER_LEFT);
        VBox ppqhInfo = new VBox(6, ppqhTop);
        if (ppqhP.ppqhOperationCode() != null) {
            Label ppqhCode = new Label("Operación " + ppqhP.ppqhOperationCode());
            ppqhCode.getStyleClass().addAll("code", "ink");
            ppqhInfo.getChildren().add(ppqhCode);
        }
        switch (ppqhP.ppqhStatus()) {
            case PENDIENTE -> ppqhInfo.getChildren().add(ppqhLabel("El cliente aún no registra este pago.", "body-s", "ink-muted"));
            case EN_VERIFICACION -> ppqhInfo.getChildren().add(ppqhLabel("Registrado el "
                    + PpqhOrdersController.PPQH_DATE.format(Instant.parse(ppqhP.ppqhUpdatedAt())), "body-s", "ink-muted"));
            case APROBADO -> ppqhInfo.getChildren().add(ppqhLabel("Aprobado por " + ppqhP.ppqhVerifiedBy() + " el "
                    + PpqhOrdersController.PPQH_DATE.format(Instant.parse(ppqhP.ppqhVerifiedAt())), "body-s", "ink-muted"));
            case RECHAZADO -> ppqhInfo.getChildren().add(ppqhLabel("Rechazado"
                    + (ppqhP.ppqhVerifiedBy() == null ? "" : " por " + ppqhP.ppqhVerifiedBy()) + ": " + ppqhP.ppqhRejectionReason(),
                    "body-s", "danger-text"));
        }
        if (ppqhP.ppqhStatus() == PpqhPaymentStatus.EN_VERIFICACION) {
            PpqhSfButton ppqhApprove = new PpqhSfButton("Aprobar", PpqhSfButton.PpqhVariant.PRIMARY, PpqhSfButton.PpqhSize.SM).ppqhWithIcon(Feather.CHECK);
            ppqhApprove.setOnAction(ppqhE -> PpqhPaymentActions.ppqhApprove(ppqhContext, ppqhContent, ppqhP, this::ppqhRender));
            PpqhSfButton ppqhReject = new PpqhSfButton("Rechazar", PpqhSfButton.PpqhVariant.DANGER, PpqhSfButton.PpqhSize.SM).ppqhWithIcon(Feather.X);
            ppqhReject.setOnAction(ppqhE -> PpqhPaymentActions.ppqhReject(ppqhContext, ppqhContent, ppqhP, this::ppqhRender));
            ppqhInfo.getChildren().add(new HBox(8, ppqhApprove, ppqhReject));
        }
        HBox.setHgrow(ppqhInfo, Priority.ALWAYS);
        HBox ppqhRow = new HBox(14);
        for (PpqhPaymentProof ppqhProof : ppqhP.ppqhProofs()) {
            ppqhRow.getChildren().add(PpqhPaymentActions.ppqhProofThumb(ppqhContext, ppqhProof, 72, 96));
        }
        ppqhRow.getChildren().add(ppqhInfo);
        ppqhRow.getStyleClass().add("sf-payment-row");
        return ppqhRow;
    }

    private Node ppqhStatusBox(PpqhOrderDetail ppqhO) {
        List<PpqhOrderStatus> ppqhNext = PpqhAdminOrderService.ppqhNextStates(ppqhO.ppqhStatus());
        VBox ppqhBox = new VBox(12);
        Label ppqhCurrent = ppqhLabel("Estado actual: " + ppqhO.ppqhStatus().ppqhLabel(), "body-s", "ink");
        ppqhBox.getChildren().add(ppqhCurrent);
        if (ppqhO.ppqhStatus() == PpqhOrderStatus.PAGO_EN_VERIFICACION) {
            ppqhBox.getChildren().add(ppqhLabel("Para pasar a Pagado, aprueba el pago en la sección Pagos de este pedido.",
                    "body-s", "ink-muted"));
        } else if (ppqhO.ppqhStatus() == PpqhOrderStatus.PENDIENTE_PAGO) {
            ppqhBox.getChildren().add(ppqhLabel("Esperando que el cliente registre su pago.", "body-s", "ink-muted"));
        }
        if (ppqhNext.isEmpty()) {
            ppqhBox.getChildren().add(ppqhLabel("Este pedido ya no cambia de estado.", "body-s", "ink-muted"));
            return ppqhBox;
        }
        ComboBox<PpqhOrderStatus> ppqhCombo = new ComboBox<>();
        ppqhCombo.getItems().setAll(ppqhNext);
        ppqhCombo.setValue(ppqhNext.get(0));
        ppqhCombo.setMaxWidth(Double.MAX_VALUE);
        ppqhCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(PpqhOrderStatus ppqhS) {
                return ppqhS == null ? "" : ppqhS.ppqhLabel();
            }

            @Override
            public PpqhOrderStatus fromString(String ppqhS) {
                return null;
            }
        });
        PpqhSfTextField ppqhNote = new PpqhSfTextField("Nota para el historial");
        ppqhNote.setPromptText("Ej. Enviado por Olva Courier, guía 123456");
        PpqhSfButton ppqhApply = new PpqhSfButton("Cambiar estado", PpqhSfButton.PpqhVariant.PRIMARY, PpqhSfButton.PpqhSize.SM).ppqhWithIcon(Feather.ARROW_RIGHT);
        ppqhApply.setOnAction(ppqhE -> {
            PpqhOrderStatus ppqhTarget = ppqhCombo.getValue();
            Runnable ppqhRun = () -> {
                ppqhApply.setDisable(true);
                PpqhAsync.ppqhRun(() -> ppqhContext.ppqhAdminOrderService().ppqhChangeStatus(ppqhO.ppqhId(), ppqhTarget, ppqhNote.getText()), ppqhUpdated -> {
                    ppqhRender(ppqhUpdated);
                    PpqhToast.ppqhSuccess(ppqhContent, ppqhO.ppqhOrderNumber() + ": " + ppqhTarget.ppqhLabel() + ".");
                }, ppqhEx -> {
                    ppqhApply.setDisable(false);
                    PpqhToast.ppqhError(ppqhContent, PpqhErrorMessages.ppqhOf(ppqhEx));
                });
            };
            if (ppqhTarget == PpqhOrderStatus.CANCELADO) {
                com.sportforce3d.component.PpqhConfirmDialog.ppqhShow(ppqhContent, "¿Cancelar " + ppqhO.ppqhOrderNumber() + "?",
                        "Se libera el stock reservado y el cliente verá la nota como motivo.", "Cancelar pedido", true, ppqhRun);
            } else {
                ppqhRun.run();
            }
        });
        HBox.setHgrow(ppqhCombo, Priority.ALWAYS);
        ppqhBox.getChildren().addAll(ppqhLabeledBox("NUEVO ESTADO", ppqhCombo), ppqhNote, ppqhApply);
        return ppqhBox;
    }

    private Node ppqhShippingBox(PpqhOrderDetail ppqhO) {
        Label ppqhShip = new Label(ppqhO.ppqhShipRecipient() + " · " + ppqhO.ppqhShipPhone() + "\n" + ppqhO.ppqhShippingOneLine()
                + (ppqhO.ppqhShipReference() == null ? "" : "\nRef.: " + ppqhO.ppqhShipReference())
                + (ppqhO.ppqhNotes() == null ? "" : "\n\nNotas del cliente: " + ppqhO.ppqhNotes()));
        ppqhShip.getStyleClass().addAll("body-s", "ink");
        ppqhShip.setWrapText(true);
        return ppqhShip;
    }

    private Node ppqhHistoryBox(PpqhOrderDetail ppqhO) {
        VBox ppqhRows = new VBox(10);
        for (int ppqhI = ppqhO.ppqhHistory().size() - 1; ppqhI >= 0; ppqhI--) {
            PpqhStatusChange ppqhC = ppqhO.ppqhHistory().get(ppqhI);
            Label ppqhWhen = ppqhLabel(PpqhOrdersController.PPQH_DATE.format(Instant.parse(ppqhC.ppqhCreatedAt()))
                    + (ppqhC.ppqhChangedBy() == null ? "" : " · " + ppqhC.ppqhChangedBy()), "body-s", "ink-muted");
            Label ppqhWhat = ppqhLabel(ppqhC.ppqhTo().ppqhLabel() + (ppqhC.ppqhNote() == null ? "" : " — " + ppqhC.ppqhNote()), "body-s", "ink");
            ppqhRows.getChildren().add(new VBox(2, ppqhWhat, ppqhWhen));
        }
        return ppqhRows;
    }

    private static Label ppqhLabel(String ppqhText, String... ppqhStyles) {
        Label ppqhL = new Label(ppqhText);
        ppqhL.getStyleClass().addAll(ppqhStyles);
        ppqhL.setWrapText(true);
        return ppqhL;
    }

    private static VBox ppqhLabeledBox(String ppqhTitle, Node ppqhNode) {
        Label ppqhL = new Label(ppqhTitle);
        ppqhL.getStyleClass().addAll("label-text", "ink-muted");
        return new VBox(6, ppqhL, ppqhNode);
    }

    private static VBox ppqhCard(String ppqhTitle, Node ppqhBody) {
        Label ppqhLabel = new Label(ppqhTitle);
        ppqhLabel.getStyleClass().addAll("title-s", "ink");
        VBox ppqhCard = new VBox(14, ppqhLabel, ppqhBody);
        ppqhCard.getStyleClass().add("sf-card");
        ppqhCard.setPadding(new Insets(20));
        return ppqhCard;
    }

    private static Color ppqhColor(String ppqhHex) {
        try {
            return Color.web(ppqhHex);
        } catch (RuntimeException ppqhE) {
            return Color.GRAY;
        }
    }
}
