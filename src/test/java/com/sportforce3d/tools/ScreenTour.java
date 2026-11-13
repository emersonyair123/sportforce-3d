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
import com.sportforce3d.model.DesignSummary;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.model.SalesReport;
import com.sportforce3d.three.Png;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public final class ScreenTour {

    private static final int W = 1440;
    private static final int H = 900;

    private final Path out;
    private AppContext context;
    private Stage stage;
    private int count;

    private ScreenTour(Path out) {
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/capturas/final");
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            new ScreenTour(out).run();
        } finally {
            Platform.exit();
        }
    }

    private void run() throws Exception {
        Files.createDirectories(out);
        Path data = Files.createTempDirectory("sf-tour");
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
        Seed seed = seed();

        pause(800);
        shot("login");
        fx(() -> {
            field("Correo").setText("cliente@sportforce3d.pe");
            field("Contraseña").setText("equivocada");
            button("Iniciar sesión").fire();
            return null;
        });
        pause(1500);
        shot("login-error");
        fx(() -> {
            context.navigator().showRegister();
            return null;
        });
        pause(1500);
        shot("registro");

        login("cliente@sportforce3d.pe", "Cliente123!");
        shot("inicio");
        go(Routes.CATALOG, null);
        shot("catalogo");
        go(Routes.CATALOG, new Routes.CatalogParams("zapatilla voladora", null));
        shot("catalogo-sin-resultados");
        go(Routes.PRODUCT, seed.productId);
        shot("producto");
        go(Routes.CART, null);
        shot("carrito");
        go(Routes.CHECKOUT, null);
        shot("checkout");
        go(Routes.ORDERS, null);
        shot("mis-pedidos");
        go(Routes.ORDER, seed.pendingOrder);
        shot("pedido-pendiente-de-pago");
        go(Routes.ORDER, seed.productionOrder);
        shot("pedido-en-produccion");
        go(Routes.DESIGNS, null);
        shot("mis-disenos");
        go(Routes.DESIGNER, Routes.DesignerParams.open(seed.designId));
        pause(3500);
        shot("disenador-3d");
        go(Routes.ACCOUNT, null);
        shot("mi-cuenta");
        go(Routes.ADMIN_SETTINGS, null);
        shot("cliente-sin-acceso-admin");

        fx(() -> {
            context.navigator().logout();
            return null;
        });
        login("nuevo.cliente@correo.pe", "Futbol2026");
        go(Routes.CART, null);
        shot("carrito-vacio");
        go(Routes.ORDERS, null);
        shot("mis-pedidos-vacio");

        fx(() -> {
            context.navigator().logout();
            return null;
        });
        login("admin@sportforce3d.pe", "Admin123!");
        go(Routes.ADMIN_DASHBOARD, null);
        pause(800);
        shot("admin-dashboard");
        go(Routes.ADMIN_REPORTS, null);
        pause(800);
        shot("admin-reportes");
        go(Routes.ADMIN_SETTINGS, null);
        shot("admin-configuracion");
        fx(() -> {
            field("Número de Yape").setText("812 345");
            field("CCI (20 dígitos)").setText("0021");
            button("Guardar datos de pago").fire();
            return null;
        });
        pause(1200);
        shot("admin-configuracion-errores");
        fx(() -> {
            field("Número de Yape").setText("912 345 678");
            field("Titular de Yape").setText("SPORTFORCE 3D S.A.C.");
            field("Número de Plin").setText("923456789");
            field("Titular de Plin").setText("SPORTFORCE 3D S.A.C.");
            field("Banco").setText("BCP");
            field("Titular de la cuenta").setText("SPORTFORCE 3D S.A.C.");
            field("Número de cuenta").setText("191-23456789-0-12");
            field("CCI (20 dígitos)").setText("00219100234567890125");
            button("Guardar datos de pago").fire();
            return null;
        });
        pause(1500);
        shot("admin-configuracion-guardada");
        pause(3000);
        go(Routes.ADMIN_PRODUCTS, null);
        shot("admin-productos");
        go(Routes.ADMIN_PRODUCT_EDIT, seed.productId);
        shot("admin-producto-editar");
        go(Routes.ADMIN_INVENTORY, null);
        shot("admin-inventario");
        go(Routes.ADMIN_ORDERS, null);
        shot("admin-pedidos");
        go(Routes.ADMIN_ORDER, seed.productionOrder);
        shot("admin-pedido-detalle");
        go(Routes.ADMIN_DESIGN, seed.designItem);
        pause(3500);
        shot("admin-uniforme-3d");
        go(Routes.ADMIN_PAYMENTS, null);
        shot("admin-pagos");
        go(Routes.ADMIN_USERS, null);
        shot("admin-usuarios");

        Path pdf = context.reportService().exportPdf(seed.report, out.resolve("reporte-ventas-ejemplo.pdf"));
        Path xlsx = context.reportService().exportExcel(seed.report, out.resolve("reporte-ventas-ejemplo.xlsx"));
        System.out.println("Reportes: " + pdf.toAbsolutePath() + " · " + xlsx.toAbsolutePath());
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(pdf.toFile())) {
            org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                count++;
                Path page = out.resolve(String.format("%02d-reporte-pdf-pagina-%d.png", count, i + 1));
                javax.imageio.ImageIO.write(renderer.renderImageWithDPI(i, 110), "png", page.toFile());
                System.out.println("Captura: " + page);
            }
        }
        fx(() -> {
            stage.close();
            return null;
        });
        System.out.println(count + " capturas en " + out.toAbsolutePath());
    }

    private record Seed(long productId, long pendingOrder, long productionOrder, long designId, long designItem,
                        SalesReport report) {
    }

    private Seed seed() throws Exception {
        var auth = context.authService();
        var cart = context.cartService();
        var orders = context.orderService();
        var payments = context.paymentService();
        Path proof = Files.createTempFile("comprobante", ".png");
        writeProof(proof);

        auth.login("admin@sportforce3d.pe", "Admin123!".toCharArray());
        long productId = context.adminProductService().list("SF-UFU-001", false).get(0).id();
        long variant = variant(productId, "M");
        long variantL = variant(productId, "L");
        long shirt = variant(context.adminProductService().list("SF-CAM-001", false).get(0).id(), "M");
        long shorts = variant(context.adminProductService().list("SF-SHO-001", false).get(0).id(), "M");
        auth.logout();

        auth.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray());
        DesignDocument halcones = SampleDesigns.halcones();
        DesignSummary design = context.designService().save(null, "Halcones 2026", halcones,
                realisticShot(halcones, "futbol_hombre_frente"), realisticShot(halcones, "futbol_hombre_espalda"));
        long address = context.addressService().list().get(0).id();

        cart.addDesign(design.id(), variant, 2);
        OrderDetail production = orders.placeOrder(address, PaymentMethod.YAPE, "Entregar en el club, por la tarde.");
        payments.register(production.id(), "48213077", proof);
        cart.add(shirt, 3);
        OrderDetail paid = orders.placeOrder(address, PaymentMethod.TRANSFERENCIA, null);
        payments.register(paid.id(), "OP-778812", proof);
        cart.add(shorts, 2);
        OrderDetail verifying = orders.placeOrder(address, PaymentMethod.PLIN, null);
        payments.register(verifying.id(), "55129034", proof);
        cart.add(shirt, 1);
        OrderDetail pending = orders.placeOrder(address, PaymentMethod.YAPE, null);
        cart.add(shirt, 2);
        cart.add(variantL, 1);
        auth.logout();

        auth.register("Nuevo Cliente", "nuevo.cliente@correo.pe", null, "Futbol2026".toCharArray(),
                "Futbol2026".toCharArray());
        auth.logout();

        auth.login("admin@sportforce3d.pe", "Admin123!".toCharArray());
        payments.approve(paymentOf(production.id()), "Visto en la app de Yape");
        payments.approve(paymentOf(paid.id()), "Abono confirmado en el estado de cuenta");
        context.adminOrderService().changeStatus(production.id(), OrderStatus.EN_PRODUCCION, "Sublimado en curso");
        long designItem = context.adminOrderService().order(production.id()).items().stream()
                .filter(i -> i.hasDesign()).findFirst().orElseThrow().id();
        LocalDate today = context.reportService().today();
        SalesReport report = context.reportService().sales(today.minusDays(29), today);
        auth.logout();
        return new Seed(productId, pending.id(), production.id(), design.id(), designItem, report);
    }

    private long paymentOf(long orderId) {
        return context.adminOrderService().order(orderId).currentPayment().id();
    }

    private long variant(long productId, String size) {
        return context.adminProductService().get(productId).variants().stream()
                .filter(v -> v.sizeName().equals(size)).findFirst()
                .orElse(context.adminProductService().get(productId).variants().get(0)).id();
    }

    private static void writeProof(Path file) throws java.io.IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(360, 640, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(0x74, 0x2D, 0x8C));
        g.fillRect(0, 0, 360, 640);
        g.setColor(java.awt.Color.WHITE);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 30));
        g.drawString("¡Pago exitoso!", 70, 200);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 54));
        g.drawString("S/ 94.80", 70, 290);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 20));
        g.drawString("Comprobante de ejemplo", 70, 360);
        g.drawString("N.º de operación 55129034", 70, 395);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", file.toFile());
    }

    private Path realisticShot(DesignDocument doc, String modelKey) throws java.io.IOException {
        Path file = Files.createTempFile("sf-tour-" + modelKey, ".png");
        com.sportforce3d.render.RecolorService.writePng(context.recolorService().renderPixels(
                com.sportforce3d.render.RecolorRequest.of(modelKey, doc.realisticOrDefault())), file);
        return file;
    }

    private static Path existing(String path) {
        Path p = Path.of(path);
        return Files.exists(p) ? p : null;
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
        pause(2500);
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
        if (node instanceof javafx.scene.Parent p) {
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
