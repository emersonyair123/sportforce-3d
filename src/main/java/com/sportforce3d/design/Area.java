package com.sportforce3d.design;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.UvRect;
import com.sportforce3d.three.model.UvLayout;
import com.sportforce3d.three.model.Zone;

import java.util.Arrays;
import java.util.List;

/**
 * Zonas donde se puede colocar un nombre, número, emblema o logo. Cada una es una isla UV de la textura de su
 * prenda y conoce su tamaño físico aproximado en la prenda (m), para que lo que se dibuja no se deforme aunque la
 * isla tenga otra proporción en la textura. "Izquierda" y "derecha" son las del jugador.
 */
public enum Area {
    JERSEY_FRONT("Pecho", Garment.JERSEY, Zone.JERSEY_BODY, UvLayout.JERSEY_FRONT, 0.47, 0.58),
    JERSEY_BACK("Espalda", Garment.JERSEY, Zone.JERSEY_BODY, UvLayout.JERSEY_BACK, 0.47, 0.58),
    SLEEVE_LEFT("Manga izquierda", Garment.JERSEY, Zone.JERSEY_SLEEVES, UvLayout.JERSEY_SLEEVE_LEFT, 0.40, 0.17),
    SLEEVE_RIGHT("Manga derecha", Garment.JERSEY, Zone.JERSEY_SLEEVES, UvLayout.JERSEY_SLEEVE_RIGHT, 0.40, 0.17),
    SHORTS_LEFT("Short · pierna izquierda", Garment.SHORTS, Zone.SHORTS_BASE, UvLayout.SHORTS_LEFT_FRONT, 0.36, 0.35),
    SHORTS_RIGHT("Short · pierna derecha", Garment.SHORTS, Zone.SHORTS_BASE, UvLayout.SHORTS_RIGHT_FRONT, 0.36, 0.35),
    SOCK_LEFT("Media izquierda · banda", Garment.SOCKS, Zone.SOCKS_BAND, UvLayout.SOCK_BAND_LEFT, 0.37, 0.053),
    SOCK_RIGHT("Media derecha · banda", Garment.SOCKS, Zone.SOCKS_BAND, UvLayout.SOCK_BAND_RIGHT, 0.37, 0.053),
    SHOE_SIDE("Zapatilla · costado", Garment.SHOES, Zone.SHOE_UPPER, UvLayout.SHOE_UPPER, 0.285, 0.26);

    private final String label;
    private final Garment garment;
    private final Zone zone;
    private final UvRect rect;
    private final double widthMeters;
    private final double heightMeters;

    Area(String label, Garment garment, Zone zone, UvRect rect, double widthMeters, double heightMeters) {
        this.label = label;
        this.garment = garment;
        this.zone = zone;
        this.rect = rect;
        this.widthMeters = widthMeters;
        this.heightMeters = heightMeters;
    }

    public String label() {
        return label;
    }

    public Garment garment() {
        return garment;
    }

    public Zone zone() {
        return zone;
    }

    public UvRect rect() {
        return rect;
    }

    public double widthMeters() {
        return widthMeters;
    }

    public double heightMeters() {
        return heightMeters;
    }

    /** Píxeles por metro en horizontal y vertical para una textura de {@code size} px. */
    public double pxPerMeterX(int size) {
        return (rect.u1() - rect.u0()) * size / widthMeters;
    }

    public double pxPerMeterY(int size) {
        return (rect.v1() - rect.v0()) * size / heightMeters;
    }

    public boolean contains(double u, double v) {
        return u >= rect.u0() && u <= rect.u1() && v >= rect.v0() && v <= rect.v1();
    }

    /** Coordenada UV de textura → posición local 0..1 dentro de la zona (sin recortar). */
    public double[] toLocal(double u, double v) {
        return new double[]{(u - rect.u0()) / (rect.u1() - rect.u0()), (v - rect.v0()) / (rect.v1() - rect.v0())};
    }

    public static List<Area> of(Garment garment) {
        return Arrays.stream(values()).filter(a -> a.garment == garment).toList();
    }
}
