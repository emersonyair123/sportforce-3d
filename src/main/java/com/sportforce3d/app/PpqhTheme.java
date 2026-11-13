package com.sportforce3d.app;

import javafx.scene.Scene;

import java.net.URL;
import java.util.Objects;

public enum PpqhTheme {
    DARK("/css/tokens-dark.css"),
    LIGHT("/css/tokens-light.css");

    private static final String PPQH_COMPONENTS = "/css/components.css";

    private final String ppqhTokens;

    PpqhTheme(String ppqhTokens) {
        this.ppqhTokens = ppqhTokens;
    }

    public void ppqhApply(Scene ppqhScene) {
        ppqhScene.getStylesheets().setAll(ppqhUrl(ppqhTokens), ppqhUrl(PPQH_COMPONENTS));
    }

    private static String ppqhUrl(String ppqhPath) {
        URL ppqhResource = Objects.requireNonNull(PpqhTheme.class.getResource(ppqhPath), "Falta el recurso " + ppqhPath);
        return ppqhResource.toExternalForm();
    }
}
