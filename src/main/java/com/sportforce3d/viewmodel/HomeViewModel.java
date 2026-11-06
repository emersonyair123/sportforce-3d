package com.sportforce3d.viewmodel;

import com.sportforce3d.app.Async;
import com.sportforce3d.model.Category;
import com.sportforce3d.model.ProductSummary;
import com.sportforce3d.service.CatalogService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class HomeViewModel {

    private record HomeData(List<Category> categories, List<ProductSummary> featured) {
    }

    private final CatalogService catalog;
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final ObservableList<ProductSummary> featured = FXCollections.observableArrayList();
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");

    public HomeViewModel(CatalogService catalog) {
        this.catalog = catalog;
    }

    public void load() {
        loading.set(true);
        error.set("");
        Async.run(() -> new HomeData(catalog.categories(), catalog.featured(8)),
                data -> {
                    categories.setAll(data.categories());
                    featured.setAll(data.featured());
                    loading.set(false);
                },
                e -> {
                    loading.set(false);
                    error.set(ErrorMessages.of(e));
                });
    }

    public ObservableList<Category> categories() {
        return categories;
    }

    public ObservableList<ProductSummary> featured() {
        return featured;
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public StringProperty errorProperty() {
        return error;
    }
}
