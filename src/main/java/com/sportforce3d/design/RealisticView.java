package com.sportforce3d.design;

import java.util.List;
import java.util.Map;

public record RealisticView(RealisticSpec spec, Text name, Text back, Text chest, DesignElement crest,
                            List<String> notices) {
    public record Text(String value, String fill, String outline, String font) {
    }

    public RealisticView {
        notices = List.copyOf(notices);
    }

    public String modelKey() {
        return spec.modelKey();
    }

    public Map<String, String> zoneColors() {
        return spec.zoneColors();
    }
}
