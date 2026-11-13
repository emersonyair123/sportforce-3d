package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.util.PpqhValidators;
import com.sportforce3d.viewmodel.PpqhAccountViewModel;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

public class PpqhAccountController {

    private final PpqhAppContext ppqhContext;
    private final PpqhAccountViewModel ppqhViewModel;
    private Runnable ppqhOnProfileSaved = () -> { };

    @FXML private Label ppqhEmailLabel;
    @FXML private Label ppqhRoleLabel;
    @FXML private PpqhSfTextField ppqhFullNameField;
    @FXML private PpqhSfTextField ppqhPhoneField;
    @FXML private PpqhSfButton ppqhSaveProfileButton;
    @FXML private PpqhSfTextField ppqhCurrentPasswordField;
    @FXML private PpqhSfTextField ppqhNewPasswordField;
    @FXML private PpqhSfTextField ppqhConfirmationField;
    @FXML private PpqhSfButton ppqhChangePasswordButton;
    @FXML private CheckBox ppqhReduceMotionCheck;

    public PpqhAccountController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
        this.ppqhViewModel = new PpqhAccountViewModel(ppqhContext.ppqhAccountService(), ppqhContext.ppqhSession());
    }

    public void ppqhSetOnProfileSaved(Runnable ppqhCallback) {
        this.ppqhOnProfileSaved = ppqhCallback;
    }

    @FXML
    private void initialize() {
        ppqhEmailLabel.textProperty().bind(ppqhViewModel.emailProperty());
        ppqhRoleLabel.textProperty().bind(Bindings.createStringBinding(
                () -> ppqhViewModel.roleLabelProperty().get().toUpperCase(), ppqhViewModel.roleLabelProperty()));

        ppqhFullNameField.textProperty().bindBidirectional(ppqhViewModel.fullNameProperty());
        ppqhPhoneField.textProperty().bindBidirectional(ppqhViewModel.phoneProperty());
        ppqhFullNameField.errorProperty().bind(ppqhViewModel.fullNameErrorProperty());
        ppqhPhoneField.errorProperty().bind(ppqhViewModel.phoneErrorProperty());
        ppqhSaveProfileButton.disableProperty().bind(ppqhViewModel.savingProfileProperty());
        ppqhSaveProfileButton.setOnAction(ppqhE -> ppqhViewModel.ppqhSaveProfile(
                ppqhUser -> {
                    ppqhOnProfileSaved.run();
                    PpqhToast.ppqhSuccess(ppqhSaveProfileButton, "Perfil actualizado.");
                },
                ppqhMessage -> PpqhToast.ppqhError(ppqhSaveProfileButton, ppqhMessage)));

        ppqhCurrentPasswordField.errorProperty().bind(ppqhViewModel.currentPasswordErrorProperty());
        ppqhNewPasswordField.errorProperty().bind(ppqhViewModel.newPasswordErrorProperty());
        ppqhConfirmationField.errorProperty().bind(ppqhViewModel.confirmationErrorProperty());
        ppqhNewPasswordField.setHint("Mínimo " + PpqhValidators.PPQH_PASSWORD_MIN + " caracteres, con letras y números.");
        ppqhChangePasswordButton.disableProperty().bind(ppqhViewModel.changingPasswordProperty());
        ppqhChangePasswordButton.setOnAction(ppqhE -> ppqhViewModel.ppqhChangePassword(
                ppqhCurrentPasswordField.getChars(), ppqhNewPasswordField.getChars(), ppqhConfirmationField.getChars(),
                () -> {
                    ppqhCurrentPasswordField.setText("");
                    ppqhNewPasswordField.setText("");
                    ppqhConfirmationField.setText("");
                    PpqhToast.ppqhSuccess(ppqhChangePasswordButton, "Contraseña actualizada.");
                },
                ppqhMessage -> PpqhToast.ppqhError(ppqhChangePasswordButton, ppqhMessage)));

        ppqhReduceMotionCheck.selectedProperty().bindBidirectional(ppqhContext.ppqhUiSettings().reduceMotionProperty());
    }
}
