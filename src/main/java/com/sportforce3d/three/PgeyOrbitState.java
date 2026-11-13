package com.sportforce3d.three;

import com.sportforce3d.three.geom.PgeyVec3;

public final class PgeyOrbitState {

    public static final double PGEY_MIN_PITCH = -12;
    public static final double PGEY_MAX_PITCH = 42;

    private double pgeyYaw;
    private double pgeyPitch;
    private double pgeyDistance;
    private PgeyVec3 pgeyTarget;
    private double pgeyMinDistance = 0.7;
    private double pgeyMaxDistance = 6.0;

    public PgeyOrbitState(double pgeyYaw, double pgeyPitch, double pgeyDistance, PgeyVec3 pgeyTarget) {
        this.pgeyYaw = pgeyYaw;
        this.pgeyPitch = pgeyPitch;
        this.pgeyDistance = pgeyDistance;
        this.pgeyTarget = pgeyTarget;
        pgeyClamp();
    }

    public PgeyOrbitState pgeyCopy() {
        PgeyOrbitState pgeyO = new PgeyOrbitState(pgeyYaw, pgeyPitch, pgeyDistance, pgeyTarget);
        pgeyO.pgeySetDistanceLimits(pgeyMinDistance, pgeyMaxDistance);
        return pgeyO;
    }

    public void pgeySetDistanceLimits(double pgeyMin, double pgeyMax) {
        this.pgeyMinDistance = pgeyMin;
        this.pgeyMaxDistance = pgeyMax;
        pgeyClamp();
    }

    public void pgeySet(PgeyOrbitState pgeyO) {
        pgeyYaw = pgeyO.pgeyYaw;
        pgeyPitch = pgeyO.pgeyPitch;
        pgeyDistance = pgeyO.pgeyDistance;
        pgeyTarget = pgeyO.pgeyTarget;
        pgeyClamp();
    }

    public void pgeyRotate(double pgeyDYaw, double pgeyDPitch) {
        pgeyYaw += pgeyDYaw;
        pgeyPitch += pgeyDPitch;
        pgeyClamp();
    }

    public void pgeyZoom(double pgeyFactor) {
        pgeyDistance *= pgeyFactor;
        pgeyClamp();
    }

    public void pgeyPan(double pgeyDy) {
        pgeyTarget = pgeyTarget.pgeyAdd(new PgeyVec3(0, pgeyDy, 0));
    }

    public void pgeyAimAt(double pgeyNewYaw, double pgeyNewPitch, double pgeyNewDistance, PgeyVec3 pgeyNewTarget) {
        double pgeyDelta = ((pgeyNewYaw - pgeyYaw) % 360 + 540) % 360 - 180;
        pgeyYaw += pgeyDelta;
        pgeyPitch = pgeyNewPitch;
        pgeyDistance = pgeyNewDistance;
        pgeyTarget = pgeyNewTarget;
        pgeyClamp();
    }

    public void pgeyApproach(PgeyOrbitState pgeyGoal, double pgeyAlpha) {
        pgeyYaw += (pgeyGoal.pgeyYaw - pgeyYaw) * pgeyAlpha;
        pgeyPitch += (pgeyGoal.pgeyPitch - pgeyPitch) * pgeyAlpha;
        pgeyDistance += (pgeyGoal.pgeyDistance - pgeyDistance) * pgeyAlpha;
        pgeyTarget = pgeyTarget.pgeyLerp(pgeyGoal.pgeyTarget, pgeyAlpha);
    }

    public static double pgeyAlpha(double pgeyDtSeconds, double pgeyRate) {
        return 1 - Math.exp(-pgeyRate * pgeyDtSeconds);
    }

    public boolean pgeyCloseTo(PgeyOrbitState pgeyO) {
        return Math.abs(pgeyYaw - pgeyO.pgeyYaw) < 0.01 && Math.abs(pgeyPitch - pgeyO.pgeyPitch) < 0.01
                && Math.abs(pgeyDistance - pgeyO.pgeyDistance) < 0.0005 && pgeyTarget.pgeySub(pgeyO.pgeyTarget).pgeyLength() < 0.0005;
    }

    private void pgeyClamp() {
        pgeyPitch = Math.max(PGEY_MIN_PITCH, Math.min(PGEY_MAX_PITCH, pgeyPitch));
        pgeyDistance = Math.max(pgeyMinDistance, Math.min(pgeyMaxDistance, pgeyDistance));
    }

    public double pgeyYaw() {
        return pgeyYaw;
    }

    public double pgeyPitch() {
        return pgeyPitch;
    }

    public double pgeyDistance() {
        return pgeyDistance;
    }

    public PgeyVec3 pgeyTarget() {
        return pgeyTarget;
    }

    public double pgeyMinDistance() {
        return pgeyMinDistance;
    }

    public double pgeyMaxDistance() {
        return pgeyMaxDistance;
    }
}
