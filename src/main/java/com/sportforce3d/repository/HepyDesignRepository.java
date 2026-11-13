package com.sportforce3d.repository;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.design.PgeyDesignElement;
import com.sportforce3d.model.PgeyDesignAsset;
import com.sportforce3d.model.PgeyDesignSummary;

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

public class HepyDesignRepository {
    public record HepyStoredDesign(PgeyDesignSummary hepySummary, long hepyUserId, String hepyDocumentJson, boolean hepyDeleted) {
    }

    private static final String HEPY_SUMMARY = """
            SELECT d.id, d.name, d.current_version, d.thumbnail_front, d.thumbnail_back, d.updated_at, d.product_id
            FROM designs d
            """;

    private final HepyDatabase hepyDb;

    public HepyDesignRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    public long hepyCreate(long hepyUserId, Long hepyProductId, String hepyName, int hepySchemaVersion, String hepyJson,
                       List<PgeyDesignElement> hepyElements, String hepyFront, String hepyBack) {
        return hepyDb.hepyInTransaction(hepyC -> {
            long hepyId;
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    INSERT INTO designs (user_id, product_id, name, current_version, thumbnail_front, thumbnail_back)
                    VALUES (?, ?, ?, 1, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                HepyJdbc.hepyBind(hepyPs, Arrays.asList(hepyUserId, hepyProductId, hepyName, hepyFront, hepyBack));
                hepyPs.executeUpdate();
                try (ResultSet hepyKeys = hepyPs.getGeneratedKeys()) {
                    hepyKeys.next();
                    hepyId = hepyKeys.getLong(1);
                }
            }
            hepyInsertVersion(hepyC, hepyId, 1, hepySchemaVersion, hepyJson, hepyElements);
            return hepyId;
        });
    }

    public int hepyAddVersion(long hepyDesignId, String hepyName, Long hepyProductId, int hepySchemaVersion, String hepyJson,
                          List<PgeyDesignElement> hepyElements, String hepyFront, String hepyBack) {
        return hepyDb.hepyInTransaction(hepyC -> {
            int hepyNext;
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "SELECT COALESCE(max(version), 0) + 1 FROM design_versions WHERE design_id = ?")) {
                hepyPs.setLong(1, hepyDesignId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    hepyRs.next();
                    hepyNext = hepyRs.getInt(1);
                }
            }
            hepyInsertVersion(hepyC, hepyDesignId, hepyNext, hepySchemaVersion, hepyJson, hepyElements);
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    UPDATE designs SET name = ?, product_id = ?, current_version = ?, thumbnail_front = ?,
                           thumbnail_back = ?
                    WHERE id = ?
                    """)) {
                HepyJdbc.hepyBind(hepyPs, Arrays.asList(hepyName, hepyProductId, hepyNext, hepyFront, hepyBack, hepyDesignId));
                hepyPs.executeUpdate();
            }
            return hepyNext;
        });
    }

    private static void hepyInsertVersion(Connection hepyC, long hepyDesignId, int hepyVersion, int hepySchemaVersion, String hepyJson,
                                      List<PgeyDesignElement> hepyElements) throws SQLException {
        long hepyVersionId;
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                INSERT INTO design_versions (design_id, version, schema_version, document_json) VALUES (?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            hepyPs.setLong(1, hepyDesignId);
            hepyPs.setInt(2, hepyVersion);
            hepyPs.setInt(3, hepySchemaVersion);
            hepyPs.setString(4, hepyJson);
            hepyPs.executeUpdate();
            try (ResultSet hepyKeys = hepyPs.getGeneratedKeys()) {
                hepyKeys.next();
                hepyVersionId = hepyKeys.getLong(1);
            }
        }
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                INSERT INTO design_elements (design_version_id, element_type, garment, zone, content, asset_id,
                                             pos_u, pos_v, scale, rotation_deg, layer_order, visible, locked)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            int hepyLayer = 0;
            for (PgeyDesignElement hepyE : hepyElements) {
                String hepyContent = switch (hepyE.pgeyType()) {
                    case NAME, NUMBER -> hepyE.pgeyText();
                    case EMBLEM -> hepyE.pgeyEmblem();
                    case LOGO -> null;
                };
                HepyJdbc.hepyBind(hepyPs, Arrays.asList(hepyVersionId, hepyE.pgeyType().pgeyDbType(), hepyE.pgeyArea().pgeyGarment().name(),
                        hepyE.pgeyArea().pgeyZone().pgeyMeshName(), hepyContent, hepyE.pgeyAssetId(), null, null, null, null, hepyLayer++,
                        hepyE.pgeyVisible(), hepyE.pgeyLocked()));
                hepyPs.setDouble(7, hepyE.pgeyU());
                hepyPs.setDouble(8, hepyE.pgeyV());
                hepyPs.setDouble(9, hepyE.pgeyScale());
                hepyPs.setDouble(10, hepyE.pgeyRotation());
                hepyPs.addBatch();
            }
            hepyPs.executeBatch();
        }
    }

    public List<PgeyDesignSummary> hepyFindByUser(long hepyUserId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    HEPY_SUMMARY + " WHERE d.user_id = ? AND d.deleted_at IS NULL ORDER BY d.updated_at DESC, d.id DESC")) {
                hepyPs.setLong(1, hepyUserId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    List<PgeyDesignSummary> hepyList = new ArrayList<>();
                    while (hepyRs.next()) {
                        hepyList.add(hepySummary(hepyRs));
                    }
                    return hepyList;
                }
            }
        });
    }

    public Optional<HepyStoredDesign> hepyFind(long hepyDesignId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT d.id, d.name, d.current_version, d.thumbnail_front, d.thumbnail_back, d.updated_at,
                           d.product_id, d.user_id, d.deleted_at, v.document_json
                    FROM designs d
                    JOIN design_versions v ON v.design_id = d.id AND v.version = d.current_version
                    WHERE d.id = ?
                    """)) {
                hepyPs.setLong(1, hepyDesignId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    if (!hepyRs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new HepyStoredDesign(hepySummary(hepyRs), hepyRs.getLong("user_id"),
                            hepyRs.getString("document_json"), hepyRs.getString("deleted_at") != null));
                }
            }
        });
    }

    public int hepyCountElements(long hepyDesignId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    SELECT count(*) FROM design_elements e
                    JOIN design_versions v ON v.id = e.design_version_id
                    JOIN designs d ON d.id = v.design_id AND d.current_version = v.version
                    WHERE d.id = ?
                    """)) {
                hepyPs.setLong(1, hepyDesignId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    hepyRs.next();
                    return hepyRs.getInt(1);
                }
            }
        });
    }

    public void hepySoftDelete(long hepyDesignId, Instant hepyWhen) {
        hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("UPDATE designs SET deleted_at = ? WHERE id = ?")) {
                hepyPs.setString(1, hepyWhen.toString());
                hepyPs.setLong(2, hepyDesignId);
                return hepyPs.executeUpdate();
            }
        });
    }

    public PgeyDesignAsset hepyInsertAsset(long hepyUserId, String hepyOriginalName, String hepyPath, String hepyMime, long hepySize, String hepySha256) {
        long hepyId = hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    INSERT INTO design_assets (user_id, original_name, stored_path, mime_type, size_bytes, sha256,
                                               rights_declared)
                    VALUES (?, ?, ?, ?, ?, ?, 1)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                HepyJdbc.hepyBind(hepyPs, List.of(hepyUserId, hepyOriginalName, hepyPath, hepyMime, hepySize, hepySha256));
                hepyPs.executeUpdate();
                try (ResultSet hepyKeys = hepyPs.getGeneratedKeys()) {
                    hepyKeys.next();
                    return hepyKeys.getLong(1);
                }
            }
        });
        return hepyFindAsset(hepyId).map(HepyStoredAsset::hepyAsset).orElseThrow();
    }

    public record HepyStoredAsset(PgeyDesignAsset hepyAsset, long hepyUserId, String hepySha256) {
    }

    public Optional<HepyStoredAsset> hepyFindAsset(long hepyAssetId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT * FROM design_assets WHERE id = ?")) {
                hepyPs.setLong(1, hepyAssetId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    return hepyRs.next() ? Optional.of(new HepyStoredAsset(hepyAsset(hepyRs), hepyRs.getLong("user_id"), hepyRs.getString("sha256")))
                            : Optional.empty();
                }
            }
        });
    }

    public Optional<PgeyDesignAsset> hepyFindAssetBySha(long hepyUserId, String hepySha256) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "SELECT * FROM design_assets WHERE user_id = ? AND sha256 = ? ORDER BY id LIMIT 1")) {
                hepyPs.setLong(1, hepyUserId);
                hepyPs.setString(2, hepySha256);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    return hepyRs.next() ? Optional.of(hepyAsset(hepyRs)) : Optional.empty();
                }
            }
        });
    }

    public List<PgeyDesignAsset> hepyFindAssets(long hepyUserId) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "SELECT * FROM design_assets WHERE user_id = ? ORDER BY id DESC")) {
                hepyPs.setLong(1, hepyUserId);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    List<PgeyDesignAsset> hepyList = new ArrayList<>();
                    while (hepyRs.next()) {
                        hepyList.add(hepyAsset(hepyRs));
                    }
                    return hepyList;
                }
            }
        });
    }

    private static PgeyDesignAsset hepyAsset(ResultSet hepyRs) throws SQLException {
        return new PgeyDesignAsset(hepyRs.getLong("id"), hepyRs.getString("original_name"), hepyRs.getString("stored_path"),
                hepyRs.getString("mime_type"), hepyRs.getLong("size_bytes"), hepyRs.getString("created_at"));
    }

    private static PgeyDesignSummary hepySummary(ResultSet hepyRs) throws SQLException {
        return new PgeyDesignSummary(hepyRs.getLong("id"), hepyRs.getString("name"), hepyRs.getInt("current_version"),
                hepyRs.getString("thumbnail_front"), hepyRs.getString("thumbnail_back"), hepyRs.getString("updated_at"),
                HepyJdbc.hepyGetLongOrNull(hepyRs, "product_id"));
    }
}
