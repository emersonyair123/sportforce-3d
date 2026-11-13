package com.sportforce3d.three.procedural;

import com.sportforce3d.three.geom.PgeyGridSurface;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyTube;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyModelData.PgeyPart;
import com.sportforce3d.three.procedural.PgeyBodyShape.PgeySide;

import java.util.ArrayList;
import java.util.List;

final class PgeyMannequinGenerator {

    private PgeyMannequinGenerator() {
    }

    static List<PgeyPart> pgeyBuild(PgeyBodyShape pgeyB) {
        List<PgeyPart> pgeyParts = new ArrayList<>();
        pgeyParts.add(new PgeyPart("mannequin_torso", pgeyTorso(pgeyB)));
        pgeyParts.add(new PgeyPart("mannequin_neck", pgeyTube(pgeyB, pgeyB.pgeyNeck(), 0, 1, 32, 56, false)));
        pgeyParts.add(new PgeyPart("mannequin_head", pgeyHead(pgeyB)));
        for (PgeySide pgeySide : PgeySide.values()) {
            String pgeySuffix = pgeySide == PgeySide.LEFT ? "_l" : "_r";
            pgeyParts.add(new PgeyPart("mannequin_arm" + pgeySuffix, pgeyTube(pgeyB, pgeyB.pgeyArm(pgeySide), 0, 1, 170, 48, false)));
            pgeyParts.add(new PgeyPart("mannequin_leg" + pgeySuffix, pgeyTube(pgeyB, pgeyB.pgeyLeg(pgeySide), 0, 1, 170, 56, false)));
        }
        return pgeyParts;
    }

    private static PgeyMeshData pgeyTorso(PgeyBodyShape pgeyB) {
        int pgeyRows = 170;
        int pgeyCols = 128;
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
        for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
            double pgeyT = (double) pgeyR / (pgeyRows - 1);
            double pgeyY = PgeyBodyShape.PGEY_TORSO_BOTTOM
                    + (PgeyBodyShape.PGEY_TORSO_TOP - PgeyBodyShape.PGEY_TORSO_BOTTOM) * (1 - Math.pow(1 - pgeyT, 1.3));
            for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                double pgeyPhi = 2 * Math.PI * pgeyC / pgeyCols;
                PgeyVec3 pgeyP = pgeyB.pgeyTorsoPoint(pgeyPhi, pgeyY, 0);
                pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOut(pgeyP), pgeyB.pgeyOut(pgeyP).pgeySub(pgeyB.pgeyOut(pgeyB.pgeyTorsoAxis(pgeyY))).pgeyAdd(new PgeyVec3(0, -0.02 * (1 - pgeyT), 0)));
            }
        }
        return pgeyG.pgeyBuild();
    }

    private static PgeyMeshData pgeyHead(PgeyBodyShape pgeyB) {
        int pgeyRows = 64;
        int pgeyCols = 72;
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
        double pgeyY0 = PgeyBodyShape.PGEY_HEAD_RX.pgeyMinX();
        double pgeyY1 = PgeyBodyShape.PGEY_HEAD_RX.pgeyMaxX();
        PgeyVec3 pgeyCenter = new PgeyVec3(0, 1.72, 0.01);
        for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
            double pgeyT = (double) pgeyR / (pgeyRows - 1);
            double pgeyY = pgeyY0 + (pgeyY1 - pgeyY0) * (0.5 - 0.5 * Math.cos(Math.PI * pgeyT));
            double pgeyRx = PgeyBodyShape.PGEY_HEAD_RX.pgeyAt(pgeyY);
            double pgeyRzf = PgeyBodyShape.PGEY_HEAD_RZ_FRONT.pgeyAt(pgeyY);
            double pgeyRzb = PgeyBodyShape.PGEY_HEAD_RZ_BACK.pgeyAt(pgeyY);
            double pgeyCz = PgeyBodyShape.PGEY_HEAD_CZ.pgeyAt(pgeyY);
            for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                double pgeyPhi = 2 * Math.PI * pgeyC / pgeyCols;
                double pgeyS = Math.sin(pgeyPhi);
                double pgeyCo = Math.cos(pgeyPhi);
                double pgeyRz = pgeyCo >= 0 ? pgeyRzf : pgeyRzb;
                double pgeyE = 2.0 / 2.15;
                double pgeyX = pgeyRx * Math.signum(pgeyS) * Math.pow(Math.abs(pgeyS), pgeyE);
                double pgeyZ = pgeyCz + pgeyRz * Math.signum(pgeyCo) * Math.pow(Math.abs(pgeyCo), pgeyE);
                PgeyVec3 pgeyP = new PgeyVec3(pgeyX, pgeyY, pgeyZ);
                pgeyG.pgeySet(pgeyR, pgeyC, pgeyB.pgeyOutHead(pgeyP), pgeyB.pgeyOutHead(pgeyP).pgeySub(pgeyB.pgeyOutHead(pgeyCenter)));
            }
        }
        return pgeyG.pgeyBuild();
    }

    static PgeyMeshData pgeyTube(PgeyBodyShape pgeyB, PgeyTube pgeyTube, double pgeyF0, double pgeyF1, int pgeyRows, int pgeyCols, boolean pgeyHead) {
        PgeyGridSurface pgeyG = new PgeyGridSurface(pgeyRows, pgeyCols, true);
        for (int pgeyR = 0; pgeyR < pgeyRows; pgeyR++) {
            double pgeyT = (double) pgeyR / (pgeyRows - 1);
            double pgeyF = pgeyF0 + (pgeyF1 - pgeyF0) * pgeyT;
            PgeyVec3 pgeyCenter = pgeyTube.pgeyCenter(pgeyF);
            for (int pgeyC = 0; pgeyC < pgeyCols; pgeyC++) {
                double pgeyAngle = 2 * Math.PI * pgeyC / pgeyCols;
                PgeyVec3 pgeyP = pgeyTube.pgeyPoint(pgeyF, pgeyAngle, 0);
                PgeyVec3 pgeyHint = pgeyP.pgeySub(pgeyCenter);
                if (pgeyT > 0.985) {
                    pgeyHint = pgeyHint.pgeyAdd(pgeyTube.pgeyTangent(pgeyF).pgeyMul(0.05));
                }
                pgeyG.pgeySet(pgeyR, pgeyC, pgeyHead ? pgeyB.pgeyOutHead(pgeyP) : pgeyB.pgeyOut(pgeyP), pgeyHint);
            }
        }
        return pgeyG.pgeyBuild();
    }
}
