package com.sportforce3d.viewmodel;

import com.sportforce3d.app.Async;
import com.sportforce3d.model.User;
import com.sportforce3d.service.AuthService;
import com.sportforce3d.service.ValidationException;
import com.sportforce3d.util.Validators;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

public class RegisterViewModel {

    private final AuthService auth;

    private final StringProperty fullName = new SimpleStringProperty("");
    private final StringProperty email = new SimpleStringProperty("");
    private final StringProperty phone = new SimpleStringProperty("");

    private final StringProperty fullNameError = new SimpleStringProperty("");
    private final StringProperty emailError = new SimpleStringProperty("");
    private final StringProperty phoneError = new SimpleStringProperty("");
    private final StringProperty passwordError = new SimpleStringProperty("");
    private final StringProperty confirmationError = new SimpleStringProperty("");
    private final StringProperty error = new SimpleStringProperty("");
    private final BooleanProperty busy = new SimpleBooleanProperty(false);

    public RegisterViewModel(AuthService auth) {
        this.auth = auth;
    }

    public void register(char[] password, char[] confirmation, Consumer<User> onSuccess) {
        if (busy.get()) {
            return;
        }
        clearErrors();
        fullNameError.set(orEmpty(Validators.fullName(fullName.get())));
        emailError.set(orEmpty(Validators.email(email.get())));
        phoneError.set(orEmpty(Validators.phone(phone.get())));
        passwordError.set(orEmpty(Validators.newPassword(password)));
        if (!Arrays.equals(password, confirmation)) {
            confirmationError.set("Las contraseñas no coinciden.");
        }
        if (hasFieldErrors()) {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            return;
        }

        busy.set(true);
        String name = fullName.get();
        String mail = email.get();
        String tel = phone.get();
        Async.run(() -> auth.register(name, mail, tel, password, confirmation),
                user -> {
                    busy.set(false);
                    onSuccess.accept(user);
                },
                e -> {
                    busy.set(false);
                    if (e instanceof ValidationException v) {
                        apply(v.fieldErrors());
                    } else {
                        error.set(ErrorMessages.of(e));
                    }
                });
    }

    private void apply(Map<String, String> errors) {
        fullNameError.set(errors.getOrDefault("fullName", ""));
        emailError.set(errors.getOrDefault("email", ""));
        phoneError.set(errors.getOrDefault("phone", ""));
        passwordError.set(errors.getOrDefault("password", ""));
        confirmationError.set(errors.getOrDefault("confirmation", ""));
    }

    private boolean hasFieldErrors() {
        return !fullNameError.get().isEmpty() || !emailError.get().isEmpty() || !phoneError.get().isEmpty()
                || !passwordError.get().isEmpty() || !confirmationError.get().isEmpty();
    }

    private void clearErrors() {
        apply(Map.of());
        error.set("");
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    public StringProperty fullNameProperty() { return fullName; }
    public StringProperty emailProperty() { return email; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty fullNameErrorProperty() { return fullNameError; }
    public StringProperty emailErrorProperty() { return emailError; }
    public StringProperty phoneErrorProperty() { return phoneError; }
    public StringProperty passwordErrorProperty() { return passwordError; }
    public StringProperty confirmationErrorProperty() { return confirmationError; }
    public StringProperty errorProperty() { return error; }
    public BooleanProperty busyProperty() { return busy; }
}
