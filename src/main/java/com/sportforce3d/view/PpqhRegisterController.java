package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfLink;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.util.PpqhValidators;
import com.sportforce3d.viewmodel.PpqhRegisterViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.feather.Feather;

public class PpqhRegisterController {

    private final PpqhAppContext ppqhContext;
    private final PpqhRegisterViewModel ppqhViewModel;

    @FXML private HBox ppqhErrorBox;
    @FXML private Label ppqhErrorLabel;
    @FXML private PpqhSfTextField ppqhFullNameField;
    @FXML private PpqhSfTextField ppqhEmailField;
    @FXML private PpqhSfTextField ppqhPhoneField;
    @FXML private PpqhSfTextField ppqhPasswordField;
    @FXML private PpqhSfTextField ppqhConfirmationField;
    @FXML private PpqhSfButton ppqhRegisterButton;
    @FXML private PpqhSfLink ppqhLoginLink;

    public PpqhRegisterController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
        this.ppqhViewModel = new PpqhRegisterViewModel(ppqhContext.ppqhAuthService());
    }

    @FXML
    private void initialize() {
        ppqhFullNameField.textProperty().bindBidirectional(ppqhViewModel.fullNameProperty());
        ppqhEmailField.textProperty().bindBidirectional(ppqhViewModel.emailProperty());
        ppqhPhoneField.textProperty().bindBidirectional(ppqhViewModel.phoneProperty());

        ppqhFullNameField.errorProperty().bind(ppqhViewModel.fullNameErrorProperty());
        ppqhEmailField.errorProperty().bind(ppqhViewModel.emailErrorProperty());
        ppqhPhoneField.errorProperty().bind(ppqhViewModel.phoneErrorProperty());
        ppqhPasswordField.errorProperty().bind(ppqhViewModel.passwordErrorProperty());
        ppqhConfirmationField.errorProperty().bind(ppqhViewModel.confirmationErrorProperty());
        ppqhPasswordField.setHint("Mínimo " + PpqhValidators.PPQH_PASSWORD_MIN + " caracteres, con letras y números.");

        ppqhErrorLabel.textProperty().bind(ppqhViewModel.errorProperty());
        ppqhErrorBox.getChildren().add(0, PpqhIcons.ppqhOf(Feather.ALERT_CIRCLE, PpqhIcons.PPQH_MD));
        ppqhErrorBox.visibleProperty().bind(ppqhViewModel.errorProperty().isNotEmpty());
        ppqhErrorBox.managedProperty().bind(ppqhErrorBox.visibleProperty());

        ppqhRegisterButton.disableProperty().bind(ppqhViewModel.busyProperty());
        ppqhRegisterButton.textProperty().bind(Bindings.when(ppqhViewModel.busyProperty())
                .then("Creando cuenta…").otherwise("Crear cuenta"));
        ppqhRegisterButton.setOnAction(ppqhE -> ppqhSubmit());
        ppqhLoginLink.setOnAction(ppqhE -> ppqhContext.ppqhNavigator().ppqhShowLogin());

        Platform.runLater(ppqhFullNameField::requestFocus);
    }

    private void ppqhSubmit() {
        ppqhViewModel.ppqhRegister(ppqhPasswordField.getChars(), ppqhConfirmationField.getChars(), ppqhUser -> {
            ppqhContext.ppqhNavigator().ppqhShowShell();
            PpqhToast.ppqhSuccess(ppqhContext.ppqhNavigator().ppqhStage().getScene().getRoot(),
                    "¡Te damos la bienvenida, " + ppqhUser.ppqhFirstName() + "! Tu cuenta está lista.");
        });
    }
}
