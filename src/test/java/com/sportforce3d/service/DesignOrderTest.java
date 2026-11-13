package com.sportforce3d.service;

import com.sportforce3d.design.PgeyDesignDefaults;
import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeyDesignJson;
import com.sportforce3d.design.PgeyElementType;
import com.sportforce3d.design.PgeyMaterialSlot;
import com.sportforce3d.design.PgeySampleDesigns;
import com.sportforce3d.model.HepyCartLine;
import com.sportforce3d.model.HepyCartSummary;
import com.sportforce3d.model.PgeyDesignSummary;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhOrderItem;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.three.model.PgeyZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DesignOrderTest {

    private static final String KIT_SKU = "SF-UFU-001-M";

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

    private PgeyDesignSummary saveHalcones() {
        PgeyDesignDocument doc = PgeySampleDesigns.pgeyHalcones().pgeyWithProduct(f.productId("SF-UFU-001"));
        return f.designs.pgeySave(null, "Los Halcones", doc, front, back);
    }

    @Test
    void designLineCarriesThumbnailSizeQuantityAndMaterialSurcharge() {
        PgeyDesignSummary d = saveHalcones();
        HepyCartSummary cart = f.cart.hepyAddDesign(d.pgeyId(), f.variantId(KIT_SKU), 3);

        HepyCartLine line = cart.hepyLines().get(0);
        assertThat(line.hepyDesignId()).isEqualTo(d.pgeyId());
        assertThat(line.hepyDesignName()).isEqualTo("Los Halcones");
        assertThat(line.hepyDesignThumbnail()).isEqualTo(d.pgeyThumbnailFront());
        assertThat(line.hepySizeName()).isEqualTo("M");
        assertThat(line.hepyQuantity()).isEqualTo(3);
        assertThat(line.hepyUnitPriceCents()).isEqualTo(14990);
        assertThat(line.hepySurchargeCents()).isEqualTo(1500);
        assertThat(cart.hepySurchargeCents()).isEqualTo(3 * 1500);

        f.cart.hepyAddDesign(d.pgeyId(), f.variantId(KIT_SKU), 1);
        HepyCartSummary both = f.cart.hepyAdd(f.variantId(KIT_SKU), 1);
        assertThat(both.hepyLines()).hasSize(2);
        assertThat(both.hepyLines().get(0).hepyQuantity()).isEqualTo(4);
        assertThat(both.hepyLines().get(1).hepyHasDesign()).isFalse();
        assertThat(both.hepyLines().get(1).hepySurchargeCents()).isZero();
    }

    @Test
    void surchargeFollowsTheLatestVersionAndMaterialsMustBeAllowed() {
        PgeyDesignSummary d = saveHalcones();
        f.cart.hepyAddDesign(d.pgeyId(), f.variantId(KIT_SKU), 1);
        PgeyDesignDocument malla = f.designs.pgeyOpen(d.pgeyId()).pgeyDocument().pgeyWithMaterial(PgeyMaterialSlot.JERSEY, "MALLA");
        f.designs.pgeySave(d.pgeyId(), "Los Halcones", malla, null, null);
        assertThat(f.cart.hepySummary().hepyLines().get(0).hepySurchargeCents()).isEqualTo(500);

        PgeyDesignDocument microfibra = malla.pgeyWithMaterial(PgeyMaterialSlot.JERSEY, "MICROFIBRA");
        PgeyDesignSummary d2 = f.designs.pgeySave(null, "Microfibra", microfibra, null, null);
        assertThatThrownBy(() -> f.cart.hepyAddDesign(d2.pgeyId(), f.variantId(KIT_SKU), 1))
                .hasMessageContaining("Microfibra interlock").hasMessageContaining("no está disponible");
        assertThatThrownBy(() -> f.cart.hepyAddDesign(d.pgeyId(), f.variantId("SF-CAM-003-M"), 1))
                .hasMessageContaining("no se puede personalizar");
        f.loginNewClient("otra@correo.pe");
        assertThatThrownBy(() -> f.cart.hepyAddDesign(d.pgeyId(), f.variantId(KIT_SKU), 1))
                .hasMessage("No encontramos ese diseño.");
    }

    @Test
    void theFourFabricsOfTheDesignerUseTheCatalogSurcharges() {
        java.util.Map<String, Long> expected = java.util.Map.of("DRY_FIT", 0L, "POLIESTER_LISO", 0L, "ALGODON", 0L,
                "MALLA", 1000L);
        expected.forEach((code, cents) -> {
            PgeyDesignDocument doc = PgeySampleDesigns.pgeyHalcones().pgeyWithProduct(f.productId("SF-UFU-001"))
                    .pgeyWithMaterial(PgeyMaterialSlot.JERSEY, code).pgeyWithMaterial(PgeyMaterialSlot.SHORTS, code);
            PgeyDesignSummary d = f.designs.pgeySave(null, "Tela " + code, doc, front, back);
            HepyCartSummary cart = f.cart.hepyAddDesign(d.pgeyId(), f.variantId(KIT_SKU), 1);
            assertThat(cart.hepyLines().stream().filter(l -> Long.valueOf(d.pgeyId()).equals(l.hepyDesignId())).findFirst().orElseThrow()
                    .hepySurchargeCents()).as(code).isEqualTo(cents);
        });
    }

    @Test
    void orderKeepsAnImmutableCopyOfTheDesign() throws Exception {
        PgeyDesignSummary d = saveHalcones();
        f.cart.hepyAddDesign(d.pgeyId(), f.variantId(KIT_SKU), 2);
        PpqhOrderDetail order = f.orders.ppqhPlaceOrder(f.addresses.ppqhList().get(0).ppqhId(), PpqhPaymentMethod.YAPE, null);

        PpqhOrderItem item = order.ppqhItems().get(0);
        assertThat(item.ppqhHasDesign()).isTrue();
        assertThat(item.ppqhDesignName()).isEqualTo("Los Halcones");
        assertThat(item.ppqhDesignVersion()).isEqualTo(1);
        assertThat(item.ppqhSurchargeCents()).isEqualTo(1500);
        assertThat(item.ppqhLineTotalCents()).isEqualTo(2 * (14990 + 1500));
        assertThat(order.ppqhSurchargeCents()).isEqualTo(3000);
        assertThat(item.ppqhDesignFrontPath()).startsWith("orders/").isNotEqualTo(d.pgeyThumbnailFront());
        assertThat(item.ppqhDesignBackPath()).startsWith("orders/");
        assertThat(Files.exists(f.storage.hepyResolve(item.ppqhDesignFrontPath()))).isTrue();
        PgeyDesignDocument frozen = PgeyDesignJson.pgeyRead(item.ppqhDesignSnapshotJson());

        PgeyDesignDocument green = frozen.pgeyWithColor(PgeyZone.JERSEY_BODY, "#0E7C3A");
        f.designs.pgeySave(d.pgeyId(), "Otro nombre", green, png(tempDir.resolve("f2.png")), png(tempDir.resolve("b2.png")));
        f.designs.pgeyDelete(d.pgeyId());
        PpqhOrderItem again = f.orders.ppqhOrder(order.ppqhId()).ppqhItems().get(0);
        assertThat(PgeyDesignJson.pgeyRead(again.ppqhDesignSnapshotJson())).isEqualTo(frozen);
        assertThat(again.ppqhDesignName()).isEqualTo("Los Halcones");
        assertThat(Files.exists(f.storage.hepyResolve(again.ppqhDesignFrontPath()))).isTrue();
        assertThat(frozen.pgeyColor(PgeyZone.JERSEY_BODY)).isEqualTo("#D7192A");
    }

    @Test
    void deletedDesignBlocksCheckoutAndNothingIsCopied() throws Exception {
        PgeyDesignSummary d = saveHalcones();
        f.cart.hepyAddDesign(d.pgeyId(), f.variantId(KIT_SKU), 1);
        f.designs.pgeyDelete(d.pgeyId());
        assertThat(f.cart.hepySummary().hepyCanCheckout()).isFalse();
        assertThatThrownBy(() -> f.orders.ppqhPlaceOrder(f.addresses.ppqhList().get(0).ppqhId(), PpqhPaymentMethod.PLIN, null))
                .isInstanceOf(PpqhServiceException.class);
        Path orders = tempDir.resolve("storage").resolve("orders");
        assertThat(!Files.exists(orders) || Files.list(orders).findAny().isEmpty()).isTrue();
    }

    @Test
    void adminSeesTheTechSheetAndDownloadsTheOriginalFiles() throws Exception {
        Path logoFile = png(tempDir.resolve("escudo club.png"));
        var logo = f.designs.pgeyUploadLogo(logoFile, true);
        PgeyDesignDocument doc = PgeySampleDesigns.pgeyHalcones().pgeyWithProduct(f.productId("SF-UFU-001"))
                .pgeyAddElement(PgeyDesignDefaults.pgeyLogo(logo.pgeyId(), logo.pgeyPath()).pgeyWithRotation(15).pgeyWithScale(1.2));
        PgeyDesignSummary d = f.designs.pgeySave(null, "Con logo", doc, front, back);
        f.cart.hepyAddDesign(d.pgeyId(), f.variantId(KIT_SKU), 1);
        long itemId = f.orders.ppqhPlaceOrder(f.addresses.ppqhList().get(0).ppqhId(), PpqhPaymentMethod.TRANSFERENCIA, null)
                .ppqhItems().get(0).ppqhId();

        assertThatThrownBy(() -> f.adminOrders.ppqhDesign(itemId)).isInstanceOf(PpqhAccessDeniedException.class);
        f.loginAdmin();
        PpqhAdminOrderService.PpqhOrderDesign view = f.adminOrders.ppqhDesign(itemId);
        assertThat(view.ppqhSheet().pgeyZones()).anySatisfy(z -> {
            assertThat(z.pgeyZone()).isEqualTo(PgeyZone.JERSEY_BODY);
            assertThat(z.pgeyHex()).isEqualTo("#D7192A");
        });
        assertThat(view.ppqhSheet().pgeyMaterials()).anySatisfy(m -> {
            assertThat(m.pgeySlot()).isEqualTo(PgeyMaterialSlot.JERSEY);
            assertThat(m.pgeyName()).isEqualTo("Jacquard");
        });
        assertThat(view.ppqhSheet().pgeyElements()).anySatisfy(e -> {
            assertThat(e.pgeyType()).isEqualTo(PgeyElementType.LOGO);
            assertThat(e.pgeyAssetName()).isEqualTo("escudo club.png");
            assertThat(e.pgeyRotationDeg()).isEqualTo(15);
            assertThat(e.pgeyScalePercent()).isEqualTo(120);
        });
        assertThat(view.ppqhSheet().pgeyElements()).anySatisfy(e -> assertThat(e.pgeyContent()).isEqualTo("QUISPE"));

        Path dir = f.adminOrders.ppqhExportDesign(itemId, tempDir.resolve("descargas"));
        assertThat(dir.resolve("diseno.json")).exists();
        assertThat(dir.resolve("captura-frente.png")).exists();
        assertThat(dir.resolve("captura-espalda.png")).exists();
        assertThat(Files.readString(dir.resolve("ficha-tecnica.csv"))).contains("Jacquard", "#D7192A", "QUISPE");
        try (var files = Files.list(dir)) {
            assertThat(files.map(p -> p.getFileName().toString()))
                    .anyMatch(n -> n.startsWith("logo-" + logo.pgeyId()) && n.endsWith("escudo_club.png"));
        }
    }
}
