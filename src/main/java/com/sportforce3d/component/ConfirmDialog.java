package com.sportforce3d.component;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    public static void show(Node anyNodeInWindow, String title, String message,
                            String confirmText, boolean destructive, Runnable onConfirm) {
        AppRoot root = AppRoot.of(anyNodeInWindow);
        if (root == null) {
            throw new IllegalStateException("ConfirmDialog necesita una ventana con AppRoot");
        }
        StackPane layer = root.dialogLayer();
        Node previousFocus = root.getScene() == null ? null : root.getScene().getFocusOwner();

        Region scrim = new Region();
        scrim.getStyleClass().add("sf-scrim");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("title-s", "ink");
        titleLabel.setWrapText(true);
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().addAll("body-s", "ink-muted");
        messageLabel.setWrapText(true);

        SfButton cancel = new SfButton("Cancelar", SfButton.Variant.GHOST);
        SfButton confirm = new SfButton(confirmText, destructive ? SfButton.Variant.DANGER : SfButton.Variant.PRIMARY);
        HBox actions = new HBox(12, cancel, confirm);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox dialog = new VBox(titleLabel, messageLabel, actions);
        dialog.getStyleClass().add("sf-dialog");
        dialog.setMaxWidth(440);
        dialog.setMaxHeight(Region.USE_PREF_SIZE);
        dialog.setAccessibleRole(javafx.scene.AccessibleRole.NODE);
        dialog.setAccessibleText(title);

        Runnable close = () -> {
            layer.getChildren().clear();
            layer.setVisible(false);
            if (previousFocus != null) {
                previousFocus.requestFocus();
            }
        };
        cancel.setOnAction(e -> close.run());
        confirm.setOnAction(e -> {
            close.run();
            onConfirm.run();
        });
        layer.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                close.run();
                e.consume();
            }
        });

        layer.getChildren().setAll(scrim, dialog);
        layer.setVisible(true);
        Motion.enter(dialog, Motion.SLOW, 12);
        cancel.requestFocus();
    }
}
