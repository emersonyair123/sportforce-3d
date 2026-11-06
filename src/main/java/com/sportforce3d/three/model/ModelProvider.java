package com.sportforce3d.three.model;

/**
 * Fuente de modelos del diseñador. {@code ProceduralModelProvider} genera todo por código (predeterminado);
 * {@code ObjModelProvider} carga un modelo exportado desde Blender con las zonas nombradas igual que {@link Zone}.
 * No usa objetos de JavaFX: se llama en segundo plano.
 */
public interface ModelProvider {

    ModelData load(BodyType type);

    /** Nombre para mostrar en logs y en la interfaz. */
    String name();
}
