package com.sportforce3d.model;

import java.util.List;

public record ProductDetail(long id, long categoryId, String categoryName, Long sportId, String sportName,
                            String sku, String name, String description, Garment garment,
                            long basePriceCents, Long compareAtPriceCents, boolean customizable,
                            boolean isNew, boolean featured, boolean active, boolean deleted,
                            List<ProductVariant> variants, List<ProductImage> images, List<Material> materials) {

    public ProductDetail {
        variants = List.copyOf(variants);
        images = List.copyOf(images);
        materials = List.copyOf(materials);
    }

    public long unitPrice(ProductVariant variant) {
        return basePriceCents + variant.priceDeltaCents();
    }

    public List<ProductVariant> activeVariants() {
        return variants.stream().filter(ProductVariant::active).toList();
    }

    public boolean onSale() {
        return compareAtPriceCents != null && compareAtPriceCents > basePriceCents;
    }

    public int discountPercent() {
        return onSale() ? (int) Math.round(100.0 * (compareAtPriceCents - basePriceCents) / compareAtPriceCents) : 0;
    }

    public String primaryImage() {
        return images.isEmpty() ? null : images.get(0).path();
    }

    public String primaryColorHex() {
        return activeVariants().isEmpty() ? null : activeVariants().get(0).colorHex();
    }
}
