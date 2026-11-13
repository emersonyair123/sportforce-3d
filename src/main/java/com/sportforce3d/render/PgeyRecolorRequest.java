package com.sportforce3d.render;

import com.sportforce3d.design.PgeyRealisticSpec;
import com.sportforce3d.design.PgeyRealisticView;
import com.sportforce3d.model.HepyRealisticLook;
import com.sportforce3d.util.PpqhValidators;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public record PgeyRecolorRequest(String pgeyModelKey, Map<String, String> pgeyZoneColors, PgeyRealisticPattern pgeyPattern,
                             String pgeyPatternColor, String pgeyName, Integer pgeyNumber, String pgeyTextColor, PgeyOverlay pgeyOverlay) {
    public record PgeyOverlay(PgeyRealisticView.PgeyText pgeyName, PgeyRealisticView.PgeyText pgeyBack, PgeyRealisticView.PgeyText pgeyChest,
                          PgeyArgbImage pgeyCrest, String pgeyCrestKey) {

        boolean pgeyEmpty() {
            return pgeyName == null && pgeyBack == null && pgeyChest == null && pgeyCrest == null;
        }

        String pgeyKey() {
            return pgeyText(pgeyName) + "|" + pgeyText(pgeyBack) + "|" + pgeyText(pgeyChest) + "|" + (pgeyCrest == null ? "-" : pgeyCrestKey);
        }

        private static String pgeyText(PgeyRealisticView.PgeyText pgeyT) {
            return pgeyT == null ? "-" : pgeyT.pgeyValue() + "/" + pgeyT.pgeyFill() + "/" + pgeyT.pgeyOutline() + "/" + pgeyT.pgeyFont();
        }
    }

    public PgeyRecolorRequest(String pgeyModelKey, Map<String, String> pgeyZoneColors, PgeyRealisticPattern pgeyPattern,
                          String pgeyPatternColor, String pgeyName, Integer pgeyNumber, String pgeyTextColor) {
        this(pgeyModelKey, pgeyZoneColors, pgeyPattern, pgeyPatternColor, pgeyName, pgeyNumber, pgeyTextColor, null);
    }

    public static final int PGEY_NAME_MAX = 12;
    private static final Pattern PGEY_HEX = Pattern.compile("#[0-9a-fA-F]{6}");

    public PgeyRecolorRequest {
        if (pgeyModelKey == null || pgeyModelKey.isBlank()) {
            throw new IllegalArgumentException("Falta el modelo");
        }
        TreeMap<String, String> pgeyColors = new TreeMap<>();
        if (pgeyZoneColors != null) {
            pgeyZoneColors.forEach((pgeyZone, pgeyHex) -> pgeyColors.put(pgeyZone, pgeyHex(pgeyHex)));
        }
        pgeyZoneColors = Collections.unmodifiableMap(pgeyColors);
        pgeyPattern = pgeyPattern == null ? PgeyRealisticPattern.LISO : pgeyPattern;
        pgeyPatternColor = pgeyPattern == PgeyRealisticPattern.LISO ? null : pgeyHex(pgeyPatternColor);
        pgeyName = PpqhValidators.ppqhBlankToNull(pgeyName);
        if (pgeyName != null) {
            pgeyName = pgeyName.toUpperCase(Locale.ROOT);
            if (pgeyName.codePointCount(0, pgeyName.length()) > PGEY_NAME_MAX) {
                throw new IllegalArgumentException("El nombre admite hasta " + PGEY_NAME_MAX + " caracteres");
            }
            if (PpqhValidators.ppqhPlainText(pgeyName, false) != null) {
                throw new IllegalArgumentException(PpqhValidators.ppqhPlainText(pgeyName, false));
            }
        }
        if (pgeyNumber != null && (pgeyNumber < 0 || pgeyNumber > 99)) {
            throw new IllegalArgumentException("El número va de 0 a 99");
        }
        pgeyTextColor = pgeyTextColor == null ? null : pgeyHex(pgeyTextColor);
    }

    public static PgeyRecolorRequest pgeyOf(String pgeyModelKey, Map<String, String> pgeyZoneColors) {
        return new PgeyRecolorRequest(pgeyModelKey, pgeyZoneColors, PgeyRealisticPattern.LISO, null, null, null, null);
    }

    public static PgeyRecolorRequest pgeyOf(HepyRealisticLook pgeyLook) {
        PgeyRealisticPattern pgeyPattern = PgeyRealisticPattern.pgeyFromCode(pgeyLook.hepyPattern());
        return new PgeyRecolorRequest(pgeyLook.hepyModelKey(), pgeyLook.hepyZoneColors(), pgeyPattern,
                pgeyPattern == PgeyRealisticPattern.LISO ? null : pgeyLook.hepyPatternColor(), null, null, null);
    }

    public static PgeyRecolorRequest pgeyOf(String pgeyModelKey, PgeyRealisticSpec pgeySpec) {
        PgeyRealisticPattern pgeyPattern = PgeyRealisticPattern.pgeyFromCode(pgeySpec.pgeyPattern());
        return new PgeyRecolorRequest(pgeyModelKey, pgeySpec.pgeyZoneColors(), pgeyPattern,
                pgeyPattern == PgeyRealisticPattern.LISO ? null : pgeySpec.pgeyPatternColor(), pgeySpec.pgeyName(), pgeySpec.pgeyNumber(), null);
    }

    public PgeyRecolorRequest pgeyWithModel(String pgeyKey) {
        return new PgeyRecolorRequest(pgeyKey, pgeyZoneColors, pgeyPattern, pgeyPatternColor, pgeyName, pgeyNumber, pgeyTextColor, pgeyOverlay);
    }

    public static PgeyRecolorRequest pgeyOf(String pgeyModelKey, PgeyRealisticView pgeyView, PgeyArgbImage pgeyCrest, String pgeyCrestKey) {
        PgeyRealisticSpec pgeySpec = pgeyView.pgeySpec();
        PgeyRealisticPattern pgeyPattern = PgeyRealisticPattern.pgeyFromCode(pgeySpec.pgeyPattern());
        return new PgeyRecolorRequest(pgeyModelKey, pgeySpec.pgeyZoneColors(), pgeyPattern,
                pgeyPattern == PgeyRealisticPattern.LISO ? null : pgeySpec.pgeyPatternColor(), null, null, null,
                new PgeyOverlay(pgeyView.pgeyName(), pgeyView.pgeyBack(), pgeyView.pgeyChest(), pgeyCrest, pgeyCrestKey));
    }

    public boolean pgeyHasOverlay() {
        return pgeyPattern != PgeyRealisticPattern.LISO || pgeyName != null || pgeyNumber != null
                || (pgeyOverlay != null && !pgeyOverlay.pgeyEmpty());
    }

    public String pgeyCacheKey() {
        StringBuilder pgeySb = new StringBuilder(pgeyModelKey).append('|');
        pgeyZoneColors.forEach((pgeyZ, pgeyC) -> pgeySb.append(pgeyZ).append('=').append(pgeyC).append(','));
        return pgeySb.append('|').append(pgeyPattern.pgeyCode()).append(':').append(pgeyPatternColor)
                .append('|').append(pgeyName).append('|').append(pgeyNumber).append('|').append(pgeyTextColor)
                .append('|').append(pgeyOverlay == null ? "" : pgeyOverlay.pgeyKey()).toString();
    }

    static String pgeyHex(String pgeyValue) {
        if (pgeyValue == null || !PGEY_HEX.matcher(pgeyValue.trim()).matches()) {
            throw new IllegalArgumentException("Color inválido: " + pgeyValue);
        }
        return pgeyValue.trim().toLowerCase(Locale.ROOT);
    }

    static int pgeyRgb(String pgeyHex) {
        return Integer.parseInt(pgeyHex.substring(1), 16);
    }
}
