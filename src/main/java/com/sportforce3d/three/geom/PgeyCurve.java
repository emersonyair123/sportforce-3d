package com.sportforce3d.three.geom;

import java.util.Arrays;

public final class PgeyCurve {

    private final double[] pgeyXs;
    private final double[] pgeyYs;
    private final double[] pgeyMs;

    private PgeyCurve(double[] pgeyXs, double[] pgeyYs) {
        this.pgeyXs = pgeyXs;
        this.pgeyYs = pgeyYs;
        this.pgeyMs = pgeySlopes(pgeyXs, pgeyYs);
    }

    public static PgeyCurve pgeyOf(double... pgeyPairs) {
        if (pgeyPairs.length < 4 || pgeyPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Se necesitan al menos 2 pares (x, y)");
        }
        int pgeyN = pgeyPairs.length / 2;
        double[] pgeyXs = new double[pgeyN];
        double[] pgeyYs = new double[pgeyN];
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            pgeyXs[pgeyI] = pgeyPairs[2 * pgeyI];
            pgeyYs[pgeyI] = pgeyPairs[2 * pgeyI + 1];
            if (pgeyI > 0 && pgeyXs[pgeyI] <= pgeyXs[pgeyI - 1]) {
                throw new IllegalArgumentException("Las x deben ser crecientes: " + Arrays.toString(pgeyXs));
            }
        }
        return new PgeyCurve(pgeyXs, pgeyYs);
    }

    public PgeyCurve pgeyScaled(double pgeyK) {
        double[] pgeyY2 = new double[pgeyYs.length];
        for (int pgeyI = 0; pgeyI < pgeyYs.length; pgeyI++) {
            pgeyY2[pgeyI] = pgeyYs[pgeyI] * pgeyK;
        }
        return new PgeyCurve(pgeyXs.clone(), pgeyY2);
    }

    public double pgeyMinX() {
        return pgeyXs[0];
    }

    public double pgeyMaxX() {
        return pgeyXs[pgeyXs.length - 1];
    }

    public double pgeyAt(double pgeyX) {
        int pgeyN = pgeyXs.length;
        if (pgeyX <= pgeyXs[0]) {
            return pgeyYs[0];
        }
        if (pgeyX >= pgeyXs[pgeyN - 1]) {
            return pgeyYs[pgeyN - 1];
        }
        int pgeyK = Arrays.binarySearch(pgeyXs, pgeyX);
        if (pgeyK >= 0) {
            return pgeyYs[pgeyK];
        }
        pgeyK = -pgeyK - 2;
        double pgeyH = pgeyXs[pgeyK + 1] - pgeyXs[pgeyK];
        double pgeyT = (pgeyX - pgeyXs[pgeyK]) / pgeyH;
        double pgeyT2 = pgeyT * pgeyT;
        double pgeyT3 = pgeyT2 * pgeyT;
        double pgeyH00 = 2 * pgeyT3 - 3 * pgeyT2 + 1;
        double pgeyH10 = pgeyT3 - 2 * pgeyT2 + pgeyT;
        double pgeyH01 = -2 * pgeyT3 + 3 * pgeyT2;
        double pgeyH11 = pgeyT3 - pgeyT2;
        return pgeyH00 * pgeyYs[pgeyK] + pgeyH10 * pgeyH * pgeyMs[pgeyK] + pgeyH01 * pgeyYs[pgeyK + 1] + pgeyH11 * pgeyH * pgeyMs[pgeyK + 1];
    }

    private static double[] pgeySlopes(double[] pgeyXs, double[] pgeyYs) {
        int pgeyN = pgeyXs.length;
        double[] pgeyD = new double[pgeyN - 1];
        double[] pgeyH = new double[pgeyN - 1];
        for (int pgeyI = 0; pgeyI < pgeyN - 1; pgeyI++) {
            pgeyH[pgeyI] = pgeyXs[pgeyI + 1] - pgeyXs[pgeyI];
            pgeyD[pgeyI] = (pgeyYs[pgeyI + 1] - pgeyYs[pgeyI]) / pgeyH[pgeyI];
        }
        double[] pgeyM = new double[pgeyN];
        pgeyM[0] = pgeyD[0];
        pgeyM[pgeyN - 1] = pgeyD[pgeyN - 2];
        for (int pgeyI = 1; pgeyI < pgeyN - 1; pgeyI++) {
            if (pgeyD[pgeyI - 1] * pgeyD[pgeyI] <= 0) {
                pgeyM[pgeyI] = 0;
            } else {
                double pgeyW1 = 2 * pgeyH[pgeyI] + pgeyH[pgeyI - 1];
                double pgeyW2 = pgeyH[pgeyI] + 2 * pgeyH[pgeyI - 1];
                pgeyM[pgeyI] = (pgeyW1 + pgeyW2) / (pgeyW1 / pgeyD[pgeyI - 1] + pgeyW2 / pgeyD[pgeyI]);
            }
        }
        if (pgeyN > 2) {
            pgeyM[0] = pgeyEndSlope(pgeyH[0], pgeyH[1], pgeyD[0], pgeyD[1]);
            pgeyM[pgeyN - 1] = pgeyEndSlope(pgeyH[pgeyN - 2], pgeyH[pgeyN - 3], pgeyD[pgeyN - 2], pgeyD[pgeyN - 3]);
        }
        return pgeyM;
    }

    private static double pgeyEndSlope(double pgeyH0, double pgeyH1, double pgeyD0, double pgeyD1) {
        double pgeyM = ((2 * pgeyH0 + pgeyH1) * pgeyD0 - pgeyH0 * pgeyD1) / (pgeyH0 + pgeyH1);
        if (Math.signum(pgeyM) != Math.signum(pgeyD0)) {
            return 0;
        }
        if (Math.signum(pgeyD0) != Math.signum(pgeyD1) && Math.abs(pgeyM) > Math.abs(3 * pgeyD0)) {
            return 3 * pgeyD0;
        }
        return pgeyM;
    }

    public static double pgeySmoothstep(double pgeyEdge0, double pgeyEdge1, double pgeyX) {
        double pgeyT = Math.max(0, Math.min(1, (pgeyX - pgeyEdge0) / (pgeyEdge1 - pgeyEdge0)));
        return pgeyT * pgeyT * (3 - 2 * pgeyT);
    }

    public static double pgeyBell(double pgeyX, double pgeyCenter, double pgeySigma) {
        double pgeyD = (pgeyX - pgeyCenter) / pgeySigma;
        return Math.exp(-0.5 * pgeyD * pgeyD);
    }

    public static double pgeyAngleDiff(double pgeyA, double pgeyB) {
        double pgeyD = (pgeyA - pgeyB) % (2 * Math.PI);
        if (pgeyD > Math.PI) {
            pgeyD -= 2 * Math.PI;
        } else if (pgeyD <= -Math.PI) {
            pgeyD += 2 * Math.PI;
        }
        return pgeyD;
    }

    public static double pgeyAngularBell(double pgeyAngle, double pgeyCenter, double pgeySigma) {
        return pgeyBell(pgeyAngleDiff(pgeyAngle, pgeyCenter), 0, pgeySigma);
    }
}
