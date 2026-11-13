package com.sportforce3d.three.geom;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class GeometryTest {

    @Test
    void curvePassesThroughKeysWithoutOvershooting() {
        PgeyCurve c = PgeyCurve.pgeyOf(0, 0, 1, 1, 2, 1, 3, 0);
        assertThat(c.pgeyAt(0)).isEqualTo(0);
        assertThat(c.pgeyAt(1)).isEqualTo(1);
        assertThat(c.pgeyAt(2)).isEqualTo(1);
        for (double x = 0; x <= 3; x += 0.01) {
            assertThat(c.pgeyAt(x)).isBetween(0.0, 1.0);
        }
        assertThat(c.pgeyAt(-5)).isEqualTo(0);
        assertThat(c.pgeyAt(9)).isEqualTo(0);
    }

    @Test
    void angularHelpersWrapAroundTheCircle() {
        assertThat(PgeyCurve.pgeyAngleDiff(0.1, 2 * Math.PI - 0.1)).isCloseTo(0.2, within(1e-9));
        assertThat(PgeyCurve.pgeyAngularBell(Math.PI, -Math.PI, 0.3)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void closedCylinderHasOutwardUnitNormalsAndConsistentWinding() {
        int rows = 10;
        int cols = 24;
        PgeyGridSurface g = new PgeyGridSurface(rows, cols, true);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double a = 2 * Math.PI * c / cols;
                PgeyVec3 p = new PgeyVec3(Math.sin(a) * 0.5, r * 0.1, Math.cos(a) * 0.5);
                g.pgeySet(r, c, p, new PgeyVec3(p.pgeyX(), 0, p.pgeyZ()));
            }
        }
        PgeyMeshData m = g.pgeyBuild();
        assertThat(m.pgeyPointCount()).isEqualTo(rows * cols);
        assertThat(m.pgeyFaceCount()).isEqualTo((rows - 1) * cols * 2);
        assertOutward(m);
    }

    @Test
    void mergeReindexesFaces() {
        PgeyGridSurface a = new PgeyGridSurface(2, 4, true);
        PgeyGridSurface b = new PgeyGridSurface(2, 4, true);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 4; c++) {
                double ang = Math.PI / 2 * c;
                a.pgeySet(r, c, new PgeyVec3(Math.sin(ang), r, Math.cos(ang)), new PgeyVec3(Math.sin(ang), 0, Math.cos(ang)));
                b.pgeySet(r, c, new PgeyVec3(Math.sin(ang) + 5, r, Math.cos(ang)), new PgeyVec3(Math.sin(ang), 0, Math.cos(ang)));
            }
        }
        PgeyMeshData merged = PgeyMeshData.pgeyMerge(List.of(a.pgeyBuild(), b.pgeyBuild()));
        assertThat(merged.pgeyPointCount()).isEqualTo(16);
        int maxPoint = 0;
        for (int i = 0; i < merged.pgeyFaces().length; i += 3) {
            maxPoint = Math.max(maxPoint, merged.pgeyFaces()[i]);
        }
        assertThat(maxPoint).isEqualTo(15);
    }

    public static void assertOutward(PgeyMeshData m) {
        float[] p = m.pgeyPoints();
        float[] n = m.pgeyNormals();
        for (int i = 0; i < n.length; i += 3) {
            double len = Math.sqrt(n[i] * n[i] + n[i + 1] * n[i + 1] + n[i + 2] * n[i + 2]);
            assertThat(len).isCloseTo(1.0, within(1e-3));
        }
        int[] f = m.pgeyFaces();
        int bad = 0;
        for (int i = 0; i < f.length; i += PgeyMeshData.PGEY_FACE_STRIDE) {
            PgeyVec3 a = pt(p, f[i]);
            PgeyVec3 b = pt(p, f[i + 3]);
            PgeyVec3 c = pt(p, f[i + 6]);
            PgeyVec3 geo = b.pgeySub(a).pgeyCross(c.pgeySub(a));
            PgeyVec3 smooth = pt(n, f[i + 1]).pgeyAdd(pt(n, f[i + 4])).pgeyAdd(pt(n, f[i + 7]));
            if (geo.pgeyLength() > 1e-12 && geo.pgeyDot(smooth) < 0) {
                bad++;
            }
        }
        assertThat(bad).as("caras con orientación invertida").isZero();
    }

    private static PgeyVec3 pt(float[] a, int i) {
        return new PgeyVec3(a[i * 3], a[i * 3 + 1], a[i * 3 + 2]);
    }
}
