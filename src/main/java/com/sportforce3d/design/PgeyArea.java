package com.sportforce3d.design;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyUvRect;
import com.sportforce3d.three.model.PgeyUvLayout;
import com.sportforce3d.three.model.PgeyZone;

import java.util.Arrays;
import java.util.List;

public enum PgeyArea {
    JERSEY_FRONT("Pecho", HepyGarment.JERSEY, PgeyZone.JERSEY_BODY, PgeyUvLayout.PGEY_JERSEY_FRONT, 0.47, 0.58),
    JERSEY_BACK("Espalda", HepyGarment.JERSEY, PgeyZone.JERSEY_BODY, PgeyUvLayout.PGEY_JERSEY_BACK, 0.47, 0.58),
    SLEEVE_LEFT("Manga izquierda", HepyGarment.JERSEY, PgeyZone.JERSEY_SLEEVES, PgeyUvLayout.PGEY_JERSEY_SLEEVE_LEFT, 0.40, 0.17),
    SLEEVE_RIGHT("Manga derecha", HepyGarment.JERSEY, PgeyZone.JERSEY_SLEEVES, PgeyUvLayout.PGEY_JERSEY_SLEEVE_RIGHT, 0.40, 0.17),
    SHORTS_LEFT("Short · pierna izquierda", HepyGarment.SHORTS, PgeyZone.SHORTS_BASE, PgeyUvLayout.PGEY_SHORTS_LEFT_FRONT, 0.36, 0.35),
    SHORTS_RIGHT("Short · pierna derecha", HepyGarment.SHORTS, PgeyZone.SHORTS_BASE, PgeyUvLayout.PGEY_SHORTS_RIGHT_FRONT, 0.36, 0.35),
    SOCK_LEFT("Media izquierda · banda", HepyGarment.SOCKS, PgeyZone.SOCKS_BAND, PgeyUvLayout.PGEY_SOCK_BAND_LEFT, 0.37, 0.053),
    SOCK_RIGHT("Media derecha · banda", HepyGarment.SOCKS, PgeyZone.SOCKS_BAND, PgeyUvLayout.PGEY_SOCK_BAND_RIGHT, 0.37, 0.053),
    SHOE_SIDE("Zapatilla · costado", HepyGarment.SHOES, PgeyZone.SHOE_UPPER, PgeyUvLayout.PGEY_SHOE_UPPER, 0.285, 0.26);

    private final String pgeyLabel;
    private final HepyGarment pgeyGarment;
    private final PgeyZone pgeyZone;
    private final PgeyUvRect pgeyRect;
    private final double pgeyWidthMeters;
    private final double pgeyHeightMeters;

    PgeyArea(String pgeyLabel, HepyGarment pgeyGarment, PgeyZone pgeyZone, PgeyUvRect pgeyRect, double pgeyWidthMeters, double pgeyHeightMeters) {
        this.pgeyLabel = pgeyLabel;
        this.pgeyGarment = pgeyGarment;
        this.pgeyZone = pgeyZone;
        this.pgeyRect = pgeyRect;
        this.pgeyWidthMeters = pgeyWidthMeters;
        this.pgeyHeightMeters = pgeyHeightMeters;
    }

    public String pgeyLabel() {
        return pgeyLabel;
    }

    public HepyGarment pgeyGarment() {
        return pgeyGarment;
    }

    public PgeyZone pgeyZone() {
        return pgeyZone;
    }

    public PgeyUvRect pgeyRect() {
        return pgeyRect;
    }

    public double pgeyWidthMeters() {
        return pgeyWidthMeters;
    }

    public double pgeyHeightMeters() {
        return pgeyHeightMeters;
    }

    public double pgeyPxPerMeterX(int pgeySize) {
        return (pgeyRect.pgeyU1() - pgeyRect.pgeyU0()) * pgeySize / pgeyWidthMeters;
    }

    public double pgeyPxPerMeterY(int pgeySize) {
        return (pgeyRect.pgeyV1() - pgeyRect.pgeyV0()) * pgeySize / pgeyHeightMeters;
    }

    public boolean pgeyContains(double pgeyU, double pgeyV) {
        return pgeyU >= pgeyRect.pgeyU0() && pgeyU <= pgeyRect.pgeyU1() && pgeyV >= pgeyRect.pgeyV0() && pgeyV <= pgeyRect.pgeyV1();
    }

    public double[] pgeyToLocal(double pgeyU, double pgeyV) {
        return new double[]{(pgeyU - pgeyRect.pgeyU0()) / (pgeyRect.pgeyU1() - pgeyRect.pgeyU0()), (pgeyV - pgeyRect.pgeyV0()) / (pgeyRect.pgeyV1() - pgeyRect.pgeyV0())};
    }

    public static List<PgeyArea> pgeyOf(HepyGarment pgeyGarment) {
        return Arrays.stream(values()).filter(pgeyA -> pgeyA.pgeyGarment == pgeyGarment).toList();
    }
}
