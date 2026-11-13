package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.PpqhPaymentInstructions;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.service.PpqhValidationException;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
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

public class PpqhAdminSettingsController {

    private final PpqhAppContext ppqhContext;
    private final BooleanProperty ppqhSaving = new SimpleBooleanProperty(false);
    private final Map<String, PpqhSfTextField> ppqhFields = new LinkedHashMap<>();
    private PpqhPaymentInstructions ppqhLoaded;

    @FXML private HBox ppqhReviewNotice;
    @FXML private Label ppqhYapeTitle;
    @FXML private Label ppqhPlinTitle;
    @FXML private Label ppqhBankTitle;
    @FXML private PpqhSfTextField ppqhYapePhoneField;
    @FXML private PpqhSfTextField ppqhYapeHolderField;
    @FXML private PpqhSfTextField ppqhPlinPhoneField;
    @FXML private PpqhSfTextField ppqhPlinHolderField;
    @FXML private PpqhSfTextField ppqhBankNameField;
    @FXML private PpqhSfTextField ppqhBankHolderField;
    @FXML private PpqhSfTextField ppqhBankAccountField;
    @FXML private PpqhSfTextField ppqhBankCciField;
    @FXML private VBox ppqhPreviewLines;
    @FXML private PpqhSfButton ppqhSaveButton;
    @FXML private PpqhSfButton ppqhResetButton;
    @FXML private Label ppqhSavedLabel;

    public PpqhAdminSettingsController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
    }

    @FXML
    private void initialize() {
        ppqhFields.put("yapePhone", ppqhYapePhoneField);
        ppqhFields.put("yapeHolder", ppqhYapeHolderField);
        ppqhFields.put("plinPhone", ppqhPlinPhoneField);
        ppqhFields.put("plinHolder", ppqhPlinHolderField);
        ppqhFields.put("bankName", ppqhBankNameField);
        ppqhFields.put("bankHolder", ppqhBankHolderField);
        ppqhFields.put("bankAccount", ppqhBankAccountField);
        ppqhFields.put("bankCci", ppqhBankCciField);
        for (PpqhSfTextField ppqhF : ppqhFields.values()) {
            ppqhF.textProperty().addListener((ppqhObs, ppqhOld, ppqhV) -> {
                ppqhF.setError("");
                ppqhRenderPreview();
            });
        }
        for (PpqhSfTextField ppqhF : new PpqhSfTextField[]{ppqhYapePhoneField, ppqhPlinPhoneField, ppqhBankAccountField, ppqhBankCciField}) {
            ppqhF.setMonospace(true);
        }
        ppqhYapePhoneField.setHint("Celular de 9 dígitos que empieza con 9.");
        ppqhPlinPhoneField.setHint("Celular de 9 dígitos que empieza con 9.");
        ppqhBankCciField.setHint("Código de cuenta interbancario: exactamente 20 dígitos.");

        ppqhYapeTitle.setGraphic(PpqhIcons.ppqhOf(Feather.SMARTPHONE, PpqhIcons.PPQH_SM));
        ppqhPlinTitle.setGraphic(PpqhIcons.ppqhOf(Feather.SMARTPHONE, PpqhIcons.PPQH_SM));
        ppqhBankTitle.setGraphic(PpqhIcons.ppqhOf(Feather.CREDIT_CARD, PpqhIcons.PPQH_SM));
        ppqhReviewNotice.getChildren().add(0, PpqhIcons.ppqhOf(Feather.ALERT_TRIANGLE, PpqhIcons.PPQH_SM));
        ppqhReviewNotice.managedProperty().bind(ppqhReviewNotice.visibleProperty());
        ppqhReviewNotice.setVisible(false);

        ppqhSaveButton.ppqhWithIcon(Feather.SAVE);
        ppqhSaveButton.disableProperty().bind(ppqhSaving);
        ppqhSaveButton.setOnAction(ppqhE -> ppqhSave());
        ppqhResetButton.disableProperty().bind(ppqhSaving);
        ppqhResetButton.setOnAction(ppqhE -> {
            if (ppqhLoaded != null) {
                ppqhFill(ppqhLoaded);
            }
        });

        PpqhAsync.ppqhRun(() -> ppqhContext.ppqhStoreSettingsService().ppqhPaymentSettings(), ppqhP -> {
            ppqhFill(ppqhP);
            ppqhSavedLabel.setText(ppqhP.ppqhReviewed() ? "" : "Aún no se han guardado datos reales.");
        }, ppqhE -> ((VBox) ppqhSaveButton.getParent().getParent()).getChildren().setAll(
                new PpqhEmptyState(Feather.LOCK, "No se pudo abrir la configuración", PpqhErrorMessages.ppqhOf(ppqhE))));
    }

    private void ppqhFill(PpqhPaymentInstructions ppqhP) {
        ppqhLoaded = ppqhP;
        ppqhYapePhoneField.setText(ppqhP.ppqhYapePhone());
        ppqhYapeHolderField.setText(ppqhP.ppqhYapeHolder());
        ppqhPlinPhoneField.setText(ppqhP.ppqhPlinPhone());
        ppqhPlinHolderField.setText(ppqhP.ppqhPlinHolder());
        ppqhBankNameField.setText(ppqhP.ppqhBankName());
        ppqhBankHolderField.setText(ppqhP.ppqhBankHolder());
        ppqhBankAccountField.setText(ppqhP.ppqhBankAccount());
        ppqhBankCciField.setText(ppqhP.ppqhBankCci());
        ppqhFields.values().forEach(ppqhF -> ppqhF.setError(""));
        ppqhReviewNotice.setVisible(!ppqhP.ppqhReviewed());
    }

    private PpqhPaymentInstructions ppqhForm() {
        return new PpqhPaymentInstructions(ppqhYapePhoneField.getText(), ppqhYapeHolderField.getText(), ppqhPlinPhoneField.getText(),
                ppqhPlinHolderField.getText(), ppqhBankNameField.getText(), ppqhBankHolderField.getText(),
                ppqhBankAccountField.getText(), ppqhBankCciField.getText(), false);
    }

    private void ppqhRenderPreview() {
        PpqhPaymentInstructions ppqhP = ppqhForm();
        ppqhPreviewLines.getChildren().clear();
        for (PpqhPaymentMethod ppqhM : new PpqhPaymentMethod[]{PpqhPaymentMethod.YAPE, PpqhPaymentMethod.PLIN, PpqhPaymentMethod.TRANSFERENCIA}) {
            Label ppqhL = new Label(ppqhM.ppqhLabel() + ": " + ppqhP.ppqhForMethod(ppqhM));
            ppqhL.getStyleClass().addAll("body-s", "ink");
            ppqhL.setWrapText(true);
            ppqhPreviewLines.getChildren().add(ppqhL);
        }
    }

    private void ppqhSave() {
        PpqhPaymentInstructions ppqhForm = ppqhForm();
        ppqhFields.values().forEach(ppqhF -> ppqhF.setError(""));
        ppqhSaving.set(true);
        PpqhAsync.ppqhRun(() -> ppqhContext.ppqhStoreSettingsService().ppqhSavePaymentSettings(ppqhForm), ppqhSaved -> {
            ppqhSaving.set(false);
            ppqhFill(ppqhSaved);
            ppqhSavedLabel.setText("Guardado. Los clientes ya ven estos datos.");
            PpqhToast.ppqhSuccess(ppqhSaveButton, "Datos de pago guardados.");
        }, ppqhE -> {
            ppqhSaving.set(false);
            if (ppqhE instanceof PpqhValidationException ppqhV && !ppqhV.ppqhFieldErrors().isEmpty()) {
                ppqhV.ppqhFieldErrors().forEach((ppqhKey, ppqhMessage) -> {
                    PpqhSfTextField ppqhF = ppqhFields.get(ppqhKey);
                    if (ppqhF != null) {
                        ppqhF.setError(ppqhMessage);
                    }
                });
                PpqhSfTextField ppqhFirst = ppqhFields.entrySet().stream()
                        .filter(ppqhEn -> ppqhV.ppqhFieldErrors().containsKey(ppqhEn.getKey()))
                        .map(Map.Entry::getValue).findFirst().orElse(null);
                if (ppqhFirst != null) {
                    ppqhFirst.requestFocus();
                    ppqhScrollTo(ppqhFirst);
                }
                PpqhToast.ppqhError(ppqhSaveButton, "Revisa los campos marcados.");
            } else {
                PpqhToast.ppqhError(ppqhSaveButton, PpqhErrorMessages.ppqhOf(ppqhE));
            }
        });
    }

    private static void ppqhScrollTo(PpqhSfTextField ppqhField) {
        for (javafx.scene.Node ppqhP = ppqhField.getParent(); ppqhP != null; ppqhP = ppqhP.getParent()) {
            if (ppqhP instanceof ScrollPane ppqhSp && ppqhSp.getContent() != null) {
                double ppqhY = ppqhSp.getContent().sceneToLocal(ppqhField.localToScene(0, 0)).getY();
                double ppqhExtra = ppqhSp.getContent().getLayoutBounds().getHeight() - ppqhSp.getViewportBounds().getHeight();
                if (ppqhExtra > 0) {
                    ppqhSp.setVvalue(Math.max(0, Math.min(1, (ppqhY - 80) / ppqhExtra)));
                }
                return;
            }
        }
    }
}
