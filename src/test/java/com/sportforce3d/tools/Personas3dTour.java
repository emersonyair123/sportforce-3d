package com.sportforce3d.tools;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhFonts;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.app.PpqhSportForceApp;
import com.sportforce3d.app.PpqhUiSettings;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.config.PpqhAppConfig;
import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeySampleDesigns;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.PgeyPng;
import com.sportforce3d.three.PgeyViewPreset;
import com.sportforce3d.three.PgeyViewer3D;
import com.sportforce3d.three.human.PgeyPersona;
import com.sportforce3d.viewmodel.PgeyDesignerViewModel;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public final class Personas3dTour {

    private static final int W = 1440;
    private static final int H = 900;
    private static final String[] MODELS = {"futbol_hombre_frente", "futbol_mujer_frente", "futbol_nino_frente",
            "futbol_nina_frente"};

    private final Path out;
    private PpqhAppContext context;
    private Stage stage;

    private Personas3dTour(Path out) {
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/capturas/reconstruccion");
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            new Personas3dTour(out).run();
        } finally {
            Platform.exit();
        }
    }

    private void run() throws Exception {
        Files.createDirectories(out);
        Path data = Files.createTempDirectory("sf-personas3d");
        HepyDatabase.hepyDeleteFiles(data.resolve("tour.db"));
        fx(() -> {
            PpqhFonts.ppqhLoadAll();
            PpqhUiSettings settings = new PpqhUiSettings(Preferences.userRoot().node("com/sportforce3d-tour"));
            settings.reduceMotionProperty().set(true);
            context = new PpqhAppContext(new PpqhAppConfig(data.resolve("tour.db")), settings);
            context.ppqhDatabase().hepyMigrate();
            stage = new Stage();
            PpqhSportForceApp.ppqhOpenMainWindow(stage, context);
            stage.setWidth(W);
            stage.setHeight(H);
            stage.show();
            return null;
        });
        pause(800);
        login();
        go(PpqhRoutes.PPQH_DESIGNER, null);
        fx(() -> {
            toggle("Vista 3D").fire();
            return null;
        });
        waitFor(() -> viewer().getModel() != null, 60_000);
        for (int i = 0; i < MODELS.length; i++) {
            int k = i;
            fx(() -> {
                PgeyDesignDocument d = PgeySampleDesigns.pgeyForPerson(k);
                vm().pgeyEdit("Persona", x -> d.pgeyWithRealistic(d.pgeyRealisticOrDefault().pgeyWithModel(MODELS[k])), null);
                vm().pgeySeal();
                return null;
            });
            waitDressed();
            PgeyPersona p = fx(() -> vm().personaProperty().get());
            boolean fallback = fx(() -> vm().personFallbackProperty().get());
            System.out.println("Persona " + p.pgeyKey() + (fallback ? ": MANIQUÍ DE RESPALDO" : ": persona 3D vestida"));
            fx(() -> {
                viewer().pgeyJumpTo(PgeyViewPreset.FRONT, HepyGarment.KIT);
                return null;
            });
            pause(1200);
            shot("vista3d-" + p.pgeyKey() + "-frente");
            fx(() -> {
                viewer().pgeyJumpTo(PgeyViewPreset.BACK, HepyGarment.KIT);
                return null;
            });
            pause(1200);
            shot("vista3d-" + p.pgeyKey() + "-espalda");
        }
        fx(() -> {
            stage.close();
            return null;
        });
    }

    private void waitDressed() throws Exception {
        pause(600);
        waitFor(() -> viewer().getModel() != null && !vm().personLoadingProperty().get(), 120_000);
        pause(2000);
    }

    private static Node find(Node node, java.util.function.Predicate<Node> match) {
        if (match.test(node)) {
            return node;
        }
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                Node f = find(child, match);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    private PgeyViewer3D viewer() {
        return (PgeyViewer3D) stage.getScene().getRoot().lookup(".sf-viewer3d");
    }

    private PgeyDesignerViewModel vm() {
        Node n = find(stage.getScene().getRoot(), x -> x.getProperties().containsKey(PgeyDesignerViewModel.class));
        if (n == null) {
            throw new IllegalStateException("No está abierto el diseñador");
        }
        return (PgeyDesignerViewModel) n.getProperties().get(PgeyDesignerViewModel.class);
    }

    private ToggleButton toggle(String text) {
        Node n = find(stage.getScene().getRoot(), x -> x instanceof ToggleButton t && text.equals(t.getText())
                && t.isVisible());
        if (n == null) {
            throw new IllegalStateException("No hay pestaña " + text);
        }
        return (ToggleButton) n;
    }

    private void login() throws Exception {
        fx(() -> {
            if (!context.ppqhSession().ppqhIsAuthenticated()) {
                context.ppqhNavigator().ppqhShowLogin();
            }
            return null;
        });
        pause(300);
        fx(() -> {
            field("Correo").setText("cliente@sportforce3d.pe");
            field("Contraseña").setText("Cliente123!");
            button("Iniciar sesión").fire();
            return null;
        });
        pause(3500);
    }

    private void go(String route, Object param) throws Exception {
        fx(() -> {
            context.ppqhRouter().ppqhGo(route, param);
            return null;
        });
        pause(1300);
    }

    private void shot(String name) throws Exception {
        WritableImage img = fx(() -> stage.getScene().snapshot(null));
        Path file = out.resolve(name + ".png");
        PgeyPng.pgeyWrite(img, file);
        System.out.println("Captura: " + file);
    }

    private PpqhSfTextField field(String label) {
        for (Node n : stage.getScene().getRoot().lookupAll(".sf-field")) {
            if (n instanceof PpqhSfTextField f && f.isVisible() && f.lookupAll(".sf-field-label").stream()
                    .anyMatch(l -> l instanceof Labeled ll && ll.getText().equalsIgnoreCase(label))) {
                return f;
            }
        }
        throw new IllegalStateException("No hay campo " + label);
    }

    private PpqhSfButton button(String text) {
        Node found = find(stage.getScene().getRoot(), x -> x instanceof PpqhSfButton b && text.equalsIgnoreCase(b.getText()));
        if (found == null) {
            throw new IllegalStateException("No hay botón " + text);
        }
        return (PpqhSfButton) found;
    }

    private static void waitFor(Callable<Boolean> condition, long maxMs) throws Exception {
        long end = System.currentTimeMillis() + maxMs;
        while (!fx(condition)) {
            if (System.currentTimeMillis() > end) {
                throw new IllegalStateException("Tiempo agotado");
            }
            Thread.sleep(200);
        }
    }

    private static void pause(long ms) throws InterruptedException {
        Thread.sleep(ms);
        CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(l::countDown);
        l.await(5, TimeUnit.SECONDS);
    }

    private static <T> T fx(Callable<T> work) throws Exception {
        CompletableFuture<T> f = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                f.complete(work.call());
            } catch (Throwable e) {
                f.completeExceptionally(e);
            }
        });
        return f.get(30, TimeUnit.SECONDS);
    }
}
