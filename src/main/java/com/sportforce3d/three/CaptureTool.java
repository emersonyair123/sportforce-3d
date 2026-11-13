package com.sportforce3d.three;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.procedural.PgeyProceduralModelProvider;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class CaptureTool {

    private record PgeyShot(String pgeyName, PgeyViewPreset pgeyView, HepyGarment pgeyFocus, int pgeyWidth, int pgeyHeight) {
    }

    private CaptureTool() {
    }

    public static void main(String[] pgeyArgs) throws Exception {
        Path pgeyOut = Path.of(pgeyArgs.length > 0 ? pgeyArgs[0] : "docs/capturas");
        String pgeyWhich = pgeyArgs.length > 1 ? pgeyArgs[1] : "ADULTO_HOMBRE";
        if (pgeyWhich.equals("ETAPA4")) {
            pgeyCaptureSampleDesign(pgeyOut);
            return;
        }
        if (pgeyWhich.equals("PERSONAS")) {
            pgeyCapturePersons(pgeyOut);
            return;
        }
        if (pgeyWhich.equals("UNIFORMES") || pgeyWhich.equals("DEFECTO")) {
            pgeyCaptureDressed(pgeyOut, pgeyWhich.equals("DEFECTO"));
            return;
        }
        if (pgeyWhich.equals("TELA")) {
            pgeyCaptureSimulated(pgeyOut);
            return;
        }
        List<PgeyBodyType> pgeyTypes = pgeyWhich.equals("ALL") ? List.of(PgeyBodyType.values()) : List.of(PgeyBodyType.valueOf(pgeyWhich));

        CountDownLatch pgeyStarted = new CountDownLatch(1);
        Platform.startup(pgeyStarted::countDown);
        pgeyStarted.await();
        try {
            for (PgeyBodyType pgeyType : pgeyTypes) {
                PgeyModelData pgeyModel = new PgeyProceduralModelProvider().pgeyLoad(pgeyType);
                String pgeyPrefix = pgeyType == PgeyBodyType.ADULTO_HOMBRE ? "" : pgeyType.pgeyKey() + "-";
                List<PgeyShot> pgeyShots = new ArrayList<>(List.of(
                        new PgeyShot(pgeyPrefix + "frente", PgeyViewPreset.FRONT, HepyGarment.KIT, 1200, 1600),
                        new PgeyShot(pgeyPrefix + "espalda", PgeyViewPreset.BACK, HepyGarment.KIT, 1200, 1600),
                        new PgeyShot(pgeyPrefix + "lateral", PgeyViewPreset.LEFT, HepyGarment.KIT, 1200, 1600)));
                if (pgeyType == PgeyBodyType.ADULTO_HOMBRE) {
                    pgeyShots.add(new PgeyShot("detalle-camiseta", PgeyViewPreset.FRONT, HepyGarment.JERSEY, 1400, 1100));
                    pgeyShots.add(new PgeyShot("detalle-zapatillas", PgeyViewPreset.LEFT, HepyGarment.SHOES, 1400, 1000));
                }
                for (PgeyShot pgeyShot : pgeyShots) {
                    WritableImage pgeyImg = pgeyOnFx(() -> pgeyRender(pgeyModel, pgeyShot));
                    PgeyPng.pgeyWrite(pgeyImg, pgeyOut.resolve(pgeyShot.pgeyName() + ".png"));
                    System.out.println("Captura: " + pgeyOut.resolve(pgeyShot.pgeyName() + ".png").toAbsolutePath());
                }
            }
        } finally {
            Platform.exit();
        }
    }

    private static void pgeyCaptureSampleDesign(Path pgeyOut) throws Exception {
        java.nio.file.Path pgeyTmp = java.nio.file.Files.createTempDirectory("sf-capturas");
        com.sportforce3d.db.HepyDatabase pgeyDb = new com.sportforce3d.db.HepyDatabase(pgeyTmp.resolve("tmp.db"));
        pgeyDb.hepyMigrate();
        var pgeyCatalog = new com.sportforce3d.repository.HepyCatalogRepository(pgeyDb);
        java.util.Map<String, com.sportforce3d.model.HepyMaterial> pgeyMaterials = new java.util.HashMap<>();
        for (var pgeyScope : com.sportforce3d.model.HepyMaterial.HepyScope.values()) {
            pgeyCatalog.hepyFindMaterials(pgeyScope).forEach(pgeyM -> pgeyMaterials.put(pgeyM.hepyCode(), pgeyM));
        }
        var pgeyDoc = com.sportforce3d.design.PgeySampleDesigns.pgeyHalcones();
        PgeyModelData pgeyModel = new PgeyProceduralModelProvider().pgeyLoad(pgeyDoc.pgeyBodyType());
        CountDownLatch pgeyStarted = new CountDownLatch(1);
        Platform.startup(pgeyStarted::countDown);
        pgeyStarted.await();
        try {
            List<PgeyShot> pgeyShots = List.of(
                    new PgeyShot("frente", PgeyViewPreset.FRONT, HepyGarment.KIT, 1200, 1600),
                    new PgeyShot("espalda", PgeyViewPreset.BACK, HepyGarment.KIT, 1200, 1600),
                    new PgeyShot("detalle-frente", PgeyViewPreset.FRONT, HepyGarment.JERSEY, 1400, 1100),
                    new PgeyShot("detalle-espalda", PgeyViewPreset.BACK, HepyGarment.JERSEY, 1400, 1100),
                    new PgeyShot("detalle-short-medias", PgeyViewPreset.FRONT, HepyGarment.SHORTS, 1400, 1100),
                    new PgeyShot("detalle-zapatillas", PgeyViewPreset.LEFT, HepyGarment.SHOES, 1400, 1000));
            for (PgeyShot pgeyShot : pgeyShots) {
                WritableImage pgeyImg = pgeyOnFx(() -> {
                    PgeyViewer3D pgeyViewer = new PgeyViewer3D();
                    pgeyViewer.autoRotateProperty().set(false);
                    pgeyViewer.resize(pgeyShot.pgeyWidth(), pgeyShot.pgeyHeight());
                    Stage pgeyStage = new Stage();
                    pgeyStage.setScene(new Scene(pgeyViewer, pgeyShot.pgeyWidth(), pgeyShot.pgeyHeight()));
                    com.sportforce3d.app.PpqhFonts.ppqhLoadAll();
                    pgeyViewer.setModel(pgeyModel);
                    new PgeyDesignRenderer(pgeyViewer, new PgeyTextureComposer(new PgeyElementImages(null))).pgeyApplyAll(pgeyDoc, pgeyMaterials);
                    pgeyViewer.pgeyJumpTo(pgeyShot.pgeyView(), pgeyShot.pgeyFocus());
                    pgeyViewer.layout();
                    WritableImage pgeyImage = pgeyViewer.pgeyCapture(pgeyShot.pgeyWidth());
                    pgeyStage.close();
                    return pgeyImage;
                });
                PgeyPng.pgeyWrite(pgeyImg, pgeyOut.resolve(pgeyShot.pgeyName() + ".png"));
                System.out.println("Captura: " + pgeyOut.resolve(pgeyShot.pgeyName() + ".png").toAbsolutePath());
            }
        } finally {
            Platform.exit();
        }
    }

    private static void pgeyCapturePersons(Path pgeyOut) throws Exception {
        CountDownLatch pgeyStarted = new CountDownLatch(1);
        Platform.startup(pgeyStarted::countDown);
        pgeyStarted.await();
        try {
            for (com.sportforce3d.three.human.PgeyPersona pgeyPersona : com.sportforce3d.three.human.PgeyPersona.values()) {
                com.sportforce3d.three.human.PgeyHumanModel pgeyHuman = com.sportforce3d.three.human.PgeyHumanModels.pgeyLoad(pgeyPersona);
                List<PgeyShot> pgeyShots = List.of(
                        new PgeyShot(pgeyPersona.pgeyKey() + "-frente", PgeyViewPreset.FRONT, HepyGarment.KIT, 1200, 1600),
                        new PgeyShot(pgeyPersona.pgeyKey() + "-espalda", PgeyViewPreset.BACK, HepyGarment.KIT, 1200, 1600),
                        new PgeyShot(pgeyPersona.pgeyKey() + "-costado", PgeyViewPreset.RIGHT, HepyGarment.KIT, 1200, 1600),
                        new PgeyShot(pgeyPersona.pgeyKey() + "-cara", PgeyViewPreset.FRONT, null, 1200, 1000));
                for (PgeyShot pgeyShot : pgeyShots) {
                    WritableImage pgeyImg = pgeyOnFx(() -> {
                        PgeyViewer3D pgeyViewer = new PgeyViewer3D();
                        pgeyViewer.autoRotateProperty().set(false);
                        pgeyViewer.resize(pgeyShot.pgeyWidth(), pgeyShot.pgeyHeight());
                        Stage pgeyStage = new Stage();
                        pgeyStage.setScene(new Scene(pgeyViewer, pgeyShot.pgeyWidth(), pgeyShot.pgeyHeight()));
                        pgeyViewer.setPerson(pgeyHuman);
                        pgeyViewer.pgeyJumpTo(pgeyShot.pgeyView(), HepyGarment.KIT);
                        if (pgeyShot.pgeyFocus() == null) {
                            pgeyViewer.pgeyJumpToFace();
                        }
                        pgeyViewer.layout();
                        WritableImage pgeyImage = pgeyViewer.pgeyCapture(pgeyShot.pgeyWidth());
                        pgeyStage.close();
                        return pgeyImage;
                    });
                    PgeyPng.pgeyWrite(pgeyImg, pgeyOut.resolve(pgeyShot.pgeyName() + ".png"));
                    System.out.println("Captura: " + pgeyOut.resolve(pgeyShot.pgeyName() + ".png").toAbsolutePath());
                }
            }
        } finally {
            Platform.exit();
        }
    }

    private static void pgeyCaptureDressed(Path pgeyOut, boolean pgeyDefaults) throws Exception {
        java.util.Map<String, com.sportforce3d.model.HepyMaterial> pgeyMaterials = pgeyMaterials();
        CountDownLatch pgeyStarted = new CountDownLatch(1);
        Platform.startup(pgeyStarted::countDown);
        pgeyStarted.await();
        try {
            for (com.sportforce3d.three.human.PgeyPersona pgeyPersona : com.sportforce3d.three.human.PgeyPersona.values()) {
                var pgeyDressed = com.sportforce3d.three.human.PgeyHumanModels.pgeyDress(pgeyPersona);
                var pgeyDoc = pgeyDefaults ? com.sportforce3d.design.PgeyDesignDefaults.pgeyDocument(pgeyPersona.pgeyFallback())
                        : com.sportforce3d.design.PgeySampleDesigns.pgeyForPerson(pgeyPersona.ordinal());
                List<PgeyShot> pgeyShots = pgeyDefaults ? List.of(
                        new PgeyShot("defecto-" + pgeyPersona.pgeyKey() + "-frente", PgeyViewPreset.FRONT, HepyGarment.KIT, 1200, 1600),
                        new PgeyShot("defecto-" + pgeyPersona.pgeyKey() + "-espalda", PgeyViewPreset.BACK, HepyGarment.KIT, 1200, 1600),
                        new PgeyShot("defecto-" + pgeyPersona.pgeyKey() + "-espalda-detalle", PgeyViewPreset.BACK, HepyGarment.JERSEY, 1400, 1100))
                        : List.of(
                        new PgeyShot("uniforme-" + pgeyPersona.pgeyKey() + "-frente", PgeyViewPreset.FRONT, HepyGarment.KIT, 1200, 1600),
                        new PgeyShot("uniforme-" + pgeyPersona.pgeyKey() + "-espalda", PgeyViewPreset.BACK, HepyGarment.KIT, 1200, 1600),
                        new PgeyShot("uniforme-" + pgeyPersona.pgeyKey() + "-costado", PgeyViewPreset.RIGHT, HepyGarment.KIT, 1200, 1600),
                        new PgeyShot("uniforme-" + pgeyPersona.pgeyKey() + "-camiseta", PgeyViewPreset.FRONT, HepyGarment.JERSEY, 1400, 1100),
                        new PgeyShot("uniforme-" + pgeyPersona.pgeyKey() + "-zapatillas", PgeyViewPreset.LEFT, HepyGarment.SHOES, 1400, 1000));
                pgeyRenderDressed(pgeyDressed, pgeyDoc, pgeyMaterials, pgeyShots, pgeyOut);
            }
        } finally {
            Platform.exit();
        }
    }

    private static void pgeyCaptureSimulated(Path pgeyOut) throws Exception {
        java.util.Map<String, com.sportforce3d.model.HepyMaterial> pgeyMaterials = pgeyMaterials();
        CountDownLatch pgeyStarted = new CountDownLatch(1);
        Platform.startup(pgeyStarted::countDown);
        pgeyStarted.await();
        try {
            for (var pgeyPersona : com.sportforce3d.three.human.PgeyPersona.values()) {
                var pgeyDressed = com.sportforce3d.three.human.PgeyHumanModels.pgeyDress(pgeyPersona);
                String pgeyP = pgeyPersona.pgeyKey() + "-";
                pgeyRenderDressed(pgeyDressed, com.sportforce3d.design.PgeySampleDesigns.pgeyForPerson(pgeyPersona.ordinal()), pgeyMaterials,
                        List.of(new PgeyShot(pgeyP + "frente", PgeyViewPreset.FRONT, HepyGarment.KIT, 1200, 1600),
                                new PgeyShot(pgeyP + "espalda", PgeyViewPreset.BACK, HepyGarment.KIT, 1200, 1600),
                                new PgeyShot(pgeyP + "costado", PgeyViewPreset.RIGHT, HepyGarment.KIT, 1200, 1600),
                                new PgeyShot(pgeyP + "costado-izquierdo", PgeyViewPreset.LEFT, HepyGarment.KIT, 1200, 1600),
                                new PgeyShot(pgeyP + "camiseta", PgeyViewPreset.FRONT, HepyGarment.JERSEY, 1400, 1100),
                                new PgeyShot(pgeyP + "camiseta-espalda", PgeyViewPreset.BACK, HepyGarment.JERSEY, 1400, 1100)), pgeyOut);
                pgeyRenderDressed(pgeyDressed, com.sportforce3d.design.PgeyDesignDefaults.pgeyDocument(pgeyPersona.pgeyFallback()), pgeyMaterials,
                        List.of(new PgeyShot(pgeyP + "defecto-frente", PgeyViewPreset.FRONT, HepyGarment.KIT, 1200, 1600)), pgeyOut);
            }
        } finally {
            Platform.exit();
        }
    }

    private static void pgeyRenderDressed(com.sportforce3d.three.human.PgeyOutfitFitter.PgeyDressedPerson pgeyDressed,
                                      com.sportforce3d.design.PgeyDesignDocument pgeyDoc,
                                      java.util.Map<String, com.sportforce3d.model.HepyMaterial> pgeyMaterials,
                                      List<PgeyShot> pgeyShots, Path pgeyOut) throws Exception {
        PgeyViewer3D pgeyViewer = pgeyOnFx(() -> {
            PgeyViewer3D pgeyV = new PgeyViewer3D();
            pgeyV.autoRotateProperty().set(false);
            Stage pgeyStage = new Stage();
            pgeyStage.setScene(new Scene(pgeyV, 1200, 1600));
            com.sportforce3d.app.PpqhFonts.ppqhLoadAll();
            pgeyV.setDressedPerson(pgeyDressed);
            new PgeyDesignRenderer(pgeyV, new PgeyTextureComposer(new PgeyElementImages(null))).pgeyApplyAll(pgeyDoc, pgeyMaterials);
            return pgeyV;
        });
        for (PgeyShot pgeyShot : pgeyShots) {
            WritableImage pgeyImg = pgeyOnFx(() -> {
                Stage pgeyStage = (Stage) pgeyViewer.getScene().getWindow();
                pgeyStage.setWidth(pgeyShot.pgeyWidth());
                pgeyStage.setHeight(pgeyShot.pgeyHeight());
                pgeyViewer.resize(pgeyShot.pgeyWidth(), pgeyShot.pgeyHeight());
                pgeyViewer.pgeyJumpTo(pgeyShot.pgeyView(), pgeyShot.pgeyFocus());
                pgeyViewer.layout();
                return pgeyViewer.pgeyCapture(pgeyShot.pgeyWidth());
            });
            PgeyPng.pgeyWrite(pgeyImg, pgeyOut.resolve(pgeyShot.pgeyName() + ".png"));
            System.out.println("Captura: " + pgeyOut.resolve(pgeyShot.pgeyName() + ".png").toAbsolutePath());
        }
        pgeyOnFx(() -> {
            Stage pgeyStage = (Stage) pgeyViewer.getScene().getWindow();
            pgeyStage.getScene().setRoot(new javafx.scene.Group());
            pgeyStage.close();
            return null;
        });
    }

    private static java.util.Map<String, com.sportforce3d.model.HepyMaterial> pgeyMaterials() throws Exception {
        java.nio.file.Path pgeyTmp = java.nio.file.Files.createTempDirectory("sf-capturas");
        com.sportforce3d.db.HepyDatabase pgeyDb = new com.sportforce3d.db.HepyDatabase(pgeyTmp.resolve("tmp.db"));
        pgeyDb.hepyMigrate();
        var pgeyCatalog = new com.sportforce3d.repository.HepyCatalogRepository(pgeyDb);
        java.util.Map<String, com.sportforce3d.model.HepyMaterial> pgeyMaterials = new java.util.HashMap<>();
        for (var pgeyScope : com.sportforce3d.model.HepyMaterial.HepyScope.values()) {
            pgeyCatalog.hepyFindMaterials(pgeyScope).forEach(pgeyM -> pgeyMaterials.put(pgeyM.hepyCode(), pgeyM));
        }
        return pgeyMaterials;
    }

    private static WritableImage pgeyRender(PgeyModelData pgeyModel, PgeyShot pgeyShot) {
        PgeyViewer3D pgeyViewer = new PgeyViewer3D();
        pgeyViewer.autoRotateProperty().set(false);
        pgeyViewer.resize(pgeyShot.pgeyWidth(), pgeyShot.pgeyHeight());
        Stage pgeyStage = new Stage();
        pgeyStage.setScene(new Scene(pgeyViewer, pgeyShot.pgeyWidth(), pgeyShot.pgeyHeight()));
        pgeyViewer.setModel(pgeyModel);
        pgeyViewer.pgeyJumpTo(pgeyShot.pgeyView(), pgeyShot.pgeyFocus());
        pgeyViewer.layout();
        WritableImage pgeyImg = pgeyViewer.pgeyCapture(pgeyShot.pgeyWidth());
        pgeyStage.close();
        return pgeyImg;
    }

    private static <T> T pgeyOnFx(java.util.concurrent.Callable<T> pgeyWork) throws Exception {
        CompletableFuture<T> pgeyF = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                pgeyF.complete(pgeyWork.call());
            } catch (Throwable pgeyT) {
                pgeyF.completeExceptionally(pgeyT);
            }
        });
        return pgeyF.get(120, TimeUnit.SECONDS);
    }
}
