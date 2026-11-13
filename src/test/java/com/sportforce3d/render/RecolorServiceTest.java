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

    static final RealisticModelRepository.ImageDecoder IMAGE_IO = in -> {
        BufferedImage img = ImageIO.read(in);
        if (img == null) {
            throw new IOException("Formato no reconocido");
        }
        int w = img.getWidth();
        int h = img.getHeight();
        return new ArgbImage(w, h, img.getRGB(0, 0, w, h, null, 0, w));
    };

    private static RealisticModelRepository repo;

    @BeforeAll
    static void load() {
        repo = new RealisticModelRepository(RealisticModelRepository.ROOT, IMAGE_IO);
    }

    @Test
    void readsAllModelsAndTheirLayers() {
        assertThat(repo.models()).as("34 + 9 de la Etapa 12B").hasSize(43);
        for (RealisticModel m : repo.models()) {
            assertThat(m.width()).isEqualTo(720);
            assertThat(m.height()).isEqualTo(900);
            ModelLayers layers = repo.layers(m.key());
            for (String zone : m.zones()) {
                assertThat(layers.zone(zone).isEmpty()).as(m.key() + " " + zone).isFalse();
            }
            assertThat(layers.detailAlpha().isEmpty()).as(m.key() + " detalle").isFalse();
            boolean garment = m.cut().equals("buzo") || m.cut().equals("casaca") || m.view() == RealisticModel.View.PRODUCTO;
            assertThat(m.shirt() == null).as(m.key() + " geometría").isEqualTo(garment);
        }
        assertThat(repo.layers("futbol_hombre_frente")).isSameAs(repo.layers("futbol_hombre_frente"));
    }

    @Test
    void modelKeysDescribeCutAudienceAndOtherView() {
        RealisticModel m = repo.require("futbol_mujer_frente");
        assertThat(m.cut()).isEqualTo("futbol");
        assertThat(m.audience()).isEqualTo("mujer");
        assertThat(repo.otherView(m)).map(RealisticModel::key).contains("futbol_mujer_espalda");
        assertThat(repo.otherView(repo.require("buzo_hombre_frente"))).isEmpty();
        assertThat(repo.require("producto_medias").audience()).isNull();
        RealisticModel.ShirtGeometry g = repo.require("futbol_hombre_frente").shirt();
        assertThat(g).isEqualTo(new RealisticModel.ShirtGeometry(355, 131, 434));
        assertThatThrownBy(() -> repo.require("no_existe")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cachesByModelColorsDesignAndText() {
        RecolorService service = new RecolorService(repo);
        RecolorRequest a = RecolorRequest.of("futbol_hombre_frente", Map.of("camiseta", "#1F5FFF"));
        RecolorRequest same = RecolorRequest.of("futbol_hombre_frente", Map.of("camiseta", "#1f5fff"));
        assertThat(service.renderPixels(a)).isSameAs(service.renderPixels(same));

        RecolorRequest numbered = new RecolorRequest("futbol_hombre_frente", Map.of("camiseta", "#1f5fff"),
                RealisticPattern.LISO, null, null, 10, null);
        assertThat(numbered.cacheKey()).isNotEqualTo(a.cacheKey());
        assertThat(service.renderPixels(numbered)).isNotSameAs(service.renderPixels(a));
    }

    @Test
    void lruEvictsTheLeastRecentlyUsed() {
        long oneImage = 720L * 900 * 4;
        RecolorService service = new RecolorService(repo, 2 * oneImage);
        RecolorRequest red = RecolorRequest.of("voley_mujer_frente", Map.of("camiseta", "#d7141e"));
        RecolorRequest blue = RecolorRequest.of("voley_mujer_frente", Map.of("camiseta", "#1f5fff"));
        RecolorRequest green = RecolorRequest.of("voley_mujer_frente", Map.of("camiseta", "#39ff88"));
        service.renderPixels(red);
        service.renderPixels(blue);
        service.renderPixels(red);
        service.renderPixels(green);
        assertThat(service.isCached(red)).isTrue();
        assertThat(service.isCached(green)).isTrue();
        assertThat(service.isCached(blue)).isFalse();
    }

    @Test
    void recolorsOnlyTheZoneAndKeepsTheRestOfThePhoto() {
        RecolorService service = new RecolorService(repo);
        ModelLayers layers = repo.layers("futbol_hombre_frente");
        ArgbImage out = service.renderPixels(RecolorRequest.of("futbol_hombre_frente", Map.of("camiseta", "#d7141e")));
        Mask cam = layers.zone("camiseta");
        Mask detail = layers.detailAlpha();
        int[] base = layers.base().pixels();
        int changedOutside = 0;
        long redInside = 0;
        long inside = 0;
        for (int y = 0; y < out.height(); y++) {
            for (int x = 0; x < out.width(); x++) {
                int i = y * out.width() + x;
                if (cam.at(x, y) == 0 && detail.at(x, y) == 0 && out.pixels()[i] != base[i]) {
                    changedOutside++;
                }
                if (cam.at(x, y) == 255) {
                    inside++;
                    int c = out.pixels()[i];
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
        RecolorService service = new RecolorService(repo);
        Map<String, String> colors = Map.of("camiseta", "#f2f5f8", "short", "#0b2a6b", "paneles", "#0b2a6b",
                "medias", "#0b2a6b");
        ArgbImage plain = service.renderPixels(RecolorRequest.of("futbol_hombre_espalda", colors));
        Mask cam = repo.layers("futbol_hombre_espalda").zone("camiseta");
        for (RealisticPattern p : RealisticPattern.values()) {
            RecolorRequest req = new RecolorRequest("futbol_hombre_espalda", colors, p,
                    p == RealisticPattern.LISO ? null : "#d7141e", "Quispe", 10, null);
            ArgbImage designed = service.renderPixels(req);
            int changedOutside = 0;
            int changedInside = 0;
            for (int y = 0; y < plain.height(); y++) {
                for (int x = 0; x < plain.width(); x++) {
                    boolean diff = designed.argb(x, y) != plain.argb(x, y);
                    if (diff && cam.at(x, y) == 0) {
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
        RecolorService service = new RecolorService(repo);
        RecolorRequest req = RecolorRequest.of("producto_zapatilla_fg", Map.of("principal", "#1f5fff"));
        ArgbImage thumb = service.thumbnailPixels(req, 240);
        assertThat(thumb.width()).isEqualTo(240);
        assertThat(thumb.height()).isEqualTo(300);
        assertThat(service.thumbnailPixels(req, 240)).isSameAs(thumb);
    }

    @Test
    void rendersAFullPhotoInLessThan50Milliseconds() {
        RecolorService service = new RecolorService(repo, 0);
        String key = "futbol_hombre_espalda";
        repo.layers(key);
        long[] times = new long[25];
        for (int run = -10; run < times.length; run++) {
            String shirt = String.format("#%06x", 0x102030 + Math.abs(run) * 0x010101 + (run < 0 ? 0x400000 : 0));
            Map<String, String> colors = new LinkedHashMap<>();
            colors.put("camiseta", shirt);
            colors.put("short", "#0b2a6b");
            colors.put("paneles", "#39ff88");
            colors.put("medias", "#0b2a6b");
            RecolorRequest req = new RecolorRequest(key, colors, RealisticPattern.RAYAS, "#0b2a6b", "ROJAS", 7, null);
            long t0 = System.nanoTime();
            service.renderPixels(req);
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
