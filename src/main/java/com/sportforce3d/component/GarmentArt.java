package com.sportforce3d.component;

import com.sportforce3d.model.Garment;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineJoin;

import java.util.ArrayList;
import java.util.List;

public class GarmentArt extends StackPane {

    private static final String JERSEY = "M32 12 L43 7 Q50 15 57 7 L68 12 L92 28 L82 47 L72 41 L72 112 L28 112 L28 41 L18 47 L8 28 Z";
    private static final String JERSEY_COLLAR = "M43 7 Q50 15 57 7 L54 6 Q50 11 46 6 Z";
    private static final String JERSEY_CUFF_L = "M8 28 L12 22 L22 40 L18 47 Z";
    private static final String JERSEY_CUFF_R = "M92 28 L88 22 L78 40 L82 47 Z";
    private static final String SHORTS = "M20 18 L80 18 L86 100 L57 104 L50 56 L43 104 L14 100 Z";
    private static final String SHORTS_WAIST = "M20 18 L80 18 L80.8 28 L19.2 28 Z";
    private static final String SHORTS_STRIPE = "M80.8 30 L84.6 94 L81 94.5 L77.6 31 Z";
    private static final String SOCK = "M38 8 L62 8 L62 76 Q62 88 76 95 Q90 102 86 112 Q84 116 76 116 L44 116 Q34 116 36 104 L38 8 Z";
    private static final String SOCK_BAND = "M38 8 L62 8 L62 24 L38 24 Z";
    private static final String SHOE = "M6 82 Q8 62 26 58 L46 48 Q54 60 70 62 Q92 66 95 80 L95 88 L6 88 Z";
    private static final String SHOE_SOLE = "M5 88 L96 88 L96 96 Q96 99 92 99 L9 99 Q5 99 5 96 Z";
    private static final String SHOE_EYELETS = dots(new double[][]{{35, 60}, {40.5, 57}, {46, 54}, {52, 57.5}}, 1.9);
    private static final String SHOE_HEEL = "M6 82 Q7 68 16 62 L19 88 L6 88 Z";
    private static final String HOODIE = "M36 14 Q50 -2 64 14 L72 17 L95 72 L84 76 L72 46 L72 112 L28 112 L28 46 L16 76 L5 72 L28 17 Z";
    private static final String HOODIE_ZIP = "M49 22 L51 22 L51 112 L49 112 Z";
    private static final String BAG = "M26 30 Q26 14 50 14 Q74 14 74 30 L78 108 Q78 116 70 116 L30 116 Q22 116 22 108 Z";
    private static final String BAG_POCKET = "M32 70 L68 70 L68 100 Q68 104 64 104 L36 104 Q32 104 32 100 Z";
    private static final String BAG_HANDLE = "M40 14 Q40 4 50 4 Q60 4 60 14";

    public GarmentArt(Garment garment, String colorHex, double height) {
        getStyleClass().add("sf-garment-art");
        Color base = parse(colorHex);
        Color detail = contrastDetail(base);
        Color outline = base.getBrightness() < 0.25 ? Color.web("#5a6472") : base.deriveColor(0, 1, 0.7, 1);

        List<SVGPath> parts = new ArrayList<>();
        switch (garment) {
            case JERSEY -> {
                parts.add(path(JERSEY, base, outline));
                parts.add(path(JERSEY_COLLAR, detail, null));
                parts.add(path(JERSEY_CUFF_L, detail, null));
                parts.add(path(JERSEY_CUFF_R, detail, null));
            }
            case KIT -> {
                SVGPath jersey = path(JERSEY, base, outline);
                SVGPath collar = path(JERSEY_COLLAR, detail, null);
                SVGPath shorts = path(SHORTS, detail.deriveColor(0, 1, 1, 1), outline);
                Group top = new Group(jersey, collar);
                top.setScaleX(0.72);
                top.setScaleY(0.72);
                top.setTranslateY(-26);
                shorts.setScaleX(0.5);
                shorts.setScaleY(0.42);
                shorts.setTranslateY(46);
                Group kit = new Group(top, shorts);
                getChildren().add(fit(kit, height));
                setAccessibleText(garment.label());
                return;
            }
            case SHORTS -> {
                parts.add(path(SHORTS, base, outline));
                parts.add(path(SHORTS_WAIST, detail, null));
                parts.add(path(SHORTS_STRIPE, detail, null));
            }
            case SOCKS -> {
                parts.add(path(SOCK, base, outline));
                parts.add(path(SOCK_BAND, detail, null));
            }
            case SHOES -> {
                parts.add(path(SHOE, base, outline));
                parts.add(path(SHOE_SOLE, detail, null));
                parts.add(path(SHOE_HEEL, detail, null));
                parts.add(path(SHOE_EYELETS, detail, null));
            }
            case HOODIE -> {
                parts.add(path(HOODIE, base, outline));
                parts.add(path(HOODIE_ZIP, detail, null));
            }
            case ACCESSORY -> {
                SVGPath handle = path(BAG_HANDLE, null, outline);
                handle.setStrokeWidth(4);
                parts.add(handle);
                parts.add(path(BAG, base, outline));
                parts.add(path(BAG_POCKET, detail, null));
            }
        }
        getChildren().add(fit(new Group(parts.toArray(SVGPath[]::new)), height));
        setAccessibleText(garment.label());
    }

    private static String dots(double[][] centers, double r) {
        StringBuilder sb = new StringBuilder();
        for (double[] c : centers) {
            sb.append(String.format(java.util.Locale.ROOT, "M%.2f %.2f a%.2f %.2f 0 1 0 %.2f 0 a%.2f %.2f 0 1 0 %.2f 0 Z ",
                    c[0] - r, c[1], r, r, 2 * r, r, r, -2 * r));
        }
        return sb.toString().trim();
    }

    private static Group fit(Group group, double height) {
        double scale = height / 120.0;
        Group wrapper = new Group(group);
        wrapper.setScaleX(scale);
        wrapper.setScaleY(scale);
        return wrapper;
    }

    private static SVGPath path(String content, Color fill, Color stroke) {
        SVGPath p = new SVGPath();
        p.setContent(content);
        p.setFill(fill);
        if (stroke != null) {
            p.setStroke(stroke);
            p.setStrokeWidth(1.2);
            p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        }
        return p;
    }

    static Color contrastDetail(Color base) {
        if (base.getBrightness() > 0.85 && base.getSaturation() < 0.2) {
            return Color.web("#1f5fff");
        }
        if (base.getBrightness() < 0.25) {
            return Color.web("#39ff88");
        }
        return base.getBrightness() > 0.6 ? base.deriveColor(0, 1, 0.55, 1) : Color.web("#f2f5f8");
    }

    private static Color parse(String hex) {
        try {
            return hex == null ? Color.web("#5a6472") : Color.web(hex);
        } catch (IllegalArgumentException e) {
            return Color.web("#5a6472");
        }
    }
}
