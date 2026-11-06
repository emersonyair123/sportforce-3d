package com.sportforce3d.render;

/**
 * Canal alfa (0–255) de una capa, recortado a la caja donde no es cero. Así una máscara de medias no ocupa
 * 720 × 900 bytes y el recoloreado solo recorre su rectángulo.
 *
 * @param x      columna izquierda de la caja en la imagen completa
 * @param y      fila superior de la caja
 * @param width  ancho de la caja (0 si la máscara está vacía)
 * @param height alto de la caja
 * @param alpha  alfa de cada píxel de la caja, fila por fila
 */
public record Mask(int x, int y, int width, int height, byte[] alpha) {

    public static final Mask EMPTY = new Mask(0, 0, 0, 0, new byte[0]);

    public Mask {
        if (alpha == null || alpha.length != width * height) {
            throw new IllegalArgumentException("Máscara inválida");
        }
    }

    public boolean isEmpty() {
        return width == 0 || height == 0;
    }

    /** Alfa (0–255) en coordenadas de la imagen completa. */
    public int at(int px, int py) {
        int lx = px - x;
        int ly = py - y;
        if (lx < 0 || ly < 0 || lx >= width || ly >= height) {
            return 0;
        }
        return alpha[ly * width + lx] & 0xFF;
    }

    /** Toma el alfa de una imagen ARGB y lo recorta a su caja. */
    public static Mask fromAlpha(ArgbImage img) {
        return crop(img, true);
    }

    /**
     * Recorta una imagen a la caja donde su alfa no es cero. Con {@code alphaChannel} guarda el alfa; si no,
     * el gris (promedio de R, G y B) de los mismos píxeles: así la capa de detalle se guarda en dos máscaras
     * con la misma caja.
     */
    static Mask crop(ArgbImage img, boolean alphaChannel) {
        return crop(img, alphaChannel, bounds(img));
    }

    static int[] bounds(ArgbImage img) {
        int w = img.width();
        int h = img.height();
        int[] p = img.pixels();
        int x0 = w;
        int y0 = h;
        int x1 = -1;
        int y1 = -1;
        for (int yy = 0; yy < h; yy++) {
            int row = yy * w;
            for (int xx = 0; xx < w; xx++) {
                if ((p[row + xx] >>> 24) != 0) {
                    if (xx < x0) {
                        x0 = xx;
                    }
                    if (xx > x1) {
                        x1 = xx;
                    }
                    if (yy < y0) {
                        y0 = yy;
                    }
                    y1 = yy;
                }
            }
        }
        return x1 < 0 ? null : new int[]{x0, y0, x1 - x0 + 1, y1 - y0 + 1};
    }

    static Mask crop(ArgbImage img, boolean alphaChannel, int[] box) {
        if (box == null) {
            return EMPTY;
        }
        int w = img.width();
        int[] p = img.pixels();
        byte[] out = new byte[box[2] * box[3]];
        for (int yy = 0; yy < box[3]; yy++) {
            int src = (box[1] + yy) * w + box[0];
            int dst = yy * box[2];
            for (int xx = 0; xx < box[2]; xx++) {
                int c = p[src + xx];
                int v = alphaChannel ? c >>> 24 : (((c >> 16) & 0xFF) + ((c >> 8) & 0xFF) + (c & 0xFF)) / 3;
                out[dst + xx] = (byte) v;
            }
        }
        return new Mask(box[0], box[1], box[2], box[3], out);
    }
}
