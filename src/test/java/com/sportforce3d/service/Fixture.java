package com.sportforce3d.service;

import com.sportforce3d.db.Database;
import com.sportforce3d.db.TestDatabases;
import com.sportforce3d.repository.AddressRepository;
import com.sportforce3d.repository.CartRepository;
import com.sportforce3d.repository.CatalogRepository;
import com.sportforce3d.repository.DashboardRepository;
import com.sportforce3d.repository.DesignRepository;
import com.sportforce3d.repository.OrderRepository;
import com.sportforce3d.repository.PaymentRepository;
import com.sportforce3d.repository.ProductRepository;
import com.sportforce3d.repository.ReportRepository;
import com.sportforce3d.repository.SettingsRepository;
import com.sportforce3d.repository.UserRepository;
import com.sportforce3d.storage.StorageService;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;

final class Fixture {

    static final String ADMIN = "admin@sportforce3d.pe";
    static final String CLIENT = "cliente@sportforce3d.pe";

    final Database db;
    final MutableClock clock = new MutableClock(Instant.parse("2026-10-17T15:00:00Z"));
    final Session session;
    final UserRepository users;
    final ProductRepository products;
    final StorageService storage;
    final AuthService auth;
    final CatalogService catalog;
    final CartService cart;
    final AddressService addresses;
    final OrderService orders;
    final AdminProductService admin;
    final DesignService designs;
    final PaymentService payments;
    final AdminOrderService adminOrders;
    final DashboardService dashboard;
    final ReportService reports;
    final StoreSettingsService settings;

    Fixture(Path tempDir) {
        db = TestDatabases.migrated(tempDir);
        users = new UserRepository(db);
        session = new Session(users::findById);
        products = new ProductRepository(db);
        CartRepository carts = new CartRepository(db);
        AddressRepository addressRepo = new AddressRepository(db);
        storage = new StorageService(tempDir.resolve("storage"));
        auth = new AuthService(users, new PasswordHasher(4), session, new LoginThrottle(clock), clock);
        catalog = new CatalogService(new CatalogRepository(db), products);
        DesignRepository designRepo = new DesignRepository(db);
        OrderRepository orderRepo = new OrderRepository(db);
        PaymentRepository paymentRepo = new PaymentRepository(db);
        cart = new CartService(carts, products, catalog, designRepo, session);
        addresses = new AddressService(addressRepo, session);
        orders = new OrderService(db, orderRepo, carts, addressRepo, cart, designRepo, catalog, storage, session, clock);
        admin = new AdminProductService(products, new CatalogRepository(db), storage, session, clock);
        designs = new DesignService(designRepo, storage, new com.sportforce3d.design.DraftStore(tempDir.resolve("drafts")),
                session, clock);
        payments = new PaymentService(db, paymentRepo, orderRepo, storage, session, clock);
        adminOrders = new AdminOrderService(db, orderRepo, orders, designRepo, catalog, storage, session, clock);
        dashboard = new DashboardService(new DashboardRepository(db), orderRepo, paymentRepo, session, clock);
        reports = new ReportService(new ReportRepository(db), session, clock);
        settings = new StoreSettingsService(new SettingsRepository(db), session, clock);
    }

    void loginAdmin() {
        auth.logout();
        auth.login(ADMIN, "Admin123!".toCharArray());
    }

    void loginClient() {
        auth.logout();
        auth.login(CLIENT, "Cliente123!".toCharArray());
    }

    void loginNewClient(String email) {
        auth.logout();
        auth.register("Otro Cliente", email, null, "Futbol2026".toCharArray(), "Futbol2026".toCharArray());
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
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Sin resultados: " + sql);
                }
                return rs.getLong(1);
            }
        });
    }

    void exec(String sql) {
        db.withConnection(c -> c.createStatement().executeUpdate(sql));
    }
}
