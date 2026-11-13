package com.sportforce3d.design;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class PgeyRealisticModels {

    private static volatile Map<String, List<String>> pgeyFront;
    private static volatile java.util.Set<String> pgeyBacks;

    private PgeyRealisticModels() {
    }

    public static boolean pgeyExists(String pgeyFrontKey) {
        return pgeyAll().containsKey(pgeyFrontKey);
    }

    public static boolean pgeyHasBack(String pgeyFrontKey) {
        pgeyAll();
        return pgeyFrontKey != null && pgeyBacks.contains(pgeyFrontKey.replaceFirst("_frente$", "_espalda"));
    }

    public static List<String> pgeyZones(String pgeyFrontKey) {
        return pgeyAll().getOrDefault(pgeyFrontKey, List.of());
    }

    private static Map<String, List<String>> pgeyAll() {
        Map<String, List<String>> pgeyM = pgeyFront;
        if (pgeyM == null) {
            try (InputStream pgeyIn = PgeyRealisticModels.class.getResourceAsStream("/models/realista/modelos.json")) {
                Map<String, List<String>> pgeyOut = new TreeMap<>();
                java.util.Set<String> pgeyBack = new java.util.HashSet<>();
                if (pgeyIn != null) {
                    JsonNode pgeyRoot = new ObjectMapper().readTree(pgeyIn);
                    for (Map.Entry<String, JsonNode> pgeyE : pgeyRoot.properties()) {
                        if ("frente".equals(pgeyE.getValue().path("vista").asText())) {
                            List<String> pgeyZones = new ArrayList<>();
                            pgeyE.getValue().path("zonas").forEach(pgeyZ -> pgeyZones.add(pgeyZ.asText()));
                            pgeyOut.put(pgeyE.getKey(), List.copyOf(pgeyZones));
                        } else if ("espalda".equals(pgeyE.getValue().path("vista").asText())) {
                            pgeyBack.add(pgeyE.getKey());
                        }
                    }
                }
                pgeyM = Map.copyOf(pgeyOut);
                pgeyBacks = java.util.Set.copyOf(pgeyBack);
                pgeyFront = pgeyM;
            } catch (IOException pgeyE) {
                throw new UncheckedIOException("No se pudo leer modelos.json", pgeyE);
            }
        }
        return pgeyM;
    }
}
