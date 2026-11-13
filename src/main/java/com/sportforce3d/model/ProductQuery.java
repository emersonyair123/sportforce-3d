package com.sportforce3d.model;

import java.util.Set;

public record ProductQuery(String text, Long categoryId, Long sportId, Set<String> sizeCodes,
                           Long minPriceCents, Long maxPriceCents, boolean inStockOnly,
                           boolean customizableOnly, ProductSort sort, int page, int pageSize) {

    public static final int DEFAULT_PAGE_SIZE = 12;

    public ProductQuery {
        sizeCodes = sizeCodes == null ? Set.of() : Set.copyOf(sizeCodes);
        sort = sort == null ? ProductSort.RELEVANCE : sort;
        page = Math.max(1, page);
        pageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, 60);
    }

    public static ProductQuery all() {
        return new ProductQuery(null, null, null, Set.of(), null, null, false, false,
                ProductSort.RELEVANCE, 1, DEFAULT_PAGE_SIZE);
    }

    public ProductQuery withPage(int newPage) {
        return new ProductQuery(text, categoryId, sportId, sizeCodes, minPriceCents, maxPriceCents,
                inStockOnly, customizableOnly, sort, newPage, pageSize);
    }

    public int offset() {
        return (page - 1) * pageSize;
    }
}
