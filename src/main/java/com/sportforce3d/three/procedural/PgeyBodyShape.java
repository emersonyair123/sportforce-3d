package com.sportforce3d.three.procedural;

import com.sportforce3d.three.geom.PgeyCurve;
import com.sportforce3d.three.geom.PgeyTube;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyBodyType;

import java.util.List;

import static com.sportforce3d.three.geom.PgeyCurve.pgeyAngularBell;
import static com.sportforce3d.three.geom.PgeyCurve.pgeyBell;

public final class PgeyBodyShape {
    public enum PgeySide {
        RIGHT(-1), LEFT(1);

        public final int pgeySign;

        PgeySide(int pgeySign) {
            this.pgeySign = pgeySign;
        }
    }

    public static final double PGEY_TORSO_BOTTOM = 0.775;
    public static final double PGEY_TORSO_TOP = 1.545;

    private final PgeyBodyType pgeyType;
    private final double pgeyScale;
    private final double pgeyGirth;
    private final PgeyCurve pgeyW;
    private final PgeyCurve pgeyDf;
    private final PgeyCurve pgeyDb;
    private final PgeyCurve pgeyCz;
    private final PgeyCurve pgeyN;
    private final double pgeyPecs;
    private final double pgeyBust;
    private final double pgeyGlutes;
    private final PgeyTube[] pgeyArms = new PgeyTube[2];
    private final PgeyTube[] pgeyLegs = new PgeyTube[2];
    private final PgeyTube pgeyNeck;
    private final double pgeyHeadScale;

    private PgeyBodyShape(PgeyBodyType pgeyType) {
        this.pgeyType = pgeyType;
        boolean pgeyFemale = pgeyType == PgeyBodyType.ADULTO_MUJER;
        boolean pgeyJunior = pgeyType == PgeyBodyType.JUVENIL;
        this.pgeyScale = pgeyFemale ? 0.955 : pgeyJunior ? 0.83 : 1.0;
        this.pgeyGirth = pgeyJunior ? 0.90 : 1.0;
        this.pgeyHeadScale = pgeyJunior ? 1.10 : pgeyFemale ? 0.97 : 1.0;

        if (pgeyFemale) {
            pgeyW = PgeyCurve.pgeyOf(0.775, .020, 0.80, .100, 0.86, .168, 0.92, .182, 0.97, .176, 1.03, .150, 1.10, .127,
                    1.18, .131, 1.27, .147, 1.34, .157, 1.395, .168, 1.425, .178, 1.455, .158, 1.49, .108,
                    1.515, .074, 1.545, .045);
            pgeyDf = PgeyCurve.pgeyOf(0.775, .010, 0.80, .052, 0.86, .088, 0.92, .095, 0.97, .093, 1.03, .086, 1.10, .080,
                    1.18, .085, 1.27, .095, 1.34, .099, 1.395, .094, 1.425, .086, 1.455, .076, 1.49, .064,
                    1.515, .056, 1.545, .044);
            pgeyDb = PgeyCurve.pgeyOf(0.775, .010, 0.80, .062, 0.86, .112, 0.92, .122, 0.97, .110, 1.03, .088, 1.10, .078,
                    1.18, .080, 1.27, .086, 1.34, .090, 1.395, .090, 1.425, .086, 1.455, .076, 1.49, .064,
                    1.515, .058, 1.545, .046);
            pgeyPecs = 0;
            pgeyBust = 0.033;
            pgeyGlutes = 0.022;
        } else {
            pgeyW = PgeyCurve.pgeyOf(0.775, .020, 0.80, .095, 0.86, .160, 0.92, .171, 0.97, .168, 1.03, .150, 1.10, .139,
                    1.20, .149, 1.29, .163, 1.36, .174, 1.405, .190, 1.435, .196, 1.465, .172, 1.495, .118,
                    1.52, .078, 1.545, .050);
            pgeyDf = PgeyCurve.pgeyOf(0.775, .010, 0.80, .050, 0.86, .086, 0.92, .094, 0.97, .094, 1.03, .090, 1.10, .088,
                    1.20, .097, 1.29, .108, 1.36, .112, 1.405, .104, 1.435, .094, 1.465, .082, 1.495, .068,
                    1.52, .060, 1.545, .049);
            pgeyDb = PgeyCurve.pgeyOf(0.775, .010, 0.80, .060, 0.86, .104, 0.92, .114, 0.97, .106, 1.03, .090, 1.10, .084,
                    1.20, .088, 1.29, .094, 1.36, .098, 1.405, .098, 1.435, .092, 1.465, .082, 1.495, .068,
                    1.52, .062, 1.545, .051);
            pgeyPecs = pgeyJunior ? 0.004 : 0.009;
            pgeyBust = 0;
            pgeyGlutes = pgeyJunior ? 0.012 : 0.016;
        }
        pgeyCz = PgeyCurve.pgeyOf(0.80, .000, 0.86, -.004, 0.92, -.008, 0.97, -.004, 1.03, .002, 1.10, .005, 1.20, .007,
                1.29, .007, 1.36, .004, 1.405, .000, 1.435, -.004, 1.465, -.008, 1.495, -.010, 1.545, -.008);
        pgeyN = PgeyCurve.pgeyOf(0.80, 2.2, 0.86, 2.5, 0.92, 2.6, 1.03, 2.5, 1.10, 2.4, 1.20, 2.5, 1.29, 2.7, 1.36, 2.9,
                1.435, 2.7, 1.465, 2.4, 1.495, 2.2, 1.52, 2.1, 1.545, 2.0);

        double pgeyArmK = pgeyFemale ? 0.86 : pgeyJunior ? 0.92 : 1.0;
        double pgeyShoulderX = pgeyFemale ? 0.140 : 0.152;
        for (PgeySide pgeySide : PgeySide.values()) {
            int pgeyS = pgeySide.pgeySign;
            pgeyArms[pgeySide.ordinal()] = new PgeyTube(List.of(
                    new PgeyVec3(pgeyS * pgeyShoulderX, 1.418, -0.006),
                    new PgeyVec3(pgeyS * (pgeyShoulderX + 0.036), 1.362, -0.008),
                    new PgeyVec3(pgeyS * (pgeyShoulderX + 0.060), 1.240, -0.010),
                    new PgeyVec3(pgeyS * (pgeyShoulderX + 0.078), 1.115, -0.012),
                    new PgeyVec3(pgeyS * (pgeyShoulderX + 0.095), 0.990, 0.004),
                    new PgeyVec3(pgeyS * (pgeyShoulderX + 0.112), 0.872, 0.026),
                    new PgeyVec3(pgeyS * (pgeyShoulderX + 0.120), 0.800, 0.034),
                    new PgeyVec3(pgeyS * (pgeyShoulderX + 0.123), 0.715, 0.038)),
                    PgeyCurve.pgeyOf(0, .030, .05, .047, .10, .053, .17, .049, .27, .044, .37, .039, .42, .035,
                            .47, .037, .55, .040, .65, .033, .745, .021, .775, .018, .82, .016, .89, .015,
                            .94, .014, .975, .012, .99, .008, 1.0, .002).pgeyScaled(pgeyArmK),
                    PgeyCurve.pgeyOf(0, .030, .05, .049, .10, .054, .17, .050, .27, .047, .37, .041, .42, .038,
                            .47, .041, .55, .043, .65, .034, .745, .027, .775, .031, .82, .039, .89, .041,
                            .94, .038, .975, .031, .99, .019, 1.0, .004).pgeyScaled(pgeyArmK),
                    PgeyCurve.pgeyOf(0, 0, .27, .004, .55, .002, 1, 0),
                    new PgeyVec3(pgeyS, 0, 0));

            double pgeyThighK = pgeyFemale ? 1.05 : 1.0;
            double pgeyCalfK = pgeyFemale ? 0.93 : 1.0;
            pgeyLegs[pgeySide.ordinal()] = new PgeyTube(List.of(
                    new PgeyVec3(pgeyS * 0.088, 0.945, 0.000),
                    new PgeyVec3(pgeyS * 0.093, 0.720, 0.010),
                    new PgeyVec3(pgeyS * 0.097, 0.510, 0.010),
                    new PgeyVec3(pgeyS * 0.101, 0.300, -0.004),
                    new PgeyVec3(pgeyS * 0.104, 0.094, -0.012)),
                    PgeyCurve.pgeyOf(0, .060 * pgeyThighK, .06, .086 * pgeyThighK, .14, .082 * pgeyThighK, .25, .074 * pgeyThighK,
                            .36, .062, .44, .052, .485, .048, .53, .050, .60, .053 * pgeyCalfK, .68, .047 * pgeyCalfK,
                            .78, .036, .88, .031, .96, .033, 1.0, .030),
                    PgeyCurve.pgeyOf(0, .062 * pgeyThighK, .06, .090 * pgeyThighK, .14, .087 * pgeyThighK, .25, .080 * pgeyThighK,
                            .36, .067, .44, .056, .485, .052, .53, .055, .60, .060 * pgeyCalfK, .68, .054 * pgeyCalfK,
                            .78, .040, .88, .034, .96, .040, 1.0, .040),
                    PgeyCurve.pgeyOf(0, 0, .25, .008, .485, .006, .60, -.014, .78, -.004, 1, 0),
                    new PgeyVec3(pgeyS, 0, 0));
        }
        pgeyNeck = new PgeyTube(List.of(new PgeyVec3(0, 1.470, -0.016), new PgeyVec3(0, 1.560, -0.010), new PgeyVec3(0, 1.665, 0.004)),
                PgeyCurve.pgeyOf(0, .056, .6, .054, 1, .051).pgeyScaled(pgeyFemale ? 0.9 : 1.0),
                PgeyCurve.pgeyOf(0, .060, .6, .058, 1, .054).pgeyScaled(pgeyFemale ? 0.9 : 1.0),
                PgeyCurve.pgeyOf(0, 0, 1, 0), new PgeyVec3(1, 0, 0));
    }

    public static PgeyBodyShape pgeyOf(PgeyBodyType pgeyType) {
        return new PgeyBodyShape(pgeyType);
    }

    public PgeyBodyType pgeyType() {
        return pgeyType;
    }

    public double pgeyHeight() {
        return 1.812 * pgeyScale;
    }

    public PgeyVec3 pgeyOut(PgeyVec3 pgeyP) {
        return new PgeyVec3(pgeyP.pgeyX() * pgeyScale * pgeyGirth, pgeyP.pgeyY() * pgeyScale, pgeyP.pgeyZ() * pgeyScale * pgeyGirth);
    }

    public PgeyVec3 pgeyOutHead(PgeyVec3 pgeyP) {
        double pgeyPivot = 1.62;
        PgeyVec3 pgeyLocal = new PgeyVec3(pgeyP.pgeyX() * pgeyHeadScale, pgeyPivot + (pgeyP.pgeyY() - pgeyPivot) * pgeyHeadScale, pgeyP.pgeyZ() * pgeyHeadScale);
        return new PgeyVec3(pgeyLocal.pgeyX() * pgeyScale, pgeyLocal.pgeyY() * pgeyScale, pgeyLocal.pgeyZ() * pgeyScale);
    }

    public double pgeyTorsoCenterZ(double pgeyY) {
        return pgeyCz.pgeyAt(pgeyY);
    }

    public double pgeyTorsoRadius(double pgeyPhi, double pgeyY) {
        double pgeyA = pgeyW.pgeyAt(pgeyY);
        double pgeyFront = pgeyDf.pgeyAt(pgeyY);
        double pgeyBack = pgeyDb.pgeyAt(pgeyY);
        double pgeyE = pgeyN.pgeyAt(pgeyY);
        double pgeyS = Math.abs(Math.sin(pgeyPhi));
        double pgeyC = Math.cos(pgeyPhi);
        double pgeyD = pgeyC >= 0 ? pgeyFront : pgeyBack;
        double pgeyR = 1.0 / Math.pow(Math.pow(pgeyS / pgeyA, pgeyE) + Math.pow(Math.abs(pgeyC) / pgeyD, pgeyE), 1.0 / pgeyE);
        return pgeyR + pgeyLobes(pgeyPhi, pgeyY);
    }

    private double pgeyLobes(double pgeyPhi, double pgeyY) {
        double pgeyR = 0;
        if (pgeyPecs > 0) {
            pgeyR += pgeyPecs * (pgeyAngularBell(pgeyPhi, 0.42, 0.32) + pgeyAngularBell(pgeyPhi, -0.42, 0.32)) * pgeyBell(pgeyY, 1.325, 0.05);
        }
        if (pgeyBust > 0) {
            pgeyR += pgeyBust * (pgeyAngularBell(pgeyPhi, 0.43, 0.30) + pgeyAngularBell(pgeyPhi, -0.43, 0.30)) * pgeyBell(pgeyY, 1.30, 0.042);
        }
        pgeyR += 0.004 * pgeyAngularBell(pgeyPhi, 0, 0.6) * pgeyBell(pgeyY, 1.07, 0.06);
        pgeyR += 0.007 * (pgeyAngularBell(pgeyPhi, Math.PI + 0.62, 0.33) + pgeyAngularBell(pgeyPhi, Math.PI - 0.62, 0.33)) * pgeyBell(pgeyY, 1.37, 0.06);
        pgeyR -= 0.005 * pgeyAngularBell(pgeyPhi, Math.PI, 0.13) * pgeyBell(pgeyY, 1.22, 0.15);
        pgeyR += pgeyGlutes * (pgeyAngularBell(pgeyPhi, Math.PI + 0.48, 0.40) + pgeyAngularBell(pgeyPhi, Math.PI - 0.48, 0.40)) * pgeyBell(pgeyY, 0.925, 0.05);
        return pgeyR;
    }

    public PgeyVec3 pgeyTorsoPoint(double pgeyPhi, double pgeyY, double pgeyExtraRadius) {
        double pgeyR = pgeyTorsoRadius(pgeyPhi, pgeyY) + pgeyExtraRadius;
        return new PgeyVec3(pgeyR * Math.sin(pgeyPhi), pgeyY, pgeyCz.pgeyAt(pgeyY) + pgeyR * Math.cos(pgeyPhi));
    }

    public PgeyVec3 pgeyTorsoAxis(double pgeyY) {
        return new PgeyVec3(0, pgeyY, pgeyCz.pgeyAt(pgeyY));
    }

    public PgeyTube pgeyArm(PgeySide pgeySide) {
        return pgeyArms[pgeySide.ordinal()];
    }

    public PgeyTube pgeyLeg(PgeySide pgeySide) {
        return pgeyLegs[pgeySide.ordinal()];
    }

    public PgeyTube pgeyNeck() {
        return pgeyNeck;
    }

    public double pgeyLegFractionAt(PgeySide pgeySide, double pgeyY) {
        PgeyTube pgeyLeg = pgeyLeg(pgeySide);
        double pgeyLo = 0;
        double pgeyHi = 1;
        for (int pgeyI = 0; pgeyI < 40; pgeyI++) {
            double pgeyMid = (pgeyLo + pgeyHi) / 2;
            if (pgeyLeg.pgeySpine(pgeyMid).pgeyY() > pgeyY) {
                pgeyLo = pgeyMid;
            } else {
                pgeyHi = pgeyMid;
            }
        }
        return (pgeyLo + pgeyHi) / 2;
    }

    public PgeyVec3 pgeyAnkle(PgeySide pgeySide) {
        PgeyVec3 pgeyC = pgeyLeg(pgeySide).pgeySpine(1.0);
        return new PgeyVec3(pgeyC.pgeyX(), 0.085, pgeyC.pgeyZ());
    }

    public static final PgeyCurve PGEY_HEAD_RX = PgeyCurve.pgeyOf(1.600, .010, 1.615, .034, 1.640, .055, 1.670, .067, 1.705, .075,
            1.740, .077, 1.770, .071, 1.792, .057, 1.806, .035, 1.814, .004);
    public static final PgeyCurve PGEY_HEAD_RZ_FRONT = PgeyCurve.pgeyOf(1.600, .012, 1.615, .040, 1.640, .072, 1.670, .090,
            1.705, .097, 1.740, .098, 1.770, .091, 1.792, .073, 1.806, .044, 1.814, .004);
    public static final PgeyCurve PGEY_HEAD_RZ_BACK = PgeyCurve.pgeyOf(1.600, .008, 1.615, .020, 1.640, .046, 1.670, .076,
            1.705, .094, 1.740, .099, 1.770, .093, 1.792, .075, 1.806, .045, 1.814, .004);
    public static final PgeyCurve PGEY_HEAD_CZ = PgeyCurve.pgeyOf(1.600, .036, 1.640, .026, 1.670, .018, 1.705, .012,
            1.740, .008, 1.770, .004, 1.814, .000);
}
