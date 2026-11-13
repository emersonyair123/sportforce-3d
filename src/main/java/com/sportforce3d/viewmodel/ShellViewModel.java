package com.sportforce3d.viewmodel;

import com.sportforce3d.model.User;
import com.sportforce3d.service.Session;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class ShellViewModel {

    private final Session session;
    private final ReadOnlyStringWrapper displayName = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper initials = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper admin = new ReadOnlyBooleanWrapper(false);

    public ShellViewModel(Session session) {
        this.session = session;
        refresh();
    }

    public void refresh() {
        User user = session.requireUser();
        displayName.set(user.firstName());
        initials.set(initialsOf(user.fullName()));
        admin.set(user.isAdmin());
    }

    static String initialsOf(String fullName) {
        String result = Arrays.stream(fullName.trim().split("\\s+"))
                .filter(s -> !s.isEmpty())
                .limit(2)
                .map(s -> s.substring(0, 1))
                .collect(Collectors.joining());
        return result.toUpperCase(Locale.forLanguageTag("es-PE"));
    }

    public ReadOnlyStringProperty displayNameProperty() {
        return displayName.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty initialsProperty() {
        return initials.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty adminProperty() {
        return admin.getReadOnlyProperty();
    }

    public boolean isAdmin() {
        return admin.get();
    }
}
