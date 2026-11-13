package com.sportforce3d.design;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DesignFonts {

    public static final String DEFAULT = "BARLOW_CONDENSED_800";

    public static final Map<String, String> LABELS = labels();

    private DesignFonts() {
    }

    private static Map<String, String> labels() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("BARLOW_CONDENSED_800", "Barlow Condensed 800");
        m.put("BARLOW_CONDENSED_700", "Barlow Condensed 700");
        m.put("BARLOW_600", "Barlow 600");
        m.put("JETBRAINS_MONO", "JetBrains Mono");
        return java.util.Collections.unmodifiableMap(m);
    }

    public static String family(String key) {
        return switch (key == null ? DEFAULT : key) {
            case "BARLOW_CONDENSED_700" -> "Barlow Condensed";
            case "BARLOW_600" -> "Barlow SemiBold";
            case "JETBRAINS_MONO" -> "JetBrains Mono Medium";
            default -> "Barlow Condensed ExtraBold";
        };
    }

    public static String file(String key) {
        return switch (key == null ? DEFAULT : key) {
            case "BARLOW_CONDENSED_700" -> "BarlowCondensed-Bold.ttf";
            case "BARLOW_600" -> "Barlow-SemiBold.ttf";
            case "JETBRAINS_MONO" -> "JetBrainsMono-Medium.ttf";
            default -> "BarlowCondensed-ExtraBold.ttf";
        };
    }

    public static boolean bold(String key) {
        return "BARLOW_CONDENSED_700".equals(key);
    }

    public static boolean isValid(String key) {
        return key != null && LABELS.containsKey(key);
    }
}
