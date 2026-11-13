package com.sportforce3d.three;

import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeyDesignElement;
import com.sportforce3d.design.PgeyMaterialSlot;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.three.model.PgeyZone;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PgeyDesignRenderer {

    private final PgeyViewer3D pgeyViewer;
    private final PgeyTextureComposer pgeyComposer;

    public PgeyDesignRenderer(PgeyViewer3D pgeyViewer, PgeyTextureComposer pgeyComposer) {
        this.pgeyViewer = pgeyViewer;
        this.pgeyComposer = pgeyComposer;
    }

    public PgeyTextureComposer pgeyComposer() {
        return pgeyComposer;
    }

    public void pgeyApplyAll(PgeyDesignDocument pgeyDoc, Map<String, HepyMaterial> pgeyMaterials) {
        for (HepyGarment pgeyG : PgeyTextureComposer.pgeyGarments()) {
            pgeyViewer.pgeySetGarmentTexture(pgeyG, pgeyComposer.pgeyCompose(pgeyG, pgeyDoc));
        }
        pgeyApplyMaterials(pgeyDoc, pgeyMaterials);
    }

    public void pgeyApplyGarments(PgeyDesignDocument pgeyDoc, Set<HepyGarment> pgeyGarments) {
        for (HepyGarment pgeyG : pgeyGarments) {
            pgeyViewer.pgeySetGarmentTexture(pgeyG, pgeyComposer.pgeyCompose(pgeyG, pgeyDoc));
        }
    }

    public void pgeyApplyMaterials(PgeyDesignDocument pgeyDoc, Map<String, HepyMaterial> pgeyMaterials) {
        for (PgeyMaterialSlot pgeySlot : PgeyMaterialSlot.values()) {
            HepyMaterial pgeyM = pgeyMaterials.get(pgeyDoc.pgeyMaterial(pgeySlot));
            if (pgeyM != null) {
                pgeyViewer.pgeySetLook(pgeySlot.pgeyZones(), PgeyMaterialLook.pgeyOf(pgeyM));
            }
        }
    }

    public static Set<HepyGarment> pgeyChangedGarments(PgeyDesignDocument pgeyBefore, PgeyDesignDocument pgeyAfter) {
        Set<HepyGarment> pgeyOut = EnumSet.noneOf(HepyGarment.class);
        for (HepyGarment pgeyG : PgeyTextureComposer.pgeyGarments()) {
            if (pgeyBefore == null || !pgeySignature(pgeyBefore, pgeyG).equals(pgeySignature(pgeyAfter, pgeyG))) {
                pgeyOut.add(pgeyG);
            }
        }
        return pgeyOut;
    }

    public static boolean pgeyMaterialsChanged(PgeyDesignDocument pgeyBefore, PgeyDesignDocument pgeyAfter) {
        return pgeyBefore == null || !pgeyBefore.pgeyMaterials().equals(pgeyAfter.pgeyMaterials());
    }

    private static List<Object> pgeySignature(PgeyDesignDocument pgeyD, HepyGarment pgeyG) {
        List<Object> pgeyColors = java.util.Arrays.stream(PgeyZone.values())
                .filter(pgeyZ -> pgeyZ.pgeyGarment() == pgeyG).map(pgeyD::pgeyColor).map(Object.class::cast).toList();
        List<PgeyDesignElement> pgeyElements = pgeyD.pgeyElements().stream().filter(pgeyE -> pgeyE.pgeyArea().pgeyGarment() == pgeyG).toList();
        return List.of(pgeyColors, Objects.toString(pgeyD.pgeyPattern(pgeyG)), pgeyElements);
    }
}
