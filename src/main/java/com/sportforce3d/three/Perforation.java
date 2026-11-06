package com.sportforce3d.three;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.UvRect;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agujeros de la malla deportiva: el relieve solo sombrea, así que los agujeros se pintan en la textura (más oscuros:
 * se ve la sombra del interior) dentro de las islas UV de las zonas con malla. La rejilla es la misma del relieve
 * {@link FabricMaps} (celdas de {@link #CELL_PX} px en una textura de 2048 px, unos 3 mm en la camiseta). Hilo de
 * JavaFX; dos imágenes por prenda que se alternan, como el compositor.
 */
final class Perforation {

    /** Paso de la rejilla de agujeros en una textura de 2048 px. */
    static final double CELL_PX = 6.0;
    /** Cuánto se oscurece el centro de un agujero. */
    private static final double DEPTH = 0.62;

    private final Map<String, float[]> masks = new HashMap<>();
    private final Map<Garment, WritableImage[]> outputs = new EnumMap<>(Garment.class);
    private final Map<Garment, Integer> next = new EnumMap<>(Garment.class);
    private int[] buf = new int[0];

    /** 1 en el centro de un agujero y 0 fuera (borde suave); coordenadas en celdas, filas desplazadas media celda. */
    static double hole(double u, double v) {
        double row = Math.floor(v);
        double fu = u + (((long) row & 1) == 0 ? 0 : 0.5);
        double du = fu - Math.floor(fu) - 0.5;
        double dv = v - row - 0.5;
        double r = Math.sqrt(du * du + dv * dv);
        return Math.max(0, Math.min(1, (0.27 - r) / 0.08));
    }

    /** Copia de {@code texture} con los agujeros dentro de {@code rects}. */
    Image apply(Garment g, Image texture, List<UvRect> rects) {
        int w = (int) texture.getWidth();
        int h = (int) texture.getHeight();
        float[] mask = masks.computeIfAbsent(g + ":" + w + "x" + h + ":" + rects, k -> mask(w, h, rects));
        if (buf.length != w * h) {
            buf = new int[w * h];
        }
        texture.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), buf, 0, w);
        for (int i = 0; i < buf.length; i++) {
            float m = mask[i];
            if (m > 0) {
                int c = buf[i];
                double f = 1 - DEPTH * m;
                int r = (int) (((c >> 16) & 0xFF) * f);
                int gr = (int) (((c >> 8) & 0xFF) * f);
                int b = (int) ((c & 0xFF) * f);
                buf[i] = (c & 0xFF000000) | (r << 16) | (gr << 8) | b;
            }
        }
        WritableImage[] pair = outputs.computeIfAbsent(g, k -> new WritableImage[2]);
        int slot = next.getOrDefault(g, 0);
        if (pair[slot] == null || pair[slot].getWidth() != w || pair[slot].getHeight() != h) {
            pair[slot] = new WritableImage(w, h);
        }
        pair[slot].getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), buf, 0, w);
        next.put(g, 1 - slot);
        return pair[slot];
    }

    static float[] mask(int w, int h, List<UvRect> rects) {
        float[] mask = new float[w * h];
        double cell = CELL_PX * w / 2048.0;
        for (UvRect r : rects) {
            int x0 = (int) Math.floor(r.u0() * w);
            int x1 = (int) Math.ceil(r.u1() * w);
            int y0 = (int) Math.floor(r.v0() * h);
            int y1 = (int) Math.ceil(r.v1() * h);
            for (int y = Math.max(0, y0); y < Math.min(h, y1); y++) {
                for (int x = Math.max(0, x0); x < Math.min(w, x1); x++) {
                    mask[y * w + x] = (float) hole((x + 0.5) / cell, (y + 0.5) / cell);
                }
            }
        }
        return mask;
    }
}
