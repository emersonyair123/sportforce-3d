package com.sportforce3d.design;

import com.sportforce3d.design.PgeyGarmentCut.PgeyShortsLength;
import com.sportforce3d.design.PgeyGarmentCut.PgeySleeve;
import com.sportforce3d.design.PgeyGarmentCut.PgeySocksLength;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.model.PgeyZone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class PgeyViewSync {
    static final Set<String> PGEY_FOOTBALL = Set.of("futbol", "mangalarga", "shortlargo");
    static final Set<String> PGEY_PHOTO_ONLY_PATTERNS = Set.of("dinamico", "geometrico", "minimal", "banda");

    public static final Map<String, String> PGEY_CUT_LABELS = pgeyCutLabels();
    private static final Map<String, String> PGEY_PATTERN_LABELS = Map.of("dinamico", "Dinámico", "geometrico",
            "Geométrico", "minimal", "Minimal", "banda", "Banda diagonal");
    private static final Map<String, String> PGEY_FABRIC_LABELS = Map.of("DRY_FIT", "Dry-fit", "POLIESTER_LISO",
            "Poliéster liso", "ALGODON", "Algodón", "MALLA", "Malla deportiva", "JACQUARD", "Jacquard", "LICRA", "Licra");

    private PgeyViewSync() {
    }

    public static PgeyRealisticView pgeyView(PgeyDesignDocument pgeyD) {
        PgeyRealisticSpec pgeyStored = pgeyD.pgeyRealisticOrDefault();
        String pgeyModel = pgeyPhotoModel(pgeyStored.pgeyModelKey(), pgeyD.pgeyCut());
        List<String> pgeyZones = PgeyRealisticModels.pgeyZones(pgeyModel);
        String pgeyFamily = pgeyFamily(pgeyModel);
        boolean pgeyShirt = pgeyZones.contains("camiseta");
        boolean pgeyBuzo = pgeyZones.contains("principal");
        Set<String> pgeyNotes = new LinkedHashSet<>();

        PgeyGarmentCut pgeyCut = pgeyD.pgeyCut();
        if (pgeyShirt && PgeyRealisticModels.pgeyExists(pgeyModel)) {
            PgeyGarmentCut pgeyNat = pgeyNativeCut(pgeyFamily, pgeyAudience(pgeyModel), pgeyCut);
            if (pgeyCut.pgeySleeve() != pgeyNat.pgeySleeve()) {
                pgeyNotes.add("Manga " + pgeyCut.pgeySleeve().pgeyLabel().toLowerCase() + ": ver en Vista 3D");
            }
            if (pgeyZones.contains("short") && pgeyCut.pgeyShorts() != pgeyNat.pgeyShorts()) {
                pgeyNotes.add("Short " + pgeyCut.pgeyShorts().pgeyLabel().toLowerCase() + ": ver en Vista 3D");
            }
            if (pgeyZones.contains("medias") && pgeyCut.pgeySocks() != PgeySocksLength.LARGA) {
                pgeyNotes.add((pgeyCut.pgeySocks() == PgeySocksLength.TOBILLERA ? "Medias tobilleras" : "Medias de media pierna")
                        + ": ver en Vista 3D");
            }
        }

        Map<String, String> pgeyColors = new TreeMap<>();
        pgeyColors.put("camiseta", pgeyD.pgeyColor(PgeyZone.JERSEY_BODY));
        pgeyColors.put("principal", pgeyD.pgeyColor(PgeyZone.JERSEY_BODY));
        pgeyColors.put("paneles", pgeyD.pgeyColor("raglan".equals(pgeyFamily) ? PgeyZone.JERSEY_SLEEVES : PgeyZone.JERSEY_SIDES));
        pgeyColors.put("short", pgeyD.pgeyColor(PgeyZone.SHORTS_BASE));
        pgeyColors.put("medias", pgeyD.pgeyColor(PgeyZone.SOCKS_LEG));
        if ((pgeyShirt || pgeyBuzo) && !"raglan".equals(pgeyFamily)
                && !pgeyD.pgeyColor(PgeyZone.JERSEY_SLEEVES).equalsIgnoreCase(pgeyD.pgeyColor(PgeyZone.JERSEY_BODY))) {
            pgeyNotes.add("Mangas de otro color: ver en Vista 3D");
        }
        if (pgeyShirt && (pgeyDiffers(pgeyD, PgeyZone.JERSEY_COLLAR, PgeyZone.JERSEY_BODY) || pgeyDiffers(pgeyD, PgeyZone.JERSEY_CUFFS, PgeyZone.JERSEY_BODY))) {
            pgeyNotes.add("Color del cuello y los puños: ver en Vista 3D");
        }
        if (pgeyZones.contains("short") && (pgeyDiffers(pgeyD, PgeyZone.SHORTS_STRIPE, PgeyZone.SHORTS_BASE)
                || pgeyDiffers(pgeyD, PgeyZone.SHORTS_WAIST, PgeyZone.SHORTS_BASE))) {
            pgeyNotes.add("Franja y pretina del short: ver en Vista 3D");
        }
        if (pgeyZones.contains("medias") && pgeyDiffers(pgeyD, PgeyZone.SOCKS_BAND, PgeyZone.SOCKS_LEG)) {
            pgeyNotes.add("Banda de las medias: ver en Vista 3D");
        }

        PgeyPatternSpec pgeyP = pgeyD.pgeyPattern(HepyGarment.JERSEY);
        String pgeyCode;
        String pgeyPatternColor = pgeyP.pgeyColor();
        switch (pgeyP.pgeyType()) {
            case LISO -> {
                pgeyCode = PGEY_PHOTO_ONLY_PATTERNS.contains(pgeyStored.pgeyPattern()) ? pgeyStored.pgeyPattern() : "liso";
                if (!"liso".equals(pgeyCode) && pgeyStored.pgeyPatternColor() != null) {
                    pgeyPatternColor = pgeyStored.pgeyPatternColor();
                }
            }
            case FRANJAS_VERTICALES -> pgeyCode = "rayas";
            case FRANJAS_HORIZONTALES -> pgeyCode = "franjas";
            default -> {
                pgeyCode = "liso";
                if (pgeyShirt) {
                    pgeyNotes.add("Diseño " + pgeyP.pgeyType().pgeyLabel().toLowerCase() + ": ver en Vista 3D");
                }
            }
        }
        if (pgeyZones.contains("short") && pgeyD.pgeyPattern(HepyGarment.SHORTS).pgeyType() != PgeyPatternType.LISO) {
            pgeyNotes.add("Patrón del short: ver en Vista 3D");
        }
        if (pgeyZones.contains("medias") && pgeyD.pgeyPattern(HepyGarment.SOCKS).pgeyType() != PgeyPatternType.LISO) {
            pgeyNotes.add("Patrón de las medias: ver en Vista 3D");
        }

        PgeyDesignElement pgeyNameEl = pgeyVisible(PgeyTextSlot.BACK_NAME.pgeyFind(pgeyD));
        PgeyDesignElement pgeyBackEl = pgeyVisible(PgeyTextSlot.BACK_NUMBER.pgeyFind(pgeyD));
        PgeyDesignElement pgeyChestEl = pgeyVisible(PgeyTextSlot.CHEST_NUMBER.pgeyFind(pgeyD));
        PgeyRealisticView.PgeyText pgeyName = null;
        PgeyRealisticView.PgeyText pgeyBack = null;
        PgeyRealisticView.PgeyText pgeyChest = null;
        if (pgeyShirt) {
            pgeyName = pgeyText(pgeyNameEl);
            pgeyBack = pgeyText(pgeyBackEl);
            pgeyChest = pgeyText(pgeyChestEl);
            pgeyPosition(pgeyNotes, pgeyD, PgeyTextSlot.BACK_NAME, pgeyNameEl, "Posición del nombre");
            pgeyPosition(pgeyNotes, pgeyD, PgeyTextSlot.BACK_NUMBER, pgeyBackEl, "Posición del número de la espalda");
            pgeyPosition(pgeyNotes, pgeyD, PgeyTextSlot.CHEST_NUMBER, pgeyChestEl, "Posición del número del pecho");
            if (!PgeyRealisticModels.pgeyHasBack(pgeyModel) && (pgeyName != null || pgeyBack != null)) {
                pgeyNotes.add("Nombre y número de la espalda: ver en Vista 3D");
            }
        } else if (pgeyNameEl != null || pgeyBackEl != null || pgeyChestEl != null) {
            pgeyNotes.add("Nombre y números: ver en Vista 3D");
        }
        if (pgeyZones.contains("short") && pgeyVisible(PgeyTextSlot.SHORTS_NUMBER.pgeyFind(pgeyD)) != null) {
            pgeyNotes.add("Número del short: ver en Vista 3D");
        }
        List<PgeyDesignElement> pgeyOthers = pgeyD.pgeyElements().stream().filter(pgeyE -> pgeyE.pgeyVisible() && pgeyE.pgeyType().pgeyIsText()
                && pgeyE != pgeyNameEl && pgeyE != pgeyBackEl && pgeyE != pgeyChestEl && PgeyTextSlot.pgeyOf(pgeyE) != PgeyTextSlot.SHORTS_NUMBER).toList();
        if (!pgeyOthers.isEmpty()) {
            pgeyNotes.add("Otros textos: ver en Vista 3D");
        }

        List<PgeyDesignElement> pgeyGraphics = pgeyD.pgeyElements().stream().filter(pgeyE -> pgeyE.pgeyVisible() && !pgeyE.pgeyType().pgeyIsText()).toList();
        PgeyDesignElement pgeyCrest = pgeyShirt ? pgeyGraphics.stream().filter(pgeyE -> pgeyE.pgeyArea() == PgeyArea.JERSEY_FRONT).findFirst()
                .orElse(null) : null;
        if (pgeyCrest != null && !pgeyCrestAtDefault(pgeyCrest)) {
            pgeyNotes.add("Posición del escudo: ver en Vista 3D");
        }
        if (pgeyGraphics.size() > (pgeyCrest == null ? 0 : 1)) {
            pgeyNotes.add("Otros escudos o logos: ver en Vista 3D");
        }

        Set<String> pgeyFabrics = new LinkedHashSet<>();
        for (PgeyMaterialSlot pgeySlot : List.of(PgeyMaterialSlot.JERSEY, PgeyMaterialSlot.SHORTS)) {
            String pgeyM = pgeyD.pgeyMaterial(pgeySlot);
            if (!pgeyM.equals(pgeySlot.pgeyDefaultCode())) {
                pgeyFabrics.add(PGEY_FABRIC_LABELS.getOrDefault(pgeyM, pgeyM));
            }
        }
        pgeyFabrics.forEach(pgeyF -> pgeyNotes.add("Tela " + pgeyF.toLowerCase() + ": ver en Vista 3D"));

        String pgeyNameText = pgeyName == null ? null : pgeyName.pgeyValue();
        Integer pgeyNumber = pgeyBack != null ? pgeyParse(pgeyBack.pgeyValue()) : pgeyChest != null ? pgeyParse(pgeyChest.pgeyValue()) : null;
        PgeyRealisticSpec pgeySpec = new PgeyRealisticSpec(pgeyModel, pgeyColors, pgeyCode, pgeyPatternColor, pgeyNameText, pgeyNumber);
        return new PgeyRealisticView(pgeySpec, pgeyName, pgeyBack, pgeyChest, pgeyCrest, new ArrayList<>(pgeyNotes));
    }

    public static PgeyDesignDocument pgeyNormalize(PgeyDesignDocument pgeyD) {
        PgeyRealisticSpec pgeyS = pgeyView(pgeyD).pgeySpec();
        return pgeyS.equals(pgeyD.pgeyRealistic()) ? pgeyD : pgeyD.pgeyWithRealistic(pgeyS);
    }

    public static boolean pgeyRealisticMatches(PgeyDesignDocument pgeyD) {
        return pgeyView(pgeyD).pgeyNotices().isEmpty();
    }

    public static List<String> pgeyNotices3d(PgeyDesignDocument pgeyD) {
        List<String> pgeyOut = new ArrayList<>();
        PgeyRealisticSpec pgeyStored = pgeyD.pgeyRealisticOrDefault();
        String pgeyFamily = pgeyFamily(pgeyStored.pgeyModelKey());
        if (PGEY_CUT_LABELS.containsKey(pgeyFamily) && !PGEY_FOOTBALL.contains(pgeyFamily)) {
            pgeyOut.add(switch (pgeyFamily) {
                case "buzo" -> "Buzo: la Vista 3D muestra camiseta de manga larga y short largo, lo más parecido.";
                case "casaca" -> "Casaca: la Vista 3D muestra camiseta de manga larga, lo más parecido.";
                case "arquero", "compresion" -> PGEY_CUT_LABELS.get(pgeyFamily) + ": la Vista 3D muestra la camiseta de "
                        + "fútbol de manga larga, el corte más parecido.";
                default -> PGEY_CUT_LABELS.get(pgeyFamily) + ": la Vista 3D muestra la camiseta de fútbol cuello V, el corte "
                        + "más parecido.";
            });
        }
        PgeyRealisticView pgeyV = pgeyView(pgeyD);
        if (PGEY_PHOTO_ONLY_PATTERNS.contains(pgeyV.pgeySpec().pgeyPattern())) {
            pgeyOut.add("Diseño " + PGEY_PATTERN_LABELS.get(pgeyV.pgeySpec().pgeyPattern()).toLowerCase() + ": solo se ve en la Vista "
                    + "realista.");
        }
        return pgeyOut;
    }

    public static PgeyDesignDocument pgeyApplyRealistic(PgeyDesignDocument pgeyD, PgeyRealisticSpec pgeyBefore, PgeyRealisticSpec pgeyAfter) {
        if (pgeyAfter.equals(pgeyBefore)) {
            return pgeyNormalize(pgeyD);
        }
        PgeyDesignDocument pgeyOut = pgeyD;
        String pgeyKey = pgeyD.pgeyRealisticOrDefault().pgeyModelKey();
        boolean pgeyPersonaOnly = PGEY_FOOTBALL.contains(pgeyFamily(pgeyAfter.pgeyModelKey())) && PGEY_FOOTBALL.contains(pgeyFamily(pgeyBefore.pgeyModelKey()))
                && !Objects.equals(pgeyAudience(pgeyAfter.pgeyModelKey()), pgeyAudience(pgeyBefore.pgeyModelKey()));
        if (pgeyPersonaOnly) {
            pgeyKey = pgeyKey("futbol", pgeyAudience(pgeyAfter.pgeyModelKey()));
        } else if (!pgeyAfter.pgeyModelKey().equals(pgeyBefore.pgeyModelKey())) {
            pgeyKey = pgeyAfter.pgeyModelKey();
            String pgeyFamily = pgeyFamily(pgeyKey);
            PgeyGarmentCut pgeyC = pgeyOut.pgeyCut();
            pgeyC = switch (pgeyFamily) {
                case "futbol" -> pgeyC.pgeyWithSleeve(PgeySleeve.CORTA).pgeyWithShorts(pgeyC.pgeyShorts() == PgeyShortsLength.LARGO
                        ? PgeyShortsLength.MEDIO : pgeyC.pgeyShorts());
                case "mangalarga" -> pgeyC.pgeyWithSleeve(PgeySleeve.LARGA);
                case "shortlargo" -> pgeyC.pgeyWithSleeve(PgeySleeve.CORTA).pgeyWithShorts(PgeyShortsLength.LARGO);
                default -> PGEY_CUT_LABELS.containsKey(pgeyFamily) ? pgeyNativeCut(pgeyFamily, pgeyAudience(pgeyKey), pgeyC) : pgeyC;
            };
            pgeyOut = pgeyOut.pgeyWithCut(pgeyC);
        }
        List<String> pgeyZones = PgeyRealisticModels.pgeyZones(pgeyPhotoModel(pgeyKey, pgeyOut.pgeyCut()));
        boolean pgeyRaglan = "raglan".equals(pgeyFamily(pgeyKey));
        for (String pgeyZ : List.of("camiseta", "principal", "paneles", "short", "medias")) {
            String pgeyNow = pgeyAfter.pgeyZoneColors().get(pgeyZ);
            if (pgeyNow == null || pgeyNow.equalsIgnoreCase(pgeyBefore.pgeyZoneColors().getOrDefault(pgeyZ, ""))) {
                continue;
            }
            switch (pgeyZ) {
                case "camiseta", "principal" -> {
                    if (pgeyZones.isEmpty() || pgeyZones.contains(pgeyZ)) {
                        pgeyOut = pgeyOut.pgeyWithColor(PgeyZone.JERSEY_BODY, pgeyNow).pgeyWithColor(PgeyZone.JERSEY_SLEEVES, pgeyNow)
                                .pgeyWithColor(PgeyZone.JERSEY_COLLAR, pgeyNow).pgeyWithColor(PgeyZone.JERSEY_CUFFS, pgeyNow);
                    }
                }
                case "paneles" -> pgeyOut = pgeyOut.pgeyWithColor(pgeyRaglan ? PgeyZone.JERSEY_SLEEVES : PgeyZone.JERSEY_SIDES, pgeyNow);
                case "short" -> pgeyOut = pgeyOut.pgeyWithColor(PgeyZone.SHORTS_BASE, pgeyNow).pgeyWithColor(PgeyZone.SHORTS_STRIPE, pgeyNow)
                        .pgeyWithColor(PgeyZone.SHORTS_WAIST, pgeyNow);
                default -> pgeyOut = pgeyOut.pgeyWithColor(PgeyZone.SOCKS_LEG, pgeyNow).pgeyWithColor(PgeyZone.SOCKS_BAND, pgeyNow);
            }
        }
        String pgeyStoredPattern = pgeyD.pgeyRealisticOrDefault().pgeyPattern();
        if (!pgeyAfter.pgeyPattern().equals(pgeyBefore.pgeyPattern())
                || !Objects.equals(pgeyAfter.pgeyPatternColor(), pgeyBefore.pgeyPatternColor())) {
            PgeyPatternSpec pgeyP = pgeyOut.pgeyPattern(HepyGarment.JERSEY);
            pgeyP = switch (pgeyAfter.pgeyPattern()) {
                case "rayas" -> pgeyP.pgeyWithType(PgeyPatternType.FRANJAS_VERTICALES);
                case "franjas" -> pgeyP.pgeyWithType(PgeyPatternType.FRANJAS_HORIZONTALES);
                default -> pgeyP.pgeyWithType(PgeyPatternType.LISO);
            };
            if (pgeyAfter.pgeyPatternColor() != null) {
                pgeyP = pgeyP.pgeyWithColor(pgeyAfter.pgeyPatternColor());
            }
            pgeyOut = pgeyOut.pgeyWithPattern(HepyGarment.JERSEY, pgeyP);
            pgeyStoredPattern = pgeyAfter.pgeyPattern();
        }
        if (!Objects.equals(pgeyAfter.pgeyName(), pgeyBefore.pgeyName())) {
            String pgeyN = pgeyAfter.pgeyName() == null ? null : PgeyDesignRules.pgeyCleanName(pgeyAfter.pgeyName());
            pgeyOut = pgeySetText(pgeyOut, PgeyTextSlot.BACK_NAME, pgeyN == null || pgeyN.isBlank() ? null : pgeyN);
        }
        if (!Objects.equals(pgeyAfter.pgeyNumber(), pgeyBefore.pgeyNumber())) {
            String pgeyN = pgeyAfter.pgeyNumber() == null ? null : String.valueOf(pgeyAfter.pgeyNumber());
            pgeyOut = pgeySetText(pgeySetText(pgeyOut, PgeyTextSlot.BACK_NUMBER, pgeyN), PgeyTextSlot.CHEST_NUMBER, pgeyN);
        }
        PgeyRealisticSpec pgeyBase = pgeyOut.pgeyRealisticOrDefault();
        pgeyOut = pgeyOut.pgeyWithRealistic(new PgeyRealisticSpec(pgeyKey, pgeyBase.pgeyZoneColors(), pgeyStoredPattern,
                pgeyAfter.pgeyPatternColor() != null ? pgeyAfter.pgeyPatternColor() : pgeyBase.pgeyPatternColor(), pgeyBase.pgeyName(), pgeyBase.pgeyNumber()));
        return pgeyNormalize(pgeyOut);
    }

    public static PgeyDesignDocument pgeySeedFromRealistic(PgeyDesignDocument pgeyD, PgeyRealisticSpec pgeyR) {
        String pgeyShirt = pgeyR.pgeyZoneColors().getOrDefault("camiseta", pgeyR.pgeyZoneColors().get("principal"));
        if (pgeyShirt != null) {
            pgeyD = pgeyD.pgeyWithColor(PgeyZone.JERSEY_COLLAR, pgeyShirt).pgeyWithColor(PgeyZone.JERSEY_CUFFS, pgeyShirt);
        }
        if (pgeyR.pgeyZoneColors().containsKey("short")) {
            pgeyD = pgeyD.pgeyWithColor(PgeyZone.SHORTS_STRIPE, pgeyR.pgeyColor("short")).pgeyWithColor(PgeyZone.SHORTS_WAIST, pgeyR.pgeyColor("short"));
        }
        if (pgeyR.pgeyZoneColors().containsKey("medias")) {
            pgeyD = pgeyD.pgeyWithColor(PgeyZone.SOCKS_BAND, pgeyR.pgeyColor("medias"));
        }
        String pgeyFamily = pgeyFamily(pgeyR.pgeyModelKey());
        if (PGEY_CUT_LABELS.containsKey(pgeyFamily) && !PGEY_FOOTBALL.contains(pgeyFamily)) {
            pgeyD = pgeyD.pgeyWithCut(pgeyNativeCut(pgeyFamily, pgeyAudience(pgeyR.pgeyModelKey()), pgeyD.pgeyCut()));
        }
        PgeyDesignDocument pgeyWithLook = pgeyD.pgeyWithRealistic(new PgeyRealisticSpec(pgeyR.pgeyModelKey(), pgeyD.pgeyRealisticOrDefault().pgeyZoneColors(),
                PGEY_PHOTO_ONLY_PATTERNS.contains(pgeyR.pgeyPattern()) ? pgeyR.pgeyPattern() : "liso", pgeyR.pgeyPatternColor(), null, null));
        PgeyRealisticSpec pgeyBefore = pgeyView(pgeyWithLook).pgeySpec();
        return pgeyApplyRealistic(pgeyWithLook, pgeyBefore, new PgeyRealisticSpec(pgeyR.pgeyModelKey(), pgeyR.pgeyZoneColors(), pgeyR.pgeyPattern(),
                pgeyR.pgeyPatternColor(), pgeyR.pgeyName(), pgeyR.pgeyNumber()));
    }

    static String pgeyPhotoModel(String pgeyStored, PgeyGarmentCut pgeyCut) {
        String pgeyFamily = pgeyFamily(pgeyStored);
        String pgeyAudience = pgeyAudience(pgeyStored);
        if (pgeyAudience == null || !PGEY_FOOTBALL.contains(pgeyFamily)) {
            return pgeyStored;
        }
        String pgeyBest = "futbol";
        if (pgeyCut.pgeySleeve() == PgeySleeve.LARGA && PgeyRealisticModels.pgeyExists(pgeyKey("mangalarga", pgeyAudience))) {
            pgeyBest = "mangalarga";
        } else if (pgeyCut.pgeySleeve() == PgeySleeve.CORTA && pgeyCut.pgeyShorts() == PgeyShortsLength.LARGO
                && PgeyRealisticModels.pgeyExists(pgeyKey("shortlargo", pgeyAudience))) {
            pgeyBest = "shortlargo";
        }
        return PgeyRealisticModels.pgeyExists(pgeyKey(pgeyBest, pgeyAudience)) ? pgeyKey(pgeyBest, pgeyAudience) : pgeyStored;
    }

    static PgeyGarmentCut pgeyNativeCut(String pgeyFamily, String pgeyAudience, PgeyGarmentCut pgeyCurrent) {
        return switch (pgeyFamily) {
            case "mangalarga", "arquero", "compresion" -> new PgeyGarmentCut(PgeySleeve.LARGA, PgeyShortsLength.MEDIO,
                    pgeyCurrent.pgeySocks());
            case "casaca" -> new PgeyGarmentCut(PgeySleeve.LARGA, pgeyCurrent.pgeyShorts(), pgeyCurrent.pgeySocks());
            case "shortlargo", "basquet" -> new PgeyGarmentCut(PgeySleeve.CORTA, PgeyShortsLength.LARGO, pgeyCurrent.pgeySocks());
            case "licra" -> new PgeyGarmentCut(PgeySleeve.CORTA, PgeyShortsLength.CORTO, pgeyCurrent.pgeySocks());
            case "voley" -> new PgeyGarmentCut(PgeySleeve.CORTA, "mujer".equals(pgeyAudience) ? PgeyShortsLength.CORTO
                    : PgeyShortsLength.MEDIO, pgeyCurrent.pgeySocks());
            case "buzo" -> new PgeyGarmentCut(PgeySleeve.LARGA, PgeyShortsLength.LARGO, pgeyCurrent.pgeySocks());
            default -> new PgeyGarmentCut(PgeySleeve.CORTA, PgeyShortsLength.MEDIO, pgeyCurrent.pgeySocks());
        };
    }

    static String pgeyFamily(String pgeyKey) {
        return pgeyKey == null ? "" : pgeyKey.split("_")[0];
    }

    static String pgeyAudience(String pgeyKey) {
        String[] pgeyParts = pgeyKey == null ? new String[0] : pgeyKey.split("_");
        return pgeyParts.length >= 3 ? pgeyParts[1] : null;
    }

    static String pgeyKey(String pgeyFamily, String pgeyAudience) {
        return pgeyFamily + "_" + pgeyAudience + "_frente";
    }

    private static boolean pgeyDiffers(PgeyDesignDocument pgeyD, PgeyZone pgeyDetail, PgeyZone pgeyBase) {
        return !pgeyD.pgeyColor(pgeyDetail).equalsIgnoreCase(pgeyD.pgeyColor(pgeyBase));
    }

    private static PgeyDesignElement pgeyVisible(PgeyDesignElement pgeyE) {
        return pgeyE != null && pgeyE.pgeyVisible() ? pgeyE : null;
    }

    private static PgeyRealisticView.PgeyText pgeyText(PgeyDesignElement pgeyE) {
        if (pgeyE == null || pgeyE.pgeyText() == null || pgeyE.pgeyText().isBlank()) {
            return null;
        }
        return new PgeyRealisticView.PgeyText(pgeyE.pgeyText(), pgeyE.pgeyColor() == null ? "#FFFFFF" : pgeyE.pgeyColor(),
                pgeyE.pgeyOutline() > 0 ? pgeyE.pgeyAccentColor() : null, pgeyE.pgeyFont());
    }

    private static void pgeyPosition(Set<String> pgeyNotes, PgeyDesignDocument pgeyD, PgeyTextSlot pgeySlot, PgeyDesignElement pgeyE, String pgeyWhat) {
        if (pgeyE != null && !pgeySlot.pgeyAtDefault(pgeyE, pgeyD)) {
            pgeyNotes.add(pgeyWhat + ": ver en Vista 3D");
        }
    }

    private static boolean pgeyCrestAtDefault(PgeyDesignElement pgeyE) {
        PgeyDesignElement pgeyDef = PgeyDesignDefaults.pgeyEmblem("x", "#000000", "#000000");
        return Math.abs(pgeyE.pgeyU() - pgeyDef.pgeyU()) < 0.02 && Math.abs(pgeyE.pgeyV() - pgeyDef.pgeyV()) < 0.02 && Math.abs(pgeyE.pgeyScale() - 1) < 0.05
                && Math.abs(pgeyE.pgeyRotation()) < 2;
    }

    private static PgeyDesignDocument pgeySetText(PgeyDesignDocument pgeyDoc, PgeyTextSlot pgeySlot, String pgeyValue) {
        PgeyDesignElement pgeyE = pgeySlot.pgeyFind(pgeyDoc);
        if (pgeyValue == null) {
            return pgeyE == null ? pgeyDoc : pgeyDoc.pgeyRemoveElement(pgeyE.pgeyId());
        }
        if (pgeyE == null) {
            return pgeyDoc.pgeyAddElement(pgeySlot.pgeyCreate(pgeyDoc).pgeyWithText(pgeyValue));
        }
        return pgeyValue.equals(pgeyE.pgeyText()) ? pgeyDoc : pgeyDoc.pgeyReplaceElement(pgeyE.pgeyWithText(pgeyValue).pgeyWithVisible(true));
    }

    private static Integer pgeyParse(String pgeyDigits) {
        try {
            int pgeyN = Integer.parseInt(pgeyDigits);
            return pgeyN >= 0 && pgeyN <= 99 ? pgeyN : null;
        } catch (NumberFormatException pgeyE) {
            return null;
        }
    }

    private static Map<String, String> pgeyCutLabels() {
        Map<String, String> pgeyM = new LinkedHashMap<>();
        pgeyM.put("futbol", "Fútbol cuello V");
        pgeyM.put("polo", "Polo");
        pgeyM.put("redondo", "Cuello redondo");
        pgeyM.put("raglan", "Raglán");
        pgeyM.put("mangalarga", "Manga larga");
        pgeyM.put("voley", "Vóley");
        pgeyM.put("basquet", "Básquet");
        pgeyM.put("buzo", "Buzo");
        pgeyM.put("shortlargo", "Short largo");
        pgeyM.put("licra", "Licra");
        pgeyM.put("arquero", "Arquero");
        pgeyM.put("compresion", "Compresión");
        pgeyM.put("casaca", "Casaca");
        return java.util.Collections.unmodifiableMap(pgeyM);
    }
}
