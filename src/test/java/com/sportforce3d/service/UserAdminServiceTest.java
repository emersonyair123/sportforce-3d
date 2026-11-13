package com.sportforce3d.service;

import com.sportforce3d.db.TestDatabases;
import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAdminServiceTest {

    @TempDir
    Path tempDir;

    private HepyUserRepository users;
    private PpqhSession session;
    private PpqhAuthService auth;
    private PpqhUserAdminService admin;

    @BeforeEach
    void setUp() {
        users = new HepyUserRepository(TestDatabases.migrated(tempDir));
        session = new PpqhSession();
        auth = new PpqhAuthService(users, new PpqhPasswordHasher(4), session, new PpqhLoginThrottle(Clock.systemUTC()), Clock.systemUTC());
        admin = new PpqhUserAdminService(users, session);
    }

    @Test
    void withoutSessionEverythingIsRejected() {
        assertThatThrownBy(admin::ppqhListUsers).isInstanceOf(PpqhAuthenticationException.class);
        assertThatThrownBy(() -> admin.ppqhSetActive(2, false)).isInstanceOf(PpqhAuthenticationException.class);
    }

    @Test
    void clienteCannotManageUsers() {
        auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray());

        assertThatThrownBy(admin::ppqhListUsers).isInstanceOf(PpqhAccessDeniedException.class);
        assertThatThrownBy(() -> admin.ppqhSetActive(1, false)).isInstanceOf(PpqhAccessDeniedException.class);
        assertThatThrownBy(() -> admin.ppqhSetAdmin(2, true)).isInstanceOf(PpqhAccessDeniedException.class);
        assertThat(users.hepyFindById(2).orElseThrow().ppqhIsAdmin()).isFalse();
    }

    @Test
    void adminListsAndManagesUsers() {
        auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray());

        assertThat(admin.ppqhListUsers()).extracting(PpqhUser::ppqhEmail)
                .containsExactlyInAnyOrder("admin@sportforce3d.pe", "cliente@sportforce3d.pe");

        assertThat(admin.ppqhSetActive(2, false).ppqhActive()).isFalse();
        assertThat(admin.ppqhSetActive(2, true).ppqhActive()).isTrue();

        PpqhUser promoted = admin.ppqhSetAdmin(2, true);
        assertThat(promoted.ppqhRoles()).containsExactlyInAnyOrder(PpqhRole.ADMIN, PpqhRole.CLIENTE);
        assertThat(admin.ppqhSetAdmin(2, false).ppqhRoles()).containsExactly(PpqhRole.CLIENTE);
    }

    @Test
    void adminCannotLockThemselvesOutOrRemoveLastAdmin() {
        auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray());

        assertThatThrownBy(() -> admin.ppqhSetActive(1, false))
                .isInstanceOf(PpqhServiceException.class).hasMessageContaining("tu propia cuenta");
        assertThatThrownBy(() -> admin.ppqhSetAdmin(1, false))
                .isInstanceOf(PpqhServiceException.class).hasMessageContaining("propio rol");

        admin.ppqhSetAdmin(2, true);
        auth.ppqhLogout();
        auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray());
        assertThat(admin.ppqhSetAdmin(1, false).ppqhIsAdmin()).isFalse();
        assertThat(users.hepyCountActiveWithRole(PpqhRole.ADMIN)).isEqualTo(1);
    }

    @Test
    void deactivatedAdminKeepsNoPrivilegesOnNextLogin() {
        auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray());
        admin.ppqhSetAdmin(2, true);
        admin.ppqhSetActive(2, false);
        assertThat(users.hepyCountActiveWithRole(PpqhRole.ADMIN)).isEqualTo(1);
        auth.ppqhLogout();

        assertThatThrownBy(() -> auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray()))
                .isInstanceOf(PpqhAuthenticationException.class);
    }
}
