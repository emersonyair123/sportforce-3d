package com.sportforce3d.viewmodel;

import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.service.PpqhSession;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class PpqhShellViewModel {

    private final PpqhSession ppqhSession;
    private final ReadOnlyStringWrapper ppqhDisplayName = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper ppqhInitials = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper ppqhAdmin = new ReadOnlyBooleanWrapper(false);

    public PpqhShellViewModel(PpqhSession ppqhSession) {
        this.ppqhSession = ppqhSession;
        ppqhRefresh();
    }

    public void ppqhRefresh() {
        PpqhUser ppqhUser = ppqhSession.ppqhRequireUser();
        ppqhDisplayName.set(ppqhUser.ppqhFirstName());
        ppqhInitials.set(ppqhInitialsOf(ppqhUser.ppqhFullName()));
        ppqhAdmin.set(ppqhUser.ppqhIsAdmin());
    }

    static String ppqhInitialsOf(String ppqhFullName) {
        String ppqhResult = Arrays.stream(ppqhFullName.trim().split("\\s+"))
                .filter(ppqhS -> !ppqhS.isEmpty())
                .limit(2)
                .map(ppqhS -> ppqhS.substring(0, 1))
                .collect(Collectors.joining());
        return ppqhResult.toUpperCase(Locale.forLanguageTag("es-PE"));
    }

    public ReadOnlyStringProperty displayNameProperty() {
        return ppqhDisplayName.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty initialsProperty() {
        return ppqhInitials.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty adminProperty() {
        return ppqhAdmin.getReadOnlyProperty();
    }

    public boolean isAdmin() {
        return ppqhAdmin.get();
    }
}
