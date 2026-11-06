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

/**
 * Qué fotos de frente tiene la Vista realista y qué zonas recolorea cada una ({@code models/realista/modelos.json}).
 * Solo datos: la sincronización ({@link ViewSync}) lo usa para elegir la foto más parecida al diseño.
 */
public final class RealisticModels {

    private static volatile Map<String, List<String>> front;
    private static volatile java.util.Set<String> backs;

    private RealisticModels() {
    }

    public static boolean exists(String frontKey) {
        return all().containsKey(frontKey);
    }

    /** true si la foto de frente tiene su espalda ({@code <corte>_<categoría>_espalda}). */
    public static boolean hasBack(String frontKey) {
        all();
        return frontKey != null && backs.contains(frontKey.replaceFirst("_frente$", "_espalda"));
    }

    /** Zonas de la foto ({@code camiseta}, {@code short}, {@code paneles}, {@code medias}, {@code principal}). */
    public static List<String> zones(String frontKey) {
        return all().getOrDefault(frontKey, List.of());
    }

    private static Map<String, List<String>> all() {
        Map<String, List<String>> m = front;
        if (m == null) {
            try (InputStream in = RealisticModels.class.getResourceAsStream("/models/realista/modelos.json")) {
                Map<String, List<String>> out = new TreeMap<>();
                java.util.Set<String> back = new java.util.HashSet<>();
                if (in != null) {
                    JsonNode root = new ObjectMapper().readTree(in);
                    for (Map.Entry<String, JsonNode> e : root.properties()) {
                        if ("frente".equals(e.getValue().path("vista").asText())) {
                            List<String> zones = new ArrayList<>();
                            e.getValue().path("zonas").forEach(z -> zones.add(z.asText()));
                            out.put(e.getKey(), List.copyOf(zones));
                        } else if ("espalda".equals(e.getValue().path("vista").asText())) {
                            back.add(e.getKey());
                        }
                    }
                }
                m = Map.copyOf(out);
                backs = java.util.Set.copyOf(back);
                front = m;
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo leer modelos.json", e);
            }
        }
        return m;
    }
}