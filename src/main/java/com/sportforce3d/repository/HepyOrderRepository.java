package com.sportforce3d.repository;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.model.PpqhAddress;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhOrderItem;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.PpqhOrderSummary;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.model.PpqhStatusChange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HepyOrderRepository {
    public record HepyNewOrder(long hepyUserId, PpqhPaymentMethod hepyMethod, long hepySubtotalCents, long hepySurchargeCents,
                           long hepyShippingCents, PpqhAddress hepyAddress, String hepyNotes, String hepyPlacedAt) {

        public long hepyTotalCents() {
            return hepySubtotalCents + hepySurchargeCents + hepyShippingCents;
        }
    }

    public record HepyNewItem(long hepyProductId, long hepyVariantId, String hepyProductName, String hepySku, String hepySizeCode,
                          String hepyColorHex, int hepyQuantity, long hepyUnitPriceCents, long hepySurchargeCents,
                          Long hepyDesignId, String hepyDesignName, Integer hepyDesignVersion, String hepyDesignJson,
                          String hepyDesignFrontPath, String hepyDesignBackPath) {
    }

    public record HepyReservation(long hepyVariantId, int hepyQuantity) {
    }

    private static final String HEPY_SUMMARY_SQL = """
            SELECT o.id, o.order_number, o.status, o.payment_method, o.placed_at, o.total_cents,
                   (SELECT COALESCE(sum(quantity), 0) FROM order_items oi WHERE oi.order_id = o.id) AS item_count,
                   u.full_name
            FROM orders o JOIN users u ON u.id = o.user_id
            """;

    private final HepyDatabase hepyDb;

    public HepyOrderRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    public int hepyNextSequence(Connection hepyC, int hepyYear) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                INSERT INTO order_counters (year, last_value) VALUES (?, 1)
                ON CONFLICT (year) DO UPDATE SET last_value = last_value + 1
                RETURNING last_value
                """)) {
            hepyPs.setInt(1, hepyYear);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                hepyRs.next();
                return hepyRs.getInt(1);
            }
        }
    }

    public long hepyInsertOrder(Connection hepyC, String hepyOrderNumber, HepyNewOrder hepyO) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                INSERT INTO orders (order_number, user_id, status, payment_method, subtotal_cents, surcharge_cents,
                                    shipping_cents, total_cents, ship_recipient, ship_phone, ship_department,
                                    ship_province, ship_district, ship_line1, ship_reference, notes, placed_at)
                VALUES (?, ?, 'PENDIENTE_PAGO', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            PpqhAddress hepyA = hepyO.hepyAddress();
            HepyJdbc.hepyBind(hepyPs, java.util.Arrays.asList(hepyOrderNumber, hepyO.hepyUserId(), hepyO.hepyMethod().name(), hepyO.hepySubtotalCents(),
                    hepyO.hepySurchargeCents(), hepyO.hepyShippingCents(), hepyO.hepyTotalCents(), hepyA.ppqhRecipient(), hepyA.ppqhPhone(), hepyA.ppqhDepartment(),
                    hepyA.ppqhProvince(), hepyA.ppqhDistrict(), hepyA.ppqhLine1(), hepyA.ppqhReference(), hepyO.hepyNotes(), hepyO.hepyPlacedAt()));
            hepyPs.executeUpdate();
            try (ResultSet hepyKeys = hepyPs.getGeneratedKeys()) {
                hepyKeys.next();
                return hepyKeys.getLong(1);
            }
        }
    }

    public void hepyInsertItem(Connection hepyC, long hepyOrderId, HepyNewItem hepyI) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                INSERT INTO order_items (order_id, product_id, variant_id, product_name, sku, size_code, color_hex,
                                         quantity, unit_price_cents, surcharge_cents, line_total_cents, design_id,
                                         design_name, design_version, design_snapshot_json, design_front_path,
                                         design_back_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            HepyJdbc.hepyBind(hepyPs, java.util.Arrays.asList(hepyOrderId, hepyI.hepyProductId(), hepyI.hepyVariantId(), hepyI.hepyProductName(), hepyI.hepySku(),
                    hepyI.hepySizeCode(), hepyI.hepyColorHex(), hepyI.hepyQuantity(), hepyI.hepyUnitPriceCents(), hepyI.hepySurchargeCents(),
                    (hepyI.hepyUnitPriceCents() + hepyI.hepySurchargeCents()) * hepyI.hepyQuantity(), hepyI.hepyDesignId(), hepyI.hepyDesignName(),
                    hepyI.hepyDesignVersion(), hepyI.hepyDesignJson(), hepyI.hepyDesignFrontPath(), hepyI.hepyDesignBackPath()));
            hepyPs.executeUpdate();
        }
    }

    public void hepyInsertHistory(Connection hepyC, long hepyOrderId, PpqhOrderStatus hepyFrom, PpqhOrderStatus hepyTo, Long hepyChangedBy,
                              String hepyNote, String hepyCreatedAt) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                INSERT INTO order_status_history (order_id, from_status, to_status, changed_by, note, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            HepyJdbc.hepyBind(hepyPs, java.util.Arrays.asList(hepyOrderId, hepyFrom == null ? null : hepyFrom.name(), hepyTo.name(), hepyChangedBy,
                    hepyNote, hepyCreatedAt));
            hepyPs.executeUpdate();
        }
    }

    public void hepyInsertPendingPayment(Connection hepyC, long hepyOrderId, PpqhPaymentMethod hepyMethod, long hepyAmountCents)
            throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(
                "INSERT INTO payments (order_id, method, status, amount_cents) VALUES (?, ?, 'PENDIENTE', ?)")) {
            hepyPs.setLong(1, hepyOrderId);
            hepyPs.setString(2, hepyMethod.name());
            hepyPs.setLong(3, hepyAmountCents);
            hepyPs.executeUpdate();
        }
    }

    public boolean hepyReserveStock(Connection hepyC, long hepyVariantId, int hepyQuantity) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(
                "UPDATE inventory SET reserved = reserved + ? WHERE variant_id = ? AND quantity - reserved >= ?")) {
            hepyPs.setInt(1, hepyQuantity);
            hepyPs.setLong(2, hepyVariantId);
            hepyPs.setInt(3, hepyQuantity);
            return hepyPs.executeUpdate() == 1;
        }
    }

    public boolean hepyCommitStock(Connection hepyC, long hepyVariantId, int hepyQuantity) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                UPDATE inventory SET quantity = quantity - ?, reserved = reserved - ?
                WHERE variant_id = ? AND reserved >= ? AND quantity >= ?
                """)) {
            HepyJdbc.hepyBind(hepyPs, List.of(hepyQuantity, hepyQuantity, hepyVariantId, hepyQuantity, hepyQuantity));
            return hepyPs.executeUpdate() == 1;
        }
    }

    public void hepyReleaseStock(Connection hepyC, long hepyVariantId, int hepyQuantity) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(
                "UPDATE inventory SET reserved = max(reserved - ?, 0) WHERE variant_id = ?")) {
            hepyPs.setInt(1, hepyQuantity);
            hepyPs.setLong(2, hepyVariantId);
            hepyPs.executeUpdate();
        }
    }

    public List<HepyReservation> hepyReservations(Connection hepyC, long hepyOrderId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(
                "SELECT variant_id, quantity FROM order_items WHERE order_id = ? AND variant_id IS NOT NULL")) {
            hepyPs.setLong(1, hepyOrderId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                List<HepyReservation> hepyList = new ArrayList<>();
                while (hepyRs.next()) {
                    hepyList.add(new HepyReservation(hepyRs.getLong(1), hepyRs.getInt(2)));
                }
                return hepyList;
            }
        }
    }

    public boolean hepyUpdateStatus(Connection hepyC, long hepyOrderId, PpqhOrderStatus hepyExpected, PpqhOrderStatus hepyNext) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("UPDATE orders SET status = ? WHERE id = ? AND status = ?")) {
            hepyPs.setString(1, hepyNext.name());
            hepyPs.setLong(2, hepyOrderId);
            hepyPs.setString(3, hepyExpected.name());
            return hepyPs.executeUpdate() == 1;
        }
    }

    public void hepyRejectOpenPayments(Connection hepyC, long hepyOrderId, String hepyReason) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                UPDATE payments SET status = 'RECHAZADO', rejection_reason = ?
                WHERE order_id = ? AND status IN ('PENDIENTE', 'EN_VERIFICACION')
                """)) {
            hepyPs.setString(1, hepyReason);
            hepyPs.setLong(2, hepyOrderId);
            hepyPs.executeUpdate();
        }
    }

    public List<PpqhOrderSummary> hepyFindByUser(long hepyUserId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(HEPY_SUMMARY_SQL + " WHERE o.user_id = ? ORDER BY o.placed_at DESC, o.id DESC")) {
                hepyPs.setLong(1, hepyUserId);
                return hepySummaries(hepyPs);
            }
        });
    }

    public List<PpqhOrderSummary> hepyFindForAdmin(PpqhOrderStatus hepyStatus, String hepyText, int hepyLimit) {
        return hepyDb.hepyWithConnection(hepyC -> {
            StringBuilder hepySql = new StringBuilder(HEPY_SUMMARY_SQL).append(" WHERE 1 = 1");
            List<Object> hepyParams = new ArrayList<>();
            if (hepyStatus != null) {
                hepySql.append(" AND o.status = ?");
                hepyParams.add(hepyStatus.name());
            }
            if (hepyText != null && !hepyText.isBlank()) {
                hepySql.append(" AND (lower(o.order_number) LIKE ? OR lower(u.full_name) LIKE ? OR lower(u.email) LIKE ?)");
                String hepyLike = "%" + hepyText.trim().toLowerCase(java.util.Locale.ROOT) + "%";
                hepyParams.add(hepyLike);
                hepyParams.add(hepyLike);
                hepyParams.add(hepyLike);
            }
            hepySql.append(" ORDER BY o.placed_at DESC, o.id DESC LIMIT ?");
            hepyParams.add(hepyLimit);
            try (PreparedStatement hepyPs = hepyC.prepareStatement(hepySql.toString())) {
                HepyJdbc.hepyBind(hepyPs, hepyParams);
                return hepySummaries(hepyPs);
            }
        });
    }

    public java.util.Map<PpqhOrderStatus, Integer> hepyCountByStatus() {
        return hepyDb.hepyWithConnection(hepyC -> {
            java.util.Map<PpqhOrderStatus, Integer> hepyMap = new java.util.EnumMap<>(PpqhOrderStatus.class);
            for (PpqhOrderStatus hepyS : PpqhOrderStatus.values()) {
                hepyMap.put(hepyS, 0);
            }
            try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT status, count(*) FROM orders GROUP BY status");
                 ResultSet hepyRs = hepyPs.executeQuery()) {
                while (hepyRs.next()) {
                    hepyMap.put(PpqhOrderStatus.valueOf(hepyRs.getString(1)), hepyRs.getInt(2));
                }
            }
            return hepyMap;
        });
    }

    public record HepyItemWithOrder(long hepyOrderId, String hepyOrderNumber, long hepyUserId, PpqhOrderItem hepyItem) {
    }

    public Optional<HepyItemWithOrder> hepyFindItem(long hepyItemId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT oi.order_id, o.order_number, o.user_id FROM order_items oi JOIN orders o ON o.id = oi.order_id
                    WHERE oi.id = ?
                    """)) {
                hepyPs.setLong(1, hepyItemId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    if (!hepyRs.next()) {
                        return Optional.empty();
                    }
                    long hepyOrderId = hepyRs.getLong(1);
                    String hepyNumber = hepyRs.getString(2);
                    long hepyUserId = hepyRs.getLong(3);
                    return hepyItems(hepyC, hepyOrderId).stream().filter(hepyI -> hepyI.ppqhId() == hepyItemId).findFirst()
                            .map(hepyI -> new HepyItemWithOrder(hepyOrderId, hepyNumber, hepyUserId, hepyI));
                }
            }
        });
    }

    public Optional<PpqhOrderDetail> hepyFindDetail(long hepyOrderId) {
        return hepyDb.hepyWithConnection(hepyC -> hepyFindDetail(hepyC, hepyOrderId));
    }

    public Optional<PpqhOrderDetail> hepyFindDetail(Connection hepyC, long hepyOrderId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                SELECT o.*, u.full_name, u.email FROM orders o JOIN users u ON u.id = o.user_id WHERE o.id = ?
                """)) {
            hepyPs.setLong(1, hepyOrderId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                if (!hepyRs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new PpqhOrderDetail(
                        hepyRs.getLong("id"), hepyRs.getString("order_number"), hepyRs.getLong("user_id"),
                        hepyRs.getString("full_name"), hepyRs.getString("email"),
                        PpqhOrderStatus.valueOf(hepyRs.getString("status")),
                        PpqhPaymentMethod.valueOf(hepyRs.getString("payment_method")),
                        hepyRs.getLong("subtotal_cents"), hepyRs.getLong("surcharge_cents"), hepyRs.getLong("shipping_cents"),
                        hepyRs.getLong("total_cents"), hepyRs.getString("ship_recipient"), hepyRs.getString("ship_phone"),
                        hepyRs.getString("ship_department"), hepyRs.getString("ship_province"), hepyRs.getString("ship_district"),
                        hepyRs.getString("ship_line1"), hepyRs.getString("ship_reference"), hepyRs.getString("notes"),
                        hepyRs.getString("placed_at"), hepyItems(hepyC, hepyOrderId), hepyHistory(hepyC, hepyOrderId),
                        HepyPaymentRepository.hepyForOrder(hepyC, hepyOrderId)));
            }
        }
    }

    private static List<PpqhOrderItem> hepyItems(Connection hepyC, long hepyOrderId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT * FROM order_items WHERE order_id = ? ORDER BY id")) {
            hepyPs.setLong(1, hepyOrderId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                List<PpqhOrderItem> hepyList = new ArrayList<>();
                while (hepyRs.next()) {
                    hepyList.add(new PpqhOrderItem(hepyRs.getLong("id"), hepyRs.getLong("product_id"), hepyRs.getString("product_name"),
                            hepyRs.getString("sku"), hepyRs.getString("size_code"), hepyRs.getString("color_hex"),
                            hepyRs.getInt("quantity"), hepyRs.getLong("unit_price_cents"), hepyRs.getLong("surcharge_cents"),
                            hepyRs.getLong("line_total_cents"), HepyJdbc.hepyGetLongOrNull(hepyRs, "design_id"),
                            hepyRs.getString("design_name"), (Integer) hepyRs.getObject("design_version"),
                            hepyRs.getString("design_snapshot_json"), hepyRs.getString("design_front_path"),
                            hepyRs.getString("design_back_path")));
                }
                return hepyList;
            }
        }
    }

    private static List<PpqhStatusChange> hepyHistory(Connection hepyC, long hepyOrderId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                SELECT h.from_status, h.to_status, u.full_name, h.note, h.created_at
                FROM order_status_history h LEFT JOIN users u ON u.id = h.changed_by
                WHERE h.order_id = ? ORDER BY h.created_at, h.id
                """)) {
            hepyPs.setLong(1, hepyOrderId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                List<PpqhStatusChange> hepyList = new ArrayList<>();
                while (hepyRs.next()) {
                    String hepyFrom = hepyRs.getString(1);
                    hepyList.add(new PpqhStatusChange(hepyFrom == null ? null : PpqhOrderStatus.valueOf(hepyFrom),
                            PpqhOrderStatus.valueOf(hepyRs.getString(2)), hepyRs.getString(3), hepyRs.getString(4), hepyRs.getString(5)));
                }
                return hepyList;
            }
        }
    }

    private static List<PpqhOrderSummary> hepySummaries(PreparedStatement hepyPs) throws SQLException {
        try (ResultSet hepyRs = hepyPs.executeQuery()) {
            List<PpqhOrderSummary> hepyList = new ArrayList<>();
            while (hepyRs.next()) {
                hepyList.add(new PpqhOrderSummary(hepyRs.getLong("id"), hepyRs.getString("order_number"),
                        PpqhOrderStatus.valueOf(hepyRs.getString("status")),
                        PpqhPaymentMethod.valueOf(hepyRs.getString("payment_method")), hepyRs.getString("placed_at"),
                        hepyRs.getLong("total_cents"), hepyRs.getInt("item_count"), hepyRs.getString("full_name")));
            }
            return hepyList;
        }
    }
}
