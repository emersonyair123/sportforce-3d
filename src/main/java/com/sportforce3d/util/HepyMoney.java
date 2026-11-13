package com.sportforce3d.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class HepyMoney {

    private HepyMoney() {
    }

    public static String hepyFormat(long hepyCents) {
        String hepySign = hepyCents < 0 ? "-" : "";
        long hepyAbs = Math.abs(hepyCents);
        return hepySign + "S/ " + hepyGroupThousands(hepyAbs / 100) + "." + String.format("%02d", hepyAbs % 100);
    }

    public static String hepySurcharge(long hepyCents) {
        return hepyCents == 0 ? "Incluido" : "+ " + hepyFormat(hepyCents);
    }

    public static String hepyToInput(long hepyCents) {
        return BigDecimal.valueOf(hepyCents, 2).toPlainString();
    }

    public static long hepyParse(String hepyText) {
        if (hepyText == null) {
            throw new IllegalArgumentException("Monto vacío");
        }
        String hepyClean = hepyText.replace("S/", "").replace(" ", "").trim();
        if (hepyClean.isEmpty() || !hepyClean.matches("[0-9.,]+")) {
            throw new IllegalArgumentException("Monto inválido: " + hepyText);
        }
        int hepyLastDot = hepyClean.lastIndexOf('.');
        int hepyLastComma = hepyClean.lastIndexOf(',');
        int hepyDecimalSep = Math.max(hepyLastDot, hepyLastComma);
        String hepyNormalized;
        if (hepyDecimalSep >= 0 && hepyClean.length() - hepyDecimalSep - 1 <= 2) {
            String hepyInteger = hepyClean.substring(0, hepyDecimalSep).replace(".", "").replace(",", "");
            hepyNormalized = (hepyInteger.isEmpty() ? "0" : hepyInteger) + "." + hepyClean.substring(hepyDecimalSep + 1);
        } else {
            hepyNormalized = hepyClean.replace(".", "").replace(",", "");
        }
        try {
            return new BigDecimal(hepyNormalized).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        } catch (ArithmeticException | NumberFormatException hepyE) {
            throw new IllegalArgumentException("Monto inválido: " + hepyText, hepyE);
        }
    }

    private static String hepyGroupThousands(long hepyValue) {
        String hepyDigits = Long.toString(hepyValue);
        StringBuilder hepyOut = new StringBuilder();
        for (int hepyI = 0; hepyI < hepyDigits.length(); hepyI++) {
            if (hepyI > 0 && (hepyDigits.length() - hepyI) % 3 == 0) {
                hepyOut.append(',');
            }
            hepyOut.append(hepyDigits.charAt(hepyI));
        }
        return hepyOut.toString();
    }
}
