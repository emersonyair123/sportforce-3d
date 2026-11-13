package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.ModalDialog;
import com.sportforce3d.component.SfBadge;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.Payment;
import com.sportforce3d.model.PaymentProof;
import com.sportforce3d.model.PaymentStatus;
import com.sportforce3d.service.PaymentService;
import com.sportforce3d.util.Money;
import com.sportforce3d.viewmodel.ErrorMessages;
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

final class PaymentActions {

    private PaymentActions() {
    }

    static SfBadge badge(PaymentStatus s) {
        return switch (s) {
            case PENDIENTE -> new SfBadge("Sin registrar").withIcon(Feather.CLOCK);
            case EN_VERIFICACION -> new SfBadge("En verificación", SfBadge.Kind.WARNING).withIcon(Feather.EYE);
            case APROBADO -> new SfBadge("Aprobado", SfBadge.Kind.SUCCESS).withIcon(Feather.CHECK);
            case RECHAZADO -> new SfBadge("Rechazado", SfBadge.Kind.DANGER).withIcon(Feather.X);
        };
    }

    static void approve(AppContext context, Node owner, Payment p, Consumer<OrderDetail> done) {
        SfTextField note = new SfTextField("Nota (opcional)");
        note.setPromptText("Ej. Visto en la app el 17/10 a las 10:42");
        VBox body = new VBox(12);
        if (p.amountCents() != p.orderTotalCents()) {
            Label warn = new Label("Atención: el pago es de " + Money.format(p.amountCents()) + " y el pedido suma "
                    + Money.format(p.orderTotalCents()) + ".");
            warn.getStyleClass().addAll("body-s", "warning-text");
            warn.setWrapText(true);
            body.getChildren().add(warn);
        }
        body.getChildren().add(note);
        ModalDialog.show(owner, "Aprobar pago de " + p.orderNumber(),
                "Confirma que viste " + Money.format(p.amountCents()) + " con la operación " + p.operationCode()
                        + " en " + where(p) + ". El pedido pasará a Pagado y se descontará el stock reservado.",
                body, "Aprobar pago", ModalDialog.Style.PRIMARY, d -> {
                    d.setBusy(true);
                    Async.run(() -> context.paymentService().approve(p.id(), note.getText()), order -> {
                        d.close();
                        Toast.success(owner, "Pago aprobado: " + p.orderNumber() + " está Pagado.");
                        done.accept(order);
                    }, e -> {
                        d.setBusy(false);
                        d.setError(ErrorMessages.of(e));
                    });
                });
    }

    static void reject(AppContext context, Node owner, Payment p, Consumer<OrderDetail> done) {
        TextArea reason = new TextArea();
        reason.setPromptText("Ej. El código no aparece en los movimientos de Yape del 17/10.");
        reason.setWrapText(true);
        reason.setPrefRowCount(3);
        reason.getStyleClass().add("sf-textarea");
        Label hint = new Label("El cliente verá este motivo y podrá registrar el pago otra vez. El stock sigue reservado.");
        hint.getStyleClass().addAll("body-s", "ink-subtle");
        hint.setWrapText(true);
        ModalDialog.show(owner, "Rechazar pago de " + p.orderNumber(), "Operación " + p.operationCode() + " · "
                        + Money.format(p.amountCents()), new VBox(8, reason, hint), "Rechazar pago",
                ModalDialog.Style.DANGER, d -> {
                    String text = reason.getText() == null ? "" : reason.getText().trim();
                    if (text.length() < PaymentService.REASON_MIN) {
                        d.setError("Escribe el motivo (al menos " + PaymentService.REASON_MIN + " caracteres).");
                        return;
                    }
                    d.setBusy(true);
                    Async.run(() -> context.paymentService().reject(p.id(), text), order -> {
                        d.close();
                        Toast.info(owner, "Pago rechazado. " + p.orderNumber() + " vuelve a Pendiente de pago.");
                        done.accept(order);
                    }, e -> {
                        d.setBusy(false);
                        d.setError(ErrorMessages.of(e));
                    });
                });
    }

    private static String where(Payment p) {
        return switch (p.method()) {
            case YAPE -> "tu app de Yape";
            case PLIN -> "tu app de Plin";
            default -> "el estado de cuenta del banco";
        };
    }

    static Node proofThumb(AppContext context, PaymentProof proof, double w, double h) {
        StackPane box = new StackPane();
        box.getStyleClass().add("sf-proof");
        box.setMinSize(w, h);
        box.setPrefSize(w, h);
        box.setMaxSize(w, h);
        if (proof.isPdf()) {
            Label pdf = new Label("PDF", Icons.of(Feather.FILE_TEXT, Icons.MD));
            pdf.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
            pdf.getStyleClass().addAll("body-s", "ink-muted");
            box.getChildren().add(pdf);
        } else {
            Image img = ProductImages.load(context.storage(), proof.path(), w * 2, h * 2);
            if (img != null && !img.isError()) {
                ImageView view = new ImageView(img);
                view.setFitWidth(w);
                view.setFitHeight(h);
                view.setPreserveRatio(true);
                box.getChildren().add(view);
            } else {
                Label none = new Label("Sin vista previa", Icons.of(Feather.IMAGE, Icons.MD));
                none.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
                none.getStyleClass().addAll("body-s", "ink-muted");
                none.setWrapText(true);
                none.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                box.getChildren().add(none);
            }
        }
        box.setCursor(Cursor.HAND);
        box.setAccessibleText("Ver comprobante");
        box.setOnMouseClicked(e -> showProof(context, box, proof));
        return box;
    }

    static void showProof(AppContext context, Node owner, PaymentProof proof) {
        Node content;
        if (proof.isPdf()) {
            Label l = new Label("El comprobante es un PDF. Descárgalo para abrirlo con tu visor de PDF.");
            l.getStyleClass().addAll("body-s", "ink-muted");
            l.setWrapText(true);
            content = l;
        } else {
            Image img = ProductImages.load(context.storage(), proof.path(), 880, 1100);
            ImageView view = new ImageView(img);
            view.setFitWidth(420);
            view.setFitHeight(520);
            view.setPreserveRatio(true);
            StackPane frame = new StackPane(view);
            frame.getStyleClass().add("sf-proof");
            frame.setAlignment(Pos.CENTER);
            content = frame;
        }
        ModalDialog.show(owner, "Comprobante de pago", proof.mimeType() + " · " + (proof.sizeBytes() / 1024) + " KB",
                content, "Descargar", ModalDialog.Style.PRIMARY, d -> {
                    if (download(context, owner, proof)) {
                        d.close();
                    }
                });
    }

    static boolean download(AppContext context, Node owner, PaymentProof proof) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar comprobante");
        String ext = proof.isPdf() ? ".pdf" : proof.mimeType().equals("image/png") ? ".png" : ".jpg";
        chooser.setInitialFileName("comprobante-" + proof.id() + ext);
        File file = chooser.showSaveDialog(owner.getScene().getWindow());
        if (file == null) {
            return false;
        }
        Path source = context.storage().resolve(proof.path());
        Async.run(() -> Files.copy(source, file.toPath(), StandardCopyOption.REPLACE_EXISTING),
                p -> Toast.success(owner, "Comprobante guardado: " + p.getFileName()),
                e -> Toast.error(owner, ErrorMessages.of(e)));
        return true;
    }
}
