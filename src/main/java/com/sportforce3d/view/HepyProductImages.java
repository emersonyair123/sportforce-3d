package com.sportforce3d.view;

import com.sportforce3d.model.HepyRealisticLook;
import com.sportforce3d.render.PgeyRecolorRequest;
import com.sportforce3d.render.PgeyRecolorService;
import com.sportforce3d.storage.HepyStorageService;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

final class HepyProductImages {

    private static final Logger hepyLog = LoggerFactory.getLogger(HepyProductImages.class);
    private static final int HEPY_MAX_ENTRIES = 120;
    static final int HEPY_REALISTIC_THUMB_WIDTH = 520;
    private static final Map<String, Image> HEPY_CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> hepyEldest) {
            return size() > HEPY_MAX_ENTRIES;
        }
    };

    private HepyProductImages() {
    }

    static Image hepyLoad(HepyStorageService hepyStorage, String hepyRelativePath, double hepyWidth, double hepyHeight) {
        if (hepyRelativePath == null) {
            return null;
        }
        String hepyKey = hepyRelativePath + "@" + (int) hepyWidth + "x" + (int) hepyHeight;
        synchronized (HEPY_CACHE) {
            Image hepyCached = HEPY_CACHE.get(hepyKey);
            if (hepyCached != null && !hepyCached.isError()) {
                return hepyCached;
            }
        }
        Path hepyFile;
        try {
            hepyFile = hepyStorage.hepyResolve(hepyRelativePath);
        } catch (IllegalArgumentException hepyE) {
            return null;
        }
        if (!Files.isRegularFile(hepyFile)) {
            return null;
        }
        Image hepyImage = new Image(hepyFile.toUri().toString(), hepyWidth * 2, hepyHeight * 2, true, true, true);
        synchronized (HEPY_CACHE) {
            HEPY_CACHE.put(hepyKey, hepyImage);
        }
        return hepyImage;
    }

    static void hepyRealistic(PgeyRecolorService hepyRecolor, HepyRealisticLook hepyLook, Consumer<Image> hepyOnImage) {
        PgeyRecolorRequest hepyReq;
        try {
            hepyReq = PgeyRecolorRequest.pgeyOf(hepyLook);
        } catch (IllegalArgumentException hepyE) {
            hepyLog.warn("Vista realista inválida ({}): {}", hepyLook.hepyModelKey(), hepyE.getMessage());
            hepyOnImage.accept(null);
            return;
        }
        Image hepyCached = hepyRecolor.pgeyCachedThumbnail(hepyReq, HEPY_REALISTIC_THUMB_WIDTH);
        if (hepyCached != null) {
            hepyOnImage.accept(hepyCached);
            return;
        }
        hepyRecolor.pgeyThumbnailAsync(hepyReq, HEPY_REALISTIC_THUMB_WIDTH, hepyOnImage, hepyE -> {
            hepyLog.warn("No se pudo recolorear {}: {}", hepyLook.hepyModelKey(), hepyE.getMessage());
            hepyOnImage.accept(null);
        });
    }

    static javafx.scene.image.ImageView hepyDesignCapture(HepyStorageService hepyStorage, String hepyRelativePath, double hepyW, double hepyH) {
        Image hepyImage = hepyLoad(hepyStorage, hepyRelativePath, hepyW * 2, hepyH * 2);
        if (hepyImage == null) {
            return null;
        }
        javafx.scene.image.ImageView hepyView = new javafx.scene.image.ImageView(hepyImage);
        hepyView.setFitWidth(hepyW);
        hepyView.setFitHeight(hepyH);
        hepyView.setSmooth(true);
        Runnable hepyCrop = () -> {
            double hepyIw = hepyImage.getWidth();
            double hepyIh = hepyImage.getHeight();
            double hepyCh = hepyIh * 0.92;
            double hepyCw = hepyCh * hepyW / hepyH;
            if (hepyCw > hepyIw) {
                hepyCw = hepyIw;
                hepyCh = hepyCw * hepyH / hepyW;
            }
            hepyView.setViewport(new javafx.geometry.Rectangle2D((hepyIw - hepyCw) / 2, (hepyIh - hepyCh) / 2, hepyCw, hepyCh));
        };
        if (hepyImage.getProgress() >= 1 && !hepyImage.isError()) {
            hepyCrop.run();
        } else {
            hepyImage.progressProperty().addListener((hepyObs, hepyOld, hepyP) -> {
                if (hepyP.doubleValue() >= 1 && !hepyImage.isError()) {
                    hepyCrop.run();
                }
            });
        }
        return hepyView;
    }
}
