package com.sportforce3d.three.model;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record ModelData(BodyType bodyType, List<Part> mannequin, Map<Zone, List<Part>> zones,
                        Map<Garment, Focus> focus, double height) {
    public record Part(String name, MeshData mesh) {
    }

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
