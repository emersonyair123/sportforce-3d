package com.sportforce3d.repository;

import com.sportforce3d.db.Database;
import com.sportforce3d.model.Address;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Direcciones de envío. Todas las consultas filtran por el usuario dueño. */
public class AddressRepository {

    private static final String SELECT = """
            SELECT a.* FROM addresses a JOIN customers cu ON cu.id = a.customer_id
            WHERE cu.user_id = ?
            """;

    private final Database db;

    public AddressRepository(Database db) {
        this.db = db;
    }

    public List<Address> findByUser(long userId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY a.is_default DESC, a.id")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Address> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                    return list;
                }
            }
        });
    }

    public Optional<Address> findById(long userId, long addressId) {
        return db.withConnection(c -> findById(c, userId, addressId));
    }

    public Optional<Address> findById(Connection c, long userId, long addressId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " AND a.id = ?")) {
            ps.setLong(1, userId);
            ps.setLong(2, addressId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Inserta o actualiza. Si es predeterminada (o la primera), las demás dejan de serlo. Devuelve el id. */
    public long save(long userId, Address a) {
        return db.inTransaction(c -> {
            long customerId = ensureCustomer(c, userId);
            boolean first = countFor(c, customerId) == 0;
            boolean makeDefault = a.isDefault() || first;
            if (makeDefault) {
                clearDefault(c, customerId);
            }
            if (a.id() == 0) {
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO addresses (customer_id, label, recipient, phone, department, province, district,
                                               line1, reference, is_default)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, customerId, a, makeDefault);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        return keys.getLong(1);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE addresses SET customer_id = ?, label = ?, recipient = ?, phone = ?, department = ?,
                           province = ?, district = ?, line1 = ?, reference = ?, is_default = ?
                    WHERE id = ? AND customer_id = ?
                    """)) {
                bind(ps, customerId, a, makeDefault);
                ps.setLong(11, a.id());
                ps.setLong(12, customerId);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Dirección " + a.id() + " no encontrada para el usuario");
                }
            }
            ensureOneDefault(c, customerId);
            return a.id();
        });
    }

    public boolean delete(long userId, long addressId) {
        return db.inTransaction(c -> {
            long customerId = ensureCustomer(c, userId);
            int deleted;
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM addresses WHERE id = ? AND customer_id = ?")) {
                ps.setLong(1, addressId);
                ps.setLong(2, customerId);
                deleted = ps.executeUpdate();
            }
            ensureOneDefault(c, customerId);
            return deleted == 1;
        });
    }

    /** Crea la ficha de cliente si el usuario (p. ej. un ADMIN) aún no tiene una. */
    static long ensureCustomer(Connection c, long userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT OR IGNORE INTO customers (user_id) VALUES (?)")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM customers WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static int countFor(Connection c, long customerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM addresses WHERE customer_id = ?")) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void clearDefault(Connection c, long customerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE addresses SET is_default = 0 WHERE customer_id = ? AND is_default = 1")) {
            ps.setLong(1, customerId);
            ps.executeUpdate();
        }
    }

    /** Si no queda ninguna predeterminada, marca la más antigua. */
    private static void ensureOneDefault(Connection c, long customerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE addresses SET is_default = 1
                WHERE id = (SELECT min(id) FROM addresses WHERE customer_id = ?)
                  AND NOT EXISTS (SELECT 1 FROM addresses WHERE customer_id = ? AND is_default = 1)
                """)) {
            ps.setLong(1, customerId);
            ps.setLong(2, customerId);
            ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, long customerId, Address a, boolean isDefault) throws SQLException {
        ps.setLong(1, customerId);
        ps.setString(2, a.label());
        ps.setString(3, a.recipient());
        ps.setString(4, a.phone());
        ps.setString(5, a.department());
        ps.setString(6, a.province());
        ps.setString(7, a.district());
        ps.setString(8, a.line1());
        ps.setString(9, a.reference());
        ps.setInt(10, isDefault ? 1 : 0);
    }

    private static Address map(ResultSet rs) throws SQLException {
        return new Address(rs.getLong("id"), rs.getString("label"), rs.getString("recipient"), rs.getString("phone"),
                rs.getString("department"), rs.getString("province"), rs.getString("district"),
                rs.getString("line1"), rs.getString("reference"), rs.getInt("is_default") == 1);
    }
}
