package com.sportforce3d.model;

import java.util.List;

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

    public boolean canCheckout() {
        return !lines.isEmpty() && lines.stream().allMatch(l -> l.purchasable() && l.enoughStock());
    }

    public long missingForFreeShipping() {
        return Math.max(0, freeShippingFromCents - subtotalCents - surchargeCents);
    }
}
