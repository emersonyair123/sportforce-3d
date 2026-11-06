package com.sportforce3d.db;

import com.sportforce3d.model.ProductQuery;
import com.sportforce3d.model.ProductSort;
import com.sportforce3d.model.ProductSummary;
import com.sportforce3d.render.RealisticModel;
import com.sportforce3d.render.RealisticModelRepository;
import com.sportforce3d.render.RecolorRequest;
import com.sportforce3d.repository.ProductRepository;
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

/** V6: los 44 productos de catalogo.json con modelo, colores por zona y diseño. */
class RealisticCatalogMigrationTest {

    @TempDir
    Path tempDir;

    /** V8 (Etapa 12A): los productos de V2 con dibujo pasan a foto realista; SF-BUZ-001 (duplicado) se oculta. */
    @Test
    void oldProductsGetARealisticPhotoAndTheDuplicateIsHidden() throws SQLException {
        Database db = TestDatabases.migrated(tempDir);
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            assertThat(text(st, "SELECT model_key FROM products WHERE sku = 'SF-UFU-002'")).isEqualTo("futbol_hombre_frente");
            assertThat(text(st, "SELECT pattern || ' ' || pattern_color FROM products WHERE sku = 'SF-UFU-002'"))
                    .isEqualTo("rayas #f2f5f8");
            // Duplicado: borrado lógico, la fila sigue (pedidos y carritos anteriores).
            assertThat(text(st, "SELECT deleted_at IS NOT NULL FROM products WHERE sku = 'SF-BUZ-001'")).isEqualTo("1");
            assertThat(text(st, "SELECT deleted_at IS NULL FROM products WHERE sku = 'SF-037'")).isEqualTo("1");
        }
    }

    /**
     * V9 (Etapa 12B): fotos propias para los provisionales y los que tenían dibujo, con el color de su variante.
     * Ningún producto visible queda con el dibujo antiguo.
     */
    @Test
    void newModelsReplaceTheDrawingsAndTheProvisionalPhotos() throws SQLException {
        Database db = TestDatabases.migrated(tempDir);
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
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
            // Siguen provisionales (ningún modelo nuevo es su prenda).
            assertThat(text(st, "SELECT model_key FROM products WHERE sku = 'SF-CAM-003'")).isEqualTo("redondo_hombre_frente");
            assertThat(text(st, "SELECT model_key FROM products WHERE sku = 'SF-SHO-003'")).isEqualTo("futbol_hombre_frente");
        }
    }

    @Test
    void loadsTheWholeCatalogWithValidModelsAndStock() throws SQLException {
        Database db = TestDatabases.migrated(tempDir);
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            assertThat(count(st, "SELECT count(*) FROM products WHERE model_key IS NOT NULL")).as("44 de V6 + 20 de V8 + 4 de V9").isEqualTo(68);
            assertThat(count(st, "SELECT count(*) FROM products WHERE sku GLOB 'SF-0[0-9][0-9]'")).as("insertados").isEqualTo(42);
            for (String name : List.of("Uniformes de vóley", "Uniformes de básquet", "Shorts", "Niños")) {
                assertThat(count(st, "SELECT count(*) FROM categories WHERE name = '" + name + "'")).as(name).isEqualTo(1);
            }
            assertThat(count(st, "SELECT count(*) FROM products p JOIN categories c ON c.id = p.category_id "
                    + "WHERE c.name = 'Niños'")).isEqualTo(7);
            // Diseño y color van juntos.
            assertThat(count(st, "SELECT count(*) FROM products WHERE model_key IS NOT NULL "
                    + "AND (pattern = 'liso') <> (pattern_color IS NULL)")).isZero();
            assertThat(count(st, "SELECT count(*) FROM products WHERE pattern <> 'liso'")).as("8 de V6 + SF-UFU-002").isEqualTo(9);
            // Cada producto tiene variantes con stock de ejemplo (salvo las Zapatillas Running Pulse, agotadas a
            // propósito desde V2 para probar ese estado).
            assertThat(count(st, "SELECT count(*) FROM products p WHERE p.model_key IS NOT NULL AND p.sku <> 'SF-ZAP-003' "
                    + "AND NOT EXISTS "
                    + "(SELECT 1 FROM product_variants v JOIN inventory i ON i.variant_id = v.id "
                    + " WHERE v.product_id = p.id AND i.quantity > 0)")).isZero();
            // Niños y niñas: tallas XS a L.
            assertThat(count(st, "SELECT count(*) FROM product_variants v JOIN products p ON p.id = v.product_id "
                    + "WHERE p.sku = 'SF-003'")).isEqualTo(4);
            // Coincidencia por nombre: se actualiza y conserva su SKU y variantes.
            assertThat(text(st, "SELECT model_key FROM products WHERE sku = 'SF-ZAP-001'")).isEqualTo("producto_zapatilla_fg");
            assertThat(text(st, "SELECT DISTINCT color_hex FROM product_variants v JOIN products p ON p.id = v.product_id "
                    + "WHERE p.sku = 'SF-ZAP-001'")).isEqualTo("#1F5FFF");
            assertThat(text(st, "SELECT model_key FROM products WHERE sku = 'SF-ACC-001'")).isEqualTo("producto_canilleras");
        }

        // Cada vista guardada se puede recolorear: el modelo existe y los colores valen. catalogo.json trae a veces
        // una zona que el modelo no tiene (paneles en vóley mujer); el recoloreado la ignora.
        RealisticModelRepository models = new RealisticModelRepository();
        ProductRepository products = new ProductRepository(db);
        List<ProductSummary> all = new ArrayList<>();
        for (int page = 1; page <= 2; page++) {
            all.addAll(products.search(new ProductQuery(null, null, null, Set.of(), null, null, false, false,
                    ProductSort.NAME, page, 60)).items());
        }
        assertThat(all).as("68 - SF-BUZ-001 (V8)").hasSize(67);
        List<ProductSummary> realistic = all.stream().filter(p -> p.look() != null).toList();
        assertThat(realistic).as("todos: 44 de V6 + 20 de V8 + 4 de V9 − SF-BUZ-001 oculto").hasSize(67);
        for (ProductSummary p : realistic) {
            RecolorRequest req = RecolorRequest.of(p.look());
            RealisticModel model = models.require(req.modelKey());
            assertThat(model.zones()).as(p.name()).containsAnyElementsOf(req.zoneColors().keySet());
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