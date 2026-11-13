package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.MeshData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

final class ClothOps {

    private ClothOps() {
    }

    static int[][] neighbors(MeshData mesh, int[] canon, int m) {
        List<java.util.Set<Integer>> sets = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            sets.add(new java.util.HashSet<>(8));
        }
        int[] f = mesh.faces();
        for (int t = 0; t < mesh.faceCount(); t++) {
            int a = canon[f[t * MeshData.FACE_STRIDE]];
            int b = canon[f[t * MeshData.FACE_STRIDE + 3]];
            int c = canon[f[t * MeshData.FACE_STRIDE + 6]];
            sets.get(a).add(b);
            sets.get(a).add(c);
            sets.get(b).add(a);
            sets.get(b).add(c);
            sets.get(c).add(a);
            sets.get(c).add(b);
        }
        int[][] out = new int[m][];
        for (int i = 0; i < m; i++) {
            out[i] = sets.get(i).stream().mapToInt(Integer::intValue).toArray();
        }
        return out;
    }

    static boolean[] boundary(MeshData mesh, int[] canon, int m) {
        Map<Long, Integer> count = new HashMap<>();
        int[] f = mesh.faces();
        for (int t = 0; t < mesh.faceCount(); t++) {
            for (int e = 0; e < 3; e++) {
                int a = canon[f[t * MeshData.FACE_STRIDE + e * 3]];
                int b = canon[f[t * MeshData.FACE_STRIDE + ((e + 1) % 3) * 3]];
                count.merge(key(a, b), 1, Integer::sum);
            }
        }
        boolean[] out = new boolean[m];
        count.forEach((k, n) -> {
            if (n == 1) {
                out[(int) (k >> 32)] = true;
                out[(int) (k & 0xFFFFFFFFL)] = true;
            }
        });
        return out;
    }

    private static long key(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }

    static void taubin(double[] pos, int[][] nb, boolean[] fixed, boolean[] side, int iterations) {
        int m = nb.length;
        double[] next = new double[pos.length];
        for (int it = 0; it < iterations * 2; it++) {
            double k = it % 2 == 0 ? 0.5 : -0.53;
            for (int i = 0; i < m; i++) {
                double sx = 0;
                double sy = 0;
                double sz = 0;
                int n = 0;
                for (int j : nb[i]) {
                    if (side[j] == side[i] && (!fixed[i] || fixed[j])) {
                        sx += pos[j * 3];
                        sy += pos[j * 3 + 1];
                        sz += pos[j * 3 + 2];
                        n++;
                    }
                }
                if (n == 0) {
                    next[i * 3] = pos[i * 3];
                    next[i * 3 + 1] = pos[i * 3 + 1];
                    next[i * 3 + 2] = pos[i * 3 + 2];
                } else {
                    next[i * 3] = pos[i * 3] + k * (sx / n - pos[i * 3]);
                    next[i * 3 + 1] = pos[i * 3 + 1] + k * (sy / n - pos[i * 3 + 1]);
                    next[i * 3 + 2] = pos[i * 3 + 2] + k * (sz / n - pos[i * 3 + 2]);
                }
            }
            System.arraycopy(next, 0, pos, 0, pos.length);
        }
    }

    static double[] follow(double[] baseSrc, double[] baseDst, double[] pts) {
        double cell = 0.02;
        Map<Long, List<Integer>> grid = new HashMap<>();
        int n = baseSrc.length / 3;
        for (int i = 0; i < n; i++) {
            grid.computeIfAbsent(cellKey(baseSrc[i * 3] / cell, baseSrc[i * 3 + 1] / cell, baseSrc[i * 3 + 2] / cell),
                    k -> new ArrayList<>()).add(i);
        }
        double[] out = new double[pts.length];
        int[] best = new int[12];
        double[] bestD = new double[12];
        for (int p = 0; p < pts.length / 3; p++) {
            double x = pts[p * 3];
            double y = pts[p * 3 + 1];
            double z = pts[p * 3 + 2];
            Arrays.fill(bestD, Double.MAX_VALUE);
            Arrays.fill(best, -1);
            for (int r = 1; r <= 4 && best[best.length - 1] < 0; r++) {
                int cx = (int) Math.floor(x / cell);
                int cy = (int) Math.floor(y / cell);
                int cz = (int) Math.floor(z / cell);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -r; dy <= r; dy++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != r && r > 1) {
                                continue;
                            }
                            List<Integer> list = grid.get(cellKey(cx + dx, cy + dy, cz + dz));
                            if (list == null) {
                                continue;
                            }
                            for (int i : list) {
                                double ex = baseSrc[i * 3] - x;
                                double ey = baseSrc[i * 3 + 1] - y;
                                double ez = baseSrc[i * 3 + 2] - z;
                                insert(best, bestD, i, ex * ex + ey * ey + ez * ez);
                            }
                        }
                    }
                }
            }
            double wx = 0;
            double wy = 0;
            double wz = 0;
            double ws = 0;
            for (int k = 0; k < best.length; k++) {
                if (best[k] < 0) {
                    continue;
                }
                double w = 1 / (bestD[k] + 1e-8);
                int i = best[k];
                wx += w * (baseDst[i * 3] - baseSrc[i * 3]);
                wy += w * (baseDst[i * 3 + 1] - baseSrc[i * 3 + 1]);
                wz += w * (baseDst[i * 3 + 2] - baseSrc[i * 3 + 2]);
                ws += w;
            }
            out[p * 3] = x + (ws > 0 ? wx / ws : 0);
            out[p * 3 + 1] = y + (ws > 0 ? wy / ws : 0);
            out[p * 3 + 2] = z + (ws > 0 ? wz / ws : 0);
        }
        return out;
    }

    static float[] followEdge(MeshData src, MeshData dst, IntPredicate edge, float[] pts, double thickness,
                              double height, double lower) {
        int n = src.pointCount();
        int[] f = src.faces();
        Map<Long, Integer> use = new HashMap<>();
        int[] normalOf = new int[n];
        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>(6));
        }
        for (int t = 0; t < src.faceCount(); t++) {
            for (int e = 0; e < 3; e++) {
                int a = f[t * MeshData.FACE_STRIDE + e * 3];
                int b = f[t * MeshData.FACE_STRIDE + ((e + 1) % 3) * 3];
                normalOf[a] = f[t * MeshData.FACE_STRIDE + e * 3 + 1];
                adj.get(a).add(b);
                adj.get(b).add(a);
                use.merge(key(a, b), 1, Integer::sum);
            }
        }
        boolean[] onEdge = new boolean[n];
        use.forEach((k, c) -> {
            if (c == 1) {
                int a = (int) (k >> 32);
                int b = (int) (k & 0xFFFFFFFFL);
                if (edge.test(a) && edge.test(b)) {
                    onEdge[a] = true;
                    onEdge[b] = true;
                }
            }
        });
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (onEdge[i]) {
                ids.add(i);
            }
        }
        double[][] frameSrc = new double[ids.size()][];
        double[][] frameDst = new double[ids.size()][];
        for (int k = 0; k < ids.size(); k++) {
            frameSrc[k] = frame(src, ids.get(k), normalOf, adj, onEdge);
            frameDst[k] = frame(dst, ids.get(k), normalOf, adj, onEdge);
        }
        float[] out = new float[pts.length];
        for (int p = 0; p < pts.length; p += 3) {
            int b0 = -1;
            int b1 = -1;
            double d0 = Double.MAX_VALUE;
            double d1 = Double.MAX_VALUE;
            for (int k = 0; k < ids.size(); k++) {
                double[] fr = frameSrc[k];
                double dx = pts[p] - fr[0];
                double dy = pts[p + 1] - fr[1];
                double dz = pts[p + 2] - fr[2];
                double d = dx * dx + dy * dy + dz * dz;
                if (d < d0) {
                    d1 = d0;
                    b1 = b0;
                    d0 = d;
                    b0 = k;
                } else if (d < d1) {
                    d1 = d;
                    b1 = k;
                }
            }
            double[] q0 = carry(pts, p, frameSrc[b0], frameDst[b0], thickness, height, lower);
            if (b1 < 0) {
                out[p] = (float) q0[0];
                out[p + 1] = (float) q0[1];
                out[p + 2] = (float) q0[2];
                continue;
            }
            double[] q1 = carry(pts, p, frameSrc[b1], frameDst[b1], thickness, height, lower);
            double w0 = 1 / (Math.sqrt(d0) + 1e-6);
            double w1 = 1 / (Math.sqrt(d1) + 1e-6);
            for (int k = 0; k < 3; k++) {
                out[p + k] = (float) ((q0[k] * w0 + q1[k] * w1) / (w0 + w1));
            }
        }
        return out;
    }

    private static double[] frame(MeshData m, int i, int[] normalOf, List<List<Integer>> adj, boolean[] onEdge) {
        float[] p = m.points();
        float[] nr = m.normals();
        int ni = normalOf[i] * 3;
        double nx = nr[ni];
        double ny = nr[ni + 1];
        double nz = nr[ni + 2];
        double ax = 0;
        double ay = 0;
        double az = 0;
        int c = 0;
        for (int j : adj.get(i)) {
            if (!onEdge[j]) {
                ax += p[j * 3];
                ay += p[j * 3 + 1];
                az += p[j * 3 + 2];
                c++;
            }
        }
        double ux = c == 0 ? 0 : p[i * 3] - ax / c;
        double uy = c == 0 ? -1 : p[i * 3 + 1] - ay / c;
        double uz = c == 0 ? 0 : p[i * 3 + 2] - az / c;
        double dot = ux * nx + uy * ny + uz * nz;
        ux -= dot * nx;
        uy -= dot * ny;
        uz -= dot * nz;
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul < 1e-12) {
            ux = 0;
            uy = -1;
            uz = 0;
            ul = 1;
        }
        ux /= ul;
        uy /= ul;
        uz /= ul;
        double tx = ny * uz - nz * uy;
        double ty = nz * ux - nx * uz;
        double tz = nx * uy - ny * ux;
        return new double[]{p[i * 3], p[i * 3 + 1], p[i * 3 + 2], nx, ny, nz, ux, uy, uz, tx, ty, tz};
    }

    private static double[] carry(float[] pts, int p, double[] a, double[] b, double thickness, double height,
                                  double lower) {
        double dx = pts[p] - a[0];
        double dy = pts[p + 1] - a[1];
        double dz = pts[p + 2] - a[2];
        double on = (dx * a[3] + dy * a[4] + dz * a[5]) * thickness;
        double ou = (dx * a[6] + dy * a[7] + dz * a[8]) * height - lower;
        double ot = dx * a[9] + dy * a[10] + dz * a[11];
        return new double[]{b[0] + b[3] * on + b[6] * ou + b[9] * ot, b[1] + b[4] * on + b[7] * ou + b[10] * ot,
                b[2] + b[5] * on + b[8] * ou + b[11] * ot};
    }

    private static void insert(int[] best, double[] bestD, int i, double d) {
        int k = best.length - 1;
        if (d >= bestD[k]) {
            return;
        }
        while (k > 0 && bestD[k - 1] > d) {
            bestD[k] = bestD[k - 1];
            best[k] = best[k - 1];
            k--;
        }
        bestD[k] = d;
        best[k] = i;
    }

    private static long cellKey(double x, double y, double z) {
        return cellKey((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    private static long cellKey(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    static MeshData hemFold(MeshData mesh, IntPredicate hem, double thickness, double depth) {
        float[] p = mesh.points();
        float[] nrm = mesh.normals();
        float[] tex = mesh.texCoords();
        int[] f = mesh.faces();
        int n = mesh.pointCount();
        Map<Long, int[]> edgeUse = new HashMap<>();
        int[] texOf = new int[n];
        int[] normalOf = new int[n];
        Arrays.fill(texOf, -1);
        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>(6));
        }
        for (int t = 0; t < mesh.faceCount(); t++) {
            for (int e = 0; e < 3; e++) {
                int a = f[t * MeshData.FACE_STRIDE + e * 3];
                int b = f[t * MeshData.FACE_STRIDE + ((e + 1) % 3) * 3];
                texOf[a] = f[t * MeshData.FACE_STRIDE + e * 3 + 2];
                normalOf[a] = f[t * MeshData.FACE_STRIDE + e * 3 + 1];
                adj.get(a).add(b);
                int[] use = edgeUse.computeIfAbsent(key(a, b), k -> new int[]{0, a, b});
                use[0]++;
            }
        }
        List<int[]> edges = new ArrayList<>();
        boolean[] onBoundary = new boolean[n];
        edgeUse.values().forEach(u -> {
            if (u[0] == 1 && hem.test(u[1]) && hem.test(u[2])) {
                edges.add(new int[]{u[1], u[2]});
                onBoundary[u[1]] = true;
                onBoundary[u[2]] = true;
            }
        });
        if (edges.isEmpty()) {
            return mesh;
        }
        int[] lip = new int[n];
        int[] inner = new int[n];
        Arrays.fill(lip, -1);
        List<float[]> newPts = new ArrayList<>();
        List<float[]> newNormals = new ArrayList<>();
        int pointBase = n;
        int normalBase = nrm.length / 3;
        for (int i = 0; i < n; i++) {
            if (!onBoundary[i]) {
                continue;
            }
            int ni = normalOf[i] * 3;
            double nx = nrm[ni];
            double ny = nrm[ni + 1];
            double nz = nrm[ni + 2];
            double ux = 0;
            double uy = 0;
            double uz = 0;
            for (int j : adj.get(i)) {
                if (!onBoundary[j]) {
                    ux += p[j * 3] - p[i * 3];
                    uy += p[j * 3 + 1] - p[i * 3 + 1];
                    uz += p[j * 3 + 2] - p[i * 3 + 2];
                }
            }
            double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
            if (ul < 1e-9) {
                ux = 0;
                uy = -1;
                uz = 0;
                ul = 1;
            }
            ux /= ul;
            uy /= ul;
            uz /= ul;
            float ax = (float) (p[i * 3] - nx * thickness);
            float ay = (float) (p[i * 3 + 1] - ny * thickness);
            float az = (float) (p[i * 3 + 2] - nz * thickness);
            lip[i] = pointBase + newPts.size();
            newPts.add(new float[]{ax, ay, az});
            int at = i;
            double walked = 0;
            while (walked < depth) {
                int next = -1;
                double best = 0;
                for (int j : adj.get(at)) {
                    double dx = p[j * 3] - p[at * 3];
                    double dy = p[j * 3 + 1] - p[at * 3 + 1];
                    double dz = p[j * 3 + 2] - p[at * 3 + 2];
                    double along = dx * ux + dy * uy + dz * uz;
                    if (along > best && !onBoundary[j]) {
                        best = along;
                        next = j;
                    }
                }
                if (next < 0) {
                    break;
                }
                walked += best;
                at = next;
            }
            int na = normalOf[at] * 3;
            inner[i] = pointBase + newPts.size();
            newPts.add(new float[]{(float) (p[at * 3] - nrm[na] * thickness), (float) (p[at * 3 + 1] - nrm[na + 1] * thickness),
                    (float) (p[at * 3 + 2] - nrm[na + 2] * thickness)});
            newNormals.add(new float[]{(float) -ux, (float) -uy, (float) -uz});
            newNormals.add(new float[]{(float) -nx, (float) -ny, (float) -nz});
        }
        float[] pts2 = Arrays.copyOf(p, p.length + newPts.size() * 3);
        for (int k = 0; k < newPts.size(); k++) {
            System.arraycopy(newPts.get(k), 0, pts2, p.length + k * 3, 3);
        }
        float[] nrm2 = Arrays.copyOf(nrm, nrm.length + newNormals.size() * 3);
        for (int k = 0; k < newNormals.size(); k++) {
            System.arraycopy(newNormals.get(k), 0, nrm2, nrm.length + k * 3, 3);
        }
        int[] faces2 = Arrays.copyOf(f, f.length + edges.size() * 4 * MeshData.FACE_STRIDE);
        int w = f.length;
        for (int[] e : edges) {
            int a = e[0];
            int b = e[1];
            int lipNa = normalBase + (lip[a] - pointBase);
            int lipNb = normalBase + (lip[b] - pointBase);
            int inNa = lipNa + 1;
            int inNb = lipNb + 1;
            w = face(faces2, w, a, lipNa, texOf[a], b, lipNb, texOf[b], lip[b], lipNb, texOf[b]);
            w = face(faces2, w, a, lipNa, texOf[a], lip[b], lipNb, texOf[b], lip[a], lipNa, texOf[a]);
            w = face(faces2, w, lip[a], inNa, texOf[a], lip[b], inNb, texOf[b], inner[b], inNb, texOf[b]);
            w = face(faces2, w, lip[a], inNa, texOf[a], inner[b], inNb, texOf[b], inner[a], inNa, texOf[a]);
        }
        return new MeshData(pts2, nrm2, tex, faces2);
    }

    private static int face(int[] out, int w, int p0, int n0, int t0, int p1, int n1, int t1, int p2, int n2, int t2) {
        out[w++] = p0;
        out[w++] = n0;
        out[w++] = t0;
        out[w++] = p1;
        out[w++] = n1;
        out[w++] = t1;
        out[w++] = p2;
        out[w++] = n2;
        out[w++] = t2;
        return w;
    }
}
