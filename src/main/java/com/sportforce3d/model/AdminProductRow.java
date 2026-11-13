package com.sportforce3d.model;

import java.util.List;

public record AdminProductRow(long id, String sku, String name, String categoryName, Garment garment,
                              long priceCents, int stock, int variantCount, boolean active, boolean deleted,
                              String colorHex, String imagePath, List<LowStockVariant> lowStockVariants) {

    public AdminProductRow {
        lowStockVariants = List.copyOf(lowStockVariants);
    }

    public boolean lowStock() {
        return !lowStockVariants.isEmpty();
    }
}
