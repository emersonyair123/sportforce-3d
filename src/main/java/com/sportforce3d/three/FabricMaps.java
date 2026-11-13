package com.sportforce3d.three;

import com.sportforce3d.model.Material;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.EnumMap;
import java.util.Map;

public final class FabricMaps {

    private static final Map<Material.Pattern, WritableImage> CACHE = new EnumMap<>(Material.Pattern.class);

    private FabricMaps() {
    }

    public static WritableImage dryFit() {
        return forPattern(Material.Pattern.DOTS);
    }

    public static synchronized WritableImage forPattern(Material.Pattern pattern) {
        if (CACHE.containsKey(pattern)) {
            return CACHE.get(pattern);
        }
        WritableImage img = switch (pattern) {
            case DOTS -> normalMap(2048, 2.0, 0.035, (u, v) -> {
                double row = Math.floor(v);
                double fu = u + (row % 2 == 0 ? 0 : 0.5);
                double du = fu - Math.floor(fu) - 0.5;
                double dv = v - row - 0.5;
                return -Math.exp(-(du * du + dv * dv * 1.3) * 18);
            });
            case MESH -> normalMap(2048, Perforation.CELL_PX, 0.12, (u, v) -> -Perforation.hole(u, v));
            case PLAIN -> normalMap(2048, 2.5, 0.14, (u, v) -> Math.abs(u - Math.floor(u) - 0.5)
                    + Math.sin(v * Math.PI * 2) * 0.25 + noise(u, v) * 0.15);
            case DIAGONAL -> normalMap(2048, 2.5, 0.035, (u, v) -> Math.sin((u + v) * Math.PI * 2) * 0.5);
            case LINES -> normalMap(2048, 1.75, 0.04, (u, v) -> Math.sin(v * Math.PI * 2) * 0.5);
            case GRID -> normalMap(2048, 1.5, 0.03, (u, v) -> Math.sin(u * Math.PI * 2) * Math.sin(v * Math.PI * 2));
            case KNIT -> normalMap(1024, 6.0, 0.18, (u, v) -> Math.abs(((u % 1) + 1) % 1 - 0.5)
                    + Math.sin(v * Math.PI * 2) * 0.25);
            case GRAIN -> normalMap(1024, 5.0, 0.12, FabricMaps::noise);
            case RUBBER -> normalMap(1024, 8.0, 0.14, (u, v) -> Math.sin(u * Math.PI * 2) * Math.cos(v * Math.PI));
            case SMOOTH, STUDS, REFLECTIVE -> null;
        };
        CACHE.put(pattern, img);
        return img;
    }

    private static double noise(double u, double v) {
        double s = Math.sin(u * 12.9898 + v * 78.233) * 43758.5453;
        double base = s - Math.floor(s);
        return (base + Math.sin(u * 2.1) * Math.cos(v * 1.7)) * 0.4;
    }

    interface Height {
        double at(double u, double v);
    }

    static WritableImage normalMap(int size, double cellPx, double strength, Height h) {
        WritableImage img = new WritableImage(size, size);
        PixelWriter w = img.getPixelWriter();
        double cells = size / cellPx;
        double e = 1.0 / size;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double u = (double) x / size * cells;
                double v = (double) y / size * cells;
                double dhdu = (h.at(u + e * cells, v) - h.at(u - e * cells, v)) / 2;
                double dhdv = (h.at(u, v + e * cells) - h.at(u, v - e * cells)) / 2;
                double nx = -dhdu * strength * 6;
                double ny = -dhdv * strength * 6;
                double len = Math.sqrt(nx * nx + ny * ny + 1);
                int r = (int) Math.round((nx / len * 0.5 + 0.5) * 255);
                int g = (int) Math.round((ny / len * 0.5 + 0.5) * 255);
                int b = (int) Math.round((1 / len * 0.5 + 0.5) * 255);
                w.setArgb(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }
}
