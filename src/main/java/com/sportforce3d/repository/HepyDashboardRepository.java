package com.sportforce3d.repository;

import com.sportforce3d.db.HepyDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HepyDashboardRepository {
    private static final String HEPY_PAID = "('PAGADO', 'EN_PRODUCCION', 'ENVIADO', 'ENTREGADO')";

    public record HepyTotal(long hepyCents, int hepyCount) {
    }

    public record HepyTopProduct(long hepyProductId, String hepyName, int hepyUnits, long hepyRevenueCents) {
    }

    private final HepyDatabase hepyDb;

    public HepyDashboardRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    public HepyTotal hepySalesBetween(LocalDate hepyFrom, LocalDate hepyTo) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT COALESCE(sum(amount_cents), 0), count(*) FROM payments
                    WHERE status = 'APROBADO' AND date(verified_at, '-5 hours') BETWEEN ? AND ?
                    """)) {
                hepyPs.setString(1, hepyFrom.toString());
                hepyPs.setString(2, hepyTo.toString());
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    hepyRs.next();
                    return new HepyTotal(hepyRs.getLong(1), hepyRs.getInt(2));
                }
            }
        });
    }

    public Map<LocalDate, Long> hepyDailySales(LocalDate hepyFrom, LocalDate hepyTo) {
        Map<LocalDate, Long> hepyMap = new LinkedHashMap<>();
        for (LocalDate hepyD = hepyFrom; !hepyD.isAfter(hepyTo); hepyD = hepyD.plusDays(1)) {
            hepyMap.put(hepyD, 0L);
        }
        hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT date(verified_at, '-5 hours') AS day, sum(amount_cents) FROM payments
                    WHERE status = 'APROBADO' AND date(verified_at, '-5 hours') BETWEEN ? AND ?
                    GROUP BY day
                    """)) {
                hepyPs.setString(1, hepyFrom.toString());
                hepyPs.setString(2, hepyTo.toString());
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    while (hepyRs.next()) {
                        hepyMap.put(LocalDate.parse(hepyRs.getString(1)), hepyRs.getLong(2));
                    }
                }
            }
            return null;
        });
        return hepyMap;
    }

    public List<HepyTopProduct> hepyTopProducts(int hepyLimit) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT oi.product_id, oi.product_name, sum(oi.quantity) AS units, sum(oi.line_total_cents) AS revenue
                    FROM order_items oi JOIN orders o ON o.id = oi.order_id
                    WHERE o.status IN %s
                    GROUP BY oi.product_id ORDER BY units DESC, revenue DESC LIMIT ?
                    """.formatted(HEPY_PAID))) {
                hepyPs.setInt(1, hepyLimit);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    List<HepyTopProduct> hepyList = new ArrayList<>();
                    while (hepyRs.next()) {
                        hepyList.add(new HepyTopProduct(hepyRs.getLong(1), hepyRs.getString(2), hepyRs.getInt(3), hepyRs.getLong(4)));
                    }
                    return hepyList;
                }
            }
        });
    }
}
