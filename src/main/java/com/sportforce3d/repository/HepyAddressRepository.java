package com.sportforce3d.repository;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.model.PpqhAddress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HepyAddressRepository {

    private static final String HEPY_SELECT = """
            SELECT a.* FROM addresses a JOIN customers cu ON cu.id = a.customer_id
            WHERE cu.user_id = ?
            """;

    private final HepyDatabase hepyDb;

    public HepyAddressRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    public List<PpqhAddress> hepyFindByUser(long hepyUserId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(HEPY_SELECT + " ORDER BY a.is_default DESC, a.id")) {
                hepyPs.setLong(1, hepyUserId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    List<PpqhAddress> hepyList = new ArrayList<>();
                    while (hepyRs.next()) {
                        hepyList.add(hepyMap(hepyRs));
                    }
                    return hepyList;
                }
            }
        });
    }

    public Optional<PpqhAddress> hepyFindById(long hepyUserId, long hepyAddressId) {
        return hepyDb.hepyWithConnection(hepyC -> hepyFindById(hepyC, hepyUserId, hepyAddressId));
    }

    public Optional<PpqhAddress> hepyFindById(Connection hepyC, long hepyUserId, long hepyAddressId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(HEPY_SELECT + " AND a.id = ?")) {
            hepyPs.setLong(1, hepyUserId);
            hepyPs.setLong(2, hepyAddressId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                return hepyRs.next() ? Optional.of(hepyMap(hepyRs)) : Optional.empty();
            }
        }
    }

    public long hepySave(long hepyUserId, PpqhAddress hepyA) {
        return hepyDb.hepyInTransaction(hepyC -> {
            long hepyCustomerId = hepyEnsureCustomer(hepyC, hepyUserId);
            boolean hepyFirst = hepyCountFor(hepyC, hepyCustomerId) == 0;
            boolean hepyMakeDefault = hepyA.ppqhIsDefault() || hepyFirst;
            if (hepyMakeDefault) {
                hepyClearDefault(hepyC, hepyCustomerId);
            }
            if (hepyA.ppqhId() == 0) {
                try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                        INSERT INTO addresses (customer_id, label, recipient, phone, department, province, district,
                                               line1, reference, is_default)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    hepyBind(hepyPs, hepyCustomerId, hepyA, hepyMakeDefault);
                    hepyPs.executeUpdate();
                    try (ResultSet hepyKeys = hepyPs.getGeneratedKeys()) {
                        hepyKeys.next();
                        return hepyKeys.getLong(1);
                    }
                }
            }
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    UPDATE addresses SET customer_id = ?, label = ?, recipient = ?, phone = ?, department = ?,
                           province = ?, district = ?, line1 = ?, reference = ?, is_default = ?
                    WHERE id = ? AND customer_id = ?
                    """)) {
                hepyBind(hepyPs, hepyCustomerId, hepyA, hepyMakeDefault);
                hepyPs.setLong(11, hepyA.ppqhId());
                hepyPs.setLong(12, hepyCustomerId);
                if (hepyPs.executeUpdate() != 1) {
                    throw new SQLException("Dirección " + hepyA.ppqhId() + " no encontrada para el usuario");
                }
            }
            hepyEnsureOneDefault(hepyC, hepyCustomerId);
            return hepyA.ppqhId();
        });
    }

    public boolean hepyDelete(long hepyUserId, long hepyAddressId) {
        return hepyDb.hepyInTransaction(hepyC -> {
            long hepyCustomerId = hepyEnsureCustomer(hepyC, hepyUserId);
            int hepyDeleted;
            try (PreparedStatement hepyPs = hepyC.prepareStatement("DELETE FROM addresses WHERE id = ? AND customer_id = ?")) {
                hepyPs.setLong(1, hepyAddressId);
                hepyPs.setLong(2, hepyCustomerId);
                hepyDeleted = hepyPs.executeUpdate();
            }
            hepyEnsureOneDefault(hepyC, hepyCustomerId);
            return hepyDeleted == 1;
        });
    }

    static long hepyEnsureCustomer(Connection hepyC, long hepyUserId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("INSERT OR IGNORE INTO customers (user_id) VALUES (?)")) {
            hepyPs.setLong(1, hepyUserId);
            hepyPs.executeUpdate();
        }
        try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT id FROM customers WHERE user_id = ?")) {
            hepyPs.setLong(1, hepyUserId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                hepyRs.next();
                return hepyRs.getLong(1);
            }
        }
    }

    private static int hepyCountFor(Connection hepyC, long hepyCustomerId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT count(*) FROM addresses WHERE customer_id = ?")) {
            hepyPs.setLong(1, hepyCustomerId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                hepyRs.next();
                return hepyRs.getInt(1);
            }
        }
    }

    private static void hepyClearDefault(Connection hepyC, long hepyCustomerId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(
                "UPDATE addresses SET is_default = 0 WHERE customer_id = ? AND is_default = 1")) {
            hepyPs.setLong(1, hepyCustomerId);
            hepyPs.executeUpdate();
        }
    }

    private static void hepyEnsureOneDefault(Connection hepyC, long hepyCustomerId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                UPDATE addresses SET is_default = 1
                WHERE id = (SELECT min(id) FROM addresses WHERE customer_id = ?)
                  AND NOT EXISTS (SELECT 1 FROM addresses WHERE customer_id = ? AND is_default = 1)
                """)) {
            hepyPs.setLong(1, hepyCustomerId);
            hepyPs.setLong(2, hepyCustomerId);
            hepyPs.executeUpdate();
        }
    }

    private static void hepyBind(PreparedStatement hepyPs, long hepyCustomerId, PpqhAddress hepyA, boolean hepyIsDefault) throws SQLException {
        hepyPs.setLong(1, hepyCustomerId);
        hepyPs.setString(2, hepyA.ppqhLabel());
        hepyPs.setString(3, hepyA.ppqhRecipient());
        hepyPs.setString(4, hepyA.ppqhPhone());
        hepyPs.setString(5, hepyA.ppqhDepartment());
        hepyPs.setString(6, hepyA.ppqhProvince());
        hepyPs.setString(7, hepyA.ppqhDistrict());
        hepyPs.setString(8, hepyA.ppqhLine1());
        hepyPs.setString(9, hepyA.ppqhReference());
        hepyPs.setInt(10, hepyIsDefault ? 1 : 0);
    }

    private static PpqhAddress hepyMap(ResultSet hepyRs) throws SQLException {
        return new PpqhAddress(hepyRs.getLong("id"), hepyRs.getString("label"), hepyRs.getString("recipient"), hepyRs.getString("phone"),
                hepyRs.getString("department"), hepyRs.getString("province"), hepyRs.getString("district"),
                hepyRs.getString("line1"), hepyRs.getString("reference"), hepyRs.getInt("is_default") == 1);
    }
}
