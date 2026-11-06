package com.sportforce3d.app;

import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Carga las fuentes OFL incluidas en resources/fonts. JavaFX registra cada peso como una familia propia:
 * "Barlow", "Barlow Medium", "Barlow SemiBold", "Barlow Condensed SemiBold", "Barlow Condensed" (Bold),
 * "Barlow Condensed ExtraBold" y "JetBrains Mono Medium". Esos son los nombres que usa components.css.
 */
public final class Fonts {

    private static final Logger log = LoggerFactory.getLogger(Fonts.class);

    static final String[] FILES = {
            "Barlow-Regular.ttf",
            "Barlow-Medium.ttf",
            "Barlow-SemiBold.ttf",
            "BarlowCondensed-SemiBold.ttf",
            "BarlowCondensed-Bold.ttf",
            "BarlowCondensed-ExtraBold.ttf",
            "JetBrainsMono-Medium.ttf",
    };

    private static boolean loaded;

    private Fonts() {
    }

    /** Devuelve cuántas fuentes se cargaron. Si falta alguna, la interfaz usa la fuente del sistema. */
    public static synchronized int loadAll() {
        if (loaded) {
            return FILES.length;
        }
        int ok = 0;
        for (String file : FILES) {
            try (InputStream in = Fonts.class.getResourceAsStream("/fonts/" + file)) {
                if (in == null) {
                    log.warn("No se encontró la fuente /fonts/{}", file);
                    continue;
                }
                Font font = Font.loadFont(in, 12);
                if (font == null) {
                    log.warn("No se pudo cargar la fuente {}", file);
                } else {
                    ok++;
                    log.debug("Fuente cargada: {} -> {}", file, font.getFamily());
                }
            } catch (Exception e) {
                log.warn("Error al cargar la fuente {}", file, e);
            }
        }
        loaded = ok == FILES.length;
        log.info("Fuentes cargadas: {}/{}", ok, FILES.length);
        return ok;
    }
}
