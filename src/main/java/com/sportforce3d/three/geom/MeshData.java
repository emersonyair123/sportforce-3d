package com.sportforce3d.three.geom;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.util.List;

public record MeshData(float[] points, float[] normals, float[] texCoords, int[] faces) {
    public static final int FACE_STRIDE = 9;

    public int pointCount() {
        return points.length / 3;
    }

    public int faceCount() {
        return faces.length / FACE_STRIDE;
    }

    public TriangleMesh toTriangleMesh() {
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        mesh.getPoints().setAll(points);
        mesh.getNormals().setAll(normals);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);
        return mesh;
    }

    public static MeshData merge(List<MeshData> parts) {
        int p = 0;
        int n = 0;
        int t = 0;
        int f = 0;
        for (MeshData m : parts) {
            p += m.points.length;
            n += m.normals.length;
            t += m.texCoords.length;
            f += m.faces.length;
        }
        float[] points = new float[p];
        float[] normals = new float[n];
        float[] tex = new float[t];
        int[] faces = new int[f];
        int po = 0;
        int no = 0;
        int to = 0;
        int fo = 0;
        for (MeshData m : parts) {
            System.arraycopy(m.points, 0, points, po, m.points.length);
            System.arraycopy(m.normals, 0, normals, no, m.normals.length);
            System.arraycopy(m.texCoords, 0, tex, to, m.texCoords.length);
            int pBase = po / 3;
            int nBase = no / 3;
            int tBase = to / 2;
            for (int i = 0; i < m.faces.length; i += 3) {
                faces[fo + i] = m.faces[i] + pBase;
                faces[fo + i + 1] = m.faces[i + 1] + nBase;
                faces[fo + i + 2] = m.faces[i + 2] + tBase;
            }
            po += m.points.length;
            no += m.normals.length;
            to += m.texCoords.length;
            fo += m.faces.length;
        }
        return new MeshData(points, normals, tex, faces);
    }
}
