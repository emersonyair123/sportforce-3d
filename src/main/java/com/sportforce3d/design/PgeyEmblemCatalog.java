package com.sportforce3d.design;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PgeyEmblemCatalog {

    public static final String PGEY_SOURCE_PRIMARY = "#1F5FFF";
    public static final String PGEY_SOURCE_ACCENT = "#39FF88";

    public static final Map<String, String> PGEY_EMBLEMS = pgeyEmblems();

    private PgeyEmblemCatalog() {
    }

    private static Map<String, String> pgeyEmblems() {
        Map<String, String> pgeyM = new LinkedHashMap<>();
        pgeyM.put("escudo-clasico", "Escudo clásico");
        pgeyM.put("rayo", "Rayo");
        pgeyM.put("alas", "Alas");
        pgeyM.put("hexagono", "Hexágono");
        pgeyM.put("llama", "Llama");
        pgeyM.put("corona", "Corona");
        pgeyM.put("pelota", "Pelota");
        pgeyM.put("chevron", "Chevron");
        return java.util.Collections.unmodifiableMap(pgeyM);
    }

    public static String pgeyResource(String pgeyKey) {
        return "/emblems/" + pgeyKey + ".svg";
    }

    public static String pgeyLabel(String pgeyKey) {
        return PGEY_EMBLEMS.getOrDefault(pgeyKey, "Emblema");
    }

    public static boolean pgeyExists(String pgeyKey) {
        return PGEY_EMBLEMS.containsKey(pgeyKey);
    }
}
