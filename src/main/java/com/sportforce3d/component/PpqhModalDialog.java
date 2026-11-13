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

public final class PpqhModalDialog {

    private final StackPane ppqhLayer;
    private final Node ppqhPreviousFocus;
    private final PpqhSfButton ppqhConfirm;
    private final PpqhSfButton ppqhCancel;
    private final Label ppqhError = new Label();

    private PpqhModalDialog(StackPane ppqhLayer, Node ppqhPreviousFocus, PpqhSfButton ppqhConfirm, PpqhSfButton ppqhCancel) {
        this.ppqhLayer = ppqhLayer;
        this.ppqhPreviousFocus = ppqhPreviousFocus;
        this.ppqhConfirm = ppqhConfirm;
        this.ppqhCancel = ppqhCancel;
    }

    public enum PpqhStyle { ACCENT, PRIMARY, DANGER }

    public static PpqhModalDialog ppqhShow(Node ppqhAnyNodeInWindow, String ppqhTitle, String ppqhMessage, Node ppqhContent,
                                   String ppqhConfirmText, PpqhStyle ppqhStyle, Consumer<PpqhModalDialog> ppqhOnConfirm) {
        PpqhAppRoot ppqhRoot = PpqhAppRoot.ppqhOf(ppqhAnyNodeInWindow);
        if (ppqhRoot == null) {
            throw new IllegalStateException("ModalDialog necesita una ventana con AppRoot");
        }
        StackPane ppqhLayer = ppqhRoot.ppqhDialogLayer();
        Node ppqhPreviousFocus = ppqhRoot.getScene() == null ? null : ppqhRoot.getScene().getFocusOwner();

        Region ppqhScrim = new Region();
        ppqhScrim.getStyleClass().add("sf-scrim");
        Label ppqhTitleLabel = new Label(ppqhTitle);
        ppqhTitleLabel.getStyleClass().addAll("title-s", "ink");
        ppqhTitleLabel.setWrapText(true);
        VBox ppqhDialog = new VBox(ppqhTitleLabel);
        if (ppqhMessage != null && !ppqhMessage.isBlank()) {
            Label ppqhMessageLabel = new Label(ppqhMessage);
            ppqhMessageLabel.getStyleClass().addAll("body-s", "ink-muted");
            ppqhMessageLabel.setWrapText(true);
            ppqhDialog.getChildren().add(ppqhMessageLabel);
        }

        PpqhSfButton ppqhCancel = new PpqhSfButton("Cancelar", PpqhSfButton.PpqhVariant.GHOST);
        PpqhSfButton ppqhConfirm = new PpqhSfButton(ppqhConfirmText, switch (ppqhStyle) {
            case ACCENT -> PpqhSfButton.PpqhVariant.ACCENT;
            case PRIMARY -> PpqhSfButton.PpqhVariant.PRIMARY;
            case DANGER -> PpqhSfButton.PpqhVariant.DANGER;
        });
        HBox ppqhActions = new HBox(12, ppqhCancel, ppqhConfirm);
        ppqhActions.setAlignment(Pos.CENTER_RIGHT);

        PpqhModalDialog ppqhD = new PpqhModalDialog(ppqhLayer, ppqhPreviousFocus, ppqhConfirm, ppqhCancel);
        ppqhD.ppqhError.getStyleClass().addAll("body-s", "danger-text");
        ppqhD.ppqhError.setWrapText(true);
        ppqhD.ppqhError.setVisible(false);
        ppqhD.ppqhError.managedProperty().bind(ppqhD.ppqhError.visibleProperty());

        ppqhDialog.getChildren().addAll(ppqhContent, ppqhD.ppqhError, ppqhActions);
        ppqhDialog.getStyleClass().add("sf-dialog");
        ppqhDialog.setMaxWidth(480);
        ppqhDialog.setMaxHeight(Region.USE_PREF_SIZE);
        ppqhDialog.setAccessibleRole(javafx.scene.AccessibleRole.NODE);
        ppqhDialog.setAccessibleText(ppqhTitle);

        ppqhCancel.setOnAction(ppqhE -> ppqhD.ppqhClose());
        ppqhConfirm.setOnAction(ppqhE -> {
            ppqhD.ppqhSetError(null);
            ppqhOnConfirm.accept(ppqhD);
        });
        ppqhLayer.setOnKeyPressed(ppqhE -> {
            if (ppqhE.getCode() == KeyCode.ESCAPE && !ppqhConfirm.isDisabled()) {
                ppqhD.ppqhClose();
                ppqhE.consume();
            }
        });
        ppqhLayer.getChildren().setAll(ppqhScrim, ppqhDialog);
        ppqhLayer.setVisible(true);
        PpqhMotion.ppqhEnter(ppqhDialog, PpqhMotion.PPQH_SLOW, 12);
        ppqhCancel.requestFocus();
        return ppqhD;
    }

    public void ppqhClose() {
        ppqhLayer.getChildren().clear();
        ppqhLayer.setVisible(false);
        if (ppqhPreviousFocus != null) {
            ppqhPreviousFocus.requestFocus();
        }
    }

    public void ppqhSetBusy(boolean ppqhBusy) {
        ppqhConfirm.setDisable(ppqhBusy);
        ppqhCancel.setDisable(ppqhBusy);
    }

    public void ppqhSetError(String ppqhMessage) {
        ppqhError.setText(ppqhMessage == null ? "" : ppqhMessage);
        ppqhError.setVisible(ppqhMessage != null && !ppqhMessage.isBlank());
    }

    public PpqhSfButton ppqhConfirmButton() {
        return ppqhConfirm;
    }
}
