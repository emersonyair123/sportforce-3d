package com.sportforce3d.render;

public record ArgbImage(int width, int height, int[] pixels) {

    public ArgbImage {
        if (width <= 0 || height <= 0 || pixels == null || pixels.length != width * height) {
            throw new IllegalArgumentException("Tamaño de imagen inválido");
        }
    }

    public int argb(int x, int y) {
        return pixels[y * width + x];
    }

    public long bytes() {
        return 4L * pixels.length;
    }
}
