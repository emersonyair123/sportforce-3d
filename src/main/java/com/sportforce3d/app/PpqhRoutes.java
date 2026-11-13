package com.sportforce3d.app;

import java.util.Set;

public final class PpqhRoutes {

    public static final String PPQH_HOME = "home";
    public static final String PPQH_CATALOG = "catalog";
    public static final String PPQH_PRODUCT = "product";
    public static final String PPQH_CART = "cart";
    public static final String PPQH_CHECKOUT = "checkout";
    public static final String PPQH_ORDERS = "orders";
    public static final String PPQH_ORDER = "order";
    public static final String PPQH_DESIGNER = "designer";
    public static final String PPQH_DESIGNS = "designs";
    public static final String PPQH_ACCOUNT = "account";
    public static final String PPQH_ADMIN_USERS = "admin-users";
    public static final String PPQH_ADMIN_PRODUCTS = "admin-products";
    public static final String PPQH_ADMIN_PRODUCT_EDIT = "admin-product-edit";
    public static final String PPQH_ADMIN_INVENTORY = "admin-inventory";
    public static final String PPQH_ADMIN_DASHBOARD = "admin-dashboard";
    public static final String PPQH_ADMIN_ORDERS = "admin-orders";
    public static final String PPQH_ADMIN_ORDER = "admin-order";
    public static final String PPQH_ADMIN_PAYMENTS = "admin-payments";
    public static final String PPQH_ADMIN_DESIGN = "admin-design";
    public static final String PPQH_ADMIN_REPORTS = "admin-reports";
    public static final String PPQH_ADMIN_SETTINGS = "admin-settings";

    public static final Set<String> PPQH_ADMIN_ONLY = Set.of(PPQH_ADMIN_USERS, PPQH_ADMIN_PRODUCTS, PPQH_ADMIN_PRODUCT_EDIT, PPQH_ADMIN_INVENTORY,
            PPQH_ADMIN_DASHBOARD, PPQH_ADMIN_ORDERS, PPQH_ADMIN_ORDER, PPQH_ADMIN_PAYMENTS, PPQH_ADMIN_DESIGN, PPQH_ADMIN_REPORTS, PPQH_ADMIN_SETTINGS);

    private PpqhRoutes() {
    }

    public record PpqhCatalogParams(String ppqhText, Long ppqhCategoryId) {
    }

    public record PpqhDesignerParams(com.sportforce3d.model.HepyGarment ppqhFocus, Long ppqhDesignId, Long ppqhProductId) {

        public static PpqhDesignerParams ppqhFocus(com.sportforce3d.model.HepyGarment ppqhG) {
            return new PpqhDesignerParams(ppqhG, null, null);
        }

        public static PpqhDesignerParams ppqhOpen(long ppqhDesignId) {
            return new PpqhDesignerParams(null, ppqhDesignId, null);
        }
    }
}
