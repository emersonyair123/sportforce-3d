package com.sportforce3d.component;

import com.sportforce3d.model.Page;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.function.IntConsumer;

public class PageNav extends HBox {

    private final SfButton previous = new SfButton("Anterior", SfButton.Variant.GHOST, SfButton.Size.SM);
    private final SfButton next = new SfButton("Siguiente", SfButton.Variant.GHOST, SfButton.Size.SM);
    private final Label label = new Label();
    private int page = 1;

    public PageNav(IntConsumer onPage) {
        super(12);
        setAlignment(Pos.CENTER);
        previous.withIcon(Feather.CHEVRON_LEFT);
        next.withIcon(Feather.CHEVRON_RIGHT);
        next.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        label.getStyleClass().addAll("body-s", "ink-muted");
        previous.setOnAction(e -> onPage.accept(page - 1));
        next.setOnAction(e -> onPage.accept(page + 1));
        getChildren().addAll(previous, label, next);
    }

    public void show(Page<?> result) {
        page = result.page();
        label.setText("Página " + result.page() + " de " + result.totalPages());
        previous.setDisable(!result.hasPrevious());
        next.setDisable(!result.hasNext());
        boolean multiple = result.totalPages() > 1;
        setVisible(multiple);
        setManaged(multiple);
    }
}
