package com.sportforce3d.model;

/**
 * Métodos de pago en soles. Yape, Plin y transferencia se verifican a mano por un ADMIN.
 * La tarjeta necesita un servidor que reciba los webhooks de la pasarela, por eso está deshabilitada.
 */
public enum PaymentMethod {
    YAPE("Yape", true),
    PLIN("Plin", true),
    TRANSFERENCIA("Transferencia bancaria", true),
    TARJETA("Tarjeta de crédito o débito", false);

    private final String label;
    private final boolean available;

    PaymentMethod(String label, boolean available) {
        this.label = label;
        this.available = available;
    }

    public String label() {
        return label;
    }

    public boolean available() {
        return available;
    }

    public boolean requiresManualVerification() {
        return this != TARJETA;
    }
}
