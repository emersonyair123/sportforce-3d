package com.sportforce3d.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Objects;

public final class PpqhViewLoader {

    public record PpqhLoaded<C>(Parent ppqhRoot, C ppqhController) {
    }

    private PpqhViewLoader() {
    }

    public static <C> PpqhLoaded<C> ppqhLoad(String ppqhName, PpqhAppContext ppqhContext) {
        URL ppqhUrl = Objects.requireNonNull(PpqhViewLoader.class.getResource("/fxml/" + ppqhName + ".fxml"),
                "No existe /fxml/" + ppqhName + ".fxml");
        FXMLLoader ppqhLoader = new FXMLLoader(ppqhUrl);
        ppqhLoader.setControllerFactory(ppqhType -> ppqhCreate(ppqhType, ppqhContext));
        try {
            Parent ppqhRoot = ppqhLoader.load();
            return new PpqhLoaded<>(ppqhRoot, ppqhLoader.getController());
        } catch (IOException ppqhE) {
            throw new UncheckedIOException("No se pudo cargar la vista " + ppqhName, ppqhE);
        }
    }

    private static Object ppqhCreate(Class<?> ppqhType, PpqhAppContext ppqhContext) {
        try {
            try {
                Constructor<?> ppqhWithContext = ppqhType.getConstructor(PpqhAppContext.class);
                return ppqhWithContext.newInstance(ppqhContext);
            } catch (NoSuchMethodException ppqhE) {
                return ppqhType.getConstructor().newInstance();
            }
        } catch (ReflectiveOperationException ppqhE) {
            throw new IllegalStateException("No se pudo crear el controlador " + ppqhType.getName(), ppqhE);
        }
    }
}
