package com.sportforce3d.design;

import com.sportforce3d.model.Garment;
import com.sportforce3d.model.Material;
import com.sportforce3d.three.model.BodyType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DesignPricingTest {

    private static Material m(String code, long cents) {
        return new Material(1, code, code, "", "", Material.Scope.TEXTIL, Set.of("JERSEY"), cents, 0.5, 0.2, 0,
                Material.Pattern.PLAIN);
    }

    private static final Map<String, Material> CATALOG = Map.of(
            "DRY_FIT", m("DRY_FIT", 0), "MALLA", m("MALLA", 500), "LICRA", m("LICRA", 1000),
            "KNIT", m("KNIT", 0), "CAUCHO", m("CAUCHO", 0), "TPU_TACOS", m("TPU_TACOS", 2000));

    @Test
    void defaultMaterialsAreIncludedEvenWithSurcharge() {
        DesignDocument doc = DesignDefaults.document(BodyType.ADULTO_HOMBRE);
        assertThat(doc.material(MaterialSlot.SOCKS)).isEqualTo("LICRA");
        assertThat(DesignPricing.surchargeCents(doc, CATALOG, Garment.KIT)).isZero();
    }

    @Test
    void onlyTheBoughtGarmentCounts() {
        DesignDocument doc = DesignDefaults.document(BodyType.ADULTO_HOMBRE)
                .withMaterial(MaterialSlot.JERSEY, "MALLA")
                .withMaterial(MaterialSlot.SHORTS, "LICRA")
                .withMaterial(MaterialSlot.SHOE_SOLE, "TPU_TACOS");
        assertThat(DesignPricing.surchargeCents(doc, CATALOG, Garment.KIT)).isEqualTo(500 + 1000);
        assertThat(DesignPricing.surchargeCents(doc, CATALOG, null)).isEqualTo(3500);
        assertThat(DesignPricing.surchargeCents(doc, CATALOG, Garment.JERSEY)).isEqualTo(500);
        assertThat(DesignPricing.surchargeCents(doc, CATALOG, Garment.SHOES)).isEqualTo(2000);
        assertThat(DesignPricing.surchargeCents(doc, CATALOG, Garment.SOCKS)).isZero();
    }
}
