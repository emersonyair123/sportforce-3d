package com.sportforce3d.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Objects;

/**
 * Carga /fxml/{nombre}.fxml. Los controladores reciben el {@link AppContext} por constructor,
 * así no usan estáticos ni buscan dependencias por su cuenta.
 */
public final class ViewLoader {

    public record Loaded<C>(Parent root, C controller) {
    }

    private ViewLoader() {
    }

    public static <C> Loaded<C> load(String name, AppContext context) {
        URL url = Objects.requireNonNull(ViewLoader.class.getResource("/fxml/" + name + ".fxml"),
                "No existe /fxml/" + name + ".fxml");
        FXMLLoader loader = new FXMLLoader(url);
        loader.setControllerFactory(type -> create(type, context));
        try {
            Parent root = loader.load();
            return new Loaded<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + name, e);
        }
    }

    private static Object create(Class<?> type, AppContext context) {
        try {
            try {
                Constructor<?> withContext = type.getConstructor(AppContext.class);
                return withContext.newInstance(context);
            } catch (NoSuchMethodException e) {
                return type.getConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo crear el controlador " + type.getName(), e);
        }
    }
}
