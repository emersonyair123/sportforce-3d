package com.sportforce3d.three;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.human.ClothShading;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

final class ClothLook {
    static final Color POLYESTER_SPECULAR = Color.gray(0.24);
    static final double POLYESTER_POWER = 20;
    private static final double DESATURATE = 0.45;
    private static final double WRINKLE_RELIEF = 0.55;
    private static final double FABRIC_ON_SIMULATED = 0.35;
    private static final int SMOOTH_RADIUS = 12;

    private final Map<Garment, ClothShading> shading;
    private final Map<Garment, float[]> shadeFull = new EnumMap<>(Garment.class);
    private final Map<Garment, int[]> pixels = new EnumMap<>(Garment.class);
    private final Map<Garment, WritableImage[]> outputs = new EnumMap<>(Garment.class);
    private final Map<Garment, Integer> next = new EnumMap<>(Garment.class);
    private final Map<Garment, Map<Image, Image>> bumps = new EnumMap<>(Garment.class);
    private final Map<Garment, Image> flatBumps = new EnumMap<>(Garment.class);

    ClothLook(Map<Garment, ClothShading> shading) {
        this.shading = shading;
    }

    boolean covers(Garment g) {
        return shading.containsKey(g);
    }

    Image shade(Garment g, Image texture) {
        ClothShading sh = shading.get(g);
        if (sh == null || texture == null) {
            return texture;
        }
        int w = (int) texture.getWidth();
        int h = (int) texture.getHeight();
        float[] s = shadeFull.computeIfAbsent(g, k -> upsample(sh, w, h));
        if (s.length != w * h) {
            s = upsample(sh, w, h);
            shadeFull.put(g, s);
        }
        int[] buf = pixels.computeIfAbsent(g, k -> new int[w * h]);
        if (buf.length != w * h) {
            buf = new int[w * h];
            pixels.put(g, buf);
        }
        texture.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), buf, 0, w);
        for (int i = 0; i < buf.length; i++) {
            int c = buf[i];
            double f = s[i];
            double r = (c >> 16) & 0xFF;
            double gr = (c >> 8) & 0xFF;
            double b = c & 0xFF;
            double lum = 0.30 * r + 0.59 * gr + 0.11 * b;
            double k = Math.max(0, 1 - f) * DESATURATE;
            r = (r + (lum - r) * k) * f;
            gr = (gr + (lum - gr) * k) * f;
            b = (b + (lum - b) * k) * f;
            buf[i] = (c & 0xFF000000) | (clamp(r) << 16) | (clamp(gr) << 8) | clamp(b);
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

    Image bump(Garment g, Image base) {
        ClothShading sh = shading.get(g);
        if (sh == null) {
            return base;
        }
        if (base == null) {
            return flatBumps.computeIfAbsent(g, k -> combine(sh.withSmoothedNormals(SMOOTH_RADIUS), null, 2048));
        }
        return bumps.computeIfAbsent(g, k -> new IdentityHashMap<>())
                .computeIfAbsent(base, b -> combine(sh, b, (int) b.getWidth()));
    }

    private static WritableImage combine(ClothShading sh, Image base, int size) {
        int[] src = new int[size * size];
        if (base != null) {
            base.getPixelReader().getPixels(0, 0, size, size, PixelFormat.getIntArgbInstance(), src, 0, size);
        }
        int[] out = new int[size * size];
        double e = 1.0 / sh.size();
        for (int y = 0; y < size; y++) {
            double v = (y + 0.5) / size;
            for (int x = 0; x < size; x++) {
                double u = (x + 0.5) / size;
                double nx;
                double ny;
                double nz;
                if (base != null) {
                    int c = src[y * size + x];
                    nx = ((c >> 16) & 0xFF) / 255.0 * 2 - 1;
                    ny = ((c >> 8) & 0xFF) / 255.0 * 2 - 1;
                    nz = (c & 0xFF) / 255.0 * 2 - 1;
                } else {
                    nx = 0;
                    ny = 0;
                    nz = 1;
                }
                double[] baked = sh.normalAt(u, v);
                if (baked != null) {
                    nx = nx * FABRIC_ON_SIMULATED + baked[0];
                    ny = ny * FABRIC_ON_SIMULATED + baked[1];
                    nz = nz * baked[2];
                } else {
                    double dhdu = (sh.heightAt(u + e, v) - sh.heightAt(u - e, v)) / 2;
                    double dhdv = (sh.heightAt(u, v + e) - sh.heightAt(u, v - e)) / 2;
                    nx -= dhdu * WRINKLE_RELIEF;
                    ny -= dhdv * WRINKLE_RELIEF;
                }
                double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
                int r = clamp((nx / len * 0.5 + 0.5) * 255);
                int gg = clamp((ny / len * 0.5 + 0.5) * 255);
                int b = clamp((nz / len * 0.5 + 0.5) * 255);
                out[y * size + x] = 0xFF000000 | (r << 16) | (gg << 8) | b;
            }
        }
        WritableImage img = new WritableImage(size, size);
        img.getPixelWriter().setPixels(0, 0, size, size, PixelFormat.getIntArgbInstance(), out, 0, size);
        return img;
    }

    private static float[] upsample(ClothShading sh, int w, int h) {
        float[] out = new float[w * h];
        for (int y = 0; y < h; y++) {
            double v = (y + 0.5) / h;
            for (int x = 0; x < w; x++) {
                out[y * w + x] = (float) sh.shadeAt((x + 0.5) / w, v);
            }
        }
        return out;
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }
}
