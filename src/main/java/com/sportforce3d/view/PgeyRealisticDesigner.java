package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.component.PgeyColorSwatchGroup;
import com.sportforce3d.component.HepyFilterChip;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.design.PgeyDesignDefaults;
import com.sportforce3d.design.PgeyRealisticSpec;
import com.sportforce3d.design.PgeyRealisticView;
import com.sportforce3d.render.PgeyArgbImage;
import com.sportforce3d.render.PgeyRealisticModel;
import com.sportforce3d.render.PgeyRealisticModelRepository;
import com.sportforce3d.render.PgeyRealisticPattern;
import com.sportforce3d.render.PgeyRecolorService;
import com.sportforce3d.viewmodel.PgeyDesignerViewModel;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class PgeyRealisticDesigner {

    private static final long PGEY_FRAME_NANOS = 1_000_000_000L / 30;
    private static final Locale PGEY_ES_PE = Locale.of("es", "PE");
    static final Map<String, String> PGEY_CATEGORIES = pgeyCategories();
    static final Map<String, String> PGEY_CUTS = pgeyCuts();

    private final PgeyDesignerViewModel pgeyViewModel;
    private final PgeyRecolorService pgeyRecolor;
    private final PgeyRealisticModelRepository pgeyModels;
    private final PgeyRealisticPreview pgeyPreview = new PgeyRealisticPreview(170);

    private final List<Runnable> pgeySyncers = new ArrayList<>();
    private boolean pgeySyncing;
    private String pgeyZone = PgeyRealisticModel.PGEY_CAMISETA;
    private PgeyRealisticView pgeyShown;
    private final com.sportforce3d.three.PgeyElementImages pgeyImages;
    private boolean pgeyBusy;
    private long pgeyLastFrame;

    private final AnimationTimer pgeyLoop = new AnimationTimer() {
        @Override
        public void handle(long pgeyNow) {
            if (pgeyNow - pgeyLastFrame >= PGEY_FRAME_NANOS) {
                pgeyLastFrame = pgeyNow;
                pgeyRenderIfChanged();
            }
        }
    };

    PgeyRealisticDesigner(PgeyDesignerViewModel pgeyViewModel, PgeyRecolorService pgeyRecolor, com.sportforce3d.three.PgeyElementImages pgeyImages) {
        this.pgeyViewModel = pgeyViewModel;
        this.pgeyImages = pgeyImages;
        this.pgeyRecolor = pgeyRecolor;
        this.pgeyModels = pgeyRecolor.pgeyModels();
    }

    Node pgeyStageNode() {
        return pgeyPreview.pgeyNode();
    }

    void start() {
        pgeyLoop.start();
    }

    void stop() {
        pgeyLoop.stop();
    }

    PgeyRealisticModel pgeyFrontModel(PgeyRealisticSpec pgeySpec) {
        return PgeyRealisticPreview.pgeyFrontModel(pgeyModels, pgeySpec);
    }

    PgeyRealisticPreview.PgeyCrest pgeyCrest(PgeyRealisticView pgeyView) {
        return PgeyRealisticPreview.pgeyCrest(pgeyImages, pgeyView.pgeyCrest());
    }

    PgeyArgbImage[] pgeyRenderPixels(PgeyRealisticView pgeyView, PgeyRealisticPreview.PgeyCrest pgeyCrest) {
        return PgeyRealisticPreview.pgeyRenderPixels(pgeyRecolor, pgeyView, pgeyCrest);
    }

    private void pgeyRenderIfChanged() {
        PgeyRealisticView pgeyView = pgeyViewModel.pgeyRealisticView();
        if (pgeyBusy || pgeyView.equals(pgeyShown)) {
            return;
        }
        pgeyBusy = true;
        PgeyRealisticPreview.PgeyCrest pgeyCrest = pgeyCrest(pgeyView);
        pgeyPreview.pgeyShowNotices(pgeyView.pgeyNotices());
        PpqhAsync.ppqhRun(() -> PgeyRealisticPreview.pgeyRender(pgeyRecolor, pgeyView, pgeyCrest), pgeyImgs -> {
            pgeyBusy = false;
            pgeyShown = pgeyView;
            pgeyPreview.pgeyShow(pgeyImgs[0], pgeyImgs[1]);
        }, pgeyE -> {
            pgeyBusy = false;
            pgeyShown = pgeyView;
            PpqhToast.ppqhError(pgeyPreview.pgeyNode(), "No se pudo dibujar la vista realista. " + PpqhErrorMessages.ppqhOf(pgeyE));
        });
    }

    String pgeyStructureKey() {
        return "R|" + pgeyFrontModel(pgeyViewModel.pgeyRealistic()).pgeyKey();
    }

    void pgeyBuild(VBox pgeyPanel) {
        pgeySyncers.clear();
        PgeyRealisticSpec pgeySpec = pgeyViewModel.pgeyRealistic();
        PgeyRealisticModel pgeyModel = pgeyFrontModel(pgeySpec);
        if (!pgeyModel.pgeyHasZone(pgeyZone)) {
            pgeyZone = pgeyModel.pgeyZones().get(0);
        }

        FlowPane pgeyCategories = new FlowPane(6, 6);
        ToggleGroup pgeyCategoryGroup = new ToggleGroup();
        PGEY_CATEGORIES.forEach((pgeyAudience, pgeyLabel) -> {
            HepyFilterChip pgeyChip = new HepyFilterChip(pgeyLabel);
            pgeyChip.setToggleGroup(pgeyCategoryGroup);
            pgeyChip.setOnAction(pgeyE -> {
                pgeyChip.setSelected(true);
                pgeySelectAudience(pgeyAudience, pgeyLabel);
            });
            pgeySync(() -> pgeyChip.setSelected(pgeyAudience.equals(pgeyFrontModel(pgeyViewModel.pgeyRealistic()).pgeyAudience())));
            pgeyCategories.getChildren().add(pgeyChip);
        });
        pgeyPanel.getChildren().add(pgeySection("CATEGORÍA", pgeyCategories));

        FlowPane pgeyCuts = new FlowPane(6, 6);
        ToggleGroup pgeyCutGroup = new ToggleGroup();
        String pgeyAudience = pgeyModel.pgeyAudience();
        PGEY_CUTS.forEach((pgeyCut, pgeyLabel) -> {
            if (pgeyModels.pgeyFind(pgeyKey(pgeyCut, pgeyAudience)).isEmpty()) {
                return;
            }
            HepyFilterChip pgeyChip = new HepyFilterChip(pgeyLabel);
            pgeyChip.setToggleGroup(pgeyCutGroup);
            pgeyChip.setOnAction(pgeyE -> {
                pgeyChip.setSelected(true);
                pgeyViewModel.pgeyEditRealistic("Corte · " + pgeyLabel, pgeyS -> pgeyS.pgeyWithModel(pgeyKey(pgeyCut, pgeyAudience)), null);
            });
            pgeySync(() -> pgeyChip.setSelected(pgeyCut.equals(pgeyFrontModel(pgeyViewModel.pgeyRealistic()).pgeyCut())));
            pgeyCuts.getChildren().add(pgeyChip);
        });
        pgeyPanel.getChildren().add(pgeySection("CORTE", pgeyCuts));

        VBox pgeyZones = new VBox(4);
        ToggleGroup pgeyZoneGroup = new ToggleGroup();
        for (String pgeyZ : pgeyModel.pgeyZones()) {
            Rectangle pgeySwatch = new Rectangle(16, 16);
            pgeySwatch.setArcWidth(5);
            pgeySwatch.setArcHeight(5);
            pgeySwatch.getStyleClass().add("sf-zone-swatch");
            ToggleButton pgeyRow = new ToggleButton(pgeyZoneLabel(pgeyModel, pgeyZ), pgeySwatch);
            pgeyRow.getStyleClass().setAll("sf-zone-row");
            pgeyRow.setMaxWidth(Double.MAX_VALUE);
            pgeyRow.setAlignment(Pos.CENTER_LEFT);
            pgeyRow.setGraphicTextGap(10);
            pgeyRow.setToggleGroup(pgeyZoneGroup);
            pgeyRow.setOnAction(pgeyE -> {
                pgeyZone = pgeyZ;
                pgeyRefresh();
            });
            pgeySync(() -> {
                pgeySwatch.setFill(pgeyColor(pgeyViewModel.pgeyRealistic().pgeyColor(pgeyZ)));
                pgeyRow.setSelected(pgeyZone.equals(pgeyZ));
            });
            pgeyZones.getChildren().add(pgeyRow);
        }
        PgeyColorSwatchGroup pgeyZoneColor = new PgeyColorSwatchGroup();
        pgeyZoneColor.valueProperty().addListener((pgeyObs, pgeyOld, pgeyC) -> {
            if (!pgeySyncing && pgeyC != null) {
                String pgeyZ = pgeyZone;
                pgeyViewModel.pgeyEditRealistic("Color · " + pgeyZoneLabel(pgeyModel, pgeyZ), pgeyS -> pgeyS.pgeyWithColor(pgeyZ, PgeyDesignDefaults.pgeyHex(pgeyC)), null);
            }
        });
        pgeySync(() -> pgeyZoneColor.setValue(pgeyColor(pgeyViewModel.pgeyRealistic().pgeyColor(pgeyZone))));
        pgeyPanel.getChildren().add(pgeySection("COLORES POR ZONA", pgeyZones, pgeyZoneColor));

        boolean pgeyShirt = pgeyModel.pgeyShirt() != null && pgeyModel.pgeyHasZone(PgeyRealisticModel.PGEY_CAMISETA);
        if (!pgeyShirt) {
            pgeyPanel.getChildren().add(pgeySection("DISEÑO Y TEXTOS",
                    pgeyNote("Los buzos, la casaca y los productos solo cambian de color: no llevan diseño, nombre ni número.")));
            return;
        }

        FlowPane pgeyPatterns = new FlowPane(6, 6);
        ToggleGroup pgeyPatternGroup = new ToggleGroup();
        for (PgeyRealisticPattern pgeyP : PgeyRealisticPattern.values()) {
            HepyFilterChip pgeyChip = new HepyFilterChip(pgeyP.pgeyLabel());
            pgeyChip.setToggleGroup(pgeyPatternGroup);
            pgeyChip.setOnAction(pgeyE -> {
                pgeyChip.setSelected(true);
                pgeyViewModel.pgeyEditRealistic("Diseño · " + pgeyP.pgeyLabel(), pgeyS -> pgeyS.pgeyWithPattern(pgeyP.pgeyCode()), null);
            });
            pgeySync(() -> pgeyChip.setSelected(pgeyP.pgeyCode().equals(pgeyViewModel.pgeyRealistic().pgeyPattern())));
            pgeyPatterns.getChildren().add(pgeyChip);
        }
        PgeyColorSwatchGroup pgeyPatternColor = new PgeyColorSwatchGroup();
        pgeyPatternColor.valueProperty().addListener((pgeyObs, pgeyOld, pgeyC) -> {
            if (!pgeySyncing && pgeyC != null) {
                pgeyViewModel.pgeyEditRealistic("Color del diseño", pgeyS -> pgeyS.pgeyWithPatternColor(PgeyDesignDefaults.pgeyHex(pgeyC)), null);
            }
        });
        VBox pgeyPatternOptions = new VBox(10, pgeyNote("Color del diseño"), pgeyPatternColor);
        pgeySync(() -> {
            boolean pgeyOn = !pgeyViewModel.pgeyRealistic().pgeyPlain();
            pgeyPatternOptions.setVisible(pgeyOn);
            pgeyPatternOptions.setManaged(pgeyOn);
            pgeyPatternColor.setValue(pgeyColor(pgeyViewModel.pgeyRealistic().pgeyPatternColor()));
        });
        pgeyPanel.getChildren().add(pgeySection("DISEÑO", pgeyPatterns, pgeyPatternOptions));

        PpqhSfTextField pgeyName = new PpqhSfTextField("Nombre (espalda)");
        pgeyName.setPromptText("Máx. " + PgeyRealisticSpec.PGEY_NAME_MAX + " letras");
        PpqhInputs.ppqhLimit(pgeyName.getInput(), PgeyRealisticSpec.PGEY_NAME_MAX);
        pgeyName.textProperty().addListener((pgeyObs, pgeyOld, pgeyText) -> {
            if (!pgeySyncing) {
                String pgeyClean = pgeyText == null || pgeyText.isBlank() ? null : pgeyText.trim().toUpperCase(PGEY_ES_PE);
                pgeyViewModel.pgeyEditRealistic("Nombre", pgeyS -> pgeyS.pgeyWithName(pgeyClean), "realistic-name");
            }
        });
        PpqhSfTextField pgeyNumber = new PpqhSfTextField("Número (0–99)");
        pgeyNumber.setPromptText("10");
        pgeyNumber.getInput().setTextFormatter(new TextFormatter<String>(pgeyChange ->
                pgeyChange.getControlNewText().matches("\\d{0,2}") ? pgeyChange : null));
        pgeyNumber.textProperty().addListener((pgeyObs, pgeyOld, pgeyText) -> {
            if (!pgeySyncing) {
                Integer pgeyN = pgeyText == null || pgeyText.isEmpty() ? null : Integer.valueOf(pgeyText);
                pgeyViewModel.pgeyEditRealistic("Número", pgeyS -> pgeyS.pgeyWithNumber(pgeyN), "realistic-number");
            }
        });
        pgeyName.getInput().focusedProperty().addListener((pgeyObs, pgeyWas, pgeyNow) -> {
            if (!pgeyNow) {
                pgeyViewModel.pgeySeal();
            }
        });
        pgeyNumber.getInput().focusedProperty().addListener((pgeyObs, pgeyWas, pgeyNow) -> {
            if (!pgeyNow) {
                pgeyViewModel.pgeySeal();
            }
        });
        pgeySync(() -> {
            PgeyRealisticSpec pgeyS = pgeyViewModel.pgeyRealistic();
            String pgeyN = pgeyS.pgeyName() == null ? "" : pgeyS.pgeyName();
            if (!pgeyN.equalsIgnoreCase(pgeyName.getText() == null ? "" : pgeyName.getText().trim())) {
                pgeyName.setText(pgeyN);
            }
            String pgeyNum = pgeyS.pgeyNumber() == null ? "" : String.valueOf(pgeyS.pgeyNumber());
            if (!pgeyNum.equals(pgeyNumber.getText())) {
                pgeyNumber.setText(pgeyNum);
            }
        });
        pgeyPanel.getChildren().add(pgeySection("NOMBRE Y NÚMERO", pgeyName, pgeyNumber,
                pgeyNote(pgeyModels.pgeyOtherView(pgeyModel).isPresent()
                        ? "El nombre va en la espalda; el número, en el pecho y en la espalda."
                        : "El número se ve en el pecho. Esta foto no tiene espalda: el nombre y el número de la "
                        + "espalda se ven en la Vista 3D.")));
    }

    void pgeySelectAudience(String pgeyAudience, String pgeyLabel) {
        String pgeyCut = pgeyFrontModel(pgeyViewModel.pgeyRealistic()).pgeyCut();
        String pgeyKey = pgeyModels.pgeyFind(pgeyKey(pgeyCut, pgeyAudience)).isPresent() ? pgeyKey(pgeyCut, pgeyAudience) : pgeyKey("futbol", pgeyAudience);
        pgeyViewModel.pgeyEditRealistic("Categoría · " + pgeyLabel, pgeyS -> pgeyS.pgeyWithModel(pgeyKey), null);
    }

    void pgeyRefresh() {
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

    static String pgeyKey(String pgeyCut, String pgeyAudience) {
        return pgeyCut + "_" + pgeyAudience + "_frente";
    }

    static String pgeyZoneLabel(PgeyRealisticModel pgeyModel, String pgeyZone) {
        return switch (pgeyZone) {
            case PgeyRealisticModel.PGEY_CAMISETA -> "Camiseta";
            case PgeyRealisticModel.PGEY_SHORT -> "Short";
            case PgeyRealisticModel.PGEY_PANELES -> "raglan".equals(pgeyModel.pgeyCut()) ? "Mangas" : "Paneles";
            case PgeyRealisticModel.PGEY_MEDIAS -> "Medias";
            case PgeyRealisticModel.PGEY_PRINCIPAL -> "casaca".equals(pgeyModel.pgeyCut()) ? "Casaca" : "Buzo";
            default -> pgeyZone;
        };
    }

    private static Map<String, String> pgeyCategories() {
        Map<String, String> pgeyM = new LinkedHashMap<>();
        pgeyM.put("hombre", "Hombre");
        pgeyM.put("mujer", "Mujer");
        pgeyM.put("nino", "Niño");
        pgeyM.put("nina", "Niña");
        return java.util.Collections.unmodifiableMap(pgeyM);
    }

    private static Map<String, String> pgeyCuts() {
        Map<String, String> pgeyM = new LinkedHashMap<>();
        pgeyM.put("futbol", "Fútbol cuello V");
        pgeyM.put("polo", "Polo");
        pgeyM.put("redondo", "Cuello redondo");
        pgeyM.put("raglan", "Raglán");
        pgeyM.put("mangalarga", "Manga larga");
        pgeyM.put("voley", "Vóley");
        pgeyM.put("basquet", "Básquet");
        pgeyM.put("buzo", "Buzo");
        pgeyM.put("shortlargo", "Short largo");
        pgeyM.put("licra", "Licra");
        pgeyM.put("arquero", "Arquero");
        pgeyM.put("compresion", "Compresión");
        pgeyM.put("casaca", "Casaca");
        return java.util.Collections.unmodifiableMap(pgeyM);
    }

    private static VBox pgeySection(String pgeyTitle, Node... pgeyContent) {
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

    private static Color pgeyColor(String pgeyHex) {
        try {
            return Color.web(pgeyHex);
        } catch (RuntimeException pgeyE) {
            return Color.GRAY;
        }
    }

    static PgeyArgbImage pgeySideBySide(PgeyArgbImage pgeyA, PgeyArgbImage pgeyB) {
        if (pgeyB == null) {
            return pgeyA;
        }
        int pgeyGap = 24;
        int pgeyW = pgeyA.pgeyWidth() + pgeyGap + pgeyB.pgeyWidth();
        int pgeyH = Math.max(pgeyA.pgeyHeight(), pgeyB.pgeyHeight());
        int[] pgeyPx = new int[pgeyW * pgeyH];
        for (int pgeyY = 0; pgeyY < pgeyA.pgeyHeight(); pgeyY++) {
            System.arraycopy(pgeyA.pgeyPixels(), pgeyY * pgeyA.pgeyWidth(), pgeyPx, pgeyY * pgeyW, pgeyA.pgeyWidth());
        }
        for (int pgeyY = 0; pgeyY < pgeyB.pgeyHeight(); pgeyY++) {
            System.arraycopy(pgeyB.pgeyPixels(), pgeyY * pgeyB.pgeyWidth(), pgeyPx, pgeyY * pgeyW + pgeyA.pgeyWidth() + pgeyGap, pgeyB.pgeyWidth());
        }
        return new PgeyArgbImage(pgeyW, pgeyH, pgeyPx);
    }

    static void pgeyWritePng(PgeyArgbImage pgeyImg, Path pgeyFile) throws java.io.IOException {
        PgeyRecolorService.pgeyWritePng(pgeyImg, pgeyFile);
    }
}
