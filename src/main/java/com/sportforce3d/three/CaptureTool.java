package com.sportforce3d.three;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.procedural.ProceduralModelProvider;
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

/**
 * Genera las capturas de referencia del visor en docs/capturas (frente, espalda, lateral y detalles) con
 * SubScene.snapshot. Uso: {@code mvnw compile exec} o la configuración "Capturas 3D" de IntelliJ.
 * Argumentos opcionales: carpeta de salida y tipo de cuerpo (ADULTO_HOMBRE, ADULTO_MUJER, JUVENIL o ALL), ETAPA4
 * (uniforme de ejemplo), PERSONAS (las personas 3D de la Etapa 8), UNIFORMES / DEFECTO (Etapa 8B) o TELA (Etapa 9A:
 * la tela simulada en Blender).
 */
public final class CaptureTool {

    private record Shot(String name, ViewPreset view, Garment focus, int width, int height) {
    }

    private CaptureTool() {
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/capturas");
        String which = args.length > 1 ? args[1] : "ADULTO_HOMBRE";
        if (which.equals("ETAPA4")) {
            captureSampleDesign(out);
            return;
        }
        if (which.equals("PERSONAS")) {
            capturePersons(out);
            return;
        }
        if (which.equals("UNIFORMES") || which.equals("DEFECTO")) {
            captureDressed(out, which.equals("DEFECTO"));
            return;
        }
        if (which.equals("TELA")) {
            captureSimulated(out);
            return;
        }
        List<BodyType> types = which.equals("ALL") ? List.of(BodyType.values()) : List.of(BodyType.valueOf(which));

        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            for (BodyType type : types) {
                ModelData model = new ProceduralModelProvider().load(type);
                String prefix = type == BodyType.ADULTO_HOMBRE ? "" : type.key() + "-";
                List<Shot> shots = new ArrayList<>(List.of(
                        new Shot(prefix + "frente", ViewPreset.FRONT, Garment.KIT, 1200, 1600),
                        new Shot(prefix + "espalda", ViewPreset.BACK, Garment.KIT, 1200, 1600),
                        new Shot(prefix + "lateral", ViewPreset.LEFT, Garment.KIT, 1200, 1600)));
                if (type == BodyType.ADULTO_HOMBRE) {
                    shots.add(new Shot("detalle-camiseta", ViewPreset.FRONT, Garment.JERSEY, 1400, 1100));
                    shots.add(new Shot("detalle-zapatillas", ViewPreset.LEFT, Garment.SHOES, 1400, 1000));
                }
                for (Shot shot : shots) {
                    WritableImage img = onFx(() -> render(model, shot));
                    Png.write(img, out.resolve(shot.name() + ".png"));
                    System.out.println("Captura: " + out.resolve(shot.name() + ".png").toAbsolutePath());
                }
            }
        } finally {
            Platform.exit();
        }
    }

    /** Uniforme de ejemplo de la Etapa 4 (colores, franjas, nombre, número y emblema): frente, espalda y detalles. */
    private static void captureSampleDesign(Path out) throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("sf-capturas");
        com.sportforce3d.db.Database db = new com.sportforce3d.db.Database(tmp.resolve("tmp.db"));
        db.migrate();
        var catalog = new com.sportforce3d.repository.CatalogRepository(db);
        java.util.Map<String, com.sportforce3d.model.Material> materials = new java.util.HashMap<>();
        for (var scope : com.sportforce3d.model.Material.Scope.values()) {
            catalog.findMaterials(scope).forEach(m -> materials.put(m.code(), m));
        }
        var doc = com.sportforce3d.design.SampleDesigns.halcones();
        ModelData model = new ProceduralModelProvider().load(doc.bodyType());
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            List<Shot> shots = List.of(
                    new Shot("frente", ViewPreset.FRONT, Garment.KIT, 1200, 1600),
                    new Shot("espalda", ViewPreset.BACK, Garment.KIT, 1200, 1600),
                    new Shot("detalle-frente", ViewPreset.FRONT, Garment.JERSEY, 1400, 1100),
                    new Shot("detalle-espalda", ViewPreset.BACK, Garment.JERSEY, 1400, 1100),
                    new Shot("detalle-short-medias", ViewPreset.FRONT, Garment.SHORTS, 1400, 1100),
                    new Shot("detalle-zapatillas", ViewPreset.LEFT, Garment.SHOES, 1400, 1000));
            for (Shot shot : shots) {
                WritableImage img = onFx(() -> {
                    Viewer3D viewer = new Viewer3D();
                    viewer.autoRotateProperty().set(false);
                    viewer.resize(shot.width(), shot.height());
                    Stage stage = new Stage();
                    stage.setScene(new Scene(viewer, shot.width(), shot.height()));
                    com.sportforce3d.app.Fonts.loadAll();
                    viewer.setModel(model);
                    new DesignRenderer(viewer, new TextureComposer(new ElementImages(null))).applyAll(doc, materials);
                    viewer.jumpTo(shot.view(), shot.focus());
                    viewer.layout();
                    WritableImage image = viewer.capture(shot.width());
                    stage.close();
                    return image;
                });
                Png.write(img, out.resolve(shot.name() + ".png"));
                System.out.println("Captura: " + out.resolve(shot.name() + ".png").toAbsolutePath());
            }
        } finally {
            Platform.exit();
        }
    }

    /** Etapa 8: las cuatro personas 3D (sin uniforme) de frente, espalda, costado y un detalle de la cara. */
    private static void capturePersons(Path out) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            for (com.sportforce3d.three.human.Persona persona : com.sportforce3d.three.human.Persona.values()) {
                com.sportforce3d.three.human.HumanModel human = com.sportforce3d.three.human.HumanModels.load(persona);
                List<Shot> shots = List.of(
                        new Shot(persona.key() + "-frente", ViewPreset.FRONT, Garment.KIT, 1200, 1600),
                        new Shot(persona.key() + "-espalda", ViewPreset.BACK, Garment.KIT, 1200, 1600),
                        new Shot(persona.key() + "-costado", ViewPreset.RIGHT, Garment.KIT, 1200, 1600),
                        new Shot(persona.key() + "-cara", ViewPreset.FRONT, null, 1200, 1000));
                for (Shot shot : shots) {
                    WritableImage img = onFx(() -> {
                        Viewer3D viewer = new Viewer3D();
                        viewer.autoRotateProperty().set(false);
                        viewer.resize(shot.width(), shot.height());
                        Stage stage = new Stage();
                        stage.setScene(new Scene(viewer, shot.width(), shot.height()));
                        viewer.setPerson(human);
                        viewer.jumpTo(shot.view(), Garment.KIT);
                        if (shot.focus() == null) {
                            viewer.jumpToFace();
                        }
                        viewer.layout();
                        WritableImage image = viewer.capture(shot.width());
                        stage.close();
                        return image;
                    });
                    Png.write(img, out.resolve(shot.name() + ".png"));
                    System.out.println("Captura: " + out.resolve(shot.name() + ".png").toAbsolutePath());
                }
            }
        } finally {
            Platform.exit();
        }
    }

    /**
     * Etapa 8B: cada persona con un uniforme distinto ({@code SampleDesigns.forPersons}) puesto: frente, espalda,
     * costado y detalle de la camiseta. Con {@code defaults}, el diseño con que abre el diseñador (prefijo
     * "defecto-"): frente, espalda y detalle de la espalda.
     */
    private static void captureDressed(Path out, boolean defaults) throws Exception {
        java.util.Map<String, com.sportforce3d.model.Material> materials = materials();
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            for (com.sportforce3d.three.human.Persona persona : com.sportforce3d.three.human.Persona.values()) {
                var dressed = com.sportforce3d.three.human.HumanModels.dress(persona);
                var doc = defaults ? com.sportforce3d.design.DesignDefaults.document(persona.fallback())
                        : com.sportforce3d.design.SampleDesigns.forPerson(persona.ordinal());
                List<Shot> shots = defaults ? List.of(
                        new Shot("defecto-" + persona.key() + "-frente", ViewPreset.FRONT, Garment.KIT, 1200, 1600),
                        new Shot("defecto-" + persona.key() + "-espalda", ViewPreset.BACK, Garment.KIT, 1200, 1600),
                        new Shot("defecto-" + persona.key() + "-espalda-detalle", ViewPreset.BACK, Garment.JERSEY, 1400, 1100))
                        : List.of(
                        new Shot("uniforme-" + persona.key() + "-frente", ViewPreset.FRONT, Garment.KIT, 1200, 1600),
                        new Shot("uniforme-" + persona.key() + "-espalda", ViewPreset.BACK, Garment.KIT, 1200, 1600),
                        new Shot("uniforme-" + persona.key() + "-costado", ViewPreset.RIGHT, Garment.KIT, 1200, 1600),
                        new Shot("uniforme-" + persona.key() + "-camiseta", ViewPreset.FRONT, Garment.JERSEY, 1400, 1100),
                        new Shot("uniforme-" + persona.key() + "-zapatillas", ViewPreset.LEFT, Garment.SHOES, 1400, 1000));
                renderDressed(dressed, doc, materials, shots, out);
            }
        } finally {
            Platform.exit();
        }
    }

    /**
     * Etapa 9: cada persona con la camiseta, el short y las medias simulados en Blender y su uniforme de ejemplo:
     * frente, espalda, ambos costados (la franja lateral debe verse igual) y detalles de la camiseta; además el frente
     * con el diseño con que abre el diseñador ("-defecto-frente").
     */
    private static void captureSimulated(Path out) throws Exception {
        java.util.Map<String, com.sportforce3d.model.Material> materials = materials();
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            for (var persona : com.sportforce3d.three.human.Persona.values()) {
                var dressed = com.sportforce3d.three.human.HumanModels.dress(persona);
                String p = persona.key() + "-";
                renderDressed(dressed, com.sportforce3d.design.SampleDesigns.forPerson(persona.ordinal()), materials,
                        List.of(new Shot(p + "frente", ViewPreset.FRONT, Garment.KIT, 1200, 1600),
                                new Shot(p + "espalda", ViewPreset.BACK, Garment.KIT, 1200, 1600),
                                new Shot(p + "costado", ViewPreset.RIGHT, Garment.KIT, 1200, 1600),
                                new Shot(p + "costado-izquierdo", ViewPreset.LEFT, Garment.KIT, 1200, 1600),
                                new Shot(p + "camiseta", ViewPreset.FRONT, Garment.JERSEY, 1400, 1100),
                                new Shot(p + "camiseta-espalda", ViewPreset.BACK, Garment.JERSEY, 1400, 1100)), out);
                renderDressed(dressed, com.sportforce3d.design.DesignDefaults.document(persona.fallback()), materials,
                        List.of(new Shot(p + "defecto-frente", ViewPreset.FRONT, Garment.KIT, 1200, 1600)), out);
            }
        } finally {
            Platform.exit();
        }
    }

    /** Un visor por persona: crear uno por toma agota la memoria de video de Prism (no se libera a tiempo). */
    private static void renderDressed(com.sportforce3d.three.human.OutfitFitter.DressedPerson dressed,
                                      com.sportforce3d.design.DesignDocument doc,
                                      java.util.Map<String, com.sportforce3d.model.Material> materials,
                                      List<Shot> shots, Path out) throws Exception {
        Viewer3D viewer = onFx(() -> {
            Viewer3D v = new Viewer3D();
            v.autoRotateProperty().set(false);
            Stage stage = new Stage();
            stage.setScene(new Scene(v, 1200, 1600));
            com.sportforce3d.app.Fonts.loadAll();
            v.setDressedPerson(dressed);
            new DesignRenderer(v, new TextureComposer(new ElementImages(null))).applyAll(doc, materials);
            return v;
        });
        for (Shot shot : shots) {
            WritableImage img = onFx(() -> {
                Stage stage = (Stage) viewer.getScene().getWindow();
                stage.setWidth(shot.width());
                stage.setHeight(shot.height());
                viewer.resize(shot.width(), shot.height());
                viewer.jumpTo(shot.view(), shot.focus());
                viewer.layout();
                return viewer.capture(shot.width());
            });
            Png.write(img, out.resolve(shot.name() + ".png"));
            System.out.println("Captura: " + out.resolve(shot.name() + ".png").toAbsolutePath());
        }
        onFx(() -> {
            Stage stage = (Stage) viewer.getScene().getWindow();
            // Sin escena el visor detiene su AnimationTimer; si no, el temporizador lo retiene con sus texturas.
            stage.getScene().setRoot(new javafx.scene.Group());
            stage.close();
            return null;
        });
    }

    /** Materiales del catálogo (una base temporal con las migraciones). */
    private static java.util.Map<String, com.sportforce3d.model.Material> materials() throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("sf-capturas");
        com.sportforce3d.db.Database db = new com.sportforce3d.db.Database(tmp.resolve("tmp.db"));
        db.migrate();
        var catalog = new com.sportforce3d.repository.CatalogRepository(db);
        java.util.Map<String, com.sportforce3d.model.Material> materials = new java.util.HashMap<>();
        for (var scope : com.sportforce3d.model.Material.Scope.values()) {
            catalog.findMaterials(scope).forEach(m -> materials.put(m.code(), m));
        }
        return materials;
    }

    private static WritableImage render(ModelData model, Shot shot) {
        Viewer3D viewer = new Viewer3D();
        viewer.autoRotateProperty().set(false);
        viewer.resize(shot.width(), shot.height());
        Stage stage = new Stage();
        stage.setScene(new Scene(viewer, shot.width(), shot.height()));
        viewer.setModel(model);
        viewer.jumpTo(shot.view(), shot.focus());
        viewer.layout();
        WritableImage img = viewer.capture(shot.width());
        stage.close();
        return img;
    }

    private static <T> T onFx(java.util.concurrent.Callable<T> work) throws Exception {
        CompletableFuture<T> f = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                f.complete(work.call());
            } catch (Throwable t) {
                f.completeExceptionally(t);
            }
        });
        return f.get(120, TimeUnit.SECONDS);
    }
}
