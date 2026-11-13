package com.sportforce3d.design;

import com.sportforce3d.model.Garment;
import com.sportforce3d.model.Material;

import java.util.List;
import java.util.Map;

public final class DesignPricing {

    private DesignPricing() {
    }

    public static List<MaterialSlot> slotsFor(Garment garment) {
        if (garment == null) {
            return List.of(MaterialSlot.values());
        }
        if (garment == Garment.KIT) {
            return List.of(MaterialSlot.JERSEY, MaterialSlot.SHORTS, MaterialSlot.SOCKS);
        }
        return MaterialSlot.of(garment);
    }

    public static long surchargeCents(DesignDocument doc, Map<String, Material> materials, Garment garment) {
        long sum = 0;
        for (MaterialSlot slot : slotsFor(garment)) {
            String code = doc.material(slot);
            Material m = materials.get(code);
            if (m != null && !code.equals(slot.defaultCode())) {
                sum += m.surchargeCents();
            }
        }
        return sum;
    }
}
