package com.sportforce3d.repository;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class HepyUserRepository {
    public record HepyStoredUser(PpqhUser hepyUser, String hepyPasswordHash) {
    }

    private static final String HEPY_SELECT_USER = """
            SELECT u.id, u.email, u.full_name, u.phone, u.active, u.created_at, u.last_login_at, u.password_hash,
                   (SELECT group_concat(r.code) FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                     WHERE ur.user_id = u.id) AS role_codes
            FROM users u
            """;

    private final HepyDatabase hepyDb;

    public HepyUserRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    public Optional<HepyStoredUser> hepyFindByEmail(String hepyEmail) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(HEPY_SELECT_USER + " WHERE u.email = ?")) {
                hepyPs.setString(1, hepyEmail);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    return hepyRs.next() ? Optional.of(new HepyStoredUser(hepyMap(hepyRs), hepyRs.getString("password_hash"))) : Optional.empty();
                }
            }
        });
    }

    public Optional<HepyStoredUser> hepyFindStoredById(long hepyId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(HEPY_SELECT_USER + " WHERE u.id = ?")) {
                hepyPs.setLong(1, hepyId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    return hepyRs.next() ? Optional.of(new HepyStoredUser(hepyMap(hepyRs), hepyRs.getString("password_hash"))) : Optional.empty();
                }
            }
        });
    }

    public Optional<PpqhUser> hepyFindById(long hepyId) {
        return hepyFindStoredById(hepyId).map(HepyStoredUser::hepyUser);
    }

    public boolean hepyExistsByEmail(String hepyEmail) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT 1 FROM users WHERE email = ?")) {
                hepyPs.setString(1, hepyEmail);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    return hepyRs.next();
                }
            }
        });
    }

    public List<PpqhUser> hepyFindAll() {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(HEPY_SELECT_USER + " ORDER BY u.full_name COLLATE NOCASE");
                 ResultSet hepyRs = hepyPs.executeQuery()) {
                List<PpqhUser> hepyUsers = new ArrayList<>();
                while (hepyRs.next()) {
                    hepyUsers.add(hepyMap(hepyRs));
                }
                return hepyUsers;
            }
        });
    }

    public PpqhUser hepyCreate(String hepyEmail, String hepyPasswordHash, String hepyFullName, String hepyPhone, Set<PpqhRole> hepyRoles) {
        long hepyId = hepyDb.hepyInTransaction(hepyC -> {
            long hepyUserId;
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "INSERT INTO users (email, password_hash, full_name, phone) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                hepyPs.setString(1, hepyEmail);
                hepyPs.setString(2, hepyPasswordHash);
                hepyPs.setString(3, hepyFullName);
                hepyPs.setString(4, hepyPhone);
                hepyPs.executeUpdate();
                try (ResultSet hepyKeys = hepyPs.getGeneratedKeys()) {
                    hepyKeys.next();
                    hepyUserId = hepyKeys.getLong(1);
                }
            }
            hepyInsertRoles(hepyC, hepyUserId, hepyRoles);
            if (hepyRoles.contains(PpqhRole.CLIENTE)) {
                try (PreparedStatement hepyPs = hepyC.prepareStatement("INSERT INTO customers (user_id) VALUES (?)")) {
                    hepyPs.setLong(1, hepyUserId);
                    hepyPs.executeUpdate();
                }
            }
            return hepyUserId;
        });
        return hepyFindById(hepyId).orElseThrow();
    }

    public void hepyUpdateProfile(long hepyId, String hepyFullName, String hepyPhone) {
        hepyUpdate("UPDATE users SET full_name = ?, phone = ? WHERE id = ?", hepyPs -> {
            hepyPs.setString(1, hepyFullName);
            hepyPs.setString(2, hepyPhone);
            hepyPs.setLong(3, hepyId);
        });
    }

    public void hepyUpdatePassword(long hepyId, String hepyPasswordHash) {
        hepyUpdate("UPDATE users SET password_hash = ? WHERE id = ?", hepyPs -> {
            hepyPs.setString(1, hepyPasswordHash);
            hepyPs.setLong(2, hepyId);
        });
    }

    public void hepySetActive(long hepyId, boolean hepyActive) {
        hepyUpdate("UPDATE users SET active = ? WHERE id = ?", hepyPs -> {
            hepyPs.setInt(1, hepyActive ? 1 : 0);
            hepyPs.setLong(2, hepyId);
        });
    }

    public void hepyTouchLastLogin(long hepyId, Instant hepyWhen) {
        hepyUpdate("UPDATE users SET last_login_at = ? WHERE id = ?", hepyPs -> {
            hepyPs.setString(1, hepyWhen.toString());
            hepyPs.setLong(2, hepyId);
        });
    }

    public void hepyReplaceRoles(long hepyId, Set<PpqhRole> hepyRoles) {
        hepyDb.hepyInTransaction(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("DELETE FROM user_roles WHERE user_id = ?")) {
                hepyPs.setLong(1, hepyId);
                hepyPs.executeUpdate();
            }
            hepyInsertRoles(hepyC, hepyId, hepyRoles);
            if (hepyRoles.contains(PpqhRole.CLIENTE)) {
                try (PreparedStatement hepyPs = hepyC.prepareStatement("INSERT OR IGNORE INTO customers (user_id) VALUES (?)")) {
                    hepyPs.setLong(1, hepyId);
                    hepyPs.executeUpdate();
                }
            }
            return null;
        });
    }

    public int hepyCountActiveWithRole(PpqhRole hepyRole) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT count(*) FROM users u
                    JOIN user_roles ur ON ur.user_id = u.id
                    JOIN roles r ON r.id = ur.role_id
                    WHERE r.code = ? AND u.active = 1
                    """)) {
                hepyPs.setString(1, hepyRole.name());
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    hepyRs.next();
                    return hepyRs.getInt(1);
                }
            }
        });
    }

    private static void hepyInsertRoles(Connection hepyC, long hepyUserId, Set<PpqhRole> hepyRoles) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(
                "INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE code = ?")) {
            for (PpqhRole hepyRole : hepyRoles) {
                hepyPs.setLong(1, hepyUserId);
                hepyPs.setString(2, hepyRole.name());
                hepyPs.addBatch();
            }
            hepyPs.executeBatch();
        }
    }

    private interface HepyBinder {
        void hepyBind(PreparedStatement hepyPs) throws SQLException;
    }

    private void hepyUpdate(String hepySql, HepyBinder hepyBinder) {
        hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(hepySql)) {
                hepyBinder.hepyBind(hepyPs);
                return hepyPs.executeUpdate();
            }
        });
    }

    private static PpqhUser hepyMap(ResultSet hepyRs) throws SQLException {
        Set<PpqhRole> hepyRoles = EnumSet.noneOf(PpqhRole.class);
        String hepyCodes = hepyRs.getString("role_codes");
        if (hepyCodes != null) {
            for (String hepyCode : hepyCodes.split(",")) {
                hepyRoles.add(PpqhRole.valueOf(hepyCode));
            }
        }
        return new PpqhUser(
                hepyRs.getLong("id"),
                hepyRs.getString("email"),
                hepyRs.getString("full_name"),
                hepyRs.getString("phone"),
                hepyRs.getInt("active") == 1,
                hepyRoles,
                hepyRs.getString("created_at"),
                hepyRs.getString("last_login_at"));
    }
}
