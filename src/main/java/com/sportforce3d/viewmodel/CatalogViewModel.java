package com.sportforce3d.viewmodel;

import com.sportforce3d.app.Async;
import com.sportforce3d.model.Category;
import com.sportforce3d.model.Page;
import com.sportforce3d.model.ProductQuery;
import com.sportforce3d.model.ProductSort;
import com.sportforce3d.model.ProductSummary;
import com.sportforce3d.model.Size;
import com.sportforce3d.model.Sport;
import com.sportforce3d.service.CatalogService;
import com.sportforce3d.util.Money;
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

public class CatalogViewModel {

    private record ReferenceData(List<Category> categories, List<Sport> sports, List<Size> sizes) {
    }

    private final CatalogService catalog;

    private final StringProperty text = new SimpleStringProperty("");
    private final ObjectProperty<Long> categoryId = new SimpleObjectProperty<>();
    private final ObjectProperty<Long> sportId = new SimpleObjectProperty<>();
    private final ObservableSet<String> sizeCodes = FXCollections.observableSet();
    private final StringProperty minPrice = new SimpleStringProperty("");
    private final StringProperty maxPrice = new SimpleStringProperty("");
    private final BooleanProperty inStockOnly = new SimpleBooleanProperty(false);
    private final BooleanProperty customizableOnly = new SimpleBooleanProperty(false);
    private final ObjectProperty<ProductSort> sort = new SimpleObjectProperty<>(ProductSort.RELEVANCE);

    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final ObservableList<Sport> sports = FXCollections.observableArrayList();
    private final ObservableList<Size> sizes = FXCollections.observableArrayList();
    private final ObjectProperty<Page<ProductSummary>> result = new SimpleObjectProperty<>();
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");
    private final StringProperty priceError = new SimpleStringProperty("");
    private int requestId;

    public CatalogViewModel(CatalogService catalog) {
        this.catalog = catalog;
    }

    public void start(Runnable onReferenceData) {
        Async.run(() -> new ReferenceData(catalog.categories(), catalog.sports(), catalog.sizes()),
                data -> {
                    categories.setAll(data.categories());
                    sports.setAll(data.sports());
                    sizes.setAll(data.sizes());
                    onReferenceData.run();
                    search(1);
                },
                e -> error.set(ErrorMessages.of(e)));
    }

    public void search(int page) {
        ProductQuery query;
        try {
            query = buildQuery(page);
        } catch (IllegalArgumentException e) {
            return;
        }
        int id = ++requestId;
        loading.set(true);
        error.set("");
        Async.run(() -> catalog.search(query),
                page1 -> {
                    if (id == requestId) {
                        result.set(page1);
                        loading.set(false);
                    }
                },
                e -> {
                    if (id == requestId) {
                        loading.set(false);
                        error.set(ErrorMessages.of(e));
                    }
                });
    }

    ProductQuery buildQuery(int page) {
        priceError.set("");
        Long min = parsePrice(minPrice.get());
        Long max = parsePrice(maxPrice.get());
        if (min != null && max != null && min > max) {
            priceError.set("El mínimo no puede ser mayor que el máximo.");
            throw new IllegalArgumentException("rango de precio");
        }
        String q = text.get() == null || text.get().isBlank() ? null : text.get().trim();
        return new ProductQuery(q, categoryId.get(), sportId.get(), Set.copyOf(sizeCodes), min, max,
                inStockOnly.get(), customizableOnly.get(), sort.get(), page, ProductQuery.DEFAULT_PAGE_SIZE);
    }

    private Long parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Money.parse(value);
        } catch (IllegalArgumentException e) {
            priceError.set("Escribe montos como 79.90");
            throw e;
        }
    }

    public void clearFilters() {
        text.set("");
        categoryId.set(null);
        sportId.set(null);
        sizeCodes.clear();
        minPrice.set("");
        maxPrice.set("");
        inStockOnly.set(false);
        customizableOnly.set(false);
        sort.set(ProductSort.RELEVANCE);
        search(1);
    }

    public String categoryName() {
        return categories.stream().filter(c -> c.id() == (categoryId.get() == null ? -1 : categoryId.get()))
                .map(Category::name).findFirst().orElse(null);
    }

    public StringProperty textProperty() { return text; }
    public ObjectProperty<Long> categoryIdProperty() { return categoryId; }
    public ObjectProperty<Long> sportIdProperty() { return sportId; }
    public ObservableSet<String> sizeCodes() { return sizeCodes; }
    public StringProperty minPriceProperty() { return minPrice; }
    public StringProperty maxPriceProperty() { return maxPrice; }
    public BooleanProperty inStockOnlyProperty() { return inStockOnly; }
    public BooleanProperty customizableOnlyProperty() { return customizableOnly; }
    public ObjectProperty<ProductSort> sortProperty() { return sort; }
    public ObservableList<Category> categories() { return categories; }
    public ObservableList<Sport> sports() { return sports; }
    public ObservableList<Size> sizes() { return sizes; }
    public ObjectProperty<Page<ProductSummary>> resultProperty() { return result; }
    public BooleanProperty loadingProperty() { return loading; }
    public StringProperty errorProperty() { return error; }
    public StringProperty priceErrorProperty() { return priceError; }
}
