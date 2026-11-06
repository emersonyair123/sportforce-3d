package com.sportforce3d.design;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Reglas del diseñador (design/prendas-y-materiales.md). Las usa el service al guardar y la vista al editar. */
public final class DesignRules {

    public static final int NAME_MAX = 12;
    public static final int NUMBER_MAX = 99;
    public static final int MAX_ELEMENTS = 30;
    private static final Pattern HEX = Pattern.compile("^#[0-9A-F]{6}$");
    private static final Pattern NUMBER = Pattern.compile("^[0-9]{1,2}$");

    private DesignRules() {
    }

    /** Nombre de jugador: mayúsculas, máx. 12 caracteres (letras, espacios, punto, guion y apóstrofo). */
    public static String cleanName(String text) {
        String t = text == null ? "" : text.toUpperCase(java.util.Locale.forLanguageTag("es-PE"));
        t = t.replaceAll("[^A-ZÁÉÍÓÚÜÑ .'\\-]", "");
        return t.length() > NAME_MAX ? t.substring(0, NAME_MAX) : t;
    }

    /** Número 0–99 o null si no es válido. */
    public static String cleanNumber(String text) {
        String t = text == null ? "" : text.replaceAll("[^0-9]", "");
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > 2) {
            t = t.substring(0, 2);
        }
        return String.valueOf(Integer.parseInt(t));
    }

    public static boolean isHex(String s) {
        return s != null && HEX.matcher(s).matches();
    }

    /** Lista de problemas; vacía si el diseño es válido. */
    public static List<String> problems(DesignDocument d) {
        List<String> out = new ArrayList<>();
        if (d.schemaVersion() != DesignDocument.SCHEMA_VERSION) {
            out.add("Versión de diseño no compatible: " + d.schemaVersion());
        }
        d.colors().forEach((zone, hex) -> {
            if (!isHex(hex)) {
                out.add("Color inválido en " + zone.label() + ": " + hex);
            }
        });
        d.patterns().forEach((g, p) -> {
            if (!isHex(p.color())) {
                out.add("Color del patrón inválido: " + p.color());
            }
        });
        if (d.realistic() != null) {
            out.addAll(d.realistic().problems());
        }
        if (d.elements().size() > MAX_ELEMENTS) {
            out.add("Un diseño admite hasta " + MAX_ELEMENTS + " elementos.");
        }
        for (DesignElement e : d.elements()) {
            if (e.area() == null || e.type() == null) {
                out.add("Elemento sin zona o tipo.");
                continue;
            }
            switch (e.type()) {
                case NAME -> {
                    if (e.text() == null || e.text().length() > NAME_MAX) {
                        out.add("El nombre admite hasta " + NAME_MAX + " caracteres.");
                    }
                }
                case NUMBER -> {
                    if (e.text() == null || !NUMBER.matcher(e.text()).matches()
                            || Integer.parseInt(e.text()) > NUMBER_MAX) {
                        out.add("El número va de 0 a " + NUMBER_MAX + ".");
                    }
                }
                case EMBLEM -> {
                    if (!EmblemCatalog.exists(e.emblem())) {
                        out.add("Emblema desconocido: " + e.emblem());
                    }
                }
                case LOGO -> {
                    if (e.assetId() == null) {
                        out.add("Logo sin archivo.");
                    }
                }
            }
            if (e.type().isText() && !DesignFonts.isValid(e.font())) {
                out.add("Tipografía no disponible: " + e.font());
            }
            if (e.color() != null && !isHex(e.color()) || e.accentColor() != null && !isHex(e.accentColor())) {
                out.add("Color inválido en " + e.displayName());
            }
        }
        return out;
    }
}