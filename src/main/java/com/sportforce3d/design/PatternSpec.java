package com.sportforce3d.design;

public record PatternSpec(PatternType type, String color, double width) {

    public static final double MIN_WIDTH = 0.01;
    public static final double MAX_WIDTH = 0.15;

    public static final PatternSpec PLAIN = new PatternSpec(PatternType.LISO, "#F2F5F8", 0.04);

    public PatternSpec {
        type = type == null ? PatternType.LISO : type;
        color = color == null ? "#F2F5F8" : color.toUpperCase();
        width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width));
    }

    public PatternSpec withType(PatternType t) {
        return new PatternSpec(t, color, width);
    }

    public PatternSpec withColor(String c) {
        return new PatternSpec(type, c, width);
    }

    public PatternSpec withWidth(double w) {
        return new PatternSpec(type, color, w);
    }
}
