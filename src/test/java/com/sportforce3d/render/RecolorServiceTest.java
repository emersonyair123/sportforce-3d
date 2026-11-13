package com.sportforce3d.render;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecolorServiceTest {

    static final PgeyRealisticModelRepository.PgeyImageDecoder IMAGE_IO = in -> {
        BufferedImage img = ImageIO.read(in);
        if (img == null) {
            throw new IOException("Formato no reconocido");
        }
        int w = img.getWidth();
        int h = img.getHeight();
        return new PgeyArgbImage(w, h, img.getRGB(0, 0, w, h, null, 0, w));
    };

    private static PgeyRealisticModelRepository repo;

    @BeforeAll
    static void load() {
        repo = new PgeyRealisticModelRepository(PgeyRealisticModelRepository.PGEY_ROOT, IMAGE_IO);
    }

    @Test
    void readsAllModelsAndTheirLayers() {
        assertThat(repo.pgeyModels()).as("34 + 9 de la Etapa 12B").hasSize(43);
        for (PgeyRealisticModel m : repo.pgeyModels()) {
            assertThat(m.pgeyWidth()).isEqualTo(720);
            assertThat(m.pgeyHeight()).isEqualTo(900);
            PgeyModelLayers layers = repo.pgeyLayers(m.pgeyKey());
            for (String zone : m.pgeyZones()) {
                assertThat(layers.pgeyZone(zone).pgeyIsEmpty()).as(m.pgeyKey() + " " + zone).isFalse();
            }
            assertThat(layers.pgeyDetailAlpha().pgeyIsEmpty()).as(m.pgeyKey() + " detalle").isFalse();
            boolean garment = m.pgeyCut().equals("buzo") || m.pgeyCut().equals("casaca") || m.pgeyView() == PgeyRealisticModel.PgeyView.PRODUCTO;
            assertThat(m.pgeyShirt() == null).as(m.pgeyKey() + " geometría").isEqualTo(garment);
        }
        assertThat(repo.pgeyLayers("futbol_hombre_frente")).isSameAs(repo.pgeyLayers("futbol_hombre_frente"));
    }

    @Test
    void modelKeysDescribeCutAudienceAndOtherView() {
        PgeyRealisticModel m = repo.pgeyRequire("futbol_mujer_frente");
        assertThat(m.pgeyCut()).isEqualTo("futbol");
        assertThat(m.pgeyAudience()).isEqualTo("mujer");
        assertThat(repo.pgeyOtherView(m)).map(PgeyRealisticModel::pgeyKey).contains("futbol_mujer_espalda");
        assertThat(repo.pgeyOtherView(repo.pgeyRequire("buzo_hombre_frente"))).isEmpty();
        assertThat(repo.pgeyRequire("producto_medias").pgeyAudience()).isNull();
        PgeyRealisticModel.PgeyShirtGeometry g = repo.pgeyRequire("futbol_hombre_frente").pgeyShirt();
        assertThat(g).isEqualTo(new PgeyRealisticModel.PgeyShirtGeometry(355, 131, 434));
        assertThatThrownBy(() -> repo.pgeyRequire("no_existe")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cachesByModelColorsDesignAndText() {
        PgeyRecolorService service = new PgeyRecolorService(repo);
        PgeyRecolorRequest a = PgeyRecolorRequest.pgeyOf("futbol_hombre_frente", Map.of("camiseta", "#1F5FFF"));
        PgeyRecolorRequest same = PgeyRecolorRequest.pgeyOf("futbol_hombre_frente", Map.of("camiseta", "#1f5fff"));
        assertThat(service.pgeyRenderPixels(a)).isSameAs(service.pgeyRenderPixels(same));

        PgeyRecolorRequest numbered = new PgeyRecolorRequest("futbol_hombre_frente", Map.of("camiseta", "#1f5fff"),
                PgeyRealisticPattern.LISO, null, null, 10, null);
        assertThat(numbered.pgeyCacheKey()).isNotEqualTo(a.pgeyCacheKey());
        assertThat(service.pgeyRenderPixels(numbered)).isNotSameAs(service.pgeyRenderPixels(a));
    }

    @Test
    void lruEvictsTheLeastRecentlyUsed() {
        long oneImage = 720L * 900 * 4;
        PgeyRecolorService service = new PgeyRecolorService(repo, 2 * oneImage);
        PgeyRecolorRequest red = PgeyRecolorRequest.pgeyOf("voley_mujer_frente", Map.of("camiseta", "#d7141e"));
        PgeyRecolorRequest blue = PgeyRecolorRequest.pgeyOf("voley_mujer_frente", Map.of("camiseta", "#1f5fff"));
        PgeyRecolorRequest green = PgeyRecolorRequest.pgeyOf("voley_mujer_frente", Map.of("camiseta", "#39ff88"));
        service.pgeyRenderPixels(red);
        service.pgeyRenderPixels(blue);
        service.pgeyRenderPixels(red);
        service.pgeyRenderPixels(green);
        assertThat(service.pgeyIsCached(red)).isTrue();
        assertThat(service.pgeyIsCached(green)).isTrue();
        assertThat(service.pgeyIsCached(blue)).isFalse();
    }

    @Test
    void recolorsOnlyTheZoneAndKeepsTheRestOfThePhoto() {
        PgeyRecolorService service = new PgeyRecolorService(repo);
        PgeyModelLayers layers = repo.pgeyLayers("futbol_hombre_frente");
        PgeyArgbImage out = service.pgeyRenderPixels(PgeyRecolorRequest.pgeyOf("futbol_hombre_frente", Map.of("camiseta", "#d7141e")));
        PgeyMask cam = layers.pgeyZone("camiseta");
        PgeyMask detail = layers.pgeyDetailAlpha();
        int[] base = layers.pgeyBase().pgeyPixels();
        int changedOutside = 0;
        long redInside = 0;
        long inside = 0;
        for (int y = 0; y < out.pgeyHeight(); y++) {
            for (int x = 0; x < out.pgeyWidth(); x++) {
                int i = y * out.pgeyWidth() + x;
                if (cam.pgeyAt(x, y) == 0 && detail.pgeyAt(x, y) == 0 && out.pgeyPixels()[i] != base[i]) {
                    changedOutside++;
                }
                if (cam.pgeyAt(x, y) == 255) {
                    inside++;
                    int c = out.pgeyPixels()[i];
                    if (((c >> 16) & 0xFF) > 2 * ((c >> 8) & 0xFF)) {
                        redInside++;
                    }
                }
            }
        }
        assertThat(changedOutside).as("píxeles fuera de la prenda cambiados").isZero();
        assertThat(redInside).as("la camiseta se ve roja").isGreaterThan(inside * 9 / 10);
    }

    @Test
    void designsAndTextsStayInsideTheShirt() {
        PgeyRecolorService service = new PgeyRecolorService(repo);
        Map<String, String> colors = Map.of("camiseta", "#f2f5f8", "short", "#0b2a6b", "paneles", "#0b2a6b",
                "medias", "#0b2a6b");
        PgeyArgbImage plain = service.pgeyRenderPixels(PgeyRecolorRequest.pgeyOf("futbol_hombre_espalda", colors));
        PgeyMask cam = repo.pgeyLayers("futbol_hombre_espalda").pgeyZone("camiseta");
        for (PgeyRealisticPattern p : PgeyRealisticPattern.values()) {
            PgeyRecolorRequest req = new PgeyRecolorRequest("futbol_hombre_espalda", colors, p,
                    p == PgeyRealisticPattern.LISO ? null : "#d7141e", "Quispe", 10, null);
            PgeyArgbImage designed = service.pgeyRenderPixels(req);
            int changedOutside = 0;
            int changedInside = 0;
            for (int y = 0; y < plain.pgeyHeight(); y++) {
                for (int x = 0; x < plain.pgeyWidth(); x++) {
                    boolean diff = designed.pgeyArgb(x, y) != plain.pgeyArgb(x, y);
                    if (diff && cam.pgeyAt(x, y) == 0) {
                        changedOutside++;
                    } else if (diff) {
                        changedInside++;
                    }
                }
            }
            assertThat(changedOutside).as(p + " fuera de la camiseta").isZero();
            assertThat(changedInside).as(p + " dentro de la camiseta").isPositive();
        }
    }

    @Test
    void thumbnailsKeepTheAspectRatio() {
        PgeyRecolorService service = new PgeyRecolorService(repo);
        PgeyRecolorRequest req = PgeyRecolorRequest.pgeyOf("producto_zapatilla_fg", Map.of("principal", "#1f5fff"));
        PgeyArgbImage thumb = service.pgeyThumbnailPixels(req, 240);
        assertThat(thumb.pgeyWidth()).isEqualTo(240);
        assertThat(thumb.pgeyHeight()).isEqualTo(300);
        assertThat(service.pgeyThumbnailPixels(req, 240)).isSameAs(thumb);
    }

    @Test
    void rendersAFullPhotoInLessThan50Milliseconds() {
        PgeyRecolorService service = new PgeyRecolorService(repo, 0);
        String key = "futbol_hombre_espalda";
        repo.pgeyLayers(key);
        long[] times = new long[25];
        for (int run = -10; run < times.length; run++) {
            String shirt = String.format("#%06x", 0x102030 + Math.abs(run) * 0x010101 + (run < 0 ? 0x400000 : 0));
            Map<String, String> colors = new LinkedHashMap<>();
            colors.put("camiseta", shirt);
            colors.put("short", "#0b2a6b");
            colors.put("paneles", "#39ff88");
            colors.put("medias", "#0b2a6b");
            PgeyRecolorRequest req = new PgeyRecolorRequest(key, colors, PgeyRealisticPattern.RAYAS, "#0b2a6b", "ROJAS", 7, null);
            long t0 = System.nanoTime();
            service.pgeyRenderPixels(req);
            long dt = System.nanoTime() - t0;
            if (run >= 0) {
                times[run] = dt;
            }
        }
        Arrays.sort(times);
        double medianMs = times[times.length / 2] / 1e6;
        System.out.printf("Recoloreado 720×900 (4 zonas + rayas + nombre + número): mediana %.1f ms, mínimo %.1f ms%n",
                medianMs, times[0] / 1e6);
        assertThat(medianMs).isLessThan(50);
    }
}
