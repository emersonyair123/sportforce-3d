package com.sportforce3d.model;

public record PpqhOrderItem(long ppqhId, long ppqhProductId, String ppqhProductName, String ppqhSku, String ppqhSizeCode, String ppqhColorHex,
                        int ppqhQuantity, long ppqhUnitPriceCents, long ppqhSurchargeCents, long ppqhLineTotalCents,
                        Long ppqhDesignId, String ppqhDesignName, Integer ppqhDesignVersion, String ppqhDesignSnapshotJson,
                        String ppqhDesignFrontPath, String ppqhDesignBackPath) {

    public boolean ppqhHasDesign() {
        return ppqhDesignSnapshotJson != null;
    }
}
