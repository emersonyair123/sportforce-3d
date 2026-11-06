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
import java.util.function.Function;

/**
 * Diálogo modal con un campo de texto (p. ej. el nombre de un diseño). {@code validate} devuelve el mensaje de
 * error o null; Enter confirma y Esc cancela.
 */
public final class PromptDialog {

    private PromptDialog() {
    }

    public static void show(Node anyNodeInWindow, String title, String message, String fieldLabel, String initial,
                            String confirmText, Function<String, String> validate, Consumer<String> onConfirm) {
        AppRoot root = AppRoot.of(anyNodeInWindow);
        if (root == null) {
            throw new IllegalStateException("PromptDialog necesita una ventana con AppRoot");
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
        SfTextField field = new SfTextField(fieldLabel);
        field.setText(initial == null ? "" : initial);

        SfButton cancel = new SfButton("Cancelar", SfButton.Variant.GHOST);
        SfButton confirm = new SfButton(confirmText, SfButton.Variant.ACCENT);
        HBox actions = new HBox(12, cancel, confirm);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox dialog = new VBox(titleLabel, messageLabel, field, actions);
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
        Runnable submit = () -> {
            String value = field.getText() == null ? "" : field.getText().trim();
            String problem = validate.apply(value);
            if (problem != null) {
                field.setError(problem);
                return;
            }
            close.run();
            onConfirm.accept(value);
        };
        cancel.setOnAction(e -> close.run());
        confirm.setOnAction(e -> submit.run());
        field.getInput().setOnAction(e -> submit.run());
        field.textProperty().addListener((obs, old, text) -> field.setError(""));
        layer.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                close.run();
                e.consume();
            }
        });

        layer.getChildren().setAll(scrim, dialog);
        layer.setVisible(true);
        Motion.enter(dialog, Motion.SLOW, 12);
        field.requestFocus();
        field.getInput().selectAll();
    }
}
