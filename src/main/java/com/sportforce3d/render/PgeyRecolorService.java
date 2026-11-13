package com.sportforce3d.render;

import com.sportforce3d.app.PpqhAsync;
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

public final class PgeyRecolorService {
    public static final long PGEY_DEFAULT_CACHE_BYTES = 96L * 1024 * 1024;

    private final PgeyRealisticModelRepository pgeyModels;
    private final PgeyLruCache<String, PgeyArgbImage> pgeyCache;

    public PgeyRecolorService(PgeyRealisticModelRepository pgeyModels) {
        this(pgeyModels, PGEY_DEFAULT_CACHE_BYTES);
    }

    public PgeyRecolorService(PgeyRealisticModelRepository pgeyModels, long pgeyCacheBytes) {
        this.pgeyModels = pgeyModels;
        this.pgeyCache = new PgeyLruCache<>(pgeyCacheBytes, PgeyArgbImage::pgeyBytes);
    }

    public PgeyRealisticModelRepository pgeyModels() {
        return pgeyModels;
    }

    public PgeyArgbImage pgeyRenderPixels(PgeyRecolorRequest pgeyReq) {
        String pgeyKey = pgeyReq.pgeyCacheKey();
        PgeyArgbImage pgeyCached = pgeyCache.pgeyGet(pgeyKey);
        if (pgeyCached != null) {
            return pgeyCached;
        }
        PgeyArgbImage pgeyOut = pgeyCompose(pgeyReq);
        pgeyCache.pgeyPut(pgeyKey, pgeyOut);
        return pgeyOut;
    }

    private PgeyArgbImage pgeyCompose(PgeyRecolorRequest pgeyReq) {
        PgeyRealisticModel pgeyModel = pgeyModels.pgeyRequire(pgeyReq.pgeyModelKey());
        PgeyModelLayers pgeyLayers = pgeyModels.pgeyLayers(pgeyModel.pgeyKey());
        Map<String, Integer> pgeyColors = new HashMap<>();
        pgeyReq.pgeyZoneColors().forEach((pgeyZone, pgeyHex) -> pgeyColors.put(pgeyZone, PgeyRecolorRequest.pgeyRgb(pgeyHex)));
        return PgeyRecolor.pgeyCompose(pgeyLayers, pgeyColors, PgeyDesignOverlay.pgeyDraw(pgeyModel, pgeyReq));
    }

    public PgeyArgbImage pgeyThumbnailPixels(PgeyRecolorRequest pgeyReq, int pgeyWidth) {
        String pgeyKey = pgeyReq.pgeyCacheKey() + "@" + pgeyWidth;
        PgeyArgbImage pgeyCached = pgeyCache.pgeyGet(pgeyKey);
        if (pgeyCached != null) {
            return pgeyCached;
        }
        PgeyArgbImage pgeyFull = pgeyCache.pgeyGet(pgeyReq.pgeyCacheKey());
        if (pgeyFull == null) {
            pgeyFull = pgeyCompose(pgeyReq);
        }
        int pgeyW = Math.min(pgeyWidth, pgeyFull.pgeyWidth());
        int pgeyH = Math.max(1, (int) Math.round((double) pgeyFull.pgeyHeight() * pgeyW / pgeyFull.pgeyWidth()));
        PgeyArgbImage pgeyOut = pgeyW == pgeyFull.pgeyWidth() ? pgeyFull : PgeyRecolor.pgeyDownscale(pgeyFull, pgeyW, pgeyH);
        pgeyCache.pgeyPut(pgeyKey, pgeyOut);
        return pgeyOut;
    }

    public Image pgeyRender(PgeyRecolorRequest pgeyReq) {
        return pgeyToImage(pgeyRenderPixels(pgeyReq));
    }

    public Image pgeyThumbnail(PgeyRecolorRequest pgeyReq, int pgeyWidth) {
        return pgeyToImage(pgeyThumbnailPixels(pgeyReq, pgeyWidth));
    }

    public Task<Image> pgeyRenderAsync(PgeyRecolorRequest pgeyReq, Consumer<Image> pgeyOnSuccess, Consumer<Throwable> pgeyOnError) {
        return PpqhAsync.ppqhRun(() -> pgeyRender(pgeyReq), pgeyOnSuccess, pgeyOnError);
    }

    public Task<Image> pgeyThumbnailAsync(PgeyRecolorRequest pgeyReq, int pgeyWidth, Consumer<Image> pgeyOnSuccess, Consumer<Throwable> pgeyOnError) {
        return PpqhAsync.ppqhRun(() -> pgeyThumbnail(pgeyReq, pgeyWidth), pgeyOnSuccess, pgeyOnError);
    }

    public Image pgeyCachedThumbnail(PgeyRecolorRequest pgeyReq, int pgeyWidth) {
        PgeyArgbImage pgeyCached = pgeyCache.pgeyGet(pgeyReq.pgeyCacheKey() + "@" + pgeyWidth);
        return pgeyCached == null ? null : pgeyToImage(pgeyCached);
    }

    public boolean pgeyIsCached(PgeyRecolorRequest pgeyReq) {
        return pgeyCache.pgeyContainsKey(pgeyReq.pgeyCacheKey());
    }

    public static void pgeyWritePng(PgeyArgbImage pgeyImg, Path pgeyFile) throws IOException {
        BufferedImage pgeyOut = new BufferedImage(pgeyImg.pgeyWidth(), pgeyImg.pgeyHeight(), BufferedImage.TYPE_INT_ARGB);
        pgeyOut.setRGB(0, 0, pgeyImg.pgeyWidth(), pgeyImg.pgeyHeight(), pgeyImg.pgeyPixels(), 0, pgeyImg.pgeyWidth());
        Path pgeyParent = pgeyFile.toAbsolutePath().getParent();
        if (pgeyParent != null) {
            Files.createDirectories(pgeyParent);
        }
        if (!ImageIO.write(pgeyOut, "png", pgeyFile.toFile())) {
            throw new IOException("No hay escritor PNG disponible");
        }
    }

    public static WritableImage pgeyToImage(PgeyArgbImage pgeyImg) {
        WritableImage pgeyOut = new WritableImage(pgeyImg.pgeyWidth(), pgeyImg.pgeyHeight());
        pgeyOut.getPixelWriter().setPixels(0, 0, pgeyImg.pgeyWidth(), pgeyImg.pgeyHeight(), PixelFormat.getIntArgbInstance(),
                pgeyImg.pgeyPixels(), 0, pgeyImg.pgeyWidth());
        return pgeyOut;
    }
}
