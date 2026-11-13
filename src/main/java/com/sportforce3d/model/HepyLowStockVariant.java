package com.sportforce3d.model;

public record HepyLowStockVariant(String hepySizeName, String hepyColorName, int hepyAvailable, int hepyThreshold, boolean hepyShowColor) {
    public String hepyLabel() {
        return "Talla " + hepySizeName + (hepyShowColor ? " · " + hepyColorName : "") + ": " + hepyAvailable + " u."
                + (hepyAvailable == 0 ? " (agotada)" : "");
    }
}
