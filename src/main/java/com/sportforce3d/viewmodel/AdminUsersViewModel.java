package com.sportforce3d.viewmodel;

import com.sportforce3d.app.Async;
import com.sportforce3d.model.User;
import com.sportforce3d.service.UserAdminService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.Locale;
import java.util.function.Consumer;

public class AdminUsersViewModel {

    private final UserAdminService service;
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final FilteredList<User> filtered = new FilteredList<>(users, u -> true);
    private final StringProperty filter = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");

    public AdminUsersViewModel(UserAdminService service) {
        this.service = service;
        filter.addListener((obs, old, text) -> {
            String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(u -> q.isEmpty()
                    || u.fullName().toLowerCase(Locale.ROOT).contains(q)
                    || u.email().toLowerCase(Locale.ROOT).contains(q));
        });
    }

    public void load() {
        loading.set(true);
        error.set("");
        Async.run(service::listUsers,
                list -> {
                    users.setAll(list);
                    loading.set(false);
                },
                e -> {
                    loading.set(false);
                    users.clear();
                    error.set(ErrorMessages.of(e));
                });
    }

    public void setActive(User user, boolean active, Consumer<User> onSuccess, Consumer<String> onError) {
        Async.run(() -> service.setActive(user.id(), active), updated -> {
            replace(updated);
            onSuccess.accept(updated);
        }, e -> onError.accept(ErrorMessages.of(e)));
    }

    public void setAdmin(User user, boolean admin, Consumer<User> onSuccess, Consumer<String> onError) {
        Async.run(() -> service.setAdmin(user.id(), admin), updated -> {
            replace(updated);
            onSuccess.accept(updated);
        }, e -> onError.accept(ErrorMessages.of(e)));
    }

    private void replace(User updated) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).id() == updated.id()) {
                users.set(i, updated);
                return;
            }
        }
    }

    public ObservableList<User> filteredUsers() {
        return filtered;
    }

    public StringProperty filterProperty() {
        return filter;
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public StringProperty errorProperty() {
        return error;
    }
}
