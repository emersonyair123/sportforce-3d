package com.sportforce3d.repository;

import com.sportforce3d.db.Database;
import com.sportforce3d.model.CartLine;
import com.sportforce3d.model.Garment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Carrito persistido por usuario. Todas las consultas filtran por user_id: un usuario nunca puede leer
 * ni modificar líneas del carrito de otro.
 */
public class CartRepository {

    /** Línea existente de la misma variante sin diseño (se suma la cantidad en lugar de duplicarla). */
    public record ExistingItem(long itemId, int quantity) {
    }

    private static final String LINES_SQL = """
            SELECT ci.id AS item_id, p.id AS product_id, v.id AS variant_id, p.name AS product_name, v.sku,
                   p.garment_type, s.name AS size_name, v.color_name, v.color_hex,
                   (SELECT pi.path FROM product_images pi WHERE pi.product_id = p.id
                     ORDER BY pi.sort_order, pi.id LIMIT 1) AS image_path,
                   p.base_price_cents + v.price_delta_cents AS unit_price_cents,
                   ci.surcharge_cents, ci.quantity,
                   max(COALESCE(i.quantity, 0) - COALESCE(i.reserved, 0), 0) AS available,
                   (p.active = 1 AND p.deleted_at IS NULL AND v.active = 1) AS purchasable
            FROM carts c
            JOIN cart_items ci ON ci.cart_id = c.id
            JOIN product_variants v ON v.id = ci.variant_id
            JOIN products p ON p.id = v.product_id
            JOIN sizes s ON s.id = v.size_id
            LEFT JOIN inventory i ON i.variant_id = v.id
            WHERE c.user_id = ?
            ORDER BY ci.created_at, ci.id
            """;

    private final Database db;

    public CartRepository(Database db) {
        this.db = db;
    }

    public List<CartLine> findLines(long userId) {
        return db.withConnection(c -> findLines(c, userId));
    }

    public List<CartLine> findLines(Connection c, long userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(LINES_SQL)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CartLine> lines = new ArrayList<>();
                while (rs.next()) {
                    lines.add(new CartLine(rs.getLong("item_id"), rs.getLong("product_id"), rs.getLong("variant_id"),
                            rs.getString("product_name"), rs.getString("sku"),
                            Garment.valueOf(rs.getString("garment_type")), rs.getString("size_name"),
                            rs.getString("color_name"), rs.getString("color_hex"), rs.getString("image_path"),
                            rs.getLong("unit_price_cents"), rs.getLong("surcharge_cents"), rs.getInt("quantity"),
                            rs.getInt("available"), rs.getInt("purchasable") == 1));
                }
                return lines;
            }
        }
    }

    public int countUnits(long userId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT COALESCE(sum(ci.quantity), 0) FROM carts c JOIN cart_items ci ON ci.cart_id = c.id
                    WHERE c.user_id = ?
                    """)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    public Optional<ExistingItem> findStandardItem(long userId, long variantId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT ci.id, ci.quantity FROM carts c JOIN cart_items ci ON ci.cart_id = c.id
                    WHERE c.user_id = ? AND ci.variant_id = ? AND ci.design_id IS NULL
                    """)) {
                ps.setLong(1, userId);
                ps.setLong(2, variantId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(new ExistingItem(rs.getLong(1), rs.getInt(2))) : Optional.empty();
                }
            }
        });
    }

    public void addItem(long userId, long variantId, int quantity, long unitPriceCents) {
        db.inTransaction(c -> {
            long cartId = ensureCart(c, userId);
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO cart_items (cart_id, variant_id, quantity, unit_price_cents, surcharge_cents)
                    VALUES (?, ?, ?, ?, 0)
                    """)) {
                ps.setLong(1, cartId);
                ps.setLong(2, variantId);
                ps.setInt(3, quantity);
                ps.setLong(4, unitPriceCents);
                ps.executeUpdate();
            }
            touch(c, cartId);
            return null;
        });
    }

    /** Cambia la cantidad solo si la línea es del usuario. Devuelve false si no existe. */
    public boolean updateQuantity(long userId, long itemId, int quantity, long unitPriceCents) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE cart_items SET quantity = ?, unit_price_cents = ?
                    WHERE id = ? AND cart_id IN (SELECT id FROM carts WHERE user_id = ?)
                    """)) {
                ps.setInt(1, quantity);
                ps.setLong(2, unitPriceCents);
                ps.setLong(3, itemId);
                ps.setLong(4, userId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    public boolean removeItem(long userId, long itemId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM cart_items WHERE id = ? AND cart_id IN (SELECT id FROM carts WHERE user_id = ?)")) {
                ps.setLong(1, itemId);
                ps.setLong(2, userId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    public void clear(Connection c, long userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = ?)")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    private static long ensureCart(Connection c, long userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT OR IGNORE INTO carts (user_id) VALUES (?)")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM carts WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static void touch(Connection c, long cartId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE carts SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = ?")) {
            ps.setLong(1, cartId);
            ps.executeUpdate();
        }
    }
}
