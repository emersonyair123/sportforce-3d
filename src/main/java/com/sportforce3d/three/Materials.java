package com.sportforce3d.three;

import com.sportforce3d.three.model.Zone;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

import java.util.EnumMap;
import java.util.Map;

/**
 * Materiales del visor. Los colores predeterminados reproducen el probador de referencia
 * (design/referencia/frente.png): camiseta azul con detalles verde neón, short negro con pretina y franja azul,
 * medias azules con banda blanca y chimpunes blancos. Son datos de la prenda (los elegirá el cliente en la
 * Etapa 4), no tokens de interfaz.
 */
public final class Materials {

    /** Gris satinado del maniquí, como en la referencia. */
    public static final Color MANNEQUIN = Color.web("#6b7482");

    public static final Map<Zone, Color> DEFAULT_COLORS = defaults();

    private Materials() {
    }

    private static Map<Zone, Color> defaults() {
        Color blue = Color.web("#1f5fff");
        Color neon = Color.web("#39ff88");
        Color white = Color.web("#eef1f5");
        Color black = Color.web("#14171c");
        Map<Zone, Color> m = new EnumMap<>(Zone.class);
        m.put(Zone.JERSEY_BODY, blue);
        m.put(Zone.JERSEY_SLEEVES, blue);
        m.put(Zone.JERSEY_COLLAR, neon);
        m.put(Zone.JERSEY_CUFFS, neon);
        m.put(Zone.JERSEY_SIDES, neon);
        m.put(Zone.SHORTS_BASE, black);
        m.put(Zone.SHORTS_STRIPE, blue);
        m.put(Zone.SHORTS_WAIST, blue);
        m.put(Zone.SOCKS_LEG, blue);
        m.put(Zone.SOCKS_BAND, white);
        m.put(Zone.SHOE_UPPER, white);
        m.put(Zone.SHOE_TOE, Color.web("#e2e6ec"));
        m.put(Zone.SHOE_HEEL, neon);
        m.put(Zone.SHOE_LACES, white);
        m.put(Zone.SHOE_SOLE, Color.web("#1c1f25"));
        return m;
    }

    /** Brillo especular según el tipo de superficie: tela mate, sintético satinado, caucho. */
    public static PhongMaterial forZone(Zone zone, Color color, Image bump) {
        PhongMaterial m = new PhongMaterial(color);
        switch (zone) {
            case SHOE_UPPER, SHOE_TOE, SHOE_HEEL -> {
                m.setSpecularColor(Color.gray(0.42));
                m.setSpecularPower(46);
            }
            case SHOE_SOLE -> {
                m.setSpecularColor(Color.gray(0.16));
                m.setSpecularPower(14);
            }
            case SHOE_LACES -> {
                m.setSpecularColor(Color.gray(0.08));
                m.setSpecularPower(8);
            }
            default -> {
                // Tela deportiva (Dry-Fit): brillo suave y amplio.
                m.setSpecularColor(Color.gray(0.15));
                m.setSpecularPower(12);
                if (bump != null) {
                    m.setBumpMap(bump);
                }
            }
        }
        return m;
    }

    public static PhongMaterial mannequin() {
        PhongMaterial m = new PhongMaterial(MANNEQUIN);
        m.setSpecularColor(Color.gray(0.30));
        m.setSpecularPower(28);
        return m;
    }
}
