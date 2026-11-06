package com.sportforce3d.model;

import java.util.List;

/** Tipos de prenda; coinciden con products.garment_type. */
public enum Garment {
    JERSEY("Camiseta"),
    SHORTS("Short"),
    SOCKS("Medias"),
    SHOES("Zapatillas"),
    HOODIE("Buzo"),
    ACCESSORY("Accesorios"),
    KIT("Uniforme completo");

    /** Prendas del selector del diseñador 3D, en el orden de la guía. */
    public static final List<Garment> DESIGNER = List.of(JERSEY, SHORTS, SOCKS, SHOES, ACCESSORY);

    /**
     * Prendas cuyo diseño se puede comprar (el diseñador 3D las dibuja y el carrito las acepta). Los buzos y los
     * accesorios se compran directo del catálogo con su color y talla: nunca son "personalizables".
     */
    public static final java.util.Set<Garment> DESIGNABLE = java.util.Collections.unmodifiableSet(
            java.util.EnumSet.of(JERSEY, SHORTS, SOCKS, SHOES, KIT));

    private final String label;

    Garment(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** true si un producto de esta prenda puede ser personalizable (ver {@link #DESIGNABLE}). */
    public boolean designable() {
        return DESIGNABLE.contains(this);
    }
}