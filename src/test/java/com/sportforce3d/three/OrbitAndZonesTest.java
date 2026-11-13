package com.sportforce3d.three;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyZone;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OrbitAndZonesTest {

    @Test
    void pitchAndDistanceAreClamped() {
        PgeyOrbitState s = new PgeyOrbitState(0, 0, 3, PgeyVec3.PGEY_ZERO);
        s.pgeySetDistanceLimits(1, 5);
        s.pgeyRotate(0, 500);
        assertThat(s.pgeyPitch()).isEqualTo(PgeyOrbitState.PGEY_MAX_PITCH);
        s.pgeyRotate(0, -500);
        assertThat(s.pgeyPitch()).isEqualTo(PgeyOrbitState.PGEY_MIN_PITCH);
        s.pgeyZoom(100);
        assertThat(s.pgeyDistance()).isEqualTo(5);
        s.pgeyZoom(0.0001);
        assertThat(s.pgeyDistance()).isEqualTo(1);
    }

    @Test
    void aimTakesTheShortestTurn() {
        PgeyOrbitState s = new PgeyOrbitState(350, 5, 3, PgeyVec3.PGEY_ZERO);
        s.pgeyAimAt(10, 5, 3, PgeyVec3.PGEY_ZERO);
        assertThat(s.pgeyYaw()).isCloseTo(370, within(1e-9));
        s.pgeyAimAt(PgeyViewPreset.BACK.pgeyYaw(), 5, 3, PgeyVec3.PGEY_ZERO);
        assertThat(Math.abs(s.pgeyYaw() - 370)).isLessThanOrEqualTo(180);
    }

    @Test
    void approachConvergesSmoothly() {
        PgeyOrbitState current = new PgeyOrbitState(0, 0, 4, PgeyVec3.PGEY_ZERO);
        PgeyOrbitState goal = new PgeyOrbitState(180, 20, 2, new PgeyVec3(0, 1, 0));
        double frame = 1 / 60.0;
        for (int i = 0; i < 20; i++) {
            current.pgeyApproach(goal, PgeyOrbitState.pgeyAlpha(frame, 11));
        }
        assertThat(current.pgeyYaw()).isGreaterThan(170);
        for (int i = 0; i < 120; i++) {
            current.pgeyApproach(goal, PgeyOrbitState.pgeyAlpha(frame, 11));
        }
        assertThat(current.pgeyCloseTo(goal)).isTrue();
    }

    @Test
    void zonesResolveBlenderNames() {
        assertThat(PgeyZone.pgeyFromMeshName("jersey_body")).contains(PgeyZone.JERSEY_BODY);
        assertThat(PgeyZone.pgeyFromMeshName("Shoe_Sole.001")).contains(PgeyZone.SHOE_SOLE);
        assertThat(PgeyZone.pgeyFromMeshName("shoe_sole_L")).contains(PgeyZone.SHOE_SOLE);
        assertThat(PgeyZone.pgeyFromMeshName("jersey_sides")).contains(PgeyZone.JERSEY_SIDES);
        assertThat(PgeyZone.pgeyFromMeshName("camiseta")).isEmpty();
        assertThat(PgeyZone.values()).hasSize(15);
        assertThat(PgeyZone.SOCKS_BAND.pgeyGarment()).isEqualTo(HepyGarment.SOCKS);
    }
}
