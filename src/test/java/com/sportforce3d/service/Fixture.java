package com.sportforce3d.service;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.db.TestDatabases;
import com.sportforce3d.repository.HepyAddressRepository;
import com.sportforce3d.repository.HepyCartRepository;
import com.sportforce3d.repository.HepyCatalogRepository;
import com.sportforce3d.repository.HepyDashboardRepository;
import com.sportforce3d.repository.HepyDesignRepository;
import com.sportforce3d.repository.HepyOrderRepository;
import com.sportforce3d.repository.HepyPaymentRepository;
import com.sportforce3d.repository.HepyProductRepository;
import com.sportforce3d.repository.HepyReportRepository;
import com.sportforce3d.repository.HepySettingsRepository;
import com.sportforce3d.repository.HepyUserRepository;
import com.sportforce3d.storage.HepyStorageService;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;

final class Fixture {

    static final String ADMIN = "admin@sportforce3d.pe";
    static final String CLIENT = "cliente@sportforce3d.pe";

    final HepyDatabase db;
    final MutableClock clock = new MutableClock(Instant.parse("2026-10-17T15:00:00Z"));
    final PpqhSession session;
    final HepyUserRepository users;
    final HepyProductRepository products;
    final HepyStorageService storage;
    final PpqhAuthService auth;
    final HepyCatalogService catalog;
    final HepyCartService cart;
    final PpqhAddressService addresses;
    final PpqhOrderService orders;
    final HepyAdminProductService admin;
    final PgeyDesignService designs;
    final PpqhPaymentService payments;
    final PpqhAdminOrderService adminOrders;
    final PpqhDashboardService dashboard;
    final PpqhReportService reports;
    final PpqhStoreSettingsService settings;

    Fixture(Path tempDir) {
        db = TestDatabases.migrated(tempDir);
        users = new HepyUserRepository(db);
        session = new PpqhSession(users::hepyFindById);
        products = new HepyProductRepository(db);
        HepyCartRepository carts = new HepyCartRepository(db);
        HepyAddressRepository addressRepo = new HepyAddressRepository(db);
        storage = new HepyStorageService(tempDir.resolve("storage"));
        auth = new PpqhAuthService(users, new PpqhPasswordHasher(4), session, new PpqhLoginThrottle(clock), clock);
        catalog = new HepyCatalogService(new HepyCatalogRepository(db), products);
        HepyDesignRepository designRepo = new HepyDesignRepository(db);
        HepyOrderRepository orderRepo = new HepyOrderRepository(db);
        HepyPaymentRepository paymentRepo = new HepyPaymentRepository(db);
        cart = new HepyCartService(carts, products, catalog, designRepo, session);
        addresses = new PpqhAddressService(addressRepo, session);
        orders = new PpqhOrderService(db, orderRepo, carts, addressRepo, cart, designRepo, catalog, storage, session, clock);
        admin = new HepyAdminProductService(products, new HepyCatalogRepository(db), storage, session, clock);
        designs = new PgeyDesignService(designRepo, storage, new com.sportforce3d.design.PgeyDraftStore(tempDir.resolve("drafts")),
                session, clock);
        payments = new PpqhPaymentService(db, paymentRepo, orderRepo, storage, session, clock);
        adminOrders = new PpqhAdminOrderService(db, orderRepo, orders, designRepo, catalog, storage, session, clock);
        dashboard = new PpqhDashboardService(new HepyDashboardRepository(db), orderRepo, paymentRepo, session, clock);
        reports = new PpqhReportService(new HepyReportRepository(db), session, clock);
        settings = new PpqhStoreSettingsService(new HepySettingsRepository(db), session, clock);
    }

    void loginAdmin() {
        auth.ppqhLogout();
        auth.ppqhLogin(ADMIN, "Admin123!".toCharArray());
    }

    void loginClient() {
        auth.ppqhLogout();
        auth.ppqhLogin(CLIENT, "Cliente123!".toCharArray());
    }

    void loginNewClient(String email) {
        auth.ppqhLogout();
        auth.ppqhRegister("Otro Cliente", email, null, "Futbol2026".toCharArray(), "Futbol2026".toCharArray());
    }

    long variantId(String sku) {
        return queryLong("SELECT id FROM product_variants WHERE sku = '" + sku + "'");
    }

    long productId(String sku) {
        return queryLong("SELECT id FROM products WHERE sku = '" + sku + "'");
    }

    int quantity(String variantSku) {
        return (int) queryLong("SELECT i.quantity FROM inventory i JOIN product_variants v ON v.id = i.variant_id WHERE v.sku = '" + variantSku + "'");
    }

    int reserved(String variantSku) {
        return (int) queryLong("SELECT i.reserved FROM inventory i JOIN product_variants v ON v.id = i.variant_id WHERE v.sku = '" + variantSku + "'");
    }

    long queryLong(String sql) {
        return db.hepyWithConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Sin resultados: " + sql);
                }
                return rs.getLong(1);
            }
        });
    }

    void exec(String sql) {
        db.hepyWithConnection(c -> c.createStatement().executeUpdate(sql));
    }
}
