package com.sportforce3d.design;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PgeyDesignDocument(@JsonProperty("schemaVersion") int pgeySchemaVersion, @JsonProperty("bodyType") PgeyBodyType pgeyBodyType, @JsonProperty("productId") Long pgeyProductId, @JsonProperty("colors") Map<PgeyZone, String> pgeyColors,
                             @JsonProperty("patterns") Map<HepyGarment, PgeyPatternSpec> pgeyPatterns, @JsonProperty("materials") Map<PgeyMaterialSlot, String> pgeyMaterials,
                             @JsonProperty("elements") List<PgeyDesignElement> pgeyElements, @JsonProperty("realistic") PgeyRealisticSpec pgeyRealistic, @JsonProperty("cut") PgeyGarmentCut pgeyCut) {
    public static final int PGEY_SCHEMA_VERSION = 4;

    public PgeyDesignDocument {
        pgeyBodyType = pgeyBodyType == null ? PgeyBodyType.ADULTO_HOMBRE : pgeyBodyType;
        Map<PgeyZone, String> pgeyC = new EnumMap<>(PgeyZone.class);
        if (pgeyColors != null) {
            pgeyColors.forEach((pgeyZ, pgeyHex) -> pgeyC.put(pgeyZ, pgeyHex.toUpperCase()));
        }
        pgeyColors = Collections.unmodifiableMap(pgeyC);
        pgeyPatterns = Collections.unmodifiableMap(pgeyPatterns == null || pgeyPatterns.isEmpty()
                ? new EnumMap<>(HepyGarment.class) : new EnumMap<>(pgeyPatterns));
        pgeyMaterials = Collections.unmodifiableMap(pgeyMaterials == null || pgeyMaterials.isEmpty()
                ? new EnumMap<>(PgeyMaterialSlot.class) : new EnumMap<>(pgeyMaterials));
        pgeyElements = List.copyOf(pgeyElements == null ? List.of() : pgeyElements);
        pgeyCut = pgeyCut == null ? PgeyGarmentCut.PGEY_DEFAULT : pgeyCut;
    }

    public PgeyRealisticSpec pgeyRealisticOrDefault() {
        return pgeyRealistic != null ? pgeyRealistic : PgeyRealisticSpec.pgeyDefaults();
    }

    public boolean pgeyRealisticPhotosMatch() {
        return PgeyViewSync.pgeyRealisticMatches(this);
    }

    public String pgeyColor(PgeyZone pgeyZone) {
        return pgeyColors.getOrDefault(pgeyZone, "#F2F5F8");
    }

    public PgeyPatternSpec pgeyPattern(HepyGarment pgeyGarment) {
        return pgeyPatterns.getOrDefault(pgeyGarment, PgeyPatternSpec.PGEY_PLAIN);
    }

    public String pgeyMaterial(PgeyMaterialSlot pgeySlot) {
        return pgeyMaterials.getOrDefault(pgeySlot, pgeySlot.pgeyDefaultCode());
    }

    public Optional<PgeyDesignElement> pgeyElement(String pgeyId) {
        return pgeyElements.stream().filter(pgeyE -> pgeyE.pgeyId().equals(pgeyId)).findFirst();
    }

    public PgeyDesignDocument pgeyWithBodyType(PgeyBodyType pgeyType) {
        return new PgeyDesignDocument(pgeySchemaVersion, pgeyType, pgeyProductId, pgeyColors, pgeyPatterns, pgeyMaterials, pgeyElements, pgeyRealistic, pgeyCut);
    }

    public PgeyDesignDocument pgeyWithProduct(Long pgeyId) {
        return new PgeyDesignDocument(pgeySchemaVersion, pgeyBodyType, pgeyId, pgeyColors, pgeyPatterns, pgeyMaterials, pgeyElements, pgeyRealistic, pgeyCut);
    }

    public PgeyDesignDocument pgeyWithColor(PgeyZone pgeyZone, String pgeyHex) {
        Map<PgeyZone, String> pgeyC = new EnumMap<>(PgeyZone.class);
        pgeyC.putAll(pgeyColors);
        pgeyC.put(pgeyZone, pgeyHex);
        return new PgeyDesignDocument(pgeySchemaVersion, pgeyBodyType, pgeyProductId, pgeyC, pgeyPatterns, pgeyMaterials, pgeyElements, pgeyRealistic, pgeyCut);
    }

    public PgeyDesignDocument pgeyWithPattern(HepyGarment pgeyGarment, PgeyPatternSpec pgeySpec) {
        Map<HepyGarment, PgeyPatternSpec> pgeyP = new EnumMap<>(HepyGarment.class);
        pgeyP.putAll(pgeyPatterns);
        pgeyP.put(pgeyGarment, pgeySpec);
        return new PgeyDesignDocument(pgeySchemaVersion, pgeyBodyType, pgeyProductId, pgeyColors, pgeyP, pgeyMaterials, pgeyElements, pgeyRealistic, pgeyCut);
    }

    public PgeyDesignDocument pgeyWithMaterial(PgeyMaterialSlot pgeySlot, String pgeyCode) {
        Map<PgeyMaterialSlot, String> pgeyM = new EnumMap<>(PgeyMaterialSlot.class);
        pgeyM.putAll(pgeyMaterials);
        pgeyM.put(pgeySlot, pgeyCode);
        return new PgeyDesignDocument(pgeySchemaVersion, pgeyBodyType, pgeyProductId, pgeyColors, pgeyPatterns, pgeyM, pgeyElements, pgeyRealistic, pgeyCut);
    }

    public PgeyDesignDocument pgeyWithElements(List<PgeyDesignElement> pgeyList) {
        return new PgeyDesignDocument(pgeySchemaVersion, pgeyBodyType, pgeyProductId, pgeyColors, pgeyPatterns, pgeyMaterials, pgeyList, pgeyRealistic, pgeyCut);
    }

    public PgeyDesignDocument pgeyWithRealistic(PgeyRealisticSpec pgeySpec) {
        return new PgeyDesignDocument(pgeySchemaVersion, pgeyBodyType, pgeyProductId, pgeyColors, pgeyPatterns, pgeyMaterials, pgeyElements, pgeySpec, pgeyCut);
    }

    public PgeyDesignDocument pgeyWithCut(PgeyGarmentCut pgeyC) {
        PgeyGarmentCut pgeyNext = pgeyC == null ? PgeyGarmentCut.PGEY_DEFAULT : pgeyC;
        List<PgeyDesignElement> pgeyList = pgeyElements;
        if (pgeyNext.pgeyShorts() != pgeyCut.pgeyShorts()) {
            PgeyGarmentCut.PgeyShortsLength pgeyFrom = pgeyCut.pgeyShorts();
            PgeyGarmentCut.PgeyShortsLength pgeyTo = pgeyNext.pgeyShorts();
            pgeyList = pgeyElements.stream().map(pgeyE -> pgeyE.pgeyType() == PgeyElementType.NUMBER && pgeyE.pgeyArea() == PgeyArea.SHORTS_LEFT
                    && Math.abs(pgeyE.pgeyV() - pgeyFrom.pgeyNumberV()) < 0.01 && Math.abs(pgeyE.pgeyU() - pgeyFrom.pgeyNumberU()) < 0.01
                    ? pgeyE.pgeyAt(pgeyTo.pgeyNumberU(), pgeyTo.pgeyNumberV()) : pgeyE).toList();
        }
        return new PgeyDesignDocument(pgeySchemaVersion, pgeyBodyType, pgeyProductId, pgeyColors, pgeyPatterns, pgeyMaterials, pgeyList, pgeyRealistic, pgeyNext);
    }

    public PgeyDesignDocument pgeyAddElement(PgeyDesignElement pgeyE) {
        List<PgeyDesignElement> pgeyList = new ArrayList<>(pgeyElements);
        pgeyList.add(pgeyE);
        return pgeyWithElements(pgeyList);
    }

    public PgeyDesignDocument pgeyReplaceElement(PgeyDesignElement pgeyE) {
        List<PgeyDesignElement> pgeyList = new ArrayList<>(pgeyElements);
        for (int pgeyI = 0; pgeyI < pgeyList.size(); pgeyI++) {
            if (pgeyList.get(pgeyI).pgeyId().equals(pgeyE.pgeyId())) {
                pgeyList.set(pgeyI, pgeyE);
            }
        }
        return pgeyWithElements(pgeyList);
    }

    public PgeyDesignDocument pgeyRemoveElement(String pgeyId) {
        return pgeyWithElements(pgeyElements.stream().filter(pgeyE -> !pgeyE.pgeyId().equals(pgeyId)).toList());
    }

    public PgeyDesignDocument pgeyMoveLayer(String pgeyId, int pgeyDelta) {
        List<PgeyDesignElement> pgeyList = new ArrayList<>(pgeyElements);
        for (int pgeyI = 0; pgeyI < pgeyList.size(); pgeyI++) {
            if (pgeyList.get(pgeyI).pgeyId().equals(pgeyId)) {
                int pgeyJ = Math.max(0, Math.min(pgeyList.size() - 1, pgeyI + pgeyDelta));
                PgeyDesignElement pgeyE = pgeyList.remove(pgeyI);
                pgeyList.add(pgeyJ, pgeyE);
                break;
            }
        }
        return pgeyWithElements(pgeyList);
    }
}
