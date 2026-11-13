package com.sportforce3d.component;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public final class ModalDialog {

    private final StackPane layer;
    private final Node previousFocus;
    private final SfButton confirm;
    private final SfButton cancel;
    private final Label error = new Label();

    private ModalDialog(StackPane layer, Node previousFocus, SfButton confirm, SfButton cancel) {
        this.layer = layer;
        this.previousFocus = previousFocus;
        this.confirm = confirm;
        this.cancel = cancel;
    }

    public enum Style { ACCENT, PRIMARY, DANGER }

    public static ModalDialog show(Node anyNodeInWindow, String title, String message, Node content,
                                   String confirmText, Style style, Consumer<ModalDialog> onConfirm) {
        AppRoot root = AppRoot.of(anyNodeInWindow);
        if (root == null) {
            throw new IllegalStateException("ModalDialog necesita una ventana con AppRoot");
        }
        StackPane layer = root.dialogLayer();
        Node previousFocus = root.getScene() == null ? null : root.getScene().getFocusOwner();

        Region scrim = new Region();
        scrim.getStyleClass().add("sf-scrim");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("title-s", "ink");
        titleLabel.setWrapText(true);
        VBox dialog = new VBox(titleLabel);
        if (message != null && !message.isBlank()) {
            Label messageLabel = new Label(message);
            messageLabel.getStyleClass().addAll("body-s", "ink-muted");
            messageLabel.setWrapText(true);
            dialog.getChildren().add(messageLabel);
        }

        SfButton cancel = new SfButton("Cancelar", SfButton.Variant.GHOST);
        SfButton confirm = new SfButton(confirmText, switch (style) {
            case ACCENT -> SfButton.Variant.ACCENT;
            case PRIMARY -> SfButton.Variant.PRIMARY;
            case DANGER -> SfButton.Variant.DANGER;
        });
        HBox actions = new HBox(12, cancel, confirm);
        actions.setAlignment(Pos.CENTER_RIGHT);

        ModalDialog d = new ModalDialog(layer, previousFocus, confirm, cancel);
        d.error.getStyleClass().addAll("body-s", "danger-text");
        d.error.setWrapText(true);
        d.error.setVisible(false);
        d.error.managedProperty().bind(d.error.visibleProperty());

        dialog.getChildren().addAll(content, d.error, actions);
        dialog.getStyleClass().add("sf-dialog");
        dialog.setMaxWidth(480);
        dialog.setMaxHeight(Region.USE_PREF_SIZE);
        dialog.setAccessibleRole(javafx.scene.AccessibleRole.NODE);
        dialog.setAccessibleText(title);

        cancel.setOnAction(e -> d.close());
        confirm.setOnAction(e -> {
            d.setError(null);
            onConfirm.accept(d);
        });
        layer.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && !confirm.isDisabled()) {
                d.close();
                e.consume();
            }
        });
        layer.getChildren().setAll(scrim, dialog);
        layer.setVisible(true);
        Motion.enter(dialog, Motion.SLOW, 12);
        cancel.requestFocus();
        return d;
    }

    public void close() {
        layer.getChildren().clear();
        layer.setVisible(false);
        if (previousFocus != null) {
            previousFocus.requestFocus();
        }
    }

    public void setBusy(boolean busy) {
        confirm.setDisable(busy);
        cancel.setDisable(busy);
    }

    public void setError(String message) {
        error.setText(message == null ? "" : message);
        error.setVisible(message != null && !message.isBlank());
    }

    public SfButton confirmButton() {
        return confirm;
    }
}
