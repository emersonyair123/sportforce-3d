package com.sportforce3d.three;

import com.sportforce3d.three.model.PgeyUvLayout;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PerforationTest {

    @Test
    void holesOnlyInsideTheGivenIslands() {
        int size = 512;
        float[] mask = PgeyPerforation.pgeyMask(size, size, List.of(PgeyUvLayout.PGEY_JERSEY_FRONT));
        int inside = 0;
        int holes = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float m = mask[y * size + x];
                boolean in = PgeyUvLayout.PGEY_JERSEY_FRONT.pgeyU0() * size - 1 <= x && x <= PgeyUvLayout.PGEY_JERSEY_FRONT.pgeyU1() * size + 1
                        && PgeyUvLayout.PGEY_JERSEY_FRONT.pgeyV0() * size - 1 <= y && y <= PgeyUvLayout.PGEY_JERSEY_FRONT.pgeyV1() * size + 1;
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
        assertThat(PgeyPerforation.pgeyHole(0.5, 0.5)).isEqualTo(1.0);
        assertThat(PgeyPerforation.pgeyHole(0.0, 0.5)).isZero();
        assertThat(PgeyPerforation.pgeyHole(0.0, 1.5)).isEqualTo(1.0);
    }
}
