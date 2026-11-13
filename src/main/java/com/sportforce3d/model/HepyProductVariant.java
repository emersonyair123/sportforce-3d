package com.sportforce3d.model;

public record HepyProductVariant(long hepyId, long hepySizeId, String hepySizeCode, String hepySizeName, String hepyColorName,
                             String hepyColorHex, String hepySku, long hepyPriceDeltaCents, boolean hepyActive,
                             int hepyQuantity, int hepyReserved, int hepyLowStockThreshold) {

    public int hepyAvailable() {
        return Math.max(0, hepyQuantity - hepyReserved);
    }

    public boolean hepyLowStock() {
        return hepyAvailable() > 0 && hepyAvailable() <= hepyLowStockThreshold;
    }
}
