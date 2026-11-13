package com.sportforce3d.design;

import com.sportforce3d.design.GarmentCut.ShortsLength;
import com.sportforce3d.design.GarmentCut.Sleeve;
import com.sportforce3d.design.GarmentCut.SocksLength;
import com.sportforce3d.model.Garment;
import com.sportforce3d.three.model.Zone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class ViewSync {
    static final Set<String> FOOTBALL = Set.of("futbol", "mangalarga", "shortlargo");
    static final Set<String> PHOTO_ONLY_PATTERNS = Set.of("dinamico", "geometrico", "minimal", "banda");

    public static final Map<String, String> CUT_LABELS = cutLabels();
    private static final Map<String, String> PATTERN_LABELS = Map.of("dinamico", "Dinámico", "geometrico",
            "Geométrico", "minimal", "Minimal", "banda", "Banda diagonal");
    private static final Map<String, String> FABRIC_LABELS = Map.of("DRY_FIT", "Dry-fit", "POLIESTER_LISO",
            "Poliéster liso", "ALGODON", "Algodón", "MALLA", "Malla deportiva", "JACQUARD", "Jacquard", "LICRA", "Licra");

    private ViewSync() {
    }

    public static RealisticView view(DesignDocument d) {
        RealisticSpec stored = d.realisticOrDefault();
        String model = photoModel(stored.modelKey(), d.cut());
        List<String> zones = RealisticModels.zones(model);
        String family = family(model);
        boolean shirt = zones.contains("camiseta");
        boolean buzo = zones.contains("principal");
        Set<String> notes = new LinkedHashSet<>();

        GarmentCut cut = d.cut();
        if (shirt && RealisticModels.exists(model)) {
            GarmentCut nat = nativeCut(family, audience(model), cut);
            if (cut.sleeve() != nat.sleeve()) {
                notes.add("Manga " + cut.sleeve().label().toLowerCase() + ": ver en Vista 3D");
            }
            if (zones.contains("short") && cut.shorts() != nat.shorts()) {
                notes.add("Short " + cut.shorts().label().toLowerCase() + ": ver en Vista 3D");
            }
            if (zones.contains("medias") && cut.socks() != SocksLength.LARGA) {
                notes.add((cut.socks() == SocksLength.TOBILLERA ? "Medias tobilleras" : "Medias de media pierna")
                        + ": ver en Vista 3D");
            }
        }

        Map<String, String> colors = new TreeMap<>();
        colors.put("camiseta", d.color(Zone.JERSEY_BODY));
        colors.put("principal", d.color(Zone.JERSEY_BODY));
        colors.put("paneles", d.color("raglan".equals(family) ? Zone.JERSEY_SLEEVES : Zone.JERSEY_SIDES));
        colors.put("short", d.color(Zone.SHORTS_BASE));
        colors.put("medias", d.color(Zone.SOCKS_LEG));
        if ((shirt || buzo) && !"raglan".equals(family)
                && !d.color(Zone.JERSEY_SLEEVES).equalsIgnoreCase(d.color(Zone.JERSEY_BODY))) {
            notes.add("Mangas de otro color: ver en Vista 3D");
        }
        if (shirt && (differs(d, Zone.JERSEY_COLLAR, Zone.JERSEY_BODY) || differs(d, Zone.JERSEY_CUFFS, Zone.JERSEY_BODY))) {
            notes.add("Color del cuello y los puños: ver en Vista 3D");
        }
        if (zones.contains("short") && (differs(d, Zone.SHORTS_STRIPE, Zone.SHORTS_BASE)
                || differs(d, Zone.SHORTS_WAIST, Zone.SHORTS_BASE))) {
            notes.add("Franja y pretina del short: ver en Vista 3D");
        }
        if (zones.contains("medias") && differs(d, Zone.SOCKS_BAND, Zone.SOCKS_LEG)) {
            notes.add("Banda de las medias: ver en Vista 3D");
        }

        PatternSpec p = d.pattern(Garment.JERSEY);
        String code;
        String patternColor = p.color();
        switch (p.type()) {
            case LISO -> {
                code = PHOTO_ONLY_PATTERNS.contains(stored.pattern()) ? stored.pattern() : "liso";
                if (!"liso".equals(code) && stored.patternColor() != null) {
                    patternColor = stored.patternColor();
                }
            }
            case FRANJAS_VERTICALES -> code = "rayas";
            case FRANJAS_HORIZONTALES -> code = "franjas";
            default -> {
                code = "liso";
                if (shirt) {
                    notes.add("Diseño " + p.type().label().toLowerCase() + ": ver en Vista 3D");
                }
            }
        }
        if (zones.contains("short") && d.pattern(Garment.SHORTS).type() != PatternType.LISO) {
            notes.add("Patrón del short: ver en Vista 3D");
        }
        if (zones.contains("medias") && d.pattern(Garment.SOCKS).type() != PatternType.LISO) {
            notes.add("Patrón de las medias: ver en Vista 3D");
        }

        DesignElement nameEl = visible(TextSlot.BACK_NAME.find(d));
        DesignElement backEl = visible(TextSlot.BACK_NUMBER.find(d));
        DesignElement chestEl = visible(TextSlot.CHEST_NUMBER.find(d));
        RealisticView.Text name = null;
        RealisticView.Text back = null;
        RealisticView.Text chest = null;
        if (shirt) {
            name = text(nameEl);
            back = text(backEl);
            chest = text(chestEl);
            position(notes, d, TextSlot.BACK_NAME, nameEl, "Posición del nombre");
            position(notes, d, TextSlot.BACK_NUMBER, backEl, "Posición del número de la espalda");
            position(notes, d, TextSlot.CHEST_NUMBER, chestEl, "Posición del número del pecho");
            if (!RealisticModels.hasBack(model) && (name != null || back != null)) {
                notes.add("Nombre y número de la espalda: ver en Vista 3D");
            }
        } else if (nameEl != null || backEl != null || chestEl != null) {
            notes.add("Nombre y números: ver en Vista 3D");
        }
        if (zones.contains("short") && visible(TextSlot.SHORTS_NUMBER.find(d)) != null) {
            notes.add("Número del short: ver en Vista 3D");
        }
        List<DesignElement> others = d.elements().stream().filter(e -> e.visible() && e.type().isText()
                && e != nameEl && e != backEl && e != chestEl && TextSlot.of(e) != TextSlot.SHORTS_NUMBER).toList();
        if (!others.isEmpty()) {
            notes.add("Otros textos: ver en Vista 3D");
        }

        List<DesignElement> graphics = d.elements().stream().filter(e -> e.visible() && !e.type().isText()).toList();
        DesignElement crest = shirt ? graphics.stream().filter(e -> e.area() == Area.JERSEY_FRONT).findFirst()
                .orElse(null) : null;
        if (crest != null && !crestAtDefault(crest)) {
            notes.add("Posición del escudo: ver en Vista 3D");
        }
        if (graphics.size() > (crest == null ? 0 : 1)) {
            notes.add("Otros escudos o logos: ver en Vista 3D");
        }

        Set<String> fabrics = new LinkedHashSet<>();
        for (MaterialSlot slot : List.of(MaterialSlot.JERSEY, MaterialSlot.SHORTS)) {
            String m = d.material(slot);
            if (!m.equals(slot.defaultCode())) {
                fabrics.add(FABRIC_LABELS.getOrDefault(m, m));
            }
        }
        fabrics.forEach(f -> notes.add("Tela " + f.toLowerCase() + ": ver en Vista 3D"));

        String nameText = name == null ? null : name.value();
        Integer number = back != null ? parse(back.value()) : chest != null ? parse(chest.value()) : null;
        RealisticSpec spec = new RealisticSpec(model, colors, code, patternColor, nameText, number);
        return new RealisticView(spec, name, back, chest, crest, new ArrayList<>(notes));
    }

    public static DesignDocument normalize(DesignDocument d) {
        RealisticSpec s = view(d).spec();
        return s.equals(d.realistic()) ? d : d.withRealistic(s);
    }

    public static boolean realisticMatches(DesignDocument d) {
        return view(d).notices().isEmpty();
    }

    public static List<String> notices3d(DesignDocument d) {
        List<String> out = new ArrayList<>();
        RealisticSpec stored = d.realisticOrDefault();
        String family = family(stored.modelKey());
        if (CUT_LABELS.containsKey(family) && !FOOTBALL.contains(family)) {
            out.add(switch (family) {
                case "buzo" -> "Buzo: la Vista 3D muestra camiseta de manga larga y short largo, lo más parecido.";
                case "casaca" -> "Casaca: la Vista 3D muestra camiseta de manga larga, lo más parecido.";
                case "arquero", "compresion" -> CUT_LABELS.get(family) + ": la Vista 3D muestra la camiseta de "
                        + "fútbol de manga larga, el corte más parecido.";
                default -> CUT_LABELS.get(family) + ": la Vista 3D muestra la camiseta de fútbol cuello V, el corte "
                        + "más parecido.";
            });
        }
        RealisticView v = view(d);
        if (PHOTO_ONLY_PATTERNS.contains(v.spec().pattern())) {
            out.add("Diseño " + PATTERN_LABELS.get(v.spec().pattern()).toLowerCase() + ": solo se ve en la Vista "
                    + "realista.");
        }
        return out;
    }

    public static DesignDocument applyRealistic(DesignDocument d, RealisticSpec before, RealisticSpec after) {
        if (after.equals(before)) {
            return normalize(d);
        }
        DesignDocument out = d;
        String key = d.realisticOrDefault().modelKey();
        boolean personaOnly = FOOTBALL.contains(family(after.modelKey())) && FOOTBALL.contains(family(before.modelKey()))
                && !Objects.equals(audience(after.modelKey()), audience(before.modelKey()));
        if (personaOnly) {
            key = key("futbol", audience(after.modelKey()));
        } else if (!after.modelKey().equals(before.modelKey())) {
            key = after.modelKey();
            String family = family(key);
            GarmentCut c = out.cut();
            c = switch (family) {
                case "futbol" -> c.withSleeve(Sleeve.CORTA).withShorts(c.shorts() == ShortsLength.LARGO
                        ? ShortsLength.MEDIO : c.shorts());
                case "mangalarga" -> c.withSleeve(Sleeve.LARGA);
                case "shortlargo" -> c.withSleeve(Sleeve.CORTA).withShorts(ShortsLength.LARGO);
                default -> CUT_LABELS.containsKey(family) ? nativeCut(family, audience(key), c) : c;
            };
            out = out.withCut(c);
        }
        List<String> zones = RealisticModels.zones(photoModel(key, out.cut()));
        boolean raglan = "raglan".equals(family(key));
        for (String z : List.of("camiseta", "principal", "paneles", "short", "medias")) {
            String now = after.zoneColors().get(z);
            if (now == null || now.equalsIgnoreCase(before.zoneColors().getOrDefault(z, ""))) {
                continue;
            }
            switch (z) {
                case "camiseta", "principal" -> {
                    if (zones.isEmpty() || zones.contains(z)) {
                        out = out.withColor(Zone.JERSEY_BODY, now).withColor(Zone.JERSEY_SLEEVES, now)
                                .withColor(Zone.JERSEY_COLLAR, now).withColor(Zone.JERSEY_CUFFS, now);
                    }
                }
                case "paneles" -> out = out.withColor(raglan ? Zone.JERSEY_SLEEVES : Zone.JERSEY_SIDES, now);
                case "short" -> out = out.withColor(Zone.SHORTS_BASE, now).withColor(Zone.SHORTS_STRIPE, now)
                        .withColor(Zone.SHORTS_WAIST, now);
                default -> out = out.withColor(Zone.SOCKS_LEG, now).withColor(Zone.SOCKS_BAND, now);
            }
        }
        String storedPattern = d.realisticOrDefault().pattern();
        if (!after.pattern().equals(before.pattern())
                || !Objects.equals(after.patternColor(), before.patternColor())) {
            PatternSpec p = out.pattern(Garment.JERSEY);
            p = switch (after.pattern()) {
                case "rayas" -> p.withType(PatternType.FRANJAS_VERTICALES);
                case "franjas" -> p.withType(PatternType.FRANJAS_HORIZONTALES);
                default -> p.withType(PatternType.LISO);
            };
            if (after.patternColor() != null) {
                p = p.withColor(after.patternColor());
            }
            out = out.withPattern(Garment.JERSEY, p);
            storedPattern = after.pattern();
        }
        if (!Objects.equals(after.name(), before.name())) {
            String n = after.name() == null ? null : DesignRules.cleanName(after.name());
            out = setText(out, TextSlot.BACK_NAME, n == null || n.isBlank() ? null : n);
        }
        if (!Objects.equals(after.number(), before.number())) {
            String n = after.number() == null ? null : String.valueOf(after.number());
            out = setText(setText(out, TextSlot.BACK_NUMBER, n), TextSlot.CHEST_NUMBER, n);
        }
        RealisticSpec base = out.realisticOrDefault();
        out = out.withRealistic(new RealisticSpec(key, base.zoneColors(), storedPattern,
                after.patternColor() != null ? after.patternColor() : base.patternColor(), base.name(), base.number()));
        return normalize(out);
    }

    public static DesignDocument seedFromRealistic(DesignDocument d, RealisticSpec r) {
        String shirt = r.zoneColors().getOrDefault("camiseta", r.zoneColors().get("principal"));
        if (shirt != null) {
            d = d.withColor(Zone.JERSEY_COLLAR, shirt).withColor(Zone.JERSEY_CUFFS, shirt);
        }
        if (r.zoneColors().containsKey("short")) {
            d = d.withColor(Zone.SHORTS_STRIPE, r.color("short")).withColor(Zone.SHORTS_WAIST, r.color("short"));
        }
        if (r.zoneColors().containsKey("medias")) {
            d = d.withColor(Zone.SOCKS_BAND, r.color("medias"));
        }
        String family = family(r.modelKey());
        if (CUT_LABELS.containsKey(family) && !FOOTBALL.contains(family)) {
            d = d.withCut(nativeCut(family, audience(r.modelKey()), d.cut()));
        }
        DesignDocument withLook = d.withRealistic(new RealisticSpec(r.modelKey(), d.realisticOrDefault().zoneColors(),
                PHOTO_ONLY_PATTERNS.contains(r.pattern()) ? r.pattern() : "liso", r.patternColor(), null, null));
        RealisticSpec before = view(withLook).spec();
        return applyRealistic(withLook, before, new RealisticSpec(r.modelKey(), r.zoneColors(), r.pattern(),
                r.patternColor(), r.name(), r.number()));
    }

    static String photoModel(String stored, GarmentCut cut) {
        String family = family(stored);
        String audience = audience(stored);
        if (audience == null || !FOOTBALL.contains(family)) {
            return stored;
        }
        String best = "futbol";
        if (cut.sleeve() == Sleeve.LARGA && RealisticModels.exists(key("mangalarga", audience))) {
            best = "mangalarga";
        } else if (cut.sleeve() == Sleeve.CORTA && cut.shorts() == ShortsLength.LARGO
                && RealisticModels.exists(key("shortlargo", audience))) {
            best = "shortlargo";
        }
        return RealisticModels.exists(key(best, audience)) ? key(best, audience) : stored;
    }

    static GarmentCut nativeCut(String family, String audience, GarmentCut current) {
        return switch (family) {
            case "mangalarga", "arquero", "compresion" -> new GarmentCut(Sleeve.LARGA, ShortsLength.MEDIO,
                    current.socks());
            case "casaca" -> new GarmentCut(Sleeve.LARGA, current.shorts(), current.socks());
            case "shortlargo", "basquet" -> new GarmentCut(Sleeve.CORTA, ShortsLength.LARGO, current.socks());
            case "licra" -> new GarmentCut(Sleeve.CORTA, ShortsLength.CORTO, current.socks());
            case "voley" -> new GarmentCut(Sleeve.CORTA, "mujer".equals(audience) ? ShortsLength.CORTO
                    : ShortsLength.MEDIO, current.socks());
            case "buzo" -> new GarmentCut(Sleeve.LARGA, ShortsLength.LARGO, current.socks());
            default -> new GarmentCut(Sleeve.CORTA, ShortsLength.MEDIO, current.socks());
        };
    }

    static String family(String key) {
        return key == null ? "" : key.split("_")[0];
    }

    static String audience(String key) {
        String[] parts = key == null ? new String[0] : key.split("_");
        return parts.length >= 3 ? parts[1] : null;
    }

    static String key(String family, String audience) {
        return family + "_" + audience + "_frente";
    }

    private static boolean differs(DesignDocument d, Zone detail, Zone base) {
        return !d.color(detail).equalsIgnoreCase(d.color(base));
    }

    private static DesignElement visible(DesignElement e) {
        return e != null && e.visible() ? e : null;
    }

    private static RealisticView.Text text(DesignElement e) {
        if (e == null || e.text() == null || e.text().isBlank()) {
            return null;
        }
        return new RealisticView.Text(e.text(), e.color() == null ? "#FFFFFF" : e.color(),
                e.outline() > 0 ? e.accentColor() : null, e.font());
    }

    private static void position(Set<String> notes, DesignDocument d, TextSlot slot, DesignElement e, String what) {
        if (e != null && !slot.atDefault(e, d)) {
            notes.add(what + ": ver en Vista 3D");
        }
    }

    private static boolean crestAtDefault(DesignElement e) {
        DesignElement def = DesignDefaults.emblem("x", "#000000", "#000000");
        return Math.abs(e.u() - def.u()) < 0.02 && Math.abs(e.v() - def.v()) < 0.02 && Math.abs(e.scale() - 1) < 0.05
                && Math.abs(e.rotation()) < 2;
    }

    private static DesignDocument setText(DesignDocument doc, TextSlot slot, String value) {
        DesignElement e = slot.find(doc);
        if (value == null) {
            return e == null ? doc : doc.removeElement(e.id());
        }
        if (e == null) {
            return doc.addElement(slot.create(doc).withText(value));
        }
        return value.equals(e.text()) ? doc : doc.replaceElement(e.withText(value).withVisible(true));
    }

    private static Integer parse(String digits) {
        try {
            int n = Integer.parseInt(digits);
            return n >= 0 && n <= 99 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Map<String, String> cutLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("futbol", "Fútbol cuello V");
        m.put("polo", "Polo");
        m.put("redondo", "Cuello redondo");
        m.put("raglan", "Raglán");
        m.put("mangalarga", "Manga larga");
        m.put("voley", "Vóley");
        m.put("basquet", "Básquet");
        m.put("buzo", "Buzo");
        m.put("shortlargo", "Short largo");
        m.put("licra", "Licra");
        m.put("arquero", "Arquero");
        m.put("compresion", "Compresión");
        m.put("casaca", "Casaca");
        return java.util.Collections.unmodifiableMap(m);
    }
}
