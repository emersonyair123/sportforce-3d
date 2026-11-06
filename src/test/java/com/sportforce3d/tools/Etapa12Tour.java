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
import com.sportforce3d.three.Png;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
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
 * Capturas de la Etapa 12 (docs/capturas/etapa12): el catálogo completo (todas las páginas, la lista entera de cada
 * una) después de dar foto realista a los productos antiguos (A); en la 12B, el catálogo por categoría, la ficha de
 * tres productos con modelo nuevo, el arquero en el diseñador con nombre y número y la suela de la zapatilla de
 * running en ADMIN › Productos (B). Base temporal con todas las migraciones.
 * Modo C: en la ventana real, el ADMIN cambia la suela de la zapatilla de running y pulsa "Guardar producto"; se
 * comprueba en la base y en el catálogo y la ficha (como CLIENTE).
 * Uso: {@code Etapa12Tour [carpeta] [A|B|C]} (por defecto A).
 */
public final class Etapa12Tour {

    private static final int W = 1440;
    private static final int H = 900;

    private final Path out;
    private AppContext context;
    private Stage stage;

    private final String mode;

    private Etapa12Tour(Path out, String mode) {
        this.out = out;
        this.mode = mode;
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/capturas/etapa12");
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            new Etapa12Tour(out, args.length > 1 ? args[1].toUpperCase(java.util.Locale.ROOT) : "A").run();
        } finally {
            Platform.exit();
        }
    }

    private void run() throws Exception {
        Files.createDirectories(out);
        Path data = Files.createTempDirectory("sf-etapa12");
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
        if (mode.contains("C")) {
            login("admin@sportforce3d.pe", "Admin123!");
            tourC();
        } else {
            login("cliente@sportforce3d.pe", "Cliente123!");
        }
        if (mode.contains("C")) {
            // hecho
        } else if (mode.contains("B")) {
            tourB();
        } else {
            tourA();
        }
        fx(() -> {
            stage.close();
            return null;
        });
    }

    private void tourA() throws Exception {
        go(Routes.CATALOG, new Routes.CatalogParams(null, null));
        pause(3500);
        shot("12a-01-catalogo");
        // Cada página del catálogo, con la lista entera (las fotos se recolorean en segundo plano).
        for (int page = 1; page <= 20; page++) {
            resultsShot(String.format("12a-%02d-catalogo-completo-pagina-%d", page + 1, page));
            SfButton next = fx(() -> nextPage());
            if (next == null) {
                break;
            }
            fx(() -> {
                next.fire();
                return null;
            });
            pause(3500);
        }
    }

    /** Etapa 12B: catálogo por categoría, fichas, diseñador del arquero y suela en el editor del ADMIN. */
    private void tourB() throws Exception {
        int n = 1;
        for (com.sportforce3d.model.Category c : fx(() -> context.catalogService().categories())) {
            go(Routes.CATALOG, new Routes.CatalogParams(null, c.id()));
            pause(3500);
            for (int page = 1; page <= 10; page++) {
                resultsShot(String.format("12b-%02d-catalogo-%s%s", n++, c.slug(), page == 1 ? "" : "-pagina-" + page));
                SfButton next = fx(this::nextPage);
                if (next == null) {
                    break;
                }
                fx(() -> {
                    next.fire();
                    return null;
                });
                pause(3500);
            }
        }
        n = 20;
        for (String sku : new String[]{"SF-UFU-003", "SF-ZAP-003", "SF-ACC-003", "SF-BUZ-002", "SF-ENT-002"}) {
            go(Routes.PRODUCT, productId(sku));
            pause(3500);
            shot(String.format("12b-%02d-ficha-%s", n++, sku.toLowerCase(java.util.Locale.ROOT)));
        }
        // Arquero en el diseñador (Vista realista) con nombre y número.
        go(Routes.DESIGNER, new Routes.DesignerParams(null, null, productId("SF-UFU-003")));
        pause(5000);
        fx(() -> {
            field("Nombre (espalda)").setText("QUISPE");
            field("Número (0–99)").setText("1");
            return null;
        });
        pause(3000);
        shot("12b-30-disenador-arquero");
        go(Routes.DESIGNER, new Routes.DesignerParams(null, null, productId("SF-ENT-002")));
        pause(5000);
        fx(() -> {
            field("Número (0–99)").setText("7");
            return null;
        });
        pause(3000);
        shot("12b-31-disenador-compresion");
        // Suela de la zapatilla de running: editable por el ADMIN.
        fx(() -> {
            context.navigator().logout();
            return null;
        });
        pause(800);
        login("admin@sportforce3d.pe", "Admin123!");
        go(Routes.ADMIN_PRODUCT_EDIT, productId("SF-ZAP-003"));
        pause(3500);
        nodeShot("#content", "12b-40-admin-zapatilla-running");
    }

    /** "Guardar producto" con un color de zona cambiado, en la ventana real. */
    private void tourC() throws Exception {
        long id = productId("SF-ZAP-003");
        go(Routes.ADMIN_PRODUCT_EDIT, id);
        pause(3500);
        nodeShot("#content", "12b-50-admin-suela-antes");
        fx(() -> {
            SfTextField hex = zoneHex("SUELA");
            hex.getInput().requestFocus();
            hex.getInput().setText("#D7192A");
            hex.getInput().fireEvent(new javafx.event.ActionEvent());
            return null;
        });
        pause(2500);
        nodeShot("#content", "12b-51-admin-suela-roja-sin-guardar");
        fx(() -> {
            button("Guardar producto").fire();
            return null;
        });
        pause(3500);
        shot("12b-52-admin-guardado");
        String saved = fx(() -> context.database().withConnection(c -> {
            try (var ps = c.prepareStatement("SELECT zone_colors FROM products WHERE id = ?")) {
                ps.setLong(1, id);
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getString(1);
                }
            }
        }));
        System.out.println("zone_colors guardado: " + saved);
        if (!saved.contains("\"paneles\":\"#d7192a\"")) {
            throw new IllegalStateException("La suela no se guardó: " + saved);
        }
        fx(() -> {
            context.navigator().logout();
            return null;
        });
        pause(800);
        login("cliente@sportforce3d.pe", "Cliente123!");
        long zapatillas = fx(() -> context.catalogService().categories().stream()
                .filter(c -> c.slug().equals("zapatillas")).findFirst().orElseThrow().id());
        go(Routes.CATALOG, new Routes.CatalogParams(null, zapatillas));
        pause(3500);
        resultsShot("12b-53-catalogo-suela-roja");
        go(Routes.PRODUCT, id);
        pause(3500);
        shot("12b-54-ficha-suela-roja");
    }

    private SfTextField zoneHex(String zoneLabel) {
        for (Node n : stage.getScene().getRoot().lookupAll(".label")) {
            if (n instanceof javafx.scene.control.Label l && zoneLabel.equals(l.getText())) {
                return (SfTextField) l.getParent().lookup(".sf-field");
            }
        }
        throw new IllegalStateException("No hay zona " + zoneLabel);
    }

    private long productId(String sku) throws Exception {
        return fx(() -> context.database().withConnection(c -> {
            try (var ps = c.prepareStatement("SELECT id FROM products WHERE sku = ?")) {
                ps.setString(1, sku);
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        }));
    }

    private void nodeShot(String selector, String name) throws Exception {
        WritableImage img = fx(() -> {
            Node node = stage.getScene().getRoot().lookup(selector);
            if (node == null) {
                return null;
            }
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(javafx.scene.paint.Color.web("#0B0E13"));
            return node.snapshot(params, null);
        });
        if (img != null) {
            save(img, name);
        }
    }

    /** Botón de página siguiente habilitado, o null en la última página. */
    private SfButton nextPage() {
        for (Node n : stage.getScene().getRoot().lookupAll(".sf-btn")) {
            if (n instanceof SfButton b && !b.isDisabled() && b.isVisible()
                    && ("Página siguiente".equals(b.getAccessibleText()) || "Siguiente".equalsIgnoreCase(b.getText()))) {
                return b;
            }
        }
        return null;
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
        save(fx(() -> stage.getScene().snapshot(null)), name);
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
        Path file = out.resolve(name + ".png");
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
