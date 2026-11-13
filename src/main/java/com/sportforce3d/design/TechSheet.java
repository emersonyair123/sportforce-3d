package com.sportforce3d.design;

import com.sportforce3d.model.Garment;
import com.sportforce3d.model.Material;
import com.sportforce3d.three.model.Zone;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record TechSheet(List<ZoneRow> zones, List<MaterialRow> materials, List<PatternRow> patterns,
                        List<ElementRow> elements, Realistic realistic, GarmentCut cut) {
    public record Realistic(String model, Map<String, String> zones, String pattern, String patternColor,
                            String name, Integer number) {
    }

    public record ZoneRow(Garment garment, Zone zone, String hex) {
    }

    public record MaterialRow(MaterialSlot slot, String code, String name, long surchargeCents) {
    }

    public record PatternRow(Garment garment, PatternType type, String color, double widthCm) {
    }

    public record ElementRow(int layer, ElementType type, String content, Area area, double u, double v,
                             double scalePercent, double rotationDeg, double heightCm, String font, String color,
                             String accentColor, double outlineMm, boolean visible, Long assetId, String assetName) {
    }

    public static TechSheet of(DesignDocument doc, Map<String, Material> materials, Map<Long, String> assetNames) {
        List<ZoneRow> zones = new ArrayList<>();
        for (Zone z : Zone.values()) {
            if (z.garment() != null && z.garment() != Garment.ACCESSORY) {
                zones.add(new ZoneRow(z.garment(), z, doc.color(z)));
            }
        }
        List<MaterialRow> mats = new ArrayList<>();
        for (MaterialSlot slot : MaterialSlot.values()) {
            String code = doc.material(slot);
            Material m = materials.get(code);
            mats.add(new MaterialRow(slot, code, m == null ? code : m.name(), m == null ? 0 : m.surchargeCents()));
        }
        List<PatternRow> patterns = new ArrayList<>();
        for (Garment g : List.of(Garment.JERSEY, Garment.SHORTS, Garment.SOCKS)) {
            PatternSpec p = doc.pattern(g);
            patterns.add(new PatternRow(g, p.type(), p.color(), p.width() * 100));
        }
        List<ElementRow> elements = new ArrayList<>();
        int layer = 1;
        for (DesignElement e : doc.elements()) {
            String content = switch (e.type()) {
                case NAME, NUMBER -> e.text();
                case EMBLEM -> EmblemCatalog.label(e.emblem()) + " (" + e.emblem() + ")";
                case LOGO -> assetNames.getOrDefault(e.assetId(), "logo #" + e.assetId());
            };
            elements.add(new ElementRow(layer++, e.type(), content, e.area(), e.u(), e.v(), e.scale() * 100,
                    e.rotation(), e.height() * e.scale() * 100, e.type().isText() ? DesignFonts.LABELS.get(e.font()) : null,
                    e.color(), e.accentColor(), e.outline() * 1000, e.visible(), e.assetId(),
                    e.assetId() == null ? null : assetNames.get(e.assetId())));
        }
        RealisticSpec r = doc.realistic();
        Realistic realistic = r == null ? null : new Realistic(r.modelKey(), r.zoneColors(), r.pattern(),
                r.plain() ? null : r.patternColor(), r.name(), r.number());
        return new TechSheet(zones, mats, patterns, elements, realistic, doc.cut());
    }

    public String toCsv(String title) {
        StringBuilder sb = new StringBuilder("﻿");
        line(sb, "Ficha técnica", title);
        sb.append('\n');
        line(sb, "CORTE", "Manga", cut.sleeve().label());
        line(sb, "", "Largo del short", cut.shorts().label() + " (" + cut.shorts().detail() + ")");
        line(sb, "", "Largo de las medias", cut.socks().label() + " (" + cut.socks().detail() + ")");
        sb.append('\n');
        line(sb, "ZONAS", "Prenda", "Zona", "Color HEX");
        for (ZoneRow z : zones) {
            line(sb, "", z.garment().label(), z.zone().label(), z.hex());
        }
        sb.append('\n');
        line(sb, "MATERIALES", "Parte", "Código", "Material");
        for (MaterialRow m : materials) {
            line(sb, "", m.slot().label(), m.code(), m.name());
        }
        sb.append('\n');
        line(sb, "PATRONES", "Prenda", "Patrón", "Color HEX", "Ancho (cm)");
        for (PatternRow p : patterns) {
            line(sb, "", p.garment().label(), p.type().label(), p.type() == PatternType.LISO ? "" : p.color(),
                    p.type() == PatternType.LISO ? "" : num(p.widthCm()));
        }
        sb.append('\n');
        if (realistic != null) {
            line(sb, "VISTA REALISTA", "Modelo", realistic.model());
            for (Map.Entry<String, String> z : realistic.zones().entrySet()) {
                line(sb, "", "Zona " + z.getKey(), z.getValue());
            }
            line(sb, "", "Diseño", realistic.pattern(), realistic.patternColor());
            line(sb, "", "Nombre", realistic.name());
            line(sb, "", "Número", realistic.number() == null ? "" : String.valueOf(realistic.number()));
            sb.append('\n');
        }
        line(sb, "ELEMENTOS", "Capa", "Tipo", "Contenido", "Zona", "Posición u", "Posición v", "Escala (%)",
                "Rotación (°)", "Alto (cm)", "Tipografía", "Color", "Contorno / acento", "Contorno (mm)", "Visible");
        for (ElementRow e : elements) {
            line(sb, "", String.valueOf(e.layer()), e.type().label(), e.content(), e.area().label(), num(e.u()),
                    num(e.v()), num(e.scalePercent()), num(e.rotationDeg()), num(e.heightCm()), e.font(), e.color(),
                    e.accentColor(), e.type().isText() ? num(e.outlineMm()) : "", e.visible() ? "Sí" : "No");
        }
        return sb.toString();
    }

    private static String num(double d) {
        return String.format(Locale.forLanguageTag("es-PE"), "%.2f", d);
    }

    private static void line(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            String c = cells[i] == null ? "" : cells[i];
            if (c.contains(";") || c.contains("\"") || c.contains("\n")) {
                c = "\"" + c.replace("\"", "\"\"") + "\"";
            }
            sb.append(c);
        }
        sb.append('\n');
    }
}
