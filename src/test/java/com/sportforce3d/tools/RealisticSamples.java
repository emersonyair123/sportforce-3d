package com.sportforce3d.tools;

import com.sportforce3d.render.ArgbImage;
import com.sportforce3d.render.RealisticModelRepository;
import com.sportforce3d.render.RealisticPattern;
import com.sportforce3d.render.RecolorRequest;
import com.sportforce3d.render.RecolorService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Láminas de prueba del recoloreado realista (para comparar con sportforce-realista/referencia). Sin JavaFX.
 * Uso: {@code RealisticSamples <carpeta>}.
 */
public final class RealisticSamples {

    private RealisticSamples() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "target/realista");
        Files.createDirectories(out);
        RealisticModelRepository repo = new RealisticModelRepository(RealisticModelRepository.ROOT, in -> {
            BufferedImage img = ImageIO.read(in);
            return new ArgbImage(img.getWidth(), img.getHeight(),
                    img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth()));
        });
        RecolorService service = new RecolorService(repo);
        Map<String, String> navy = Map.of("camiseta", "#f2f5f8", "short", "#0b2a6b", "paneles", "#0b2a6b", "medias", "#0b2a6b");
        Map<String, String> dark = Map.of("camiseta", "#0b0e13", "short", "#0b0e13", "paneles", "#39ff88", "medias", "#0b0e13");
        Map<String, String> blue = Map.of("camiseta", "#1f5fff", "short", "#f2f5f8", "paneles", "#f2f5f8", "medias", "#1f5fff");
        Map<String, String> yellow = Map.of("camiseta", "#ffd400", "short", "#0b0e13", "paneles", "#0b0e13", "medias", "#ffd400");
        Map<String, String> white = Map.of("camiseta", "#f2f5f8", "short", "#0b0e13", "paneles", "#d7141e", "medias", "#f2f5f8");
        Map<String, String> blueYellow = Map.of("camiseta", "#0b2a6b", "short", "#0b2a6b", "paneles", "#ffd400", "medias", "#0b2a6b");
        sheet(service, out.resolve("disenos-frente.png"), List.of(
                new RecolorRequest("futbol_hombre_frente", navy, RealisticPattern.RAYAS, "#0b2a6b", null, null, null),
                new RecolorRequest("futbol_mujer_frente", blueYellow, RealisticPattern.FRANJAS, "#ffd400", null, null, null),
                new RecolorRequest("futbol_mujer_frente", dark, RealisticPattern.DINAMICO, "#39ff88", null, null, null),
                new RecolorRequest("futbol_hombre_frente", blue, RealisticPattern.GEOMETRICO, "#f2f5f8", null, null, null),
                new RecolorRequest("futbol_nino_frente", yellow, RealisticPattern.MINIMAL, "#0b0e13", null, null, null),
                new RecolorRequest("futbol_hombre_frente", white, RealisticPattern.BANDA, "#d7141e", null, 10, null)));
        sheet(service, out.resolve("disenos-espalda.png"), List.of(
                new RecolorRequest("futbol_hombre_espalda", navy, RealisticPattern.RAYAS, "#0b2a6b", "QUISPE", 9, null),
                new RecolorRequest("futbol_mujer_espalda", blueYellow, RealisticPattern.FRANJAS, "#ffd400", "MAMANI", 7, null),
                new RecolorRequest("futbol_mujer_espalda", dark, RealisticPattern.DINAMICO, "#39ff88", "FLORES", 11, null),
                new RecolorRequest("voley_hombre_espalda", blue, RealisticPattern.GEOMETRICO, "#f2f5f8", "HUAMAN", 4, null),
                new RecolorRequest("futbol_nino_espalda", yellow, RealisticPattern.MINIMAL, "#0b0e13", "LEO", 10, null),
                new RecolorRequest("basquet_hombre_espalda", white, RealisticPattern.BANDA, "#d7141e", "CHÁVEZ", 23, null)));
        sheet(service, out.resolve("cortes.png"), List.of(
                RecolorRequest.of("polo_hombre_frente", Map.of("camiseta", "#0b2a6b", "short", "#0b2a6b", "paneles", "#f2f5f8", "medias", "#0b2a6b")),
                RecolorRequest.of("redondo_hombre_frente", Map.of("camiseta", "#d7141e", "short", "#0b0e13", "paneles", "#0b0e13", "medias", "#d7141e")),
                RecolorRequest.of("raglan_mujer_frente", Map.of("camiseta", "#f2f5f8", "short", "#0b0e13", "paneles", "#ff5fb0", "medias", "#f2f5f8")),
                RecolorRequest.of("shortlargo_hombre_frente", Map.of("camiseta", "#f2f5f8", "short", "#0b0e13", "paneles", "#39ff88")),
                RecolorRequest.of("licra_mujer_frente", Map.of("camiseta", "#f2f5f8", "short", "#8b3dff", "paneles", "#0b0e13")),
                RecolorRequest.of("buzo_hombre_frente", Map.of("principal", "#0b2a6b", "paneles", "#39ff88")),
                RecolorRequest.of("producto_zapatilla_fg", Map.of("principal", "#1f5fff"))));
    }

    /** Une varias miniaturas de 360 px en una lámina horizontal. */
    private static void sheet(RecolorService service, Path file, List<RecolorRequest> requests) throws IOException {
        int w = 360;
        int h = 450;
        BufferedImage img = new BufferedImage(w * requests.size(), h, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < requests.size(); i++) {
            ArgbImage t = service.thumbnailPixels(requests.get(i), w);
            img.setRGB(i * w, 0, t.width(), t.height(), t.pixels(), 0, t.width());
        }
        ImageIO.write(img, "png", file.toFile());
        System.out.println("Lámina: " + file.toAbsolutePath());
    }
}
