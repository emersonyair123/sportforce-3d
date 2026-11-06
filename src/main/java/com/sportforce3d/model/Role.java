package com.sportforce3d.model;

public enum Role {
    ADMIN("Administrador"),
    CLIENTE("Cliente");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
