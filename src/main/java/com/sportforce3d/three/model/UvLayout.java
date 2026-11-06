package com.sportforce3d.three.model;

import com.sportforce3d.three.geom.UvRect;

import java.util.List;
import java.util.Map;

/**
 * Distribución de islas UV dentro de la textura de cada prenda (una textura de 2048 × 2048 por prenda,
 * compartida por todas sus zonas). Las islas no se superponen; frente y espalda van separadas.
 * La Etapa 4 pinta colores, patrones, textos e imágenes en estas islas. Ver docs/modelos-blender.md.
 */
public final class UvLayout {

    private UvLayout() {
    }

    // ---------------------------------------------------------------- Camiseta
    public static final UvRect JERSEY_FRONT = new UvRect(0.02, 0.02, 0.48, 0.54);
    public static final UvRect JERSEY_BACK = new UvRect(0.52, 0.02, 0.98, 0.54);
    /** Manga derecha del jugador (lado -X, a la izquierda de la pantalla en vista frontal). */
    public static final UvRect JERSEY_SLEEVE_RIGHT = new UvRect(0.02, 0.58, 0.30, 0.78);
    public static final UvRect JERSEY_SLEEVE_LEFT = new UvRect(0.34, 0.58, 0.62, 0.78);
    /**
     * Antebrazo de la manga larga (Etapa 10, solo prendas simuladas): sigue a la isla de la manga, que conserva los
     * primeros 17 cm (ahí van los logos de la manga); comprimido a lo largo, sin elementos.
     */
    public static final UvRect JERSEY_FOREARM_RIGHT = new UvRect(0.02, 0.80, 0.30, 0.92);
    public static final UvRect JERSEY_FOREARM_LEFT = new UvRect(0.34, 0.80, 0.62, 0.92);
    public static final UvRect JERSEY_CUFF_RIGHT = new UvRect(0.66, 0.58, 0.81, 0.63);
    public static final UvRect JERSEY_CUFF_LEFT = new UvRect(0.83, 0.58, 0.98, 0.63);
    public static final UvRect JERSEY_SIDE_RIGHT = new UvRect(0.66, 0.66, 0.74, 0.90);
    public static final UvRect JERSEY_SIDE_LEFT = new UvRect(0.78, 0.66, 0.86, 0.90);
    public static final UvRect JERSEY_COLLAR = new UvRect(0.02, 0.93, 0.98, 0.97);

    // ---------------------------------------------------------------- Short
    public static final UvRect SHORTS_RIGHT_FRONT = new UvRect(0.02, 0.02, 0.24, 0.78);
    public static final UvRect SHORTS_RIGHT_BACK = new UvRect(0.26, 0.02, 0.48, 0.78);
    public static final UvRect SHORTS_LEFT_FRONT = new UvRect(0.52, 0.02, 0.74, 0.78);
    public static final UvRect SHORTS_LEFT_BACK = new UvRect(0.76, 0.02, 0.98, 0.78);
    public static final UvRect SHORTS_WAIST = new UvRect(0.02, 0.82, 0.98, 0.88);
    public static final UvRect SHORTS_STRIPE_RIGHT = new UvRect(0.02, 0.91, 0.48, 0.97);
    public static final UvRect SHORTS_STRIPE_LEFT = new UvRect(0.52, 0.91, 0.98, 0.97);

    // ---------------------------------------------------------------- Medias
    public static final UvRect SOCK_RIGHT = new UvRect(0.02, 0.02, 0.48, 0.78);
    public static final UvRect SOCK_LEFT = new UvRect(0.52, 0.02, 0.98, 0.78);
    public static final UvRect SOCK_BAND_RIGHT = new UvRect(0.02, 0.82, 0.48, 0.96);
    public static final UvRect SOCK_BAND_LEFT = new UvRect(0.52, 0.82, 0.98, 0.96);

    // ---------------------------------------------------------------- Zapatillas (ambos pies comparten islas)
    public static final UvRect SHOE_UPPER = new UvRect(0.02, 0.02, 0.98, 0.46);
    public static final UvRect SHOE_TOE = new UvRect(0.02, 0.50, 0.48, 0.66);
    public static final UvRect SHOE_HEEL = new UvRect(0.52, 0.50, 0.98, 0.66);
    public static final UvRect SHOE_LACES = new UvRect(0.02, 0.70, 0.48, 0.78);
    public static final UvRect SHOE_SOLE = new UvRect(0.02, 0.82, 0.98, 0.98);

    /** Islas por zona, para validar que no se superponen dentro de una misma prenda. */
    public static final Map<Zone, List<UvRect>> BY_ZONE = Map.ofEntries(
            Map.entry(Zone.JERSEY_BODY, List.of(JERSEY_FRONT, JERSEY_BACK)),
            Map.entry(Zone.JERSEY_SLEEVES, List.of(JERSEY_SLEEVE_RIGHT, JERSEY_SLEEVE_LEFT, JERSEY_FOREARM_RIGHT,
                    JERSEY_FOREARM_LEFT)),
            Map.entry(Zone.JERSEY_CUFFS, List.of(JERSEY_CUFF_RIGHT, JERSEY_CUFF_LEFT)),
            Map.entry(Zone.JERSEY_SIDES, List.of(JERSEY_SIDE_RIGHT, JERSEY_SIDE_LEFT)),
            Map.entry(Zone.JERSEY_COLLAR, List.of(JERSEY_COLLAR)),
            Map.entry(Zone.SHORTS_BASE, List.of(SHORTS_RIGHT_FRONT, SHORTS_RIGHT_BACK, SHORTS_LEFT_FRONT, SHORTS_LEFT_BACK)),
            Map.entry(Zone.SHORTS_WAIST, List.of(SHORTS_WAIST)),
            Map.entry(Zone.SHORTS_STRIPE, List.of(SHORTS_STRIPE_RIGHT, SHORTS_STRIPE_LEFT)),
            Map.entry(Zone.SOCKS_LEG, List.of(SOCK_RIGHT, SOCK_LEFT)),
            Map.entry(Zone.SOCKS_BAND, List.of(SOCK_BAND_RIGHT, SOCK_BAND_LEFT)),
            Map.entry(Zone.SHOE_UPPER, List.of(SHOE_UPPER)),
            Map.entry(Zone.SHOE_TOE, List.of(SHOE_TOE)),
            Map.entry(Zone.SHOE_HEEL, List.of(SHOE_HEEL)),
            Map.entry(Zone.SHOE_LACES, List.of(SHOE_LACES)),
            Map.entry(Zone.SHOE_SOLE, List.of(SHOE_SOLE)));
}
