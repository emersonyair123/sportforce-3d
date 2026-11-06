package com.sportforce3d.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Colores del tema claro (el de reportes imprimibles según la guía de marca), leídos de css/tokens-light.css para
 * no duplicar valores HEX: si cambia el token, cambia el reporte.
 */
public final class ReportPalette {

    private static final Pattern TOKEN = Pattern.compile("-sf-([a-z0-9-]+):\\s*#([0-9a-fA-F]{6})\\s*;");
    private static final Map<String, Integer> LIGHT = load("/css/tokens-light.css");

    private ReportPalette() {
    }

    /** RGB del token (sin el prefijo -sf-), p. ej. {@code rgb("brand")}. */
    public static int rgb(String token) {
        return Objects.requireNonNull(LIGHT.get(token), "Token sin color HEX en tokens-light.css: " + token);
    }

    public static String hex(String token) {
        return String.format("%06X", rgb(token));
    }

    static Map<String, Integer> load(String resource) {
        try (InputStream in = ReportPalette.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("No existe " + resource);
            }
            String css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Integer> map = new HashMap<>();
            Matcher m = TOKEN.matcher(css);
            while (m.find()) {
                map.put(m.group(1), Integer.parseInt(m.group(2), 16));
            }
            return Map.copyOf(map);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
