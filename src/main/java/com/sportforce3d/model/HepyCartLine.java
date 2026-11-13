package com.sportforce3d.model;

public record HepyCartLine(long hepyItemId, long hepyProductId, long hepyVariantId, String hepyProductName, String hepySku,
                       HepyGarment hepyGarment, String hepySizeName, String hepyColorName, String hepyColorHex, String hepyImagePath,
                       long hepyUnitPriceCents, long hepySurchargeCents, int hepyQuantity, int hepyAvailable, boolean hepyPurchasable,
                       Long hepyDesignId, String hepyDesignName, String hepyDesignThumbnail) {

    public long hepyLineTotalCents() {
        return (hepyUnitPriceCents + hepySurchargeCents) * hepyQuantity;
    }

    public boolean hepyEnoughStock() {
        return hepyQuantity <= hepyAvailable;
    }

    public boolean hepyHasDesign() {
        return hepyDesignId != null;
    }

    public HepyCartLine hepyWithSurcharge(long hepyCents) {
        return new HepyCartLine(hepyItemId, hepyProductId, hepyVariantId, hepyProductName, hepySku, hepyGarment, hepySizeName, hepyColorName, hepyColorHex,
                hepyImagePath, hepyUnitPriceCents, hepyCents, hepyQuantity, hepyAvailable, hepyPurchasable, hepyDesignId, hepyDesignName,
                hepyDesignThumbnail);
    }
}
