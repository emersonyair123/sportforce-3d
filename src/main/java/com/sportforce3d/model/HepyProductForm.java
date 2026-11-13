package com.sportforce3d.model;

import java.util.List;
import java.util.Set;

public record HepyProductForm(Long hepyId, long hepyCategoryId, Long hepySportId, String hepySku, String hepyName, String hepyDescription,
                          HepyGarment hepyGarment, long hepyBasePriceCents, Long hepyCompareAtPriceCents, boolean hepyCustomizable,
                          boolean hepyIsNew, boolean hepyFeatured, boolean hepyActive, Set<Long> hepyMaterialIds,
                          List<HepyVariantForm> hepyVariants) {

    public HepyProductForm {
        hepyMaterialIds = hepyMaterialIds == null ? Set.of() : Set.copyOf(hepyMaterialIds);
        hepyVariants = hepyVariants == null ? List.of() : List.copyOf(hepyVariants);
    }

    public record HepyVariantForm(Long hepyId, long hepySizeId, String hepyColorName, String hepyColorHex, String hepySku,
                              long hepyPriceDeltaCents, int hepyStock, int hepyLowStockThreshold, boolean hepyActive) {
    }
}
