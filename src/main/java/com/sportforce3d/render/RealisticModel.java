package com.sportforce3d.render;

import java.util.List;

/**
 * Un modelo de {@code models/realista/modelos.json}: foto base + máscaras por zona + textura de detalle.
 * La clave sigue el patrón {@code <corte>_<categoría>_<vista>} (por ejemplo {@code futbol_mujer_espalda}) o
 * {@code producto_<nombre>} para los productos sueltos.
 *
 * @param key      nombre de la carpeta
 * @param view     frente, espalda o producto
 * @param zones    zonas que se pueden colorear ({@code camiseta}, {@code short}, {@code paneles}, {@code medias},
 *                 {@code principal}); cada una tiene su {@code zona_<nombre>.png}
 * @param shirt    geometría de la camiseta para colocar diseños y textos; null en buzos y productos
 * @param width    ancho de las imágenes en px
 * @param height   alto de las imágenes en px
 */
public record RealisticModel(String key, View view, List<String> zones, ShirtGeometry shirt, int width, int height) {

    public static final String CAMISETA = "camiseta";
    public static final String SHORT = "short";
    public static final String PANELES = "paneles";
    public static final String MEDIAS = "medias";
    public static final String PRINCIPAL = "principal";

    public enum View {
        FRENTE, ESPALDA, PRODUCTO
    }

    /**
     * Geometría de la camiseta en px de la imagen.
     *
     * @param centerX columna del centro del pecho (o de la espalda)
     * @param neckY   fila del cuello
     * @param hemY    fila de la basta
     */
    public record ShirtGeometry(int centerX, int neckY, int hemY) {

        public ShirtGeometry {
            if (hemY <= neckY) {
                throw new IllegalArgumentException("La basta debe estar debajo del cuello");
            }
        }

        /** Alto de la camiseta: {@code h = bastaY - cuelloY}. */
        public int height() {
            return hemY - neckY;
        }
    }

    public RealisticModel {
        zones = List.copyOf(zones);
    }

    public boolean hasZone(String zone) {
        return zones.contains(zone);
    }

    /** Corte: futbol, polo, redondo, raglan, mangalarga, voley, basquet, buzo, shortlargo, licra o producto. */
    public String cut() {
        int i = key.indexOf('_');
        return i < 0 ? key : key.substring(0, i);
    }

    /** Categoría (hombre, mujer, nino, nina, juvenil) o null en los productos. */
    public String audience() {
        if (view == View.PRODUCTO) {
            return null;
        }
        String[] parts = key.split("_");
        return parts.length >= 3 ? parts[1] : null;
    }

    /** Clave del modelo de la otra vista (frente ↔ espalda); puede no existir en el repositorio. */
    public String otherViewKey() {
        return switch (view) {
            case FRENTE -> key.replaceFirst("_frente$", "_espalda");
            case ESPALDA -> key.replaceFirst("_espalda$", "_frente");
            case PRODUCTO -> null;
        };
    }
}
