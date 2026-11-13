package com.sportforce3d.design;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class DesignJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private DesignJson() {
    }

    public static String write(DesignDocument doc) {
        try {
            return MAPPER.writeValueAsString(doc);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo convertir el diseño a JSON", e);
        }
    }

    private static DesignDocument upgrade(DesignDocument old) {
        DesignDocument up = new DesignDocument(DesignDocument.SCHEMA_VERSION, old.bodyType(), old.productId(),
                old.colors(), old.patterns(), old.materials(), old.elements(), old.realistic(), old.cut());
        return synced(up);
    }

    static DesignDocument synced(DesignDocument up) {
        RealisticSpec r = up.realistic();
        if (r == null) {
            return up;
        }
        boolean serieCut = up.cut().equals(GarmentCut.DEFAULT)
                && up.material(MaterialSlot.JERSEY).equals(MaterialSlot.JERSEY.defaultCode())
                && up.material(MaterialSlot.SHORTS).equals(MaterialSlot.SHORTS.defaultCode());
        if (serieCut && !r.equals(RealisticSpec.defaults())) {
            return ViewSync.seedFromRealistic(up, r);
        }
        return ViewSync.normalize(up);
    }

    public static DesignDocument read(String json) {
        try {
            JsonNode tree = MAPPER.readTree(json);
            int version = tree.path("schemaVersion").asInt(0);
            if (version < 1) {
                throw new IllegalArgumentException("El archivo no es un diseño de SPORTFORCE 3D.");
            }
            if (version > DesignDocument.SCHEMA_VERSION) {
                throw new IllegalArgumentException("Este diseño se creó con una versión más nueva de la aplicación.");
            }
            DesignDocument doc = MAPPER.treeToValue(tree, DesignDocument.class);
            return version == DesignDocument.SCHEMA_VERSION ? doc : upgrade(doc);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("El diseño está dañado y no se puede abrir.", e);
        }
    }
}
