package com.sportforce3d.component;

import javafx.beans.NamedArg;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.Locale;

public class PpqhSfTextField extends VBox {

    private static final PseudoClass PPQH_ERROR = PseudoClass.getPseudoClass("error");

    private final TextField ppqhInput;
    private final Label ppqhHint = new Label();
    private final Label ppqhError = new Label();

    public PpqhSfTextField(@NamedArg("label") String ppqhLabelText,
                       @NamedArg(value = "password", defaultValue = "false") boolean ppqhPassword) {
        getStyleClass().add("sf-field");

        Label ppqhLabel = new Label(ppqhLabelText == null ? "" : ppqhLabelText.toUpperCase(Locale.forLanguageTag("es-PE")));
        ppqhLabel.getStyleClass().add("sf-field-label");

        ppqhInput = ppqhPassword ? new PasswordField() : new TextField();
        ppqhInput.getStyleClass().add("sf-input");
        ppqhInput.setAccessibleText(ppqhLabelText);
        ppqhLabel.setLabelFor(ppqhInput);

        ppqhError.getStyleClass().add("sf-field-error");
        ppqhError.setWrapText(true);
        ppqhError.setGraphic(PpqhIcons.ppqhOf(Feather.ALERT_CIRCLE, 14));
        ppqhError.managedProperty().bind(ppqhError.textProperty().isNotEmpty());
        ppqhError.visibleProperty().bind(ppqhError.managedProperty());

        ppqhHint.getStyleClass().add("sf-field-hint");
        ppqhHint.setWrapText(true);
        ppqhHint.managedProperty().bind(ppqhHint.textProperty().isNotEmpty().and(ppqhError.textProperty().isEmpty()));
        ppqhHint.visibleProperty().bind(ppqhHint.managedProperty());

        ppqhError.textProperty().addListener((ppqhObs, ppqhOld, ppqhText) -> {
            boolean ppqhHasError = ppqhText != null && !ppqhText.isEmpty();
            pseudoClassStateChanged(PPQH_ERROR, ppqhHasError);
            ppqhInput.setAccessibleHelp(ppqhHasError ? ppqhText : ppqhHint.getText());
        });

        getChildren().addAll(ppqhLabel, ppqhInput, ppqhHint, ppqhError);
    }

    public PpqhSfTextField(String ppqhLabelText) {
        this(ppqhLabelText, false);
    }

    public TextField getInput() {
        return ppqhInput;
    }

    public StringProperty textProperty() {
        return ppqhInput.textProperty();
    }

    public String getText() {
        return ppqhInput.getText();
    }

    public void setText(String ppqhText) {
        ppqhInput.setText(ppqhText);
    }

    public String getPromptText() {
        return ppqhInput.getPromptText();
    }

    public void setPromptText(String ppqhText) {
        ppqhInput.setPromptText(ppqhText);
    }

    public StringProperty hintProperty() {
        return ppqhHint.textProperty();
    }

    public String getHint() {
        return ppqhHint.getText();
    }

    public void setHint(String ppqhText) {
        ppqhHint.setText(ppqhText);
    }

    public StringProperty errorProperty() {
        return ppqhError.textProperty();
    }

    public String getError() {
        return ppqhError.getText();
    }

    public void setError(String ppqhText) {
        ppqhError.setText(ppqhText == null ? "" : ppqhText);
    }

    public char[] getChars() {
        CharSequence ppqhChars = ppqhInput.getCharacters();
        char[] ppqhOut = new char[ppqhChars.length()];
        for (int ppqhI = 0; ppqhI < ppqhOut.length; ppqhI++) {
            ppqhOut[ppqhI] = ppqhChars.charAt(ppqhI);
        }
        return ppqhOut;
    }

    public boolean isMonospace() {
        return ppqhInput.getStyleClass().contains("code");
    }

    public void setMonospace(boolean ppqhMono) {
        ppqhInput.getStyleClass().remove("code");
        if (ppqhMono) {
            ppqhInput.getStyleClass().add("code");
        }
    }

    @Override
    public void requestFocus() {
        ppqhInput.requestFocus();
    }
}
