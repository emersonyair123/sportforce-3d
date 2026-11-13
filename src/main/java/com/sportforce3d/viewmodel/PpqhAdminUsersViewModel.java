package com.sportforce3d.viewmodel;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.service.PpqhUserAdminService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.Locale;
import java.util.function.Consumer;

public class PpqhAdminUsersViewModel {

    private final PpqhUserAdminService ppqhService;
    private final ObservableList<PpqhUser> ppqhUsers = FXCollections.observableArrayList();
    private final FilteredList<PpqhUser> ppqhFiltered = new FilteredList<>(ppqhUsers, ppqhU -> true);
    private final StringProperty ppqhFilter = new SimpleStringProperty("");
    private final BooleanProperty ppqhLoading = new SimpleBooleanProperty(false);
    private final StringProperty ppqhError = new SimpleStringProperty("");

    public PpqhAdminUsersViewModel(PpqhUserAdminService ppqhService) {
        this.ppqhService = ppqhService;
        ppqhFilter.addListener((ppqhObs, ppqhOld, ppqhText) -> {
            String ppqhQ = ppqhText == null ? "" : ppqhText.trim().toLowerCase(Locale.ROOT);
            ppqhFiltered.setPredicate(ppqhU -> ppqhQ.isEmpty()
                    || ppqhU.ppqhFullName().toLowerCase(Locale.ROOT).contains(ppqhQ)
                    || ppqhU.ppqhEmail().toLowerCase(Locale.ROOT).contains(ppqhQ));
        });
    }

    public void ppqhLoad() {
        ppqhLoading.set(true);
        ppqhError.set("");
        PpqhAsync.ppqhRun(ppqhService::ppqhListUsers,
                ppqhList -> {
                    ppqhUsers.setAll(ppqhList);
                    ppqhLoading.set(false);
                },
                ppqhE -> {
                    ppqhLoading.set(false);
                    ppqhUsers.clear();
                    ppqhError.set(PpqhErrorMessages.ppqhOf(ppqhE));
                });
    }

    public void ppqhSetActive(PpqhUser ppqhUser, boolean ppqhActive, Consumer<PpqhUser> ppqhOnSuccess, Consumer<String> ppqhOnError) {
        PpqhAsync.ppqhRun(() -> ppqhService.ppqhSetActive(ppqhUser.ppqhId(), ppqhActive), ppqhUpdated -> {
            ppqhReplace(ppqhUpdated);
            ppqhOnSuccess.accept(ppqhUpdated);
        }, ppqhE -> ppqhOnError.accept(PpqhErrorMessages.ppqhOf(ppqhE)));
    }

    public void ppqhSetAdmin(PpqhUser ppqhUser, boolean ppqhAdmin, Consumer<PpqhUser> ppqhOnSuccess, Consumer<String> ppqhOnError) {
        PpqhAsync.ppqhRun(() -> ppqhService.ppqhSetAdmin(ppqhUser.ppqhId(), ppqhAdmin), ppqhUpdated -> {
            ppqhReplace(ppqhUpdated);
            ppqhOnSuccess.accept(ppqhUpdated);
        }, ppqhE -> ppqhOnError.accept(PpqhErrorMessages.ppqhOf(ppqhE)));
    }

    private void ppqhReplace(PpqhUser ppqhUpdated) {
        for (int ppqhI = 0; ppqhI < ppqhUsers.size(); ppqhI++) {
            if (ppqhUsers.get(ppqhI).ppqhId() == ppqhUpdated.ppqhId()) {
                ppqhUsers.set(ppqhI, ppqhUpdated);
                return;
            }
        }
    }

    public ObservableList<PpqhUser> ppqhFilteredUsers() {
        return ppqhFiltered;
    }

    public StringProperty filterProperty() {
        return ppqhFilter;
    }

    public BooleanProperty loadingProperty() {
        return ppqhLoading;
    }

    public StringProperty errorProperty() {
        return ppqhError;
    }
}
