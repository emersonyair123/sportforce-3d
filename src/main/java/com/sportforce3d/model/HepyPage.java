package com.sportforce3d.model;

import java.util.List;

public record HepyPage<T>(List<T> hepyItems, int hepyPage, int hepyPageSize, long hepyTotal) {

    public HepyPage {
        hepyItems = List.copyOf(hepyItems);
    }

    public int hepyTotalPages() {
        return hepyTotal == 0 ? 1 : (int) ((hepyTotal + hepyPageSize - 1) / hepyPageSize);
    }

    public boolean hepyHasPrevious() {
        return hepyPage > 1;
    }

    public boolean hepyHasNext() {
        return hepyPage < hepyTotalPages();
    }
}
