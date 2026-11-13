package com.sportforce3d.model;

import java.util.Set;

public record HepyMaterial(long hepyId, String hepyCode, String hepyName, String hepyComposition, String hepyUsage,
                       HepyScope hepyScope, Set<String> hepySuitableFor, long hepySurchargeCents,
                       double hepyRoughness, double hepySheen, double hepyMetalness, HepyPattern hepyPreviewPattern) {

    public enum HepyScope { TEXTIL, CALZADO }

    public enum HepyPattern { DOTS, MESH, DIAGONAL, LINES, PLAIN, GRID, KNIT, SMOOTH, GRAIN, RUBBER, STUDS, REFLECTIVE }

    public HepyMaterial {
        hepySuitableFor = Set.copyOf(hepySuitableFor);
    }

    public boolean hepyIsIncluded() {
        return hepySurchargeCents == 0;
    }
}
