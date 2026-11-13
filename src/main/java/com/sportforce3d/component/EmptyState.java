package com.sportforce3d.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.Ikon;

public class EmptyState extends VBox {

    public EmptyState(Ikon icon, String title, String message, Button action) {
        getStyleClass().add("sf-empty");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("title-s", "ink");
        titleLabel.setWrapText(true);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.setAlignment(Pos.CENTER);

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().addAll("body-s", "ink-muted");
        messageLabel.setWrapText(true);
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        messageLabel.setMaxWidth(420);
        messageLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(Icons.of(icon, 48), titleLabel, messageLabel);
        if (action != null) {
            getChildren().add(action);
        }

        parentProperty().addListener((obs, old, parent) -> {
            prefWidthProperty().unbind();
            if (parent instanceof FlowPane flow) {
                prefWidthProperty().bind(flow.widthProperty().subtract(flow.getPadding().getLeft() + flow.getPadding().getRight()));
            } else {
                setPrefWidth(USE_COMPUTED_SIZE);
            }
        });
    }

    public EmptyState(Ikon icon, String title, String message) {
        this(icon, title, message, null);
    }

}
