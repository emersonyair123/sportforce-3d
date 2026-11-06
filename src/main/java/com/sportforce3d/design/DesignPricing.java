package com.sportforce3d.design;

import com.sportforce3d.model.Garment;
import com.sportforce3d.model.Material;

import java.util.List;
import java.util.Map;

/**
 * Recargo por materiales de un diseño. Regla: el material por defecto de cada ranura va incluido en el precio base
 * (p. ej. la licra de las medias, que es su único material); los demás suman su recargo del catálogo. Solo cuentan
 * las ranuras de la prenda que se compra (el uniforme completo cuenta todas).
 */
public final class DesignPricing {

    private DesignPricing() {
    }

    public static List<MaterialSlot> slotsFor(Garment garment) {
        return garment == null || garment == Garment.KIT ? List.of(MaterialSlot.values()) : MaterialSlot.of(garment);
    }

    /** Suma en céntimos; un código que no está en {@code materials} no suma (se valida al guardar). */
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
