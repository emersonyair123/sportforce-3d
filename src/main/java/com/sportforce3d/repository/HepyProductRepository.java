package com.sportforce3d.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.model.HepyAdminProductRow;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyInventoryRow;
import com.sportforce3d.model.HepyLowStockVariant;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.HepyPage;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.model.HepyProductForm;
import com.sportforce3d.model.HepyProductImage;
import com.sportforce3d.model.HepyProductQuery;
import com.sportforce3d.model.HepyProductSummary;
import com.sportforce3d.model.HepyProductVariant;
import com.sportforce3d.model.HepyRealisticLook;
import com.sportforce3d.util.HepyTexts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HepyProductRepository {
    private static final String HEPY_STOCK_JOIN = """
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

    private static final String HEPY_SUMMARY_COLUMNS = """
            SELECT p.id, p.sku, p.name, cat.name AS category_name, p.garment_type,
                   p.base_price_cents, p.compare_at_price_cents, p.is_new, p.customizable,
                   COALESCE(st.stock, 0) AS stock, COALESCE(st.variant_count, 0) AS variant_count,
                   st.single_variant_id,
                   (SELECT v2.color_hex FROM product_variants v2
                     WHERE v2.product_id = p.id AND v2.active = 1 ORDER BY v2.id LIMIT 1) AS color_hex,
                   (SELECT pi.path FROM product_images pi
                     WHERE pi.product_id = p.id ORDER BY pi.sort_order, pi.id LIMIT 1) AS image_path,
                   p.model_key, p.zone_colors, p.pattern, p.pattern_color
            """;

    private static final String HEPY_PUBLIC_FROM = """
            FROM products p
            JOIN categories cat ON cat.id = p.category_id
            """ + HEPY_STOCK_JOIN + """
            WHERE p.active = 1 AND p.deleted_at IS NULL AND cat.active = 1
            """;

    private final HepyDatabase hepyDb;

    public HepyProductRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    public HepyPage<HepyProductSummary> hepySearch(HepyProductQuery hepyQuery) {
        StringBuilder hepyWhere = new StringBuilder();
        List<Object> hepyParams = new ArrayList<>();

        if (hepyQuery.hepyText() != null && !hepyQuery.hepyText().isBlank()) {
            for (String hepyToken : HepyTexts.hepySearchKey(hepyQuery.hepyText()).split("\\s+")) {
                if (!hepyToken.isEmpty()) {
                    hepyWhere.append(" AND p.search_key LIKE ? ESCAPE '\\'");
                    hepyParams.add("%" + hepyToken.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
                }
            }
        }
        if (hepyQuery.hepyCategoryId() != null) {
            hepyWhere.append(" AND p.category_id = ?");
            hepyParams.add(hepyQuery.hepyCategoryId());
        }
        if (hepyQuery.hepySportId() != null) {
            hepyWhere.append(" AND p.sport_id = ?");
            hepyParams.add(hepyQuery.hepySportId());
        }
        if (!hepyQuery.hepySizeCodes().isEmpty()) {
            hepyWhere.append("""
                     AND EXISTS (SELECT 1 FROM product_variants sv
                                 JOIN sizes sz ON sz.id = sv.size_id
                                 LEFT JOIN inventory si ON si.variant_id = sv.id
                                 WHERE sv.product_id = p.id AND sv.active = 1 AND sz.code IN (%s)%s)
                    """.formatted(HepyJdbc.hepyPlaceholders(hepyQuery.hepySizeCodes().size()),
                    hepyQuery.hepyInStockOnly() ? " AND COALESCE(si.quantity, 0) - COALESCE(si.reserved, 0) > 0" : ""));
            hepyParams.addAll(hepyQuery.hepySizeCodes().stream().sorted().toList());
        }
        if (hepyQuery.hepyMinPriceCents() != null) {
            hepyWhere.append(" AND p.base_price_cents >= ?");
            hepyParams.add(hepyQuery.hepyMinPriceCents());
        }
        if (hepyQuery.hepyMaxPriceCents() != null) {
            hepyWhere.append(" AND p.base_price_cents <= ?");
            hepyParams.add(hepyQuery.hepyMaxPriceCents());
        }
        if (hepyQuery.hepyInStockOnly()) {
            hepyWhere.append(" AND COALESCE(st.stock, 0) > 0");
        }
        if (hepyQuery.hepyCustomizableOnly()) {
            hepyWhere.append(" AND p.customizable = 1");
        }

        String hepyOrderBy = switch (hepyQuery.hepySort()) {
            case RELEVANCE -> "(COALESCE(st.stock, 0) > 0) DESC, p.featured DESC, p.is_new DESC, p.id";
            case NEWEST -> "p.is_new DESC, p.created_at DESC, p.id DESC";
            case PRICE_ASC -> "p.base_price_cents, p.id";
            case PRICE_DESC -> "p.base_price_cents DESC, p.id";
            case NAME -> "p.name COLLATE NOCASE, p.id";
        };

        return hepyDb.hepyWithConnection(hepyC -> {
            long hepyTotal;
            try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT count(*) " + HEPY_PUBLIC_FROM + hepyWhere)) {
                HepyJdbc.hepyBind(hepyPs, hepyParams);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    hepyRs.next();
                    hepyTotal = hepyRs.getLong(1);
                }
            }
            List<Object> hepyPageParams = new ArrayList<>(hepyParams);
            hepyPageParams.add(hepyQuery.hepyPageSize());
            hepyPageParams.add(hepyQuery.hepyOffset());
            List<HepyProductSummary> hepyItems = new ArrayList<>();
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    HEPY_SUMMARY_COLUMNS + HEPY_PUBLIC_FROM + hepyWhere + " ORDER BY " + hepyOrderBy + " LIMIT ? OFFSET ?")) {
                HepyJdbc.hepyBind(hepyPs, hepyPageParams);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    while (hepyRs.next()) {
                        hepyItems.add(hepyMapSummary(hepyRs));
                    }
                }
            }
            return new HepyPage<>(hepyItems, hepyQuery.hepyPage(), hepyQuery.hepyPageSize(), hepyTotal);
        });
    }

    public List<HepyProductSummary> hepyFindFeatured(int hepyLimit) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    HEPY_SUMMARY_COLUMNS + HEPY_PUBLIC_FROM + " AND p.featured = 1 ORDER BY p.id LIMIT ?")) {
                hepyPs.setInt(1, hepyLimit);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    List<HepyProductSummary> hepyList = new ArrayList<>();
                    while (hepyRs.next()) {
                        hepyList.add(hepyMapSummary(hepyRs));
                    }
                    return hepyList;
                }
            }
        });
    }

    public Optional<HepyRealisticLook> hepyFindLook(long hepyProductId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "SELECT model_key, zone_colors, pattern, pattern_color FROM products WHERE id = ?")) {
                hepyPs.setLong(1, hepyProductId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    return hepyRs.next() ? Optional.ofNullable(hepyLook(hepyRs)) : Optional.empty();
                }
            }
        });
    }

    public Optional<HepyProductDetail> hepyFindDetail(long hepyProductId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT p.*, cat.name AS category_name, sp.name AS sport_name
                    FROM products p
                    JOIN categories cat ON cat.id = p.category_id
                    LEFT JOIN sports sp ON sp.id = p.sport_id
                    WHERE p.id = ?
                    """)) {
                hepyPs.setLong(1, hepyProductId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    if (!hepyRs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new HepyProductDetail(
                            hepyRs.getLong("id"),
                            hepyRs.getLong("category_id"),
                            hepyRs.getString("category_name"),
                            HepyJdbc.hepyGetLongOrNull(hepyRs, "sport_id"),
                            hepyRs.getString("sport_name"),
                            hepyRs.getString("sku"),
                            hepyRs.getString("name"),
                            hepyRs.getString("description"),
                            HepyGarment.valueOf(hepyRs.getString("garment_type")),
                            hepyRs.getLong("base_price_cents"),
                            HepyJdbc.hepyGetLongOrNull(hepyRs, "compare_at_price_cents"),
                            hepyRs.getInt("customizable") == 1,
                            hepyRs.getInt("is_new") == 1,
                            hepyRs.getInt("featured") == 1,
                            hepyRs.getInt("active") == 1,
                            hepyRs.getString("deleted_at") != null,
                            hepyVariants(hepyC, hepyProductId),
                            hepyImages(hepyC, hepyProductId),
                            hepyMaterials(hepyC, hepyProductId)));
                }
            }
        });
    }

    private static List<HepyProductVariant> hepyVariants(Connection hepyC, long hepyProductId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
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
            hepyPs.setLong(1, hepyProductId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                List<HepyProductVariant> hepyList = new ArrayList<>();
                while (hepyRs.next()) {
                    hepyList.add(new HepyProductVariant(hepyRs.getLong("id"), hepyRs.getLong("size_id"), hepyRs.getString("size_code"),
                            hepyRs.getString("size_name"), hepyRs.getString("color_name"), hepyRs.getString("color_hex"),
                            hepyRs.getString("sku"), hepyRs.getLong("price_delta_cents"), hepyRs.getInt("active") == 1,
                            hepyRs.getInt("quantity"), hepyRs.getInt("reserved"), hepyRs.getInt("low_stock_threshold")));
                }
                return hepyList;
            }
        }
    }

    private static List<HepyProductImage> hepyImages(Connection hepyC, long hepyProductId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(
                "SELECT id, path, alt_text, sort_order FROM product_images WHERE product_id = ? ORDER BY sort_order, id")) {
            hepyPs.setLong(1, hepyProductId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                List<HepyProductImage> hepyList = new ArrayList<>();
                while (hepyRs.next()) {
                    hepyList.add(new HepyProductImage(hepyRs.getLong("id"), hepyRs.getString("path"), hepyRs.getString("alt_text"),
                            hepyRs.getInt("sort_order")));
                }
                return hepyList;
            }
        }
    }

    private static List<HepyMaterial> hepyMaterials(Connection hepyC, long hepyProductId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                SELECT m.* FROM product_materials pm JOIN materials m ON m.id = pm.material_id
                WHERE pm.product_id = ? AND m.active = 1
                ORDER BY pm.is_default DESC, m.sort_order
                """)) {
            hepyPs.setLong(1, hepyProductId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                List<HepyMaterial> hepyList = new ArrayList<>();
                while (hepyRs.next()) {
                    hepyList.add(HepyCatalogRepository.hepyMapMaterial(hepyRs));
                }
                return hepyList;
            }
        }
    }

    public record HepyVariantInfo(long hepyVariantId, long hepyProductId, String hepyProductName, String hepySizeName,
                              long hepyUnitPriceCents, int hepyAvailable, boolean hepyPurchasable) {
    }

    public Optional<HepyVariantInfo> hepyFindVariantInfo(long hepyVariantId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
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
                hepyPs.setLong(1, hepyVariantId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    if (!hepyRs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new HepyVariantInfo(hepyRs.getLong(1), hepyRs.getLong("product_id"), hepyRs.getString("name"),
                            hepyRs.getString("size_name"), hepyRs.getLong("unit_price"), hepyRs.getInt("available"),
                            hepyRs.getInt("purchasable") == 1));
                }
            }
        });
    }

    public List<HepyAdminProductRow> hepyFindForAdmin(String hepyText, boolean hepyIncludeDeleted) {
        StringBuilder hepySql = new StringBuilder("""
                SELECT p.id, p.sku, p.name, cat.name AS category_name, p.garment_type, p.base_price_cents,
                       p.active, p.deleted_at,
                       COALESCE(st.stock, 0) AS stock, COALESCE(st.variant_count, 0) AS variant_count,
                       (SELECT v2.color_hex FROM product_variants v2
                         WHERE v2.product_id = p.id ORDER BY v2.active DESC, v2.id LIMIT 1) AS color_hex,
                       (SELECT pi.path FROM product_images pi
                         WHERE pi.product_id = p.id ORDER BY pi.sort_order, pi.id LIMIT 1) AS image_path
                FROM products p
                JOIN categories cat ON cat.id = p.category_id
                """ + HEPY_STOCK_JOIN + " WHERE 1 = 1");
        List<Object> hepyParams = new ArrayList<>();
        if (!hepyIncludeDeleted) {
            hepySql.append(" AND p.deleted_at IS NULL");
        }
        if (hepyText != null && !hepyText.isBlank()) {
            hepySql.append(" AND p.search_key LIKE ?");
            hepyParams.add("%" + HepyTexts.hepySearchKey(hepyText) + "%");
        }
        hepySql.append(" ORDER BY p.deleted_at IS NOT NULL, p.name COLLATE NOCASE");
        return hepyDb.hepyWithConnection(hepyC -> {
            Map<Long, List<HepyLowStockVariant>> hepyLow = hepyLowStockVariants(hepyC);
            try (PreparedStatement hepyPs = hepyC.prepareStatement(hepySql.toString())) {
                HepyJdbc.hepyBind(hepyPs, hepyParams);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    List<HepyAdminProductRow> hepyList = new ArrayList<>();
                    while (hepyRs.next()) {
                        long hepyId = hepyRs.getLong("id");
                        hepyList.add(new HepyAdminProductRow(hepyId, hepyRs.getString("sku"), hepyRs.getString("name"),
                                hepyRs.getString("category_name"), HepyGarment.valueOf(hepyRs.getString("garment_type")),
                                hepyRs.getLong("base_price_cents"), hepyRs.getInt("stock"), hepyRs.getInt("variant_count"),
                                hepyRs.getInt("active") == 1, hepyRs.getString("deleted_at") != null,
                                hepyRs.getString("color_hex"), hepyRs.getString("image_path"),
                                hepyLow.getOrDefault(hepyId, List.of())));
                    }
                    return hepyList;
                }
            }
        });
    }

    private static Map<Long, List<HepyLowStockVariant>> hepyLowStockVariants(Connection hepyC) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
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
             ResultSet hepyRs = hepyPs.executeQuery()) {
            Map<Long, List<HepyLowStockVariant>> hepyMap = new HashMap<>();
            while (hepyRs.next()) {
                hepyMap.computeIfAbsent(hepyRs.getLong("product_id"), hepyK -> new ArrayList<>())
                        .add(new HepyLowStockVariant(hepyRs.getString("size_name"), hepyRs.getString("color_name"),
                                hepyRs.getInt("available"), hepyRs.getInt("threshold"), hepyRs.getInt("multi_color") == 1));
            }
            return hepyMap;
        }
    }

    public boolean hepySkuExists(String hepySku, Long hepyExceptProductId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "SELECT 1 FROM products WHERE sku = ? AND id <> COALESCE(?, -1)")) {
                hepyPs.setString(1, hepySku);
                if (hepyExceptProductId == null) {
                    hepyPs.setNull(2, java.sql.Types.INTEGER);
                } else {
                    hepyPs.setLong(2, hepyExceptProductId);
                }
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    return hepyRs.next();
                }
            }
        });
    }

    public long hepySave(HepyProductForm hepyForm) {
        return hepyDb.hepyInTransaction(hepyC -> {
            long hepyId;
            String hepySlug = HepyTexts.hepySearchKey(hepyForm.hepyName()).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "")
                    + "-" + HepyTexts.hepySkuPart(hepyForm.hepySku()).toLowerCase();
            String hepySearchKey = HepyTexts.hepySearchKey(hepyForm.hepyName() + " " + (hepyForm.hepyDescription() == null ? "" : hepyForm.hepyDescription())
                    + " " + hepyForm.hepySku());
            if (hepyForm.hepyId() == null) {
                try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                        INSERT INTO products (category_id, sport_id, sku, slug, name, description, garment_type,
                                              base_price_cents, compare_at_price_cents, customizable, is_new, featured,
                                              active, search_key)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    HepyJdbc.hepyBind(hepyPs, hepyProductParams(hepyForm, hepySlug, hepySearchKey));
                    hepyPs.executeUpdate();
                    try (ResultSet hepyKeys = hepyPs.getGeneratedKeys()) {
                        hepyKeys.next();
                        hepyId = hepyKeys.getLong(1);
                    }
                }
            } else {
                hepyId = hepyForm.hepyId();
                List<Object> hepyParams = hepyProductParams(hepyForm, hepySlug, hepySearchKey);
                hepyParams.add(hepyId);
                try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                        UPDATE products SET category_id = ?, sport_id = ?, sku = ?, slug = ?, name = ?, description = ?,
                               garment_type = ?, base_price_cents = ?, compare_at_price_cents = ?, customizable = ?,
                               is_new = ?, featured = ?, active = ?, search_key = ?
                        WHERE id = ?
                        """)) {
                    HepyJdbc.hepyBind(hepyPs, hepyParams);
                    hepyPs.executeUpdate();
                }
            }

            try (PreparedStatement hepyPs = hepyC.prepareStatement("DELETE FROM product_materials WHERE product_id = ?")) {
                hepyPs.setLong(1, hepyId);
                hepyPs.executeUpdate();
            }
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "INSERT INTO product_materials (product_id, material_id, is_default) VALUES (?, ?, 0)")) {
                for (Long hepyMaterialId : hepyForm.hepyMaterialIds()) {
                    hepyPs.setLong(1, hepyId);
                    hepyPs.setLong(2, hepyMaterialId);
                    hepyPs.addBatch();
                }
                hepyPs.executeBatch();
            }

            for (HepyProductForm.HepyVariantForm hepyV : hepyForm.hepyVariants()) {
                hepySaveVariant(hepyC, hepyId, hepyV);
            }
            return hepyId;
        });
    }

    private static List<Object> hepyProductParams(HepyProductForm hepyF, String hepySlug, String hepySearchKey) {
        List<Object> hepyParams = new ArrayList<>();
        hepyParams.add(hepyF.hepyCategoryId());
        hepyParams.add(hepyF.hepySportId());
        hepyParams.add(hepyF.hepySku());
        hepyParams.add(hepySlug);
        hepyParams.add(hepyF.hepyName());
        hepyParams.add(hepyF.hepyDescription());
        hepyParams.add(hepyF.hepyGarment().name());
        hepyParams.add(hepyF.hepyBasePriceCents());
        hepyParams.add(hepyF.hepyCompareAtPriceCents());
        hepyParams.add(hepyF.hepyCustomizable());
        hepyParams.add(hepyF.hepyIsNew());
        hepyParams.add(hepyF.hepyFeatured());
        hepyParams.add(hepyF.hepyActive());
        hepyParams.add(hepySearchKey);
        return hepyParams;
    }

    private static void hepySaveVariant(Connection hepyC, long hepyProductId, HepyProductForm.HepyVariantForm hepyV) throws SQLException {
        long hepyVariantId;
        if (hepyV.hepyId() == null) {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    INSERT INTO product_variants (product_id, size_id, color_name, color_hex, sku, price_delta_cents, active)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                HepyJdbc.hepyBind(hepyPs, List.of(hepyProductId, hepyV.hepySizeId(), hepyV.hepyColorName(), hepyV.hepyColorHex(), hepyV.hepySku(),
                        hepyV.hepyPriceDeltaCents(), hepyV.hepyActive()));
                hepyPs.executeUpdate();
                try (ResultSet hepyKeys = hepyPs.getGeneratedKeys()) {
                    hepyKeys.next();
                    hepyVariantId = hepyKeys.getLong(1);
                }
            }
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "INSERT INTO inventory (variant_id, quantity, low_stock_threshold) VALUES (?, ?, ?)")) {
                hepyPs.setLong(1, hepyVariantId);
                hepyPs.setInt(2, hepyV.hepyStock());
                hepyPs.setInt(3, hepyV.hepyLowStockThreshold());
                hepyPs.executeUpdate();
            }
        } else {
            hepyVariantId = hepyV.hepyId();
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    UPDATE product_variants SET size_id = ?, color_name = ?, color_hex = ?, sku = ?,
                           price_delta_cents = ?, active = ?
                    WHERE id = ? AND product_id = ?
                    """)) {
                HepyJdbc.hepyBind(hepyPs, List.of(hepyV.hepySizeId(), hepyV.hepyColorName(), hepyV.hepyColorHex(), hepyV.hepySku(), hepyV.hepyPriceDeltaCents(),
                        hepyV.hepyActive(), hepyVariantId, hepyProductId));
                if (hepyPs.executeUpdate() != 1) {
                    throw new SQLException("La variante " + hepyVariantId + " no pertenece al producto " + hepyProductId);
                }
            }
            hepyUpsertInventory(hepyC, hepyVariantId, hepyV.hepyStock(), hepyV.hepyLowStockThreshold());
        }
    }

    private static void hepyUpsertInventory(Connection hepyC, long hepyVariantId, int hepyQuantity, int hepyThreshold) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                INSERT INTO inventory (variant_id, quantity, low_stock_threshold) VALUES (?, ?, ?)
                ON CONFLICT (variant_id) DO UPDATE SET quantity = excluded.quantity,
                                                     low_stock_threshold = excluded.low_stock_threshold
                """)) {
            hepyPs.setLong(1, hepyVariantId);
            hepyPs.setInt(2, hepyQuantity);
            hepyPs.setInt(3, hepyThreshold);
            hepyPs.executeUpdate();
        }
    }

    public void hepySoftDelete(long hepyProductId, Instant hepyWhen) {
        hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "UPDATE products SET deleted_at = ?, active = 0 WHERE id = ? AND deleted_at IS NULL")) {
                hepyPs.setString(1, hepyWhen.toString());
                hepyPs.setLong(2, hepyProductId);
                return hepyPs.executeUpdate();
            }
        });
    }

    public void hepyRestore(long hepyProductId) {
        hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("UPDATE products SET deleted_at = NULL WHERE id = ?")) {
                hepyPs.setLong(1, hepyProductId);
                return hepyPs.executeUpdate();
            }
        });
    }

    public void hepyUpdateZoneColors(long hepyProductId, Map<String, String> hepyZoneColors) {
        String hepyJson;
        try {
            hepyJson = HEPY_JSON.writeValueAsString(new LinkedHashMap<>(hepyZoneColors));
        } catch (JsonProcessingException hepyE) {
            throw new IllegalArgumentException("Colores por zona inválidos", hepyE);
        }
        hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("UPDATE products SET zone_colors = ? WHERE id = ?")) {
                hepyPs.setString(1, hepyJson);
                hepyPs.setLong(2, hepyProductId);
                return hepyPs.executeUpdate();
            }
        });
    }

    public HepyProductImage hepyAddImage(long hepyProductId, String hepyPath, String hepyAltText) {
        return hepyDb.hepyInTransaction(hepyC -> {
            int hepyOrder;
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "SELECT COALESCE(max(sort_order) + 1, 0) FROM product_images WHERE product_id = ?")) {
                hepyPs.setLong(1, hepyProductId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    hepyRs.next();
                    hepyOrder = hepyRs.getInt(1);
                }
            }
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "INSERT INTO product_images (product_id, path, alt_text, sort_order) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                hepyPs.setLong(1, hepyProductId);
                hepyPs.setString(2, hepyPath);
                hepyPs.setString(3, hepyAltText);
                hepyPs.setInt(4, hepyOrder);
                hepyPs.executeUpdate();
                try (ResultSet hepyKeys = hepyPs.getGeneratedKeys()) {
                    hepyKeys.next();
                    return new HepyProductImage(hepyKeys.getLong(1), hepyPath, hepyAltText, hepyOrder);
                }
            }
        });
    }

    public Optional<String> hepyRemoveImage(long hepyProductId, long hepyImageId) {
        return hepyDb.hepyInTransaction(hepyC -> {
            String hepyPath = null;
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "SELECT path FROM product_images WHERE id = ? AND product_id = ?")) {
                hepyPs.setLong(1, hepyImageId);
                hepyPs.setLong(2, hepyProductId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    if (hepyRs.next()) {
                        hepyPath = hepyRs.getString(1);
                    }
                }
            }
            if (hepyPath != null) {
                try (PreparedStatement hepyPs = hepyC.prepareStatement("DELETE FROM product_images WHERE id = ?")) {
                    hepyPs.setLong(1, hepyImageId);
                    hepyPs.executeUpdate();
                }
            }
            return Optional.ofNullable(hepyPath);
        });
    }

    public List<HepyInventoryRow> hepyFindInventory(String hepyText, boolean hepyLowStockOnly) {
        StringBuilder hepySql = new StringBuilder("""
                SELECT v.id AS variant_id, p.id AS product_id, p.name AS product_name, v.sku, s.name AS size_name,
                       v.color_name, v.color_hex, COALESCE(i.quantity, 0) AS quantity, COALESCE(i.reserved, 0) AS reserved,
                       COALESCE(i.low_stock_threshold, 5) AS low_stock_threshold, p.active AS product_active
                FROM product_variants v
                JOIN products p ON p.id = v.product_id
                JOIN sizes s ON s.id = v.size_id
                LEFT JOIN inventory i ON i.variant_id = v.id
                WHERE p.deleted_at IS NULL AND v.active = 1
                """);
        List<Object> hepyParams = new ArrayList<>();
        if (hepyText != null && !hepyText.isBlank()) {
            hepySql.append(" AND (p.search_key LIKE ? OR lower(v.sku) LIKE ?)");
            String hepyLike = "%" + HepyTexts.hepySearchKey(hepyText) + "%";
            hepyParams.add(hepyLike);
            hepyParams.add(hepyLike);
        }
        if (hepyLowStockOnly) {
            hepySql.append(" AND COALESCE(i.quantity, 0) - COALESCE(i.reserved, 0) <= COALESCE(i.low_stock_threshold, 5)");
        }
        hepySql.append(" ORDER BY (COALESCE(i.quantity, 0) - COALESCE(i.reserved, 0)) <= COALESCE(i.low_stock_threshold, 5) DESC,")
                .append(" p.name COLLATE NOCASE, s.sort_order");
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(hepySql.toString())) {
                HepyJdbc.hepyBind(hepyPs, hepyParams);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    List<HepyInventoryRow> hepyList = new ArrayList<>();
                    while (hepyRs.next()) {
                        hepyList.add(new HepyInventoryRow(hepyRs.getLong("variant_id"), hepyRs.getLong("product_id"),
                                hepyRs.getString("product_name"), hepyRs.getString("sku"), hepyRs.getString("size_name"),
                                hepyRs.getString("color_name"), hepyRs.getString("color_hex"), hepyRs.getInt("quantity"),
                                hepyRs.getInt("reserved"), hepyRs.getInt("low_stock_threshold"),
                                hepyRs.getInt("product_active") == 1));
                    }
                    return hepyList;
                }
            }
        });
    }

    public Optional<HepyInventoryRow> hepyFindInventoryRow(long hepyVariantId) {
        return hepyFindInventory(null, false).stream().filter(hepyR -> hepyR.hepyVariantId() == hepyVariantId).findFirst();
    }

    public void hepyUpdateStock(long hepyVariantId, int hepyQuantity, int hepyThreshold) {
        hepyDb.hepyInTransaction(hepyC -> {
            hepyUpsertInventory(hepyC, hepyVariantId, hepyQuantity, hepyThreshold);
            return null;
        });
    }

    private static HepyProductSummary hepyMapSummary(ResultSet hepyRs) throws SQLException {
        return new HepyProductSummary(
                hepyRs.getLong("id"),
                hepyRs.getString("sku"),
                hepyRs.getString("name"),
                hepyRs.getString("category_name"),
                HepyGarment.valueOf(hepyRs.getString("garment_type")),
                hepyRs.getLong("base_price_cents"),
                HepyJdbc.hepyGetLongOrNull(hepyRs, "compare_at_price_cents"),
                hepyRs.getInt("is_new") == 1,
                hepyRs.getInt("customizable") == 1,
                hepyRs.getInt("stock"),
                hepyRs.getString("color_hex"),
                hepyRs.getInt("variant_count"),
                HepyJdbc.hepyGetLongOrNull(hepyRs, "single_variant_id"),
                hepyRs.getString("image_path"),
                hepyLook(hepyRs));
    }

    private static final ObjectMapper HEPY_JSON = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, String>> HEPY_ZONE_COLORS = new TypeReference<>() {
    };

    private static HepyRealisticLook hepyLook(ResultSet hepyRs) throws SQLException {
        String hepyModel = hepyRs.getString("model_key");
        if (hepyModel == null || hepyModel.isBlank()) {
            return null;
        }
        String hepyColors = hepyRs.getString("zone_colors");
        try {
            Map<String, String> hepyZones = hepyColors == null ? Map.of() : HEPY_JSON.readValue(hepyColors, HEPY_ZONE_COLORS);
            return new HepyRealisticLook(hepyModel, hepyZones, hepyRs.getString("pattern"), hepyRs.getString("pattern_color"));
        } catch (JsonProcessingException hepyE) {
            return null;
        }
    }
}
