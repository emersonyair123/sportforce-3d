package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.MeshData;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.List;

public record HumanModel(Persona persona, List<Part> parts, double minY, double height) {
    public record Part(String name, MeshData mesh, Color diffuse, Color specular, double specularPower, Image texture,
                       boolean transparent) {
    }

    public HumanModel {
        parts = List.copyOf(parts);
    }
}
