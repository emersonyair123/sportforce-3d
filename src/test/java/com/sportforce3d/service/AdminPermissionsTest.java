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

/**
 * Ninguna acción de ADMIN se ejecuta con una sesión CLIENTE (ni sin sesión), aunque alguien llame al service
 * directamente sin pasar por la interfaz. Recorre por reflexión todos los métodos públicos de los services de
 * administración: si mañana se agrega uno sin {@code session.requireRole(Role.ADMIN)} al inicio, esta prueba falla.
 */
class AdminPermissionsTest {

    /** Huella de las tablas que no deben cambiar por un intento rechazado. */
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
        adminServices.put(new UserAdminService(f.users, f.session), Set.of());
        adminServices.put(f.admin, Set.of());
        adminServices.put(f.adminOrders, Set.of());
        adminServices.put(f.dashboard, Set.of());
        adminServices.put(f.reports, Set.of("today"));
        adminServices.put(f.settings, Set.of());
        // En PaymentService, registrar el pago e instrucciones son del cliente; el resto es del ADMIN.
        adminServices.put(f.payments, Set.of("register", "instructions"));
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
                        .isInstanceOf(AccessDeniedException.class);
                checked.add(e.getKey().getClass().getSimpleName() + "." + m.getName());
            }
        }

        assertThat(checked).hasSizeGreaterThanOrEqualTo(29)
                .contains("AdminProductService.save", "AdminProductService.updateStock", "PaymentService.approve",
                        "PaymentService.reject", "AdminOrderService.changeStatus", "UserAdminService.setAdmin",
                        "StoreSettingsService.savePaymentSettings", "ReportService.exportPdf", "DashboardService.stats");
        assertThat(counts()).isEqualTo(before);
        assertThat(f.session.current().orElseThrow().isAdmin()).isFalse();
    }

    @Test
    void everyAdminActionAsksToLogInWithoutSession() throws Exception {
        f.auth.logout();
        for (Map.Entry<Object, Set<String>> e : adminServices.entrySet()) {
            for (Method m : adminMethods(e.getKey().getClass(), e.getValue())) {
                assertThat(invoke(e.getKey(), m)).as(m.getName()).isInstanceOf(AuthenticationException.class);
            }
        }
    }

    @Test
    void anAdminWhoLosesTheRoleIsDeniedRightAwayWithoutLoggingOut() {
        f.loginAdmin();
        f.loginNewClient("segundo.admin@correo.pe");
        long secondId = f.session.current().orElseThrow().id();
        f.loginAdmin();
        UserAdminService userAdmin = new UserAdminService(f.users, f.session);
        userAdmin.setAdmin(secondId, true);

        // El segundo ADMIN trabaja en otra ventana con su propia sesión.
        Session other = new Session(f.users::findById);
        new AuthService(f.users, new PasswordHasher(4), other, new LoginThrottle(f.clock), f.clock)
                .login("segundo.admin@correo.pe", "Futbol2026".toCharArray());
        ReportService otherReports = new ReportService(new com.sportforce3d.repository.ReportRepository(f.db), other, f.clock);
        assertThat(otherReports.sales(java.time.LocalDate.of(2026, 10, 1), java.time.LocalDate.of(2026, 10, 17))).isNotNull();

        userAdmin.setAdmin(secondId, false);
        assertThat(invoke(otherReports, method(ReportService.class, "sales"))).isInstanceOf(AccessDeniedException.class);

        userAdmin.setAdmin(secondId, true);
        userAdmin.setActive(secondId, false);
        assertThat(invoke(otherReports, method(ReportService.class, "sales"))).isInstanceOf(AuthenticationException.class);
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

    /** Llama al método con argumentos "vacíos" (null, 0, false) y devuelve la excepción que lanzó. */
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
        f.db.withConnection(c -> {
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