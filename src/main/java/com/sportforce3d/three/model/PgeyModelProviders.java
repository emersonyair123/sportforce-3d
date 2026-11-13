package com.sportforce3d.three.model;

import com.sportforce3d.three.obj.PgeyObjModelProvider;
import com.sportforce3d.three.procedural.PgeyProceduralModelProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PgeyModelProviders {

    private static final Map<PgeyBodyType, PgeyModelData> PGEY_CACHE = new ConcurrentHashMap<>();

    private PgeyModelProviders() {
    }

    public static PgeyModelProvider pgeyForBody(PgeyBodyType pgeyType) {
        return PgeyObjModelProvider.pgeyAvailable(pgeyType) ? new PgeyObjModelProvider() : new PgeyProceduralModelProvider();
    }

    public static PgeyModelData pgeyLoad(PgeyBodyType pgeyType) {
        return PGEY_CACHE.computeIfAbsent(pgeyType, pgeyT -> pgeyForBody(pgeyT).pgeyLoad(pgeyT));
    }
}
