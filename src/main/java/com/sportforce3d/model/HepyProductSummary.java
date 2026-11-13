package com.sportforce3d.model;

public record HepyProductSummary(long hepyId, String hepySku, String hepyName, String hepyCategoryName, HepyGarment hepyGarment,
                             long hepyPriceCents, Long hepyCompareAtPriceCents, boolean hepyIsNew,
                             boolean hepyCustomizable, int hepyStock, String hepyColorHex,
                             int hepyVariantCount, Long hepySingleVariantId, String hepyImagePath,
                             HepyRealisticLook hepyLook) {

    public boolean hepyInStock() {
        return hepyStock > 0;
    }

    public boolean hepyOnSale() {
        return hepyCompareAtPriceCents != null && hepyCompareAtPriceCents > hepyPriceCents;
    }

    public int hepyDiscountPercent() {
        if (!hepyOnSale()) {
            return 0;
        }
        return (int) Math.round(100.0 * (hepyCompareAtPriceCents - hepyPriceCents) / hepyCompareAtPriceCents);
    }
}
