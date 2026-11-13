package com.sportforce3d.viewmodel;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.design.PgeyCommandStack;
import com.sportforce3d.design.PgeyDesignDefaults;
import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeyDesignElement;
import com.sportforce3d.design.PgeyDesignJson;
import com.sportforce3d.design.PgeyDraftStore;
import com.sportforce3d.design.PgeyGarmentCut;
import com.sportforce3d.design.PgeyDesignPricing;
import com.sportforce3d.design.PgeyRealisticSpec;
import com.sportforce3d.design.PgeyRealisticView;
import com.sportforce3d.design.PgeyViewSync;
import com.sportforce3d.model.PgeyDesignSummary;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.model.HepyRealisticLook;
import com.sportforce3d.service.HepyCatalogService;
import com.sportforce3d.service.PgeyDesignService;
import com.sportforce3d.three.human.PgeyHumanModels;
import com.sportforce3d.three.human.PgeyOutfitFitter.PgeyDressedPerson;
import com.sportforce3d.three.human.PgeyPersona;
import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.model.PgeyModelProviders;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class PgeyDesignerViewModel {

    private final PgeyDesignService pgeyDesigns;
    private final HepyCatalogService pgeyCatalog;
    private final PgeyCommandStack pgeyStack;

    private final ObjectProperty<PgeyDesignDocument> pgeyDocument = new SimpleObjectProperty<>();
    private final StringProperty pgeySelectedId = new SimpleStringProperty();
    private final ObjectProperty<HepyGarment> pgeyGarment = new SimpleObjectProperty<>(HepyGarment.JERSEY);
    private final ObjectProperty<PgeyModelData> pgeyModel = new SimpleObjectProperty<>();
    private final ObjectProperty<Map<String, HepyMaterial>> pgeyMaterials = new SimpleObjectProperty<>(Map.of());
    private final BooleanProperty pgeyLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty pgeySaving = new SimpleBooleanProperty(false);
    private final BooleanProperty pgeyDirty = new SimpleBooleanProperty(false);
    private final BooleanProperty pgeyCanUndo = new SimpleBooleanProperty(false);
    private final BooleanProperty pgeyCanRedo = new SimpleBooleanProperty(false);
    private final StringProperty pgeyUndoLabel = new SimpleStringProperty("");
    private final StringProperty pgeyRedoLabel = new SimpleStringProperty("");
    private final StringProperty pgeyError = new SimpleStringProperty("");
    private final StringProperty pgeyName = new SimpleStringProperty("");
    private final ObjectProperty<Long> pgeyDesignId = new SimpleObjectProperty<>();
    private final ObjectProperty<HepyProductDetail> pgeyProduct = new SimpleObjectProperty<>();
    private final LongProperty pgeySurchargeCents = new SimpleLongProperty(0);
    private final LongProperty pgeyTotalCents = new SimpleLongProperty(0);
    private final ObjectProperty<PgeyPersona> pgeyPersona = new SimpleObjectProperty<>(PgeyPersona.HOMBRE);
    private final ObjectProperty<PgeyDressedPerson> pgeyPerson = new SimpleObjectProperty<>();
    private final BooleanProperty pgeyPersonLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty pgeyPersonFallback = new SimpleBooleanProperty(false);
    private int pgeyRequest;
    private int pgeyPersonRequest;
    private PgeyGarmentCut pgeyShownCut;
    private PgeyPersona pgeyLoadingPersona;
    private PgeyGarmentCut pgeyLoadingCut;
    private PgeyBodyType pgeyLoadedBody;

    public PgeyDesignerViewModel(PgeyDesignService pgeyDesigns, HepyCatalogService pgeyCatalog, Clock pgeyClock) {
        this.pgeyDesigns = pgeyDesigns;
        this.pgeyCatalog = pgeyCatalog;
        PgeyDesignDocument pgeyInitial = pgeyStartDocument(PgeyBodyType.ADULTO_HOMBRE);
        this.pgeyStack = new PgeyCommandStack(pgeyInitial, pgeyClock);
        pgeyDocument.set(pgeyInitial);
        pgeyDocument.addListener((pgeyObs, pgeyOld, pgeyDoc) -> {
            if (pgeyOld == null || pgeyOld.pgeyBodyType() != pgeyDoc.pgeyBodyType()) {
                pgeyLoadModel();
            }
            PgeyPersona pgeyP = PgeyPersona.pgeyFromModelKey(pgeyDoc.pgeyRealisticOrDefault().pgeyModelKey());
            if (pgeyP != null) {
                pgeyPersona.set(pgeyP);
            }
            pgeyUpdatePrice();
            if (pgeySelectedId.get() != null && pgeyDoc.pgeyElement(pgeySelectedId.get()).isEmpty()) {
                pgeySelectedId.set(null);
            }
        });
        pgeyMaterials.addListener((pgeyObs, pgeyOld, pgeyM) -> pgeyUpdatePrice());
        pgeyProduct.addListener((pgeyObs, pgeyOld, pgeyP) -> pgeyUpdatePrice());
    }

    static PgeyDesignDocument pgeyStartDocument(PgeyBodyType pgeyType) {
        return PgeyViewSync.pgeySeedFromRealistic(PgeyDesignDefaults.pgeyDocument(pgeyType), PgeyRealisticSpec.pgeyDefaults());
    }

    public void pgeyLoadMaterials() {
        PpqhAsync.ppqhRun(() -> {
                    Map<String, HepyMaterial> pgeyMap = new HashMap<>();
                    for (HepyMaterial.HepyScope pgeyScope : HepyMaterial.HepyScope.values()) {
                        pgeyCatalog.hepyMaterials(pgeyScope).forEach(pgeyM -> pgeyMap.put(pgeyM.hepyCode(), pgeyM));
                    }
                    return Map.copyOf(pgeyMap);
                },
                pgeyMaterials::set,
                pgeyE -> pgeyError.set("No se pudo cargar los materiales. " + PpqhErrorMessages.ppqhOf(pgeyE)));
    }

    public void pgeyLoadProduct(long pgeyProductId) {
        record PgeyLoaded(HepyProductDetail pgeyDetail, Optional<HepyRealisticLook> pgeyLook) {
        }
        PpqhAsync.ppqhRun(() -> new PgeyLoaded(pgeyCatalog.hepyDetail(pgeyProductId), pgeyCatalog.hepyRealisticLook(pgeyProductId)), pgeyLoaded -> {
            HepyProductDetail pgeyP = pgeyLoaded.pgeyDetail();
            pgeyProduct.set(pgeyP);
            if (!java.util.Objects.equals(pgeyDocument.get().pgeyProductId(), pgeyP.hepyId())) {
                PgeyDesignDocument pgeyDoc = pgeyDocument.get().pgeyWithProduct(pgeyP.hepyId());
                if (pgeyDesignId.get() == null && pgeyLoaded.pgeyLook().isPresent()) {
                    pgeyDoc = PgeyViewSync.pgeySeedFromRealistic(pgeyDoc, pgeyFromLook(pgeyLoaded.pgeyLook().get()));
                }
                pgeyDoc = PgeyViewSync.pgeyNormalize(pgeyDoc);
                pgeyStack.pgeyReset(pgeyDoc);
                pgeyDocument.set(pgeyStack.pgeyCurrent());
            }
        }, pgeyE -> pgeyProduct.set(null));
    }

    static PgeyRealisticSpec pgeyFromLook(HepyRealisticLook pgeyLook) {
        PgeyRealisticSpec pgeyBase = PgeyRealisticSpec.pgeyDefaults();
        String pgeyModel = pgeyLook.hepyModelKey().replaceFirst("_espalda$", "_frente");
        Map<String, String> pgeyColors = new java.util.TreeMap<>(pgeyBase.pgeyZoneColors());
        pgeyColors.putAll(pgeyLook.hepyZoneColors());
        return new PgeyRealisticSpec(pgeyModel, pgeyColors, pgeyLook.hepyPattern(),
                pgeyLook.hepyPatternColor() != null ? pgeyLook.hepyPatternColor() : pgeyBase.pgeyPatternColor(), null, null);
    }

    public void pgeyLoadModel() {
        PgeyBodyType pgeyType = pgeyDocument.get().pgeyBodyType();
        if (pgeyType == pgeyLoadedBody && pgeyModel.get() != null) {
            return;
        }
        int pgeyId = ++pgeyRequest;
        pgeyLoading.set(true);
        PpqhAsync.ppqhRun(() -> PgeyModelProviders.pgeyLoad(pgeyType),
                pgeyData -> {
                    if (pgeyId == pgeyRequest) {
                        pgeyLoading.set(false);
                        pgeyLoadedBody = pgeyType;
                        pgeyModel.set(pgeyData);
                    }
                },
                pgeyE -> {
                    if (pgeyId == pgeyRequest) {
                        pgeyLoading.set(false);
                        pgeyError.set("No se pudo generar el maniquí 3D. " + PpqhErrorMessages.ppqhOf(pgeyE));
                    }
                });
    }

    public void pgeyLoadPerson() {
        PgeyPersona pgeyP = pgeyPersona.get();
        PgeyGarmentCut pgeyCut = pgeyDocument.get().pgeyCut();
        PgeyDressedPerson pgeyShown = pgeyPerson.get();
        if (pgeyShown != null && pgeyShown.pgeyBody().pgeyPersona() == pgeyP && pgeyCut.equals(pgeyShownCut) && !pgeyPersonFallback.get()) {
            return;
        }
        if (pgeyPersonLoading.get() && pgeyP == pgeyLoadingPersona && pgeyCut.equals(pgeyLoadingCut)) {
            return;
        }
        int pgeyId = ++pgeyPersonRequest;
        if (!PgeyHumanModels.pgeyAvailable(pgeyP)) {
            pgeyPersonLoading.set(false);
            pgeyPersonFallback.set(true);
            return;
        }
        pgeyLoadingPersona = pgeyP;
        pgeyLoadingCut = pgeyCut;
        pgeyPersonLoading.set(true);
        PpqhAsync.ppqhRun(() -> PgeyHumanModels.pgeyDress(pgeyP, pgeyCut),
                pgeyData -> {
                    if (pgeyId == pgeyPersonRequest) {
                        pgeyPersonLoading.set(false);
                        pgeyPersonFallback.set(false);
                        pgeyShownCut = pgeyCut;
                        pgeyPerson.set(pgeyData);
                    }
                },
                pgeyE -> {
                    if (pgeyId == pgeyPersonRequest) {
                        pgeyPersonLoading.set(false);
                        pgeyPersonFallback.set(true);
                        pgeyError.set("No se pudo cargar la persona 3D; se muestra el maniquí. " + PpqhErrorMessages.ppqhOf(pgeyE));
                    }
                });
    }

    public boolean pgeyPersonMatchesDocument() {
        PgeyDressedPerson pgeyShown = pgeyPerson.get();
        return pgeyShown != null && !pgeyPersonFallback.get() && !pgeyPersonLoading.get() && pgeyShown.pgeyBody().pgeyPersona() == pgeyPersona.get()
                && pgeyDocument.get().pgeyCut().equals(pgeyShownCut);
    }

    public void pgeyOpen(long pgeyId, Runnable pgeyOnDone, Consumer<Throwable> pgeyOnError) {
        PpqhAsync.ppqhRun(() -> pgeyDesigns.pgeyOpen(pgeyId), pgeyOpened -> {
            pgeyDesignId.set(pgeyOpened.pgeySummary().pgeyId());
            pgeyName.set(pgeyOpened.pgeySummary().pgeyName());
            pgeyReplaceDocument(pgeyOpened.pgeyDocument());
            pgeyDirty.set(false);
            if (pgeyOpened.pgeyDocument().pgeyProductId() != null) {
                pgeyLoadProduct(pgeyOpened.pgeyDocument().pgeyProductId());
            }
            pgeyOnDone.run();
        }, pgeyOnError);
    }

    public void pgeyRestore(PgeyDraftStore.PgeyDraft pgeyDraft) {
        pgeyDesignId.set(pgeyDraft.pgeyDesignId());
        pgeyName.set(pgeyDraft.pgeyName() == null ? "" : pgeyDraft.pgeyName());
        pgeyReplaceDocument(PgeyDesignJson.pgeyRead(pgeyDraft.pgeyDocumentJson()));
        pgeyDirty.set(true);
        if (pgeyDocument.get().pgeyProductId() != null) {
            pgeyLoadProduct(pgeyDocument.get().pgeyProductId());
        }
    }

    public Optional<PgeyDraftStore.PgeyDraft> pgeyDraft() {
        try {
            return pgeyDesigns.pgeyDraft();
        } catch (RuntimeException pgeyE) {
            return Optional.empty();
        }
    }

    public void pgeyDiscardDraft() {
        PpqhAsync.ppqhRun(() -> {
            pgeyDesigns.pgeyDiscardDraft();
            return true;
        }, pgeyOk -> { }, pgeyE -> { });
    }

    private void pgeyReplaceDocument(PgeyDesignDocument pgeyLoaded) {
        PgeyDesignDocument pgeyDoc = PgeyViewSync.pgeyNormalize(pgeyLoaded);
        pgeyStack.pgeyReset(pgeyDoc);
        pgeySelectedId.set(null);
        pgeyDocument.set(pgeyDoc);
        pgeySyncHistory();
    }

    public void pgeyEdit(String pgeyLabel, UnaryOperator<PgeyDesignDocument> pgeyChange, String pgeyMergeKey) {
        PgeyDesignDocument pgeyBefore = pgeyDocument.get();
        PgeyDesignDocument pgeyAfter = pgeyStack.pgeyExecute(pgeyLabel, pgeyD -> {
            PgeyDesignDocument pgeyNext = pgeyChange.apply(pgeyD);
            return pgeyNext == pgeyD ? pgeyD : PgeyViewSync.pgeyNormalize(pgeyNext);
        }, pgeyMergeKey);
        if (pgeyAfter != pgeyBefore) {
            pgeyDocument.set(pgeyAfter);
            pgeyDirty.set(true);
        }
        pgeySyncHistory();
    }

    public void pgeyEditRealistic(String pgeyLabel, UnaryOperator<PgeyRealisticSpec> pgeyChange, String pgeyMergeKey) {
        pgeyEdit(pgeyLabel, pgeyD -> {
            PgeyRealisticSpec pgeyBefore = PgeyViewSync.pgeyView(pgeyD).pgeySpec();
            PgeyRealisticSpec pgeyAfter = pgeyChange.apply(pgeyBefore);
            return pgeyAfter.equals(pgeyBefore) ? pgeyD : PgeyViewSync.pgeyApplyRealistic(pgeyD, pgeyBefore, pgeyAfter);
        }, pgeyMergeKey);
    }

    public PgeyRealisticSpec pgeyRealistic() {
        return pgeyRealisticView().pgeySpec();
    }

    public PgeyRealisticView pgeyRealisticView() {
        return PgeyViewSync.pgeyView(pgeyDocument.get());
    }

    public void pgeyEditElement(String pgeyId, String pgeyLabel, UnaryOperator<PgeyDesignElement> pgeyChange, String pgeyMergeKey) {
        pgeyEdit(pgeyLabel, pgeyD -> pgeyD.pgeyElement(pgeyId).map(pgeyE -> pgeyD.pgeyReplaceElement(pgeyChange.apply(pgeyE))).orElse(pgeyD), pgeyMergeKey);
    }

    public void pgeySeal() {
        pgeyStack.pgeySeal();
    }

    public void pgeyUndo() {
        PgeyDesignDocument pgeyBefore = pgeyDocument.get();
        pgeyDocument.set(pgeyStack.pgeyUndo());
        pgeyDirty.set(pgeyDirty.get() || pgeyDocument.get() != pgeyBefore);
        pgeySyncHistory();
    }

    public void pgeyRedo() {
        PgeyDesignDocument pgeyBefore = pgeyDocument.get();
        pgeyDocument.set(pgeyStack.pgeyRedo());
        pgeyDirty.set(pgeyDirty.get() || pgeyDocument.get() != pgeyBefore);
        pgeySyncHistory();
    }

    public void pgeyResetDesign() {
        pgeyEdit("Restablecer diseño", pgeyD -> pgeyStartDocument(pgeyD.pgeyBodyType()).pgeyWithProduct(pgeyD.pgeyProductId()), null);
        pgeyStack.pgeySeal();
    }

    public PgeyDesignElement pgeyAdd(PgeyDesignElement pgeyElement) {
        pgeyEdit("Agregar " + pgeyElement.pgeyDisplayName(), pgeyD -> pgeyD.pgeyAddElement(pgeyElement), null);
        pgeyStack.pgeySeal();
        pgeySelectedId.set(pgeyElement.pgeyId());
        return pgeyElement;
    }

    public void pgeyRemove(String pgeyId) {
        String pgeyLabel = pgeyDocument.get().pgeyElement(pgeyId).map(pgeyE -> "Eliminar " + pgeyE.pgeyDisplayName()).orElse("Eliminar");
        pgeyEdit(pgeyLabel, pgeyD -> pgeyD.pgeyRemoveElement(pgeyId), null);
        pgeyStack.pgeySeal();
    }

    private void pgeySyncHistory() {
        pgeyCanUndo.set(pgeyStack.pgeyCanUndo());
        pgeyCanRedo.set(pgeyStack.pgeyCanRedo());
        pgeyUndoLabel.set(pgeyStack.pgeyCanUndo() ? "Deshacer: " + pgeyStack.pgeyUndoLabel() : "Nada que deshacer");
        pgeyRedoLabel.set(pgeyStack.pgeyCanRedo() ? "Rehacer: " + pgeyStack.pgeyRedoLabel() : "Nada que rehacer");
    }

    private void pgeyUpdatePrice() {
        HepyProductDetail pgeyP = pgeyProduct.get();
        long pgeySum = PgeyDesignPricing.pgeySurchargeCents(pgeyDocument.get(), pgeyMaterials.get(), pgeyP == null ? null : pgeyP.hepyGarment());
        pgeySurchargeCents.set(pgeySum);
        pgeyTotalCents.set(pgeyProduct.get() == null ? pgeySum : pgeyProduct.get().hepyBasePriceCents() + pgeySum);
    }

    public void pgeySave(String pgeyNewName, Path pgeyFront, Path pgeyBack, Consumer<PgeyDesignSummary> pgeyOnSaved, Consumer<Throwable> pgeyOnError) {
        PgeyDesignDocument pgeyDoc = pgeyDocument.get();
        Long pgeyId = pgeyDesignId.get();
        pgeySaving.set(true);
        PpqhAsync.ppqhRun(() -> pgeyDesigns.pgeySave(pgeyId, pgeyNewName, pgeyDoc, pgeyFront, pgeyBack), pgeySummary -> {
            pgeySaving.set(false);
            pgeyDesignId.set(pgeySummary.pgeyId());
            pgeyName.set(pgeySummary.pgeyName());
            pgeyDirty.set(pgeyDocument.get() != pgeyDoc);
            pgeyOnSaved.accept(pgeySummary);
        }, pgeyE -> {
            pgeySaving.set(false);
            pgeyOnError.accept(pgeyE);
        });
    }

    public void pgeyAutosave() {
        if (!pgeyDirty.get() || pgeySaving.get()) {
            return;
        }
        PgeyDesignDocument pgeyDoc = pgeyDocument.get();
        Long pgeyId = pgeyDesignId.get();
        String pgeyN = pgeyName.get();
        PpqhAsync.ppqhRun(() -> {
            pgeyDesigns.pgeySaveDraft(pgeyId, pgeyN, pgeyDoc);
            return true;
        }, pgeyOk -> { }, pgeyE -> { });
    }

    public PgeyDesignDocument pgeyDoc() { return pgeyDocument.get(); }
    public Optional<PgeyDesignElement> pgeySelected() {
        return pgeySelectedId.get() == null ? Optional.empty() : pgeyDocument.get().pgeyElement(pgeySelectedId.get());
    }

    public ObjectProperty<PgeyDesignDocument> documentProperty() { return pgeyDocument; }
    public StringProperty selectedIdProperty() { return pgeySelectedId; }
    public ObjectProperty<HepyGarment> garmentProperty() { return pgeyGarment; }
    public ObjectProperty<PgeyModelData> modelProperty() { return pgeyModel; }
    public ObjectProperty<PgeyPersona> personaProperty() { return pgeyPersona; }
    public ObjectProperty<PgeyDressedPerson> personProperty() { return pgeyPerson; }
    public BooleanProperty personLoadingProperty() { return pgeyPersonLoading; }
    public BooleanProperty personFallbackProperty() { return pgeyPersonFallback; }
    public ObjectProperty<Map<String, HepyMaterial>> materialsProperty() { return pgeyMaterials; }
    public BooleanProperty loadingProperty() { return pgeyLoading; }
    public BooleanProperty savingProperty() { return pgeySaving; }
    public BooleanProperty dirtyProperty() { return pgeyDirty; }
    public BooleanProperty canUndoProperty() { return pgeyCanUndo; }
    public BooleanProperty canRedoProperty() { return pgeyCanRedo; }
    public StringProperty undoLabelProperty() { return pgeyUndoLabel; }
    public StringProperty redoLabelProperty() { return pgeyRedoLabel; }
    public StringProperty errorProperty() { return pgeyError; }
    public StringProperty nameProperty() { return pgeyName; }
    public ObjectProperty<Long> designIdProperty() { return pgeyDesignId; }
    public ObjectProperty<HepyProductDetail> productProperty() { return pgeyProduct; }
    public LongProperty surchargeCentsProperty() { return pgeySurchargeCents; }
    public LongProperty totalCentsProperty() { return pgeyTotalCents; }
}
