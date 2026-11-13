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

public class PpqhLogo extends HBox {

    public static final String PPQH_WORDMARK = "SPORTFORCE";

    public PpqhLogo(@NamedArg(value = "fontSize", defaultValue = "26") double ppqhFontSize) {
        Text ppqhWord = new Text(PPQH_WORDMARK + " ");
        ppqhWord.getStyleClass().add("sf-logo-word");
        ppqhWord.setStyle("-fx-font-size: " + ppqhFontSize + "px;");
        Text ppqhThreeD = new Text("3D");
        ppqhThreeD.getStyleClass().add("sf-logo-3d");
        ppqhThreeD.setStyle("-fx-font-size: " + ppqhFontSize + "px;");
        TextFlow ppqhFlow = new TextFlow(ppqhWord, ppqhThreeD);
        setAlignment(Pos.CENTER_LEFT);
        getChildren().add(ppqhFlow);
        setAccessibleText("SPORTFORCE 3D");
    }

    public static Image ppqhMonogram(int ppqhSize) {
        Rectangle ppqhBg = new Rectangle(ppqhSize, ppqhSize);
        ppqhBg.setArcWidth(ppqhSize * 0.3);
        ppqhBg.setArcHeight(ppqhSize * 0.3);
        ppqhBg.setFill(Color.web("#0b0e13"));

        Font ppqhFont = Font.font("Barlow Condensed ExtraBold", ppqhSize * 0.56);
        Text ppqhS = new Text("S");
        ppqhS.setFont(ppqhFont);
        ppqhS.setFill(Color.web("#f2f5f8"));
        Text ppqhF = new Text("F");
        ppqhF.setFont(ppqhFont);
        ppqhF.setFill(Color.web("#39ff88"));
        HBox ppqhLetters = new HBox(ppqhS, ppqhF);
        ppqhLetters.setAlignment(Pos.CENTER);

        StackPane ppqhPane = new StackPane(ppqhBg, ppqhLetters);
        ppqhPane.setStyle("-fx-background-color: transparent;");
        SnapshotParameters ppqhParams = new SnapshotParameters();
        ppqhParams.setFill(Color.TRANSPARENT);
        return ppqhPane.snapshot(ppqhParams, null);
    }
}
