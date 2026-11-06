package com.sportforce3d.model;

/**
 * Línea de pedido con los datos congelados al comprar. Si es personalizada, guarda una copia del diseño:
 * JSON ({@code designSnapshotJson}) y capturas propias del pedido, que no cambian aunque el cliente edite o borre
 * el diseño después.
 */
public record OrderItem(long id, long productId, String productName, String sku, String sizeCode, String colorHex,
                        int quantity, long unitPriceCents, long surchargeCents, long lineTotalCents,
                        Long designId, String designName, Integer designVersion, String designSnapshotJson,
                        String designFrontPath, String designBackPath) {

    public boolean hasDesign() {
        return designSnapshotJson != null;
    }
}
