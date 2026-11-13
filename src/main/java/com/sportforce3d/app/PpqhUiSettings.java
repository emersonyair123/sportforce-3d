package com.sportforce3d.app;

import com.sportforce3d.component.PpqhMotion;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.prefs.Preferences;

public class PpqhUiSettings {

    private static final String PPQH_REDUCE_MOTION = "reduceMotion";
    private static final String PPQH_SIDEBAR_COLLAPSED = "sidebarCollapsed";

    private final Preferences ppqhPrefs;
    private final BooleanProperty ppqhSidebarCollapsed;

    public PpqhUiSettings(Preferences ppqhPrefs) {
        this.ppqhPrefs = ppqhPrefs;
        PpqhMotion.reducedProperty().set(ppqhPrefs.getBoolean(PPQH_REDUCE_MOTION, false));
        PpqhMotion.reducedProperty().addListener((ppqhObs, ppqhOld, ppqhV) -> ppqhPrefs.putBoolean(PPQH_REDUCE_MOTION, ppqhV));
        ppqhSidebarCollapsed = new SimpleBooleanProperty(ppqhPrefs.getBoolean(PPQH_SIDEBAR_COLLAPSED, false));
        ppqhSidebarCollapsed.addListener((ppqhObs, ppqhOld, ppqhV) -> ppqhPrefs.putBoolean(PPQH_SIDEBAR_COLLAPSED, ppqhV));
    }

    public static PpqhUiSettings ppqhForUser() {
        return new PpqhUiSettings(Preferences.userRoot().node("com/sportforce3d"));
    }

    public BooleanProperty reduceMotionProperty() {
        return PpqhMotion.reducedProperty();
    }

    public BooleanProperty sidebarCollapsedProperty() {
        return ppqhSidebarCollapsed;
    }
}
