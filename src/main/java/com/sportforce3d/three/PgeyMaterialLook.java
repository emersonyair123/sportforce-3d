package com.sportforce3d.three;

import com.sportforce3d.model.HepyMaterial;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public record PgeyMaterialLook(Image pgeyBump, Color pgeySpecular, double pgeyPower, boolean pgeyPerforated) {

    public static PgeyMaterialLook pgeyOf(HepyMaterial pgeyM) {
        double pgeyGloss = 1 - pgeyM.hepyRoughness();
        double pgeyIntensity = 0.05 + pgeyM.hepySheen() * 0.30 + pgeyGloss * 0.22 + pgeyM.hepyMetalness() * 0.55;
        double pgeyPower = 6 + pgeyGloss * pgeyGloss * 90 + pgeyM.hepyMetalness() * 40;
        return new PgeyMaterialLook(PgeyFabricMaps.pgeyForPattern(pgeyM.hepyPreviewPattern()),
                Color.gray(Math.min(0.85, pgeyIntensity)), pgeyPower, pgeyM.hepyPreviewPattern() == HepyMaterial.HepyPattern.MESH);
    }
}
