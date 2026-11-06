package com.sportforce3d.service;

import com.sportforce3d.db.TestDatabases;
import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los permisos se comprueban en el service, aunque la vista oculte el menú. */
class UserAdminServiceTest {

    @TempDir
    Path tempDir;

    private UserRepository users;
    private Session session;
    private AuthService auth;
    private UserAdminService admin;

    @BeforeEach
    void setUp() {
        users = new UserRepository(TestDatabases.migrated(tempDir));
        session = new Session();
        auth = new AuthService(users, new PasswordHasher(4), session, new LoginThrottle(Clock.systemUTC()), Clock.systemUTC());
        admin = new UserAdminService(users, session);
    }

    @Test
    void withoutSessionEverythingIsRejected() {
        assertThatThrownBy(admin::listUsers).isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> admin.setActive(2, false)).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void clienteCannotManageUsers() {
        auth.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray());

        assertThatThrownBy(admin::listUsers).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> admin.setActive(1, false)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> admin.setAdmin(2, true)).isInstanceOf(AccessDeniedException.class);
        assertThat(users.findById(2).orElseThrow().isAdmin()).isFalse();
    }

    @Test
    void adminListsAndManagesUsers() {
        auth.login("admin@sportforce3d.pe", "Admin123!".toCharArray());

        assertThat(admin.listUsers()).extracting(User::email)
                .containsExactlyInAnyOrder("admin@sportforce3d.pe", "cliente@sportforce3d.pe");

        assertThat(admin.setActive(2, false).active()).isFalse();
        assertThat(admin.setActive(2, true).active()).isTrue();

        User promoted = admin.setAdmin(2, true);
        assertThat(promoted.roles()).containsExactlyInAnyOrder(Role.ADMIN, Role.CLIENTE);
        assertThat(admin.setAdmin(2, false).roles()).containsExactly(Role.CLIENTE);
    }

    @Test
    void adminCannotLockThemselvesOutOrRemoveLastAdmin() {
        auth.login("admin@sportforce3d.pe", "Admin123!".toCharArray());

        assertThatThrownBy(() -> admin.setActive(1, false))
                .isInstanceOf(ServiceException.class).hasMessageContaining("tu propia cuenta");
        assertThatThrownBy(() -> admin.setAdmin(1, false))
                .isInstanceOf(ServiceException.class).hasMessageContaining("propio rol");

        // Con un segundo admin, el primero sí puede perder el rol si lo quita otro.
        admin.setAdmin(2, true);
        auth.logout();
        auth.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray());
        assertThat(admin.setAdmin(1, false).isAdmin()).isFalse();
        // Ahora el usuario 2 es el último admin activo.
        assertThat(users.countActiveWithRole(Role.ADMIN)).isEqualTo(1);
    }

    @Test
    void deactivatedAdminKeepsNoPrivilegesOnNextLogin() {
        auth.login("admin@sportforce3d.pe", "Admin123!".toCharArray());
        admin.setAdmin(2, true);
        admin.setActive(2, false);
        assertThat(users.countActiveWithRole(Role.ADMIN)).isEqualTo(1);
        auth.logout();

        assertThatThrownBy(() -> auth.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray()))
                .isInstanceOf(AuthenticationException.class);
    }
}
