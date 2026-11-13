package com.sportforce3d.repository;

import com.sportforce3d.db.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardRepository {
    private static final String PAID = "('PAGADO', 'EN_PRODUCCION', 'ENVIADO', 'ENTREGADO')";

    public record Total(long cents, int count) {
    }

    public record TopProduct(long productId, String name, int units, long revenueCents) {
    }

    private final Database db;

    public DashboardRepository(Database db) {
        this.db = db;
    }

    public Total salesBetween(LocalDate from, LocalDate to) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT COALESCE(sum(amount_cents), 0), count(*) FROM payments
                    WHERE status = 'APROBADO' AND date(verified_at, '-5 hours') BETWEEN ? AND ?
                    """)) {
                ps.setString(1, from.toString());
                ps.setString(2, to.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return new Total(rs.getLong(1), rs.getInt(2));
                }
            }
        });
    }

    public Map<LocalDate, Long> dailySales(LocalDate from, LocalDate to) {
        Map<LocalDate, Long> map = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            map.put(d, 0L);
        }
        db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT date(verified_at, '-5 hours') AS day, sum(amount_cents) FROM payments
                    WHERE status = 'APROBADO' AND date(verified_at, '-5 hours') BETWEEN ? AND ?
                    GROUP BY day
                    """)) {
                ps.setString(1, from.toString());
                ps.setString(2, to.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        map.put(LocalDate.parse(rs.getString(1)), rs.getLong(2));
                    }
                }
            }
            return null;
        });
        return map;
    }

    public List<TopProduct> topProducts(int limit) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT oi.product_id, oi.product_name, sum(oi.quantity) AS units, sum(oi.line_total_cents) AS revenue
                    FROM order_items oi JOIN orders o ON o.id = oi.order_id
                    WHERE o.status IN %s
                    GROUP BY oi.product_id ORDER BY units DESC, revenue DESC LIMIT ?
                    """.formatted(PAID))) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<TopProduct> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(new TopProduct(rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getLong(4)));
                    }
                    return list;
                }
            }
        });
    }
}
