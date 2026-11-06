package com.sportforce3d.model;

import java.util.List;

/**
 * Fila de la tabla de productos del ADMIN (incluye inactivos y eliminados). {@code stock} es el total disponible
 * de las variantes activas; {@code lowStockVariants} dice cuáles están en su umbral de aviso o por debajo.
 */
public record AdminProductRow(long id, String sku, String name, String categoryName, Garment garment,
                              long priceCents, int stock, int variantCount, boolean active, boolean deleted,
                              String colorHex, String imagePath, List<LowStockVariant> lowStockVariants) {

    public AdminProductRow {
        lowStockVariants = List.copyOf(lowStockVariants);
    }

    /**
     * Stock bajo si alguna variante activa está en su umbral o por debajo. Cubre también el total: si el total
     * disponible no supera el umbral de una variante, esa variante (que tiene como mucho el total) también lo cumple.
     */
    public boolean lowStock() {
        return !lowStockVariants.isEmpty();
    }
}
