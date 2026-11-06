package com.sportforce3d.app;

import com.sportforce3d.config.AppConfig;
import javafx.application.Application;

/**
 * Punto de entrada. No extiende Application a propósito: así la aplicación arranca desde el classpath
 * (IntelliJ, java -jar) sin el error "JavaFX runtime components are missing".
 */
public final class Launcher {

    public static final String LOG_DIR_PROPERTY = "sportforce.logdir";

    private Launcher() {
    }

    public static void main(String[] args) {
        // Antes del primer logger: logback.xml escribe el archivo de registro junto a la base de datos.
        System.setProperty(LOG_DIR_PROPERTY, AppConfig.load().logDir().toString());
        Application.launch(SportForceApp.class, args);
    }
}