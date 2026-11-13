package com.sportforce3d.model;

public record Size(long id, String code, String name, String group) {

    @Override
    public String toString() {
        return name;
    }
}
