package com.sportforce3d.app;

import javafx.scene.Scene;

import java.net.URL;
import java.util.Objects;

public enum Theme {
    DARK("/css/tokens-dark.css"),
    LIGHT("/css/tokens-light.css");

    private static final String COMPONENTS = "/css/components.css";

    private final String tokens;

    Theme(String tokens) {
        this.tokens = tokens;
    }

    public void apply(Scene scene) {
        scene.getStylesheets().setAll(url(tokens), url(COMPONENTS));
    }

    private static String url(String path) {
        URL resource = Objects.requireNonNull(Theme.class.getResource(path), "Falta el recurso " + path);
        return resource.toExternalForm();
    }
}
