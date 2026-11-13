package com.sportforce3d.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PpqhValidators {

    private static final Pattern PPQH_EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PPQH_PHONE = Pattern.compile("^\\+?[0-9 ]{6,20}$");

    public static final int PPQH_PASSWORD_MIN = 8;
    public static final int PPQH_PASSWORD_MAX = 72;

    private PpqhValidators() {
    }

    public static String ppqhNormalizeEmail(String ppqhEmail) {
        return ppqhEmail == null ? "" : ppqhEmail.trim().toLowerCase(Locale.ROOT);
    }

    public static String ppqhEmail(String ppqhEmail) {
        String ppqhValue = ppqhNormalizeEmail(ppqhEmail);
        if (ppqhValue.isEmpty()) {
            return "Ingresa tu correo.";
        }
        return PPQH_EMAIL.matcher(ppqhValue).matches() ? null : "Ingresa un correo válido, p. ej. nombre@correo.com.";
    }

    public static String ppqhFullName(String ppqhName) {
        String ppqhValue = ppqhName == null ? "" : ppqhName.trim();
        if (ppqhValue.length() < 3) {
            return "Ingresa tu nombre completo.";
        }
        if (ppqhValue.length() > 80) {
            return "El nombre admite hasta 80 caracteres.";
        }
        return ppqhPlainText(ppqhValue, false);
    }

    public static String ppqhPhone(String ppqhPhone) {
        if (ppqhPhone == null || ppqhPhone.isBlank()) {
            return null;
        }
        return PPQH_PHONE.matcher(ppqhPhone.trim()).matches() ? null : "Ingresa solo números (6 a 20 dígitos).";
    }

    public static String ppqhNewPassword(char[] ppqhPassword) {
        if (ppqhPassword == null || ppqhPassword.length < PPQH_PASSWORD_MIN) {
            return "Usa al menos " + PPQH_PASSWORD_MIN + " caracteres.";
        }
        if (ppqhPassword.length > PPQH_PASSWORD_MAX || ppqhUtf8Length(ppqhPassword) > PPQH_PASSWORD_MAX) {
            return "Usa como máximo " + PPQH_PASSWORD_MAX + " caracteres (menos si llevan tildes o ñ).";
        }
        for (char ppqhCh : ppqhPassword) {
            if (Character.isISOControl(ppqhCh)) {
                return "La contraseña no puede tener saltos de línea ni caracteres de control.";
            }
        }
        boolean ppqhLetter = false;
        boolean ppqhDigit = false;
        for (char ppqhCh : ppqhPassword) {
            ppqhLetter |= Character.isLetter(ppqhCh);
            ppqhDigit |= Character.isDigit(ppqhCh);
        }
        return ppqhLetter && ppqhDigit ? null : "Combina letras y números.";
    }

    public static int ppqhUtf8Length(char[] ppqhChars) {
        int ppqhBytes = 0;
        for (int ppqhI = 0; ppqhI < ppqhChars.length; ppqhI++) {
            char ppqhCh = ppqhChars[ppqhI];
            if (ppqhCh < 0x80) {
                ppqhBytes += 1;
            } else if (ppqhCh < 0x800) {
                ppqhBytes += 2;
            } else if (Character.isHighSurrogate(ppqhCh) && ppqhI + 1 < ppqhChars.length && Character.isLowSurrogate(ppqhChars[ppqhI + 1])) {
                ppqhBytes += 4;
                ppqhI++;
            } else {
                ppqhBytes += 3;
            }
        }
        return ppqhBytes;
    }

    public static String ppqhPlainText(String ppqhValue, boolean ppqhMultiline) {
        if (ppqhValue == null) {
            return null;
        }
        for (int ppqhI = 0; ppqhI < ppqhValue.length(); ppqhI++) {
            char ppqhCh = ppqhValue.charAt(ppqhI);
            boolean ppqhAllowed = ppqhMultiline && (ppqhCh == '\n' || ppqhCh == '\r' || ppqhCh == '\t');
            if ((Character.isISOControl(ppqhCh) && !ppqhAllowed) || ppqhCh == ' ' || ppqhCh == ' ') {
                return "Quita los caracteres especiales invisibles del texto.";
            }
        }
        return null;
    }

    public static String ppqhBlankToNull(String ppqhValue) {
        return ppqhValue == null || ppqhValue.isBlank() ? null : ppqhValue.trim();
    }
}
