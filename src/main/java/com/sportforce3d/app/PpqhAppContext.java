package com.sportforce3d.app;

import com.sportforce3d.config.PpqhAppConfig;
import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.render.PgeyRealisticModelRepository;
import com.sportforce3d.render.PgeyRecolorService;
import com.sportforce3d.repository.HepyAddressRepository;
import com.sportforce3d.repository.HepyCartRepository;
import com.sportforce3d.repository.HepyCatalogRepository;
import com.sportforce3d.repository.HepyDashboardRepository;
import com.sportforce3d.repository.HepyDesignRepository;
import com.sportforce3d.repository.HepyLoginAttemptRepository;
import com.sportforce3d.repository.HepyOrderRepository;
import com.sportforce3d.repository.HepyPaymentRepository;
import com.sportforce3d.repository.HepyProductRepository;
import com.sportforce3d.repository.HepyReportRepository;
import com.sportforce3d.repository.HepySettingsRepository;
import com.sportforce3d.repository.HepyUserRepository;
import com.sportforce3d.service.PpqhAccountService;
import com.sportforce3d.service.PpqhAddressService;
import com.sportforce3d.service.PpqhAdminOrderService;
import com.sportforce3d.service.HepyAdminProductService;
import com.sportforce3d.service.PpqhAuthService;
import com.sportforce3d.service.HepyCartService;
import com.sportforce3d.service.HepyCatalogService;
import com.sportforce3d.service.PpqhDashboardService;
import com.sportforce3d.service.PgeyDesignService;
import com.sportforce3d.service.PpqhLoginThrottle;
import com.sportforce3d.service.PpqhOrderService;
import com.sportforce3d.service.PpqhPasswordHasher;
import com.sportforce3d.service.PpqhPaymentService;
import com.sportforce3d.service.PpqhReportService;
import com.sportforce3d.service.PpqhSession;
import com.sportforce3d.service.PpqhStoreSettingsService;
import com.sportforce3d.service.PpqhUserAdminService;
import com.sportforce3d.storage.HepyStorageService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.time.Clock;

public final class PpqhAppContext {

    private final PpqhAppConfig ppqhConfig;
    private final HepyDatabase ppqhDatabase;
    private final HepyStorageService ppqhStorage;
    private final PpqhSession ppqhSession;
    private final PpqhAuthService ppqhAuthService;
    private final PpqhAccountService ppqhAccountService;
    private final PpqhUserAdminService ppqhUserAdminService;
    private final HepyCatalogService ppqhCatalogService;
    private final HepyCartService ppqhCartService;
    private final PpqhAddressService ppqhAddressService;
    private final PpqhOrderService ppqhOrderService;
    private final HepyAdminProductService ppqhAdminProductService;
    private final PgeyDesignService ppqhDesignService;
    private final PpqhPaymentService ppqhPaymentService;
    private final PpqhAdminOrderService ppqhAdminOrderService;
    private final PpqhDashboardService ppqhDashboardService;
    private final PpqhReportService ppqhReportService;
    private final PpqhStoreSettingsService ppqhStoreSettingsService;
    private final PgeyRecolorService ppqhRecolorService;
    private final PpqhUiSettings ppqhUiSettings;
    private final IntegerProperty ppqhCartCount = new SimpleIntegerProperty(0);
    private PpqhNavigator ppqhNavigator;
    private PpqhRouter ppqhRouter;

    public PpqhAppContext(PpqhAppConfig ppqhConfig, PpqhUiSettings ppqhUiSettings) {
        this.ppqhConfig = ppqhConfig;
        this.ppqhUiSettings = ppqhUiSettings;
        this.ppqhDatabase = new HepyDatabase(ppqhConfig.ppqhDatabaseFile());
        this.ppqhStorage = new HepyStorageService(ppqhConfig.ppqhStorageDir());

        Clock ppqhClock = Clock.systemUTC();
        PpqhPasswordHasher ppqhHasher = new PpqhPasswordHasher();
        HepyUserRepository ppqhUsers = new HepyUserRepository(ppqhDatabase);
        HepyCatalogRepository ppqhCatalog = new HepyCatalogRepository(ppqhDatabase);
        HepyProductRepository ppqhProducts = new HepyProductRepository(ppqhDatabase);
        HepyCartRepository ppqhCarts = new HepyCartRepository(ppqhDatabase);
        HepyAddressRepository ppqhAddresses = new HepyAddressRepository(ppqhDatabase);
        HepyOrderRepository ppqhOrders = new HepyOrderRepository(ppqhDatabase);
        HepyPaymentRepository ppqhPayments = new HepyPaymentRepository(ppqhDatabase);
        HepyDesignRepository ppqhDesigns = new HepyDesignRepository(ppqhDatabase);

        this.ppqhSession = new PpqhSession(ppqhUsers::hepyFindById);
        this.ppqhAuthService = new PpqhAuthService(ppqhUsers, ppqhHasher, ppqhSession, new PpqhLoginThrottle(ppqhClock, new HepyLoginAttemptRepository(ppqhDatabase)), ppqhClock);
        this.ppqhAccountService = new PpqhAccountService(ppqhUsers, ppqhHasher, ppqhSession);
        this.ppqhUserAdminService = new PpqhUserAdminService(ppqhUsers, ppqhSession);
        this.ppqhCatalogService = new HepyCatalogService(ppqhCatalog, ppqhProducts);
        this.ppqhCartService = new HepyCartService(ppqhCarts, ppqhProducts, ppqhCatalogService, ppqhDesigns, ppqhSession);
        this.ppqhAddressService = new PpqhAddressService(ppqhAddresses, ppqhSession);
        this.ppqhOrderService = new PpqhOrderService(ppqhDatabase, ppqhOrders, ppqhCarts, ppqhAddresses, ppqhCartService, ppqhDesigns, ppqhCatalogService,
                ppqhStorage, ppqhSession, ppqhClock);
        this.ppqhPaymentService = new PpqhPaymentService(ppqhDatabase, ppqhPayments, ppqhOrders, ppqhStorage, ppqhSession, ppqhClock);
        this.ppqhAdminOrderService = new PpqhAdminOrderService(ppqhDatabase, ppqhOrders, ppqhOrderService, ppqhDesigns, ppqhCatalogService, ppqhStorage,
                ppqhSession, ppqhClock);
        this.ppqhDashboardService = new PpqhDashboardService(new HepyDashboardRepository(ppqhDatabase), ppqhOrders, ppqhPayments, ppqhSession, ppqhClock);
        this.ppqhReportService = new PpqhReportService(new HepyReportRepository(ppqhDatabase), ppqhSession, ppqhClock);
        this.ppqhStoreSettingsService = new PpqhStoreSettingsService(new HepySettingsRepository(ppqhDatabase), ppqhSession, ppqhClock);
        this.ppqhAdminProductService = new HepyAdminProductService(ppqhProducts, ppqhCatalog, ppqhStorage, ppqhSession, ppqhClock);
        java.nio.file.Path ppqhDataDir = ppqhConfig.ppqhDatabaseFile().getParent();
        this.ppqhDesignService = new PgeyDesignService(ppqhDesigns, ppqhStorage,
                new com.sportforce3d.design.PgeyDraftStore((ppqhDataDir == null ? java.nio.file.Path.of(".") : ppqhDataDir).resolve("drafts")),
                ppqhSession, ppqhClock);
        this.ppqhRecolorService = new PgeyRecolorService(new PgeyRealisticModelRepository());
    }

    public PpqhAppConfig ppqhConfig() {
        return ppqhConfig;
    }

    public HepyDatabase ppqhDatabase() {
        return ppqhDatabase;
    }

    public HepyStorageService ppqhStorage() {
        return ppqhStorage;
    }

    public PpqhSession ppqhSession() {
        return ppqhSession;
    }

    public PpqhAuthService ppqhAuthService() {
        return ppqhAuthService;
    }

    public PpqhAccountService ppqhAccountService() {
        return ppqhAccountService;
    }

    public PpqhUserAdminService ppqhUserAdminService() {
        return ppqhUserAdminService;
    }

    public HepyCatalogService ppqhCatalogService() {
        return ppqhCatalogService;
    }

    public HepyCartService ppqhCartService() {
        return ppqhCartService;
    }

    public PpqhAddressService ppqhAddressService() {
        return ppqhAddressService;
    }

    public PpqhOrderService ppqhOrderService() {
        return ppqhOrderService;
    }

    public HepyAdminProductService ppqhAdminProductService() {
        return ppqhAdminProductService;
    }

    public PgeyDesignService ppqhDesignService() {
        return ppqhDesignService;
    }

    public PpqhPaymentService ppqhPaymentService() {
        return ppqhPaymentService;
    }

    public PpqhAdminOrderService ppqhAdminOrderService() {
        return ppqhAdminOrderService;
    }

    public PpqhDashboardService ppqhDashboardService() {
        return ppqhDashboardService;
    }

    public PpqhReportService ppqhReportService() {
        return ppqhReportService;
    }

    public PpqhStoreSettingsService ppqhStoreSettingsService() {
        return ppqhStoreSettingsService;
    }

    public PgeyRecolorService ppqhRecolorService() {
        return ppqhRecolorService;
    }

    public PpqhUiSettings ppqhUiSettings() {
        return ppqhUiSettings;
    }

    public PpqhNavigator ppqhNavigator() {
        return ppqhNavigator;
    }

    void ppqhSetNavigator(PpqhNavigator ppqhNavigator) {
        this.ppqhNavigator = ppqhNavigator;
    }

    public PpqhRouter ppqhRouter() {
        return ppqhRouter;
    }

    public void ppqhSetRouter(PpqhRouter ppqhRouter) {
        this.ppqhRouter = ppqhRouter;
    }

    public ReadOnlyIntegerProperty cartCountProperty() {
        return ppqhCartCount;
    }

    public void ppqhRefreshCartCount() {
        PpqhAsync.ppqhRun(ppqhCartService::hepyCountUnits, ppqhCartCount::set, ppqhE -> { });
    }

    public void setCartCount(int ppqhUnits) {
        ppqhCartCount.set(ppqhUnits);
    }
}
