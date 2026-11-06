package com.sportforce3d.component;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/** Notificación breve abajo a la derecha. Se cierra sola a los 4 s (6 s si es error) o con clic. */
public final class Toast {

    private static final Logger log = LoggerFactory.getLogger(Toast.class);

    public enum Kind { INFO, SUCCESS, WARNING, ERROR }

    private Toast() {
    }

    public static void show(Node anyNodeInWindow, String message, Kind kind) {
        AppRoot root = AppRoot.of(anyNodeInWindow);
        if (root == null) {
            log.warn("Toast sin ventana: {}", message);
            return;
        }
        Ikon ikon = switch (kind) {
            case INFO -> Feather.INFO;
            case SUCCESS -> Feather.CHECK_CIRCLE;
            case WARNING -> Feather.ALERT_TRIANGLE;
            case ERROR -> Feather.ALERT_OCTAGON;
        };
        Label text = new Label(message);
        text.setWrapText(true);
        text.setMaxWidth(340);

        HBox toast = new HBox(Icons.of(ikon, Icons.MD), text);
        toast.getStyleClass().addAll("sf-toast", "sf-toast-" + kind.name().toLowerCase(Locale.ROOT));
        toast.setMaxWidth(400);
        toast.setMaxHeight(HBox.USE_PREF_SIZE);
        toast.setAccessibleText(message);
        toast.setAccessibleRole(javafx.scene.AccessibleRole.TEXT);

        root.toastLayer().getChildren().add(toast);
        Motion.enter(toast, Motion.MEDIUM, 8);

        Runnable dismiss = () -> {
            var fade = Motion.fadeOut(toast, Motion.MEDIUM);
            fade.setOnFinished(e -> root.toastLayer().getChildren().remove(toast));
            fade.play();
        };
        toast.setOnMouseClicked(e -> dismiss.run());
        PauseTransition wait = new PauseTransition(Duration.seconds(kind == Kind.ERROR ? 6 : 4));
        wait.setOnFinished(e -> dismiss.run());
        wait.play();
    }

    public static void success(Node node, String message) {
        show(node, message, Kind.SUCCESS);
    }

    public static void error(Node node, String message) {
        show(node, message, Kind.ERROR);
    }

    public static void info(Node node, String message) {
        show(node, message, Kind.INFO);
    }
}
