package com.sportforce3d.component;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class PpqhAppRoot extends StackPane {

    private final StackPane ppqhContentLayer = new StackPane();
    private final StackPane ppqhDialogLayer = new StackPane();
    private final VBox ppqhToastLayer = new VBox();

    public PpqhAppRoot() {
        getStyleClass().add("sf-page");
        ppqhDialogLayer.setVisible(false);
        ppqhDialogLayer.setPickOnBounds(true);
        ppqhToastLayer.getStyleClass().add("sf-toast-host");
        ppqhToastLayer.setAlignment(Pos.BOTTOM_RIGHT);
        ppqhToastLayer.setPickOnBounds(false);
        ppqhToastLayer.setMouseTransparent(false);
        getChildren().addAll(ppqhContentLayer, ppqhDialogLayer, ppqhToastLayer);
    }

    public void setContent(Node ppqhNode) {
        ppqhContentLayer.getChildren().setAll(ppqhNode);
    }

    public Node getContent() {
        return ppqhContentLayer.getChildren().isEmpty() ? null : ppqhContentLayer.getChildren().get(0);
    }

    StackPane ppqhDialogLayer() {
        return ppqhDialogLayer;
    }

    VBox ppqhToastLayer() {
        return ppqhToastLayer;
    }

    static PpqhAppRoot ppqhOf(Node ppqhNode) {
        if (ppqhNode == null) {
            return null;
        }
        if (ppqhNode instanceof PpqhAppRoot ppqhRoot) {
            return ppqhRoot;
        }
        Scene ppqhScene = ppqhNode.getScene();
        return ppqhScene != null && ppqhScene.getRoot() instanceof PpqhAppRoot ppqhRoot ? ppqhRoot : null;
    }
}
