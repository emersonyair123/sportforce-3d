package com.sportforce3d.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class PpqhSidebarNav extends VBox {

    public static final double PPQH_EXPANDED_WIDTH = 248;
    public static final double PPQH_COLLAPSED_WIDTH = 72;

    private static final PseudoClass PPQH_SELECTED = PseudoClass.getPseudoClass("selected");

    private final ObjectProperty<String> ppqhSelectedId = new SimpleObjectProperty<>(this, "selectedId");
    private final BooleanProperty ppqhCollapsed = new SimpleBooleanProperty(this, "collapsed", false);
    private final Map<String, Button> ppqhItems = new LinkedHashMap<>();
    private final VBox ppqhItemsBox = new VBox(4);
    private java.util.function.Consumer<String> ppqhOnNavigate;

    public void setOnNavigate(java.util.function.Consumer<String> ppqhHandler) {
        this.ppqhOnNavigate = ppqhHandler;
    }

    public PpqhSidebarNav() {
        getStyleClass().add("sf-sidebar");
        setMinHeight(0);
        ScrollPane ppqhScroll = new ScrollPane(ppqhItemsBox);
        ppqhScroll.setFitToWidth(true);
        ppqhScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        ppqhScroll.setMinHeight(0);
        VBox.setVgrow(ppqhScroll, Priority.ALWAYS);

        PpqhSfButton ppqhToggle = PpqhSfButton.ppqhIcon(Feather.CHEVRONS_LEFT, "Contraer menú");
        ppqhToggle.setOnAction(ppqhE -> ppqhCollapsed.set(!ppqhCollapsed.get()));
        ppqhCollapsed.addListener((ppqhObs, ppqhWas, ppqhNow) -> {
            ppqhToggle.setGraphic(PpqhIcons.ppqhOf(ppqhNow ? Feather.CHEVRONS_RIGHT : Feather.CHEVRONS_LEFT, PpqhIcons.PPQH_MD));
            ppqhToggle.setAccessibleText(ppqhNow ? "Expandir menú" : "Contraer menú");
            ppqhToggle.getTooltip().setText(ppqhToggle.getAccessibleText());
            ppqhApplyWidth();
        });

        getChildren().addAll(ppqhScroll, ppqhToggle);
        ppqhSelectedId.addListener((ppqhObs, ppqhOld, ppqhId) -> ppqhItems.forEach((ppqhKey, ppqhButton) ->
                ppqhButton.pseudoClassStateChanged(PPQH_SELECTED, ppqhKey.equals(ppqhId))));
        ppqhApplyWidth();
    }

    public void ppqhAddSection(String ppqhTitle) {
        Label ppqhLabel = new Label(ppqhTitle.toUpperCase(Locale.forLanguageTag("es-PE")));
        ppqhLabel.getStyleClass().add("sf-nav-section");
        ppqhLabel.managedProperty().bind(ppqhCollapsed.not());
        ppqhLabel.visibleProperty().bind(ppqhCollapsed.not());
        ppqhItemsBox.getChildren().add(ppqhLabel);
    }

    public Button ppqhAddItem(String ppqhId, String ppqhText, Ikon ppqhIcon, boolean ppqhAvailable) {
        Label ppqhLabel = new Label(ppqhText);
        ppqhLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(ppqhLabel, Priority.ALWAYS);
        HBox ppqhRow = new HBox(12, PpqhIcons.ppqhOf(ppqhIcon, PpqhIcons.PPQH_MD), ppqhLabel);
        ppqhRow.setAlignment(Pos.CENTER_LEFT);
        if (!ppqhAvailable) {
            PpqhSfBadge ppqhSoon = new PpqhSfBadge("Pronto");
            ppqhSoon.setAccessibleText("Próximamente");
            ppqhRow.getChildren().add(ppqhSoon);
            ppqhSoon.managedProperty().bind(ppqhCollapsed.not());
            ppqhSoon.visibleProperty().bind(ppqhCollapsed.not());
        }
        ppqhLabel.managedProperty().bind(ppqhCollapsed.not());
        ppqhLabel.visibleProperty().bind(ppqhCollapsed.not());

        Button ppqhButton = new Button();
        ppqhButton.getStyleClass().setAll("sf-nav-item");
        ppqhButton.setGraphic(ppqhRow);
        ppqhButton.setMaxWidth(Double.MAX_VALUE);
        ppqhButton.setAccessibleText(ppqhAvailable ? ppqhText : ppqhText + ", próximamente");
        ppqhButton.setTooltip(new Tooltip(ppqhText));
        ppqhButton.setDisable(!ppqhAvailable);
        ppqhButton.setOnAction(ppqhE -> {
            ppqhSelectedId.set(ppqhId);
            if (ppqhOnNavigate != null) {
                ppqhOnNavigate.accept(ppqhId);
            }
        });
        ppqhRow.prefWidthProperty().bind(ppqhButton.widthProperty().subtract(24));

        ppqhItems.put(ppqhId, ppqhButton);
        ppqhItemsBox.getChildren().add(ppqhButton);
        return ppqhButton;
    }

    public void ppqhSetVisible(Node ppqhNode, boolean ppqhVisible) {
        ppqhNode.setVisible(ppqhVisible);
        ppqhNode.setManaged(ppqhVisible);
    }

    private void ppqhApplyWidth() {
        double ppqhW = ppqhCollapsed.get() ? PPQH_COLLAPSED_WIDTH : PPQH_EXPANDED_WIDTH;
        setMinWidth(ppqhW);
        setPrefWidth(ppqhW);
        setMaxWidth(ppqhW);
    }

    public ObjectProperty<String> selectedIdProperty() {
        return ppqhSelectedId;
    }

    public String getSelectedId() {
        return ppqhSelectedId.get();
    }

    public void ppqhSelect(String ppqhId) {
        ppqhSelectedId.set(ppqhId);
    }

    public BooleanProperty collapsedProperty() {
        return ppqhCollapsed;
    }

    public Button ppqhItem(String ppqhId) {
        return ppqhItems.get(ppqhId);
    }
}
