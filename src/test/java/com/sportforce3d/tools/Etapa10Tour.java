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
import com.sportforce3d.design.PgeyArea;
import com.sportforce3d.design.PgeyDesignDefaults;
import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeyGarmentCut;
import com.sportforce3d.design.PgeyGarmentCut.PgeyShortsLength;
import com.sportforce3d.design.PgeyGarmentCut.PgeySleeve;
import com.sportforce3d.design.PgeyGarmentCut.PgeySocksLength;
import com.sportforce3d.design.PgeyMaterialSlot;
import com.sportforce3d.design.PgeySampleDesigns;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.three.PgeyOrbitState;
import com.sportforce3d.three.PgeyPng;
import com.sportforce3d.three.PgeyViewPreset;
import com.sportforce3d.three.PgeyViewer3D;
import com.sportforce3d.three.model.PgeyZone;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public final class Etapa10Tour {

    private static final int W = 1440;
    private static final int H = 900;

    private final Path out;
    private PpqhAppContext context;
    private Stage stage;

    private Etapa10Tour(Path out) {
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/capturas/etapa10");
        String part = args.length > 1 ? args[1] : "A";
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            new Etapa10Tour(out).run(part);
        } finally {
            Platform.exit();
        }
    }

    private void run(String part) throws Exception {
        Files.createDirectories(out);
        Path data = Files.createTempDirectory("sf-etapa10");
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
        pause(1500);
        if (part.contains("A")) {
            cameraAndTexts();
        }
        if (part.contains("B")) {
            fabrics();
        }
        if (part.contains("C")) {
            cuts();
        }
        if (part.contains("D")) {
            orderPhotos();
        }
        fx(() -> {
            stage.close();
            return null;
        });
    }

    private void cameraAndTexts() throws Exception {
        fx(() -> {
            button("Espalda").fire();
            viewer().pgeyZoomBy(0.82);
            return null;
        });
        pause(800);
        shot("10a-01-camara-espalda-antes-de-editar");
        fx(() -> {
            vm().pgeyEdit("Color", d -> d.pgeyWithColor(PgeyZone.JERSEY_BODY, "#D7141E").pgeyWithColor(PgeyZone.JERSEY_SLEEVES, "#0B0E13"), null);
            vm().pgeySeal();
            return null;
        });
        pause(800);
        shot("10a-02-color-cambiado-misma-camara");
        fx(() -> {
            tab("Texto").fire();
            return null;
        });
        pause(500);
        shot("10a-03-texto-cuatro-opciones");
        fx(() -> {
            button("Nombre en la espalda").fire();
            return null;
        });
        pause(500);
        fx(() -> {
            PpqhSfTextField f = field("Nombre (máx. 12)");
            f.getInput().setText("QUISPE");
            return null;
        });
        pause(1200);
        shot("10a-04-nombre-editor-en-su-tarjeta");
        fx(() -> {
            button("Número en la espalda").fire();
            return null;
        });
        pause(500);
        fx(() -> {
            field("Número (0–99)").getInput().setText("9");
            return null;
        });
        pause(1200);
        shot("10a-05-numero-espalda-editor-en-su-tarjeta");
        fx(() -> {
            button("Número en el short").fire();
            button("Nombre en la espalda").fire();
            button("Número en la espalda").fire();
            return null;
        });
        pause(1200);
        shot("10a-06-numero-short-y-tarjetas-cerradas");
        pause(5000);
        shot("10a-07-cinco-segundos-despues-sin-giro");
    }

    private void fabrics() throws Exception {
        fx(() -> {
            tab("Diseño").fire();
            vm().pgeyEdit("Colores", d -> d.pgeyWithColor(PgeyZone.JERSEY_BODY, "#1F5FFF").pgeyWithColor(PgeyZone.JERSEY_SLEEVES, "#F2F5F8")
                    .pgeyWithColor(PgeyZone.SHORTS_BASE, "#F2F5F8"), null);
            vm().pgeySeal();
            button("Reiniciar").fire();
            return null;
        });
        pause(800);
        String[][] fabrics = {{"DRY_FIT", "dry-fit"}, {"POLIESTER_LISO", "poliester-liso"}, {"ALGODON", "algodon"},
                {"MALLA", "malla-deportiva"}};
        int i = 1;
        for (String[] f : fabrics) {
            fx(() -> {
                vm().pgeyEdit("Tela", d -> d.pgeyWithMaterial(PgeyMaterialSlot.JERSEY, f[0])
                        .pgeyWithMaterial(PgeyMaterialSlot.SHORTS, f[0]), null);
                vm().pgeySeal();
                Node tela = find(stage.getScene().getRoot(), n -> n instanceof Labeled l && "TELA".equals(l.getText()));
                ScrollPane sp = (ScrollPane) stage.getScene().getRoot().lookup("#scroll");
                double y = sp.getContent().sceneToLocal(tela.localToScene(0, 0)).getY();
                sp.setVvalue(Math.min(1, y / Math.max(1, sp.getContent().getLayoutBounds().getHeight()
                        - sp.getViewportBounds().getHeight())));
                button("Reiniciar").fire();
                return null;
            });
            pause(1500);
            shot(String.format("10b-%02d-tela-%s", i++, f[1]));
            fx(() -> {
                viewer().pgeyJumpTo(PgeyViewPreset.FRONT, HepyGarment.JERSEY);
                viewer().pgeyZoomBy(0.42);
                return null;
            });
            pause(800);
            viewerShot(String.format("10b-%02d-tela-%s-cerca", i++, f[1]));
            if (f[0].equals("POLIESTER_LISO")) {
                WritableImage big = fx(() -> viewer().pgeyCapture(1400));
                save(big, "10b-10-tela-poliester-liso-cerca-1400px");
            }
        }
    }

    private static final String[] MODELS = {"futbol_hombre_frente", "futbol_mujer_frente", "futbol_nino_frente",
            "futbol_nina_frente"};
    private static final String[] PEOPLE = {"hombre", "mujer", "nino", "nina"};

    private static PgeyDesignDocument kit(int i, PgeyGarmentCut cut) {
        PgeyDesignDocument d = PgeySampleDesigns.pgeyForPerson(i);
        return d.pgeyWithRealistic(d.pgeyRealisticOrDefault().pgeyWithModel(MODELS[i])).pgeyWithCut(cut)
                .pgeyAddElement(PgeyDesignDefaults.pgeyEmblem("escudo-clasico", "#F2F5F8", "#0B0E13")
                        .pgeyWithArea(PgeyArea.SLEEVE_LEFT).pgeyAt(0.25, 0.45));
    }

    private void cuts() throws Exception {
        PgeyGarmentCut largaCortoTob = new PgeyGarmentCut(PgeySleeve.LARGA, PgeyShortsLength.CORTO, PgeySocksLength.TOBILLERA);
        PgeyGarmentCut largaLargoMedia = new PgeyGarmentCut(PgeySleeve.LARGA, PgeyShortsLength.LARGO, PgeySocksLength.MEDIA);
        fx(() -> {
            tab("Diseño").fire();
            vm().pgeyEdit("Kit", d -> kit(0, PgeyGarmentCut.PGEY_DEFAULT), null);
            vm().pgeySeal();
            return null;
        });
        waitDressed();
        fx(() -> {
            viewer().pgeyJumpTo(PgeyViewPreset.FRONT, HepyGarment.KIT);
            return null;
        });
        pause(500);
        viewerShot("10c-00-hombre-corte-de-siempre-frente");
        fx(() -> {
            button("Costado").fire();
            viewer().pgeyZoomBy(0.9);
            return null;
        });
        pause(800);
        shot("10c-01-corte-de-siempre-camara-costado");
        fx(() -> {
            vm().pgeyEdit("Manga larga", d -> d.pgeyWithCut(d.pgeyCut().pgeyWithSleeve(PgeySleeve.LARGA)), null);
            vm().pgeySeal();
            return null;
        });
        waitDressed();
        shot("10c-02-manga-larga-misma-camara");
        int n = 3;
        for (int i = 0; i < 4; i++) {
            for (PgeyGarmentCut cut : List.of(largaCortoTob, largaLargoMedia)) {
                int k = i;
                fx(() -> {
                    vm().pgeyEdit("Kit", d -> kit(k, cut), null);
                    vm().pgeySeal();
                    return null;
                });
                waitDressed();
                String tag = PEOPLE[i] + (cut == largaCortoTob ? "-manga-larga-short-corto-tobilleras"
                        : "-manga-larga-short-largo-media-pierna");
                for (PgeyViewPreset view : List.of(PgeyViewPreset.FRONT, PgeyViewPreset.BACK, PgeyViewPreset.LEFT)) {
                    fx(() -> {
                        viewer().pgeyJumpTo(view, HepyGarment.KIT);
                        return null;
                    });
                    pause(500);
                    String side = view == PgeyViewPreset.FRONT ? "frente" : view == PgeyViewPreset.BACK ? "espalda" : "costado";
                    viewerShot(String.format("10c-%02d-%s-%s", n++, tag, side));
                }
            }
        }
        fx(() -> {
            vm().pgeyEdit("Kit", d -> kit(1, largaLargoMedia), null);
            vm().pgeySeal();
            return null;
        });
        waitDressed();
        fx(() -> {
            viewer().pgeyJumpTo(PgeyViewPreset.FRONT, HepyGarment.JERSEY);
            viewer().pgeyZoomBy(0.7);
            return null;
        });
        pause(800);
        viewerShot(String.format("10c-%02d-mujer-manga-larga-hombros-cerca", n++));
        fx(() -> {
            viewer().pgeyJumpTo(PgeyViewPreset.LEFT, HepyGarment.JERSEY);
            viewer().pgeyZoomBy(0.7);
            return null;
        });
        pause(800);
        viewerShot(String.format("10c-%02d-mujer-manga-larga-hombro-costado-cerca", n++));
        fx(() -> {
            scrollTo("CORTE");
            viewer().pgeyJumpTo(PgeyViewPreset.FRONT, HepyGarment.KIT);
            return null;
        });
        pause(800);
        shot(String.format("10c-%02d-panel-corte", n));
    }

    private void orderPhotos() throws Exception {
        PgeyGarmentCut cut = new PgeyGarmentCut(PgeySleeve.LARGA, PgeyShortsLength.CORTO, PgeySocksLength.TOBILLERA);
        fx(() -> {
            toggle("Vista realista").fire();
            vm().pgeyEdit("Kit", d -> kit(0, cut).pgeyWithMaterial(PgeyMaterialSlot.JERSEY, "MALLA")
                    .pgeyWithMaterial(PgeyMaterialSlot.SHORTS, "MALLA"), null);
            vm().pgeySeal();
            return null;
        });
        pause(1500);
        fx(() -> {
            button("Guardar diseño").fire();
            return null;
        });
        pause(800);
        fx(() -> {
            field("Nombre del diseño").getInput().setText("Halcones manga larga");
            button("Guardar").fire();
            return null;
        });
        waitFor(() -> context.ppqhDesignService().pgeyMyDesigns().stream()
                .anyMatch(d -> d.pgeyName().equals("Halcones manga larga")), 120_000);
        pause(1000);
        shot("10d-01-guardado-desde-vista-realista");
        long design = context.ppqhDesignService().pgeyMyDesigns().stream()
                .filter(d -> d.pgeyName().equals("Halcones manga larga")).findFirst().orElseThrow().pgeyId();
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
        Files.copy(context.ppqhStorage().hepyResolve(line.ppqhDesignFrontPath()), out.resolve("10d-02-foto-del-pedido-frente.png"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.copy(context.ppqhStorage().hepyResolve(line.ppqhDesignBackPath()), out.resolve("10d-03-foto-del-pedido-espalda.png"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Fotos del pedido: " + line.ppqhDesignFrontPath() + ", " + line.ppqhDesignBackPath());
        go(PpqhRoutes.PPQH_ADMIN_DESIGN, line.ppqhId());
        waitFor(() -> viewer().getModel() != null, 90_000);
        pause(2500);
        shot("10d-04-admin-visor-3d-persona-con-el-corte");
        fx(() -> {
            viewer().pgeyJumpTo(PgeyViewPreset.BACK, HepyGarment.KIT);
            return null;
        });
        pause(800);
        shot("10d-05-admin-visor-3d-espalda");
        fx(() -> {
            scrollTo("CORTE");
            return null;
        });
        pause(800);
        shot("10d-06-admin-ficha-corte");
        fx(() -> {
            scrollTo("MATERIALES");
            return null;
        });
        pause(800);
        shot("10d-07-admin-ficha-telas");
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

    private void viewerShot(String name) throws Exception {
        WritableImage img = fx(() -> viewer().pgeyCapture((int) viewer().getWidth()));
        save(img, name);
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
        Node n = withProperty(stage.getScene().getRoot());
        if (n == null) {
            throw new IllegalStateException("No está abierto el diseñador");
        }
        return (PgeyDesignerViewModel) n.getProperties().get(PgeyDesignerViewModel.class);
    }

    private static Node withProperty(Node node) {
        if (node.getProperties().containsKey(PgeyDesignerViewModel.class)) {
            return node;
        }
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                Node f = withProperty(child);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    private ToggleButton toggle(String text) {
        for (Node n : stage.getScene().getRoot().lookupAll(".sf-seg-item")) {
            if (n instanceof ToggleButton t && text.equals(t.getText())) {
                return t;
            }
        }
        throw new IllegalStateException("No hay pestaña " + text);
    }

    private ToggleButton tab(String text) {
        return toggle(text);
    }

    private void login() throws Exception {
        loginAs("cliente@sportforce3d.pe", "Cliente123!");
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
        WritableImage img = fx(() -> stage.getScene().snapshot(null));
        PgeyOrbitState cam = fx(() -> viewer().pgeyCameraGoal());
        save(img, name);
        System.out.printf("  cámara: yaw=%.3f pitch=%.3f dist=%.4f target=%s%n", cam.pgeyYaw(), cam.pgeyPitch(),
                cam.pgeyDistance(), cam.pgeyTarget());
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
        PpqhSfButton found = find(stage.getScene().getRoot(), text);
        if (found == null) {
            throw new IllegalStateException("No hay botón " + text);
        }
        return found;
    }

    private static PpqhSfButton find(Node node, String text) {
        if (node instanceof PpqhSfButton b && text.equalsIgnoreCase(b.getText())) {
            return b;
        }
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                PpqhSfButton f = find(child, text);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
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
