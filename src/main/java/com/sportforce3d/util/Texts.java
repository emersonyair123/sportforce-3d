package com.sportforce3d.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Texts {

    public static final Locale ES_PE = Locale.forLanguageTag("es-PE");

    private Texts() {
    }

    public static String searchKey(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    public static String upper(String text) {
        return text == null ? "" : text.toUpperCase(ES_PE);
    }

    public static String skuPart(String text) {
        return searchKey(text).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "");
    }
}
