package com.sportforce3d.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de ventas de un rango de fechas de Lima (ambos días incluidos). Una venta es un pedido con su pago
 * APROBADO, en el día en que el ADMIN lo aprobó (igual que el dashboard). Montos en céntimos.
 *
 * @param productsCents  subtotal + recargos de los pedidos (sin envío): lo que suman categorías y productos
 * @param totalCents     lo cobrado: productos + envío
 */
public record SalesReport(LocalDate from, LocalDate to, Grouping grouping, Instant generatedAt, String generatedBy,
                          int orders, int units, long productsCents, long shippingCents, long totalCents,
                          List<PeriodRow> periods, List<CategoryRow> categories, List<ProductRow> products,
                          List<MethodRow> methods) {

    /** Hasta {@link #DAY_LIMIT} días se agrupa por día; más largo, por mes. */
    public enum Grouping {
        DAY, MONTH;

        public static final int DAY_LIMIT = 62;
    }

    /** @param start primer día del periodo (el día, o el 1 del mes) */
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

    /** Ticket promedio (total cobrado / pedidos), redondeado al céntimo. */
    public long averageTicketCents() {
        return orders == 0 ? 0 : Math.round((double) totalCents / orders);
    }

    /** Participación de un monto en las ventas de productos, en %, con un decimal. */
    public double sharePercent(long cents) {
        return productsCents == 0 ? 0 : Math.round(cents * 1000.0 / productsCents) / 10.0;
    }
}
