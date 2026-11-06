package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.FilterChip;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfLink;
import com.sportforce3d.model.Payment;
import com.sportforce3d.model.PaymentProof;
import com.sportforce3d.model.PaymentStatus;
import com.sportforce3d.util.Money;
import com.sportforce3d.viewmodel.ErrorMessages;
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

/** Administración → Pagos: cola de verificación (los más antiguos primero) y pagos ya resueltos. */
public class AdminPaymentsController {

    private final AppContext context;
    private final ToggleGroup group = new ToggleGroup();
    private PaymentStatus status = PaymentStatus.EN_VERIFICACION;

    @FXML private FlowPane chips;
    @FXML private VBox list;

    public AdminPaymentsController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        for (PaymentStatus s : List.of(PaymentStatus.EN_VERIFICACION, PaymentStatus.APROBADO, PaymentStatus.RECHAZADO)) {
            FilterChip chip = new FilterChip(s == PaymentStatus.EN_VERIFICACION ? "Por verificar" : s.label() + "s");
            chip.setToggleGroup(group);
            chip.setSelected(s == status);
            chip.setOnAction(e -> {
                chip.setSelected(true);
                status = s;
                load();
            });
            chips.getChildren().add(chip);
        }
        load();
    }

    private void load() {
        PaymentStatus s = status;
        Async.run(() -> context.paymentService().queue(s), this::render,
                e -> list.getChildren().setAll(new EmptyState(Feather.LOCK, "No se pudo cargar los pagos", ErrorMessages.of(e))));
    }

    private void render(List<Payment> payments) {
        if (payments.isEmpty()) {
            list.getChildren().setAll(status == PaymentStatus.EN_VERIFICACION
                    ? new EmptyState(Feather.CHECK_CIRCLE, "No hay pagos por verificar",
                    "Cuando un cliente registre su código de operación, aparecerá aquí.")
                    : new EmptyState(Feather.CREDIT_CARD, "Sin pagos", "No hay pagos en este estado."));
            return;
        }
        list.getChildren().clear();
        for (Payment p : payments) {
            list.getChildren().add(card(p));
        }
    }

    private Node card(Payment p) {
        HBox row = new HBox(20);
        row.getStyleClass().add("sf-card");
        row.setPadding(new Insets(16));

        VBox proofs = new VBox(8);
        for (PaymentProof proof : p.proofs()) {
            proofs.getChildren().add(PaymentActions.proofThumb(context, proof, 120, 160));
        }
        if (p.proofs().isEmpty()) {
            proofs.getChildren().add(label("Sin comprobante", "body-s", "ink-subtle"));
        }

        SfLink number = new SfLink(p.orderNumber());
        number.setOnAction(e -> context.router().go(Routes.ADMIN_ORDER, p.orderId()));
        HBox head = new HBox(12, number, PaymentActions.badge(p.status()));
        head.setAlignment(Pos.CENTER_LEFT);
        Label amount = label(Money.format(p.amountCents()), "price", "ink");
        Label who = label(p.customerName() + " · " + p.method().label(), "body-s", "ink");
        Label code = label("Operación " + (p.operationCode() == null ? "—" : p.operationCode()), "code", "ink");
        Label when = label("Registrado el " + OrdersController.DATE.format(Instant.parse(p.updatedAt())), "body-s", "ink-muted");
        VBox info = new VBox(6, head, amount, who, code, when);
        if (p.amountCents() != p.orderTotalCents()) {
            info.getChildren().add(label("El pedido suma " + Money.format(p.orderTotalCents()) + ".", "body-s", "warning-text"));
        }
        if (p.status() == PaymentStatus.APROBADO) {
            info.getChildren().add(label("Aprobado por " + p.verifiedBy() + " el "
                    + OrdersController.DATE.format(Instant.parse(p.verifiedAt())), "body-s", "ink-muted"));
        } else if (p.status() == PaymentStatus.RECHAZADO) {
            info.getChildren().add(label("Motivo: " + p.rejectionReason(), "body-s", "danger-text"));
        }
        HBox.setHgrow(info, Priority.ALWAYS);
        info.setMinWidth(0);

        VBox actions = new VBox(8);
        actions.setAlignment(Pos.TOP_RIGHT);
        if (p.status() == PaymentStatus.EN_VERIFICACION) {
            SfButton approve = new SfButton("Aprobar", SfButton.Variant.PRIMARY, SfButton.Size.SM).withIcon(Feather.CHECK);
            approve.setOnAction(e -> PaymentActions.approve(context, list, p, o -> load()));
            SfButton reject = new SfButton("Rechazar", SfButton.Variant.DANGER, SfButton.Size.SM).withIcon(Feather.X);
            reject.setOnAction(e -> PaymentActions.reject(context, list, p, o -> load()));
            approve.setMaxWidth(Double.MAX_VALUE);
            reject.setMaxWidth(Double.MAX_VALUE);
            actions.getChildren().addAll(approve, reject);
        }
        if (!p.proofs().isEmpty()) {
            SfButton download = new SfButton("Descargar comprobante", SfButton.Variant.GHOST, SfButton.Size.SM)
                    .withIcon(Feather.DOWNLOAD);
            download.setOnAction(e -> PaymentActions.download(context, list, p.proofs().get(0)));
            actions.getChildren().add(download);
        }
        actions.setMinWidth(190);
        row.getChildren().addAll(proofs, info, actions);
        return row;
    }

    private static Label label(String text, String... styles) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styles);
        l.setWrapText(true);
        return l;
    }
}
