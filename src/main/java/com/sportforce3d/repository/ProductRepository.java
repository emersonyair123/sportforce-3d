package com.sportforce3d.repository;

import com.sportforce3d.db.Database;
import com.sportforce3d.model.AdminProductRow;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.InventoryRow;
import com.sportforce3d.model.LowStockVariant;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.Page;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.model.ProductForm;
import com.sportforce3d.model.ProductImage;
import com.sportforce3d.model.ProductQuery;
import com.sportforce3d.model.ProductSummary;
import com.sportforce3d.model.ProductVariant;
import com.sportforce3d.model.RealisticLook;
import com.sportforce3d.util.Texts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Productos, variantes, imágenes e inventario. Sin reglas de permisos (eso es de la capa service). */
public class ProductRepository {

    /** Stock disponible, número de variantes, variante única y color principal de cada producto. */
    private static final String STOCK_JOIN = """
            LEFT JOIN (
                SELECT v.product_id,
                       sum(max(COALESCE(i.quantity, 0) - COALESCE(i.reserved, 0), 0)) AS stock,
                       count(*) AS variant_count,
                       CASE WHEN count(*) = 1 THEN min(v.id) END AS single_variant_id
                FROM product_variants v
                LEFT JOIN inventory i ON i.variant_id = v.id
                WHERE v.active = 1
                GROUP BY v.product_id
            ) st ON st.product_id = p.id
            """;

    private static final String SUMMARY_COLUMNS = """
            SELECT p.id, p.sku, p.name, cat.name AS category_name, p.garment_type,
                   p.base_price_cents, p.compare_at_price_cents, p.is_new, p.customizable,
                   COALESCE(st.stock, 0) AS stock, COALESCE(st.variant_count, 0) AS variant_count,
                   st.single_variant_id,
                   (SELECT v2.color_hex FROM product_variants v2
                     WHERE v2.product_id = p.id AND v2.active = 1 ORDER BY v2.id LIMIT 1) AS color_hex,
                   (SELECT pi.path FROM product_images pi
                     WHERE pi.product_id = p.id ORDER BY pi.sort_order, pi.id LIMIT 1) AS image_path
            """;

    private static final String PUBLIC_FROM = """
            FROM products p
            JOIN categories cat ON cat.id = p.category_id
            """ + STOCK_JOIN + """
            WHERE p.active = 1 AND p.deleted_at IS NULL AND cat.active = 1
            """;

    private final Database db;

    public ProductRepository(Database db) {
        this.db = db;
    }

    // ------------------------------------------------------------------ Catálogo público

    public Page<ProductSummary> search(ProductQuery query) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (query.text() != null && !query.text().isBlank()) {
            for (String token : Texts.searchKey(query.text()).split("\\s+")) {
                if (!token.isEmpty()) {
                    where.append(" AND p.search_key LIKE ? ESCAPE '\\'");
                    params.add("%" + token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
                }
            }
        }
        if (query.categoryId() != null) {
            where.append(" AND p.category_id = ?");
            params.add(query.categoryId());
        }
        if (query.sportId() != null) {
            where.append(" AND p.sport_id = ?");
            params.add(query.sportId());
        }
        if (!query.sizeCodes().isEmpty()) {
            where.append("""
                     AND EXISTS (SELECT 1 FROM product_variants sv
                                 JOIN sizes sz ON sz.id = sv.size_id
                                 LEFT JOIN inventory si ON si.variant_id = sv.id
                                 WHERE sv.product_id = p.id AND sv.active = 1 AND sz.code IN (%s)%s)
                    """.formatted(Jdbc.placeholders(query.sizeCodes().size()),
                    query.inStockOnly() ? " AND COALESCE(si.quantity, 0) - COALESCE(si.reserved, 0) > 0" : ""));
            params.addAll(query.sizeCodes().stream().sorted().toList());
        }
        if (query.minPriceCents() != null) {
            where.append(" AND p.base_price_cents >= ?");
            params.add(query.minPriceCents());
        }
        if (query.maxPriceCents() != null) {
            where.append(" AND p.base_price_cents <= ?");
            params.add(query.maxPriceCents());
        }
        if (query.inStockOnly()) {
            where.append(" AND COALESCE(st.stock, 0) > 0");
        }
        if (query.customizableOnly()) {
            where.append(" AND p.customizable = 1");
        }

        String orderBy = switch (query.sort()) {
            case RELEVANCE -> "(COALESCE(st.stock, 0) > 0) DESC, p.featured DESC, p.is_new DESC, p.id";
            case NEWEST -> "p.is_new DESC, p.created_at DESC, p.id DESC";
            case PRICE_ASC -> "p.base_price_cents, p.id";
            case PRICE_DESC -> "p.base_price_cents DESC, p.id";
            case NAME -> "p.name COLLATE NOCASE, p.id";
        };

        return db.withConnection(c -> {
            long total;
            try (PreparedStatement ps = c.prepareStatement("SELECT count(*) " + PUBLIC_FROM + where)) {
                Jdbc.bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            List<Object> pageParams = new ArrayList<>(params);
            pageParams.add(query.pageSize());
            pageParams.add(query.offset());
            List<ProductSummary> items = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    SUMMARY_COLUMNS + PUBLIC_FROM + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?")) {
                Jdbc.bind(ps, pageParams);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapSummary(rs));
                    }
                }
            }
            return new Page<>(items, query.page(), query.pageSize(), total);
        });
    }

    public List<ProductSummary> findFeatured(int limit) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    SUMMARY_COLUMNS + PUBLIC_FROM + " AND p.featured = 1 ORDER BY p.id LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<ProductSummary> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(mapSummary(rs));
                    }
                    return list;
                }
            }
        });
    }

    /** Ficha completa, incluidas variantes inactivas. La capa service decide qué mostrar. */
    /** Vista realista guardada del producto (modelo, colores por zona y diseño), si tiene. */
    public Optional<RealisticLook> findLook(long productId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT model_key, zone_colors, pattern, pattern_color FROM products WHERE id = ?")) {
                ps.setLong(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.ofNullable(look(rs)) : Optional.empty();
                }
            }
        });
    }

    public Optional<ProductDetail> findDetail(long productId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT p.*, cat.name AS category_name, sp.name AS sport_name
                    FROM products p
                    JOIN categories cat ON cat.id = p.category_id
                    LEFT JOIN sports sp ON sp.id = p.sport_id
                    WHERE p.id = ?
                    """)) {
                ps.setLong(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new ProductDetail(
                            rs.getLong("id"),
                            rs.getLong("category_id"),
                            rs.getString("category_name"),
                            Jdbc.getLongOrNull(rs, "sport_id"),
                            rs.getString("sport_name"),
                            rs.getString("sku"),
                            rs.getString("name"),
                            rs.getString("description"),
                            Garment.valueOf(rs.getString("garment_type")),
                            rs.getLong("base_price_cents"),
                            Jdbc.getLongOrNull(rs, "compare_at_price_cents"),
                            rs.getInt("customizable") == 1,
                            rs.getInt("is_new") == 1,
                            rs.getInt("featured") == 1,
                            rs.getInt("active") == 1,
                            rs.getString("deleted_at") != null,
                            variants(c, productId),
                            images(c, productId),
                            materials(c, productId)));
                }
            }
        });
    }

    private static List<ProductVariant> variants(Connection c, long productId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT v.id, v.size_id, s.code AS size_code, s.name AS size_name, v.color_name, v.color_hex, v.sku,
                       v.price_delta_cents, v.active,
                       COALESCE(i.quantity, 0) AS quantity, COALESCE(i.reserved, 0) AS reserved,
                       COALESCE(i.low_stock_threshold, 5) AS low_stock_threshold
                FROM product_variants v
                JOIN sizes s ON s.id = v.size_id
                LEFT JOIN inventory i ON i.variant_id = v.id
                WHERE v.product_id = ?
                ORDER BY v.color_name, s.sort_order, v.id
                """)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ProductVariant> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new ProductVariant(rs.getLong("id"), rs.getLong("size_id"), rs.getString("size_code"),
                            rs.getString("size_name"), rs.getString("color_name"), rs.getString("color_hex"),
                            rs.getString("sku"), rs.getLong("price_delta_cents"), rs.getInt("active") == 1,
                            rs.getInt("quantity"), rs.getInt("reserved"), rs.getInt("low_stock_threshold")));
                }
                return list;
            }
        }
    }

    private static List<ProductImage> images(Connection c, long productId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, path, alt_text, sort_order FROM product_images WHERE product_id = ? ORDER BY sort_order, id")) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ProductImage> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new ProductImage(rs.getLong("id"), rs.getString("path"), rs.getString("alt_text"),
                            rs.getInt("sort_order")));
                }
                return list;
            }
        }
    }

    private static List<Material> materials(Connection c, long productId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT m.* FROM product_materials pm JOIN materials m ON m.id = pm.material_id
                WHERE pm.product_id = ? AND m.active = 1
                ORDER BY pm.is_default DESC, m.sort_order
                """)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Material> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(CatalogRepository.mapMaterial(rs));
                }
                return list;
            }
        }
    }

    /** Lo que el carrito necesita saber de una variante para agregarla. */
    public record VariantInfo(long variantId, long productId, String productName, String sizeName,
                              long unitPriceCents, int available, boolean purchasable) {
    }

    public Optional<VariantInfo> findVariantInfo(long variantId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT v.id, p.id AS product_id, p.name, s.name AS size_name,
                           p.base_price_cents + v.price_delta_cents AS unit_price,
                           max(COALESCE(i.quantity, 0) - COALESCE(i.reserved, 0), 0) AS available,
                           (p.active = 1 AND p.deleted_at IS NULL AND v.active = 1) AS purchasable
                    FROM product_variants v
                    JOIN products p ON p.id = v.product_id
                    JOIN sizes s ON s.id = v.size_id
                    LEFT JOIN inventory i ON i.variant_id = v.id
                    WHERE v.id = ?
                    """)) {
                ps.setLong(1, variantId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new VariantInfo(rs.getLong(1), rs.getLong("product_id"), rs.getString("name"),
                            rs.getString("size_name"), rs.getLong("unit_price"), rs.getInt("available"),
                            rs.getInt("purchasable") == 1));
                }
            }
        });
    }

    // ------------------------------------------------------------------ Administración

    public List<AdminProductRow> findForAdmin(String text, boolean includeDeleted) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.sku, p.name, cat.name AS category_name, p.garment_type, p.base_price_cents,
                       p.active, p.deleted_at,
                       COALESCE(st.stock, 0) AS stock, COALESCE(st.variant_count, 0) AS variant_count,
                       (SELECT v2.color_hex FROM product_variants v2
                         WHERE v2.product_id = p.id ORDER BY v2.active DESC, v2.id LIMIT 1) AS color_hex,
                       (SELECT pi.path FROM product_images pi
                         WHERE pi.product_id = p.id ORDER BY pi.sort_order, pi.id LIMIT 1) AS image_path
                FROM products p
                JOIN categories cat ON cat.id = p.category_id
                """ + STOCK_JOIN + " WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (!includeDeleted) {
            sql.append(" AND p.deleted_at IS NULL");
        }
        if (text != null && !text.isBlank()) {
            sql.append(" AND p.search_key LIKE ?");
            params.add("%" + Texts.searchKey(text) + "%");
        }
        sql.append(" ORDER BY p.deleted_at IS NOT NULL, p.name COLLATE NOCASE");
        return db.withConnection(c -> {
            Map<Long, List<LowStockVariant>> low = lowStockVariants(c);
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                Jdbc.bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<AdminProductRow> list = new ArrayList<>();
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        list.add(new AdminProductRow(id, rs.getString("sku"), rs.getString("name"),
                                rs.getString("category_name"), Garment.valueOf(rs.getString("garment_type")),
                                rs.getLong("base_price_cents"), rs.getInt("stock"), rs.getInt("variant_count"),
                                rs.getInt("active") == 1, rs.getString("deleted_at") != null,
                                rs.getString("color_hex"), rs.getString("image_path"),
                                low.getOrDefault(id, List.of())));
                    }
                    return list;
                }
            }
        });
    }

    /** Variantes activas con disponible ≤ umbral, agrupadas por producto y ordenadas por talla. */
    private static Map<Long, List<LowStockVariant>> lowStockVariants(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT v.product_id, s.name AS size_name, v.color_name,
                       max(COALESCE(i.quantity, 0) - COALESCE(i.reserved, 0), 0) AS available,
                       COALESCE(i.low_stock_threshold, 5) AS threshold,
                       (SELECT count(DISTINCT v2.color_hex) FROM product_variants v2
                         WHERE v2.product_id = v.product_id AND v2.active = 1) > 1 AS multi_color
                FROM product_variants v
                JOIN sizes s ON s.id = v.size_id
                LEFT JOIN inventory i ON i.variant_id = v.id
                WHERE v.active = 1
                  AND COALESCE(i.quantity, 0) - COALESCE(i.reserved, 0) <= COALESCE(i.low_stock_threshold, 5)
                ORDER BY v.product_id, available, s.sort_order
                """);
             ResultSet rs = ps.executeQuery()) {
            Map<Long, List<LowStockVariant>> map = new HashMap<>();
            while (rs.next()) {
                map.computeIfAbsent(rs.getLong("product_id"), k -> new ArrayList<>())
                        .add(new LowStockVariant(rs.getString("size_name"), rs.getString("color_name"),
                                rs.getInt("available"), rs.getInt("threshold"), rs.getInt("multi_color") == 1));
            }
            return map;
        }
    }

    public boolean skuExists(String sku, Long exceptProductId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM products WHERE sku = ? AND id <> COALESCE(?, -1)")) {
                ps.setString(1, sku);
                if (exceptProductId == null) {
                    ps.setNull(2, java.sql.Types.INTEGER);
                } else {
                    ps.setLong(2, exceptProductId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** Crea o actualiza el producto con sus materiales y variantes en una sola transacción. Devuelve el id. */
    public long save(ProductForm form) {
        return db.inTransaction(c -> {
            long id;
            String slug = Texts.searchKey(form.name()).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "")
                    + "-" + Texts.skuPart(form.sku()).toLowerCase();
            String searchKey = Texts.searchKey(form.name() + " " + (form.description() == null ? "" : form.description())
                    + " " + form.sku());
            if (form.id() == null) {
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO products (category_id, sport_id, sku, slug, name, description, garment_type,
                                              base_price_cents, compare_at_price_cents, customizable, is_new, featured,
                                              active, search_key)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    Jdbc.bind(ps, productParams(form, slug, searchKey));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getLong(1);
                    }
                }
            } else {
                id = form.id();
                List<Object> params = productParams(form, slug, searchKey);
                params.add(id);
                try (PreparedStatement ps = c.prepareStatement("""
                        UPDATE products SET category_id = ?, sport_id = ?, sku = ?, slug = ?, name = ?, description = ?,
                               garment_type = ?, base_price_cents = ?, compare_at_price_cents = ?, customizable = ?,
                               is_new = ?, featured = ?, active = ?, search_key = ?
                        WHERE id = ?
                        """)) {
                    Jdbc.bind(ps, params);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = c.prepareStatement("DELETE FROM product_materials WHERE product_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO product_materials (product_id, material_id, is_default) VALUES (?, ?, 0)")) {
                for (Long materialId : form.materialIds()) {
                    ps.setLong(1, id);
                    ps.setLong(2, materialId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            for (ProductForm.VariantForm v : form.variants()) {
                saveVariant(c, id, v);
            }
            return id;
        });
    }

    private static List<Object> productParams(ProductForm f, String slug, String searchKey) {
        List<Object> params = new ArrayList<>();
        params.add(f.categoryId());
        params.add(f.sportId());
        params.add(f.sku());
        params.add(slug);
        params.add(f.name());
        params.add(f.description());
        params.add(f.garment().name());
        params.add(f.basePriceCents());
        params.add(f.compareAtPriceCents());
        params.add(f.customizable());
        params.add(f.isNew());
        params.add(f.featured());
        params.add(f.active());
        params.add(searchKey);
        return params;
    }

    private static void saveVariant(Connection c, long productId, ProductForm.VariantForm v) throws SQLException {
        long variantId;
        if (v.id() == null) {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO product_variants (product_id, size_id, color_name, color_hex, sku, price_delta_cents, active)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                Jdbc.bind(ps, List.of(productId, v.sizeId(), v.colorName(), v.colorHex(), v.sku(),
                        v.priceDeltaCents(), v.active()));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    variantId = keys.getLong(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO inventory (variant_id, quantity, low_stock_threshold) VALUES (?, ?, ?)")) {
                ps.setLong(1, variantId);
                ps.setInt(2, v.stock());
                ps.setInt(3, v.lowStockThreshold());
                ps.executeUpdate();
            }
        } else {
            variantId = v.id();
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE product_variants SET size_id = ?, color_name = ?, color_hex = ?, sku = ?,
                           price_delta_cents = ?, active = ?
                    WHERE id = ? AND product_id = ?
                    """)) {
                Jdbc.bind(ps, List.of(v.sizeId(), v.colorName(), v.colorHex(), v.sku(), v.priceDeltaCents(),
                        v.active(), variantId, productId));
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("La variante " + variantId + " no pertenece al producto " + productId);
                }
            }
            upsertInventory(c, variantId, v.stock(), v.lowStockThreshold());
        }
    }

    private static void upsertInventory(Connection c, long variantId, int quantity, int threshold) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO inventory (variant_id, quantity, low_stock_threshold) VALUES (?, ?, ?)
                ON CONFLICT (variant_id) DO UPDATE SET quantity = excluded.quantity,
                                                     low_stock_threshold = excluded.low_stock_threshold
                """)) {
            ps.setLong(1, variantId);
            ps.setInt(2, quantity);
            ps.setInt(3, threshold);
            ps.executeUpdate();
        }
    }

    /** Borrado lógico: el producto deja de verse en la tienda pero los pedidos antiguos lo conservan. */
    public void softDelete(long productId, Instant when) {
        db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE products SET deleted_at = ?, active = 0 WHERE id = ? AND deleted_at IS NULL")) {
                ps.setString(1, when.toString());
                ps.setLong(2, productId);
                return ps.executeUpdate();
            }
        });
    }

    public void restore(long productId) {
        db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE products SET deleted_at = NULL WHERE id = ?")) {
                ps.setLong(1, productId);
                return ps.executeUpdate();
            }
        });
    }

    /** Reemplaza los colores por zona de la foto realista (JSON zona → #rrggbb). */
    public void updateZoneColors(long productId, Map<String, String> zoneColors) {
        String json;
        try {
            json = JSON.writeValueAsString(new LinkedHashMap<>(zoneColors));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Colores por zona inválidos", e);
        }
        db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE products SET zone_colors = ? WHERE id = ?")) {
                ps.setString(1, json);
                ps.setLong(2, productId);
                return ps.executeUpdate();
            }
        });
    }

    public ProductImage addImage(long productId, String path, String altText) {
        return db.inTransaction(c -> {
            int order;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(max(sort_order) + 1, 0) FROM product_images WHERE product_id = ?")) {
                ps.setLong(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    order = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO product_images (product_id, path, alt_text, sort_order) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, productId);
                ps.setString(2, path);
                ps.setString(3, altText);
                ps.setInt(4, order);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return new ProductImage(keys.getLong(1), path, altText, order);
                }
            }
        });
    }

    /** Borra el registro y devuelve la ruta del archivo para que el service lo elimine del disco. */
    public Optional<String> removeImage(long productId, long imageId) {
        return db.inTransaction(c -> {
            String path = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT path FROM product_images WHERE id = ? AND product_id = ?")) {
                ps.setLong(1, imageId);
                ps.setLong(2, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        path = rs.getString(1);
                    }
                }
            }
            if (path != null) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM product_images WHERE id = ?")) {
                    ps.setLong(1, imageId);
                    ps.executeUpdate();
                }
            }
            return Optional.ofNullable(path);
        });
    }

    // ------------------------------------------------------------------ Inventario

    public List<InventoryRow> findInventory(String text, boolean lowStockOnly) {
        StringBuilder sql = new StringBuilder("""
                SELECT v.id AS variant_id, p.id AS product_id, p.name AS product_name, v.sku, s.name AS size_name,
                       v.color_name, v.color_hex, COALESCE(i.quantity, 0) AS quantity, COALESCE(i.reserved, 0) AS reserved,
                       COALESCE(i.low_stock_threshold, 5) AS low_stock_threshold, p.active AS product_active
                FROM product_variants v
                JOIN products p ON p.id = v.product_id
                JOIN sizes s ON s.id = v.size_id
                LEFT JOIN inventory i ON i.variant_id = v.id
                WHERE p.deleted_at IS NULL AND v.active = 1
                """);
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            sql.append(" AND (p.search_key LIKE ? OR lower(v.sku) LIKE ?)");
            String like = "%" + Texts.searchKey(text) + "%";
            params.add(like);
            params.add(like);
        }
        if (lowStockOnly) {
            sql.append(" AND COALESCE(i.quantity, 0) - COALESCE(i.reserved, 0) <= COALESCE(i.low_stock_threshold, 5)");
        }
        sql.append(" ORDER BY (COALESCE(i.quantity, 0) - COALESCE(i.reserved, 0)) <= COALESCE(i.low_stock_threshold, 5) DESC,")
                .append(" p.name COLLATE NOCASE, s.sort_order");
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                Jdbc.bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<InventoryRow> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(new InventoryRow(rs.getLong("variant_id"), rs.getLong("product_id"),
                                rs.getString("product_name"), rs.getString("sku"), rs.getString("size_name"),
                                rs.getString("color_name"), rs.getString("color_hex"), rs.getInt("quantity"),
                                rs.getInt("reserved"), rs.getInt("low_stock_threshold"),
                                rs.getInt("product_active") == 1));
                    }
                    return list;
                }
            }
        });
    }

    public Optional<InventoryRow> findInventoryRow(long variantId) {
        return findInventory(null, false).stream().filter(r -> r.variantId() == variantId).findFirst();
    }

    public void updateStock(long variantId, int quantity, int threshold) {
        db.inTransaction(c -> {
            upsertInventory(c, variantId, quantity, threshold);
            return null;
        });
    }

    // ------------------------------------------------------------------ Mapeo

    private static ProductSummary mapSummary(ResultSet rs) throws SQLException {
        return new ProductSummary(
                rs.getLong("id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("category_name"),
                Garment.valueOf(rs.getString("garment_type")),
                rs.getLong("base_price_cents"),
                Jdbc.getLongOrNull(rs, "compare_at_price_cents"),
                rs.getInt("is_new") == 1,
                rs.getInt("customizable") == 1,
                rs.getInt("stock"),
                rs.getString("color_hex"),
                rs.getInt("variant_count"),
                Jdbc.getLongOrNull(rs, "single_variant_id"),
                rs.getString("image_path"));
    }
}
