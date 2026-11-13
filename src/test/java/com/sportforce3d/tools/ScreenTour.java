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
import com.sportforce3d.model.PgeyDesignSummary;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.model.PpqhSalesReport;
import com.sportforce3d.three.PgeyPng;
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
    private PpqhAppContext context;
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
            context.ppqhNavigator().ppqhShowRegister();
            return null;
        });
        pause(1500);
        shot("registro");

        login("cliente@sportforce3d.pe", "Cliente123!");
        shot("inicio");
        go(PpqhRoutes.PPQH_CATALOG, null);
        shot("catalogo");
        go(PpqhRoutes.PPQH_CATALOG, new PpqhRoutes.PpqhCatalogParams("zapatilla voladora", null));
        shot("catalogo-sin-resultados");
        go(PpqhRoutes.PPQH_PRODUCT, seed.productId);
        shot("producto");
        go(PpqhRoutes.PPQH_CART, null);
        shot("carrito");
        go(PpqhRoutes.PPQH_CHECKOUT, null);
        shot("checkout");
        go(PpqhRoutes.PPQH_ORDERS, null);
        shot("mis-pedidos");
        go(PpqhRoutes.PPQH_ORDER, seed.pendingOrder);
        shot("pedido-pendiente-de-pago");
        go(PpqhRoutes.PPQH_ORDER, seed.productionOrder);
        shot("pedido-en-produccion");
        go(PpqhRoutes.PPQH_DESIGNS, null);
        shot("mis-disenos");
        go(PpqhRoutes.PPQH_DESIGNER, PpqhRoutes.PpqhDesignerParams.ppqhOpen(seed.designId));
        pause(3500);
        shot("disenador-3d");
        go(PpqhRoutes.PPQH_ACCOUNT, null);
        shot("mi-cuenta");
        go(PpqhRoutes.PPQH_ADMIN_SETTINGS, null);
        shot("cliente-sin-acceso-admin");

        fx(() -> {
            context.ppqhNavigator().ppqhLogout();
            return null;
        });
        login("nuevo.cliente@correo.pe", "Futbol2026");
        go(PpqhRoutes.PPQH_CART, null);
        shot("carrito-vacio");
        go(PpqhRoutes.PPQH_ORDERS, null);
        shot("mis-pedidos-vacio");

        fx(() -> {
            context.ppqhNavigator().ppqhLogout();
            return null;
        });
        login("admin@sportforce3d.pe", "Admin123!");
        go(PpqhRoutes.PPQH_ADMIN_DASHBOARD, null);
        pause(800);
        shot("admin-dashboard");
        go(PpqhRoutes.PPQH_ADMIN_REPORTS, null);
        pause(800);
        shot("admin-reportes");
        go(PpqhRoutes.PPQH_ADMIN_SETTINGS, null);
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
        go(PpqhRoutes.PPQH_ADMIN_PRODUCTS, null);
        shot("admin-productos");
        go(PpqhRoutes.PPQH_ADMIN_PRODUCT_EDIT, seed.productId);
        shot("admin-producto-editar");
        go(PpqhRoutes.PPQH_ADMIN_INVENTORY, null);
        shot("admin-inventario");
        go(PpqhRoutes.PPQH_ADMIN_ORDERS, null);
        shot("admin-pedidos");
        go(PpqhRoutes.PPQH_ADMIN_ORDER, seed.productionOrder);
        shot("admin-pedido-detalle");
        go(PpqhRoutes.PPQH_ADMIN_DESIGN, seed.designItem);
        pause(3500);
        shot("admin-uniforme-3d");
        go(PpqhRoutes.PPQH_ADMIN_PAYMENTS, null);
        shot("admin-pagos");
        go(PpqhRoutes.PPQH_ADMIN_USERS, null);
        shot("admin-usuarios");

        Path pdf = context.ppqhReportService().ppqhExportPdf(seed.report, out.resolve("reporte-ventas-ejemplo.pdf"));
        Path xlsx = context.ppqhReportService().ppqhExportExcel(seed.report, out.resolve("reporte-ventas-ejemplo.xlsx"));
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
                        PpqhSalesReport report) {
    }

    private Seed seed() throws Exception {
        var auth = context.ppqhAuthService();
        var cart = context.ppqhCartService();
        var orders = context.ppqhOrderService();
        var payments = context.ppqhPaymentService();
        Path proof = Files.createTempFile("comprobante", ".png");
        writeProof(proof);

        auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray());
        long productId = context.ppqhAdminProductService().hepyList("SF-UFU-001", false).get(0).hepyId();
        long variant = variant(productId, "M");
        long variantL = variant(productId, "L");
        long shirt = variant(context.ppqhAdminProductService().hepyList("SF-CAM-001", false).get(0).hepyId(), "M");
        long shorts = variant(context.ppqhAdminProductService().hepyList("SF-SHO-001", false).get(0).hepyId(), "M");
        auth.ppqhLogout();

        auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray());
        PgeyDesignDocument halcones = PgeySampleDesigns.pgeyHalcones();
        PgeyDesignSummary design = context.ppqhDesignService().pgeySave(null, "Halcones 2026", halcones,
                realisticShot(halcones, "futbol_hombre_frente"), realisticShot(halcones, "futbol_hombre_espalda"));
        long address = context.ppqhAddressService().ppqhList().get(0).ppqhId();

        cart.hepyAddDesign(design.pgeyId(), variant, 2);
        PpqhOrderDetail production = orders.ppqhPlaceOrder(address, PpqhPaymentMethod.YAPE, "Entregar en el club, por la tarde.");
        payments.ppqhRegister(production.ppqhId(), "48213077", proof);
        cart.hepyAdd(shirt, 3);
        PpqhOrderDetail paid = orders.ppqhPlaceOrder(address, PpqhPaymentMethod.TRANSFERENCIA, null);
        payments.ppqhRegister(paid.ppqhId(), "OP-778812", proof);
        cart.hepyAdd(shorts, 2);
        PpqhOrderDetail verifying = orders.ppqhPlaceOrder(address, PpqhPaymentMethod.PLIN, null);
        payments.ppqhRegister(verifying.ppqhId(), "55129034", proof);
        cart.hepyAdd(shirt, 1);
        PpqhOrderDetail pending = orders.ppqhPlaceOrder(address, PpqhPaymentMethod.YAPE, null);
        cart.hepyAdd(shirt, 2);
        cart.hepyAdd(variantL, 1);
        auth.ppqhLogout();

        auth.ppqhRegister("Nuevo Cliente", "nuevo.cliente@correo.pe", null, "Futbol2026".toCharArray(),
                "Futbol2026".toCharArray());
        auth.ppqhLogout();

        auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray());
        payments.ppqhApprove(paymentOf(production.ppqhId()), "Visto en la app de Yape");
        payments.ppqhApprove(paymentOf(paid.ppqhId()), "Abono confirmado en el estado de cuenta");
        context.ppqhAdminOrderService().ppqhChangeStatus(production.ppqhId(), PpqhOrderStatus.EN_PRODUCCION, "Sublimado en curso");
        long designItem = context.ppqhAdminOrderService().ppqhOrder(production.ppqhId()).ppqhItems().stream()
                .filter(i -> i.ppqhHasDesign()).findFirst().orElseThrow().ppqhId();
        LocalDate today = context.ppqhReportService().ppqhToday();
        PpqhSalesReport report = context.ppqhReportService().ppqhSales(today.minusDays(29), today);
        auth.ppqhLogout();
        return new Seed(productId, pending.ppqhId(), production.ppqhId(), design.pgeyId(), designItem, report);
    }

    private long paymentOf(long orderId) {
        return context.ppqhAdminOrderService().ppqhOrder(orderId).ppqhCurrentPayment().ppqhId();
    }

    private long variant(long productId, String size) {
        return context.ppqhAdminProductService().hepyGet(productId).hepyVariants().stream()
                .filter(v -> v.hepySizeName().equals(size)).findFirst()
                .orElse(context.ppqhAdminProductService().hepyGet(productId).hepyVariants().get(0)).hepyId();
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

    private Path realisticShot(PgeyDesignDocument doc, String modelKey) throws java.io.IOException {
        Path file = Files.createTempFile("sf-tour-" + modelKey, ".png");
        com.sportforce3d.render.PgeyRecolorService.pgeyWritePng(context.ppqhRecolorService().pgeyRenderPixels(
                com.sportforce3d.render.PgeyRecolorRequest.pgeyOf(modelKey, doc.pgeyRealisticOrDefault())), file);
        return file;
    }

    private static Path existing(String path) {
        Path p = Path.of(path);
        return Files.exists(p) ? p : null;
    }

    private void login(String email, String password) throws Exception {
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
        pause(2500);
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
        count++;
        Path file = out.resolve(String.format("%02d-%s.png", count, name));
        PgeyPng.pgeyWrite(img, file);
        System.out.println("Captura: " + file);
    }

    private PpqhSfTextField field(String label) {
        for (Node n : stage.getScene().getRoot().lookupAll(".sf-field")) {
            if (n instanceof PpqhSfTextField f && f.lookupAll(".sf-field-label").stream()
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
        if (node instanceof javafx.scene.Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                PpqhSfButton f = find(child, text);
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
