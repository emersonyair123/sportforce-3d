package com.sportforce3d.design;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class PgeyDesignRules {

    public static final int PGEY_NAME_MAX = 12;
    public static final int PGEY_NUMBER_MAX = 99;
    public static final int PGEY_MAX_ELEMENTS = 30;
    private static final Pattern PGEY_HEX = Pattern.compile("^#[0-9A-F]{6}$");
    private static final Pattern PGEY_NUMBER = Pattern.compile("^[0-9]{1,2}$");

    private PgeyDesignRules() {
    }

    public static String pgeyCleanName(String pgeyText) {
        String pgeyT = pgeyText == null ? "" : pgeyText.toUpperCase(java.util.Locale.forLanguageTag("es-PE"));
        pgeyT = pgeyT.replaceAll("[^A-ZÁÉÍÓÚÜÑ .'\\-]", "");
        return pgeyT.length() > PGEY_NAME_MAX ? pgeyT.substring(0, PGEY_NAME_MAX) : pgeyT;
    }

    public static String pgeyCleanNumber(String pgeyText) {
        String pgeyT = pgeyText == null ? "" : pgeyText.replaceAll("[^0-9]", "");
        if (pgeyT.isEmpty()) {
            return null;
        }
        if (pgeyT.length() > 2) {
            pgeyT = pgeyT.substring(0, 2);
        }
        return String.valueOf(Integer.parseInt(pgeyT));
    }

    public static boolean pgeyIsHex(String pgeyS) {
        return pgeyS != null && PGEY_HEX.matcher(pgeyS).matches();
    }

    public static List<String> pgeyProblems(PgeyDesignDocument pgeyD) {
        List<String> pgeyOut = new ArrayList<>();
        if (pgeyD.pgeySchemaVersion() != PgeyDesignDocument.PGEY_SCHEMA_VERSION) {
            pgeyOut.add("Versión de diseño no compatible: " + pgeyD.pgeySchemaVersion());
        }
        pgeyD.pgeyColors().forEach((pgeyZone, pgeyHex) -> {
            if (!pgeyIsHex(pgeyHex)) {
                pgeyOut.add("Color inválido en " + pgeyZone.pgeyLabel() + ": " + pgeyHex);
            }
        });
        pgeyD.pgeyPatterns().forEach((pgeyG, pgeyP) -> {
            if (!pgeyIsHex(pgeyP.pgeyColor())) {
                pgeyOut.add("Color del patrón inválido: " + pgeyP.pgeyColor());
            }
        });
        if (pgeyD.pgeyRealistic() != null) {
            pgeyOut.addAll(pgeyD.pgeyRealistic().pgeyProblems());
        }
        if (pgeyD.pgeyElements().size() > PGEY_MAX_ELEMENTS) {
            pgeyOut.add("Un diseño admite hasta " + PGEY_MAX_ELEMENTS + " elementos.");
        }
        for (PgeyDesignElement pgeyE : pgeyD.pgeyElements()) {
            if (pgeyE.pgeyArea() == null || pgeyE.pgeyType() == null) {
                pgeyOut.add("Elemento sin zona o tipo.");
                continue;
            }
            switch (pgeyE.pgeyType()) {
                case NAME -> {
                    if (pgeyE.pgeyText() == null || pgeyE.pgeyText().length() > PGEY_NAME_MAX) {
                        pgeyOut.add("El nombre admite hasta " + PGEY_NAME_MAX + " caracteres.");
                    }
                }
                case NUMBER -> {
                    if (pgeyE.pgeyText() == null || !PGEY_NUMBER.matcher(pgeyE.pgeyText()).matches()
                            || Integer.parseInt(pgeyE.pgeyText()) > PGEY_NUMBER_MAX) {
                        pgeyOut.add("El número va de 0 a " + PGEY_NUMBER_MAX + ".");
                    }
                }
                case EMBLEM -> {
                    if (!PgeyEmblemCatalog.pgeyExists(pgeyE.pgeyEmblem())) {
                        pgeyOut.add("Emblema desconocido: " + pgeyE.pgeyEmblem());
                    }
                }
                case LOGO -> {
                    if (pgeyE.pgeyAssetId() == null) {
                        pgeyOut.add("Logo sin archivo.");
                    }
                }
            }
            if (pgeyE.pgeyType().pgeyIsText() && !PgeyDesignFonts.pgeyIsValid(pgeyE.pgeyFont())) {
                pgeyOut.add("Tipografía no disponible: " + pgeyE.pgeyFont());
            }
            if (pgeyE.pgeyColor() != null && !pgeyIsHex(pgeyE.pgeyColor()) || pgeyE.pgeyAccentColor() != null && !pgeyIsHex(pgeyE.pgeyAccentColor())) {
                pgeyOut.add("Color inválido en " + pgeyE.pgeyDisplayName());
            }
        }
        return pgeyOut;
    }
}
