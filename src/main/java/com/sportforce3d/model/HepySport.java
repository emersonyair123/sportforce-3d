package com.sportforce3d.model;

public record HepySport(long hepyId, String hepySlug, String hepyName) {

    @Override
    public String toString() {
        return hepyName;
    }
}
