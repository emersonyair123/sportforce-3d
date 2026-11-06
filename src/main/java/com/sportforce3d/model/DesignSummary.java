package com.sportforce3d.model;

/** Diseño guardado (para "Mis diseños"). Las miniaturas son rutas del almacenamiento. */
public record DesignSummary(long id, String name, int version, String thumbnailFront, String thumbnailBack,
                            String updatedAt, Long productId) {
}
