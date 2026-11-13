package com.sportforce3d.model;

import java.util.Map;

public record RealisticLook(String modelKey, Map<String, String> zoneColors, String pattern, String patternColor) {

    public RealisticLook {
        zoneColors = zoneColors == null ? Map.of() : Map.copyOf(zoneColors);
        pattern = pattern == null || pattern.isBlank() ? "liso" : pattern;
    }
}
