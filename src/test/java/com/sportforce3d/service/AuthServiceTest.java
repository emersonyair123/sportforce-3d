package com.sportforce3d.service;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.db.TestDatabases;
import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyLoginAttemptRepository;
import com.sportforce3d.repository.HepyUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    @TempDir
    Path tempDir;

    private HepyDatabase db;
    private HepyUserRepository users;
    private PpqhSession session;
    private MutableClock clock;
    private PpqhAuthService auth;

    @BeforeEach
    void setUp() {
        db = TestDatabases.migrated(tempDir);
        users = new HepyUserRepository(db);
        session = new PpqhSession();
        clock = new MutableClock(Instant.parse("2026-10-17T15:00:00Z"));
        auth = new PpqhAuthService(users, new PpqhPasswordHasher(4), session, new PpqhLoginThrottle(clock), clock);
    }

    @Test
    void seededAdminAndClientCanLogIn() {
        PpqhUser admin = auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray());
        assertThat(admin.ppqhRoles()).containsExactly(PpqhRole.ADMIN);
        assertThat(admin.ppqhLastLoginAt()).isEqualTo("2026-10-17T15:00:00Z");
        assertThat(session.ppqhCurrent()).contains(admin);

        auth.ppqhLogout();
        assertThat(session.ppqhIsAuthenticated()).isFalse();

        PpqhUser client = auth.ppqhLogin("  CLIENTE@sportforce3d.pe ", "Cliente123!".toCharArray());
        assertThat(client.ppqhRoles()).containsExactly(PpqhRole.CLIENTE);
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameMessage() {
        assertThatThrownBy(() -> auth.ppqhLogin("admin@sportforce3d.pe", "otra".toCharArray()))
                .isInstanceOf(PpqhAuthenticationException.class)
                .hasMessage("Correo o contraseña incorrectos.");
        assertThatThrownBy(() -> auth.ppqhLogin("nadie@sportforce3d.pe", "Admin123!".toCharArray()))
                .isInstanceOf(PpqhAuthenticationException.class)
                .hasMessage("Correo o contraseña incorrectos.");
        assertThat(session.ppqhIsAuthenticated()).isFalse();
    }

    @Test
    void passwordArrayIsWipedAfterLogin() {
        char[] password = "Admin123!".toCharArray();
        auth.ppqhLogin("admin@sportforce3d.pe", password);
        assertThat(password).containsOnly('\0');
    }

    @Test
    void inactiveAccountCannotLogIn() {
        users.hepySetActive(2, false);
        assertThatThrownBy(() -> auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray()))
                .isInstanceOf(PpqhAuthenticationException.class)
                .hasMessageContaining("desactivada");
    }

    @Test
    void locksEmailAfterFiveFailuresForSixtySeconds() {
        for (int i = 0; i < PpqhLoginThrottle.PPQH_MAX_FAILURES; i++) {
            assertThatThrownBy(() -> auth.ppqhLogin("admin@sportforce3d.pe", "mala".toCharArray()))
                    .hasMessage("Correo o contraseña incorrectos.");
        }
        assertThatThrownBy(() -> auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray()))
                .isInstanceOf(PpqhAuthenticationException.class)
                .hasMessageContaining("Demasiados intentos");

        clock.advance(PpqhLoginThrottle.PPQH_LOCK_TIME.plus(Duration.ofSeconds(1)));
        assertThat(auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray()).ppqhIsAdmin()).isTrue();
    }

    @Test
    void lockSurvivesRestartingTheAppAndShowsTheWaitingTime() {
        HepyLoginAttemptRepository store = new HepyLoginAttemptRepository(db);
        PpqhAuthService first = new PpqhAuthService(users, new PpqhPasswordHasher(4), session, new PpqhLoginThrottle(clock, store), clock);
        for (int i = 0; i < PpqhLoginThrottle.PPQH_MAX_FAILURES; i++) {
            assertThatThrownBy(() -> first.ppqhLogin("Cliente@sportforce3d.pe", "mala".toCharArray()))
                    .hasMessage("Correo o contraseña incorrectos.");
        }

        PpqhAuthService second = new PpqhAuthService(users, new PpqhPasswordHasher(4), new PpqhSession(), new PpqhLoginThrottle(clock, store), clock);
        assertThatThrownBy(() -> second.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray()))
                .isInstanceOf(PpqhAuthenticationException.class)
                .hasMessage("Demasiados intentos fallidos. Por seguridad, espera 5 minutos e inténtalo de nuevo.");

        clock.advance(PpqhLoginThrottle.PPQH_LOCK_TIME.minusSeconds(30));
        assertThatThrownBy(() -> second.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray()))
                .hasMessageContaining("espera 30 s");

        clock.advance(Duration.ofSeconds(31));
        assertThat(second.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray()).ppqhEmail())
                .isEqualTo("cliente@sportforce3d.pe");
        assertThat(store.ppqhFind("cliente@sportforce3d.pe")).isEmpty();
    }

    @Test
    void successResetsTheFailureCount() {
        for (int i = 0; i < PpqhLoginThrottle.PPQH_MAX_FAILURES - 1; i++) {
            assertThatThrownBy(() -> auth.ppqhLogin("admin@sportforce3d.pe", "mala".toCharArray()));
        }
        auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray());
        auth.ppqhLogout();
        for (int i = 0; i < PpqhLoginThrottle.PPQH_MAX_FAILURES - 1; i++) {
            assertThatThrownBy(() -> auth.ppqhLogin("admin@sportforce3d.pe", "mala".toCharArray()))
                    .hasMessage("Correo o contraseña incorrectos.");
        }
        assertThat(auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray()).ppqhIsAdmin()).isTrue();
    }

    @Test
    void registerCreatesClienteWithCustomerProfileAndStartsSession() throws Exception {
        PpqhUser user = auth.ppqhRegister("Diego Mamani", "Diego@Correo.pe", "951 234 567",
                "Futbol2026".toCharArray(), "Futbol2026".toCharArray());

        assertThat(user.ppqhEmail()).isEqualTo("diego@correo.pe");
        assertThat(user.ppqhRoles()).containsExactly(PpqhRole.CLIENTE);
        assertThat(session.ppqhCurrent()).contains(user);
        int customers = db.hepyWithConnection(c -> {
            try (ResultSet rs = c.createStatement().executeQuery(
                    "SELECT count(*) FROM customers WHERE user_id = " + user.ppqhId())) {
                rs.next();
                return rs.getInt(1);
            }
        });
        assertThat(customers).isEqualTo(1);

        String hash = users.hepyFindByEmail("diego@correo.pe").orElseThrow().hepyPasswordHash();
        assertThat(hash).startsWith("$2a$").doesNotContain("Futbol2026");

        auth.ppqhLogout();
        assertThat(auth.ppqhLogin("diego@correo.pe", "Futbol2026".toCharArray()).ppqhId()).isEqualTo(user.ppqhId());
    }

    @Test
    void registerValidatesFieldsAndDuplicates() {
        assertThatThrownBy(() -> auth.ppqhRegister("Lu", "no-es-correo", "abc", "corta".toCharArray(), "otra".toCharArray()))
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors())
                        .containsOnlyKeys("fullName", "email", "phone", "password", "confirmation"));

        assertThatThrownBy(() -> auth.ppqhRegister("Otra Lucía", "cliente@sportforce3d.pe", null,
                "Cliente999".toCharArray(), "Cliente999".toCharArray()))
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors())
                        .containsEntry("email", "Ya existe una cuenta con este correo."));
        assertThat(session.ppqhIsAuthenticated()).isFalse();
    }
}
