package com.sportforce3d.model;

/** Talla. {@code group}: ROPA, CALZADO, MEDIAS o UNICA. */
public record Size(long id, String code, String name, String group) {

    @Override
    public String toString() {
        return name;
    }
}
