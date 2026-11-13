package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.PgeyMeshData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

final class PgeyClothOps {

    private PgeyClothOps() {
    }

    static int[][] pgeyNeighbors(PgeyMeshData pgeyMesh, int[] pgeyCanon, int pgeyM) {
        List<java.util.Set<Integer>> pgeySets = new ArrayList<>(pgeyM);
        for (int pgeyI = 0; pgeyI < pgeyM; pgeyI++) {
            pgeySets.add(new java.util.HashSet<>(8));
        }
        int[] pgeyF = pgeyMesh.pgeyFaces();
        for (int pgeyT = 0; pgeyT < pgeyMesh.pgeyFaceCount(); pgeyT++) {
            int pgeyA = pgeyCanon[pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE]];
            int pgeyB = pgeyCanon[pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + 3]];
            int pgeyC = pgeyCanon[pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + 6]];
            pgeySets.get(pgeyA).add(pgeyB);
            pgeySets.get(pgeyA).add(pgeyC);
            pgeySets.get(pgeyB).add(pgeyA);
            pgeySets.get(pgeyB).add(pgeyC);
            pgeySets.get(pgeyC).add(pgeyA);
            pgeySets.get(pgeyC).add(pgeyB);
        }
        int[][] pgeyOut = new int[pgeyM][];
        for (int pgeyI = 0; pgeyI < pgeyM; pgeyI++) {
            pgeyOut[pgeyI] = pgeySets.get(pgeyI).stream().mapToInt(Integer::intValue).toArray();
        }
        return pgeyOut;
    }

    static boolean[] pgeyBoundary(PgeyMeshData pgeyMesh, int[] pgeyCanon, int pgeyM) {
        Map<Long, Integer> pgeyCount = new HashMap<>();
        int[] pgeyF = pgeyMesh.pgeyFaces();
        for (int pgeyT = 0; pgeyT < pgeyMesh.pgeyFaceCount(); pgeyT++) {
            for (int pgeyE = 0; pgeyE < 3; pgeyE++) {
                int pgeyA = pgeyCanon[pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + pgeyE * 3]];
                int pgeyB = pgeyCanon[pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + ((pgeyE + 1) % 3) * 3]];
                pgeyCount.merge(pgeyKey(pgeyA, pgeyB), 1, Integer::sum);
            }
        }
        boolean[] pgeyOut = new boolean[pgeyM];
        pgeyCount.forEach((pgeyK, pgeyN) -> {
            if (pgeyN == 1) {
                pgeyOut[(int) (pgeyK >> 32)] = true;
                pgeyOut[(int) (pgeyK & 0xFFFFFFFFL)] = true;
            }
        });
        return pgeyOut;
    }

    private static long pgeyKey(int pgeyA, int pgeyB) {
        int pgeyLo = Math.min(pgeyA, pgeyB);
        int pgeyHi = Math.max(pgeyA, pgeyB);
        return ((long) pgeyLo << 32) | (pgeyHi & 0xFFFFFFFFL);
    }

    static void pgeyTaubin(double[] pgeyPos, int[][] pgeyNb, boolean[] pgeyFixed, boolean[] pgeySide, int pgeyIterations) {
        int pgeyM = pgeyNb.length;
        double[] pgeyNext = new double[pgeyPos.length];
        for (int pgeyIt = 0; pgeyIt < pgeyIterations * 2; pgeyIt++) {
            double pgeyK = pgeyIt % 2 == 0 ? 0.5 : -0.53;
            for (int pgeyI = 0; pgeyI < pgeyM; pgeyI++) {
                double pgeySx = 0;
                double pgeySy = 0;
                double pgeySz = 0;
                int pgeyN = 0;
                for (int pgeyJ : pgeyNb[pgeyI]) {
                    if (pgeySide[pgeyJ] == pgeySide[pgeyI] && (!pgeyFixed[pgeyI] || pgeyFixed[pgeyJ])) {
                        pgeySx += pgeyPos[pgeyJ * 3];
                        pgeySy += pgeyPos[pgeyJ * 3 + 1];
                        pgeySz += pgeyPos[pgeyJ * 3 + 2];
                        pgeyN++;
                    }
                }
                if (pgeyN == 0) {
                    pgeyNext[pgeyI * 3] = pgeyPos[pgeyI * 3];
                    pgeyNext[pgeyI * 3 + 1] = pgeyPos[pgeyI * 3 + 1];
                    pgeyNext[pgeyI * 3 + 2] = pgeyPos[pgeyI * 3 + 2];
                } else {
                    pgeyNext[pgeyI * 3] = pgeyPos[pgeyI * 3] + pgeyK * (pgeySx / pgeyN - pgeyPos[pgeyI * 3]);
                    pgeyNext[pgeyI * 3 + 1] = pgeyPos[pgeyI * 3 + 1] + pgeyK * (pgeySy / pgeyN - pgeyPos[pgeyI * 3 + 1]);
                    pgeyNext[pgeyI * 3 + 2] = pgeyPos[pgeyI * 3 + 2] + pgeyK * (pgeySz / pgeyN - pgeyPos[pgeyI * 3 + 2]);
                }
            }
            System.arraycopy(pgeyNext, 0, pgeyPos, 0, pgeyPos.length);
        }
    }

    static double[] pgeyFollow(double[] pgeyBaseSrc, double[] pgeyBaseDst, double[] pgeyPts) {
        double pgeyCell = 0.02;
        Map<Long, List<Integer>> pgeyGrid = new HashMap<>();
        int pgeyN = pgeyBaseSrc.length / 3;
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            pgeyGrid.computeIfAbsent(pgeyCellKey(pgeyBaseSrc[pgeyI * 3] / pgeyCell, pgeyBaseSrc[pgeyI * 3 + 1] / pgeyCell, pgeyBaseSrc[pgeyI * 3 + 2] / pgeyCell),
                    pgeyK -> new ArrayList<>()).add(pgeyI);
        }
        double[] pgeyOut = new double[pgeyPts.length];
        int[] pgeyBest = new int[12];
        double[] pgeyBestD = new double[12];
        for (int pgeyP = 0; pgeyP < pgeyPts.length / 3; pgeyP++) {
            double pgeyX = pgeyPts[pgeyP * 3];
            double pgeyY = pgeyPts[pgeyP * 3 + 1];
            double pgeyZ = pgeyPts[pgeyP * 3 + 2];
            Arrays.fill(pgeyBestD, Double.MAX_VALUE);
            Arrays.fill(pgeyBest, -1);
            for (int pgeyR = 1; pgeyR <= 4 && pgeyBest[pgeyBest.length - 1] < 0; pgeyR++) {
                int pgeyCx = (int) Math.floor(pgeyX / pgeyCell);
                int pgeyCy = (int) Math.floor(pgeyY / pgeyCell);
                int pgeyCz = (int) Math.floor(pgeyZ / pgeyCell);
                for (int pgeyDx = -pgeyR; pgeyDx <= pgeyR; pgeyDx++) {
                    for (int pgeyDy = -pgeyR; pgeyDy <= pgeyR; pgeyDy++) {
                        for (int pgeyDz = -pgeyR; pgeyDz <= pgeyR; pgeyDz++) {
                            if (Math.max(Math.abs(pgeyDx), Math.max(Math.abs(pgeyDy), Math.abs(pgeyDz))) != pgeyR && pgeyR > 1) {
                                continue;
                            }
                            List<Integer> pgeyList = pgeyGrid.get(pgeyCellKey(pgeyCx + pgeyDx, pgeyCy + pgeyDy, pgeyCz + pgeyDz));
                            if (pgeyList == null) {
                                continue;
                            }
                            for (int pgeyI : pgeyList) {
                                double pgeyEx = pgeyBaseSrc[pgeyI * 3] - pgeyX;
                                double pgeyEy = pgeyBaseSrc[pgeyI * 3 + 1] - pgeyY;
                                double pgeyEz = pgeyBaseSrc[pgeyI * 3 + 2] - pgeyZ;
                                pgeyInsert(pgeyBest, pgeyBestD, pgeyI, pgeyEx * pgeyEx + pgeyEy * pgeyEy + pgeyEz * pgeyEz);
                            }
                        }
                    }
                }
            }
            double pgeyWx = 0;
            double pgeyWy = 0;
            double pgeyWz = 0;
            double pgeyWs = 0;
            for (int pgeyK = 0; pgeyK < pgeyBest.length; pgeyK++) {
                if (pgeyBest[pgeyK] < 0) {
                    continue;
                }
                double pgeyW = 1 / (pgeyBestD[pgeyK] + 1e-8);
                int pgeyI = pgeyBest[pgeyK];
                pgeyWx += pgeyW * (pgeyBaseDst[pgeyI * 3] - pgeyBaseSrc[pgeyI * 3]);
                pgeyWy += pgeyW * (pgeyBaseDst[pgeyI * 3 + 1] - pgeyBaseSrc[pgeyI * 3 + 1]);
                pgeyWz += pgeyW * (pgeyBaseDst[pgeyI * 3 + 2] - pgeyBaseSrc[pgeyI * 3 + 2]);
                pgeyWs += pgeyW;
            }
            pgeyOut[pgeyP * 3] = pgeyX + (pgeyWs > 0 ? pgeyWx / pgeyWs : 0);
            pgeyOut[pgeyP * 3 + 1] = pgeyY + (pgeyWs > 0 ? pgeyWy / pgeyWs : 0);
            pgeyOut[pgeyP * 3 + 2] = pgeyZ + (pgeyWs > 0 ? pgeyWz / pgeyWs : 0);
        }
        return pgeyOut;
    }

    static float[] pgeyFollowEdge(PgeyMeshData pgeySrc, PgeyMeshData pgeyDst, IntPredicate pgeyEdge, float[] pgeyPts, double pgeyThickness,
                              double pgeyHeight, double pgeyLower) {
        int pgeyN = pgeySrc.pgeyPointCount();
        int[] pgeyF = pgeySrc.pgeyFaces();
        Map<Long, Integer> pgeyUse = new HashMap<>();
        int[] pgeyNormalOf = new int[pgeyN];
        List<List<Integer>> pgeyAdj = new ArrayList<>(pgeyN);
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            pgeyAdj.add(new ArrayList<>(6));
        }
        for (int pgeyT = 0; pgeyT < pgeySrc.pgeyFaceCount(); pgeyT++) {
            for (int pgeyE = 0; pgeyE < 3; pgeyE++) {
                int pgeyA = pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + pgeyE * 3];
                int pgeyB = pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + ((pgeyE + 1) % 3) * 3];
                pgeyNormalOf[pgeyA] = pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + pgeyE * 3 + 1];
                pgeyAdj.get(pgeyA).add(pgeyB);
                pgeyAdj.get(pgeyB).add(pgeyA);
                pgeyUse.merge(pgeyKey(pgeyA, pgeyB), 1, Integer::sum);
            }
        }
        boolean[] pgeyOnEdge = new boolean[pgeyN];
        pgeyUse.forEach((pgeyK, pgeyC) -> {
            if (pgeyC == 1) {
                int pgeyA = (int) (pgeyK >> 32);
                int pgeyB = (int) (pgeyK & 0xFFFFFFFFL);
                if (pgeyEdge.test(pgeyA) && pgeyEdge.test(pgeyB)) {
                    pgeyOnEdge[pgeyA] = true;
                    pgeyOnEdge[pgeyB] = true;
                }
            }
        });
        List<Integer> pgeyIds = new ArrayList<>();
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            if (pgeyOnEdge[pgeyI]) {
                pgeyIds.add(pgeyI);
            }
        }
        double[][] pgeyFrameSrc = new double[pgeyIds.size()][];
        double[][] pgeyFrameDst = new double[pgeyIds.size()][];
        for (int pgeyK = 0; pgeyK < pgeyIds.size(); pgeyK++) {
            pgeyFrameSrc[pgeyK] = pgeyFrame(pgeySrc, pgeyIds.get(pgeyK), pgeyNormalOf, pgeyAdj, pgeyOnEdge);
            pgeyFrameDst[pgeyK] = pgeyFrame(pgeyDst, pgeyIds.get(pgeyK), pgeyNormalOf, pgeyAdj, pgeyOnEdge);
        }
        float[] pgeyOut = new float[pgeyPts.length];
        for (int pgeyP = 0; pgeyP < pgeyPts.length; pgeyP += 3) {
            int pgeyB0 = -1;
            int pgeyB1 = -1;
            double pgeyD0 = Double.MAX_VALUE;
            double pgeyD1 = Double.MAX_VALUE;
            for (int pgeyK = 0; pgeyK < pgeyIds.size(); pgeyK++) {
                double[] pgeyFr = pgeyFrameSrc[pgeyK];
                double pgeyDx = pgeyPts[pgeyP] - pgeyFr[0];
                double pgeyDy = pgeyPts[pgeyP + 1] - pgeyFr[1];
                double pgeyDz = pgeyPts[pgeyP + 2] - pgeyFr[2];
                double pgeyD = pgeyDx * pgeyDx + pgeyDy * pgeyDy + pgeyDz * pgeyDz;
                if (pgeyD < pgeyD0) {
                    pgeyD1 = pgeyD0;
                    pgeyB1 = pgeyB0;
                    pgeyD0 = pgeyD;
                    pgeyB0 = pgeyK;
                } else if (pgeyD < pgeyD1) {
                    pgeyD1 = pgeyD;
                    pgeyB1 = pgeyK;
                }
            }
            double[] pgeyQ0 = pgeyCarry(pgeyPts, pgeyP, pgeyFrameSrc[pgeyB0], pgeyFrameDst[pgeyB0], pgeyThickness, pgeyHeight, pgeyLower);
            if (pgeyB1 < 0) {
                pgeyOut[pgeyP] = (float) pgeyQ0[0];
                pgeyOut[pgeyP + 1] = (float) pgeyQ0[1];
                pgeyOut[pgeyP + 2] = (float) pgeyQ0[2];
                continue;
            }
            double[] pgeyQ1 = pgeyCarry(pgeyPts, pgeyP, pgeyFrameSrc[pgeyB1], pgeyFrameDst[pgeyB1], pgeyThickness, pgeyHeight, pgeyLower);
            double pgeyW0 = 1 / (Math.sqrt(pgeyD0) + 1e-6);
            double pgeyW1 = 1 / (Math.sqrt(pgeyD1) + 1e-6);
            for (int pgeyK = 0; pgeyK < 3; pgeyK++) {
                pgeyOut[pgeyP + pgeyK] = (float) ((pgeyQ0[pgeyK] * pgeyW0 + pgeyQ1[pgeyK] * pgeyW1) / (pgeyW0 + pgeyW1));
            }
        }
        return pgeyOut;
    }

    private static double[] pgeyFrame(PgeyMeshData pgeyM, int pgeyI, int[] pgeyNormalOf, List<List<Integer>> pgeyAdj, boolean[] pgeyOnEdge) {
        float[] pgeyP = pgeyM.pgeyPoints();
        float[] pgeyNr = pgeyM.pgeyNormals();
        int pgeyNi = pgeyNormalOf[pgeyI] * 3;
        double pgeyNx = pgeyNr[pgeyNi];
        double pgeyNy = pgeyNr[pgeyNi + 1];
        double pgeyNz = pgeyNr[pgeyNi + 2];
        double pgeyAx = 0;
        double pgeyAy = 0;
        double pgeyAz = 0;
        int pgeyC = 0;
        for (int pgeyJ : pgeyAdj.get(pgeyI)) {
            if (!pgeyOnEdge[pgeyJ]) {
                pgeyAx += pgeyP[pgeyJ * 3];
                pgeyAy += pgeyP[pgeyJ * 3 + 1];
                pgeyAz += pgeyP[pgeyJ * 3 + 2];
                pgeyC++;
            }
        }
        double pgeyUx = pgeyC == 0 ? 0 : pgeyP[pgeyI * 3] - pgeyAx / pgeyC;
        double pgeyUy = pgeyC == 0 ? -1 : pgeyP[pgeyI * 3 + 1] - pgeyAy / pgeyC;
        double pgeyUz = pgeyC == 0 ? 0 : pgeyP[pgeyI * 3 + 2] - pgeyAz / pgeyC;
        double pgeyDot = pgeyUx * pgeyNx + pgeyUy * pgeyNy + pgeyUz * pgeyNz;
        pgeyUx -= pgeyDot * pgeyNx;
        pgeyUy -= pgeyDot * pgeyNy;
        pgeyUz -= pgeyDot * pgeyNz;
        double pgeyUl = Math.sqrt(pgeyUx * pgeyUx + pgeyUy * pgeyUy + pgeyUz * pgeyUz);
        if (pgeyUl < 1e-12) {
            pgeyUx = 0;
            pgeyUy = -1;
            pgeyUz = 0;
            pgeyUl = 1;
        }
        pgeyUx /= pgeyUl;
        pgeyUy /= pgeyUl;
        pgeyUz /= pgeyUl;
        double pgeyTx = pgeyNy * pgeyUz - pgeyNz * pgeyUy;
        double pgeyTy = pgeyNz * pgeyUx - pgeyNx * pgeyUz;
        double pgeyTz = pgeyNx * pgeyUy - pgeyNy * pgeyUx;
        return new double[]{pgeyP[pgeyI * 3], pgeyP[pgeyI * 3 + 1], pgeyP[pgeyI * 3 + 2], pgeyNx, pgeyNy, pgeyNz, pgeyUx, pgeyUy, pgeyUz, pgeyTx, pgeyTy, pgeyTz};
    }

    private static double[] pgeyCarry(float[] pgeyPts, int pgeyP, double[] pgeyA, double[] pgeyB, double pgeyThickness, double pgeyHeight,
                                  double pgeyLower) {
        double pgeyDx = pgeyPts[pgeyP] - pgeyA[0];
        double pgeyDy = pgeyPts[pgeyP + 1] - pgeyA[1];
        double pgeyDz = pgeyPts[pgeyP + 2] - pgeyA[2];
        double pgeyOn = (pgeyDx * pgeyA[3] + pgeyDy * pgeyA[4] + pgeyDz * pgeyA[5]) * pgeyThickness;
        double pgeyOu = (pgeyDx * pgeyA[6] + pgeyDy * pgeyA[7] + pgeyDz * pgeyA[8]) * pgeyHeight - pgeyLower;
        double pgeyOt = pgeyDx * pgeyA[9] + pgeyDy * pgeyA[10] + pgeyDz * pgeyA[11];
        return new double[]{pgeyB[0] + pgeyB[3] * pgeyOn + pgeyB[6] * pgeyOu + pgeyB[9] * pgeyOt, pgeyB[1] + pgeyB[4] * pgeyOn + pgeyB[7] * pgeyOu + pgeyB[10] * pgeyOt,
                pgeyB[2] + pgeyB[5] * pgeyOn + pgeyB[8] * pgeyOu + pgeyB[11] * pgeyOt};
    }

    private static void pgeyInsert(int[] pgeyBest, double[] pgeyBestD, int pgeyI, double pgeyD) {
        int pgeyK = pgeyBest.length - 1;
        if (pgeyD >= pgeyBestD[pgeyK]) {
            return;
        }
        while (pgeyK > 0 && pgeyBestD[pgeyK - 1] > pgeyD) {
            pgeyBestD[pgeyK] = pgeyBestD[pgeyK - 1];
            pgeyBest[pgeyK] = pgeyBest[pgeyK - 1];
            pgeyK--;
        }
        pgeyBestD[pgeyK] = pgeyD;
        pgeyBest[pgeyK] = pgeyI;
    }

    private static long pgeyCellKey(double pgeyX, double pgeyY, double pgeyZ) {
        return pgeyCellKey((int) Math.floor(pgeyX), (int) Math.floor(pgeyY), (int) Math.floor(pgeyZ));
    }

    private static long pgeyCellKey(int pgeyX, int pgeyY, int pgeyZ) {
        return ((long) (pgeyX & 0x1FFFFF) << 42) | ((long) (pgeyY & 0x1FFFFF) << 21) | (pgeyZ & 0x1FFFFF);
    }

    static PgeyMeshData pgeyHemFold(PgeyMeshData pgeyMesh, IntPredicate pgeyHem, double pgeyThickness, double pgeyDepth) {
        float[] pgeyP = pgeyMesh.pgeyPoints();
        float[] pgeyNrm = pgeyMesh.pgeyNormals();
        float[] pgeyTex = pgeyMesh.pgeyTexCoords();
        int[] pgeyF = pgeyMesh.pgeyFaces();
        int pgeyN = pgeyMesh.pgeyPointCount();
        Map<Long, int[]> pgeyEdgeUse = new HashMap<>();
        int[] pgeyTexOf = new int[pgeyN];
        int[] pgeyNormalOf = new int[pgeyN];
        Arrays.fill(pgeyTexOf, -1);
        List<List<Integer>> pgeyAdj = new ArrayList<>(pgeyN);
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            pgeyAdj.add(new ArrayList<>(6));
        }
        for (int pgeyT = 0; pgeyT < pgeyMesh.pgeyFaceCount(); pgeyT++) {
            for (int pgeyE = 0; pgeyE < 3; pgeyE++) {
                int pgeyA = pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + pgeyE * 3];
                int pgeyB = pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + ((pgeyE + 1) % 3) * 3];
                pgeyTexOf[pgeyA] = pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + pgeyE * 3 + 2];
                pgeyNormalOf[pgeyA] = pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + pgeyE * 3 + 1];
                pgeyAdj.get(pgeyA).add(pgeyB);
                int[] pgeyUse = pgeyEdgeUse.computeIfAbsent(pgeyKey(pgeyA, pgeyB), pgeyK -> new int[]{0, pgeyA, pgeyB});
                pgeyUse[0]++;
            }
        }
        List<int[]> pgeyEdges = new ArrayList<>();
        boolean[] pgeyOnBoundary = new boolean[pgeyN];
        pgeyEdgeUse.values().forEach(pgeyU -> {
            if (pgeyU[0] == 1 && pgeyHem.test(pgeyU[1]) && pgeyHem.test(pgeyU[2])) {
                pgeyEdges.add(new int[]{pgeyU[1], pgeyU[2]});
                pgeyOnBoundary[pgeyU[1]] = true;
                pgeyOnBoundary[pgeyU[2]] = true;
            }
        });
        if (pgeyEdges.isEmpty()) {
            return pgeyMesh;
        }
        int[] pgeyLip = new int[pgeyN];
        int[] pgeyInner = new int[pgeyN];
        Arrays.fill(pgeyLip, -1);
        List<float[]> pgeyNewPts = new ArrayList<>();
        List<float[]> pgeyNewNormals = new ArrayList<>();
        int pgeyPointBase = pgeyN;
        int pgeyNormalBase = pgeyNrm.length / 3;
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            if (!pgeyOnBoundary[pgeyI]) {
                continue;
            }
            int pgeyNi = pgeyNormalOf[pgeyI] * 3;
            double pgeyNx = pgeyNrm[pgeyNi];
            double pgeyNy = pgeyNrm[pgeyNi + 1];
            double pgeyNz = pgeyNrm[pgeyNi + 2];
            double pgeyUx = 0;
            double pgeyUy = 0;
            double pgeyUz = 0;
            for (int pgeyJ : pgeyAdj.get(pgeyI)) {
                if (!pgeyOnBoundary[pgeyJ]) {
                    pgeyUx += pgeyP[pgeyJ * 3] - pgeyP[pgeyI * 3];
                    pgeyUy += pgeyP[pgeyJ * 3 + 1] - pgeyP[pgeyI * 3 + 1];
                    pgeyUz += pgeyP[pgeyJ * 3 + 2] - pgeyP[pgeyI * 3 + 2];
                }
            }
            double pgeyUl = Math.sqrt(pgeyUx * pgeyUx + pgeyUy * pgeyUy + pgeyUz * pgeyUz);
            if (pgeyUl < 1e-9) {
                pgeyUx = 0;
                pgeyUy = -1;
                pgeyUz = 0;
                pgeyUl = 1;
            }
            pgeyUx /= pgeyUl;
            pgeyUy /= pgeyUl;
            pgeyUz /= pgeyUl;
            float pgeyAx = (float) (pgeyP[pgeyI * 3] - pgeyNx * pgeyThickness);
            float pgeyAy = (float) (pgeyP[pgeyI * 3 + 1] - pgeyNy * pgeyThickness);
            float pgeyAz = (float) (pgeyP[pgeyI * 3 + 2] - pgeyNz * pgeyThickness);
            pgeyLip[pgeyI] = pgeyPointBase + pgeyNewPts.size();
            pgeyNewPts.add(new float[]{pgeyAx, pgeyAy, pgeyAz});
            int pgeyAt = pgeyI;
            double pgeyWalked = 0;
            while (pgeyWalked < pgeyDepth) {
                int pgeyNext = -1;
                double pgeyBest = 0;
                for (int pgeyJ : pgeyAdj.get(pgeyAt)) {
                    double pgeyDx = pgeyP[pgeyJ * 3] - pgeyP[pgeyAt * 3];
                    double pgeyDy = pgeyP[pgeyJ * 3 + 1] - pgeyP[pgeyAt * 3 + 1];
                    double pgeyDz = pgeyP[pgeyJ * 3 + 2] - pgeyP[pgeyAt * 3 + 2];
                    double pgeyAlong = pgeyDx * pgeyUx + pgeyDy * pgeyUy + pgeyDz * pgeyUz;
                    if (pgeyAlong > pgeyBest && !pgeyOnBoundary[pgeyJ]) {
                        pgeyBest = pgeyAlong;
                        pgeyNext = pgeyJ;
                    }
                }
                if (pgeyNext < 0) {
                    break;
                }
                pgeyWalked += pgeyBest;
                pgeyAt = pgeyNext;
            }
            int pgeyNa = pgeyNormalOf[pgeyAt] * 3;
            pgeyInner[pgeyI] = pgeyPointBase + pgeyNewPts.size();
            pgeyNewPts.add(new float[]{(float) (pgeyP[pgeyAt * 3] - pgeyNrm[pgeyNa] * pgeyThickness), (float) (pgeyP[pgeyAt * 3 + 1] - pgeyNrm[pgeyNa + 1] * pgeyThickness),
                    (float) (pgeyP[pgeyAt * 3 + 2] - pgeyNrm[pgeyNa + 2] * pgeyThickness)});
            pgeyNewNormals.add(new float[]{(float) -pgeyUx, (float) -pgeyUy, (float) -pgeyUz});
            pgeyNewNormals.add(new float[]{(float) -pgeyNx, (float) -pgeyNy, (float) -pgeyNz});
        }
        float[] pgeyPts2 = Arrays.copyOf(pgeyP, pgeyP.length + pgeyNewPts.size() * 3);
        for (int pgeyK = 0; pgeyK < pgeyNewPts.size(); pgeyK++) {
            System.arraycopy(pgeyNewPts.get(pgeyK), 0, pgeyPts2, pgeyP.length + pgeyK * 3, 3);
        }
        float[] pgeyNrm2 = Arrays.copyOf(pgeyNrm, pgeyNrm.length + pgeyNewNormals.size() * 3);
        for (int pgeyK = 0; pgeyK < pgeyNewNormals.size(); pgeyK++) {
            System.arraycopy(pgeyNewNormals.get(pgeyK), 0, pgeyNrm2, pgeyNrm.length + pgeyK * 3, 3);
        }
        int[] pgeyFaces2 = Arrays.copyOf(pgeyF, pgeyF.length + pgeyEdges.size() * 4 * PgeyMeshData.PGEY_FACE_STRIDE);
        int pgeyW = pgeyF.length;
        for (int[] pgeyE : pgeyEdges) {
            int pgeyA = pgeyE[0];
            int pgeyB = pgeyE[1];
            int pgeyLipNa = pgeyNormalBase + (pgeyLip[pgeyA] - pgeyPointBase);
            int pgeyLipNb = pgeyNormalBase + (pgeyLip[pgeyB] - pgeyPointBase);
            int pgeyInNa = pgeyLipNa + 1;
            int pgeyInNb = pgeyLipNb + 1;
            pgeyW = pgeyFace(pgeyFaces2, pgeyW, pgeyA, pgeyLipNa, pgeyTexOf[pgeyA], pgeyB, pgeyLipNb, pgeyTexOf[pgeyB], pgeyLip[pgeyB], pgeyLipNb, pgeyTexOf[pgeyB]);
            pgeyW = pgeyFace(pgeyFaces2, pgeyW, pgeyA, pgeyLipNa, pgeyTexOf[pgeyA], pgeyLip[pgeyB], pgeyLipNb, pgeyTexOf[pgeyB], pgeyLip[pgeyA], pgeyLipNa, pgeyTexOf[pgeyA]);
            pgeyW = pgeyFace(pgeyFaces2, pgeyW, pgeyLip[pgeyA], pgeyInNa, pgeyTexOf[pgeyA], pgeyLip[pgeyB], pgeyInNb, pgeyTexOf[pgeyB], pgeyInner[pgeyB], pgeyInNb, pgeyTexOf[pgeyB]);
            pgeyW = pgeyFace(pgeyFaces2, pgeyW, pgeyLip[pgeyA], pgeyInNa, pgeyTexOf[pgeyA], pgeyInner[pgeyB], pgeyInNb, pgeyTexOf[pgeyB], pgeyInner[pgeyA], pgeyInNa, pgeyTexOf[pgeyA]);
        }
        return new PgeyMeshData(pgeyPts2, pgeyNrm2, pgeyTex, pgeyFaces2);
    }

    private static int pgeyFace(int[] pgeyOut, int pgeyW, int pgeyP0, int pgeyN0, int pgeyT0, int pgeyP1, int pgeyN1, int pgeyT1, int pgeyP2, int pgeyN2, int pgeyT2) {
        pgeyOut[pgeyW++] = pgeyP0;
        pgeyOut[pgeyW++] = pgeyN0;
        pgeyOut[pgeyW++] = pgeyT0;
        pgeyOut[pgeyW++] = pgeyP1;
        pgeyOut[pgeyW++] = pgeyN1;
        pgeyOut[pgeyW++] = pgeyT1;
        pgeyOut[pgeyW++] = pgeyP2;
        pgeyOut[pgeyW++] = pgeyN2;
        pgeyOut[pgeyW++] = pgeyT2;
        return pgeyW;
    }
}
