package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyVec3;

import java.util.Arrays;
import java.util.List;

final class PgeyTriangleIndex {

    private final double[] pgeyV;
    private final int[] pgeyTri;
    private final double[] pgeyFaceNormal;
    private final double pgeyCell;
    private final double pgeyMinX;
    private final double pgeyMinY;
    private final double pgeyMinZ;
    private final int pgeyNx;
    private final int pgeyNy;
    private final int pgeyNz;
    private final int[] pgeyCellStart;
    private final int[] pgeyCellTris;

    private PgeyTriangleIndex(double[] pgeyV, int[] pgeyTri, double pgeyCell) {
        this.pgeyV = pgeyV;
        this.pgeyTri = pgeyTri;
        this.pgeyCell = pgeyCell;
        int pgeyTriCount = pgeyTri.length / 3;
        pgeyFaceNormal = new double[pgeyTriCount * 3];
        double[] pgeyLo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] pgeyHi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (int pgeyI = 0; pgeyI < pgeyV.length; pgeyI += 3) {
            for (int pgeyK = 0; pgeyK < 3; pgeyK++) {
                pgeyLo[pgeyK] = Math.min(pgeyLo[pgeyK], pgeyV[pgeyI + pgeyK]);
                pgeyHi[pgeyK] = Math.max(pgeyHi[pgeyK], pgeyV[pgeyI + pgeyK]);
            }
        }
        pgeyMinX = pgeyLo[0] - pgeyCell;
        pgeyMinY = pgeyLo[1] - pgeyCell;
        pgeyMinZ = pgeyLo[2] - pgeyCell;
        pgeyNx = (int) Math.ceil((pgeyHi[0] - pgeyMinX) / pgeyCell) + 2;
        pgeyNy = (int) Math.ceil((pgeyHi[1] - pgeyMinY) / pgeyCell) + 2;
        pgeyNz = (int) Math.ceil((pgeyHi[2] - pgeyMinZ) / pgeyCell) + 2;

        int[] pgeyCount = new int[pgeyNx * pgeyNy * pgeyNz + 1];
        for (int pgeyPass = 0; pgeyPass < 2; pgeyPass++) {
            int[] pgeyFill = pgeyPass == 1 ? Arrays.copyOf(pgeyCount, pgeyCount.length) : null;
            for (int pgeyT = 0; pgeyT < pgeyTriCount; pgeyT++) {
                int pgeyA = pgeyTri[pgeyT * 3] * 3;
                int pgeyB = pgeyTri[pgeyT * 3 + 1] * 3;
                int pgeyC = pgeyTri[pgeyT * 3 + 2] * 3;
                if (pgeyPass == 0) {
                    pgeyComputeNormal(pgeyT, pgeyA, pgeyB, pgeyC);
                }
                int pgeyX0 = pgeyCx(Math.min(pgeyV[pgeyA], Math.min(pgeyV[pgeyB], pgeyV[pgeyC])));
                int pgeyX1 = pgeyCx(Math.max(pgeyV[pgeyA], Math.max(pgeyV[pgeyB], pgeyV[pgeyC])));
                int pgeyY0 = pgeyCy(Math.min(pgeyV[pgeyA + 1], Math.min(pgeyV[pgeyB + 1], pgeyV[pgeyC + 1])));
                int pgeyY1 = pgeyCy(Math.max(pgeyV[pgeyA + 1], Math.max(pgeyV[pgeyB + 1], pgeyV[pgeyC + 1])));
                int pgeyZ0 = pgeyCz(Math.min(pgeyV[pgeyA + 2], Math.min(pgeyV[pgeyB + 2], pgeyV[pgeyC + 2])));
                int pgeyZ1 = pgeyCz(Math.max(pgeyV[pgeyA + 2], Math.max(pgeyV[pgeyB + 2], pgeyV[pgeyC + 2])));
                for (int pgeyX = pgeyX0; pgeyX <= pgeyX1; pgeyX++) {
                    for (int pgeyY = pgeyY0; pgeyY <= pgeyY1; pgeyY++) {
                        for (int pgeyZ = pgeyZ0; pgeyZ <= pgeyZ1; pgeyZ++) {
                            int pgeyId = (pgeyX * pgeyNy + pgeyY) * pgeyNz + pgeyZ;
                            if (pgeyPass == 0) {
                                pgeyCount[pgeyId + 1]++;
                            } else {
                                pgeyCellTrisHolder[pgeyFill[pgeyId]++] = pgeyT;
                            }
                        }
                    }
                }
            }
            if (pgeyPass == 0) {
                for (int pgeyI = 1; pgeyI < pgeyCount.length; pgeyI++) {
                    pgeyCount[pgeyI] += pgeyCount[pgeyI - 1];
                }
                pgeyCellTrisHolder = new int[pgeyCount[pgeyCount.length - 1]];
            }
        }
        pgeyCellStart = pgeyCount;
        pgeyCellTris = pgeyCellTrisHolder;
        pgeyCellTrisHolder = null;
    }

    private int[] pgeyCellTrisHolder;

    static PgeyTriangleIndex pgeyOf(List<PgeyMeshData> pgeyMeshes, double pgeyCell) {
        int pgeyPoints = 0;
        int pgeyFaces = 0;
        for (PgeyMeshData pgeyM : pgeyMeshes) {
            pgeyPoints += pgeyM.pgeyPointCount();
            pgeyFaces += pgeyM.pgeyFaceCount();
        }
        double[] pgeyV = new double[pgeyPoints * 3];
        int[] pgeyTri = new int[pgeyFaces * 3];
        int pgeyPb = 0;
        int pgeyFb = 0;
        for (PgeyMeshData pgeyM : pgeyMeshes) {
            float[] pgeyP = pgeyM.pgeyPoints();
            for (int pgeyI = 0; pgeyI < pgeyP.length; pgeyI += 3) {
                pgeyV[pgeyPb * 3 + pgeyI] = pgeyP[pgeyI];
                pgeyV[pgeyPb * 3 + pgeyI + 1] = -pgeyP[pgeyI + 1];
                pgeyV[pgeyPb * 3 + pgeyI + 2] = -pgeyP[pgeyI + 2];
            }
            int[] pgeyF = pgeyM.pgeyFaces();
            for (int pgeyK = 0; pgeyK < pgeyM.pgeyFaceCount(); pgeyK++) {
                for (int pgeyC = 0; pgeyC < 3; pgeyC++) {
                    pgeyTri[(pgeyFb + pgeyK) * 3 + pgeyC] = pgeyPb + pgeyF[pgeyK * PgeyMeshData.PGEY_FACE_STRIDE + pgeyC * 3];
                }
            }
            pgeyPb += pgeyM.pgeyPointCount();
            pgeyFb += pgeyM.pgeyFaceCount();
        }
        return new PgeyTriangleIndex(pgeyV, pgeyTri, pgeyCell);
    }

    private void pgeyComputeNormal(int pgeyT, int pgeyA, int pgeyB, int pgeyC) {
        double pgeyUx = pgeyV[pgeyB] - pgeyV[pgeyA];
        double pgeyUy = pgeyV[pgeyB + 1] - pgeyV[pgeyA + 1];
        double pgeyUz = pgeyV[pgeyB + 2] - pgeyV[pgeyA + 2];
        double pgeyWx = pgeyV[pgeyC] - pgeyV[pgeyA];
        double pgeyWy = pgeyV[pgeyC + 1] - pgeyV[pgeyA + 1];
        double pgeyWz = pgeyV[pgeyC + 2] - pgeyV[pgeyA + 2];
        double pgeyX = pgeyUy * pgeyWz - pgeyUz * pgeyWy;
        double pgeyY = pgeyUz * pgeyWx - pgeyUx * pgeyWz;
        double pgeyZ = pgeyUx * pgeyWy - pgeyUy * pgeyWx;
        double pgeyLen = Math.sqrt(pgeyX * pgeyX + pgeyY * pgeyY + pgeyZ * pgeyZ);
        if (pgeyLen > 0) {
            pgeyFaceNormal[pgeyT * 3] = pgeyX / pgeyLen;
            pgeyFaceNormal[pgeyT * 3 + 1] = pgeyY / pgeyLen;
            pgeyFaceNormal[pgeyT * 3 + 2] = pgeyZ / pgeyLen;
        }
    }

    private int pgeyCx(double pgeyX) {
        return Math.max(0, Math.min(pgeyNx - 1, (int) ((pgeyX - pgeyMinX) / pgeyCell)));
    }

    private int pgeyCy(double pgeyY) {
        return Math.max(0, Math.min(pgeyNy - 1, (int) ((pgeyY - pgeyMinY) / pgeyCell)));
    }

    private int pgeyCz(double pgeyZ) {
        return Math.max(0, Math.min(pgeyNz - 1, (int) ((pgeyZ - pgeyMinZ) / pgeyCell)));
    }

    double[] pgeyVertices() {
        return pgeyV;
    }

    int[] pgeyTriangles() {
        return pgeyTri;
    }

    record PgeyHit(PgeyVec3 pgeyPoint, PgeyVec3 pgeyNormal, double pgeyDistance) {
    }

    PgeyQuery pgeyQuery() {
        return new PgeyQuery();
    }

    final class PgeyQuery {
        private final int[] pgeyMark = new int[pgeyTri.length / 3];
        private int pgeyStamp;

        double pgeyRay(PgeyVec3 pgeyOrigin, PgeyVec3 pgeyDir, double pgeyMaxDist) {
            pgeyNext();
            double pgeyBest = Double.NaN;
            int pgeyLastCell = -1;
            double pgeyStep = pgeyCell * 0.5;
            for (double pgeyS = 0; pgeyS <= pgeyMaxDist + pgeyStep; pgeyS += pgeyStep) {
                if (!Double.isNaN(pgeyBest) && pgeyS - pgeyStep > pgeyBest) {
                    break;
                }
                int pgeyId = pgeyCellId(pgeyOrigin.pgeyX() + pgeyDir.pgeyX() * pgeyS, pgeyOrigin.pgeyY() + pgeyDir.pgeyY() * pgeyS, pgeyOrigin.pgeyZ() + pgeyDir.pgeyZ() * pgeyS);
                if (pgeyId == pgeyLastCell) {
                    continue;
                }
                pgeyLastCell = pgeyId;
                for (int pgeyN : pgeyNeighbors(pgeyId)) {
                    for (int pgeyK = pgeyCellStart[pgeyN]; pgeyK < pgeyCellStart[pgeyN + 1]; pgeyK++) {
                        int pgeyT = pgeyCellTris[pgeyK];
                        if (pgeyMark[pgeyT] == pgeyStamp) {
                            continue;
                        }
                        pgeyMark[pgeyT] = pgeyStamp;
                        double pgeyD = pgeyIntersect(pgeyT, pgeyOrigin, pgeyDir);
                        if (pgeyD > 1e-7 && pgeyD <= pgeyMaxDist && (Double.isNaN(pgeyBest) || pgeyD < pgeyBest)) {
                            pgeyBest = pgeyD;
                        }
                    }
                }
            }
            return pgeyBest;
        }

        PgeyHit pgeyNearest(PgeyVec3 pgeyP, double pgeyRadius) {
            pgeyNext();
            int pgeyX0 = pgeyCx(pgeyP.pgeyX() - pgeyRadius);
            int pgeyX1 = pgeyCx(pgeyP.pgeyX() + pgeyRadius);
            int pgeyY0 = pgeyCy(pgeyP.pgeyY() - pgeyRadius);
            int pgeyY1 = pgeyCy(pgeyP.pgeyY() + pgeyRadius);
            int pgeyZ0 = pgeyCz(pgeyP.pgeyZ() - pgeyRadius);
            int pgeyZ1 = pgeyCz(pgeyP.pgeyZ() + pgeyRadius);
            double pgeyBest = pgeyRadius * pgeyRadius;
            double[] pgeyQ = new double[3];
            double[] pgeyBestQ = null;
            int pgeyBestT = -1;
            for (int pgeyX = pgeyX0; pgeyX <= pgeyX1; pgeyX++) {
                for (int pgeyY = pgeyY0; pgeyY <= pgeyY1; pgeyY++) {
                    for (int pgeyZ = pgeyZ0; pgeyZ <= pgeyZ1; pgeyZ++) {
                        int pgeyId = (pgeyX * pgeyNy + pgeyY) * pgeyNz + pgeyZ;
                        for (int pgeyK = pgeyCellStart[pgeyId]; pgeyK < pgeyCellStart[pgeyId + 1]; pgeyK++) {
                            int pgeyT = pgeyCellTris[pgeyK];
                            if (pgeyMark[pgeyT] == pgeyStamp) {
                                continue;
                            }
                            pgeyMark[pgeyT] = pgeyStamp;
                            pgeyClosest(pgeyT, pgeyP, pgeyQ);
                            double pgeyDx = pgeyQ[0] - pgeyP.pgeyX();
                            double pgeyDy = pgeyQ[1] - pgeyP.pgeyY();
                            double pgeyDz = pgeyQ[2] - pgeyP.pgeyZ();
                            double pgeyD2 = pgeyDx * pgeyDx + pgeyDy * pgeyDy + pgeyDz * pgeyDz;
                            if (pgeyD2 < pgeyBest) {
                                pgeyBest = pgeyD2;
                                pgeyBestQ = pgeyQ.clone();
                                pgeyBestT = pgeyT;
                            }
                        }
                    }
                }
            }
            if (pgeyBestT < 0) {
                return null;
            }
            return new PgeyHit(new PgeyVec3(pgeyBestQ[0], pgeyBestQ[1], pgeyBestQ[2]),
                    new PgeyVec3(pgeyFaceNormal[pgeyBestT * 3], pgeyFaceNormal[pgeyBestT * 3 + 1], pgeyFaceNormal[pgeyBestT * 3 + 2]),
                    Math.sqrt(pgeyBest));
        }

        private void pgeyNext() {
            pgeyStamp++;
            if (pgeyStamp == Integer.MAX_VALUE) {
                Arrays.fill(pgeyMark, 0);
                pgeyStamp = 1;
            }
        }

        private final int[] pgeyNeighborBuf = new int[27];
        private int pgeyNeighborCount;

        private int[] pgeyNeighbors(int pgeyId) {
            int pgeyZ = pgeyId % pgeyNz;
            int pgeyY = (pgeyId / pgeyNz) % pgeyNy;
            int pgeyX = pgeyId / (pgeyNz * pgeyNy);
            pgeyNeighborCount = 0;
            for (int pgeyDx = -1; pgeyDx <= 1; pgeyDx++) {
                for (int pgeyDy = -1; pgeyDy <= 1; pgeyDy++) {
                    for (int pgeyDz = -1; pgeyDz <= 1; pgeyDz++) {
                        int pgeyXx = pgeyX + pgeyDx;
                        int pgeyYy = pgeyY + pgeyDy;
                        int pgeyZz = pgeyZ + pgeyDz;
                        if (pgeyXx >= 0 && pgeyXx < pgeyNx && pgeyYy >= 0 && pgeyYy < pgeyNy && pgeyZz >= 0 && pgeyZz < pgeyNz) {
                            pgeyNeighborBuf[pgeyNeighborCount++] = (pgeyXx * pgeyNy + pgeyYy) * pgeyNz + pgeyZz;
                        }
                    }
                }
            }
            return pgeyNeighborCount == 27 ? pgeyNeighborBuf : Arrays.copyOf(pgeyNeighborBuf, pgeyNeighborCount);
        }
    }

    private int pgeyCellId(double pgeyX, double pgeyY, double pgeyZ) {
        return (pgeyCx(pgeyX) * pgeyNy + pgeyCy(pgeyY)) * pgeyNz + pgeyCz(pgeyZ);
    }

    private double pgeyIntersect(int pgeyT, PgeyVec3 pgeyO, PgeyVec3 pgeyD) {
        int pgeyA = pgeyTri[pgeyT * 3] * 3;
        int pgeyB = pgeyTri[pgeyT * 3 + 1] * 3;
        int pgeyC = pgeyTri[pgeyT * 3 + 2] * 3;
        double pgeyE1x = pgeyV[pgeyB] - pgeyV[pgeyA];
        double pgeyE1y = pgeyV[pgeyB + 1] - pgeyV[pgeyA + 1];
        double pgeyE1z = pgeyV[pgeyB + 2] - pgeyV[pgeyA + 2];
        double pgeyE2x = pgeyV[pgeyC] - pgeyV[pgeyA];
        double pgeyE2y = pgeyV[pgeyC + 1] - pgeyV[pgeyA + 1];
        double pgeyE2z = pgeyV[pgeyC + 2] - pgeyV[pgeyA + 2];
        double pgeyPx = pgeyD.pgeyY() * pgeyE2z - pgeyD.pgeyZ() * pgeyE2y;
        double pgeyPy = pgeyD.pgeyZ() * pgeyE2x - pgeyD.pgeyX() * pgeyE2z;
        double pgeyPz = pgeyD.pgeyX() * pgeyE2y - pgeyD.pgeyY() * pgeyE2x;
        double pgeyDet = pgeyE1x * pgeyPx + pgeyE1y * pgeyPy + pgeyE1z * pgeyPz;
        if (Math.abs(pgeyDet) < 1e-14) {
            return -1;
        }
        double pgeyInv = 1 / pgeyDet;
        double pgeyTx = pgeyO.pgeyX() - pgeyV[pgeyA];
        double pgeyTy = pgeyO.pgeyY() - pgeyV[pgeyA + 1];
        double pgeyTz = pgeyO.pgeyZ() - pgeyV[pgeyA + 2];
        double pgeyU = (pgeyTx * pgeyPx + pgeyTy * pgeyPy + pgeyTz * pgeyPz) * pgeyInv;
        if (pgeyU < 0 || pgeyU > 1) {
            return -1;
        }
        double pgeyQx = pgeyTy * pgeyE1z - pgeyTz * pgeyE1y;
        double pgeyQy = pgeyTz * pgeyE1x - pgeyTx * pgeyE1z;
        double pgeyQz = pgeyTx * pgeyE1y - pgeyTy * pgeyE1x;
        double pgeyW = (pgeyD.pgeyX() * pgeyQx + pgeyD.pgeyY() * pgeyQy + pgeyD.pgeyZ() * pgeyQz) * pgeyInv;
        if (pgeyW < 0 || pgeyU + pgeyW > 1) {
            return -1;
        }
        return (pgeyE2x * pgeyQx + pgeyE2y * pgeyQy + pgeyE2z * pgeyQz) * pgeyInv;
    }

    private void pgeyClosest(int pgeyT, PgeyVec3 pgeyP, double[] pgeyOut) {
        int pgeyIa = pgeyTri[pgeyT * 3] * 3;
        int pgeyIb = pgeyTri[pgeyT * 3 + 1] * 3;
        int pgeyIc = pgeyTri[pgeyT * 3 + 2] * 3;
        double pgeyAx = pgeyV[pgeyIa], pgeyAy = pgeyV[pgeyIa + 1], pgeyAz = pgeyV[pgeyIa + 2];
        double pgeyBx = pgeyV[pgeyIb], pgeyBy = pgeyV[pgeyIb + 1], pgeyBz = pgeyV[pgeyIb + 2];
        double pgeyCx = pgeyV[pgeyIc], pgeyCy = pgeyV[pgeyIc + 1], pgeyCz = pgeyV[pgeyIc + 2];
        double pgeyAbx = pgeyBx - pgeyAx, pgeyAby = pgeyBy - pgeyAy, pgeyAbz = pgeyBz - pgeyAz;
        double pgeyAcx = pgeyCx - pgeyAx, pgeyAcy = pgeyCy - pgeyAy, pgeyAcz = pgeyCz - pgeyAz;
        double pgeyApx = pgeyP.pgeyX() - pgeyAx, pgeyApy = pgeyP.pgeyY() - pgeyAy, pgeyApz = pgeyP.pgeyZ() - pgeyAz;
        double pgeyD1 = pgeyAbx * pgeyApx + pgeyAby * pgeyApy + pgeyAbz * pgeyApz;
        double pgeyD2 = pgeyAcx * pgeyApx + pgeyAcy * pgeyApy + pgeyAcz * pgeyApz;
        if (pgeyD1 <= 0 && pgeyD2 <= 0) {
            pgeySet(pgeyOut, pgeyAx, pgeyAy, pgeyAz);
            return;
        }
        double pgeyBpx = pgeyP.pgeyX() - pgeyBx, pgeyBpy = pgeyP.pgeyY() - pgeyBy, pgeyBpz = pgeyP.pgeyZ() - pgeyBz;
        double pgeyD3 = pgeyAbx * pgeyBpx + pgeyAby * pgeyBpy + pgeyAbz * pgeyBpz;
        double pgeyD4 = pgeyAcx * pgeyBpx + pgeyAcy * pgeyBpy + pgeyAcz * pgeyBpz;
        if (pgeyD3 >= 0 && pgeyD4 <= pgeyD3) {
            pgeySet(pgeyOut, pgeyBx, pgeyBy, pgeyBz);
            return;
        }
        double pgeyVc = pgeyD1 * pgeyD4 - pgeyD3 * pgeyD2;
        if (pgeyVc <= 0 && pgeyD1 >= 0 && pgeyD3 <= 0) {
            double pgeyK = pgeyD1 / (pgeyD1 - pgeyD3);
            pgeySet(pgeyOut, pgeyAx + pgeyK * pgeyAbx, pgeyAy + pgeyK * pgeyAby, pgeyAz + pgeyK * pgeyAbz);
            return;
        }
        double pgeyCpx = pgeyP.pgeyX() - pgeyCx, pgeyCpy = pgeyP.pgeyY() - pgeyCy, pgeyCpz = pgeyP.pgeyZ() - pgeyCz;
        double pgeyD5 = pgeyAbx * pgeyCpx + pgeyAby * pgeyCpy + pgeyAbz * pgeyCpz;
        double pgeyD6 = pgeyAcx * pgeyCpx + pgeyAcy * pgeyCpy + pgeyAcz * pgeyCpz;
        if (pgeyD6 >= 0 && pgeyD5 <= pgeyD6) {
            pgeySet(pgeyOut, pgeyCx, pgeyCy, pgeyCz);
            return;
        }
        double pgeyVb = pgeyD5 * pgeyD2 - pgeyD1 * pgeyD6;
        if (pgeyVb <= 0 && pgeyD2 >= 0 && pgeyD6 <= 0) {
            double pgeyK = pgeyD2 / (pgeyD2 - pgeyD6);
            pgeySet(pgeyOut, pgeyAx + pgeyK * pgeyAcx, pgeyAy + pgeyK * pgeyAcy, pgeyAz + pgeyK * pgeyAcz);
            return;
        }
        double pgeyVa = pgeyD3 * pgeyD6 - pgeyD5 * pgeyD4;
        if (pgeyVa <= 0 && (pgeyD4 - pgeyD3) >= 0 && (pgeyD5 - pgeyD6) >= 0) {
            double pgeyK = (pgeyD4 - pgeyD3) / ((pgeyD4 - pgeyD3) + (pgeyD5 - pgeyD6));
            pgeySet(pgeyOut, pgeyBx + pgeyK * (pgeyCx - pgeyBx), pgeyBy + pgeyK * (pgeyCy - pgeyBy), pgeyBz + pgeyK * (pgeyCz - pgeyBz));
            return;
        }
        double pgeyDenom = 1 / (pgeyVa + pgeyVb + pgeyVc);
        double pgeyVv = pgeyVb * pgeyDenom;
        double pgeyWw = pgeyVc * pgeyDenom;
        pgeySet(pgeyOut, pgeyAx + pgeyAbx * pgeyVv + pgeyAcx * pgeyWw, pgeyAy + pgeyAby * pgeyVv + pgeyAcy * pgeyWw, pgeyAz + pgeyAbz * pgeyVv + pgeyAcz * pgeyWw);
    }

    private static void pgeySet(double[] pgeyOut, double pgeyX, double pgeyY, double pgeyZ) {
        pgeyOut[0] = pgeyX;
        pgeyOut[1] = pgeyY;
        pgeyOut[2] = pgeyZ;
    }
}
