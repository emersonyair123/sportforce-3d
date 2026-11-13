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
import com.sportforce3d.design.PgeyGarmentCut;
import com.sportforce3d.design.PgeyGarmentCut.PgeyShortsLength;
import com.sportforce3d.design.PgeyGarmentCut.PgeySleeve;
import com.sportforce3d.design.PgeyGarmentCut.PgeySocksLength;
import com.sportforce3d.design.PgeySampleDesigns;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.three.PgeyPng;
import com.sportforce3d.three.PgeyViewPreset;
import com.sportforce3d.three.PgeyViewer3D;
import com.sportforce3d.viewmodel.PgeyDesignerViewModel;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public final class Etapa11Tour {

    private static final int W = 1440;
    private static final int H = 900;
    private static final String DESIGN_NAME = "Halcones Etapa 11";

    private final Path out;
    private PpqhAppContext context;
    private Stage stage;

    private Etapa11Tour(Path out) {
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/capturas/etapa11");
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            new Etapa11Tour(out).run();
        } finally {
            Platform.exit();
        }
    }

    private void run() throws Exception {
        Files.createDirectories(out);
        Path data = Files.createTempDirectory("sf-etapa11");
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
        loginAs("cliente@sportforce3d.pe", "Cliente123!");
        go(PpqhRoutes.PPQH_DESIGNER, null);
        fx(() -> {
            toggle("Vista 3D").fire();
            return null;
        });
        waitFor(() -> viewer().getModel() != null, 60_000);
        pause(1500);
        sameDesign();
        realisticToThreeD();
        notices();
        orderAndAdmin();
        fx(() -> {
            stage.close();
            return null;
        });
    }

    private void sameDesign() throws Exception {
        fx(() -> {
            PgeyDesignDocument d = PgeySampleDesigns.pgeyHalcones();
            vm().pgeyEdit("Diseño", x -> d.pgeyWithRealistic(d.pgeyRealisticOrDefault().pgeyWithModel("futbol_hombre_frente")), null);
            vm().pgeySeal();
            return null;
        });
        waitDressed();
        shot("11-01-3d-diseno-frente");
        fx(() -> {
            viewer().pgeyJumpTo(PgeyViewPreset.BACK, HepyGarment.KIT);
            return null;
        });
        pause(1000);
        shot("11-02-3d-diseno-espalda");
        fx(() -> {
            toggle("Vista realista").fire();
            return null;
        });
        pause(3000);
        shot("11-03-realista-mismo-diseno");
    }

    private void realisticToThreeD() throws Exception {
        fx(() -> {
            vm().pgeyEditRealistic("Colores", s -> s.pgeyWithColor("camiseta", "#D7141E").pgeyWithColor("short", "#0B0E13")
                    .pgeyWithName("QUISPE").pgeyWithNumber(9), null);
            vm().pgeySeal();
            return null;
        });
        pause(3000);
        shot("11-04-realista-cambiado");
        fx(() -> {
            toggle("Vista 3D").fire();
            return null;
        });
        waitDressed();
        fx(() -> {
            viewer().pgeyJumpTo(PgeyViewPreset.FRONT, HepyGarment.KIT);
            return null;
        });
        pause(1000);
        shot("11-05-3d-tras-cambiar-en-realista-frente");
        fx(() -> {
            viewer().pgeyJumpTo(PgeyViewPreset.BACK, HepyGarment.KIT);
            return null;
        });
        pause(1000);
        shot("11-06-3d-tras-cambiar-en-realista-espalda-textos");
    }

    private void notices() throws Exception {
        PgeyGarmentCut cut = new PgeyGarmentCut(PgeySleeve.LARGA, PgeyShortsLength.CORTO, PgeySocksLength.TOBILLERA);
        fx(() -> {
            toggle("Vista realista").fire();
            vm().pgeyEditRealistic("Categoría", s -> s.pgeyWithModel("futbol_mujer_frente"), null);
            vm().pgeyEdit("Corte", d -> d.pgeyWithCut(cut), null);
            vm().pgeySeal();
            return null;
        });
        pause(3000);
        shot("11-07-realista-mujer-avisos");
        fx(() -> {
            toggle("Vista 3D").fire();
            return null;
        });
        waitDressed();
        shot("11-08-3d-mujer-manga-larga-short-corto-tobilleras");
        fx(() -> {
            toggle("Vista realista").fire();
            vm().pgeyEdit("Corte", d -> d.pgeyWithCut(PgeyGarmentCut.PGEY_DEFAULT), null);
            vm().pgeyEditRealistic("Modelo", s -> s.pgeyWithModel("polo_hombre_frente"), null);
            vm().pgeySeal();
            return null;
        });
        pause(3000);
        shot("11-09-realista-polo");
        fx(() -> {
            toggle("Vista 3D").fire();
            return null;
        });
        waitDressed();
        shot("11-10-3d-polo-aviso");
    }

    private void orderAndAdmin() throws Exception {
        fx(() -> {
            toggle("Vista realista").fire();
            vm().pgeyEditRealistic("Modelo", s -> s.pgeyWithModel("futbol_hombre_frente"), null);
            vm().pgeySeal();
            return null;
        });
        pause(3000);
        shot("11-11-realista-antes-de-guardar");
        if (!com.sportforce3d.three.human.PgeyHumanModels.pgeyAvailable(com.sportforce3d.three.human.PgeyPersona.HOMBRE)) {
            System.out.println("Sin las personas 3D (models/humanos): se omiten las capturas 11-12 a 11-16.");
            return;
        }
        fx(() -> {
            button("Guardar diseño").fire();
            return null;
        });
        pause(800);
        fx(() -> {
            field("Nombre del diseño").getInput().setText(DESIGN_NAME);
            button("Guardar").fire();
            return null;
        });
        waitFor(() -> context.ppqhDesignService().pgeyMyDesigns().stream().anyMatch(d -> d.pgeyName().equals(DESIGN_NAME)), 120_000);
        pause(1000);
        long design = context.ppqhDesignService().pgeyMyDesigns().stream()
                .filter(d -> d.pgeyName().equals(DESIGN_NAME)).findFirst().orElseThrow().pgeyId();
        long address = context.ppqhAddressService().ppqhList().get(0).ppqhId();
        long variant = context.ppqhDatabase().hepyWithConnection(c -> {
            try (var ps = c.prepareStatement("SELECT id FROM product_variants WHERE sku = 'SF-UFU-001-M'");
                 var rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        });
        context.ppqhCartService().hepyAddDesign(design, variant, 1);
        long order = context.ppqhOrderService().ppqhPlaceOrder(address, PpqhPaymentMethod.YAPE, null).ppqhId();
        fx(() -> {
            context.ppqhNavigator().ppqhLogout();
            return null;
        });
        pause(500);
        loginAs("admin@sportforce3d.pe", "Admin123!");
        var line = context.ppqhAdminOrderService().ppqhOrder(order).ppqhItems().stream().filter(i -> i.ppqhHasDesign())
                .findFirst().orElseThrow();
        Files.copy(context.ppqhStorage().hepyResolve(line.ppqhDesignFrontPath()), out.resolve("11-12-foto-del-pedido-frente.png"),
                StandardCopyOption.REPLACE_EXISTING);
        if (line.ppqhDesignBackPath() != null) {
            Files.copy(context.ppqhStorage().hepyResolve(line.ppqhDesignBackPath()), out.resolve("11-13-foto-del-pedido-espalda.png"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("Fotos del pedido: " + line.ppqhDesignFrontPath() + ", " + line.ppqhDesignBackPath());
        go(PpqhRoutes.PPQH_ADMIN_DESIGN, line.ppqhId());
        pause(4000);
        fx(() -> {
            ToggleButton real = toggleOrNull("Vista realista");
            if (real != null) {
                real.fire();
            }
            return null;
        });
        pause(3000);
        shot("11-14-admin-vista-realista");
        fx(() -> {
            scrollTo("VISTA REALISTA");
            return null;
        });
        pause(800);
        shot("11-15-admin-ficha-vista-realista");
        fx(() -> {
            toggle("Vista 3D").fire();
            return null;
        });
        waitFor(() -> viewer() != null && viewer().getModel() != null, 90_000);
        pause(2500);
        shot("11-16-admin-vista-3d");
    }

    private void scrollTo(String title) {
        Node label = find(stage.getScene().getRoot(), x -> x instanceof Labeled l && title.equals(l.getText())
                && x.isVisible());
        if (label == null) {
            throw new IllegalStateException("No hay " + title);
        }
        for (Node p = label.getParent(); p != null; p = p.getParent()) {
            if (p instanceof ScrollPane sp) {
                double y = sp.getContent().sceneToLocal(label.localToScene(0, 0)).getY() - 30;
                sp.setVvalue(Math.max(0, Math.min(1, y / Math.max(1, sp.getContent().getLayoutBounds().getHeight()
                        - sp.getViewportBounds().getHeight()))));
                sp.layout();
                return;
            }
        }
    }

    private void waitDressed() throws Exception {
        pause(400);
        waitFor(() -> viewer().getModel() != null && !vm().personLoadingProperty().get(), 90_000);
        pause(1500);
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

    private ToggleButton toggleOrNull(String text) {
        Node n = find(stage.getScene().getRoot(), x -> x instanceof ToggleButton t && text.equals(t.getText())
                && t.isVisible());
        return (ToggleButton) n;
    }

    private ToggleButton toggle(String text) {
        ToggleButton t = toggleOrNull(text);
        if (t == null) {
            throw new IllegalStateException("No hay pestaña " + text);
        }
        return t;
    }

    private void loginAs(String email, String password) throws Exception {
        fx(() -> {
            if (!context.ppqhSession().ppqhIsAuthenticated()) {
                context.ppqhNavigator().ppqhShowLogin();
            }
            return null;
        });
        pause(300);
        fx(() -> {
            field("Correo").setText(email);
            field("Contraseña").setText(password);
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
        save(fx(() -> stage.getScene().snapshot(null)), name);
    }

    private void save(WritableImage img, String name) throws Exception {
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
