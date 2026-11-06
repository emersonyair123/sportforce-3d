package com.sportforce3d.model;

/**
 * Variante activa con stock disponible igual o menor que su umbral de aviso.
 * {@code showColor} es true cuando el producto tiene más de un color y hace falta distinguirlo.
 */
public record LowStockVariant(String sizeName, String colorName, int available, int threshold, boolean showColor) {

    /** "Talla XXL: 2 u.", "Talla M · Negro: 0 u. (agotada)". */
    public String label() {
        return "Talla " + sizeName + (showColor ? " · " + colorName : "") + ": " + available + " u."
                + (available == 0 ? " (agotada)" : "");
    }
}
