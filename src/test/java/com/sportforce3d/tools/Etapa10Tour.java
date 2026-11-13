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
import com.sportforce3d.design.Area;
import com.sportforce3d.design.DesignDefaults;
import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.GarmentCut;
import com.sportforce3d.design.GarmentCut.ShortsLength;
import com.sportforce3d.design.GarmentCut.Sleeve;
import com.sportforce3d.design.GarmentCut.SocksLength;
import com.sportforce3d.design.MaterialSlot;
import com.sportforce3d.design.SampleDesigns;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.three.OrbitState;
import com.sportforce3d.three.Png;
import com.sportforce3d.three.ViewPreset;
import com.sportforce3d.three.Viewer3D;
import com.sportforce3d.three.model.Zone;
import com.sportforce3d.viewmodel.DesignerViewModel;
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
    private AppContext context;
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
            viewer().zoomBy(0.82);
            return null;
        });
        pause(800);
        shot("10a-01-camara-espalda-antes-de-editar");
        fx(() -> {
            vm().edit("Color", d -> d.withColor(Zone.JERSEY_BODY, "#D7141E").withColor(Zone.JERSEY_SLEEVES, "#0B0E13"), null);
            vm().seal();
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
            SfTextField f = field("Nombre (máx. 12)");
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
            vm().edit("Colores", d -> d.withColor(Zone.JERSEY_BODY, "#1F5FFF").withColor(Zone.JERSEY_SLEEVES, "#F2F5F8")
                    .withColor(Zone.SHORTS_BASE, "#F2F5F8"), null);
            vm().seal();
            button("Reiniciar").fire();
            return null;
        });
        pause(800);
        String[][] fabrics = {{"DRY_FIT", "dry-fit"}, {"POLIESTER_LISO", "poliester-liso"}, {"ALGODON", "algodon"},
                {"MALLA", "malla-deportiva"}};
        int i = 1;
        for (String[] f : fabrics) {
            fx(() -> {
                vm().edit("Tela", d -> d.withMaterial(MaterialSlot.JERSEY, f[0])
                        .withMaterial(MaterialSlot.SHORTS, f[0]), null);
                vm().seal();
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
                viewer().jumpTo(ViewPreset.FRONT, Garment.JERSEY);
                viewer().zoomBy(0.42);
                return null;
            });
            pause(800);
            viewerShot(String.format("10b-%02d-tela-%s-cerca", i++, f[1]));
            if (f[0].equals("POLIESTER_LISO")) {
                WritableImage big = fx(() -> viewer().capture(1400));
                save(big, "10b-10-tela-poliester-liso-cerca-1400px");
            }
        }
    }

    private static final String[] MODELS = {"futbol_hombre_frente", "futbol_mujer_frente", "futbol_nino_frente",
            "futbol_nina_frente"};
    private static final String[] PEOPLE = {"hombre", "mujer", "nino", "nina"};

    private static DesignDocument kit(int i, GarmentCut cut) {
        DesignDocument d = SampleDesigns.forPerson(i);
        return d.withRealistic(d.realisticOrDefault().withModel(MODELS[i])).withCut(cut)
                .addElement(DesignDefaults.emblem("escudo-clasico", "#F2F5F8", "#0B0E13")
                        .withArea(Area.SLEEVE_LEFT).at(0.25, 0.45));
    }

    private void cuts() throws Exception {
        GarmentCut largaCortoTob = new GarmentCut(Sleeve.LARGA, ShortsLength.CORTO, SocksLength.TOBILLERA);
        GarmentCut largaLargoMedia = new GarmentCut(Sleeve.LARGA, ShortsLength.LARGO, SocksLength.MEDIA);
        fx(() -> {
            tab("Diseño").fire();
            vm().edit("Kit", d -> kit(0, GarmentCut.DEFAULT), null);
            vm().seal();
            return null;
        });
        waitDressed();
        fx(() -> {
            viewer().jumpTo(ViewPreset.FRONT, Garment.KIT);
            return null;
        });
        pause(500);
        viewerShot("10c-00-hombre-corte-de-siempre-frente");
        fx(() -> {
            button("Costado").fire();
            viewer().zoomBy(0.9);
            return null;
        });
        pause(800);
        shot("10c-01-corte-de-siempre-camara-costado");
        fx(() -> {
            vm().edit("Manga larga", d -> d.withCut(d.cut().withSleeve(Sleeve.LARGA)), null);
            vm().seal();
            return null;
        });
        waitDressed();
        shot("10c-02-manga-larga-misma-camara");
        int n = 3;
        for (int i = 0; i < 4; i++) {
            for (GarmentCut cut : List.of(largaCortoTob, largaLargoMedia)) {
                int k = i;
                fx(() -> {
                    vm().edit("Kit", d -> kit(k, cut), null);
                    vm().seal();
                    return null;
                });
                waitDressed();
                String tag = PEOPLE[i] + (cut == largaCortoTob ? "-manga-larga-short-corto-tobilleras"
                        : "-manga-larga-short-largo-media-pierna");
                for (ViewPreset view : List.of(ViewPreset.FRONT, ViewPreset.BACK, ViewPreset.LEFT)) {
                    fx(() -> {
                        viewer().jumpTo(view, Garment.KIT);
                        return null;
                    });
                    pause(500);
                    String side = view == ViewPreset.FRONT ? "frente" : view == ViewPreset.BACK ? "espalda" : "costado";
                    viewerShot(String.format("10c-%02d-%s-%s", n++, tag, side));
                }
            }
        }
        fx(() -> {
            vm().edit("Kit", d -> kit(1, largaLargoMedia), null);
            vm().seal();
            return null;
        });
        waitDressed();
        fx(() -> {
            viewer().jumpTo(ViewPreset.FRONT, Garment.JERSEY);
            viewer().zoomBy(0.7);
            return null;
        });
        pause(800);
        viewerShot(String.format("10c-%02d-mujer-manga-larga-hombros-cerca", n++));
        fx(() -> {
            viewer().jumpTo(ViewPreset.LEFT, Garment.JERSEY);
            viewer().zoomBy(0.7);
            return null;
        });
        pause(800);
        viewerShot(String.format("10c-%02d-mujer-manga-larga-hombro-costado-cerca", n++));
        fx(() -> {
            scrollTo("CORTE");
            viewer().jumpTo(ViewPreset.FRONT, Garment.KIT);
            return null;
        });
        pause(800);
        shot(String.format("10c-%02d-panel-corte", n));
    }

    private void orderPhotos() throws Exception {
        GarmentCut cut = new GarmentCut(Sleeve.LARGA, ShortsLength.CORTO, SocksLength.TOBILLERA);
        fx(() -> {
            toggle("Vista realista").fire();
            vm().edit("Kit", d -> kit(0, cut).withMaterial(MaterialSlot.JERSEY, "MALLA")
                    .withMaterial(MaterialSlot.SHORTS, "MALLA"), null);
            vm().seal();
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
        waitFor(() -> context.designService().myDesigns().stream()
                .anyMatch(d -> d.name().equals("Halcones manga larga")), 120_000);
        pause(1000);
        shot("10d-01-guardado-desde-vista-realista");
        long design = context.designService().myDesigns().stream()
                .filter(d -> d.name().equals("Halcones manga larga")).findFirst().orElseThrow().id();
        long address = context.addressService().list().get(0).id();
        long variant = context.database().withConnection(c -> {
            try (var ps = c.prepareStatement("SELECT id FROM product_variants WHERE sku = 'SF-UFU-001-M'");
                 var rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        });
        context.cartService().addDesign(design, variant, 1);
        long order = context.orderService().placeOrder(address, PaymentMethod.YAPE, null).id();
        fx(() -> {
            context.navigator().logout();
            return null;
        });
        pause(500);
        loginAs("admin@sportforce3d.pe", "Admin123!");
        var line = context.adminOrderService().order(order).items().stream().filter(i -> i.hasDesign())
                .findFirst().orElseThrow();
        Files.copy(context.storage().resolve(line.designFrontPath()), out.resolve("10d-02-foto-del-pedido-frente.png"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.copy(context.storage().resolve(line.designBackPath()), out.resolve("10d-03-foto-del-pedido-espalda.png"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Fotos del pedido: " + line.designFrontPath() + ", " + line.designBackPath());
        go(Routes.ADMIN_DESIGN, line.id());
        waitFor(() -> viewer().getModel() != null, 90_000);
        pause(2500);
        shot("10d-04-admin-visor-3d-persona-con-el-corte");
        fx(() -> {
            viewer().jumpTo(ViewPreset.BACK, Garment.KIT);
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
        WritableImage img = fx(() -> viewer().capture((int) viewer().getWidth()));
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

    private Viewer3D viewer() {
        return (Viewer3D) stage.getScene().getRoot().lookup(".sf-viewer3d");
    }

    private DesignerViewModel vm() {
        Node n = withProperty(stage.getScene().getRoot());
        if (n == null) {
            throw new IllegalStateException("No está abierto el diseñador");
        }
        return (DesignerViewModel) n.getProperties().get(DesignerViewModel.class);
    }

    private static Node withProperty(Node node) {
        if (node.getProperties().containsKey(DesignerViewModel.class)) {
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
            if (!context.session().isAuthenticated()) {
                context.navigator().showLogin();
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
            context.router().go(route, param);
            return null;
        });
        pause(1300);
    }

    private void shot(String name) throws Exception {
        WritableImage img = fx(() -> stage.getScene().snapshot(null));
        OrbitState cam = fx(() -> viewer().cameraGoal());
        save(img, name);
        System.out.printf("  cámara: yaw=%.3f pitch=%.3f dist=%.4f target=%s%n", cam.yaw(), cam.pitch(),
                cam.distance(), cam.target());
    }

    private void save(WritableImage img, String name) throws Exception {
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
        SfButton found = find(stage.getScene().getRoot(), text);
        if (found == null) {
            throw new IllegalStateException("No hay botón " + text);
        }
        return found;
    }

    private static SfButton find(Node node, String text) {
        if (node instanceof SfButton b && text.equalsIgnoreCase(b.getText())) {
            return b;
        }
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                SfButton f = find(child, text);
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
