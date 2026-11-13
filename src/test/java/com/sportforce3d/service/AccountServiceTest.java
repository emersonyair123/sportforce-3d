package com.sportforce3d.service;

import com.sportforce3d.db.TestDatabases;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountServiceTest {

    @TempDir
    Path tempDir;

    private PpqhSession session;
    private PpqhAuthService auth;
    private PpqhAccountService account;

    @BeforeEach
    void setUp() {
        HepyUserRepository users = new HepyUserRepository(TestDatabases.migrated(tempDir));
        PpqhPasswordHasher hasher = new PpqhPasswordHasher(4);
        session = new PpqhSession();
        auth = new PpqhAuthService(users, hasher, session, new PpqhLoginThrottle(Clock.systemUTC()), Clock.systemUTC());
        account = new PpqhAccountService(users, hasher, session);
    }

    @Test
    void requiresSession() {
        assertThatThrownBy(() -> account.ppqhUpdateProfile("Alguien", null)).isInstanceOf(PpqhAuthenticationException.class);
    }

    @Test
    void updatesProfileAndRefreshesSession() {
        auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray());

        PpqhUser updated = account.ppqhUpdateProfile("  Lucía Quispe Mamani ", "");

        assertThat(updated.ppqhFullName()).isEqualTo("Lucía Quispe Mamani");
        assertThat(updated.ppqhPhone()).isNull();
        assertThat(session.ppqhCurrent().orElseThrow().ppqhFullName()).isEqualTo("Lucía Quispe Mamani");
    }

    @Test
    void changesPasswordOnlyWithCurrentPassword() {
        auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray());

        assertThatThrownBy(() -> account.ppqhChangePassword("mal".toCharArray(), "Nueva2026".toCharArray(), "Nueva2026".toCharArray()))
                .isInstanceOfSatisfying(PpqhValidationException.class,
                        e -> assertThat(e.ppqhFieldErrors()).containsKey("current"));
        assertThatThrownBy(() -> account.ppqhChangePassword("Cliente123!".toCharArray(), "nueva".toCharArray(), "nueva".toCharArray()))
                .isInstanceOfSatisfying(PpqhValidationException.class,
                        e -> assertThat(e.ppqhFieldErrors()).containsKey("next"));

        account.ppqhChangePassword("Cliente123!".toCharArray(), "Nueva2026".toCharArray(), "Nueva2026".toCharArray());
        auth.ppqhLogout();

        assertThatThrownBy(() -> auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray()))
                .isInstanceOf(PpqhAuthenticationException.class);
        assertThat(auth.ppqhLogin("cliente@sportforce3d.pe", "Nueva2026".toCharArray()).ppqhEmail())
                .isEqualTo("cliente@sportforce3d.pe");
    }
}
