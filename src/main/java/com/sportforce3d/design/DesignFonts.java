package com.sportforce3d.design;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tipografías para nombre y número: Barlow Condensed 800 (predeterminada) y tres alternativas, todas incluidas
 * en la aplicación (licencia OFL), así el diseño se ve igual en cualquier computadora.
 */
public final class DesignFonts {

    public static final String DEFAULT = "BARLOW_CONDENSED_800";

    /** clave → etiqueta para el selector. */
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

    /** Familia registrada en JavaFX y si va en negrita (ver app/Fonts). */
    public static String family(String key) {
        return switch (key == null ? DEFAULT : key) {
            case "BARLOW_CONDENSED_700" -> "Barlow Condensed";
            case "BARLOW_600" -> "Barlow SemiBold";
            case "JETBRAINS_MONO" -> "JetBrains Mono Medium";
            default -> "Barlow Condensed ExtraBold";
        };
    }

    public static boolean bold(String key) {
        return "BARLOW_CONDENSED_700".equals(key);
    }

    public static boolean isValid(String key) {
        return key != null && LABELS.containsKey(key);
    }
}
