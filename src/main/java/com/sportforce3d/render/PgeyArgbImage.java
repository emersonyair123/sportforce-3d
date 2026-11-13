package com.sportforce3d.render;

public record PgeyArgbImage(int pgeyWidth, int pgeyHeight, int[] pgeyPixels) {

    public PgeyArgbImage {
        if (pgeyWidth <= 0 || pgeyHeight <= 0 || pgeyPixels == null || pgeyPixels.length != pgeyWidth * pgeyHeight) {
            throw new IllegalArgumentException("Tamaño de imagen inválido");
        }
    }

    public int pgeyArgb(int pgeyX, int pgeyY) {
        return pgeyPixels[pgeyY * pgeyWidth + pgeyX];
    }

    public long pgeyBytes() {
        return 4L * pgeyPixels.length;
    }
}
