package com.sportforce3d.model;

/** Comprobante subido por el cliente (ruta relativa en el almacenamiento). */
public record PaymentProof(long id, String path, String mimeType, long sizeBytes, String createdAt) {

    public boolean isPdf() {
        return "application/pdf".equals(mimeType);
    }
}
