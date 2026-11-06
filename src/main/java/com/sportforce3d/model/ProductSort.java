package com.sportforce3d.model;

public enum ProductSort {
    RELEVANCE("Relevancia"),
    NEWEST("Novedades"),
    PRICE_ASC("Precio: menor a mayor"),
    PRICE_DESC("Precio: mayor a menor"),
    NAME("Nombre A–Z");

    private final String label;

    ProductSort(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
