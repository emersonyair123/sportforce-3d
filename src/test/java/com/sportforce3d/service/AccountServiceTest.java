package com.sportforce3d.service;

import com.sportforce3d.db.TestDatabases;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.UserRepository;
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

    private Session session;
    private AuthService auth;
    private AccountService account;

    @BeforeEach
    void setUp() {
        UserRepository users = new UserRepository(TestDatabases.migrated(tempDir));
        PasswordHasher hasher = new PasswordHasher(4);
        session = new Session();
        auth = new AuthService(users, hasher, session, new LoginThrottle(Clock.systemUTC()), Clock.systemUTC());
        account = new AccountService(users, hasher, session);
    }

    @Test
    void requiresSession() {
        assertThatThrownBy(() -> account.updateProfile("Alguien", null)).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void updatesProfileAndRefreshesSession() {
        auth.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray());

        User updated = account.updateProfile("  Lucía Quispe Mamani ", "");

        assertThat(updated.fullName()).isEqualTo("Lucía Quispe Mamani");
        assertThat(updated.phone()).isNull();
        assertThat(session.current().orElseThrow().fullName()).isEqualTo("Lucía Quispe Mamani");
    }

    @Test
    void changesPasswordOnlyWithCurrentPassword() {
        auth.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray());

        assertThatThrownBy(() -> account.changePassword("mal".toCharArray(), "Nueva2026".toCharArray(), "Nueva2026".toCharArray()))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.fieldErrors()).containsKey("current"));
        assertThatThrownBy(() -> account.changePassword("Cliente123!".toCharArray(), "nueva".toCharArray(), "nueva".toCharArray()))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.fieldErrors()).containsKey("next"));

        account.changePassword("Cliente123!".toCharArray(), "Nueva2026".toCharArray(), "Nueva2026".toCharArray());
        auth.logout();

        assertThatThrownBy(() -> auth.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray()))
                .isInstanceOf(AuthenticationException.class);
        assertThat(auth.login("cliente@sportforce3d.pe", "Nueva2026".toCharArray()).email())
                .isEqualTo("cliente@sportforce3d.pe");
    }
}
