package com.sportforce3d.service;

import com.sportforce3d.model.HepyPage;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.model.HepyProductQuery;
import com.sportforce3d.model.HepyProductSort;
import com.sportforce3d.model.HepyProductSummary;
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

    private static HepyProductQuery query(String text, Long category, Set<String> sizes, Long min, Long max,
                                      boolean inStock, boolean customizable, HepyProductSort sort, int page) {
        return new HepyProductQuery(text, category, null, sizes, min, max, inStock, customizable, sort, page, 12);
    }

    @Test
    void paginatesTheWholeCatalogFromTheDatabase() {
        HepyPage<HepyProductSummary> first = f.catalog.hepySearch(HepyProductQuery.hepyAll());
        assertThat(first.hepyTotal()).as("68 - SF-BUZ-001 (V8)").isEqualTo(67);
        assertThat(first.hepyTotalPages()).isEqualTo(6);
        assertThat(first.hepyItems()).hasSize(12);

        HepyPage<HepyProductSummary> last = f.catalog.hepySearch(HepyProductQuery.hepyAll().hepyWithPage(6));
        assertThat(last.hepyItems()).hasSize(7);
        assertThat(last.hepyHasNext()).isFalse();
    }

    @Test
    void searchIgnoresAccentsAndCase() {
        HepyPage<HepyProductSummary> plain = f.catalog.hepySearch(query("futbol", null, Set.of(), null, null, false, false, HepyProductSort.NAME, 1));
        HepyPage<HepyProductSummary> accented = f.catalog.hepySearch(query("FÚTBOL", null, Set.of(), null, null, false, false, HepyProductSort.NAME, 1));
        assertThat(plain.hepyItems()).isNotEmpty();
        assertThat(accented.hepyItems()).extracting(HepyProductSummary::hepySku).containsExactlyElementsOf(
                plain.hepyItems().stream().map(HepyProductSummary::hepySku).toList());
        assertThat(plain.hepyItems()).extracting(HepyProductSummary::hepyName).contains("Uniforme Fútbol Pro");
    }

    @Test
    void filtersByCategorySizePriceStockAndCustomizable() {
        assertThat(f.catalog.hepySearch(query(null, 2L, Set.of(), null, null, false, false, HepyProductSort.NAME, 1)).hepyTotal())
                .as("uniformes de fútbol: 3 de V2 + 3 de catalogo.json").isEqualTo(6);

        HepyPage<HepyProductSummary> size40 = f.catalog.hepySearch(query(null, null, Set.of("40"), null, null, false, false, HepyProductSort.NAME, 1));
        assertThat(size40.hepyItems()).allMatch(p -> p.hepyGarment() == com.sportforce3d.model.HepyGarment.SHOES).hasSize(3);
        HepyPage<HepyProductSummary> size40InStock = f.catalog.hepySearch(query(null, null, Set.of("40"), null, null, true, false, HepyProductSort.NAME, 1));
        assertThat(size40InStock.hepyItems()).extracting(HepyProductSummary::hepySku).doesNotContain("SF-ZAP-003");

        HepyPage<HepyProductSummary> range = f.catalog.hepySearch(query(null, null, Set.of(), 5000L, 9000L, false, false, HepyProductSort.PRICE_ASC, 1));
        assertThat(range.hepyItems()).isNotEmpty()
                .allMatch(p -> p.hepyPriceCents() >= 5000 && p.hepyPriceCents() <= 9000);
        assertThat(range.hepyItems()).extracting(HepyProductSummary::hepyPriceCents).isSorted();

        assertThat(f.catalog.hepySearch(query(null, null, Set.of(), null, null, false, true, HepyProductSort.NAME, 1)).hepyItems())
                .allMatch(HepyProductSummary::hepyCustomizable);
    }

    @Test
    void rejectsInvertedPriceRange() {
        assertThatThrownBy(() -> f.catalog.hepySearch(query(null, null, Set.of(), 9000L, 5000L, false, false, HepyProductSort.NAME, 1)))
                .isInstanceOf(PpqhValidationException.class);
    }

    @Test
    void relevancePutsOutOfStockLast() {
        HepyPage<HepyProductSummary> all = f.catalog.hepySearch(new HepyProductQuery(null, null, null, Set.of(), null, null, false, false,
                HepyProductSort.RELEVANCE, 2, 60));
        assertThat(all.hepyHasNext()).isFalse();
        assertThat(all.hepyItems().get(all.hepyItems().size() - 1).hepySku()).isEqualTo("SF-ZAP-003");
    }

    @Test
    void singleVariantProductsCanBeAddedWithoutChoosingSize() {
        HepyProductSummary canilleras = f.catalog.hepySearch(query("canilleras", null, Set.of(), null, null, false, false, HepyProductSort.NAME, 1))
                .hepyItems().get(0);
        assertThat(canilleras.hepyVariantCount()).isEqualTo(1);
        assertThat(canilleras.hepySingleVariantId()).isEqualTo(f.variantId("SF-ACC-001-UNICA"));

        HepyProductSummary jersey = f.catalog.hepySearch(query("SF-CAM-001", null, Set.of(), null, null, false, false, HepyProductSort.NAME, 1))
                .hepyItems().get(0);
        assertThat(jersey.hepySingleVariantId()).isNull();
        assertThat(jersey.hepyColorHex()).isEqualTo("#1F5FFF");
    }

    @Test
    void deletedOrInactiveProductsAreHiddenFromTheStore() {
        long id = f.productId("SF-CAM-002");
        f.products.hepySoftDelete(id, Instant.now());

        assertThat(f.catalog.hepySearch(query("training malla", null, Set.of(), null, null, false, false, HepyProductSort.NAME, 1)).hepyTotal()).isZero();
        assertThatThrownBy(() -> f.catalog.hepyDetail(id)).hasMessageContaining("ya no está disponible");
    }

    @Test
    void detailShowsOnlyActiveVariantsAndMaterials() {
        long id = f.productId("SF-UFU-001");
        f.exec("UPDATE product_variants SET active = 0 WHERE sku = 'SF-UFU-001-XS'");

        HepyProductDetail detail = f.catalog.hepyDetail(id);
        assertThat(detail.hepyVariants()).extracting(v -> v.hepySku()).doesNotContain("SF-UFU-001-XS").hasSize(5);
        assertThat(detail.hepyMaterials()).extracting(m -> m.hepyCode()).contains("DRY_FIT", "JACQUARD").doesNotContain("KNIT");
        assertThat(detail.hepyMaterials().get(0).hepyCode()).as("la tela predeterminada va primero").isEqualTo("DRY_FIT");
    }

    @Test
    void shippingIsFreeFromTheConfiguredThreshold() {
        assertThat(f.catalog.hepyShippingFor(0)).isZero();
        assertThat(f.catalog.hepyShippingFor(29_899)).isEqualTo(1500);
        assertThat(f.catalog.hepyShippingFor(29_900)).isZero();
    }
}
