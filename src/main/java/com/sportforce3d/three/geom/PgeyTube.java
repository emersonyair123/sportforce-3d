package com.sportforce3d.three.geom;

import java.util.List;

public final class PgeyTube {

    private static final int PGEY_SAMPLES = 800;

    private final PgeyVec3[] pgeyPts = new PgeyVec3[PGEY_SAMPLES + 1];
    private final double[] pgeyLen = new double[PGEY_SAMPLES + 1];
    private final PgeyCurve pgeyRx;
    private final PgeyCurve pgeyRz;
    private final PgeyCurve pgeyFrontShift;
    private final PgeyVec3 pgeyOutward;

    public PgeyTube(List<PgeyVec3> pgeyControl, PgeyCurve pgeyRx, PgeyCurve pgeyRz, PgeyCurve pgeyFrontShift, PgeyVec3 pgeyOutward) {
        this.pgeyRx = pgeyRx;
        this.pgeyRz = pgeyRz;
        this.pgeyFrontShift = pgeyFrontShift;
        this.pgeyOutward = pgeyOutward.pgeyNormalize();
        int pgeyN = pgeyControl.size();
        double pgeyAcc = 0;
        for (int pgeyI = 0; pgeyI <= PGEY_SAMPLES; pgeyI++) {
            double pgeyT = (double) pgeyI / PGEY_SAMPLES * (pgeyN - 1);
            int pgeyK = Math.min((int) Math.floor(pgeyT), pgeyN - 2);
            double pgeyU = pgeyT - pgeyK;
            PgeyVec3 pgeyP0 = pgeyControl.get(Math.max(0, pgeyK - 1));
            PgeyVec3 pgeyP1 = pgeyControl.get(pgeyK);
            PgeyVec3 pgeyP2 = pgeyControl.get(pgeyK + 1);
            PgeyVec3 pgeyP3 = pgeyControl.get(Math.min(pgeyN - 1, pgeyK + 2));
            pgeyPts[pgeyI] = pgeyCatmullRom(pgeyP0, pgeyP1, pgeyP2, pgeyP3, pgeyU);
            if (pgeyI > 0) {
                pgeyAcc += pgeyPts[pgeyI].pgeySub(pgeyPts[pgeyI - 1]).pgeyLength();
            }
            pgeyLen[pgeyI] = pgeyAcc;
        }
    }

    public double pgeyLength() {
        return pgeyLen[PGEY_SAMPLES];
    }

    public PgeyVec3 pgeySpine(double pgeyF) {
        double pgeyTarget = Math.max(0, Math.min(1, pgeyF)) * pgeyLength();
        int pgeyLo = 0;
        int pgeyHi = PGEY_SAMPLES;
        while (pgeyHi - pgeyLo > 1) {
            int pgeyMid = (pgeyLo + pgeyHi) >>> 1;
            if (pgeyLen[pgeyMid] < pgeyTarget) {
                pgeyLo = pgeyMid;
            } else {
                pgeyHi = pgeyMid;
            }
        }
        double pgeySeg = pgeyLen[pgeyHi] - pgeyLen[pgeyLo];
        double pgeyT = pgeySeg < 1e-12 ? 0 : (pgeyTarget - pgeyLen[pgeyLo]) / pgeySeg;
        return pgeyPts[pgeyLo].pgeyLerp(pgeyPts[pgeyHi], pgeyT);
    }

    public PgeyVec3 pgeyTangent(double pgeyF) {
        double pgeyD = 0.002;
        PgeyVec3 pgeyA = pgeySpine(Math.max(0, pgeyF - pgeyD));
        PgeyVec3 pgeyB = pgeySpine(Math.min(1, pgeyF + pgeyD));
        return pgeyB.pgeySub(pgeyA).pgeyNormalize();
    }

    public PgeyVec3 pgeyFrontAxis(double pgeyF) {
        PgeyVec3 pgeyT = pgeyTangent(pgeyF);
        PgeyVec3 pgeyFront = PgeyVec3.PGEY_FRONT.pgeySub(pgeyT.pgeyMul(pgeyT.pgeyDot(PgeyVec3.PGEY_FRONT)));
        if (pgeyFront.pgeyLength() < 1e-6) {
            pgeyFront = PgeyVec3.PGEY_UP.pgeySub(pgeyT.pgeyMul(pgeyT.pgeyDot(PgeyVec3.PGEY_UP)));
        }
        return pgeyFront.pgeyNormalize();
    }

    public PgeyVec3 pgeySideAxis(double pgeyF) {
        PgeyVec3 pgeyT = pgeyTangent(pgeyF);
        PgeyVec3 pgeyFront = pgeyFrontAxis(pgeyF);
        PgeyVec3 pgeySide = pgeyT.pgeyCross(pgeyFront).pgeyNormalize();
        return pgeySide.pgeyDot(pgeyOutward) < 0 ? pgeySide.pgeyMul(-1) : pgeySide;
    }

    public PgeyVec3 pgeyCenter(double pgeyF) {
        return pgeySpine(pgeyF).pgeyAddScaled(pgeyFrontAxis(pgeyF), pgeyFrontShift.pgeyAt(pgeyF));
    }

    public double pgeyRadiusSide(double pgeyF) {
        return pgeyRx.pgeyAt(pgeyF);
    }

    public double pgeyRadiusFront(double pgeyF) {
        return pgeyRz.pgeyAt(pgeyF);
    }

    public PgeyVec3 pgeyPoint(double pgeyF, double pgeyAngle, double pgeyExtra) {
        PgeyVec3 pgeyC = pgeyCenter(pgeyF);
        double pgeyS = Math.sin(pgeyAngle);
        double pgeyCo = Math.cos(pgeyAngle);
        return pgeyC.pgeyAddScaled(pgeySideAxis(pgeyF), (pgeyRx.pgeyAt(pgeyF) + pgeyExtra) * pgeyS)
                .pgeyAddScaled(pgeyFrontAxis(pgeyF), (pgeyRz.pgeyAt(pgeyF) + pgeyExtra) * pgeyCo);
    }

    private static PgeyVec3 pgeyCatmullRom(PgeyVec3 pgeyP0, PgeyVec3 pgeyP1, PgeyVec3 pgeyP2, PgeyVec3 pgeyP3, double pgeyT) {
        double pgeyT2 = pgeyT * pgeyT;
        double pgeyT3 = pgeyT2 * pgeyT;
        return new PgeyVec3(
                pgeyCr(pgeyP0.pgeyX(), pgeyP1.pgeyX(), pgeyP2.pgeyX(), pgeyP3.pgeyX(), pgeyT, pgeyT2, pgeyT3),
                pgeyCr(pgeyP0.pgeyY(), pgeyP1.pgeyY(), pgeyP2.pgeyY(), pgeyP3.pgeyY(), pgeyT, pgeyT2, pgeyT3),
                pgeyCr(pgeyP0.pgeyZ(), pgeyP1.pgeyZ(), pgeyP2.pgeyZ(), pgeyP3.pgeyZ(), pgeyT, pgeyT2, pgeyT3));
    }

    private static double pgeyCr(double pgeyA, double pgeyB, double pgeyC, double pgeyD, double pgeyT, double pgeyT2, double pgeyT3) {
        return 0.5 * (2 * pgeyB + (-pgeyA + pgeyC) * pgeyT + (2 * pgeyA - 5 * pgeyB + 4 * pgeyC - pgeyD) * pgeyT2 + (-pgeyA + 3 * pgeyB - 3 * pgeyC + pgeyD) * pgeyT3);
    }
}
