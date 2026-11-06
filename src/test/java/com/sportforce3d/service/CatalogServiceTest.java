package com.sportforce3d.service;

import com.sportforce3d.model.Page;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.model.ProductQuery;
import com.sportforce3d.model.ProductSort;
import com.sportforce3d.model.ProductSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogServiceTest {

    @TempDir
    Path tempDir;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = new Fixture(tempDir);
    }

    private static ProductQuery query(String text, Long category, Set<String> sizes, Long min, Long max,
                                      boolean inStock, boolean customizable, ProductSort sort, int page) {
        return new ProductQuery(text, category, null, sizes, min, max, inStock, customizable, sort, page, 12);
    }

    @Test
    void paginatesTheWholeCatalogFromTheDatabase() {
        Page<ProductSummary> first = f.catalog.search(ProductQuery.all());
        // 26 de V2 + 42 de catalogo.json (V6; otros 2 ya existían con el mismo nombre y se actualizaron).
        assertThat(first.total()).as("68 - SF-BUZ-001 (V8)").isEqualTo(67);
        assertThat(first.totalPages()).isEqualTo(6);
        assertThat(first.items()).hasSize(12);

        Page<ProductSummary> last = f.catalog.search(ProductQuery.all().withPage(6));
        assertThat(last.items()).hasSize(8);
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void searchIgnoresAccentsAndCase() {
        Page<ProductSummary> plain = f.catalog.search(query("futbol", null, Set.of(), null, null, false, false, ProductSort.NAME, 1));
        Page<ProductSummary> accented = f.catalog.search(query("FÚTBOL", null, Set.of(), null, null, false, false, ProductSort.NAME, 1));
        assertThat(plain.items()).isNotEmpty();
        assertThat(accented.items()).extracting(ProductSummary::sku).containsExactlyElementsOf(
                plain.items().stream().map(ProductSummary::sku).toList());
        assertThat(plain.items()).extracting(ProductSummary::name).contains("Uniforme Fútbol Pro");
    }

    @Test
    void filtersByCategorySizePriceStockAndCustomizable() {
        assertThat(f.catalog.search(query(null, 2L, Set.of(), null, null, false, false, ProductSort.NAME, 1)).total())
                .as("uniformes de fútbol: 3 de V2 + 3 de catalogo.json").isEqualTo(6);

        Page<ProductSummary> size40 = f.catalog.search(query(null, null, Set.of("40"), null, null, false, false, ProductSort.NAME, 1));
        assertThat(size40.items()).allMatch(p -> p.garment() == com.sportforce3d.model.Garment.SHOES).hasSize(3);
        Page<ProductSummary> size40InStock = f.catalog.search(query(null, null, Set.of("40"), null, null, true, false, ProductSort.NAME, 1));
        assertThat(size40InStock.items()).extracting(ProductSummary::sku).doesNotContain("SF-ZAP-003");

        Page<ProductSummary> range = f.catalog.search(query(null, null, Set.of(), 5000L, 9000L, false, false, ProductSort.PRICE_ASC, 1));
        assertThat(range.items()).isNotEmpty()
                .allMatch(p -> p.priceCents() >= 5000 && p.priceCents() <= 9000);
        assertThat(range.items()).extracting(ProductSummary::priceCents).isSorted();

        assertThat(f.catalog.search(query(null, null, Set.of(), null, null, false, true, ProductSort.NAME, 1)).items())
                .allMatch(ProductSummary::customizable);
    }

    @Test
    void rejectsInvertedPriceRange() {
        assertThatThrownBy(() -> f.catalog.search(query(null, null, Set.of(), 9000L, 5000L, false, false, ProductSort.NAME, 1)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void relevancePutsOutOfStockLast() {
        Page<ProductSummary> all = f.catalog.search(new ProductQuery(null, null, null, Set.of(), null, null, false, false,
                ProductSort.RELEVANCE, 2, 60));
        assertThat(all.hasNext()).isFalse();
        assertThat(all.items().get(all.items().size() - 1).sku()).isEqualTo("SF-ZAP-003");
    }

    @Test
    void singleVariantProductsCanBeAddedWithoutChoosingSize() {
        ProductSummary canilleras = f.catalog.search(query("canilleras", null, Set.of(), null, null, false, false, ProductSort.NAME, 1))
                .items().get(0);
        assertThat(canilleras.variantCount()).isEqualTo(1);
        assertThat(canilleras.singleVariantId()).isEqualTo(f.variantId("SF-ACC-001-UNICA"));

        ProductSummary jersey = f.catalog.search(query("SF-CAM-001", null, Set.of(), null, null, false, false, ProductSort.NAME, 1))
                .items().get(0);
        assertThat(jersey.singleVariantId()).isNull();
        assertThat(jersey.colorHex()).isEqualTo("#1F5FFF");
    }

    @Test
    void deletedOrInactiveProductsAreHiddenFromTheStore() {
        long id = f.productId("SF-CAM-002");
        f.products.softDelete(id, Instant.now());

        assertThat(f.catalog.search(query("training malla", null, Set.of(), null, null, false, false, ProductSort.NAME, 1)).total()).isZero();
        assertThatThrownBy(() -> f.catalog.detail(id)).hasMessageContaining("ya no está disponible");
    }

    @Test
    void detailShowsOnlyActiveVariantsAndMaterials() {
        long id = f.productId("SF-UFU-001");
        f.exec("UPDATE product_variants SET active = 0 WHERE sku = 'SF-UFU-001-XS'");

        ProductDetail detail = f.catalog.detail(id);
        assertThat(detail.variants()).extracting(v -> v.sku()).doesNotContain("SF-UFU-001-XS").hasSize(5);
        assertThat(detail.materials()).extracting(m -> m.code()).contains("DRY_FIT", "JACQUARD").doesNotContain("KNIT");
        assertThat(detail.materials().get(0).code()).as("la tela predeterminada va primero").isEqualTo("DRY_FIT");
    }

    @Test
    void shippingIsFreeFromTheConfiguredThreshold() {
        assertThat(f.catalog.shippingFor(0)).isZero();
        assertThat(f.catalog.shippingFor(29_899)).isEqualTo(1500);
        assertThat(f.catalog.shippingFor(29_900)).isZero();
    }
}