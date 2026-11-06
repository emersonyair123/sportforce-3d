package com.sportforce3d.component;

import javafx.beans.NamedArg;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Logotipo tipográfico provisional: "SPORTFORCE" en Barlow Condensed 800 (ink) + "3D" en accent-text.
 * El monograma "SF" se usa como icono de ventana.
 */
public class Logo extends HBox {

    public static final String WORDMARK = "SPORTFORCE";

    public Logo(@NamedArg(value = "fontSize", defaultValue = "26") double fontSize) {
        Text word = new Text(WORDMARK + " ");
        word.getStyleClass().add("sf-logo-word");
        word.setStyle("-fx-font-size: " + fontSize + "px;");
        Text threeD = new Text("3D");
        threeD.getStyleClass().add("sf-logo-3d");
        threeD.setStyle("-fx-font-size: " + fontSize + "px;");
        TextFlow flow = new TextFlow(word, threeD);
        setAlignment(Pos.CENTER_LEFT);
        getChildren().add(flow);
        setAccessibleText("SPORTFORCE 3D");
    }

    /**
     * Monograma "SF" (casi negro, S en blanco y F en verde) como imagen para el icono de la ventana.
     * Los colores son los fijos de design/emblemas/monograma-sf.svg. Debe llamarse en el hilo de JavaFX.
     */
    public static Image monogram(int size) {
        Rectangle bg = new Rectangle(size, size);
        bg.setArcWidth(size * 0.3);
        bg.setArcHeight(size * 0.3);
        bg.setFill(Color.web("#0b0e13"));

        Font font = Font.font("Barlow Condensed ExtraBold", size * 0.56);
        Text s = new Text("S");
        s.setFont(font);
        s.setFill(Color.web("#f2f5f8"));
        Text f = new Text("F");
        f.setFont(font);
        f.setFill(Color.web("#39ff88"));
        HBox letters = new HBox(s, f);
        letters.setAlignment(Pos.CENTER);

        StackPane pane = new StackPane(bg, letters);
        pane.setStyle("-fx-background-color: transparent;");
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return pane.snapshot(params, null);
    }
}
