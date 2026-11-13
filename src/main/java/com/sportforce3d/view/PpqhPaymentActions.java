package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhModalDialog;
import com.sportforce3d.component.PpqhSfBadge;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhPayment;
import com.sportforce3d.model.PpqhPaymentProof;
import com.sportforce3d.model.PpqhPaymentStatus;
import com.sportforce3d.service.PpqhPaymentService;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

final class PpqhPaymentActions {

    private PpqhPaymentActions() {
    }

    static PpqhSfBadge ppqhBadge(PpqhPaymentStatus ppqhS) {
        return switch (ppqhS) {
            case PENDIENTE -> new PpqhSfBadge("Sin registrar").ppqhWithIcon(Feather.CLOCK);
            case EN_VERIFICACION -> new PpqhSfBadge("En verificación", PpqhSfBadge.PpqhKind.WARNING).ppqhWithIcon(Feather.EYE);
            case APROBADO -> new PpqhSfBadge("Aprobado", PpqhSfBadge.PpqhKind.SUCCESS).ppqhWithIcon(Feather.CHECK);
            case RECHAZADO -> new PpqhSfBadge("Rechazado", PpqhSfBadge.PpqhKind.DANGER).ppqhWithIcon(Feather.X);
        };
    }

    static void ppqhApprove(PpqhAppContext ppqhContext, Node ppqhOwner, PpqhPayment ppqhP, Consumer<PpqhOrderDetail> ppqhDone) {
        PpqhSfTextField ppqhNote = new PpqhSfTextField("Nota (opcional)");
        ppqhNote.setPromptText("Ej. Visto en la app el 17/10 a las 10:42");
        VBox ppqhBody = new VBox(12);
        if (ppqhP.ppqhAmountCents() != ppqhP.ppqhOrderTotalCents()) {
            Label ppqhWarn = new Label("Atención: el pago es de " + HepyMoney.hepyFormat(ppqhP.ppqhAmountCents()) + " y el pedido suma "
                    + HepyMoney.hepyFormat(ppqhP.ppqhOrderTotalCents()) + ".");
            ppqhWarn.getStyleClass().addAll("body-s", "warning-text");
            ppqhWarn.setWrapText(true);
            ppqhBody.getChildren().add(ppqhWarn);
        }
        ppqhBody.getChildren().add(ppqhNote);
        PpqhModalDialog.ppqhShow(ppqhOwner, "Aprobar pago de " + ppqhP.ppqhOrderNumber(),
                "Confirma que viste " + HepyMoney.hepyFormat(ppqhP.ppqhAmountCents()) + " con la operación " + ppqhP.ppqhOperationCode()
                        + " en " + ppqhWhere(ppqhP) + ". El pedido pasará a Pagado y se descontará el stock reservado.",
                ppqhBody, "Aprobar pago", PpqhModalDialog.PpqhStyle.PRIMARY, ppqhD -> {
                    ppqhD.ppqhSetBusy(true);
                    PpqhAsync.ppqhRun(() -> ppqhContext.ppqhPaymentService().ppqhApprove(ppqhP.ppqhId(), ppqhNote.getText()), ppqhOrder -> {
                        ppqhD.ppqhClose();
                        PpqhToast.ppqhSuccess(ppqhOwner, "Pago aprobado: " + ppqhP.ppqhOrderNumber() + " está Pagado.");
                        ppqhDone.accept(ppqhOrder);
                    }, ppqhE -> {
                        ppqhD.ppqhSetBusy(false);
                        ppqhD.ppqhSetError(PpqhErrorMessages.ppqhOf(ppqhE));
                    });
                });
    }

    static void ppqhReject(PpqhAppContext ppqhContext, Node ppqhOwner, PpqhPayment ppqhP, Consumer<PpqhOrderDetail> ppqhDone) {
        TextArea ppqhReason = new TextArea();
        ppqhReason.setPromptText("Ej. El código no aparece en los movimientos de Yape del 17/10.");
        ppqhReason.setWrapText(true);
        ppqhReason.setPrefRowCount(3);
        ppqhReason.getStyleClass().add("sf-textarea");
        Label ppqhHint = new Label("El cliente verá este motivo y podrá registrar el pago otra vez. El stock sigue reservado.");
        ppqhHint.getStyleClass().addAll("body-s", "ink-subtle");
        ppqhHint.setWrapText(true);
        PpqhModalDialog.ppqhShow(ppqhOwner, "Rechazar pago de " + ppqhP.ppqhOrderNumber(), "Operación " + ppqhP.ppqhOperationCode() + " · "
                        + HepyMoney.hepyFormat(ppqhP.ppqhAmountCents()), new VBox(8, ppqhReason, ppqhHint), "Rechazar pago",
                PpqhModalDialog.PpqhStyle.DANGER, ppqhD -> {
                    String ppqhText = ppqhReason.getText() == null ? "" : ppqhReason.getText().trim();
                    if (ppqhText.length() < PpqhPaymentService.PPQH_REASON_MIN) {
                        ppqhD.ppqhSetError("Escribe el motivo (al menos " + PpqhPaymentService.PPQH_REASON_MIN + " caracteres).");
                        return;
                    }
                    ppqhD.ppqhSetBusy(true);
                    PpqhAsync.ppqhRun(() -> ppqhContext.ppqhPaymentService().ppqhReject(ppqhP.ppqhId(), ppqhText), ppqhOrder -> {
                        ppqhD.ppqhClose();
                        PpqhToast.ppqhInfo(ppqhOwner, "Pago rechazado. " + ppqhP.ppqhOrderNumber() + " vuelve a Pendiente de pago.");
                        ppqhDone.accept(ppqhOrder);
                    }, ppqhE -> {
                        ppqhD.ppqhSetBusy(false);
                        ppqhD.ppqhSetError(PpqhErrorMessages.ppqhOf(ppqhE));
                    });
                });
    }

    private static String ppqhWhere(PpqhPayment ppqhP) {
        return switch (ppqhP.ppqhMethod()) {
            case YAPE -> "tu app de Yape";
            case PLIN -> "tu app de Plin";
            default -> "el estado de cuenta del banco";
        };
    }

    static Node ppqhProofThumb(PpqhAppContext ppqhContext, PpqhPaymentProof ppqhProof, double ppqhW, double ppqhH) {
        StackPane ppqhBox = new StackPane();
        ppqhBox.getStyleClass().add("sf-proof");
        ppqhBox.setMinSize(ppqhW, ppqhH);
        ppqhBox.setPrefSize(ppqhW, ppqhH);
        ppqhBox.setMaxSize(ppqhW, ppqhH);
        if (ppqhProof.ppqhIsPdf()) {
            Label ppqhPdf = new Label("PDF", PpqhIcons.ppqhOf(Feather.FILE_TEXT, PpqhIcons.PPQH_MD));
            ppqhPdf.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
            ppqhPdf.getStyleClass().addAll("body-s", "ink-muted");
            ppqhBox.getChildren().add(ppqhPdf);
        } else {
            Image ppqhImg = HepyProductImages.hepyLoad(ppqhContext.ppqhStorage(), ppqhProof.ppqhPath(), ppqhW * 2, ppqhH * 2);
            if (ppqhImg != null && !ppqhImg.isError()) {
                ImageView ppqhView = new ImageView(ppqhImg);
                ppqhView.setFitWidth(ppqhW);
                ppqhView.setFitHeight(ppqhH);
                ppqhView.setPreserveRatio(true);
                ppqhBox.getChildren().add(ppqhView);
            } else {
                Label ppqhNone = new Label("Sin vista previa", PpqhIcons.ppqhOf(Feather.IMAGE, PpqhIcons.PPQH_MD));
                ppqhNone.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
                ppqhNone.getStyleClass().addAll("body-s", "ink-muted");
                ppqhNone.setWrapText(true);
                ppqhNone.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                ppqhBox.getChildren().add(ppqhNone);
            }
        }
        ppqhBox.setCursor(Cursor.HAND);
        ppqhBox.setAccessibleText("Ver comprobante");
        ppqhBox.setOnMouseClicked(ppqhE -> ppqhShowProof(ppqhContext, ppqhBox, ppqhProof));
        return ppqhBox;
    }

    static void ppqhShowProof(PpqhAppContext ppqhContext, Node ppqhOwner, PpqhPaymentProof ppqhProof) {
        Node ppqhContent;
        if (ppqhProof.ppqhIsPdf()) {
            Label ppqhL = new Label("El comprobante es un PDF. Descárgalo para abrirlo con tu visor de PDF.");
            ppqhL.getStyleClass().addAll("body-s", "ink-muted");
            ppqhL.setWrapText(true);
            ppqhContent = ppqhL;
        } else {
            Image ppqhImg = HepyProductImages.hepyLoad(ppqhContext.ppqhStorage(), ppqhProof.ppqhPath(), 880, 1100);
            ImageView ppqhView = new ImageView(ppqhImg);
            ppqhView.setFitWidth(420);
            ppqhView.setFitHeight(520);
            ppqhView.setPreserveRatio(true);
            StackPane ppqhFrame = new StackPane(ppqhView);
            ppqhFrame.getStyleClass().add("sf-proof");
            ppqhFrame.setAlignment(Pos.CENTER);
            ppqhContent = ppqhFrame;
        }
        PpqhModalDialog.ppqhShow(ppqhOwner, "Comprobante de pago", ppqhProof.ppqhMimeType() + " · " + (ppqhProof.ppqhSizeBytes() / 1024) + " KB",
                ppqhContent, "Descargar", PpqhModalDialog.PpqhStyle.PRIMARY, ppqhD -> {
                    if (ppqhDownload(ppqhContext, ppqhOwner, ppqhProof)) {
                        ppqhD.ppqhClose();
                    }
                });
    }

    static boolean ppqhDownload(PpqhAppContext ppqhContext, Node ppqhOwner, PpqhPaymentProof ppqhProof) {
        FileChooser ppqhChooser = new FileChooser();
        ppqhChooser.setTitle("Guardar comprobante");
        String ppqhExt = ppqhProof.ppqhIsPdf() ? ".pdf" : ppqhProof.ppqhMimeType().equals("image/png") ? ".png" : ".jpg";
        ppqhChooser.setInitialFileName("comprobante-" + ppqhProof.ppqhId() + ppqhExt);
        File ppqhFile = ppqhChooser.showSaveDialog(ppqhOwner.getScene().getWindow());
        if (ppqhFile == null) {
            return false;
        }
        Path ppqhSource = ppqhContext.ppqhStorage().hepyResolve(ppqhProof.ppqhPath());
        PpqhAsync.ppqhRun(() -> Files.copy(ppqhSource, ppqhFile.toPath(), StandardCopyOption.REPLACE_EXISTING),
                ppqhP -> PpqhToast.ppqhSuccess(ppqhOwner, "Comprobante guardado: " + ppqhP.getFileName()),
                ppqhE -> PpqhToast.ppqhError(ppqhOwner, PpqhErrorMessages.ppqhOf(ppqhE)));
        return true;
    }
}
