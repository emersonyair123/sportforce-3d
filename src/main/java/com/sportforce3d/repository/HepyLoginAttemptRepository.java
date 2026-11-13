package com.sportforce3d.repository;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.service.PpqhLoginThrottle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HepyLoginAttemptRepository implements PpqhLoginThrottle.PpqhStore {

    private final HepyDatabase hepyDb;

    public HepyLoginAttemptRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    @Override
    public Optional<PpqhLoginThrottle.PpqhAttempts> ppqhFind(String hepyEmail) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "SELECT failures, locked_until FROM login_attempts WHERE email = ?")) {
                hepyPs.setString(1, hepyEmail);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    if (!hepyRs.next()) {
                        return Optional.empty();
                    }
                    String hepyLocked = hepyRs.getString(2);
                    return Optional.of(new PpqhLoginThrottle.PpqhAttempts(hepyRs.getInt(1), hepyLocked == null ? null : Instant.parse(hepyLocked)));
                }
            }
        });
    }

    @Override
    public void ppqhSave(String hepyEmail, PpqhLoginThrottle.PpqhAttempts hepyAttempts) {
        hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    INSERT INTO login_attempts (email, failures, locked_until, updated_at) VALUES (?, ?, ?, ?)
                    ON CONFLICT (email) DO UPDATE SET failures = excluded.failures,
                        locked_until = excluded.locked_until, updated_at = excluded.updated_at
                    """)) {
                List<Object> hepyParams = new ArrayList<>();
                hepyParams.add(hepyEmail);
                hepyParams.add(hepyAttempts.ppqhFailures());
                hepyParams.add(hepyAttempts.ppqhLockedUntil() == null ? null : hepyAttempts.ppqhLockedUntil().toString());
                hepyParams.add(Instant.now().toString());
                HepyJdbc.hepyBind(hepyPs, hepyParams);
                return hepyPs.executeUpdate();
            }
        });
    }

    @Override
    public void ppqhDelete(String hepyEmail) {
        hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("DELETE FROM login_attempts WHERE email = ?")) {
                hepyPs.setString(1, hepyEmail);
                return hepyPs.executeUpdate();
            }
        });
    }
}
