package com.sportforce3d.model;

public enum HepyProductSort {
    RELEVANCE("Relevancia"),
    NEWEST("Novedades"),
    PRICE_ASC("Precio: menor a mayor"),
    PRICE_DESC("Precio: mayor a menor"),
    NAME("Nombre A–Z");

    private final String hepyLabel;

    HepyProductSort(String hepyLabel) {
        this.hepyLabel = hepyLabel;
    }

    public String hepyLabel() {
        return hepyLabel;
    }

    @Override
    public String toString() {
        return hepyLabel;
    }
}
