package com.sportforce3d.viewmodel;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.service.PpqhAuthService;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.function.Consumer;

public class PpqhLoginViewModel {

    private final PpqhAuthService ppqhAuth;

    private final StringProperty ppqhEmail = new SimpleStringProperty("");
    private final StringProperty ppqhError = new SimpleStringProperty("");
    private final BooleanProperty ppqhBusy = new SimpleBooleanProperty(false);
    private final BooleanBinding ppqhCanSubmit = ppqhEmail.isNotEmpty().and(ppqhBusy.not());

    public PpqhLoginViewModel(PpqhAuthService ppqhAuth) {
        this.ppqhAuth = ppqhAuth;
    }

    public void ppqhLogin(char[] ppqhPassword, Consumer<PpqhUser> ppqhOnSuccess, Runnable ppqhOnFailure) {
        if (ppqhBusy.get()) {
            return;
        }
        ppqhBusy.set(true);
        ppqhError.set("");
        String ppqhEmailValue = ppqhEmail.get();
        PpqhAsync.ppqhRun(() -> ppqhAuth.ppqhLogin(ppqhEmailValue, ppqhPassword),
                ppqhUser -> {
                    ppqhBusy.set(false);
                    ppqhOnSuccess.accept(ppqhUser);
                },
                ppqhE -> {
                    ppqhBusy.set(false);
                    ppqhError.set(PpqhErrorMessages.ppqhOf(ppqhE));
                    ppqhOnFailure.run();
                });
    }

    public StringProperty emailProperty() {
        return ppqhEmail;
    }

    public StringProperty errorProperty() {
        return ppqhError;
    }

    public BooleanProperty busyProperty() {
        return ppqhBusy;
    }

    public BooleanBinding canSubmitProperty() {
        return ppqhCanSubmit;
    }
}
