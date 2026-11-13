package com.sportforce3d.model;

import java.util.List;

public enum HepyGarment {
    JERSEY("Camiseta"),
    SHORTS("Short"),
    SOCKS("Medias"),
    SHOES("Zapatillas"),
    HOODIE("Buzo"),
    ACCESSORY("Accesorios"),
    KIT("Uniforme completo");

    public static final List<HepyGarment> HEPY_DESIGNER = List.of(JERSEY, SHORTS, SOCKS, SHOES, ACCESSORY);

    public static final java.util.Set<HepyGarment> HEPY_DESIGNABLE = java.util.Collections.unmodifiableSet(
            java.util.EnumSet.of(JERSEY, SHORTS, SOCKS, SHOES, KIT));

    private final String hepyLabel;

    HepyGarment(String hepyLabel) {
        this.hepyLabel = hepyLabel;
    }

    public String hepyLabel() {
        return hepyLabel;
    }

    public boolean hepyDesignable() {
        return HEPY_DESIGNABLE.contains(this);
    }
}
