package com.sportforce3d.three;

import com.sportforce3d.design.EmblemCatalog;
import com.sportforce3d.storage.StorageService;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Imágenes de emblemas (SVG de la biblioteca, recoloreados) y logos subidos (PNG o SVG), con caché.
 * Se usa en el hilo de JavaFX (los SVG se dibujan con snapshot).
 */
public class ElementImages {

    private static final Logger log = LoggerFactory.getLogger(ElementImages.class);
    private static final int RESOLUTION = 640;

    private final StorageService storage;
    private final Map<String, Image> cache = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > 48;
        }
    };
    private final Map<String, SvgLoader.SvgDoc> svgs = new LinkedHashMap<>();

    public ElementImages(StorageService storage) {
        this.storage = storage;
    }

    /** Emblema con sus dos colores reemplazados (null = colores originales). */
    public Image emblem(String key, String primary, String accent) {
        String id = "e|" + key + "|" + primary + "|" + accent;
        Image cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        SvgLoader.SvgDoc doc = svgs.computeIfAbsent(key, this::loadEmblem);
        Map<String, String> recolor = new java.util.HashMap<>();
        if (primary != null) {
            recolor.put(EmblemCatalog.SOURCE_PRIMARY, primary);
        }
        if (accent != null) {
            recolor.put(EmblemCatalog.SOURCE_ACCENT, accent);
        }
        Image img = SvgLoader.render(doc, RESOLUTION, recolor);
        cache.put(id, img);
        return img;
    }

    /** Logo subido por el cliente; null si el archivo ya no existe o no se puede leer. */
    public Image logo(String storedPath) {
        if (storedPath == null || storage == null) {
            return null;
        }
        String id = "l|" + storedPath;
        if (cache.containsKey(id)) {
            return cache.get(id);
        }
        Image img = null;
        try {
            Path file = storage.resolve(storedPath);
            if (Files.isRegularFile(file)) {
                if (storedPath.endsWith(".svg")) {
                    try (InputStream in = Files.newInputStream(file)) {
                        img = SvgLoader.render(SvgLoader.parse(in), RESOLUTION, Map.of());
                    }
                } else {
                    Image loaded = new Image(file.toUri().toString(), RESOLUTION, RESOLUTION, true, true, false);
                    img = loaded.isError() ? null : loaded;
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo {}", storedPath, e);
        }
        cache.put(id, img);
        return img;
    }

    private SvgLoader.SvgDoc loadEmblem(String key) {
        try (InputStream in = ElementImages.class.getResourceAsStream(EmblemCatalog.resource(key))) {
            if (in == null) {
                throw new IllegalArgumentException("Emblema no encontrado: " + key);
            }
            return SvgLoader.parse(in);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
