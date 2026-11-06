package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.MeshData;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Persona 3D lista para el visor: una malla por material (piel, ojos, pelo…) con su textura ya cargada. Se crea
 * en segundo plano ({@link HumanModels#load}); los {@code MeshView} se arman en el hilo de JavaFX.
 *
 * @param parts  primero las partes opacas y al final las que tienen transparencia (pelo, ojos): JavaFX no ordena
 *               por profundidad, así que lo transparente se dibuja después de lo que tapa
 * @param height altura en metros (los pies en Y = {@code minY})
 */
public record HumanModel(Persona persona, List<Part> parts, double minY, double height) {

    /**
     * @param name          nombre del material en el MTL
     * @param diffuse       Kd del MTL (tiñe la textura)
     * @param specular      brillo especular
     * @param specularPower dureza del brillo
     * @param texture       map_Kd o null
     * @param transparent   la textura tiene canal alfa (pelo, pestañas, córnea)
     */
    public record Part(String name, MeshData mesh, Color diffuse, Color specular, double specularPower, Image texture,
                       boolean transparent) {
    }

    public HumanModel {
        parts = List.copyOf(parts);
    }
}
