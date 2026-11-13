package com.sportforce3d.three;

import com.sportforce3d.three.model.PgeyZone;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

import java.util.EnumMap;
import java.util.Map;

public final class PgeyMaterials {
    public static final Color PGEY_MANNEQUIN = Color.web("#6b7482");

    public static final Map<PgeyZone, Color> PGEY_DEFAULT_COLORS = pgeyDefaults();

    private PgeyMaterials() {
    }

    private static Map<PgeyZone, Color> pgeyDefaults() {
        Color pgeyBlue = Color.web("#1f5fff");
        Color pgeyNeon = Color.web("#39ff88");
        Color pgeyWhite = Color.web("#eef1f5");
        Color pgeyBlack = Color.web("#14171c");
        Map<PgeyZone, Color> pgeyM = new EnumMap<>(PgeyZone.class);
        pgeyM.put(PgeyZone.JERSEY_BODY, pgeyBlue);
        pgeyM.put(PgeyZone.JERSEY_SLEEVES, pgeyBlue);
        pgeyM.put(PgeyZone.JERSEY_COLLAR, pgeyNeon);
        pgeyM.put(PgeyZone.JERSEY_CUFFS, pgeyNeon);
        pgeyM.put(PgeyZone.JERSEY_SIDES, pgeyNeon);
        pgeyM.put(PgeyZone.SHORTS_BASE, pgeyBlack);
        pgeyM.put(PgeyZone.SHORTS_STRIPE, pgeyBlue);
        pgeyM.put(PgeyZone.SHORTS_WAIST, pgeyBlue);
        pgeyM.put(PgeyZone.SOCKS_LEG, pgeyBlue);
        pgeyM.put(PgeyZone.SOCKS_BAND, pgeyWhite);
        pgeyM.put(PgeyZone.SHOE_UPPER, pgeyWhite);
        pgeyM.put(PgeyZone.SHOE_TOE, Color.web("#e2e6ec"));
        pgeyM.put(PgeyZone.SHOE_HEEL, pgeyNeon);
        pgeyM.put(PgeyZone.SHOE_LACES, pgeyWhite);
        pgeyM.put(PgeyZone.SHOE_SOLE, Color.web("#1c1f25"));
        return pgeyM;
    }

    public static PhongMaterial pgeyForZone(PgeyZone pgeyZone, Color pgeyColor, Image pgeyBump) {
        PhongMaterial pgeyM = new PhongMaterial(pgeyColor);
        switch (pgeyZone) {
            case SHOE_UPPER, SHOE_TOE, SHOE_HEEL -> {
                pgeyM.setSpecularColor(Color.gray(0.42));
                pgeyM.setSpecularPower(46);
            }
            case SHOE_SOLE -> {
                pgeyM.setSpecularColor(Color.gray(0.16));
                pgeyM.setSpecularPower(14);
            }
            case SHOE_LACES -> {
                pgeyM.setSpecularColor(Color.gray(0.08));
                pgeyM.setSpecularPower(8);
            }
            default -> {
                pgeyM.setSpecularColor(Color.gray(0.15));
                pgeyM.setSpecularPower(12);
                if (pgeyBump != null) {
                    pgeyM.setBumpMap(pgeyBump);
                }
            }
        }
        return pgeyM;
    }

    public static PhongMaterial pgeyMannequin() {
        PhongMaterial pgeyM = new PhongMaterial(PGEY_MANNEQUIN);
        pgeyM.setSpecularColor(Color.gray(0.30));
        pgeyM.setSpecularPower(28);
        return pgeyM;
    }
}
