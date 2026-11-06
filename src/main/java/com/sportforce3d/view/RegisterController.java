package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfLink;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.Toast;
import com.sportforce3d.util.Validators;
import com.sportforce3d.viewmodel.RegisterViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.feather.Feather;

public class RegisterController {

    private final AppContext context;
    private final RegisterViewModel viewModel;

    @FXML private HBox errorBox;
    @FXML private Label errorLabel;
    @FXML private SfTextField fullNameField;
    @FXML private SfTextField emailField;
    @FXML private SfTextField phoneField;
    @FXML private SfTextField passwordField;
    @FXML private SfTextField confirmationField;
    @FXML private SfButton registerButton;
    @FXML private SfLink loginLink;

    public RegisterController(AppContext context) {
        this.context = context;
        this.viewModel = new RegisterViewModel(context.authService());
    }

    @FXML
    private void initialize() {
        fullNameField.textProperty().bindBidirectional(viewModel.fullNameProperty());
        emailField.textProperty().bindBidirectional(viewModel.emailProperty());
        phoneField.textProperty().bindBidirectional(viewModel.phoneProperty());

        fullNameField.errorProperty().bind(viewModel.fullNameErrorProperty());
        emailField.errorProperty().bind(viewModel.emailErrorProperty());
        phoneField.errorProperty().bind(viewModel.phoneErrorProperty());
        passwordField.errorProperty().bind(viewModel.passwordErrorProperty());
        confirmationField.errorProperty().bind(viewModel.confirmationErrorProperty());
        passwordField.setHint("Mínimo " + Validators.PASSWORD_MIN + " caracteres, con letras y números.");

        errorLabel.textProperty().bind(viewModel.errorProperty());
        errorBox.getChildren().add(0, Icons.of(Feather.ALERT_CIRCLE, Icons.MD));
        errorBox.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
        errorBox.managedProperty().bind(errorBox.visibleProperty());

        registerButton.disableProperty().bind(viewModel.busyProperty());
        registerButton.textProperty().bind(Bindings.when(viewModel.busyProperty())
                .then("Creando cuenta…").otherwise("Crear cuenta"));
        registerButton.setOnAction(e -> submit());
        loginLink.setOnAction(e -> context.navigator().showLogin());

        Platform.runLater(fullNameField::requestFocus);
    }

    private void submit() {
        viewModel.register(passwordField.getChars(), confirmationField.getChars(), user -> {
            context.navigator().showShell();
            Toast.success(context.navigator().stage().getScene().getRoot(),
                    "¡Te damos la bienvenida, " + user.firstName() + "! Tu cuenta está lista.");
        });
    }
}
