package com.sportforce3d.render;

/**
 * Imagen en memoria como arreglo {@code int[]} ARGB sin premultiplicar (el mismo formato que
 * {@code PixelFormat.getIntArgbInstance()}), fila por fila. No depende del hilo de JavaFX.
 */
public record ArgbImage(int width, int height, int[] pixels) {

    public ArgbImage {
        if (width <= 0 || height <= 0 || pixels == null || pixels.length != width * height) {
            throw new IllegalArgumentException("Tamaño de imagen inválido");
        }
    }

    public int argb(int x, int y) {
        return pixels[y * width + x];
    }

    /** Memoria aproximada en bytes (para la caché). */
    public long bytes() {
        return 4L * pixels.length;
    }
}
