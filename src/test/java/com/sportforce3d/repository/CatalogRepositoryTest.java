package com.sportforce3d.repository;

import com.sportforce3d.db.TestDatabases;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.ProductSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogRepositoryTest {

    @TempDir
    Path tempDir;

    private CatalogRepository catalog;

    @BeforeEach
    void setUp() {
        catalog = new CatalogRepository(TestDatabases.migrated(tempDir));
    }

    @Test
    void listsTenCategoriesInOrder() {
        assertThat(catalog.findActiveCategories())
                .hasSize(10)
                .first().extracting(c -> c.name()).isEqualTo("Camisetas deportivas");
    }

    @Test
    void featuredProductsCarryPricesInCentsAndStock() {
        List<ProductSummary> featured = catalog.findFeatured(20);

        assertThat(featured).isNotEmpty().allMatch(ProductSummary::inStock);
        ProductSummary franjas = featured.stream().filter(p -> p.sku().equals("SF-UFU-002")).findFirst().orElseThrow();
        assertThat(franjas.priceCents()).isEqualTo(12990);
        assertThat(franjas.compareAtPriceCents()).isEqualTo(15990);
        assertThat(franjas.discountPercent()).isEqualTo(19);
    }

    @Test
    void materialsAreSplitByScope() {
        assertThat(catalog.findMaterials(Material.Scope.TEXTIL)).extracting(Material::code)
                .containsExactly("DRY_FIT", "MALLA", "JACQUARD", "LICRA", "ALGODON", "MICROFIBRA");
        assertThat(catalog.findMaterials(Material.Scope.CALZADO)).hasSize(6)
                .noneMatch(m -> m.code().equals("LICRA"));
    }
}
