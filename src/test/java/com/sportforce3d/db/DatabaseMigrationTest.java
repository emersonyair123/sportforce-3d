package com.sportforce3d.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void createsFileAndAppliesSchemaAndSeed() {
        HepyDatabase db = new HepyDatabase(tempDir.resolve("nueva").resolve("sportforce3d.db"));

        assertThat(db.hepyMigrate()).isEqualTo(10);
        assertThat(Files.exists(db.hepyFile())).isTrue();
        assertThat(db.hepyMigrate()).isZero();
    }

    @Test
    void seedHasCatalogUsersAndStock() throws SQLException {
        HepyDatabase db = TestDatabases.migrated(tempDir);
        try (Connection c = db.hepyGetConnection(); Statement st = c.createStatement()) {
            assertThat(count(st, "SELECT count(*) FROM products")).isGreaterThanOrEqualTo(20);
            assertThat(count(st, "SELECT count(*) FROM categories")).as("10 de V2 + Niños de V6").isEqualTo(11);
            assertThat(count(st, "SELECT count(*) FROM materials")).as("12 de V2 + Poliéster liso de V7").isEqualTo(13);
            assertThat(count(st, "SELECT count(*) FROM users")).isEqualTo(2);
            assertThat(count(st, "SELECT count(*) FROM products p WHERE NOT EXISTS "
                    + "(SELECT 1 FROM product_variants v WHERE v.product_id = p.id)")).isZero();
            assertThat(count(st, "SELECT count(*) FROM product_variants v LEFT JOIN inventory i ON i.variant_id = v.id "
                    + "WHERE i.variant_id IS NULL")).isZero();
            assertThat(count(st, "SELECT sum(i.quantity) FROM inventory i JOIN product_variants v ON v.id = i.variant_id "
                    + "WHERE v.product_id = 18")).isZero();
            assertThat(count(st, "SELECT surcharge_cents FROM materials WHERE code = 'JACQUARD'")).isEqualTo(1500);
            assertThat(count(st, "SELECT surcharge_cents FROM materials WHERE code = 'CUERO_SINTETICO'")).isEqualTo(2500);
            assertThat(count(st, "SELECT surcharge_cents FROM materials WHERE code = 'MALLA'")).isEqualTo(500);
            assertThat(count(st, "SELECT surcharge_cents FROM materials WHERE code = 'POLIESTER_LISO'")).isZero();
            assertThat(count(st, "SELECT count(*) FROM products p WHERE p.customizable = 1 AND p.garment_type IN "
                    + "('JERSEY', 'SHORTS', 'KIT') AND (SELECT count(*) FROM product_materials pm JOIN materials m "
                    + "ON m.id = pm.material_id WHERE pm.product_id = p.id AND m.code IN "
                    + "('DRY_FIT', 'POLIESTER_LISO', 'ALGODON', 'MALLA')) < 4")).as("las 4 telas en cada uniforme").isZero();
        }
    }

    @Test
    void foreignKeysAreEnforced() throws SQLException {
        HepyDatabase db = TestDatabases.migrated(tempDir);
        try (Connection c = db.hepyGetConnection(); Statement st = c.createStatement()) {
            assertThat(count(st, "PRAGMA foreign_keys")).isEqualTo(1);
            assertThatThrownBy(() -> st.executeUpdate("INSERT INTO user_roles (user_id, role_id) VALUES (999, 1)"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("FOREIGN KEY");
        }
    }

    @Test
    void checkConstraintsProtectMoneyAndDates() throws SQLException {
        HepyDatabase db = TestDatabases.migrated(tempDir);
        try (Connection c = db.hepyGetConnection(); Statement st = c.createStatement()) {
            assertThatThrownBy(() -> st.executeUpdate(
                    "UPDATE products SET base_price_cents = -1 WHERE id = 1"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> st.executeUpdate("""
                    INSERT INTO orders (order_number, user_id, payment_method, subtotal_cents, shipping_cents, total_cents,
                                        ship_recipient, ship_phone, ship_department, ship_province, ship_district, ship_line1)
                    VALUES ('PED-2026-000001', 2, 'YAPE', 10000, 1000, 99999,
                            'Lucía', '987654321', 'Puno', 'Juliaca', 'Juliaca', 'Jr. Mariano Núñez 245')
                    """))
                    .as("el total debe ser subtotal + recargos + envío")
                    .isInstanceOf(SQLException.class);

            try (ResultSet rs = st.executeQuery("SELECT created_at FROM users WHERE id = 1")) {
                rs.next();
                assertThat(rs.getString(1)).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
            }
        }
    }

    @Test
    void updatedAtTriggerTouchesRow() throws Exception {
        HepyDatabase db = TestDatabases.migrated(tempDir);
        try (Connection c = db.hepyGetConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("UPDATE users SET updated_at = '2000-01-01T00:00:00.000Z' WHERE id = 1");
            st.executeUpdate("UPDATE users SET full_name = 'Admin Renombrado' WHERE id = 1");
            try (ResultSet rs = st.executeQuery("SELECT updated_at FROM users WHERE id = 1")) {
                rs.next();
                assertThat(rs.getString(1)).isGreaterThan("2000-01-01T00:00:00.000Z");
            }
        }
    }

    @Test
    void deleteFilesAllowsRecreatingFromScratch() {
        HepyDatabase db = TestDatabases.migrated(tempDir);
        db.hepyWithConnection(c -> c.createStatement().executeUpdate("DELETE FROM addresses"));

        HepyDatabase.hepyDeleteFiles(db.hepyFile());
        assertThat(Files.exists(db.hepyFile())).isFalse();

        HepyDatabase fresh = new HepyDatabase(db.hepyFile());
        assertThat(fresh.hepyMigrate()).isEqualTo(10);
        int addresses = fresh.hepyWithConnection(c -> {
            try (ResultSet rs = c.createStatement().executeQuery("SELECT count(*) FROM addresses")) {
                rs.next();
                return rs.getInt(1);
            }
        });
        assertThat(addresses).isEqualTo(1);
    }

    @Test
    void repairsChecksumsOfDatabasesCreatedWithOlderScripts() throws SQLException {
        HepyDatabase db = TestDatabases.migrated(tempDir);
        long original;
        try (Connection c = db.hepyGetConnection(); Statement st = c.createStatement()) {
            original = count(st, "SELECT checksum FROM flyway_schema_history WHERE version = '1'");
            st.executeUpdate("UPDATE flyway_schema_history SET checksum = checksum + 1 WHERE version IN ('1', '6')");
            st.executeUpdate("UPDATE users SET full_name = 'Nombre Conservado' WHERE id = 2");
        }

        assertThat(new HepyDatabase(db.hepyFile()).hepyMigrate()).isZero();

        try (Connection c = db.hepyGetConnection(); Statement st = c.createStatement()) {
            assertThat(count(st, "SELECT checksum FROM flyway_schema_history WHERE version = '1'")).isEqualTo(original);
            assertThat(count(st, "SELECT count(*) FROM users WHERE full_name = 'Nombre Conservado'")).isEqualTo(1);
        }
    }

    private static long count(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
