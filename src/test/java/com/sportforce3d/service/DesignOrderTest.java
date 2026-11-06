package com.sportforce3d.service;

import com.sportforce3d.design.DesignDefaults;
import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.DesignJson;
import com.sportforce3d.design.ElementType;
import com.sportforce3d.design.MaterialSlot;
import com.sportforce3d.design.SampleDesigns;
import com.sportforce3d.model.CartLine;
import com.sportforce3d.model.CartSummary;
import com.sportforce3d.model.DesignSummary;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.OrderItem;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.three.model.Zone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Diseñador → carrito → pedido con copia inmutable del diseño → visor y descarga del ADMIN. */
class DesignOrderTest {

    private static final String KIT_SKU = "SF-UFU-001-M"; // Uniforme Fútbol Pro, S/ 149.90

    @TempDir
    Path tempDir;

    private Fixture f;
    private Path front;
    private Path back;

    @BeforeEach
    void setUp() throws Exception {
        f = new Fixture(tempDir);
        f.loginClient();
        front = png(tempDir.resolve("frente.png"));
        back = png(tempDir.resolve("espalda.png"));
    }

    private static Path png(Path p) throws Exception {
        Files.write(p, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3});
        return p;
    }

    private DesignSummary saveHalcones() {
        // Halcones usa Jacquard (+15) en la camiseta; el short y las medias van con su material de serie.
        DesignDocument doc = SampleDesigns.halcones().withProduct(f.productId("SF-UFU-001"));
        return f.designs.save(null, "Los Halcones", doc, front, back);
    }

    @Test
    void designLineCarriesThumbnailSizeQuantityAndMaterialSurcharge() {
        DesignSummary d = saveHalcones();
        CartSummary cart = f.cart.addDesign(d.id(), f.variantId(KIT_SKU), 3);

        CartLine line = cart.lines().get(0);
        assertThat(line.designId()).isEqualTo(d.id());
        assertThat(line.designName()).isEqualTo("Los Halcones");
        assertThat(line.designThumbnail()).isEqualTo(d.thumbnailFront());
        assertThat(line.sizeName()).isEqualTo("M");
        assertThat(line.quantity()).isEqualTo(3);
        assertThat(line.unitPriceCents()).isEqualTo(14990);
        assertThat(line.surchargeCents()).isEqualTo(1500);
        assertThat(cart.surchargeCents()).isEqualTo(3 * 1500);

        // Mismo diseño y talla: suma la cantidad. El mismo producto sin diseño va en otra línea.
        f.cart.addDesign(d.id(), f.variantId(KIT_SKU), 1);
        CartSummary both = f.cart.add(f.variantId(KIT_SKU), 1);
        assertThat(both.lines()).hasSize(2);
        assertThat(both.lines().get(0).quantity()).isEqualTo(4);
        assertThat(both.lines().get(1).hasDesign()).isFalse();
        assertThat(both.lines().get(1).surchargeCents()).isZero();
    }

    @Test
    void surchargeFollowsTheLatestVersionAndMaterialsMustBeAllowed() {
        DesignSummary d = saveHalcones();
        f.cart.addDesign(d.id(), f.variantId(KIT_SKU), 1);
        DesignDocument malla = f.designs.open(d.id()).document().withMaterial(MaterialSlot.JERSEY, "MALLA");
        f.designs.save(d.id(), "Los Halcones", malla, null, null);
        assertThat(f.cart.summary().lines().get(0).surchargeCents()).isEqualTo(500);

        // La microfibra (de buzos) no está permitida en el uniforme de fútbol.
        DesignDocument microfibra = malla.withMaterial(MaterialSlot.JERSEY, "MICROFIBRA");
        DesignSummary d2 = f.designs.save(null, "Microfibra", microfibra, null, null);
        assertThatThrownBy(() -> f.cart.addDesign(d2.id(), f.variantId(KIT_SKU), 1))
                .hasMessageContaining("Microfibra interlock").hasMessageContaining("no está disponible");
        // Un producto que no se personaliza.
        assertThatThrownBy(() -> f.cart.addDesign(d.id(), f.variantId("SF-CAM-003-M"), 1))
                .hasMessageContaining("no se puede personalizar");
        // El diseño de otro cliente.
        f.loginNewClient("otra@correo.pe");
        assertThatThrownBy(() -> f.cart.addDesign(d.id(), f.variantId(KIT_SKU), 1))
                .hasMessage("No encontramos ese diseño.");
    }

    @Test
    void theFourFabricsOfTheDesignerUseTheCatalogSurcharges() {
        // Sección TELA (Etapa 10B): la tela va en camiseta y short; Dry-fit, Poliéster liso y Algodón sin recargo,
        // Malla deportiva +S/ 5.00 por prenda (las medias siguen con su licra de serie).
        java.util.Map<String, Long> expected = java.util.Map.of("DRY_FIT", 0L, "POLIESTER_LISO", 0L, "ALGODON", 0L,
                "MALLA", 1000L);
        expected.forEach((code, cents) -> {
            DesignDocument doc = SampleDesigns.halcones().withProduct(f.productId("SF-UFU-001"))
                    .withMaterial(MaterialSlot.JERSEY, code).withMaterial(MaterialSlot.SHORTS, code);
            DesignSummary d = f.designs.save(null, "Tela " + code, doc, front, back);
            CartSummary cart = f.cart.addDesign(d.id(), f.variantId(KIT_SKU), 1);
            assertThat(cart.lines().stream().filter(l -> Long.valueOf(d.id()).equals(l.designId())).findFirst().orElseThrow()
                    .surchargeCents()).as(code).isEqualTo(cents);
        });
    }

    @Test
    void orderKeepsAnImmutableCopyOfTheDesign() throws Exception {
        DesignSummary d = saveHalcones();
        f.cart.addDesign(d.id(), f.variantId(KIT_SKU), 2);
        OrderDetail order = f.orders.placeOrder(f.addresses.list().get(0).id(), PaymentMethod.YAPE, null);

        OrderItem item = order.items().get(0);
        assertThat(item.hasDesign()).isTrue();
        assertThat(item.designName()).isEqualTo("Los Halcones");
        assertThat(item.designVersion()).isEqualTo(1);
        assertThat(item.surchargeCents()).isEqualTo(1500);
        assertThat(item.lineTotalCents()).isEqualTo(2 * (14990 + 1500));
        assertThat(order.surchargeCents()).isEqualTo(3000);
        assertThat(item.designFrontPath()).startsWith("orders/").isNotEqualTo(d.thumbnailFront());
        assertThat(item.designBackPath()).startsWith("orders/");
        assertThat(Files.exists(f.storage.resolve(item.designFrontPath()))).isTrue();
        DesignDocument frozen = DesignJson.read(item.designSnapshotJson());

        // El cliente cambia y borra el diseño: el pedido no cambia.
        DesignDocument green = frozen.withColor(Zone.JERSEY_BODY, "#0E7C3A");
        f.designs.save(d.id(), "Otro nombre", green, png(tempDir.resolve("f2.png")), png(tempDir.resolve("b2.png")));
        f.designs.delete(d.id());
        OrderItem again = f.orders.order(order.id()).items().get(0);
        assertThat(DesignJson.read(again.designSnapshotJson())).isEqualTo(frozen);
        assertThat(again.designName()).isEqualTo("Los Halcones");
        assertThat(Files.exists(f.storage.resolve(again.designFrontPath()))).isTrue();
        assertThat(frozen.color(Zone.JERSEY_BODY)).isEqualTo("#D7192A");
    }

    @Test
    void deletedDesignBlocksCheckoutAndNothingIsCopied() throws Exception {
        DesignSummary d = saveHalcones();
        f.cart.addDesign(d.id(), f.variantId(KIT_SKU), 1);
        f.designs.delete(d.id());
        assertThat(f.cart.summary().canCheckout()).isFalse();
        assertThatThrownBy(() -> f.orders.placeOrder(f.addresses.list().get(0).id(), PaymentMethod.PLIN, null))
                .isInstanceOf(ServiceException.class);
        Path orders = tempDir.resolve("storage").resolve("orders");
        assertThat(!Files.exists(orders) || Files.list(orders).findAny().isEmpty()).isTrue();
    }

    @Test
    void adminSeesTheTechSheetAndDownloadsTheOriginalFiles() throws Exception {
        Path logoFile = png(tempDir.resolve("escudo club.png"));
        var logo = f.designs.uploadLogo(logoFile, true);
        DesignDocument doc = SampleDesigns.halcones().withProduct(f.productId("SF-UFU-001"))
                .addElement(DesignDefaults.logo(logo.id(), logo.path()).withRotation(15).withScale(1.2));
        DesignSummary d = f.designs.save(null, "Con logo", doc, front, back);
        f.cart.addDesign(d.id(), f.variantId(KIT_SKU), 1);
        long itemId = f.orders.placeOrder(f.addresses.list().get(0).id(), PaymentMethod.TRANSFERENCIA, null)
                .items().get(0).id();

        assertThatThrownBy(() -> f.adminOrders.design(itemId)).isInstanceOf(AccessDeniedException.class);
        f.loginAdmin();
        AdminOrderService.OrderDesign view = f.adminOrders.design(itemId);
        assertThat(view.sheet().zones()).anySatisfy(z -> {
            assertThat(z.zone()).isEqualTo(Zone.JERSEY_BODY);
            assertThat(z.hex()).isEqualTo("#D7192A");
        });
        assertThat(view.sheet().materials()).anySatisfy(m -> {
            assertThat(m.slot()).isEqualTo(MaterialSlot.JERSEY);
            assertThat(m.name()).isEqualTo("Jacquard");
        });
        assertThat(view.sheet().elements()).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo(ElementType.LOGO);
            assertThat(e.assetName()).isEqualTo("escudo club.png");
            assertThat(e.rotationDeg()).isEqualTo(15);
            assertThat(e.scalePercent()).isEqualTo(120);
        });
        assertThat(view.sheet().elements()).anySatisfy(e -> assertThat(e.content()).isEqualTo("QUISPE"));

        Path dir = f.adminOrders.exportDesign(itemId, tempDir.resolve("descargas"));
        assertThat(dir.resolve("diseno.json")).exists();
        assertThat(dir.resolve("captura-frente.png")).exists();
        assertThat(dir.resolve("captura-espalda.png")).exists();
        assertThat(Files.readString(dir.resolve("ficha-tecnica.csv"))).contains("Jacquard", "#D7192A", "QUISPE");
        try (var files = Files.list(dir)) {
            assertThat(files.map(p -> p.getFileName().toString()))
                    .anyMatch(n -> n.startsWith("logo-" + logo.id()) && n.endsWith("escudo_club.png"));
        }
    }
}
