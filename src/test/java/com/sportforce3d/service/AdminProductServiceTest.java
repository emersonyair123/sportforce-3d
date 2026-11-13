package com.sportforce3d.service;

import com.sportforce3d.model.HepyAdminProductRow;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyInventoryRow;
import com.sportforce3d.model.HepyLowStockVariant;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.model.HepyProductForm;
import com.sportforce3d.model.HepyProductForm.HepyVariantForm;
import com.sportforce3d.model.HepyProductImage;
import com.sportforce3d.model.HepyProductQuery;
import com.sportforce3d.model.HepyProductSort;
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

    private HepyProductForm form(Long id, String sku, long price, Long compareAt, Set<Long> materials, List<HepyVariantForm> variants) {
        return new HepyProductForm(id, 1, 1L, sku, "Camiseta Halcones 2026", "Edición especial", HepyGarment.JERSEY, price,
                compareAt, true, true, false, true, materials, variants);
    }

    private HepyVariantForm variant(Long id, long sizeId, String sku, int stock) {
        return new HepyVariantForm(id, sizeId, "Negro", "#0B0E13", sku, 0, stock, 5, true);
    }

    @Test
    void clienteCannotManageProducts() {
        f.loginClient();
        assertThatThrownBy(() -> f.admin.hepyList(null, false)).isInstanceOf(PpqhAccessDeniedException.class);
        assertThatThrownBy(() -> f.admin.hepySave(form(null, "SF-HAL-001", 8990, null, Set.of(), List.of())))
                .isInstanceOf(PpqhAccessDeniedException.class);
        assertThatThrownBy(() -> f.admin.hepyUpdateStock(f.variantId("SF-CAM-001-M"), 1, 1)).isInstanceOf(PpqhAccessDeniedException.class);
        assertThatThrownBy(() -> f.admin.hepyDelete(1)).isInstanceOf(PpqhAccessDeniedException.class);
    }

    @Test
    void createsProductVisibleInTheStore() {
        f.loginAdmin();
        HepyProductDetail created = f.admin.hepySave(form(null, "SF-HAL-001", 8990, 10990L, Set.of(dryFit),
                List.of(variant(null, sizeM, "SF-HAL-001-M", 10), variant(null, sizeL, "SF-HAL-001-L", 4))));

        assertThat(created.hepyVariants()).hasSize(2);
        assertThat(created.hepyMaterials()).extracting(m -> m.hepyCode()).containsExactly("DRY_FIT");
        assertThat(f.catalog.hepySearch(new HepyProductQuery("halcones", null, null, Set.of(), null, null, false, false,
                HepyProductSort.NAME, 1, 12)).hepyItems()).singleElement()
                .satisfies(p -> {
                    assertThat(p.hepyStock()).isEqualTo(14);
                    assertThat(p.hepyDiscountPercent()).isEqualTo(18);
                });
    }

    @Test
    void validatesProductData() {
        f.loginAdmin();
        assertThatThrownBy(() -> f.admin.hepySave(form(null, "SF-CAM-001", 8990, null, Set.of(), List.of(variant(null, sizeM, "X-NUEVO-M", 1)))))
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors()).containsKey("sku"));
        assertThatThrownBy(() -> f.admin.hepySave(form(null, "SF-HAL-002", 8990, 8990L, Set.of(), List.of(variant(null, sizeM, "SF-HAL-002-M", 1)))))
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors()).containsKey("compareAt"));
        assertThatThrownBy(() -> f.admin.hepySave(form(null, "SF-HAL-003", 8990, null, Set.of(knit), List.of(variant(null, sizeM, "SF-HAL-003-M", 1)))))
                .as("una camiseta no admite materiales de calzado")
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors()).containsKey("materials"));
        assertThatThrownBy(() -> f.admin.hepySave(form(null, "SF-HAL-004", 8990, null, Set.of(), List.of())))
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors()).containsKey("variants"));
        assertThatThrownBy(() -> f.admin.hepySave(form(null, "SF-HAL-005", 8990, null, Set.of(),
                List.of(variant(null, sizeM, "SF-HAL-005-M", 1), variant(null, sizeM, "SF-HAL-005-M2", 1)))))
                .as("misma talla y color").isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.admin.hepySave(form(null, "SF-HAL-006", 8990, null, Set.of(),
                List.of(new HepyVariantForm(null, sizeM, "Negro", "negro", "SF-HAL-006-M", 0, 1, 5, true)))))
                .as("HEX inválido").isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.admin.hepySave(form(null, "SF-HAL-007", 8990, null, Set.of(),
                List.of(variant(null, sizeM, "SF-CAM-001-M", 1)))))
                .as("SKU de variante usado por otro producto")
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors()).containsKey("variants"));
    }

    @Test
    void updatesPriceAndStockButNeverBelowReserved() {
        f.loginAdmin();
        HepyProductDetail p = f.admin.hepySave(form(null, "SF-HAL-010", 8990, null, Set.of(),
                List.of(variant(null, sizeM, "SF-HAL-010-M", 5))));
        long variantId = p.hepyVariants().get(0).hepyId();

        f.loginClient();
        f.cart.hepyAdd(variantId, 3);
        f.orders.ppqhPlaceOrder(f.addresses.ppqhList().get(0).ppqhId(), PpqhPaymentMethod.YAPE, null);

        f.loginAdmin();
        assertThatThrownBy(() -> f.admin.hepyUpdateStock(variantId, 2, 5)).hasMessageContaining("3 unidades reservadas");
        HepyInventoryRow row = f.admin.hepyUpdateStock(variantId, 8, 2);
        assertThat(row.hepyAvailable()).isEqualTo(5);

        HepyProductDetail updated = f.admin.hepySave(form(p.hepyId(), "SF-HAL-010", 7990, null, Set.of(),
                List.of(variant(variantId, sizeM, "SF-HAL-010-M", 8))));
        assertThat(updated.hepyBasePriceCents()).isEqualTo(7990);
        assertThatThrownBy(() -> f.admin.hepySave(form(p.hepyId(), "SF-HAL-010", 7990, null, Set.of(),
                List.of(variant(variantId, sizeM, "SF-HAL-010-M", 1)))))
                .isInstanceOfSatisfying(PpqhValidationException.class,
                        e -> assertThat(e.ppqhFieldErrors().get("variants")).contains("reservadas"));
    }

    @Test
    void adminChangesTheZoneColorsOfTheRealisticPhoto() {
        long id = f.productId("SF-ZAP-003");
        f.loginClient();
        assertThatThrownBy(() -> f.admin.hepyUpdateZoneColors(id, java.util.Map.of("paneles", "#0B0E13")))
                .isInstanceOf(PpqhAccessDeniedException.class);
        f.loginAdmin();
        assertThat(f.admin.hepyLook(id).orElseThrow().hepyZoneColors()).containsEntry("paneles", "#f2f5f8");

        var look = f.admin.hepyUpdateZoneColors(id, java.util.Map.of("paneles", "#0b0e13"));
        assertThat(look.hepyModelKey()).isEqualTo("producto_zapatilla_running");
        assertThat(look.hepyZoneColors()).containsEntry("paneles", "#0b0e13").containsEntry("principal", "#1f5fff");
        assertThat(f.catalog.hepyRealisticLook(id).orElseThrow().hepyZoneColors()).containsEntry("paneles", "#0b0e13");

        assertThatThrownBy(() -> f.admin.hepyUpdateZoneColors(id, java.util.Map.of("camiseta", "#0B0E13")))
                .as("zona que la foto no tiene").isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.admin.hepyUpdateZoneColors(id, java.util.Map.of("paneles", "blanco")))
                .as("HEX inválido").isInstanceOf(PpqhValidationException.class);
    }

    @Test
    void softDeleteAndRestore() {
        f.loginAdmin();
        long id = f.productId("SF-CAM-003");
        f.admin.hepyDelete(id);
        assertThat(f.admin.hepyList("reflect", false)).isEmpty();
        assertThat(f.admin.hepyList("reflect", true)).singleElement().satisfies(r -> assertThat(r.hepyDeleted()).isTrue());

        f.admin.hepyRestore(id);
        assertThat(f.admin.hepyGet(id).hepyDeleted()).isFalse();
        assertThat(f.admin.hepyGet(id).hepyActive()).as("queda inactivo hasta que el ADMIN lo active").isFalse();
    }

    @Test
    void storesValidatedImagesAndDeletesTheFileOnRemove() throws Exception {
        f.loginAdmin();
        long id = f.productId("SF-CAM-001");
        Path png = tempDir.resolve("foto.png");
        Files.write(png, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0});
        Path fake = tempDir.resolve("virus.png");
        Files.writeString(fake, "esto no es una imagen");

        HepyProductImage image = f.admin.hepyAddImage(id, png);
        assertThat(image.hepyPath()).startsWith("products/").endsWith(".png").doesNotContain("foto");
        assertThat(Files.exists(f.storage.hepyResolve(image.hepyPath()))).isTrue();
        assertThatThrownBy(() -> f.admin.hepyAddImage(id, fake)).isInstanceOf(PpqhValidationException.class);

        f.admin.hepyRemoveImage(id, image.hepyId());
        assertThat(Files.exists(f.storage.hepyResolve(image.hepyPath()))).isFalse();
        assertThat(f.admin.hepyGet(id).hepyImages()).isEmpty();
    }

    @Test
    void lowStockBadgeNamesTheVariantThatTriggersIt() {
        f.loginAdmin();
        HepyAdminProductRow jersey = f.admin.hepyList("SF-CAM-001", false).get(0);
        assertThat(jersey.hepyStock()).isGreaterThan(100);
        assertThat(jersey.hepyLowStock()).isTrue();
        assertThat(jersey.hepyLowStockVariants()).extracting(HepyLowStockVariant::hepyLabel).contains("Talla XXL: 2 u.");

        HepyAdminProductRow shinGuards = f.admin.hepyList("canilleras", false).get(0);
        assertThat(shinGuards.hepyStock()).isEqualTo(16);
        assertThat(shinGuards.hepyLowStock()).isFalse();

        f.admin.hepyUpdateStock(f.variantId("SF-ACC-001-UNICA"), 5, 5);
        HepyAdminProductRow after = f.admin.hepyList("canilleras", false).get(0);
        assertThat(after.hepyStock()).isEqualTo(5);
        assertThat(after.hepyLowStockVariants()).extracting(HepyLowStockVariant::hepyLabel).containsExactly("Talla Única: 5 u.");

        HepyAdminProductRow soldOut = f.admin.hepyList("SF-ZAP-003", false).get(0);
        assertThat(soldOut.hepyLowStockVariants()).hasSize(9)
                .allSatisfy(v -> assertThat(v.hepyLabel()).endsWith("0 u. (agotada)"));
    }

    @Test
    void lowStockLabelShowsColorOnlyWhenProductHasSeveralColors() {
        assertThat(new HepyLowStockVariant("M", "Negro", 2, 5, false).hepyLabel()).isEqualTo("Talla M: 2 u.");
        assertThat(new HepyLowStockVariant("M", "Negro", 0, 5, true).hepyLabel()).isEqualTo("Talla M · Negro: 0 u. (agotada)");
    }

    @Test
    void inventoryListsLowStockFirst() {
        f.loginAdmin();
        List<HepyInventoryRow> low = f.admin.hepyInventory(null, true);
        assertThat(low).isNotEmpty().allMatch(HepyInventoryRow::hepyLowStock);
        assertThat(low).extracting(HepyInventoryRow::hepySku).contains("SF-CAM-001-XXL", "SF-ZAP-003-40");
    }
}
