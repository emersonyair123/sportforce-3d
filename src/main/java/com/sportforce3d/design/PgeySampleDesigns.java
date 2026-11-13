package com.sportforce3d.design;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyZone;

public final class PgeySampleDesigns {

    private PgeySampleDesigns() {
    }

    public static PgeyDesignDocument pgeyHalcones() {
        String pgeyRed = "#D7192A";
        String pgeyWhite = "#F2F5F8";
        String pgeyBlack = "#0B0E13";
        PgeyDesignDocument pgeyD = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE)
                .pgeyWithColor(PgeyZone.JERSEY_BODY, pgeyRed)
                .pgeyWithColor(PgeyZone.JERSEY_SLEEVES, pgeyRed)
                .pgeyWithColor(PgeyZone.JERSEY_COLLAR, pgeyBlack)
                .pgeyWithColor(PgeyZone.JERSEY_CUFFS, pgeyBlack)
                .pgeyWithColor(PgeyZone.JERSEY_SIDES, pgeyBlack)
                .pgeyWithPattern(HepyGarment.JERSEY, new PgeyPatternSpec(PgeyPatternType.FRANJAS_VERTICALES, pgeyWhite, 0.045))
                .pgeyWithColor(PgeyZone.SHORTS_BASE, pgeyBlack)
                .pgeyWithColor(PgeyZone.SHORTS_STRIPE, pgeyRed)
                .pgeyWithColor(PgeyZone.SHORTS_WAIST, pgeyRed)
                .pgeyWithColor(PgeyZone.SOCKS_LEG, pgeyRed)
                .pgeyWithPattern(HepyGarment.SOCKS, new PgeyPatternSpec(PgeyPatternType.FRANJAS_HORIZONTALES, pgeyWhite, 0.025))
                .pgeyWithColor(PgeyZone.SOCKS_BAND, pgeyBlack)
                .pgeyWithColor(PgeyZone.SHOE_UPPER, pgeyBlack)
                .pgeyWithColor(PgeyZone.SHOE_TOE, "#1E2229")
                .pgeyWithColor(PgeyZone.SHOE_HEEL, pgeyRed)
                .pgeyWithColor(PgeyZone.SHOE_LACES, pgeyWhite)
                .pgeyWithColor(PgeyZone.SHOE_SOLE, pgeyWhite)
                .pgeyWithMaterial(PgeyMaterialSlot.JERSEY, "JACQUARD")
                .pgeyWithMaterial(PgeyMaterialSlot.SHOE_UPPER, "CUERO_SINTETICO")
                .pgeyWithMaterial(PgeyMaterialSlot.SHOE_SOLE, "TPU_TACOS")
                .pgeyWithRealistic(new PgeyRealisticSpec(PgeyRealisticSpec.PGEY_DEFAULT_MODEL, java.util.Map.of("camiseta", pgeyRed,
                        "short", pgeyBlack, "paneles", pgeyBlack, "medias", pgeyRed, "principal", pgeyRed), "rayas", pgeyWhite, "QUISPE", 10));
        return pgeyD
                .pgeyAddElement(PgeyDesignDefaults.pgeyName("QUISPE", pgeyWhite, pgeyBlack).pgeyWithOutline(0.0045))
                .pgeyAddElement(PgeyDesignDefaults.pgeyBackNumber("10", pgeyWhite, pgeyBlack).pgeyWithOutline(0.006))
                .pgeyAddElement(PgeyDesignDefaults.pgeyChestNumber("10", pgeyWhite, pgeyBlack).pgeyWithOutline(0.003))
                .pgeyAddElement(PgeyDesignDefaults.pgeyShortsNumber("10", pgeyWhite, pgeyRed).pgeyWithOutline(0.002))
                .pgeyAddElement(PgeyDesignDefaults.pgeyEmblem("escudo-clasico", pgeyBlack, pgeyWhite));
    }

    public static PgeyDesignDocument pgeyForPerson(int pgeyI) {
        return switch (Math.floorMod(pgeyI, 4)) {
            case 0 -> pgeyHalcones();
            case 1 -> pgeyKit(PgeyBodyType.ADULTO_MUJER, "#1F5FFF", "#F2F5F8", "#0B0E13",
                    new PgeyPatternSpec(PgeyPatternType.DIAGONALES, "#39FF88", 0.03), "ROJAS", "7", "rayo");
            case 2 -> pgeyKit(PgeyBodyType.JUVENIL, "#12A150", "#FFD23F", "#0B2A6B",
                    new PgeyPatternSpec(PgeyPatternType.CUADROS, "#0E7A3C", 0.06), "MAMANI", "9", "llama")
                    .pgeyWithColor(PgeyZone.JERSEY_SLEEVES, "#FFD23F");
            default -> pgeyKit(PgeyBodyType.JUVENIL, "#7B2FF7", "#F2F5F8", "#1E2229",
                    new PgeyPatternSpec(PgeyPatternType.DEGRADADO, "#FF5FB0", 0.5), "LUZ", "11", "corona");
        };
    }

    private static PgeyDesignDocument pgeyKit(PgeyBodyType pgeyType, String pgeyMain, String pgeyLight, String pgeyDark, PgeyPatternSpec pgeyPattern,
                                      String pgeyName, String pgeyNumber, String pgeyEmblem) {
        return PgeyDesignDefaults.pgeyDocument(pgeyType)
                .pgeyWithColor(PgeyZone.JERSEY_BODY, pgeyMain)
                .pgeyWithColor(PgeyZone.JERSEY_SLEEVES, pgeyMain)
                .pgeyWithColor(PgeyZone.JERSEY_COLLAR, pgeyLight)
                .pgeyWithColor(PgeyZone.JERSEY_CUFFS, pgeyLight)
                .pgeyWithColor(PgeyZone.JERSEY_SIDES, pgeyDark)
                .pgeyWithPattern(HepyGarment.JERSEY, pgeyPattern)
                .pgeyWithColor(PgeyZone.SHORTS_BASE, pgeyDark)
                .pgeyWithColor(PgeyZone.SHORTS_STRIPE, pgeyMain)
                .pgeyWithColor(PgeyZone.SHORTS_WAIST, pgeyMain)
                .pgeyWithColor(PgeyZone.SOCKS_LEG, pgeyMain)
                .pgeyWithColor(PgeyZone.SOCKS_BAND, pgeyLight)
                .pgeyWithColor(PgeyZone.SHOE_UPPER, pgeyLight)
                .pgeyWithColor(PgeyZone.SHOE_TOE, pgeyMain)
                .pgeyWithColor(PgeyZone.SHOE_HEEL, pgeyDark)
                .pgeyWithColor(PgeyZone.SHOE_LACES, pgeyDark)
                .pgeyWithColor(PgeyZone.SHOE_SOLE, pgeyDark)
                .pgeyAddElement(PgeyDesignDefaults.pgeyName(pgeyName, pgeyLight, pgeyDark).pgeyWithOutline(0.0045))
                .pgeyAddElement(PgeyDesignDefaults.pgeyBackNumber(pgeyNumber, pgeyLight, pgeyDark).pgeyWithOutline(0.006))
                .pgeyAddElement(PgeyDesignDefaults.pgeyChestNumber(pgeyNumber, pgeyLight, pgeyDark).pgeyWithOutline(0.003))
                .pgeyAddElement(PgeyDesignDefaults.pgeyShortsNumber(pgeyNumber, pgeyLight, pgeyMain).pgeyWithOutline(0.002))
                .pgeyAddElement(PgeyDesignDefaults.pgeyEmblem(pgeyEmblem, pgeyMain, pgeyLight));
    }
}
