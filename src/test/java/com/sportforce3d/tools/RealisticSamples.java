package com.sportforce3d.tools;

import com.sportforce3d.render.PgeyArgbImage;
import com.sportforce3d.render.PgeyRealisticModelRepository;
import com.sportforce3d.render.PgeyRealisticPattern;
import com.sportforce3d.render.PgeyRecolorRequest;
import com.sportforce3d.render.PgeyRecolorService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class RealisticSamples {

    private RealisticSamples() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "target/realista");
        Files.createDirectories(out);
        PgeyRealisticModelRepository repo = new PgeyRealisticModelRepository(PgeyRealisticModelRepository.PGEY_ROOT, in -> {
            BufferedImage img = ImageIO.read(in);
            return new PgeyArgbImage(img.getWidth(), img.getHeight(),
                    img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth()));
        });
        PgeyRecolorService service = new PgeyRecolorService(repo);
        Map<String, String> navy = Map.of("camiseta", "#f2f5f8", "short", "#0b2a6b", "paneles", "#0b2a6b", "medias", "#0b2a6b");
        Map<String, String> dark = Map.of("camiseta", "#0b0e13", "short", "#0b0e13", "paneles", "#39ff88", "medias", "#0b0e13");
        Map<String, String> blue = Map.of("camiseta", "#1f5fff", "short", "#f2f5f8", "paneles", "#f2f5f8", "medias", "#1f5fff");
        Map<String, String> yellow = Map.of("camiseta", "#ffd400", "short", "#0b0e13", "paneles", "#0b0e13", "medias", "#ffd400");
        Map<String, String> white = Map.of("camiseta", "#f2f5f8", "short", "#0b0e13", "paneles", "#d7141e", "medias", "#f2f5f8");
        Map<String, String> blueYellow = Map.of("camiseta", "#0b2a6b", "short", "#0b2a6b", "paneles", "#ffd400", "medias", "#0b2a6b");
        sheet(service, out.resolve("disenos-frente.png"), List.of(
                new PgeyRecolorRequest("futbol_hombre_frente", navy, PgeyRealisticPattern.RAYAS, "#0b2a6b", null, null, null),
                new PgeyRecolorRequest("futbol_mujer_frente", blueYellow, PgeyRealisticPattern.FRANJAS, "#ffd400", null, null, null),
                new PgeyRecolorRequest("futbol_mujer_frente", dark, PgeyRealisticPattern.DINAMICO, "#39ff88", null, null, null),
                new PgeyRecolorRequest("futbol_hombre_frente", blue, PgeyRealisticPattern.GEOMETRICO, "#f2f5f8", null, null, null),
                new PgeyRecolorRequest("futbol_nino_frente", yellow, PgeyRealisticPattern.MINIMAL, "#0b0e13", null, null, null),
                new PgeyRecolorRequest("futbol_hombre_frente", white, PgeyRealisticPattern.BANDA, "#d7141e", null, 10, null)));
        sheet(service, out.resolve("disenos-espalda.png"), List.of(
                new PgeyRecolorRequest("futbol_hombre_espalda", navy, PgeyRealisticPattern.RAYAS, "#0b2a6b", "QUISPE", 9, null),
                new PgeyRecolorRequest("futbol_mujer_espalda", blueYellow, PgeyRealisticPattern.FRANJAS, "#ffd400", "MAMANI", 7, null),
                new PgeyRecolorRequest("futbol_mujer_espalda", dark, PgeyRealisticPattern.DINAMICO, "#39ff88", "FLORES", 11, null),
                new PgeyRecolorRequest("voley_hombre_espalda", blue, PgeyRealisticPattern.GEOMETRICO, "#f2f5f8", "HUAMAN", 4, null),
                new PgeyRecolorRequest("futbol_nino_espalda", yellow, PgeyRealisticPattern.MINIMAL, "#0b0e13", "LEO", 10, null),
                new PgeyRecolorRequest("basquet_hombre_espalda", white, PgeyRealisticPattern.BANDA, "#d7141e", "CHÁVEZ", 23, null)));
        sheet(service, out.resolve("cortes.png"), List.of(
                PgeyRecolorRequest.pgeyOf("polo_hombre_frente", Map.of("camiseta", "#0b2a6b", "short", "#0b2a6b", "paneles", "#f2f5f8", "medias", "#0b2a6b")),
                PgeyRecolorRequest.pgeyOf("redondo_hombre_frente", Map.of("camiseta", "#d7141e", "short", "#0b0e13", "paneles", "#0b0e13", "medias", "#d7141e")),
                PgeyRecolorRequest.pgeyOf("raglan_mujer_frente", Map.of("camiseta", "#f2f5f8", "short", "#0b0e13", "paneles", "#ff5fb0", "medias", "#f2f5f8")),
                PgeyRecolorRequest.pgeyOf("shortlargo_hombre_frente", Map.of("camiseta", "#f2f5f8", "short", "#0b0e13", "paneles", "#39ff88")),
                PgeyRecolorRequest.pgeyOf("licra_mujer_frente", Map.of("camiseta", "#f2f5f8", "short", "#8b3dff", "paneles", "#0b0e13")),
                PgeyRecolorRequest.pgeyOf("buzo_hombre_frente", Map.of("principal", "#0b2a6b", "paneles", "#39ff88")),
                PgeyRecolorRequest.pgeyOf("producto_zapatilla_fg", Map.of("principal", "#1f5fff"))));
    }

    private static void sheet(PgeyRecolorService service, Path file, List<PgeyRecolorRequest> requests) throws IOException {
        int w = 360;
        int h = 450;
        BufferedImage img = new BufferedImage(w * requests.size(), h, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < requests.size(); i++) {
            PgeyArgbImage t = service.pgeyThumbnailPixels(requests.get(i), w);
            img.setRGB(i * w, 0, t.pgeyWidth(), t.pgeyHeight(), t.pgeyPixels(), 0, t.pgeyWidth());
        }
        ImageIO.write(img, "png", file.toFile());
        System.out.println("Lámina: " + file.toAbsolutePath());
    }
}
