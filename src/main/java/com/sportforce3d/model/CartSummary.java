package com.sportforce3d.model;

import java.util.List;

/** Carrito con totales calculados en el service. Montos en céntimos. */
public record CartSummary(List<CartLine> lines, long subtotalCents, long surchargeCents, long shippingCents,
                          long freeShippingFromCents) {

    public CartSummary {
        lines = List.copyOf(lines);
    }

    public long totalCents() {
        return subtotalCents + surchargeCents + shippingCents;
    }

    public int itemCount() {
        return lines.stream().mapToInt(CartLine::quantity).sum();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** Todas las líneas se pueden comprar y hay stock suficiente. */
    public boolean canCheckout() {
        return !lines.isEmpty() && lines.stream().allMatch(l -> l.purchasable() && l.enoughStock());
    }

    /** Cuánto falta para el envío gratis (0 si ya aplica). */
    public long missingForFreeShipping() {
        return Math.max(0, freeShippingFromCents - subtotalCents - surchargeCents);
    }
}
