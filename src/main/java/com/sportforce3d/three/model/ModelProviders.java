package com.sportforce3d.three.model;

import com.sportforce3d.three.obj.ObjModelProvider;
import com.sportforce3d.three.procedural.ProceduralModelProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ModelProviders {

    private static final Map<BodyType, ModelData> CACHE = new ConcurrentHashMap<>();

    private ModelProviders() {
    }

    public static ModelProvider forBody(BodyType type) {
        return ObjModelProvider.available(type) ? new ObjModelProvider() : new ProceduralModelProvider();
    }

    public static ModelData load(BodyType type) {
        return CACHE.computeIfAbsent(type, t -> forBody(t).load(t));
    }
}
