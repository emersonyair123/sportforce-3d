package com.sportforce3d.design;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.three.model.PgeyZone;

import java.util.List;

public enum PgeyMaterialSlot {
    JERSEY("Tela de la camiseta", HepyGarment.JERSEY, HepyMaterial.HepyScope.TEXTIL, "JERSEY", "DRY_FIT",
            List.of(PgeyZone.JERSEY_BODY, PgeyZone.JERSEY_SLEEVES, PgeyZone.JERSEY_SIDES)),
    SHORTS("Tela del short", HepyGarment.SHORTS, HepyMaterial.HepyScope.TEXTIL, "SHORTS", "DRY_FIT",
            List.of(PgeyZone.SHORTS_BASE, PgeyZone.SHORTS_STRIPE)),
    SOCKS("Tela de las medias", HepyGarment.SOCKS, HepyMaterial.HepyScope.TEXTIL, "SOCKS", "LICRA",
            List.of(PgeyZone.SOCKS_LEG)),
    SHOE_UPPER("Capellada", HepyGarment.SHOES, HepyMaterial.HepyScope.CALZADO, "shoe_upper", "KNIT",
            List.of(PgeyZone.SHOE_UPPER, PgeyZone.SHOE_TOE, PgeyZone.SHOE_HEEL)),
    SHOE_SOLE("Suela", HepyGarment.SHOES, HepyMaterial.HepyScope.CALZADO, "shoe_sole", "CAUCHO",
            List.of(PgeyZone.SHOE_SOLE));

    private final String pgeyLabel;
    private final HepyGarment pgeyGarment;
    private final HepyMaterial.HepyScope pgeyScope;
    private final String pgeySuitableKey;
    private final String pgeyDefaultCode;
    private final List<PgeyZone> pgeyZones;

    PgeyMaterialSlot(String pgeyLabel, HepyGarment pgeyGarment, HepyMaterial.HepyScope pgeyScope, String pgeySuitableKey, String pgeyDefaultCode,
                 List<PgeyZone> pgeyZones) {
        this.pgeyLabel = pgeyLabel;
        this.pgeyGarment = pgeyGarment;
        this.pgeyScope = pgeyScope;
        this.pgeySuitableKey = pgeySuitableKey;
        this.pgeyDefaultCode = pgeyDefaultCode;
        this.pgeyZones = pgeyZones;
    }

    public String pgeyLabel() {
        return pgeyLabel;
    }

    public HepyGarment pgeyGarment() {
        return pgeyGarment;
    }

    public List<PgeyZone> pgeyZones() {
        return pgeyZones;
    }

    public String pgeyDefaultCode() {
        return pgeyDefaultCode;
    }

    public boolean pgeyAccepts(HepyMaterial pgeyM) {
        return pgeyM.hepyScope() == pgeyScope && pgeyM.hepySuitableFor().contains(pgeySuitableKey);
    }

    public static List<PgeyMaterialSlot> pgeyOf(HepyGarment pgeyGarment) {
        return java.util.Arrays.stream(values()).filter(pgeyS -> pgeyS.pgeyGarment == pgeyGarment).toList();
    }
}
