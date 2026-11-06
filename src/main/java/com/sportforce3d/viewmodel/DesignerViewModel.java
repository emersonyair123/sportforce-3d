package com.sportforce3d.viewmodel;

import com.sportforce3d.app.Async;
import com.sportforce3d.design.CommandStack;
import com.sportforce3d.design.DesignDefaults;
import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.RealisticSpec;
import com.sportforce3d.model.RealisticLook;
import com.sportforce3d.design.DesignElement;
import com.sportforce3d.design.DesignJson;
import com.sportforce3d.design.DraftStore;
import com.sportforce3d.design.MaterialSlot;
import com.sportforce3d.model.DesignSummary;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.service.CatalogService;
import com.sportforce3d.service.DesignService;
import com.sportforce3d.three.human.HumanModel;
import com.sportforce3d.three.human.HumanModels;
import com.sportforce3d.three.human.Persona;
import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.model.ModelProviders;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Estado del diseñador 3D: el {@link DesignDocument} actual con su historial (deshacer/rehacer), el elemento
 * elegido, los materiales del catálogo con el recargo en vivo, y abrir/guardar/borrador. La geometría del maniquí
 * y la persona 3D se cargan fuera del hilo de la interfaz. La persona sale de la categoría de la vista realista
 * ({@link Persona#fromModelKey}); si su modelo falta o no se puede leer, la Vista 3D usa el maniquí.
 */
public class DesignerViewModel {

    private final DesignService designs;
    private final CatalogService catalog;
    private final CommandStack stack;

    private final ObjectProperty<DesignDocument> document = new SimpleObjectProperty<>();
    private final StringProperty selectedId = new SimpleStringProperty();
    private final ObjectProperty<Garment> garment = new SimpleObjectProperty<>(Garment.JERSEY);
    private final ObjectProperty<ModelData> model = new SimpleObjectProperty<>();
    private final ObjectProperty<Map<String, Material>> materials = new SimpleObjectProperty<>(Map.of());
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty saving = new SimpleBooleanProperty(false);
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private final BooleanProperty canUndo = new SimpleBooleanProperty(false);
    private final BooleanProperty canRedo = new SimpleBooleanProperty(false);
    private final StringProperty undoLabel = new SimpleStringProperty("");
    private final StringProperty redoLabel = new SimpleStringProperty("");
    private final StringProperty error = new SimpleStringProperty("");
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<Long> designId = new SimpleObjectProperty<>();
    private final ObjectProperty<ProductDetail> product = new SimpleObjectProperty<>();
    private final LongProperty surchargeCents = new SimpleLongProperty(0);
    private final LongProperty totalCents = new SimpleLongProperty(0);
    private final ObjectProperty<Persona> persona = new SimpleObjectProperty<>(Persona.HOMBRE);
    private final ObjectProperty<HumanModel> person = new SimpleObjectProperty<>();
    private final BooleanProperty personLoading = new SimpleBooleanProperty(false);
    /** true = la persona elegida no está disponible: se muestra el maniquí procedural. */
    private final BooleanProperty personFallback = new SimpleBooleanProperty(false);
    private int request;
    private int personRequest;
    private BodyType loadedBody;

    public DesignerViewModel(DesignService designs, CatalogService catalog, Clock clock) {
        this.designs = designs;
        this.catalog = catalog;
        DesignDocument initial = DesignDefaults.document(BodyType.ADULTO_HOMBRE);
        this.stack = new CommandStack(initial, clock);
        document.set(initial);
        document.addListener((obs, old, doc) -> {
            if (old == null || old.bodyType() != doc.bodyType()) {
                loadModel();
            }
            Persona p = Persona.fromModelKey(doc.realisticOrDefault().modelKey());
            if (p != null) {
                persona.set(p);
            }
            updatePrice();
            if (selectedId.get() != null && doc.element(selectedId.get()).isEmpty()) {
                selectedId.set(null);
            }
        });
        materials.addListener((obs, old, m) -> updatePrice());
        product.addListener((obs, old, p) -> updatePrice());
    }

    // ---------------------------------------------------------------- Carga

    /** Materiales del catálogo (ambos ámbitos), en segundo plano. */
    public void loadMaterials() {
        Async.run(() -> {
                    Map<String, Material> map = new HashMap<>();
                    for (Material.Scope scope : Material.Scope.values()) {
                        catalog.materials(scope).forEach(m -> map.put(m.code(), m));
                    }
                    return Map.copyOf(map);
                },
                materials::set,
                e -> error.set("No se pudo cargar los materiales. " + ErrorMessages.of(e)));
    }

    /** Producto base del diseño: aporta el precio y lo que cuenta para el recargo. */
    public void loadProduct(long productId) {
        Async.run(() -> catalog.detail(productId), p -> {
            product.set(p);
            if (!java.util.Objects.equals(document.get().productId(), p.id())) {
                stack.reset(document.get().withProduct(p.id()));
                document.set(stack.current());
            }
        }, e -> product.set(null));
    }

    public void loadModel() {
        BodyType type = document.get().bodyType();
        if (type == loadedBody && model.get() != null) {
            return;
        }
        int id = ++request;
        loading.set(true);
        Async.run(() -> ModelProviders.load(type),
                data -> {
                    if (id == request) {
                        loading.set(false);
                        loadedBody = type;
                        model.set(data);
                    }
                },
                e -> {
                    if (id == request) {
                        loading.set(false);
                        error.set("No se pudo generar el maniquí 3D. " + ErrorMessages.of(e));
                    }
                });
    }

    /** Persona de la Vista 3D (la de la categoría realista), en segundo plano. Sin modelo: maniquí de respaldo. */
    public void loadPerson() {
        Persona p = persona.get();
        HumanModel shown = person.get();
        if (shown != null && shown.persona() == p && !personFallback.get()) {
            return;
        }
        int id = ++personRequest;
        if (!HumanModels.available(p)) {
            personLoading.set(false);
            personFallback.set(true);
            return;
        }
        personLoading.set(true);
        Async.run(() -> HumanModels.load(p),
                data -> {
                    if (id == personRequest) {
                        personLoading.set(false);
                        personFallback.set(false);
                        person.set(data);
                    }
                },
                e -> {
                    if (id == personRequest) {
                        personLoading.set(false);
                        personFallback.set(true);
                        error.set("No se pudo cargar la persona 3D; se muestra el maniquí. " + ErrorMessages.of(e));
                    }
                });
    }

    /** true si se ve la persona del documento vestida con su corte (lista para las fotos del pedido). */
    public boolean personMatchesDocument() {
        DressedPerson shown = person.get();
        return shown != null && !personFallback.get() && !personLoading.get() && shown.body().persona() == persona.get()
                && document.get().cut().equals(shownCut);
    }

    /** Abre un diseño guardado (reemplaza el actual y vacía el historial). */
    public void open(long id, Runnable onDone, Consumer<Throwable> onError) {
        Async.run(() -> designs.open(id), opened -> {
            designId.set(opened.summary().id());
            name.set(opened.summary().name());
            replaceDocument(opened.document());
            dirty.set(false);
            if (opened.document().productId() != null) {
                loadProduct(opened.document().productId());
            }
            onDone.run();
        }, onError);
    }

    /** Recupera el borrador autoguardado. */
    public void restore(DraftStore.Draft draft) {
        designId.set(draft.designId());
        name.set(draft.name() == null ? "" : draft.name());
        replaceDocument(DesignJson.read(draft.documentJson()));
        dirty.set(true);
        if (document.get().productId() != null) {
            loadProduct(document.get().productId());
        }
    }

    public Optional<DraftStore.Draft> draft() {
        try {
            return designs.draft();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public void discardDraft() {
        Async.run(() -> {
            designs.discardDraft();
            return true;
        }, ok -> { }, e -> { });
    }

    private void replaceDocument(DesignDocument doc) {
        stack.reset(doc);
        selectedId.set(null);
        document.set(doc);
        syncHistory();
    }

    // ---------------------------------------------------------------- Edición

    /**
     * Aplica un cambio con nombre (para "Deshacer …"). Los cambios continuos (arrastrar, deslizar, escribir) pasan
     * la misma {@code mergeKey} y quedan como un solo paso del historial.
     */
    public void edit(String label, UnaryOperator<DesignDocument> change, String mergeKey) {
        DesignDocument before = document.get();
        DesignDocument after = stack.execute(label, change, mergeKey);
        if (after != before) {
            document.set(after);
            dirty.set(true);
        }
        syncHistory();
    }

    /** Cambia solo el elemento indicado (si sigue existiendo). */
    public void editElement(String id, String label, UnaryOperator<DesignElement> change, String mergeKey) {
        edit(label, d -> d.element(id).map(e -> d.replaceElement(change.apply(e))).orElse(d), mergeKey);
    }

    /** Termina el gesto actual: el próximo cambio es un paso nuevo aunque tenga la misma clave. */
    public void seal() {
        stack.seal();
    }

    public void undo() {
        DesignDocument before = document.get();
        document.set(stack.undo());
        dirty.set(dirty.get() || document.get() != before);
        syncHistory();
    }

    public void redo() {
        DesignDocument before = document.get();
        document.set(stack.redo());
        dirty.set(dirty.get() || document.get() != before);
        syncHistory();
    }

    /** Vuelve a los colores y materiales por defecto sin elementos (se puede deshacer). */
    public void resetDesign() {
        edit("Restablecer diseño", d -> DesignDefaults.document(d.bodyType()).withProduct(d.productId()), null);
        stack.seal();
    }

    public DesignElement add(DesignElement element) {
        edit("Agregar " + element.displayName(), d -> d.addElement(element), null);
        stack.seal();
        selectedId.set(element.id());
        return element;
    }

    public void remove(String id) {
        String label = document.get().element(id).map(e -> "Eliminar " + e.displayName()).orElse("Eliminar");
        edit(label, d -> d.removeElement(id), null);
        stack.seal();
    }

    private void syncHistory() {
        canUndo.set(stack.canUndo());
        canRedo.set(stack.canRedo());
        undoLabel.set(stack.canUndo() ? "Deshacer: " + stack.undoLabel() : "Nada que deshacer");
        redoLabel.set(stack.canRedo() ? "Rehacer: " + stack.redoLabel() : "Nada que rehacer");
    }

    // ---------------------------------------------------------------- Precio

    /** Ranuras de material que cuentan para el precio: las del producto elegido o todo el uniforme. */
    public List<MaterialSlot> pricedSlots() {
        ProductDetail p = product.get();
        if (p == null || p.garment() == Garment.KIT) {
            return List.of(MaterialSlot.values());
        }
        return MaterialSlot.of(p.garment());
    }

    private void updatePrice() {
        DesignDocument doc = document.get();
        long sum = 0;
        for (MaterialSlot slot : pricedSlots()) {
            Material m = materials.get().get(doc.material(slot));
            if (m != null) {
                sum += m.surchargeCents();
            }
        }
        surchargeCents.set(sum);
        totalCents.set(product.get() == null ? sum : product.get().basePriceCents() + sum);
    }

    // ---------------------------------------------------------------- Guardar

    /** Guarda (versión nueva si ya existía) con las capturas de frente y espalda ya escritas en disco. */
    public void save(String newName, Path front, Path back, Consumer<DesignSummary> onSaved, Consumer<Throwable> onError) {
        DesignDocument doc = document.get();
        Long id = designId.get();
        saving.set(true);
        Async.run(() -> designs.save(id, newName, doc, front, back), summary -> {
            saving.set(false);
            designId.set(summary.id());
            name.set(summary.name());
            // Si se editó mientras se guardaba, sigue habiendo cambios sin guardar.
            dirty.set(document.get() != doc);
            onSaved.accept(summary);
        }, e -> {
            saving.set(false);
            onError.accept(e);
        });
    }

    /** Autoguardado: solo si hay cambios sin guardar. Corre en segundo plano y no molesta si falla. */
    public void autosave() {
        if (!dirty.get() || saving.get()) {
            return;
        }
        DesignDocument doc = document.get();
        Long id = designId.get();
        String n = name.get();
        Async.run(() -> {
            designs.saveDraft(id, n, doc);
            return true;
        }, ok -> { }, e -> { });
    }

    // ---------------------------------------------------------------- Propiedades

    public DesignDocument doc() { return document.get(); }
    public Optional<DesignElement> selected() {
        return selectedId.get() == null ? Optional.empty() : document.get().element(selectedId.get());
    }

    public ObjectProperty<DesignDocument> documentProperty() { return document; }
    public StringProperty selectedIdProperty() { return selectedId; }
    public ObjectProperty<Garment> garmentProperty() { return garment; }
    public ObjectProperty<ModelData> modelProperty() { return model; }
    public ObjectProperty<Persona> personaProperty() { return persona; }
    public ObjectProperty<HumanModel> personProperty() { return person; }
    public BooleanProperty personLoadingProperty() { return personLoading; }
    public BooleanProperty personFallbackProperty() { return personFallback; }
    public ObjectProperty<Map<String, Material>> materialsProperty() { return materials; }
    public BooleanProperty loadingProperty() { return loading; }
    public BooleanProperty savingProperty() { return saving; }
    public BooleanProperty dirtyProperty() { return dirty; }
    public BooleanProperty canUndoProperty() { return canUndo; }
    public BooleanProperty canRedoProperty() { return canRedo; }
    public StringProperty undoLabelProperty() { return undoLabel; }
    public StringProperty redoLabelProperty() { return redoLabel; }
    public StringProperty errorProperty() { return error; }
    public StringProperty nameProperty() { return name; }
    public ObjectProperty<Long> designIdProperty() { return designId; }
    public ObjectProperty<ProductDetail> productProperty() { return product; }
    public LongProperty surchargeCentsProperty() { return surchargeCents; }
    public LongProperty totalCentsProperty() { return totalCents; }
}
