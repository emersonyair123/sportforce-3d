package com.sportforce3d.service;

import com.sportforce3d.model.HepyCategory;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.HepyPage;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.model.HepyProductQuery;
import com.sportforce3d.model.HepyProductSummary;
import com.sportforce3d.model.HepyProductVariant;
import com.sportforce3d.model.HepyRealisticLook;
import com.sportforce3d.model.HepySize;
import com.sportforce3d.model.HepySport;
import com.sportforce3d.repository.HepyCatalogRepository;
import com.sportforce3d.repository.HepyProductRepository;

import java.util.List;
import java.util.Optional;

public class HepyCatalogService {

    public static final long HEPY_DEFAULT_SHIPPING_CENTS = 1500;
    public static final long HEPY_DEFAULT_FREE_SHIPPING_FROM_CENTS = 29900;

    private final HepyCatalogRepository hepyCatalog;
    private final HepyProductRepository hepyProducts;

    public static final int HEPY_SEARCH_MAX = 100;

    public HepyCatalogService(HepyCatalogRepository hepyCatalog, HepyProductRepository hepyProducts) {
        this.hepyCatalog = hepyCatalog;
        this.hepyProducts = hepyProducts;
    }

    public List<HepyCategory> hepyCategories() {
        return hepyCatalog.hepyFindActiveCategories();
    }

    public List<HepySport> hepySports() {
        return hepyCatalog.hepyFindSports();
    }

    public List<HepySize> hepySizes() {
        return hepyCatalog.hepyFindSizes();
    }

    public List<HepyProductSummary> hepyFeatured(int hepyLimit) {
        return hepyProducts.hepyFindFeatured(Math.max(1, Math.min(hepyLimit, 24)));
    }

    public HepyPage<HepyProductSummary> hepySearch(HepyProductQuery hepyQuery) {
        if (hepyQuery == null) {
            hepyQuery = HepyProductQuery.hepyAll();
        }
        if (hepyQuery.hepyText() != null && hepyQuery.hepyText().length() > HEPY_SEARCH_MAX) {
            throw PpqhValidationException.ppqhOf("text", "La búsqueda admite hasta " + HEPY_SEARCH_MAX + " caracteres.");
        }
        if ((hepyQuery.hepyMinPriceCents() != null && hepyQuery.hepyMinPriceCents() < 0)
                || (hepyQuery.hepyMaxPriceCents() != null && hepyQuery.hepyMaxPriceCents() < 0)) {
            throw PpqhValidationException.ppqhOf("price", "Los precios no pueden ser negativos.");
        }
        if (hepyQuery.hepyMinPriceCents() != null && hepyQuery.hepyMaxPriceCents() != null
                && hepyQuery.hepyMinPriceCents() > hepyQuery.hepyMaxPriceCents()) {
            throw PpqhValidationException.ppqhOf("price", "El precio mínimo no puede ser mayor que el máximo.");
        }
        return hepyProducts.hepySearch(hepyQuery);
    }

    public HepyProductDetail hepyDetail(long hepyProductId) {
        HepyProductDetail hepyD = hepyProducts.hepyFindDetail(hepyProductId)
                .filter(hepyP -> hepyP.hepyActive() && !hepyP.hepyDeleted())
                .orElseThrow(() -> new PpqhServiceException("Este producto ya no está disponible."));
        List<HepyProductVariant> hepyActive = hepyD.hepyActiveVariants();
        return new HepyProductDetail(hepyD.hepyId(), hepyD.hepyCategoryId(), hepyD.hepyCategoryName(), hepyD.hepySportId(), hepyD.hepySportName(), hepyD.hepySku(),
                hepyD.hepyName(), hepyD.hepyDescription(), hepyD.hepyGarment(), hepyD.hepyBasePriceCents(), hepyD.hepyCompareAtPriceCents(),
                hepyD.hepyCustomizable(), hepyD.hepyIsNew(), hepyD.hepyFeatured(), hepyD.hepyActive(), hepyD.hepyDeleted(), hepyActive, hepyD.hepyImages(), hepyD.hepyMaterials());
    }

    public List<HepyMaterial> hepyMaterials(HepyMaterial.HepyScope hepyScope) {
        return hepyCatalog.hepyFindMaterials(hepyScope);
    }

    public List<HepyProductSummary> hepyDesignableProducts() {
        List<HepyProductSummary> hepyOut = new java.util.ArrayList<>();
        for (int hepyPage = 1; ; hepyPage++) {
            HepyPage<HepyProductSummary> hepyResult = hepySearch(new HepyProductQuery(null, null, null, java.util.Set.of(), null, null,
                    false, true, com.sportforce3d.model.HepyProductSort.RELEVANCE, hepyPage, 60));
            hepyResult.hepyItems().stream().filter(hepyP -> HepyCartService.HEPY_DESIGNABLE.contains(hepyP.hepyGarment())).forEach(hepyOut::add);
            if (!hepyResult.hepyHasNext()) {
                return hepyOut;
            }
        }
    }

    public Optional<HepyRealisticLook> hepyRealisticLook(long hepyProductId) {
        return hepyProducts.hepyFindLook(hepyProductId);
    }

    public java.util.Map<String, HepyMaterial> hepyMaterialsByCode() {
        java.util.Map<String, HepyMaterial> hepyMap = new java.util.HashMap<>();
        for (HepyMaterial.HepyScope hepyScope : HepyMaterial.HepyScope.values()) {
            hepyCatalog.hepyFindMaterials(hepyScope).forEach(hepyM -> hepyMap.put(hepyM.hepyCode(), hepyM));
        }
        return java.util.Map.copyOf(hepyMap);
    }

    public long hepyShippingFlatCents() {
        return hepyCatalog.hepyFindLongSetting("shipping_flat_cents").orElse(HEPY_DEFAULT_SHIPPING_CENTS);
    }

    public long hepyFreeShippingFromCents() {
        return hepyCatalog.hepyFindLongSetting("free_shipping_from_cents").orElse(HEPY_DEFAULT_FREE_SHIPPING_FROM_CENTS);
    }

    public long hepyShippingFor(long hepyGoodsCents) {
        if (hepyGoodsCents <= 0) {
            return 0;
        }
        return hepyGoodsCents >= hepyFreeShippingFromCents() ? 0 : hepyShippingFlatCents();
    }
}
