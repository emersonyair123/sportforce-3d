package com.sportforce3d.three.model;

import com.sportforce3d.model.HepyGarment;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum PgeyZone {
    JERSEY_BODY("jersey_body", HepyGarment.JERSEY, "Cuerpo"),
    JERSEY_SLEEVES("jersey_sleeves", HepyGarment.JERSEY, "Mangas"),
    JERSEY_COLLAR("jersey_collar", HepyGarment.JERSEY, "Cuello"),
    JERSEY_CUFFS("jersey_cuffs", HepyGarment.JERSEY, "Puños"),
    JERSEY_SIDES("jersey_sides", HepyGarment.JERSEY, "Paneles laterales"),
    SHORTS_BASE("shorts_base", HepyGarment.SHORTS, "Base"),
    SHORTS_STRIPE("shorts_stripe", HepyGarment.SHORTS, "Franja lateral"),
    SHORTS_WAIST("shorts_waist", HepyGarment.SHORTS, "Pretina"),
    SOCKS_LEG("socks_leg", HepyGarment.SOCKS, "Caña"),
    SOCKS_BAND("socks_band", HepyGarment.SOCKS, "Banda superior"),
    SHOE_UPPER("shoe_upper", HepyGarment.SHOES, "Capellada"),
    SHOE_TOE("shoe_toe", HepyGarment.SHOES, "Puntera"),
    SHOE_HEEL("shoe_heel", HepyGarment.SHOES, "Talón"),
    SHOE_LACES("shoe_laces", HepyGarment.SHOES, "Cordones"),
    SHOE_SOLE("shoe_sole", HepyGarment.SHOES, "Suela");

    private final String pgeyMeshName;
    private final HepyGarment pgeyGarment;
    private final String pgeyLabel;

    PgeyZone(String pgeyMeshName, HepyGarment pgeyGarment, String pgeyLabel) {
        this.pgeyMeshName = pgeyMeshName;
        this.pgeyGarment = pgeyGarment;
        this.pgeyLabel = pgeyLabel;
    }

    public String pgeyMeshName() {
        return pgeyMeshName;
    }

    public HepyGarment pgeyGarment() {
        return pgeyGarment;
    }

    public String pgeyLabel() {
        return pgeyLabel;
    }

    public static Optional<PgeyZone> pgeyFromMeshName(String pgeyName) {
        if (pgeyName == null) {
            return Optional.empty();
        }
        String pgeyN = pgeyName.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(pgeyZ -> pgeyN.equals(pgeyZ.pgeyMeshName) || pgeyN.startsWith(pgeyZ.pgeyMeshName + ".") || pgeyN.startsWith(pgeyZ.pgeyMeshName + "_"))
                .max((pgeyA, pgeyB) -> Integer.compare(pgeyA.pgeyMeshName.length(), pgeyB.pgeyMeshName.length()));
    }
}
