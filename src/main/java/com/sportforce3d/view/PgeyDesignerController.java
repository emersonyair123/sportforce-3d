package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.app.PpqhRouter;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PgeyColorSwatchGroup;
import com.sportforce3d.component.PpqhConfirmDialog;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.HepyFilterChip;
import com.sportforce3d.component.PgeyGarmentPicker;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PgeyMaterialPicker;
import com.sportforce3d.component.PpqhPromptDialog;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.design.PgeyArea;
import com.sportforce3d.design.PgeyDesignDefaults;
import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeyDesignElement;
import com.sportforce3d.design.PgeyDesignFonts;
import com.sportforce3d.design.PgeyDesignRules;
import com.sportforce3d.design.PgeyDraftStore;
import com.sportforce3d.design.PgeyElementType;
import com.sportforce3d.design.PgeyGarmentCut;
import com.sportforce3d.design.PgeyEmblemCatalog;
import com.sportforce3d.design.PgeyMaterialSlot;
import com.sportforce3d.design.PgeyPatternSpec;
import com.sportforce3d.design.PgeyPatternType;
import com.sportforce3d.design.PgeyRealisticSpec;
import com.sportforce3d.design.PgeyRealisticView;
import com.sportforce3d.design.PgeyTextSlot;
import com.sportforce3d.design.PgeyViewSync;
import com.sportforce3d.model.PgeyDesignAsset;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.render.PgeyArgbImage;
import com.sportforce3d.three.PgeyDesignRenderer;
import com.sportforce3d.three.PgeyElementImages;
import com.sportforce3d.three.PgeyPng;
import com.sportforce3d.three.PgeyTextureComposer;
import com.sportforce3d.three.PgeyViewPreset;
import com.sportforce3d.three.PgeyViewer3D;
import com.sportforce3d.three.human.PgeyOutfitFitter.PgeyDressedPerson;
import com.sportforce3d.three.human.PgeyPersona;
import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.model.PgeyZone;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.PgeyDesignerViewModel;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
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

public class PgeyDesignerController implements PpqhRouter.PpqhRoutable {
    private static final long PGEY_FRAME_NANOS = 1_000_000_000L / 30;
    private static final Duration PGEY_AUTOSAVE_EVERY = Duration.seconds(30);
    private static final List<HepyGarment> PGEY_GARMENTS = List.of(HepyGarment.JERSEY, HepyGarment.SHORTS, HepyGarment.SOCKS, HepyGarment.SHOES);

    private enum PgeyTab {
        DESIGN("Diseño"), TEXT("Texto"), LOGOS("Logos"), LAYERS("Capas");

        final String pgeyLabel;

        PgeyTab(String pgeyLabel) {
            this.pgeyLabel = pgeyLabel;
        }
    }

    private final PpqhAppContext pgeyContext;
    private final PgeyDesignerViewModel pgeyViewModel;
    private final PgeyViewer3D pgeyViewer = new PgeyViewer3D();
    private final PgeyElementImages pgeyImages;
    private final PgeyDesignRenderer pgeyRenderer;
    private final PgeyRealisticDesigner pgeyRealistic;
    private final BooleanProperty pgeyRealisticMode = new SimpleBooleanProperty(true);
    private final List<Node> pgeyViewerButtons = new ArrayList<>();

    @FXML private HBox pgeyRoot;
    @FXML private StackPane pgeyStage;
    @FXML private HBox pgeyToolbar;
    @FXML private HBox pgeyDraftBanner;
    @FXML private VBox pgeyLoadingBox;
    @FXML private Label pgeyLoadingLabel;
    @FXML private Label pgeyNameLabel;
    @FXML private HBox pgeyViewTabs;
    @FXML private HBox pgeyTabs;
    @FXML private Label pgeyHint;
    @FXML private ScrollPane pgeyScroll;
    @FXML private VBox pgeyPanel;
    @FXML private VBox pgeyPriceBox;
    @FXML private PpqhSfButton pgeySaveButton;
    @FXML private PpqhSfButton pgeyCartButton;

    private PgeyTab pgeyTab = PgeyTab.DESIGN;
    private final ToggleGroup pgeyTabGroup = new ToggleGroup();
    private PgeyZone pgeyZone = PgeyZone.JERSEY_BODY;
    private final List<Runnable> pgeySyncers = new ArrayList<>();
    private boolean pgeySyncing;
    private String pgeyStructureKey = "";
    private List<PgeyDesignAsset> pgeyMyLogos = List.of();

    private PgeyDesignDocument pgeyApplied;
    private long pgeyLastRender;
    private HepyGarment pgeyInitialFocus;
    private String pgeyDragId;
    private double pgeyDragOffU;
    private double pgeyDragOffV;

    private final AnimationTimer pgeyRenderLoop = new AnimationTimer() {
        @Override
        public void handle(long pgeyNow) {
            if (pgeyNow - pgeyLastRender >= PGEY_FRAME_NANOS) {
                pgeyFlushRender();
                pgeyLastRender = pgeyNow;
            }
        }
    };
    private final Timeline pgeyAutosave = new Timeline();

    public PgeyDesignerController(PpqhAppContext pgeyContext) {
        this.pgeyContext = pgeyContext;
        this.pgeyViewModel = new PgeyDesignerViewModel(pgeyContext.ppqhDesignService(), pgeyContext.ppqhCatalogService(), Clock.systemUTC());
        this.pgeyImages = new PgeyElementImages(pgeyContext.ppqhStorage());
        this.pgeyRenderer = new PgeyDesignRenderer(pgeyViewer, new PgeyTextureComposer(pgeyImages));
        this.pgeyRealistic = new PgeyRealisticDesigner(pgeyViewModel, pgeyContext.ppqhRecolorService(), pgeyImages);
    }

    @FXML
    private void initialize() {
        pgeyStage.getChildren().add(0, pgeyViewer);
        pgeyStage.getChildren().add(1, pgeyRealistic.pgeyStageNode());
        pgeyNotices3d.getStyleClass().addAll("body-s", "ink-muted", "sf-realistic-notices");
        pgeyNotices3d.setWrapText(true);
        pgeyNotices3d.setMaxWidth(560);
        pgeyNotices3d.setMouseTransparent(true);
        pgeyNotices3d.setVisible(false);
        StackPane.setAlignment(pgeyNotices3d, Pos.BOTTOM_CENTER);
        StackPane.setMargin(pgeyNotices3d, new javafx.geometry.Insets(0, 16, 40, 16));
        pgeyStage.getChildren().add(2, pgeyNotices3d);
        pgeyRoot.getProperties().put(PgeyRealisticDesigner.class, pgeyRealistic);
        pgeyRoot.getProperties().put(PgeyDesignerViewModel.class, pgeyViewModel);
        pgeyBuildToolbar();
        pgeyBuildViewTabs();
        pgeyBuildTabs();
        pgeyBuildFooter();

        BooleanBinding pgeyLoading3d = Bindings.when(pgeyViewModel.personFallbackProperty())
                .then(pgeyViewModel.loadingProperty()).otherwise(pgeyViewModel.personLoadingProperty());
        pgeyLoadingBox.visibleProperty().bind(pgeyLoading3d.and(pgeyRealisticMode.not()).or(pgeyViewModel.savingProperty()));
        pgeyLoadingLabel.textProperty().bind(Bindings.when(pgeyViewModel.savingProperty()).then("Guardando diseño…")
                .otherwise(Bindings.when(pgeyViewModel.personFallbackProperty())
                        .then("Generando maniquí 3D…").otherwise("Cargando persona 3D…")));
        pgeyViewModel.modelProperty().addListener((pgeyObs, pgeyOld, pgeyModel) -> pgeyShowModel(pgeyModel));
        pgeyViewModel.personProperty().addListener((pgeyObs, pgeyOld, pgeyPerson) -> pgeyShowPerson(pgeyPerson));
        pgeyViewModel.personFallbackProperty().addListener((pgeyObs, pgeyOld, pgeyFallback) -> {
            if (pgeyFallback) {
                pgeyShowModel(pgeyViewModel.modelProperty().get());
            } else {
                pgeyShowPerson(pgeyViewModel.personProperty().get());
            }
            pgeyStructureKey = "";
            pgeyRefreshPanel();
            pgeyApplyHint();
        });
        pgeyViewModel.personaProperty().addListener((pgeyObs, pgeyOld, pgeyP) -> {
            if (!pgeyRealisticMode.get()) {
                pgeyViewModel.pgeyLoadPerson();
            }
        });
        pgeyViewModel.errorProperty().addListener((pgeyObs, pgeyOld, pgeyError) -> {
            if (pgeyError != null && !pgeyError.isEmpty()) {
                PpqhToast.ppqhError(pgeyStage, pgeyError);
            }
        });
        pgeyViewModel.documentProperty().addListener((pgeyObs, pgeyOld, pgeyDoc) -> {
            pgeyRefreshPanel();
            pgeyUpdateNotices3d();
            if (!pgeyRealisticMode.get() && pgeyOld != null && !pgeyOld.pgeyCut().equals(pgeyDoc.pgeyCut())) {
                pgeyViewModel.pgeyLoadPerson();
            }
        });
        pgeyViewModel.selectedIdProperty().addListener((pgeyObs, pgeyOld, pgeyId) -> {
            pgeyUpdateHighlight(pgeyOld, pgeyId);
            pgeyViewModel.pgeySelected().map(PgeyTextSlot::pgeyOf).ifPresent(pgeyOpenText::add);
            pgeyRefreshPanel();
        });
        pgeyViewModel.materialsProperty().addListener((pgeyObs, pgeyOld, pgeyM) -> {
            pgeyRenderer.pgeyApplyMaterials(pgeyViewModel.pgeyDoc(), pgeyM);
            pgeyStructureKey = "";
            pgeyRefreshPanel();
        });
        pgeyViewModel.garmentProperty().addListener((pgeyObs, pgeyOld, pgeyG) -> {
            pgeyZone = pgeyFirstZone(pgeyG);
            pgeyStructureKey = "";
            pgeyRefreshPanel();
        });
        pgeyViewModel.nameProperty().addListener((pgeyObs, pgeyOld, pgeyN) -> pgeyUpdateTitle());
        pgeyViewModel.dirtyProperty().addListener((pgeyObs, pgeyOld, pgeyD) -> pgeyUpdateTitle());
        pgeyViewModel.productProperty().addListener((pgeyObs, pgeyOld, pgeyP) -> {
            pgeyUpdateTitle();
            pgeyUpdatePrice();
        });
        pgeyViewModel.totalCentsProperty().addListener((pgeyObs, pgeyOld, pgeyT) -> pgeyUpdatePrice());

        pgeyViewer.setPickDrag(new PgeyPickDragHandler());
        pgeyViewer.autoRotateProperty().set(false);
        pgeyRoot.addEventHandler(KeyEvent.KEY_PRESSED, this::pgeyOnKey);
        pgeyRoot.sceneProperty().addListener((pgeyObs, pgeyOld, pgeyScene) -> {
            if (pgeyScene == null) {
                pgeyRenderLoop.stop();
                pgeyRealistic.stop();
                pgeyAutosave.stop();
                pgeyViewModel.pgeyAutosave();
            } else {
                pgeyRenderLoop.start();
                pgeyRealistic.start();
                pgeyAutosave.play();
            }
        });
        pgeyAutosave.getKeyFrames().add(new KeyFrame(PGEY_AUTOSAVE_EVERY, pgeyE -> pgeyViewModel.pgeyAutosave()));
        pgeyAutosave.setCycleCount(Timeline.INDEFINITE);

        pgeyUpdateTitle();
        pgeyUpdatePrice();
        pgeyApplyMode();
    }

    @Override
    public void ppqhOnRoute(Object pgeyParam) {
        PpqhRoutes.PpqhDesignerParams pgeyP = pgeyParam instanceof PpqhRoutes.PpqhDesignerParams pgeyD ? pgeyD : new PpqhRoutes.PpqhDesignerParams(null, null, null);
        pgeyInitialFocus = pgeyP.ppqhFocus() != null && PGEY_GARMENTS.contains(pgeyP.ppqhFocus()) ? pgeyP.ppqhFocus() : null;
        if (pgeyInitialFocus == HepyGarment.SHOES || pgeyInitialFocus == HepyGarment.SOCKS) {
            pgeyRealisticMode.set(false);
        }
        if (pgeyInitialFocus != null) {
            pgeyViewModel.garmentProperty().set(pgeyInitialFocus);
        }
        pgeyViewModel.pgeyLoadMaterials();
        pgeyViewModel.pgeyLoadModel();
        if (pgeyP.ppqhDesignId() != null) {
            pgeyViewModel.pgeyOpen(pgeyP.ppqhDesignId(), () -> { }, pgeyE -> PpqhToast.ppqhError(pgeyStage, PpqhErrorMessages.ppqhOf(pgeyE)));
        } else if (pgeyP.ppqhProductId() != null) {
            pgeyViewModel.pgeyLoadProduct(pgeyP.ppqhProductId());
        }
        pgeyLoadMyLogos();
        pgeyViewModel.pgeyDraft()
                .filter(pgeyD -> pgeyP.ppqhDesignId() == null || Objects.equals(pgeyD.pgeyDesignId(), pgeyP.ppqhDesignId()))
                .ifPresent(this::pgeyShowDraftBanner);
    }

    private void pgeyShowModel(PgeyModelData pgeyModel) {
        if (pgeyModel == null || !pgeyViewModel.personFallbackProperty().get()) {
            return;
        }
        pgeyViewer.setModel(pgeyModel);
        pgeyRenderer.pgeyApplyAll(pgeyViewModel.pgeyDoc(), pgeyViewModel.materialsProperty().get());
        pgeyApplied = pgeyViewModel.pgeyDoc();
        if (pgeyInitialFocus != null) {
            pgeyViewer.pgeyFocus(pgeyInitialFocus);
        }
        pgeyInitialFocus = null;
        pgeyViewer.requestFocus();
    }

    private void pgeyShowPerson(PgeyDressedPerson pgeyPerson) {
        if (pgeyPerson == null || pgeyViewModel.personFallbackProperty().get()) {
            return;
        }
        pgeyViewer.setDressedPerson(pgeyPerson);
        pgeyRenderer.pgeyApplyAll(pgeyViewModel.pgeyDoc(), pgeyViewModel.materialsProperty().get());
        pgeyApplied = pgeyViewModel.pgeyDoc();
        if (pgeyInitialFocus != null) {
            pgeyViewer.pgeyFocus(pgeyInitialFocus);
        }
        pgeyInitialFocus = null;
        if (!pgeyRealisticMode.get()) {
            pgeyViewer.requestFocus();
        }
    }

    private void pgeyFlushRender() {
        PgeyDesignDocument pgeyDoc = pgeyViewModel.pgeyDoc();
        if (pgeyDoc == pgeyApplied || pgeyViewer.getModel() == null) {
            return;
        }
        Set<HepyGarment> pgeyChanged = PgeyDesignRenderer.pgeyChangedGarments(pgeyApplied, pgeyDoc);
        pgeyRenderer.pgeyApplyGarments(pgeyDoc, pgeyChanged);
        if (PgeyDesignRenderer.pgeyMaterialsChanged(pgeyApplied, pgeyDoc)) {
            pgeyRenderer.pgeyApplyMaterials(pgeyDoc, pgeyViewModel.materialsProperty().get());
        }
        pgeyApplied = pgeyDoc;
    }

    private final class PgeyPickDragHandler implements PgeyViewer3D.PgeyPickDrag {
        @Override
        public boolean pgeyPress(PgeyZone pgeyZ, double pgeyU, double pgeyV) {
            if (pgeyZ == null) {
                return false;
            }
            pgeyFlushRender();
            String pgeyId = pgeyRenderer.pgeyComposer().pgeyHit(pgeyZ.pgeyGarment(), pgeyU, pgeyV, pgeyViewModel.pgeyDoc());
            if (pgeyId == null) {
                return false;
            }
            PgeyDesignElement pgeyE = pgeyViewModel.pgeyDoc().pgeyElement(pgeyId).orElse(null);
            if (pgeyE == null) {
                return false;
            }
            pgeyViewModel.selectedIdProperty().set(pgeyId);
            if (pgeyTab == PgeyTab.DESIGN) {
                pgeySelectTab(pgeyE.pgeyType().pgeyIsText() ? PgeyTab.TEXT : PgeyTab.LOGOS);
            }
            if (pgeyE.pgeyLocked()) {
                return false;
            }
            double[] pgeyLocal = pgeyE.pgeyArea().pgeyToLocal(pgeyU, pgeyV);
            pgeyDragOffU = pgeyLocal[0] - pgeyE.pgeyU();
            pgeyDragOffV = pgeyLocal[1] - pgeyE.pgeyV();
            pgeyDragId = pgeyId;
            pgeyViewModel.pgeySeal();
            return true;
        }

        @Override
        public void pgeyDrag(PgeyZone pgeyZ, double pgeyU, double pgeyV) {
            if (pgeyDragId == null || pgeyZ == null) {
                return;
            }
            PgeyDesignElement pgeyE = pgeyViewModel.pgeyDoc().pgeyElement(pgeyDragId).orElse(null);
            if (pgeyE == null || pgeyZ.pgeyGarment() != pgeyE.pgeyArea().pgeyGarment() || !pgeyE.pgeyArea().pgeyContains(pgeyU, pgeyV)) {
                return;
            }
            double[] pgeyLocal = pgeyE.pgeyArea().pgeyToLocal(pgeyU, pgeyV);
            double pgeyNu = pgeyLocal[0] - pgeyDragOffU;
            double pgeyNv = pgeyLocal[1] - pgeyDragOffV;
            pgeyViewModel.pgeyEditElement(pgeyDragId, "Mover " + pgeyE.pgeyDisplayName(), pgeyEl -> pgeyEl.pgeyAt(pgeyNu, pgeyNv), "move:" + pgeyDragId);
        }

        @Override
        public void pgeyRelease() {
            pgeyDragId = null;
            pgeyViewModel.pgeySeal();
        }
    }

    private void pgeyOnKey(KeyEvent pgeyE) {
        boolean pgeyCtrl = pgeyE.isShortcutDown();
        if (pgeyCtrl && pgeyE.getCode() == KeyCode.Z && !pgeyE.isShiftDown()) {
            pgeyViewModel.pgeyUndo();
        } else if (pgeyCtrl && (pgeyE.getCode() == KeyCode.Y || (pgeyE.getCode() == KeyCode.Z && pgeyE.isShiftDown()))) {
            pgeyViewModel.pgeyRedo();
        } else if (pgeyCtrl && pgeyE.getCode() == KeyCode.S) {
            pgeySave(null);
        } else if ((pgeyE.getCode() == KeyCode.DELETE) && !pgeyRealisticMode.get() && pgeyViewModel.pgeySelected().isPresent()
                && !(pgeyE.getTarget() instanceof javafx.scene.control.TextInputControl)) {
            pgeyViewModel.pgeyRemove(pgeyViewModel.selectedIdProperty().get());
        } else {
            return;
        }
        pgeyE.consume();
    }

    private void pgeyBuildToolbar() {
        pgeyToolbar.setAlignment(Pos.CENTER);
        pgeyToolbar.setSpacing(4);
        boolean[] pgeyLeft = {false};
        Map<String, Runnable> pgeyViews = new LinkedHashMap<>();
        pgeyViews.put("Frente", () -> pgeyViewer.pgeyShowView(PgeyViewPreset.FRONT));
        pgeyViews.put("Espalda", () -> pgeyViewer.pgeyShowView(PgeyViewPreset.BACK));
        pgeyViews.put("Costado", () -> {
            pgeyViewer.pgeyShowView(pgeyLeft[0] ? PgeyViewPreset.LEFT : PgeyViewPreset.RIGHT);
            pgeyLeft[0] = !pgeyLeft[0];
        });
        pgeyViews.forEach((pgeyLabel, pgeyAction) -> {
            PpqhSfButton pgeyB = new PpqhSfButton(pgeyLabel, PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
            pgeyB.getStyleClass().add("sf-toolbar-btn");
            pgeyB.setOnAction(pgeyE -> pgeyAction.run());
            pgeyToolbar.getChildren().add(pgeyB);
            pgeyViewerButtons.add(pgeyB);
        });
        PpqhSfButton pgeyZoomOut = PpqhSfButton.ppqhIcon(Feather.ZOOM_OUT, "Alejar");
        pgeyZoomOut.setOnAction(pgeyE -> pgeyViewer.pgeyZoomBy(1 / 0.82));
        PpqhSfButton pgeyZoomIn = PpqhSfButton.ppqhIcon(Feather.ZOOM_IN, "Acercar");
        pgeyZoomIn.setOnAction(pgeyE -> pgeyViewer.pgeyZoomBy(0.82));
        PpqhSfButton pgeyResetView = new PpqhSfButton("Reiniciar", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
        pgeyResetView.getStyleClass().add("sf-toolbar-btn");
        pgeyResetView.setOnAction(pgeyE -> {
            pgeyLeft[0] = false;
            pgeyViewer.pgeyResetView();
        });

        PpqhSfButton pgeyUndo = PpqhSfButton.ppqhIcon(Feather.CORNER_UP_LEFT, "Deshacer (Ctrl+Z)");
        pgeyUndo.setOnAction(pgeyE -> pgeyViewModel.pgeyUndo());
        pgeyUndo.disableProperty().bind(pgeyViewModel.canUndoProperty().not());
        pgeyUndo.getTooltip().textProperty().bind(pgeyViewModel.undoLabelProperty().concat(" (Ctrl+Z)"));
        PpqhSfButton pgeyRedo = PpqhSfButton.ppqhIcon(Feather.CORNER_UP_RIGHT, "Rehacer (Ctrl+Y)");
        pgeyRedo.setOnAction(pgeyE -> pgeyViewModel.pgeyRedo());
        pgeyRedo.disableProperty().bind(pgeyViewModel.canRedoProperty().not());
        pgeyRedo.getTooltip().textProperty().bind(pgeyViewModel.redoLabelProperty().concat(" (Ctrl+Y)"));

        PpqhSfButton pgeyCapture = PpqhSfButton.ppqhIcon(Feather.CAMERA, "Guardar captura (PNG)");
        pgeyCapture.setOnAction(pgeyE -> pgeyCapture());
        Region pgeyZoomSep = pgeySeparator();
        Region pgeyHistorySep = pgeySeparator();
        pgeyToolbar.getChildren().addAll(pgeyZoomSep, pgeyZoomOut, pgeyZoomIn, pgeyResetView, pgeyHistorySep, pgeyUndo, pgeyRedo, pgeySeparator(), pgeyCapture);
        pgeyViewerButtons.addAll(List.of(pgeyZoomSep, pgeyZoomOut, pgeyZoomIn, pgeyResetView, pgeyHistorySep));
    }

    private void pgeyBuildViewTabs() {
        ToggleGroup pgeyGroup = new ToggleGroup();
        for (boolean pgeyReal : new boolean[]{true, false}) {
            ToggleButton pgeyItem = new ToggleButton(pgeyReal ? "Vista realista" : "Vista 3D");
            pgeyItem.getStyleClass().setAll("sf-seg-item");
            pgeyItem.setToggleGroup(pgeyGroup);
            pgeyItem.setUserData(pgeyReal);
            pgeyItem.setMnemonicParsing(false);
            pgeyItem.setSelected(pgeyReal == pgeyRealisticMode.get());
            pgeyItem.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(pgeyItem, Priority.ALWAYS);
            pgeyViewTabs.getChildren().add(pgeyItem);
        }
        pgeyGroup.selectedToggleProperty().addListener((pgeyObs, pgeyOld, pgeyToggle) -> {
            if (pgeyToggle == null) {
                pgeyGroup.selectToggle(pgeyOld);
                return;
            }
            pgeyRealisticMode.set((Boolean) pgeyToggle.getUserData());
        });
        pgeyRealisticMode.addListener((pgeyObs, pgeyOld, pgeyReal) -> {
            pgeyGroup.getToggles().stream().filter(pgeyT -> pgeyReal.equals(pgeyT.getUserData())).findFirst()
                    .ifPresent(pgeyGroup::selectToggle);
            pgeyApplyMode();
        });
    }

    private void pgeyApplyMode() {
        boolean pgeyReal = pgeyRealisticMode.get();
        pgeyRealistic.pgeyStageNode().setVisible(pgeyReal);
        pgeyViewer.setVisible(!pgeyReal);
        for (Node pgeyN : pgeyViewerButtons) {
            pgeyN.setVisible(!pgeyReal);
            pgeyN.setManaged(!pgeyReal);
        }
        pgeyTabs.setVisible(!pgeyReal);
        pgeyTabs.setManaged(!pgeyReal);
        pgeyApplyHint();
        pgeyUpdateNotices3d();
        pgeyStructureKey = "";
        pgeyRefreshPanel();
        pgeyScroll.setVvalue(0);
        if (!pgeyReal) {
            pgeyViewModel.pgeyLoadPerson();
            pgeyViewer.requestFocus();
        }
    }

    private final Label pgeyNotices3d = new Label();

    private void pgeyUpdateNotices3d() {
        java.util.List<String> pgeyList = PgeyViewSync.pgeyNotices3d(pgeyViewModel.pgeyDoc());
        pgeyNotices3d.setText(String.join(" · ", pgeyList));
        boolean pgeyShow = !pgeyList.isEmpty() && !pgeyRealisticMode.get();
        pgeyNotices3d.setVisible(pgeyShow);
    }

    private void pgeyApplyHint() {
        pgeyHint.setText(pgeyRealisticMode.get()
                ? "Elige categoría, corte, colores y diseño: la foto se actualiza al instante · Ctrl+Z / Ctrl+Y"
                : "Arrastra para girar 360° · rueda para acercar · arrastra un nombre, número o emblema para moverlo · Ctrl+Z / Ctrl+Y");
    }

    private static Region pgeySeparator() {
        Region pgeySep = new Region();
        pgeySep.getStyleClass().add("sf-toolbar-sep");
        return pgeySep;
    }

    private void pgeyCapture() {
        boolean pgeyReal = pgeyRealisticMode.get();
        FileChooser pgeyChooser = new FileChooser();
        pgeyChooser.setTitle("Guardar captura del diseño");
        pgeyChooser.setInitialFileName("sportforce3d-diseno.png");
        pgeyChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
        File pgeyFile = pgeyChooser.showSaveDialog(pgeyStage.getScene().getWindow());
        if (pgeyFile == null) {
            return;
        }
        Path pgeyPath = pgeyFile.toPath();
        if (pgeyReal) {
            PgeyRealisticView pgeyView = pgeyViewModel.pgeyRealisticView();
            PgeyRealisticPreview.PgeyCrest pgeyCrest = pgeyRealistic.pgeyCrest(pgeyView);
            PpqhAsync.ppqhRun(() -> {
                        PgeyArgbImage[] pgeyPx = pgeyRealistic.pgeyRenderPixels(pgeyView, pgeyCrest);
                        PgeyRealisticDesigner.pgeyWritePng(PgeyRealisticDesigner.pgeySideBySide(pgeyPx[0], pgeyPx[1]), pgeyPath);
                        return pgeyPath;
                    },
                    pgeyP -> PpqhToast.ppqhSuccess(pgeyStage, "Captura guardada: " + pgeyP.getFileName()),
                    pgeyE -> PpqhToast.ppqhError(pgeyStage, PpqhErrorMessages.ppqhOf(pgeyE)));
            return;
        }
        WritableImage pgeyImage = pgeyCleanCapture(() -> pgeyViewer.pgeyCapture(1600));
        PpqhAsync.ppqhRun(() -> {
                    PgeyPng.pgeyWrite(pgeyImage, pgeyPath);
                    return pgeyPath;
                },
                pgeyP -> PpqhToast.ppqhSuccess(pgeyStage, "Captura guardada: " + pgeyP.getFileName()),
                pgeyE -> PpqhToast.ppqhError(pgeyStage, PpqhErrorMessages.ppqhOf(pgeyE)));
    }

    private void pgeyShowDraftBanner(PgeyDraftStore.PgeyDraft pgeyDraft) {
        Label pgeyText = new Label("Tienes un borrador sin guardar" + pgeyWhen(pgeyDraft.pgeySavedAt()) + ".");
        pgeyText.getStyleClass().addAll("body-s", "ink");
        PpqhSfButton pgeyRecover = new PpqhSfButton("Recuperar", PpqhSfButton.PpqhVariant.PRIMARY, PpqhSfButton.PpqhSize.SM);
        PpqhSfButton pgeyDiscard = new PpqhSfButton("Descartar", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
        Runnable pgeyHide = () -> {
            pgeyDraftBanner.setVisible(false);
            pgeyDraftBanner.setManaged(false);
        };
        pgeyRecover.setOnAction(pgeyE -> {
            try {
                pgeyViewModel.pgeyRestore(pgeyDraft);
                PpqhToast.ppqhSuccess(pgeyStage, "Borrador recuperado.");
            } catch (RuntimeException pgeyEx) {
                PpqhToast.ppqhError(pgeyStage, PpqhErrorMessages.ppqhOf(pgeyEx));
            }
            pgeyHide.run();
        });
        pgeyDiscard.setOnAction(pgeyE -> {
            pgeyViewModel.pgeyDiscardDraft();
            pgeyHide.run();
        });
        pgeyDraftBanner.setAlignment(Pos.CENTER_LEFT);
        pgeyDraftBanner.getChildren().setAll(PpqhIcons.ppqhOf(Feather.CLOCK, PpqhIcons.PPQH_SM), pgeyText, pgeyRecover, pgeyDiscard);
        pgeyDraftBanner.setVisible(true);
        pgeyDraftBanner.setManaged(true);
    }

    private static String pgeyWhen(String pgeyIso) {
        try {
            return " del " + PpqhOrdersController.PPQH_DATE.format(Instant.parse(pgeyIso));
        } catch (RuntimeException pgeyE) {
            return "";
        }
    }

    private void pgeyUpdateTitle() {
        String pgeyN = pgeyViewModel.nameProperty().get();
        StringBuilder pgeySb = new StringBuilder(pgeyN == null || pgeyN.isBlank() ? "Diseño nuevo" : pgeyN);
        HepyProductDetail pgeyP = pgeyViewModel.productProperty().get();
        if (pgeyP != null) {
            pgeySb.append(" · ").append(pgeyP.hepyName());
        }
        if (pgeyViewModel.dirtyProperty().get()) {
            pgeySb.append(" · sin guardar");
        }
        pgeyNameLabel.setText(pgeySb.toString());
    }

    private void pgeyBuildTabs() {
        for (PgeyTab pgeyT : PgeyTab.values()) {
            ToggleButton pgeyItem = new ToggleButton(pgeyT.pgeyLabel);
            pgeyItem.getStyleClass().setAll("sf-seg-item");
            pgeyItem.setToggleGroup(pgeyTabGroup);
            pgeyItem.setUserData(pgeyT);
            pgeyItem.setMnemonicParsing(false);
            pgeyItem.setSelected(pgeyT == pgeyTab);
            pgeyItem.setMaxWidth(Double.MAX_VALUE);
            pgeyItem.setMinWidth(Region.USE_PREF_SIZE);
            HBox.setHgrow(pgeyItem, Priority.ALWAYS);
            pgeyTabs.getChildren().add(pgeyItem);
        }
        pgeyTabGroup.selectedToggleProperty().addListener((pgeyObs, pgeyOld, pgeyToggle) -> {
            if (pgeyToggle == null) {
                pgeyTabGroup.selectToggle(pgeyOld);
                return;
            }
            pgeyTab = (PgeyTab) pgeyToggle.getUserData();
            pgeyStructureKey = "";
            pgeyRefreshPanel();
            pgeyScroll.setVvalue(0);
        });
    }

    private void pgeySelectTab(PgeyTab pgeyT) {
        pgeyTabGroup.getToggles().stream().filter(pgeyX -> pgeyX.getUserData() == pgeyT).findFirst().ifPresent(pgeyTabGroup::selectToggle);
    }

    private void pgeyBuildFooter() {
        pgeySaveButton.ppqhWithIcon(Feather.SAVE);
        pgeySaveButton.setOnAction(pgeyE -> pgeySave(null));
        pgeySaveButton.disableProperty().bind(pgeyViewModel.savingProperty());
        pgeyCartButton.ppqhWithIcon(Feather.SHOPPING_CART);
        pgeyCartButton.setOnAction(pgeyE -> pgeyAddToCart());
        pgeyCartButton.disableProperty().bind(pgeyViewModel.savingProperty());
    }

    private void pgeyUpdateHighlight(String pgeyOldId, String pgeyNewId) {
        pgeyRenderer.pgeyComposer().pgeySetHighlight(pgeyNewId);
        if (pgeyViewer.getModel() == null) {
            return;
        }
        java.util.Set<HepyGarment> pgeyGarments = java.util.EnumSet.noneOf(HepyGarment.class);
        for (String pgeyId : new String[]{pgeyOldId, pgeyNewId}) {
            if (pgeyId != null) {
                pgeyViewModel.pgeyDoc().pgeyElement(pgeyId).ifPresent(pgeyE -> pgeyGarments.add(pgeyE.pgeyArea().pgeyGarment()));
            }
        }
        pgeyFlushRender();
        pgeyRenderer.pgeyApplyGarments(pgeyViewModel.pgeyDoc(), pgeyGarments);
    }

    private <T> T pgeyCleanCapture(java.util.function.Supplier<T> pgeyShot) {
        pgeyFlushRender();
        String pgeySelected = pgeyRenderer.pgeyComposer().pgeyHighlight();
        if (pgeySelected == null) {
            return pgeyShot.get();
        }
        pgeyUpdateHighlight(pgeySelected, null);
        try {
            return pgeyShot.get();
        } finally {
            pgeyUpdateHighlight(null, pgeySelected);
        }
    }

    private void pgeyAddToCart() {
        if (pgeyViewModel.designIdProperty().get() == null || pgeyViewModel.dirtyProperty().get()) {
            pgeySave(this::pgeyOpenCartDialog);
        } else {
            pgeyOpenCartDialog();
        }
    }

    private void pgeyOpenCartDialog() {
        Long pgeyDesignId = pgeyViewModel.designIdProperty().get();
        if (pgeyDesignId == null) {
            return;
        }
        HepyProductDetail pgeyPreset = pgeyViewModel.productProperty().get();
        boolean pgeyPresetOk = pgeyPreset != null && pgeyPreset.hepyCustomizable()
                && com.sportforce3d.service.HepyCartService.HEPY_DESIGNABLE.contains(pgeyPreset.hepyGarment());
        PpqhAsync.ppqhRun(() -> pgeyPresetOk ? List.<com.sportforce3d.model.HepyProductSummary>of()
                        : pgeyContext.ppqhCatalogService().hepyDesignableProducts(),
                pgeyList -> pgeyShowCartDialog(pgeyDesignId, pgeyPresetOk ? pgeyPreset : null, pgeyList),
                pgeyE -> PpqhToast.ppqhError(pgeyStage, PpqhErrorMessages.ppqhOf(pgeyE)));
    }

    private void pgeyShowCartDialog(long pgeyDesignId, HepyProductDetail pgeyPreset, List<com.sportforce3d.model.HepyProductSummary> pgeyChoices) {
        VBox pgeyContent = new VBox(16);
        VBox pgeyProductBox = new VBox(8);
        FlowPane pgeySizes = new FlowPane(6, 6);
        com.sportforce3d.component.HepyQuantityStepper pgeyQty = new com.sportforce3d.component.HepyQuantityStepper();
        pgeyQty.setMin(1);
        pgeyQty.setMax(1);
        VBox pgeyPriceRows = new VBox(4);
        Label pgeyStockNote = pgeyNote("");
        ToggleGroup pgeySizeGroup = new ToggleGroup();
        javafx.beans.property.ObjectProperty<HepyProductDetail> pgeyProduct = new javafx.beans.property.SimpleObjectProperty<>();
        Runnable pgeyUpdatePrice = () -> {
            pgeyPriceRows.getChildren().clear();
            HepyProductDetail pgeyP = pgeyProduct.get();
            com.sportforce3d.model.HepyProductVariant pgeyV = pgeySizeGroup.getSelectedToggle() == null ? null
                    : (com.sportforce3d.model.HepyProductVariant) pgeySizeGroup.getSelectedToggle().getUserData();
            if (pgeyP == null) {
                return;
            }
            long pgeyUnit = pgeyP.hepyBasePriceCents() + (pgeyV == null ? 0 : pgeyV.hepyPriceDeltaCents());
            long pgeySurcharge = com.sportforce3d.design.PgeyDesignPricing.pgeySurchargeCents(pgeyViewModel.pgeyDoc(),
                    pgeyViewModel.materialsProperty().get(), pgeyP.hepyGarment());
            pgeyPriceRows.getChildren().addAll(pgeyPriceRow("Precio por unidad", HepyMoney.hepyFormat(pgeyUnit), false),
                    pgeyPriceRow("Recargo por materiales", HepyMoney.hepySurcharge(pgeySurcharge), false),
                    pgeyPriceRow("Total (" + pgeyQty.getValue() + " u.)", HepyMoney.hepyFormat((pgeyUnit + pgeySurcharge) * pgeyQty.getValue()), true));
        };
        java.util.function.Consumer<HepyProductDetail> pgeyShowProduct = pgeyP -> {
            pgeyProduct.set(pgeyP);
            pgeySizes.getChildren().clear();
            pgeySizeGroup.getToggles().clear();
            for (com.sportforce3d.model.HepyProductVariant pgeyV : pgeyP.hepyVariants()) {
                HepyFilterChip pgeyChip = new HepyFilterChip(pgeyV.hepySizeCode());
                pgeyChip.setToggleGroup(pgeySizeGroup);
                pgeyChip.setUserData(pgeyV);
                pgeyChip.setDisable(pgeyV.hepyAvailable() == 0);
                pgeyChip.setAccessibleHelp(pgeyV.hepyAvailable() == 0 ? "Agotado" : pgeyV.hepyAvailable() + " disponibles");
                pgeySizes.getChildren().add(pgeyChip);
            }
            pgeyStockNote.setText(pgeyP.hepyVariants().stream().allMatch(pgeyV -> pgeyV.hepyAvailable() == 0)
                    ? "Este producto está agotado." : "Elige la talla.");
            pgeyUpdatePrice.run();
        };
        pgeySizeGroup.selectedToggleProperty().addListener((pgeyObs, pgeyOld, pgeyT) -> {
            if (pgeyT == null) {
                pgeyStockNote.setText("Elige la talla.");
                pgeyQty.setMax(1);
            } else {
                com.sportforce3d.model.HepyProductVariant pgeyV = (com.sportforce3d.model.HepyProductVariant) pgeyT.getUserData();
                pgeyQty.setMax(Math.max(1, Math.min(com.sportforce3d.service.HepyCartService.HEPY_MAX_PER_LINE, pgeyV.hepyAvailable())));
                pgeyStockNote.setText(pgeyV.hepyLowStock() ? "Quedan " + pgeyV.hepyAvailable() + " en talla " + pgeyV.hepySizeCode() + "."
                        : pgeyV.hepyAvailable() + " disponibles en talla " + pgeyV.hepySizeCode() + ".");
            }
            pgeyUpdatePrice.run();
        });
        pgeyQty.valueProperty().addListener((pgeyObs, pgeyOld, pgeyV) -> pgeyUpdatePrice.run());

        if (pgeyPreset != null) {
            Label pgeyName = new Label(pgeyPreset.hepyName());
            pgeyName.getStyleClass().addAll("body", "ink", "sf-strong");
            pgeyProductBox.getChildren().addAll(pgeyLabeled("PRODUCTO", pgeyName));
            pgeyShowProduct.accept(pgeyPreset);
        } else {
            ComboBox<com.sportforce3d.model.HepyProductSummary> pgeyCombo = new ComboBox<>();
            pgeyCombo.getItems().setAll(pgeyChoices);
            pgeyCombo.setPromptText("Elige el producto que se fabricará");
            pgeyCombo.setMaxWidth(Double.MAX_VALUE);
            pgeyCombo.setConverter(new StringConverter<>() {
                @Override
                public String toString(com.sportforce3d.model.HepyProductSummary pgeyP) {
                    return pgeyP == null ? "" : pgeyP.hepyName() + " · " + HepyMoney.hepyFormat(pgeyP.hepyPriceCents());
                }

                @Override
                public com.sportforce3d.model.HepyProductSummary fromString(String pgeyS) {
                    return null;
                }
            });
            pgeyCombo.valueProperty().addListener((pgeyObs, pgeyOld, pgeyP) -> {
                if (pgeyP != null) {
                    PpqhAsync.ppqhRun(() -> pgeyContext.ppqhCatalogService().hepyDetail(pgeyP.hepyId()), pgeyShowProduct,
                            pgeyE -> PpqhToast.ppqhError(pgeyStage, PpqhErrorMessages.ppqhOf(pgeyE)));
                }
            });
            pgeyProductBox.getChildren().add(pgeyLabeled("PRODUCTO", pgeyCombo));
        }
        pgeyContent.getChildren().addAll(pgeyProductBox, pgeyLabeled("TALLA", pgeySizes, pgeyStockNote), pgeyLabeled("CANTIDAD", pgeyQty), pgeyPriceRows,
                pgeyNote("Al confirmar el pedido se guarda una copia de tu diseño tal como esté en ese momento."));

        com.sportforce3d.component.PpqhModalDialog.ppqhShow(pgeyStage, "Agregar al carrito",
                "Diseño: " + pgeyViewModel.nameProperty().get(), pgeyContent, "Agregar al carrito",
                com.sportforce3d.component.PpqhModalDialog.PpqhStyle.ACCENT, pgeyD -> {
                    if (pgeyProduct.get() == null) {
                        pgeyD.ppqhSetError("Elige el producto.");
                        return;
                    }
                    if (pgeySizeGroup.getSelectedToggle() == null) {
                        pgeyD.ppqhSetError("Elige la talla.");
                        return;
                    }
                    long pgeyVariantId = ((com.sportforce3d.model.HepyProductVariant) pgeySizeGroup.getSelectedToggle().getUserData()).hepyId();
                    int pgeyUnits = pgeyQty.getValue();
                    pgeyD.ppqhSetBusy(true);
                    PpqhAsync.ppqhRun(() -> pgeyContext.ppqhCartService().hepyAddDesign(pgeyDesignId, pgeyVariantId, pgeyUnits), pgeySummary -> {
                        pgeyD.ppqhClose();
                        pgeyContext.setCartCount(pgeySummary.hepyItemCount());
                        PpqhToast.ppqhSuccess(pgeyStage, pgeyUnits + (pgeyUnits == 1 ? " uniforme agregado" : " uniformes agregados")
                                + " al carrito.");
                    }, pgeyE -> {
                        pgeyD.ppqhSetBusy(false);
                        pgeyD.ppqhSetError(PpqhErrorMessages.ppqhOf(pgeyE));
                    });
                });
    }

    private void pgeyUpdatePrice() {
        pgeyPriceBox.getChildren().clear();
        HepyProductDetail pgeyP = pgeyViewModel.productProperty().get();
        long pgeySurcharge = pgeyViewModel.surchargeCentsProperty().get();
        if (pgeyP != null) {
            pgeyPriceBox.getChildren().add(pgeyPriceRow("Precio base", HepyMoney.hepyFormat(pgeyP.hepyBasePriceCents()), false));
        }
        pgeyPriceBox.getChildren().add(pgeyPriceRow("Recargo por materiales", HepyMoney.hepySurcharge(pgeySurcharge), false));
        if (pgeyP != null) {
            pgeyPriceBox.getChildren().add(pgeyPriceRow("Total por unidad", HepyMoney.hepyFormat(pgeyViewModel.totalCentsProperty().get()), true));
        } else {
            Label pgeyNote = new Label("Para ver el precio final, elige un producto del catálogo y pulsa “Personalizar en 3D”.");
            pgeyNote.getStyleClass().addAll("body-s", "ink-subtle");
            pgeyNote.setWrapText(true);
            pgeyNote.setMinHeight(Region.USE_PREF_SIZE);
            pgeyPriceBox.getChildren().add(pgeyNote);
        }
    }

    private static HBox pgeyPriceRow(String pgeyLabel, String pgeyValue, boolean pgeyStrong) {
        Label pgeyL = new Label(pgeyLabel);
        pgeyL.getStyleClass().addAll("body-s", pgeyStrong ? "ink" : "ink-muted");
        Region pgeyGrow = new Region();
        HBox.setHgrow(pgeyGrow, Priority.ALWAYS);
        Label pgeyV = new Label(pgeyValue);
        pgeyV.getStyleClass().addAll(pgeyStrong ? "price" : "body-s", "ink");
        HBox pgeyRow = new HBox(8, pgeyL, pgeyGrow, pgeyV);
        pgeyRow.setAlignment(Pos.CENTER_LEFT);
        return pgeyRow;
    }

    private void pgeySave(Runnable pgeyAfter) {
        if (pgeyViewModel.savingProperty().get()) {
            return;
        }
        List<String> pgeyProblems = PgeyDesignRules.pgeyProblems(pgeyViewModel.pgeyDoc());
        if (!pgeyProblems.isEmpty()) {
            PpqhToast.ppqhError(pgeyStage, pgeyProblems.get(0));
            return;
        }
        String pgeyCurrent = pgeyViewModel.nameProperty().get();
        boolean pgeyExisting = pgeyViewModel.designIdProperty().get() != null;
        String pgeyWhy = pgeyAfter == null ? "" : "Para agregarlo al carrito primero se guarda. ";
        PpqhPromptDialog.ppqhShow(pgeyStage, pgeyExisting ? "Guardar nueva versión" : "Guardar diseño",
                pgeyWhy + (pgeyExisting ? "Se guarda como una nueva versión del mismo diseño, con capturas de frente y espalda."
                        : "Lo encontrarás en “Mis diseños”, con capturas de frente y espalda."),
                "Nombre del diseño", pgeyCurrent == null || pgeyCurrent.isBlank() ? "Mi uniforme" : pgeyCurrent, "Guardar",
                pgeyN -> pgeyN.isEmpty() ? "Escribe un nombre." : pgeyN.length() > 80 ? "Máximo 80 caracteres." : null,
                pgeyN -> pgeySaveAs(pgeyN, pgeyAfter));
    }

    private void pgeySaveAs(String pgeyName, Runnable pgeyAfter) {
        if (!pgeyViewModel.pgeyDoc().pgeyRealisticPhotosMatch()) {
            pgeyCapture3d(pgeyPaths -> pgeyStore(pgeyName, pgeyPaths, pgeyAfter));
            return;
        }
        PgeyRealisticView pgeyView = pgeyViewModel.pgeyRealisticView();
        PgeyRealisticPreview.PgeyCrest pgeyCrest = pgeyRealistic.pgeyCrest(pgeyView);
        PpqhAsync.ppqhRun(() -> {
            PgeyArgbImage[] pgeyPx = pgeyRealistic.pgeyRenderPixels(pgeyView, pgeyCrest);
            Path pgeyF = Files.createTempFile("sf-frente", ".png");
            PgeyRealisticDesigner.pgeyWritePng(pgeyPx[0], pgeyF);
            Path pgeyB = null;
            if (pgeyPx[1] != null) {
                pgeyB = Files.createTempFile("sf-espalda", ".png");
                PgeyRealisticDesigner.pgeyWritePng(pgeyPx[1], pgeyB);
            }
            return new Path[]{pgeyF, pgeyB};
        }, pgeyPaths -> pgeyStore(pgeyName, pgeyPaths, pgeyAfter), pgeyE -> PpqhToast.ppqhError(pgeyStage, PpqhErrorMessages.ppqhOf(pgeyE)));
    }

    private static final double PGEY_PHOTO_ASPECT = 0.75;

    private void pgeyCapture3d(java.util.function.Consumer<Path[]> pgeyDone) {
        if (pgeyViewModel.pgeyPersonMatchesDocument() && pgeyViewer.getModel() != null) {
            pgeyShoot3d(pgeyDone);
            return;
        }
        pgeyViewModel.pgeyLoadPerson();
        if (pgeyViewModel.personFallbackProperty().get()) {
            PpqhToast.ppqhError(pgeyStage, "No se pudo cargar la persona 3D con el corte elegido; el diseño no se guardó.");
            return;
        }
        PpqhToast.ppqhInfo(pgeyStage, "Preparando las fotos del diseño en 3D…");
        javafx.beans.value.ChangeListener<Boolean> pgeyWait = new javafx.beans.value.ChangeListener<>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Boolean> pgeyObs, Boolean pgeyWas, Boolean pgeyLoading) {
                if (pgeyLoading) {
                    return;
                }
                pgeyViewModel.personLoadingProperty().removeListener(this);
                javafx.application.Platform.runLater(() -> {
                    if (pgeyViewModel.pgeyPersonMatchesDocument() && pgeyViewer.getModel() != null) {
                        pgeyShoot3d(pgeyDone);
                    } else {
                        PpqhToast.ppqhError(pgeyStage, "No se pudo cargar la persona 3D con el corte elegido; el diseño no se guardó.");
                    }
                });
            }
        };
        pgeyViewModel.personLoadingProperty().addListener(pgeyWait);
    }

    private void pgeyShoot3d(java.util.function.Consumer<Path[]> pgeyDone) {
        boolean pgeyShown = pgeyViewer.isVisible();
        WritableImage[] pgeyShots;
        pgeyViewer.setVisible(true);
        try {
            pgeyShots = pgeyCleanCapture(() -> new WritableImage[]{pgeyPortrait(pgeyViewer.pgeyCaptureView(PgeyViewPreset.FRONT, 1400)),
                    pgeyPortrait(pgeyViewer.pgeyCaptureView(PgeyViewPreset.BACK, 1400))});
        } finally {
            pgeyViewer.setVisible(pgeyShown);
        }
        PpqhAsync.ppqhRun(() -> {
            Path pgeyF = Files.createTempFile("sf-frente", ".png");
            PgeyPng.pgeyWrite(pgeyShots[0], pgeyF);
            Path pgeyB = Files.createTempFile("sf-espalda", ".png");
            PgeyPng.pgeyWrite(pgeyShots[1], pgeyB);
            return new Path[]{pgeyF, pgeyB};
        }, pgeyDone, pgeyE -> PpqhToast.ppqhError(pgeyStage, PpqhErrorMessages.ppqhOf(pgeyE)));
    }

    private static WritableImage pgeyPortrait(WritableImage pgeyImg) {
        int pgeyW = (int) pgeyImg.getWidth();
        int pgeyH = (int) pgeyImg.getHeight();
        int pgeyCw = Math.min(pgeyW, (int) Math.round(pgeyH * PGEY_PHOTO_ASPECT));
        int pgeyCh = Math.min(pgeyH, (int) Math.round(pgeyCw / PGEY_PHOTO_ASPECT));
        return new WritableImage(pgeyImg.getPixelReader(), (pgeyW - pgeyCw) / 2, (pgeyH - pgeyCh) / 2, pgeyCw, pgeyCh);
    }

    private void pgeyStore(String pgeyName, Path[] pgeyPaths, Runnable pgeyAfter) {
        pgeyViewModel.pgeySave(pgeyName, pgeyPaths[0], pgeyPaths[1], pgeySummary -> {
            pgeyDeleteQuietly(pgeyPaths);
            pgeyDraftBanner.setVisible(false);
            pgeyDraftBanner.setManaged(false);
            PpqhToast.ppqhSuccess(pgeyStage, "Diseño guardado (versión " + pgeySummary.pgeyVersion() + ").");
            if (pgeyAfter != null) {
                pgeyAfter.run();
            }
        }, pgeyE -> {
            pgeyDeleteQuietly(pgeyPaths);
            PpqhToast.ppqhError(pgeyStage, PpqhErrorMessages.ppqhOf(pgeyE));
        });
    }

    private static void pgeyDeleteQuietly(Path[] pgeyPaths) {
        for (Path pgeyP : pgeyPaths) {
            if (pgeyP == null) {
                continue;
            }
            try {
                Files.deleteIfExists(pgeyP);
            } catch (java.io.IOException pgeyIgnored) {
            }
        }
    }

    private void pgeyRefreshPanel() {
        if (pgeyRealisticMode.get()) {
            String pgeyKey = pgeyRealistic.pgeyStructureKey();
            if (!pgeyKey.equals(pgeyStructureKey)) {
                pgeyStructureKey = pgeyKey;
                pgeySyncers.clear();
                pgeyPanel.getChildren().clear();
                pgeyRealistic.pgeyBuild(pgeyPanel);
            }
            pgeyRealistic.pgeyRefresh();
            return;
        }
        PgeyDesignDocument pgeyDoc = pgeyViewModel.pgeyDoc();
        String pgeyKey = pgeyTab == PgeyTab.TEXT ? pgeyTab + "|" + pgeyTextStructureKey(pgeyDoc)
                : pgeyTab + "|" + pgeyViewModel.personFallbackProperty().get() + "|" + pgeyViewModel.garmentProperty().get() + "|"
                + pgeyViewModel.selectedIdProperty().get() + "|" + pgeyDoc.pgeyElements().stream().map(pgeyE -> pgeyE.pgeyId() + pgeyE.pgeyArea()).toList()
                + "|" + pgeyViewModel.materialsProperty().get().size() + "|" + pgeyMyLogos.size();
        if (!pgeyKey.equals(pgeyStructureKey)) {
            pgeyStructureKey = pgeyKey;
            pgeySyncers.clear();
            pgeyPanel.getChildren().clear();
            switch (pgeyTab) {
                case DESIGN -> pgeyBuildDesignTab();
                case TEXT -> pgeyBuildTextTab();
                case LOGOS -> pgeyBuildLogosTab();
                case LAYERS -> pgeyBuildLayersTab();
            }
        }
        pgeySyncing = true;
        try {
            pgeySyncers.forEach(Runnable::run);
        } finally {
            pgeySyncing = false;
        }
    }

    private void pgeySync(Runnable pgeyR) {
        pgeySyncers.add(pgeyR);
    }

    private static VBox pgeySection(String pgeyTitle, javafx.scene.Node... pgeyContent) {
        Label pgeyLabel = new Label(pgeyTitle);
        pgeyLabel.getStyleClass().addAll("label-text", "ink-muted");
        VBox pgeyBox = new VBox(10, pgeyLabel);
        pgeyBox.getChildren().addAll(pgeyContent);
        return pgeyBox;
    }

    private static Label pgeyNote(String pgeyText) {
        Label pgeyL = new Label(pgeyText);
        pgeyL.getStyleClass().addAll("body-s", "ink-subtle");
        pgeyL.setWrapText(true);
        return pgeyL;
    }

    private static PgeyZone pgeyFirstZone(HepyGarment pgeyG) {
        for (PgeyZone pgeyZ : PgeyZone.values()) {
            if (pgeyZ.pgeyGarment() == pgeyG) {
                return pgeyZ;
            }
        }
        return PgeyZone.JERSEY_BODY;
    }

    private static Color pgeyColor(String pgeyHex) {
        try {
            return Color.web(pgeyHex);
        } catch (RuntimeException pgeyE) {
            return Color.GRAY;
        }
    }

    private void pgeyBuildDesignTab() {
        HepyGarment pgeyGarment = pgeyViewModel.garmentProperty().get();
        PgeyGarmentPicker pgeyPicker = new PgeyGarmentPicker(PGEY_GARMENTS);
        pgeyPicker.setSelected(pgeyGarment);
        pgeyPicker.selectedProperty().addListener((pgeyObs, pgeyOld, pgeyG) -> {
            if (pgeyG != null && !pgeySyncing) {
                pgeyViewModel.garmentProperty().set(pgeyG);
            }
        });
        pgeyPanel.getChildren().add(pgeySection("PRENDA", pgeyPicker));

        if (!pgeyViewModel.personFallbackProperty().get()) {
            FlowPane pgeyPersons = new FlowPane(6, 6);
            ToggleGroup pgeyPersonGroup = new ToggleGroup();
            for (PgeyPersona pgeyP : PgeyPersona.values()) {
                HepyFilterChip pgeyChip = new HepyFilterChip(pgeyP.pgeyLabel());
                pgeyChip.setToggleGroup(pgeyPersonGroup);
                pgeyChip.setOnAction(pgeyE -> {
                    pgeyChip.setSelected(true);
                    pgeyRealistic.pgeySelectAudience(pgeyP.pgeyKey(), pgeyP.pgeyLabel());
                });
                pgeySync(() -> pgeyChip.setSelected(pgeyViewModel.personaProperty().get() == pgeyP));
                pgeyPersons.getChildren().add(pgeyChip);
            }
            pgeyPanel.getChildren().add(pgeySection("PERSONA", pgeyPersons,
                    pgeyNote("Es la misma persona de la Vista realista: si cambias una, cambia la otra.")));
        }

        pgeyPanel.getChildren().add(pgeyBuildCutSection());

        FlowPane pgeyBodies = new FlowPane(6, 6);
        ToggleGroup pgeyBodyGroup = new ToggleGroup();
        for (PgeyBodyType pgeyType : PgeyBodyType.values()) {
            HepyFilterChip pgeyChip = new HepyFilterChip(pgeyType.pgeyLabel());
            pgeyChip.setToggleGroup(pgeyBodyGroup);
            pgeyChip.setOnAction(pgeyE -> {
                pgeyChip.setSelected(true);
                pgeyViewModel.pgeyEdit("Maniquí · " + pgeyType.pgeyLabel(), pgeyD -> pgeyD.pgeyWithBodyType(pgeyType), null);
            });
            pgeySync(() -> pgeyChip.setSelected(pgeyViewModel.pgeyDoc().pgeyBodyType() == pgeyType));
            pgeyBodies.getChildren().add(pgeyChip);
        }
        if (pgeyViewModel.personFallbackProperty().get()) {
            pgeyPanel.getChildren().add(pgeySection("MANIQUÍ", pgeyBodies));
        }

        VBox pgeyZones = new VBox(4);
        ToggleGroup pgeyZoneGroup = new ToggleGroup();
        for (PgeyZone pgeyZ : PgeyZone.values()) {
            if (pgeyZ.pgeyGarment() != pgeyGarment) {
                continue;
            }
            Rectangle pgeySwatch = new Rectangle(16, 16);
            pgeySwatch.setArcWidth(5);
            pgeySwatch.setArcHeight(5);
            pgeySwatch.getStyleClass().add("sf-zone-swatch");
            ToggleButton pgeyRow = new ToggleButton(pgeyZ.pgeyLabel(), pgeySwatch);
            pgeyRow.getStyleClass().setAll("sf-zone-row");
            pgeyRow.setMaxWidth(Double.MAX_VALUE);
            pgeyRow.setAlignment(Pos.CENTER_LEFT);
            pgeyRow.setGraphicTextGap(10);
            pgeyRow.setToggleGroup(pgeyZoneGroup);
            pgeyRow.setOnAction(pgeyE -> {
                pgeyZone = pgeyZ;
                pgeyRefreshPanel();
            });
            pgeySync(() -> {
                pgeySwatch.setFill(pgeyColor(pgeyViewModel.pgeyDoc().pgeyColor(pgeyZ)));
                pgeyRow.setSelected(pgeyZone == pgeyZ);
            });
            pgeyZones.getChildren().add(pgeyRow);
        }
        PgeyColorSwatchGroup pgeyZoneColor = new PgeyColorSwatchGroup();
        pgeyZoneColor.valueProperty().addListener((pgeyObs, pgeyOld, pgeyC) -> {
            if (!pgeySyncing && pgeyC != null) {
                PgeyZone pgeyZ = pgeyZone;
                pgeyViewModel.pgeyEdit("Color · " + pgeyZ.pgeyLabel(), pgeyD -> pgeyD.pgeyWithColor(pgeyZ, PgeyDesignDefaults.pgeyHex(pgeyC)), null);
            }
        });
        pgeySync(() -> pgeyZoneColor.setValue(pgeyColor(pgeyViewModel.pgeyDoc().pgeyColor(pgeyZone))));
        pgeyPanel.getChildren().add(pgeySection("COLORES", pgeyZones, pgeyZoneColor));

        if (pgeyGarment != HepyGarment.SHOES) {
            pgeyPanel.getChildren().add(pgeyBuildPatternSection(pgeyGarment));
        }

        if (pgeyGarment == HepyGarment.JERSEY || pgeyGarment == HepyGarment.SHORTS) {
            pgeyPanel.getChildren().add(pgeyBuildFabricSection());
        }
        for (PgeyMaterialSlot pgeySlot : PgeyMaterialSlot.pgeyOf(pgeyGarment)) {
            if (!PGEY_FABRIC_SLOTS.contains(pgeySlot)) {
                pgeyPanel.getChildren().add(pgeyBuildMaterialSection(pgeySlot));
            }
        }

        PpqhSfButton pgeyReset = new PpqhSfButton("Restablecer diseño", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM)
                .ppqhWithIcon(Feather.ROTATE_CCW);
        pgeyReset.setOnAction(pgeyE -> PpqhConfirmDialog.ppqhShow(pgeyStage, "¿Restablecer el diseño?",
                "Vuelven los colores y materiales por defecto y se quitan nombres, números y emblemas. "
                        + "Puedes deshacerlo con Ctrl+Z.", "Restablecer", true, () -> {
                    pgeyViewModel.pgeyResetDesign();
                    PpqhToast.ppqhInfo(pgeyStage, "Diseño restablecido. Ctrl+Z para deshacer.");
                }));
        pgeyPanel.getChildren().add(pgeyReset);
    }

    private VBox pgeyBuildCutSection() {
        VBox pgeyRows = new VBox(10);
        pgeyRows.getChildren().add(pgeyCutRow("Manga", PgeyGarmentCut.PgeySleeve.values(), PgeyGarmentCut.PgeySleeve::pgeyLabel,
                pgeyS -> pgeyViewModel.pgeyDoc().pgeyCut().pgeySleeve() == pgeyS, (pgeyC, pgeyS) -> pgeyC.pgeyWithSleeve(pgeyS), "Manga "));
        pgeyRows.getChildren().add(pgeyCutRow("Short", PgeyGarmentCut.PgeyShortsLength.values(), PgeyGarmentCut.PgeyShortsLength::pgeyLabel,
                pgeyS -> pgeyViewModel.pgeyDoc().pgeyCut().pgeyShorts() == pgeyS, (pgeyC, pgeyS) -> pgeyC.pgeyWithShorts(pgeyS), "Short "));
        pgeyRows.getChildren().add(pgeyCutRow("Medias", PgeyGarmentCut.PgeySocksLength.values(), PgeyGarmentCut.PgeySocksLength::pgeyLabel,
                pgeyS -> pgeyViewModel.pgeyDoc().pgeyCut().pgeySocks() == pgeyS, (pgeyC, pgeyS) -> pgeyC.pgeyWithSocks(pgeyS), "Medias "));
        Label pgeyDetail = pgeyNote("");
        pgeySync(() -> {
            PgeyGarmentCut pgeyC = pgeyViewModel.pgeyDoc().pgeyCut();
            pgeyDetail.setText("Short " + pgeyC.pgeyShorts().pgeyDetail() + " · medias " + pgeyC.pgeySocks().pgeyDetail() + ".");
        });
        String pgeyFallback = pgeyViewModel.personFallbackProperty().get()
                ? "El maniquí de respaldo se ve siempre con manga corta y los largos de serie; el corte se guarda igual. "
                : "";
        return pgeySection("CORTE", pgeyRows, pgeyDetail, pgeyNote(pgeyFallback + "No cambia el precio. La Vista realista solo tiene fotos "
                + "con el corte y la tela de serie: si eliges otro corte u otra tela, las fotos del diseño (las que "
                + "recibe el taller) se toman de la Vista 3D."));
    }

    private <T> HBox pgeyCutRow(String pgeyLabel, T[] pgeyOptions, java.util.function.Function<T, String> pgeyName,
                            java.util.function.Predicate<T> pgeySelected,
                            java.util.function.BiFunction<PgeyGarmentCut, T, PgeyGarmentCut> pgeyApply, String pgeyUndoLabel) {
        Label pgeyTitle = new Label(pgeyLabel);
        pgeyTitle.getStyleClass().addAll("body-s", "ink-muted");
        pgeyTitle.setMinWidth(56);
        FlowPane pgeyChips = new FlowPane(6, 6);
        ToggleGroup pgeyGroup = new ToggleGroup();
        for (T pgeyOption : pgeyOptions) {
            HepyFilterChip pgeyChip = new HepyFilterChip(pgeyName.apply(pgeyOption));
            pgeyChip.setToggleGroup(pgeyGroup);
            pgeyChip.setOnAction(pgeyE -> {
                pgeyChip.setSelected(true);
                if (!pgeySelected.test(pgeyOption)) {
                    pgeyViewModel.pgeyEdit(pgeyUndoLabel + pgeyName.apply(pgeyOption).toLowerCase(java.util.Locale.ROOT),
                            pgeyD -> pgeyD.pgeyWithCut(pgeyApply.apply(pgeyD.pgeyCut(), pgeyOption)), null);
                }
            });
            pgeySync(() -> pgeyChip.setSelected(pgeySelected.test(pgeyOption)));
            pgeyChips.getChildren().add(pgeyChip);
        }
        HBox.setHgrow(pgeyChips, Priority.ALWAYS);
        HBox pgeyRow = new HBox(8, pgeyTitle, pgeyChips);
        pgeyRow.setAlignment(Pos.CENTER_LEFT);
        return pgeyRow;
    }

    private VBox pgeyBuildPatternSection(HepyGarment pgeyGarment) {
        FlowPane pgeyChips = new FlowPane(6, 6);
        ToggleGroup pgeyGroup = new ToggleGroup();
        for (PgeyPatternType pgeyType : PgeyPatternType.values()) {
            HepyFilterChip pgeyChip = new HepyFilterChip(pgeyType.pgeyLabel());
            pgeyChip.setToggleGroup(pgeyGroup);
            pgeyChip.setOnAction(pgeyE -> {
                pgeyChip.setSelected(true);
                pgeyViewModel.pgeyEdit("Patrón · " + pgeyType.pgeyLabel(), pgeyD -> pgeyD.pgeyWithPattern(pgeyGarment, pgeyD.pgeyPattern(pgeyGarment).pgeyWithType(pgeyType)), null);
            });
            pgeySync(() -> pgeyChip.setSelected(pgeyViewModel.pgeyDoc().pgeyPattern(pgeyGarment).pgeyType() == pgeyType));
            pgeyChips.getChildren().add(pgeyChip);
        }

        PgeyColorSwatchGroup pgeyPatternColor = new PgeyColorSwatchGroup();
        pgeyPatternColor.valueProperty().addListener((pgeyObs, pgeyOld, pgeyC) -> {
            if (!pgeySyncing && pgeyC != null) {
                pgeyViewModel.pgeyEdit("Color del patrón", pgeyD -> pgeyD.pgeyWithPattern(pgeyGarment, pgeyD.pgeyPattern(pgeyGarment).pgeyWithColor(PgeyDesignDefaults.pgeyHex(pgeyC))), null);
            }
        });
        Label pgeyWidthLabel = new Label();
        pgeyWidthLabel.getStyleClass().addAll("body-s", "ink-muted");
        Slider pgeyWidth = new Slider(PgeyPatternSpec.PGEY_MIN_WIDTH * 100, PgeyPatternSpec.PGEY_MAX_WIDTH * 100, 4);
        pgeyWidth.valueProperty().addListener((pgeyObs, pgeyOld, pgeyV) -> {
            if (!pgeySyncing) {
                double pgeyM = pgeyV.doubleValue() / 100;
                pgeyViewModel.pgeyEdit("Ancho del patrón", pgeyD -> pgeyD.pgeyWithPattern(pgeyGarment, pgeyD.pgeyPattern(pgeyGarment).pgeyWithWidth(pgeyM)), "pattern-width:" + pgeyGarment);
            }
        });
        pgeyWidth.setOnMouseReleased(pgeyE -> pgeyViewModel.pgeySeal());
        VBox pgeyWidthBox = new VBox(4, pgeyWidthLabel, pgeyWidth);
        VBox pgeyOptions = new VBox(10, pgeyNote("Segundo color"), pgeyPatternColor, pgeyWidthBox);
        pgeySync(() -> {
            PgeyPatternSpec pgeySpec = pgeyViewModel.pgeyDoc().pgeyPattern(pgeyGarment);
            boolean pgeyOn = pgeySpec.pgeyType() != PgeyPatternType.LISO;
            pgeyOptions.setVisible(pgeyOn);
            pgeyOptions.setManaged(pgeyOn);
            boolean pgeyHasWidth = pgeySpec.pgeyType() != PgeyPatternType.DEGRADADO;
            pgeyWidthBox.setVisible(pgeyHasWidth);
            pgeyWidthBox.setManaged(pgeyHasWidth);
            pgeyPatternColor.setValue(pgeyColor(pgeySpec.pgeyColor()));
            pgeyWidth.setValue(pgeySpec.pgeyWidth() * 100);
            pgeyWidthLabel.setText(String.format("Ancho: %.1f cm", pgeySpec.pgeyWidth() * 100));
        });
        return pgeySection("PATRÓN", pgeyChips, pgeyOptions);
    }

    static final List<String> PGEY_FABRICS = List.of("DRY_FIT", "POLIESTER_LISO", "ALGODON", "MALLA");
    static final List<PgeyMaterialSlot> PGEY_FABRIC_SLOTS = List.of(PgeyMaterialSlot.JERSEY, PgeyMaterialSlot.SHORTS);

    private VBox pgeyBuildFabricSection() {
        Map<String, HepyMaterial> pgeyAll = pgeyViewModel.materialsProperty().get();
        List<HepyMaterial> pgeyList = PGEY_FABRICS.stream().map(pgeyAll::get).filter(Objects::nonNull)
                .map(pgeyM -> pgeyM.hepyCode().equals(PgeyMaterialSlot.JERSEY.pgeyDefaultCode()) ? pgeyIncluded(pgeyM) : pgeyM).toList();
        if (pgeyList.isEmpty()) {
            return pgeySection("TELA", pgeyNote("Cargando telas…"));
        }
        PgeyMaterialPicker pgeyPicker = new PgeyMaterialPicker();
        pgeySyncing = true;
        try {
            pgeyPicker.setMaterials(pgeyList);
        } finally {
            pgeySyncing = false;
        }
        pgeyPicker.selectedProperty().addListener((pgeyObs, pgeyOld, pgeyM) -> {
            if (!pgeySyncing && pgeyM != null) {
                pgeyViewModel.pgeyEdit("Tela · " + pgeyM.hepyName(), pgeyD -> {
                    PgeyDesignDocument pgeyOut = pgeyD;
                    for (PgeyMaterialSlot pgeySlot : PGEY_FABRIC_SLOTS) {
                        pgeyOut = pgeyOut.pgeyWithMaterial(pgeySlot, pgeyM.hepyCode());
                    }
                    return pgeyOut;
                }, null);
            }
        });
        Label pgeyOther = pgeyNote("");
        pgeySync(() -> {
            PgeyDesignDocument pgeyDoc = pgeyViewModel.pgeyDoc();
            String pgeyJersey = pgeyDoc.pgeyMaterial(PgeyMaterialSlot.JERSEY);
            String pgeyShorts = pgeyDoc.pgeyMaterial(PgeyMaterialSlot.SHORTS);
            boolean pgeySame = Objects.equals(pgeyJersey, pgeyShorts) && PGEY_FABRICS.contains(pgeyJersey);
            pgeyPicker.setSelected(pgeySame ? pgeyAll.get(pgeyJersey) : null);
            pgeyPicker.sampleColorProperty().set(pgeyColor(pgeyDoc.pgeyColor(PgeyZone.JERSEY_BODY)));
            pgeyOther.setText(pgeySame ? "" : "Tela actual: camiseta " + pgeyMaterialName(pgeyJersey) + ", short " + pgeyMaterialName(pgeyShorts)
                    + ". Elige una para usarla en las dos prendas.");
            pgeyOther.setVisible(!pgeySame);
            pgeyOther.setManaged(!pgeySame);
        });
        return pgeySection("TELA", pgeyNote("Se usa en la camiseta y el short. Cambia el relieve y el brillo en la Vista 3D. "
                + "Las que dicen “Incluido” no cambian el precio; las demás suman su recargo por prenda."), pgeyPicker, pgeyOther);
    }

    private String pgeyMaterialName(String pgeyCode) {
        HepyMaterial pgeyM = pgeyViewModel.materialsProperty().get().get(pgeyCode);
        return pgeyM == null ? pgeyCode : pgeyM.hepyName();
    }

    private VBox pgeyBuildMaterialSection(PgeyMaterialSlot pgeySlot) {
        List<HepyMaterial> pgeyList = pgeyViewModel.materialsProperty().get().values().stream()
                .filter(pgeySlot::pgeyAccepts)
                .map(pgeyM -> pgeyM.hepyCode().equals(pgeySlot.pgeyDefaultCode()) ? pgeyIncluded(pgeyM) : pgeyM)
                .sorted(java.util.Comparator.comparingLong(HepyMaterial::hepySurchargeCents).thenComparing(HepyMaterial::hepyName))
                .toList();
        if (pgeyList.isEmpty()) {
            return pgeySection(pgeySlot.pgeyLabel().toUpperCase(java.util.Locale.ROOT), pgeyNote("Cargando materiales…"));
        }
        PgeyMaterialPicker pgeyPicker = new PgeyMaterialPicker();
        pgeySyncing = true;
        try {
            pgeyPicker.setMaterials(pgeyList);
        } finally {
            pgeySyncing = false;
        }
        pgeyPicker.selectedProperty().addListener((pgeyObs, pgeyOld, pgeyM) -> {
            if (!pgeySyncing && pgeyM != null) {
                pgeyViewModel.pgeyEdit(pgeySlot.pgeyLabel() + " · " + pgeyM.hepyName(), pgeyD -> pgeyD.pgeyWithMaterial(pgeySlot, pgeyM.hepyCode()), null);
            }
        });
        pgeySync(() -> {
            pgeyPicker.setSelected(pgeyViewModel.materialsProperty().get().get(pgeyViewModel.pgeyDoc().pgeyMaterial(pgeySlot)));
            pgeyPicker.sampleColorProperty().set(pgeyColor(pgeyViewModel.pgeyDoc().pgeyColor(pgeySlot.pgeyZones().get(0))));
        });
        return pgeySection(pgeySlot.pgeyLabel().toUpperCase(java.util.Locale.ROOT),
                pgeyNote("Cada material cambia el relieve y el brillo en el 3D. Los que dicen “Incluido” no cambian el "
                        + "precio; los demás suman su recargo."), pgeyPicker);
    }

    private static HepyMaterial pgeyIncluded(HepyMaterial pgeyM) {
        return new HepyMaterial(pgeyM.hepyId(), pgeyM.hepyCode(), pgeyM.hepyName(), pgeyM.hepyComposition(), pgeyM.hepyUsage(), pgeyM.hepyScope(), pgeyM.hepySuitableFor(), 0,
                pgeyM.hepyRoughness(), pgeyM.hepySheen(), pgeyM.hepyMetalness(), pgeyM.hepyPreviewPattern());
    }

    private final Set<PgeyTextSlot> pgeyOpenText = java.util.EnumSet.noneOf(PgeyTextSlot.class);

    private String pgeyTextStructureKey(PgeyDesignDocument pgeyDoc) {
        StringBuilder pgeySb = new StringBuilder();
        for (PgeyTextSlot pgeyS : PgeyTextSlot.values()) {
            PgeyDesignElement pgeyE = pgeyS.pgeyFind(pgeyDoc);
            pgeySb.append(pgeyS).append('=').append(pgeyE == null ? "-" : pgeyE.pgeyId()).append(pgeyOpenText.contains(pgeyS) ? "+" : "").append(';');
        }
        return pgeySb.append(pgeyDoc.pgeyElements().stream().filter(pgeyE -> pgeyE.pgeyType().pgeyIsText() && PgeyTextSlot.pgeyOf(pgeyE) == null).count())
                .toString();
    }

    private void pgeyBuildTextTab() {
        PgeyDesignDocument pgeyDoc = pgeyViewModel.pgeyDoc();
        VBox pgeyCards = new VBox(8);
        for (PgeyTextSlot pgeySlot : PgeyTextSlot.values()) {
            pgeyCards.getChildren().add(pgeyTextCard(pgeySlot, pgeySlot.pgeyFind(pgeyDoc)));
        }
        pgeyPanel.getChildren().add(pgeySection("NOMBRE Y NÚMEROS", pgeyCards,
                pgeyNote("Arrastra el texto sobre la persona para moverlo dentro de su zona.")));
        long pgeyOthers = pgeyDoc.pgeyElements().stream().filter(pgeyE -> pgeyE.pgeyType().pgeyIsText() && PgeyTextSlot.pgeyOf(pgeyE) == null).count();
        if (pgeyOthers > 0) {
            pgeyPanel.getChildren().add(pgeyNote(pgeyOthers == 1 ? "Hay 1 texto más en otra zona: edítalo en Capas."
                    : "Hay " + pgeyOthers + " textos más en otras zonas: edítalos en Capas."));
        }
    }

    private VBox pgeyTextCard(PgeyTextSlot pgeySlot, PgeyDesignElement pgeyElement) {
        VBox pgeyCard = new VBox(4);
        pgeyCard.getStyleClass().add("sf-text-card");
        boolean pgeyOpen = pgeyElement != null && pgeyOpenText.contains(pgeySlot);
        PpqhSfButton pgeyHeader = new PpqhSfButton(pgeySlot.pgeyLabel(), PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM)
                .ppqhWithIcon(pgeyElement == null ? Feather.PLUS : pgeyOpen ? Feather.CHEVRON_UP : Feather.CHEVRON_DOWN);
        pgeyHeader.getStyleClass().add("sf-text-card-head");
        pgeyHeader.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pgeyHeader, Priority.ALWAYS);
        pgeyHeader.setAccessibleHelp(pgeyElement == null ? "Agregar" : pgeyOpen ? "Cerrar ajustes" : "Abrir ajustes");
        pgeyHeader.setOnAction(pgeyE -> {
            PgeyDesignElement pgeyCur = pgeySlot.pgeyFind(pgeyViewModel.pgeyDoc());
            if (pgeyCur == null) {
                if (pgeyViewModel.pgeyDoc().pgeyElements().size() >= PgeyDesignRules.PGEY_MAX_ELEMENTS) {
                    PpqhToast.ppqhError(pgeyStage, "Máximo " + PgeyDesignRules.PGEY_MAX_ELEMENTS + " elementos por diseño.");
                    return;
                }
                pgeyOpenText.add(pgeySlot);
                pgeyViewModel.pgeyAdd(pgeySlot.pgeyCreate(pgeyViewModel.pgeyDoc()));
            } else if (pgeyOpenText.remove(pgeySlot)) {
                pgeyRefreshPanel();
            } else {
                pgeyOpenText.add(pgeySlot);
                pgeyViewModel.selectedIdProperty().set(pgeyCur.pgeyId());
                pgeyRefreshPanel();
            }
        });
        HBox pgeyHead = new HBox(4, pgeyHeader);
        pgeyHead.setAlignment(Pos.CENTER_LEFT);
        pgeyCard.getChildren().add(pgeyHead);
        if (pgeyElement == null) {
            return pgeyCard;
        }
        String pgeyId = pgeyElement.pgeyId();
        PpqhSfButton pgeyRemove = PpqhSfButton.ppqhIcon(Feather.TRASH_2, "Quitar " + pgeySlot.pgeyLabel().toLowerCase(java.util.Locale.ROOT));
        pgeyRemove.setOnAction(pgeyE -> {
            pgeyOpenText.remove(pgeySlot);
            pgeyViewModel.pgeyRemove(pgeyId);
        });
        pgeyHead.getChildren().add(pgeyRemove);
        pgeySync(() -> pgeyCard.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"),
                pgeyId.equals(pgeyViewModel.selectedIdProperty().get())));
        if (pgeyOpen) {
            VBox pgeyEditor = new VBox(16);
            pgeyEditor.getStyleClass().add("sf-text-card-editor");
            pgeyElementControls(pgeyElement, pgeyEditor, false);
            pgeyCard.getChildren().add(pgeyEditor);
            pgeyEditor.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, pgeyX -> pgeySelect(pgeyId));
            pgeyEditor.lookupAll(".text-field").forEach(pgeyN -> pgeyN.focusedProperty().addListener((pgeyObs, pgeyWas, pgeyNow) -> {
                if (pgeyNow) {
                    pgeySelect(pgeyId);
                }
            }));
        }
        return pgeyCard;
    }

    private void pgeySelect(String pgeyId) {
        if (!pgeyId.equals(pgeyViewModel.selectedIdProperty().get())) {
            pgeyViewModel.selectedIdProperty().set(pgeyId);
        }
    }

    private static String pgeyContrast(String pgeyHex) {
        Color pgeyC = pgeyColor(pgeyHex);
        double pgeyLum = 0.2126 * pgeyC.getRed() + 0.7152 * pgeyC.getGreen() + 0.0722 * pgeyC.getBlue();
        return pgeyLum > 0.55 ? "#0B0E13" : "#FFFFFF";
    }

    private void pgeyBuildLogosTab() {
        PgeyDesignDocument pgeyDoc = pgeyViewModel.pgeyDoc();
        String pgeyBody = pgeyDoc.pgeyColor(PgeyZone.JERSEY_BODY);
        String pgeyCollar = pgeyDoc.pgeyColor(PgeyZone.JERSEY_COLLAR);
        String pgeyPrimary = Objects.equals(pgeyCollar, pgeyBody) ? pgeyContrast(pgeyBody) : pgeyCollar;
        String pgeyAccent = Objects.equals(pgeyCollar, pgeyBody) ? pgeyBody : pgeyContrast(pgeyCollar);

        FlowPane pgeyEmblems = new FlowPane(8, 8);
        for (String pgeyKey : PgeyEmblemCatalog.PGEY_EMBLEMS.keySet()) {
            PpqhSfButton pgeyB = pgeyImageButton(pgeyImages.pgeyEmblem(pgeyKey, null, null), PgeyEmblemCatalog.pgeyLabel(pgeyKey));
            pgeyB.setOnAction(pgeyE -> pgeyAddGraphic(PgeyDesignDefaults.pgeyEmblem(pgeyKey, pgeyPrimary, pgeyAccent)));
            pgeyEmblems.getChildren().add(pgeyB);
        }
        pgeyPanel.getChildren().add(pgeySection("EMBLEMAS", pgeyEmblems,
                pgeyNote("Se agregan en el pecho con los colores de tu camiseta; luego puedes cambiarlos.")));

        CheckBox pgeyRights = new CheckBox("Declaro que este logo es mío o tengo permiso para usarlo. No es una marca de terceros.");
        pgeyRights.setWrapText(true);
        PpqhSfButton pgeyUpload = new PpqhSfButton("Subir logo (PNG o SVG)", PpqhSfButton.PpqhVariant.PRIMARY, PpqhSfButton.PpqhSize.SM)
                .ppqhWithIcon(Feather.UPLOAD);
        pgeyUpload.disableProperty().bind(pgeyRights.selectedProperty().not());
        pgeyUpload.setOnAction(pgeyE -> pgeyUploadLogo());
        FlowPane pgeyMine = new FlowPane(8, 8);
        for (PgeyDesignAsset pgeyA : pgeyMyLogos) {
            Image pgeyImg = pgeyImages.pgeyLogo(pgeyA.pgeyPath());
            PpqhSfButton pgeyB = pgeyImageButton(pgeyImg, pgeyA.pgeyOriginalName());
            pgeyB.setOnAction(pgeyE -> pgeyAddGraphic(PgeyDesignDefaults.pgeyLogo(pgeyA.pgeyId(), pgeyA.pgeyPath())));
            pgeyMine.getChildren().add(pgeyB);
        }
        VBox pgeyLogos = pgeySection("TU LOGO", pgeyNote("Máximo 5 MB. Se guarda en tu cuenta para otros diseños."), pgeyRights, pgeyUpload);
        if (!pgeyMyLogos.isEmpty()) {
            pgeyLogos.getChildren().addAll(pgeyNote("Tus logos"), pgeyMine);
        }
        pgeyPanel.getChildren().add(pgeyLogos);

        PgeyDesignElement pgeySelected = pgeyViewModel.pgeySelected().orElse(null);
        if (pgeySelected != null && !pgeySelected.pgeyType().pgeyIsText()) {
            pgeyPanel.getChildren().add(pgeyBuildInspector(pgeySelected));
        }
    }

    private PpqhSfButton pgeyImageButton(Image pgeyImage, String pgeyLabel) {
        ImageView pgeyView = new ImageView(pgeyImage);
        pgeyView.setFitWidth(52);
        pgeyView.setFitHeight(52);
        pgeyView.setPreserveRatio(true);
        pgeyView.setSmooth(true);
        PpqhSfButton pgeyB = new PpqhSfButton("", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
        pgeyB.setGraphic(pgeyView);
        pgeyB.getStyleClass().add("sf-emblem-btn");
        pgeyB.setAccessibleText(pgeyLabel);
        pgeyB.setTooltip(new Tooltip(pgeyLabel));
        return pgeyB;
    }

    private void pgeyAddGraphic(PgeyDesignElement pgeyEl) {
        if (pgeyViewModel.pgeyDoc().pgeyElements().size() >= PgeyDesignRules.PGEY_MAX_ELEMENTS) {
            PpqhToast.ppqhError(pgeyStage, "Máximo " + PgeyDesignRules.PGEY_MAX_ELEMENTS + " elementos por diseño.");
            return;
        }
        pgeyViewModel.pgeyAdd(pgeyEl);
    }

    private void pgeyUploadLogo() {
        FileChooser pgeyChooser = new FileChooser();
        pgeyChooser.setTitle("Elegir logo");
        pgeyChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG o SVG", "*.png", "*.svg"));
        File pgeyFile = pgeyChooser.showOpenDialog(pgeyStage.getScene().getWindow());
        if (pgeyFile == null) {
            return;
        }
        PpqhAsync.ppqhRun(() -> pgeyContext.ppqhDesignService().pgeyUploadLogo(pgeyFile.toPath(), true), pgeyAsset -> {
            PpqhToast.ppqhSuccess(pgeyStage, "Logo subido.");
            pgeyAddGraphic(PgeyDesignDefaults.pgeyLogo(pgeyAsset.pgeyId(), pgeyAsset.pgeyPath()));
            pgeyLoadMyLogos();
        }, pgeyE -> PpqhToast.ppqhError(pgeyStage, PpqhErrorMessages.ppqhOf(pgeyE)));
    }

    private void pgeyLoadMyLogos() {
        PpqhAsync.ppqhRun(pgeyContext.ppqhDesignService()::pgeyMyLogos, pgeyList -> {
            pgeyMyLogos = pgeyList;
            pgeyRefreshPanel();
        }, pgeyE -> { });
    }

    private void pgeyBuildLayersTab() {
        List<PgeyDesignElement> pgeyElements = pgeyViewModel.pgeyDoc().pgeyElements();
        if (pgeyElements.isEmpty()) {
            pgeyPanel.getChildren().add(new PpqhEmptyState(Feather.LAYERS, "Sin capas todavía",
                    "Agrega un nombre, un número o un emblema desde las pestañas Texto y Logos."));
            return;
        }
        VBox pgeyList = new VBox(4);
        for (int pgeyI = pgeyElements.size() - 1; pgeyI >= 0; pgeyI--) {
            pgeyList.getChildren().add(pgeyLayerRow(pgeyElements.get(pgeyI), pgeyI, pgeyElements.size()));
        }
        pgeyPanel.getChildren().add(pgeySection("CAPAS", pgeyNote("La de arriba se dibuja encima de las demás."), pgeyList));
        pgeyViewModel.pgeySelected().ifPresent(pgeyE -> pgeyPanel.getChildren().add(pgeyBuildInspector(pgeyE)));
    }

    private HBox pgeyLayerRow(PgeyDesignElement pgeyE, int pgeyIndex, int pgeyCount) {
        String pgeyId = pgeyE.pgeyId();
        PpqhSfButton pgeyEye = PpqhSfButton.ppqhIcon(pgeyE.pgeyVisible() ? Feather.EYE : Feather.EYE_OFF, pgeyE.pgeyVisible() ? "Ocultar" : "Mostrar");
        pgeyEye.setOnAction(pgeyX -> pgeyViewModel.pgeyEditElement(pgeyId, pgeyE.pgeyVisible() ? "Ocultar capa" : "Mostrar capa",
                pgeyEl -> pgeyEl.pgeyWithVisible(!pgeyEl.pgeyVisible()), null));
        PpqhSfButton pgeyLock = PpqhSfButton.ppqhIcon(pgeyE.pgeyLocked() ? Feather.LOCK : Feather.UNLOCK, pgeyE.pgeyLocked() ? "Desbloquear" : "Bloquear");
        pgeyLock.setOnAction(pgeyX -> pgeyViewModel.pgeyEditElement(pgeyId, pgeyE.pgeyLocked() ? "Desbloquear capa" : "Bloquear capa",
                pgeyEl -> pgeyEl.pgeyWithLocked(!pgeyEl.pgeyLocked()), null));
        Label pgeyName = new Label();
        pgeyName.getStyleClass().addAll("body-s", "ink");
        pgeyName.setWrapText(true);
        Label pgeyArea = new Label(pgeyE.pgeyArea().pgeyLabel());
        pgeyArea.getStyleClass().addAll("body-s", "ink-subtle");
        VBox pgeyText = new VBox(0, pgeyName, pgeyArea);
        pgeyText.setMinWidth(0);
        HBox.setHgrow(pgeyText, Priority.ALWAYS);
        PpqhSfButton pgeyUp = PpqhSfButton.ppqhIcon(Feather.ARROW_UP, "Subir capa");
        pgeyUp.setDisable(pgeyIndex == pgeyCount - 1);
        pgeyUp.setOnAction(pgeyX -> pgeyViewModel.pgeyEdit("Subir capa", pgeyD -> pgeyD.pgeyMoveLayer(pgeyId, 1), null));
        PpqhSfButton pgeyDown = PpqhSfButton.ppqhIcon(Feather.ARROW_DOWN, "Bajar capa");
        pgeyDown.setDisable(pgeyIndex == 0);
        pgeyDown.setOnAction(pgeyX -> pgeyViewModel.pgeyEdit("Bajar capa", pgeyD -> pgeyD.pgeyMoveLayer(pgeyId, -1), null));
        PpqhSfButton pgeyDelete = PpqhSfButton.ppqhIcon(Feather.TRASH_2, "Eliminar");
        pgeyDelete.setOnAction(pgeyX -> pgeyViewModel.pgeyRemove(pgeyId));

        HBox pgeyRow = new HBox(4, pgeyEye, pgeyLock, pgeyText, pgeyUp, pgeyDown, pgeyDelete);
        pgeyRow.setAlignment(Pos.CENTER_LEFT);
        pgeyRow.getStyleClass().add("sf-layer-row");
        pgeyRow.setOnMouseClicked(pgeyX -> pgeyViewModel.selectedIdProperty().set(pgeyId));
        pgeySync(() -> {
            PgeyDesignElement pgeyCur = pgeyViewModel.pgeyDoc().pgeyElement(pgeyId).orElse(pgeyE);
            pgeyName.setText(pgeyCur.pgeyDisplayName());
            pgeyRow.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"),
                    pgeyId.equals(pgeyViewModel.selectedIdProperty().get()));
            pgeyRow.setOpacity(pgeyCur.pgeyVisible() ? 1 : 0.55);
        });
        return pgeyRow;
    }

    private VBox pgeyBuildInspector(PgeyDesignElement pgeyE) {
        String pgeyId = pgeyE.pgeyId();
        VBox pgeyBox = new VBox(16);
        pgeyBox.getStyleClass().add("sf-inspector");

        Label pgeyTitle = new Label();
        pgeyTitle.getStyleClass().addAll("title-s", "ink");
        pgeyTitle.setWrapText(true);
        pgeyTitle.setMinWidth(0);
        HBox.setHgrow(pgeyTitle, Priority.ALWAYS);
        pgeyTitle.setMaxWidth(Double.MAX_VALUE);
        PpqhSfButton pgeyClose = PpqhSfButton.ppqhIcon(Feather.X, "Deseleccionar");
        pgeyClose.setOnAction(pgeyX -> pgeyViewModel.selectedIdProperty().set(null));
        PpqhSfButton pgeyDelete = PpqhSfButton.ppqhIcon(Feather.TRASH_2, "Eliminar (Supr)");
        pgeyDelete.setOnAction(pgeyX -> pgeyViewModel.pgeyRemove(pgeyId));
        HBox pgeyHead = new HBox(4, pgeyTitle, pgeyDelete, pgeyClose);
        pgeyHead.setAlignment(Pos.CENTER_LEFT);
        pgeyBox.getChildren().add(pgeyHead);
        pgeySync(() -> pgeyViewModel.pgeyDoc().pgeyElement(pgeyId).ifPresent(pgeyCur -> pgeyTitle.setText(pgeyCur.pgeyDisplayName())));
        pgeyElementControls(pgeyE, pgeyBox, true);
        return pgeyBox;
    }

    private void pgeyElementControls(PgeyDesignElement pgeyE, VBox pgeyBox, boolean pgeyWithArea) {
        String pgeyId = pgeyE.pgeyId();
        if (pgeyE.pgeyType().pgeyIsText()) {
            boolean pgeyIsName = pgeyE.pgeyType() == PgeyElementType.NAME;
            PpqhSfTextField pgeyField = new PpqhSfTextField(pgeyIsName ? "Nombre (máx. 12)" : "Número (0–99)");
            pgeyField.getInput().setTextFormatter(new TextFormatter<String>(pgeyChange -> {
                String pgeyNext = pgeyChange.getControlNewText();
                if (pgeyIsName) {
                    pgeyChange.setText(pgeyChange.getText().toUpperCase(java.util.Locale.forLanguageTag("es-PE")));
                    return pgeyNext.length() <= PgeyDesignRules.PGEY_NAME_MAX ? pgeyChange : null;
                }
                return pgeyNext.matches("[0-9]{0,2}") ? pgeyChange : null;
            }));
            pgeyField.textProperty().addListener((pgeyObs, pgeyOld, pgeyT) -> {
                if (pgeySyncing) {
                    return;
                }
                String pgeyClean = pgeyIsName ? PgeyDesignRules.pgeyCleanName(pgeyT) : PgeyDesignRules.pgeyCleanNumber(pgeyT);
                if (pgeyClean == null) {
                    pgeyField.setError("Escribe un número de 0 a 99.");
                    return;
                }
                pgeyField.setError(pgeyIsName && !pgeyClean.equals(pgeyT.toUpperCase(java.util.Locale.forLanguageTag("es-PE")))
                        ? "Solo letras, espacios, punto, guion y apóstrofo." : "");
                pgeyViewModel.pgeyEditElement(pgeyId, pgeyIsName ? "Nombre" : "Número", pgeyEl -> pgeyEl.pgeyWithText(pgeyClean), "text:" + pgeyId);
            });
            pgeySync(() -> pgeyViewModel.pgeyDoc().pgeyElement(pgeyId).ifPresent(pgeyCur -> {
                String pgeyShown = pgeyIsName ? PgeyDesignRules.pgeyCleanName(pgeyField.getText()) : PgeyDesignRules.pgeyCleanNumber(pgeyField.getText());
                if (!Objects.equals(pgeyShown, pgeyCur.pgeyText())) {
                    pgeyField.setText(pgeyCur.pgeyText());
                }
            }));

            ComboBox<String> pgeyFont = new ComboBox<>();
            pgeyFont.getItems().setAll(PgeyDesignFonts.PGEY_LABELS.keySet());
            pgeyFont.setConverter(new StringConverter<>() {
                @Override
                public String toString(String pgeyKey) {
                    return pgeyKey == null ? "" : PgeyDesignFonts.PGEY_LABELS.getOrDefault(pgeyKey, pgeyKey);
                }

                @Override
                public String fromString(String pgeyS) {
                    return pgeyS;
                }
            });
            pgeyFont.setMaxWidth(Double.MAX_VALUE);
            pgeyFont.valueProperty().addListener((pgeyObs, pgeyOld, pgeyK) -> {
                if (!pgeySyncing && pgeyK != null) {
                    pgeyViewModel.pgeyEditElement(pgeyId, "Tipografía", pgeyEl -> pgeyEl.pgeyWithFont(pgeyK), null);
                }
            });
            pgeySync(() -> pgeyViewModel.pgeyDoc().pgeyElement(pgeyId).ifPresent(pgeyCur -> pgeyFont.setValue(pgeyCur.pgeyFont())));
            pgeyBox.getChildren().addAll(pgeyField, pgeyLabeled("TIPOGRAFÍA", pgeyFont));
        }

        if (pgeyWithArea) {
            pgeyBox.getChildren().add(pgeyLabeled("ZONA", pgeyAreaCombo(pgeyE)));
        }

        if (pgeyE.pgeyType() != PgeyElementType.LOGO) {
            PgeyColorSwatchGroup pgeyFill = pgeyColorGroup(pgeyId, pgeyE.pgeyType().pgeyIsText() ? "Color del texto" : "Color principal",
                    PgeyDesignElement::pgeyColor, (pgeyEl, pgeyHex) -> pgeyEl.pgeyWithColor(pgeyHex));
            pgeyBox.getChildren().add(pgeyLabeled(pgeyE.pgeyType().pgeyIsText() ? "COLOR" : "COLOR PRINCIPAL", pgeyFill));
            PgeyColorSwatchGroup pgeyAccent = pgeyColorGroup(pgeyId, pgeyE.pgeyType().pgeyIsText() ? "Color del contorno" : "Color de acento",
                    PgeyDesignElement::pgeyAccentColor, (pgeyEl, pgeyHex) -> pgeyEl.pgeyWithAccent(pgeyHex));
            if (pgeyE.pgeyType().pgeyIsText()) {
                Slider pgeyOutline = pgeySlider(0, 12, pgeyE.pgeyOutline() * 1000, "outline:" + pgeyId, "Contorno",
                        (pgeyEl, pgeyV) -> pgeyEl.pgeyWithOutline(pgeyV / 1000), pgeyEl -> pgeyEl.pgeyOutline() * 1000, pgeyId);
                Label pgeyOutlineLabel = pgeyValueLabel(pgeyId, pgeyEl -> pgeyEl.pgeyOutline() == 0 ? "Sin contorno"
                        : String.format("Grosor: %.1f mm", pgeyEl.pgeyOutline() * 1000));
                pgeyBox.getChildren().add(pgeyLabeled("CONTORNO", pgeyAccent, pgeyOutlineLabel, pgeyOutline));
            } else {
                pgeyBox.getChildren().add(pgeyLabeled("COLOR DE ACENTO", pgeyAccent));
            }
        }

        Slider pgeySize = pgeySlider(PgeyDesignElement.PGEY_MIN_SCALE * 100, PgeyDesignElement.PGEY_MAX_SCALE * 100, pgeyE.pgeyScale() * 100, "scale:" + pgeyId,
                "Tamaño", (pgeyEl, pgeyV) -> pgeyEl.pgeyWithScale(pgeyV / 100), pgeyEl -> pgeyEl.pgeyScale() * 100, pgeyId);
        Label pgeySizeLabel = pgeyValueLabel(pgeyId, pgeyEl -> String.format("%.0f %%", pgeyEl.pgeyScale() * 100));
        Slider pgeyRotation = pgeySlider(-180, 180, pgeyE.pgeyRotation(), "rotate:" + pgeyId, "Giro",
                (pgeyEl, pgeyV) -> pgeyEl.pgeyWithRotation(pgeyV), PgeyDesignElement::pgeyRotation, pgeyId);
        Label pgeyRotationLabel = pgeyValueLabel(pgeyId, pgeyEl -> String.format("%.0f°", pgeyEl.pgeyRotation()));
        pgeyBox.getChildren().addAll(pgeyLabeled("TAMAÑO", pgeySizeLabel, pgeySize), pgeyLabeled("GIRO", pgeyRotationLabel, pgeyRotation));
    }

    private ComboBox<PgeyArea> pgeyAreaCombo(PgeyDesignElement pgeyE) {
        ComboBox<PgeyArea> pgeyCombo = new ComboBox<>();
        pgeyCombo.getItems().setAll(PgeyArea.values());
        pgeyCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(PgeyArea pgeyA) {
                return pgeyA == null ? "" : pgeyA.pgeyLabel();
            }

            @Override
            public PgeyArea fromString(String pgeyS) {
                return null;
            }
        });
        pgeyCombo.setMaxWidth(Double.MAX_VALUE);
        pgeyCombo.setValue(pgeyE.pgeyArea());
        pgeyCombo.valueProperty().addListener((pgeyObs, pgeyOld, pgeyA) -> {
            if (!pgeySyncing && pgeyA != null && pgeyA != pgeyOld) {
                pgeyViewModel.pgeyEditElement(pgeyE.pgeyId(), "Zona · " + pgeyA.pgeyLabel(), pgeyEl -> pgeyEl.pgeyWithArea(pgeyA).pgeyAt(0.5, 0.5), null);
                pgeyViewModel.garmentProperty().set(pgeyA.pgeyGarment());
            }
        });
        return pgeyCombo;
    }

    private PgeyColorSwatchGroup pgeyColorGroup(String pgeyId, String pgeyLabel, java.util.function.Function<PgeyDesignElement, String> pgeyGet,
                                        java.util.function.BiFunction<PgeyDesignElement, String, PgeyDesignElement> pgeySet) {
        PgeyColorSwatchGroup pgeyGroup = new PgeyColorSwatchGroup();
        pgeyGroup.setAccessibleText(pgeyLabel);
        pgeyGroup.valueProperty().addListener((pgeyObs, pgeyOld, pgeyC) -> {
            if (!pgeySyncing && pgeyC != null) {
                pgeyViewModel.pgeyEditElement(pgeyId, pgeyLabel, pgeyEl -> pgeySet.apply(pgeyEl, PgeyDesignDefaults.pgeyHex(pgeyC)), null);
            }
        });
        pgeySync(() -> pgeyViewModel.pgeyDoc().pgeyElement(pgeyId).ifPresent(pgeyCur -> {
            String pgeyHex = pgeyGet.apply(pgeyCur);
            if (pgeyHex != null) {
                pgeyGroup.setValue(pgeyColor(pgeyHex));
            }
        }));
        return pgeyGroup;
    }

    private Slider pgeySlider(double pgeyMin, double pgeyMax, double pgeyValue, String pgeyMergeKey, String pgeyLabel,
                          java.util.function.BiFunction<PgeyDesignElement, Double, PgeyDesignElement> pgeySet,
                          java.util.function.ToDoubleFunction<PgeyDesignElement> pgeyGet, String pgeyId) {
        Slider pgeyS = new Slider(pgeyMin, pgeyMax, pgeyValue);
        pgeyS.setAccessibleText(pgeyLabel);
        pgeyS.valueProperty().addListener((pgeyObs, pgeyOld, pgeyV) -> {
            if (!pgeySyncing) {
                pgeyViewModel.pgeyEditElement(pgeyId, pgeyLabel, pgeyEl -> pgeySet.apply(pgeyEl, pgeyV.doubleValue()), pgeyMergeKey);
            }
        });
        pgeyS.setOnMouseReleased(pgeyX -> pgeyViewModel.pgeySeal());
        pgeySync(() -> pgeyViewModel.pgeyDoc().pgeyElement(pgeyId).ifPresent(pgeyCur -> pgeyS.setValue(pgeyGet.applyAsDouble(pgeyCur))));
        return pgeyS;
    }

    private Label pgeyValueLabel(String pgeyId, java.util.function.Function<PgeyDesignElement, String> pgeyText) {
        Label pgeyL = new Label();
        pgeyL.getStyleClass().addAll("body-s", "ink-muted");
        pgeySync(() -> pgeyViewModel.pgeyDoc().pgeyElement(pgeyId).ifPresent(pgeyCur -> pgeyL.setText(pgeyText.apply(pgeyCur))));
        return pgeyL;
    }

    private static VBox pgeyLabeled(String pgeyTitle, javafx.scene.Node... pgeyContent) {
        Label pgeyLabel = new Label(pgeyTitle);
        pgeyLabel.getStyleClass().addAll("label-text", "ink-muted");
        VBox pgeyBox = new VBox(8, pgeyLabel);
        pgeyBox.getChildren().addAll(pgeyContent);
        return pgeyBox;
    }

}
