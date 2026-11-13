package com.sportforce3d.model;

public record PpqhPaymentInstructions(String ppqhYapePhone, String ppqhYapeHolder, String ppqhPlinPhone, String ppqhPlinHolder,
                                  String ppqhBankName, String ppqhBankHolder, String ppqhBankAccount, String ppqhBankCci,
                                  boolean ppqhReviewed) {
    public String ppqhForMethod(PpqhPaymentMethod ppqhMethod) {
        return switch (ppqhMethod) {
            case YAPE -> "Yapea al " + ppqhYapePhone + " (" + ppqhYapeHolder + ").";
            case PLIN -> "Paga con Plin al " + ppqhPlinPhone + " (" + ppqhPlinHolder + ").";
            case TRANSFERENCIA -> "Transfiere a " + ppqhBankName + ", cuenta " + ppqhBankAccount + ", CCI " + ppqhBankCci
                    + " (" + ppqhBankHolder + ").";
            case TARJETA -> "Pago con tarjeta: próximamente.";
        };
    }
}
