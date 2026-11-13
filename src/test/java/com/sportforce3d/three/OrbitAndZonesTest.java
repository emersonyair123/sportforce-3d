package com.sportforce3d.three;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.Zone;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OrbitAndZonesTest {

    @Test
    void pitchAndDistanceAreClamped() {
        OrbitState s = new OrbitState(0, 0, 3, Vec3.ZERO);
        s.setDistanceLimits(1, 5);
        s.rotate(0, 500);
        assertThat(s.pitch()).isEqualTo(OrbitState.MAX_PITCH);
        s.rotate(0, -500);
        assertThat(s.pitch()).isEqualTo(OrbitState.MIN_PITCH);
        s.zoom(100);
        assertThat(s.distance()).isEqualTo(5);
        s.zoom(0.0001);
        assertThat(s.distance()).isEqualTo(1);
    }

    @Test
    void aimTakesTheShortestTurn() {
        OrbitState s = new OrbitState(350, 5, 3, Vec3.ZERO);
        s.aimAt(10, 5, 3, Vec3.ZERO);
        assertThat(s.yaw()).isCloseTo(370, within(1e-9));
        s.aimAt(ViewPreset.BACK.yaw(), 5, 3, Vec3.ZERO);
        assertThat(Math.abs(s.yaw() - 370)).isLessThanOrEqualTo(180);
    }

    @Test
    void approachConvergesSmoothly() {
        OrbitState current = new OrbitState(0, 0, 4, Vec3.ZERO);
        OrbitState goal = new OrbitState(180, 20, 2, new Vec3(0, 1, 0));
        double frame = 1 / 60.0;
        for (int i = 0; i < 20; i++) {
            current.approach(goal, OrbitState.alpha(frame, 11));
        }
        assertThat(current.yaw()).isGreaterThan(170);
        for (int i = 0; i < 120; i++) {
            current.approach(goal, OrbitState.alpha(frame, 11));
        }
        assertThat(current.closeTo(goal)).isTrue();
    }

    @Test
    void zonesResolveBlenderNames() {
        assertThat(Zone.fromMeshName("jersey_body")).contains(Zone.JERSEY_BODY);
        assertThat(Zone.fromMeshName("Shoe_Sole.001")).contains(Zone.SHOE_SOLE);
        assertThat(Zone.fromMeshName("shoe_sole_L")).contains(Zone.SHOE_SOLE);
        assertThat(Zone.fromMeshName("jersey_sides")).contains(Zone.JERSEY_SIDES);
        assertThat(Zone.fromMeshName("camiseta")).isEmpty();
        assertThat(Zone.values()).hasSize(15);
        assertThat(Zone.SOCKS_BAND.garment()).isEqualTo(Garment.SOCKS);
    }
}
