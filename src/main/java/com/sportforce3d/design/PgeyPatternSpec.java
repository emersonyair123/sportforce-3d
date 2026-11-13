package com.sportforce3d.design;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PgeyPatternSpec(@JsonProperty("type") PgeyPatternType pgeyType, @JsonProperty("color") String pgeyColor, @JsonProperty("width") double pgeyWidth) {

    public static final double PGEY_MIN_WIDTH = 0.01;
    public static final double PGEY_MAX_WIDTH = 0.15;

    public static final PgeyPatternSpec PGEY_PLAIN = new PgeyPatternSpec(PgeyPatternType.LISO, "#F2F5F8", 0.04);

    public PgeyPatternSpec {
        pgeyType = pgeyType == null ? PgeyPatternType.LISO : pgeyType;
        pgeyColor = pgeyColor == null ? "#F2F5F8" : pgeyColor.toUpperCase();
        pgeyWidth = Math.max(PGEY_MIN_WIDTH, Math.min(PGEY_MAX_WIDTH, pgeyWidth));
    }

    public PgeyPatternSpec pgeyWithType(PgeyPatternType pgeyT) {
        return new PgeyPatternSpec(pgeyT, pgeyColor, pgeyWidth);
    }

    public PgeyPatternSpec pgeyWithColor(String pgeyC) {
        return new PgeyPatternSpec(pgeyType, pgeyC, pgeyWidth);
    }

    public PgeyPatternSpec pgeyWithWidth(double pgeyW) {
        return new PgeyPatternSpec(pgeyType, pgeyColor, pgeyW);
    }
}
