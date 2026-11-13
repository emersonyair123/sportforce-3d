package com.sportforce3d.model;

public record HepyInventoryRow(long hepyVariantId, long hepyProductId, String hepyProductName, String hepySku, String hepySizeName,
                           String hepyColorName, String hepyColorHex, int hepyQuantity, int hepyReserved, int hepyLowStockThreshold,
                           boolean hepyProductActive) {

    public int hepyAvailable() {
        return Math.max(0, hepyQuantity - hepyReserved);
    }

    public boolean hepyLowStock() {
        return hepyAvailable() <= hepyLowStockThreshold;
    }
}
