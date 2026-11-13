package com.sportforce3d.model;

import java.util.List;
import java.util.Set;

public record ProductForm(Long id, long categoryId, Long sportId, String sku, String name, String description,
                          Garment garment, long basePriceCents, Long compareAtPriceCents, boolean customizable,
                          boolean isNew, boolean featured, boolean active, Set<Long> materialIds,
                          List<VariantForm> variants) {

    public ProductForm {
        materialIds = materialIds == null ? Set.of() : Set.copyOf(materialIds);
        variants = variants == null ? List.of() : List.copyOf(variants);
    }

    public record VariantForm(Long id, long sizeId, String colorName, String colorHex, String sku,
                              long priceDeltaCents, int stock, int lowStockThreshold, boolean active) {
    }
}
