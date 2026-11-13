package com.sportforce3d.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.Ikon;

public class PpqhEmptyState extends VBox {

    public PpqhEmptyState(Ikon ppqhIcon, String ppqhTitle, String ppqhMessage, Button ppqhAction) {
        getStyleClass().add("sf-empty");

        Label ppqhTitleLabel = new Label(ppqhTitle);
        ppqhTitleLabel.getStyleClass().addAll("title-s", "ink");
        ppqhTitleLabel.setWrapText(true);
        ppqhTitleLabel.setTextAlignment(TextAlignment.CENTER);
        ppqhTitleLabel.setAlignment(Pos.CENTER);

        Label ppqhMessageLabel = new Label(ppqhMessage);
        ppqhMessageLabel.getStyleClass().addAll("body-s", "ink-muted");
        ppqhMessageLabel.setWrapText(true);
        ppqhMessageLabel.setTextAlignment(TextAlignment.CENTER);
        ppqhMessageLabel.setMaxWidth(420);
        ppqhMessageLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(PpqhIcons.ppqhOf(ppqhIcon, 48), ppqhTitleLabel, ppqhMessageLabel);
        if (ppqhAction != null) {
            getChildren().add(ppqhAction);
        }

        parentProperty().addListener((ppqhObs, ppqhOld, ppqhParent) -> {
            prefWidthProperty().unbind();
            if (ppqhParent instanceof FlowPane ppqhFlow) {
                prefWidthProperty().bind(ppqhFlow.widthProperty().subtract(ppqhFlow.getPadding().getLeft() + ppqhFlow.getPadding().getRight()));
            } else {
                setPrefWidth(USE_COMPUTED_SIZE);
            }
        });
    }

    public PpqhEmptyState(Ikon ppqhIcon, String ppqhTitle, String ppqhMessage) {
        this(ppqhIcon, ppqhTitle, ppqhMessage, null);
    }

}
