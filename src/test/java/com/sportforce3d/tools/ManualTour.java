package com.sportforce3d.tools;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhFonts;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.app.PpqhSportForceApp;
import com.sportforce3d.app.PpqhUiSettings;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.config.PpqhAppConfig;
import com.sportforce3d.db.HepyDatabase;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

public class ManualTour {

    protected static final int W = 1280;
    protected static final int H = 800;

    protected final Path out;
    protected PpqhAppContext context;
    protected Stage stage;
    protected final List<String> manifest = new ArrayList<>();
    private int offsetX;
    private int offsetY;

    protected ManualTour(Path out) {
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "target/manual");
        Path db = Path.of(args.length > 1 ? args[1] : "data/sportforce3d.db");
        String mode = args.length > 2 ? args[2] : "survey";
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            ManualTour tour = mode.equals("survey") ? new ManualTour(out) : new ManualSteps(out);
            tour.open(db);
            try {
                if (mode.equals("continuar")) {
                    ((ManualSteps) tour).continuar();
                } else {
                    tour.run();
                }
            } finally {
                Files.write(out.resolve(mode.equals("continuar") ? "manifest-continuacion.tsv" : "manifest.tsv"),
                        tour.manifest, StandardCharsets.UTF_8);
            }
            tour.fx(() -> {
                tour.stage.close();
                return null;
            });
        } finally {
            Platform.exit();
        }
    }

    protected void open(Path sourceDb) throws Exception {
        Files.createDirectories(out);
        Path data = Files.createTempDirectory("sf-manual");
        Path copy = data.resolve("sportforce3d.db");
        if (Files.exists(sourceDb)) {
            Files.copy(sourceDb, copy, StandardCopyOption.REPLACE_EXISTING);
            Path storage = sourceDb.toAbsolutePath().getParent().resolve("storage");
            if (Files.isDirectory(storage) && sourceDb.toAbsolutePath().toString().contains("sf-manual")) {
                try (var walk = Files.walk(storage)) {
                    for (Path src : (Iterable<Path>) walk::iterator) {
                        Path dst = data.resolve("storage").resolve(storage.relativize(src).toString());
                        if (Files.isDirectory(src)) {
                            Files.createDirectories(dst);
                        } else {
                            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
        }
        System.out.println("Base de datos de trabajo: " + copy.toAbsolutePath());
        fx(() -> {
            PpqhFonts.ppqhLoadAll();
            PpqhUiSettings settings = new PpqhUiSettings(Preferences.userRoot().node("com/sportforce3d-manual"));
            settings.reduceMotionProperty().set(true);
            context = new PpqhAppContext(new PpqhAppConfig(copy), settings);
            context.ppqhDatabase().hepyMigrate();
            stage = new Stage();
            PpqhSportForceApp.ppqhOpenMainWindow(stage, context);
            stage.setWidth(W);
            stage.setHeight(H);
            stage.show();
            return null;
        });
        pause(1000);
    }

    protected void run() throws Exception {
        long[] seed = seed();
        dump("login");
        fx(() -> {
            context.ppqhNavigator().ppqhShowRegister();
            return null;
        });
        pause(800);
        dump("register");
        login("cliente@sportforce3d.pe", "Cliente123!");
        fx(() -> {
            click(css(".menu-button"));
            return null;
        });
        pause(500);
        dump("usermenu");
        fx(() -> {
            ((javafx.scene.control.MenuButton) css(".menu-button")).hide();
            return null;
        });
        for (String r : new String[]{PpqhRoutes.PPQH_HOME, PpqhRoutes.PPQH_CATALOG, PpqhRoutes.PPQH_CART,
                PpqhRoutes.PPQH_CHECKOUT, PpqhRoutes.PPQH_ORDERS, PpqhRoutes.PPQH_DESIGNS, PpqhRoutes.PPQH_ACCOUNT}) {
            go(r, null);
            dump(r);
        }
        go(PpqhRoutes.PPQH_ORDER, seed[1]);
        dump("order-pending");
        go(PpqhRoutes.PPQH_ORDER, seed[2]);
        dump("order-paid");
        go(PpqhRoutes.PPQH_PRODUCT, seed[0]);
        dump("product");
        go(PpqhRoutes.PPQH_DESIGNER, null);
        pause(2500);
        dump("designer");
        fx(() -> {
            click(text("Vista 3D"));
            return null;
        });
        pause(5000);
        dump("designer3d");
        for (String tab : new String[]{"Texto", "Logos", "Capas"}) {
            fx(() -> {
                click(text(tab));
                return null;
            });
            pause(800);
            dump("designer3d-" + tab.toLowerCase(Locale.ROOT));
        }
        logout();
        login("admin@sportforce3d.pe", "Admin123!");
        for (String r : new String[]{PpqhRoutes.PPQH_ADMIN_DASHBOARD, PpqhRoutes.PPQH_ADMIN_PRODUCTS,
                PpqhRoutes.PPQH_ADMIN_INVENTORY, PpqhRoutes.PPQH_ADMIN_ORDERS, PpqhRoutes.PPQH_ADMIN_PAYMENTS,
                PpqhRoutes.PPQH_ADMIN_USERS, PpqhRoutes.PPQH_ADMIN_REPORTS, PpqhRoutes.PPQH_ADMIN_SETTINGS}) {
            go(r, null);
            dump(r);
        }
        go(PpqhRoutes.PPQH_ADMIN_PRODUCT_EDIT, seed[0]);
        dump("admin-product-edit");
        go(PpqhRoutes.PPQH_ADMIN_PRODUCT_EDIT, null);
        dump("admin-product-new");
        go(PpqhRoutes.PPQH_ADMIN_ORDER, seed[2]);
        dump("admin-order");
        go(PpqhRoutes.PPQH_ADMIN_DESIGN, seed[3]);
        pause(3000);
        dump("admin-design");
    }

    protected long[] seed() throws Exception {
        var auth = context.ppqhAuthService();
        var cart = context.ppqhCartService();
        var orders = context.ppqhOrderService();
        var payments = context.ppqhPaymentService();
        Path proof = Files.createTempFile("comprobante", ".png");
        writeProof(proof);
        auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray());
        long productId = context.ppqhAdminProductService().hepyList("SF-UFU-001", false).get(0).hepyId();
        long variant = context.ppqhAdminProductService().hepyGet(productId).hepyVariants().get(1).hepyId();
        auth.ppqhLogout();
        auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray());
        var doc = com.sportforce3d.design.PgeySampleDesigns.pgeyHalcones();
        var design = context.ppqhDesignService().pgeySave(null, "Halcones 2026", doc, null, null);
        long address = context.ppqhAddressService().ppqhList().get(0).ppqhId();
        cart.hepyAddDesign(design.pgeyId(), variant, 2);
        var paid = orders.ppqhPlaceOrder(address, com.sportforce3d.model.PpqhPaymentMethod.YAPE, null);
        payments.ppqhRegister(paid.ppqhId(), "48213077", proof);
        cart.hepyAdd(variant, 1);
        var pending = orders.ppqhPlaceOrder(address, com.sportforce3d.model.PpqhPaymentMethod.PLIN, null);
        cart.hepyAdd(variant, 1);
        auth.ppqhLogout();
        auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray());
        long designItem = context.ppqhAdminOrderService().ppqhOrder(paid.ppqhId()).ppqhItems().stream()
                .filter(i -> i.ppqhHasDesign()).findFirst().orElseThrow().ppqhId();
        auth.ppqhLogout();
        return new long[]{productId, pending.ppqhId(), paid.ppqhId(), designItem};
    }

    protected void login(String email, String password) throws Exception {
        fx(() -> {
            if (!context.ppqhSession().ppqhIsAuthenticated()) {
                context.ppqhNavigator().ppqhShowLogin();
            }
            return null;
        });
        pause(400);
        fx(() -> {
            field("Correo").setText(email);
            field("Contraseña").setText(password);
            click(text("Iniciar sesión"));
            return null;
        });
        pause(2500);
    }

    protected void logout() throws Exception {
        fx(() -> {
            context.ppqhNavigator().ppqhLogout();
            return null;
        });
        pause(800);
    }

    protected void go(String route, Object param) throws Exception {
        fx(() -> {
            context.ppqhRouter().ppqhGo(route, param);
            return null;
        });
        pause(1500);
    }

    protected void shot(String id, String caption, Node... marks) throws Exception {
        BufferedImage img = fx(() -> {
            WritableImage snap = stage.getScene().snapshot(null);
            BufferedImage base = toAwt(snap);
            double kx = base.getWidth() / stage.getScene().getWidth();
            double ky = base.getHeight() / stage.getScene().getHeight();
            List<Object[]> pops = new ArrayList<>();
            int minX = 0;
            int minY = 0;
            int maxX = base.getWidth();
            int maxY = base.getHeight();
            for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                if (w instanceof javafx.stage.PopupWindow pw && pw.isShowing() && pw.getScene() != null
                        && !(pw instanceof javafx.scene.control.Tooltip)) {
                    javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters();
                    sp.setFill(javafx.scene.paint.Color.TRANSPARENT);
                    WritableImage ps = pw.getScene().getRoot().snapshot(sp, null);
                    BufferedImage pi = toAwtArgb(ps);
                    Bounds rb = pw.getScene().getRoot().localToScreen(pw.getScene().getRoot().getBoundsInLocal());
                    Point2D origin = stage.getScene().getRoot().localToScreen(0, 0);
                    int x = (int) Math.round((rb.getMinX() - origin.getX()) * kx);
                    int y = (int) Math.round((rb.getMinY() - origin.getY()) * ky);
                    int w2 = (int) Math.round(ps.getWidth() * kx);
                    int h2 = (int) Math.round(ps.getHeight() * ky);
                    pops.add(new Object[]{pi, x, y, w2, h2});
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x + w2);
                    maxY = Math.max(maxY, y + h2);
                }
            }
            if (pops.isEmpty()) {
                return base;
            }
            BufferedImage canvas = new BufferedImage(maxX - minX, maxY - minY, BufferedImage.TYPE_INT_RGB);
            Graphics2D pg = canvas.createGraphics();
            pg.setColor(Color.WHITE);
            pg.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            pg.drawImage(base, -minX, -minY, null);
            pg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            for (Object[] p : pops) {
                pg.drawImage((BufferedImage) p[0], (int) p[1] - minX, (int) p[2] - minY, (int) p[3], (int) p[4], null);
            }
            pg.dispose();
            offsetX = -minX;
            offsetY = -minY;
            return canvas;
        });
        List<Bounds> boxes = fx(() -> {
            List<Bounds> list = new ArrayList<>();
            for (Node n : marks) {
                if (n != null && n.getScene() != null) {
                    list.add(n.localToScene(n.getBoundsInLocal()));
                }
            }
            return list;
        });
        double sx = fx(() -> stage.getScene().getRoot().snapshot(null, null).getWidth()) / stage.getScene().getWidth();
        double sy = sx;
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xE5, 0x1A, 0x1A));
        g.setStroke(new BasicStroke((float) (3 * sx)));
        for (Bounds b : boxes) {
            int pad = (int) Math.round(4 * sx);
            int x = (int) Math.round(b.getMinX() * sx) - pad + offsetX;
            int y = (int) Math.round(b.getMinY() * sy) - pad + offsetY;
            int w = (int) Math.round(b.getWidth() * sx) + 2 * pad;
            int h = (int) Math.round(b.getHeight() * sy) + 2 * pad;
            g.drawRoundRect(x, y, w, h, (int) (10 * sx), (int) (10 * sy));
        }
        g.dispose();
        offsetX = 0;
        offsetY = 0;
        Path file = out.resolve(id + ".png");
        ImageIO.write(img, "png", file.toFile());
        manifest.add(id + "\t" + caption.replace('\t', ' '));
        System.out.println("Captura: " + file.getFileName() + " (" + boxes.size() + " marcas)");
    }

    protected void drag(Node target, double dx, double dy, int steps) throws Exception {
        fx(() -> {
            Bounds b = target.getBoundsInLocal();
            double x0 = b.getCenterX();
            double y0 = b.getCenterY();
            fireMouse(target, MouseEvent.MOUSE_PRESSED, x0, y0, true);
            for (int i = 1; i <= steps; i++) {
                fireMouse(target, MouseEvent.MOUSE_DRAGGED, x0 + dx * i / steps, y0 + dy * i / steps, true);
            }
            fireMouse(target, MouseEvent.MOUSE_RELEASED, x0 + dx, y0 + dy, false);
            return null;
        });
    }

    protected void fireMouse(Node target, javafx.event.EventType<MouseEvent> type, double x, double y, boolean down) {
        Point2D scene = target.localToScene(x, y);
        Point2D screen = target.localToScreen(x, y);
        PickResult pick = new PickResult(target, scene.getX(), scene.getY());
        Event.fireEvent(target, new MouseEvent(type, x, y, screen.getX(), screen.getY(), MouseButton.PRIMARY, 1,
                false, false, false, false, down, false, false, true, false, false, pick));
    }

    protected void scroll(Node target, double deltaY) throws Exception {
        fx(() -> {
            Bounds b = target.getBoundsInLocal();
            Point2D screen = target.localToScreen(b.getCenterX(), b.getCenterY());
            Event.fireEvent(target, new javafx.scene.input.ScrollEvent(javafx.scene.input.ScrollEvent.SCROLL,
                    b.getCenterX(), b.getCenterY(), screen.getX(), screen.getY(), false, false, false, false, false, false,
                    0, deltaY, 0, deltaY, javafx.scene.input.ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    javafx.scene.input.ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null));
            return null;
        });
    }

    protected static BufferedImage toAwtArgb(WritableImage snap) {
        int w = (int) snap.getWidth();
        int h = (int) snap.getHeight();
        int[] px = new int[w * h];
        snap.getPixelReader().getPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, w);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, px, 0, w);
        return img;
    }

    protected static BufferedImage toAwt(WritableImage snap) {
        int w = (int) snap.getWidth();
        int h = (int) snap.getHeight();
        int[] px = new int[w * h];
        PixelReader r = snap.getPixelReader();
        r.getPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, w);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, w, h, px, 0, w);
        return img;
    }

    protected void dump(String id) throws Exception {
        List<String> lines = fx(() -> {
            List<String> l = new ArrayList<>();
            walk(stage.getScene().getRoot(), 0, l);
            return l;
        });
        Files.createDirectories(out.resolve("dump"));
        Files.write(out.resolve("dump/" + id + ".txt"), lines, StandardCharsets.UTF_8);
        shot("survey-" + id, id);
    }

    private static void walk(Node n, int depth, List<String> out) {
        if (!n.isVisible()) {
            return;
        }
        String desc = null;
        if (n instanceof Labeled l && l.getText() != null && !l.getText().isBlank()) {
            desc = l.getClass().getSimpleName() + " \"" + l.getText().replace('\n', ' ') + "\"" + (n instanceof ButtonBase ? " [boton]" : "")
                    + (l.isDisabled() ? " [deshabilitado]" : "");
        } else if (n instanceof TextInputControl t) {
            desc = t.getClass().getSimpleName() + " prompt=\"" + t.getPromptText() + "\" text=\"" + t.getText() + "\"";
        } else if (n instanceof ButtonBase b) {
            desc = b.getClass().getSimpleName() + " [boton sin texto] " + b.getStyleClass()
                    + (b.getTooltip() != null ? " tooltip=\"" + b.getTooltip().getText() + "\"" : "")
                    + (b.getAccessibleText() != null ? " accesible=\"" + b.getAccessibleText() + "\"" : "")
                    + (b.isDisabled() ? " [deshabilitado]" : "");
        } else if (n.getId() != null || n.getClass().getName().startsWith("com.sportforce3d")) {
            desc = n.getClass().getSimpleName() + (n.getId() != null ? " #" + n.getId() : "") + " " + n.getStyleClass();
        }
        if (desc != null) {
            out.add("  ".repeat(Math.min(depth, 30)) + desc);
        }
        if (n instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                walk(c, desc != null ? depth + 1 : depth, out);
            }
        }
    }

    protected Node root() {
        return stage.getScene().getRoot();
    }

    protected List<Node> all(Predicate<Node> test) {
        List<Node> list = new ArrayList<>();
        collect(root(), test, list);
        return list;
    }

    private static void collect(Node n, Predicate<Node> test, List<Node> list) {
        if (!n.isVisible()) {
            return;
        }
        if (test.test(n)) {
            list.add(n);
        }
        if (n instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                collect(c, test, list);
            }
        }
    }

    protected Node text(String text) {
        return text(text, 0);
    }

    protected Node text(String text, int index) {
        String want = norm(text);
        List<Node> found = all(n -> n instanceof Labeled l && l.getText() != null && norm(l.getText()).equals(want));
        found.sort((a, b) -> Boolean.compare(!(a instanceof ButtonBase), !(b instanceof ButtonBase)));
        if (found.size() <= index) {
            throw new IllegalStateException("No hay texto \"" + text + "\" (" + found.size() + ")");
        }
        return found.get(index);
    }

    protected Node acc(String accessible) {
        return acc(accessible, 0);
    }

    protected Node acc(String accessible, int index) {
        List<Node> found = all(n -> n.getAccessibleText() != null && norm(n.getAccessibleText()).equals(norm(accessible)));
        if (found.size() <= index) {
            throw new IllegalStateException("No hay control \"" + accessible + "\"");
        }
        return found.get(index);
    }

    protected Node textStarting(String prefix) {
        String want = norm(prefix);
        return all(n -> n instanceof Labeled l && l.getText() != null && norm(l.getText()).startsWith(want)).stream()
                .findFirst().orElseThrow(() -> new IllegalStateException("No hay texto que empiece con " + prefix));
    }

    protected static String norm(String s) {
        return s.replace('\n', ' ').trim().toLowerCase(Locale.ROOT);
    }

    protected PpqhSfTextField field(String label) {
        for (Node n : root().lookupAll(".sf-field")) {
            if (n instanceof PpqhSfTextField f && n.isVisible() && f.lookupAll(".sf-field-label").stream()
                    .anyMatch(l -> l instanceof Labeled ll && norm(ll.getText()).equals(norm(label)))) {
                return f;
            }
        }
        throw new IllegalStateException("No hay campo " + label);
    }

    protected Node css(String selector) {
        Node n = root().lookup(selector);
        if (n == null) {
            throw new IllegalStateException("No hay " + selector);
        }
        return n;
    }

    protected Node clickable(Node n) {
        for (Node c = n; c != null; c = c.getParent()) {
            if (c instanceof ButtonBase) {
                return c;
            }
        }
        return n;
    }

    protected void click(Node n) {
        Node target = clickable(n);
        if (target instanceof ButtonBase b) {
            b.fire();
            return;
        }
        Bounds b = target.getBoundsInLocal();
        double x = b.getCenterX();
        double y = b.getCenterY();
        Point2D scene = target.localToScene(x, y);
        Point2D screen = target.localToScreen(x, y);
        PickResult pick = new PickResult(target, scene.getX(), scene.getY());
        for (var type : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED)) {
            Event.fireEvent(target, new MouseEvent(type, x, y, screen.getX(), screen.getY(), MouseButton.PRIMARY, 1,
                    false, false, false, false, type == MouseEvent.MOUSE_PRESSED, false, false, true, false,
                    type == MouseEvent.MOUSE_CLICKED, pick));
        }
    }

    protected void clickFx(Node n) throws Exception {
        fx(() -> {
            click(n);
            return null;
        });
    }

    protected static void pause(long ms) throws InterruptedException {
        Thread.sleep(ms);
        CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(l::countDown);
        l.await(5, TimeUnit.SECONDS);
    }

    protected <T> T fx(Callable<T> work) throws Exception {
        CompletableFuture<T> f = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                f.complete(work.call());
            } catch (Throwable e) {
                f.completeExceptionally(e);
            }
        });
        return f.get(60, TimeUnit.SECONDS);
    }

    protected static void writeProof(Path file) throws IOException {
        BufferedImage img = new BufferedImage(360, 640, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x74, 0x2D, 0x8C));
        g.fillRect(0, 0, 360, 640);
        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 30));
        g.drawString("¡Pago exitoso!", 70, 200);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 20));
        g.drawString("Comprobante de ejemplo", 70, 360);
        g.dispose();
        ImageIO.write(img, "png", file.toFile());
    }
}
