package com.sportforce3d.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SalesReport(LocalDate from, LocalDate to, Grouping grouping, Instant generatedAt, String generatedBy,
                          int orders, int units, long productsCents, long shippingCents, long totalCents,
                          List<PeriodRow> periods, List<CategoryRow> categories, List<ProductRow> products,
                          List<MethodRow> methods) {
    public enum Grouping {
        DAY, MONTH;

        public static final int DAY_LIMIT = 62;
    }

    public record PeriodRow(LocalDate start, int orders, int units, long totalCents) {
    }

    public record CategoryRow(String category, int orders, int units, long revenueCents) {
    }

    public record ProductRow(String sku, String name, String category, int orders, int units, long revenueCents) {
    }

    public record MethodRow(PaymentMethod method, int orders, long totalCents) {
    }

    public SalesReport {
        periods = List.copyOf(periods);
        categories = List.copyOf(categories);
        products = List.copyOf(products);
        methods = List.copyOf(methods);
    }

    public boolean isEmpty() {
        return orders == 0;
    }

    public long averageTicketCents() {
        return orders == 0 ? 0 : Math.round((double) totalCents / orders);
    }

    public double sharePercent(long cents) {
        return productsCents == 0 ? 0 : Math.round(cents * 1000.0 / productsCents) / 10.0;
    }
}
