package com.sportforce3d.three;

import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.DesignElement;
import com.sportforce3d.design.MaterialSlot;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.Material;
import com.sportforce3d.three.model.Zone;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DesignRenderer {

    private final Viewer3D viewer;
    private final TextureComposer composer;

    public DesignRenderer(Viewer3D viewer, TextureComposer composer) {
        this.viewer = viewer;
        this.composer = composer;
    }

    public TextureComposer composer() {
        return composer;
    }

    public void applyAll(DesignDocument doc, Map<String, Material> materials) {
        for (Garment g : TextureComposer.garments()) {
            viewer.setGarmentTexture(g, composer.compose(g, doc));
        }
        applyMaterials(doc, materials);
    }

    public void applyGarments(DesignDocument doc, Set<Garment> garments) {
        for (Garment g : garments) {
            viewer.setGarmentTexture(g, composer.compose(g, doc));
        }
    }

    public void applyMaterials(DesignDocument doc, Map<String, Material> materials) {
        for (MaterialSlot slot : MaterialSlot.values()) {
            Material m = materials.get(doc.material(slot));
            if (m != null) {
                viewer.setLook(slot.zones(), MaterialLook.of(m));
            }
        }
    }

    public static Set<Garment> changedGarments(DesignDocument before, DesignDocument after) {
        Set<Garment> out = EnumSet.noneOf(Garment.class);
        for (Garment g : TextureComposer.garments()) {
            if (before == null || !signature(before, g).equals(signature(after, g))) {
                out.add(g);
            }
        }
        return out;
    }

    public static boolean materialsChanged(DesignDocument before, DesignDocument after) {
        return before == null || !before.materials().equals(after.materials());
    }

    private static List<Object> signature(DesignDocument d, Garment g) {
        List<Object> colors = java.util.Arrays.stream(Zone.values())
                .filter(z -> z.garment() == g).map(d::color).map(Object.class::cast).toList();
        List<DesignElement> elements = d.elements().stream().filter(e -> e.area().garment() == g).toList();
        return List.of(colors, Objects.toString(d.pattern(g)), elements);
    }
}
