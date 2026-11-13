package com.sportforce3d.three.geom;

public final class GridSurface {
    @FunctionalInterface
    public interface Uv {
        void at(int r, int c, int island, float[] out);
    }

    @FunctionalInterface
    public interface Island {
        int of(int r, int c);
    }

    private final int rows;
    private final int cols;
    private final boolean closed;
    private final double[] p;
    private final double[] h;

    public GridSurface(int rows, int cols, boolean closedU) {
        if (rows < 2 || cols < 2) {
            throw new IllegalArgumentException("Rejilla demasiado pequeña");
        }
        this.rows = rows;
        this.cols = cols;
        this.closed = closedU;
        this.p = new double[rows * cols * 3];
        this.h = new double[rows * cols * 3];
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public void set(int r, int c, Vec3 point, Vec3 outwardHint) {
        int i = (r * cols + c) * 3;
        p[i] = point.x();
        p[i + 1] = point.y();
        p[i + 2] = point.z();
        h[i] = outwardHint.x();
        h[i + 1] = outwardHint.y();
        h[i + 2] = outwardHint.z();
    }

    public Vec3 point(int r, int c) {
        int i = (r * cols + c) * 3;
        return new Vec3(p[i], p[i + 1], p[i + 2]);
    }

    private Vec3 hint(int r, int c) {
        int i = (r * cols + c) * 3;
        return new Vec3(h[i], h[i + 1], h[i + 2]);
    }

    public MeshData build() {
        int colsT = closed ? cols : cols - 1;
        return build(1, (r, c) -> 0, (r, c, island, out) -> {
            out[0] = (float) c / colsT;
            out[1] = (float) r / (rows - 1);
        });
    }

    public MeshData build(int islands, Island island, Uv uv) {
        int n = rows * cols;
        Vec3[] normals = new Vec3[n];
        boolean[] degenerate = new boolean[n];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Vec3 tu = point(r, colNext(c)).sub(point(r, colPrev(c)));
                Vec3 tv = point(Math.min(rows - 1, r + 1), c).sub(point(Math.max(0, r - 1), c));
                Vec3 nrm = tu.cross(tv);
                if (nrm.length() < 1e-12) {
                    degenerate[r * cols + c] = true;
                    normals[r * cols + c] = hint(r, c).normalize();
                    continue;
                }
                nrm = nrm.normalize();
                if (nrm.dot(hint(r, c)) < 0) {
                    nrm = nrm.mul(-1);
                }
                normals[r * cols + c] = nrm;
            }
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!degenerate[r * cols + c]) {
                    continue;
                }
                int nr = r == 0 ? 1 : r == rows - 1 ? rows - 2 : r;
                Vec3 sum = Vec3.ZERO;
                for (int k = 0; k < cols; k++) {
                    if (!degenerate[nr * cols + k]) {
                        sum = sum.add(normals[nr * cols + k]);
                    }
                }
                Vec3 avg = sum.normalize();
                normals[r * cols + c] = avg.length() > 0 ? avg : hint(r, c).normalize();
            }
        }

        float[] points = new float[n * 3];
        float[] norms = new float[n * 3];
        for (int i = 0; i < n; i++) {
            points[i * 3] = (float) p[i * 3];
            points[i * 3 + 1] = (float) -p[i * 3 + 1];
            points[i * 3 + 2] = (float) -p[i * 3 + 2];
            Vec3 nm = normals[i];
            norms[i * 3] = (float) nm.x();
            norms[i * 3 + 1] = (float) -nm.y();
            norms[i * 3 + 2] = (float) -nm.z();
        }

        int colsT = closed ? cols + 1 : cols;
        float[] tex = new float[islands * rows * colsT * 2];
        float[] tmp = new float[2];
        for (int is = 0; is < islands; is++) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < colsT; c++) {
                    uv.at(r, c, is, tmp);
                    int t = ((is * rows + r) * colsT + c) * 2;
                    tex[t] = tmp[0];
                    tex[t + 1] = tmp[1];
                }
            }
        }

        int quadCols = closed ? cols : cols - 1;
        int[] faces = new int[(rows - 1) * quadCols * 2 * MeshData.FACE_STRIDE];
        int f = 0;
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < quadCols; c++) {
                int is = island.of(r, c);
                int c1 = c + 1;
                int pc1 = c1 % cols;
                int a = r * cols + c;
                int b = (r + 1) * cols + c;
                int cc = (r + 1) * cols + pc1;
                int d = r * cols + pc1;
                int ta = (is * rows + r) * colsT + c;
                int tb = (is * rows + r + 1) * colsT + c;
                int tc = (is * rows + r + 1) * colsT + c1;
                int td = (is * rows + r) * colsT + c1;
                f = triangle(faces, f, normals, a, ta, b, tb, cc, tc);
                f = triangle(faces, f, normals, a, ta, cc, tc, d, td);
            }
        }
        return new MeshData(points, norms, tex, faces);
    }

    private int triangle(int[] faces, int f, Vec3[] normals, int a, int ta, int b, int tb, int c, int tc) {
        Vec3 pa = point(a / cols, a % cols);
        Vec3 pb = point(b / cols, b % cols);
        Vec3 pc = point(c / cols, c % cols);
        Vec3 geometric = pb.sub(pa).cross(pc.sub(pa));
        Vec3 smooth = normals[a].add(normals[b]).add(normals[c]);
        boolean keep = geometric.dot(smooth) >= 0;
        if (!FRONT_IS_CCW_IN_MODEL) {
            keep = !keep;
        }
        int p1 = keep ? b : c;
        int t1 = keep ? tb : tc;
        int p2 = keep ? c : b;
        int t2 = keep ? tc : tb;
        faces[f++] = a;
        faces[f++] = a;
        faces[f++] = ta;
        faces[f++] = p1;
        faces[f++] = p1;
        faces[f++] = t1;
        faces[f++] = p2;
        faces[f++] = p2;
        faces[f++] = t2;
        return f;
    }

    static final boolean FRONT_IS_CCW_IN_MODEL = true;

    private int colNext(int c) {
        if (c + 1 < cols) {
            return c + 1;
        }
        return closed ? 0 : c;
    }

    private int colPrev(int c) {
        if (c > 0) {
            return c - 1;
        }
        return closed ? cols - 1 : c;
    }
}
