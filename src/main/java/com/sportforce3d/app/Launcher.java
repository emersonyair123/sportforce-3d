package com.sportforce3d.app;

import com.sportforce3d.config.PpqhAppConfig;
import javafx.application.Application;

public final class Launcher {

    public static final String PPQH_LOG_DIR_PROPERTY = "sportforce.logdir";

    private Launcher() {
    }

    public static void main(String[] ppqhArgs) {
        System.setProperty(PPQH_LOG_DIR_PROPERTY, PpqhAppConfig.ppqhLoad().ppqhLogDir().toString());
        Application.launch(PpqhSportForceApp.class, ppqhArgs);
    }
}
