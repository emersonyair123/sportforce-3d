package com.sportforce3d.design;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyMaterial;

import java.util.List;
import java.util.Map;

public final class PgeyDesignPricing {

    private PgeyDesignPricing() {
    }

    public static List<PgeyMaterialSlot> pgeySlotsFor(HepyGarment pgeyGarment) {
        if (pgeyGarment == null) {
            return List.of(PgeyMaterialSlot.values());
        }
        if (pgeyGarment == HepyGarment.KIT) {
            return List.of(PgeyMaterialSlot.JERSEY, PgeyMaterialSlot.SHORTS, PgeyMaterialSlot.SOCKS);
        }
        return PgeyMaterialSlot.pgeyOf(pgeyGarment);
    }

    public static long pgeySurchargeCents(PgeyDesignDocument pgeyDoc, Map<String, HepyMaterial> pgeyMaterials, HepyGarment pgeyGarment) {
        long pgeySum = 0;
        for (PgeyMaterialSlot pgeySlot : pgeySlotsFor(pgeyGarment)) {
            String pgeyCode = pgeyDoc.pgeyMaterial(pgeySlot);
            HepyMaterial pgeyM = pgeyMaterials.get(pgeyCode);
            if (pgeyM != null && !pgeyCode.equals(pgeySlot.pgeyDefaultCode())) {
                pgeySum += pgeyM.hepySurchargeCents();
            }
        }
        return pgeySum;
    }
}
