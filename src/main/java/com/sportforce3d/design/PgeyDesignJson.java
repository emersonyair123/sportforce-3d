package com.sportforce3d.design;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class PgeyDesignJson {

    private static final ObjectMapper PGEY_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private PgeyDesignJson() {
    }

    public static String pgeyWrite(PgeyDesignDocument pgeyDoc) {
        try {
            return PGEY_MAPPER.writeValueAsString(pgeyDoc);
        } catch (JsonProcessingException pgeyE) {
            throw new IllegalStateException("No se pudo convertir el diseño a JSON", pgeyE);
        }
    }

    private static PgeyDesignDocument pgeyUpgrade(PgeyDesignDocument pgeyOld) {
        PgeyDesignDocument pgeyUp = new PgeyDesignDocument(PgeyDesignDocument.PGEY_SCHEMA_VERSION, pgeyOld.pgeyBodyType(), pgeyOld.pgeyProductId(),
                pgeyOld.pgeyColors(), pgeyOld.pgeyPatterns(), pgeyOld.pgeyMaterials(), pgeyOld.pgeyElements(), pgeyOld.pgeyRealistic(), pgeyOld.pgeyCut());
        return pgeySynced(pgeyUp);
    }

    static PgeyDesignDocument pgeySynced(PgeyDesignDocument pgeyUp) {
        PgeyRealisticSpec pgeyR = pgeyUp.pgeyRealistic();
        if (pgeyR == null) {
            return pgeyUp;
        }
        boolean pgeySerieCut = pgeyUp.pgeyCut().equals(PgeyGarmentCut.PGEY_DEFAULT)
                && pgeyUp.pgeyMaterial(PgeyMaterialSlot.JERSEY).equals(PgeyMaterialSlot.JERSEY.pgeyDefaultCode())
                && pgeyUp.pgeyMaterial(PgeyMaterialSlot.SHORTS).equals(PgeyMaterialSlot.SHORTS.pgeyDefaultCode());
        if (pgeySerieCut && !pgeyR.equals(PgeyRealisticSpec.pgeyDefaults())) {
            return PgeyViewSync.pgeySeedFromRealistic(pgeyUp, pgeyR);
        }
        return PgeyViewSync.pgeyNormalize(pgeyUp);
    }

    public static PgeyDesignDocument pgeyRead(String pgeyJson) {
        try {
            JsonNode pgeyTree = PGEY_MAPPER.readTree(pgeyJson);
            int pgeyVersion = pgeyTree.path("schemaVersion").asInt(0);
            if (pgeyVersion < 1) {
                throw new IllegalArgumentException("El archivo no es un diseño de SPORTFORCE 3D.");
            }
            if (pgeyVersion > PgeyDesignDocument.PGEY_SCHEMA_VERSION) {
                throw new IllegalArgumentException("Este diseño se creó con una versión más nueva de la aplicación.");
            }
            PgeyDesignDocument pgeyDoc = PGEY_MAPPER.treeToValue(pgeyTree, PgeyDesignDocument.class);
            return pgeyVersion == PgeyDesignDocument.PGEY_SCHEMA_VERSION ? pgeyDoc : pgeyUpgrade(pgeyDoc);
        } catch (JsonProcessingException pgeyE) {
            throw new IllegalArgumentException("El diseño está dañado y no se puede abrir.", pgeyE);
        }
    }
}
