package com.sportforce3d.design;

/**
 * Corte del uniforme en la Vista 3D (Etapa 10C): largo de la manga, del short y de las medias. Cada combinación usa
 * las prendas simuladas en Blender para esa persona; no cambia el precio. Un diseño anterior (sin corte) abre con
 * {@link #DEFAULT}: manga corta, short medio y media larga, que son las prendas de siempre.
 */
public record GarmentCut(Sleeve sleeve, ShortsLength shorts, SocksLength socks) {

    public enum Sleeve {
        CORTA("Corta"), LARGA("Larga");

        private final String label;

        Sleeve(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum ShortsLength {
        CORTO("Corto", "medio muslo", 0.74, 0.665), MEDIO("Medio", "sobre la rodilla", 0.66, 0.55),
        LARGO("Largo", "a la rodilla", 0.64, 0.34);

        private final String label;
        private final String detail;
        private final double numberU;
        private final double numberV;

        ShortsLength(String label, String detail, double numberU, double numberV) {
            this.label = label;
            this.detail = detail;
            this.numberU = numberU;
            this.numberV = numberV;
        }

        /**
         * u (0–1 en Area.SHORTS_LEFT) del frente del muslo izquierdo a la altura del número: en la isla, u = 0,5 es la
         * línea media del cuerpo junto a la entrepierna y se abre hacia el muslo más abajo, así que en el short corto
         * (número más arriba) va más hacia afuera.
         */
        public double numberU() {
            return numberU;
        }

        /**
         * v (0–1 en Area.SHORTS_LEFT) de la mitad de lo que se ve del short con este largo, entre el ruedo de la
         * camiseta y la basta, calculada con las medidas de las cuatro personas (las UV del corto y del largo usan la
         * escala del medio anclada a la basta; difieren menos de 0,04 entre personas). GarmentCutVariantsTest
         * comprueba en las prendas simuladas que el número queda a la vista.
         */
        public double numberV() {
            return numberV;
        }

        public String label() {
            return label;
        }

        public String detail() {
            return detail;
        }
    }

    public enum SocksLength {
        TOBILLERA("Tobillera", "sobre el tobillo"), MEDIA("Media pierna", "a media pantorrilla"),
        LARGA("Larga", "bajo la rodilla");

        private final String label;
        private final String detail;

        SocksLength(String label, String detail) {
            this.label = label;
            this.detail = detail;
        }

        public String label() {
            return label;
        }

        public String detail() {
            return detail;
        }
    }

    public static final GarmentCut DEFAULT = new GarmentCut(Sleeve.CORTA, ShortsLength.MEDIO, SocksLength.LARGA);

    public GarmentCut {
        sleeve = sleeve == null ? Sleeve.CORTA : sleeve;
        shorts = shorts == null ? ShortsLength.MEDIO : shorts;
        socks = socks == null ? SocksLength.LARGA : socks;
    }

    public GarmentCut withSleeve(Sleeve s) {
        return new GarmentCut(s, shorts, socks);
    }

    public GarmentCut withShorts(ShortsLength s) {
        return new GarmentCut(sleeve, s, socks);
    }

    public GarmentCut withSocks(SocksLength s) {
        return new GarmentCut(sleeve, shorts, s);
    }

    /** Para la ficha técnica: "Manga larga · Short corto (medio muslo) · Medias tobilleras (sobre el tobillo)". */
    public String summary() {
        return "Manga " + sleeve.label().toLowerCase(java.util.Locale.ROOT)
                + " · Short " + shorts.label().toLowerCase(java.util.Locale.ROOT) + " (" + shorts.detail() + ")"
                + " · Medias: " + socks.label().toLowerCase(java.util.Locale.ROOT) + " (" + socks.detail() + ")";
    }
}
