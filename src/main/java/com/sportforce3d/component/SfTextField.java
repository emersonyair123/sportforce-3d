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

/**
 * Campo con etiqueta arriba (estilo label, mayúsculas), ayuda y error debajo.
 * Con {@code password=true} usa un PasswordField. Si hay error, la ayuda se oculta.
 */
public class SfTextField extends VBox {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    private final TextField input;
    private final Label hint = new Label();
    private final Label error = new Label();

    public SfTextField(@NamedArg("label") String labelText,
                       @NamedArg(value = "password", defaultValue = "false") boolean password) {
        getStyleClass().add("sf-field");

        Label label = new Label(labelText == null ? "" : labelText.toUpperCase(Locale.forLanguageTag("es-PE")));
        label.getStyleClass().add("sf-field-label");

        input = password ? new PasswordField() : new TextField();
        input.getStyleClass().add("sf-input");
        input.setAccessibleText(labelText);
        label.setLabelFor(input);

        error.getStyleClass().add("sf-field-error");
        error.setWrapText(true);
        error.setGraphic(Icons.of(Feather.ALERT_CIRCLE, 14));
        error.managedProperty().bind(error.textProperty().isNotEmpty());
        error.visibleProperty().bind(error.managedProperty());

        hint.getStyleClass().add("sf-field-hint");
        hint.setWrapText(true);
        hint.managedProperty().bind(hint.textProperty().isNotEmpty().and(error.textProperty().isEmpty()));
        hint.visibleProperty().bind(hint.managedProperty());

        error.textProperty().addListener((obs, old, text) -> {
            boolean hasError = text != null && !text.isEmpty();
            pseudoClassStateChanged(ERROR, hasError);
            input.setAccessibleHelp(hasError ? text : hint.getText());
        });

        getChildren().addAll(label, input, hint, error);
    }

    public SfTextField(String labelText) {
        this(labelText, false);
    }

    public TextField getInput() {
        return input;
    }

    public StringProperty textProperty() {
        return input.textProperty();
    }

    public String getText() {
        return input.getText();
    }

    public void setText(String text) {
        input.setText(text);
    }

    public String getPromptText() {
        return input.getPromptText();
    }

    public void setPromptText(String text) {
        input.setPromptText(text);
    }

    public StringProperty hintProperty() {
        return hint.textProperty();
    }

    public String getHint() {
        return hint.getText();
    }

    public void setHint(String text) {
        hint.setText(text);
    }

    /** Texto de error; vacío lo oculta. Se puede enlazar a una propiedad del ViewModel. */
    public StringProperty errorProperty() {
        return error.textProperty();
    }

    public String getError() {
        return error.getText();
    }

    public void setError(String text) {
        error.setText(text == null ? "" : text);
    }

    /** Contenido como char[] (para contraseñas): el service lo limpia después de usarlo. */
    public char[] getChars() {
        CharSequence chars = input.getCharacters();
        char[] out = new char[chars.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = chars.charAt(i);
        }
        return out;
    }

    public boolean isMonospace() {
        return input.getStyleClass().contains("code");
    }

    public void setMonospace(boolean mono) {
        input.getStyleClass().remove("code");
        if (mono) {
            input.getStyleClass().add("code");
        }
    }

    @Override
    public void requestFocus() {
        input.requestFocus();
    }
}
