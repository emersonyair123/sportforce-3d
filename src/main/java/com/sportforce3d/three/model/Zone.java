package com.sportforce3d.three.model;

import com.sportforce3d.model.Garment;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Zonas personalizables con su nombre fijo (design/prendas-y-materiales.md). Un modelo de Blender reemplaza
 * al procedural si sus objetos se llaman exactamente así.
 */
public enum Zone {
    JERSEY_BODY("jersey_body", Garment.JERSEY, "Cuerpo"),
    JERSEY_SLEEVES("jersey_sleeves", Garment.JERSEY, "Mangas"),
    JERSEY_COLLAR("jersey_collar", Garment.JERSEY, "Cuello"),
    JERSEY_CUFFS("jersey_cuffs", Garment.JERSEY, "Puños"),
    JERSEY_SIDES("jersey_sides", Garment.JERSEY, "Paneles laterales"),
    SHORTS_BASE("shorts_base", Garment.SHORTS, "Base"),
    SHORTS_STRIPE("shorts_stripe", Garment.SHORTS, "Franja lateral"),
    SHORTS_WAIST("shorts_waist", Garment.SHORTS, "Pretina"),
    SOCKS_LEG("socks_leg", Garment.SOCKS, "Caña"),
    SOCKS_BAND("socks_band", Garment.SOCKS, "Banda superior"),
    SHOE_UPPER("shoe_upper", Garment.SHOES, "Capellada"),
    SHOE_TOE("shoe_toe", Garment.SHOES, "Puntera"),
    SHOE_HEEL("shoe_heel", Garment.SHOES, "Talón"),
    SHOE_LACES("shoe_laces", Garment.SHOES, "Cordones"),
    SHOE_SOLE("shoe_sole", Garment.SHOES, "Suela");

    private final String meshName;
    private final Garment garment;
    private final String label;

    Zone(String meshName, Garment garment, String label) {
        this.meshName = meshName;
        this.garment = garment;
        this.label = label;
    }

    public String meshName() {
        return meshName;
    }

    public Garment garment() {
        return garment;
    }

    public String label() {
        return label;
    }

    /** Busca por nombre de objeto de un OBJ: exacto o con sufijos de Blender ("jersey_body.001", "shoe_sole_L"). */
    public static Optional<Zone> fromMeshName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String n = name.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(z -> n.equals(z.meshName) || n.startsWith(z.meshName + ".") || n.startsWith(z.meshName + "_"))
                // "shoe_sole_l" no debe confundirse con otra zona más corta: gana el nombre más largo.
                .max((a, b) -> Integer.compare(a.meshName.length(), b.meshName.length()));
    }
}
