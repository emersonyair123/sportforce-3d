package com.sportforce3d.model;

public record LowStockVariant(String sizeName, String colorName, int available, int threshold, boolean showColor) {
    public String label() {
        return "Talla " + sizeName + (showColor ? " · " + colorName : "") + ": " + available + " u."
                + (available == 0 ? " (agotada)" : "");
    }
}
