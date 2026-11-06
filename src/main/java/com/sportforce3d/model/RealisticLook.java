package com.sportforce3d.model;

import java.util.Map;

/**
 * Cómo se ve un producto en la vista realista: modelo de {@code models/realista}, color por zona y diseño
 * (columnas {@code products.model_key}, {@code zone_colors}, {@code pattern}, {@code pattern_color}).
 *
 * @param zoneColors zona → {@code #rrggbb}
 * @param pattern    código del diseño ({@code liso}, {@code rayas}…; ver {@code render.RealisticPattern})
 */
public record RealisticLook(String modelKey, Map<String, String> zoneColors, String pattern, String patternColor) {

    public RealisticLook {
        zoneColors = zoneColors == null ? Map.of() : Map.copyOf(zoneColors);
        pattern = pattern == null || pattern.isBlank() ? "liso" : pattern;
    }
}
