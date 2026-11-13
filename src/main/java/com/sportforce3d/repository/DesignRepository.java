package com.sportforce3d.repository;

import com.sportforce3d.db.Database;
import com.sportforce3d.design.DesignElement;
import com.sportforce3d.model.DesignAsset;
import com.sportforce3d.model.DesignSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DesignRepository {
    public record StoredDesign(DesignSummary summary, long userId, String documentJson, boolean deleted) {
    }

    private static final String SUMMARY = """
            SELECT d.id, d.name, d.current_version, d.thumbnail_front, d.thumbnail_back, d.updated_at, d.product_id
            FROM designs d
            """;

    private final Database db;

    public DesignRepository(Database db) {
        this.db = db;
    }

    public long create(long userId, Long productId, String name, int schemaVersion, String json,
                       List<DesignElement> elements, String front, String back) {
        return db.inTransaction(c -> {
            long id;
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO designs (user_id, product_id, name, current_version, thumbnail_front, thumbnail_back)
                    VALUES (?, ?, ?, 1, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                Jdbc.bind(ps, Arrays.asList(userId, productId, name, front, back));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
            }
            insertVersion(c, id, 1, schemaVersion, json, elements);
            return id;
        });
    }

    public int addVersion(long designId, String name, Long productId, int schemaVersion, String json,
                          List<DesignElement> elements, String front, String back) {
        return db.inTransaction(c -> {
            int next;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(max(version), 0) + 1 FROM design_versions WHERE design_id = ?")) {
                ps.setLong(1, designId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    next = rs.getInt(1);
                }
            }
            insertVersion(c, designId, next, schemaVersion, json, elements);
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE designs SET name = ?, product_id = ?, current_version = ?, thumbnail_front = ?,
                           thumbnail_back = ?
                    WHERE id = ?
                    """)) {
                Jdbc.bind(ps, Arrays.asList(name, productId, next, front, back, designId));
                ps.executeUpdate();
            }
            return next;
        });
    }

    private static void insertVersion(Connection c, long designId, int version, int schemaVersion, String json,
                                      List<DesignElement> elements) throws SQLException {
        long versionId;
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO design_versions (design_id, version, schema_version, document_json) VALUES (?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, designId);
            ps.setInt(2, version);
            ps.setInt(3, schemaVersion);
            ps.setString(4, json);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                versionId = keys.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO design_elements (design_version_id, element_type, garment, zone, content, asset_id,
                                             pos_u, pos_v, scale, rotation_deg, layer_order, visible, locked)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            int layer = 0;
            for (DesignElement e : elements) {
                String content = switch (e.type()) {
                    case NAME, NUMBER -> e.text();
                    case EMBLEM -> e.emblem();
                    case LOGO -> null;
                };
                Jdbc.bind(ps, Arrays.asList(versionId, e.type().dbType(), e.area().garment().name(),
                        e.area().zone().meshName(), content, e.assetId(), null, null, null, null, layer++,
                        e.visible(), e.locked()));
                ps.setDouble(7, e.u());
                ps.setDouble(8, e.v());
                ps.setDouble(9, e.scale());
                ps.setDouble(10, e.rotation());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<DesignSummary> findByUser(long userId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    SUMMARY + " WHERE d.user_id = ? AND d.deleted_at IS NULL ORDER BY d.updated_at DESC, d.id DESC")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<DesignSummary> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(summary(rs));
                    }
                    return list;
                }
            }
        });
    }

    public Optional<StoredDesign> find(long designId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT d.id, d.name, d.current_version, d.thumbnail_front, d.thumbnail_back, d.updated_at,
                           d.product_id, d.user_id, d.deleted_at, v.document_json
                    FROM designs d
                    JOIN design_versions v ON v.design_id = d.id AND v.version = d.current_version
                    WHERE d.id = ?
                    """)) {
                ps.setLong(1, designId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new StoredDesign(summary(rs), rs.getLong("user_id"),
                            rs.getString("document_json"), rs.getString("deleted_at") != null));
                }
            }
        });
    }

    public int countElements(long designId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT count(*) FROM design_elements e
                    JOIN design_versions v ON v.id = e.design_version_id
                    JOIN designs d ON d.id = v.design_id AND d.current_version = v.version
                    WHERE d.id = ?
                    """)) {
                ps.setLong(1, designId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    public void softDelete(long designId, Instant when) {
        db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE designs SET deleted_at = ? WHERE id = ?")) {
                ps.setString(1, when.toString());
                ps.setLong(2, designId);
                return ps.executeUpdate();
            }
        });
    }

    public DesignAsset insertAsset(long userId, String originalName, String path, String mime, long size, String sha256) {
        long id = db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO design_assets (user_id, original_name, stored_path, mime_type, size_bytes, sha256,
                                               rights_declared)
                    VALUES (?, ?, ?, ?, ?, ?, 1)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                Jdbc.bind(ps, List.of(userId, originalName, path, mime, size, sha256));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
        return findAsset(id).map(StoredAsset::asset).orElseThrow();
    }

    public record StoredAsset(DesignAsset asset, long userId, String sha256) {
    }

    public Optional<StoredAsset> findAsset(long assetId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM design_assets WHERE id = ?")) {
                ps.setLong(1, assetId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(new StoredAsset(asset(rs), rs.getLong("user_id"), rs.getString("sha256")))
                            : Optional.empty();
                }
            }
        });
    }

    public Optional<DesignAsset> findAssetBySha(long userId, String sha256) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM design_assets WHERE user_id = ? AND sha256 = ? ORDER BY id LIMIT 1")) {
                ps.setLong(1, userId);
                ps.setString(2, sha256);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(asset(rs)) : Optional.empty();
                }
            }
        });
    }

    public List<DesignAsset> findAssets(long userId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM design_assets WHERE user_id = ? ORDER BY id DESC")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<DesignAsset> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(asset(rs));
                    }
                    return list;
                }
            }
        });
    }

    private static DesignAsset asset(ResultSet rs) throws SQLException {
        return new DesignAsset(rs.getLong("id"), rs.getString("original_name"), rs.getString("stored_path"),
                rs.getString("mime_type"), rs.getLong("size_bytes"), rs.getString("created_at"));
    }

    private static DesignSummary summary(ResultSet rs) throws SQLException {
        return new DesignSummary(rs.getLong("id"), rs.getString("name"), rs.getInt("current_version"),
                rs.getString("thumbnail_front"), rs.getString("thumbnail_back"), rs.getString("updated_at"),
                Jdbc.getLongOrNull(rs, "product_id"));
    }
}
