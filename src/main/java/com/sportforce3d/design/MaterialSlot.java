package com.sportforce3d.design;

import com.sportforce3d.model.Garment;
import com.sportforce3d.model.Material;
import com.sportforce3d.three.model.Zone;

import java.util.List;

public enum MaterialSlot {
    JERSEY("Tela de la camiseta", Garment.JERSEY, Material.Scope.TEXTIL, "JERSEY", "DRY_FIT",
            List.of(Zone.JERSEY_BODY, Zone.JERSEY_SLEEVES, Zone.JERSEY_SIDES)),
    SHORTS("Tela del short", Garment.SHORTS, Material.Scope.TEXTIL, "SHORTS", "DRY_FIT",
            List.of(Zone.SHORTS_BASE, Zone.SHORTS_STRIPE)),
    SOCKS("Tela de las medias", Garment.SOCKS, Material.Scope.TEXTIL, "SOCKS", "LICRA",
            List.of(Zone.SOCKS_LEG)),
    SHOE_UPPER("Capellada", Garment.SHOES, Material.Scope.CALZADO, "shoe_upper", "KNIT",
            List.of(Zone.SHOE_UPPER, Zone.SHOE_TOE, Zone.SHOE_HEEL)),
    SHOE_SOLE("Suela", Garment.SHOES, Material.Scope.CALZADO, "shoe_sole", "CAUCHO",
            List.of(Zone.SHOE_SOLE));

    private final String label;
    private final Garment garment;
    private final Material.Scope scope;
    private final String suitableKey;
    private final String defaultCode;
    private final List<Zone> zones;

    MaterialSlot(String label, Garment garment, Material.Scope scope, String suitableKey, String defaultCode,
                 List<Zone> zones) {
        this.label = label;
        this.garment = garment;
        this.scope = scope;
        this.suitableKey = suitableKey;
        this.defaultCode = defaultCode;
        this.zones = zones;
    }

    public String label() {
        return label;
    }

    public Garment garment() {
        return garment;
    }

    public List<Zone> zones() {
        return zones;
    }

    public String defaultCode() {
        return defaultCode;
    }

    public boolean accepts(Material m) {
        return m.scope() == scope && m.suitableFor().contains(suitableKey);
    }

    public static List<MaterialSlot> of(Garment garment) {
        return java.util.Arrays.stream(values()).filter(s -> s.garment == garment).toList();
    }
}
