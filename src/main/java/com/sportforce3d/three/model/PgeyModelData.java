package com.sportforce3d.three.model;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyVec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record PgeyModelData(PgeyBodyType pgeyBodyType, List<PgeyPart> pgeyMannequin, Map<PgeyZone, List<PgeyPart>> pgeyZones,
                        Map<HepyGarment, PgeyFocus> pgeyFocus, double pgeyHeight) {
    public record PgeyPart(String pgeyName, PgeyMeshData pgeyMesh) {
    }

    public record PgeyFocus(PgeyVec3 pgeyTarget, double pgeyDistance, double pgeyPitch) {
    }

    public PgeyModelData {
        pgeyMannequin = List.copyOf(pgeyMannequin);
        Map<PgeyZone, List<PgeyPart>> pgeyCopy = new EnumMap<>(PgeyZone.class);
        pgeyZones.forEach((pgeyZ, pgeyParts) -> pgeyCopy.put(pgeyZ, List.copyOf(pgeyParts)));
        pgeyZones = pgeyCopy;
        pgeyFocus = Map.copyOf(pgeyFocus);
    }

    public int pgeyTotalFaces() {
        int pgeyFaces = pgeyMannequin.stream().mapToInt(pgeyP -> pgeyP.pgeyMesh().pgeyFaceCount()).sum();
        for (List<PgeyPart> pgeyParts : pgeyZones.values()) {
            pgeyFaces += pgeyParts.stream().mapToInt(pgeyP -> pgeyP.pgeyMesh().pgeyFaceCount()).sum();
        }
        return pgeyFaces;
    }
}
