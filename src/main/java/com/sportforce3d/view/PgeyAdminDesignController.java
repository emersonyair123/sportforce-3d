package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.app.PpqhRouter;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhModalDialog;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfLink;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.design.PgeyArea;
import com.sportforce3d.design.PgeyPatternType;
import com.sportforce3d.design.PgeyRealisticSpec;
import com.sportforce3d.design.PgeyRealisticView;
import com.sportforce3d.design.PgeyViewSync;
import com.sportforce3d.design.PgeyTechSheet;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.PpqhOrderItem;
import com.sportforce3d.render.PgeyRealisticModel;
import com.sportforce3d.render.PgeyRealisticModelRepository;
import com.sportforce3d.render.PgeyRealisticPattern;
import com.sportforce3d.service.PpqhAdminOrderService.PpqhOrderDesign;
import com.sportforce3d.three.PgeyDesignRenderer;
import com.sportforce3d.three.PgeyElementImages;
import com.sportforce3d.three.PgeyTextureComposer;
import com.sportforce3d.three.PgeyViewPreset;
import com.sportforce3d.three.PgeyViewer3D;
import com.sportforce3d.three.human.PgeyHumanModels;
import com.sportforce3d.three.human.PgeyOutfitFitter;
import com.sportforce3d.three.human.PgeyPersona;
import com.sportforce3d.three.model.PgeyModelProviders;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PgeyAdminDesignController implements PpqhRouter.PpqhRoutable {

    private final PpqhAppContext pgeyContext;
    private final PgeyViewer3D pgeyViewer = new PgeyViewer3D();
    private final PgeyDesignRenderer pgeyRenderer;
    private final PgeyRealisticPreview pgeyPreview = new PgeyRealisticPreview(150);
    private final HBox pgeyViewSwitch = new HBox(4);
    private final Region pgeySwitchSep = new Region();
    private final List<Node> pgeyViewerButtons = new ArrayList<>();
    private final ToggleGroup pgeyViewGroup = new ToggleGroup();
    private final PgeyElementImages pgeyImages;
    private PpqhOrderDesign pgeyDesign;
    private boolean pgeyDressed;
    private long pgeyOrderId;

    @FXML private StackPane pgeyStage;
    @FXML private HBox pgeyToolbar;
    @FXML private VBox pgeyLoadingBox;
    @FXML private VBox pgeyPanel;
    @FXML private PpqhSfLink pgeyBackLink;
    @FXML private Label pgeyHint;

    public PgeyAdminDesignController(PpqhAppContext pgeyContext) {
        this.pgeyContext = pgeyContext;
        this.pgeyImages = new PgeyElementImages(pgeyContext.ppqhStorage());
        this.pgeyRenderer = new PgeyDesignRenderer(pgeyViewer, new PgeyTextureComposer(pgeyImages));
    }

    @FXML
    private void initialize() {
        pgeyStage.getChildren().add(0, pgeyViewer);
        pgeyStage.getChildren().add(1, pgeyPreview.pgeyNode());
        pgeyViewer.autoRotateProperty().set(false);
        pgeyBackLink.setGraphic(PpqhIcons.ppqhOf(Feather.ARROW_LEFT, PpqhIcons.PPQH_SM));
        pgeyBackLink.setOnAction(pgeyE -> pgeyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_ORDER, pgeyOrderId));
        pgeyToolbar.setAlignment(Pos.CENTER);
        pgeyToolbar.setSpacing(4);
        pgeyViewSwitch.getStyleClass().add("sf-seg");
        for (boolean pgeyReal : new boolean[]{true, false}) {
            ToggleButton pgeyItem = new ToggleButton(pgeyReal ? "Vista realista" : "Vista 3D");
            pgeyItem.getStyleClass().setAll("sf-seg-item");
            pgeyItem.setToggleGroup(pgeyViewGroup);
            pgeyItem.setUserData(pgeyReal);
            pgeyItem.setMnemonicParsing(false);
            pgeyViewSwitch.getChildren().add(pgeyItem);
        }
        pgeyViewGroup.selectedToggleProperty().addListener((pgeyObs, pgeyOld, pgeyT) -> {
            if (pgeyT == null) {
                pgeyViewGroup.selectToggle(pgeyOld);
            } else {
                pgeyShowRealistic((Boolean) pgeyT.getUserData());
            }
        });
        pgeySwitchSep.getStyleClass().add("sf-toolbar-sep");
        pgeyToolbar.getChildren().addAll(pgeyViewSwitch, pgeySwitchSep);
        for (PgeyViewPreset pgeyPreset : PgeyViewPreset.values()) {
            PpqhSfButton pgeyB = new PpqhSfButton(pgeyPreset.pgeyLabel(), PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
            pgeyB.getStyleClass().add("sf-toolbar-btn");
            pgeyB.setOnAction(pgeyE -> pgeyViewer.pgeyShowView(pgeyPreset));
            pgeyToolbar.getChildren().add(pgeyB);
            pgeyViewerButtons.add(pgeyB);
        }
        PpqhSfButton pgeyReset = PpqhSfButton.ppqhIcon(Feather.MAXIMIZE, "Cuerpo completo");
        pgeyReset.setOnAction(pgeyE -> pgeyViewer.pgeyResetView());
        pgeyToolbar.getChildren().add(pgeyReset);
        pgeyViewerButtons.add(pgeyReset);
        pgeyShowRealistic(false);
    }

    private void pgeyShowRealistic(boolean pgeyReal) {
        pgeyPreview.pgeyNode().setVisible(pgeyReal);
        pgeyViewer.setVisible(!pgeyReal);
        for (Node pgeyN : pgeyViewerButtons) {
            pgeyN.setVisible(!pgeyReal);
            pgeyN.setManaged(!pgeyReal);
        }
        boolean pgeySep = !pgeyReal && pgeyViewSwitch.isManaged();
        pgeySwitchSep.setVisible(pgeySep);
        pgeySwitchSep.setManaged(pgeySep);
        pgeyHint.setText(pgeyReal
                ? "Solo lectura · copia del diseño tal como se compró · vista realista dibujada con sus colores y textos"
                : "Solo lectura · copia del diseño tal como se compró · arrastra para girar, rueda para acercar");
    }

    @Override
    public void ppqhOnRoute(Object pgeyParam) {
        if (!(pgeyParam instanceof Long pgeyItemId)) {
            pgeyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_ORDERS);
            return;
        }
        PpqhAsync.ppqhRun(() -> {
            PpqhOrderDesign pgeyD = pgeyContext.ppqhAdminOrderService().ppqhDesign(pgeyItemId);
            var pgeyDoc = pgeyD.ppqhDocument();
            PgeyPersona pgeyP = PgeyPersona.pgeyForDesign(pgeyDoc.pgeyRealisticOrDefault().pgeyModelKey(), pgeyDoc.pgeyBodyType());
            Object pgeyModel;
            try {
                pgeyModel = PgeyHumanModels.pgeyAvailable(pgeyP) ? PgeyHumanModels.pgeyDress(pgeyP, pgeyDoc.pgeyCut()) : null;
            } catch (RuntimeException pgeyE) {
                pgeyModel = null;
            }
            return new Object[]{pgeyD, pgeyModel != null ? pgeyModel : PgeyModelProviders.pgeyLoad(pgeyDoc.pgeyBodyType())};
        }, pgeyResult -> {
            pgeyDesign = (PpqhOrderDesign) pgeyResult[0];
            pgeyOrderId = pgeyDesign.ppqhOrderId();
            pgeyDressed = pgeyResult[1] instanceof PgeyOutfitFitter.PgeyDressedPerson;
            if (pgeyDressed) {
                pgeyViewer.setDressedPerson((PgeyOutfitFitter.PgeyDressedPerson) pgeyResult[1]);
            } else {
                pgeyViewer.setModel((com.sportforce3d.three.model.PgeyModelData) pgeyResult[1]);
            }
            pgeyRenderer.pgeyApplyAll(pgeyDesign.ppqhDocument(), pgeyDesign.ppqhMaterials());
            pgeyLoadingBox.setVisible(false);
            pgeyShowRealisticView();
            pgeyBuildPanel();
        }, pgeyE -> {
            pgeyLoadingBox.setVisible(false);
            pgeyPanel.getChildren().setAll(pgeyBackLink, new PpqhEmptyState(Feather.ALERT_TRIANGLE, "No se pudo abrir el uniforme",
                    PpqhErrorMessages.ppqhOf(pgeyE)));
        });
    }

    private void pgeyShowRealisticView() {
        PgeyRealisticView pgeyView = PgeyViewSync.pgeyView(pgeyDesign.ppqhDocument());
        boolean pgeyShow = pgeyDesign.ppqhDocument().pgeyRealistic() != null && pgeyView.pgeyNotices().isEmpty();
        pgeyViewSwitch.setVisible(pgeyShow);
        pgeyViewSwitch.setManaged(pgeyShow);
        if (!pgeyShow) {
            pgeyShowRealistic(false);
            return;
        }
        pgeyViewGroup.getToggles().stream().filter(pgeyT -> Boolean.TRUE.equals(pgeyT.getUserData())).findFirst()
                .ifPresent(pgeyViewGroup::selectToggle);
        PgeyRealisticPreview.PgeyCrest pgeyCrest = PgeyRealisticPreview.pgeyCrest(pgeyImages, pgeyView.pgeyCrest());
        PpqhAsync.ppqhRun(() -> PgeyRealisticPreview.pgeyRender(pgeyContext.ppqhRecolorService(), pgeyView, pgeyCrest),
                pgeyImgs -> pgeyPreview.pgeyShow(pgeyImgs[0], pgeyImgs[1]),
                pgeyE -> pgeyPreview.pgeyShowMessage("No se pudo dibujar la vista realista. " + PpqhErrorMessages.ppqhOf(pgeyE)));
    }

    private void pgeyBuildPanel() {
        PpqhOrderItem pgeyItem = pgeyDesign.ppqhItem();
        Label pgeyTitle = new Label("UNIFORME");
        pgeyTitle.getStyleClass().addAll("display-l", "ink");
        Label pgeySub = pgeyMuted(pgeyDesign.ppqhOrderNumber() + " · " + pgeyItem.ppqhProductName() + " · talla " + pgeyItem.ppqhSizeCode() + " · "
                + pgeyItem.ppqhQuantity() + (pgeyItem.ppqhQuantity() == 1 ? " unidad" : " unidades"));
        Label pgeyName = new Label(pgeyItem.ppqhDesignName() + " · versión " + pgeyItem.ppqhDesignVersion() + " · "
                + pgeyDesign.ppqhDocument().pgeyBodyType().pgeyLabel());
        pgeyName.getStyleClass().addAll("body-s", "brand-text");
        pgeyName.setWrapText(true);

        PpqhSfButton pgeyDownload = new PpqhSfButton("Descargar archivos", PpqhSfButton.PpqhVariant.PRIMARY, PpqhSfButton.PpqhSize.SM)
                .ppqhWithIcon(Feather.DOWNLOAD);
        pgeyDownload.setMaxWidth(Double.MAX_VALUE);
        pgeyDownload.setOnAction(pgeyE -> pgeyDownload());
        Label pgeyFilesNote = pgeyMuted("Carpeta con diseño (JSON), capturas de frente y espalda, ficha técnica (CSV para Excel)"
                + (pgeyDesign.ppqhAssets().isEmpty() ? "." : " y " + pgeyDesign.ppqhAssets().size()
                + (pgeyDesign.ppqhAssets().size() == 1 ? " logo original." : " logos originales.")));

        PgeyTechSheet pgeySheet = pgeyDesign.ppqhSheet();
        pgeyPanel.getChildren().setAll(pgeyBackLink, new VBox(6, pgeyTitle, pgeySub, pgeyName), pgeyCaptures(pgeyItem),
                new VBox(8, pgeyDownload, pgeyFilesNote), pgeyRealistic(pgeySheet), pgeyCut(pgeySheet), pgeyZones(pgeySheet), pgeyMaterials(pgeySheet),
                pgeyPatterns(pgeySheet), pgeyElements(pgeySheet));
    }

    private Node pgeyCaptures(PpqhOrderItem pgeyItem) {
        HBox pgeyRow = new HBox(12, pgeyCapture(pgeyItem.ppqhDesignFrontPath(), "Frente"), pgeyCapture(pgeyItem.ppqhDesignBackPath(), "Espalda"));
        return pgeySection("CAPTURAS DEL PEDIDO", pgeyRow);
    }

    private Node pgeyCapture(String pgeyPath, String pgeyLabel) {
        ImageView pgeyView = HepyProductImages.hepyDesignCapture(pgeyContext.ppqhStorage(), pgeyPath, 180, 220);
        StackPane pgeyBox = new StackPane();
        pgeyBox.getStyleClass().add("sf-proof");
        pgeyBox.setPrefSize(180, 220);
        pgeyBox.setMinSize(180, 220);
        if (pgeyView != null) {
            pgeyBox.getChildren().add(pgeyView);
            pgeyBox.setCursor(Cursor.HAND);
            pgeyBox.setOnMouseClicked(pgeyE -> {
                ImageView pgeyBig = new ImageView(HepyProductImages.hepyLoad(pgeyContext.ppqhStorage(), pgeyPath, 900, 1200));
                pgeyBig.setFitWidth(430);
                pgeyBig.setFitHeight(560);
                pgeyBig.setPreserveRatio(true);
                PpqhModalDialog.ppqhShow(pgeyStage, pgeyLabel, null, new StackPane(pgeyBig), "Cerrar", PpqhModalDialog.PpqhStyle.PRIMARY,
                        PpqhModalDialog::ppqhClose);
            });
        } else {
            pgeyBox.getChildren().add(pgeyMuted("Sin captura"));
        }
        Label pgeyL = pgeyMuted(pgeyLabel);
        return new VBox(6, pgeyBox, pgeyL);
    }

    private Node pgeyRealistic(PgeyTechSheet pgeySheet) {
        PgeyTechSheet.PgeyRealistic pgeyR = pgeySheet.pgeyRealistic();
        if (pgeyR == null) {
            return pgeySection("VISTA REALISTA", pgeyMuted("Diseño anterior a la vista realista: solo tiene el visor 3D."));
        }
        PgeyRealisticModelRepository pgeyModels = pgeyContext.ppqhRecolorService().pgeyModels();
        PgeyRealisticModel pgeyModel = pgeyModels.pgeyFind(pgeyR.pgeyModel()).orElse(null);
        GridPane pgeyGrid = pgeyGrid();
        int pgeyRow = 0;
        String pgeyModelLabel = pgeyModel == null ? pgeyR.pgeyModel()
                : PgeyRealisticDesigner.PGEY_CUTS.getOrDefault(pgeyModel.pgeyCut(), pgeyModel.pgeyCut()) + " · "
                + PgeyRealisticDesigner.PGEY_CATEGORIES.getOrDefault(pgeyModel.pgeyAudience(), String.valueOf(pgeyModel.pgeyAudience()));
        pgeyGrid.addRow(pgeyRow++, pgeyMuted("Modelo"), new VBox(2, pgeyText(pgeyModelLabel), pgeyMono(pgeyR.pgeyModel())));
        List<String> pgeyOrder = pgeyModel == null ? new ArrayList<>(pgeyR.pgeyZones().keySet()) : pgeyModel.pgeyZones();
        for (String pgeyZ : pgeyOrder) {
            String pgeyHex = pgeyR.pgeyZones().get(pgeyZ);
            if (pgeyHex != null) {
                pgeyGrid.addRow(pgeyRow++, pgeyMuted(pgeyModel == null ? pgeyZ : PgeyRealisticDesigner.pgeyZoneLabel(pgeyModel, pgeyZ)), pgeySwatchHex(pgeyHex));
            }
        }
        String pgeyPattern;
        try {
            pgeyPattern = PgeyRealisticPattern.pgeyFromCode(pgeyR.pgeyPattern()).pgeyLabel();
        } catch (IllegalArgumentException pgeyE) {
            pgeyPattern = pgeyR.pgeyPattern();
        }
        pgeyGrid.addRow(pgeyRow++, pgeyMuted("Diseño"), pgeyR.pgeyPatternColor() == null ? pgeyText(pgeyPattern)
                : new HBox(10, pgeyText(pgeyPattern), pgeySwatchHex(pgeyR.pgeyPatternColor())));
        pgeyGrid.addRow(pgeyRow++, pgeyMuted("Nombre"), pgeyR.pgeyName() == null ? pgeyText("—") : pgeyMono(pgeyR.pgeyName()));
        pgeyGrid.addRow(pgeyRow++, pgeyMuted("Número"), pgeyR.pgeyNumber() == null ? pgeyText("—") : pgeyMono(String.valueOf(pgeyR.pgeyNumber())));
        if (!pgeyDesign.ppqhDocument().pgeyRealisticPhotosMatch()) {
            Label pgeyNote = pgeyMuted("Solo referencia: la foto no muestra todo este uniforme ("
                    + String.join(", ", com.sportforce3d.design.PgeyViewSync.pgeyView(pgeyDesign.ppqhDocument()).pgeyNotices()
                    .stream().map(pgeyN -> pgeyN.replace(": ver en Vista 3D", "").toLowerCase()).toList())
                    + "). Se fabrica según la Vista 3D, las capturas del pedido y las secciones de abajo.");
            pgeyNote.setWrapText(true);
            return pgeySection("VISTA REALISTA", new VBox(8, pgeyNote, pgeyGrid));
        }
        return pgeySection("VISTA REALISTA", pgeyGrid);
    }

    private Node pgeyCut(PgeyTechSheet pgeySheet) {
        GridPane pgeyGrid = pgeyGrid();
        pgeyGrid.addRow(0, pgeyMuted("Manga"), pgeyText(pgeySheet.pgeyCut().pgeySleeve().pgeyLabel()));
        pgeyGrid.addRow(1, pgeyMuted("Short"), pgeyText(pgeySheet.pgeyCut().pgeyShorts().pgeyLabel() + " (" + pgeySheet.pgeyCut().pgeyShorts().pgeyDetail() + ")"));
        pgeyGrid.addRow(2, pgeyMuted("Medias"), pgeyText(pgeySheet.pgeyCut().pgeySocks().pgeyLabel() + " (" + pgeySheet.pgeyCut().pgeySocks().pgeyDetail() + ")"));
        Label pgeyNote = pgeyMuted(pgeyDressed ? "El visor 3D y las capturas del pedido muestran este corte."
                : "Sin el modelo de la persona, el visor 3D muestra el maniquí con el corte de serie; vale esta ficha.");
        pgeyNote.setWrapText(true);
        return pgeySection("CORTE", new VBox(8, pgeyGrid, pgeyNote));
    }

    private Node pgeyZones(PgeyTechSheet pgeySheet) {
        VBox pgeyBox = new VBox(14);
        for (HepyGarment pgeyG : List.of(HepyGarment.JERSEY, HepyGarment.SHORTS, HepyGarment.SOCKS, HepyGarment.SHOES)) {
            GridPane pgeyGrid = pgeyGrid();
            int pgeyR = 0;
            for (PgeyTechSheet.PgeyZoneRow pgeyZ : pgeySheet.pgeyZones()) {
                if (pgeyZ.pgeyGarment() != pgeyG) {
                    continue;
                }
                Rectangle pgeySw = new Rectangle(14, 14, pgeyColor(pgeyZ.pgeyHex()));
                pgeySw.setArcWidth(4);
                pgeySw.setArcHeight(4);
                pgeySw.getStyleClass().add("sf-zone-swatch");
                Label pgeyHex = new Label(pgeyZ.pgeyHex());
                pgeyHex.getStyleClass().addAll("code", "ink");
                pgeyGrid.addRow(pgeyR++, pgeySw, pgeyText(pgeyZ.pgeyZone().pgeyLabel()), pgeyHex);
            }
            Label pgeyHead = new Label(pgeyG.hepyLabel());
            pgeyHead.getStyleClass().addAll("body-s", "ink", "sf-strong");
            pgeyBox.getChildren().add(new VBox(6, pgeyHead, pgeyGrid));
        }
        return pgeySection("COLORES POR ZONA (VISOR 3D)", pgeyBox);
    }

    private Node pgeyMaterials(PgeyTechSheet pgeySheet) {
        GridPane pgeyGrid = pgeyGrid();
        int pgeyR = 0;
        for (PgeyTechSheet.PgeyMaterialRow pgeyM : pgeySheet.pgeyMaterials()) {
            Label pgeyCode = new Label(pgeyM.pgeyCode());
            pgeyCode.getStyleClass().addAll("code", "ink-muted");
            pgeyGrid.addRow(pgeyR++, pgeyText(pgeyM.pgeySlot().pgeyLabel()), new VBox(2, pgeyText(pgeyM.pgeyName()), pgeyCode));
        }
        return pgeySection("MATERIALES", pgeyGrid);
    }

    private Node pgeyPatterns(PgeyTechSheet pgeySheet) {
        GridPane pgeyGrid = pgeyGrid();
        int pgeyR = 0;
        for (PgeyTechSheet.PgeyPatternRow pgeyP : pgeySheet.pgeyPatterns()) {
            boolean pgeyPlain = pgeyP.pgeyType() == PgeyPatternType.LISO;
            Node pgeyColor = pgeyPlain ? pgeyText("—") : pgeySwatchHex(pgeyP.pgeyColor());
            pgeyGrid.addRow(pgeyR++, pgeyText(pgeyP.pgeyGarment().hepyLabel()), pgeyText(pgeyP.pgeyType().pgeyLabel()), pgeyColor,
                    pgeyText(pgeyPlain || pgeyP.pgeyType() == PgeyPatternType.DEGRADADO ? "" : String.format(Locale.ROOT, "%.1f cm", pgeyP.pgeyWidthCm())));
        }
        return pgeySection("PATRONES", pgeyGrid);
    }

    private Node pgeyElements(PgeyTechSheet pgeySheet) {
        VBox pgeyList = new VBox(10);
        if (pgeySheet.pgeyElements().isEmpty()) {
            pgeyList.getChildren().add(pgeyMuted("Sin nombres, números, emblemas ni logos."));
        }
        for (int pgeyI = pgeySheet.pgeyElements().size() - 1; pgeyI >= 0; pgeyI--) {
            PgeyTechSheet.PgeyElementRow pgeyE = pgeySheet.pgeyElements().get(pgeyI);
            String pgeyId = pgeyDesign.ppqhDocument().pgeyElements().get(pgeyE.pgeyLayer() - 1).pgeyId();
            Label pgeyHead = new Label("Capa " + pgeyE.pgeyLayer() + " · " + pgeyE.pgeyType().pgeyLabel() + " · " + pgeyE.pgeyContent());
            pgeyHead.getStyleClass().addAll("body-s", "ink", "sf-strong");
            pgeyHead.setWrapText(true);
            GridPane pgeyGrid = pgeyGrid();
            int pgeyR = 0;
            pgeyGrid.addRow(pgeyR++, pgeyMuted("Zona"), pgeyText(pgeyE.pgeyArea().pgeyLabel() + (pgeyE.pgeyVisible() ? "" : " (oculto)")));
            pgeyGrid.addRow(pgeyR++, pgeyMuted("Posición"), pgeyMono(String.format(Locale.ROOT, "u %.3f · v %.3f", pgeyE.pgeyU(), pgeyE.pgeyV())));
            pgeyGrid.addRow(pgeyR++, pgeyMuted("Escala / giro"), pgeyMono(String.format(Locale.ROOT, "%.0f %% · %.0f°", pgeyE.pgeyScalePercent(), pgeyE.pgeyRotationDeg())));
            pgeyGrid.addRow(pgeyR++, pgeyMuted("Alto"), pgeyMono(String.format(Locale.ROOT, "%.1f cm", pgeyE.pgeyHeightCm())));
            if (pgeyE.pgeyFont() != null) {
                pgeyGrid.addRow(pgeyR++, pgeyMuted("Tipografía"), pgeyText(pgeyE.pgeyFont()));
            }
            if (pgeyE.pgeyColor() != null) {
                pgeyGrid.addRow(pgeyR++, pgeyMuted(pgeyE.pgeyType().pgeyIsText() ? "Color" : "Color principal"), pgeySwatchHex(pgeyE.pgeyColor()));
            }
            if (pgeyE.pgeyAccentColor() != null && (!pgeyE.pgeyType().pgeyIsText() || pgeyE.pgeyOutlineMm() > 0)) {
                pgeyGrid.addRow(pgeyR++, pgeyMuted(pgeyE.pgeyType().pgeyIsText() ? "Contorno" : "Acento"), pgeyE.pgeyType().pgeyIsText()
                        ? new HBox(8, pgeySwatchHex(pgeyE.pgeyAccentColor()), pgeyMono(String.format(Locale.ROOT, "%.1f mm", pgeyE.pgeyOutlineMm())))
                        : pgeySwatchHex(pgeyE.pgeyAccentColor()));
            }
            if (pgeyE.pgeyAssetName() != null) {
                pgeyGrid.addRow(pgeyR++, pgeyMuted("Archivo"), pgeyText(pgeyE.pgeyAssetName()));
            }
            VBox pgeyCard = new VBox(8, pgeyHead, pgeyGrid);
            pgeyCard.getStyleClass().add("sf-layer-row");
            pgeyCard.setStyle("-fx-padding: 12;");
            pgeyCard.setOnMouseClicked(pgeyX -> pgeyFocusElement(pgeyId, pgeyE.pgeyArea()));
            pgeyList.getChildren().add(pgeyCard);
        }
        return pgeySection("ELEMENTOS (clic para ubicarlo en el 3D)", pgeyList);
    }

    private void pgeyFocusElement(String pgeyId, PgeyArea pgeyArea) {
        String pgeyOld = pgeyRenderer.pgeyComposer().pgeyHighlight();
        pgeyRenderer.pgeyComposer().pgeySetHighlight(pgeyId);
        Set<HepyGarment> pgeyGarments = EnumSet.of(pgeyArea.pgeyGarment());
        pgeyDesign.ppqhDocument().pgeyElement(pgeyOld == null ? "" : pgeyOld).ifPresent(pgeyE -> pgeyGarments.add(pgeyE.pgeyArea().pgeyGarment()));
        pgeyRenderer.pgeyApplyGarments(pgeyDesign.ppqhDocument(), pgeyGarments);
        pgeyViewer.pgeyJumpTo(pgeyArea == PgeyArea.JERSEY_BACK ? PgeyViewPreset.BACK : PgeyViewPreset.FRONT, pgeyArea.pgeyGarment());
    }

    private void pgeyDownload() {
        DirectoryChooser pgeyChooser = new DirectoryChooser();
        pgeyChooser.setTitle("Elige dónde guardar los archivos del uniforme");
        File pgeyDir = pgeyChooser.showDialog(pgeyStage.getScene().getWindow());
        if (pgeyDir == null) {
            return;
        }
        long pgeyItemId = pgeyDesign.ppqhItem().ppqhId();
        PpqhAsync.ppqhRun(() -> pgeyContext.ppqhAdminOrderService().ppqhExportDesign(pgeyItemId, pgeyDir.toPath()),
                pgeyFolder -> PpqhToast.ppqhSuccess(pgeyStage, "Archivos guardados en " + pgeyFolder.getFileName()),
                pgeyE -> PpqhToast.ppqhError(pgeyStage, PpqhErrorMessages.ppqhOf(pgeyE)));
    }

    private static VBox pgeySection(String pgeyTitle, Node pgeyBody) {
        Label pgeyL = new Label(pgeyTitle);
        pgeyL.getStyleClass().addAll("label-text", "ink-muted");
        return new VBox(10, pgeyL, pgeyBody);
    }

    private static GridPane pgeyGrid() {
        GridPane pgeyG = new GridPane();
        pgeyG.setHgap(12);
        pgeyG.setVgap(6);
        return pgeyG;
    }

    private static Node pgeySwatchHex(String pgeyHex) {
        Rectangle pgeySw = new Rectangle(14, 14, pgeyColor(pgeyHex));
        pgeySw.setArcWidth(4);
        pgeySw.setArcHeight(4);
        pgeySw.getStyleClass().add("sf-zone-swatch");
        HBox pgeyBox = new HBox(8, pgeySw, pgeyMono(pgeyHex));
        pgeyBox.setAlignment(Pos.CENTER_LEFT);
        return pgeyBox;
    }

    private static Label pgeyText(String pgeyS) {
        Label pgeyL = new Label(pgeyS);
        pgeyL.getStyleClass().addAll("body-s", "ink");
        pgeyL.setWrapText(true);
        return pgeyL;
    }

    private static Label pgeyMono(String pgeyS) {
        Label pgeyL = new Label(pgeyS);
        pgeyL.getStyleClass().addAll("code", "ink");
        return pgeyL;
    }

    private static Label pgeyMuted(String pgeyS) {
        Label pgeyL = new Label(pgeyS);
        pgeyL.getStyleClass().addAll("body-s", "ink-muted");
        pgeyL.setWrapText(true);
        pgeyL.setMinHeight(Region.USE_PREF_SIZE);
        return pgeyL;
    }

    private static Color pgeyColor(String pgeyHex) {
        try {
            return Color.web(pgeyHex);
        } catch (RuntimeException pgeyE) {
            return Color.GRAY;
        }
    }
}
