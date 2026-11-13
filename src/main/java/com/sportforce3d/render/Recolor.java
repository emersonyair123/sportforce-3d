package com.sportforce3d.render;

import java.util.Map;

public final class Recolor {
    static final float SHADOW = 0.85f;
    static final float DETAIL = 0.85f;
    private static final float[] UNIT = unit();

    private Recolor() {
    }

    private static float[] unit() {
        float[] t = new float[256];
        for (int i = 0; i < 256; i++) {
            t[i] = i / 255f;
        }
        return t;
    }

    public static ArgbImage compose(ModelLayers layers, Map<String, Integer> colors, ArgbImage overlay) {
        int w = layers.width();
        int h = layers.height();
        int n = w * h;
        int[] base = layers.base().pixels();
        float[] r = new float[n];
        float[] g = new float[n];
        float[] b = new float[n];
        for (int i = 0; i < n; i++) {
            int c = base[i];
            r[i] = UNIT[(c >> 16) & 0xFF];
            g[i] = UNIT[(c >> 8) & 0xFF];
            b[i] = UNIT[c & 0xFF];
        }

        for (Map.Entry<String, Integer> e : colors.entrySet()) {
            Mask m = layers.zones().get(e.getKey());
            if (m == null || m.isEmpty()) {
                continue;
            }
            int c = e.getValue();
            float kr = 1 - UNIT[(c >> 16) & 0xFF];
            float kg = 1 - UNIT[(c >> 8) & 0xFF];
            float kb = 1 - UNIT[c & 0xFF];
            byte[] alpha = m.alpha();
            for (int y = 0; y < m.height(); y++) {
                int src = y * m.width();
                int dst = (m.y() + y) * w + m.x();
                for (int x = 0; x < m.width(); x++) {
                    int av = alpha[src + x] & 0xFF;
                    if (av == 0) {
                        continue;
                    }
                    float a = UNIT[av];
                    int i = dst + x;
                    r[i] *= 1 - a * kr;
                    g[i] *= 1 - a * kg;
                    b[i] *= 1 - a * kb;
                }
            }
        }

        Mask cam = layers.zone(RealisticModel.CAMISETA);
        if (overlay != null && !cam.isEmpty()) {
            if (overlay.width() != w || overlay.height() != h) {
                throw new IllegalArgumentException("El diseño debe medir lo mismo que la foto");
            }
            int[] over = overlay.pixels();
            byte[] alpha = cam.alpha();
            for (int y = 0; y < cam.height(); y++) {
                int src = y * cam.width();
                int dst = (cam.y() + y) * w + cam.x();
                for (int x = 0; x < cam.width(); x++) {
                    int av = alpha[src + x] & 0xFF;
                    if (av == 0) {
                        continue;
                    }
                    float aCam = UNIT[av];
                    int i = dst + x;
                    int o = over[i];
                    float ao = UNIT[o >>> 24] * aCam;
                    if (ao > 0) {
                        r[i] = r[i] * (1 - ao) + UNIT[(o >> 16) & 0xFF] * ao;
                        g[i] = g[i] * (1 - ao) + UNIT[(o >> 8) & 0xFF] * ao;
                        b[i] = b[i] * (1 - ao) + UNIT[o & 0xFF] * ao;
                    }
                    float k = aCam * SHADOW;
                    int c = base[i];
                    r[i] *= 1 - k * (1 - UNIT[(c >> 16) & 0xFF]);
                    g[i] *= 1 - k * (1 - UNIT[(c >> 8) & 0xFF]);
                    b[i] *= 1 - k * (1 - UNIT[c & 0xFF]);
                }
            }
        }

        Mask dg = layers.detailGray();
        Mask da = layers.detailAlpha();
        if (!da.isEmpty()) {
            byte[] gray = dg.alpha();
            byte[] alpha = da.alpha();
            for (int y = 0; y < da.height(); y++) {
                int src = y * da.width();
                int dst = (da.y() + y) * w + da.x();
                for (int x = 0; x < da.width(); x++) {
                    int av = alpha[src + x] & 0xFF;
                    if (av == 0) {
                        continue;
                    }
                    float ad = UNIT[av] * DETAIL;
                    float d = UNIT[gray[src + x] & 0xFF];
                    int i = dst + x;
                    r[i] = overlayBlend(r[i], d, ad);
                    g[i] = overlayBlend(g[i], d, ad);
                    b[i] = overlayBlend(b[i], d, ad);
                }
            }
        }

        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = (base[i] & 0xFF000000) | (to255(r[i]) << 16) | (to255(g[i]) << 8) | to255(b[i]);
        }
        return new ArgbImage(w, h, out);
    }

    private static float overlayBlend(float res, float d, float ad) {
        float ov = res < 0.5f ? 2 * res * d : 1 - 2 * (1 - res) * (1 - d);
        return res * (1 - ad) + ov * ad;
    }

    private static int to255(float v) {
        int i = (int) (v * 255f + 0.5f);
        return i < 0 ? 0 : Math.min(i, 255);
    }

    public static ArgbImage downscale(ArgbImage src, int dw, int dh) {
        int sw = src.width();
        int sh = src.height();
        if (dw <= 0 || dh <= 0 || dw > sw || dh > sh) {
            throw new IllegalArgumentException("Solo reduce: " + sw + "×" + sh + " → " + dw + "×" + dh);
        }
        int[] p = src.pixels();
        int[] out = new int[dw * dh];
        for (int dy = 0; dy < dh; dy++) {
            int y0 = (int) ((long) dy * sh / dh);
            int y1 = Math.max(y0 + 1, (int) ((long) (dy + 1) * sh / dh));
            for (int dx = 0; dx < dw; dx++) {
                int x0 = (int) ((long) dx * sw / dw);
                int x1 = Math.max(x0 + 1, (int) ((long) (dx + 1) * sw / dw));
                long sa = 0;
                long sr = 0;
                long sg = 0;
                long sb = 0;
                for (int y = y0; y < y1; y++) {
                    int row = y * sw;
                    for (int x = x0; x < x1; x++) {
                        int c = p[row + x];
                        int a = c >>> 24;
                        sa += a;
                        sr += (long) ((c >> 16) & 0xFF) * a;
                        sg += (long) ((c >> 8) & 0xFF) * a;
                        sb += (long) (c & 0xFF) * a;
                    }
                }
                int count = (y1 - y0) * (x1 - x0);
                int argb = 0;
                if (sa > 0) {
                    int a = (int) ((sa + count / 2) / count);
                    argb = (a << 24)
                            | (int) ((sr + sa / 2) / sa) << 16
                            | (int) ((sg + sa / 2) / sa) << 8
                            | (int) ((sb + sa / 2) / sa);
                }
                out[dy * dw + dx] = argb;
            }
        }
        return new ArgbImage(dw, dh, out);
    }
}
