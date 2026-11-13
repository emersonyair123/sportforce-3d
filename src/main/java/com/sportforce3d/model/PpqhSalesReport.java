package com.sportforce3d.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PpqhSalesReport(LocalDate ppqhFrom, LocalDate ppqhTo, PpqhGrouping ppqhGrouping, Instant ppqhGeneratedAt, String ppqhGeneratedBy,
                          int ppqhOrders, int ppqhUnits, long ppqhProductsCents, long ppqhShippingCents, long ppqhTotalCents,
                          List<PpqhPeriodRow> ppqhPeriods, List<PpqhCategoryRow> ppqhCategories, List<PpqhProductRow> ppqhProducts,
                          List<PpqhMethodRow> ppqhMethods) {
    public enum PpqhGrouping {
        DAY, MONTH;

        public static final int PPQH_DAY_LIMIT = 62;
    }

    public record PpqhPeriodRow(LocalDate start, int ppqhOrders, int ppqhUnits, long ppqhTotalCents) {
    }

    public record PpqhCategoryRow(String ppqhCategory, int ppqhOrders, int ppqhUnits, long ppqhRevenueCents) {
    }

    public record PpqhProductRow(String ppqhSku, String ppqhName, String ppqhCategory, int ppqhOrders, int ppqhUnits, long ppqhRevenueCents) {
    }

    public record PpqhMethodRow(PpqhPaymentMethod ppqhMethod, int ppqhOrders, long ppqhTotalCents) {
    }

    public PpqhSalesReport {
        ppqhPeriods = List.copyOf(ppqhPeriods);
        ppqhCategories = List.copyOf(ppqhCategories);
        ppqhProducts = List.copyOf(ppqhProducts);
        ppqhMethods = List.copyOf(ppqhMethods);
    }

    public boolean ppqhIsEmpty() {
        return ppqhOrders == 0;
    }

    public long ppqhAverageTicketCents() {
        return ppqhOrders == 0 ? 0 : Math.round((double) ppqhTotalCents / ppqhOrders);
    }

    public double ppqhSharePercent(long ppqhCents) {
        return ppqhProductsCents == 0 ? 0 : Math.round(ppqhCents * 1000.0 / ppqhProductsCents) / 10.0;
    }
}
