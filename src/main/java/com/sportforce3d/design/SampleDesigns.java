package com.sportforce3d.design;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.Zone;

/** Diseños de ejemplo (capturas de documentación y punto de partida para probar el diseñador). */
public final class SampleDesigns {

    private SampleDesigns() {
    }

    /**
     * "Los Halcones": camiseta roja con franjas verticales blancas, detalles negros, short negro con franja roja,
     * medias rojas con aros blancos, chimpunes negros; nombre y número en la espalda, número en el pecho y en el
     * short, escudo en el pecho. En la vista realista: camiseta roja con rayas blancas, short y paneles negros,
     * medias rojas, QUISPE 10.
     */
    public static DesignDocument halcones() {
        String red = "#D7192A";
        String white = "#F2F5F8";
        String black = "#0B0E13";
        DesignDocument d = DesignDefaults.document(BodyType.ADULTO_HOMBRE)
                .withColor(Zone.JERSEY_BODY, red)
                .withColor(Zone.JERSEY_SLEEVES, red)
                .withColor(Zone.JERSEY_COLLAR, black)
                .withColor(Zone.JERSEY_CUFFS, black)
                .withColor(Zone.JERSEY_SIDES, black)
                .withPattern(Garment.JERSEY, new PatternSpec(PatternType.FRANJAS_VERTICALES, white, 0.045))
                .withColor(Zone.SHORTS_BASE, black)
                .withColor(Zone.SHORTS_STRIPE, red)
                .withColor(Zone.SHORTS_WAIST, red)
                .withColor(Zone.SOCKS_LEG, red)
                .withPattern(Garment.SOCKS, new PatternSpec(PatternType.FRANJAS_HORIZONTALES, white, 0.025))
                .withColor(Zone.SOCKS_BAND, black)
                .withColor(Zone.SHOE_UPPER, black)
                .withColor(Zone.SHOE_TOE, "#1E2229")
                .withColor(Zone.SHOE_HEEL, red)
                .withColor(Zone.SHOE_LACES, white)
                .withColor(Zone.SHOE_SOLE, white)
                .withMaterial(MaterialSlot.JERSEY, "JACQUARD")
                .withMaterial(MaterialSlot.SHOE_UPPER, "CUERO_SINTETICO")
                .withMaterial(MaterialSlot.SHOE_SOLE, "TPU_TACOS")
                .withRealistic(new RealisticSpec(RealisticSpec.DEFAULT_MODEL, java.util.Map.of("camiseta", red,
                        "short", black, "paneles", black, "medias", red, "principal", red), "rayas", white, "QUISPE", 10));
        return d
                .addElement(DesignDefaults.name("QUISPE", white, black).withOutline(0.0045))
                .addElement(DesignDefaults.backNumber("10", white, black).withOutline(0.006))
                .addElement(DesignDefaults.chestNumber("10", white, black).withOutline(0.003))
                .addElement(DesignDefaults.shortsNumber("10", white, red).withOutline(0.002))
                .addElement(DesignDefaults.emblem("escudo-clasico", black, white));
    }

    /**
     * Cuatro uniformes distintos para las capturas de las personas 3D (Etapa 8): 0 = Los Halcones; 1 = azul con
     * diagonales; 2 = verde a cuadros con mangas amarillas; 3 = morado con degradado.
     */
    public static DesignDocument forPerson(int i) {
        return switch (Math.floorMod(i, 4)) {
            case 0 -> halcones();
            case 1 -> kit(BodyType.ADULTO_MUJER, "#1F5FFF", "#F2F5F8", "#0B0E13",
                    new PatternSpec(PatternType.DIAGONALES, "#39FF88", 0.03), "ROJAS", "7", "rayo");
            case 2 -> kit(BodyType.JUVENIL, "#12A150", "#FFD23F", "#0B2A6B",
                    new PatternSpec(PatternType.CUADROS, "#0E7A3C", 0.06), "MAMANI", "9", "llama")
                    .withColor(Zone.JERSEY_SLEEVES, "#FFD23F");
            default -> kit(BodyType.JUVENIL, "#7B2FF7", "#F2F5F8", "#1E2229",
                    new PatternSpec(PatternType.DEGRADADO, "#FF5FB0", 0.5), "LUZ", "11", "corona");
        };
    }

    private static DesignDocument kit(BodyType type, String main, String light, String dark, PatternSpec pattern,
                                      String name, String number, String emblem) {
        return DesignDefaults.document(type)
                .withColor(Zone.JERSEY_BODY, main)
                .withColor(Zone.JERSEY_SLEEVES, main)
                .withColor(Zone.JERSEY_COLLAR, light)
                .withColor(Zone.JERSEY_CUFFS, light)
                .withColor(Zone.JERSEY_SIDES, dark)
                .withPattern(Garment.JERSEY, pattern)
                .withColor(Zone.SHORTS_BASE, dark)
                .withColor(Zone.SHORTS_STRIPE, main)
                .withColor(Zone.SHORTS_WAIST, main)
                .withColor(Zone.SOCKS_LEG, main)
                .withColor(Zone.SOCKS_BAND, light)
                .withColor(Zone.SHOE_UPPER, light)
                .withColor(Zone.SHOE_TOE, main)
                .withColor(Zone.SHOE_HEEL, dark)
                .withColor(Zone.SHOE_LACES, dark)
                .withColor(Zone.SHOE_SOLE, dark)
                .addElement(DesignDefaults.name(name, light, dark).withOutline(0.0045))
                .addElement(DesignDefaults.backNumber(number, light, dark).withOutline(0.006))
                .addElement(DesignDefaults.chestNumber(number, light, dark).withOutline(0.003))
                .addElement(DesignDefaults.shortsNumber(number, light, main).withOutline(0.002))
                .addElement(DesignDefaults.emblem(emblem, main, light));
    }
}