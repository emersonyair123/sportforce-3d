package com.sportforce3d.three.human;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ClothShadingSmoothTest {

    private static final int FLAT = 0xFF8080FF;
    private static final int TILTED = 0xFFC080E0;

    @Test
    void aSharpStepBecomesGradualAndAFlatMapStaysFlat() {
        int size = 32;
        int[] normals = new int[size * size];
        for (int i = 0; i < normals.length; i++) {
            normals[i] = (i % size) < size / 2 ? FLAT : TILTED;
        }
        PgeyClothShading sharp = new PgeyClothShading(size, new float[size * size], new float[size * size], normals);
        PgeyClothShading smooth = sharp.pgeyWithSmoothedNormals(3);

        double v = 0.5 / size;
        double jumpBefore = maxJump(sharp, v, size);
        double jumpAfter = maxJump(smooth, v, size);
        assertThat(jumpAfter).isLessThan(jumpBefore / 4);
        assertThat(smooth.pgeyNormalAt(2.5 / size, v)[0]).isCloseTo(sharp.pgeyNormalAt(2.5 / size, v)[0], within(0.02));

        int[] flat = new int[size * size];
        java.util.Arrays.fill(flat, FLAT);
        PgeyClothShading flatSmooth = new PgeyClothShading(size, new float[size * size], new float[size * size], flat)
                .pgeyWithSmoothedNormals(3);
        assertThat(flatSmooth.pgeyNormals()).containsOnly(FLAT);
        assertThat(new PgeyClothShading(size, new float[size * size], new float[size * size]).pgeyWithSmoothedNormals(3)
                .pgeyNormals()).isNull();
    }

    private static double maxJump(PgeyClothShading s, double v, int size) {
        double max = 0;
        for (int x = 0; x < size - 1; x++) {
            double a = s.pgeyNormalAt((x + 0.5) / size, v)[0];
            double b = s.pgeyNormalAt((x + 1.5) / size, v)[0];
            max = Math.max(max, Math.abs(b - a));
        }
        return max;
    }
}
