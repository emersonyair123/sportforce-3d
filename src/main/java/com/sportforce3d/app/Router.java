package com.sportforce3d.app;

public interface Router {

    void go(String route, Object param);

    default void go(String route) {
        go(route, null);
    }

    void back();

    interface Routable {
        void onRoute(Object param);
    }
}
