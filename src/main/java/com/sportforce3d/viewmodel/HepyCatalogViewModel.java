package com.sportforce3d.viewmodel;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.model.HepyCategory;
import com.sportforce3d.model.HepyPage;
import com.sportforce3d.model.HepyProductQuery;
import com.sportforce3d.model.HepyProductSort;
import com.sportforce3d.model.HepyProductSummary;
import com.sportforce3d.model.HepySize;
import com.sportforce3d.model.HepySport;
import com.sportforce3d.service.HepyCatalogService;
import com.sportforce3d.util.HepyMoney;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.util.List;
import java.util.Set;

public class HepyCatalogViewModel {

    private record HepyReferenceData(List<HepyCategory> hepyCategories, List<HepySport> hepySports, List<HepySize> hepySizes) {
    }

    private final HepyCatalogService hepyCatalog;

    private final StringProperty hepyText = new SimpleStringProperty("");
    private final ObjectProperty<Long> hepyCategoryId = new SimpleObjectProperty<>();
    private final ObjectProperty<Long> hepySportId = new SimpleObjectProperty<>();
    private final ObservableSet<String> hepySizeCodes = FXCollections.observableSet();
    private final StringProperty hepyMinPrice = new SimpleStringProperty("");
    private final StringProperty hepyMaxPrice = new SimpleStringProperty("");
    private final BooleanProperty hepyInStockOnly = new SimpleBooleanProperty(false);
    private final BooleanProperty hepyCustomizableOnly = new SimpleBooleanProperty(false);
    private final ObjectProperty<HepyProductSort> hepySort = new SimpleObjectProperty<>(HepyProductSort.RELEVANCE);

    private final ObservableList<HepyCategory> hepyCategories = FXCollections.observableArrayList();
    private final ObservableList<HepySport> hepySports = FXCollections.observableArrayList();
    private final ObservableList<HepySize> hepySizes = FXCollections.observableArrayList();
    private final ObjectProperty<HepyPage<HepyProductSummary>> hepyResult = new SimpleObjectProperty<>();
    private final BooleanProperty hepyLoading = new SimpleBooleanProperty(false);
    private final StringProperty hepyError = new SimpleStringProperty("");
    private final StringProperty hepyPriceError = new SimpleStringProperty("");
    private int hepyRequestId;

    public HepyCatalogViewModel(HepyCatalogService hepyCatalog) {
        this.hepyCatalog = hepyCatalog;
    }

    public void start(Runnable hepyOnReferenceData) {
        PpqhAsync.ppqhRun(() -> new HepyReferenceData(hepyCatalog.hepyCategories(), hepyCatalog.hepySports(), hepyCatalog.hepySizes()),
                hepyData -> {
                    hepyCategories.setAll(hepyData.hepyCategories());
                    hepySports.setAll(hepyData.hepySports());
                    hepySizes.setAll(hepyData.hepySizes());
                    hepyOnReferenceData.run();
                    hepySearch(1);
                },
                hepyE -> hepyError.set(PpqhErrorMessages.ppqhOf(hepyE)));
    }

    public void hepySearch(int hepyPage) {
        HepyProductQuery hepyQuery;
        try {
            hepyQuery = hepyBuildQuery(hepyPage);
        } catch (IllegalArgumentException hepyE) {
            return;
        }
        int hepyId = ++hepyRequestId;
        hepyLoading.set(true);
        hepyError.set("");
        PpqhAsync.ppqhRun(() -> hepyCatalog.hepySearch(hepyQuery),
                hepyPage1 -> {
                    if (hepyId == hepyRequestId) {
                        hepyResult.set(hepyPage1);
                        hepyLoading.set(false);
                    }
                },
                hepyE -> {
                    if (hepyId == hepyRequestId) {
                        hepyLoading.set(false);
                        hepyError.set(PpqhErrorMessages.ppqhOf(hepyE));
                    }
                });
    }

    HepyProductQuery hepyBuildQuery(int hepyPage) {
        hepyPriceError.set("");
        Long hepyMin = hepyParsePrice(hepyMinPrice.get());
        Long hepyMax = hepyParsePrice(hepyMaxPrice.get());
        if (hepyMin != null && hepyMax != null && hepyMin > hepyMax) {
            hepyPriceError.set("El mínimo no puede ser mayor que el máximo.");
            throw new IllegalArgumentException("rango de precio");
        }
        String hepyQ = hepyText.get() == null || hepyText.get().isBlank() ? null : hepyText.get().trim();
        return new HepyProductQuery(hepyQ, hepyCategoryId.get(), hepySportId.get(), Set.copyOf(hepySizeCodes), hepyMin, hepyMax,
                hepyInStockOnly.get(), hepyCustomizableOnly.get(), hepySort.get(), hepyPage, HepyProductQuery.HEPY_DEFAULT_PAGE_SIZE);
    }

    private Long hepyParsePrice(String hepyValue) {
        if (hepyValue == null || hepyValue.isBlank()) {
            return null;
        }
        try {
            return HepyMoney.hepyParse(hepyValue);
        } catch (IllegalArgumentException hepyE) {
            hepyPriceError.set("Escribe montos como 79.90");
            throw hepyE;
        }
    }

    public void hepyClearFilters() {
        hepyText.set("");
        hepyCategoryId.set(null);
        hepySportId.set(null);
        hepySizeCodes.clear();
        hepyMinPrice.set("");
        hepyMaxPrice.set("");
        hepyInStockOnly.set(false);
        hepyCustomizableOnly.set(false);
        hepySort.set(HepyProductSort.RELEVANCE);
        hepySearch(1);
    }

    public String hepyCategoryName() {
        return hepyCategories.stream().filter(hepyC -> hepyC.hepyId() == (hepyCategoryId.get() == null ? -1 : hepyCategoryId.get()))
                .map(HepyCategory::hepyName).findFirst().orElse(null);
    }

    public StringProperty textProperty() { return hepyText; }
    public ObjectProperty<Long> categoryIdProperty() { return hepyCategoryId; }
    public ObjectProperty<Long> sportIdProperty() { return hepySportId; }
    public ObservableSet<String> hepySizeCodes() { return hepySizeCodes; }
    public StringProperty minPriceProperty() { return hepyMinPrice; }
    public StringProperty maxPriceProperty() { return hepyMaxPrice; }
    public BooleanProperty inStockOnlyProperty() { return hepyInStockOnly; }
    public BooleanProperty customizableOnlyProperty() { return hepyCustomizableOnly; }
    public ObjectProperty<HepyProductSort> sortProperty() { return hepySort; }
    public ObservableList<HepyCategory> hepyCategories() { return hepyCategories; }
    public ObservableList<HepySport> hepySports() { return hepySports; }
    public ObservableList<HepySize> hepySizes() { return hepySizes; }
    public ObjectProperty<HepyPage<HepyProductSummary>> resultProperty() { return hepyResult; }
    public BooleanProperty loadingProperty() { return hepyLoading; }
    public StringProperty errorProperty() { return hepyError; }
    public StringProperty priceErrorProperty() { return hepyPriceError; }
}
