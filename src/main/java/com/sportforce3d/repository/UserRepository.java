package com.sportforce3d.repository;

import com.sportforce3d.db.Database;
import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;

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

/** Acceso a users, user_roles y customers. No aplica reglas de negocio ni de permisos: eso es de la capa service. */
public class UserRepository {

    /** Usuario junto con su hash BCrypt, solo para verificar contraseñas. */
    public record StoredUser(User user, String passwordHash) {
    }

    private static final String SELECT_USER = """
            SELECT u.id, u.email, u.full_name, u.phone, u.active, u.created_at, u.last_login_at, u.password_hash,
                   (SELECT group_concat(r.code) FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                     WHERE ur.user_id = u.id) AS role_codes
            FROM users u
            """;

    private final Database db;

    public UserRepository(Database db) {
        this.db = db;
    }

    public Optional<StoredUser> findByEmail(String email) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_USER + " WHERE u.email = ?")) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(new StoredUser(map(rs), rs.getString("password_hash"))) : Optional.empty();
                }
            }
        });
    }

    public Optional<StoredUser> findStoredById(long id) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_USER + " WHERE u.id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(new StoredUser(map(rs), rs.getString("password_hash"))) : Optional.empty();
                }
            }
        });
    }

    public Optional<User> findById(long id) {
        return findStoredById(id).map(StoredUser::user);
    }

    public boolean existsByEmail(String email) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE email = ?")) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public List<User> findAll() {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_USER + " ORDER BY u.full_name COLLATE NOCASE");
                 ResultSet rs = ps.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(map(rs));
                }
                return users;
            }
        });
    }

    /** Inserta el usuario con sus roles y, si es CLIENTE, su ficha en customers. Todo o nada. */
    public User create(String email, String passwordHash, String fullName, String phone, Set<Role> roles) {
        long id = db.inTransaction(c -> {
            long userId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users (email, password_hash, full_name, phone) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, email);
                ps.setString(2, passwordHash);
                ps.setString(3, fullName);
                ps.setString(4, phone);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    userId = keys.getLong(1);
                }
            }
            insertRoles(c, userId, roles);
            if (roles.contains(Role.CLIENTE)) {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO customers (user_id) VALUES (?)")) {
                    ps.setLong(1, userId);
                    ps.executeUpdate();
                }
            }
            return userId;
        });
        return findById(id).orElseThrow();
    }

    public void updateProfile(long id, String fullName, String phone) {
        update("UPDATE users SET full_name = ?, phone = ? WHERE id = ?", ps -> {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setLong(3, id);
        });
    }

    public void updatePassword(long id, String passwordHash) {
        update("UPDATE users SET password_hash = ? WHERE id = ?", ps -> {
            ps.setString(1, passwordHash);
            ps.setLong(2, id);
        });
    }

    public void setActive(long id, boolean active) {
        update("UPDATE users SET active = ? WHERE id = ?", ps -> {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, id);
        });
    }

    public void touchLastLogin(long id, Instant when) {
        update("UPDATE users SET last_login_at = ? WHERE id = ?", ps -> {
            ps.setString(1, when.toString());
            ps.setLong(2, id);
        });
    }

    /** Reemplaza los roles del usuario. Si gana CLIENTE se le crea su ficha de cliente. */
    public void replaceRoles(long id, Set<Role> roles) {
        db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM user_roles WHERE user_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            insertRoles(c, id, roles);
            if (roles.contains(Role.CLIENTE)) {
                try (PreparedStatement ps = c.prepareStatement("INSERT OR IGNORE INTO customers (user_id) VALUES (?)")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    public int countActiveWithRole(Role role) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT count(*) FROM users u
                    JOIN user_roles ur ON ur.user_id = u.id
                    JOIN roles r ON r.id = ur.role_id
                    WHERE r.code = ? AND u.active = 1
                    """)) {
                ps.setString(1, role.name());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    private static void insertRoles(Connection c, long userId, Set<Role> roles) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE code = ?")) {
            for (Role role : roles) {
                ps.setLong(1, userId);
                ps.setString(2, role.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private void update(String sql, Binder binder) {
        db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                binder.bind(ps);
                return ps.executeUpdate();
            }
        });
    }

    private static User map(ResultSet rs) throws SQLException {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        String codes = rs.getString("role_codes");
        if (codes != null) {
            for (String code : codes.split(",")) {
                roles.add(Role.valueOf(code));
            }
        }
        return new User(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getInt("active") == 1,
                roles,
                rs.getString("created_at"),
                rs.getString("last_login_at"));
    }
}
