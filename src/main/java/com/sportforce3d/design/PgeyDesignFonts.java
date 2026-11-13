package com.sportforce3d.design;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PgeyDesignFonts {

    public static final String PGEY_DEFAULT = "BARLOW_CONDENSED_800";

    public static final Map<String, String> PGEY_LABELS = pgeyLabels();

    private PgeyDesignFonts() {
    }

    private static Map<String, String> pgeyLabels() {
        Map<String, String> pgeyM = new LinkedHashMap<>();
        pgeyM.put("BARLOW_CONDENSED_800", "Barlow Condensed 800");
        pgeyM.put("BARLOW_CONDENSED_700", "Barlow Condensed 700");
        pgeyM.put("BARLOW_600", "Barlow 600");
        pgeyM.put("JETBRAINS_MONO", "JetBrains Mono");
        return java.util.Collections.unmodifiableMap(pgeyM);
    }

    public static String pgeyFamily(String pgeyKey) {
        return switch (pgeyKey == null ? PGEY_DEFAULT : pgeyKey) {
            case "BARLOW_CONDENSED_700" -> "Barlow Condensed";
            case "BARLOW_600" -> "Barlow SemiBold";
            case "JETBRAINS_MONO" -> "JetBrains Mono Medium";
            default -> "Barlow Condensed ExtraBold";
        };
    }

    public static String pgeyFile(String pgeyKey) {
        return switch (pgeyKey == null ? PGEY_DEFAULT : pgeyKey) {
            case "BARLOW_CONDENSED_700" -> "BarlowCondensed-Bold.ttf";
            case "BARLOW_600" -> "Barlow-SemiBold.ttf";
            case "JETBRAINS_MONO" -> "JetBrainsMono-Medium.ttf";
            default -> "BarlowCondensed-ExtraBold.ttf";
        };
    }

    public static boolean pgeyBold(String pgeyKey) {
        return "BARLOW_CONDENSED_700".equals(pgeyKey);
    }

    public static boolean pgeyIsValid(String pgeyKey) {
        return pgeyKey != null && PGEY_LABELS.containsKey(pgeyKey);
    }
}
