package com.sportforce3d.util;

import java.text.Normalizer;
import java.util.Locale;

public final class HepyTexts {

    public static final Locale HEPY_ES_PE = Locale.forLanguageTag("es-PE");

    private HepyTexts() {
    }

    public static String hepySearchKey(String hepyText) {
        if (hepyText == null) {
            return "";
        }
        String hepyDecomposed = Normalizer.normalize(hepyText, Normalizer.Form.NFD);
        return hepyDecomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    public static String hepyUpper(String hepyText) {
        return hepyText == null ? "" : hepyText.toUpperCase(HEPY_ES_PE);
    }

    public static String hepySkuPart(String hepyText) {
        return hepySearchKey(hepyText).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "");
    }
}
