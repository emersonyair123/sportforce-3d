package com.sportforce3d.model;

public record Sport(long id, String slug, String name) {

    @Override
    public String toString() {
        return name;
    }
}
