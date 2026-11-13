package com.sportforce3d.three;

import com.sportforce3d.three.model.UvLayout;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PerforationTest {

    @Test
    void holesOnlyInsideTheGivenIslands() {
        int size = 512;
        float[] mask = Perforation.mask(size, size, List.of(UvLayout.JERSEY_FRONT));
        int inside = 0;
        int holes = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float m = mask[y * size + x];
                boolean in = UvLayout.JERSEY_FRONT.u0() * size - 1 <= x && x <= UvLayout.JERSEY_FRONT.u1() * size + 1
                        && UvLayout.JERSEY_FRONT.v0() * size - 1 <= y && y <= UvLayout.JERSEY_FRONT.v1() * size + 1;
                if (!in) {
                    assertThat(m).as("fuera de la isla (%d, %d)", x, y).isZero();
                } else {
                    inside++;
                    if (m > 0.5f) {
                        holes++;
                    }
                }
            }
        }
        assertThat((double) holes / inside).isBetween(0.10, 0.30);
    }

    @Test
    void holeIsCenteredInTheCellAndRowsAreStaggered() {
        assertThat(Perforation.hole(0.5, 0.5)).isEqualTo(1.0);
        assertThat(Perforation.hole(0.0, 0.5)).isZero();
        assertThat(Perforation.hole(0.0, 1.5)).isEqualTo(1.0);
    }
}
