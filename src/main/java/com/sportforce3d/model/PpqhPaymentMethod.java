package com.sportforce3d.model;

public enum PpqhPaymentMethod {
    YAPE("Yape", true),
    PLIN("Plin", true),
    TRANSFERENCIA("Transferencia bancaria", true),
    TARJETA("Tarjeta de crédito o débito", false);

    private final String ppqhLabel;
    private final boolean ppqhAvailable;

    PpqhPaymentMethod(String ppqhLabel, boolean ppqhAvailable) {
        this.ppqhLabel = ppqhLabel;
        this.ppqhAvailable = ppqhAvailable;
    }

    public String ppqhLabel() {
        return ppqhLabel;
    }

    public boolean ppqhAvailable() {
        return ppqhAvailable;
    }

    public boolean ppqhRequiresManualVerification() {
        return this != TARJETA;
    }
}
