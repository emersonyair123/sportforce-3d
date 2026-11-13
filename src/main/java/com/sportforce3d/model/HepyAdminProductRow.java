package com.sportforce3d.model;

import java.util.List;

public record HepyAdminProductRow(long hepyId, String hepySku, String hepyName, String hepyCategoryName, HepyGarment hepyGarment,
                              long hepyPriceCents, int hepyStock, int hepyVariantCount, boolean hepyActive, boolean hepyDeleted,
                              String hepyColorHex, String hepyImagePath, List<HepyLowStockVariant> hepyLowStockVariants) {

    public HepyAdminProductRow {
        hepyLowStockVariants = List.copyOf(hepyLowStockVariants);
    }

    public boolean hepyLowStock() {
        return !hepyLowStockVariants.isEmpty();
    }
}
