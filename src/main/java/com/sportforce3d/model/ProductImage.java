package com.sportforce3d.model;

/** Imagen de producto; {@code path} es relativo a la carpeta de almacenamiento. */
public record ProductImage(long id, String path, String altText, int sortOrder) {
}
