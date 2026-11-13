package com.sportforce3d.component;

import javafx.beans.NamedArg;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.Ikon;

import java.util.Locale;

public class SfButton extends Button {

    public enum Variant { ACCENT, PRIMARY, GHOST, DANGER, ICON }

    public enum Size { SM, MD, LG }

    private Variant variant = Variant.PRIMARY;
    private Size size = Size.MD;

    public SfButton() {
        this("", Variant.PRIMARY, Size.MD);
    }

    public SfButton(@NamedArg("text") String text,
                    @NamedArg(value = "variant", defaultValue = "PRIMARY") Variant variant,
                    @NamedArg(value = "size", defaultValue = "MD") Size size) {
        super(size == Size.LG ? upper(text) : text);
        this.variant = variant;
        this.size = size;
        refreshStyle();
        setMnemonicParsing(false);
    }

    public SfButton(String text, Variant variant) {
        this(text, variant, Size.MD);
    }

    public static SfButton icon(Ikon ikon, String accessibleText) {
        SfButton button = new SfButton("", Variant.ICON, Size.SM);
        button.setGraphic(Icons.of(ikon, Icons.MD));
        button.setAccessibleText(accessibleText);
        button.setTooltip(new Tooltip(accessibleText));
        return button;
    }

    public SfButton withIcon(Ikon ikon) {
        setGraphic(Icons.of(ikon, size == Size.SM ? Icons.SM : Icons.MD));
        return this;
    }

    public SfButton comingSoon() {
        setDisable(true);
        SfBadge badge = new SfBadge("Próximamente");
        setGraphic(badge);
        setContentDisplay(ContentDisplay.RIGHT);
        setAccessibleHelp("Próximamente");
        return this;
    }

    public Variant getVariant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
        refreshStyle();
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
        if (size == Size.LG) {
            setText(upper(getText()));
        }
        refreshStyle();
    }

    private void refreshStyle() {
        getStyleClass().setAll("sf-btn",
                "sf-btn-" + variant.name().toLowerCase(Locale.ROOT),
                "sf-btn-" + size.name().toLowerCase(Locale.ROOT));
    }

    private static String upper(String text) {
        return text == null ? "" : text.toUpperCase(Locale.forLanguageTag("es-PE"));
    }
}
