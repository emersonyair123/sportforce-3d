package com.sportforce3d.repository;

import com.sportforce3d.db.Database;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.model.SalesReport.CategoryRow;
import com.sportforce3d.model.SalesReport.MethodRow;
import com.sportforce3d.model.SalesReport.ProductRow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas de los reportes. Igual que el dashboard, una venta es un pago APROBADO y cuenta el día de Lima en que se
 * aprobó ({@code date(verified_at, '-5 hours')}: Lima es UTC−5 todo el año). Cada pedido tiene a lo sumo un pago
 * aprobado (los rechazados abren uno nuevo).
 */
public class ReportRepository {

    /** Pedidos vendidos en [?, ?] con su día de venta y monto cobrado. */
    private static final String SOLD = """
            SELECT p.order_id, p.method, p.amount_cents, date(p.verified_at, '-5 hours') AS day
            FROM payments p
            WHERE p.status = 'APROBADO' AND date(p.verified_at, '-5 hours') BETWEEN ? AND ?
            """;

    /** Totales de un día o mes. */
    public record PeriodTotal(int orders, int units, long totalCents) {
    }

    public record Totals(int orders, int units, long productsCents, long shippingCents, long totalCents) {
    }

    private final Database db;

    public ReportRepository(Database db) {
        this.db = db;
    }

    public Totals totals(LocalDate from, LocalDate to) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT count(*), COALESCE(sum((SELECT sum(quantity) FROM order_items WHERE order_id = s.order_id)), 0),
                           COALESCE(sum(o.subtotal_cents + o.surcharge_cents), 0), COALESCE(sum(o.shipping_cents), 0),
                           COALESCE(sum(s.amount_cents), 0)
                    FROM (%s) s JOIN orders o ON o.id = s.order_id
                    """.formatted(SOLD))) {
                bindRange(ps, from, to);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return new Totals(rs.getInt(1), rs.getInt(2), rs.getLong(3), rs.getLong(4), rs.getLong(5));
                }
            }
        });
    }

    /**
     * Ventas por periodo, clave = 'YYYY-MM-DD' (por día) o 'YYYY-MM' (por mes). Solo periodos con ventas: el service
     * completa los vacíos.
     */
    public Map<String, PeriodTotal> byPeriod(LocalDate from, LocalDate to, boolean monthly) {
        String key = monthly ? "substr(s.day, 1, 7)" : "s.day";
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT %s AS period, count(*),
                           COALESCE(sum((SELECT sum(quantity) FROM order_items WHERE order_id = s.order_id)), 0),
                           sum(s.amount_cents)
                    FROM (%s) s GROUP BY period ORDER BY period
                    """.formatted(key, SOLD))) {
                bindRange(ps, from, to);
                Map<String, PeriodTotal> map = new LinkedHashMap<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        map.put(rs.getString(1), new PeriodTotal(rs.getInt(2), rs.getInt(3), rs.getLong(4)));
                    }
                }
                return map;
            }
        });
    }

    /** Ventas de productos (sin envío) por categoría actual del producto, de mayor a menor. */
    public List<CategoryRow> byCategory(LocalDate from, LocalDate to) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT cat.name, count(DISTINCT oi.order_id), sum(oi.quantity), sum(oi.line_total_cents) AS revenue
                    FROM (%s) s
                    JOIN order_items oi ON oi.order_id = s.order_id
                    JOIN products pr ON pr.id = oi.product_id
                    JOIN categories cat ON cat.id = pr.category_id
                    GROUP BY cat.id ORDER BY revenue DESC, cat.name
                    """.formatted(SOLD))) {
                bindRange(ps, from, to);
                List<CategoryRow> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new CategoryRow(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getLong(4)));
                    }
                }
                return list;
            }
        });
    }

    /** Ventas por producto (incluye recargos de diseño), de mayor a menor. Nombre y SKU actuales del producto. */
    public List<ProductRow> byProduct(LocalDate from, LocalDate to) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT pr.sku, pr.name, cat.name, count(DISTINCT oi.order_id), sum(oi.quantity),
                           sum(oi.line_total_cents) AS revenue
                    FROM (%s) s
                    JOIN order_items oi ON oi.order_id = s.order_id
                    JOIN products pr ON pr.id = oi.product_id
                    JOIN categories cat ON cat.id = pr.category_id
                    GROUP BY pr.id ORDER BY revenue DESC, pr.name
                    """.formatted(SOLD))) {
                bindRange(ps, from, to);
                List<ProductRow> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new ProductRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                                rs.getInt(5), rs.getLong(6)));
                    }
                }
                return list;
            }
        });
    }

    public List<MethodRow> byMethod(LocalDate from, LocalDate to) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT s.method, count(*), sum(s.amount_cents) AS total FROM (%s) s
                    GROUP BY s.method ORDER BY total DESC
                    """.formatted(SOLD))) {
                bindRange(ps, from, to);
                List<MethodRow> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new MethodRow(PaymentMethod.valueOf(rs.getString(1)), rs.getInt(2), rs.getLong(3)));
                    }
                }
                return list;
            }
        });
    }

    private static void bindRange(PreparedStatement ps, LocalDate from, LocalDate to) throws SQLException {
        ps.setString(1, from.toString());
        ps.setString(2, to.toString());
    }
}