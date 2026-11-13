package com.sportforce3d.viewmodel;

import com.sportforce3d.app.Async;
import com.sportforce3d.model.CartSummary;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.model.ProductVariant;
import com.sportforce3d.model.RealisticLook;
import com.sportforce3d.service.CartService;
import com.sportforce3d.service.CatalogService;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ProductViewModel {

    private final CatalogService catalog;
    private final CartService cart;

    private final ObjectProperty<ProductDetail> detail = new SimpleObjectProperty<>();
    private RealisticLook look;
    private final StringProperty colorHex = new SimpleStringProperty();
    private final ObjectProperty<ProductVariant> variant = new SimpleObjectProperty<>();
    private final IntegerProperty quantity = new SimpleIntegerProperty(1);
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty("");

    private final IntegerBinding available = Bindings.createIntegerBinding(
            () -> variant.get() == null ? 0 : variant.get().available(), variant);
    private final BooleanBinding canAdd = variant.isNotNull().and(available.greaterThan(0)).and(busy.not());

    public ProductViewModel(CatalogService catalog, CartService cart) {
        this.catalog = catalog;
        this.cart = cart;
        colorHex.addListener((obs, old, hex) -> {
            ProductVariant previous = variant.get();
            variant.set(previous == null ? null : variantsFor(hex).stream()
                    .filter(v -> v.sizeId() == previous.sizeId()).findFirst().orElse(null));
        });
        variant.addListener((obs, old, v) -> quantity.set(1));
    }

    private record Loaded(ProductDetail detail, RealisticLook look, long shippingFlat, long freeFrom) {
    }

    private long shippingFlatCents;
    private long freeShippingFromCents;

    public void load(long productId) {
        error.set("");
        Async.run(() -> new Loaded(catalog.detail(productId), catalog.realisticLook(productId).orElse(null),
                        catalog.shippingFlatCents(), catalog.freeShippingFromCents()),
                loaded -> {
                    look = loaded.look();
                    shippingFlatCents = loaded.shippingFlat();
                    freeShippingFromCents = loaded.freeFrom();
                    ProductDetail d = loaded.detail();
                    detail.set(d);
                    List<ProductVariant> active = d.variants();
                    colorHex.set(active.isEmpty() ? null : active.get(0).colorHex());
                    List<ProductVariant> forColor = variantsFor(colorHex.get());
                    variant.set(forColor.size() == 1 ? forColor.get(0) : null);
                },
                e -> error.set(ErrorMessages.of(e)));
    }

    public RealisticLook look() {
        return look;
    }

    public Map<String, String> colors() {
        Map<String, String> map = new LinkedHashMap<>();
        if (detail.get() != null) {
            detail.get().variants().forEach(v -> map.putIfAbsent(v.colorHex(), v.colorName()));
        }
        return map;
    }

    public List<ProductVariant> variantsFor(String hex) {
        if (detail.get() == null || hex == null) {
            return List.of();
        }
        return detail.get().variants().stream().filter(v -> v.colorHex().equals(hex)).toList();
    }

    public long unitPrice() {
        ProductDetail d = detail.get();
        if (d == null) {
            return 0;
        }
        return variant.get() == null ? d.basePriceCents() : d.unitPrice(variant.get());
    }

    public void addToCart(Consumer<CartSummary> onSuccess, Consumer<String> onError) {
        ProductVariant v = variant.get();
        if (v == null) {
            onError.accept("Elige una talla.");
            return;
        }
        busy.set(true);
        int qty = quantity.get();
        Async.run(() -> cart.add(v.id(), qty),
                summary -> {
                    busy.set(false);
                    onSuccess.accept(summary);
                },
                e -> {
                    busy.set(false);
                    onError.accept(ErrorMessages.of(e));
                });
    }

    public long shippingFlatCents() { return shippingFlatCents; }
    public long freeShippingFromCents() { return freeShippingFromCents; }
    public ObjectProperty<ProductDetail> detailProperty() { return detail; }
    public StringProperty colorHexProperty() { return colorHex; }
    public ObjectProperty<ProductVariant> variantProperty() { return variant; }
    public IntegerProperty quantityProperty() { return quantity; }
    public IntegerBinding availableProperty() { return available; }
    public BooleanBinding canAddProperty() { return canAdd; }
    public BooleanProperty busyProperty() { return busy; }
    public StringProperty errorProperty() { return error; }
}
