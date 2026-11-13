package com.sportforce3d.component;

import com.sportforce3d.model.HepyPage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.function.IntConsumer;

public class HepyPageNav extends HBox {

    private final PpqhSfButton hepyPrevious = new PpqhSfButton("Anterior", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
    private final PpqhSfButton hepyNext = new PpqhSfButton("Siguiente", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
    private final Label hepyLabel = new Label();
    private int hepyPage = 1;

    public HepyPageNav(IntConsumer hepyOnPage) {
        super(12);
        setAlignment(Pos.CENTER);
        hepyPrevious.ppqhWithIcon(Feather.CHEVRON_LEFT);
        hepyNext.ppqhWithIcon(Feather.CHEVRON_RIGHT);
        hepyNext.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        hepyLabel.getStyleClass().addAll("body-s", "ink-muted");
        hepyPrevious.setOnAction(hepyE -> hepyOnPage.accept(hepyPage - 1));
        hepyNext.setOnAction(hepyE -> hepyOnPage.accept(hepyPage + 1));
        getChildren().addAll(hepyPrevious, hepyLabel, hepyNext);
    }

    public void hepyShow(HepyPage<?> hepyResult) {
        hepyPage = hepyResult.hepyPage();
        hepyLabel.setText("Página " + hepyResult.hepyPage() + " de " + hepyResult.hepyTotalPages());
        hepyPrevious.setDisable(!hepyResult.hepyHasPrevious());
        hepyNext.setDisable(!hepyResult.hepyHasNext());
        boolean hepyMultiple = hepyResult.hepyTotalPages() > 1;
        setVisible(hepyMultiple);
        setManaged(hepyMultiple);
    }
}
