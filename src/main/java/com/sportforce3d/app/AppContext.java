package com.sportforce3d.app;

import com.sportforce3d.config.AppConfig;
import com.sportforce3d.db.Database;
import com.sportforce3d.render.RealisticModelRepository;
import com.sportforce3d.render.RecolorService;
import com.sportforce3d.repository.AddressRepository;
import com.sportforce3d.repository.CartRepository;
import com.sportforce3d.repository.CatalogRepository;
import com.sportforce3d.repository.DashboardRepository;
import com.sportforce3d.repository.DesignRepository;
import com.sportforce3d.repository.LoginAttemptRepository;
import com.sportforce3d.repository.OrderRepository;
import com.sportforce3d.repository.PaymentRepository;
import com.sportforce3d.repository.ProductRepository;
import com.sportforce3d.repository.ReportRepository;
import com.sportforce3d.repository.SettingsRepository;
import com.sportforce3d.repository.UserRepository;
import com.sportforce3d.service.AccountService;
import com.sportforce3d.service.AddressService;
import com.sportforce3d.service.AdminOrderService;
import com.sportforce3d.service.AdminProductService;
import com.sportforce3d.service.AuthService;
import com.sportforce3d.service.CartService;
import com.sportforce3d.service.CatalogService;
import com.sportforce3d.service.DashboardService;
import com.sportforce3d.service.DesignService;
import com.sportforce3d.service.LoginThrottle;
import com.sportforce3d.service.OrderService;
import com.sportforce3d.service.PasswordHasher;
import com.sportforce3d.service.PaymentService;
import com.sportforce3d.service.ReportService;
import com.sportforce3d.service.Session;
import com.sportforce3d.service.StoreSettingsService;
import com.sportforce3d.service.UserAdminService;
import com.sportforce3d.storage.StorageService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.time.Clock;

public final class AppContext {

    private final AppConfig config;
    private final Database database;
    private final StorageService storage;
    private final Session session;
    private final AuthService authService;
    private final AccountService accountService;
    private final UserAdminService userAdminService;
    private final CatalogService catalogService;
    private final CartService cartService;
    private final AddressService addressService;
    private final OrderService orderService;
    private final AdminProductService adminProductService;
    private final DesignService designService;
    private final PaymentService paymentService;
    private final AdminOrderService adminOrderService;
    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final StoreSettingsService storeSettingsService;
    private final RecolorService recolorService;
    private final UiSettings uiSettings;
    private final IntegerProperty cartCount = new SimpleIntegerProperty(0);
    private Navigator navigator;
    private Router router;

    public AppContext(AppConfig config, UiSettings uiSettings) {
        this.config = config;
        this.uiSettings = uiSettings;
        this.database = new Database(config.databaseFile());
        this.storage = new StorageService(config.storageDir());

        Clock clock = Clock.systemUTC();
        PasswordHasher hasher = new PasswordHasher();
        UserRepository users = new UserRepository(database);
        CatalogRepository catalog = new CatalogRepository(database);
        ProductRepository products = new ProductRepository(database);
        CartRepository carts = new CartRepository(database);
        AddressRepository addresses = new AddressRepository(database);
        OrderRepository orders = new OrderRepository(database);
        PaymentRepository payments = new PaymentRepository(database);
        DesignRepository designs = new DesignRepository(database);

        this.session = new Session(users::findById);
        this.authService = new AuthService(users, hasher, session, new LoginThrottle(clock, new LoginAttemptRepository(database)), clock);
        this.accountService = new AccountService(users, hasher, session);
        this.userAdminService = new UserAdminService(users, session);
        this.catalogService = new CatalogService(catalog, products);
        this.cartService = new CartService(carts, products, catalogService, designs, session);
        this.addressService = new AddressService(addresses, session);
        this.orderService = new OrderService(database, orders, carts, addresses, cartService, designs, catalogService,
                storage, session, clock);
        this.paymentService = new PaymentService(database, payments, orders, storage, session, clock);
        this.adminOrderService = new AdminOrderService(database, orders, orderService, designs, catalogService, storage,
                session, clock);
        this.dashboardService = new DashboardService(new DashboardRepository(database), orders, payments, session, clock);
        this.reportService = new ReportService(new ReportRepository(database), session, clock);
        this.storeSettingsService = new StoreSettingsService(new SettingsRepository(database), session, clock);
        this.adminProductService = new AdminProductService(products, catalog, storage, session, clock);
        java.nio.file.Path dataDir = config.databaseFile().getParent();
        this.designService = new DesignService(designs, storage,
                new com.sportforce3d.design.DraftStore((dataDir == null ? java.nio.file.Path.of(".") : dataDir).resolve("drafts")),
                session, clock);
        this.recolorService = new RecolorService(new RealisticModelRepository());
    }

    public AppConfig config() {
        return config;
    }

    public Database database() {
        return database;
    }

    public StorageService storage() {
        return storage;
    }

    public Session session() {
        return session;
    }

    public AuthService authService() {
        return authService;
    }

    public AccountService accountService() {
        return accountService;
    }

    public UserAdminService userAdminService() {
        return userAdminService;
    }

    public CatalogService catalogService() {
        return catalogService;
    }

    public CartService cartService() {
        return cartService;
    }

    public AddressService addressService() {
        return addressService;
    }

    public OrderService orderService() {
        return orderService;
    }

    public AdminProductService adminProductService() {
        return adminProductService;
    }

    public DesignService designService() {
        return designService;
    }

    public PaymentService paymentService() {
        return paymentService;
    }

    public AdminOrderService adminOrderService() {
        return adminOrderService;
    }

    public DashboardService dashboardService() {
        return dashboardService;
    }

    public ReportService reportService() {
        return reportService;
    }

    public StoreSettingsService storeSettingsService() {
        return storeSettingsService;
    }

    public RecolorService recolorService() {
        return recolorService;
    }

    public UiSettings uiSettings() {
        return uiSettings;
    }

    public Navigator navigator() {
        return navigator;
    }

    void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    public Router router() {
        return router;
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    public ReadOnlyIntegerProperty cartCountProperty() {
        return cartCount;
    }

    public void refreshCartCount() {
        Async.run(cartService::countUnits, cartCount::set, e -> { });
    }

    public void setCartCount(int units) {
        cartCount.set(units);
    }
}
