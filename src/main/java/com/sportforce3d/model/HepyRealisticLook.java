package com.sportforce3d.model;

import java.util.Map;

public record HepyRealisticLook(String hepyModelKey, Map<String, String> hepyZoneColors, String hepyPattern, String hepyPatternColor) {

    public HepyRealisticLook {
        hepyZoneColors = hepyZoneColors == null ? Map.of() : Map.copyOf(hepyZoneColors);
        hepyPattern = hepyPattern == null || hepyPattern.isBlank() ? "liso" : hepyPattern;
    }
}
