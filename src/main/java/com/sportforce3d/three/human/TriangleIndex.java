package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Vec3;

import java.util.Arrays;
import java.util.List;

/**
 * Triángulos de un cuerpo en una rejilla uniforme, en espacio del modelo (metros, Y arriba, +Z frente). Responde
 * "¿a qué distancia sale este rayo de la piel?" y "¿cuál es el punto de piel más cercano?". Inmutable salvo
 * {@link Query}, que lleva sus marcas propias: usar una por hilo.
 */
final class TriangleIndex {

    private final double[] v;
    private final int[] tri;
    private final double[] faceNormal;
    private final double cell;
    private final double minX;
    private final double minY;
    private final double minZ;
    private final int nx;
    private final int ny;
    private final int nz;
    private final int[] cellStart;
    private final int[] cellTris;

    private TriangleIndex(double[] v, int[] tri, double cell) {
        this.v = v;
        this.tri = tri;
        this.cell = cell;
        int triCount = tri.length / 3;
        faceNormal = new double[triCount * 3];
        double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (int i = 0; i < v.length; i += 3) {
            for (int k = 0; k < 3; k++) {
                lo[k] = Math.min(lo[k], v[i + k]);
                hi[k] = Math.max(hi[k], v[i + k]);
            }
        }
        minX = lo[0] - cell;
        minY = lo[1] - cell;
        minZ = lo[2] - cell;
        nx = (int) Math.ceil((hi[0] - minX) / cell) + 2;
        ny = (int) Math.ceil((hi[1] - minY) / cell) + 2;
        nz = (int) Math.ceil((hi[2] - minZ) / cell) + 2;

        // Dos pasadas: contar triángulos por celda y luego llenar (sin listas por celda).
        int[] count = new int[nx * ny * nz + 1];
        for (int pass = 0; pass < 2; pass++) {
            int[] fill = pass == 1 ? Arrays.copyOf(count, count.length) : null;
            for (int t = 0; t < triCount; t++) {
                int a = tri[t * 3] * 3;
                int b = tri[t * 3 + 1] * 3;
                int c = tri[t * 3 + 2] * 3;
                if (pass == 0) {
                    computeNormal(t, a, b, c);
                }
                int x0 = cx(Math.min(v[a], Math.min(v[b], v[c])));
                int x1 = cx(Math.max(v[a], Math.max(v[b], v[c])));
                int y0 = cy(Math.min(v[a + 1], Math.min(v[b + 1], v[c + 1])));
                int y1 = cy(Math.max(v[a + 1], Math.max(v[b + 1], v[c + 1])));
                int z0 = cz(Math.min(v[a + 2], Math.min(v[b + 2], v[c + 2])));
                int z1 = cz(Math.max(v[a + 2], Math.max(v[b + 2], v[c + 2])));
                for (int x = x0; x <= x1; x++) {
                    for (int y = y0; y <= y1; y++) {
                        for (int z = z0; z <= z1; z++) {
                            int id = (x * ny + y) * nz + z;
                            if (pass == 0) {
                                count[id + 1]++;
                            } else {
                                cellTrisHolder[fill[id]++] = t;
                            }
                        }
                    }
                }
            }
            if (pass == 0) {
                for (int i = 1; i < count.length; i++) {
                    count[i] += count[i - 1];
                }
                cellTrisHolder = new int[count[count.length - 1]];
            }
        }
        cellStart = count;
        cellTris = cellTrisHolder;
        cellTrisHolder = null;
    }

    /** Solo durante la construcción. */
    private int[] cellTrisHolder;

    /** Índice de las mallas indicadas (en coordenadas de JavaFX, como {@link MeshData}). */
    static TriangleIndex of(List<MeshData> meshes, double cell) {
        int points = 0;
        int faces = 0;
        for (MeshData m : meshes) {
            points += m.pointCount();
            faces += m.faceCount();
        }
        double[] v = new double[points * 3];
        int[] tri = new int[faces * 3];
        int pb = 0;
        int fb = 0;
        for (MeshData m : meshes) {
            float[] p = m.points();
            for (int i = 0; i < p.length; i += 3) {
                v[pb * 3 + i] = p[i];
                v[pb * 3 + i + 1] = -p[i + 1];
                v[pb * 3 + i + 2] = -p[i + 2];
            }
            int[] f = m.faces();
            for (int k = 0; k < m.faceCount(); k++) {
                for (int c = 0; c < 3; c++) {
                    tri[(fb + k) * 3 + c] = pb + f[k * MeshData.FACE_STRIDE + c * 3];
                }
            }
            pb += m.pointCount();
            fb += m.faceCount();
        }
        return new TriangleIndex(v, tri, cell);
    }

    private void computeNormal(int t, int a, int b, int c) {
        double ux = v[b] - v[a];
        double uy = v[b + 1] - v[a + 1];
        double uz = v[b + 2] - v[a + 2];
        double wx = v[c] - v[a];
        double wy = v[c + 1] - v[a + 1];
        double wz = v[c + 2] - v[a + 2];
        double x = uy * wz - uz * wy;
        double y = uz * wx - ux * wz;
        double z = ux * wy - uy * wx;
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len > 0) {
            faceNormal[t * 3] = x / len;
            faceNormal[t * 3 + 1] = y / len;
            faceNormal[t * 3 + 2] = z / len;
        }
    }

    private int cx(double x) {
        return Math.max(0, Math.min(nx - 1, (int) ((x - minX) / cell)));
    }

    private int cy(double y) {
        return Math.max(0, Math.min(ny - 1, (int) ((y - minY) / cell)));
    }

    private int cz(double z) {
        return Math.max(0, Math.min(nz - 1, (int) ((z - minZ) / cell)));
    }

    double[] vertices() {
        return v;
    }

    /** Índices de vértice de cada triángulo (tres por triángulo). */
    int[] triangles() {
        return tri;
    }

    /** Punto de piel más cercano y la normal de su cara (hacia afuera, por el sentido de los triángulos). */
    record Hit(Vec3 point, Vec3 normal, double distance) {
    }

    Query query() {
        return new Query();
    }

    /** Consultas con marcas propias (para no probar dos veces el mismo triángulo). No es seguro entre hilos. */
    final class Query {
        private final int[] mark = new int[tri.length / 3];
        private int stamp;

        /**
         * Distancia a la que el rayo {@code origin + t·dir} (dir unitario) cruza la primera cara, hasta
         * {@code maxDist}; {@code NaN} si no cruza ninguna.
         */
        double ray(Vec3 origin, Vec3 dir, double maxDist) {
            next();
            double best = Double.NaN;
            int lastCell = -1;
            double step = cell * 0.5;
            for (double s = 0; s <= maxDist + step; s += step) {
                if (!Double.isNaN(best) && s - step > best) {
                    break;
                }
                int id = cellId(origin.x() + dir.x() * s, origin.y() + dir.y() * s, origin.z() + dir.z() * s);
                if (id == lastCell) {
                    continue;
                }
                lastCell = id;
                // Las celdas vecinas cubren el paso diagonal entre muestras.
                for (int n : neighbors(id)) {
                    for (int k = cellStart[n]; k < cellStart[n + 1]; k++) {
                        int t = cellTris[k];
                        if (mark[t] == stamp) {
                            continue;
                        }
                        mark[t] = stamp;
                        double d = intersect(t, origin, dir);
                        if (d > 1e-7 && d <= maxDist && (Double.isNaN(best) || d < best)) {
                            best = d;
                        }
                    }
                }
            }
            return best;
        }

        /** Punto de piel más cercano a {@code p} dentro de {@code radius}; null si no hay ninguno. */
        Hit nearest(Vec3 p, double radius) {
            next();
            int x0 = cx(p.x() - radius);
            int x1 = cx(p.x() + radius);
            int y0 = cy(p.y() - radius);
            int y1 = cy(p.y() + radius);
            int z0 = cz(p.z() - radius);
            int z1 = cz(p.z() + radius);
            double best = radius * radius;
            double[] q = new double[3];
            double[] bestQ = null;
            int bestT = -1;
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        int id = (x * ny + y) * nz + z;
                        for (int k = cellStart[id]; k < cellStart[id + 1]; k++) {
                            int t = cellTris[k];
                            if (mark[t] == stamp) {
                                continue;
                            }
                            mark[t] = stamp;
                            closest(t, p, q);
                            double dx = q[0] - p.x();
                            double dy = q[1] - p.y();
                            double dz = q[2] - p.z();
                            double d2 = dx * dx + dy * dy + dz * dz;
                            if (d2 < best) {
                                best = d2;
                                bestQ = q.clone();
                                bestT = t;
                            }
                        }
                    }
                }
            }
            if (bestT < 0) {
                return null;
            }
            return new Hit(new Vec3(bestQ[0], bestQ[1], bestQ[2]),
                    new Vec3(faceNormal[bestT * 3], faceNormal[bestT * 3 + 1], faceNormal[bestT * 3 + 2]),
                    Math.sqrt(best));
        }

        private void next() {
            stamp++;
            if (stamp == Integer.MAX_VALUE) {
                Arrays.fill(mark, 0);
                stamp = 1;
            }
        }

        private final int[] neighborBuf = new int[27];
        private int neighborCount;

        private int[] neighbors(int id) {
            int z = id % nz;
            int y = (id / nz) % ny;
            int x = id / (nz * ny);
            neighborCount = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int xx = x + dx;
                        int yy = y + dy;
                        int zz = z + dz;
                        if (xx >= 0 && xx < nx && yy >= 0 && yy < ny && zz >= 0 && zz < nz) {
                            neighborBuf[neighborCount++] = (xx * ny + yy) * nz + zz;
                        }
                    }
                }
            }
            return neighborCount == 27 ? neighborBuf : Arrays.copyOf(neighborBuf, neighborCount);
        }
    }

    private int cellId(double x, double y, double z) {
        return (cx(x) * ny + cy(y)) * nz + cz(z);
    }

    /** Möller–Trumbore: distancia a lo largo del rayo o -1. */
    private double intersect(int t, Vec3 o, Vec3 d) {
        int a = tri[t * 3] * 3;
        int b = tri[t * 3 + 1] * 3;
        int c = tri[t * 3 + 2] * 3;
        double e1x = v[b] - v[a];
        double e1y = v[b + 1] - v[a + 1];
        double e1z = v[b + 2] - v[a + 2];
        double e2x = v[c] - v[a];
        double e2y = v[c + 1] - v[a + 1];
        double e2z = v[c + 2] - v[a + 2];
        double px = d.y() * e2z - d.z() * e2y;
        double py = d.z() * e2x - d.x() * e2z;
        double pz = d.x() * e2y - d.y() * e2x;
        double det = e1x * px + e1y * py + e1z * pz;
        if (Math.abs(det) < 1e-14) {
            return -1;
        }
        double inv = 1 / det;
        double tx = o.x() - v[a];
        double ty = o.y() - v[a + 1];
        double tz = o.z() - v[a + 2];
        double u = (tx * px + ty * py + tz * pz) * inv;
        if (u < 0 || u > 1) {
            return -1;
        }
        double qx = ty * e1z - tz * e1y;
        double qy = tz * e1x - tx * e1z;
        double qz = tx * e1y - ty * e1x;
        double w = (d.x() * qx + d.y() * qy + d.z() * qz) * inv;
        if (w < 0 || u + w > 1) {
            return -1;
        }
        return (e2x * qx + e2y * qy + e2z * qz) * inv;
    }

    /** Punto del triángulo más cercano a p (Ericson, Real-Time Collision Detection 5.1.5). */
    private void closest(int t, Vec3 p, double[] out) {
        int ia = tri[t * 3] * 3;
        int ib = tri[t * 3 + 1] * 3;
        int ic = tri[t * 3 + 2] * 3;
        double ax = v[ia], ay = v[ia + 1], az = v[ia + 2];
        double bx = v[ib], by = v[ib + 1], bz = v[ib + 2];
        double cx = v[ic], cy = v[ic + 1], cz = v[ic + 2];
        double abx = bx - ax, aby = by - ay, abz = bz - az;
        double acx = cx - ax, acy = cy - ay, acz = cz - az;
        double apx = p.x() - ax, apy = p.y() - ay, apz = p.z() - az;
        double d1 = abx * apx + aby * apy + abz * apz;
        double d2 = acx * apx + acy * apy + acz * apz;
        if (d1 <= 0 && d2 <= 0) {
            set(out, ax, ay, az);
            return;
        }
        double bpx = p.x() - bx, bpy = p.y() - by, bpz = p.z() - bz;
        double d3 = abx * bpx + aby * bpy + abz * bpz;
        double d4 = acx * bpx + acy * bpy + acz * bpz;
        if (d3 >= 0 && d4 <= d3) {
            set(out, bx, by, bz);
            return;
        }
        double vc = d1 * d4 - d3 * d2;
        if (vc <= 0 && d1 >= 0 && d3 <= 0) {
            double k = d1 / (d1 - d3);
            set(out, ax + k * abx, ay + k * aby, az + k * abz);
            return;
        }
        double cpx = p.x() - cx, cpy = p.y() - cy, cpz = p.z() - cz;
        double d5 = abx * cpx + aby * cpy + abz * cpz;
        double d6 = acx * cpx + acy * cpy + acz * cpz;
        if (d6 >= 0 && d5 <= d6) {
            set(out, cx, cy, cz);
            return;
        }
        double vb = d5 * d2 - d1 * d6;
        if (vb <= 0 && d2 >= 0 && d6 <= 0) {
            double k = d2 / (d2 - d6);
            set(out, ax + k * acx, ay + k * acy, az + k * acz);
            return;
        }
        double va = d3 * d6 - d5 * d4;
        if (va <= 0 && (d4 - d3) >= 0 && (d5 - d6) >= 0) {
            double k = (d4 - d3) / ((d4 - d3) + (d5 - d6));
            set(out, bx + k * (cx - bx), by + k * (cy - by), bz + k * (cz - bz));
            return;
        }
        double denom = 1 / (va + vb + vc);
        double vv = vb * denom;
        double ww = vc * denom;
        set(out, ax + abx * vv + acx * ww, ay + aby * vv + acy * ww, az + abz * vv + acz * ww);
    }

    private static void set(double[] out, double x, double y, double z) {
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }
}
