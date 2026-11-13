package com.sportforce3d.component;

import javafx.beans.NamedArg;
import javafx.scene.control.Label;
import org.kordamp.ikonli.Ikon;

import java.util.Locale;

public class SfBadge extends Label {

    public enum Kind { NEUTRAL, NEW, BRAND, SALE, SUCCESS, WARNING, DANGER }

    private Kind kind;

    public SfBadge(@NamedArg("text") String text,
                   @NamedArg(value = "kind", defaultValue = "NEUTRAL") Kind kind) {
        super(text == null ? "" : text.toUpperCase(Locale.forLanguageTag("es-PE")));
        setKind(kind);
        setMinWidth(USE_PREF_SIZE);
    }

    public SfBadge(String text) {
        this(text, Kind.NEUTRAL);
    }

    public SfBadge withIcon(Ikon ikon) {
        setGraphic(Icons.of(ikon, 12));
        setGraphicTextGap(4);
        return this;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
        getStyleClass().setAll("sf-badge");
        if (kind != Kind.NEUTRAL) {
            getStyleClass().add("sf-badge-" + kind.name().toLowerCase(Locale.ROOT));
        }
    }
}
