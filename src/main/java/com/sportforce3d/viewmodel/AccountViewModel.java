package com.sportforce3d.viewmodel;

import com.sportforce3d.app.Async;
import com.sportforce3d.model.User;
import com.sportforce3d.service.AccountService;
import com.sportforce3d.service.Session;
import com.sportforce3d.service.ValidationException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Map;
import java.util.function.Consumer;

public class AccountViewModel {

    private final AccountService account;

    private final ReadOnlyStringWrapper email = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper roleLabel = new ReadOnlyStringWrapper("");
    private final StringProperty fullName = new SimpleStringProperty("");
    private final StringProperty phone = new SimpleStringProperty("");
    private final StringProperty fullNameError = new SimpleStringProperty("");
    private final StringProperty phoneError = new SimpleStringProperty("");
    private final BooleanProperty savingProfile = new SimpleBooleanProperty(false);

    private final StringProperty currentPasswordError = new SimpleStringProperty("");
    private final StringProperty newPasswordError = new SimpleStringProperty("");
    private final StringProperty confirmationError = new SimpleStringProperty("");
    private final BooleanProperty changingPassword = new SimpleBooleanProperty(false);

    public AccountViewModel(AccountService account, Session session) {
        this.account = account;
        User user = session.requireUser();
        email.set(user.email());
        roleLabel.set(user.roles().stream().map(r -> r.label()).sorted().reduce((a, b) -> a + " · " + b).orElse(""));
        fullName.set(user.fullName());
        phone.set(user.phone() == null ? "" : user.phone());
    }

    public void saveProfile(Consumer<User> onSuccess, Consumer<String> onError) {
        savingProfile.set(true);
        fullNameError.set("");
        phoneError.set("");
        String name = fullName.get();
        String tel = phone.get();
        Async.run(() -> account.updateProfile(name, tel),
                user -> {
                    savingProfile.set(false);
                    fullName.set(user.fullName());
                    phone.set(user.phone() == null ? "" : user.phone());
                    onSuccess.accept(user);
                },
                e -> {
                    savingProfile.set(false);
                    if (e instanceof ValidationException v) {
                        fullNameError.set(v.fieldErrors().getOrDefault("fullName", ""));
                        phoneError.set(v.fieldErrors().getOrDefault("phone", ""));
                    } else {
                        onError.accept(ErrorMessages.of(e));
                    }
                });
    }

    public void changePassword(char[] current, char[] next, char[] confirmation,
                               Runnable onSuccess, Consumer<String> onError) {
        changingPassword.set(true);
        applyPasswordErrors(Map.of());
        Async.run(() -> {
                    account.changePassword(current, next, confirmation);
                    return Boolean.TRUE;
                },
                ok -> {
                    changingPassword.set(false);
                    onSuccess.run();
                },
                e -> {
                    changingPassword.set(false);
                    if (e instanceof ValidationException v) {
                        applyPasswordErrors(v.fieldErrors());
                    } else {
                        onError.accept(ErrorMessages.of(e));
                    }
                });
    }

    private void applyPasswordErrors(Map<String, String> errors) {
        currentPasswordError.set(errors.getOrDefault("current", ""));
        newPasswordError.set(errors.getOrDefault("next", ""));
        confirmationError.set(errors.getOrDefault("confirmation", ""));
    }

    public ReadOnlyStringProperty emailProperty() { return email.getReadOnlyProperty(); }
    public ReadOnlyStringProperty roleLabelProperty() { return roleLabel.getReadOnlyProperty(); }
    public StringProperty fullNameProperty() { return fullName; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty fullNameErrorProperty() { return fullNameError; }
    public StringProperty phoneErrorProperty() { return phoneError; }
    public BooleanProperty savingProfileProperty() { return savingProfile; }
    public StringProperty currentPasswordErrorProperty() { return currentPasswordError; }
    public StringProperty newPasswordErrorProperty() { return newPasswordError; }
    public StringProperty confirmationErrorProperty() { return confirmationError; }
    public BooleanProperty changingPasswordProperty() { return changingPassword; }
}
