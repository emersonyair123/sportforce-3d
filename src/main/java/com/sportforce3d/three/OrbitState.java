package com.sportforce3d.three;

import com.sportforce3d.three.geom.Vec3;

public final class OrbitState {

    public static final double MIN_PITCH = -12;
    public static final double MAX_PITCH = 42;

    private double yaw;
    private double pitch;
    private double distance;
    private Vec3 target;
    private double minDistance = 0.7;
    private double maxDistance = 6.0;

    public OrbitState(double yaw, double pitch, double distance, Vec3 target) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.distance = distance;
        this.target = target;
        clamp();
    }

    public OrbitState copy() {
        OrbitState o = new OrbitState(yaw, pitch, distance, target);
        o.setDistanceLimits(minDistance, maxDistance);
        return o;
    }

    public void setDistanceLimits(double min, double max) {
        this.minDistance = min;
        this.maxDistance = max;
        clamp();
    }

    public void set(OrbitState o) {
        yaw = o.yaw;
        pitch = o.pitch;
        distance = o.distance;
        target = o.target;
        clamp();
    }

    public void rotate(double dYaw, double dPitch) {
        yaw += dYaw;
        pitch += dPitch;
        clamp();
    }

    public void zoom(double factor) {
        distance *= factor;
        clamp();
    }

    public void pan(double dy) {
        target = target.add(new Vec3(0, dy, 0));
    }

    public void aimAt(double newYaw, double newPitch, double newDistance, Vec3 newTarget) {
        double delta = ((newYaw - yaw) % 360 + 540) % 360 - 180;
        yaw += delta;
        pitch = newPitch;
        distance = newDistance;
        target = newTarget;
        clamp();
    }

    public void approach(OrbitState goal, double alpha) {
        yaw += (goal.yaw - yaw) * alpha;
        pitch += (goal.pitch - pitch) * alpha;
        distance += (goal.distance - distance) * alpha;
        target = target.lerp(goal.target, alpha);
    }

    public static double alpha(double dtSeconds, double rate) {
        return 1 - Math.exp(-rate * dtSeconds);
    }

    public boolean closeTo(OrbitState o) {
        return Math.abs(yaw - o.yaw) < 0.01 && Math.abs(pitch - o.pitch) < 0.01
                && Math.abs(distance - o.distance) < 0.0005 && target.sub(o.target).length() < 0.0005;
    }

    private void clamp() {
        pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));
        distance = Math.max(minDistance, Math.min(maxDistance, distance));
    }

    public double yaw() {
        return yaw;
    }

    public double pitch() {
        return pitch;
    }

    public double distance() {
        return distance;
    }

    public Vec3 target() {
        return target;
    }

    public double minDistance() {
        return minDistance;
    }

    public double maxDistance() {
        return maxDistance;
    }
}
