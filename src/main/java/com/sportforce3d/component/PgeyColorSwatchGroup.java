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

public class PgeyColorSwatchGroup extends VBox {
    public static final Map<String, Color> PGEY_DEFAULT_PALETTE = pgeyPalette();

    private static final Pattern PGEY_HEX = Pattern.compile("^#?[0-9A-Fa-f]{6}$");

    private final ObjectProperty<Color> pgeyValue = new SimpleObjectProperty<>(this, "value");
    private final List<PgeyColorSwatch> pgeySwatches = new ArrayList<>();
    private final PpqhSfTextField pgeyHexField = new PpqhSfTextField("HEX");

    public PgeyColorSwatchGroup() {
        this(PGEY_DEFAULT_PALETTE);
    }

    public PgeyColorSwatchGroup(Map<String, Color> pgeyColors) {
        getStyleClass().add("sf-swatch-group");
        FlowPane pgeyRow = new FlowPane(8, 8);
        pgeyRow.setAlignment(Pos.CENTER_LEFT);
        pgeyRow.setPrefWrapLength(400);
        pgeyColors.forEach((pgeyName, pgeyColor) -> {
            PgeyColorSwatch pgeySwatch = new PgeyColorSwatch(pgeyColor, pgeyName);
            pgeySwatch.setOnAction(pgeyE -> pgeyValue.set(pgeyColor));
            pgeySwatches.add(pgeySwatch);
            pgeyRow.getChildren().add(pgeySwatch);
        });

        pgeyHexField.setMonospace(true);
        pgeyHexField.setMaxWidth(160);
        pgeyHexField.setPromptText("#1F5FFF");
        pgeyHexField.getInput().setOnAction(pgeyE -> pgeyApplyHex());
        pgeyHexField.getInput().focusedProperty().addListener((pgeyObs, pgeyWas, pgeyFocused) -> {
            if (!pgeyFocused) {
                pgeyApplyHex();
            }
        });

        pgeyValue.addListener((pgeyObs, pgeyOld, pgeyColor) -> {
            for (PgeyColorSwatch pgeyS : pgeySwatches) {
                pgeyS.setSelected(pgeyColor != null && pgeyS.getColor().equals(pgeyColor));
            }
            pgeyHexField.setText(pgeyColor == null ? "" : PgeyColorSwatch.pgeyToHex(pgeyColor));
            pgeyHexField.setError("");
        });

        getChildren().addAll(pgeyRow, pgeyHexField);
        if (!pgeyColors.isEmpty()) {
            pgeyValue.set(pgeyColors.values().iterator().next());
        }
    }

    private void pgeyApplyHex() {
        String pgeyText = pgeyHexField.getText() == null ? "" : pgeyHexField.getText().trim();
        if (pgeyText.isEmpty()) {
            return;
        }
        if (!PGEY_HEX.matcher(pgeyText).matches()) {
            pgeyHexField.setError("Usa 6 dígitos hexadecimales, p. ej. #1F5FFF.");
            return;
        }
        pgeyValue.set(Color.web(pgeyText.startsWith("#") ? pgeyText : "#" + pgeyText));
    }

    public ObjectProperty<Color> valueProperty() {
        return pgeyValue;
    }

    public Color getValue() {
        return pgeyValue.get();
    }

    public void setValue(Color pgeyColor) {
        pgeyValue.set(pgeyColor);
    }

    private static Map<String, Color> pgeyPalette() {
        Map<String, Color> pgeyMap = new LinkedHashMap<>();
        pgeyMap.put("Azul eléctrico", Color.web("#1F5FFF"));
        pgeyMap.put("Verde neón", Color.web("#39FF88"));
        pgeyMap.put("Blanco", Color.web("#F2F5F8"));
        pgeyMap.put("Negro", Color.web("#0B0E13"));
        pgeyMap.put("Rojo", Color.web("#D7192A"));
        pgeyMap.put("Amarillo", Color.web("#FFD400"));
        pgeyMap.put("Morado", Color.web("#7B2CBF"));
        pgeyMap.put("Verde bosque", Color.web("#0E7C3A"));
        pgeyMap.put("Naranja", Color.web("#FF7A00"));
        pgeyMap.put("Celeste", Color.web("#7CC4FF"));
        return java.util.Collections.unmodifiableMap(pgeyMap);
    }
}
