package com.sportforce3d.model;

import java.util.Set;

public record HepyProductQuery(String hepyText, Long hepyCategoryId, Long hepySportId, Set<String> hepySizeCodes,
                           Long hepyMinPriceCents, Long hepyMaxPriceCents, boolean hepyInStockOnly,
                           boolean hepyCustomizableOnly, HepyProductSort hepySort, int hepyPage, int hepyPageSize) {

    public static final int HEPY_DEFAULT_PAGE_SIZE = 12;

    public HepyProductQuery {
        hepySizeCodes = hepySizeCodes == null ? Set.of() : Set.copyOf(hepySizeCodes);
        hepySort = hepySort == null ? HepyProductSort.RELEVANCE : hepySort;
        hepyPage = Math.max(1, hepyPage);
        hepyPageSize = hepyPageSize <= 0 ? HEPY_DEFAULT_PAGE_SIZE : Math.min(hepyPageSize, 60);
    }

    public static HepyProductQuery hepyAll() {
        return new HepyProductQuery(null, null, null, Set.of(), null, null, false, false,
                HepyProductSort.RELEVANCE, 1, HEPY_DEFAULT_PAGE_SIZE);
    }

    public HepyProductQuery hepyWithPage(int hepyNewPage) {
        return new HepyProductQuery(hepyText, hepyCategoryId, hepySportId, hepySizeCodes, hepyMinPriceCents, hepyMaxPriceCents,
                hepyInStockOnly, hepyCustomizableOnly, hepySort, hepyNewPage, hepyPageSize);
    }

    public int hepyOffset() {
        return (hepyPage - 1) * hepyPageSize;
    }
}
