package com.sportforce3d.render;

import com.sportforce3d.app.Async;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Recolorea las fotos de {@link RealisticModelRepository} con la fórmula de {@link Recolor}, más diseños y textos
 * ({@link DesignOverlay}). Guarda los resultados en una caché LRU por (modelo + colores + diseño + texto) y
 * genera miniaturas para las tarjetas.
 * <p>
 * Los métodos {@code render…}/{@code thumbnail…} sincrónicos se llaman en segundo plano; los {@code …Async} usan
 * un {@link Task} ({@link Async#run}) y entregan la imagen en el hilo de JavaFX.
 */
public final class RecolorService {

    /** Memoria de la caché de imágenes recoloreadas (≈ 2.6 MB por foto completa de 720 × 900). */
    public static final long DEFAULT_CACHE_BYTES = 96L * 1024 * 1024;

    private final RealisticModelRepository models;
    private final LruCache<String, ArgbImage> cache;

    public RecolorService(RealisticModelRepository models) {
        this(models, DEFAULT_CACHE_BYTES);
    }

    public RecolorService(RealisticModelRepository models, long cacheBytes) {
        this.models = models;
        this.cache = new LruCache<>(cacheBytes, ArgbImage::bytes);
    }

    public RealisticModelRepository models() {
        return models;
    }

    /** Foto recoloreada en tamaño completo (cualquier hilo; usa la caché). */
    public ArgbImage renderPixels(RecolorRequest req) {
        String key = req.cacheKey();
        ArgbImage cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        ArgbImage out = compose(req);
        cache.put(key, out);
        return out;
    }

    private ArgbImage compose(RecolorRequest req) {
        RealisticModel model = models.require(req.modelKey());
        ModelLayers layers = models.layers(model.key());
        Map<String, Integer> colors = new HashMap<>();
        req.zoneColors().forEach((zone, hex) -> colors.put(zone, RecolorRequest.rgb(hex)));
        return Recolor.compose(layers, colors, DesignOverlay.draw(model, req));
    }

    /** Miniatura de {@code width} px de ancho con la proporción de la foto (cualquier hilo; usa la caché). */
    public ArgbImage thumbnailPixels(RecolorRequest req, int width) {
        String key = req.cacheKey() + "@" + width;
        ArgbImage cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        // Si la foto completa no está en la caché, se calcula sin guardarla: las tarjetas solo necesitan la miniatura.
        ArgbImage full = cache.get(req.cacheKey());
        if (full == null) {
            full = compose(req);
        }
        int w = Math.min(width, full.width());
        int h = Math.max(1, (int) Math.round((double) full.height() * w / full.width()));
        ArgbImage out = w == full.width() ? full : Recolor.downscale(full, w, h);
        cache.put(key, out);
        return out;
    }

    public Image render(RecolorRequest req) {
        return toImage(renderPixels(req));
    }

    public Image thumbnail(RecolorRequest req, int width) {
        return toImage(thumbnailPixels(req, width));
    }

    public Task<Image> renderAsync(RecolorRequest req, Consumer<Image> onSuccess, Consumer<Throwable> onError) {
        return Async.run(() -> render(req), onSuccess, onError);
    }

    public Task<Image> thumbnailAsync(RecolorRequest req, int width, Consumer<Image> onSuccess, Consumer<Throwable> onError) {
        return Async.run(() -> thumbnail(req, width), onSuccess, onError);
    }

    /** La miniatura si ya está en la caché (para mostrarla sin esperar a un Task); si no, null. */
    public Image cachedThumbnail(RecolorRequest req, int width) {
        ArgbImage cached = cache.get(req.cacheKey() + "@" + width);
        return cached == null ? null : toImage(cached);
    }

    /** ¿Ya está en la caché? (para no mostrar un indicador de carga si la imagen sale al instante). */
    public boolean isCached(RecolorRequest req) {
        return cache.containsKey(req.cacheKey());
    }

    /** Guarda la imagen como PNG (Java2D, cualquier hilo). */
    public static void writePng(ArgbImage img, Path file) throws IOException {
        BufferedImage out = new BufferedImage(img.width(), img.height(), BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, img.width(), img.height(), img.pixels(), 0, img.width());
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!ImageIO.write(out, "png", file.toFile())) {
            throw new IOException("No hay escritor PNG disponible");
        }
    }

    /** Copia los píxeles a una imagen de JavaFX nueva (cada llamada devuelve una imagen propia). */
    public static WritableImage toImage(ArgbImage img) {
        WritableImage out = new WritableImage(img.width(), img.height());
        out.getPixelWriter().setPixels(0, 0, img.width(), img.height(), PixelFormat.getIntArgbInstance(),
                img.pixels(), 0, img.width());
        return out;
    }
}
