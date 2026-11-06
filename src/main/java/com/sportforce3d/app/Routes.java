package com.sportforce3d.app;

import java.util.Set;

/** Ids de las vistas de la tienda. Coinciden con /fxml/{id}.fxml. */
public final class Routes {

    public static final String HOME = "home";
    public static final String CATALOG = "catalog";
    public static final String PRODUCT = "product";
    public static final String CART = "cart";
    public static final String CHECKOUT = "checkout";
    public static final String ORDERS = "orders";
    public static final String ORDER = "order";
    public static final String DESIGNER = "designer";
    public static final String DESIGNS = "designs";
    public static final String ACCOUNT = "account";
    public static final String ADMIN_USERS = "admin-users";
    public static final String ADMIN_PRODUCTS = "admin-products";
    public static final String ADMIN_PRODUCT_EDIT = "admin-product-edit";
    public static final String ADMIN_INVENTORY = "admin-inventory";
    public static final String ADMIN_DASHBOARD = "admin-dashboard";
    public static final String ADMIN_ORDERS = "admin-orders";
    /** Parámetro: id del pedido (Long). */
    public static final String ADMIN_ORDER = "admin-order";
    public static final String ADMIN_PAYMENTS = "admin-payments";
    /** Visor del uniforme de una línea de pedido. Parámetro: id de order_items (Long). */
    public static final String ADMIN_DESIGN = "admin-design";
    public static final String ADMIN_REPORTS = "admin-reports";
    /** Datos de pago (Yape, Plin, transferencia). */
    public static final String ADMIN_SETTINGS = "admin-settings";

    /** Vistas que requieren rol ADMIN (el service lo vuelve a comprobar). */
    public static final Set<String> ADMIN_ONLY = Set.of(ADMIN_USERS, ADMIN_PRODUCTS, ADMIN_PRODUCT_EDIT, ADMIN_INVENTORY,
            ADMIN_DASHBOARD, ADMIN_ORDERS, ADMIN_ORDER, ADMIN_PAYMENTS, ADMIN_DESIGN, ADMIN_REPORTS, ADMIN_SETTINGS);

    private Routes() {
    }

    /** Parámetro de la ruta CATALOG: búsqueda y/o categoría iniciales. */
    public record CatalogParams(String text, Long categoryId) {
    }

    /**
     * Parámetro de la ruta DESIGNER: prenda a enfocar al abrir (null = la camiseta), diseño guardado a abrir y
     * producto del catálogo que se personaliza (aporta el precio base). Todos pueden ser null.
     */
    public record DesignerParams(com.sportforce3d.model.Garment focus, Long designId, Long productId) {

        public static DesignerParams focus(com.sportforce3d.model.Garment g) {
            return new DesignerParams(g, null, null);
        }

        public static DesignerParams open(long designId) {
            return new DesignerParams(null, designId, null);
        }
    }
}