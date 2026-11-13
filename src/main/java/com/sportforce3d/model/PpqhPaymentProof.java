package com.sportforce3d.model;

public record PpqhPaymentProof(long ppqhId, String ppqhPath, String ppqhMimeType, long ppqhSizeBytes, String ppqhCreatedAt) {

    public boolean ppqhIsPdf() {
        return "application/pdf".equals(ppqhMimeType);
    }
}
