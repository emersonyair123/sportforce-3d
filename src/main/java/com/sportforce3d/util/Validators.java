package com.sportforce3d.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Validators {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ]{6,20}$");

    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 72;

    private Validators() {
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String email(String email) {
        String value = normalizeEmail(email);
        if (value.isEmpty()) {
            return "Ingresa tu correo.";
        }
        return EMAIL.matcher(value).matches() ? null : "Ingresa un correo válido, p. ej. nombre@correo.com.";
    }

    public static String fullName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.length() < 3) {
            return "Ingresa tu nombre completo.";
        }
        if (value.length() > 80) {
            return "El nombre admite hasta 80 caracteres.";
        }
        return plainText(value, false);
    }

    public static String phone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return PHONE.matcher(phone.trim()).matches() ? null : "Ingresa solo números (6 a 20 dígitos).";
    }

    public static String newPassword(char[] password) {
        if (password == null || password.length < PASSWORD_MIN) {
            return "Usa al menos " + PASSWORD_MIN + " caracteres.";
        }
        if (password.length > PASSWORD_MAX || utf8Length(password) > PASSWORD_MAX) {
            return "Usa como máximo " + PASSWORD_MAX + " caracteres (menos si llevan tildes o ñ).";
        }
        for (char ch : password) {
            if (Character.isISOControl(ch)) {
                return "La contraseña no puede tener saltos de línea ni caracteres de control.";
            }
        }
        boolean letter = false;
        boolean digit = false;
        for (char ch : password) {
            letter |= Character.isLetter(ch);
            digit |= Character.isDigit(ch);
        }
        return letter && digit ? null : "Combina letras y números.";
    }

    public static int utf8Length(char[] chars) {
        int bytes = 0;
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (ch < 0x80) {
                bytes += 1;
            } else if (ch < 0x800) {
                bytes += 2;
            } else if (Character.isHighSurrogate(ch) && i + 1 < chars.length && Character.isLowSurrogate(chars[i + 1])) {
                bytes += 4;
                i++;
            } else {
                bytes += 3;
            }
        }
        return bytes;
    }

    public static String plainText(String value, boolean multiline) {
        if (value == null) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean allowed = multiline && (ch == '\n' || ch == '\r' || ch == '\t');
            if ((Character.isISOControl(ch) && !allowed) || ch == ' ' || ch == ' ') {
                return "Quita los caracteres especiales invisibles del texto.";
            }
        }
        return null;
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
