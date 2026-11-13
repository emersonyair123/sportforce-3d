package com.sportforce3d.component;

import com.sportforce3d.model.HepyGarment;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineJoin;

import java.util.ArrayList;
import java.util.List;

public class HepyGarmentArt extends StackPane {

    private static final String HEPY_JERSEY = "M32 12 L43 7 Q50 15 57 7 L68 12 L92 28 L82 47 L72 41 L72 112 L28 112 L28 41 L18 47 L8 28 Z";
    private static final String HEPY_JERSEY_COLLAR = "M43 7 Q50 15 57 7 L54 6 Q50 11 46 6 Z";
    private static final String HEPY_JERSEY_CUFF_L = "M8 28 L12 22 L22 40 L18 47 Z";
    private static final String HEPY_JERSEY_CUFF_R = "M92 28 L88 22 L78 40 L82 47 Z";
    private static final String HEPY_SHORTS = "M20 18 L80 18 L86 100 L57 104 L50 56 L43 104 L14 100 Z";
    private static final String HEPY_SHORTS_WAIST = "M20 18 L80 18 L80.8 28 L19.2 28 Z";
    private static final String HEPY_SHORTS_STRIPE = "M80.8 30 L84.6 94 L81 94.5 L77.6 31 Z";
    private static final String HEPY_SOCK = "M38 8 L62 8 L62 76 Q62 88 76 95 Q90 102 86 112 Q84 116 76 116 L44 116 Q34 116 36 104 L38 8 Z";
    private static final String HEPY_SOCK_BAND = "M38 8 L62 8 L62 24 L38 24 Z";
    private static final String HEPY_SHOE = "M6 82 Q8 62 26 58 L46 48 Q54 60 70 62 Q92 66 95 80 L95 88 L6 88 Z";
    private static final String HEPY_SHOE_SOLE = "M5 88 L96 88 L96 96 Q96 99 92 99 L9 99 Q5 99 5 96 Z";
    private static final String HEPY_SHOE_EYELETS = hepyDots(new double[][]{{35, 60}, {40.5, 57}, {46, 54}, {52, 57.5}}, 1.9);
    private static final String HEPY_SHOE_HEEL = "M6 82 Q7 68 16 62 L19 88 L6 88 Z";
    private static final String HEPY_HOODIE = "M36 14 Q50 -2 64 14 L72 17 L95 72 L84 76 L72 46 L72 112 L28 112 L28 46 L16 76 L5 72 L28 17 Z";
    private static final String HEPY_HOODIE_ZIP = "M49 22 L51 22 L51 112 L49 112 Z";
    private static final String HEPY_BAG = "M26 30 Q26 14 50 14 Q74 14 74 30 L78 108 Q78 116 70 116 L30 116 Q22 116 22 108 Z";
    private static final String HEPY_BAG_POCKET = "M32 70 L68 70 L68 100 Q68 104 64 104 L36 104 Q32 104 32 100 Z";
    private static final String HEPY_BAG_HANDLE = "M40 14 Q40 4 50 4 Q60 4 60 14";

    public HepyGarmentArt(HepyGarment hepyGarment, String hepyColorHex, double hepyHeight) {
        getStyleClass().add("sf-garment-art");
        Color hepyBase = hepyParse(hepyColorHex);
        Color hepyDetail = hepyContrastDetail(hepyBase);
        Color hepyOutline = hepyBase.getBrightness() < 0.25 ? Color.web("#5a6472") : hepyBase.deriveColor(0, 1, 0.7, 1);

        List<SVGPath> hepyParts = new ArrayList<>();
        switch (hepyGarment) {
            case JERSEY -> {
                hepyParts.add(hepyPath(HEPY_JERSEY, hepyBase, hepyOutline));
                hepyParts.add(hepyPath(HEPY_JERSEY_COLLAR, hepyDetail, null));
                hepyParts.add(hepyPath(HEPY_JERSEY_CUFF_L, hepyDetail, null));
                hepyParts.add(hepyPath(HEPY_JERSEY_CUFF_R, hepyDetail, null));
            }
            case KIT -> {
                SVGPath hepyJersey = hepyPath(HEPY_JERSEY, hepyBase, hepyOutline);
                SVGPath hepyCollar = hepyPath(HEPY_JERSEY_COLLAR, hepyDetail, null);
                SVGPath hepyShorts = hepyPath(HEPY_SHORTS, hepyDetail.deriveColor(0, 1, 1, 1), hepyOutline);
                Group hepyTop = new Group(hepyJersey, hepyCollar);
                hepyTop.setScaleX(0.72);
                hepyTop.setScaleY(0.72);
                hepyTop.setTranslateY(-26);
                hepyShorts.setScaleX(0.5);
                hepyShorts.setScaleY(0.42);
                hepyShorts.setTranslateY(46);
                Group hepyKit = new Group(hepyTop, hepyShorts);
                getChildren().add(hepyFit(hepyKit, hepyHeight));
                setAccessibleText(hepyGarment.hepyLabel());
                return;
            }
            case SHORTS -> {
                hepyParts.add(hepyPath(HEPY_SHORTS, hepyBase, hepyOutline));
                hepyParts.add(hepyPath(HEPY_SHORTS_WAIST, hepyDetail, null));
                hepyParts.add(hepyPath(HEPY_SHORTS_STRIPE, hepyDetail, null));
            }
            case SOCKS -> {
                hepyParts.add(hepyPath(HEPY_SOCK, hepyBase, hepyOutline));
                hepyParts.add(hepyPath(HEPY_SOCK_BAND, hepyDetail, null));
            }
            case SHOES -> {
                hepyParts.add(hepyPath(HEPY_SHOE, hepyBase, hepyOutline));
                hepyParts.add(hepyPath(HEPY_SHOE_SOLE, hepyDetail, null));
                hepyParts.add(hepyPath(HEPY_SHOE_HEEL, hepyDetail, null));
                hepyParts.add(hepyPath(HEPY_SHOE_EYELETS, hepyDetail, null));
            }
            case HOODIE -> {
                hepyParts.add(hepyPath(HEPY_HOODIE, hepyBase, hepyOutline));
                hepyParts.add(hepyPath(HEPY_HOODIE_ZIP, hepyDetail, null));
            }
            case ACCESSORY -> {
                SVGPath hepyHandle = hepyPath(HEPY_BAG_HANDLE, null, hepyOutline);
                hepyHandle.setStrokeWidth(4);
                hepyParts.add(hepyHandle);
                hepyParts.add(hepyPath(HEPY_BAG, hepyBase, hepyOutline));
                hepyParts.add(hepyPath(HEPY_BAG_POCKET, hepyDetail, null));
            }
        }
        getChildren().add(hepyFit(new Group(hepyParts.toArray(SVGPath[]::new)), hepyHeight));
        setAccessibleText(hepyGarment.hepyLabel());
    }

    private static String hepyDots(double[][] hepyCenters, double hepyR) {
        StringBuilder hepySb = new StringBuilder();
        for (double[] hepyC : hepyCenters) {
            hepySb.append(String.format(java.util.Locale.ROOT, "M%.2f %.2f a%.2f %.2f 0 1 0 %.2f 0 a%.2f %.2f 0 1 0 %.2f 0 Z ",
                    hepyC[0] - hepyR, hepyC[1], hepyR, hepyR, 2 * hepyR, hepyR, hepyR, -2 * hepyR));
        }
        return hepySb.toString().trim();
    }

    private static Group hepyFit(Group hepyGroup, double hepyHeight) {
        double hepyScale = hepyHeight / 120.0;
        Group hepyWrapper = new Group(hepyGroup);
        hepyWrapper.setScaleX(hepyScale);
        hepyWrapper.setScaleY(hepyScale);
        return hepyWrapper;
    }

    private static SVGPath hepyPath(String hepyContent, Color hepyFill, Color hepyStroke) {
        SVGPath hepyP = new SVGPath();
        hepyP.setContent(hepyContent);
        hepyP.setFill(hepyFill);
        if (hepyStroke != null) {
            hepyP.setStroke(hepyStroke);
            hepyP.setStrokeWidth(1.2);
            hepyP.setStrokeLineJoin(StrokeLineJoin.ROUND);
        }
        return hepyP;
    }

    static Color hepyContrastDetail(Color hepyBase) {
        if (hepyBase.getBrightness() > 0.85 && hepyBase.getSaturation() < 0.2) {
            return Color.web("#1f5fff");
        }
        if (hepyBase.getBrightness() < 0.25) {
            return Color.web("#39ff88");
        }
        return hepyBase.getBrightness() > 0.6 ? hepyBase.deriveColor(0, 1, 0.55, 1) : Color.web("#f2f5f8");
    }

    private static Color hepyParse(String hepyHex) {
        try {
            return hepyHex == null ? Color.web("#5a6472") : Color.web(hepyHex);
        } catch (IllegalArgumentException hepyE) {
            return Color.web("#5a6472");
        }
    }
}
