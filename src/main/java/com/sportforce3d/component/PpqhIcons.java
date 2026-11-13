package com.sportforce3d.component;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

public final class PpqhIcons {

    public static final int PPQH_SM = 16;
    public static final int PPQH_MD = 20;
    public static final int PPQH_LG = 24;

    private PpqhIcons() {
    }

    public static FontIcon ppqhOf(Ikon ppqhIkon, int ppqhSize) {
        FontIcon ppqhIcon = new FontIcon(ppqhIkon);
        ppqhIcon.setIconSize(ppqhSize);
        ppqhIcon.getStyleClass().add("sf-icon");
        return ppqhIcon;
    }

    public static FontIcon ppqhOf(Ikon ppqhIkon) {
        return ppqhOf(ppqhIkon, PPQH_MD);
    }
}
