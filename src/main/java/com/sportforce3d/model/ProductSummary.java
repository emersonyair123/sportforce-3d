package com.sportforce3d.model;

public record ProductSummary(long id, String sku, String name, String categoryName, Garment garment,
                             long priceCents, Long compareAtPriceCents, boolean isNew,
                             boolean customizable, int stock, String colorHex,
                             int variantCount, Long singleVariantId, String imagePath,
                             RealisticLook look) {

    public boolean inStock() {
        return stock > 0;
    }

    public boolean onSale() {
        return compareAtPriceCents != null && compareAtPriceCents > priceCents;
    }

    public int discountPercent() {
        if (!onSale()) {
            return 0;
        }
        return (int) Math.round(100.0 * (compareAtPriceCents - priceCents) / compareAtPriceCents);
    }
}
