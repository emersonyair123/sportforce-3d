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

public class SidebarNav extends VBox {

    public static final double EXPANDED_WIDTH = 248;
    public static final double COLLAPSED_WIDTH = 72;

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final ObjectProperty<String> selectedId = new SimpleObjectProperty<>(this, "selectedId");
    private final BooleanProperty collapsed = new SimpleBooleanProperty(this, "collapsed", false);
    private final Map<String, Button> items = new LinkedHashMap<>();
    private final VBox itemsBox = new VBox(4);
    private java.util.function.Consumer<String> onNavigate;

    public void setOnNavigate(java.util.function.Consumer<String> handler) {
        this.onNavigate = handler;
    }

    public SidebarNav() {
        getStyleClass().add("sf-sidebar");
        setMinHeight(0);
        ScrollPane scroll = new ScrollPane(itemsBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinHeight(0);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        SfButton toggle = SfButton.icon(Feather.CHEVRONS_LEFT, "Contraer menú");
        toggle.setOnAction(e -> collapsed.set(!collapsed.get()));
        collapsed.addListener((obs, was, now) -> {
            toggle.setGraphic(Icons.of(now ? Feather.CHEVRONS_RIGHT : Feather.CHEVRONS_LEFT, Icons.MD));
            toggle.setAccessibleText(now ? "Expandir menú" : "Contraer menú");
            toggle.getTooltip().setText(toggle.getAccessibleText());
            applyWidth();
        });

        getChildren().addAll(scroll, toggle);
        selectedId.addListener((obs, old, id) -> items.forEach((key, button) ->
                button.pseudoClassStateChanged(SELECTED, key.equals(id))));
        applyWidth();
    }

    public void addSection(String title) {
        Label label = new Label(title.toUpperCase(Locale.forLanguageTag("es-PE")));
        label.getStyleClass().add("sf-nav-section");
        label.managedProperty().bind(collapsed.not());
        label.visibleProperty().bind(collapsed.not());
        itemsBox.getChildren().add(label);
    }

    public Button addItem(String id, String text, Ikon icon, boolean available) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        HBox row = new HBox(12, Icons.of(icon, Icons.MD), label);
        row.setAlignment(Pos.CENTER_LEFT);
        if (!available) {
            SfBadge soon = new SfBadge("Pronto");
            soon.setAccessibleText("Próximamente");
            row.getChildren().add(soon);
            soon.managedProperty().bind(collapsed.not());
            soon.visibleProperty().bind(collapsed.not());
        }
        label.managedProperty().bind(collapsed.not());
        label.visibleProperty().bind(collapsed.not());

        Button button = new Button();
        button.getStyleClass().setAll("sf-nav-item");
        button.setGraphic(row);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAccessibleText(available ? text : text + ", próximamente");
        button.setTooltip(new Tooltip(text));
        button.setDisable(!available);
        button.setOnAction(e -> {
            selectedId.set(id);
            if (onNavigate != null) {
                onNavigate.accept(id);
            }
        });
        row.prefWidthProperty().bind(button.widthProperty().subtract(24));

        items.put(id, button);
        itemsBox.getChildren().add(button);
        return button;
    }

    public void setVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void applyWidth() {
        double w = collapsed.get() ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
        setMinWidth(w);
        setPrefWidth(w);
        setMaxWidth(w);
    }

    public ObjectProperty<String> selectedIdProperty() {
        return selectedId;
    }

    public String getSelectedId() {
        return selectedId.get();
    }

    public void select(String id) {
        selectedId.set(id);
    }

    public BooleanProperty collapsedProperty() {
        return collapsed;
    }

    public Button item(String id) {
        return items.get(id);
    }
}
