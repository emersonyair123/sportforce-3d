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

public final class PpqhToast {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhToast.class);

    public enum PpqhKind { INFO, SUCCESS, WARNING, ERROR }

    private PpqhToast() {
    }

    public static void ppqhShow(Node ppqhAnyNodeInWindow, String ppqhMessage, PpqhKind ppqhKind) {
        PpqhAppRoot ppqhRoot = PpqhAppRoot.ppqhOf(ppqhAnyNodeInWindow);
        if (ppqhRoot == null) {
            ppqhLog.warn("Toast sin ventana: {}", ppqhMessage);
            return;
        }
        Ikon ppqhIkon = switch (ppqhKind) {
            case INFO -> Feather.INFO;
            case SUCCESS -> Feather.CHECK_CIRCLE;
            case WARNING -> Feather.ALERT_TRIANGLE;
            case ERROR -> Feather.ALERT_OCTAGON;
        };
        Label ppqhText = new Label(ppqhMessage);
        ppqhText.setWrapText(true);
        ppqhText.setMaxWidth(340);

        HBox ppqhToast = new HBox(PpqhIcons.ppqhOf(ppqhIkon, PpqhIcons.PPQH_MD), ppqhText);
        ppqhToast.getStyleClass().addAll("sf-toast", "sf-toast-" + ppqhKind.name().toLowerCase(Locale.ROOT));
        ppqhToast.setMaxWidth(400);
        ppqhToast.setMaxHeight(HBox.USE_PREF_SIZE);
        ppqhToast.setAccessibleText(ppqhMessage);
        ppqhToast.setAccessibleRole(javafx.scene.AccessibleRole.TEXT);

        ppqhRoot.ppqhToastLayer().getChildren().add(ppqhToast);
        PpqhMotion.ppqhEnter(ppqhToast, PpqhMotion.PPQH_MEDIUM, 8);

        Runnable ppqhDismiss = () -> {
            var ppqhFade = PpqhMotion.ppqhFadeOut(ppqhToast, PpqhMotion.PPQH_MEDIUM);
            ppqhFade.setOnFinished(ppqhE -> ppqhRoot.ppqhToastLayer().getChildren().remove(ppqhToast));
            ppqhFade.play();
        };
        ppqhToast.setOnMouseClicked(ppqhE -> ppqhDismiss.run());
        PauseTransition ppqhWait = new PauseTransition(Duration.seconds(ppqhKind == PpqhKind.ERROR ? 6 : 4));
        ppqhWait.setOnFinished(ppqhE -> ppqhDismiss.run());
        ppqhWait.play();
    }

    public static void ppqhSuccess(Node ppqhNode, String ppqhMessage) {
        ppqhShow(ppqhNode, ppqhMessage, PpqhKind.SUCCESS);
    }

    public static void ppqhError(Node ppqhNode, String ppqhMessage) {
        ppqhShow(ppqhNode, ppqhMessage, PpqhKind.ERROR);
    }

    public static void ppqhInfo(Node ppqhNode, String ppqhMessage) {
        ppqhShow(ppqhNode, ppqhMessage, PpqhKind.INFO);
    }
}
