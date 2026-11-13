package com.sportforce3d.model;

public record OrderItem(long id, long productId, String productName, String sku, String sizeCode, String colorHex,
                        int quantity, long unitPriceCents, long surchargeCents, long lineTotalCents,
                        Long designId, String designName, Integer designVersion, String designSnapshotJson,
                        String designFrontPath, String designBackPath) {

    public boolean hasDesign() {
        return designSnapshotJson != null;
    }
}
