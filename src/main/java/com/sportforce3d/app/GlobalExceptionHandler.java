package com.sportforce3d.app;

import com.sportforce3d.component.Toast;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.application.Platform;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public final class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private GlobalExceptionHandler() {
    }

    public static void install(Supplier<Node> anyNodeInMainWindow) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            log.error("Error no controlado en {}", thread.getName(), error);
            Platform.runLater(() -> {
                Node node = anyNodeInMainWindow.get();
                if (node != null) {
                    Toast.error(node, ErrorMessages.of(error));
                }
            });
        });
    }
}
