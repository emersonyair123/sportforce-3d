package com.sportforce3d.service;

import com.sportforce3d.db.Database;
import com.sportforce3d.db.TestDatabases;
import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.LoginAttemptRepository;
import com.sportforce3d.repository.UserRepository;
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

    private Database db;
    private UserRepository users;
    private Session session;
    private MutableClock clock;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        db = TestDatabases.migrated(tempDir);
        users = new UserRepository(db);
        session = new Session();
        clock = new MutableClock(Instant.parse("2026-10-17T15:00:00Z"));
        // Coste 4 para que las pruebas sean rápidas; los hashes del seed (coste 12) se verifican igual.
        auth = new AuthService(users, new PasswordHasher(4), session, new LoginThrottle(clock), clock);
    }

    @Test
    void seededAdminAndClientCanLogIn() {
        User admin = auth.login("admin@sportforce3d.pe", "Admin123!".toCharArray());
        assertThat(admin.roles()).containsExactly(Role.ADMIN);
        assertThat(admin.lastLoginAt()).isEqualTo("2026-10-17T15:00:00Z");
        assertThat(session.current()).contains(admin);

        auth.logout();
        assertThat(session.isAuthenticated()).isFalse();

        User client = auth.login("  CLIENTE@sportforce3d.pe ", "Cliente123!".toCharArray());
        assertThat(client.roles()).containsExactly(Role.CLIENTE);
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameMessage() {
        assertThatThrownBy(() -> auth.login("admin@sportforce3d.pe", "otra".toCharArray()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Correo o contraseña incorrectos.");
        assertThatThrownBy(() -> auth.login("nadie@sportforce3d.pe", "Admin123!".toCharArray()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Correo o contraseña incorrectos.");
        assertThat(session.isAuthenticated()).isFalse();
    }

    @Test
    void passwordArrayIsWipedAfterLogin() {
        char[] password = "Admin123!".toCharArray();
        auth.login("admin@sportforce3d.pe", password);
        assertThat(password).containsOnly('\0');
    }

    @Test
    void inactiveAccountCannotLogIn() {
        users.setActive(2, false);
        assertThatThrownBy(() -> auth.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("desactivada");
    }

    @Test
    void locksEmailAfterFiveFailuresForSixtySeconds() {
        for (int i = 0; i < LoginThrottle.MAX_FAILURES; i++) {
            assertThatThrownBy(() -> auth.login("admin@sportforce3d.pe", "mala".toCharArray()))
                    .hasMessage("Correo o contraseña incorrectos.");
        }
        // Bloqueado aunque ahora la contraseña sea correcta.
        assertThatThrownBy(() -> auth.login("admin@sportforce3d.pe", "Admin123!".toCharArray()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Demasiados intentos");

        clock.advance(LoginThrottle.LOCK_TIME.plus(Duration.ofSeconds(1)));
        assertThat(auth.login("admin@sportforce3d.pe", "Admin123!".toCharArray()).isAdmin()).isTrue();
    }

    @Test
    void lockSurvivesRestartingTheAppAndShowsTheWaitingTime() {
        LoginAttemptRepository store = new LoginAttemptRepository(db);
        AuthService first = new AuthService(users, new PasswordHasher(4), session, new LoginThrottle(clock, store), clock);
        for (int i = 0; i < LoginThrottle.MAX_FAILURES; i++) {
            assertThatThrownBy(() -> first.login("Cliente@sportforce3d.pe", "mala".toCharArray()))
                    .hasMessage("Correo o contraseña incorrectos.");
        }

        // "Reinicio": servicios nuevos sobre la misma base de datos.
        AuthService second = new AuthService(users, new PasswordHasher(4), new Session(), new LoginThrottle(clock, store), clock);
        assertThatThrownBy(() -> second.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Demasiados intentos fallidos. Por seguridad, espera 5 minutos e inténtalo de nuevo.");

        clock.advance(LoginThrottle.LOCK_TIME.minusSeconds(30));
        assertThatThrownBy(() -> second.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray()))
                .hasMessageContaining("espera 30 s");

        clock.advance(Duration.ofSeconds(31));
        assertThat(second.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray()).email())
                .isEqualTo("cliente@sportforce3d.pe");
        assertThat(store.find("cliente@sportforce3d.pe")).isEmpty();
    }

    @Test
    void successResetsTheFailureCount() {
        for (int i = 0; i < LoginThrottle.MAX_FAILURES - 1; i++) {
            assertThatThrownBy(() -> auth.login("admin@sportforce3d.pe", "mala".toCharArray()));
        }
        auth.login("admin@sportforce3d.pe", "Admin123!".toCharArray());
        auth.logout();
        for (int i = 0; i < LoginThrottle.MAX_FAILURES - 1; i++) {
            assertThatThrownBy(() -> auth.login("admin@sportforce3d.pe", "mala".toCharArray()))
                    .hasMessage("Correo o contraseña incorrectos.");
        }
        assertThat(auth.login("admin@sportforce3d.pe", "Admin123!".toCharArray()).isAdmin()).isTrue();
    }

    @Test
    void registerCreatesClienteWithCustomerProfileAndStartsSession() throws Exception {
        User user = auth.register("Diego Mamani", "Diego@Correo.pe", "951 234 567",
                "Futbol2026".toCharArray(), "Futbol2026".toCharArray());

        assertThat(user.email()).isEqualTo("diego@correo.pe");
        assertThat(user.roles()).containsExactly(Role.CLIENTE);
        assertThat(session.current()).contains(user);
        int customers = db.withConnection(c -> {
            try (ResultSet rs = c.createStatement().executeQuery(
                    "SELECT count(*) FROM customers WHERE user_id = " + user.id())) {
                rs.next();
                return rs.getInt(1);
            }
        });
        assertThat(customers).isEqualTo(1);

        String hash = users.findByEmail("diego@correo.pe").orElseThrow().passwordHash();
        assertThat(hash).startsWith("$2a$").doesNotContain("Futbol2026");

        auth.logout();
        assertThat(auth.login("diego@correo.pe", "Futbol2026".toCharArray()).id()).isEqualTo(user.id());
    }

    @Test
    void registerValidatesFieldsAndDuplicates() {
        assertThatThrownBy(() -> auth.register("Lu", "no-es-correo", "abc", "corta".toCharArray(), "otra".toCharArray()))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors())
                        .containsOnlyKeys("fullName", "email", "phone", "password", "confirmation"));

        assertThatThrownBy(() -> auth.register("Otra Lucía", "cliente@sportforce3d.pe", null,
                "Cliente999".toCharArray(), "Cliente999".toCharArray()))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors())
                        .containsEntry("email", "Ya existe una cuenta con este correo."));
        assertThat(session.isAuthenticated()).isFalse();
    }
}
