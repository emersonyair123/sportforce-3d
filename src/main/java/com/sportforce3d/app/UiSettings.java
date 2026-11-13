package com.sportforce3d.app;

import com.sportforce3d.component.Motion;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.prefs.Preferences;

public class UiSettings {

    private static final String REDUCE_MOTION = "reduceMotion";
    private static final String SIDEBAR_COLLAPSED = "sidebarCollapsed";

    private final Preferences prefs;
    private final BooleanProperty sidebarCollapsed;

    public UiSettings(Preferences prefs) {
        this.prefs = prefs;
        Motion.reducedProperty().set(prefs.getBoolean(REDUCE_MOTION, false));
        Motion.reducedProperty().addListener((obs, old, v) -> prefs.putBoolean(REDUCE_MOTION, v));
        sidebarCollapsed = new SimpleBooleanProperty(prefs.getBoolean(SIDEBAR_COLLAPSED, false));
        sidebarCollapsed.addListener((obs, old, v) -> prefs.putBoolean(SIDEBAR_COLLAPSED, v));
    }

    public static UiSettings forUser() {
        return new UiSettings(Preferences.userRoot().node("com/sportforce3d"));
    }

    public BooleanProperty reduceMotionProperty() {
        return Motion.reducedProperty();
    }

    public BooleanProperty sidebarCollapsedProperty() {
        return sidebarCollapsed;
    }
}
