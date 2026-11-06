package com.sportforce3d.app;

/**
 * Navegación dentro de la tienda (shell). Las rutas son los ids de {@link Routes}; {@code param}
 * depende de la ruta (p. ej. el id del producto para {@link Routes#PRODUCT}).
 */
public interface Router {

    void go(String route, Object param);

    default void go(String route) {
        go(route, null);
    }

    /** Vuelve a la vista anterior, o al inicio si no hay historial. */
    void back();

    /** Los controladores que reciben parámetros implementan esta interfaz. */
    interface Routable {
        void onRoute(Object param);
    }
}
