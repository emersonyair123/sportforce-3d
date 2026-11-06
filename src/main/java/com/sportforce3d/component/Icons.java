package com.sportforce3d.component;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/** Iconos Feather (trazo fino) de Ikonli. El color lo pone el CSS con -fx-icon-color. */
public final class Icons {

    public static final int SM = 16;
    public static final int MD = 20;
    public static final int LG = 24;

    private Icons() {
    }

    public static FontIcon of(Ikon ikon, int size) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(size);
        icon.getStyleClass().add("sf-icon");
        return icon;
    }

    public static FontIcon of(Ikon ikon) {
        return of(ikon, MD);
    }
}
