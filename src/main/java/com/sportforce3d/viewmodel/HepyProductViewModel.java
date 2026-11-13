package com.sportforce3d.viewmodel;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.model.HepyCartSummary;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.model.HepyProductVariant;
import com.sportforce3d.model.HepyRealisticLook;
import com.sportforce3d.service.HepyCartService;
import com.sportforce3d.service.HepyCatalogService;
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

public class HepyProductViewModel {

    private final HepyCatalogService hepyCatalog;
    private final HepyCartService hepyCart;

    private final ObjectProperty<HepyProductDetail> hepyDetail = new SimpleObjectProperty<>();
    private HepyRealisticLook hepyLook;
    private final StringProperty hepyColorHex = new SimpleStringProperty();
    private final ObjectProperty<HepyProductVariant> hepyVariant = new SimpleObjectProperty<>();
    private final IntegerProperty hepyQuantity = new SimpleIntegerProperty(1);
    private final BooleanProperty hepyBusy = new SimpleBooleanProperty(false);
    private final StringProperty hepyError = new SimpleStringProperty("");

    private final IntegerBinding hepyAvailable = Bindings.createIntegerBinding(
            () -> hepyVariant.get() == null ? 0 : hepyVariant.get().hepyAvailable(), hepyVariant);
    private final BooleanBinding hepyCanAdd = hepyVariant.isNotNull().and(hepyAvailable.greaterThan(0)).and(hepyBusy.not());

    public HepyProductViewModel(HepyCatalogService hepyCatalog, HepyCartService hepyCart) {
        this.hepyCatalog = hepyCatalog;
        this.hepyCart = hepyCart;
        hepyColorHex.addListener((hepyObs, hepyOld, hepyHex) -> {
            HepyProductVariant hepyPrevious = hepyVariant.get();
            hepyVariant.set(hepyPrevious == null ? null : hepyVariantsFor(hepyHex).stream()
                    .filter(hepyV -> hepyV.hepySizeId() == hepyPrevious.hepySizeId()).findFirst().orElse(null));
        });
        hepyVariant.addListener((hepyObs, hepyOld, hepyV) -> hepyQuantity.set(1));
    }

    private record HepyLoaded(HepyProductDetail hepyDetail, HepyRealisticLook hepyLook, long hepyShippingFlat, long hepyFreeFrom) {
    }

    private long hepyShippingFlatCents;
    private long hepyFreeShippingFromCents;

    public void hepyLoad(long hepyProductId) {
        hepyError.set("");
        PpqhAsync.ppqhRun(() -> new HepyLoaded(hepyCatalog.hepyDetail(hepyProductId), hepyCatalog.hepyRealisticLook(hepyProductId).orElse(null),
                        hepyCatalog.hepyShippingFlatCents(), hepyCatalog.hepyFreeShippingFromCents()),
                hepyLoaded -> {
                    hepyLook = hepyLoaded.hepyLook();
                    hepyShippingFlatCents = hepyLoaded.hepyShippingFlat();
                    hepyFreeShippingFromCents = hepyLoaded.hepyFreeFrom();
                    HepyProductDetail hepyD = hepyLoaded.hepyDetail();
                    hepyDetail.set(hepyD);
                    List<HepyProductVariant> hepyActive = hepyD.hepyVariants();
                    hepyColorHex.set(hepyActive.isEmpty() ? null : hepyActive.get(0).hepyColorHex());
                    List<HepyProductVariant> hepyForColor = hepyVariantsFor(hepyColorHex.get());
                    hepyVariant.set(hepyForColor.size() == 1 ? hepyForColor.get(0) : null);
                },
                hepyE -> hepyError.set(PpqhErrorMessages.ppqhOf(hepyE)));
    }

    public HepyRealisticLook hepyLook() {
        return hepyLook;
    }

    public Map<String, String> hepyColors() {
        Map<String, String> hepyMap = new LinkedHashMap<>();
        if (hepyDetail.get() != null) {
            hepyDetail.get().hepyVariants().forEach(hepyV -> hepyMap.putIfAbsent(hepyV.hepyColorHex(), hepyV.hepyColorName()));
        }
        return hepyMap;
    }

    public List<HepyProductVariant> hepyVariantsFor(String hepyHex) {
        if (hepyDetail.get() == null || hepyHex == null) {
            return List.of();
        }
        return hepyDetail.get().hepyVariants().stream().filter(hepyV -> hepyV.hepyColorHex().equals(hepyHex)).toList();
    }

    public long hepyUnitPrice() {
        HepyProductDetail hepyD = hepyDetail.get();
        if (hepyD == null) {
            return 0;
        }
        return hepyVariant.get() == null ? hepyD.hepyBasePriceCents() : hepyD.hepyUnitPrice(hepyVariant.get());
    }

    public void hepyAddToCart(Consumer<HepyCartSummary> hepyOnSuccess, Consumer<String> hepyOnError) {
        HepyProductVariant hepyV = hepyVariant.get();
        if (hepyV == null) {
            hepyOnError.accept("Elige una talla.");
            return;
        }
        hepyBusy.set(true);
        int hepyQty = hepyQuantity.get();
        PpqhAsync.ppqhRun(() -> hepyCart.hepyAdd(hepyV.hepyId(), hepyQty),
                hepySummary -> {
                    hepyBusy.set(false);
                    hepyOnSuccess.accept(hepySummary);
                },
                hepyE -> {
                    hepyBusy.set(false);
                    hepyOnError.accept(PpqhErrorMessages.ppqhOf(hepyE));
                });
    }

    public long hepyShippingFlatCents() { return hepyShippingFlatCents; }
    public long hepyFreeShippingFromCents() { return hepyFreeShippingFromCents; }
    public ObjectProperty<HepyProductDetail> detailProperty() { return hepyDetail; }
    public StringProperty colorHexProperty() { return hepyColorHex; }
    public ObjectProperty<HepyProductVariant> variantProperty() { return hepyVariant; }
    public IntegerProperty quantityProperty() { return hepyQuantity; }
    public IntegerBinding availableProperty() { return hepyAvailable; }
    public BooleanBinding canAddProperty() { return hepyCanAdd; }
    public BooleanProperty busyProperty() { return hepyBusy; }
    public StringProperty errorProperty() { return hepyError; }
}
