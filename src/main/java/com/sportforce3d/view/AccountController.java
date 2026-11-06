package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.Toast;
import com.sportforce3d.util.Validators;
import com.sportforce3d.viewmodel.AccountViewModel;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

public class AccountController {

    private final AppContext context;
    private final AccountViewModel viewModel;
    private Runnable onProfileSaved = () -> { };

    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private SfTextField fullNameField;
    @FXML private SfTextField phoneField;
    @FXML private SfButton saveProfileButton;
    @FXML private SfTextField currentPasswordField;
    @FXML private SfTextField newPasswordField;
    @FXML private SfTextField confirmationField;
    @FXML private SfButton changePasswordButton;
    @FXML private CheckBox reduceMotionCheck;

    public AccountController(AppContext context) {
        this.context = context;
        this.viewModel = new AccountViewModel(context.accountService(), context.session());
    }

    /** El shell lo usa para refrescar el nombre de la barra superior. */
    public void setOnProfileSaved(Runnable callback) {
        this.onProfileSaved = callback;
    }

    @FXML
    private void initialize() {
        emailLabel.textProperty().bind(viewModel.emailProperty());
        roleLabel.textProperty().bind(Bindings.createStringBinding(
                () -> viewModel.roleLabelProperty().get().toUpperCase(), viewModel.roleLabelProperty()));

        fullNameField.textProperty().bindBidirectional(viewModel.fullNameProperty());
        phoneField.textProperty().bindBidirectional(viewModel.phoneProperty());
        fullNameField.errorProperty().bind(viewModel.fullNameErrorProperty());
        phoneField.errorProperty().bind(viewModel.phoneErrorProperty());
        saveProfileButton.disableProperty().bind(viewModel.savingProfileProperty());
        saveProfileButton.setOnAction(e -> viewModel.saveProfile(
                user -> {
                    onProfileSaved.run();
                    Toast.success(saveProfileButton, "Perfil actualizado.");
                },
                message -> Toast.error(saveProfileButton, message)));

        currentPasswordField.errorProperty().bind(viewModel.currentPasswordErrorProperty());
        newPasswordField.errorProperty().bind(viewModel.newPasswordErrorProperty());
        confirmationField.errorProperty().bind(viewModel.confirmationErrorProperty());
        newPasswordField.setHint("Mínimo " + Validators.PASSWORD_MIN + " caracteres, con letras y números.");
        changePasswordButton.disableProperty().bind(viewModel.changingPasswordProperty());
        changePasswordButton.setOnAction(e -> viewModel.changePassword(
                currentPasswordField.getChars(), newPasswordField.getChars(), confirmationField.getChars(),
                () -> {
                    currentPasswordField.setText("");
                    newPasswordField.setText("");
                    confirmationField.setText("");
                    Toast.success(changePasswordButton, "Contraseña actualizada.");
                },
                message -> Toast.error(changePasswordButton, message)));

        reduceMotionCheck.selectedProperty().bindBidirectional(context.uiSettings().reduceMotionProperty());
    }
}
