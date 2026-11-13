package com.sportforce3d.design;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PgeyGarmentCut(@JsonProperty("sleeve") PgeySleeve pgeySleeve, @JsonProperty("shorts") PgeyShortsLength pgeyShorts, @JsonProperty("socks") PgeySocksLength pgeySocks) {

    public enum PgeySleeve {
        CORTA("Corta"), LARGA("Larga");

        private final String pgeyLabel;

        PgeySleeve(String pgeyLabel) {
            this.pgeyLabel = pgeyLabel;
        }

        public String pgeyLabel() {
            return pgeyLabel;
        }
    }

    public enum PgeyShortsLength {
        CORTO("Corto", "medio muslo", 0.74, 0.665), MEDIO("Medio", "sobre la rodilla", 0.66, 0.55),
        LARGO("Largo", "a la rodilla", 0.64, 0.34);

        private final String pgeyLabel;
        private final String pgeyDetail;
        private final double pgeyNumberU;
        private final double pgeyNumberV;

        PgeyShortsLength(String pgeyLabel, String pgeyDetail, double pgeyNumberU, double pgeyNumberV) {
            this.pgeyLabel = pgeyLabel;
            this.pgeyDetail = pgeyDetail;
            this.pgeyNumberU = pgeyNumberU;
            this.pgeyNumberV = pgeyNumberV;
        }

        public double pgeyNumberU() {
            return pgeyNumberU;
        }

        public double pgeyNumberV() {
            return pgeyNumberV;
        }

        public String pgeyLabel() {
            return pgeyLabel;
        }

        public String pgeyDetail() {
            return pgeyDetail;
        }
    }

    public enum PgeySocksLength {
        TOBILLERA("Tobillera", "sobre el tobillo"), MEDIA("Media pierna", "a media pantorrilla"),
        LARGA("Larga", "bajo la rodilla");

        private final String pgeyLabel;
        private final String pgeyDetail;

        PgeySocksLength(String pgeyLabel, String pgeyDetail) {
            this.pgeyLabel = pgeyLabel;
            this.pgeyDetail = pgeyDetail;
        }

        public String pgeyLabel() {
            return pgeyLabel;
        }

        public String pgeyDetail() {
            return pgeyDetail;
        }
    }

    public static final PgeyGarmentCut PGEY_DEFAULT = new PgeyGarmentCut(PgeySleeve.CORTA, PgeyShortsLength.MEDIO, PgeySocksLength.LARGA);

    public PgeyGarmentCut {
        pgeySleeve = pgeySleeve == null ? PgeySleeve.CORTA : pgeySleeve;
        pgeyShorts = pgeyShorts == null ? PgeyShortsLength.MEDIO : pgeyShorts;
        pgeySocks = pgeySocks == null ? PgeySocksLength.LARGA : pgeySocks;
    }

    public PgeyGarmentCut pgeyWithSleeve(PgeySleeve pgeyS) {
        return new PgeyGarmentCut(pgeyS, pgeyShorts, pgeySocks);
    }

    public PgeyGarmentCut pgeyWithShorts(PgeyShortsLength pgeyS) {
        return new PgeyGarmentCut(pgeySleeve, pgeyS, pgeySocks);
    }

    public PgeyGarmentCut pgeyWithSocks(PgeySocksLength pgeyS) {
        return new PgeyGarmentCut(pgeySleeve, pgeyShorts, pgeyS);
    }

    public String pgeySummary() {
        return "Manga " + pgeySleeve.pgeyLabel().toLowerCase(java.util.Locale.ROOT)
                + " · Short " + pgeyShorts.pgeyLabel().toLowerCase(java.util.Locale.ROOT) + " (" + pgeyShorts.pgeyDetail() + ")"
                + " · Medias: " + pgeySocks.pgeyLabel().toLowerCase(java.util.Locale.ROOT) + " (" + pgeySocks.pgeyDetail() + ")";
    }
}
