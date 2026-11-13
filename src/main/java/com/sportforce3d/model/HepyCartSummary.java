package com.sportforce3d.model;

import java.util.List;

public record HepyCartSummary(List<HepyCartLine> hepyLines, long hepySubtotalCents, long hepySurchargeCents, long hepyShippingCents,
                          long hepyFreeShippingFromCents) {

    public HepyCartSummary {
        hepyLines = List.copyOf(hepyLines);
    }

    public long hepyTotalCents() {
        return hepySubtotalCents + hepySurchargeCents + hepyShippingCents;
    }

    public int hepyItemCount() {
        return hepyLines.stream().mapToInt(HepyCartLine::hepyQuantity).sum();
    }

    public boolean hepyIsEmpty() {
        return hepyLines.isEmpty();
    }

    public boolean hepyCanCheckout() {
        return !hepyLines.isEmpty() && hepyLines.stream().allMatch(hepyL -> hepyL.hepyPurchasable() && hepyL.hepyEnoughStock());
    }

    public long hepyMissingForFreeShipping() {
        return Math.max(0, hepyFreeShippingFromCents - hepySubtotalCents - hepySurchargeCents);
    }
}
