package com.sportforce3d.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPermissionsTest {
    private static final List<String> SNAPSHOT = List.of(
            "SELECT count(*) || ':' || group_concat(email || active) FROM users",
            "SELECT count(*) || ':' || group_concat(user_id || '-' || role_id) FROM user_roles",
            "SELECT count(*) || ':' || group_concat(name || base_price_cents || active || COALESCE(deleted_at, '')) FROM products",
            "SELECT count(*) || ':' || group_concat(quantity || '/' || reserved) FROM inventory",
            "SELECT count(*) || ':' || COALESCE(group_concat(status), '') FROM orders",
            "SELECT count(*) || ':' || COALESCE(group_concat(status), '') FROM payments",
            "SELECT count(*) || ':' || group_concat(key || '=' || value) FROM store_settings",
            "SELECT count(*) FROM product_images");

    @TempDir
    Path tempDir;

    private Fixture f;
    private Map<Object, Set<String>> adminServices;

    @BeforeEach
    void setUp() {
        f = new Fixture(tempDir);
        adminServices = new LinkedHashMap<>();
        adminServices.put(new PpqhUserAdminService(f.users, f.session), Set.of());
        adminServices.put(f.admin, Set.of());
        adminServices.put(f.adminOrders, Set.of());
        adminServices.put(f.dashboard, Set.of());
        adminServices.put(f.reports, Set.of("ppqhToday"));
        adminServices.put(f.settings, Set.of());
        adminServices.put(f.payments, Set.of("ppqhRegister", "ppqhInstructions"));
    }

    @Test
    void everyAdminActionIsDeniedToAClient() throws Exception {
        f.loginClient();
        Map<String, String> before = counts();

        List<String> checked = new ArrayList<>();
        for (Map.Entry<Object, Set<String>> e : adminServices.entrySet()) {
            for (Method m : adminMethods(e.getKey().getClass(), e.getValue())) {
                Throwable error = invoke(e.getKey(), m);
                assertThat(error).as(e.getKey().getClass().getSimpleName() + "." + m.getName())
                        .isInstanceOf(PpqhAccessDeniedException.class);
                checked.add(e.getKey().getClass().getSimpleName() + "." + m.getName());
            }
        }

        assertThat(checked).hasSizeGreaterThanOrEqualTo(29)
                .contains("HepyAdminProductService.hepySave", "HepyAdminProductService.hepyUpdateStock", "PpqhPaymentService.ppqhApprove",
                        "PpqhPaymentService.ppqhReject", "PpqhAdminOrderService.ppqhChangeStatus", "PpqhUserAdminService.ppqhSetAdmin",
                        "PpqhStoreSettingsService.ppqhSavePaymentSettings", "PpqhReportService.ppqhExportPdf", "PpqhDashboardService.ppqhStats");
        assertThat(counts()).isEqualTo(before);
        assertThat(f.session.ppqhCurrent().orElseThrow().ppqhIsAdmin()).isFalse();
    }

    @Test
    void everyAdminActionAsksToLogInWithoutSession() throws Exception {
        f.auth.ppqhLogout();
        for (Map.Entry<Object, Set<String>> e : adminServices.entrySet()) {
            for (Method m : adminMethods(e.getKey().getClass(), e.getValue())) {
                assertThat(invoke(e.getKey(), m)).as(m.getName()).isInstanceOf(PpqhAuthenticationException.class);
            }
        }
    }

    @Test
    void anAdminWhoLosesTheRoleIsDeniedRightAwayWithoutLoggingOut() {
        f.loginAdmin();
        f.loginNewClient("segundo.admin@correo.pe");
        long secondId = f.session.ppqhCurrent().orElseThrow().ppqhId();
        f.loginAdmin();
        PpqhUserAdminService userAdmin = new PpqhUserAdminService(f.users, f.session);
        userAdmin.ppqhSetAdmin(secondId, true);

        PpqhSession other = new PpqhSession(f.users::hepyFindById);
        new PpqhAuthService(f.users, new PpqhPasswordHasher(4), other, new PpqhLoginThrottle(f.clock), f.clock)
                .ppqhLogin("segundo.admin@correo.pe", "Futbol2026".toCharArray());
        PpqhReportService otherReports = new PpqhReportService(new com.sportforce3d.repository.HepyReportRepository(f.db), other, f.clock);
        assertThat(otherReports.ppqhSales(java.time.LocalDate.of(2026, 10, 1), java.time.LocalDate.of(2026, 10, 17))).isNotNull();

        userAdmin.ppqhSetAdmin(secondId, false);
        assertThat(invoke(otherReports, method(PpqhReportService.class, "ppqhSales"))).isInstanceOf(PpqhAccessDeniedException.class);

        userAdmin.ppqhSetAdmin(secondId, true);
        userAdmin.ppqhSetActive(secondId, false);
        assertThat(invoke(otherReports, method(PpqhReportService.class, "ppqhSales"))).isInstanceOf(PpqhAuthenticationException.class);
    }

    private static List<Method> adminMethods(Class<?> type, Set<String> excluded) {
        List<Method> list = new ArrayList<>();
        for (Method m : type.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && !m.isSynthetic()
                    && !excluded.contains(m.getName())) {
                list.add(m);
            }
        }
        assertThat(list).as(type.getSimpleName()).isNotEmpty();
        return list;
    }

    private static Method method(Class<?> type, String name) {
        for (Method m : type.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new IllegalArgumentException(name);
    }

    private static Throwable invoke(Object target, Method m) {
        Object[] args = new Object[m.getParameterCount()];
        Class<?>[] types = m.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            Class<?> t = types[i];
            if (t == boolean.class) {
                args[i] = false;
            } else if (t == int.class) {
                args[i] = 1;
            } else if (t == long.class) {
                args[i] = 1L;
            } else if (t == double.class) {
                args[i] = 1d;
            } else {
                args[i] = null;
            }
        }
        try {
            m.invoke(target, args);
            return null;
        } catch (InvocationTargetException e) {
            return e.getCause();
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private Map<String, String> counts() {
        Map<String, String> map = new LinkedHashMap<>();
        f.db.hepyWithConnection(c -> {
            for (String sql : SNAPSHOT) {
                try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    map.put(sql, rs.getString(1));
                }
            }
            return null;
        });
        return map;
    }
}
