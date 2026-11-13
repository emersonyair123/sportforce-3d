package com.sportforce3d.design;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.three.model.PgeyBodyType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DesignPricingTest {

    private static HepyMaterial m(String code, long cents) {
        return new HepyMaterial(1, code, code, "", "", HepyMaterial.HepyScope.TEXTIL, Set.of("JERSEY"), cents, 0.5, 0.2, 0,
                HepyMaterial.HepyPattern.PLAIN);
    }

    private static final Map<String, HepyMaterial> CATALOG = Map.of(
            "DRY_FIT", m("DRY_FIT", 0), "MALLA", m("MALLA", 500), "LICRA", m("LICRA", 1000),
            "KNIT", m("KNIT", 0), "CAUCHO", m("CAUCHO", 0), "TPU_TACOS", m("TPU_TACOS", 2000));

    @Test
    void defaultMaterialsAreIncludedEvenWithSurcharge() {
        PgeyDesignDocument doc = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE);
        assertThat(doc.pgeyMaterial(PgeyMaterialSlot.SOCKS)).isEqualTo("LICRA");
        assertThat(PgeyDesignPricing.pgeySurchargeCents(doc, CATALOG, HepyGarment.KIT)).isZero();
    }

    @Test
    void onlyTheBoughtGarmentCounts() {
        PgeyDesignDocument doc = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE)
                .pgeyWithMaterial(PgeyMaterialSlot.JERSEY, "MALLA")
                .pgeyWithMaterial(PgeyMaterialSlot.SHORTS, "LICRA")
                .pgeyWithMaterial(PgeyMaterialSlot.SHOE_SOLE, "TPU_TACOS");
        assertThat(PgeyDesignPricing.pgeySurchargeCents(doc, CATALOG, HepyGarment.KIT)).isEqualTo(500 + 1000);
        assertThat(PgeyDesignPricing.pgeySurchargeCents(doc, CATALOG, null)).isEqualTo(3500);
        assertThat(PgeyDesignPricing.pgeySurchargeCents(doc, CATALOG, HepyGarment.JERSEY)).isEqualTo(500);
        assertThat(PgeyDesignPricing.pgeySurchargeCents(doc, CATALOG, HepyGarment.SHOES)).isEqualTo(2000);
        assertThat(PgeyDesignPricing.pgeySurchargeCents(doc, CATALOG, HepyGarment.SOCKS)).isZero();
    }
}
