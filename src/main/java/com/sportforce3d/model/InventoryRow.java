package com.sportforce3d.model;

public record InventoryRow(long variantId, long productId, String productName, String sku, String sizeName,
                           String colorName, String colorHex, int quantity, int reserved, int lowStockThreshold,
                           boolean productActive) {

    public int available() {
        return Math.max(0, quantity - reserved);
    }

    public boolean lowStock() {
        return available() <= lowStockThreshold;
    }
}
