package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.app.PpqhRouter;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhConfirmDialog;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhOrderStatusBadge;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfLink;
import com.sportforce3d.component.PpqhStatusTimeline;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhPayment;
import com.sportforce3d.model.PpqhPaymentInstructions;
import com.sportforce3d.model.PpqhPaymentStatus;
import com.sportforce3d.model.PpqhOrderItem;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.PpqhStatusChange;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.time.Instant;
import java.util.Set;

public class PpqhOrderController implements PpqhRouter.PpqhRoutable {

    private static final Set<PpqhOrderStatus> PPQH_CANCELLABLE = Set.of(PpqhOrderStatus.PENDIENTE_PAGO, PpqhOrderStatus.PAGO_EN_VERIFICACION);

    private final PpqhAppContext ppqhContext;

    @FXML private PpqhSfLink ppqhBackLink;
    @FXML private VBox ppqhContent;
    private PpqhPaymentInstructions ppqhInstructions;

    public PpqhOrderController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
    }

    @FXML
    private void initialize() {
        ppqhBackLink.setGraphic(PpqhIcons.ppqhOf(Feather.ARROW_LEFT, PpqhIcons.PPQH_SM));
        ppqhBackLink.setOnAction(ppqhE -> ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ORDERS));
    }

    @Override
    public void ppqhOnRoute(Object ppqhParam) {
        if (ppqhParam instanceof Long ppqhId) {
            ppqhLoad(ppqhId);
        } else {
            ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ORDERS);
        }
    }

    private void ppqhLoad(long ppqhId) {
        PpqhAsync.ppqhRun(() -> {
                    ppqhInstructions = ppqhContext.ppqhPaymentService().ppqhInstructions();
                    return ppqhContext.ppqhOrderService().ppqhOrder(ppqhId);
                }, this::ppqhRender,
                ppqhE -> ppqhContent.getChildren().setAll(new PpqhEmptyState(Feather.ALERT_TRIANGLE, "No se pudo abrir el pedido",
                        PpqhErrorMessages.ppqhOf(ppqhE))));
    }

    private void ppqhRender(PpqhOrderDetail ppqhO) {
        Label ppqhNumber = new Label(ppqhO.ppqhOrderNumber());
        ppqhNumber.getStyleClass().addAll("display-l", "ink");
        Label ppqhDate = new Label("Realizado el " + PpqhOrdersController.PPQH_DATE.format(Instant.parse(ppqhO.ppqhPlacedAt())));
        ppqhDate.getStyleClass().addAll("body-s", "ink-muted");
        HBox ppqhHeader = new HBox(16, new VBox(4, ppqhNumber, ppqhDate), PpqhOrderStatusBadge.ppqhOf(ppqhO.ppqhStatus()));
        ppqhHeader.setAlignment(Pos.CENTER_LEFT);

        PpqhStatusTimeline ppqhTimeline = new PpqhStatusTimeline();
        if (ppqhO.ppqhStatus() == PpqhOrderStatus.CANCELADO) {
            ppqhTimeline.setCancelled(ppqhO.ppqhLastStatusBeforeCancel());
        } else {
            ppqhTimeline.setStatus(ppqhO.ppqhStatus());
        }
        VBox ppqhTimelineCard = ppqhCard("Estado del pedido", ppqhTimeline);

        VBox ppqhItems = new VBox(10);
        for (PpqhOrderItem ppqhI : ppqhO.ppqhItems()) {
            Node ppqhMarker = ppqhItemMarker(ppqhI);
            Label ppqhName = new Label(ppqhI.ppqhQuantity() + " × " + ppqhI.ppqhProductName());
            ppqhName.getStyleClass().addAll("body", "ink");
            ppqhName.setWrapText(true);
            Label ppqhMeta = new Label("Talla " + ppqhI.ppqhSizeCode() + " · " + ppqhI.ppqhSku() + " · " + HepyMoney.hepyFormat(ppqhI.ppqhUnitPriceCents()) + " c/u");
            ppqhMeta.getStyleClass().addAll("body-s", "ink-muted");
            VBox ppqhText = new VBox(2, ppqhName, ppqhMeta);
            if (ppqhI.ppqhHasDesign()) {
                Label ppqhDesign = new Label("Diseño: " + ppqhI.ppqhDesignName() + " (versión " + ppqhI.ppqhDesignVersion() + ")"
                        + (ppqhI.ppqhSurchargeCents() == 0 ? "" : " · materiales " + HepyMoney.hepySurcharge(ppqhI.ppqhSurchargeCents()) + " c/u"));
                ppqhDesign.getStyleClass().addAll("body-s", "brand-text");
                ppqhDesign.setWrapText(true);
                ppqhText.getChildren().add(ppqhDesign);
            }
            HBox.setHgrow(ppqhText, Priority.ALWAYS);
            Label ppqhTotal = new Label(HepyMoney.hepyFormat(ppqhI.ppqhLineTotalCents()));
            ppqhTotal.getStyleClass().addAll("body", "ink");
            ppqhTotal.setMinWidth(Region.USE_PREF_SIZE);
            HBox ppqhRow = new HBox(12, ppqhMarker, ppqhText, ppqhTotal);
            ppqhRow.setAlignment(Pos.CENTER_LEFT);
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
        VBox ppqhItemsCard = ppqhCard("Productos", new VBox(12, ppqhItems, ppqhDivider,
                HepyCartController.hepyRow("Subtotal", HepyMoney.hepyFormat(ppqhO.ppqhSubtotalCents())),
                HepyCartController.hepyRow("Recargos por materiales", HepyMoney.hepyFormat(ppqhO.ppqhSurchargeCents())),
                HepyCartController.hepyRow("Envío", ppqhO.ppqhShippingCents() == 0 ? "Gratis" : HepyMoney.hepyFormat(ppqhO.ppqhShippingCents())),
                ppqhTotalRow));
        ppqhItemsCard.setPrefWidth(600);
        ppqhItemsCard.setMinWidth(460);

        Label ppqhShip = new Label(ppqhO.ppqhShipRecipient() + " · " + ppqhO.ppqhShipPhone() + "\n" + ppqhO.ppqhShippingOneLine()
                + (ppqhO.ppqhShipReference() == null ? "" : "\nRef.: " + ppqhO.ppqhShipReference()));
        ppqhShip.getStyleClass().addAll("body-s", "ink");
        ppqhShip.setWrapText(true);
        Label ppqhPay = new Label(ppqhO.ppqhPaymentMethod().ppqhLabel());
        ppqhPay.getStyleClass().addAll("body-s", "ink");
        VBox ppqhSide = new VBox(16, ppqhCard("Envío", ppqhShip), ppqhCard("Pago", new VBox(8, ppqhPay, ppqhPaymentStatus(ppqhO))));
        if (ppqhO.ppqhNotes() != null) {
            Label ppqhNotes = new Label(ppqhO.ppqhNotes());
            ppqhNotes.getStyleClass().addAll("body-s", "ink-muted");
            ppqhNotes.setWrapText(true);
            ppqhSide.getChildren().add(ppqhCard("Notas", ppqhNotes));
        }
        if (PPQH_CANCELLABLE.contains(ppqhO.ppqhStatus())) {
            PpqhSfButton ppqhCancel = new PpqhSfButton("Cancelar pedido", PpqhSfButton.PpqhVariant.DANGER).ppqhWithIcon(Feather.X_CIRCLE);
            ppqhCancel.setMaxWidth(Double.MAX_VALUE);
            ppqhCancel.setOnAction(ppqhE -> PpqhConfirmDialog.ppqhShow(ppqhContent, "¿Cancelar el pedido " + ppqhO.ppqhOrderNumber() + "?",
                    "Se liberará el stock reservado. Si ya pagaste, escribe a la tienda antes de cancelar.",
                    "Cancelar pedido", true, () -> ppqhCancel(ppqhO)));
            ppqhSide.getChildren().add(ppqhCancel);
        }
        ppqhSide.setPrefWidth(340);
        ppqhSide.setMinWidth(300);

        FlowPane ppqhColumns = new FlowPane(24, 24, ppqhItemsCard, ppqhSide);
        ppqhContent.getChildren().setAll(ppqhHeader, ppqhTimelineCard, ppqhColumns, ppqhHistoryCard(ppqhO));
    }

    private Node ppqhItemMarker(PpqhOrderItem ppqhI) {
        if (ppqhI.ppqhHasDesign()) {
            javafx.scene.image.ImageView ppqhView = HepyProductImages.hepyDesignCapture(ppqhContext.ppqhStorage(), ppqhI.ppqhDesignFrontPath(), 48, 60);
            if (ppqhView != null) {
                javafx.scene.layout.StackPane ppqhThumb = new javafx.scene.layout.StackPane(ppqhView);
                ppqhThumb.getStyleClass().add("sf-thumb");
                ppqhThumb.setMinSize(48, 60);
                ppqhThumb.setMaxSize(48, 60);
                return ppqhThumb;
            }
        }
        Rectangle ppqhColor = new Rectangle(14, 14, ppqhParse(ppqhI.ppqhColorHex()));
        ppqhColor.setArcWidth(4);
        ppqhColor.setArcHeight(4);
        return ppqhColor;
    }

    private Node ppqhPaymentStatus(PpqhOrderDetail ppqhO) {
        PpqhPayment ppqhCurrent = ppqhO.ppqhCurrentPayment();
        PpqhPayment ppqhRejected = ppqhO.ppqhPayments().stream().filter(ppqhP -> ppqhP.ppqhStatus() == PpqhPaymentStatus.RECHAZADO)
                .reduce((ppqhA, ppqhB) -> ppqhB).orElse(null);
        VBox ppqhBox = new VBox(10);
        switch (ppqhO.ppqhStatus()) {
            case PENDIENTE_PAGO -> {
                if (ppqhRejected != null) {
                    Label ppqhWhy = new Label("Tu pago anterior fue rechazado: " + ppqhRejected.ppqhRejectionReason()
                            + ". Revisa el código y vuelve a enviarlo.");
                    ppqhWhy.getStyleClass().addAll("body-s", "danger-text");
                    ppqhWhy.setWrapText(true);
                    ppqhBox.getChildren().add(ppqhWhy);
                }
                ppqhBox.getChildren().add(ppqhPaymentForm(ppqhO));
            }
            case PAGO_EN_VERIFICACION -> ppqhBox.getChildren().add(ppqhMuted("Pago en verificación: enviaste la operación "
                    + ppqhCurrent.ppqhOperationCode() + ". Un administrador la compara con " + ppqhWhereToCheck(ppqhO)
                    + "; te avisaremos aquí cuando la apruebe."));
            case CANCELADO -> ppqhBox.getChildren().add(ppqhMuted("Pedido cancelado."));
            default -> {
                PpqhPayment ppqhOk = ppqhO.ppqhPayments().stream().filter(ppqhP -> ppqhP.ppqhStatus() == PpqhPaymentStatus.APROBADO).findFirst().orElse(null);
                ppqhBox.getChildren().add(ppqhMuted(ppqhOk == null ? "Pago confirmado."
                        : "Pago aprobado el " + PpqhOrdersController.PPQH_DATE.format(Instant.parse(ppqhOk.ppqhVerifiedAt()))
                        + " (operación " + ppqhOk.ppqhOperationCode() + ")."));
            }
        }
        return ppqhBox;
    }

    private static String ppqhWhereToCheck(PpqhOrderDetail ppqhO) {
        return switch (ppqhO.ppqhPaymentMethod()) {
            case YAPE -> "los movimientos de Yape";
            case PLIN -> "los movimientos de Plin";
            default -> "el estado de cuenta del banco";
        };
    }

    private static Label ppqhMuted(String ppqhS) {
        Label ppqhL = new Label(ppqhS);
        ppqhL.getStyleClass().addAll("body-s", "ink-muted");
        ppqhL.setWrapText(true);
        return ppqhL;
    }

    private Node ppqhPaymentForm(PpqhOrderDetail ppqhO) {
        Label ppqhAmount = new Label("Monto a pagar: " + HepyMoney.hepyFormat(ppqhO.ppqhTotalCents()));
        ppqhAmount.getStyleClass().addAll("body", "ink", "sf-strong");
        Label ppqhHow = ppqhMuted(ppqhInstructions == null ? ppqhO.ppqhPaymentMethod().ppqhLabel() : ppqhInstructions.ppqhForMethod(ppqhO.ppqhPaymentMethod()));
        Label ppqhThen = ppqhMuted("Luego escribe el código de operación y sube la captura o el PDF del comprobante (PNG, JPG o PDF, máx. 5 MB).");

        PpqhSfTextField ppqhCode = new PpqhSfTextField("Código de operación");
        ppqhCode.setPromptText(ppqhO.ppqhPaymentMethod() == com.sportforce3d.model.PpqhPaymentMethod.TRANSFERENCIA ? "N.° de operación" : "Ej. 12345678");
        ppqhCode.setMonospace(true);
        File[] ppqhChosen = new File[1];
        Label ppqhFileLabel = ppqhMuted("Ningún archivo elegido");
        PpqhSfButton ppqhPick = new PpqhSfButton("Elegir comprobante", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM).ppqhWithIcon(Feather.PAPERCLIP);
        ppqhPick.setOnAction(ppqhE -> {
            javafx.stage.FileChooser ppqhChooser = new javafx.stage.FileChooser();
            ppqhChooser.setTitle("Comprobante de pago");
            ppqhChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Imagen o PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
            File ppqhF = ppqhChooser.showOpenDialog(ppqhContent.getScene().getWindow());
            if (ppqhF != null) {
                ppqhChosen[0] = ppqhF;
                ppqhFileLabel.setText(ppqhF.getName());
            }
        });
        HBox ppqhFileRow = new HBox(10, ppqhPick, ppqhFileLabel);
        ppqhFileRow.setAlignment(Pos.CENTER_LEFT);

        PpqhSfButton ppqhSend = new PpqhSfButton("Enviar comprobante", PpqhSfButton.PpqhVariant.PRIMARY).ppqhWithIcon(Feather.SEND);
        ppqhSend.setMaxWidth(Double.MAX_VALUE);
        ppqhSend.setOnAction(ppqhE -> {
            ppqhCode.setError("");
            if (ppqhChosen[0] == null) {
                PpqhToast.ppqhError(ppqhContent, "Elige el comprobante de pago.");
                return;
            }
            ppqhSend.setDisable(true);
            PpqhAsync.ppqhRun(() -> ppqhContext.ppqhPaymentService().ppqhRegister(ppqhO.ppqhId(), ppqhCode.getText(), ppqhChosen[0].toPath()),
                    ppqhUpdated -> {
                        ppqhRender(ppqhUpdated);
                        PpqhToast.ppqhSuccess(ppqhContent, "Recibimos tu pago. Queda en verificación.");
                    },
                    ppqhEx -> {
                        ppqhSend.setDisable(false);
                        if (ppqhEx instanceof com.sportforce3d.service.PpqhValidationException ppqhV && ppqhV.ppqhFieldErrors().containsKey("code")) {
                            ppqhCode.setError(ppqhV.ppqhFieldErrors().get("code"));
                        } else {
                            PpqhToast.ppqhError(ppqhContent, PpqhErrorMessages.ppqhOf(ppqhEx));
                        }
                    });
        });
        ppqhSend.getProperties().put("proof", (java.util.function.Consumer<File>) ppqhF -> {
            ppqhChosen[0] = ppqhF;
            ppqhFileLabel.setText(ppqhF.getName());
        });
        ppqhSend.setId("send-payment");
        VBox ppqhForm = new VBox(10, ppqhAmount, ppqhHow, ppqhThen, ppqhCode, ppqhFileRow, ppqhSend);
        ppqhForm.getStyleClass().add("sf-payment-form");
        return ppqhForm;
    }

    private VBox ppqhHistoryCard(PpqhOrderDetail ppqhO) {
        VBox ppqhRows = new VBox(8);
        for (PpqhStatusChange ppqhChange : ppqhO.ppqhHistory()) {
            Label ppqhWhen = new Label(PpqhOrdersController.PPQH_DATE.format(Instant.parse(ppqhChange.ppqhCreatedAt())));
            ppqhWhen.getStyleClass().addAll("code", "ink-muted");
            ppqhWhen.setMinWidth(150);
            Label ppqhWhat = new Label(ppqhChange.ppqhTo().ppqhLabel() + (ppqhChange.ppqhNote() == null ? "" : " — " + ppqhChange.ppqhNote())
                    + (ppqhChange.ppqhChangedBy() == null ? "" : " (" + ppqhChange.ppqhChangedBy() + ")"));
            ppqhWhat.getStyleClass().addAll("body-s", "ink");
            ppqhWhat.setWrapText(true);
            ppqhRows.getChildren().add(new HBox(12, ppqhWhen, ppqhWhat));
        }
        return ppqhCard("Historial", ppqhRows);
    }

    private void ppqhCancel(PpqhOrderDetail ppqhO) {
        PpqhAsync.ppqhRun(() -> ppqhContext.ppqhOrderService().ppqhCancel(ppqhO.ppqhId(), null),
                ppqhUpdated -> {
                    ppqhRender(ppqhUpdated);
                    PpqhToast.ppqhSuccess(ppqhContent, "Pedido " + ppqhO.ppqhOrderNumber() + " cancelado.");
                },
                ppqhE -> PpqhToast.ppqhError(ppqhContent, PpqhErrorMessages.ppqhOf(ppqhE)));
    }

    private static VBox ppqhCard(String ppqhTitle, Node ppqhBody) {
        Label ppqhLabel = new Label(ppqhTitle);
        ppqhLabel.getStyleClass().addAll("title-s", "ink");
        VBox ppqhCard = new VBox(12, ppqhLabel, ppqhBody);
        ppqhCard.getStyleClass().add("sf-card");
        ppqhCard.setPadding(new Insets(20));
        return ppqhCard;
    }

    private static Color ppqhParse(String ppqhHex) {
        try {
            return Color.web(ppqhHex);
        } catch (RuntimeException ppqhE) {
            return Color.GRAY;
        }
    }
}
