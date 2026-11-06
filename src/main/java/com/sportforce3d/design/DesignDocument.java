package com.sportforce3d.design;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.Zone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Diseño completo e inmutable. Se guarda como JSON versionado ({@link #schemaVersion}) en design_versions;
 * cada cambio produce un documento nuevo, lo que hace trivial deshacer y rehacer.
 *
 * <p>Los elementos están en orden de capa: el último se dibuja encima. {@code realistic} es el diseño de la vista
 * realista (puede ser null en diseños anteriores a la Etapa 7). {@code cut} es el largo de manga, short y medias de
 * la Vista 3D (nunca null: los diseños anteriores a la Etapa 10 abren con {@link GarmentCut#DEFAULT}).
 */
public record DesignDocument(int schemaVersion, BodyType bodyType, Long productId, Map<Zone, String> colors,
                             Map<Garment, PatternSpec> patterns, Map<MaterialSlot, String> materials,
                             List<DesignElement> elements, RealisticSpec realistic, GarmentCut cut) {

    /**
     * 1: diseño 3D. 2: agrega {@link #realistic} (vista realista). 3: agrega {@link #cut} (manga y largos).
     * 4: las dos vistas son un solo diseño ({@link ViewSync}): la vista realista guardada sale del 3D. Un diseño de
     * una versión anterior se abre igual (ver {@code DesignJson}).
     */
    public static final int SCHEMA_VERSION = 4;

    public DesignDocument {
        bodyType = bodyType == null ? BodyType.ADULTO_HOMBRE : bodyType;
        Map<Zone, String> c = new EnumMap<>(Zone.class);
        if (colors != null) {
            colors.forEach((z, hex) -> c.put(z, hex.toUpperCase()));
        }
        colors = Collections.unmodifiableMap(c);
        patterns = Collections.unmodifiableMap(patterns == null || patterns.isEmpty()
                ? new EnumMap<>(Garment.class) : new EnumMap<>(patterns));
        materials = Collections.unmodifiableMap(materials == null || materials.isEmpty()
                ? new EnumMap<>(MaterialSlot.class) : new EnumMap<>(materials));
        elements = List.copyOf(elements == null ? List.of() : elements);
        cut = cut == null ? GarmentCut.DEFAULT : cut;
    }

    /** La vista realista guardada o, si el diseño es anterior a la Etapa 7, la predeterminada. */
    public RealisticSpec realisticOrDefault() {
        return realistic != null ? realistic : RealisticSpec.defaults();
    }

    /**
     * true si las fotos de la vista realista muestran todo lo que se compra (sin avisos de {@link ViewSync#view}:
     * corte, tela, colores, textos y escudo). Si no, las fotos del diseño (y del pedido) salen de la Vista 3D.
     */
    public boolean realisticPhotosMatch() {
        return ViewSync.realisticMatches(this);
    }

    public String color(Zone zone) {
        return colors.getOrDefault(zone, "#F2F5F8");
    }

    public PatternSpec pattern(Garment garment) {
        return patterns.getOrDefault(garment, PatternSpec.PLAIN);
    }

    public String material(MaterialSlot slot) {
        return materials.getOrDefault(slot, slot.defaultCode());
    }

    public Optional<DesignElement> element(String id) {
        return elements.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    // ---------------------------------------------------------------- Cambios (devuelven un documento nuevo)

    public DesignDocument withBodyType(BodyType type) {
        return new DesignDocument(schemaVersion, type, productId, colors, patterns, materials, elements, realistic, cut);
    }

    public DesignDocument withProduct(Long id) {
        return new DesignDocument(schemaVersion, bodyType, id, colors, patterns, materials, elements, realistic, cut);
    }

    public DesignDocument withColor(Zone zone, String hex) {
        Map<Zone, String> c = new EnumMap<>(Zone.class);
        c.putAll(colors);
        c.put(zone, hex);
        return new DesignDocument(schemaVersion, bodyType, productId, c, patterns, materials, elements, realistic, cut);
    }

    public DesignDocument withPattern(Garment garment, PatternSpec spec) {
        Map<Garment, PatternSpec> p = new EnumMap<>(Garment.class);
        p.putAll(patterns);
        p.put(garment, spec);
        return new DesignDocument(schemaVersion, bodyType, productId, colors, p, materials, elements, realistic, cut);
    }

    public DesignDocument withMaterial(MaterialSlot slot, String code) {
        Map<MaterialSlot, String> m = new EnumMap<>(MaterialSlot.class);
        m.putAll(materials);
        m.put(slot, code);
        return new DesignDocument(schemaVersion, bodyType, productId, colors, patterns, m, elements, realistic, cut);
    }

    public DesignDocument withElements(List<DesignElement> list) {
        return new DesignDocument(schemaVersion, bodyType, productId, colors, patterns, materials, list, realistic, cut);
    }

    public DesignDocument withRealistic(RealisticSpec spec) {
        return new DesignDocument(schemaVersion, bodyType, productId, colors, patterns, materials, elements, spec, cut);
    }

    /**
     * Cambia el corte. Si cambia el largo del short, los números del short que siguen en la posición por defecto del
     * largo anterior pasan a la del nuevo (a media altura de lo que se ve); los que el cliente movió no se tocan.
     */
    public DesignDocument withCut(GarmentCut c) {
        GarmentCut next = c == null ? GarmentCut.DEFAULT : c;
        List<DesignElement> list = elements;
        if (next.shorts() != cut.shorts()) {
            GarmentCut.ShortsLength from = cut.shorts();
            GarmentCut.ShortsLength to = next.shorts();
            list = elements.stream().map(e -> e.type() == ElementType.NUMBER && e.area() == Area.SHORTS_LEFT
                    && Math.abs(e.v() - from.numberV()) < 0.01 && Math.abs(e.u() - from.numberU()) < 0.01
                    ? e.at(to.numberU(), to.numberV()) : e).toList();
        }
        return new DesignDocument(schemaVersion, bodyType, productId, colors, patterns, materials, list, realistic, next);
    }

    public DesignDocument addElement(DesignElement e) {
        List<DesignElement> list = new ArrayList<>(elements);
        list.add(e);
        return withElements(list);
    }

    public DesignDocument replaceElement(DesignElement e) {
        List<DesignElement> list = new ArrayList<>(elements);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(e.id())) {
                list.set(i, e);
            }
        }
        return withElements(list);
    }

    public DesignDocument removeElement(String id) {
        return withElements(elements.stream().filter(e -> !e.id().equals(id)).toList());
    }

    /** Sube (+1) o baja (-1) una capa. */
    public DesignDocument moveLayer(String id, int delta) {
        List<DesignElement> list = new ArrayList<>(elements);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(id)) {
                int j = Math.max(0, Math.min(list.size() - 1, i + delta));
                DesignElement e = list.remove(i);
                list.add(j, e);
                break;
            }
        }
        return withElements(list);
    }
}
