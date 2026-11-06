package com.sportforce3d.three.human;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.model.ModelProviders;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

class ShadeDumpTest {
    @Test
    void dump() throws Exception {
        var h = OutfitFitterTest.geometryOnly(Persona.HOMBRE);
        var d = OutfitFitter.fit(h, ModelProviders.load(Persona.HOMBRE.fallback()));
        for (Garment g : d.shading().keySet()) {
            ClothShading s = d.shading().get(g);
            BufferedImage img = new BufferedImage(s.size(), s.size(), BufferedImage.TYPE_INT_RGB);
            double min = 9, max = -9;
            for (int i = 0; i < s.shade().length; i++) {
                int v = (int) Math.max(0, Math.min(255, s.shade()[i] * 230));
                int hh = (int) Math.max(0, Math.min(255, 128 + s.height()[i] * 120));
                img.setRGB(i % s.size(), i / s.size(), (v << 16) | (v << 8) | hh);
                min = Math.min(min, s.shade()[i]); max = Math.max(max, s.shade()[i]);
            }
            ImageIO.write(img, "png", new File("target/shade-" + g + ".png"));
            System.out.println("SHADE " + g + " min=" + min + " max=" + max);
        }
    }
}
