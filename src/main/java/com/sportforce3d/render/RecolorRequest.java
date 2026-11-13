package com.sportforce3d.render;

import com.sportforce3d.design.RealisticSpec;
import com.sportforce3d.design.RealisticView;
import com.sportforce3d.model.RealisticLook;
import com.sportforce3d.util.Validators;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public record RecolorRequest(String modelKey, Map<String, String> zoneColors, RealisticPattern pattern,
                             String patternColor, String name, Integer number, String textColor, Overlay overlay) {
    public record Overlay(RealisticView.Text name, RealisticView.Text back, RealisticView.Text chest,
                          ArgbImage crest, String crestKey) {

        boolean empty() {
            return name == null && back == null && chest == null && crest == null;
        }

        String key() {
            return text(name) + "|" + text(back) + "|" + text(chest) + "|" + (crest == null ? "-" : crestKey);
        }

        private static String text(RealisticView.Text t) {
            return t == null ? "-" : t.value() + "/" + t.fill() + "/" + t.outline() + "/" + t.font();
        }
    }

    public RecolorRequest(String modelKey, Map<String, String> zoneColors, RealisticPattern pattern,
                          String patternColor, String name, Integer number, String textColor) {
        this(modelKey, zoneColors, pattern, patternColor, name, number, textColor, null);
    }

    public static final int NAME_MAX = 12;
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");

    public RecolorRequest {
        if (modelKey == null || modelKey.isBlank()) {
            throw new IllegalArgumentException("Falta el modelo");
        }
        TreeMap<String, String> colors = new TreeMap<>();
        if (zoneColors != null) {
            zoneColors.forEach((zone, hex) -> colors.put(zone, hex(hex)));
        }
        zoneColors = Collections.unmodifiableMap(colors);
        pattern = pattern == null ? RealisticPattern.LISO : pattern;
        patternColor = pattern == RealisticPattern.LISO ? null : hex(patternColor);
        name = Validators.blankToNull(name);
        if (name != null) {
            name = name.toUpperCase(Locale.ROOT);
            if (name.codePointCount(0, name.length()) > NAME_MAX) {
                throw new IllegalArgumentException("El nombre admite hasta " + NAME_MAX + " caracteres");
            }
            if (Validators.plainText(name, false) != null) {
                throw new IllegalArgumentException(Validators.plainText(name, false));
            }
        }
        if (number != null && (number < 0 || number > 99)) {
            throw new IllegalArgumentException("El número va de 0 a 99");
        }
        textColor = textColor == null ? null : hex(textColor);
    }

    public static RecolorRequest of(String modelKey, Map<String, String> zoneColors) {
        return new RecolorRequest(modelKey, zoneColors, RealisticPattern.LISO, null, null, null, null);
    }

    public static RecolorRequest of(RealisticLook look) {
        RealisticPattern pattern = RealisticPattern.fromCode(look.pattern());
        return new RecolorRequest(look.modelKey(), look.zoneColors(), pattern,
                pattern == RealisticPattern.LISO ? null : look.patternColor(), null, null, null);
    }

    public static RecolorRequest of(String modelKey, RealisticSpec spec) {
        RealisticPattern pattern = RealisticPattern.fromCode(spec.pattern());
        return new RecolorRequest(modelKey, spec.zoneColors(), pattern,
                pattern == RealisticPattern.LISO ? null : spec.patternColor(), spec.name(), spec.number(), null);
    }

    public RecolorRequest withModel(String key) {
        return new RecolorRequest(key, zoneColors, pattern, patternColor, name, number, textColor, overlay);
    }

    public static RecolorRequest of(String modelKey, RealisticView view, ArgbImage crest, String crestKey) {
        RealisticSpec spec = view.spec();
        RealisticPattern pattern = RealisticPattern.fromCode(spec.pattern());
        return new RecolorRequest(modelKey, spec.zoneColors(), pattern,
                pattern == RealisticPattern.LISO ? null : spec.patternColor(), null, null, null,
                new Overlay(view.name(), view.back(), view.chest(), crest, crestKey));
    }

    public boolean hasOverlay() {
        return pattern != RealisticPattern.LISO || name != null || number != null
                || (overlay != null && !overlay.empty());
    }

    public String cacheKey() {
        StringBuilder sb = new StringBuilder(modelKey).append('|');
        zoneColors.forEach((z, c) -> sb.append(z).append('=').append(c).append(','));
        return sb.append('|').append(pattern.code()).append(':').append(patternColor)
                .append('|').append(name).append('|').append(number).append('|').append(textColor)
                .append('|').append(overlay == null ? "" : overlay.key()).toString();
    }

    static String hex(String value) {
        if (value == null || !HEX.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Color inválido: " + value);
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    static int rgb(String hex) {
        return Integer.parseInt(hex.substring(1), 16);
    }
}
