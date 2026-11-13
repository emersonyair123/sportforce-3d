package com.sportforce3d.three;

import com.sportforce3d.design.PgeyEmblemCatalog;
import com.sportforce3d.storage.HepyStorageService;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class PgeyElementImages {

    private static final Logger pgeyLog = LoggerFactory.getLogger(PgeyElementImages.class);
    private static final int PGEY_RESOLUTION = 640;

    private final HepyStorageService pgeyStorage;
    private final Map<String, Image> pgeyCache = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> pgeyEldest) {
            return size() > 48;
        }
    };
    private final Map<String, PgeySvgLoader.PgeySvgDoc> pgeySvgs = new LinkedHashMap<>();

    public PgeyElementImages(HepyStorageService pgeyStorage) {
        this.pgeyStorage = pgeyStorage;
    }

    public Image pgeyEmblem(String pgeyKey, String pgeyPrimary, String pgeyAccent) {
        String pgeyId = "e|" + pgeyKey + "|" + pgeyPrimary + "|" + pgeyAccent;
        Image pgeyCached = pgeyCache.get(pgeyId);
        if (pgeyCached != null) {
            return pgeyCached;
        }
        PgeySvgLoader.PgeySvgDoc pgeyDoc = pgeySvgs.computeIfAbsent(pgeyKey, this::pgeyLoadEmblem);
        Map<String, String> pgeyRecolor = new java.util.HashMap<>();
        if (pgeyPrimary != null) {
            pgeyRecolor.put(PgeyEmblemCatalog.PGEY_SOURCE_PRIMARY, pgeyPrimary);
        }
        if (pgeyAccent != null) {
            pgeyRecolor.put(PgeyEmblemCatalog.PGEY_SOURCE_ACCENT, pgeyAccent);
        }
        Image pgeyImg = PgeySvgLoader.pgeyRender(pgeyDoc, PGEY_RESOLUTION, pgeyRecolor);
        pgeyCache.put(pgeyId, pgeyImg);
        return pgeyImg;
    }

    public Image pgeyLogo(String pgeyStoredPath) {
        if (pgeyStoredPath == null || pgeyStorage == null) {
            return null;
        }
        String pgeyId = "l|" + pgeyStoredPath;
        if (pgeyCache.containsKey(pgeyId)) {
            return pgeyCache.get(pgeyId);
        }
        Image pgeyImg = null;
        try {
            Path pgeyFile = pgeyStorage.hepyResolve(pgeyStoredPath);
            if (Files.isRegularFile(pgeyFile)) {
                if (pgeyStoredPath.endsWith(".svg")) {
                    try (InputStream pgeyIn = Files.newInputStream(pgeyFile)) {
                        pgeyImg = PgeySvgLoader.pgeyRender(PgeySvgLoader.pgeyParse(pgeyIn), PGEY_RESOLUTION, Map.of());
                    }
                } else {
                    Image pgeyLoaded = new Image(pgeyFile.toUri().toString(), PGEY_RESOLUTION, PGEY_RESOLUTION, true, true, false);
                    pgeyImg = pgeyLoaded.isError() ? null : pgeyLoaded;
                }
            }
        } catch (Exception pgeyE) {
            pgeyLog.warn("No se pudo cargar el logo {}", pgeyStoredPath, pgeyE);
        }
        pgeyCache.put(pgeyId, pgeyImg);
        return pgeyImg;
    }

    private PgeySvgLoader.PgeySvgDoc pgeyLoadEmblem(String pgeyKey) {
        try (InputStream pgeyIn = PgeyElementImages.class.getResourceAsStream(PgeyEmblemCatalog.pgeyResource(pgeyKey))) {
            if (pgeyIn == null) {
                throw new IllegalArgumentException("Emblema no encontrado: " + pgeyKey);
            }
            return PgeySvgLoader.pgeyParse(pgeyIn);
        } catch (java.io.IOException pgeyE) {
            throw new java.io.UncheckedIOException(pgeyE);
        }
    }
}
