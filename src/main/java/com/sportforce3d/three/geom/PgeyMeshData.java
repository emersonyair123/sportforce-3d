package com.sportforce3d.three.geom;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.util.List;

public record PgeyMeshData(float[] pgeyPoints, float[] pgeyNormals, float[] pgeyTexCoords, int[] pgeyFaces) {
    public static final int PGEY_FACE_STRIDE = 9;

    public int pgeyPointCount() {
        return pgeyPoints.length / 3;
    }

    public int pgeyFaceCount() {
        return pgeyFaces.length / PGEY_FACE_STRIDE;
    }

    public TriangleMesh pgeyToTriangleMesh() {
        TriangleMesh pgeyMesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        pgeyMesh.getPoints().setAll(pgeyPoints);
        pgeyMesh.getNormals().setAll(pgeyNormals);
        pgeyMesh.getTexCoords().setAll(pgeyTexCoords);
        pgeyMesh.getFaces().setAll(pgeyFaces);
        return pgeyMesh;
    }

    public static PgeyMeshData pgeyMerge(List<PgeyMeshData> pgeyParts) {
        int pgeyP = 0;
        int pgeyN = 0;
        int pgeyT = 0;
        int pgeyF = 0;
        for (PgeyMeshData pgeyM : pgeyParts) {
            pgeyP += pgeyM.pgeyPoints.length;
            pgeyN += pgeyM.pgeyNormals.length;
            pgeyT += pgeyM.pgeyTexCoords.length;
            pgeyF += pgeyM.pgeyFaces.length;
        }
        float[] pgeyPoints = new float[pgeyP];
        float[] pgeyNormals = new float[pgeyN];
        float[] pgeyTex = new float[pgeyT];
        int[] pgeyFaces = new int[pgeyF];
        int pgeyPo = 0;
        int pgeyNo = 0;
        int pgeyTo = 0;
        int pgeyFo = 0;
        for (PgeyMeshData pgeyM : pgeyParts) {
            System.arraycopy(pgeyM.pgeyPoints, 0, pgeyPoints, pgeyPo, pgeyM.pgeyPoints.length);
            System.arraycopy(pgeyM.pgeyNormals, 0, pgeyNormals, pgeyNo, pgeyM.pgeyNormals.length);
            System.arraycopy(pgeyM.pgeyTexCoords, 0, pgeyTex, pgeyTo, pgeyM.pgeyTexCoords.length);
            int pgeyPBase = pgeyPo / 3;
            int pgeyNBase = pgeyNo / 3;
            int pgeyTBase = pgeyTo / 2;
            for (int pgeyI = 0; pgeyI < pgeyM.pgeyFaces.length; pgeyI += 3) {
                pgeyFaces[pgeyFo + pgeyI] = pgeyM.pgeyFaces[pgeyI] + pgeyPBase;
                pgeyFaces[pgeyFo + pgeyI + 1] = pgeyM.pgeyFaces[pgeyI + 1] + pgeyNBase;
                pgeyFaces[pgeyFo + pgeyI + 2] = pgeyM.pgeyFaces[pgeyI + 2] + pgeyTBase;
            }
            pgeyPo += pgeyM.pgeyPoints.length;
            pgeyNo += pgeyM.pgeyNormals.length;
            pgeyTo += pgeyM.pgeyTexCoords.length;
            pgeyFo += pgeyM.pgeyFaces.length;
        }
        return new PgeyMeshData(pgeyPoints, pgeyNormals, pgeyTex, pgeyFaces);
    }
}
