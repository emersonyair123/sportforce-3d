package com.sportforce3d.component;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AppRoot extends StackPane {

    private final StackPane contentLayer = new StackPane();
    private final StackPane dialogLayer = new StackPane();
    private final VBox toastLayer = new VBox();

    public AppRoot() {
        getStyleClass().add("sf-page");
        dialogLayer.setVisible(false);
        dialogLayer.setPickOnBounds(true);
        toastLayer.getStyleClass().add("sf-toast-host");
        toastLayer.setAlignment(Pos.BOTTOM_RIGHT);
        toastLayer.setPickOnBounds(false);
        toastLayer.setMouseTransparent(false);
        getChildren().addAll(contentLayer, dialogLayer, toastLayer);
    }

    public void setContent(Node node) {
        contentLayer.getChildren().setAll(node);
    }

    public Node getContent() {
        return contentLayer.getChildren().isEmpty() ? null : contentLayer.getChildren().get(0);
    }

    StackPane dialogLayer() {
        return dialogLayer;
    }

    VBox toastLayer() {
        return toastLayer;
    }

    static AppRoot of(Node node) {
        if (node == null) {
            return null;
        }
        if (node instanceof AppRoot root) {
            return root;
        }
        Scene scene = node.getScene();
        return scene != null && scene.getRoot() instanceof AppRoot root ? root : null;
    }
}
