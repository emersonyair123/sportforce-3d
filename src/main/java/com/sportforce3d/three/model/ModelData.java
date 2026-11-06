package com.sportforce3d.three.model;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo completo sin objetos de JavaFX (se puede construir en segundo plano): partes del maniquí,
 * partes de cada zona y los puntos a los que la cámara enfoca cada prenda (espacio del modelo, metros).
 */
public record ModelData(BodyType bodyType, List<Part> mannequin, Map<Zone, List<Part>> zones,
                        Map<Garment, Focus> focus, double height) {

    /** Una malla con nombre (para depurar y para la ficha técnica). */
    public record Part(String name, MeshData mesh) {
    }

    /** Hacia dónde mira la cámara y a qué distancia al enfocar una prenda. */
    public record Focus(Vec3 target, double distance, double pitch) {
    }

    public ModelData {
        mannequin = List.copyOf(mannequin);
        Map<Zone, List<Part>> copy = new EnumMap<>(Zone.class);
        zones.forEach((z, parts) -> copy.put(z, List.copyOf(parts)));
        zones = copy;
        focus = Map.copyOf(focus);
    }

    public int totalFaces() {
        int faces = mannequin.stream().mapToInt(p -> p.mesh().faceCount()).sum();
        for (List<Part> parts : zones.values()) {
            faces += parts.stream().mapToInt(p -> p.mesh().faceCount()).sum();
        }
        return faces;
    }
}
