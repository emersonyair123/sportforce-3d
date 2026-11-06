package com.sportforce3d.three.procedural;

import com.sportforce3d.three.geom.GridSurface;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Tube;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.ModelData.Part;
import com.sportforce3d.three.procedural.BodyShape.Side;

import java.util.ArrayList;
import java.util.List;

/** Maniquí estilizado sin rostro: torso, cuello, cabeza, brazos con manos y piernas. */
final class MannequinGenerator {

    private MannequinGenerator() {
    }

    static List<Part> build(BodyShape b) {
        List<Part> parts = new ArrayList<>();
        parts.add(new Part("mannequin_torso", torso(b)));
        parts.add(new Part("mannequin_neck", tube(b, b.neck(), 0, 1, 32, 56, false)));
        parts.add(new Part("mannequin_head", head(b)));
        for (Side side : Side.values()) {
            String suffix = side == Side.LEFT ? "_l" : "_r";
            parts.add(new Part("mannequin_arm" + suffix, tube(b, b.arm(side), 0, 1, 170, 48, false)));
            parts.add(new Part("mannequin_leg" + suffix, tube(b, b.leg(side), 0, 1, 170, 56, false)));
        }
        return parts;
    }

    private static MeshData torso(BodyShape b) {
        int rows = 170;
        int cols = 128;
        GridSurface g = new GridSurface(rows, cols, true);
        for (int r = 0; r < rows; r++) {
            double t = (double) r / (rows - 1);
            // Filas más densas arriba, donde la pendiente de los hombros cambia rápido.
            double y = BodyShape.TORSO_BOTTOM
                    + (BodyShape.TORSO_TOP - BodyShape.TORSO_BOTTOM) * (1 - Math.pow(1 - t, 1.3));
            for (int c = 0; c < cols; c++) {
                double phi = 2 * Math.PI * c / cols;
                Vec3 p = b.torsoPoint(phi, y, 0);
                g.set(r, c, b.out(p), b.out(p).sub(b.out(b.torsoAxis(y))).add(new Vec3(0, -0.02 * (1 - t), 0)));
            }
        }
        return g.build();
    }

    private static MeshData head(BodyShape b) {
        int rows = 64;
        int cols = 72;
        GridSurface g = new GridSurface(rows, cols, true);
        double y0 = BodyShape.HEAD_RX.minX();
        double y1 = BodyShape.HEAD_RX.maxX();
        Vec3 center = new Vec3(0, 1.72, 0.01);
        for (int r = 0; r < rows; r++) {
            double t = (double) r / (rows - 1);
            // Más filas en los polos para que cierren redondos.
            double y = y0 + (y1 - y0) * (0.5 - 0.5 * Math.cos(Math.PI * t));
            double rx = BodyShape.HEAD_RX.at(y);
            double rzf = BodyShape.HEAD_RZ_FRONT.at(y);
            double rzb = BodyShape.HEAD_RZ_BACK.at(y);
            double cz = BodyShape.HEAD_CZ.at(y);
            for (int c = 0; c < cols; c++) {
                double phi = 2 * Math.PI * c / cols;
                double s = Math.sin(phi);
                double co = Math.cos(phi);
                double rz = co >= 0 ? rzf : rzb;
                double e = 2.0 / 2.15;
                double x = rx * Math.signum(s) * Math.pow(Math.abs(s), e);
                double z = cz + rz * Math.signum(co) * Math.pow(Math.abs(co), e);
                Vec3 p = new Vec3(x, y, z);
                g.set(r, c, b.outHead(p), b.outHead(p).sub(b.outHead(center)));
            }
        }
        return g.build();
    }

    /** Malla de un tubo entre las fracciones {@code f0}..{@code f1} con espesor extra 0. */
    static MeshData tube(BodyShape b, Tube tube, double f0, double f1, int rows, int cols, boolean head) {
        GridSurface g = new GridSurface(rows, cols, true);
        for (int r = 0; r < rows; r++) {
            double t = (double) r / (rows - 1);
            double f = f0 + (f1 - f0) * t;
            Vec3 center = tube.center(f);
            for (int c = 0; c < cols; c++) {
                double angle = 2 * Math.PI * c / cols;
                Vec3 p = tube.point(f, angle, 0);
                Vec3 hint = p.sub(center);
                if (t > 0.985) {
                    hint = hint.add(tube.tangent(f).mul(0.05));
                }
                g.set(r, c, head ? b.outHead(p) : b.out(p), hint);
            }
        }
        return g.build();
    }
}
