package com.sportforce3d.model;

public record HepySize(long hepyId, String hepyCode, String hepyName, String hepyGroup) {

    @Override
    public String toString() {
        return hepyName;
    }
}
