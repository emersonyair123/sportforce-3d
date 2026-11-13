package com.sportforce3d.repository;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.model.PpqhSalesReport.PpqhCategoryRow;
import com.sportforce3d.model.PpqhSalesReport.PpqhMethodRow;
import com.sportforce3d.model.PpqhSalesReport.PpqhProductRow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HepyReportRepository {
    private static final String HEPY_SOLD = """
            SELECT p.order_id, p.method, p.amount_cents, date(p.verified_at, '-5 hours') AS day
            FROM payments p
            WHERE p.status = 'APROBADO' AND date(p.verified_at, '-5 hours') BETWEEN ? AND ?
            """;

    public record HepyPeriodTotal(int hepyOrders, int hepyUnits, long hepyTotalCents) {
    }

    public record HepyTotals(int hepyOrders, int hepyUnits, long hepyProductsCents, long hepyShippingCents, long hepyTotalCents) {
    }

    private final HepyDatabase hepyDb;

    public HepyReportRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    public HepyTotals hepyTotals(LocalDate hepyFrom, LocalDate hepyTo) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT count(*), COALESCE(sum((SELECT sum(quantity) FROM order_items WHERE order_id = s.order_id)), 0),
                           COALESCE(sum(o.subtotal_cents + o.surcharge_cents), 0), COALESCE(sum(o.shipping_cents), 0),
                           COALESCE(sum(s.amount_cents), 0)
                    FROM (%s) s JOIN orders o ON o.id = s.order_id
                    """.formatted(HEPY_SOLD))) {
                hepyBindRange(hepyPs, hepyFrom, hepyTo);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    hepyRs.next();
                    return new HepyTotals(hepyRs.getInt(1), hepyRs.getInt(2), hepyRs.getLong(3), hepyRs.getLong(4), hepyRs.getLong(5));
                }
            }
        });
    }

    public Map<String, HepyPeriodTotal> hepyByPeriod(LocalDate hepyFrom, LocalDate hepyTo, boolean hepyMonthly) {
        String hepyKey = hepyMonthly ? "substr(s.day, 1, 7)" : "s.day";
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT %s AS period, count(*),
                           COALESCE(sum((SELECT sum(quantity) FROM order_items WHERE order_id = s.order_id)), 0),
                           sum(s.amount_cents)
                    FROM (%s) s GROUP BY period ORDER BY period
                    """.formatted(hepyKey, HEPY_SOLD))) {
                hepyBindRange(hepyPs, hepyFrom, hepyTo);
                Map<String, HepyPeriodTotal> hepyMap = new LinkedHashMap<>();
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    while (hepyRs.next()) {
                        hepyMap.put(hepyRs.getString(1), new HepyPeriodTotal(hepyRs.getInt(2), hepyRs.getInt(3), hepyRs.getLong(4)));
                    }
                }
                return hepyMap;
            }
        });
    }

    public List<PpqhCategoryRow> hepyByCategory(LocalDate hepyFrom, LocalDate hepyTo) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT cat.name, count(DISTINCT oi.order_id), sum(oi.quantity), sum(oi.line_total_cents) AS revenue
                    FROM (%s) s
                    JOIN order_items oi ON oi.order_id = s.order_id
                    JOIN products pr ON pr.id = oi.product_id
                    JOIN categories cat ON cat.id = pr.category_id
                    GROUP BY cat.id ORDER BY revenue DESC, cat.name
                    """.formatted(HEPY_SOLD))) {
                hepyBindRange(hepyPs, hepyFrom, hepyTo);
                List<PpqhCategoryRow> hepyList = new ArrayList<>();
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    while (hepyRs.next()) {
                        hepyList.add(new PpqhCategoryRow(hepyRs.getString(1), hepyRs.getInt(2), hepyRs.getInt(3), hepyRs.getLong(4)));
                    }
                }
                return hepyList;
            }
        });
    }

    public List<PpqhProductRow> hepyByProduct(LocalDate hepyFrom, LocalDate hepyTo) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT pr.sku, pr.name, cat.name, count(DISTINCT oi.order_id), sum(oi.quantity),
                           sum(oi.line_total_cents) AS revenue
                    FROM (%s) s
                    JOIN order_items oi ON oi.order_id = s.order_id
                    JOIN products pr ON pr.id = oi.product_id
                    JOIN categories cat ON cat.id = pr.category_id
                    GROUP BY pr.id ORDER BY revenue DESC, pr.name
                    """.formatted(HEPY_SOLD))) {
                hepyBindRange(hepyPs, hepyFrom, hepyTo);
                List<PpqhProductRow> hepyList = new ArrayList<>();
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    while (hepyRs.next()) {
                        hepyList.add(new PpqhProductRow(hepyRs.getString(1), hepyRs.getString(2), hepyRs.getString(3), hepyRs.getInt(4),
                                hepyRs.getInt(5), hepyRs.getLong(6)));
                    }
                }
                return hepyList;
            }
        });
    }

    public List<PpqhMethodRow> hepyByMethod(LocalDate hepyFrom, LocalDate hepyTo) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT s.method, count(*), sum(s.amount_cents) AS total FROM (%s) s
                    GROUP BY s.method ORDER BY total DESC
                    """.formatted(HEPY_SOLD))) {
                hepyBindRange(hepyPs, hepyFrom, hepyTo);
                List<PpqhMethodRow> hepyList = new ArrayList<>();
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    while (hepyRs.next()) {
                        hepyList.add(new PpqhMethodRow(PpqhPaymentMethod.valueOf(hepyRs.getString(1)), hepyRs.getInt(2), hepyRs.getLong(3)));
                    }
                }
                return hepyList;
            }
        });
    }

    private static void hepyBindRange(PreparedStatement hepyPs, LocalDate hepyFrom, LocalDate hepyTo) throws SQLException {
        hepyPs.setString(1, hepyFrom.toString());
        hepyPs.setString(2, hepyTo.toString());
    }
}
