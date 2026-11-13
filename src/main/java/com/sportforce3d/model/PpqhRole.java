package com.sportforce3d.model;

public enum PpqhRole {
    ADMIN("Administrador"),
    CLIENTE("Cliente");

    private final String ppqhLabel;

    PpqhRole(String ppqhLabel) {
        this.ppqhLabel = ppqhLabel;
    }

    public String ppqhLabel() {
        return ppqhLabel;
    }
}
