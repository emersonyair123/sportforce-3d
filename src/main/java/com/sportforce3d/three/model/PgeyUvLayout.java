package com.sportforce3d.three.model;

import com.sportforce3d.three.geom.PgeyUvRect;

import java.util.List;
import java.util.Map;

public final class PgeyUvLayout {

    private PgeyUvLayout() {
    }

    public static final PgeyUvRect PGEY_JERSEY_FRONT = new PgeyUvRect(0.02, 0.02, 0.48, 0.54);
    public static final PgeyUvRect PGEY_JERSEY_BACK = new PgeyUvRect(0.52, 0.02, 0.98, 0.54);
    public static final PgeyUvRect PGEY_JERSEY_SLEEVE_RIGHT = new PgeyUvRect(0.02, 0.58, 0.30, 0.78);
    public static final PgeyUvRect PGEY_JERSEY_SLEEVE_LEFT = new PgeyUvRect(0.34, 0.58, 0.62, 0.78);
    public static final PgeyUvRect PGEY_JERSEY_FOREARM_RIGHT = new PgeyUvRect(0.02, 0.80, 0.30, 0.92);
    public static final PgeyUvRect PGEY_JERSEY_FOREARM_LEFT = new PgeyUvRect(0.34, 0.80, 0.62, 0.92);
    public static final PgeyUvRect PGEY_JERSEY_CUFF_RIGHT = new PgeyUvRect(0.66, 0.58, 0.81, 0.63);
    public static final PgeyUvRect PGEY_JERSEY_CUFF_LEFT = new PgeyUvRect(0.83, 0.58, 0.98, 0.63);
    public static final PgeyUvRect PGEY_JERSEY_SIDE_RIGHT = new PgeyUvRect(0.66, 0.66, 0.74, 0.90);
    public static final PgeyUvRect PGEY_JERSEY_SIDE_LEFT = new PgeyUvRect(0.78, 0.66, 0.86, 0.90);
    public static final PgeyUvRect PGEY_JERSEY_COLLAR = new PgeyUvRect(0.02, 0.93, 0.98, 0.97);

    public static final PgeyUvRect PGEY_SHORTS_RIGHT_FRONT = new PgeyUvRect(0.02, 0.02, 0.24, 0.78);
    public static final PgeyUvRect PGEY_SHORTS_RIGHT_BACK = new PgeyUvRect(0.26, 0.02, 0.48, 0.78);
    public static final PgeyUvRect PGEY_SHORTS_LEFT_FRONT = new PgeyUvRect(0.52, 0.02, 0.74, 0.78);
    public static final PgeyUvRect PGEY_SHORTS_LEFT_BACK = new PgeyUvRect(0.76, 0.02, 0.98, 0.78);
    public static final PgeyUvRect PGEY_SHORTS_WAIST = new PgeyUvRect(0.02, 0.82, 0.98, 0.88);
    public static final PgeyUvRect PGEY_SHORTS_STRIPE_RIGHT = new PgeyUvRect(0.02, 0.91, 0.48, 0.97);
    public static final PgeyUvRect PGEY_SHORTS_STRIPE_LEFT = new PgeyUvRect(0.52, 0.91, 0.98, 0.97);

    public static final PgeyUvRect PGEY_SOCK_RIGHT = new PgeyUvRect(0.02, 0.02, 0.48, 0.78);
    public static final PgeyUvRect PGEY_SOCK_LEFT = new PgeyUvRect(0.52, 0.02, 0.98, 0.78);
    public static final PgeyUvRect PGEY_SOCK_BAND_RIGHT = new PgeyUvRect(0.02, 0.82, 0.48, 0.96);
    public static final PgeyUvRect PGEY_SOCK_BAND_LEFT = new PgeyUvRect(0.52, 0.82, 0.98, 0.96);

    public static final PgeyUvRect PGEY_SHOE_UPPER = new PgeyUvRect(0.02, 0.02, 0.98, 0.46);
    public static final PgeyUvRect PGEY_SHOE_TOE = new PgeyUvRect(0.02, 0.50, 0.48, 0.66);
    public static final PgeyUvRect PGEY_SHOE_HEEL = new PgeyUvRect(0.52, 0.50, 0.98, 0.66);
    public static final PgeyUvRect PGEY_SHOE_LACES = new PgeyUvRect(0.02, 0.70, 0.48, 0.78);
    public static final PgeyUvRect PGEY_SHOE_SOLE = new PgeyUvRect(0.02, 0.82, 0.98, 0.98);

    public static final Map<PgeyZone, List<PgeyUvRect>> PGEY_BY_ZONE = Map.ofEntries(
            Map.entry(PgeyZone.JERSEY_BODY, List.of(PGEY_JERSEY_FRONT, PGEY_JERSEY_BACK)),
            Map.entry(PgeyZone.JERSEY_SLEEVES, List.of(PGEY_JERSEY_SLEEVE_RIGHT, PGEY_JERSEY_SLEEVE_LEFT, PGEY_JERSEY_FOREARM_RIGHT,
                    PGEY_JERSEY_FOREARM_LEFT)),
            Map.entry(PgeyZone.JERSEY_CUFFS, List.of(PGEY_JERSEY_CUFF_RIGHT, PGEY_JERSEY_CUFF_LEFT)),
            Map.entry(PgeyZone.JERSEY_SIDES, List.of(PGEY_JERSEY_SIDE_RIGHT, PGEY_JERSEY_SIDE_LEFT)),
            Map.entry(PgeyZone.JERSEY_COLLAR, List.of(PGEY_JERSEY_COLLAR)),
            Map.entry(PgeyZone.SHORTS_BASE, List.of(PGEY_SHORTS_RIGHT_FRONT, PGEY_SHORTS_RIGHT_BACK, PGEY_SHORTS_LEFT_FRONT, PGEY_SHORTS_LEFT_BACK)),
            Map.entry(PgeyZone.SHORTS_WAIST, List.of(PGEY_SHORTS_WAIST)),
            Map.entry(PgeyZone.SHORTS_STRIPE, List.of(PGEY_SHORTS_STRIPE_RIGHT, PGEY_SHORTS_STRIPE_LEFT)),
            Map.entry(PgeyZone.SOCKS_LEG, List.of(PGEY_SOCK_RIGHT, PGEY_SOCK_LEFT)),
            Map.entry(PgeyZone.SOCKS_BAND, List.of(PGEY_SOCK_BAND_RIGHT, PGEY_SOCK_BAND_LEFT)),
            Map.entry(PgeyZone.SHOE_UPPER, List.of(PGEY_SHOE_UPPER)),
            Map.entry(PgeyZone.SHOE_TOE, List.of(PGEY_SHOE_TOE)),
            Map.entry(PgeyZone.SHOE_HEEL, List.of(PGEY_SHOE_HEEL)),
            Map.entry(PgeyZone.SHOE_LACES, List.of(PGEY_SHOE_LACES)),
            Map.entry(PgeyZone.SHOE_SOLE, List.of(PGEY_SHOE_SOLE)));
}
