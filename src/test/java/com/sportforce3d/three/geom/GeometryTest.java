package com.sportforce3d.three.geom;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class GeometryTest {

    @Test
    void curvePassesThroughKeysWithoutOvershooting() {
        Curve c = Curve.of(0, 0, 1, 1, 2, 1, 3, 0);
        assertThat(c.at(0)).isEqualTo(0);
        assertThat(c.at(1)).isEqualTo(1);
        assertThat(c.at(2)).isEqualTo(1);
        for (double x = 0; x <= 3; x += 0.01) {
            assertThat(c.at(x)).isBetween(0.0, 1.0);
        }
        assertThat(c.at(-5)).isEqualTo(0);
        assertThat(c.at(9)).isEqualTo(0);
    }

    @Test
    void angularHelpersWrapAroundTheCircle() {
        assertThat(Curve.angleDiff(0.1, 2 * Math.PI - 0.1)).isCloseTo(0.2, within(1e-9));
        assertThat(Curve.angularBell(Math.PI, -Math.PI, 0.3)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void closedCylinderHasOutwardUnitNormalsAndConsistentWinding() {
        int rows = 10;
        int cols = 24;
        GridSurface g = new GridSurface(rows, cols, true);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double a = 2 * Math.PI * c / cols;
                Vec3 p = new Vec3(Math.sin(a) * 0.5, r * 0.1, Math.cos(a) * 0.5);
                g.set(r, c, p, new Vec3(p.x(), 0, p.z()));
            }
        }
        MeshData m = g.build();
        assertThat(m.pointCount()).isEqualTo(rows * cols);
        assertThat(m.faceCount()).isEqualTo((rows - 1) * cols * 2);
        assertOutward(m);
    }

    @Test
    void mergeReindexesFaces() {
        GridSurface a = new GridSurface(2, 4, true);
        GridSurface b = new GridSurface(2, 4, true);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 4; c++) {
                double ang = Math.PI / 2 * c;
                a.set(r, c, new Vec3(Math.sin(ang), r, Math.cos(ang)), new Vec3(Math.sin(ang), 0, Math.cos(ang)));
                b.set(r, c, new Vec3(Math.sin(ang) + 5, r, Math.cos(ang)), new Vec3(Math.sin(ang), 0, Math.cos(ang)));
            }
        }
        MeshData merged = MeshData.merge(List.of(a.build(), b.build()));
        assertThat(merged.pointCount()).isEqualTo(16);
        int maxPoint = 0;
        for (int i = 0; i < merged.faces().length; i += 3) {
            maxPoint = Math.max(maxPoint, merged.faces()[i]);
        }
        assertThat(maxPoint).isEqualTo(15);
    }

    public static void assertOutward(MeshData m) {
        float[] p = m.points();
        float[] n = m.normals();
        for (int i = 0; i < n.length; i += 3) {
            double len = Math.sqrt(n[i] * n[i] + n[i + 1] * n[i + 1] + n[i + 2] * n[i + 2]);
            assertThat(len).isCloseTo(1.0, within(1e-3));
        }
        int[] f = m.faces();
        int bad = 0;
        for (int i = 0; i < f.length; i += MeshData.FACE_STRIDE) {
            Vec3 a = pt(p, f[i]);
            Vec3 b = pt(p, f[i + 3]);
            Vec3 c = pt(p, f[i + 6]);
            Vec3 geo = b.sub(a).cross(c.sub(a));
            Vec3 smooth = pt(n, f[i + 1]).add(pt(n, f[i + 4])).add(pt(n, f[i + 7]));
            if (geo.length() > 1e-12 && geo.dot(smooth) < 0) {
                bad++;
            }
        }
        assertThat(bad).as("caras con orientación invertida").isZero();
    }

    private static Vec3 pt(float[] a, int i) {
        return new Vec3(a[i * 3], a[i * 3 + 1], a[i * 3 + 2]);
    }
}
