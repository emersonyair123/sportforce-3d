package com.sportforce3d.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formato de soles de la guía: "S/ 129.90" (espacio tras S/, dos decimales, punto decimal). */
public final class Money {

    private Money() {
    }

    public static String format(long cents) {
        String sign = cents < 0 ? "-" : "";
        long abs = Math.abs(cents);
        return sign + "S/ " + groupThousands(abs / 100) + "." + String.format("%02d", abs % 100);
    }

    /** Recargo de material: "+ S/ 15.00" o "Incluido" si es cero. */
    public static String surcharge(long cents) {
        return cents == 0 ? "Incluido" : "+ " + format(cents);
    }

    /** Monto para un campo de texto editable: 12990 → "129.90". */
    public static String toInput(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }

    /**
     * Lee un monto escrito por una persona: "129.90", "129,90", "S/ 129.9", "1,299.00" o "129".
     * Devuelve los céntimos o lanza IllegalArgumentException si no es un monto válido.
     */
    public static long parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Monto vacío");
        }
        String clean = text.replace("S/", "").replace(" ", "").trim();
        if (clean.isEmpty() || !clean.matches("[0-9.,]+")) {
            throw new IllegalArgumentException("Monto inválido: " + text);
        }
        int lastDot = clean.lastIndexOf('.');
        int lastComma = clean.lastIndexOf(',');
        int decimalSep = Math.max(lastDot, lastComma);
        String normalized;
        if (decimalSep >= 0 && clean.length() - decimalSep - 1 <= 2) {
            String integer = clean.substring(0, decimalSep).replace(".", "").replace(",", "");
            normalized = (integer.isEmpty() ? "0" : integer) + "." + clean.substring(decimalSep + 1);
        } else {
            normalized = clean.replace(".", "").replace(",", "");
        }
        try {
            return new BigDecimal(normalized).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Monto inválido: " + text, e);
        }
    }

    private static String groupThousands(long value) {
        String digits = Long.toString(value);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                out.append(',');
            }
            out.append(digits.charAt(i));
        }
        return out.toString();
    }
}
