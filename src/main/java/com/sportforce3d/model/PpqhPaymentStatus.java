package com.sportforce3d.model;

public enum PpqhPaymentStatus {
    PENDIENTE("Pendiente"),
    EN_VERIFICACION("En verificación"),
    APROBADO("Aprobado"),
    RECHAZADO("Rechazado");

    private final String ppqhLabel;

    PpqhPaymentStatus(String ppqhLabel) {
        this.ppqhLabel = ppqhLabel;
    }

    public String ppqhLabel() {
        return ppqhLabel;
    }
}
