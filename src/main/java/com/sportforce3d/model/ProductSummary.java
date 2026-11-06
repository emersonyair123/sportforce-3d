package com.sportforce3d.model;

/**
 * Datos mínimos para pintar una ProductCard. Montos en céntimos.
 * {@code singleVariantId} existe solo si el producto tiene una única variante activa (se agrega al carrito sin elegir talla).
 * {@code look} es la foto realista recoloreada (null si el producto no tiene modelo realista).
 */
public record ProductSummary(long id, String sku, String name, String categoryName, Garment garment,
                             long priceCents, Long compareAtPriceCents, boolean isNew,
                             boolean customizable, int stock, String colorHex,
                             int variantCount, Long singleVariantId, String imagePath,
                             RealisticLook look) {

    public boolean inStock() {
        return stock > 0;
    }

    public boolean onSale() {
        return compareAtPriceCents != null && compareAtPriceCents > priceCents;
    }

    /** Descuento entero redondeado, p. ej. 19 para "-19 %". */
    public int discountPercent() {
        if (!onSale()) {
            return 0;
        }
        return (int) Math.round(100.0 * (compareAtPriceCents - priceCents) / compareAtPriceCents);
    }
}