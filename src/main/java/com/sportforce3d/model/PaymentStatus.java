package com.sportforce3d.model;

/** Estado de un intento de pago (tabla payments). */
public enum PaymentStatus {
    PENDIENTE("Pendiente"),
    EN_VERIFICACION("En verificación"),
    APROBADO("Aprobado"),
    RECHAZADO("Rechazado");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
