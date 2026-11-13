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

public final class RealisticModelRepository {

    public static final String ROOT = "/models/realista/";

    @FunctionalInterface
    public interface ImageDecoder {
        ArgbImage decode(InputStream in) throws IOException;
    }

    public static final ImageDecoder JAVAFX = in -> {
        Image img = new Image(in);
        if (img.isError() || img.getPixelReader() == null) {
            throw new IOException("Imagen inválida", img.getException());
        }
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        int[] px = new int[w * h];
        img.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
        return new ArgbImage(w, h, px);
    };

    private final String root;
    private final ImageDecoder decoder;
    private final Map<String, RealisticModel> models;
    private final Map<String, ModelLayers> layers = new ConcurrentHashMap<>();

    public RealisticModelRepository() {
        this(ROOT, JAVAFX);
    }

    public RealisticModelRepository(String root, ImageDecoder decoder) {
        this.root = root.endsWith("/") ? root : root + "/";
        this.decoder = decoder;
        this.models = Collections.unmodifiableMap(readIndex(this.root));
    }

    public List<RealisticModel> models() {
        return List.copyOf(models.values());
    }

    public Optional<RealisticModel> find(String key) {
        return Optional.ofNullable(key == null ? null : models.get(key));
    }

    public RealisticModel require(String key) {
        return find(key).orElseThrow(() -> new IllegalArgumentException("No existe el modelo " + key));
    }

    public Optional<RealisticModel> otherView(RealisticModel model) {
        return find(model.otherViewKey());
    }

    public ModelLayers layers(String key) {
        RealisticModel model = require(key);
        ModelLayers cached = layers.get(key);
        if (cached != null) {
            return cached;
        }
        ModelLayers loaded = load(model);
        ModelLayers previous = layers.putIfAbsent(key, loaded);
        return previous != null ? previous : loaded;
    }

    private ModelLayers load(RealisticModel model) {
        ArgbImage base = image(model, "base.jpg");
        Map<String, Mask> zones = new HashMap<>();
        for (String zone : model.zones()) {
            zones.put(zone, Mask.fromAlpha(image(model, "zona_" + zone + ".png")));
        }
        Mask[] detail = ModelLayers.detail(image(model, "detalle.png"));
        return new ModelLayers(base, zones, detail[0], detail[1]);
    }

    private ArgbImage image(RealisticModel model, String file) {
        String path = root + model.key() + "/" + file;
        try (InputStream in = RealisticModelRepository.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Falta " + path);
            }
            ArgbImage img = decoder.decode(in);
            if (img.width() != model.width() || img.height() != model.height()) {
                throw new IllegalStateException(path + " mide " + img.width() + " × " + img.height()
                        + " y el modelo " + model.width() + " × " + model.height());
            }
            return img;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + path, e);
        }
    }

    private static Map<String, RealisticModel> readIndex(String root) {
        String path = root + "modelos.json";
        try (InputStream in = RealisticModelRepository.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Falta " + path);
            }
            JsonNode json = new ObjectMapper().readTree(in);
            Map<String, RealisticModel> out = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> e : json.properties()) {
                out.put(e.getKey(), parse(e.getKey(), e.getValue()));
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + path, e);
        }
    }

    static RealisticModel parse(String key, JsonNode node) {
        RealisticModel.View view = RealisticModel.View.valueOf(node.path("vista").asText("").toUpperCase(Locale.ROOT));
        List<String> zones = new ArrayList<>();
        node.path("zonas").forEach(z -> zones.add(z.asText()));
        JsonNode g = node.path("geometriaCamiseta");
        RealisticModel.ShirtGeometry shirt = g.has("centroX") && g.has("cuelloY") && g.has("bastaY")
                ? new RealisticModel.ShirtGeometry(g.get("centroX").asInt(), g.get("cuelloY").asInt(), g.get("bastaY").asInt())
                : null;
        JsonNode size = node.path("tamano");
        return new RealisticModel(key, view, zones, shirt, size.path(0).asInt(720), size.path(1).asInt(900));
    }
}
