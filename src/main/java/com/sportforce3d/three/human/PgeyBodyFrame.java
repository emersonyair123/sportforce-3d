package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.PgeyTube;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.procedural.PgeyBodyShape;
import com.sportforce3d.three.procedural.PgeyProceduralModelProvider;

import java.util.Arrays;

abstract class PgeyBodyFrame {

    final double pgeyFloor;
    final double pgeyCrotch;
    final double pgeyArmpit;
    final double pgeyNeck;
    final double pgeyTop;

    PgeyBodyFrame(double pgeyFloor, double pgeyCrotch, double pgeyArmpit, double pgeyNeck, double pgeyTop) {
        this.pgeyFloor = pgeyFloor;
        this.pgeyCrotch = pgeyCrotch;
        this.pgeyArmpit = pgeyArmpit;
        this.pgeyNeck = pgeyNeck;
        this.pgeyTop = pgeyTop;
    }

    double pgeyHeight() {
        return pgeyTop - pgeyFloor;
    }

    abstract PgeyVec3 pgeyTorsoCenter(double pgeyY);

    abstract PgeyVec3 pgeyLegCenter(int pgeySign, double pgeyY);

    abstract PgeyVec3 pgeyArmPoint(int pgeySign, double pgeyS);

    abstract PgeyVec3 pgeyArmDirection(int pgeySign, double pgeyS);

    abstract double pgeyArmProject(int pgeySign, PgeyVec3 pgeyP);

    double pgeyTorsoRadius(double pgeyY, PgeyVec3 pgeyDir) {
        return Double.NaN;
    }

    double pgeyLegRadius(int pgeySign, double pgeyY, PgeyVec3 pgeyDir) {
        return Double.NaN;
    }

    double pgeyTorsoExit(PgeyVec3 pgeyOrigin, PgeyVec3 pgeyDir) {
        return Double.NaN;
    }

    double pgeyArmRadius(int pgeySign, double pgeyS, PgeyVec3 pgeyDir) {
        return Double.NaN;
    }

    boolean pgeyInsideTorso(PgeyVec3 pgeyP) {
        return false;
    }

    static PgeyBodyFrame pgeyOf(PgeyBodyShape pgeyShape, double[] pgeyMannequinVertices) {
        return new PgeyProcedural(pgeyShape, pgeyMannequinVertices);
    }

    private static final class PgeyProcedural extends PgeyBodyFrame {
        private final PgeyBodyShape pgeyB;
        private final double pgeyScale;
        private final double pgeyGirth;
        private final PgeyVec3[][] pgeyArm = new PgeyVec3[2][];
        private final double[][] pgeyArmLen = new double[2][];
        private final double[] pgeyArmpitS = new double[2];

        PgeyProcedural(PgeyBodyShape pgeyB, double[] pgeyV) {
            super(0, PgeyProceduralModelProvider.PGEY_SHORTS_CROTCH * pgeyB.pgeyHeight() / 1.812, PgeyProceduralModelProvider.PGEY_ARMPIT * pgeyB.pgeyHeight() / 1.812,
                    pgeyNeckBase(pgeyV, PgeyProceduralModelProvider.PGEY_ARMPIT * pgeyB.pgeyHeight() / 1.812, pgeyB.pgeyHeight()), pgeyB.pgeyHeight());
            this.pgeyB = pgeyB;
            this.pgeyScale = pgeyB.pgeyHeight() / 1.812;
            this.pgeyGirth = pgeyB.pgeyOut(new PgeyVec3(1, 0, 0)).pgeyX();
            for (PgeyBodyShape.PgeySide pgeySide : PgeyBodyShape.PgeySide.values()) {
                PgeyTube pgeyT = pgeyB.pgeyArm(pgeySide);
                int pgeyN = 200;
                PgeyVec3[] pgeyPts = new PgeyVec3[pgeyN + 1];
                double[] pgeyLen = new double[pgeyN + 1];
                for (int pgeyI = 0; pgeyI <= pgeyN; pgeyI++) {
                    pgeyPts[pgeyI] = pgeyB.pgeyOut(pgeyT.pgeySpine((double) pgeyI / pgeyN));
                    pgeyLen[pgeyI] = pgeyI == 0 ? 0 : pgeyLen[pgeyI - 1] + pgeyPts[pgeyI].pgeySub(pgeyPts[pgeyI - 1]).pgeyLength();
                }
                int pgeyK = pgeySide.pgeySign > 0 ? 1 : 0;
                pgeyArm[pgeyK] = pgeyPts;
                pgeyArmLen[pgeyK] = pgeyLen;
                pgeyArmpitS[pgeyK] = pgeyArcAtHeight(pgeyPts, pgeyLen, pgeyArmpit);
            }
        }

        @Override
        PgeyVec3 pgeyTorsoCenter(double pgeyY) {
            PgeyVec3 pgeyA = pgeyB.pgeyOut(pgeyB.pgeyTorsoAxis(pgeyY / pgeyScale));
            return new PgeyVec3(0, pgeyY, pgeyA.pgeyZ());
        }

        @Override
        PgeyVec3 pgeyLegCenter(int pgeySign, double pgeyY) {
            PgeyBodyShape.PgeySide pgeySide = pgeySign > 0 ? PgeyBodyShape.PgeySide.LEFT : PgeyBodyShape.PgeySide.RIGHT;
            PgeyTube pgeyLeg = pgeyB.pgeyLeg(pgeySide);
            PgeyVec3 pgeyC = pgeyB.pgeyOut(pgeyLeg.pgeyCenter(pgeyB.pgeyLegFractionAt(pgeySide, pgeyY / pgeyScale)));
            return new PgeyVec3(pgeyC.pgeyX(), pgeyY, pgeyC.pgeyZ());
        }

        @Override
        PgeyVec3 pgeyArmPoint(int pgeySign, double pgeyS) {
            int pgeyK = pgeySign > 0 ? 1 : 0;
            return pgeyAt(pgeyArm[pgeyK], pgeyArmLen[pgeyK], pgeyArmpitS[pgeyK] + pgeyS);
        }

        @Override
        PgeyVec3 pgeyArmDirection(int pgeySign, double pgeyS) {
            int pgeyK = pgeySign > 0 ? 1 : 0;
            double pgeyA = pgeyArmpitS[pgeyK] + pgeyS;
            return pgeyAt(pgeyArm[pgeyK], pgeyArmLen[pgeyK], pgeyA + 0.01).pgeySub(pgeyAt(pgeyArm[pgeyK], pgeyArmLen[pgeyK], pgeyA - 0.01)).pgeyNormalize();
        }

        @Override
        double pgeyArmProject(int pgeySign, PgeyVec3 pgeyP) {
            int pgeyK = pgeySign > 0 ? 1 : 0;
            PgeyVec3[] pgeyPts = pgeyArm[pgeyK];
            double[] pgeyLen = pgeyArmLen[pgeyK];
            double pgeyBest = Double.MAX_VALUE;
            double pgeyBestS = 0;
            for (int pgeyI = 0; pgeyI < pgeyPts.length - 1; pgeyI++) {
                PgeyVec3 pgeyAb = pgeyPts[pgeyI + 1].pgeySub(pgeyPts[pgeyI]);
                double pgeyL2 = pgeyAb.pgeyDot(pgeyAb);
                double pgeyT = pgeyL2 < 1e-12 ? 0 : Math.max(0, Math.min(1, pgeyP.pgeySub(pgeyPts[pgeyI]).pgeyDot(pgeyAb) / pgeyL2));
                double pgeyD = pgeyPts[pgeyI].pgeyAddScaled(pgeyAb, pgeyT).pgeySub(pgeyP).pgeyLength();
                if (pgeyD < pgeyBest) {
                    pgeyBest = pgeyD;
                    pgeyBestS = pgeyLen[pgeyI] + pgeyT * Math.sqrt(pgeyL2);
                }
            }
            return pgeyBestS - pgeyArmpitS[pgeyK];
        }

        @Override
        double pgeyTorsoRadius(double pgeyY, PgeyVec3 pgeyDir) {
            return pgeyB.pgeyTorsoRadius(Math.atan2(pgeyDir.pgeyX(), pgeyDir.pgeyZ()), pgeyY / pgeyScale) * pgeyGirth;
        }

        @Override
        double pgeyLegRadius(int pgeySign, double pgeyY, PgeyVec3 pgeyDir) {
            PgeyBodyShape.PgeySide pgeySide = pgeySign > 0 ? PgeyBodyShape.PgeySide.LEFT : PgeyBodyShape.PgeySide.RIGHT;
            return pgeyEllipse(pgeyB.pgeyLeg(pgeySide), pgeyB.pgeyLegFractionAt(pgeySide, pgeyY / pgeyScale), pgeyDir) * pgeyGirth;
        }

        @Override
        double pgeyArmRadius(int pgeySign, double pgeyS, PgeyVec3 pgeyDir) {
            int pgeyK = pgeySign > 0 ? 1 : 0;
            double pgeyTotal = pgeyArmLen[pgeyK][pgeyArmLen[pgeyK].length - 1];
            double pgeyF = Math.max(0, Math.min(1, (pgeyArmpitS[pgeyK] + pgeyS) / pgeyTotal));
            return pgeyEllipse(pgeyB.pgeyArm(pgeySign > 0 ? PgeyBodyShape.PgeySide.LEFT : PgeyBodyShape.PgeySide.RIGHT), pgeyF, pgeyDir) * pgeyGirth;
        }

        @Override
        double pgeyTorsoExit(PgeyVec3 pgeyOrigin, PgeyVec3 pgeyDir) {
            double pgeyStep = 0.006;
            double pgeyT = 0;
            while (pgeyT < 0.6 && pgeyInsideTorso(pgeyOrigin.pgeyAddScaled(pgeyDir, pgeyT))) {
                pgeyT += pgeyStep;
            }
            if (pgeyT >= 0.6) {
                return Double.NaN;
            }
            double pgeyLo = Math.max(0, pgeyT - pgeyStep);
            double pgeyHi = pgeyT;
            for (int pgeyI = 0; pgeyI < 20; pgeyI++) {
                double pgeyMid = (pgeyLo + pgeyHi) / 2;
                if (pgeyInsideTorso(pgeyOrigin.pgeyAddScaled(pgeyDir, pgeyMid))) {
                    pgeyLo = pgeyMid;
                } else {
                    pgeyHi = pgeyMid;
                }
            }
            return (pgeyLo + pgeyHi) / 2;
        }

        @Override
        boolean pgeyInsideTorso(PgeyVec3 pgeyQ) {
            double pgeyYd = pgeyQ.pgeyY() / pgeyScale;
            if (pgeyYd > PgeyBodyShape.PGEY_TORSO_TOP || pgeyYd < PgeyBodyShape.PGEY_TORSO_BOTTOM) {
                return false;
            }
            PgeyVec3 pgeyC = pgeyTorsoCenter(pgeyQ.pgeyY());
            double pgeyDx = pgeyQ.pgeyX() - pgeyC.pgeyX();
            double pgeyDz = pgeyQ.pgeyZ() - pgeyC.pgeyZ();
            double pgeyR = Math.sqrt(pgeyDx * pgeyDx + pgeyDz * pgeyDz);
            return pgeyR < pgeyB.pgeyTorsoRadius(Math.atan2(pgeyDx, pgeyDz), pgeyYd) * pgeyGirth;
        }

        private static double pgeyEllipse(PgeyTube pgeyT, double pgeyF, PgeyVec3 pgeyDir) {
            double pgeyA = pgeyDir.pgeyDot(pgeyT.pgeyFrontAxis(pgeyF));
            double pgeyC = pgeyDir.pgeyDot(pgeyT.pgeySideAxis(pgeyF));
            double pgeyRz = pgeyT.pgeyRadiusFront(pgeyF);
            double pgeyRx = pgeyT.pgeyRadiusSide(pgeyF);
            return 1 / Math.sqrt((pgeyA / pgeyRz) * (pgeyA / pgeyRz) + (pgeyC / pgeyRx) * (pgeyC / pgeyRx));
        }

        private static PgeyVec3 pgeyAt(PgeyVec3[] pgeyPts, double[] pgeyLen, double pgeyS) {
            if (pgeyS <= 0) {
                return pgeyPts[0].pgeyAddScaled(pgeyPts[1].pgeySub(pgeyPts[0]).pgeyNormalize(), pgeyS);
            }
            for (int pgeyI = 1; pgeyI < pgeyPts.length; pgeyI++) {
                if (pgeyLen[pgeyI] >= pgeyS) {
                    double pgeySeg = pgeyLen[pgeyI] - pgeyLen[pgeyI - 1];
                    return pgeyPts[pgeyI - 1].pgeyLerp(pgeyPts[pgeyI], pgeySeg < 1e-12 ? 0 : (pgeyS - pgeyLen[pgeyI - 1]) / pgeySeg);
                }
            }
            return pgeyPts[pgeyPts.length - 1];
        }

        private static double pgeyArcAtHeight(PgeyVec3[] pgeyPts, double[] pgeyLen, double pgeyY) {
            for (int pgeyI = 1; pgeyI < pgeyPts.length; pgeyI++) {
                if (pgeyPts[pgeyI].pgeyY() <= pgeyY) {
                    double pgeyDy = pgeyPts[pgeyI - 1].pgeyY() - pgeyPts[pgeyI].pgeyY();
                    double pgeyT = pgeyDy < 1e-12 ? 0 : (pgeyPts[pgeyI - 1].pgeyY() - pgeyY) / pgeyDy;
                    return pgeyLen[pgeyI - 1] + pgeyT * (pgeyLen[pgeyI] - pgeyLen[pgeyI - 1]);
                }
            }
            return 0;
        }
    }

    static PgeyBodyFrame pgeyMeasure(double[] pgeyV, int[] pgeyTri, double pgeyFloor, double pgeyHeight) {
        PgeySlices pgeyS = new PgeySlices(pgeyV, pgeyTri);
        return new PgeyMeasured(pgeyS, pgeyFloor, pgeyHeight, pgeyS.pgeyArmpit(pgeyFloor, pgeyHeight));
    }

    private static final class PgeyMeasured extends PgeyBodyFrame {
        private static final double PGEY_STEP = 0.004;
        private final double[] pgeyTorsoZ;
        private final double[][] pgeyLegX = new double[2][];
        private final double[][] pgeyLegZ = new double[2][];
        private final PgeyVec3[] pgeyArmOrigin = new PgeyVec3[2];
        private final PgeyVec3[] pgeyArmDir = new PgeyVec3[2];

        PgeyMeasured(PgeySlices pgeyS, double pgeyFloor, double pgeyH, double pgeyArmpit) {
            super(pgeyFloor, pgeyLowestCenter(pgeyS.pgeyV, pgeyH + pgeyFloor), pgeyArmpit, pgeyNeckBase(pgeyS.pgeyV, pgeyArmpit, pgeyFloor + pgeyH), pgeyFloor + pgeyH);
            int pgeyN = (int) Math.ceil(pgeyH / PGEY_STEP) + 2;
            pgeyTorsoZ = new double[pgeyN];
            for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
                double pgeyY = pgeyFloor + pgeyI * PGEY_STEP;
                double[] pgeyBox = pgeyS.pgeyContour(pgeyY, -0.03, 0.03);
                pgeyTorsoZ[pgeyI] = pgeyBox == null ? Double.NaN : (pgeyBox[2] + pgeyBox[3]) / 2;
            }
            pgeyFillGaps(pgeyTorsoZ);
            double pgeyLegMax = 0.135 * pgeyH;
            for (int pgeyK = 0; pgeyK < 2; pgeyK++) {
                int pgeySign = pgeyK == 0 ? -1 : 1;
                pgeyLegX[pgeyK] = new double[pgeyN];
                pgeyLegZ[pgeyK] = new double[pgeyN];
                for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
                    double pgeyY = pgeyFloor + pgeyI * PGEY_STEP;
                    double[] pgeyBox = pgeyY < pgeyCrotch - 0.01 ? pgeyS.pgeyContour(pgeyY, pgeySign < 0 ? -pgeyLegMax : 0.0, pgeySign < 0 ? 0.0 : pgeyLegMax) : null;
                    pgeyLegX[pgeyK][pgeyI] = pgeyBox == null ? Double.NaN : (pgeyBox[0] + pgeyBox[1]) / 2;
                    pgeyLegZ[pgeyK][pgeyI] = pgeyBox == null ? Double.NaN : (pgeyBox[2] + pgeyBox[3]) / 2;
                }
                pgeyFillGaps(pgeyLegX[pgeyK]);
                pgeyFillGaps(pgeyLegZ[pgeyK]);
                pgeyFitArm(pgeyS, pgeyK, pgeySign, pgeyH);
            }
        }

        private void pgeyFitArm(PgeySlices pgeyS, int pgeyK, int pgeySign, double pgeyH) {
            int pgeyN = 0;
            double pgeySy = 0;
            double pgeySyy = 0;
            double pgeySx = 0;
            double pgeySxy = 0;
            double pgeySz = 0;
            double pgeySzy = 0;
            for (double pgeyY = pgeyArmpit - 0.11 * pgeyH; pgeyY < pgeyArmpit - 0.015 * pgeyH; pgeyY += 0.006) {
                double pgeyW = pgeyS.pgeyTorsoHalfWidth(pgeyY, pgeySign);
                if (Double.isNaN(pgeyW)) {
                    continue;
                }
                double[] pgeyBox = pgeySign > 0 ? pgeyS.pgeyContour(pgeyY, pgeyW + 0.004, 10) : pgeyS.pgeyContour(pgeyY, -10, -(pgeyW + 0.004));
                if (pgeyBox == null) {
                    continue;
                }
                double pgeyCx = (pgeyBox[0] + pgeyBox[1]) / 2;
                double pgeyCz = (pgeyBox[2] + pgeyBox[3]) / 2;
                pgeyN++;
                pgeySy += pgeyY;
                pgeySyy += pgeyY * pgeyY;
                pgeySx += pgeyCx;
                pgeySxy += pgeyCx * pgeyY;
                pgeySz += pgeyCz;
                pgeySzy += pgeyCz * pgeyY;
            }
            double pgeyDet = pgeyN * pgeySyy - pgeySy * pgeySy;
            if (pgeyN < 4 || Math.abs(pgeyDet) < 1e-12) {
                pgeyArmOrigin[pgeyK] = new PgeyVec3(pgeySign * 0.11 * pgeyH, pgeyArmpit, 0);
                pgeyArmDir[pgeyK] = new PgeyVec3(pgeySign * 0.25, -1, 0).pgeyNormalize();
                return;
            }
            double pgeyBx = (pgeyN * pgeySxy - pgeySx * pgeySy) / pgeyDet;
            double pgeyAx = (pgeySx - pgeyBx * pgeySy) / pgeyN;
            double pgeyBz = (pgeyN * pgeySzy - pgeySz * pgeySy) / pgeyDet;
            double pgeyAz = (pgeySz - pgeyBz * pgeySy) / pgeyN;
            pgeyArmOrigin[pgeyK] = new PgeyVec3(pgeyAx + pgeyBx * pgeyArmpit, pgeyArmpit, pgeyAz + pgeyBz * pgeyArmpit);
            pgeyArmDir[pgeyK] = new PgeyVec3(-pgeyBx, -1, -pgeyBz).pgeyNormalize();
        }

        @Override
        PgeyVec3 pgeyTorsoCenter(double pgeyY) {
            return new PgeyVec3(0, pgeyY, pgeySample(pgeyTorsoZ, pgeyY));
        }

        @Override
        PgeyVec3 pgeyLegCenter(int pgeySign, double pgeyY) {
            int pgeyK = pgeySign > 0 ? 1 : 0;
            double pgeyYy = Math.min(pgeyY, pgeyCrotch - 0.02);
            return new PgeyVec3(pgeySample(pgeyLegX[pgeyK], pgeyYy), pgeyY, pgeySample(pgeyLegZ[pgeyK], pgeyYy));
        }

        @Override
        PgeyVec3 pgeyArmPoint(int pgeySign, double pgeyS) {
            int pgeyK = pgeySign > 0 ? 1 : 0;
            return pgeyArmOrigin[pgeyK].pgeyAddScaled(pgeyArmDir[pgeyK], pgeyS);
        }

        @Override
        PgeyVec3 pgeyArmDirection(int pgeySign, double pgeyS) {
            return pgeyArmDir[pgeySign > 0 ? 1 : 0];
        }

        @Override
        double pgeyArmProject(int pgeySign, PgeyVec3 pgeyP) {
            int pgeyK = pgeySign > 0 ? 1 : 0;
            return pgeyP.pgeySub(pgeyArmOrigin[pgeyK]).pgeyDot(pgeyArmDir[pgeyK]);
        }

        private double pgeySample(double[] pgeyTable, double pgeyY) {
            double pgeyF = (pgeyY - pgeyFloor) / PGEY_STEP;
            int pgeyI = (int) Math.floor(pgeyF);
            if (pgeyI < 0) {
                return pgeyTable[0];
            }
            if (pgeyI >= pgeyTable.length - 1) {
                return pgeyTable[pgeyTable.length - 1];
            }
            double pgeyT = pgeyF - pgeyI;
            return pgeyTable[pgeyI] * (1 - pgeyT) + pgeyTable[pgeyI + 1] * pgeyT;
        }
    }

    private static final class PgeySlices {
        final double[] pgeyV;
        final int[] pgeyTri;
        final int[] pgeyOrder;
        final double[] pgeyYs;

        PgeySlices(double[] pgeyV, int[] pgeyTri) {
            this.pgeyV = pgeyV;
            this.pgeyTri = pgeyTri;
            int pgeyN = pgeyV.length / 3;
            Integer[] pgeyIdx = new Integer[pgeyN];
            for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
                pgeyIdx[pgeyI] = pgeyI;
            }
            Arrays.sort(pgeyIdx, (pgeyA, pgeyB) -> Double.compare(pgeyV[pgeyA * 3 + 1], pgeyV[pgeyB * 3 + 1]));
            pgeyOrder = new int[pgeyN];
            pgeyYs = new double[pgeyN];
            for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
                pgeyOrder[pgeyI] = pgeyIdx[pgeyI];
                pgeyYs[pgeyI] = pgeyV[pgeyIdx[pgeyI] * 3 + 1];
            }
        }

        private int pgeyLower(double pgeyY) {
            int pgeyI = Arrays.binarySearch(pgeyYs, pgeyY);
            if (pgeyI < 0) {
                return -pgeyI - 1;
            }
            while (pgeyI > 0 && pgeyYs[pgeyI - 1] == pgeyY) {
                pgeyI--;
            }
            return pgeyI;
        }

        double[] pgeyContour(double pgeyY, double pgeyX0, double pgeyX1) {
            double[] pgeyB = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
            boolean pgeyAny = false;
            for (int pgeyT = 0; pgeyT < pgeyTri.length; pgeyT += 3) {
                for (int pgeyE = 0; pgeyE < 3; pgeyE++) {
                    int pgeyA = pgeyTri[pgeyT + pgeyE] * 3;
                    int pgeyC = pgeyTri[pgeyT + (pgeyE + 1) % 3] * 3;
                    double pgeyYa = pgeyV[pgeyA + 1];
                    double pgeyYc = pgeyV[pgeyC + 1];
                    if ((pgeyYa - pgeyY) * (pgeyYc - pgeyY) > 0 || pgeyYa == pgeyYc) {
                        continue;
                    }
                    double pgeyK = (pgeyY - pgeyYa) / (pgeyYc - pgeyYa);
                    double pgeyX = pgeyV[pgeyA] + pgeyK * (pgeyV[pgeyC] - pgeyV[pgeyA]);
                    if (pgeyX < pgeyX0 || pgeyX > pgeyX1) {
                        continue;
                    }
                    double pgeyZ = pgeyV[pgeyA + 2] + pgeyK * (pgeyV[pgeyC + 2] - pgeyV[pgeyA + 2]);
                    pgeyB[0] = Math.min(pgeyB[0], pgeyX);
                    pgeyB[1] = Math.max(pgeyB[1], pgeyX);
                    pgeyB[2] = Math.min(pgeyB[2], pgeyZ);
                    pgeyB[3] = Math.max(pgeyB[3], pgeyZ);
                    pgeyAny = true;
                }
            }
            return pgeyAny ? pgeyB : null;
        }

        double[] pgeyBox(double pgeyY, double pgeyHalf, double pgeyX0, double pgeyX1, double pgeyZ0, double pgeyZ1) {
            double[] pgeyB = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
            boolean pgeyAny = false;
            for (int pgeyI = pgeyLower(pgeyY - pgeyHalf); pgeyI < pgeyYs.length && pgeyYs[pgeyI] <= pgeyY + pgeyHalf; pgeyI++) {
                int pgeyP = pgeyOrder[pgeyI] * 3;
                double pgeyX = pgeyV[pgeyP];
                double pgeyZ = pgeyV[pgeyP + 2];
                if (pgeyX >= pgeyX0 && pgeyX <= pgeyX1 && pgeyZ >= pgeyZ0 && pgeyZ <= pgeyZ1) {
                    pgeyB[0] = Math.min(pgeyB[0], pgeyX);
                    pgeyB[1] = Math.max(pgeyB[1], pgeyX);
                    pgeyB[2] = Math.min(pgeyB[2], pgeyZ);
                    pgeyB[3] = Math.max(pgeyB[3], pgeyZ);
                    pgeyAny = true;
                }
            }
            return pgeyAny ? pgeyB : null;
        }

        double pgeyTorsoHalfWidth(double pgeyY, int pgeySign) {
            double[] pgeyStart = new double[64];
            double[] pgeyEnd = new double[64];
            int pgeyN = 0;
            double[] pgeyXs = new double[3];
            for (int pgeyT = 0; pgeyT < pgeyTri.length; pgeyT += 3) {
                int pgeyA = pgeyTri[pgeyT] * 3;
                int pgeyB = pgeyTri[pgeyT + 1] * 3;
                int pgeyC = pgeyTri[pgeyT + 2] * 3;
                double pgeyYa = pgeyV[pgeyA + 1];
                double pgeyYb = pgeyV[pgeyB + 1];
                double pgeyYc = pgeyV[pgeyC + 1];
                if (Math.min(pgeyYa, Math.min(pgeyYb, pgeyYc)) > pgeyY || Math.max(pgeyYa, Math.max(pgeyYb, pgeyYc)) < pgeyY) {
                    continue;
                }
                int pgeyK = 0;
                pgeyK = pgeyCross(pgeyA, pgeyB, pgeyY, pgeyXs, pgeyK);
                pgeyK = pgeyCross(pgeyB, pgeyC, pgeyY, pgeyXs, pgeyK);
                pgeyK = pgeyCross(pgeyC, pgeyA, pgeyY, pgeyXs, pgeyK);
                if (pgeyK < 2) {
                    continue;
                }
                double pgeyU0 = Math.min(pgeyXs[0], pgeyXs[1]) * pgeySign;
                double pgeyU1 = Math.max(pgeyXs[0], pgeyXs[1]) * pgeySign;
                double pgeyLo = Math.min(pgeyU0, pgeyU1);
                double pgeyHi = Math.max(pgeyU0, pgeyU1);
                if (pgeyHi <= 0) {
                    continue;
                }
                if (pgeyN == pgeyStart.length) {
                    pgeyStart = Arrays.copyOf(pgeyStart, pgeyN * 2);
                    pgeyEnd = Arrays.copyOf(pgeyEnd, pgeyN * 2);
                }
                pgeyStart[pgeyN] = Math.max(0, pgeyLo);
                pgeyEnd[pgeyN] = pgeyHi;
                pgeyN++;
            }
            if (pgeyN == 0) {
                return Double.NaN;
            }
            Integer[] pgeyOrder = new Integer[pgeyN];
            for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
                pgeyOrder[pgeyI] = pgeyI;
            }
            final double[] pgeySt = pgeyStart;
            Arrays.sort(pgeyOrder, (pgeyP, pgeyQ) -> Double.compare(pgeySt[pgeyP], pgeySt[pgeyQ]));
            double pgeyTorsoEnd = Double.NaN;
            double pgeyCurEnd = -1;
            double pgeyLastEnd = -1;
            for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
                double pgeyS0 = pgeyStart[pgeyOrder[pgeyI]];
                double pgeyE0 = pgeyEnd[pgeyOrder[pgeyI]];
                if (pgeyCurEnd < 0) {
                    pgeyCurEnd = pgeyE0;
                } else if (pgeyS0 > pgeyCurEnd + 0.008 && Double.isNaN(pgeyTorsoEnd)) {
                    pgeyTorsoEnd = pgeyCurEnd;
                    pgeyCurEnd = pgeyE0;
                } else {
                    pgeyCurEnd = Math.max(pgeyCurEnd, pgeyE0);
                }
                pgeyLastEnd = Math.max(pgeyLastEnd, pgeyE0);
            }
            if (Double.isNaN(pgeyTorsoEnd) || pgeyTorsoEnd < 0.05 || pgeyLastEnd - pgeyTorsoEnd < 0.03) {
                return Double.NaN;
            }
            return pgeyTorsoEnd;
        }

        private int pgeyCross(int pgeyA, int pgeyB, double pgeyY, double[] pgeyXs, int pgeyK) {
            double pgeyYa = pgeyV[pgeyA + 1];
            double pgeyYb = pgeyV[pgeyB + 1];
            if (pgeyK >= 2 || (pgeyYa - pgeyY) * (pgeyYb - pgeyY) > 0 || pgeyYa == pgeyYb) {
                return pgeyK;
            }
            double pgeyT = (pgeyY - pgeyYa) / (pgeyYb - pgeyYa);
            pgeyXs[pgeyK] = pgeyV[pgeyA] + pgeyT * (pgeyV[pgeyB] - pgeyV[pgeyA]);
            return pgeyK + 1;
        }

        double pgeyArmpit(double pgeyFloor, double pgeyH) {
            double pgeyStep = 0.003;
            int pgeyNeed = (int) Math.ceil(0.06 * pgeyH / pgeyStep);
            int pgeyRun = 0;
            double pgeyTop = Double.NaN;
            for (double pgeyY = pgeyFloor + 0.86 * pgeyH; pgeyY > pgeyFloor + 0.5 * pgeyH; pgeyY -= pgeyStep) {
                if (!Double.isNaN(pgeyTorsoHalfWidth(pgeyY, -1)) && !Double.isNaN(pgeyTorsoHalfWidth(pgeyY, 1))) {
                    if (pgeyRun == 0) {
                        pgeyTop = pgeyY;
                    }
                    if (++pgeyRun >= pgeyNeed) {
                        return pgeyTop;
                    }
                } else {
                    pgeyRun = 0;
                }
            }
            return pgeyFloor + 0.73 * pgeyH;
        }
    }

    static double pgeyLowestCenter(double[] pgeyV, double pgeyTop) {
        double pgeyBest = Double.MAX_VALUE;
        for (int pgeyI = 0; pgeyI < pgeyV.length; pgeyI += 3) {
            double pgeyY = pgeyV[pgeyI + 1];
            if (Math.abs(pgeyV[pgeyI]) < 0.012 && pgeyY > 0.3 * pgeyTop && pgeyY < 0.65 * pgeyTop) {
                pgeyBest = Math.min(pgeyBest, pgeyY);
            }
        }
        return pgeyBest == Double.MAX_VALUE ? 0.47 * pgeyTop : pgeyBest;
    }

    static double pgeyNeckBase(double[] pgeyV, double pgeyArmpit, double pgeyTop) {
        double pgeyH = pgeyTop;
        double pgeyStep = 0.004;
        int pgeyN = (int) Math.ceil((pgeyTop - pgeyArmpit) / pgeyStep) + 1;
        double[] pgeyWidth = new double[pgeyN];
        for (int pgeyI = 0; pgeyI < pgeyV.length; pgeyI += 3) {
            double pgeyY = pgeyV[pgeyI + 1];
            if (pgeyY >= pgeyArmpit && pgeyY < pgeyTop) {
                int pgeyK = (int) ((pgeyY - pgeyArmpit) / pgeyStep);
                pgeyWidth[pgeyK] = Math.max(pgeyWidth[pgeyK], Math.abs(pgeyV[pgeyI]));
            }
        }
        double pgeyShoulders = 0;
        for (int pgeyK = 0; pgeyK < pgeyN && pgeyK * pgeyStep < 0.08 * pgeyH; pgeyK++) {
            pgeyShoulders = Math.max(pgeyShoulders, pgeyWidth[pgeyK]);
        }
        for (int pgeyK = 0; pgeyK < pgeyN; pgeyK++) {
            if (pgeyK * pgeyStep > 0.02 && pgeyWidth[pgeyK] > 0 && pgeyWidth[pgeyK] < 0.45 * pgeyShoulders) {
                return pgeyArmpit + pgeyK * pgeyStep;
            }
        }
        return pgeyArmpit + 0.1 * pgeyH;
    }

    private static void pgeyFillGaps(double[] pgeyT) {
        int pgeyFirst = -1;
        for (int pgeyI = 0; pgeyI < pgeyT.length; pgeyI++) {
            if (!Double.isNaN(pgeyT[pgeyI])) {
                pgeyFirst = pgeyI;
                break;
            }
        }
        if (pgeyFirst < 0) {
            Arrays.fill(pgeyT, 0);
            return;
        }
        for (int pgeyI = 0; pgeyI < pgeyFirst; pgeyI++) {
            pgeyT[pgeyI] = pgeyT[pgeyFirst];
        }
        for (int pgeyI = pgeyFirst + 1; pgeyI < pgeyT.length; pgeyI++) {
            if (Double.isNaN(pgeyT[pgeyI])) {
                pgeyT[pgeyI] = pgeyT[pgeyI - 1];
            }
        }
    }
}
