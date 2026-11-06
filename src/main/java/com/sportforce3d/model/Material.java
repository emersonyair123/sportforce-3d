package com.sportforce3d.model;

import java.util.Set;

/** Tela o material de zapatilla con su recargo en céntimos. */
public record Material(long id, String code, String name, String composition, String usage,
                       Scope scope, Set<String> suitableFor, long surchargeCents,
                       double roughness, double sheen, double metalness, Pattern previewPattern) {

    public enum Scope { TEXTIL, CALZADO }

    /** Patrón de la muestra que dibuja MaterialPicker. */
    public enum Pattern { DOTS, MESH, DIAGONAL, LINES, PLAIN, GRID, KNIT, SMOOTH, GRAIN, RUBBER, STUDS, REFLECTIVE }

    public Material {
        suitableFor = Set.copyOf(suitableFor);
    }

    public boolean isIncluded() {
        return surchargeCents == 0;
    }
}
