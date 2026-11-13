package com.sportforce3d.viewmodel;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.model.HepyCategory;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.model.HepyProductForm;
import com.sportforce3d.model.HepyRealisticLook;
import com.sportforce3d.model.HepySize;
import com.sportforce3d.model.HepySport;
import com.sportforce3d.service.HepyAdminProductService;
import com.sportforce3d.service.HepyCatalogService;
import com.sportforce3d.service.PpqhValidationException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class HepyAdminProductEditViewModel {
    public record HepyReferenceData(List<HepyCategory> hepyCategories, List<HepySport> hepySports, List<HepySize> hepySizes,
                                List<HepyMaterial> hepyTextiles, List<HepyMaterial> hepyShoeMaterials) {
    }

    private final HepyAdminProductService hepyAdmin;
    private final HepyCatalogService hepyCatalog;
    private final ObjectProperty<HepyReferenceData> hepyReference = new SimpleObjectProperty<>();
    private final ObjectProperty<HepyProductDetail> hepyProduct = new SimpleObjectProperty<>();
    private final ObjectProperty<HepyRealisticLook> hepyLook = new SimpleObjectProperty<>();
    private final BooleanProperty hepySaving = new SimpleBooleanProperty(false);

    public HepyAdminProductEditViewModel(HepyAdminProductService hepyAdmin, HepyCatalogService hepyCatalog) {
        this.hepyAdmin = hepyAdmin;
        this.hepyCatalog = hepyCatalog;
    }

    public void hepyLoad(Long hepyProductId, Consumer<String> hepyOnError) {
        PpqhAsync.ppqhRun(() -> {
                    HepyReferenceData hepyData = new HepyReferenceData(hepyCatalog.hepyCategories(), hepyCatalog.hepySports(), hepyCatalog.hepySizes(),
                            hepyCatalog.hepyMaterials(HepyMaterial.HepyScope.TEXTIL), hepyCatalog.hepyMaterials(HepyMaterial.HepyScope.CALZADO));
                    HepyProductDetail hepyDetail = hepyProductId == null ? null : hepyAdmin.hepyGet(hepyProductId);
                    HepyRealisticLook hepyRealistic = hepyProductId == null ? null : hepyAdmin.hepyLook(hepyProductId).orElse(null);
                    return new HepyLoaded(hepyData, hepyDetail, hepyRealistic);
                },
                hepyResult -> {
                    hepyLook.set(hepyResult.hepyLook());
                    hepyReference.set(hepyResult.hepyData());
                    hepyProduct.set(hepyResult.hepyDetail());
                },
                hepyE -> hepyOnError.accept(PpqhErrorMessages.ppqhOf(hepyE)));
    }

    private record HepyLoaded(HepyReferenceData hepyData, HepyProductDetail hepyDetail, HepyRealisticLook hepyLook) {
    }

    private record HepySaved(HepyProductDetail hepyDetail, HepyRealisticLook hepyLook) {
    }

    public void hepySave(HepyProductForm hepyForm, Map<String, String> hepyZoneColors, Consumer<HepyProductDetail> hepyOnSuccess, Consumer<Map<String, String>> hepyOnFieldErrors,
                     Consumer<String> hepyOnError) {
        hepySaving.set(true);
        PpqhAsync.ppqhRun(() -> {
                    HepyProductDetail hepySaved = hepyAdmin.hepySave(hepyForm);
                    HepyRealisticLook hepyRealistic = hepyZoneColors.isEmpty() ? hepyAdmin.hepyLook(hepySaved.hepyId()).orElse(null)
                            : hepyAdmin.hepyUpdateZoneColors(hepySaved.hepyId(), hepyZoneColors);
                    return new HepySaved(hepySaved, hepyRealistic);
                },
                hepySaved -> {
                    hepySaving.set(false);
                    hepyLook.set(hepySaved.hepyLook());
                    hepyProduct.set(hepySaved.hepyDetail());
                    hepyOnSuccess.accept(hepySaved.hepyDetail());
                },
                hepyE -> {
                    hepySaving.set(false);
                    if (hepyE instanceof PpqhValidationException hepyV) {
                        hepyOnFieldErrors.accept(hepyV.ppqhFieldErrors());
                    } else {
                        hepyOnError.accept(PpqhErrorMessages.ppqhOf(hepyE));
                    }
                });
    }

    public void hepyAddImage(Path hepyFile, Consumer<String> hepyOnError) {
        HepyProductDetail hepyP = hepyProduct.get();
        if (hepyP == null) {
            return;
        }
        PpqhAsync.ppqhRun(() -> {
                    hepyAdmin.hepyAddImage(hepyP.hepyId(), hepyFile);
                    return hepyAdmin.hepyGet(hepyP.hepyId());
                },
                hepyProduct::set,
                hepyE -> hepyOnError.accept(PpqhErrorMessages.ppqhOf(hepyE)));
    }

    public void hepyRemoveImage(long hepyImageId, Consumer<String> hepyOnError) {
        HepyProductDetail hepyP = hepyProduct.get();
        if (hepyP == null) {
            return;
        }
        PpqhAsync.ppqhRun(() -> {
                    hepyAdmin.hepyRemoveImage(hepyP.hepyId(), hepyImageId);
                    return hepyAdmin.hepyGet(hepyP.hepyId());
                },
                hepyProduct::set,
                hepyE -> hepyOnError.accept(PpqhErrorMessages.ppqhOf(hepyE)));
    }

    public ObjectProperty<HepyReferenceData> referenceProperty() { return hepyReference; }
    public ObjectProperty<HepyProductDetail> productProperty() { return hepyProduct; }
    public ObjectProperty<HepyRealisticLook> lookProperty() { return hepyLook; }
    public BooleanProperty savingProperty() { return hepySaving; }
}
