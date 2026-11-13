package com.sportforce3d.viewmodel;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.model.HepyCategory;
import com.sportforce3d.model.HepyProductSummary;
import com.sportforce3d.service.HepyCatalogService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class HepyHomeViewModel {

    private record HepyHomeData(List<HepyCategory> hepyCategories, List<HepyProductSummary> hepyFeatured) {
    }

    private final HepyCatalogService hepyCatalog;
    private final ObservableList<HepyCategory> hepyCategories = FXCollections.observableArrayList();
    private final ObservableList<HepyProductSummary> hepyFeatured = FXCollections.observableArrayList();
    private final BooleanProperty hepyLoading = new SimpleBooleanProperty(false);
    private final StringProperty hepyError = new SimpleStringProperty("");

    public HepyHomeViewModel(HepyCatalogService hepyCatalog) {
        this.hepyCatalog = hepyCatalog;
    }

    public void hepyLoad() {
        hepyLoading.set(true);
        hepyError.set("");
        PpqhAsync.ppqhRun(() -> new HepyHomeData(hepyCatalog.hepyCategories(), hepyCatalog.hepyFeatured(8)),
                hepyData -> {
                    hepyCategories.setAll(hepyData.hepyCategories());
                    hepyFeatured.setAll(hepyData.hepyFeatured());
                    hepyLoading.set(false);
                },
                hepyE -> {
                    hepyLoading.set(false);
                    hepyError.set(PpqhErrorMessages.ppqhOf(hepyE));
                });
    }

    public ObservableList<HepyCategory> hepyCategories() {
        return hepyCategories;
    }

    public ObservableList<HepyProductSummary> hepyFeatured() {
        return hepyFeatured;
    }

    public BooleanProperty loadingProperty() {
        return hepyLoading;
    }

    public StringProperty errorProperty() {
        return hepyError;
    }
}
