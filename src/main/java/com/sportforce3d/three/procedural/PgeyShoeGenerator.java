package com.sportforce3d.three.procedural;

import com.sportforce3d.three.geom.PgeyCurve;
import com.sportforce3d.three.geom.PgeyGridSurface;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyTube;
import com.sportforce3d.three.geom.PgeyUvRect;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyUvLayout;
import com.sportforce3d.three.model.PgeyZone;
import com.sportforce3d.three.procedural.PgeyBodyShape.PgeySide;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.sportforce3d.three.geom.PgeyCurve.pgeySmoothstep;

final class PgeyShoeGenerator {

    static final double PGEY_LENGTH = 0.285;
    static final double PGEY_HEEL_BACK = -0.072;
    static final double PGEY_SOLE_BOTTOM = 0.0125;
    static final double PGEY_SOLE_TOP = 0.027;
    static final double PGEY_TOE_OUT_DEG = 6;

    static final PgeyCurve PGEY_WIDTH = PgeyCurve.pgeyOf(0, .000, .012, .022, .035, .034, .09, .041, .22, .044, .42, .045,
            .58, .048, .71, .0500, .83, .0465, .92, .0375, .965, .026, .99, .013, 1.0, .000);
    static final PgeyCurve PGEY_HEIGHT = PgeyCurve.pgeyOf(0, .070, .03, .076, .10, .080, .20, .080, .30, .078, .42, .068,
            .55, .055, .68, .044, .80, .036, .90, .029, 1.0, .018);
    static final PgeyCurve PGEY_SOLE_WIDTH = PgeyCurve.pgeyOf(-.022, .000, -.010, .022, .010, .036, .06, .043, .22, .048, .42, .049,
            .58, .052, .71, .0545, .83, .0510, .92, .0420, .97, .0300, 1.0, .018, 1.022, .000);

    private PgeyShoeGenerator() {
    }

    static double pgeyToeSpring(double pgeyT) {
        return 0.013 * Math.pow(pgeySmoothstep(0.74, 1.04, pgeyT), 1.4);
    }

    record PgeyFoot(PgeyVec3 pgeyOrigin, PgeyVec3 pgeyForward, PgeyVec3 pgeyLateral, int pgeySign) {

        PgeyVec3 pgeyAt(double pgeyT, double pgeyX, double pgeyY) {
            double pgeyZ = PGEY_HEEL_BACK + pgeyT * PGEY_LENGTH;
            return pgeyOrigin.pgeyAddScaled(pgeyForward, pgeyZ).pgeyAddScaled(pgeyLateral, pgeyX).pgeyAdd(new PgeyVec3(0, pgeyY, 0));
        }
    }

    static PgeyFoot pgeyFoot(PgeyBodyShape pgeyB, PgeySide pgeySide) {
        PgeyVec3 pgeyAnkle = pgeyB.pgeyAnkle(pgeySide);
        double pgeyA = Math.toRadians(PGEY_TOE_OUT_DEG);
        PgeyVec3 pgeyForward = new PgeyVec3(pgeySide.pgeySign * Math.sin(pgeyA), 0, Math.cos(pgeyA));
        PgeyVec3 pgeyLateral = new PgeyVec3(Math.cos(pgeyA) * pgeySide.pgeySign, 0, -Math.sin(pgeyA));
        return new PgeyFoot(new PgeyVec3(pgeyAnkle.pgeyX(), 0, pgeyAnkle.pgeyZ()), pgeyForward, pgeyLateral, pgeySide.pgeySign);
    }

    static PgeyVec3 pgeyUpperPoint(PgeyFoot pgeyFoot, double pgeyT, double pgeyA, double pgeyExtra, double pgeyHeightCap) {
        double pgeyOpening = 1 - pgeySmoothstep(0.30, 0.46, pgeyT);
        double pgeyW = PGEY_WIDTH.pgeyAt(pgeyT) + pgeyExtra;
        double pgeyH = Math.min(PGEY_HEIGHT.pgeyAt(pgeyT), PGEY_OPENING_HEIGHT + 0.004 * (1 - pgeyOpening)) + pgeyExtra;
        double pgeyC = Math.cos(pgeyA);
        double pgeyS = Math.sin(pgeyA);
        double pgeyTopExp = 2.0 / (2.25 + 2.6 * pgeyOpening);
        double pgeySideExp = 2.0 / (2.4 + 0.8 * pgeyOpening);
        double pgeyX = pgeyW * Math.signum(pgeyC) * Math.pow(Math.abs(pgeyC), pgeySideExp);
        double pgeyY = pgeyS >= 0 ? Math.min(pgeyH * Math.pow(pgeyS, pgeyTopExp), pgeyHeightCap) : -0.003 * Math.abs(pgeyS);
        return pgeyFoot.pgeyAt(pgeyT, pgeyX, PGEY_SOLE_TOP + pgeyY + pgeyToeSpring(pgeyT));
    }

    static final double PGEY_OPENING_HEIGHT = 0.072;

    static Map<PgeyZone, PgeyMeshData> pgeyBuild(PgeyBodyShape pgeyB) {
        Map<PgeyZone, List<PgeyMeshData>> pgeyParts = new EnumMap<>(PgeyZone.class);
        for (PgeySide pgeySide : PgeySide.values()) {
            PgeyFoot pgeyFoot = pgeyFoot(pgeyB, pgeySide);
            pgeyAdd(pgeyParts, PgeyZone.SHOE_UPPER, pgeyUpper(pgeyB, pgeyFoot));
            pgeyAdd(pgeyParts, PgeyZone.SHOE_UPPER, pgeyTongue(pgeyB, pgeyFoot));
            pgeyAdd(pgeyParts, PgeyZone.SHOE_TOE, pgeyOverlay(pgeyB, pgeyFoot, 0.755, 1.0, 0.0018, 9, PgeyUvLayout.PGEY_SHOE_TOE));
            pgeyAdd(pgeyParts, PgeyZone.SHOE_HEEL, pgeyOverlay(pgeyB, pgeyFoot, 0.0, 0.245, 0.0018, 0.058, PgeyUvLayout.PGEY_SHOE_HEEL));
            pgeyAdd(pgeyParts, PgeyZone.SHOE_UPPER, pgeyCollar(pgeyB, pgeyFoot, pgeySide));
            pgeyAdd(pgeyParts, PgeyZone.SHOE_LACES, pgeyLaces(pgeyB, pgeyFoot));
            pgeyAdd(pgeyParts, PgeyZone.SHOE_SOLE, pgeySole(pgeyB, pgeyFoot));
            pgeyAdd(pgeyParts, PgeyZone.SHOE_SOLE, pgeyStuds(pgeyB, pgeyFoot));
        }
        Map<PgeyZone, PgeyMeshData> pgeyMerged = new EnumMap<>(PgeyZone.class);
        pgeyParts.forEach((pgeyZone, pgeyList) -> pgeyMerged.put(pgeyZone, PgeyMeshData.pgeyMerge(pgeyList)));
        return pgeyMerged;
    }

    private static void pgeyAdd(Map<PgeyZone, List<PgeyMeshData>> pgeyMap, PgeyZone pgeyZone, PgeyMeshData pgeyMesh) {
        pgeyMap.computeIfAbsent(pgeyZone, pgeyZ -> new ArrayList<>()).add(pgeyMesh);
    }

    private static PgeyVec3 pgeyAxisPoint(PgeyFoot pgeyFoot, double pgeyT) {
        return pgeyFoot.pgeyAt(pgeyT, 0, PGEY_SOLE_TOP + PGEY_HEIGHT.pgeyAt(pgeyT) * 0.35 + pgeyToeSpring(pgeyT));
    }

    private static PgeyMeshData pgeyUpper(PgeyBodyShape pgeyB, PgeyFoot pgeyFoot) {
        int pgeyRows = 130;
        int pgeyCols = 72;
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
        for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
            double pgeyU = (double) pgeyR / (pgeyRows - 1);
            double pgeyT = 0.5 - 0.5 * Math.cos(Math.PI * pgeyU);
            for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                double pgeyA = 2 * Math.PI * pgeyC / pgeyCols;
                PgeyVec3 pgeyP = pgeyUpperPoint(pgeyFoot, pgeyT, pgeyA, 0, 1);
                PgeyVec3 pgeyHint = pgeyP.pgeySub(pgeyAxisPoint(pgeyFoot, pgeyT)).pgeyAdd(pgeyFoot.pgeyForward().pgeyMul(pgeyT < 0.02 ? -0.05 : pgeyT > 0.98 ? 0.05 : 0));
                pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyHint);
            }
        }
        PgeyUvRect pgeyRect = PgeyUvLayout.PGEY_SHOE_UPPER;
        return pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
            pgeyOut[0] = pgeyRect.pgeyU((double) pgeyR / (pgeyRows - 1));
            pgeyOut[1] = pgeyRect.pgeyV((double) pgeyC / pgeyCols);
        });
    }

    private static PgeyMeshData pgeyOverlay(PgeyBodyShape pgeyB, PgeyFoot pgeyFoot, double pgeyT0, double pgeyT1, double pgeyExtra, double pgeyHeightCap,
                                    PgeyUvRect pgeyRect) {
        int pgeyRows = 46;
        int pgeyCols = 44;
        double pgeyA0 = -0.20;
        double pgeyA1 = Math.PI + 0.20;
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, false);
        for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
            double pgeyT = pgeyT0 + (pgeyT1 - pgeyT0) * pgeyR / (pgeyRows - 1);
            for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                double pgeyA = pgeyA0 + (pgeyA1 - pgeyA0) * pgeyC / (pgeyCols - 1);
                double pgeyCap = pgeyHeightCap * (pgeyT0 == 0 ? 1 - 0.35 * pgeySmoothstep(0.14, 0.245, pgeyT) : 1);
                PgeyVec3 pgeyP = pgeyUpperPoint(pgeyFoot, pgeyT, pgeyA, pgeyExtra, pgeyCap);
                PgeyVec3 pgeyHint = pgeyP.pgeySub(pgeyAxisPoint(pgeyFoot, pgeyT)).pgeyAdd(pgeyFoot.pgeyForward().pgeyMul(pgeyT < 0.02 ? -0.05 : pgeyT > 0.98 ? 0.05 : 0));
                pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyHint);
            }
        }
        return pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
            pgeyOut[0] = pgeyRect.pgeyU((double) pgeyR / (pgeyRows - 1));
            pgeyOut[1] = pgeyRect.pgeyV((double) pgeyC / (pgeyCols - 1));
        });
    }

    static final double PGEY_COLLAR_Y = PGEY_SOLE_TOP + PGEY_OPENING_HEIGHT;

    private static PgeyMeshData pgeyCollar(PgeyBodyShape pgeyB, PgeyFoot pgeyFoot, PgeySide pgeySide) {
        PgeyTube pgeyLeg = pgeyB.pgeyLeg(pgeySide);
        double pgeyF = 1.0;
        PgeyVec3 pgeyC0 = pgeyLeg.pgeyCenter(pgeyF);
        PgeyVec3 pgeyCenter = new PgeyVec3(pgeyC0.pgeyX(), PGEY_COLLAR_Y, pgeyC0.pgeyZ());
        PgeyVec3 pgeySideAxis = pgeyLeg.pgeySideAxis(pgeyF);
        PgeyVec3 pgeyFrontAxis = pgeyLeg.pgeyFrontAxis(pgeyF);
        double pgeyRx = pgeyLeg.pgeyRadiusSide(pgeyF) + 0.0085;
        double pgeyRz = pgeyLeg.pgeyRadiusFront(pgeyF) + 0.0085;
        int pgeyRing = 80;
        int pgeySection = 16;
        double pgeyTube = 0.0068;
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeySection + 1, pgeyRing, true);
        for (int pgeyR = 0; pgeyR <= pgeySection; pgeyR++) {
            double pgeyS = 2 * Math.PI * pgeyR / pgeySection;
            for (int pgeyC = 0; pgeyC < pgeyRing; pgeyC++) {
                double pgeyA = 2 * Math.PI * pgeyC / pgeyRing;
                PgeyVec3 pgeyRadial = pgeySideAxis.pgeyMul(Math.sin(pgeyA) * pgeyRx).pgeyAdd(pgeyFrontAxis.pgeyMul(Math.cos(pgeyA) * pgeyRz));
                PgeyVec3 pgeyRingPoint = pgeyCenter.pgeyAdd(pgeyRadial);
                PgeyVec3 pgeyOut = new PgeyVec3(pgeyRadial.pgeyX(), 0, pgeyRadial.pgeyZ()).pgeyNormalize();
                PgeyVec3 pgeyP = pgeyRingPoint.pgeyAddScaled(pgeyOut, Math.cos(pgeyS) * pgeyTube).pgeyAdd(new PgeyVec3(0, Math.sin(pgeyS) * pgeyTube * 0.9, 0));
                pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyP.pgeySub(pgeyRingPoint));
            }
        }
        PgeyUvRect pgeyRect = PgeyUvLayout.PGEY_SHOE_UPPER;
        return pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
            pgeyOut[0] = pgeyRect.pgeyU(0.10 + 0.20 * pgeyC / pgeyRing);
            pgeyOut[1] = pgeyRect.pgeyV(0.90 + 0.10 * pgeyR / pgeySection);
        });
    }

    private static PgeyMeshData pgeyTongue(PgeyBodyShape pgeyB, PgeyFoot pgeyFoot) {
        int pgeyRows = 12;
        int pgeyCols = 14;
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, false);
        for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
            double pgeyV = (double) pgeyR / (pgeyRows - 1);
            double pgeyT = 0.315 - 0.055 * pgeyV;
            for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                double pgeyX = 0.021 * (2.0 * pgeyC / (pgeyCols - 1) - 1);
                double pgeyCurve = 1 - Math.pow(Math.abs(pgeyX) / 0.024, 2) * 0.35;
                double pgeyY = PGEY_SOLE_TOP + PGEY_HEIGHT.pgeyAt(0.315) * 0.92 + 0.030 * pgeyV * pgeyCurve + pgeyToeSpring(pgeyT);
                PgeyVec3 pgeyP = pgeyFoot.pgeyAt(pgeyT, pgeyX, pgeyY);
                pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyFoot.pgeyForward().pgeyAdd(new PgeyVec3(0, 0.8, 0)));
            }
        }
        PgeyUvRect pgeyRect = PgeyUvLayout.PGEY_SHOE_UPPER;
        return pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
            pgeyOut[0] = pgeyRect.pgeyU(0.30 + 0.06 * pgeyC / (pgeyCols - 1));
            pgeyOut[1] = pgeyRect.pgeyV(0.20 * pgeyR / (pgeyRows - 1));
        });
    }

    private static PgeyMeshData pgeyLaces(PgeyBodyShape pgeyB, PgeyFoot pgeyFoot) {
        List<PgeyMeshData> pgeyList = new ArrayList<>();
        double[] pgeyTs = {0.345, 0.395, 0.445, 0.495, 0.545};
        for (int pgeyI = 0; pgeyI < pgeyTs.length; pgeyI++) {
            double pgeySkew = (pgeyI % 2 == 0 ? 1 : -1) * 0.012;
            List<PgeyVec3> pgeyPath = new ArrayList<>();
            int pgeySamples = 9;
            for (int pgeyK = 0; pgeyK < pgeySamples; pgeyK++) {
                double pgeyA = Math.PI / 2 + 0.40 * (2.0 * pgeyK / (pgeySamples - 1) - 1);
                double pgeyTt = pgeyTs[pgeyI] + pgeySkew * (2.0 * pgeyK / (pgeySamples - 1) - 1);
                pgeyPath.add(pgeyUpperPoint(pgeyFoot, pgeyTt, pgeyA, 0.0022, 1));
            }
            PgeyTube pgeyLace = new PgeyTube(pgeyPath, PgeyCurve.pgeyOf(0, .0024, 1, .0024), PgeyCurve.pgeyOf(0, .0031, 1, .0031),
                    PgeyCurve.pgeyOf(0, 0, 1, 0), new PgeyVec3(0, 1, 0));
            int pgeyRows = 22;
            int pgeyCols = 12;
            PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
            for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
                double pgeyF = (double) pgeyR / (pgeyRows - 1);
                double pgeyEndTaper = 1 - 0.6 * Math.pow(Math.abs(2 * pgeyF - 1), 8);
                for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                    double pgeyAng = 2 * Math.PI * pgeyC / pgeyCols;
                    PgeyVec3 pgeyUp = pgeyLace.pgeySideAxis(pgeyF);
                    PgeyVec3 pgeyAcross = pgeyLace.pgeyFrontAxis(pgeyF);
                    PgeyVec3 pgeyP = pgeyLace.pgeyCenter(pgeyF).pgeyAddScaled(pgeyAcross, 0.0034 * pgeyEndTaper * Math.sin(pgeyAng))
                            .pgeyAddScaled(pgeyUp, 0.0016 * pgeyEndTaper * Math.cos(pgeyAng));
                    pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyP.pgeySub(pgeyLace.pgeyCenter(pgeyF)));
                }
            }
            PgeyUvRect pgeyRect = PgeyUvLayout.PGEY_SHOE_LACES;
            final int pgeyIdx = pgeyI;
            pgeyList.add(pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
                pgeyOut[0] = pgeyRect.pgeyU((pgeyIdx + (double) pgeyR / (pgeyRows - 1)) / pgeyTs.length);
                pgeyOut[1] = pgeyRect.pgeyV((double) pgeyC / pgeyCols);
            }));
        }
        return PgeyMeshData.pgeyMerge(pgeyList);
    }

    private static PgeyMeshData pgeySole(PgeyBodyShape pgeyB, PgeyFoot pgeyFoot) {
        int pgeyRows = 120;
        int pgeyCols = 64;
        double pgeyT0 = PGEY_SOLE_WIDTH.pgeyMinX();
        double pgeyT1 = PGEY_SOLE_WIDTH.pgeyMaxX();
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
        for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
            double pgeyU = (double) pgeyR / (pgeyRows - 1);
            double pgeyT = pgeyT0 + (pgeyT1 - pgeyT0) * (0.5 - 0.5 * Math.cos(Math.PI * pgeyU));
            double pgeyW = PGEY_SOLE_WIDTH.pgeyAt(pgeyT);
            double pgeyMid = (PGEY_SOLE_BOTTOM + PGEY_SOLE_TOP + 0.002) / 2;
            double pgeyHalf = (PGEY_SOLE_TOP + 0.002 - PGEY_SOLE_BOTTOM) / 2;
            double pgeyLift = pgeyToeSpring(Math.min(1, Math.max(0, pgeyT)));
            for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                double pgeyA = 2 * Math.PI * pgeyC / pgeyCols;
                double pgeyCo = Math.cos(pgeyA);
                double pgeyS = Math.sin(pgeyA);
                double pgeyX = pgeyW * Math.signum(pgeyCo) * Math.pow(Math.abs(pgeyCo), 2.0 / 6);
                double pgeyY = pgeyMid + pgeyHalf * Math.signum(pgeyS) * Math.pow(Math.abs(pgeyS), 2.0 / 6) + pgeyLift;
                PgeyVec3 pgeyP = pgeyFoot.pgeyAt(pgeyT, pgeyX, pgeyY);
                PgeyVec3 pgeyHint = pgeyP.pgeySub(pgeyFoot.pgeyAt(pgeyT, 0, pgeyMid + pgeyLift)).pgeyAdd(pgeyFoot.pgeyForward().pgeyMul(pgeyT < 0 ? -0.05 : pgeyT > 1 ? 0.05 : 0));
                pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyHint);
            }
        }
        PgeyUvRect pgeyRect = PgeyUvLayout.PGEY_SHOE_SOLE;
        return pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
            pgeyOut[0] = pgeyRect.pgeyU((double) pgeyR / (pgeyRows - 1));
            pgeyOut[1] = pgeyRect.pgeyV((double) pgeyC / pgeyCols);
        });
    }

    private static PgeyMeshData pgeyStuds(PgeyBodyShape pgeyB, PgeyFoot pgeyFoot) {
        double[][] pgeyPositions = {
                {0.585, 0.60}, {0.585, -0.60}, {0.745, 0.62}, {0.745, -0.62}, {0.885, 0.36}, {0.885, -0.36},
                {0.085, 0.52}, {0.085, -0.52}, {0.225, 0.57}, {0.225, -0.57},
        };
        List<PgeyMeshData> pgeyList = new ArrayList<>();
        for (double[] pgeyPos : pgeyPositions) {
            double pgeyT = pgeyPos[0];
            double pgeyX = PGEY_SOLE_WIDTH.pgeyAt(pgeyT) * pgeyPos[1];
            double pgeyTop = PGEY_SOLE_BOTTOM + pgeyToeSpring(pgeyT) + 0.001;
            double pgeyHeight = pgeyTop - 0.0004;
            int pgeyRows = 8;
            int pgeyCols = 20;
            PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
            for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
                double pgeyV = (double) pgeyR / (pgeyRows - 1);
                double pgeyRadius = pgeyR == pgeyRows - 1 ? 0.0002 : 0.0068 - 0.0026 * pgeyV - (pgeyR == pgeyRows - 2 ? 0.0012 : 0);
                double pgeyY = pgeyTop - pgeyHeight * Math.min(1, pgeyV * 1.0);
                for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                    double pgeyA = 2 * Math.PI * pgeyC / pgeyCols;
                    PgeyVec3 pgeyCenter = pgeyFoot.pgeyAt(pgeyT, pgeyX, pgeyY);
                    PgeyVec3 pgeyP = pgeyCenter.pgeyAddScaled(pgeyFoot.pgeyLateral(), pgeyRadius * Math.sin(pgeyA)).pgeyAddScaled(pgeyFoot.pgeyForward(), pgeyRadius * Math.cos(pgeyA));
                    PgeyVec3 pgeyHint = pgeyP.pgeySub(pgeyCenter).pgeyAdd(new PgeyVec3(0, pgeyR >= pgeyRows - 2 ? -0.004 : 0, 0));
                    pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyHint);
                }
            }
            PgeyUvRect pgeyRect = PgeyUvLayout.PGEY_SHOE_SOLE;
            pgeyList.add(pgeyG.pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
                pgeyOut[0] = pgeyRect.pgeyU(0.9 + 0.1 * pgeyC / pgeyCols);
                pgeyOut[1] = pgeyRect.pgeyV((double) pgeyR / (pgeyRows - 1));
            }));
        }
        return PgeyMeshData.pgeyMerge(pgeyList);
    }
}
