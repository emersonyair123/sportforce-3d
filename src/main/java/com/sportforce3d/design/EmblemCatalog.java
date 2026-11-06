package com.sportforce3d.design;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Emblemas originales para los clientes (design/emblemas, copiados en resources/emblems). No incluye el
 * monograma ni el logotipo de la tienda, ni marcas de terceros. Sus colores fijos son azul (#1F5FFF) y verde
 * (#39FF88): al usarlos se reemplazan por el color principal y el de acento del elemento.
 */
public final class EmblemCatalog {

    public static final String SOURCE_PRIMARY = "#1F5FFF";
    public static final String SOURCE_ACCENT = "#39FF88";

    public static final Map<String, String> EMBLEMS = emblems();

    private EmblemCatalog() {
    }

    private static Map<String, String> emblems() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("escudo-clasico", "Escudo clásico");
        m.put("rayo", "Rayo");
        m.put("alas", "Alas");
        m.put("hexagono", "Hexágono");
        m.put("llama", "Llama");
        m.put("corona", "Corona");
        m.put("pelota", "Pelota");
        m.put("chevron", "Chevron");
        return java.util.Collections.unmodifiableMap(m);
    }

    public static String resource(String key) {
        return "/emblems/" + key + ".svg";
    }

    public static String label(String key) {
        return EMBLEMS.getOrDefault(key, "Emblema");
    }

    public static boolean exists(String key) {
        return EMBLEMS.containsKey(key);
    }
}
