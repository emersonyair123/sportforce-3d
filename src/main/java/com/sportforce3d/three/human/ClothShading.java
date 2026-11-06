package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.MeshData;

import java.util.List;

/**
 * Sombreado horneado de una prenda en su espacio UV (la misma distribución de islas que la textura del diseño):
 * {@code shade} multiplica la textura (oclusión ambiental y valles de los pliegues) y {@code height} es el relieve
 * de los pliegues, que se suma al de la tela. JavaFX no tiene oclusión ambiental ni colores por vértice: la única
 * forma de oscurecer axilas, cuello o entrepierna es pintarlo en la textura.
 *
 * <p>Las prendas simuladas en Blender ({@link SimulatedGarments}) traen la oclusión horneada con Cycles y un normal
 * map de sus pliegues ({@code normals}, ARGB en espacio tangente, verde hacia la v de la textura); entonces
 * {@code height} es plano y los pliegues están en la geometría y en el normal map.
 *
 * @param size    lado de los mapas en píxeles (menor que la textura: se muestrean con interpolación)
 * @param normals normal map horneado (size × size) o null
 */
public record ClothShading(int size, float[] shade, float[] height, int[] normals) {

    /** Oclusión horneada mínima (los huecos más cerrados no quedan negros: hay luz rebotada). */
    private static final double AO_FLOOR = 0.40;
    private static final double AO_GAMMA = 1.25;

    public ClothShading(int size, float[] shade, float[] height) {
        this(size, shade, height, null);
    }

    /** Datos por punto de cada malla: apertura (1 = sin oclusión), peso del pliegue y su ángulo en UV (rad). */
    record PointData(float[] openness, float[] wrinkle, float[] angle) {
    }

    /**
     * Sombreado de una prenda simulada: oclusión ambiental (gris, 1 = abierto) y normal map horneados en Blender
     * sobre las mismas UV. Las dos imágenes deben ser cuadradas y del mismo lado.
     */
    static ClothShading fromMaps(java.awt.image.BufferedImage ao, java.awt.image.BufferedImage normal) {
        int size = ao.getWidth();
        if (ao.getHeight() != size || normal.getWidth() != size || normal.getHeight() != size) {
            throw new IllegalArgumentException("ao.png y normal.png deben ser cuadradas y del mismo tamaño");
        }
        int[] a = ao.getRGB(0, 0, size, size, null, 0, size);
        int[] n = normal.getRGB(0, 0, size, size, null, 0, size);
        float[] shade = new float[size * size];
        for (int i = 0; i < a.length; i++) {
            double open = ((a[i] >> 8) & 0xFF) / 255.0;
            shade[i] = (float) (AO_FLOOR + (1 - AO_FLOOR) * Math.pow(open, AO_GAMMA));
        }
        return new ClothShading(size, shade, new float[size * size], n);
    }

    /** Normal horneada en (u, v) como (x, y, z) en [-1, 1] con interpolación bilineal; null si no hay. */
    public double[] normalAt(double u, double v) {
        if (normals == null) {
            return null;
        }
        double x = Math.max(0, Math.min(size - 1.001, u * size - 0.5));
        double y = Math.max(0, Math.min(size - 1.001, v * size - 0.5));
        int x0 = (int) x;
        int y0 = (int) y;
        double fx = x - x0;
        double fy = y - y0;
        double[] out = new double[3];
        int[] idx = {y0 * size + x0, y0 * size + x0 + 1, (y0 + 1) * size + x0, (y0 + 1) * size + x0 + 1};
        double[] w = {(1 - fx) * (1 - fy), fx * (1 - fy), (1 - fx) * fy, fx * fy};
        for (int k = 0; k < 4; k++) {
            int c = normals[idx[k]];
            out[0] += w[k] * (((c >> 16) & 0xFF) / 255.0 * 2 - 1);
            out[1] += w[k] * (((c >> 8) & 0xFF) / 255.0 * 2 - 1);
            out[2] += w[k] * ((c & 0xFF) / 255.0 * 2 - 1);
        }
        return out;
    }

    /** Valor del sombreado en (u, v) ∈ [0, 1] con interpolación bilineal. */
    public double shadeAt(double u, double v) {
        return sample(shade, u, v);
    }

    public double heightAt(double u, double v) {
        return sample(height, u, v);
    }

    private double sample(float[] map, double u, double v) {
        double x = Math.max(0, Math.min(size - 1.001, u * size - 0.5));
        double y = Math.max(0, Math.min(size - 1.001, v * size - 0.5));
        int x0 = (int) x;
        int y0 = (int) y;
        double fx = x - x0;
        double fy = y - y0;
        int i = y0 * size + x0;
        double a = map[i] * (1 - fx) + map[i + 1] * fx;
        double b = map[i + size] * (1 - fx) + map[i + size + 1] * fx;
        return a * (1 - fy) + b * fy;
    }

    /**
     * Rasteriza las mallas de una prenda en UV: interpola por triángulo la apertura, el peso y el ángulo del pliegue
     * de sus puntos; dibuja los pliegues (crestas suaves cuya intensidad varía con un ruido de baja frecuencia) y
     * extiende unos píxeles el borde de cada isla para que el filtrado de la textura no muestre costuras.
     */
    static ClothShading bake(List<MeshData> meshes, List<PointData> data, int size) {
        int n = size * size;
        float[] open = new float[n];
        float[] w = new float[n];
        float[] ca = new float[n];
        float[] sa = new float[n];
        boolean[] covered = new boolean[n];
        for (int k = 0; k < meshes.size(); k++) {
            MeshData m = meshes.get(k);
            PointData d = data.get(k);
            float[] tex = m.texCoords();
            int[] f = m.faces();
            for (int t = 0; t < m.faceCount(); t++) {
                int b = t * MeshData.FACE_STRIDE;
                int[] p = {f[b], f[b + 3], f[b + 6]};
                int[] tc = {f[b + 2], f[b + 5], f[b + 8]};
                if (p[0] >= d.openness().length || p[1] >= d.openness().length || p[2] >= d.openness().length) {
                    continue; // caras agregadas después (basta): sin datos, quedan con el valor del vecino
                }
                double[] x = new double[3];
                double[] y = new double[3];
                for (int c = 0; c < 3; c++) {
                    x[c] = tex[tc[c] * 2] * size - 0.5;
                    y[c] = tex[tc[c] * 2 + 1] * size - 0.5;
                }
                raster(x, y, size, (i, l0, l1, l2) -> {
                    open[i] = (float) (l0 * d.openness()[p[0]] + l1 * d.openness()[p[1]] + l2 * d.openness()[p[2]]);
                    w[i] = (float) (l0 * d.wrinkle()[p[0]] + l1 * d.wrinkle()[p[1]] + l2 * d.wrinkle()[p[2]]);
                    ca[i] = (float) (l0 * Math.cos(d.angle()[p[0]]) + l1 * Math.cos(d.angle()[p[1]])
                            + l2 * Math.cos(d.angle()[p[2]]));
                    sa[i] = (float) (l0 * Math.sin(d.angle()[p[0]]) + l1 * Math.sin(d.angle()[p[1]])
                            + l2 * Math.sin(d.angle()[p[2]]));
                    covered[i] = true;
                });
            }
        }
        dilate(open, covered.clone(), size, 4, 1);
        dilate(w, covered.clone(), size, 4, 0);
        float[] shade = new float[n];
        float[] height = new float[n];
        double period = size / 34.0; // ~3 cm sobre la camiseta
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int i = y * size + x;
                double len = Math.hypot(ca[i], sa[i]);
                double c = len < 1e-6 ? 0 : ca[i] / len;
                double s = len < 1e-6 ? 1 : sa[i] / len;
                double along = (x * c + y * s) / period;
                double across = (-x * s + y * c) / (period * 3.2);
                // Crestas que nacen y se apagan: la fase y la intensidad varían despacio a lo largo del pliegue.
                double wobble = 1.1 * noise(across * 0.8, along * 0.15);
                double ridge = Math.sin(2 * Math.PI * (along + wobble));
                // Solo algunos pliegues, en manchas: una tela real no se arruga en rayas parejas.
                double ruido = noise(across * 0.6 + 7.1, along * 0.45 + 3.3);
                double fade = ruido * ruido * (3 - 2 * ruido);
                fade = Math.max(0, Math.min(1, (fade - 0.25) / 0.5));
                double h = w[i] * ridge * fade;
                height[i] = (float) h;
                double ao = 0.42 + 0.58 * Math.pow(Math.max(0, Math.min(1, open[i])), 1.6);
                double fold = h < 0 ? 1 + 0.32 * h : 1 + 0.05 * h;
                shade[i] = (float) Math.max(0.35, Math.min(1.05, ao * fold));
            }
        }
        return new ClothShading(size, shade, height);
    }

    @FunctionalInterface
    private interface PixelSink {
        void put(int index, double l0, double l1, double l2);
    }

    /** Recorre los píxeles cuyo centro cae en el triángulo (coordenadas en píxeles) con sus baricéntricas. */
    private static void raster(double[] x, double[] y, int size, PixelSink sink) {
        int x0 = Math.max(0, (int) Math.floor(Math.min(x[0], Math.min(x[1], x[2]))));
        int x1 = Math.min(size - 1, (int) Math.ceil(Math.max(x[0], Math.max(x[1], x[2]))));
        int y0 = Math.max(0, (int) Math.floor(Math.min(y[0], Math.min(y[1], y[2]))));
        int y1 = Math.min(size - 1, (int) Math.ceil(Math.max(y[0], Math.max(y[1], y[2]))));
        double den = (y[1] - y[2]) * (x[0] - x[2]) + (x[2] - x[1]) * (y[0] - y[2]);
        if (Math.abs(den) < 1e-12) {
            return;
        }
        for (int py = y0; py <= y1; py++) {
            for (int px = x0; px <= x1; px++) {
                double l0 = ((y[1] - y[2]) * (px - x[2]) + (x[2] - x[1]) * (py - y[2])) / den;
                double l1 = ((y[2] - y[0]) * (px - x[2]) + (x[0] - x[2]) * (py - y[2])) / den;
                double l2 = 1 - l0 - l1;
                if (l0 >= -1e-6 && l1 >= -1e-6 && l2 >= -1e-6) {
                    sink.put(py * size + px, l0, l1, l2);
                }
            }
        }
    }

    /** Extiende los valores cubiertos a sus vecinos sin dato ({@code passes} veces); el resto toma {@code fill}. */
    private static void dilate(float[] map, boolean[] covered, int size, int passes, float fill) {
        for (int pass = 0; pass < passes; pass++) {
            boolean[] next = covered.clone();
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int i = y * size + x;
                    if (covered[i]) {
                        continue;
                    }
                    double sum = 0;
                    int c = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int xx = x + dx;
                            int yy = y + dy;
                            if (xx >= 0 && yy >= 0 && xx < size && yy < size && covered[yy * size + xx]) {
                                sum += map[yy * size + xx];
                                c++;
                            }
                        }
                    }
                    if (c > 0) {
                        map[i] = (float) (sum / c);
                        next[i] = true;
                    }
                }
            }
            System.arraycopy(next, 0, covered, 0, covered.length);
        }
        for (int i = 0; i < map.length; i++) {
            if (!covered[i]) {
                map[i] = fill;
            }
        }
    }

    /** Ruido de valor suave en [0, 1] (interpolación cúbica de una rejilla pseudoaleatoria). */
    static double noise(double x, double y) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        double fx = x - xi;
        double fy = y - yi;
        double sx = fx * fx * (3 - 2 * fx);
        double sy = fy * fy * (3 - 2 * fy);
        double a = hash(xi, yi);
        double b = hash(xi + 1, yi);
        double c = hash(xi, yi + 1);
        double d = hash(xi + 1, yi + 1);
        return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy;
    }

    private static double hash(int x, int y) {
        long h = x * 374761393L + y * 668265263L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
    }
}
