package com.sportforce3d.component;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Tokens de movimiento de la guía: 120 ms (hover/color), 200 ms (tarjetas/menús), 320 ms (drawers, modales, cámara 3D)
 * con la curva cubic-bezier(0.2, 0.8, 0.2, 1). Con "Reducir animaciones" no hay desplazamientos, solo opacidad.
 */
public final class Motion {

    public static final Duration FAST = Duration.millis(120);
    public static final Duration MEDIUM = Duration.millis(200);
    public static final Duration SLOW = Duration.millis(320);
    public static final Interpolator EASE = Interpolator.SPLINE(0.2, 0.8, 0.2, 1);

    private static final BooleanProperty REDUCED = new SimpleBooleanProperty(false);

    private Motion() {
    }

    public static BooleanProperty reducedProperty() {
        return REDUCED;
    }

    public static boolean isReduced() {
        return REDUCED.get();
    }

    /** Hover de tarjeta: sube 4 px en 200 ms (la sombra la pone el CSS :hover). */
    public static void liftOnHover(Node node) {
        TranslateTransition lift = new TranslateTransition(MEDIUM, node);
        lift.setInterpolator(EASE);
        node.hoverProperty().addListener((obs, was, hovered) -> {
            lift.stop();
            if (isReduced()) {
                node.setTranslateY(0);
                return;
            }
            lift.setFromY(node.getTranslateY());
            lift.setToY(hovered ? -4 : 0);
            lift.playFromStart();
        });
    }

    /** Aparece con opacidad y, si se permite movimiento, sube {@code dy} px. */
    public static void enter(Node node, Duration duration, double dy) {
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setInterpolator(EASE);
        fade.setToValue(1);
        if (!isReduced() && dy != 0) {
            node.setTranslateY(dy);
            TranslateTransition move = new TranslateTransition(duration, node);
            move.setInterpolator(EASE);
            move.setToY(0);
            move.play();
        }
        fade.play();
    }

    public static FadeTransition fadeOut(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setInterpolator(EASE);
        fade.setToValue(0);
        return fade;
    }
}
