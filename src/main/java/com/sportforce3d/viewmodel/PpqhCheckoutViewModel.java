package com.sportforce3d.viewmodel;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.model.PpqhAddress;
import com.sportforce3d.model.HepyCartSummary;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.service.PpqhAddressService;
import com.sportforce3d.service.HepyCartService;
import com.sportforce3d.service.PpqhOrderService;
import com.sportforce3d.service.PpqhValidationException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PpqhCheckoutViewModel {

    private record PpqhLoaded(List<PpqhAddress> ppqhAddresses, HepyCartSummary ppqhSummary) {
    }

    private final PpqhAddressService ppqhAddressService;
    private final HepyCartService ppqhCartService;
    private final PpqhOrderService ppqhOrderService;

    private final ObservableList<PpqhAddress> ppqhAddresses = FXCollections.observableArrayList();
    private final ObjectProperty<Long> ppqhAddressId = new SimpleObjectProperty<>();
    private final ObjectProperty<PpqhPaymentMethod> ppqhMethod = new SimpleObjectProperty<>(PpqhPaymentMethod.YAPE);
    private final StringProperty ppqhNotes = new SimpleStringProperty("");
    private final ObjectProperty<HepyCartSummary> ppqhSummary = new SimpleObjectProperty<>();
    private final BooleanProperty ppqhPlacing = new SimpleBooleanProperty(false);
    private final StringProperty ppqhError = new SimpleStringProperty("");

    public PpqhCheckoutViewModel(PpqhAddressService ppqhAddressService, HepyCartService ppqhCartService, PpqhOrderService ppqhOrderService) {
        this.ppqhAddressService = ppqhAddressService;
        this.ppqhCartService = ppqhCartService;
        this.ppqhOrderService = ppqhOrderService;
    }

    public void ppqhLoad() {
        PpqhAsync.ppqhRun(() -> new PpqhLoaded(ppqhAddressService.ppqhList(), ppqhCartService.hepySummary()),
                ppqhLoaded -> {
                    ppqhSetAddresses(ppqhLoaded.ppqhAddresses(), null);
                    ppqhSummary.set(ppqhLoaded.ppqhSummary());
                },
                ppqhE -> ppqhError.set(PpqhErrorMessages.ppqhOf(ppqhE)));
    }

    private void ppqhSetAddresses(List<PpqhAddress> ppqhList, Long ppqhPrefer) {
        ppqhAddresses.setAll(ppqhList);
        Long ppqhKeep = ppqhPrefer != null ? ppqhPrefer : ppqhAddressId.get();
        boolean ppqhStillThere = ppqhKeep != null && ppqhList.stream().anyMatch(ppqhA -> ppqhA.ppqhId() == ppqhKeep);
        ppqhAddressId.set(ppqhStillThere ? ppqhKeep
                : ppqhList.stream().filter(PpqhAddress::ppqhIsDefault).map(PpqhAddress::ppqhId).findFirst()
                .orElse(ppqhList.isEmpty() ? null : ppqhList.get(0).ppqhId()));
    }

    public void ppqhSaveAddress(PpqhAddress ppqhForm, Runnable ppqhOnSuccess, Consumer<Map<String, String>> ppqhOnFieldErrors,
                            Consumer<String> ppqhOnError) {
        PpqhAsync.ppqhRun(() -> {
                    PpqhAddress ppqhSaved = ppqhAddressService.ppqhSave(ppqhForm);
                    return Map.entry(ppqhSaved, ppqhAddressService.ppqhList());
                },
                ppqhResult -> {
                    ppqhSetAddresses(ppqhResult.getValue(), ppqhResult.getKey().ppqhId());
                    ppqhOnSuccess.run();
                },
                ppqhE -> {
                    if (ppqhE instanceof PpqhValidationException ppqhV) {
                        ppqhOnFieldErrors.accept(ppqhV.ppqhFieldErrors());
                    } else {
                        ppqhOnError.accept(PpqhErrorMessages.ppqhOf(ppqhE));
                    }
                });
    }

    public void ppqhDeleteAddress(long ppqhId, Consumer<String> ppqhOnError) {
        PpqhAsync.ppqhRun(() -> {
                    ppqhAddressService.ppqhDelete(ppqhId);
                    return ppqhAddressService.ppqhList();
                },
                ppqhList -> ppqhSetAddresses(ppqhList, null),
                ppqhE -> ppqhOnError.accept(PpqhErrorMessages.ppqhOf(ppqhE)));
    }

    public void ppqhPlaceOrder(Consumer<PpqhOrderDetail> ppqhOnSuccess, Consumer<String> ppqhOnError) {
        if (ppqhAddressId.get() == null) {
            ppqhOnError.accept("Agrega o elige una dirección de envío.");
            return;
        }
        ppqhPlacing.set(true);
        long ppqhAddress = ppqhAddressId.get();
        PpqhPaymentMethod ppqhM = ppqhMethod.get();
        String ppqhN = ppqhNotes.get();
        PpqhAsync.ppqhRun(() -> ppqhOrderService.ppqhPlaceOrder(ppqhAddress, ppqhM, ppqhN),
                ppqhOrder -> {
                    ppqhPlacing.set(false);
                    ppqhOnSuccess.accept(ppqhOrder);
                },
                ppqhE -> {
                    ppqhPlacing.set(false);
                    ppqhOnError.accept(PpqhErrorMessages.ppqhOf(ppqhE));
                    PpqhAsync.ppqhRun(ppqhCartService::hepySummary, ppqhSummary::set, ppqhIgnored -> { });
                });
    }

    public ObservableList<PpqhAddress> ppqhAddresses() { return ppqhAddresses; }
    public ObjectProperty<Long> addressIdProperty() { return ppqhAddressId; }
    public ObjectProperty<PpqhPaymentMethod> methodProperty() { return ppqhMethod; }
    public StringProperty notesProperty() { return ppqhNotes; }
    public ObjectProperty<HepyCartSummary> summaryProperty() { return ppqhSummary; }
    public BooleanProperty placingProperty() { return ppqhPlacing; }
    public StringProperty errorProperty() { return ppqhError; }
}
