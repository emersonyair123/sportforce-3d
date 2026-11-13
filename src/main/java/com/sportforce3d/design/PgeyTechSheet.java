package com.sportforce3d.design;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.three.model.PgeyZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record PgeyTechSheet(List<PgeyZoneRow> pgeyZones, List<PgeyMaterialRow> pgeyMaterials, List<PgeyPatternRow> pgeyPatterns,
                        List<PgeyElementRow> pgeyElements, PgeyRealistic pgeyRealistic, PgeyGarmentCut pgeyCut) {
    public record PgeyRealistic(String pgeyModel, Map<String, String> pgeyZones, String pgeyPattern, String pgeyPatternColor,
                            String pgeyName, Integer pgeyNumber) {
    }

    public record PgeyZoneRow(HepyGarment pgeyGarment, PgeyZone pgeyZone, String pgeyHex) {
    }

    public record PgeyMaterialRow(PgeyMaterialSlot pgeySlot, String pgeyCode, String pgeyName, long pgeySurchargeCents) {
    }

    public record PgeyPatternRow(HepyGarment pgeyGarment, PgeyPatternType pgeyType, String pgeyColor, double pgeyWidthCm) {
    }

    public record PgeyElementRow(int pgeyLayer, PgeyElementType pgeyType, String pgeyContent, PgeyArea pgeyArea, double pgeyU, double pgeyV,
                             double pgeyScalePercent, double pgeyRotationDeg, double pgeyHeightCm, String pgeyFont, String pgeyColor,
                             String pgeyAccentColor, double pgeyOutlineMm, boolean pgeyVisible, Long pgeyAssetId, String pgeyAssetName) {
    }

    public static PgeyTechSheet pgeyOf(PgeyDesignDocument pgeyDoc, Map<String, HepyMaterial> pgeyMaterials, Map<Long, String> pgeyAssetNames) {
        List<PgeyZoneRow> pgeyZones = new ArrayList<>();
        for (PgeyZone pgeyZ : PgeyZone.values()) {
            if (pgeyZ.pgeyGarment() != null && pgeyZ.pgeyGarment() != HepyGarment.ACCESSORY) {
                pgeyZones.add(new PgeyZoneRow(pgeyZ.pgeyGarment(), pgeyZ, pgeyDoc.pgeyColor(pgeyZ)));
            }
        }
        List<PgeyMaterialRow> pgeyMats = new ArrayList<>();
        for (PgeyMaterialSlot pgeySlot : PgeyMaterialSlot.values()) {
            String pgeyCode = pgeyDoc.pgeyMaterial(pgeySlot);
            HepyMaterial pgeyM = pgeyMaterials.get(pgeyCode);
            pgeyMats.add(new PgeyMaterialRow(pgeySlot, pgeyCode, pgeyM == null ? pgeyCode : pgeyM.hepyName(), pgeyM == null ? 0 : pgeyM.hepySurchargeCents()));
        }
        List<PgeyPatternRow> pgeyPatterns = new ArrayList<>();
        for (HepyGarment pgeyG : List.of(HepyGarment.JERSEY, HepyGarment.SHORTS, HepyGarment.SOCKS)) {
            PgeyPatternSpec pgeyP = pgeyDoc.pgeyPattern(pgeyG);
            pgeyPatterns.add(new PgeyPatternRow(pgeyG, pgeyP.pgeyType(), pgeyP.pgeyColor(), pgeyP.pgeyWidth() * 100));
        }
        List<PgeyElementRow> pgeyElements = new ArrayList<>();
        int pgeyLayer = 1;
        for (PgeyDesignElement pgeyE : pgeyDoc.pgeyElements()) {
            String pgeyContent = switch (pgeyE.pgeyType()) {
                case NAME, NUMBER -> pgeyE.pgeyText();
                case EMBLEM -> PgeyEmblemCatalog.pgeyLabel(pgeyE.pgeyEmblem()) + " (" + pgeyE.pgeyEmblem() + ")";
                case LOGO -> pgeyAssetNames.getOrDefault(pgeyE.pgeyAssetId(), "logo #" + pgeyE.pgeyAssetId());
            };
            pgeyElements.add(new PgeyElementRow(pgeyLayer++, pgeyE.pgeyType(), pgeyContent, pgeyE.pgeyArea(), pgeyE.pgeyU(), pgeyE.pgeyV(), pgeyE.pgeyScale() * 100,
                    pgeyE.pgeyRotation(), pgeyE.pgeyHeight() * pgeyE.pgeyScale() * 100, pgeyE.pgeyType().pgeyIsText() ? PgeyDesignFonts.PGEY_LABELS.get(pgeyE.pgeyFont()) : null,
                    pgeyE.pgeyColor(), pgeyE.pgeyAccentColor(), pgeyE.pgeyOutline() * 1000, pgeyE.pgeyVisible(), pgeyE.pgeyAssetId(),
                    pgeyE.pgeyAssetId() == null ? null : pgeyAssetNames.get(pgeyE.pgeyAssetId())));
        }
        PgeyRealisticSpec pgeyR = pgeyDoc.pgeyRealistic();
        PgeyRealistic pgeyRealistic = pgeyR == null ? null : new PgeyRealistic(pgeyR.pgeyModelKey(), pgeyR.pgeyZoneColors(), pgeyR.pgeyPattern(),
                pgeyR.pgeyPlain() ? null : pgeyR.pgeyPatternColor(), pgeyR.pgeyName(), pgeyR.pgeyNumber());
        return new PgeyTechSheet(pgeyZones, pgeyMats, pgeyPatterns, pgeyElements, pgeyRealistic, pgeyDoc.pgeyCut());
    }

    public String pgeyToCsv(String pgeyTitle) {
        StringBuilder pgeySb = new StringBuilder("﻿");
        pgeyLine(pgeySb, "Ficha técnica", pgeyTitle);
        pgeySb.append('\n');
        pgeyLine(pgeySb, "CORTE", "Manga", pgeyCut.pgeySleeve().pgeyLabel());
        pgeyLine(pgeySb, "", "Largo del short", pgeyCut.pgeyShorts().pgeyLabel() + " (" + pgeyCut.pgeyShorts().pgeyDetail() + ")");
        pgeyLine(pgeySb, "", "Largo de las medias", pgeyCut.pgeySocks().pgeyLabel() + " (" + pgeyCut.pgeySocks().pgeyDetail() + ")");
        pgeySb.append('\n');
        pgeyLine(pgeySb, "ZONAS", "Prenda", "Zona", "Color HEX");
        for (PgeyZoneRow pgeyZ : pgeyZones) {
            pgeyLine(pgeySb, "", pgeyZ.pgeyGarment().hepyLabel(), pgeyZ.pgeyZone().pgeyLabel(), pgeyZ.pgeyHex());
        }
        pgeySb.append('\n');
        pgeyLine(pgeySb, "MATERIALES", "Parte", "Código", "Material");
        for (PgeyMaterialRow pgeyM : pgeyMaterials) {
            pgeyLine(pgeySb, "", pgeyM.pgeySlot().pgeyLabel(), pgeyM.pgeyCode(), pgeyM.pgeyName());
        }
        pgeySb.append('\n');
        pgeyLine(pgeySb, "PATRONES", "Prenda", "Patrón", "Color HEX", "Ancho (cm)");
        for (PgeyPatternRow pgeyP : pgeyPatterns) {
            pgeyLine(pgeySb, "", pgeyP.pgeyGarment().hepyLabel(), pgeyP.pgeyType().pgeyLabel(), pgeyP.pgeyType() == PgeyPatternType.LISO ? "" : pgeyP.pgeyColor(),
                    pgeyP.pgeyType() == PgeyPatternType.LISO ? "" : pgeyNum(pgeyP.pgeyWidthCm()));
        }
        pgeySb.append('\n');
        if (pgeyRealistic != null) {
            pgeyLine(pgeySb, "VISTA REALISTA", "Modelo", pgeyRealistic.pgeyModel());
            for (Map.Entry<String, String> pgeyZ : pgeyRealistic.pgeyZones().entrySet()) {
                pgeyLine(pgeySb, "", "Zona " + pgeyZ.getKey(), pgeyZ.getValue());
            }
            pgeyLine(pgeySb, "", "Diseño", pgeyRealistic.pgeyPattern(), pgeyRealistic.pgeyPatternColor());
            pgeyLine(pgeySb, "", "Nombre", pgeyRealistic.pgeyName());
            pgeyLine(pgeySb, "", "Número", pgeyRealistic.pgeyNumber() == null ? "" : String.valueOf(pgeyRealistic.pgeyNumber()));
            pgeySb.append('\n');
        }
        pgeyLine(pgeySb, "ELEMENTOS", "Capa", "Tipo", "Contenido", "Zona", "Posición u", "Posición v", "Escala (%)",
                "Rotación (°)", "Alto (cm)", "Tipografía", "Color", "Contorno / acento", "Contorno (mm)", "Visible");
        for (PgeyElementRow pgeyE : pgeyElements) {
            pgeyLine(pgeySb, "", String.valueOf(pgeyE.pgeyLayer()), pgeyE.pgeyType().pgeyLabel(), pgeyE.pgeyContent(), pgeyE.pgeyArea().pgeyLabel(), pgeyNum(pgeyE.pgeyU()),
                    pgeyNum(pgeyE.pgeyV()), pgeyNum(pgeyE.pgeyScalePercent()), pgeyNum(pgeyE.pgeyRotationDeg()), pgeyNum(pgeyE.pgeyHeightCm()), pgeyE.pgeyFont(), pgeyE.pgeyColor(),
                    pgeyE.pgeyAccentColor(), pgeyE.pgeyType().pgeyIsText() ? pgeyNum(pgeyE.pgeyOutlineMm()) : "", pgeyE.pgeyVisible() ? "Sí" : "No");
        }
        return pgeySb.toString();
    }

    private static String pgeyNum(double pgeyD) {
        return String.format(Locale.forLanguageTag("es-PE"), "%.2f", pgeyD);
    }

    private static void pgeyLine(StringBuilder pgeySb, String... pgeyCells) {
        for (int pgeyI = 0; pgeyI < pgeyCells.length; pgeyI++) {
            if (pgeyI > 0) {
                pgeySb.append(';');
            }
            String pgeyC = pgeyCells[pgeyI] == null ? "" : pgeyCells[pgeyI];
            if (pgeyC.contains(";") || pgeyC.contains("\"") || pgeyC.contains("\n")) {
                pgeyC = "\"" + pgeyC.replace("\"", "\"\"") + "\"";
            }
            pgeySb.append(pgeyC);
        }
        pgeySb.append('\n');
    }
}
