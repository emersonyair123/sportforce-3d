package com.sportforce3d.model;

/** Variante talla + color con su stock. {@code available} = cantidad − reservado por pedidos sin pagar. */
public record ProductVariant(long id, long sizeId, String sizeCode, String sizeName, String colorName,
                             String colorHex, String sku, long priceDeltaCents, boolean active,
                             int quantity, int reserved, int lowStockThreshold) {

    public int available() {
        return Math.max(0, quantity - reserved);
    }

    public boolean lowStock() {
        return available() > 0 && available() <= lowStockThreshold;
    }
}
