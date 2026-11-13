package com.sportforce3d.three;

import com.sportforce3d.model.Material;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public record MaterialLook(Image bump, Color specular, double power, boolean perforated) {

    public static MaterialLook of(Material m) {
        double gloss = 1 - m.roughness();
        double intensity = 0.05 + m.sheen() * 0.30 + gloss * 0.22 + m.metalness() * 0.55;
        double power = 6 + gloss * gloss * 90 + m.metalness() * 40;
        return new MaterialLook(FabricMaps.forPattern(m.previewPattern()),
                Color.gray(Math.min(0.85, intensity)), power, m.previewPattern() == Material.Pattern.MESH);
    }
}
