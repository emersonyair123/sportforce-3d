package com.sportforce3d.render;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PgeyRealisticModelRepository {

    public static final String PGEY_ROOT = "/models/realista/";

    @FunctionalInterface
    public interface PgeyImageDecoder {
        PgeyArgbImage pgeyDecode(InputStream pgeyIn) throws IOException;
    }

    public static final PgeyImageDecoder PGEY_JAVAFX = pgeyIn -> {
        Image pgeyImg = new Image(pgeyIn);
        if (pgeyImg.isError() || pgeyImg.getPixelReader() == null) {
            throw new IOException("Imagen inválida", pgeyImg.getException());
        }
        int pgeyW = (int) pgeyImg.getWidth();
        int pgeyH = (int) pgeyImg.getHeight();
        int[] pgeyPx = new int[pgeyW * pgeyH];
        pgeyImg.getPixelReader().getPixels(0, 0, pgeyW, pgeyH, PixelFormat.getIntArgbInstance(), pgeyPx, 0, pgeyW);
        return new PgeyArgbImage(pgeyW, pgeyH, pgeyPx);
    };

    private final String pgeyRoot;
    private final PgeyImageDecoder pgeyDecoder;
    private final Map<String, PgeyRealisticModel> pgeyModels;
    private final Map<String, PgeyModelLayers> pgeyLayers = new ConcurrentHashMap<>();

    public PgeyRealisticModelRepository() {
        this(PGEY_ROOT, PGEY_JAVAFX);
    }

    public PgeyRealisticModelRepository(String pgeyRoot, PgeyImageDecoder pgeyDecoder) {
        this.pgeyRoot = pgeyRoot.endsWith("/") ? pgeyRoot : pgeyRoot + "/";
        this.pgeyDecoder = pgeyDecoder;
        this.pgeyModels = Collections.unmodifiableMap(pgeyReadIndex(this.pgeyRoot));
    }

    public List<PgeyRealisticModel> pgeyModels() {
        return List.copyOf(pgeyModels.values());
    }

    public Optional<PgeyRealisticModel> pgeyFind(String pgeyKey) {
        return Optional.ofNullable(pgeyKey == null ? null : pgeyModels.get(pgeyKey));
    }

    public PgeyRealisticModel pgeyRequire(String pgeyKey) {
        return pgeyFind(pgeyKey).orElseThrow(() -> new IllegalArgumentException("No existe el modelo " + pgeyKey));
    }

    public Optional<PgeyRealisticModel> pgeyOtherView(PgeyRealisticModel pgeyModel) {
        return pgeyFind(pgeyModel.pgeyOtherViewKey());
    }

    public PgeyModelLayers pgeyLayers(String pgeyKey) {
        PgeyRealisticModel pgeyModel = pgeyRequire(pgeyKey);
        PgeyModelLayers pgeyCached = pgeyLayers.get(pgeyKey);
        if (pgeyCached != null) {
            return pgeyCached;
        }
        PgeyModelLayers pgeyLoaded = pgeyLoad(pgeyModel);
        PgeyModelLayers pgeyPrevious = pgeyLayers.putIfAbsent(pgeyKey, pgeyLoaded);
        return pgeyPrevious != null ? pgeyPrevious : pgeyLoaded;
    }

    private PgeyModelLayers pgeyLoad(PgeyRealisticModel pgeyModel) {
        PgeyArgbImage pgeyBase = pgeyImage(pgeyModel, "base.jpg");
        Map<String, PgeyMask> pgeyZones = new HashMap<>();
        for (String pgeyZone : pgeyModel.pgeyZones()) {
            pgeyZones.put(pgeyZone, PgeyMask.pgeyFromAlpha(pgeyImage(pgeyModel, "zona_" + pgeyZone + ".png")));
        }
        PgeyMask[] pgeyDetail = PgeyModelLayers.pgeyDetail(pgeyImage(pgeyModel, "detalle.png"));
        return new PgeyModelLayers(pgeyBase, pgeyZones, pgeyDetail[0], pgeyDetail[1]);
    }

    private PgeyArgbImage pgeyImage(PgeyRealisticModel pgeyModel, String pgeyFile) {
        String pgeyPath = pgeyRoot + pgeyModel.pgeyKey() + "/" + pgeyFile;
        try (InputStream pgeyIn = PgeyRealisticModelRepository.class.getResourceAsStream(pgeyPath)) {
            if (pgeyIn == null) {
                throw new IllegalStateException("Falta " + pgeyPath);
            }
            PgeyArgbImage pgeyImg = pgeyDecoder.pgeyDecode(pgeyIn);
            if (pgeyImg.pgeyWidth() != pgeyModel.pgeyWidth() || pgeyImg.pgeyHeight() != pgeyModel.pgeyHeight()) {
                throw new IllegalStateException(pgeyPath + " mide " + pgeyImg.pgeyWidth() + " × " + pgeyImg.pgeyHeight()
                        + " y el modelo " + pgeyModel.pgeyWidth() + " × " + pgeyModel.pgeyHeight());
            }
            return pgeyImg;
        } catch (IOException pgeyE) {
            throw new UncheckedIOException("No se pudo leer " + pgeyPath, pgeyE);
        }
    }

    private static Map<String, PgeyRealisticModel> pgeyReadIndex(String pgeyRoot) {
        String pgeyPath = pgeyRoot + "modelos.json";
        try (InputStream pgeyIn = PgeyRealisticModelRepository.class.getResourceAsStream(pgeyPath)) {
            if (pgeyIn == null) {
                throw new IllegalStateException("Falta " + pgeyPath);
            }
            JsonNode pgeyJson = new ObjectMapper().readTree(pgeyIn);
            Map<String, PgeyRealisticModel> pgeyOut = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> pgeyE : pgeyJson.properties()) {
                pgeyOut.put(pgeyE.getKey(), pgeyParse(pgeyE.getKey(), pgeyE.getValue()));
            }
            return pgeyOut;
        } catch (IOException pgeyE) {
            throw new UncheckedIOException("No se pudo leer " + pgeyPath, pgeyE);
        }
    }

    static PgeyRealisticModel pgeyParse(String pgeyKey, JsonNode pgeyNode) {
        PgeyRealisticModel.PgeyView pgeyView = PgeyRealisticModel.PgeyView.valueOf(pgeyNode.path("vista").asText("").toUpperCase(Locale.ROOT));
        List<String> pgeyZones = new ArrayList<>();
        pgeyNode.path("zonas").forEach(pgeyZ -> pgeyZones.add(pgeyZ.asText()));
        JsonNode pgeyG = pgeyNode.path("geometriaCamiseta");
        PgeyRealisticModel.PgeyShirtGeometry pgeyShirt = pgeyG.has("centroX") && pgeyG.has("cuelloY") && pgeyG.has("bastaY")
                ? new PgeyRealisticModel.PgeyShirtGeometry(pgeyG.get("centroX").asInt(), pgeyG.get("cuelloY").asInt(), pgeyG.get("bastaY").asInt())
                : null;
        JsonNode pgeySize = pgeyNode.path("tamano");
        return new PgeyRealisticModel(pgeyKey, pgeyView, pgeyZones, pgeyShirt, pgeySize.path(0).asInt(720), pgeySize.path(1).asInt(900));
    }
}
