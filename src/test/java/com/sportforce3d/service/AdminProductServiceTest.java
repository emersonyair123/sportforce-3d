package com.sportforce3d.service;

import com.sportforce3d.model.AdminProductRow;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.InventoryRow;
import com.sportforce3d.model.LowStockVariant;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.model.ProductForm;
import com.sportforce3d.model.ProductForm.VariantForm;
import com.sportforce3d.model.ProductImage;
import com.sportforce3d.model.ProductQuery;
import com.sportforce3d.model.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminProductServiceTest {

    @TempDir
    Path tempDir;

    private Fixture f;
    private long sizeM;
    private long sizeL;
    private long dryFit;
    private long knit;

    @BeforeEach
    void setUp() {
        f = new Fixture(tempDir);
        sizeM = f.queryLong("SELECT id FROM sizes WHERE code = 'M'");
        sizeL = f.queryLong("SELECT id FROM sizes WHERE code = 'L'");
        dryFit = f.queryLong("SELECT id FROM materials WHERE code = 'DRY_FIT'");
        knit = f.queryLong("SELECT id FROM materials WHERE code = 'KNIT'");
    }

    private ProductForm form(Long id, String sku, long price, Long compareAt, Set<Long> materials, List<VariantForm> variants) {
        return new ProductForm(id, 1, 1L, sku, "Camiseta Halcones 2026", "Edición especial", Garment.JERSEY, price,
                compareAt, true, true, false, true, materials, variants);
    }

    private VariantForm variant(Long id, long sizeId, String sku, int stock) {
        return new VariantForm(id, sizeId, "Negro", "#0B0E13", sku, 0, stock, 5, true);
    }

    @Test
    void clienteCannotManageProducts() {
        f.loginClient();
        assertThatThrownBy(() -> f.admin.list(null, false)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> f.admin.save(form(null, "SF-HAL-001", 8990, null, Set.of(), List.of())))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> f.admin.updateStock(f.variantId("SF-CAM-001-M"), 1, 1)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> f.admin.delete(1)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createsProductVisibleInTheStore() {
        f.loginAdmin();
        ProductDetail created = f.admin.save(form(null, "SF-HAL-001", 8990, 10990L, Set.of(dryFit),
                List.of(variant(null, sizeM, "SF-HAL-001-M", 10), variant(null, sizeL, "SF-HAL-001-L", 4))));

        assertThat(created.variants()).hasSize(2);
        assertThat(created.materials()).extracting(m -> m.code()).containsExactly("DRY_FIT");
        assertThat(f.catalog.search(new ProductQuery("halcones", null, null, Set.of(), null, null, false, false,
                ProductSort.NAME, 1, 12)).items()).singleElement()
                .satisfies(p -> {
                    assertThat(p.stock()).isEqualTo(14);
                    assertThat(p.discountPercent()).isEqualTo(18);
                });
    }

    @Test
    void validatesProductData() {
        f.loginAdmin();
        assertThatThrownBy(() -> f.admin.save(form(null, "SF-CAM-001", 8990, null, Set.of(), List.of(variant(null, sizeM, "X-NUEVO-M", 1)))))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors()).containsKey("sku"));
        assertThatThrownBy(() -> f.admin.save(form(null, "SF-HAL-002", 8990, 8990L, Set.of(), List.of(variant(null, sizeM, "SF-HAL-002-M", 1)))))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors()).containsKey("compareAt"));
        assertThatThrownBy(() -> f.admin.save(form(null, "SF-HAL-003", 8990, null, Set.of(knit), List.of(variant(null, sizeM, "SF-HAL-003-M", 1)))))
                .as("una camiseta no admite materiales de calzado")
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors()).containsKey("materials"));
        assertThatThrownBy(() -> f.admin.save(form(null, "SF-HAL-004", 8990, null, Set.of(), List.of())))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors()).containsKey("variants"));
        assertThatThrownBy(() -> f.admin.save(form(null, "SF-HAL-005", 8990, null, Set.of(),
                List.of(variant(null, sizeM, "SF-HAL-005-M", 1), variant(null, sizeM, "SF-HAL-005-M2", 1)))))
                .as("misma talla y color").isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.admin.save(form(null, "SF-HAL-006", 8990, null, Set.of(),
                List.of(new VariantForm(null, sizeM, "Negro", "negro", "SF-HAL-006-M", 0, 1, 5, true)))))
                .as("HEX inválido").isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.admin.save(form(null, "SF-HAL-007", 8990, null, Set.of(),
                List.of(variant(null, sizeM, "SF-CAM-001-M", 1)))))
                .as("SKU de variante usado por otro producto")
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors()).containsKey("variants"));
    }

    @Test
    void updatesPriceAndStockButNeverBelowReserved() {
        f.loginAdmin();
        ProductDetail p = f.admin.save(form(null, "SF-HAL-010", 8990, null, Set.of(),
                List.of(variant(null, sizeM, "SF-HAL-010-M", 5))));
        long variantId = p.variants().get(0).id();

        // Un cliente reserva 3 unidades con un pedido sin pagar.
        f.loginClient();
        f.cart.add(variantId, 3);
        f.orders.placeOrder(f.addresses.list().get(0).id(), PaymentMethod.YAPE, null);

        f.loginAdmin();
        assertThatThrownBy(() -> f.admin.updateStock(variantId, 2, 5)).hasMessageContaining("3 unidades reservadas");
        InventoryRow row = f.admin.updateStock(variantId, 8, 2);
        assertThat(row.available()).isEqualTo(5);

        ProductDetail updated = f.admin.save(form(p.id(), "SF-HAL-010", 7990, null, Set.of(),
                List.of(variant(variantId, sizeM, "SF-HAL-010-M", 8))));
        assertThat(updated.basePriceCents()).isEqualTo(7990);
        assertThatThrownBy(() -> f.admin.save(form(p.id(), "SF-HAL-010", 7990, null, Set.of(),
                List.of(variant(variantId, sizeM, "SF-HAL-010-M", 1)))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.fieldErrors().get("variants")).contains("reservadas"));
    }

    /** Etapa 12B: la suela de la zapatilla de running (zona "paneles") es blanca y el ADMIN puede cambiarla. */
    @Test
    void adminChangesTheZoneColorsOfTheRealisticPhoto() {
        long id = f.productId("SF-ZAP-003");
        f.loginClient();
        assertThatThrownBy(() -> f.admin.updateZoneColors(id, java.util.Map.of("paneles", "#0B0E13")))
                .isInstanceOf(AccessDeniedException.class);
        f.loginAdmin();
        assertThat(f.admin.look(id).orElseThrow().zoneColors()).containsEntry("paneles", "#f2f5f8");

        var look = f.admin.updateZoneColors(id, java.util.Map.of("paneles", "#0b0e13"));
        assertThat(look.modelKey()).isEqualTo("producto_zapatilla_running");
        assertThat(look.zoneColors()).containsEntry("paneles", "#0b0e13").containsEntry("principal", "#1f5fff");
        assertThat(f.catalog.realisticLook(id).orElseThrow().zoneColors()).containsEntry("paneles", "#0b0e13");

        assertThatThrownBy(() -> f.admin.updateZoneColors(id, java.util.Map.of("camiseta", "#0B0E13")))
                .as("zona que la foto no tiene").isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.admin.updateZoneColors(id, java.util.Map.of("paneles", "blanco")))
                .as("HEX inválido").isInstanceOf(ValidationException.class);
    }

    @Test
    void softDeleteAndRestore() {
        f.loginAdmin();
        long id = f.productId("SF-CAM-003");
        f.admin.delete(id);
        assertThat(f.admin.list("reflect", false)).isEmpty();
        assertThat(f.admin.list("reflect", true)).singleElement().satisfies(r -> assertThat(r.deleted()).isTrue());

        f.admin.restore(id);
        assertThat(f.admin.get(id).deleted()).isFalse();
        assertThat(f.admin.get(id).active()).as("queda inactivo hasta que el ADMIN lo active").isFalse();
    }

    @Test
    void storesValidatedImagesAndDeletesTheFileOnRemove() throws Exception {
        f.loginAdmin();
        long id = f.productId("SF-CAM-001");
        Path png = tempDir.resolve("foto.png");
        Files.write(png, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0});
        Path fake = tempDir.resolve("virus.png");
        Files.writeString(fake, "esto no es una imagen");

        ProductImage image = f.admin.addImage(id, png);
        assertThat(image.path()).startsWith("products/").endsWith(".png").doesNotContain("foto");
        assertThat(Files.exists(f.storage.resolve(image.path()))).isTrue();
        assertThatThrownBy(() -> f.admin.addImage(id, fake)).isInstanceOf(ValidationException.class);

        f.admin.removeImage(id, image.id());
        assertThat(Files.exists(f.storage.resolve(image.path()))).isFalse();
        assertThat(f.admin.get(id).images()).isEmpty();
    }

    @Test
    void lowStockBadgeNamesTheVariantThatTriggersIt() {
        f.loginAdmin();
        // Camiseta Pro: 120 u. en total, pero la XXL tiene 2 (umbral 5) → baja, y se dice cuál.
        AdminProductRow jersey = f.admin.list("SF-CAM-001", false).get(0);
        assertThat(jersey.stock()).isGreaterThan(100);
        assertThat(jersey.lowStock()).isTrue();
        assertThat(jersey.lowStockVariants()).extracting(LowStockVariant::label).contains("Talla XXL: 2 u.");

        // Canilleras: una sola variante con 16 u. (> 5) → no es stock bajo.
        AdminProductRow shinGuards = f.admin.list("canilleras", false).get(0);
        assertThat(shinGuards.stock()).isEqualTo(16);
        assertThat(shinGuards.lowStock()).isFalse();

        // Al bajar a 5 (= umbral) se marca, y el total también queda en el umbral.
        f.admin.updateStock(f.variantId("SF-ACC-001-UNICA"), 5, 5);
        AdminProductRow after = f.admin.list("canilleras", false).get(0);
        assertThat(after.stock()).isEqualTo(5);
        assertThat(after.lowStockVariants()).extracting(LowStockVariant::label).containsExactly("Talla Única: 5 u.");

        // Producto agotado: todas sus tallas aparecen, marcadas como agotadas.
        AdminProductRow soldOut = f.admin.list("SF-ZAP-003", false).get(0);
        assertThat(soldOut.lowStockVariants()).hasSize(9)
                .allSatisfy(v -> assertThat(v.label()).endsWith("0 u. (agotada)"));
    }

    @Test
    void lowStockLabelShowsColorOnlyWhenProductHasSeveralColors() {
        assertThat(new LowStockVariant("M", "Negro", 2, 5, false).label()).isEqualTo("Talla M: 2 u.");
        assertThat(new LowStockVariant("M", "Negro", 0, 5, true).label()).isEqualTo("Talla M · Negro: 0 u. (agotada)");
    }

    @Test
    void inventoryListsLowStockFirst() {
        f.loginAdmin();
        List<InventoryRow> low = f.admin.inventory(null, true);
        assertThat(low).isNotEmpty().allMatch(InventoryRow::lowStock);
        assertThat(low).extracting(InventoryRow::sku).contains("SF-CAM-001-XXL", "SF-ZAP-003-40");
    }
}
