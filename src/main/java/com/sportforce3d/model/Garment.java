package com.sportforce3d.model;

import java.util.List;

public enum Garment {
    JERSEY("Camiseta"),
    SHORTS("Short"),
    SOCKS("Medias"),
    SHOES("Zapatillas"),
    HOODIE("Buzo"),
    ACCESSORY("Accesorios"),
    KIT("Uniforme completo");

    public static final List<Garment> DESIGNER = List.of(JERSEY, SHORTS, SOCKS, SHOES, ACCESSORY);

    public static final java.util.Set<Garment> DESIGNABLE = java.util.Collections.unmodifiableSet(
            java.util.EnumSet.of(JERSEY, SHORTS, SOCKS, SHOES, KIT));

    private final String label;

    Garment(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean designable() {
        return DESIGNABLE.contains(this);
    }
}
