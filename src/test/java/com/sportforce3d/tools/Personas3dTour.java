package com.sportforce3d.tools;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Fonts;
import com.sportforce3d.app.Routes;
import com.sportforce3d.app.SportForceApp;
import com.sportforce3d.app.UiSettings;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.config.AppConfig;
import com.sportforce3d.db.Database;
import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.SampleDesigns;
import com.sportforce3d.model.Garment;
import com.sportforce3d.three.Png;
import com.sportforce3d.three.ViewPreset;
import com.sportforce3d.three.Viewer3D;
import com.sportforce3d.three.human.Persona;
import com.sportforce3d.viewmodel.DesignerViewModel;
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

/**
 * Capturas de la Vista 3D del diseñador con las cuatro personas vestidas (frente y espalda), en la ventana real de la
 * aplicación sobre una base temporal. Sirve para revisar las personas de MakeHuman y las prendas simuladas en Blender
 * tras regenerarlas. Imprime si se vio la persona o el maniquí de respaldo.
 * Uso: {@code Personas3dTour [carpeta]} (por defecto docs/capturas/reconstruccion).
 */
public final class Personas3dTour {

    private static final int W = 1440;
    private static final int H = 900;
    private static final String[] MODELS = {"futbol_hombre_frente", "futbol_mujer_frente", "futbol_nino_frente",
            "futbol_nina_frente"};

    private final Path out;
    private AppContext context;
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
        Database.deleteFiles(data.resolve("tour.db"));
        fx(() -> {
            Fonts.loadAll();
            UiSettings settings = new UiSettings(Preferences.userRoot().node("com/sportforce3d-tour"));
            settings.reduceMotionProperty().set(true);
            context = new AppContext(new AppConfig(data.resolve("tour.db")), settings);
            context.database().migrate();
            stage = new Stage();
            SportForceApp.openMainWindow(stage, context);
            stage.setWidth(W);
            stage.setHeight(H);
            stage.show();
            return null;
        });
        pause(800);
        login();
        go(Routes.DESIGNER, null);
        fx(() -> {
            toggle("Vista 3D").fire();
            return null;
        });
        waitFor(() -> viewer().getModel() != null, 60_000);
        for (int i = 0; i < MODELS.length; i++) {
            int k = i;
            fx(() -> {
                DesignDocument d = SampleDesigns.forPerson(k);
                vm().edit("Persona", x -> d.withRealistic(d.realisticOrDefault().withModel(MODELS[k])), null);
                vm().seal();
                return null;
            });
            waitDressed();
            Persona p = fx(() -> vm().personaProperty().get());
            boolean fallback = fx(() -> vm().personFallbackProperty().get());
            System.out.println("Persona " + p.key() + (fallback ? ": MANIQUÍ DE RESPALDO" : ": persona 3D vestida"));
            fx(() -> {
                viewer().jumpTo(ViewPreset.FRONT, Garment.KIT);
                return null;
            });
            pause(1200);
            shot("vista3d-" + p.key() + "-frente");
            fx(() -> {
                viewer().jumpTo(ViewPreset.BACK, Garment.KIT);
                return null;
            });
            pause(1200);
            shot("vista3d-" + p.key() + "-espalda");
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

    private Viewer3D viewer() {
        return (Viewer3D) stage.getScene().getRoot().lookup(".sf-viewer3d");
    }

    private DesignerViewModel vm() {
        Node n = find(stage.getScene().getRoot(), x -> x.getProperties().containsKey(DesignerViewModel.class));
        if (n == null) {
            throw new IllegalStateException("No está abierto el diseñador");
        }
        return (DesignerViewModel) n.getProperties().get(DesignerViewModel.class);
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
            if (!context.session().isAuthenticated()) {
                context.navigator().showLogin();
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
            context.router().go(route, param);
            return null;
        });
        pause(1300);
    }

    private void shot(String name) throws Exception {
        WritableImage img = fx(() -> stage.getScene().snapshot(null));
        Path file = out.resolve(name + ".png");
        Png.write(img, file);
        System.out.println("Captura: " + file);
    }

    private SfTextField field(String label) {
        for (Node n : stage.getScene().getRoot().lookupAll(".sf-field")) {
            if (n instanceof SfTextField f && f.isVisible() && f.lookupAll(".sf-field-label").stream()
                    .anyMatch(l -> l instanceof Labeled ll && ll.getText().equalsIgnoreCase(label))) {
                return f;
            }
        }
        throw new IllegalStateException("No hay campo " + label);
    }

    private SfButton button(String text) {
        Node found = find(stage.getScene().getRoot(), x -> x instanceof SfButton b && text.equalsIgnoreCase(b.getText()));
        if (found == null) {
            throw new IllegalStateException("No hay botón " + text);
        }
        return (SfButton) found;
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
