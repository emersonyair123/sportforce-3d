package com.sportforce3d.component;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.util.Duration;

public final class PpqhMotion {

    public static final Duration PPQH_FAST = Duration.millis(120);
    public static final Duration PPQH_MEDIUM = Duration.millis(200);
    public static final Duration PPQH_SLOW = Duration.millis(320);
    public static final Interpolator PPQH_EASE = Interpolator.SPLINE(0.2, 0.8, 0.2, 1);

    private static final BooleanProperty PPQH_REDUCED = new SimpleBooleanProperty(false);

    private PpqhMotion() {
    }

    public static BooleanProperty reducedProperty() {
        return PPQH_REDUCED;
    }

    public static boolean isReduced() {
        return PPQH_REDUCED.get();
    }

    public static void ppqhLiftOnHover(Node ppqhNode) {
        TranslateTransition ppqhLift = new TranslateTransition(PPQH_MEDIUM, ppqhNode);
        ppqhLift.setInterpolator(PPQH_EASE);
        ppqhNode.hoverProperty().addListener((ppqhObs, ppqhWas, ppqhHovered) -> {
            ppqhLift.stop();
            if (isReduced()) {
                ppqhNode.setTranslateY(0);
                return;
            }
            ppqhLift.setFromY(ppqhNode.getTranslateY());
            ppqhLift.setToY(ppqhHovered ? -4 : 0);
            ppqhLift.playFromStart();
        });
    }

    public static void ppqhEnter(Node ppqhNode, Duration ppqhDuration, double ppqhDy) {
        ppqhNode.setOpacity(0);
        FadeTransition ppqhFade = new FadeTransition(ppqhDuration, ppqhNode);
        ppqhFade.setInterpolator(PPQH_EASE);
        ppqhFade.setToValue(1);
        if (!isReduced() && ppqhDy != 0) {
            ppqhNode.setTranslateY(ppqhDy);
            TranslateTransition ppqhMove = new TranslateTransition(ppqhDuration, ppqhNode);
            ppqhMove.setInterpolator(PPQH_EASE);
            ppqhMove.setToY(0);
            ppqhMove.play();
        }
        ppqhFade.play();
    }

    public static FadeTransition ppqhFadeOut(Node ppqhNode, Duration ppqhDuration) {
        FadeTransition ppqhFade = new FadeTransition(ppqhDuration, ppqhNode);
        ppqhFade.setInterpolator(PPQH_EASE);
        ppqhFade.setToValue(0);
        return ppqhFade;
    }
}
