package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.PgeyMeshData;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.List;

public record PgeyHumanModel(PgeyPersona pgeyPersona, List<PgeyPart> pgeyParts, double pgeyMinY, double pgeyHeight) {
    public record PgeyPart(String pgeyName, PgeyMeshData pgeyMesh, Color pgeyDiffuse, Color pgeySpecular, double pgeySpecularPower, Image pgeyTexture,
                       boolean pgeyTransparent) {
    }

    public PgeyHumanModel {
        pgeyParts = List.copyOf(pgeyParts);
    }
}
