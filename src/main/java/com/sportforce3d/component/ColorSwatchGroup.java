package com.sportforce3d.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Fila de ColorSwatch + campo HEX (estilo code). El valor elegido está en {@link #valueProperty()}. */
public class ColorSwatchGroup extends VBox {

    /** Paleta del probador de referencia (design/referencia/frente.png). */
    public static final Map<String, Color> DEFAULT_PALETTE = palette();

    private static final Pattern HEX = Pattern.compile("^#?[0-9A-Fa-f]{6}$");

    private final ObjectProperty<Color> value = new SimpleObjectProperty<>(this, "value");
    private final List<ColorSwatch> swatches = new ArrayList<>();
    private final SfTextField hexField = new SfTextField("HEX");

    public ColorSwatchGroup() {
        this(DEFAULT_PALETTE);
    }

    public ColorSwatchGroup(Map<String, Color> colors) {
        getStyleClass().add("sf-swatch-group");
        FlowPane row = new FlowPane(8, 8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefWrapLength(400);
        colors.forEach((name, color) -> {
            ColorSwatch swatch = new ColorSwatch(color, name);
            swatch.setOnAction(e -> value.set(color));
            swatches.add(swatch);
            row.getChildren().add(swatch);
        });

        hexField.setMonospace(true);
        hexField.setMaxWidth(160);
        hexField.setPromptText("#1F5FFF");
        hexField.getInput().setOnAction(e -> applyHex());
        hexField.getInput().focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                applyHex();
            }
        });

        value.addListener((obs, old, color) -> {
            for (ColorSwatch s : swatches) {
                s.setSelected(color != null && s.getColor().equals(color));
            }
            hexField.setText(color == null ? "" : ColorSwatch.toHex(color));
            hexField.setError("");
        });

        getChildren().addAll(row, hexField);
        if (!colors.isEmpty()) {
            value.set(colors.values().iterator().next());
        }
    }

    private void applyHex() {
        String text = hexField.getText() == null ? "" : hexField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        if (!HEX.matcher(text).matches()) {
            hexField.setError("Usa 6 dígitos hexadecimales, p. ej. #1F5FFF.");
            return;
        }
        value.set(Color.web(text.startsWith("#") ? text : "#" + text));
    }

    public ObjectProperty<Color> valueProperty() {
        return value;
    }

    public Color getValue() {
        return value.get();
    }

    public void setValue(Color color) {
        value.set(color);
    }

    private static Map<String, Color> palette() {
        Map<String, Color> map = new LinkedHashMap<>();
        map.put("Azul eléctrico", Color.web("#1F5FFF"));
        map.put("Verde neón", Color.web("#39FF88"));
        map.put("Blanco", Color.web("#F2F5F8"));
        map.put("Negro", Color.web("#0B0E13"));
        map.put("Rojo", Color.web("#D7192A"));
        map.put("Amarillo", Color.web("#FFD400"));
        map.put("Morado", Color.web("#7B2CBF"));
        map.put("Verde bosque", Color.web("#0E7C3A"));
        map.put("Naranja", Color.web("#FF7A00"));
        map.put("Celeste", Color.web("#7CC4FF"));
        return java.util.Collections.unmodifiableMap(map);
    }
}
