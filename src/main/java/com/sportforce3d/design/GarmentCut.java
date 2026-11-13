package com.sportforce3d.design;

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

        public double numberU() {
            return numberU;
        }

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

    public String summary() {
        return "Manga " + sleeve.label().toLowerCase(java.util.Locale.ROOT)
                + " · Short " + shorts.label().toLowerCase(java.util.Locale.ROOT) + " (" + shorts.detail() + ")"
                + " · Medias: " + socks.label().toLowerCase(java.util.Locale.ROOT) + " (" + socks.detail() + ")";
    }
}
