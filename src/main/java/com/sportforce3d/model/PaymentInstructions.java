package com.sportforce3d.model;

/**
 * A dónde paga el cliente (store_settings). El ADMIN los edita en Administración → Configuración.
 *
 * @param reviewed false mientras sigan los datos de prueba de la instalación
 */
public record PaymentInstructions(String yapePhone, String yapeHolder, String plinPhone, String plinHolder,
                                  String bankName, String bankHolder, String bankAccount, String bankCci,
                                  boolean reviewed) {

    /** Texto para el método elegido. */
    public String forMethod(PaymentMethod method) {
        return switch (method) {
            case YAPE -> "Yapea al " + yapePhone + " (" + yapeHolder + ").";
            case PLIN -> "Paga con Plin al " + plinPhone + " (" + plinHolder + ").";
            case TRANSFERENCIA -> "Transfiere a " + bankName + ", cuenta " + bankAccount + ", CCI " + bankCci
                    + " (" + bankHolder + ").";
            case TARJETA -> "Pago con tarjeta: próximamente.";
        };
    }
}
