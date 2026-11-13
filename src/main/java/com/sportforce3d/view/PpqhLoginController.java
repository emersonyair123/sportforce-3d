package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfLink;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.viewmodel.PpqhLoginViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.feather.Feather;

public class PpqhLoginController {

    private final PpqhAppContext ppqhContext;
    private final PpqhLoginViewModel ppqhViewModel;

    @FXML private HBox ppqhErrorBox;
    @FXML private Label ppqhErrorLabel;
    @FXML private PpqhSfTextField ppqhEmailField;
    @FXML private PpqhSfTextField ppqhPasswordField;
    @FXML private PpqhSfButton ppqhLoginButton;
    @FXML private PpqhSfLink ppqhRegisterLink;

    public PpqhLoginController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
        this.ppqhViewModel = new PpqhLoginViewModel(ppqhContext.ppqhAuthService());
    }

    @FXML
    private void initialize() {
        ppqhEmailField.textProperty().bindBidirectional(ppqhViewModel.emailProperty());
        ppqhErrorLabel.textProperty().bind(ppqhViewModel.errorProperty());
        ppqhErrorBox.getChildren().add(0, PpqhIcons.ppqhOf(Feather.ALERT_CIRCLE, PpqhIcons.PPQH_MD));
        ppqhErrorBox.visibleProperty().bind(ppqhViewModel.errorProperty().isNotEmpty());
        ppqhErrorBox.managedProperty().bind(ppqhErrorBox.visibleProperty());

        ppqhLoginButton.disableProperty().bind(ppqhViewModel.canSubmitProperty().not());
        ppqhLoginButton.textProperty().bind(javafx.beans.binding.Bindings.when(ppqhViewModel.busyProperty())
                .then("Ingresando…").otherwise("Iniciar sesión"));
        ppqhLoginButton.setOnAction(ppqhE -> ppqhSubmit());
        ppqhPasswordField.getInput().setOnAction(ppqhE -> ppqhSubmit());
        ppqhRegisterLink.setOnAction(ppqhE -> ppqhContext.ppqhNavigator().ppqhShowRegister());

        Platform.runLater(ppqhEmailField::requestFocus);
    }

    private void ppqhSubmit() {
        if (!ppqhViewModel.canSubmitProperty().get()) {
            return;
        }
        char[] ppqhPassword = ppqhPasswordField.getChars();
        ppqhViewModel.ppqhLogin(ppqhPassword,
                ppqhUser -> {
                    ppqhPasswordField.setText("");
                    ppqhContext.ppqhNavigator().ppqhShowShell();
                },
                () -> {
                    ppqhPasswordField.setText("");
                    ppqhPasswordField.requestFocus();
                });
    }
}
