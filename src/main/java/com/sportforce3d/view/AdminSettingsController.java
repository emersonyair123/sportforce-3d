package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.PaymentInstructions;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.service.ValidationException;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdminSettingsController {

    private final AppContext context;
    private final BooleanProperty saving = new SimpleBooleanProperty(false);
    private final Map<String, SfTextField> fields = new LinkedHashMap<>();
    private PaymentInstructions loaded;

    @FXML private HBox reviewNotice;
    @FXML private Label yapeTitle;
    @FXML private Label plinTitle;
    @FXML private Label bankTitle;
    @FXML private SfTextField yapePhoneField;
    @FXML private SfTextField yapeHolderField;
    @FXML private SfTextField plinPhoneField;
    @FXML private SfTextField plinHolderField;
    @FXML private SfTextField bankNameField;
    @FXML private SfTextField bankHolderField;
    @FXML private SfTextField bankAccountField;
    @FXML private SfTextField bankCciField;
    @FXML private VBox previewLines;
    @FXML private SfButton saveButton;
    @FXML private SfButton resetButton;
    @FXML private Label savedLabel;

    public AdminSettingsController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        fields.put("yapePhone", yapePhoneField);
        fields.put("yapeHolder", yapeHolderField);
        fields.put("plinPhone", plinPhoneField);
        fields.put("plinHolder", plinHolderField);
        fields.put("bankName", bankNameField);
        fields.put("bankHolder", bankHolderField);
        fields.put("bankAccount", bankAccountField);
        fields.put("bankCci", bankCciField);
        for (SfTextField f : fields.values()) {
            f.textProperty().addListener((obs, old, v) -> {
                f.setError("");
                renderPreview();
            });
        }
        for (SfTextField f : new SfTextField[]{yapePhoneField, plinPhoneField, bankAccountField, bankCciField}) {
            f.setMonospace(true);
        }
        yapePhoneField.setHint("Celular de 9 dígitos que empieza con 9.");
        plinPhoneField.setHint("Celular de 9 dígitos que empieza con 9.");
        bankCciField.setHint("Código de cuenta interbancario: exactamente 20 dígitos.");

        yapeTitle.setGraphic(Icons.of(Feather.SMARTPHONE, Icons.SM));
        plinTitle.setGraphic(Icons.of(Feather.SMARTPHONE, Icons.SM));
        bankTitle.setGraphic(Icons.of(Feather.CREDIT_CARD, Icons.SM));
        reviewNotice.getChildren().add(0, Icons.of(Feather.ALERT_TRIANGLE, Icons.SM));
        reviewNotice.managedProperty().bind(reviewNotice.visibleProperty());
        reviewNotice.setVisible(false);

        saveButton.withIcon(Feather.SAVE);
        saveButton.disableProperty().bind(saving);
        saveButton.setOnAction(e -> save());
        resetButton.disableProperty().bind(saving);
        resetButton.setOnAction(e -> {
            if (loaded != null) {
                fill(loaded);
            }
        });

        Async.run(() -> context.storeSettingsService().paymentSettings(), p -> {
            fill(p);
            savedLabel.setText(p.reviewed() ? "" : "Aún no se han guardado datos reales.");
        }, e -> ((VBox) saveButton.getParent().getParent()).getChildren().setAll(
                new EmptyState(Feather.LOCK, "No se pudo abrir la configuración", ErrorMessages.of(e))));
    }

    private void fill(PaymentInstructions p) {
        loaded = p;
        yapePhoneField.setText(p.yapePhone());
        yapeHolderField.setText(p.yapeHolder());
        plinPhoneField.setText(p.plinPhone());
        plinHolderField.setText(p.plinHolder());
        bankNameField.setText(p.bankName());
        bankHolderField.setText(p.bankHolder());
        bankAccountField.setText(p.bankAccount());
        bankCciField.setText(p.bankCci());
        fields.values().forEach(f -> f.setError(""));
        reviewNotice.setVisible(!p.reviewed());
    }

    private PaymentInstructions form() {
        return new PaymentInstructions(yapePhoneField.getText(), yapeHolderField.getText(), plinPhoneField.getText(),
                plinHolderField.getText(), bankNameField.getText(), bankHolderField.getText(),
                bankAccountField.getText(), bankCciField.getText(), false);
    }

    private void renderPreview() {
        PaymentInstructions p = form();
        previewLines.getChildren().clear();
        for (PaymentMethod m : new PaymentMethod[]{PaymentMethod.YAPE, PaymentMethod.PLIN, PaymentMethod.TRANSFERENCIA}) {
            Label l = new Label(m.label() + ": " + p.forMethod(m));
            l.getStyleClass().addAll("body-s", "ink");
            l.setWrapText(true);
            previewLines.getChildren().add(l);
        }
    }

    private void save() {
        PaymentInstructions form = form();
        fields.values().forEach(f -> f.setError(""));
        saving.set(true);
        Async.run(() -> context.storeSettingsService().savePaymentSettings(form), saved -> {
            saving.set(false);
            fill(saved);
            savedLabel.setText("Guardado. Los clientes ya ven estos datos.");
            Toast.success(saveButton, "Datos de pago guardados.");
        }, e -> {
            saving.set(false);
            if (e instanceof ValidationException v && !v.fieldErrors().isEmpty()) {
                v.fieldErrors().forEach((key, message) -> {
                    SfTextField f = fields.get(key);
                    if (f != null) {
                        f.setError(message);
                    }
                });
                SfTextField first = fields.entrySet().stream()
                        .filter(en -> v.fieldErrors().containsKey(en.getKey()))
                        .map(Map.Entry::getValue).findFirst().orElse(null);
                if (first != null) {
                    first.requestFocus();
                    scrollTo(first);
                }
                Toast.error(saveButton, "Revisa los campos marcados.");
            } else {
                Toast.error(saveButton, ErrorMessages.of(e));
            }
        });
    }

    private static void scrollTo(SfTextField field) {
        for (javafx.scene.Node p = field.getParent(); p != null; p = p.getParent()) {
            if (p instanceof ScrollPane sp && sp.getContent() != null) {
                double y = sp.getContent().sceneToLocal(field.localToScene(0, 0)).getY();
                double extra = sp.getContent().getLayoutBounds().getHeight() - sp.getViewportBounds().getHeight();
                if (extra > 0) {
                    sp.setVvalue(Math.max(0, Math.min(1, (y - 80) / extra)));
                }
                return;
            }
        }
    }
}
