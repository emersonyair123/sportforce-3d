package com.sportforce3d.viewmodel;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.service.PpqhAccountService;
import com.sportforce3d.service.PpqhSession;
import com.sportforce3d.service.PpqhValidationException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Map;
import java.util.function.Consumer;

public class PpqhAccountViewModel {

    private final PpqhAccountService ppqhAccount;

    private final ReadOnlyStringWrapper ppqhEmail = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper ppqhRoleLabel = new ReadOnlyStringWrapper("");
    private final StringProperty ppqhFullName = new SimpleStringProperty("");
    private final StringProperty ppqhPhone = new SimpleStringProperty("");
    private final StringProperty ppqhFullNameError = new SimpleStringProperty("");
    private final StringProperty ppqhPhoneError = new SimpleStringProperty("");
    private final BooleanProperty ppqhSavingProfile = new SimpleBooleanProperty(false);

    private final StringProperty ppqhCurrentPasswordError = new SimpleStringProperty("");
    private final StringProperty ppqhNewPasswordError = new SimpleStringProperty("");
    private final StringProperty ppqhConfirmationError = new SimpleStringProperty("");
    private final BooleanProperty ppqhChangingPassword = new SimpleBooleanProperty(false);

    public PpqhAccountViewModel(PpqhAccountService ppqhAccount, PpqhSession ppqhSession) {
        this.ppqhAccount = ppqhAccount;
        PpqhUser ppqhUser = ppqhSession.ppqhRequireUser();
        ppqhEmail.set(ppqhUser.ppqhEmail());
        ppqhRoleLabel.set(ppqhUser.ppqhRoles().stream().map(ppqhR -> ppqhR.ppqhLabel()).sorted().reduce((ppqhA, ppqhB) -> ppqhA + " · " + ppqhB).orElse(""));
        ppqhFullName.set(ppqhUser.ppqhFullName());
        ppqhPhone.set(ppqhUser.ppqhPhone() == null ? "" : ppqhUser.ppqhPhone());
    }

    public void ppqhSaveProfile(Consumer<PpqhUser> ppqhOnSuccess, Consumer<String> ppqhOnError) {
        ppqhSavingProfile.set(true);
        ppqhFullNameError.set("");
        ppqhPhoneError.set("");
        String ppqhName = ppqhFullName.get();
        String ppqhTel = ppqhPhone.get();
        PpqhAsync.ppqhRun(() -> ppqhAccount.ppqhUpdateProfile(ppqhName, ppqhTel),
                ppqhUser -> {
                    ppqhSavingProfile.set(false);
                    ppqhFullName.set(ppqhUser.ppqhFullName());
                    ppqhPhone.set(ppqhUser.ppqhPhone() == null ? "" : ppqhUser.ppqhPhone());
                    ppqhOnSuccess.accept(ppqhUser);
                },
                ppqhE -> {
                    ppqhSavingProfile.set(false);
                    if (ppqhE instanceof PpqhValidationException ppqhV) {
                        ppqhFullNameError.set(ppqhV.ppqhFieldErrors().getOrDefault("fullName", ""));
                        ppqhPhoneError.set(ppqhV.ppqhFieldErrors().getOrDefault("phone", ""));
                    } else {
                        ppqhOnError.accept(PpqhErrorMessages.ppqhOf(ppqhE));
                    }
                });
    }

    public void ppqhChangePassword(char[] ppqhCurrent, char[] ppqhNext, char[] ppqhConfirmation,
                               Runnable ppqhOnSuccess, Consumer<String> ppqhOnError) {
        ppqhChangingPassword.set(true);
        ppqhApplyPasswordErrors(Map.of());
        PpqhAsync.ppqhRun(() -> {
                    ppqhAccount.ppqhChangePassword(ppqhCurrent, ppqhNext, ppqhConfirmation);
                    return Boolean.TRUE;
                },
                ppqhOk -> {
                    ppqhChangingPassword.set(false);
                    ppqhOnSuccess.run();
                },
                ppqhE -> {
                    ppqhChangingPassword.set(false);
                    if (ppqhE instanceof PpqhValidationException ppqhV) {
                        ppqhApplyPasswordErrors(ppqhV.ppqhFieldErrors());
                    } else {
                        ppqhOnError.accept(PpqhErrorMessages.ppqhOf(ppqhE));
                    }
                });
    }

    private void ppqhApplyPasswordErrors(Map<String, String> ppqhErrors) {
        ppqhCurrentPasswordError.set(ppqhErrors.getOrDefault("current", ""));
        ppqhNewPasswordError.set(ppqhErrors.getOrDefault("next", ""));
        ppqhConfirmationError.set(ppqhErrors.getOrDefault("confirmation", ""));
    }

    public ReadOnlyStringProperty emailProperty() { return ppqhEmail.getReadOnlyProperty(); }
    public ReadOnlyStringProperty roleLabelProperty() { return ppqhRoleLabel.getReadOnlyProperty(); }
    public StringProperty fullNameProperty() { return ppqhFullName; }
    public StringProperty phoneProperty() { return ppqhPhone; }
    public StringProperty fullNameErrorProperty() { return ppqhFullNameError; }
    public StringProperty phoneErrorProperty() { return ppqhPhoneError; }
    public BooleanProperty savingProfileProperty() { return ppqhSavingProfile; }
    public StringProperty currentPasswordErrorProperty() { return ppqhCurrentPasswordError; }
    public StringProperty newPasswordErrorProperty() { return ppqhNewPasswordError; }
    public StringProperty confirmationErrorProperty() { return ppqhConfirmationError; }
    public BooleanProperty changingPasswordProperty() { return ppqhChangingPassword; }
}
