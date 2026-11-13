package com.sportforce3d.component;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

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
