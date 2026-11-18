package com.sportforce3d.tools;

import com.sportforce3d.app.PpqhRoutes;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SubScene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.WritableImage;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public final class ManualBotones extends ManualTour {

    private final List<String> fallos = new ArrayList<>();

    private ManualBotones(Path out) {
        super(out);
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "target/manual-botones");
        Path db = Path.of(args.length > 1 ? args[1] : "target/no-existe.db");
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            ManualBotones tour = new ManualBotones(out);
            tour.open(db);
            tour.run();
            Files.write(out.resolve("fallos.txt"), tour.fallos, StandardCharsets.UTF_8);
            System.out.println("FALLOS: " + tour.fallos.size());
            tour.fallos.forEach(System.out::println);
            tour.fx(() -> {
                tour.stage.close();
                return null;
            });
        } finally {
            Platform.exit();
        }
    }

    private interface Paso {
        void run() throws Exception;
    }

    private interface Listo {
        boolean ok() throws Exception;
    }

    private Listo listo = null;

    private void esperar(Listo cond, long maxMs) throws Exception {
        long end = System.currentTimeMillis() + maxMs;
        while (System.currentTimeMillis() < end) {
            if (fx(cond::ok)) {
                pause(1200);
                return;
            }
            pause(400);
        }
        fallos.add("tiempo agotado esperando");
    }

    private boolean hay(String cls) {
        return !all(n -> n.getClass().getSimpleName().equals(cls)).isEmpty();
    }

    private boolean sinTexto(String prefix) {
        return all(n -> n instanceof Labeled l && l.getText() != null && norm(l.getText()).startsWith(norm(prefix))).isEmpty();
    }

    private void pantalla(String id, int height, long wait, Listo cond, Paso action) {
        listo = cond;
        pantalla(id, height, wait, action);
    }

    private int base = 800;

    private void pantalla(String id, int height, long wait, Paso action) {
        try {
            fx(() -> {
                stage.setWidth(1340);
                stage.setHeight(height < 0 ? base : height);
                return null;
            });
            pause(300);
            if (action != null) {
                fx(() -> {
                    action.run();
                    return null;
                });
            }
            pause(wait);
            if (listo != null) {
                esperar(listo, 60000);
                listo = null;
            }
            if (height < 0) {
                ajustarAlto(-height);
            }
            capturar(id);
        } catch (Throwable e) {
            fallos.add(id + ": " + e);
            System.out.println("FALLO " + id + ": " + e);
        }
        try {
            fx(() -> {
                for (Window w : List.copyOf(Window.getWindows())) {
                    if (w instanceof PopupWindow pw) {
                        pw.hide();
                    }
                }
                return null;
            });
        } catch (Exception e) {
            fallos.add(id + ": ventanas emergentes");
        }
    }

    private long[] preparar() throws Exception {
        var auth = context.ppqhAuthService();
        auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray());
        var doc = com.sportforce3d.design.PgeySampleDesigns.pgeyHalcones();
        long design = context.ppqhDesignService().pgeySave(null, "Halcones 2026", doc, null, null).pgeyId();
        auth.ppqhLogout();
        login("cliente@sportforce3d.pe", "Cliente123!");
        go(PpqhRoutes.PPQH_DESIGNER, PpqhRoutes.PpqhDesignerParams.ppqhOpen(design));
        pause(4000);
        miniaturas(design, doc);
        long productId = sql("SELECT id FROM products WHERE sku = 'SF-UFU-001'");
        long variant = sql("SELECT id FROM product_variants WHERE product_id = " + productId + " ORDER BY id LIMIT 1 OFFSET 1");
        Path proof = Files.createTempFile("comprobante", ".png");
        writeProof(proof);
        var cart = context.ppqhCartService();
        var orders = context.ppqhOrderService();
        long address = context.ppqhAddressService().ppqhList().get(0).ppqhId();
        cart.hepyAddDesign(design, variant, 2);
        var paid = orders.ppqhPlaceOrder(address, com.sportforce3d.model.PpqhPaymentMethod.YAPE, null);
        context.ppqhPaymentService().ppqhRegister(paid.ppqhId(), "48213077", proof);
        cart.hepyAdd(variant, 1);
        var pending = orders.ppqhPlaceOrder(address, com.sportforce3d.model.PpqhPaymentMethod.PLIN, null);
        cart.hepyAddDesign(design, variant, 1);
        cart.hepyAdd(variant, 1);
        long designItem = sql("SELECT id FROM order_items WHERE order_id = " + paid.ppqhId() + " AND design_snapshot_json IS NOT NULL");
        logout();
        return new long[]{productId, pending.ppqhId(), paid.ppqhId(), designItem, design};
    }

    @Override
    protected void run() throws Exception {
        long[] seed = preparar();
        long accesorio = sql("SELECT id FROM products WHERE sku = 'SF-ACC-004'");
        fx(() -> {
            context.ppqhNavigator().ppqhShowLogin();
            return null;
        });
        pantalla("01-login", 800, 7000, null);
        pantalla("02-registro", 800, 800, () -> context.ppqhNavigator().ppqhShowRegister());
        login("cliente@sportforce3d.pe", "Cliente123!");
        go(PpqhRoutes.PPQH_HOME, null);
        pantalla("03-barra", 800, 700, () -> click(css(".menu-button")));
        pantalla("04-inicio", -1400, 300, () -> ((MenuButton) css(".menu-button")).hide());
        go(PpqhRoutes.PPQH_CATALOG, new PpqhRoutes.PpqhCatalogParams("futbol", null));
        esperar(() -> hay("HepyProductCard"), 30000);
        pantalla("05-catalogo", 960, 2500, () -> hay("HepyProductCard"), () -> click(text("Siguiente")));
        pantalla("06-ficha", -1100, 1800, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_PRODUCT, seed[0]));
        pantalla("07-disenador-realista", 1500, 3500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_DESIGNER, null));
        pantalla("08-disenador-3d", 1500, 3000, () -> sinTexto("Cargando"), () -> click(text("Vista 3D")));
        pantalla("09-mis-disenos", -2000, 1800, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_DESIGNS, null));
        pantalla("10-carrito", -2000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CART, null));
        pantalla("11-checkout", -2000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CHECKOUT, null));
        pantalla("12-mis-pedidos", -2000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ORDERS, null));
        pantalla("13-detalle-pedido", -2000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ORDER, seed[1]));
        pantalla("14-mi-cuenta", -2000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ACCOUNT, null));
        logout();
        login("admin@sportforce3d.pe", "Admin123!");
        base = 960;
        pantalla("16-productos", -1000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_PRODUCTS, null));
        pantalla("17-producto-form", -2000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_PRODUCT_EDIT, accesorio));
        pantalla("18-inventario", -1000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_INVENTORY, null));
        pantalla("19-pedidos", -1000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_ORDERS, null));
        pantalla("20-pedido-admin", -2000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_ORDER, seed[2]));
        pantalla("21-visor-uniforme", 1000, 3000, () -> sinTexto("Cargando"), () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_DESIGN, seed[3]));
        pantalla("22-pagos", -1000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_PAYMENTS, null));
        long pago = sql("SELECT id FROM payments WHERE status = 'EN_VERIFICACION' LIMIT 1");
        context.ppqhPaymentService().ppqhApprove(pago, "Código verificado en la app de Yape");
        pantalla("15-dashboard", -2000, 1800, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_DASHBOARD, null));
        pantalla("23-reportes", -2000, 2500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_REPORTS, null));
        pantalla("24-usuarios", -1000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_USERS, null));
        pantalla("25-configuracion", -2000, 1500, () -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_SETTINGS, null));
    }

    private void ajustarAlto(int cap) throws Exception {
        for (int i = 0; i < 3; i++) {
            boolean changed = fx(() -> {
                ScrollPane best = null;
                double area = 0;
                for (Node n : all(x -> x instanceof ScrollPane)) {
                    Bounds b = n.localToScene(n.getBoundsInLocal());
                    if (b.getWidth() * b.getHeight() > area) {
                        area = b.getWidth() * b.getHeight();
                        best = (ScrollPane) n;
                    }
                }
                if (best == null || best.getContent() == null) {
                    return false;
                }
                double extra = best.getContent().getLayoutBounds().getHeight() - best.getViewportBounds().getHeight();
                if (extra <= 2 || stage.getHeight() >= cap) {
                    return false;
                }
                stage.setHeight(Math.min(cap, stage.getHeight() + extra + 4));
                return true;
            });
            if (!changed) {
                return;
            }
            pause(1200);
        }
    }

    private long sql(String query) throws Exception {
        return context.ppqhDatabase().hepyWithConnection(c -> {
            try (var st = c.createStatement(); var rs = st.executeQuery(query)) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        });
    }

    private void miniaturas(long design, com.sportforce3d.design.PgeyDesignDocument doc) throws Exception {
        List<javafx.scene.image.Image> fotos = fx(() -> {
            List<Node> views = all(n -> n instanceof javafx.scene.image.ImageView iv && iv.getImage() != null
                    && n.localToScene(n.getBoundsInLocal()).getWidth() > 200);
            views.sort((a, b) -> Double.compare(a.localToScene(0, 0).getX(), b.localToScene(0, 0).getX()));
            List<javafx.scene.image.Image> l = new ArrayList<>();
            for (Node v : views) {
                l.add(((javafx.scene.image.ImageView) v).getImage());
            }
            return l;
        });
        if (fotos.size() < 2) {
            fallos.add("miniaturas: " + fotos.size() + " fotos");
            return;
        }
        Path front = Files.createTempFile("frente", ".png");
        Path back = Files.createTempFile("espalda", ".png");
        ImageIO.write(toAwt(toWritable(fotos.get(0))), "png", front.toFile());
        ImageIO.write(toAwt(toWritable(fotos.get(1))), "png", back.toFile());
        context.ppqhDesignService().pgeySave(design, "Halcones 2026", doc, front, back);
    }

    private static WritableImage toWritable(javafx.scene.image.Image img) {
        return new WritableImage(img.getPixelReader(), (int) img.getWidth(), (int) img.getHeight());
    }

    private void capturar(String id) throws Exception {
        List<String> rows = new ArrayList<>();
        BufferedImage img = fx(() -> {
            WritableImage snap = stage.getScene().snapshot(null);
            BufferedImage base = toAwt(snap);
            double k = base.getWidth() / stage.getScene().getWidth();
            Point2D origin = stage.getScene().getRoot().localToScreen(0, 0);
            List<Object[]> pops = new ArrayList<>();
            int minX = 0;
            int minY = 0;
            int maxX = base.getWidth();
            int maxY = base.getHeight();
            for (Window w : Window.getWindows()) {
                if (w instanceof PopupWindow pw && pw.isShowing() && pw.getScene() != null
                        && !(pw instanceof javafx.scene.control.Tooltip)) {
                    javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters();
                    sp.setFill(javafx.scene.paint.Color.TRANSPARENT);
                    Parent pr = pw.getScene().getRoot();
                    WritableImage ps = pr.snapshot(sp, null);
                    Bounds rb = pr.localToScreen(pr.getBoundsInLocal());
                    int x = (int) Math.round((rb.getMinX() - origin.getX()) * k);
                    int y = (int) Math.round((rb.getMinY() - origin.getY()) * k);
                    int w2 = (int) Math.round(ps.getWidth() * k);
                    int h2 = (int) Math.round(ps.getHeight() * k);
                    pops.add(new Object[]{toAwtArgb(ps), x, y, w2, h2, pr});
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x + w2);
                    maxY = Math.max(maxY, y + h2);
                }
            }
            int ox = -minX;
            int oy = -minY;
            BufferedImage canvas = base;
            if (!pops.isEmpty()) {
                canvas = new BufferedImage(maxX - minX, maxY - minY, BufferedImage.TYPE_INT_RGB);
                Graphics2D pg = canvas.createGraphics();
                pg.setColor(java.awt.Color.WHITE);
                pg.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                pg.drawImage(base, ox, oy, null);
                for (Object[] p : pops) {
                    pg.drawImage((BufferedImage) p[0], (int) p[1] + ox, (int) p[2] + oy, (int) p[3], (int) p[4], null);
                }
                pg.dispose();
            }
            List<Node> found = new ArrayList<>();
            collectAll(stage.getScene().getRoot(), found);
            for (Object[] p : pops) {
                collectAll((Parent) p[5], found);
            }
            int idx = 0;
            for (Node n : found) {
                Bounds b = visible(n);
                if (b == null || b.getWidth() < 3 || b.getHeight() < 3) {
                    continue;
                }
                Bounds sb = n.getScene() == stage.getScene() ? n.localToScene(b) : null;
                double x;
                double y;
                if (sb != null) {
                    x = sb.getMinX() * k;
                    y = sb.getMinY() * k;
                } else {
                    Bounds scr = n.localToScreen(b);
                    x = (scr.getMinX() - origin.getX()) * k;
                    y = (scr.getMinY() - origin.getY()) * k;
                }
                rows.add(String.join("\t", String.valueOf(idx++), n.getClass().getSimpleName(), clean(key(n)),
                        String.valueOf(Math.round(x + ox)), String.valueOf(Math.round(y + oy)),
                        String.valueOf(Math.round(b.getWidth() * k)), String.valueOf(Math.round(b.getHeight() * k)),
                        n.isDisabled() ? "deshabilitado" : "", clean(context(n))));
            }
            return canvas;
        });
        ImageIO.write(img, "png", out.resolve(id + ".png").toFile());
        Files.write(out.resolve(id + ".tsv"), rows, StandardCharsets.UTF_8);
        System.out.println("Captura " + id + ": " + rows.size() + " controles");
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').trim();
    }

    private static boolean interactive(Node n) {
        if (n instanceof ButtonBase || n instanceof TextInputControl || n instanceof ComboBoxBase<?>
                || n instanceof ChoiceBox<?> || n instanceof Slider || n instanceof Spinner<?>
                || n instanceof TableView<?> || n instanceof SubScene) {
            return true;
        }
        if (n.getStyleClass().contains("tab") || n.getStyleClass().contains("menu-item")) {
            return true;
        }
        if (n.getOnMouseClicked() != null || n.getOnMousePressed() != null) {
            return true;
        }
        return n.getCursor() == Cursor.HAND;
    }

    private static void collectAll(Node n, List<Node> list) {
        if (!n.isVisible()) {
            return;
        }
        if (interactive(n)) {
            list.add(n);
            if (n instanceof TextInputControl || n instanceof ComboBoxBase<?> || n instanceof Slider
                    || n instanceof Spinner<?> || n instanceof ButtonBase && !(n instanceof MenuButton)) {
                return;
            }
        }
        if (n instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                collectAll(c, list);
            }
        }
    }

    private static Bounds visible(Node n) {
        Bounds b = n.getBoundsInLocal();
        Bounds scene = n.localToScene(b);
        double minX = scene.getMinX();
        double minY = scene.getMinY();
        double maxX = scene.getMaxX();
        double maxY = scene.getMaxY();
        for (Node p = n.getParent(); p != null; p = p.getParent()) {
            if (p instanceof ScrollPane sp) {
                Node vp = sp.lookup(".viewport");
                Bounds v = vp != null ? vp.localToScene(vp.getBoundsInLocal()) : sp.localToScene(sp.getBoundsInLocal());
                minX = Math.max(minX, v.getMinX());
                minY = Math.max(minY, v.getMinY());
                maxX = Math.min(maxX, v.getMaxX());
                maxY = Math.min(maxY, v.getMaxY());
            }
        }
        if (n.getScene() != null && n.getScene().getWindow() == null) {
            return null;
        }
        if (maxX <= minX || maxY <= minY) {
            return null;
        }
        Point2D a = n.sceneToLocal(minX, minY);
        Point2D c = n.sceneToLocal(maxX, maxY);
        return new BoundingBox(Math.min(a.getX(), c.getX()), Math.min(a.getY(), c.getY()),
                Math.abs(c.getX() - a.getX()), Math.abs(c.getY() - a.getY()));
    }

    private static String key(Node n) {
        StringBuilder sb = new StringBuilder();
        if (n instanceof Labeled l && l.getText() != null && !l.getText().isBlank()) {
            sb.append("texto=").append(l.getText()).append(' ');
        }
        if (n instanceof TextInputControl t) {
            sb.append("prompt=").append(t.getPromptText()).append(" valor=").append(t.getText()).append(' ');
        }
        if (n instanceof ComboBoxBase<?> c) {
            sb.append("valor=").append(c.getValue()).append(" prompt=").append(c.getPromptText()).append(' ');
        }
        if (n.getAccessibleText() != null) {
            sb.append("acc=").append(n.getAccessibleText()).append(' ');
        }
        if (n instanceof javafx.scene.control.Control c && c.getTooltip() != null) {
            sb.append("tip=").append(c.getTooltip().getText()).append(' ');
        }
        if (n.getId() != null) {
            sb.append("id=").append(n.getId()).append(' ');
        }
        if (sb.isEmpty() || n.getStyleClass().contains("tab") || n.getStyleClass().contains("menu-item")) {
            String inner = firstText(n);
            if (inner != null) {
                sb.append("interno=").append(inner).append(' ');
            }
            sb.append("css=").append(n.getStyleClass());
        }
        return sb.toString();
    }

    private static String firstText(Node n) {
        if (n instanceof Labeled l && l.getText() != null && !l.getText().isBlank()) {
            return l.getText();
        }
        if (n instanceof javafx.scene.text.Text t && t.getText() != null && !t.getText().isBlank()) {
            return t.getText();
        }
        if (n instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                String s = firstText(c);
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    private static String context(Node n) {
        for (Node p = n.getParent(); p != null; p = p.getParent()) {
            if (p.getClass().getName().startsWith("com.sportforce3d") || p.getId() != null) {
                return p.getClass().getSimpleName() + (p.getId() != null ? "#" + p.getId() : "");
            }
            if (p instanceof javafx.scene.control.TableRow<?>) {
                return "fila";
            }
            if (p instanceof SubScene) {
                return "subscene";
            }
        }
        return "";
    }
}
