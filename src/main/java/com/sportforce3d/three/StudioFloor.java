package com.sportforce3d.three;

import com.sportforce3d.three.geom.GridSurface;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Vec3;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;

import java.util.List;

public final class StudioFloor {

    static final double RADIUS = 1.6;
    private static final int TEX = 1024;

    private StudioFloor() {
    }

    public static MeshView create(List<float[]> contactPoints, double figureCenterX, double figureCenterZ,
                                  Color background, Color floorCenter) {
        MeshView view = new MeshView(disc().toTriangleMesh());
        PhongMaterial material = new PhongMaterial(Color.BLACK);
        material.setSpecularColor(Color.BLACK);
        material.setSelfIlluminationMap(texture(contactPoints, figureCenterX, figureCenterZ, background, floorCenter));
        view.setMaterial(material);
        return view;
    }

    private static MeshData disc() {
        int rows = 40;
        int cols = 128;
        GridSurface g = new GridSurface(rows, cols, true);
        for (int r = 0; r < rows; r++) {
            double rad = RADIUS * (r == 0 ? 0.0005 : (double) r / (rows - 1));
            for (int c = 0; c < cols; c++) {
                double a = 2 * Math.PI * c / cols;
                g.set(r, c, new Vec3(rad * Math.sin(a), 0, rad * Math.cos(a)), Vec3.UP);
            }
        }
        return g.build(1, (r, c) -> 0, (r, c, island, out) -> {
            double rad = (double) r / (rows - 1);
            double a = 2 * Math.PI * c / cols;
            out[0] = (float) (0.5 + 0.5 * rad * Math.sin(a));
            out[1] = (float) (0.5 + 0.5 * rad * -Math.cos(a));
        });
    }

    private static WritableImage texture(List<float[]> pts, double cx, double cz, Color bg, Color center) {
        double[] occ = new double[TEX * TEX];
        for (float[] p : pts) {
            double height = -p[1];
            if (height > 0.03) {
                continue;
            }
            double weight = 1 - height / 0.03;
            int px = (int) Math.round((0.5 + p[0] / (2 * RADIUS)) * (TEX - 1));
            int pz = (int) Math.round((0.5 + p[2] / (2 * RADIUS)) * (TEX - 1));
            if (px >= 0 && px < TEX && pz >= 0 && pz < TEX) {
                occ[pz * TEX + px] += weight;
            }
        }
        double[] contact = blur(occ, 7);
        double[] contactWide = blur(occ, 22);
        double maxC = 1e-9;
        double maxW = 1e-9;
        for (int i = 0; i < occ.length; i++) {
            maxC = Math.max(maxC, contact[i]);
            maxW = Math.max(maxW, contactWide[i]);
        }

        WritableImage img = new WritableImage(TEX, TEX);
        PixelWriter w = img.getPixelWriter();
        for (int y = 0; y < TEX; y++) {
            for (int x = 0; x < TEX; x++) {
                double u = (double) x / (TEX - 1) - 0.5;
                double v = (double) y / (TEX - 1) - 0.5;
                double r = Math.sqrt(u * u + v * v) * 2;
                double glow = Math.pow(Math.max(0, 1 - r / 0.82), 1.6);
                Color base = bg.interpolate(center, glow);
                double sx = (u * 2 * RADIUS - cx) / 0.34;
                double sz = (v * 2 * RADIUS - cz) / 0.24;
                double ambientShadow = 0.38 * Math.exp(-(sx * sx + sz * sz) * 1.4);
                double c = Math.min(1, contact[y * TEX + x] / maxC * 1.25);
                double cw = Math.min(1, contactWide[y * TEX + x] / maxW * 1.1);
                double shadow = Math.min(0.92, ambientShadow + 0.75 * c + 0.35 * cw);
                Color out = base.interpolate(Color.BLACK, shadow);
                w.setColor(x, y, out);
            }
        }
        return img;
    }

    static double[] blur(double[] src, int radius) {
        double sigma = radius / 2.0;
        double[] k = new double[radius * 2 + 1];
        double sum = 0;
        for (int i = -radius; i <= radius; i++) {
            k[i + radius] = Math.exp(-(i * i) / (2 * sigma * sigma));
            sum += k[i + radius];
        }
        for (int i = 0; i < k.length; i++) {
            k[i] /= sum;
        }
        double[] tmp = new double[src.length];
        double[] out = new double[src.length];
        for (int y = 0; y < TEX; y++) {
            for (int x = 0; x < TEX; x++) {
                double acc = 0;
                for (int i = -radius; i <= radius; i++) {
                    int xx = Math.min(TEX - 1, Math.max(0, x + i));
                    acc += src[y * TEX + xx] * k[i + radius];
                }
                tmp[y * TEX + x] = acc;
            }
        }
        for (int y = 0; y < TEX; y++) {
            for (int x = 0; x < TEX; x++) {
                double acc = 0;
                for (int i = -radius; i <= radius; i++) {
                    int yy = Math.min(TEX - 1, Math.max(0, y + i));
                    acc += tmp[yy * TEX + x] * k[i + radius];
                }
                out[y * TEX + x] = acc;
            }
        }
        return out;
    }
}
