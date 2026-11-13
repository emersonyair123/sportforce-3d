package com.sportforce3d.component;

import javafx.beans.NamedArg;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.Ikon;

import java.util.Locale;

public class PpqhSfButton extends Button {

    public enum PpqhVariant { ACCENT, PRIMARY, GHOST, DANGER, ICON }

    public enum PpqhSize { SM, MD, LG }

    private PpqhVariant ppqhVariant = PpqhVariant.PRIMARY;
    private PpqhSize ppqhSize = PpqhSize.MD;

    public PpqhSfButton() {
        this("", PpqhVariant.PRIMARY, PpqhSize.MD);
    }

    public PpqhSfButton(@NamedArg("text") String ppqhText,
                    @NamedArg(value = "variant", defaultValue = "PRIMARY") PpqhVariant ppqhVariant,
                    @NamedArg(value = "size", defaultValue = "MD") PpqhSize ppqhSize) {
        super(ppqhSize == PpqhSize.LG ? ppqhUpper(ppqhText) : ppqhText);
        this.ppqhVariant = ppqhVariant;
        this.ppqhSize = ppqhSize;
        ppqhRefreshStyle();
        setMnemonicParsing(false);
    }

    public PpqhSfButton(String ppqhText, PpqhVariant ppqhVariant) {
        this(ppqhText, ppqhVariant, PpqhSize.MD);
    }

    public static PpqhSfButton ppqhIcon(Ikon ppqhIkon, String ppqhAccessibleText) {
        PpqhSfButton ppqhButton = new PpqhSfButton("", PpqhVariant.ICON, PpqhSize.SM);
        ppqhButton.setGraphic(PpqhIcons.ppqhOf(ppqhIkon, PpqhIcons.PPQH_MD));
        ppqhButton.setAccessibleText(ppqhAccessibleText);
        ppqhButton.setTooltip(new Tooltip(ppqhAccessibleText));
        return ppqhButton;
    }

    public PpqhSfButton ppqhWithIcon(Ikon ppqhIkon) {
        setGraphic(PpqhIcons.ppqhOf(ppqhIkon, ppqhSize == PpqhSize.SM ? PpqhIcons.PPQH_SM : PpqhIcons.PPQH_MD));
        return this;
    }

    public PpqhSfButton ppqhComingSoon() {
        setDisable(true);
        PpqhSfBadge ppqhBadge = new PpqhSfBadge("Próximamente");
        setGraphic(ppqhBadge);
        setContentDisplay(ContentDisplay.RIGHT);
        setAccessibleHelp("Próximamente");
        return this;
    }

    public PpqhVariant getVariant() {
        return ppqhVariant;
    }

    public void setVariant(PpqhVariant ppqhVariant) {
        this.ppqhVariant = ppqhVariant;
        ppqhRefreshStyle();
    }

    public PpqhSize getSize() {
        return ppqhSize;
    }

    public void setSize(PpqhSize ppqhSize) {
        this.ppqhSize = ppqhSize;
        if (ppqhSize == PpqhSize.LG) {
            setText(ppqhUpper(getText()));
        }
        ppqhRefreshStyle();
    }

    private void ppqhRefreshStyle() {
        getStyleClass().setAll("sf-btn",
                "sf-btn-" + ppqhVariant.name().toLowerCase(Locale.ROOT),
                "sf-btn-" + ppqhSize.name().toLowerCase(Locale.ROOT));
    }

    private static String ppqhUpper(String ppqhText) {
        return ppqhText == null ? "" : ppqhText.toUpperCase(Locale.forLanguageTag("es-PE"));
    }
}
