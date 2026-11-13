package com.sportforce3d.db;

import com.sportforce3d.model.HepyProductQuery;
import com.sportforce3d.model.HepyProductSort;
import com.sportforce3d.model.HepyProductSummary;
import com.sportforce3d.render.PgeyRealisticModel;
import com.sportforce3d.render.PgeyRealisticModelRepository;
import com.sportforce3d.render.PgeyRecolorRequest;
import com.sportforce3d.repository.HepyProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RealisticCatalogMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void oldProductsGetARealisticPhotoAndTheDuplicateIsHidden() throws SQLException {
        HepyDatabase db = TestDatabases.migrated(tempDir);
        try (Connection c = db.hepyGetConnection(); Statement st = c.createStatement()) {
            assertThat(text(st, "SELECT model_key FROM products WHERE sku = 'SF-UFU-002'")).isEqualTo("futbol_hombre_frente");
            assertThat(text(st, "SELECT pattern || ' ' || pattern_color FROM products WHERE sku = 'SF-UFU-002'"))
                    .isEqualTo("rayas #f2f5f8");
            assertThat(text(st, "SELECT deleted_at IS NOT NULL FROM products WHERE sku = 'SF-BUZ-001'")).isEqualTo("1");
            assertThat(text(st, "SELECT deleted_at IS NULL FROM products WHERE sku = 'SF-037'")).isEqualTo("1");
        }
    }

    @Test
    void newModelsReplaceTheDrawingsAndTheProvisionalPhotos() throws SQLException {
        HepyDatabase db = TestDatabases.migrated(tempDir);
        try (Connection c = db.hepyGetConnection(); Statement st = c.createStatement()) {
            assertThat(count(st, "SELECT count(*) FROM products WHERE deleted_at IS NULL AND model_key IS NULL"))
                    .as("ninguno con dibujo").isZero();
            for (String[] row : new String[][]{
                    {"SF-UFU-003", "arquero_hombre_frente", "{\"camiseta\":\"#ff7a00\",\"short\":\"#0b0e13\",\"medias\":\"#ff7a00\"}"},
                    {"SF-ENT-002", "compresion_hombre_frente", "{\"camiseta\":\"#0b0e13\"}"},
                    {"SF-BUZ-002", "casaca_hombre_frente", "{\"principal\":\"#0e7c3a\"}"},
                    {"SF-MED-002", "producto_media_cana", "{\"principal\":\"#0b0e13\"}"},
                    {"SF-ZAP-002", "producto_zapatilla_futsal", "{\"principal\":\"#0b0e13\"}"},
                    {"SF-ZAP-003", "producto_zapatilla_running", "{\"principal\":\"#1f5fff\",\"paneles\":\"#f2f5f8\"}"},
                    {"SF-ACC-002", "producto_brazalete", "{\"principal\":\"#ffd400\"}"},
                    {"SF-ACC-003", "producto_guantes", "{\"principal\":\"#39ff88\"}"},
                    {"SF-ACC-004", "producto_mochila", "{\"principal\":\"#0b0e13\"}"}}) {
                assertThat(text(st, "SELECT model_key || ' ' || zone_colors || ' ' || pattern FROM products WHERE sku = '"
                        + row[0] + "'")).as(row[0]).isEqualTo(row[1] + " " + row[2] + " liso");
            }
            assertThat(text(st, "SELECT model_key FROM products WHERE sku = 'SF-CAM-003'")).isEqualTo("redondo_hombre_frente");
            assertThat(text(st, "SELECT model_key FROM products WHERE sku = 'SF-SHO-003'")).isEqualTo("futbol_hombre_frente");
        }
    }

    @Test
    void loadsTheWholeCatalogWithValidModelsAndStock() throws SQLException {
        HepyDatabase db = TestDatabases.migrated(tempDir);
        try (Connection c = db.hepyGetConnection(); Statement st = c.createStatement()) {
            assertThat(count(st, "SELECT count(*) FROM products WHERE model_key IS NOT NULL")).as("44 de V6 + 20 de V8 + 4 de V9").isEqualTo(68);
            assertThat(count(st, "SELECT count(*) FROM products WHERE sku GLOB 'SF-0[0-9][0-9]'")).as("insertados").isEqualTo(42);
            for (String name : List.of("Uniformes de vóley", "Uniformes de básquet", "Shorts", "Niños")) {
                assertThat(count(st, "SELECT count(*) FROM categories WHERE name = '" + name + "'")).as(name).isEqualTo(1);
            }
            assertThat(count(st, "SELECT count(*) FROM products p JOIN categories c ON c.id = p.category_id "
                    + "WHERE c.name = 'Niños'")).isEqualTo(7);
            assertThat(count(st, "SELECT count(*) FROM products WHERE model_key IS NOT NULL "
                    + "AND (pattern = 'liso') <> (pattern_color IS NULL)")).isZero();
            assertThat(count(st, "SELECT count(*) FROM products WHERE pattern <> 'liso'")).as("8 de V6 + SF-UFU-002").isEqualTo(9);
            assertThat(count(st, "SELECT count(*) FROM products p WHERE p.model_key IS NOT NULL AND p.sku <> 'SF-ZAP-003' "
                    + "AND NOT EXISTS "
                    + "(SELECT 1 FROM product_variants v JOIN inventory i ON i.variant_id = v.id "
                    + " WHERE v.product_id = p.id AND i.quantity > 0)")).isZero();
            assertThat(count(st, "SELECT count(*) FROM product_variants v JOIN products p ON p.id = v.product_id "
                    + "WHERE p.sku = 'SF-003'")).isEqualTo(4);
            assertThat(text(st, "SELECT model_key FROM products WHERE sku = 'SF-ZAP-001'")).isEqualTo("producto_zapatilla_fg");
            assertThat(text(st, "SELECT DISTINCT color_hex FROM product_variants v JOIN products p ON p.id = v.product_id "
                    + "WHERE p.sku = 'SF-ZAP-001'")).isEqualTo("#1F5FFF");
            assertThat(text(st, "SELECT model_key FROM products WHERE sku = 'SF-ACC-001'")).isEqualTo("producto_canilleras");
        }

        PgeyRealisticModelRepository models = new PgeyRealisticModelRepository();
        HepyProductRepository products = new HepyProductRepository(db);
        List<HepyProductSummary> all = new ArrayList<>();
        for (int page = 1; page <= 2; page++) {
            all.addAll(products.hepySearch(new HepyProductQuery(null, null, null, Set.of(), null, null, false, false,
                    HepyProductSort.NAME, page, 60)).hepyItems());
        }
        assertThat(all).as("68 - SF-BUZ-001 (V8)").hasSize(67);
        List<HepyProductSummary> realistic = all.stream().filter(p -> p.hepyLook() != null).toList();
        assertThat(realistic).as("todos: 44 de V6 + 20 de V8 + 4 de V9 − SF-BUZ-001 oculto").hasSize(67);
        for (HepyProductSummary p : realistic) {
            PgeyRecolorRequest req = PgeyRecolorRequest.pgeyOf(p.hepyLook());
            PgeyRealisticModel model = models.pgeyRequire(req.pgeyModelKey());
            assertThat(model.pgeyZones()).as(p.hepyName()).containsAnyElementsOf(req.pgeyZoneColors().keySet());
        }
    }

    private static long count(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String text(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
