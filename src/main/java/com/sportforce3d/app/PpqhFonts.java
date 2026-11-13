package com.sportforce3d.app;

import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public final class PpqhFonts {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhFonts.class);

    static final String[] PPQH_FILES = {
            "Barlow-Regular.ttf",
            "Barlow-Medium.ttf",
            "Barlow-SemiBold.ttf",
            "BarlowCondensed-SemiBold.ttf",
            "BarlowCondensed-Bold.ttf",
            "BarlowCondensed-ExtraBold.ttf",
            "JetBrainsMono-Medium.ttf",
    };

    private static boolean ppqhLoaded;

    private PpqhFonts() {
    }

    public static synchronized int ppqhLoadAll() {
        if (ppqhLoaded) {
            return PPQH_FILES.length;
        }
        int ppqhOk = 0;
        for (String ppqhFile : PPQH_FILES) {
            try (InputStream ppqhIn = PpqhFonts.class.getResourceAsStream("/fonts/" + ppqhFile)) {
                if (ppqhIn == null) {
                    ppqhLog.warn("No se encontró la fuente /fonts/{}", ppqhFile);
                    continue;
                }
                Font ppqhFont = Font.loadFont(ppqhIn, 12);
                if (ppqhFont == null) {
                    ppqhLog.warn("No se pudo cargar la fuente {}", ppqhFile);
                } else {
                    ppqhOk++;
                    ppqhLog.debug("Fuente cargada: {} -> {}", ppqhFile, ppqhFont.getFamily());
                }
            } catch (Exception ppqhE) {
                ppqhLog.warn("Error al cargar la fuente {}", ppqhFile, ppqhE);
            }
        }
        ppqhLoaded = ppqhOk == PPQH_FILES.length;
        ppqhLog.info("Fuentes cargadas: {}/{}", ppqhOk, PPQH_FILES.length);
        return ppqhOk;
    }
}
