package com.sportforce3d.design;

import com.sportforce3d.util.Validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Diseño de la vista realista (fotos recoloreadas, ver {@code render.RecolorService}): modelo, color por zona,
 * diseño de la camiseta, nombre y número. Inmutable; los cambios devuelven una copia.
 *
 * @param modelKey     modelo de frente de {@code models/realista/modelos.json} (la espalda se deduce)
 * @param zoneColors   zona ({@code camiseta}, {@code short}, {@code paneles}, {@code medias}, {@code principal}) →
 *                     {@code #RRGGBB}
 * @param pattern      código del diseño: {@code liso}, {@code rayas}, {@code franjas}, {@code dinamico},
 *                     {@code geometrico}, {@code minimal} o {@code banda}
 * @param patternColor color del diseño
 * @param name         nombre en la espalda (máx. 12) o null
 * @param number       número 0–99 o null
 */
public record RealisticSpec(String modelKey, Map<String, String> zoneColors, String pattern, String patternColor,
                            String name, Integer number) {

    public static final String DEFAULT_MODEL = "futbol_hombre_frente";
    public static final List<String> PATTERNS =
            List.of("liso", "rayas", "franjas", "dinamico", "geometrico", "minimal", "banda");
    public static final int NAME_MAX = 12;

    public RealisticSpec {
        modelKey = modelKey == null || modelKey.isBlank() ? DEFAULT_MODEL : modelKey;
        TreeMap<String, String> c = new TreeMap<>();
        if (zoneColors != null) {
            zoneColors.forEach((z, hex) -> c.put(z, hex == null ? null : hex.toUpperCase(Locale.ROOT)));
        }
        c.values().removeIf(Objects::isNull);
        zoneColors = Collections.unmodifiableMap(c);
        pattern = pattern == null || pattern.isBlank() ? "liso" : pattern.toLowerCase(Locale.ROOT);
        patternColor = patternColor == null ? null : patternColor.toUpperCase(Locale.ROOT);
        name = name == null || name.isBlank() ? null : name;
    }

    /** Uniforme de fútbol de hombre azul con paneles verdes, como el diseñador 3D. */
    public static RealisticSpec defaults() {
        return new RealisticSpec(DEFAULT_MODEL, Map.of("camiseta", "#1F5FFF", "short", "#0B0E13",
                "paneles", "#39FF88", "medias", "#1F5FFF", "principal", "#1F5FFF"), "liso", "#F2F5F8", null, null);
    }

    public String color(String zone) {
        return zoneColors.getOrDefault(zone, "#F2F5F8");
    }

    public boolean plain() {
        return "liso".equals(pattern);
    }

    public RealisticSpec withModel(String key) {
        return new RealisticSpec(key, zoneColors, pattern, patternColor, name, number);
    }

    public RealisticSpec withColor(String zone, String hex) {
        Map<String, String> c = new TreeMap<>(zoneColors);
        c.put(zone, hex);
        return new RealisticSpec(modelKey, c, pattern, patternColor, name, number);
    }

    public RealisticSpec withPattern(String code) {
        return new RealisticSpec(modelKey, zoneColors, code, patternColor, name, number);
    }

    public RealisticSpec withPatternColor(String hex) {
        return new RealisticSpec(modelKey, zoneColors, pattern, hex, name, number);
    }

    public RealisticSpec withName(String text) {
        return new RealisticSpec(modelKey, zoneColors, pattern, patternColor, text, number);
    }

    public RealisticSpec withNumber(Integer value) {
        return new RealisticSpec(modelKey, zoneColors, pattern, patternColor, name, value);
    }

    /** Problemas para mostrar al usuario (vacío si está bien). */
    public List<String> problems() {
        List<String> out = new ArrayList<>();
        if (!modelKey.matches("[a-z]+(_[a-z]+)*")) {
            out.add("El modelo de la vista realista no es válido.");
        }
        zoneColors.forEach((z, hex) -> {
            if (!hex.matches("#[0-9A-F]{6}")) {
                out.add("El color de " + z + " no es válido.");
            }
        });
        if (!PATTERNS.contains(pattern)) {
            out.add("El diseño de la vista realista no es válido.");
        }
        if (!plain() && (patternColor == null || !patternColor.matches("#[0-9A-F]{6}"))) {
            out.add("Elige el color del diseño.");
        }
        if (name != null && (name.codePointCount(0, name.length()) > NAME_MAX
                || Validators.plainText(name, false) != null)) {
            out.add("El nombre admite hasta " + NAME_MAX + " caracteres visibles.");
        }
        if (number != null && (number < 0 || number > 99)) {
            out.add("El número va de 0 a 99.");
        }
        return out;
    }
}
