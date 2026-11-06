package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.app.Router;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.ColorSwatchGroup;
import com.sportforce3d.component.ConfirmDialog;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.FilterChip;
import com.sportforce3d.component.GarmentPicker;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.MaterialPicker;
import com.sportforce3d.component.PromptDialog;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.Toast;
import com.sportforce3d.design.Area;
import com.sportforce3d.design.DesignDefaults;
import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.DesignElement;
import com.sportforce3d.design.DesignFonts;
import com.sportforce3d.design.DesignRules;
import com.sportforce3d.design.DraftStore;
import com.sportforce3d.design.ElementType;
import com.sportforce3d.design.GarmentCut;
import com.sportforce3d.design.EmblemCatalog;
import com.sportforce3d.design.MaterialSlot;
import com.sportforce3d.design.PatternSpec;
import com.sportforce3d.design.PatternType;
import com.sportforce3d.design.RealisticSpec;
import com.sportforce3d.model.DesignAsset;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.render.ArgbImage;
import com.sportforce3d.three.DesignRenderer;
import com.sportforce3d.three.ElementImages;
import com.sportforce3d.three.Png;
import com.sportforce3d.three.TextureComposer;
import com.sportforce3d.three.ViewPreset;
import com.sportforce3d.three.Viewer3D;
import com.sportforce3d.three.human.OutfitFitter.DressedPerson;
import com.sportforce3d.three.human.Persona;
import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.model.Zone;
import com.sportforce3d.util.Money;
import com.sportforce3d.viewmodel.DesignerViewModel;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Diseñador con dos vistas: la <b>vista realista</b> (principal, fotos recoloreadas: {@link RealisticDesigner}) y el
 * <b>visor 3D</b>: colores por zona, patrones y materiales; nombre, número, emblemas y logos que se arrastran sobre
 * el modelo; capas. Las dos comparten deshacer/rehacer, guardado y borrador automático; las capturas que se guardan
 * (y pasan al pedido) son las fotos realistas de frente y espalda.
 */
public class DesignerController implements Router.Routable {

    /** Máximo de texturas recompuestas por segundo mientras se edita. */
    private static final long FRAME_NANOS = 1_000_000_000L / 30;
    private static final Duration AUTOSAVE_EVERY = Duration.seconds(30);
    private static final List<Garment> GARMENTS = List.of(Garment.JERSEY, Garment.SHORTS, Garment.SOCKS, Garment.SHOES);

    private enum Tab {
        DESIGN("Diseño"), TEXT("Texto"), LOGOS("Logos"), LAYERS("Capas");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private final AppContext context;
    private final DesignerViewModel viewModel;
    private final Viewer3D viewer = new Viewer3D();
    private final ElementImages images;
    private final DesignRenderer renderer;
    private final RealisticDesigner realistic;
    private final BooleanProperty realisticMode = new SimpleBooleanProperty(true);
    /** Botones de la barra que solo sirven en el visor 3D (vistas y zoom). */
    private final List<Node> viewerButtons = new ArrayList<>();

    @FXML private HBox root;
    @FXML private StackPane stage;
    @FXML private HBox toolbar;
    @FXML private HBox draftBanner;
    @FXML private VBox loadingBox;
    @FXML private Label loadingLabel;
    @FXML private Label nameLabel;
    @FXML private HBox viewTabs;
    @FXML private HBox tabs;
    @FXML private Label hint;
    @FXML private ScrollPane scroll;
    @FXML private VBox panel;
    @FXML private VBox priceBox;
    @FXML private SfButton saveButton;
    @FXML private SfButton cartButton;

    private Tab tab = Tab.DESIGN;
    private final ToggleGroup tabGroup = new ToggleGroup();
    private Zone zone = Zone.JERSEY_BODY;
    /** Actualizadores de los controles visibles a partir del documento. */
    private final List<Runnable> syncers = new ArrayList<>();
    private boolean syncing;
    private String structureKey = "";
    private List<DesignAsset> myLogos = List.of();

    private DesignDocument applied;
    private long lastRender;
    private Garment initialFocus;
    private String dragId;
    private double dragOffU;
    private double dragOffV;

    private final AnimationTimer renderLoop = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (now - lastRender >= FRAME_NANOS) {
                flushRender();
                lastRender = now;
            }
        }
    };
    private final Timeline autosave = new Timeline();

    public DesignerController(AppContext context) {
        this.context = context;
        this.viewModel = new DesignerViewModel(context.designService(), context.catalogService(), Clock.systemUTC());
        this.images = new ElementImages(context.storage());
        this.renderer = new DesignRenderer(viewer, new TextureComposer(images));
        this.realistic = new RealisticDesigner(viewModel, context.recolorService());
    }

    @FXML
    private void initialize() {
        stage.getChildren().add(0, viewer);
        stage.getChildren().add(1, realistic.stageNode());
        root.getProperties().put(RealisticDesigner.class, realistic);
        // Para pruebas de interfaz (TestFX): el estado se lee del ViewModel, no de los controles.
        root.getProperties().put(DesignerViewModel.class, viewModel);
        buildToolbar();
        buildViewTabs();
        buildTabs();
        buildFooter();

        // La persona (o el maniquí de respaldo) se carga en segundo plano: el aviso solo se ve en el visor 3D (o al
        // guardar).
        BooleanBinding loading3d = Bindings.when(viewModel.personFallbackProperty())
                .then(viewModel.loadingProperty()).otherwise(viewModel.personLoadingProperty());
        loadingBox.visibleProperty().bind(loading3d.and(realisticMode.not()).or(viewModel.savingProperty()));
        loadingLabel.textProperty().bind(Bindings.when(viewModel.savingProperty()).then("Guardando diseño…")
                .otherwise(Bindings.when(viewModel.personFallbackProperty())
                        .then("Generando maniquí 3D…").otherwise("Cargando persona 3D…")));
        viewModel.modelProperty().addListener((obs, old, model) -> showModel(model));
        viewModel.personProperty().addListener((obs, old, person) -> showPerson(person));
        viewModel.personFallbackProperty().addListener((obs, old, fallback) -> {
            if (fallback) {
                showModel(viewModel.modelProperty().get());
            } else {
                showPerson(viewModel.personProperty().get());
            }
            structureKey = "";
            refreshPanel();
            applyHint();
        });
        viewModel.personaProperty().addListener((obs, old, p) -> {
            if (!realisticMode.get()) {
                viewModel.loadPerson();
            }
        });
        viewModel.errorProperty().addListener((obs, old, error) -> {
            if (error != null && !error.isEmpty()) {
                Toast.error(stage, error);
            }
        });
        viewModel.documentProperty().addListener((obs, old, doc) -> {
            refreshPanel();
            // Manga o largos distintos: otra prenda simulada (la cámara no se mueve, ver Viewer3D.frame).
            if (!realisticMode.get() && old != null && !old.cut().equals(doc.cut())) {
                viewModel.loadPerson();
            }
        });
        viewModel.selectedIdProperty().addListener((obs, old, id) -> {
            updateHighlight(old, id);
            // Un nombre o número elegido (en la persona, en Capas o recién agregado) abre su tarjeta en Texto.
            viewModel.selected().map(TextSlot::of).ifPresent(openText::add);
            refreshPanel();
        });
        viewModel.materialsProperty().addListener((obs, old, m) -> {
            renderer.applyMaterials(viewModel.doc(), m);
            structureKey = "";
            refreshPanel();
        });
        viewModel.garmentProperty().addListener((obs, old, g) -> {
            zone = firstZone(g);
            structureKey = "";
            refreshPanel();
        });
        viewModel.nameProperty().addListener((obs, old, n) -> updateTitle());
        viewModel.dirtyProperty().addListener((obs, old, d) -> updateTitle());
        viewModel.productProperty().addListener((obs, old, p) -> {
            updateTitle();
            updatePrice();
        });
        viewModel.totalCentsProperty().addListener((obs, old, t) -> updatePrice());

        viewer.setPickDrag(new PickDragHandler());
        // Cámara fija mientras se edita: solo la mueven el arrastre, la rueda y Frente/Espalda/Costado/Reiniciar.
        viewer.autoRotateProperty().set(false);
        root.addEventHandler(KeyEvent.KEY_PRESSED, this::onKey);
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                renderLoop.stop();
                realistic.stop();
                autosave.stop();
                viewModel.autosave();
            } else {
                renderLoop.start();
                realistic.start();
                autosave.play();
            }
        });
        autosave.getKeyFrames().add(new KeyFrame(AUTOSAVE_EVERY, e -> viewModel.autosave()));
        autosave.setCycleCount(Timeline.INDEFINITE);

        updateTitle();
        updatePrice();
        applyMode();
    }

    @Override
    public void onRoute(Object param) {
        Routes.DesignerParams p = param instanceof Routes.DesignerParams d ? d : new Routes.DesignerParams(null, null, null);
        initialFocus = p.focus() != null && GARMENTS.contains(p.focus()) ? p.focus() : null;
        // La vista realista no tiene medias sueltas ni zapatillas: esas prendas se abren en el visor 3D.
        if (initialFocus == Garment.SHOES || initialFocus == Garment.SOCKS) {
            realisticMode.set(false);
        }
        if (initialFocus != null) {
            viewModel.garmentProperty().set(initialFocus);
        }
        viewModel.loadMaterials();
        viewModel.loadModel();
        if (p.designId() != null) {
            viewModel.open(p.designId(), () -> { }, e -> Toast.error(stage, ErrorMessages.of(e)));
        } else if (p.productId() != null) {
            viewModel.loadProduct(p.productId());
        }
        loadMyLogos();
        viewModel.draft()
                .filter(d -> p.designId() == null || Objects.equals(d.designId(), p.designId()))
                .ifPresent(this::showDraftBanner);
    }

    // ---------------------------------------------------------------- Visor

    /** Maniquí procedural: solo si la persona no está disponible (respaldo). */
    private void showModel(ModelData model) {
        if (model == null || !viewModel.personFallbackProperty().get()) {
            return;
        }
        viewer.setModel(model);
        renderer.applyAll(viewModel.doc(), viewModel.materialsProperty().get());
        applied = viewModel.doc();
        // Al abrir se ve el cuerpo completo (como "Reiniciar"); solo un producto de zapatillas o medias enfoca su prenda.
        if (initialFocus != null) {
            viewer.focus(initialFocus);
        }
        initialFocus = null;
        viewer.requestFocus();
    }

    /**
     * Persona de MakeHuman con el uniforme puesto: sus prendas son zonas como las del maniquí, así que el mismo
     * {@link DesignRenderer} les aplica colores, patrones, textos, logos y telas, y se actualizan en vivo.
     */
    private void showPerson(DressedPerson person) {
        if (person == null || viewModel.personFallbackProperty().get()) {
            return;
        }
        viewer.setDressedPerson(person);
        renderer.applyAll(viewModel.doc(), viewModel.materialsProperty().get());
        applied = viewModel.doc();
        if (initialFocus != null) {
            viewer.focus(initialFocus);
        }
        initialFocus = null;
        if (!realisticMode.get()) {
            viewer.requestFocus();
        }
    }

    /** Lleva al 3D lo que cambió desde la última vez (solo las prendas afectadas). Máx. 30 veces por segundo. */
    private void flushRender() {
        DesignDocument doc = viewModel.doc();
        if (doc == applied || viewer.getModel() == null) {
            return;
        }
        Set<Garment> changed = DesignRenderer.changedGarments(applied, doc);
        renderer.applyGarments(doc, changed);
        if (DesignRenderer.materialsChanged(applied, doc)) {
            renderer.applyMaterials(doc, viewModel.materialsProperty().get());
        }
        applied = doc;
    }

    /** Arrastre de elementos sobre la prenda con la coordenada de textura del punto tocado. */
    private final class PickDragHandler implements Viewer3D.PickDrag {
        @Override
        public boolean press(Zone z, double u, double v) {
            if (z == null) {
                return false;
            }
            flushRender();
            String id = renderer.composer().hit(z.garment(), u, v, viewModel.doc());
            if (id == null) {
                return false;
            }
            DesignElement e = viewModel.doc().element(id).orElse(null);
            if (e == null) {
                return false;
            }
            viewModel.selectedIdProperty().set(id);
            if (tab == Tab.DESIGN) {
                selectTab(e.type().isText() ? Tab.TEXT : Tab.LOGOS);
            }
            if (e.locked()) {
                return false;
            }
            double[] local = e.area().toLocal(u, v);
            dragOffU = local[0] - e.u();
            dragOffV = local[1] - e.v();
            dragId = id;
            viewModel.seal();
            return true;
        }

        @Override
        public void drag(Zone z, double u, double v) {
            if (dragId == null || z == null) {
                return;
            }
            DesignElement e = viewModel.doc().element(dragId).orElse(null);
            // El elemento no sale de su zona: si el cursor sale, queda en el último punto válido.
            if (e == null || z.garment() != e.area().garment() || !e.area().contains(u, v)) {
                return;
            }
            double[] local = e.area().toLocal(u, v);
            double nu = local[0] - dragOffU;
            double nv = local[1] - dragOffV;
            viewModel.editElement(dragId, "Mover " + e.displayName(), el -> el.at(nu, nv), "move:" + dragId);
        }

        @Override
        public void release() {
            dragId = null;
            viewModel.seal();
        }
    }

    private void onKey(KeyEvent e) {
        boolean ctrl = e.isShortcutDown();
        if (ctrl && e.getCode() == KeyCode.Z && !e.isShiftDown()) {
            viewModel.undo();
        } else if (ctrl && (e.getCode() == KeyCode.Y || (e.getCode() == KeyCode.Z && e.isShiftDown()))) {
            viewModel.redo();
        } else if (ctrl && e.getCode() == KeyCode.S) {
            save(null);
        } else if ((e.getCode() == KeyCode.DELETE) && !realisticMode.get() && viewModel.selected().isPresent()
                && !(e.getTarget() instanceof javafx.scene.control.TextInputControl)) {
            viewModel.remove(viewModel.selectedIdProperty().get());
        } else {
            return;
        }
        e.consume();
    }

    // ---------------------------------------------------------------- Barra flotante

    private void buildToolbar() {
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setSpacing(4);
        // Costado: cada clic pasa al otro lado (derecha ↔ izquierda de la persona).
        boolean[] left = {false};
        Map<String, Runnable> views = new LinkedHashMap<>();
        views.put("Frente", () -> viewer.showView(ViewPreset.FRONT));
        views.put("Espalda", () -> viewer.showView(ViewPreset.BACK));
        views.put("Costado", () -> {
            viewer.showView(left[0] ? ViewPreset.LEFT : ViewPreset.RIGHT);
            left[0] = !left[0];
        });
        views.forEach((label, action) -> {
            SfButton b = new SfButton(label, SfButton.Variant.GHOST, SfButton.Size.SM);
            b.getStyleClass().add("sf-toolbar-btn");
            b.setOnAction(e -> action.run());
            toolbar.getChildren().add(b);
            viewerButtons.add(b);
        });
        SfButton zoomOut = SfButton.icon(Feather.ZOOM_OUT, "Alejar");
        zoomOut.setOnAction(e -> viewer.zoomBy(1 / 0.82));
        SfButton zoomIn = SfButton.icon(Feather.ZOOM_IN, "Acercar");
        zoomIn.setOnAction(e -> viewer.zoomBy(0.82));
        SfButton resetView = new SfButton("Reiniciar", SfButton.Variant.GHOST, SfButton.Size.SM);
        resetView.getStyleClass().add("sf-toolbar-btn");
        resetView.setOnAction(e -> {
            left[0] = false;
            viewer.resetView();
        });

        SfButton undo = SfButton.icon(Feather.CORNER_UP_LEFT, "Deshacer (Ctrl+Z)");
        undo.setOnAction(e -> viewModel.undo());
        undo.disableProperty().bind(viewModel.canUndoProperty().not());
        undo.getTooltip().textProperty().bind(viewModel.undoLabelProperty().concat(" (Ctrl+Z)"));
        SfButton redo = SfButton.icon(Feather.CORNER_UP_RIGHT, "Rehacer (Ctrl+Y)");
        redo.setOnAction(e -> viewModel.redo());
        redo.disableProperty().bind(viewModel.canRedoProperty().not());
        redo.getTooltip().textProperty().bind(viewModel.redoLabelProperty().concat(" (Ctrl+Y)"));

        SfButton capture = SfButton.icon(Feather.CAMERA, "Guardar captura (PNG)");
        capture.setOnAction(e -> capture());
        Region zoomSep = separator();
        Region historySep = separator();
        toolbar.getChildren().addAll(zoomSep, zoomOut, zoomIn, resetView, historySep, undo, redo, separator(), capture);
        viewerButtons.addAll(List.of(zoomSep, zoomOut, zoomIn, resetView, historySep));
    }

    // ---------------------------------------------------------------- Vista realista / visor 3D

    private void buildViewTabs() {
        ToggleGroup group = new ToggleGroup();
        for (boolean real : new boolean[]{true, false}) {
            ToggleButton item = new ToggleButton(real ? "Vista realista" : "Vista 3D");
            item.getStyleClass().setAll("sf-seg-item");
            item.setToggleGroup(group);
            item.setUserData(real);
            item.setMnemonicParsing(false);
            item.setSelected(real == realisticMode.get());
            item.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(item, Priority.ALWAYS);
            viewTabs.getChildren().add(item);
        }
        group.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (toggle == null) {
                group.selectToggle(old);
                return;
            }
            realisticMode.set((Boolean) toggle.getUserData());
        });
        realisticMode.addListener((obs, old, real) -> {
            group.getToggles().stream().filter(t -> real.equals(t.getUserData())).findFirst()
                    .ifPresent(group::selectToggle);
            applyMode();
        });
    }

    /** Muestra la vista elegida: escenario, botones de la barra, pestañas del 3D y panel. */
    private void applyMode() {
        boolean real = realisticMode.get();
        realistic.stageNode().setVisible(real);
        viewer.setVisible(!real);
        for (Node n : viewerButtons) {
            n.setVisible(!real);
            n.setManaged(!real);
        }
        tabs.setVisible(!real);
        tabs.setManaged(!real);
        applyHint();
        structureKey = "";
        refreshPanel();
        scroll.setVvalue(0);
        if (!real) {
            viewModel.loadPerson();
            viewer.requestFocus();
        }
    }

    private void applyHint() {
        hint.setText(realisticMode.get()
                ? "Elige categoría, corte, colores y diseño: la foto se actualiza al instante · Ctrl+Z / Ctrl+Y"
                : "Arrastra para girar 360° · rueda para acercar · arrastra un nombre, número o emblema para moverlo · Ctrl+Z / Ctrl+Y");
    }

    private static Region separator() {
        Region sep = new Region();
        sep.getStyleClass().add("sf-toolbar-sep");
        return sep;
    }

    private void capture() {
        boolean real = realisticMode.get();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar captura del diseño");
        chooser.setInitialFileName("sportforce3d-diseno.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
        File file = chooser.showSaveDialog(stage.getScene().getWindow());
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        if (real) {
            RealisticSpec spec = viewModel.realistic();
            Async.run(() -> {
                        ArgbImage[] px = realistic.renderPixels(spec);
                        RealisticDesigner.writePng(RealisticDesigner.sideBySide(px[0], px[1]), path);
                        return path;
                    },
                    p -> Toast.success(stage, "Captura guardada: " + p.getFileName()),
                    e -> Toast.error(stage, ErrorMessages.of(e)));
            return;
        }
        WritableImage image = cleanCapture(() -> viewer.capture(1600));
        Async.run(() -> {
                    Png.write(image, path);
                    return path;
                },
                p -> Toast.success(stage, "Captura guardada: " + p.getFileName()),
                e -> Toast.error(stage, ErrorMessages.of(e)));
    }

    private void showDraftBanner(DraftStore.Draft draft) {
        Label text = new Label("Tienes un borrador sin guardar" + when(draft.savedAt()) + ".");
        text.getStyleClass().addAll("body-s", "ink");
        SfButton recover = new SfButton("Recuperar", SfButton.Variant.PRIMARY, SfButton.Size.SM);
        SfButton discard = new SfButton("Descartar", SfButton.Variant.GHOST, SfButton.Size.SM);
        Runnable hide = () -> {
            draftBanner.setVisible(false);
            draftBanner.setManaged(false);
        };
        recover.setOnAction(e -> {
            try {
                viewModel.restore(draft);
                Toast.success(stage, "Borrador recuperado.");
            } catch (RuntimeException ex) {
                Toast.error(stage, ErrorMessages.of(ex));
            }
            hide.run();
        });
        discard.setOnAction(e -> {
            viewModel.discardDraft();
            hide.run();
        });
        draftBanner.setAlignment(Pos.CENTER_LEFT);
        draftBanner.getChildren().setAll(Icons.of(Feather.CLOCK, Icons.SM), text, recover, discard);
        draftBanner.setVisible(true);
        draftBanner.setManaged(true);
    }

    private static String when(String iso) {
        try {
            return " del " + OrdersController.DATE.format(Instant.parse(iso));
        } catch (RuntimeException e) {
            return "";
        }
    }

    // ---------------------------------------------------------------- Encabezado, pestañas y pie

    private void updateTitle() {
        String n = viewModel.nameProperty().get();
        StringBuilder sb = new StringBuilder(n == null || n.isBlank() ? "Diseño nuevo" : n);
        ProductDetail p = viewModel.productProperty().get();
        if (p != null) {
            sb.append(" · ").append(p.name());
        }
        if (viewModel.dirtyProperty().get()) {
            sb.append(" · sin guardar");
        }
        nameLabel.setText(sb.toString());
    }

    private void buildTabs() {
        for (Tab t : Tab.values()) {
            ToggleButton item = new ToggleButton(t.label);
            item.getStyleClass().setAll("sf-seg-item");
            item.setToggleGroup(tabGroup);
            item.setUserData(t);
            item.setMnemonicParsing(false);
            item.setSelected(t == tab);
            item.setMaxWidth(Double.MAX_VALUE);
            item.setMinWidth(Region.USE_PREF_SIZE);
            HBox.setHgrow(item, Priority.ALWAYS);
            tabs.getChildren().add(item);
        }
        tabGroup.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (toggle == null) {
                tabGroup.selectToggle(old);
                return;
            }
            tab = (Tab) toggle.getUserData();
            structureKey = "";
            refreshPanel();
            scroll.setVvalue(0);
        });
    }

    private void selectTab(Tab t) {
        tabGroup.getToggles().stream().filter(x -> x.getUserData() == t).findFirst().ifPresent(tabGroup::selectToggle);
    }

    private void buildFooter() {
        saveButton.withIcon(Feather.SAVE);
        saveButton.setOnAction(e -> save(null));
        saveButton.disableProperty().bind(viewModel.savingProperty());
        cartButton.withIcon(Feather.SHOPPING_CART);
        cartButton.setOnAction(e -> addToCart());
        cartButton.disableProperty().bind(viewModel.savingProperty());
    }

    // ---------------------------------------------------------------- Resaltado del elemento elegido

    /** Redibuja las prendas del elemento que deja de estar elegido y del nuevo, con el contorno de foco. */
    private void updateHighlight(String oldId, String newId) {
        renderer.composer().setHighlight(newId);
        if (viewer.getModel() == null) {
            return;
        }
        java.util.Set<Garment> garments = java.util.EnumSet.noneOf(Garment.class);
        for (String id : new String[]{oldId, newId}) {
            if (id != null) {
                viewModel.doc().element(id).ifPresent(e -> garments.add(e.area().garment()));
            }
        }
        flushRender();
        renderer.applyGarments(viewModel.doc(), garments);
    }

    /** Captura sin el contorno de selección (capturas guardadas, del pedido o PNG). */
    private <T> T cleanCapture(java.util.function.Supplier<T> shot) {
        flushRender();
        String selected = renderer.composer().highlight();
        if (selected == null) {
            return shot.get();
        }
        updateHighlight(selected, null);
        try {
            return shot.get();
        } finally {
            updateHighlight(null, selected);
        }
    }

    // ---------------------------------------------------------------- Agregar al carrito

    /**
     * El carrito guarda una referencia al diseño guardado: si hay cambios sin guardar (o el diseño es nuevo) primero
     * se guarda, y luego se elige producto, talla y cantidad.
     */
    private void addToCart() {
        if (viewModel.designIdProperty().get() == null || viewModel.dirtyProperty().get()) {
            save(this::openCartDialog);
        } else {
            openCartDialog();
        }
    }

    private void openCartDialog() {
        Long designId = viewModel.designIdProperty().get();
        if (designId == null) {
            return;
        }
        ProductDetail preset = viewModel.productProperty().get();
        boolean presetOk = preset != null && preset.customizable()
                && com.sportforce3d.service.CartService.DESIGNABLE.contains(preset.garment());
        Async.run(() -> presetOk ? List.<com.sportforce3d.model.ProductSummary>of()
                        : context.catalogService().designableProducts(),
                list -> showCartDialog(designId, presetOk ? preset : null, list),
                e -> Toast.error(stage, ErrorMessages.of(e)));
    }

    private void showCartDialog(long designId, ProductDetail preset, List<com.sportforce3d.model.ProductSummary> choices) {
        VBox content = new VBox(16);
        VBox productBox = new VBox(8);
        FlowPane sizes = new FlowPane(6, 6);
        com.sportforce3d.component.QuantityStepper qty = new com.sportforce3d.component.QuantityStepper();
        qty.setMin(1);
        qty.setMax(1);
        VBox priceRows = new VBox(4);
        Label stockNote = note("");
        ToggleGroup sizeGroup = new ToggleGroup();
        javafx.beans.property.ObjectProperty<ProductDetail> product = new javafx.beans.property.SimpleObjectProperty<>();
        Runnable updatePrice = () -> {
            priceRows.getChildren().clear();
            ProductDetail p = product.get();
            com.sportforce3d.model.ProductVariant v = sizeGroup.getSelectedToggle() == null ? null
                    : (com.sportforce3d.model.ProductVariant) sizeGroup.getSelectedToggle().getUserData();
            if (p == null) {
                return;
            }
            long unit = p.basePriceCents() + (v == null ? 0 : v.priceDeltaCents());
            long surcharge = com.sportforce3d.design.DesignPricing.surchargeCents(viewModel.doc(),
                    viewModel.materialsProperty().get(), p.garment());
            priceRows.getChildren().addAll(priceRow("Precio por unidad", Money.format(unit), false),
                    priceRow("Recargo por materiales", Money.surcharge(surcharge), false),
                    priceRow("Total (" + qty.getValue() + " u.)", Money.format((unit + surcharge) * qty.getValue()), true));
        };
        java.util.function.Consumer<ProductDetail> showProduct = p -> {
            product.set(p);
            sizes.getChildren().clear();
            sizeGroup.getToggles().clear();
            for (com.sportforce3d.model.ProductVariant v : p.variants()) {
                FilterChip chip = new FilterChip(v.sizeCode());
                chip.setToggleGroup(sizeGroup);
                chip.setUserData(v);
                chip.setDisable(v.available() == 0);
                chip.setAccessibleHelp(v.available() == 0 ? "Agotado" : v.available() + " disponibles");
                sizes.getChildren().add(chip);
            }
            stockNote.setText(p.variants().stream().allMatch(v -> v.available() == 0)
                    ? "Este producto está agotado." : "Elige la talla.");
            updatePrice.run();
        };
        sizeGroup.selectedToggleProperty().addListener((obs, old, t) -> {
            if (t == null) {
                stockNote.setText("Elige la talla.");
                qty.setMax(1);
            } else {
                com.sportforce3d.model.ProductVariant v = (com.sportforce3d.model.ProductVariant) t.getUserData();
                qty.setMax(Math.max(1, Math.min(com.sportforce3d.service.CartService.MAX_PER_LINE, v.available())));
                stockNote.setText(v.lowStock() ? "Quedan " + v.available() + " en talla " + v.sizeCode() + "."
                        : v.available() + " disponibles en talla " + v.sizeCode() + ".");
            }
            updatePrice.run();
        });
        qty.valueProperty().addListener((obs, old, v) -> updatePrice.run());

        if (preset != null) {
            Label name = new Label(preset.name());
            name.getStyleClass().addAll("body", "ink", "sf-strong");
            productBox.getChildren().addAll(labeled("PRODUCTO", name));
            showProduct.accept(preset);
        } else {
            ComboBox<com.sportforce3d.model.ProductSummary> combo = new ComboBox<>();
            combo.getItems().setAll(choices);
            combo.setPromptText("Elige el producto que se fabricará");
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setConverter(new StringConverter<>() {
                @Override
                public String toString(com.sportforce3d.model.ProductSummary p) {
                    return p == null ? "" : p.name() + " · " + Money.format(p.priceCents());
                }

                @Override
                public com.sportforce3d.model.ProductSummary fromString(String s) {
                    return null;
                }
            });
            combo.valueProperty().addListener((obs, old, p) -> {
                if (p != null) {
                    Async.run(() -> context.catalogService().detail(p.id()), showProduct,
                            e -> Toast.error(stage, ErrorMessages.of(e)));
                }
            });
            productBox.getChildren().add(labeled("PRODUCTO", combo));
        }
        content.getChildren().addAll(productBox, labeled("TALLA", sizes, stockNote), labeled("CANTIDAD", qty), priceRows,
                note("Al confirmar el pedido se guarda una copia de tu diseño tal como esté en ese momento."));

        com.sportforce3d.component.ModalDialog.show(stage, "Agregar al carrito",
                "Diseño: " + viewModel.nameProperty().get(), content, "Agregar al carrito",
                com.sportforce3d.component.ModalDialog.Style.ACCENT, d -> {
                    if (product.get() == null) {
                        d.setError("Elige el producto.");
                        return;
                    }
                    if (sizeGroup.getSelectedToggle() == null) {
                        d.setError("Elige la talla.");
                        return;
                    }
                    long variantId = ((com.sportforce3d.model.ProductVariant) sizeGroup.getSelectedToggle().getUserData()).id();
                    int units = qty.getValue();
                    d.setBusy(true);
                    Async.run(() -> context.cartService().addDesign(designId, variantId, units), summary -> {
                        d.close();
                        context.setCartCount(summary.itemCount());
                        Toast.success(stage, units + (units == 1 ? " uniforme agregado" : " uniformes agregados")
                                + " al carrito.");
                    }, e -> {
                        d.setBusy(false);
                        d.setError(ErrorMessages.of(e));
                    });
                });
    }

    private void updatePrice() {
        priceBox.getChildren().clear();
        ProductDetail p = viewModel.productProperty().get();
        long surcharge = viewModel.surchargeCentsProperty().get();
        if (p != null) {
            priceBox.getChildren().add(priceRow("Precio base", Money.format(p.basePriceCents()), false));
        }
        priceBox.getChildren().add(priceRow("Recargo por materiales", Money.surcharge(surcharge), false));
        if (p != null) {
            priceBox.getChildren().add(priceRow("Total por unidad", Money.format(viewModel.totalCentsProperty().get()), true));
        } else {
            Label note = new Label("Para ver el precio final, elige un producto del catálogo y pulsa “Personalizar en 3D”.");
            note.getStyleClass().addAll("body-s", "ink-subtle");
            note.setWrapText(true);
            note.setMinHeight(Region.USE_PREF_SIZE);
            priceBox.getChildren().add(note);
        }
    }

    private static HBox priceRow(String label, String value, boolean strong) {
        Label l = new Label(label);
        l.getStyleClass().addAll("body-s", strong ? "ink" : "ink-muted");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Label v = new Label(value);
        v.getStyleClass().addAll(strong ? "price" : "body-s", "ink");
        HBox row = new HBox(8, l, grow, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ---------------------------------------------------------------- Guardar

    /** Guarda con nombre; {@code after} (opcional) corre cuando el guardado terminó bien. */
    private void save(Runnable after) {
        if (viewModel.savingProperty().get()) {
            return;
        }
        List<String> problems = DesignRules.problems(viewModel.doc());
        if (!problems.isEmpty()) {
            Toast.error(stage, problems.get(0));
            return;
        }
        String current = viewModel.nameProperty().get();
        boolean existing = viewModel.designIdProperty().get() != null;
        String why = after == null ? "" : "Para agregarlo al carrito primero se guarda. ";
        PromptDialog.show(stage, existing ? "Guardar nueva versión" : "Guardar diseño",
                why + (existing ? "Se guarda como una nueva versión del mismo diseño, con capturas de frente y espalda."
                        : "Lo encontrarás en “Mis diseños”, con capturas de frente y espalda."),
                "Nombre del diseño", current == null || current.isBlank() ? "Mi uniforme" : current, "Guardar",
                n -> n.isEmpty() ? "Escribe un nombre." : n.length() > 80 ? "Máximo 80 caracteres." : null,
                n -> saveAs(n, after));
    }

    /**
     * Las capturas que se guardan (y que el pedido copia) son las fotos realistas de frente y espalda, dibujadas en
     * segundo plano con la vista realista del documento. Los buzos no tienen foto de espalda: se guarda solo el frente.
     * Si el diseño tiene otro corte o tela (que las fotos realistas no muestran), las fotos salen de la Vista 3D: el
     * taller recibe lo que se compró.
     */
    private void saveAs(String name, Runnable after) {
        if (!viewModel.doc().realisticPhotosMatch()) {
            capture3d(paths -> store(name, paths, after));
            return;
        }
        RealisticSpec spec = viewModel.realistic();
        Async.run(() -> {
            ArgbImage[] px = realistic.renderPixels(spec);
            Path f = Files.createTempFile("sf-frente", ".png");
            RealisticDesigner.writePng(px[0], f);
            Path b = null;
            if (px[1] != null) {
                b = Files.createTempFile("sf-espalda", ".png");
                RealisticDesigner.writePng(px[1], b);
            }
            return new Path[]{f, b};
        }, paths -> store(name, paths, after), e -> Toast.error(stage, ErrorMessages.of(e)));
    }

    /** Proporción (ancho/alto) de las fotos del diseño tomadas en la Vista 3D: retrato, persona completa. */
    private static final double PHOTO_ASPECT = 0.75;

    /**
     * Fotos de frente y espalda desde la Vista 3D con la persona vestida con el corte del diseño (se carga si hace
     * falta, también desde la Vista realista), cuerpo completo y fondo oscuro, sin el contorno de selección. La cámara
     * del cliente no cambia. Con el maniquí de respaldo no se guarda: no mostraría el corte elegido.
     */
    private void capture3d(java.util.function.Consumer<Path[]> done) {
        if (viewModel.personMatchesDocument() && viewer.getModel() != null) {
            shoot3d(done);
            return;
        }
        viewModel.loadPerson();
        if (viewModel.personFallbackProperty().get()) {
            Toast.error(stage, "No se pudo cargar la persona 3D con el corte elegido; el diseño no se guardó.");
            return;
        }
        Toast.info(stage, "Preparando las fotos del diseño en 3D…");
        javafx.beans.value.ChangeListener<Boolean> wait = new javafx.beans.value.ChangeListener<>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Boolean> obs, Boolean was, Boolean loading) {
                if (loading) {
                    return;
                }
                viewModel.personLoadingProperty().removeListener(this);
                // showPerson ya vistió el visor (mismo cambio de propiedad); se espera un pulso para el render.
                javafx.application.Platform.runLater(() -> {
                    if (viewModel.personMatchesDocument() && viewer.getModel() != null) {
                        shoot3d(done);
                    } else {
                        Toast.error(stage, "No se pudo cargar la persona 3D con el corte elegido; el diseño no se guardó.");
                    }
                });
            }
        };
        viewModel.personLoadingProperty().addListener(wait);
    }

    private void shoot3d(java.util.function.Consumer<Path[]> done) {
        boolean shown = viewer.isVisible();
        WritableImage[] shots;
        viewer.setVisible(true);
        try {
            shots = cleanCapture(() -> new WritableImage[]{portrait(viewer.captureView(ViewPreset.FRONT, 1400)),
                    portrait(viewer.captureView(ViewPreset.BACK, 1400))});
        } finally {
            viewer.setVisible(shown);
        }
        Async.run(() -> {
            Path f = Files.createTempFile("sf-frente", ".png");
            Png.write(shots[0], f);
            Path b = Files.createTempFile("sf-espalda", ".png");
            Png.write(shots[1], b);
            return new Path[]{f, b};
        }, done, e -> Toast.error(stage, ErrorMessages.of(e)));
    }

    /** Recorte vertical centrado (la persona completa queda en el centro del visor). */
    private static WritableImage portrait(WritableImage img) {
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        int cw = Math.min(w, (int) Math.round(h * PHOTO_ASPECT));
        int ch = Math.min(h, (int) Math.round(cw / PHOTO_ASPECT));
        return new WritableImage(img.getPixelReader(), (w - cw) / 2, (h - ch) / 2, cw, ch);
    }

    private void store(String name, Path[] paths, Runnable after) {
        viewModel.save(name, paths[0], paths[1], summary -> {
            deleteQuietly(paths);
            // Guardar borra el borrador: el aviso de recuperarlo ya no aplica.
            draftBanner.setVisible(false);
            draftBanner.setManaged(false);
            Toast.success(stage, "Diseño guardado (versión " + summary.version() + ").");
            if (after != null) {
                after.run();
            }
        }, e -> {
            deleteQuietly(paths);
            Toast.error(stage, ErrorMessages.of(e));
        });
    }

    private static void deleteQuietly(Path[] paths) {
        for (Path p : paths) {
            if (p == null) {
                continue;
            }
            try {
                Files.deleteIfExists(p);
            } catch (java.io.IOException ignored) {
                // Archivo temporal: el sistema lo limpiará.
            }
        }
    }

    // ---------------------------------------------------------------- Panel

    /**
     * Reconstruye los controles cuando cambia la estructura (pestaña, prenda, elemento elegido, lista de
     * elementos); si no, solo actualiza sus valores para no interrumpir un deslizador o un campo en uso.
     */
    private void refreshPanel() {
        if (realisticMode.get()) {
            String key = realistic.structureKey();
            if (!key.equals(structureKey)) {
                structureKey = key;
                syncers.clear();
                panel.getChildren().clear();
                realistic.build(panel);
            }
            realistic.refresh();
            return;
        }
        DesignDocument doc = viewModel.doc();
        // En Texto cada tarjeta tiene su editor: elegir otro elemento no reconstruye el panel (no se pierde el foco).
        String key = tab == Tab.TEXT ? tab + "|" + textStructureKey(doc)
                : tab + "|" + viewModel.personFallbackProperty().get() + "|" + viewModel.garmentProperty().get() + "|"
                + viewModel.selectedIdProperty().get() + "|" + doc.elements().stream().map(e -> e.id() + e.area()).toList()
                + "|" + viewModel.materialsProperty().get().size() + "|" + myLogos.size();
        if (!key.equals(structureKey)) {
            structureKey = key;
            syncers.clear();
            panel.getChildren().clear();
            switch (tab) {
                case DESIGN -> buildDesignTab();
                case TEXT -> buildTextTab();
                case LOGOS -> buildLogosTab();
                case LAYERS -> buildLayersTab();
            }
        }
        syncing = true;
        try {
            syncers.forEach(Runnable::run);
        } finally {
            syncing = false;
        }
    }

    private void sync(Runnable r) {
        syncers.add(r);
    }

    private static VBox section(String title, javafx.scene.Node... content) {
        Label label = new Label(title);
        label.getStyleClass().addAll("label-text", "ink-muted");
        VBox box = new VBox(10, label);
        box.getChildren().addAll(content);
        return box;
    }

    private static Label note(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("body-s", "ink-subtle");
        l.setWrapText(true);
        return l;
    }

    private static Zone firstZone(Garment g) {
        for (Zone z : Zone.values()) {
            if (z.garment() == g) {
                return z;
            }
        }
        return Zone.JERSEY_BODY;
    }

    private static Color color(String hex) {
        try {
            return Color.web(hex);
        } catch (RuntimeException e) {
            return Color.GRAY;
        }
    }

    // ---------------------------------------------------------------- Pestaña Diseño

    private void buildDesignTab() {
        Garment garment = viewModel.garmentProperty().get();
        GarmentPicker picker = new GarmentPicker(GARMENTS);
        picker.setSelected(garment);
        picker.selectedProperty().addListener((obs, old, g) -> {
            if (g != null && !syncing) {
                viewModel.garmentProperty().set(g);
            }
        });
        panel.getChildren().add(section("PRENDA", picker));

        if (!viewModel.personFallbackProperty().get()) {
            // Persona: la misma categoría que la vista realista.
            FlowPane persons = new FlowPane(6, 6);
            ToggleGroup personGroup = new ToggleGroup();
            for (Persona p : Persona.values()) {
                FilterChip chip = new FilterChip(p.label());
                chip.setToggleGroup(personGroup);
                chip.setOnAction(e -> {
                    chip.setSelected(true);
                    realistic.selectAudience(p.key(), p.label());
                });
                sync(() -> chip.setSelected(viewModel.personaProperty().get() == p));
                persons.getChildren().add(chip);
            }
            panel.getChildren().add(section("PERSONA", persons,
                    note("Es la misma persona de la Vista realista: si cambias una, cambia la otra.")));
        }

        panel.getChildren().add(buildCutSection());

        // Maniquí (respaldo si falta el modelo de la persona)
        FlowPane bodies = new FlowPane(6, 6);
        ToggleGroup bodyGroup = new ToggleGroup();
        for (BodyType type : BodyType.values()) {
            FilterChip chip = new FilterChip(type.label());
            chip.setToggleGroup(bodyGroup);
            chip.setOnAction(e -> {
                chip.setSelected(true);
                viewModel.edit("Maniquí · " + type.label(), d -> d.withBodyType(type), null);
            });
            sync(() -> chip.setSelected(viewModel.doc().bodyType() == type));
            bodies.getChildren().add(chip);
        }
        if (viewModel.personFallbackProperty().get()) {
            panel.getChildren().add(section("MANIQUÍ", bodies));
        }

        // Colores por zona: se elige la zona y la paleta la colorea.
        VBox zones = new VBox(4);
        ToggleGroup zoneGroup = new ToggleGroup();
        for (Zone z : Zone.values()) {
            if (z.garment() != garment) {
                continue;
            }
            Rectangle swatch = new Rectangle(16, 16);
            swatch.setArcWidth(5);
            swatch.setArcHeight(5);
            swatch.getStyleClass().add("sf-zone-swatch");
            ToggleButton row = new ToggleButton(z.label(), swatch);
            row.getStyleClass().setAll("sf-zone-row");
            row.setMaxWidth(Double.MAX_VALUE);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setGraphicTextGap(10);
            row.setToggleGroup(zoneGroup);
            row.setOnAction(e -> {
                zone = z;
                refreshPanel();
            });
            sync(() -> {
                swatch.setFill(color(viewModel.doc().color(z)));
                row.setSelected(zone == z);
            });
            zones.getChildren().add(row);
        }
        ColorSwatchGroup zoneColor = new ColorSwatchGroup();
        zoneColor.valueProperty().addListener((obs, old, c) -> {
            if (!syncing && c != null) {
                Zone z = zone;
                viewModel.edit("Color · " + z.label(), d -> d.withColor(z, DesignDefaults.hex(c)), null);
            }
        });
        sync(() -> zoneColor.setValue(color(viewModel.doc().color(zone))));
        panel.getChildren().add(section("COLORES", zones, zoneColor));

        if (garment != Garment.SHOES) {
            panel.getChildren().add(buildPatternSection(garment));
        }

        if (garment == Garment.JERSEY || garment == Garment.SHORTS) {
            panel.getChildren().add(buildFabricSection());
        }
        for (MaterialSlot slot : MaterialSlot.of(garment)) {
            if (!FABRIC_SLOTS.contains(slot)) {
                panel.getChildren().add(buildMaterialSection(slot));
            }
        }

        SfButton reset = new SfButton("Restablecer diseño", SfButton.Variant.GHOST, SfButton.Size.SM)
                .withIcon(Feather.ROTATE_CCW);
        reset.setOnAction(e -> ConfirmDialog.show(stage, "¿Restablecer el diseño?",
                "Vuelven los colores y materiales por defecto y se quitan nombres, números y emblemas. "
                        + "Puedes deshacerlo con Ctrl+Z.", "Restablecer", true, () -> {
                    viewModel.resetDesign();
                    Toast.info(stage, "Diseño restablecido. Ctrl+Z para deshacer.");
                }));
        panel.getChildren().add(reset);
    }

    /** Manga corta o larga, largo del short y de las medias (Etapa 10C): prendas simuladas de cada largo. */
    private VBox buildCutSection() {
        VBox rows = new VBox(10);
        rows.getChildren().add(cutRow("Manga", GarmentCut.Sleeve.values(), GarmentCut.Sleeve::label,
                s -> viewModel.doc().cut().sleeve() == s, (c, s) -> c.withSleeve(s), "Manga "));
        rows.getChildren().add(cutRow("Short", GarmentCut.ShortsLength.values(), GarmentCut.ShortsLength::label,
                s -> viewModel.doc().cut().shorts() == s, (c, s) -> c.withShorts(s), "Short "));
        rows.getChildren().add(cutRow("Medias", GarmentCut.SocksLength.values(), GarmentCut.SocksLength::label,
                s -> viewModel.doc().cut().socks() == s, (c, s) -> c.withSocks(s), "Medias "));
        Label detail = note("");
        sync(() -> {
            GarmentCut c = viewModel.doc().cut();
            detail.setText("Short " + c.shorts().detail() + " · medias " + c.socks().detail() + ".");
        });
        String fallback = viewModel.personFallbackProperty().get()
                ? "El maniquí de respaldo se ve siempre con manga corta y los largos de serie; el corte se guarda igual. "
                : "";
        return section("CORTE", rows, detail, note(fallback + "No cambia el precio. La Vista realista solo tiene fotos "
                + "con el corte y la tela de serie: si eliges otro corte u otra tela, las fotos del diseño (las que "
                + "recibe el taller) se toman de la Vista 3D."));
    }

    private <T> HBox cutRow(String label, T[] options, java.util.function.Function<T, String> name,
                            java.util.function.Predicate<T> selected,
                            java.util.function.BiFunction<GarmentCut, T, GarmentCut> apply, String undoLabel) {
        Label title = new Label(label);
        title.getStyleClass().addAll("body-s", "ink-muted");
        title.setMinWidth(56);
        FlowPane chips = new FlowPane(6, 6);
        ToggleGroup group = new ToggleGroup();
        for (T option : options) {
            FilterChip chip = new FilterChip(name.apply(option));
            chip.setToggleGroup(group);
            chip.setOnAction(e -> {
                chip.setSelected(true);
                if (!selected.test(option)) {
                    viewModel.edit(undoLabel + name.apply(option).toLowerCase(java.util.Locale.ROOT),
                            d -> d.withCut(apply.apply(d.cut(), option)), null);
                }
            });
            sync(() -> chip.setSelected(selected.test(option)));
            chips.getChildren().add(chip);
        }
        HBox.setHgrow(chips, Priority.ALWAYS);
        HBox row = new HBox(8, title, chips);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildPatternSection(Garment garment) {
        FlowPane chips = new FlowPane(6, 6);
        ToggleGroup group = new ToggleGroup();
        for (PatternType type : PatternType.values()) {
            FilterChip chip = new FilterChip(type.label());
            chip.setToggleGroup(group);
            chip.setOnAction(e -> {
                chip.setSelected(true);
                viewModel.edit("Patrón · " + type.label(), d -> d.withPattern(garment, d.pattern(garment).withType(type)), null);
            });
            sync(() -> chip.setSelected(viewModel.doc().pattern(garment).type() == type));
            chips.getChildren().add(chip);
        }

        ColorSwatchGroup patternColor = new ColorSwatchGroup();
        patternColor.valueProperty().addListener((obs, old, c) -> {
            if (!syncing && c != null) {
                viewModel.edit("Color del patrón", d -> d.withPattern(garment, d.pattern(garment).withColor(DesignDefaults.hex(c))), null);
            }
        });
        Label widthLabel = new Label();
        widthLabel.getStyleClass().addAll("body-s", "ink-muted");
        Slider width = new Slider(PatternSpec.MIN_WIDTH * 100, PatternSpec.MAX_WIDTH * 100, 4);
        width.valueProperty().addListener((obs, old, v) -> {
            if (!syncing) {
                double m = v.doubleValue() / 100;
                viewModel.edit("Ancho del patrón", d -> d.withPattern(garment, d.pattern(garment).withWidth(m)), "pattern-width:" + garment);
            }
        });
        width.setOnMouseReleased(e -> viewModel.seal());
        VBox widthBox = new VBox(4, widthLabel, width);
        VBox options = new VBox(10, note("Segundo color"), patternColor, widthBox);
        sync(() -> {
            PatternSpec spec = viewModel.doc().pattern(garment);
            boolean on = spec.type() != PatternType.LISO;
            options.setVisible(on);
            options.setManaged(on);
            boolean hasWidth = spec.type() != PatternType.DEGRADADO;
            widthBox.setVisible(hasWidth);
            widthBox.setManaged(hasWidth);
            patternColor.setValue(color(spec.color()));
            width.setValue(spec.width() * 100);
            widthLabel.setText(String.format("Ancho: %.1f cm", spec.width() * 100));
        });
        return section("PATRÓN", chips, options);
    }

    /** Telas del uniforme (sección TELA), en este orden; son materiales del catálogo con su recargo. */
    static final List<String> FABRICS = List.of("DRY_FIT", "POLIESTER_LISO", "ALGODON", "MALLA");
    /** La tela elegida se aplica a la camiseta y al short a la vez. */
    static final List<MaterialSlot> FABRIC_SLOTS = List.of(MaterialSlot.JERSEY, MaterialSlot.SHORTS);

    /**
     * Sección TELA: Dry-fit, Poliéster liso, Algodón y Malla deportiva para camiseta y short. Cada una cambia relieve,
     * brillo (y en la malla, agujeros) en la Vista 3D; se guarda como el material de las dos ranuras del documento, así
     * el precio usa los recargos del catálogo y la ficha técnica del ADMIN la muestra por prenda.
     */
    private VBox buildFabricSection() {
        Map<String, Material> all = viewModel.materialsProperty().get();
        List<Material> list = FABRICS.stream().map(all::get).filter(Objects::nonNull)
                .map(m -> m.code().equals(MaterialSlot.JERSEY.defaultCode()) ? included(m) : m).toList();
        if (list.isEmpty()) {
            return section("TELA", note("Cargando telas…"));
        }
        MaterialPicker picker = new MaterialPicker();
        syncing = true;
        try {
            picker.setMaterials(list);
        } finally {
            syncing = false;
        }
        picker.selectedProperty().addListener((obs, old, m) -> {
            if (!syncing && m != null) {
                viewModel.edit("Tela · " + m.name(), d -> {
                    DesignDocument out = d;
                    for (MaterialSlot slot : FABRIC_SLOTS) {
                        out = out.withMaterial(slot, m.code());
                    }
                    return out;
                }, null);
            }
        });
        Label other = note("");
        sync(() -> {
            DesignDocument doc = viewModel.doc();
            String jersey = doc.material(MaterialSlot.JERSEY);
            String shorts = doc.material(MaterialSlot.SHORTS);
            boolean same = Objects.equals(jersey, shorts) && FABRICS.contains(jersey);
            picker.setSelected(same ? all.get(jersey) : null);
            picker.sampleColorProperty().set(color(doc.color(Zone.JERSEY_BODY)));
            // Diseños anteriores pueden tener otra tela (Jacquard, Licra) o una distinta en cada prenda.
            other.setText(same ? "" : "Tela actual: camiseta " + materialName(jersey) + ", short " + materialName(shorts)
                    + ". Elige una para usarla en las dos prendas.");
            other.setVisible(!same);
            other.setManaged(!same);
        });
        return section("TELA", note("Se usa en la camiseta y el short. Cambia el relieve y el brillo en la Vista 3D. "
                + "Las que dicen “Incluido” no cambian el precio; las demás suman su recargo por prenda."), picker, other);
    }

    private String materialName(String code) {
        Material m = viewModel.materialsProperty().get().get(code);
        return m == null ? code : m.name();
    }

    private VBox buildMaterialSection(MaterialSlot slot) {
        // El material de serie de la ranura va incluido en el precio (DesignPricing): su tarjeta dice "Incluido".
        List<Material> list = viewModel.materialsProperty().get().values().stream()
                .filter(slot::accepts)
                .map(m -> m.code().equals(slot.defaultCode()) ? included(m) : m)
                .sorted(java.util.Comparator.comparingLong(Material::surchargeCents).thenComparing(Material::name))
                .toList();
        if (list.isEmpty()) {
            return section(slot.label().toUpperCase(java.util.Locale.ROOT), note("Cargando materiales…"));
        }
        MaterialPicker picker = new MaterialPicker();
        syncing = true;
        try {
            picker.setMaterials(list);
        } finally {
            syncing = false;
        }
        picker.selectedProperty().addListener((obs, old, m) -> {
            if (!syncing && m != null) {
                viewModel.edit(slot.label() + " · " + m.name(), d -> d.withMaterial(slot, m.code()), null);
            }
        });
        sync(() -> {
            picker.setSelected(viewModel.materialsProperty().get().get(viewModel.doc().material(slot)));
            picker.sampleColorProperty().set(color(viewModel.doc().color(slot.zones().get(0))));
        });
        return section(slot.label().toUpperCase(java.util.Locale.ROOT),
                note("Cada material cambia el relieve y el brillo en el 3D. Los que dicen “Incluido” no cambian el "
                        + "precio; los demás suman su recargo."), picker);
    }

    private static Material included(Material m) {
        return new Material(m.id(), m.code(), m.name(), m.composition(), m.usage(), m.scope(), m.suitableFor(), 0,
                m.roughness(), m.sheen(), m.metalness(), m.previewPattern());
    }

    // ---------------------------------------------------------------- Pestaña Texto

    /** Opciones de la pestaña Texto: cada una es una tarjeta con su propio editor debajo del botón. */
    enum TextSlot {
        BACK_NAME("Nombre en la espalda"),
        BACK_NUMBER("Número en la espalda"),
        CHEST_NUMBER("Número en el pecho"),
        SHORTS_NUMBER("Número en el short");

        final String label;

        TextSlot(String label) {
            this.label = label;
        }

        boolean matches(DesignElement e) {
            return switch (this) {
                case BACK_NAME -> e.type() == ElementType.NAME && e.area() == Area.JERSEY_BACK;
                case BACK_NUMBER -> e.type() == ElementType.NUMBER && e.area() == Area.JERSEY_BACK;
                case CHEST_NUMBER -> e.type() == ElementType.NUMBER && e.area() == Area.JERSEY_FRONT;
                case SHORTS_NUMBER -> e.type() == ElementType.NUMBER && e.area().garment() == Garment.SHORTS;
            };
        }

        /** El primer elemento del documento que corresponde a esta opción (o null). */
        DesignElement find(DesignDocument doc) {
            return doc.elements().stream().filter(this::matches).findFirst().orElse(null);
        }

        static TextSlot of(DesignElement e) {
            for (TextSlot s : values()) {
                if (s.matches(e)) {
                    return s;
                }
            }
            return null;
        }

        DesignElement create(DesignDocument doc) {
            String fill = contrast(doc.color(Zone.JERSEY_BODY));
            String edge = fill.equals("#FFFFFF") ? "#0B0E13" : "#FFFFFF";
            String shortsFill = contrast(doc.color(Zone.SHORTS_BASE));
            // Con contorno desde el inicio: sobre franjas o colores medios el texto sin borde se pierde.
            return switch (this) {
                case BACK_NAME -> DesignDefaults.name("TU NOMBRE", fill, edge).withOutline(0.004);
                case BACK_NUMBER -> DesignDefaults.backNumber("10", fill, edge).withOutline(0.006);
                case CHEST_NUMBER -> DesignDefaults.chestNumber("10", fill, edge).withOutline(0.003);
                case SHORTS_NUMBER -> DesignDefaults.shortsNumber("10", shortsFill,
                        shortsFill.equals("#FFFFFF") ? "#0B0E13" : "#FFFFFF").withOutline(0.003);
            };
        }
    }

    /** Tarjetas de texto abiertas (con su editor a la vista). */
    private final Set<TextSlot> openText = java.util.EnumSet.noneOf(TextSlot.class);

    private String textStructureKey(DesignDocument doc) {
        StringBuilder sb = new StringBuilder();
        for (TextSlot s : TextSlot.values()) {
            DesignElement e = s.find(doc);
            sb.append(s).append('=').append(e == null ? "-" : e.id()).append(openText.contains(s) ? "+" : "").append(';');
        }
        return sb.append(doc.elements().stream().filter(e -> e.type().isText() && TextSlot.of(e) == null).count())
                .toString();
    }

    private void buildTextTab() {
        DesignDocument doc = viewModel.doc();
        VBox cards = new VBox(8);
        for (TextSlot slot : TextSlot.values()) {
            cards.getChildren().add(textCard(slot, slot.find(doc)));
        }
        panel.getChildren().add(section("NOMBRE Y NÚMEROS", cards,
                note("Arrastra el texto sobre la persona para moverlo dentro de su zona.")));
        long others = doc.elements().stream().filter(e -> e.type().isText() && TextSlot.of(e) == null).count();
        if (others > 0) {
            panel.getChildren().add(note(others == 1 ? "Hay 1 texto más en otra zona: edítalo en Capas."
                    : "Hay " + others + " textos más en otras zonas: edítalos en Capas."));
        }
    }

    /**
     * Tarjeta de una opción de texto: el botón la agrega (o abre y cierra su editor) y el editor (texto, tipografía,
     * color, contorno, tamaño y giro) aparece justo debajo, con su botón para quitarla.
     */
    private VBox textCard(TextSlot slot, DesignElement element) {
        VBox card = new VBox(4);
        card.getStyleClass().add("sf-text-card");
        boolean open = element != null && openText.contains(slot);
        SfButton header = new SfButton(slot.label, SfButton.Variant.GHOST, SfButton.Size.SM)
                .withIcon(element == null ? Feather.PLUS : open ? Feather.CHEVRON_UP : Feather.CHEVRON_DOWN);
        header.getStyleClass().add("sf-text-card-head");
        header.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(header, Priority.ALWAYS);
        header.setAccessibleHelp(element == null ? "Agregar" : open ? "Cerrar ajustes" : "Abrir ajustes");
        header.setOnAction(e -> {
            DesignElement cur = slot.find(viewModel.doc());
            if (cur == null) {
                if (viewModel.doc().elements().size() >= DesignRules.MAX_ELEMENTS) {
                    Toast.error(stage, "Máximo " + DesignRules.MAX_ELEMENTS + " elementos por diseño.");
                    return;
                }
                openText.add(slot);
                viewModel.add(slot.create(viewModel.doc()));
            } else if (openText.remove(slot)) {
                refreshPanel();
            } else {
                openText.add(slot);
                viewModel.selectedIdProperty().set(cur.id());
                refreshPanel();
            }
        });
        HBox head = new HBox(4, header);
        head.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(head);
        if (element == null) {
            return card;
        }
        String id = element.id();
        SfButton remove = SfButton.icon(Feather.TRASH_2, "Quitar " + slot.label.toLowerCase(java.util.Locale.ROOT));
        remove.setOnAction(e -> {
            openText.remove(slot);
            viewModel.remove(id);
        });
        head.getChildren().add(remove);
        sync(() -> card.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"),
                id.equals(viewModel.selectedIdProperty().get())));
        if (open) {
            VBox editor = new VBox(16);
            editor.getStyleClass().add("sf-text-card-editor");
            elementControls(element, editor, false);
            card.getChildren().add(editor);
            // Usar el editor elige el elemento: se resalta en la persona (sin reconstruir el panel).
            editor.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, x -> select(id));
            editor.lookupAll(".text-field").forEach(n -> n.focusedProperty().addListener((obs, was, now) -> {
                if (now) {
                    select(id);
                }
            }));
        }
        return card;
    }

    private void select(String id) {
        if (!id.equals(viewModel.selectedIdProperty().get())) {
            viewModel.selectedIdProperty().set(id);
        }
    }

    /** Blanco o negro, lo que más contraste con el color de fondo. */
    private static String contrast(String hex) {
        Color c = color(hex);
        double lum = 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue();
        return lum > 0.55 ? "#0B0E13" : "#FFFFFF";
    }

    // ---------------------------------------------------------------- Pestaña Logos

    private void buildLogosTab() {
        DesignDocument doc = viewModel.doc();
        // Escudo del color del cuello (el acento del uniforme) con detalle en blanco o negro; si el cuello es del
        // mismo color que el cuerpo, escudo en blanco/negro con detalle del color del cuerpo.
        String body = doc.color(Zone.JERSEY_BODY);
        String collar = doc.color(Zone.JERSEY_COLLAR);
        String primary = Objects.equals(collar, body) ? contrast(body) : collar;
        String accent = Objects.equals(collar, body) ? body : contrast(collar);

        FlowPane emblems = new FlowPane(8, 8);
        for (String key : EmblemCatalog.EMBLEMS.keySet()) {
            SfButton b = imageButton(images.emblem(key, null, null), EmblemCatalog.label(key));
            b.setOnAction(e -> addGraphic(DesignDefaults.emblem(key, primary, accent)));
            emblems.getChildren().add(b);
        }
        panel.getChildren().add(section("EMBLEMAS", emblems,
                note("Se agregan en el pecho con los colores de tu camiseta; luego puedes cambiarlos.")));

        CheckBox rights = new CheckBox("Declaro que este logo es mío o tengo permiso para usarlo. No es una marca de terceros.");
        rights.setWrapText(true);
        SfButton upload = new SfButton("Subir logo (PNG o SVG)", SfButton.Variant.PRIMARY, SfButton.Size.SM)
                .withIcon(Feather.UPLOAD);
        upload.disableProperty().bind(rights.selectedProperty().not());
        upload.setOnAction(e -> uploadLogo());
        FlowPane mine = new FlowPane(8, 8);
        for (DesignAsset a : myLogos) {
            Image img = images.logo(a.path());
            SfButton b = imageButton(img, a.originalName());
            b.setOnAction(e -> addGraphic(DesignDefaults.logo(a.id(), a.path())));
            mine.getChildren().add(b);
        }
        VBox logos = section("TU LOGO", note("Máximo 5 MB. Se guarda en tu cuenta para otros diseños."), rights, upload);
        if (!myLogos.isEmpty()) {
            logos.getChildren().addAll(note("Tus logos"), mine);
        }
        panel.getChildren().add(logos);

        DesignElement selected = viewModel.selected().orElse(null);
        if (selected != null && !selected.type().isText()) {
            panel.getChildren().add(buildInspector(selected));
        }
    }

    private SfButton imageButton(Image image, String label) {
        ImageView view = new ImageView(image);
        view.setFitWidth(52);
        view.setFitHeight(52);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        SfButton b = new SfButton("", SfButton.Variant.GHOST, SfButton.Size.SM);
        b.setGraphic(view);
        b.getStyleClass().add("sf-emblem-btn");
        b.setAccessibleText(label);
        b.setTooltip(new Tooltip(label));
        return b;
    }

    private void addGraphic(DesignElement el) {
        if (viewModel.doc().elements().size() >= DesignRules.MAX_ELEMENTS) {
            Toast.error(stage, "Máximo " + DesignRules.MAX_ELEMENTS + " elementos por diseño.");
            return;
        }
        viewModel.add(el);
    }

    private void uploadLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir logo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG o SVG", "*.png", "*.svg"));
        File file = chooser.showOpenDialog(stage.getScene().getWindow());
        if (file == null) {
            return;
        }
        Async.run(() -> context.designService().uploadLogo(file.toPath(), true), asset -> {
            Toast.success(stage, "Logo subido.");
            addGraphic(DesignDefaults.logo(asset.id(), asset.path()));
            loadMyLogos();
        }, e -> Toast.error(stage, ErrorMessages.of(e)));
    }

    private void loadMyLogos() {
        Async.run(context.designService()::myLogos, list -> {
            myLogos = list;
            refreshPanel();
        }, e -> { });
    }

    // ---------------------------------------------------------------- Pestaña Capas

    private void buildLayersTab() {
        List<DesignElement> elements = viewModel.doc().elements();
        if (elements.isEmpty()) {
            panel.getChildren().add(new EmptyState(Feather.LAYERS, "Sin capas todavía",
                    "Agrega un nombre, un número o un emblema desde las pestañas Texto y Logos."));
            return;
        }
        VBox list = new VBox(4);
        // La capa de arriba (la última que se dibuja) va primero.
        for (int i = elements.size() - 1; i >= 0; i--) {
            list.getChildren().add(layerRow(elements.get(i), i, elements.size()));
        }
        panel.getChildren().add(section("CAPAS", note("La de arriba se dibuja encima de las demás."), list));
        viewModel.selected().ifPresent(e -> panel.getChildren().add(buildInspector(e)));
    }

    private HBox layerRow(DesignElement e, int index, int count) {
        String id = e.id();
        SfButton eye = SfButton.icon(e.visible() ? Feather.EYE : Feather.EYE_OFF, e.visible() ? "Ocultar" : "Mostrar");
        eye.setOnAction(x -> viewModel.editElement(id, e.visible() ? "Ocultar capa" : "Mostrar capa",
                el -> el.withVisible(!el.visible()), null));
        SfButton lock = SfButton.icon(e.locked() ? Feather.LOCK : Feather.UNLOCK, e.locked() ? "Desbloquear" : "Bloquear");
        lock.setOnAction(x -> viewModel.editElement(id, e.locked() ? "Desbloquear capa" : "Bloquear capa",
                el -> el.withLocked(!el.locked()), null));
        Label name = new Label();
        name.getStyleClass().addAll("body-s", "ink");
        name.setWrapText(true);
        Label area = new Label(e.area().label());
        area.getStyleClass().addAll("body-s", "ink-subtle");
        VBox text = new VBox(0, name, area);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);
        SfButton up = SfButton.icon(Feather.ARROW_UP, "Subir capa");
        up.setDisable(index == count - 1);
        up.setOnAction(x -> viewModel.edit("Subir capa", d -> d.moveLayer(id, 1), null));
        SfButton down = SfButton.icon(Feather.ARROW_DOWN, "Bajar capa");
        down.setDisable(index == 0);
        down.setOnAction(x -> viewModel.edit("Bajar capa", d -> d.moveLayer(id, -1), null));
        SfButton delete = SfButton.icon(Feather.TRASH_2, "Eliminar");
        delete.setOnAction(x -> viewModel.remove(id));

        HBox row = new HBox(4, eye, lock, text, up, down, delete);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("sf-layer-row");
        row.setOnMouseClicked(x -> viewModel.selectedIdProperty().set(id));
        sync(() -> {
            DesignElement cur = viewModel.doc().element(id).orElse(e);
            name.setText(cur.displayName());
            row.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"),
                    id.equals(viewModel.selectedIdProperty().get()));
            row.setOpacity(cur.visible() ? 1 : 0.55);
        });
        return row;
    }

    // ---------------------------------------------------------------- Inspector del elemento elegido

    private VBox buildInspector(DesignElement e) {
        String id = e.id();
        VBox box = new VBox(16);
        box.getStyleClass().add("sf-inspector");

        Label title = new Label();
        title.getStyleClass().addAll("title-s", "ink");
        title.setWrapText(true);
        title.setMinWidth(0);
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setMaxWidth(Double.MAX_VALUE);
        SfButton close = SfButton.icon(Feather.X, "Deseleccionar");
        close.setOnAction(x -> viewModel.selectedIdProperty().set(null));
        SfButton delete = SfButton.icon(Feather.TRASH_2, "Eliminar (Supr)");
        delete.setOnAction(x -> viewModel.remove(id));
        HBox head = new HBox(4, title, delete, close);
        head.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(head);
        sync(() -> viewModel.doc().element(id).ifPresent(cur -> title.setText(cur.displayName())));
        elementControls(e, box, true);
        return box;
    }

    /** Controles de un elemento (texto, tipografía, zona, colores, tamaño y giro) dentro de {@code box}. */
    private void elementControls(DesignElement e, VBox box, boolean withArea) {
        String id = e.id();
        if (e.type().isText()) {
            boolean isName = e.type() == ElementType.NAME;
            SfTextField field = new SfTextField(isName ? "Nombre (máx. 12)" : "Número (0–99)");
            field.getInput().setTextFormatter(new TextFormatter<String>(change -> {
                String next = change.getControlNewText();
                if (isName) {
                    change.setText(change.getText().toUpperCase(java.util.Locale.forLanguageTag("es-PE")));
                    return next.length() <= DesignRules.NAME_MAX ? change : null;
                }
                return next.matches("[0-9]{0,2}") ? change : null;
            }));
            field.textProperty().addListener((obs, old, t) -> {
                if (syncing) {
                    return;
                }
                String clean = isName ? DesignRules.cleanName(t) : DesignRules.cleanNumber(t);
                if (clean == null) {
                    field.setError("Escribe un número de 0 a 99.");
                    return;
                }
                field.setError(isName && !clean.equals(t.toUpperCase(java.util.Locale.forLanguageTag("es-PE")))
                        ? "Solo letras, espacios, punto, guion y apóstrofo." : "");
                viewModel.editElement(id, isName ? "Nombre" : "Número", el -> el.withText(clean), "text:" + id);
            });
            sync(() -> viewModel.doc().element(id).ifPresent(cur -> {
                String shown = isName ? DesignRules.cleanName(field.getText()) : DesignRules.cleanNumber(field.getText());
                if (!Objects.equals(shown, cur.text())) {
                    field.setText(cur.text());
                }
            }));

            ComboBox<String> font = new ComboBox<>();
            font.getItems().setAll(DesignFonts.LABELS.keySet());
            font.setConverter(new StringConverter<>() {
                @Override
                public String toString(String key) {
                    return key == null ? "" : DesignFonts.LABELS.getOrDefault(key, key);
                }

                @Override
                public String fromString(String s) {
                    return s;
                }
            });
            font.setMaxWidth(Double.MAX_VALUE);
            font.valueProperty().addListener((obs, old, k) -> {
                if (!syncing && k != null) {
                    viewModel.editElement(id, "Tipografía", el -> el.withFont(k), null);
                }
            });
            sync(() -> viewModel.doc().element(id).ifPresent(cur -> font.setValue(cur.font())));
            box.getChildren().addAll(field, labeled("TIPOGRAFÍA", font));
        }

        if (withArea) {
            box.getChildren().add(labeled("ZONA", areaCombo(e)));
        }

        if (e.type() != ElementType.LOGO) {
            ColorSwatchGroup fill = colorGroup(id, e.type().isText() ? "Color del texto" : "Color principal",
                    DesignElement::color, (el, hex) -> el.withColor(hex));
            box.getChildren().add(labeled(e.type().isText() ? "COLOR" : "COLOR PRINCIPAL", fill));
            ColorSwatchGroup accent = colorGroup(id, e.type().isText() ? "Color del contorno" : "Color de acento",
                    DesignElement::accentColor, (el, hex) -> el.withAccent(hex));
            if (e.type().isText()) {
                Slider outline = slider(0, 12, e.outline() * 1000, "outline:" + id, "Contorno",
                        (el, v) -> el.withOutline(v / 1000), el -> el.outline() * 1000, id);
                Label outlineLabel = valueLabel(id, el -> el.outline() == 0 ? "Sin contorno"
                        : String.format("Grosor: %.1f mm", el.outline() * 1000));
                box.getChildren().add(labeled("CONTORNO", accent, outlineLabel, outline));
            } else {
                box.getChildren().add(labeled("COLOR DE ACENTO", accent));
            }
        }

        Slider size = slider(DesignElement.MIN_SCALE * 100, DesignElement.MAX_SCALE * 100, e.scale() * 100, "scale:" + id,
                "Tamaño", (el, v) -> el.withScale(v / 100), el -> el.scale() * 100, id);
        Label sizeLabel = valueLabel(id, el -> String.format("%.0f %%", el.scale() * 100));
        Slider rotation = slider(-180, 180, e.rotation(), "rotate:" + id, "Giro",
                (el, v) -> el.withRotation(v), DesignElement::rotation, id);
        Label rotationLabel = valueLabel(id, el -> String.format("%.0f°", el.rotation()));
        box.getChildren().addAll(labeled("TAMAÑO", sizeLabel, size), labeled("GIRO", rotationLabel, rotation));
    }

    private ComboBox<Area> areaCombo(DesignElement e) {
        ComboBox<Area> combo = new ComboBox<>();
        combo.getItems().setAll(Area.values());
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Area a) {
                return a == null ? "" : a.label();
            }

            @Override
            public Area fromString(String s) {
                return null;
            }
        });
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setValue(e.area());
        combo.valueProperty().addListener((obs, old, a) -> {
            if (!syncing && a != null && a != old) {
                viewModel.editElement(e.id(), "Zona · " + a.label(), el -> el.withArea(a).at(0.5, 0.5), null);
                viewModel.garmentProperty().set(a.garment());
            }
        });
        return combo;
    }

    private ColorSwatchGroup colorGroup(String id, String label, java.util.function.Function<DesignElement, String> get,
                                        java.util.function.BiFunction<DesignElement, String, DesignElement> set) {
        ColorSwatchGroup group = new ColorSwatchGroup();
        group.setAccessibleText(label);
        group.valueProperty().addListener((obs, old, c) -> {
            if (!syncing && c != null) {
                viewModel.editElement(id, label, el -> set.apply(el, DesignDefaults.hex(c)), null);
            }
        });
        sync(() -> viewModel.doc().element(id).ifPresent(cur -> {
            String hex = get.apply(cur);
            if (hex != null) {
                group.setValue(color(hex));
            }
        }));
        return group;
    }

    private Slider slider(double min, double max, double value, String mergeKey, String label,
                          java.util.function.BiFunction<DesignElement, Double, DesignElement> set,
                          java.util.function.ToDoubleFunction<DesignElement> get, String id) {
        Slider s = new Slider(min, max, value);
        s.setAccessibleText(label);
        s.valueProperty().addListener((obs, old, v) -> {
            if (!syncing) {
                viewModel.editElement(id, label, el -> set.apply(el, v.doubleValue()), mergeKey);
            }
        });
        s.setOnMouseReleased(x -> viewModel.seal());
        sync(() -> viewModel.doc().element(id).ifPresent(cur -> s.setValue(get.applyAsDouble(cur))));
        return s;
    }

    private Label valueLabel(String id, java.util.function.Function<DesignElement, String> text) {
        Label l = new Label();
        l.getStyleClass().addAll("body-s", "ink-muted");
        sync(() -> viewModel.doc().element(id).ifPresent(cur -> l.setText(text.apply(cur))));
        return l;
    }

    private static VBox labeled(String title, javafx.scene.Node... content) {
        Label label = new Label(title);
        label.getStyleClass().addAll("label-text", "ink-muted");
        VBox box = new VBox(8, label);
        box.getChildren().addAll(content);
        return box;
    }

}
