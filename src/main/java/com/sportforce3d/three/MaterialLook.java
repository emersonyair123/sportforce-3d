package com.sportforce3d.three;

import com.sportforce3d.model.Material;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Cómo se ve un material en JavaFX (Phong). Traduce los parámetros PBR del documento de materiales
 * (roughness, sheen, metalness) a brillo especular y agrega el relieve de la tela. {@code perforated}: la tela tiene
 * agujeros que se ven (malla deportiva) y se pintan en la textura ({@link Perforation}).
 */
public record MaterialLook(Image bump, Color specular, double power, boolean perforated) {

    public static MaterialLook of(Material m) {
        double gloss = 1 - m.roughness();
        double intensity = 0.05 + m.sheen() * 0.30 + gloss * 0.22 + m.metalness() * 0.55;
        double power = 6 + gloss * gloss * 90 + m.metalness() * 40;
        return new MaterialLook(FabricMaps.forPattern(m.previewPattern()),
                Color.gray(Math.min(0.85, intensity)), power, m.previewPattern() == Material.Pattern.MESH);
    }
}
