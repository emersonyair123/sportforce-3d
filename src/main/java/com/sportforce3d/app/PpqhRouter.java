package com.sportforce3d.app;

public interface PpqhRouter {

    void ppqhGo(String ppqhRoute, Object ppqhParam);

    default void ppqhGo(String ppqhRoute) {
        ppqhGo(ppqhRoute, null);
    }

    void ppqhBack();

    interface PpqhRoutable {
        void ppqhOnRoute(Object ppqhParam);
    }
}
