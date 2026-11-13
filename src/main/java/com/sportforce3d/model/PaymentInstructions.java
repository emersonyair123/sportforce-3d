package com.sportforce3d.model;

public record PaymentInstructions(String yapePhone, String yapeHolder, String plinPhone, String plinHolder,
                                  String bankName, String bankHolder, String bankAccount, String bankCci,
                                  boolean reviewed) {
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
