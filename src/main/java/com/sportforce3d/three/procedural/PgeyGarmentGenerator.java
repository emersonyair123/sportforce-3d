package com.sportforce3d.three.procedural;

import com.sportforce3d.three.geom.PgeyGridSurface;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyTube;
import com.sportforce3d.three.geom.PgeyUvRect;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyUvLayout;
import com.sportforce3d.three.procedural.PgeyBodyShape.PgeySide;

import java.util.ArrayList;
import java.util.List;

import static com.sportforce3d.three.geom.PgeyCurve.pgeyAngleDiff;
import static com.sportforce3d.three.geom.PgeyCurve.pgeyAngularBell;
import static com.sportforce3d.three.geom.PgeyCurve.pgeyBell;
import static com.sportforce3d.three.geom.PgeyCurve.pgeySmoothstep;

final class PgeyGarmentGenerator {
    static final double PGEY_HEM_FRONT = 0.990;
    static final double PGEY_HEM_BACK = 0.972;
    static final double PGEY_NECK_SIDE = 1.533;
    static final double PGEY_NECK_BACK = 1.538;
    static final double PGEY_V_DEPTH = 0.104;
    static final double PGEY_V_HALF = 0.62;
    static final double PGEY_JERSEY_V_TOP = 1.545;
    static final double PGEY_JERSEY_V_BOTTOM = 0.965;
    static final double PGEY_ARMPIT = 1.352;
    static final double PGEY_SLEEVE_END = 0.235;
    static final double PGEY_CUFF_WIDTH = 0.026;
    static final double PGEY_DRAPE_TOP = 1.29;
    static final double PGEY_DRAPE_BOTTOM = 1.02;

    static final double PGEY_SHORTS_TOP = 0.962;
    static final double PGEY_SHORTS_HEM = 0.615;
    static final double PGEY_CROTCH = 0.825;
    static final double PGEY_HIP_FREEZE = 0.905;
    static final double PGEY_WAIST_BOTTOM = 0.948;
    static final double PGEY_WAIST_TOP = 0.992;

    static final double PGEY_SOCK_TOP = 0.458;
    static final double PGEY_SOCK_BAND_BOTTOM = 0.405;

    @FunctionalInterface
    interface PgeySurface {
        PgeyVec3 pgeyAt(double pgeyU, double pgeyV);
    }

    private PgeyGarmentGenerator() {
    }

    static PgeyVec3 pgeyNormal(PgeySurface pgeyS, double pgeyU, double pgeyV, double pgeyDu, double pgeyDv, PgeyVec3 pgeyHint) {
        PgeyVec3 pgeyPu = pgeyS.pgeyAt(pgeyU + pgeyDu, pgeyV).pgeySub(pgeyS.pgeyAt(pgeyU - pgeyDu, pgeyV));
        PgeyVec3 pgeyPv = pgeyS.pgeyAt(pgeyU, pgeyV + pgeyDv).pgeySub(pgeyS.pgeyAt(pgeyU, pgeyV - pgeyDv));
        PgeyVec3 pgeyN = pgeyPu.pgeyCross(pgeyPv).pgeyNormalize();
        if (pgeyN.pgeyLength() == 0) {
            return pgeyHint.pgeyNormalize();
        }
        return pgeyN.pgeyDot(pgeyHint) < 0 ? pgeyN.pgeyMul(-1) : pgeyN;
    }

    static double pgeyHemY(double pgeyPhi) {
        return PGEY_HEM_BACK + (PGEY_HEM_FRONT - PGEY_HEM_BACK) * (1 + Math.cos(pgeyPhi)) / 2;
    }

    static double pgeyNecklineY(double pgeyPhi) {
        double pgeyBase = PGEY_NECK_SIDE + (PGEY_NECK_BACK - PGEY_NECK_SIDE) * (1 - Math.cos(pgeyPhi)) / 2;
        double pgeyK = Math.max(0, 1 - Math.abs(pgeyAngleDiff(pgeyPhi, 0)) / PGEY_V_HALF);
        return pgeyBase - PGEY_V_DEPTH * Math.pow(pgeyK, 1.18);
    }

    static PgeyVec3 pgeyJerseyBase(PgeyBodyShape pgeyB, double pgeyPhi, double pgeyY) {
        double pgeyRb = pgeyB.pgeyTorsoRadius(pgeyPhi, pgeyY);
        double pgeyRChest = pgeyB.pgeyTorsoRadius(pgeyPhi, PGEY_DRAPE_TOP) * 0.985;
        double pgeyK = 0.62 * pgeySmoothstep(PGEY_DRAPE_TOP, PGEY_DRAPE_BOTTOM, pgeyY);
        double pgeyR = Math.max(pgeyRb, pgeyRb + (pgeyRChest - pgeyRb) * pgeyK);
        return new PgeyVec3(pgeyR * Math.sin(pgeyPhi), pgeyY, pgeyB.pgeyTorsoCenterZ(pgeyY) + pgeyR * Math.cos(pgeyPhi));
    }

    static double pgeyJerseyEase(double pgeyPhi, double pgeyY) {
        double pgeyEase = 0.0065 + 0.0115 * pgeySmoothstep(1.10, 0.99, pgeyY) + 0.003 * pgeySmoothstep(1.47, 1.53, pgeyY);
        double pgeyHemFolds = (0.0030 * pgeySmoothstep(1.17, 0.99, pgeyY) + 0.0006)
                * (0.6 * Math.sin(5 * pgeyPhi + 0.7) + 0.4 * Math.sin(11 * pgeyPhi + 2.1));
        double pgeySides = pgeyAngularBell(pgeyPhi, Math.PI / 2, 0.25) + pgeyAngularBell(pgeyPhi, -Math.PI / 2, 0.25);
        double pgeyWaistWrinkles = 0.0012 * Math.sin(pgeyY * 110) * pgeySides * pgeyBell(pgeyY, 1.12, 0.05);
        double pgeyBackDrape = 0.0015 * Math.sin(7 * pgeyPhi) * pgeyAngularBell(pgeyPhi, Math.PI, 0.9) * pgeySmoothstep(1.20, 1.0, pgeyY);
        return pgeyEase + pgeyHemFolds + pgeyWaistWrinkles + pgeyBackDrape;
    }

    static PgeyVec3 pgeyJerseyPoint(PgeyBodyShape pgeyB, double pgeyPhi, double pgeyY, double pgeyExtra) {
        PgeySurface pgeyBase = (pgeyU, pgeyV) -> pgeyJerseyBase(pgeyB, pgeyU, pgeyV);
        PgeyVec3 pgeyP = pgeyBase.pgeyAt(pgeyPhi, pgeyY);
        PgeyVec3 pgeyN = pgeyNormal(pgeyBase, pgeyPhi, pgeyY, 0.002, 0.002, pgeyP.pgeySub(pgeyB.pgeyTorsoAxis(pgeyY)));
        return pgeyP.pgeyAddScaled(pgeyN, pgeyJerseyEase(pgeyPhi, pgeyY) + pgeyExtra);
    }

    static PgeyMeshData pgeyJerseyBody(PgeyBodyShape pgeyB) {
        int pgeyRows = 130;
        int pgeyCols = 160;
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
        double[] pgeyYs = new double[pgeyRows * pgeyCols];
        for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
            double pgeyPhi = 2 * Math.PI * pgeyC / pgeyCols;
            double pgeyHem = pgeyHemY(pgeyPhi);
            double pgeyTop = pgeyNecklineY(pgeyPhi);
            for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
                double pgeyT = (double) pgeyR / (pgeyRows - 1);
                double pgeyY = pgeyHem + (pgeyTop - pgeyHem) * (pgeyT * pgeyT * (3 - 2 * pgeyT) * 0.35 + pgeyT * 0.65);
                pgeyYs[pgeyR * pgeyCols + pgeyC] = pgeyY;
                PgeyVec3 pgeyP = pgeyJerseyPoint(pgeyB, pgeyPhi, pgeyY, 0);
                pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyP.pgeySub(pgeyB.pgeyTorsoAxis(pgeyY)));
            }
        }
        int pgeyBack0 = pgeyCols / 4;
        int pgeyBack1 = 3 * pgeyCols / 4;
        return pgeyG.pgeyBuild(2,
                (pgeyR, pgeyC) -> pgeyC >= pgeyBack0 && pgeyC < pgeyBack1 ? 1 : 0,
                (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
                    double pgeyPhi = 2 * Math.PI * pgeyC / pgeyCols;
                    double pgeyY = pgeyYs[pgeyR * pgeyCols + (pgeyC % pgeyCols)];
                    double pgeyV = (PGEY_JERSEY_V_TOP - pgeyY) / (PGEY_JERSEY_V_TOP - PGEY_JERSEY_V_BOTTOM);
                    if (pgeyIsland == 0) {
                        double pgeyA = pgeyAngleDiff(pgeyPhi, 0);
                        pgeyOut[0] = PgeyUvLayout.PGEY_JERSEY_FRONT.pgeyU(0.5 + pgeyA / Math.PI);
                        pgeyOut[1] = PgeyUvLayout.PGEY_JERSEY_FRONT.pgeyV(pgeyV);
                    } else {
                        pgeyOut[0] = PgeyUvLayout.PGEY_JERSEY_BACK.pgeyU((pgeyPhi - Math.PI / 2) / Math.PI);
                        pgeyOut[1] = PgeyUvLayout.PGEY_JERSEY_BACK.pgeyV(pgeyV);
                    }
                });
    }

    static PgeyMeshData pgeyCollar(PgeyBodyShape pgeyB) {
        int pgeyCols = 240;
        double[][] pgeyRowsDef = {
                {-0.0155, 0.0022},
                {-0.0137, 0.0036},
                {-0.003, 0.0040},
                {0.0025, 0.0024},
                {0.0030, -0.0010},
                {0.0010, -0.0045},
        };
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRowsDef.length, pgeyCols, true);
        for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
            double pgeyPhi = 2 * Math.PI * pgeyC / pgeyCols;
            double pgeyTop = pgeyNecklineY(pgeyPhi);
            for (int pgeyR = 0; pgeyR < pgeyRowsDef.length; pgeyR++) {
                double pgeyDy = pgeyRowsDef[pgeyR][0];
                double pgeyY = pgeyDy < 0 ? pgeyYBelowByArc(pgeyB, pgeyPhi, pgeyTop, -pgeyDy) : pgeyTop + pgeyDy;
                PgeyVec3 pgeyP = pgeyJerseyPoint(pgeyB, pgeyPhi, pgeyY, pgeyRowsDef[pgeyR][1]);
                PgeyVec3 pgeyRadial = pgeyP.pgeySub(pgeyB.pgeyTorsoAxis(pgeyY)).pgeyNormalize();
                PgeyVec3 pgeyHint = pgeyR <= 2 ? pgeyRadial : pgeyR == 3 ? pgeyRadial.pgeyAdd(new PgeyVec3(0, 1.5, 0)) : pgeyRadial.pgeyMul(-1).pgeyAdd(new PgeyVec3(0, 0.3, 0));
                pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyHint);
            }
        }
        PgeyUvRect pgeyRect = PgeyUvLayout.PGEY_JERSEY_COLLAR;
        return pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
            pgeyOut[0] = pgeyRect.pgeyU((double) pgeyC / pgeyCols);
            pgeyOut[1] = pgeyRect.pgeyV((double) pgeyR / (pgeyRowsDef.length - 1));
        });
    }

    static double pgeyYBelowByArc(PgeyBodyShape pgeyB, double pgeyPhi, double pgeyTop, double pgeyDist) {
        PgeyVec3 pgeyStart = pgeyJerseyPoint(pgeyB, pgeyPhi, pgeyTop, 0);
        double pgeyLo = pgeyTop - pgeyDist;
        double pgeyHi = pgeyTop;
        for (int pgeyI = 0; pgeyI < 30; pgeyI++) {
            double pgeyMid = (pgeyLo + pgeyHi) / 2;
            if (pgeyJerseyPoint(pgeyB, pgeyPhi, pgeyMid, 0).pgeySub(pgeyStart).pgeyLength() > pgeyDist) {
                pgeyLo = pgeyMid;
            } else {
                pgeyHi = pgeyMid;
            }
        }
        return (pgeyLo + pgeyHi) / 2;
    }

    static PgeyMeshData pgeySides(PgeyBodyShape pgeyB) {
        List<PgeyMeshData> pgeyParts = new ArrayList<>();
        for (PgeySide pgeySide : PgeySide.values()) {
            int pgeyRows = 80;
            int pgeyCols = 12;
            double pgeyCenter = pgeySide.pgeySign * Math.PI / 2;
            PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, false);
            for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
                double pgeyT = (double) pgeyR / (pgeyRows - 1);
                for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                    double pgeyHalf = 0.105 * (1 - 0.55 * pgeySmoothstep(0.82, 1.0, pgeyT));
                    double pgeyPhi = pgeyCenter + pgeySide.pgeySign * pgeyHalf * (2.0 * pgeyC / (pgeyCols - 1) - 1);
                    double pgeyY = pgeyHemY(pgeyPhi) + 0.002 + (PGEY_ARMPIT - pgeyHemY(pgeyPhi)) * pgeyT;
                    PgeyVec3 pgeyP = pgeyJerseyPoint(pgeyB, pgeyPhi, pgeyY, 0.0018);
                    pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyP.pgeySub(pgeyB.pgeyTorsoAxis(pgeyY)));
                }
            }
            PgeyUvRect pgeyRect = pgeySide == PgeySide.LEFT ? PgeyUvLayout.PGEY_JERSEY_SIDE_LEFT : PgeyUvLayout.PGEY_JERSEY_SIDE_RIGHT;
            pgeyParts.add(pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
                pgeyOut[0] = pgeyRect.pgeyU((double) pgeyC / (pgeyCols - 1));
                pgeyOut[1] = pgeyRect.pgeyV(1 - (double) pgeyR / (pgeyRows - 1));
            }));
        }
        return PgeyMeshData.pgeyMerge(pgeyParts);
    }

    static double pgeySleeveEase(double pgeyF, double pgeyAngle, int pgeySign) {
        double pgeyFlare = 0.0085 + 0.0100 * pgeySmoothstep(0.06, PGEY_SLEEVE_END, pgeyF);
        double pgeyFolds = 0.0018 * Math.sin(4 * pgeyAngle + pgeySign * 0.9) * pgeySmoothstep(0.10, PGEY_SLEEVE_END, pgeyF);
        double pgeyDrape = 0.0025 * Math.max(0, -Math.cos(pgeyAngle)) * pgeySmoothstep(0.12, PGEY_SLEEVE_END, pgeyF);
        return pgeyFlare + pgeyFolds + pgeyDrape;
    }

    static PgeyMeshData pgeySleeves(PgeyBodyShape pgeyB) {
        List<PgeyMeshData> pgeyParts = new ArrayList<>();
        for (PgeySide pgeySide : PgeySide.values()) {
            PgeyTube pgeyArm = pgeyB.pgeyArm(pgeySide);
            int pgeyRows = 70;
            int pgeyCols = 64;
            PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
            for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
                double pgeyF = PGEY_SLEEVE_END * pgeyR / (pgeyRows - 1);
                PgeyVec3 pgeyCenter = pgeyArm.pgeyCenter(pgeyF);
                for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                    double pgeyAngle = 2 * Math.PI * pgeyC / pgeyCols;
                    PgeyVec3 pgeyP = pgeyArm.pgeyPoint(pgeyF, pgeyAngle, pgeySleeveEase(pgeyF, pgeyAngle, pgeySide.pgeySign));
                    pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyP.pgeySub(pgeyCenter));
                }
            }
            PgeyUvRect pgeyRect = pgeySide == PgeySide.LEFT ? PgeyUvLayout.PGEY_JERSEY_SLEEVE_LEFT : PgeyUvLayout.PGEY_JERSEY_SLEEVE_RIGHT;
            pgeyParts.add(pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
                pgeyOut[0] = pgeyRect.pgeyU((double) pgeyC / pgeyCols);
                pgeyOut[1] = pgeyRect.pgeyV((double) pgeyR / (pgeyRows - 1));
            }));
        }
        return PgeyMeshData.pgeyMerge(pgeyParts);
    }

    static PgeyMeshData pgeyCuffs(PgeyBodyShape pgeyB) {
        List<PgeyMeshData> pgeyParts = new ArrayList<>();
        double pgeyF1 = PGEY_SLEEVE_END + 0.002;
        double pgeyF0 = pgeyF1 - PGEY_CUFF_WIDTH / pgeyB.pgeyArm(PgeySide.LEFT).pgeyLength();
        double[][] pgeyRowsDef = {
                {0.00, 0.0006}, {0.06, 0.0034}, {0.50, 0.0040}, {0.94, 0.0034}, {1.00, 0.0006}, {0.985, -0.0030},
        };
        for (PgeySide pgeySide : PgeySide.values()) {
            PgeyTube pgeyArm = pgeyB.pgeyArm(pgeySide);
            int pgeyCols = 64;
            PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRowsDef.length, pgeyCols, true);
            for (int pgeyR = 0; pgeyR < pgeyRowsDef.length; pgeyR++) {
                double pgeyF = pgeyF0 + (pgeyF1 - pgeyF0) * pgeyRowsDef[pgeyR][0];
                PgeyVec3 pgeyCenter = pgeyArm.pgeyCenter(pgeyF);
                for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                    double pgeyAngle = 2 * Math.PI * pgeyC / pgeyCols;
                    double pgeyE = pgeySleeveEase(Math.min(pgeyF, PGEY_SLEEVE_END), pgeyAngle, pgeySide.pgeySign) + pgeyRowsDef[pgeyR][1];
                    PgeyVec3 pgeyP = pgeyArm.pgeyPoint(pgeyF, pgeyAngle, pgeyE);
                    PgeyVec3 pgeyRadial = pgeyP.pgeySub(pgeyCenter).pgeyNormalize();
                    PgeyVec3 pgeyHint = pgeyR == pgeyRowsDef.length - 1 ? pgeyRadial.pgeyMul(-1).pgeyAdd(pgeyArm.pgeyTangent(pgeyF).pgeyMul(0.3))
                            : pgeyR == pgeyRowsDef.length - 2 ? pgeyRadial.pgeyAdd(pgeyArm.pgeyTangent(pgeyF).pgeyMul(1.5)) : pgeyRadial;
                    pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyHint);
                }
            }
            PgeyUvRect pgeyRect = pgeySide == PgeySide.LEFT ? PgeyUvLayout.PGEY_JERSEY_CUFF_LEFT : PgeyUvLayout.PGEY_JERSEY_CUFF_RIGHT;
            pgeyParts.add(pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
                pgeyOut[0] = pgeyRect.pgeyU((double) pgeyC / pgeyCols);
                pgeyOut[1] = pgeyRect.pgeyV((double) pgeyR / (pgeyRowsDef.length - 1));
            }));
        }
        return PgeyMeshData.pgeyMerge(pgeyParts);
    }

    static double pgeyShortsEase(double pgeyY) {
        return 0.013 + 0.009 * pgeySmoothstep(0.96, PGEY_CROTCH, pgeyY) + 0.011 * pgeySmoothstep(PGEY_CROTCH, PGEY_SHORTS_HEM, pgeyY);
    }

    static PgeyVec3 pgeyShortsPoint(PgeyBodyShape pgeyB, PgeySide pgeySide, double pgeyQ, double pgeyY, double pgeyExtra) {
        double pgeyEase = pgeyShortsEase(pgeyY) + pgeyExtra;
        double pgeyFolds = 0.0028 * Math.sin(2 * Math.PI * pgeyQ * 7 + pgeySide.pgeySign * 1.3) * pgeySmoothstep(PGEY_CROTCH, PGEY_SHORTS_HEM, pgeyY);
        double pgeyYd = Math.max(pgeyY, PGEY_HIP_FREEZE);
        PgeyVec3 pgeyD;
        if (pgeyQ <= 0.5) {
            pgeyD = pgeyB.pgeyTorsoPoint(pgeySide.pgeySign * 2 * Math.PI * pgeyQ, pgeyYd, pgeyEase);
        } else {
            PgeyVec3 pgeyBack = pgeyB.pgeyTorsoPoint(pgeySide.pgeySign * Math.PI, pgeyYd, pgeyEase);
            PgeyVec3 pgeyFront = pgeyB.pgeyTorsoPoint(0, pgeyYd, pgeyEase);
            pgeyD = pgeyBack.pgeyLerp(pgeyFront, (pgeyQ - 0.5) / 0.5);
        }
        pgeyD = new PgeyVec3(pgeyD.pgeyX(), pgeyY, pgeyD.pgeyZ());
        double pgeyBlend = pgeySmoothstep(PGEY_CROTCH + 0.03, PGEY_CROTCH - 0.07, pgeyY);
        PgeyTube pgeyLeg = pgeyB.pgeyLeg(pgeySide);
        double pgeyF = pgeyB.pgeyLegFractionAt(pgeySide, pgeyY);
        PgeyVec3 pgeyP = pgeyD;
        if (pgeyBlend > 0) {
            PgeyVec3 pgeyO = pgeyLeg.pgeyPoint(pgeyF, 2 * Math.PI * pgeyQ, pgeyEase + pgeyFolds);
            pgeyP = pgeyD.pgeyLerp(new PgeyVec3(pgeyO.pgeyX(), pgeyY, pgeyO.pgeyZ()), pgeyBlend);
        }
        return pgeyOutsideLeg(pgeyLeg, pgeyF, pgeyP, pgeyEase);
    }

    static PgeyVec3 pgeyOutsideLeg(PgeyTube pgeyLeg, double pgeyF, PgeyVec3 pgeyP, double pgeyEase) {
        PgeyVec3 pgeyC = pgeyLeg.pgeyCenter(pgeyF);
        PgeyVec3 pgeyD = new PgeyVec3(pgeyP.pgeyX() - pgeyC.pgeyX(), 0, pgeyP.pgeyZ() - pgeyC.pgeyZ());
        double pgeyDist = pgeyD.pgeyLength();
        if (pgeyDist < 1e-9) {
            return pgeyP;
        }
        double pgeyAngle = Math.atan2(pgeyD.pgeyDot(pgeyLeg.pgeySideAxis(pgeyF)), pgeyD.pgeyDot(pgeyLeg.pgeyFrontAxis(pgeyF)));
        PgeyVec3 pgeySurface = pgeyLeg.pgeyPoint(pgeyF, pgeyAngle, pgeyEase);
        double pgeyNeed = new PgeyVec3(pgeySurface.pgeyX() - pgeyC.pgeyX(), 0, pgeySurface.pgeyZ() - pgeyC.pgeyZ()).pgeyLength();
        if (pgeyDist >= pgeyNeed) {
            return pgeyP;
        }
        PgeyVec3 pgeyPushed = pgeyC.pgeyAddScaled(pgeyD.pgeyNormalize(), pgeyNeed);
        return new PgeyVec3(pgeyPushed.pgeyX(), pgeyP.pgeyY(), pgeyPushed.pgeyZ());
    }

    static PgeyMeshData pgeyShorts(PgeyBodyShape pgeyB) {
        List<PgeyMeshData> pgeyParts = new ArrayList<>();
        for (PgeySide pgeySide : PgeySide.values()) {
            int pgeyRows = 100;
            int pgeyCols = 96;
            PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
            for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
                double pgeyT = (double) pgeyR / (pgeyRows - 1);
                double pgeyY = PGEY_SHORTS_TOP + (PGEY_SHORTS_HEM - PGEY_SHORTS_TOP) * pgeyT;
                for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                    double pgeyQ = (double) pgeyC / pgeyCols;
                    PgeyVec3 pgeyP = pgeyShortsPoint(pgeyB, pgeySide, pgeyQ, pgeyY, 0);
                    PgeyVec3 pgeyAxis = pgeyY > PGEY_CROTCH ? pgeyB.pgeyTorsoAxis(pgeyY).pgeyAdd(new PgeyVec3(pgeySide.pgeySign * 0.05, 0, 0))
                            : pgeyB.pgeyLeg(pgeySide).pgeyCenter(pgeyB.pgeyLegFractionAt(pgeySide, pgeyY));
                    pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyP.pgeySub(new PgeyVec3(pgeyAxis.pgeyX(), pgeyY, pgeyAxis.pgeyZ())));
                }
            }
            PgeyUvRect pgeyFront = pgeySide == PgeySide.LEFT ? PgeyUvLayout.PGEY_SHORTS_LEFT_FRONT : PgeyUvLayout.PGEY_SHORTS_RIGHT_FRONT;
            PgeyUvRect pgeyBack = pgeySide == PgeySide.LEFT ? PgeyUvLayout.PGEY_SHORTS_LEFT_BACK : PgeyUvLayout.PGEY_SHORTS_RIGHT_BACK;
            int pgeyQ25 = pgeyCols / 4;
            int pgeyQ75 = 3 * pgeyCols / 4;
            pgeyParts.add(pgeyG.pgeyBuild(2,
                    (pgeyR, pgeyC) -> pgeyC >= pgeyQ25 && pgeyC < pgeyQ75 ? 1 : 0,
                    (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
                        double pgeyPsi = 2 * Math.PI * pgeyC / pgeyCols;
                        double pgeyV = (double) pgeyR / (pgeyRows - 1);
                        if (pgeyIsland == 0) {
                            double pgeyA = pgeyAngleDiff(pgeyPsi, 0);
                            pgeyOut[0] = pgeyFront.pgeyU(0.5 + pgeySide.pgeySign * pgeyA / Math.PI);
                            pgeyOut[1] = pgeyFront.pgeyV(pgeyV);
                        } else {
                            pgeyOut[0] = pgeyBack.pgeyU(0.5 + pgeySide.pgeySign * (pgeyPsi - Math.PI) / Math.PI);
                            pgeyOut[1] = pgeyBack.pgeyV(pgeyV);
                        }
                    }));
        }
        return PgeyMeshData.pgeyMerge(pgeyParts);
    }

    static PgeyMeshData pgeyWaistband(PgeyBodyShape pgeyB) {
        double[][] pgeyRowsDef = {
                {PGEY_WAIST_BOTTOM, 0.0145}, {PGEY_WAIST_BOTTOM + 0.002, 0.0168}, {PGEY_WAIST_TOP - 0.003, 0.0168},
                {PGEY_WAIST_TOP, 0.0150}, {PGEY_WAIST_TOP - 0.001, 0.0110},
        };
        int pgeyCols = 160;
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRowsDef.length, pgeyCols, true);
        for (int pgeyR = 0; pgeyR < pgeyRowsDef.length; pgeyR++) {
            double pgeyY = pgeyRowsDef[pgeyR][0];
            for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                double pgeyPhi = 2 * Math.PI * pgeyC / pgeyCols;
                PgeyVec3 pgeyP = pgeyB.pgeyTorsoPoint(pgeyPhi, pgeyY, pgeyRowsDef[pgeyR][1]);
                PgeyVec3 pgeyRadial = pgeyP.pgeySub(pgeyB.pgeyTorsoAxis(pgeyY)).pgeyNormalize();
                PgeyVec3 pgeyHint = pgeyR == pgeyRowsDef.length - 1 ? pgeyRadial.pgeyMul(-1).pgeyAdd(new PgeyVec3(0, 0.3, 0))
                        : pgeyR == pgeyRowsDef.length - 2 ? pgeyRadial.pgeyAdd(new PgeyVec3(0, 1.5, 0)) : pgeyRadial;
                pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyHint);
            }
        }
        PgeyUvRect pgeyRect = PgeyUvLayout.PGEY_SHORTS_WAIST;
        return pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
            pgeyOut[0] = pgeyRect.pgeyU((double) pgeyC / pgeyCols);
            pgeyOut[1] = pgeyRect.pgeyV((double) pgeyR / (pgeyRowsDef.length - 1));
        });
    }

    static PgeyMeshData pgeyStripes(PgeyBodyShape pgeyB) {
        List<PgeyMeshData> pgeyParts = new ArrayList<>();
        for (PgeySide pgeySide : PgeySide.values()) {
            int pgeyRows = 90;
            int pgeyCols = 8;
            double pgeyHalfWidth = 0.019;
            PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, false);
            for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
                double pgeyY = PGEY_WAIST_BOTTOM - 0.002 + (PGEY_SHORTS_HEM + 0.003 - PGEY_WAIST_BOTTOM + 0.002) * pgeyR / (pgeyRows - 1);
                for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                    double pgeyQ = 0.25 + pgeyHalfWidth * (2.0 * pgeyC / (pgeyCols - 1) - 1);
                    PgeyVec3 pgeyP = pgeyShortsPoint(pgeyB, pgeySide, pgeyQ, pgeyY, 0.0026);
                    PgeyVec3 pgeyAxis = pgeyY > PGEY_CROTCH ? pgeyB.pgeyTorsoAxis(pgeyY) : pgeyB.pgeyLeg(pgeySide).pgeyCenter(pgeyB.pgeyLegFractionAt(pgeySide, pgeyY));
                    pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyP.pgeySub(new PgeyVec3(pgeyAxis.pgeyX(), pgeyY, pgeyAxis.pgeyZ())));
                }
            }
            PgeyUvRect pgeyRect = pgeySide == PgeySide.LEFT ? PgeyUvLayout.PGEY_SHORTS_STRIPE_LEFT : PgeyUvLayout.PGEY_SHORTS_STRIPE_RIGHT;
            pgeyParts.add(pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
                pgeyOut[0] = pgeyRect.pgeyU((double) pgeyR / (pgeyRows - 1));
                pgeyOut[1] = pgeyRect.pgeyV((double) pgeyC / (pgeyCols - 1));
            }));
        }
        return PgeyMeshData.pgeyMerge(pgeyParts);
    }

    static PgeyMeshData pgeySocks(PgeyBodyShape pgeyB) {
        List<PgeyMeshData> pgeyParts = new ArrayList<>();
        for (PgeySide pgeySide : PgeySide.values()) {
            PgeyTube pgeyLeg = pgeyB.pgeyLeg(pgeySide);
            double pgeyF0 = pgeyB.pgeyLegFractionAt(pgeySide, PGEY_SOCK_BAND_BOTTOM + 0.004);
            int pgeyRows = 90;
            int pgeyCols = 56;
            PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
            for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
                double pgeyF = pgeyF0 + (1 - pgeyF0) * pgeyR / (pgeyRows - 1);
                PgeyVec3 pgeyCenter = pgeyLeg.pgeyCenter(pgeyF);
                for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                    double pgeyAngle = 2 * Math.PI * pgeyC / pgeyCols;
                    double pgeyRibs = 0.00045 * Math.cos(pgeyAngle * 28);
                    PgeyVec3 pgeyP = pgeyLeg.pgeyPoint(pgeyF, pgeyAngle, 0.0035 + pgeyRibs);
                    pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyP.pgeySub(pgeyCenter));
                }
            }
            PgeyUvRect pgeyRect = pgeySide == PgeySide.LEFT ? PgeyUvLayout.PGEY_SOCK_LEFT : PgeyUvLayout.PGEY_SOCK_RIGHT;
            pgeyParts.add(pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
                pgeyOut[0] = pgeyRect.pgeyU((double) pgeyC / pgeyCols);
                pgeyOut[1] = pgeyRect.pgeyV((double) pgeyR / (pgeyRows - 1));
            }));
        }
        return PgeyMeshData.pgeyMerge(pgeyParts);
    }

    static PgeyMeshData pgeySockBands(PgeyBodyShape pgeyB) {
        List<PgeyMeshData> pgeyParts = new ArrayList<>();
        double[][] pgeyRowsDef = {
                {PGEY_SOCK_BAND_BOTTOM, 0.0038}, {PGEY_SOCK_BAND_BOTTOM + 0.002, 0.0056}, {PGEY_SOCK_TOP - 0.004, 0.0060},
                {PGEY_SOCK_TOP, 0.0050}, {PGEY_SOCK_TOP - 0.0015, 0.0012},
        };
        for (PgeySide pgeySide : PgeySide.values()) {
            PgeyTube pgeyLeg = pgeyB.pgeyLeg(pgeySide);
            int pgeyCols = 56;
            PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRowsDef.length, pgeyCols, true);
            for (int pgeyR = 0; pgeyR < pgeyRowsDef.length; pgeyR++) {
                double pgeyF = pgeyB.pgeyLegFractionAt(pgeySide, pgeyRowsDef[pgeyR][0]);
                PgeyVec3 pgeyCenter = pgeyLeg.pgeyCenter(pgeyF);
                for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                    double pgeyAngle = 2 * Math.PI * pgeyC / pgeyCols;
                    double pgeyRibs = 0.0004 * Math.cos(pgeyAngle * 28);
                    PgeyVec3 pgeyP = pgeyLeg.pgeyPoint(pgeyF, pgeyAngle, pgeyRowsDef[pgeyR][1] + pgeyRibs);
                    PgeyVec3 pgeyRadial = pgeyP.pgeySub(pgeyCenter).pgeyNormalize();
                    PgeyVec3 pgeyHint = pgeyR == pgeyRowsDef.length - 1 ? pgeyRadial.pgeyMul(-1).pgeyAdd(new PgeyVec3(0, 0.3, 0))
                            : pgeyR == pgeyRowsDef.length - 2 ? pgeyRadial.pgeyAdd(new PgeyVec3(0, 1.5, 0)) : pgeyRadial;
                    pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyHint);
                }
            }
            PgeyUvRect pgeyRect = pgeySide == PgeySide.LEFT ? PgeyUvLayout.PGEY_SOCK_BAND_LEFT : PgeyUvLayout.PGEY_SOCK_BAND_RIGHT;
            pgeyParts.add(pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
                pgeyOut[0] = pgeyRect.pgeyU((double) pgeyC / pgeyCols);
                pgeyOut[1] = pgeyRect.pgeyV((double) pgeyR / (pgeyRowsDef.length - 1));
            }));
        }
        return PgeyMeshData.pgeyMerge(pgeyParts);
    }
}
