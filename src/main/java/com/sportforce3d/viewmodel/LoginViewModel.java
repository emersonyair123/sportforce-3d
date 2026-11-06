package com.sportforce3d.viewmodel;

import com.sportforce3d.app.Async;
import com.sportforce3d.model.User;
import com.sportforce3d.service.AuthService;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.function.Consumer;

public class LoginViewModel {

    private final AuthService auth;

    private final StringProperty email = new SimpleStringProperty("");
    private final StringProperty error = new SimpleStringProperty("");
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final BooleanBinding canSubmit = email.isNotEmpty().and(busy.not());

    public LoginViewModel(AuthService auth) {
        this.auth = auth;
    }

    /** BCrypt tarda ~250 ms: se ejecuta fuera del hilo de la interfaz. */
    public void login(char[] password, Consumer<User> onSuccess, Runnable onFailure) {
        if (busy.get()) {
            return;
        }
        busy.set(true);
        error.set("");
        String emailValue = email.get();
        Async.run(() -> auth.login(emailValue, password),
                user -> {
                    busy.set(false);
                    onSuccess.accept(user);
                },
                e -> {
                    busy.set(false);
                    error.set(ErrorMessages.of(e));
                    onFailure.run();
                });
    }

    public StringProperty emailProperty() {
        return email;
    }

    public StringProperty errorProperty() {
        return error;
    }

    public BooleanProperty busyProperty() {
        return busy;
    }

    public BooleanBinding canSubmitProperty() {
        return canSubmit;
    }
}
