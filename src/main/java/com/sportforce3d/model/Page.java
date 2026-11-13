package com.sportforce3d.model;

import java.util.List;

public record Page<T>(List<T> items, int page, int pageSize, long total) {

    public Page {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return total == 0 ? 1 : (int) ((total + pageSize - 1) / pageSize);
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages();
    }
}
