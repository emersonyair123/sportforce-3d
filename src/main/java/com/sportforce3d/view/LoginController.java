package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfLink;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.viewmodel.LoginViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.feather.Feather;

public class LoginController {

    private final AppContext context;
    private final LoginViewModel viewModel;

    @FXML private HBox errorBox;
    @FXML private Label errorLabel;
    @FXML private SfTextField emailField;
    @FXML private SfTextField passwordField;
    @FXML private SfButton loginButton;
    @FXML private SfLink registerLink;

    public LoginController(AppContext context) {
        this.context = context;
        this.viewModel = new LoginViewModel(context.authService());
    }

    @FXML
    private void initialize() {
        emailField.textProperty().bindBidirectional(viewModel.emailProperty());
        errorLabel.textProperty().bind(viewModel.errorProperty());
        errorBox.getChildren().add(0, Icons.of(Feather.ALERT_CIRCLE, Icons.MD));
        errorBox.visibleProperty().bind(viewModel.errorProperty().isNotEmpty());
        errorBox.managedProperty().bind(errorBox.visibleProperty());

        loginButton.disableProperty().bind(viewModel.canSubmitProperty().not());
        loginButton.textProperty().bind(javafx.beans.binding.Bindings.when(viewModel.busyProperty())
                .then("Ingresando…").otherwise("Iniciar sesión"));
        loginButton.setOnAction(e -> submit());
        passwordField.getInput().setOnAction(e -> submit());
        registerLink.setOnAction(e -> context.navigator().showRegister());

        Platform.runLater(emailField::requestFocus);
    }

    private void submit() {
        if (!viewModel.canSubmitProperty().get()) {
            return;
        }
        char[] password = passwordField.getChars();
        viewModel.login(password,
                user -> {
                    passwordField.setText("");
                    context.navigator().showShell();
                },
                () -> {
                    passwordField.setText("");
                    passwordField.requestFocus();
                });
    }
}
