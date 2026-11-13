package com.sportforce3d.app;

import com.sportforce3d.config.AppConfig;
import javafx.application.Application;

public final class Launcher {

    public static final String LOG_DIR_PROPERTY = "sportforce.logdir";

    private Launcher() {
    }

    public static void main(String[] args) {
        System.setProperty(LOG_DIR_PROPERTY, AppConfig.load().logDir().toString());
        Application.launch(SportForceApp.class, args);
    }
}
