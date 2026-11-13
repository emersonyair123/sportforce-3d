package com.sportforce3d.three.geom;

public final class PgeyGridSurface {
    @FunctionalInterface
    public interface PgeyUv {
        void pgeyAt(int pgeyR, int pgeyC, int pgeyIsland, float[] pgeyOut);
    }

    @FunctionalInterface
    public interface PgeyIsland {
        int pgeyOf(int pgeyR, int pgeyC);
    }

    private final int pgeyRows;
    private final int pgeyCols;
    private final boolean pgeyClosed;
    private final double[] pgeyP;
    private final double[] pgeyH;

    public PgeyGridSurface(int pgeyRows, int pgeyCols, boolean pgeyClosedU) {
        if (pgeyRows < 2 || pgeyCols < 2) {
            throw new IllegalArgumentException("Rejilla demasiado pequeña");
        }
        this.pgeyRows = pgeyRows;
        this.pgeyCols = pgeyCols;
        this.pgeyClosed = pgeyClosedU;
        this.pgeyP = new double[pgeyRows * pgeyCols * 3];
        this.pgeyH = new double[pgeyRows * pgeyCols * 3];
    }

    public int pgeyRows() {
        return pgeyRows;
    }

    public int pgeyCols() {
        return pgeyCols;
    }

    public void pgeySet(int pgeyR, int pgeyC, PgeyVec3 pgeyPoint, PgeyVec3 pgeyOutwardHint) {
        int pgeyI = (pgeyR * pgeyCols + pgeyC) * 3;
        pgeyP[pgeyI] = pgeyPoint.pgeyX();
        pgeyP[pgeyI + 1] = pgeyPoint.pgeyY();
        pgeyP[pgeyI + 2] = pgeyPoint.pgeyZ();
        pgeyH[pgeyI] = pgeyOutwardHint.pgeyX();
        pgeyH[pgeyI + 1] = pgeyOutwardHint.pgeyY();
        pgeyH[pgeyI + 2] = pgeyOutwardHint.pgeyZ();
    }

    public PgeyVec3 pgeyPoint(int pgeyR, int pgeyC) {
        int pgeyI = (pgeyR * pgeyCols + pgeyC) * 3;
        return new PgeyVec3(pgeyP[pgeyI], pgeyP[pgeyI + 1], pgeyP[pgeyI + 2]);
    }

    private PgeyVec3 pgeyHint(int pgeyR, int pgeyC) {
        int pgeyI = (pgeyR * pgeyCols + pgeyC) * 3;
        return new PgeyVec3(pgeyH[pgeyI], pgeyH[pgeyI + 1], pgeyH[pgeyI + 2]);
    }

    public PgeyMeshData pgeyBuild() {
        int pgeyColsT = pgeyClosed ? pgeyCols : pgeyCols - 1;
        return pgeyBuild(1, (pgeyR, pgeyC) -> 0, (pgeyR, pgeyC, pgeyIsland, pgeyOut) -> {
            pgeyOut[0] = (float) pgeyC / pgeyColsT;
            pgeyOut[1] = (float) pgeyR / (pgeyRows - 1);
        });
    }

    public PgeyMeshData pgeyBuild(int pgeyIslands, PgeyIsland pgeyIsland, PgeyUv pgeyUv) {
        int pgeyN = pgeyRows * pgeyCols;
        PgeyVec3[] pgeyNormals = new PgeyVec3[pgeyN];
        boolean[] pgeyDegenerate = new boolean[pgeyN];
        for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
            for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                PgeyVec3 pgeyTu = pgeyPoint(pgeyR, pgeyColNext(pgeyC)).pgeySub(pgeyPoint(pgeyR, pgeyColPrev(pgeyC)));
                PgeyVec3 pgeyTv = pgeyPoint(Math.min(pgeyRows - 1, pgeyR + 1), pgeyC).pgeySub(pgeyPoint(Math.max(0, pgeyR - 1), pgeyC));
                PgeyVec3 pgeyNrm = pgeyTu.pgeyCross(pgeyTv);
                if (pgeyNrm.pgeyLength() < 1e-12) {
                    pgeyDegenerate[pgeyR * pgeyCols + pgeyC] = true;
                    pgeyNormals[pgeyR * pgeyCols + pgeyC] = pgeyHint(pgeyR, pgeyC).pgeyNormalize();
                    continue;
                }
                pgeyNrm = pgeyNrm.pgeyNormalize();
                if (pgeyNrm.pgeyDot(pgeyHint(pgeyR, pgeyC)) < 0) {
                    pgeyNrm = pgeyNrm.pgeyMul(-1);
                }
                pgeyNormals[pgeyR * pgeyCols + pgeyC] = pgeyNrm;
            }
        }
        for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
            for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                if (!pgeyDegenerate[pgeyR * pgeyCols + pgeyC]) {
                    continue;
                }
                int pgeyNr = pgeyR == 0 ? 1 : pgeyR == pgeyRows - 1 ? pgeyRows - 2 : pgeyR;
                PgeyVec3 pgeySum = PgeyVec3.PGEY_ZERO;
                for (int pgeyK = 0; pgeyK < pgeyCols; pgeyK++) {
                    if (!pgeyDegenerate[pgeyNr * pgeyCols + pgeyK]) {
                        pgeySum = pgeySum.pgeyAdd(pgeyNormals[pgeyNr * pgeyCols + pgeyK]);
                    }
                }
                PgeyVec3 pgeyAvg = pgeySum.pgeyNormalize();
                pgeyNormals[pgeyR * pgeyCols + pgeyC] = pgeyAvg.pgeyLength() > 0 ? pgeyAvg : pgeyHint(pgeyR, pgeyC).pgeyNormalize();
            }
        }

        float[] pgeyPoints = new float[pgeyN * 3];
        float[] pgeyNorms = new float[pgeyN * 3];
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            pgeyPoints[pgeyI * 3] = (float) pgeyP[pgeyI * 3];
            pgeyPoints[pgeyI * 3 + 1] = (float) -pgeyP[pgeyI * 3 + 1];
            pgeyPoints[pgeyI * 3 + 2] = (float) -pgeyP[pgeyI * 3 + 2];
            PgeyVec3 pgeyNm = pgeyNormals[pgeyI];
            pgeyNorms[pgeyI * 3] = (float) pgeyNm.pgeyX();
            pgeyNorms[pgeyI * 3 + 1] = (float) -pgeyNm.pgeyY();
            pgeyNorms[pgeyI * 3 + 2] = (float) -pgeyNm.pgeyZ();
        }

        int pgeyColsT = pgeyClosed ? pgeyCols + 1 : pgeyCols;
        float[] pgeyTex = new float[pgeyIslands * pgeyRows * pgeyColsT * 2];
        float[] pgeyTmp = new float[2];
        for (int pgeyIs = 0; pgeyIs < pgeyIslands; pgeyIs++) {
            for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
                for (int pgeyC = 0; pgeyC < pgeyColsT; pgeyC++) {
                    pgeyUv.pgeyAt(pgeyR, pgeyC, pgeyIs, pgeyTmp);
                    int pgeyT = ((pgeyIs * pgeyRows + pgeyR) * pgeyColsT + pgeyC) * 2;
                    pgeyTex[pgeyT] = pgeyTmp[0];
                    pgeyTex[pgeyT + 1] = pgeyTmp[1];
                }
            }
        }

        int pgeyQuadCols = pgeyClosed ? pgeyCols : pgeyCols - 1;
        int[] pgeyFaces = new int[(pgeyRows - 1) * pgeyQuadCols * 2 * PgeyMeshData.PGEY_FACE_STRIDE];
        int pgeyF = 0;
        for (int pgeyR = 0; pgeyR < pgeyRows - 1; pgeyR++) {
            for (int pgeyC = 0; pgeyC < pgeyQuadCols; pgeyC++) {
                int pgeyIs = pgeyIsland.pgeyOf(pgeyR, pgeyC);
                int pgeyC1 = pgeyC + 1;
                int pgeyPc1 = pgeyC1 % pgeyCols;
                int pgeyA = pgeyR * pgeyCols + pgeyC;
                int pgeyB = (pgeyR + 1) * pgeyCols + pgeyC;
                int pgeyCc = (pgeyR + 1) * pgeyCols + pgeyPc1;
                int pgeyD = pgeyR * pgeyCols + pgeyPc1;
                int pgeyTa = (pgeyIs * pgeyRows + pgeyR) * pgeyColsT + pgeyC;
                int pgeyTb = (pgeyIs * pgeyRows + pgeyR + 1) * pgeyColsT + pgeyC;
                int pgeyTc = (pgeyIs * pgeyRows + pgeyR + 1) * pgeyColsT + pgeyC1;
                int pgeyTd = (pgeyIs * pgeyRows + pgeyR) * pgeyColsT + pgeyC1;
                pgeyF = pgeyTriangle(pgeyFaces, pgeyF, pgeyNormals, pgeyA, pgeyTa, pgeyB, pgeyTb, pgeyCc, pgeyTc);
                pgeyF = pgeyTriangle(pgeyFaces, pgeyF, pgeyNormals, pgeyA, pgeyTa, pgeyCc, pgeyTc, pgeyD, pgeyTd);
            }
        }
        return new PgeyMeshData(pgeyPoints, pgeyNorms, pgeyTex, pgeyFaces);
    }

    private int pgeyTriangle(int[] pgeyFaces, int pgeyF, PgeyVec3[] pgeyNormals, int pgeyA, int pgeyTa, int pgeyB, int pgeyTb, int pgeyC, int pgeyTc) {
        PgeyVec3 pgeyPa = pgeyPoint(pgeyA / pgeyCols, pgeyA % pgeyCols);
        PgeyVec3 pgeyPb = pgeyPoint(pgeyB / pgeyCols, pgeyB % pgeyCols);
        PgeyVec3 pgeyPc = pgeyPoint(pgeyC / pgeyCols, pgeyC % pgeyCols);
        PgeyVec3 pgeyGeometric = pgeyPb.pgeySub(pgeyPa).pgeyCross(pgeyPc.pgeySub(pgeyPa));
        PgeyVec3 pgeySmooth = pgeyNormals[pgeyA].pgeyAdd(pgeyNormals[pgeyB]).pgeyAdd(pgeyNormals[pgeyC]);
        boolean pgeyKeep = pgeyGeometric.pgeyDot(pgeySmooth) >= 0;
        if (!PGEY_FRONT_IS_CCW_IN_MODEL) {
            pgeyKeep = !pgeyKeep;
        }
        int pgeyP1 = pgeyKeep ? pgeyB : pgeyC;
        int pgeyT1 = pgeyKeep ? pgeyTb : pgeyTc;
        int pgeyP2 = pgeyKeep ? pgeyC : pgeyB;
        int pgeyT2 = pgeyKeep ? pgeyTc : pgeyTb;
        pgeyFaces[pgeyF++] = pgeyA;
        pgeyFaces[pgeyF++] = pgeyA;
        pgeyFaces[pgeyF++] = pgeyTa;
        pgeyFaces[pgeyF++] = pgeyP1;
        pgeyFaces[pgeyF++] = pgeyP1;
        pgeyFaces[pgeyF++] = pgeyT1;
        pgeyFaces[pgeyF++] = pgeyP2;
        pgeyFaces[pgeyF++] = pgeyP2;
        pgeyFaces[pgeyF++] = pgeyT2;
        return pgeyF;
    }

    static final boolean PGEY_FRONT_IS_CCW_IN_MODEL = true;

    private int pgeyColNext(int pgeyC) {
        if (pgeyC + 1 < pgeyCols) {
            return pgeyC + 1;
        }
        return pgeyClosed ? 0 : pgeyC;
    }

    private int pgeyColPrev(int pgeyC) {
        if (pgeyC > 0) {
            return pgeyC - 1;
        }
        return pgeyClosed ? pgeyCols - 1 : pgeyC;
    }
}
