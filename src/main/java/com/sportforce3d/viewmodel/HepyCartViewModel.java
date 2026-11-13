package com.sportforce3d.viewmodel;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.model.HepyCartSummary;
import com.sportforce3d.service.HepyCartService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class HepyCartViewModel {

    private final HepyCartService hepyCart;
    private final ObjectProperty<HepyCartSummary> hepySummary = new SimpleObjectProperty<>();
    private final BooleanProperty hepyBusy = new SimpleBooleanProperty(false);
    private final StringProperty hepyError = new SimpleStringProperty("");

    public HepyCartViewModel(HepyCartService hepyCart) {
        this.hepyCart = hepyCart;
    }

    public void hepyLoad() {
        hepyRun(hepyCart::hepySummary, hepyS -> { }, hepyMessage -> hepyError.set(hepyMessage));
    }

    public void hepyUpdateQuantity(long hepyItemId, int hepyQuantity, Consumer<String> hepyOnError) {
        hepyRun(() -> hepyCart.hepyUpdateQuantity(hepyItemId, hepyQuantity), hepyS -> { }, hepyOnError);
    }

    public void hepyRemove(long hepyItemId, Runnable hepyOnSuccess, Consumer<String> hepyOnError) {
        hepyRun(() -> hepyCart.hepyRemove(hepyItemId), hepyS -> hepyOnSuccess.run(), hepyOnError);
    }

    private void hepyRun(Callable<HepyCartSummary> hepyWork, Consumer<HepyCartSummary> hepyOnSuccess, Consumer<String> hepyOnError) {
        hepyBusy.set(true);
        hepyError.set("");
        PpqhAsync.ppqhRun(hepyWork,
                hepyS -> {
                    hepyBusy.set(false);
                    hepySummary.set(hepyS);
                    hepyOnSuccess.accept(hepyS);
                },
                hepyE -> {
                    hepyBusy.set(false);
                    hepyOnError.accept(PpqhErrorMessages.ppqhOf(hepyE));
                    PpqhAsync.ppqhRun(hepyCart::hepySummary, hepySummary::set, hepyIgnored -> { });
                });
    }

    public ObjectProperty<HepyCartSummary> summaryProperty() { return hepySummary; }
    public BooleanProperty busyProperty() { return hepyBusy; }
    public StringProperty errorProperty() { return hepyError; }
}
