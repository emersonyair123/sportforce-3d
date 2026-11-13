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

public final class PpqhPromptDialog {

    private PpqhPromptDialog() {
    }

    public static void ppqhShow(Node ppqhAnyNodeInWindow, String ppqhTitle, String ppqhMessage, String ppqhFieldLabel, String ppqhInitial,
                            String ppqhConfirmText, Function<String, String> ppqhValidate, Consumer<String> ppqhOnConfirm) {
        PpqhAppRoot ppqhRoot = PpqhAppRoot.ppqhOf(ppqhAnyNodeInWindow);
        if (ppqhRoot == null) {
            throw new IllegalStateException("PromptDialog necesita una ventana con AppRoot");
        }
        StackPane ppqhLayer = ppqhRoot.ppqhDialogLayer();
        Node ppqhPreviousFocus = ppqhRoot.getScene() == null ? null : ppqhRoot.getScene().getFocusOwner();

        Region ppqhScrim = new Region();
        ppqhScrim.getStyleClass().add("sf-scrim");

        Label ppqhTitleLabel = new Label(ppqhTitle);
        ppqhTitleLabel.getStyleClass().addAll("title-s", "ink");
        ppqhTitleLabel.setWrapText(true);
        Label ppqhMessageLabel = new Label(ppqhMessage);
        ppqhMessageLabel.getStyleClass().addAll("body-s", "ink-muted");
        ppqhMessageLabel.setWrapText(true);
        PpqhSfTextField ppqhField = new PpqhSfTextField(ppqhFieldLabel);
        ppqhField.setText(ppqhInitial == null ? "" : ppqhInitial);

        PpqhSfButton ppqhCancel = new PpqhSfButton("Cancelar", PpqhSfButton.PpqhVariant.GHOST);
        PpqhSfButton ppqhConfirm = new PpqhSfButton(ppqhConfirmText, PpqhSfButton.PpqhVariant.ACCENT);
        HBox ppqhActions = new HBox(12, ppqhCancel, ppqhConfirm);
        ppqhActions.setAlignment(Pos.CENTER_RIGHT);

        VBox ppqhDialog = new VBox(ppqhTitleLabel, ppqhMessageLabel, ppqhField, ppqhActions);
        ppqhDialog.getStyleClass().add("sf-dialog");
        ppqhDialog.setMaxWidth(440);
        ppqhDialog.setMaxHeight(Region.USE_PREF_SIZE);
        ppqhDialog.setAccessibleRole(javafx.scene.AccessibleRole.NODE);
        ppqhDialog.setAccessibleText(ppqhTitle);

        Runnable ppqhClose = () -> {
            ppqhLayer.getChildren().clear();
            ppqhLayer.setVisible(false);
            if (ppqhPreviousFocus != null) {
                ppqhPreviousFocus.requestFocus();
            }
        };
        Runnable ppqhSubmit = () -> {
            String ppqhValue = ppqhField.getText() == null ? "" : ppqhField.getText().trim();
            String ppqhProblem = ppqhValidate.apply(ppqhValue);
            if (ppqhProblem != null) {
                ppqhField.setError(ppqhProblem);
                return;
            }
            ppqhClose.run();
            ppqhOnConfirm.accept(ppqhValue);
        };
        ppqhCancel.setOnAction(ppqhE -> ppqhClose.run());
        ppqhConfirm.setOnAction(ppqhE -> ppqhSubmit.run());
        ppqhField.getInput().setOnAction(ppqhE -> ppqhSubmit.run());
        ppqhField.textProperty().addListener((ppqhObs, ppqhOld, ppqhText) -> ppqhField.setError(""));
        ppqhLayer.setOnKeyPressed(ppqhE -> {
            if (ppqhE.getCode() == KeyCode.ESCAPE) {
                ppqhClose.run();
                ppqhE.consume();
            }
        });

        ppqhLayer.getChildren().setAll(ppqhScrim, ppqhDialog);
        ppqhLayer.setVisible(true);
        PpqhMotion.ppqhEnter(ppqhDialog, PpqhMotion.PPQH_SLOW, 12);
        ppqhField.requestFocus();
        ppqhField.getInput().selectAll();
    }
}
