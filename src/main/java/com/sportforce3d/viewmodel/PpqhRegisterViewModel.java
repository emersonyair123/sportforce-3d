package com.sportforce3d.viewmodel;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.service.PpqhAuthService;
import com.sportforce3d.service.PpqhValidationException;
import com.sportforce3d.util.PpqhValidators;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

public class PpqhRegisterViewModel {

    private final PpqhAuthService ppqhAuth;

    private final StringProperty ppqhFullName = new SimpleStringProperty("");
    private final StringProperty ppqhEmail = new SimpleStringProperty("");
    private final StringProperty ppqhPhone = new SimpleStringProperty("");

    private final StringProperty ppqhFullNameError = new SimpleStringProperty("");
    private final StringProperty ppqhEmailError = new SimpleStringProperty("");
    private final StringProperty ppqhPhoneError = new SimpleStringProperty("");
    private final StringProperty ppqhPasswordError = new SimpleStringProperty("");
    private final StringProperty ppqhConfirmationError = new SimpleStringProperty("");
    private final StringProperty ppqhError = new SimpleStringProperty("");
    private final BooleanProperty ppqhBusy = new SimpleBooleanProperty(false);

    public PpqhRegisterViewModel(PpqhAuthService ppqhAuth) {
        this.ppqhAuth = ppqhAuth;
    }

    public void ppqhRegister(char[] ppqhPassword, char[] ppqhConfirmation, Consumer<PpqhUser> ppqhOnSuccess) {
        if (ppqhBusy.get()) {
            return;
        }
        ppqhClearErrors();
        ppqhFullNameError.set(ppqhOrEmpty(PpqhValidators.ppqhFullName(ppqhFullName.get())));
        ppqhEmailError.set(ppqhOrEmpty(PpqhValidators.ppqhEmail(ppqhEmail.get())));
        ppqhPhoneError.set(ppqhOrEmpty(PpqhValidators.ppqhPhone(ppqhPhone.get())));
        ppqhPasswordError.set(ppqhOrEmpty(PpqhValidators.ppqhNewPassword(ppqhPassword)));
        if (!Arrays.equals(ppqhPassword, ppqhConfirmation)) {
            ppqhConfirmationError.set("Las contraseñas no coinciden.");
        }
        if (ppqhHasFieldErrors()) {
            Arrays.fill(ppqhPassword, '\0');
            Arrays.fill(ppqhConfirmation, '\0');
            return;
        }

        ppqhBusy.set(true);
        String ppqhName = ppqhFullName.get();
        String ppqhMail = ppqhEmail.get();
        String ppqhTel = ppqhPhone.get();
        PpqhAsync.ppqhRun(() -> ppqhAuth.ppqhRegister(ppqhName, ppqhMail, ppqhTel, ppqhPassword, ppqhConfirmation),
                ppqhUser -> {
                    ppqhBusy.set(false);
                    ppqhOnSuccess.accept(ppqhUser);
                },
                ppqhE -> {
                    ppqhBusy.set(false);
                    if (ppqhE instanceof PpqhValidationException ppqhV) {
                        ppqhApply(ppqhV.ppqhFieldErrors());
                    } else {
                        ppqhError.set(PpqhErrorMessages.ppqhOf(ppqhE));
                    }
                });
    }

    private void ppqhApply(Map<String, String> ppqhErrors) {
        ppqhFullNameError.set(ppqhErrors.getOrDefault("fullName", ""));
        ppqhEmailError.set(ppqhErrors.getOrDefault("email", ""));
        ppqhPhoneError.set(ppqhErrors.getOrDefault("phone", ""));
        ppqhPasswordError.set(ppqhErrors.getOrDefault("password", ""));
        ppqhConfirmationError.set(ppqhErrors.getOrDefault("confirmation", ""));
    }

    private boolean ppqhHasFieldErrors() {
        return !ppqhFullNameError.get().isEmpty() || !ppqhEmailError.get().isEmpty() || !ppqhPhoneError.get().isEmpty()
                || !ppqhPasswordError.get().isEmpty() || !ppqhConfirmationError.get().isEmpty();
    }

    private void ppqhClearErrors() {
        ppqhApply(Map.of());
        ppqhError.set("");
    }

    private static String ppqhOrEmpty(String ppqhS) {
        return ppqhS == null ? "" : ppqhS;
    }

    public StringProperty fullNameProperty() { return ppqhFullName; }
    public StringProperty emailProperty() { return ppqhEmail; }
    public StringProperty phoneProperty() { return ppqhPhone; }
    public StringProperty fullNameErrorProperty() { return ppqhFullNameError; }
    public StringProperty emailErrorProperty() { return ppqhEmailError; }
    public StringProperty phoneErrorProperty() { return ppqhPhoneError; }
    public StringProperty passwordErrorProperty() { return ppqhPasswordError; }
    public StringProperty confirmationErrorProperty() { return ppqhConfirmationError; }
    public StringProperty errorProperty() { return ppqhError; }
    public BooleanProperty busyProperty() { return ppqhBusy; }
}
