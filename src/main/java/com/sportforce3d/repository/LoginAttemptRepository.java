package com.sportforce3d.repository;

import com.sportforce3d.db.Database;
import com.sportforce3d.service.LoginThrottle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Intentos fallidos de inicio de sesión por correo (tabla login_attempts). */
public class LoginAttemptRepository implements LoginThrottle.Store {

    private final Database db;

    public LoginAttemptRepository(Database db) {
        this.db = db;
    }

    @Override
    public Optional<LoginThrottle.Attempts> find(String email) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT failures, locked_until FROM login_attempts WHERE email = ?")) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    String locked = rs.getString(2);
                    return Optional.of(new LoginThrottle.Attempts(rs.getInt(1), locked == null ? null : Instant.parse(locked)));
                }
            }
        });
    }

    @Override
    public void save(String email, LoginThrottle.Attempts attempts) {
        db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO login_attempts (email, failures, locked_until, updated_at) VALUES (?, ?, ?, ?)
                    ON CONFLICT (email) DO UPDATE SET failures = excluded.failures,
                        locked_until = excluded.locked_until, updated_at = excluded.updated_at
                    """)) {
                List<Object> params = new ArrayList<>();
                params.add(email);
                params.add(attempts.failures());
                params.add(attempts.lockedUntil() == null ? null : attempts.lockedUntil().toString());
                params.add(Instant.now().toString());
                Jdbc.bind(ps, params);
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public void delete(String email) {
        db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM login_attempts WHERE email = ?")) {
                ps.setString(1, email);
                return ps.executeUpdate();
            }
        });
    }
}
