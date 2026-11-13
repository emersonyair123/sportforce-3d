package com.sportforce3d.model;

import java.util.List;

public record HepyProductDetail(long hepyId, long hepyCategoryId, String hepyCategoryName, Long hepySportId, String hepySportName,
                            String hepySku, String hepyName, String hepyDescription, HepyGarment hepyGarment,
                            long hepyBasePriceCents, Long hepyCompareAtPriceCents, boolean hepyCustomizable,
                            boolean hepyIsNew, boolean hepyFeatured, boolean hepyActive, boolean hepyDeleted,
                            List<HepyProductVariant> hepyVariants, List<HepyProductImage> hepyImages, List<HepyMaterial> hepyMaterials) {

    public HepyProductDetail {
        hepyVariants = List.copyOf(hepyVariants);
        hepyImages = List.copyOf(hepyImages);
        hepyMaterials = List.copyOf(hepyMaterials);
    }

    public long hepyUnitPrice(HepyProductVariant hepyVariant) {
        return hepyBasePriceCents + hepyVariant.hepyPriceDeltaCents();
    }

    public List<HepyProductVariant> hepyActiveVariants() {
        return hepyVariants.stream().filter(HepyProductVariant::hepyActive).toList();
    }

    public boolean hepyOnSale() {
        return hepyCompareAtPriceCents != null && hepyCompareAtPriceCents > hepyBasePriceCents;
    }

    public int hepyDiscountPercent() {
        return hepyOnSale() ? (int) Math.round(100.0 * (hepyCompareAtPriceCents - hepyBasePriceCents) / hepyCompareAtPriceCents) : 0;
    }

    public String hepyPrimaryImage() {
        return hepyImages.isEmpty() ? null : hepyImages.get(0).hepyPath();
    }

    public String hepyPrimaryColorHex() {
        return hepyActiveVariants().isEmpty() ? null : hepyActiveVariants().get(0).hepyColorHex();
    }
}
