package com.sportforce3d.component;

import javafx.beans.NamedArg;
import javafx.scene.control.Label;
import org.kordamp.ikonli.Ikon;

import java.util.Locale;

public class PpqhSfBadge extends Label {

    public enum PpqhKind { NEUTRAL, NEW, BRAND, SALE, SUCCESS, WARNING, DANGER }

    private PpqhKind ppqhKind;

    public PpqhSfBadge(@NamedArg("text") String ppqhText,
                   @NamedArg(value = "kind", defaultValue = "NEUTRAL") PpqhKind ppqhKind) {
        super(ppqhText == null ? "" : ppqhText.toUpperCase(Locale.forLanguageTag("es-PE")));
        setKind(ppqhKind);
        setMinWidth(USE_PREF_SIZE);
    }

    public PpqhSfBadge(String ppqhText) {
        this(ppqhText, PpqhKind.NEUTRAL);
    }

    public PpqhSfBadge ppqhWithIcon(Ikon ppqhIkon) {
        setGraphic(PpqhIcons.ppqhOf(ppqhIkon, 12));
        setGraphicTextGap(4);
        return this;
    }

    public PpqhKind getKind() {
        return ppqhKind;
    }

    public void setKind(PpqhKind ppqhKind) {
        this.ppqhKind = ppqhKind;
        getStyleClass().setAll("sf-badge");
        if (ppqhKind != PpqhKind.NEUTRAL) {
            getStyleClass().add("sf-badge-" + ppqhKind.name().toLowerCase(Locale.ROOT));
        }
    }
}
