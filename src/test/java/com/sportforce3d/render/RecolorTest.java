package com.sportforce3d.render;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecolorTest {

    private static final int W = 4;
    private static final int H = 3;

    @Test
    void whiteTimesColorIsTheColor() {
        ModelLayers layers = layers(fill(0xFFFFFFFF), Map.of("camiseta", mask(255)), empty());
        ArgbImage out = Recolor.compose(layers, Map.of("camiseta", 0x1F5FFF), null);
        assertThat(out.pixels()).containsOnly(0xFF1F5FFF);
    }

    @Test
    void multiplyKeepsTheShadowsOfThePhoto() {
        ModelLayers layers = layers(fill(0xFF808080), Map.of("camiseta", mask(255)), empty());
        ArgbImage out = Recolor.compose(layers, Map.of("camiseta", 0xFF0000), null);
        assertThat(out.pixels()).containsOnly(0xFF800000);
    }

    @Test
    void zeroAlphaLeavesThePixelUnchanged() {
        int[] base = {0xFF102030, 0xFFFFFFFF, 0xFF808080, 0xFF0B0E13, 0xFFABCDEF, 0xFF000000,
                0xFF7F7F7F, 0xFF123456, 0xFFFEDCBA, 0xFF010203, 0xFFF2F5F8, 0xFF39FF88};
        ModelLayers layers = layers(new ArgbImage(W, H, base.clone()), Map.of("camiseta", mask(0)), empty());
        ArgbImage out = Recolor.compose(layers, Map.of("camiseta", 0xD7141E), null);
        assertThat(out.pixels()).containsExactly(base);
    }

    @Test
    void partialAlphaMixesLinearly() {
        ModelLayers layers = layers(fill(0xFFFFFFFF), Map.of("camiseta", mask(51)), empty());
        ArgbImage out = Recolor.compose(layers, Map.of("camiseta", 0xFF0000), null);
        assertThat(out.pixels()).containsOnly(0xFFFFCCCC);
    }

    @Test
    void neutralOverlay128LeavesTheColorUnchanged() {
        int[] base = new int[W * H];
        for (int i = 0; i < base.length; i++) {
            int v = i * 23;
            base[i] = 0xFF000000 | (v << 16) | ((255 - v) << 8) | (v / 2);
        }
        ModelLayers layers = layers(new ArgbImage(W, H, base.clone()), Map.of(), detail(128, 255));
        ArgbImage out = Recolor.compose(layers, Map.of(), null);
        for (int i = 0; i < base.length; i++) {
            for (int shift = 0; shift <= 16; shift += 8) {
                int before = (base[i] >> shift) & 0xFF;
                int after = (out.pixels()[i] >> shift) & 0xFF;
                assertThat(after).as("píxel %d canal %d", i, shift).isCloseTo(before, org.assertj.core.data.Offset.offset(1));
            }
        }
    }

    @Test
    void darkDetailDarkensAndTransparentDetailDoesNothing() {
        ModelLayers dark = layers(fill(0xFF7F7F7F), Map.of(), detail(0, 255));
        int v = Recolor.compose(dark, Map.of(), null).pixels()[0] & 0xFF;
        assertThat(v).isEqualTo(19);

        ModelLayers none = layers(fill(0xFF808080), Map.of(), detail(0, 0));
        assertThat(Recolor.compose(none, Map.of(), null).pixels()).containsOnly(0xFF808080);
    }

    @Test
    void designIsClippedToTheShirtAndShadedByThePhoto() {
        byte[] cam = new byte[W * H];
        for (int y = 0; y < H; y++) {
            cam[y * W] = (byte) 255;
        }
        int[] base = new int[W * H];
        Arrays.fill(base, 0xFFFFFFFF);
        base[0] = 0xFF808080;
        ModelLayers layers = layers(new ArgbImage(W, H, base), Map.of("camiseta", new Mask(0, 0, W, H, cam)), empty());
        ArgbImage out = Recolor.compose(layers, Map.of(), fill(0xFFFF0000));

        assertThat(out.argb(1, 0)).isEqualTo(0xFFFFFFFF);
        assertThat(out.argb(3, 2)).isEqualTo(0xFFFFFFFF);
        assertThat(out.argb(0, 1)).isEqualTo(0xFFFF0000);
        assertThat(out.argb(0, 0)).isEqualTo(0xFF930000);
    }

    @Test
    void zonesTheModelDoesNotHaveAreIgnored() {
        ModelLayers layers = layers(fill(0xFFFFFFFF), Map.of("camiseta", mask(255)), empty());
        ArgbImage out = Recolor.compose(layers, Map.of("medias", 0x000000), null);
        assertThat(out.pixels()).containsOnly(0xFFFFFFFF);
    }

    @Test
    void masksAreCroppedToTheirBoundingBox() {
        int[] px = new int[W * H];
        px[1 * W + 2] = 0x80FFFFFF;
        px[2 * W + 3] = 0xFF000000;
        Mask m = Mask.fromAlpha(new ArgbImage(W, H, px));
        assertThat(m.x()).isEqualTo(2);
        assertThat(m.y()).isEqualTo(1);
        assertThat(m.width()).isEqualTo(2);
        assertThat(m.height()).isEqualTo(2);
        assertThat(m.at(2, 1)).isEqualTo(0x80);
        assertThat(m.at(3, 2)).isEqualTo(255);
        assertThat(m.at(0, 0)).isZero();
        assertThat(Mask.fromAlpha(new ArgbImage(W, H, new int[W * H])).isEmpty()).isTrue();
    }

    @Test
    void downscaleAveragesBlocks() {
        int[] px = {0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFFFF0000,
                0xFFFFFFFF, 0xFF000000, 0xFF0000FF, 0xFF0000FF};
        ArgbImage out = Recolor.downscale(new ArgbImage(4, 2, px), 2, 1);
        assertThat(out.pixels()).containsExactly(0xFF808080, 0xFF800080);
        assertThatThrownBy(() -> Recolor.downscale(new ArgbImage(4, 2, px), 8, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void downscaleIgnoresTransparentPixelsColor() {
        int[] px = {0x00FF0000, 0xFF00FF00};
        ArgbImage out = Recolor.downscale(new ArgbImage(2, 1, px), 1, 1);
        assertThat(out.pixels()[0]).isEqualTo(0x8000FF00);
    }

    private static ArgbImage fill(int argb) {
        int[] px = new int[W * H];
        Arrays.fill(px, argb);
        return new ArgbImage(W, H, px);
    }

    private static Mask mask(int alpha) {
        byte[] a = new byte[W * H];
        Arrays.fill(a, (byte) alpha);
        return new Mask(0, 0, W, H, a);
    }

    private static Mask[] empty() {
        return new Mask[]{Mask.EMPTY, Mask.EMPTY};
    }

    private static Mask[] detail(int gray, int alpha) {
        byte[] g = new byte[W * H];
        Arrays.fill(g, (byte) gray);
        return new Mask[]{new Mask(0, 0, W, H, g), mask(alpha)};
    }

    private static ModelLayers layers(ArgbImage base, Map<String, Mask> zones, Mask[] detail) {
        return new ModelLayers(base, zones, detail[0], detail[1]);
    }
}
