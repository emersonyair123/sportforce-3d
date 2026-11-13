package com.sportforce3d.view;

import com.sportforce3d.model.RealisticLook;
import com.sportforce3d.render.RecolorRequest;
import com.sportforce3d.render.RecolorService;
import com.sportforce3d.storage.StorageService;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

final class ProductImages {

    private static final Logger log = LoggerFactory.getLogger(ProductImages.class);
    private static final int MAX_ENTRIES = 120;
    static final int REALISTIC_THUMB_WIDTH = 520;
    private static final Map<String, Image> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private ProductImages() {
    }

    static Image load(StorageService storage, String relativePath, double width, double height) {
        if (relativePath == null) {
            return null;
        }
        String key = relativePath + "@" + (int) width + "x" + (int) height;
        synchronized (CACHE) {
            Image cached = CACHE.get(key);
            if (cached != null && !cached.isError()) {
                return cached;
            }
        }
        Path file;
        try {
            file = storage.resolve(relativePath);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!Files.isRegularFile(file)) {
            return null;
        }
        Image image = new Image(file.toUri().toString(), width * 2, height * 2, true, true, true);
        synchronized (CACHE) {
            CACHE.put(key, image);
        }
        return image;
    }

    static void realistic(RecolorService recolor, RealisticLook look, Consumer<Image> onImage) {
        RecolorRequest req;
        try {
            req = RecolorRequest.of(look);
        } catch (IllegalArgumentException e) {
            log.warn("Vista realista inválida ({}): {}", look.modelKey(), e.getMessage());
            onImage.accept(null);
            return;
        }
        Image cached = recolor.cachedThumbnail(req, REALISTIC_THUMB_WIDTH);
        if (cached != null) {
            onImage.accept(cached);
            return;
        }
        recolor.thumbnailAsync(req, REALISTIC_THUMB_WIDTH, onImage, e -> {
            log.warn("No se pudo recolorear {}: {}", look.modelKey(), e.getMessage());
            onImage.accept(null);
        });
    }

    static javafx.scene.image.ImageView designCapture(StorageService storage, String relativePath, double w, double h) {
        Image image = load(storage, relativePath, w * 2, h * 2);
        if (image == null) {
            return null;
        }
        javafx.scene.image.ImageView view = new javafx.scene.image.ImageView(image);
        view.setFitWidth(w);
        view.setFitHeight(h);
        view.setSmooth(true);
        Runnable crop = () -> {
            double iw = image.getWidth();
            double ih = image.getHeight();
            double ch = ih * 0.92;
            double cw = ch * w / h;
            if (cw > iw) {
                cw = iw;
                ch = cw * h / w;
            }
            view.setViewport(new javafx.geometry.Rectangle2D((iw - cw) / 2, (ih - ch) / 2, cw, ch));
        };
        if (image.getProgress() >= 1 && !image.isError()) {
            crop.run();
        } else {
            image.progressProperty().addListener((obs, old, p) -> {
                if (p.doubleValue() >= 1 && !image.isError()) {
                    crop.run();
                }
            });
        }
        return view;
    }
}
