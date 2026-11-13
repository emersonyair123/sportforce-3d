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
import com.sportforce3d.design.RealisticSpec;
import com.sportforce3d.design.SampleDesigns;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.three.Png;
import com.sportforce3d.viewmodel.DesignerViewModel;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public final class RealisticTour {

    private static final int W = 1440;
    private static final int H = 900;

    private final Path out;
    private AppContext context;
    private Stage stage;
    private int count;

    private RealisticTour(Path out) {
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/capturas/etapa7");
        String part = args.length > 1 ? args[1] : "todo";
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            new RealisticTour(out).run(part);
        } finally {
            Platform.exit();
        }
    }

    private void run(String part) throws Exception {
        Files.createDirectories(out);
        Path data = Files.createTempDirectory("sf-etapa7");
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
        login("cliente@sportforce3d.pe", "Cliente123!");
        if (!part.equals("disenador")) {
            catalogTour();
        }
        if (!part.equals("catalogo")) {
            count = 30;
            designerTour();
            count = 50;
            adminTour();
        }
        fx(() -> {
            stage.close();
            return null;
        });
    }

    private void catalogTour() throws Exception {
        shot("inicio");
        catalog("catalogo", null, null);
        catalog("catalogo-uniformes-futbol", null, "Uniformes de fútbol");
        catalog("catalogo-camisetas", null, "Camisetas deportivas");
        catalog("catalogo-ninos", null, "Niños");
        catalog("catalogo-shorts", null, "Shorts");
        catalog("catalogo-voley", null, "Uniformes de vóley");
        catalog("catalogo-basquet", null, "Uniformes de básquet");
        catalog("catalogo-buzos", null, "Buzos y conjuntos");
        catalog("catalogo-zapatillas", null, "Zapatillas");
        catalog("catalogo-accesorios", null, "Accesorios");
        catalog("catalogo-busqueda-camiseta", "camiseta", null);
    }

    private void catalog(String name, String text, String category) throws Exception {
        Long categoryId = category == null ? null : categoryId(category);
        go(Routes.CATALOG, new Routes.CatalogParams(text, categoryId));
        pause(2200);
        shot(name);
        resultsShot(name + "-completo");
    }

    private void designerTour() throws Exception {
        go(Routes.DESIGNER, null);
        pause(2500);
        shot("disenador-realista-inicial");

        look("hombre-futbol-rayas-nombre", "futbol_hombre_frente", Map.of("camiseta", "#F2F5F8", "short", "#0B2A6B",
                "paneles", "#0B2A6B", "medias", "#0B2A6B"), "rayas", "#0B2A6B", "QUISPE", 9);
        look("mujer-futbol-franjas", "futbol_mujer_frente", Map.of("camiseta", "#0B2A6B", "short", "#0B2A6B",
                "paneles", "#FFD400", "medias", "#0B2A6B"), "franjas", "#FFD400", "MAMANI", 7);
        look("nino-futbol-minimal", "futbol_nino_frente", Map.of("camiseta", "#FFD400", "short", "#0B0E13",
                "paneles", "#0B0E13", "medias", "#FFD400"), "minimal", "#0B0E13", "LEO", 10);
        look("nina-futbol-dinamico", "futbol_nina_frente", Map.of("camiseta", "#8B3DFF", "short", "#8B3DFF",
                "paneles", "#FFD400", "medias", "#F2F5F8"), "dinamico", "#FFD400", "VALE", 11);
        look("hombre-polo-geometrico", "polo_hombre_frente", Map.of("camiseta", "#1F5FFF", "short", "#F2F5F8",
                "paneles", "#F2F5F8", "medias", "#1F5FFF"), "geometrico", "#F2F5F8", "ROJAS", 5);
        look("mujer-voley-banda", "voley_mujer_frente", Map.of("camiseta", "#F2F5F8", "short", "#D7141E",
                "medias", "#D7141E"), "banda", "#D7141E", "FLORES", 3);
        look("hombre-basquet-liso", "basquet_hombre_frente", Map.of("camiseta", "#FF7A00", "short", "#FF7A00",
                "paneles", "#0B0E13", "medias", "#F2F5F8"), "liso", "#F2F5F8", "CHÁVEZ", 23);
        look("mujer-raglan", "raglan_mujer_frente", Map.of("camiseta", "#F2F5F8", "short", "#0B0E13",
                "paneles", "#FF5FB0", "medias", "#F2F5F8"), "liso", "#F2F5F8", null, 8);
        look("hombre-buzo-solo-frente", "buzo_hombre_frente", Map.of("principal", "#0B2A6B", "paneles", "#39FF88"),
                "liso", "#F2F5F8", null, null);

        fx(() -> {
            toggle("Vista 3D").fire();
            return null;
        });
        pause(15000);
        shot("disenador-vista-3d");
        fx(() -> {
            toggle("Vista realista").fire();
            return null;
        });

        go(Routes.DESIGNER, new Routes.DesignerParams(null, null, productId("SF-009")));
        pause(3000);
        shot("disenador-desde-producto-rayas");

        look("guardar-hombre-banda", "futbol_hombre_frente", Map.of("camiseta", "#F2F5F8", "short", "#0B0E13",
                "paneles", "#D7141E", "medias", "#F2F5F8"), "banda", "#D7141E", "GUERRERO", 9);
        fx(() -> {
            button("Guardar diseño").fire();
            return null;
        });
        pause(800);
        fx(() -> {
            button("Guardar").fire();
            return null;
        });
        pause(3500);
        go(Routes.DESIGNS, null);
        pause(1500);
        shot("mis-disenos-capturas-realistas");
    }

    private void adminTour() throws Exception {
        long address = context.addressService().list().get(0).id();
        long saved = number("SELECT max(id) FROM designs");
        context.cartService().addDesign(saved, number("SELECT id FROM product_variants WHERE sku = 'SF-001-M'"), 2);
        long realOrder = context.orderService().placeOrder(address, PaymentMethod.YAPE, null).id();

        DesignDocument old = SampleDesigns.halcones().withRealistic(null);
        long oldDesign = context.designService().save(null, "Halcones (diseño anterior)", old,
                existing("docs/capturas/etapa4/frente.png"), existing("docs/capturas/etapa4/espalda.png")).id();
        context.cartService().addDesign(oldDesign, number("SELECT id FROM product_variants WHERE sku = 'SF-UFU-001-M'"), 1);
        long oldOrder = context.orderService().placeOrder(address, PaymentMethod.PLIN, null).id();

        fx(() -> {
            context.navigator().logout();
            return null;
        });
        login("admin@sportforce3d.pe", "Admin123!");
        go(Routes.ADMIN_DESIGN, designItem(realOrder));
        pause(4500);
        shot("admin-uniforme-vista-realista");
        panelShot("admin-uniforme-ficha-tecnica-completa");
        fx(() -> {
            toggle("Vista 3D").fire();
            return null;
        });
        pause(1500);
        shot("admin-uniforme-vista-3d");
        go(Routes.ADMIN_DESIGN, designItem(oldOrder));
        pause(4500);
        shot("admin-uniforme-diseno-anterior-solo-3d");
    }

    private long designItem(long orderId) {
        return context.adminOrderService().order(orderId).items().stream()
                .filter(i -> i.hasDesign()).findFirst().orElseThrow().id();
    }

    private void panelShot(String name) throws Exception {
        WritableImage img = fx(() -> {
            Node panel = stage.getScene().getRoot().lookup("#panel");
            if (panel == null) {
                return null;
            }
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(javafx.scene.paint.Color.web("#0E1014"));
            return panel.snapshot(params, null);
        });
        if (img != null) {
            save(img, name);
        }
    }

    private static Path existing(String path) {
        Path p = Path.of(path);
        return Files.exists(p) ? p : null;
    }

    private void look(String name, String model, Map<String, String> colors, String pattern, String patternColor,
                      String text, Integer number) throws Exception {
        fx(() -> {
            DesignerViewModel vm = viewModel();
            vm.editRealistic("Captura", s -> new RealisticSpec(model, merged(s.zoneColors(), colors), pattern,
                    patternColor, text, number), null);
            vm.seal();
            return null;
        });
        pause(1800);
        shot("disenador-" + name);
    }

    private static Map<String, String> merged(Map<String, String> base, Map<String, String> over) {
        Map<String, String> m = new TreeMap<>(base);
        m.putAll(over);
        return m;
    }

    private DesignerViewModel viewModel() {
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

    private long categoryId(String name) {
        return id("SELECT id FROM categories WHERE name = ?", name);
    }

    private long productId(String sku) {
        return id("SELECT id FROM products WHERE sku = ?", sku);
    }

    private long number(String sql) {
        return context.database().withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Sin resultado: " + sql);
                }
                return rs.getLong(1);
            }
        });
    }

    private long id(String sql, String value) {
        return context.database().withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, value);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("No existe " + value);
                    }
                    return rs.getLong(1);
                }
            }
        });
    }

    private void login(String email, String password) throws Exception {
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
        save(img, name);
    }

    private void resultsShot(String name) throws Exception {
        WritableImage img = fx(() -> {
            Node scroll = stage.getScene().getRoot().lookup("#resultsScroll");
            if (!(scroll instanceof ScrollPane sp) || sp.getContent() == null) {
                return null;
            }
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(javafx.scene.paint.Color.web("#0B0E13"));
            return sp.getContent().snapshot(params, null);
        });
        if (img != null) {
            save(img, name);
        }
    }

    private void save(WritableImage img, String name) throws Exception {
        count++;
        Path file = out.resolve(String.format("%02d-%s.png", count, name));
        Png.write(img, file);
        System.out.println("Captura: " + file);
    }

    private SfTextField field(String label) {
        for (Node n : stage.getScene().getRoot().lookupAll(".sf-field")) {
            if (n instanceof SfTextField f && f.lookupAll(".sf-field-label").stream()
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
