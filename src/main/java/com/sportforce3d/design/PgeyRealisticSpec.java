package com.sportforce3d.design;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.sportforce3d.util.PpqhValidators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record PgeyRealisticSpec(@JsonProperty("modelKey") String pgeyModelKey, @JsonProperty("zoneColors") Map<String, String> pgeyZoneColors, @JsonProperty("pattern") String pgeyPattern, @JsonProperty("patternColor") String pgeyPatternColor,
                            @JsonProperty("name") String pgeyName, @JsonProperty("number") Integer pgeyNumber) {

    public static final String PGEY_DEFAULT_MODEL = "futbol_hombre_frente";
    public static final List<String> PGEY_PATTERNS =
            List.of("liso", "rayas", "franjas", "dinamico", "geometrico", "minimal", "banda");
    public static final int PGEY_NAME_MAX = 12;

    public PgeyRealisticSpec {
        pgeyModelKey = pgeyModelKey == null || pgeyModelKey.isBlank() ? PGEY_DEFAULT_MODEL : pgeyModelKey;
        TreeMap<String, String> pgeyC = new TreeMap<>();
        if (pgeyZoneColors != null) {
            pgeyZoneColors.forEach((pgeyZ, pgeyHex) -> pgeyC.put(pgeyZ, pgeyHex == null ? null : pgeyHex.toUpperCase(Locale.ROOT)));
        }
        pgeyC.values().removeIf(Objects::isNull);
        pgeyZoneColors = Collections.unmodifiableMap(pgeyC);
        pgeyPattern = pgeyPattern == null || pgeyPattern.isBlank() ? "liso" : pgeyPattern.toLowerCase(Locale.ROOT);
        pgeyPatternColor = pgeyPatternColor == null ? null : pgeyPatternColor.toUpperCase(Locale.ROOT);
        pgeyName = pgeyName == null || pgeyName.isBlank() ? null : pgeyName;
    }

    public static PgeyRealisticSpec pgeyDefaults() {
        return new PgeyRealisticSpec(PGEY_DEFAULT_MODEL, Map.of("camiseta", "#1F5FFF", "short", "#0B0E13",
                "paneles", "#39FF88", "medias", "#1F5FFF", "principal", "#1F5FFF"), "liso", "#F2F5F8", null, null);
    }

    public String pgeyColor(String pgeyZone) {
        return pgeyZoneColors.getOrDefault(pgeyZone, "#F2F5F8");
    }

    public boolean pgeyPlain() {
        return "liso".equals(pgeyPattern);
    }

    public PgeyRealisticSpec pgeyWithModel(String pgeyKey) {
        return new PgeyRealisticSpec(pgeyKey, pgeyZoneColors, pgeyPattern, pgeyPatternColor, pgeyName, pgeyNumber);
    }

    public PgeyRealisticSpec pgeyWithColor(String pgeyZone, String pgeyHex) {
        Map<String, String> pgeyC = new TreeMap<>(pgeyZoneColors);
        pgeyC.put(pgeyZone, pgeyHex);
        return new PgeyRealisticSpec(pgeyModelKey, pgeyC, pgeyPattern, pgeyPatternColor, pgeyName, pgeyNumber);
    }

    public PgeyRealisticSpec pgeyWithPattern(String pgeyCode) {
        return new PgeyRealisticSpec(pgeyModelKey, pgeyZoneColors, pgeyCode, pgeyPatternColor, pgeyName, pgeyNumber);
    }

    public PgeyRealisticSpec pgeyWithPatternColor(String pgeyHex) {
        return new PgeyRealisticSpec(pgeyModelKey, pgeyZoneColors, pgeyPattern, pgeyHex, pgeyName, pgeyNumber);
    }

    public PgeyRealisticSpec pgeyWithName(String pgeyText) {
        return new PgeyRealisticSpec(pgeyModelKey, pgeyZoneColors, pgeyPattern, pgeyPatternColor, pgeyText, pgeyNumber);
    }

    public PgeyRealisticSpec pgeyWithNumber(Integer pgeyValue) {
        return new PgeyRealisticSpec(pgeyModelKey, pgeyZoneColors, pgeyPattern, pgeyPatternColor, pgeyName, pgeyValue);
    }

    public List<String> pgeyProblems() {
        List<String> pgeyOut = new ArrayList<>();
        if (!pgeyModelKey.matches("[a-z]+(_[a-z]+)*")) {
            pgeyOut.add("El modelo de la vista realista no es válido.");
        }
        pgeyZoneColors.forEach((pgeyZ, pgeyHex) -> {
            if (!pgeyHex.matches("#[0-9A-F]{6}")) {
                pgeyOut.add("El color de " + pgeyZ + " no es válido.");
            }
        });
        if (!PGEY_PATTERNS.contains(pgeyPattern)) {
            pgeyOut.add("El diseño de la vista realista no es válido.");
        }
        if (!pgeyPlain() && (pgeyPatternColor == null || !pgeyPatternColor.matches("#[0-9A-F]{6}"))) {
            pgeyOut.add("Elige el color del diseño.");
        }
        if (pgeyName != null && (pgeyName.codePointCount(0, pgeyName.length()) > PGEY_NAME_MAX
                || PpqhValidators.ppqhPlainText(pgeyName, false) != null)) {
            pgeyOut.add("El nombre admite hasta " + PGEY_NAME_MAX + " caracteres visibles.");
        }
        if (pgeyNumber != null && (pgeyNumber < 0 || pgeyNumber > 99)) {
            pgeyOut.add("El número va de 0 a 99.");
        }
        return pgeyOut;
    }
}
