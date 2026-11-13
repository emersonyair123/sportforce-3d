package com.sportforce3d.repository;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.model.HepyCartLine;
import com.sportforce3d.model.HepyGarment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HepyCartRepository {
    public record HepyExistingItem(long hepyItemId, int hepyQuantity) {
    }

    private static final String HEPY_LINES_SQL = """
            SELECT ci.id AS item_id, p.id AS product_id, v.id AS variant_id, p.name AS product_name, v.sku,
                   p.garment_type, s.name AS size_name, v.color_name, v.color_hex,
                   (SELECT pi.path FROM product_images pi WHERE pi.product_id = p.id
                     ORDER BY pi.sort_order, pi.id LIMIT 1) AS image_path,
                   p.base_price_cents + v.price_delta_cents AS unit_price_cents,
                   ci.surcharge_cents, ci.quantity,
                   max(COALESCE(i.quantity, 0) - COALESCE(i.reserved, 0), 0) AS available,
                   (p.active = 1 AND p.deleted_at IS NULL AND v.active = 1
                    AND (ci.design_id IS NULL OR (d.id IS NOT NULL AND d.deleted_at IS NULL))) AS purchasable,
                   ci.design_id, d.name AS design_name, d.thumbnail_front AS design_thumbnail
            FROM carts c
            JOIN cart_items ci ON ci.cart_id = c.id
            JOIN product_variants v ON v.id = ci.variant_id
            JOIN products p ON p.id = v.product_id
            JOIN sizes s ON s.id = v.size_id
            LEFT JOIN inventory i ON i.variant_id = v.id
            LEFT JOIN designs d ON d.id = ci.design_id
            WHERE c.user_id = ?
            ORDER BY ci.created_at, ci.id
            """;

    private final HepyDatabase hepyDb;

    public HepyCartRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    public List<HepyCartLine> hepyFindLines(long hepyUserId) {
        return hepyDb.hepyWithConnection(hepyC -> hepyFindLines(hepyC, hepyUserId));
    }

    public List<HepyCartLine> hepyFindLines(Connection hepyC, long hepyUserId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(HEPY_LINES_SQL)) {
            hepyPs.setLong(1, hepyUserId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                List<HepyCartLine> hepyLines = new ArrayList<>();
                while (hepyRs.next()) {
                    hepyLines.add(new HepyCartLine(hepyRs.getLong("item_id"), hepyRs.getLong("product_id"), hepyRs.getLong("variant_id"),
                            hepyRs.getString("product_name"), hepyRs.getString("sku"),
                            HepyGarment.valueOf(hepyRs.getString("garment_type")), hepyRs.getString("size_name"),
                            hepyRs.getString("color_name"), hepyRs.getString("color_hex"), hepyRs.getString("image_path"),
                            hepyRs.getLong("unit_price_cents"), hepyRs.getLong("surcharge_cents"), hepyRs.getInt("quantity"),
                            hepyRs.getInt("available"), hepyRs.getInt("purchasable") == 1,
                            HepyJdbc.hepyGetLongOrNull(hepyRs, "design_id"), hepyRs.getString("design_name"),
                            hepyRs.getString("design_thumbnail")));
                }
                return hepyLines;
            }
        }
    }

    public int hepyCountUnits(long hepyUserId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT COALESCE(sum(ci.quantity), 0) FROM carts c JOIN cart_items ci ON ci.cart_id = c.id
                    WHERE c.user_id = ?
                    """)) {
                hepyPs.setLong(1, hepyUserId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    hepyRs.next();
                    return hepyRs.getInt(1);
                }
            }
        });
    }

    public Optional<HepyExistingItem> hepyFindStandardItem(long hepyUserId, long hepyVariantId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT ci.id, ci.quantity FROM carts c JOIN cart_items ci ON ci.cart_id = c.id
                    WHERE c.user_id = ? AND ci.variant_id = ? AND ci.design_id IS NULL
                    """)) {
                hepyPs.setLong(1, hepyUserId);
                hepyPs.setLong(2, hepyVariantId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    return hepyRs.next() ? Optional.of(new HepyExistingItem(hepyRs.getLong(1), hepyRs.getInt(2))) : Optional.empty();
                }
            }
        });
    }

    public Optional<HepyExistingItem> hepyFindDesignItem(long hepyUserId, long hepyVariantId, long hepyDesignId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT ci.id, ci.quantity FROM carts c JOIN cart_items ci ON ci.cart_id = c.id
                    WHERE c.user_id = ? AND ci.variant_id = ? AND ci.design_id = ?
                    """)) {
                hepyPs.setLong(1, hepyUserId);
                hepyPs.setLong(2, hepyVariantId);
                hepyPs.setLong(3, hepyDesignId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    return hepyRs.next() ? Optional.of(new HepyExistingItem(hepyRs.getLong(1), hepyRs.getInt(2))) : Optional.empty();
                }
            }
        });
    }

    public void hepyAddDesignItem(long hepyUserId, long hepyVariantId, long hepyDesignId, int hepyQuantity, long hepyUnitPriceCents,
                              long hepySurchargeCents) {
        hepyDb.hepyInTransaction(hepyC -> {
            long hepyCartId = hepyEnsureCart(hepyC, hepyUserId);
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    INSERT INTO cart_items (cart_id, variant_id, design_id, quantity, unit_price_cents, surcharge_cents)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                HepyJdbc.hepyBind(hepyPs, List.of(hepyCartId, hepyVariantId, hepyDesignId, hepyQuantity, hepyUnitPriceCents, hepySurchargeCents));
                hepyPs.executeUpdate();
            }
            hepyTouch(hepyC, hepyCartId);
            return null;
        });
    }

    public void hepyUpdateSurcharge(long hepyUserId, long hepyItemId, long hepySurchargeCents) {
        hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    UPDATE cart_items SET surcharge_cents = ?
                    WHERE id = ? AND cart_id IN (SELECT id FROM carts WHERE user_id = ?)
                    """)) {
                hepyPs.setLong(1, hepySurchargeCents);
                hepyPs.setLong(2, hepyItemId);
                hepyPs.setLong(3, hepyUserId);
                return hepyPs.executeUpdate();
            }
        });
    }

    public void hepyAddItem(long hepyUserId, long hepyVariantId, int hepyQuantity, long hepyUnitPriceCents) {
        hepyDb.hepyInTransaction(hepyC -> {
            long hepyCartId = hepyEnsureCart(hepyC, hepyUserId);
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    INSERT INTO cart_items (cart_id, variant_id, quantity, unit_price_cents, surcharge_cents)
                    VALUES (?, ?, ?, ?, 0)
                    """)) {
                hepyPs.setLong(1, hepyCartId);
                hepyPs.setLong(2, hepyVariantId);
                hepyPs.setInt(3, hepyQuantity);
                hepyPs.setLong(4, hepyUnitPriceCents);
                hepyPs.executeUpdate();
            }
            hepyTouch(hepyC, hepyCartId);
            return null;
        });
    }

    public boolean hepyUpdateQuantity(long hepyUserId, long hepyItemId, int hepyQuantity, long hepyUnitPriceCents) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    UPDATE cart_items SET quantity = ?, unit_price_cents = ?
                    WHERE id = ? AND cart_id IN (SELECT id FROM carts WHERE user_id = ?)
                    """)) {
                hepyPs.setInt(1, hepyQuantity);
                hepyPs.setLong(2, hepyUnitPriceCents);
                hepyPs.setLong(3, hepyItemId);
                hepyPs.setLong(4, hepyUserId);
                return hepyPs.executeUpdate() == 1;
            }
        });
    }

    public boolean hepyRemoveItem(long hepyUserId, long hepyItemId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "DELETE FROM cart_items WHERE id = ? AND cart_id IN (SELECT id FROM carts WHERE user_id = ?)")) {
                hepyPs.setLong(1, hepyItemId);
                hepyPs.setLong(2, hepyUserId);
                return hepyPs.executeUpdate() == 1;
            }
        });
    }

    public void hepyClear(Connection hepyC, long hepyUserId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(
                "DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = ?)")) {
            hepyPs.setLong(1, hepyUserId);
            hepyPs.executeUpdate();
        }
    }

    private static long hepyEnsureCart(Connection hepyC, long hepyUserId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("INSERT OR IGNORE INTO carts (user_id) VALUES (?)")) {
            hepyPs.setLong(1, hepyUserId);
            hepyPs.executeUpdate();
        }
        try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT id FROM carts WHERE user_id = ?")) {
            hepyPs.setLong(1, hepyUserId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                hepyRs.next();
                return hepyRs.getLong(1);
            }
        }
    }

    private static void hepyTouch(Connection hepyC, long hepyCartId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(
                "UPDATE carts SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = ?")) {
            hepyPs.setLong(1, hepyCartId);
            hepyPs.executeUpdate();
        }
    }
}
