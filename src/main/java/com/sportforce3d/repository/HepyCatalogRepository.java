package com.sportforce3d.repository;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.model.HepyCategory;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.HepySize;
import com.sportforce3d.model.HepySport;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

public class HepyCatalogRepository {

    private final HepyDatabase hepyDb;

    public HepyCatalogRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    public List<HepyCategory> hepyFindActiveCategories() {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "SELECT id, slug, name, description FROM categories WHERE active = 1 ORDER BY sort_order, name");
                 ResultSet hepyRs = hepyPs.executeQuery()) {
                List<HepyCategory> hepyList = new ArrayList<>();
                while (hepyRs.next()) {
                    hepyList.add(new HepyCategory(hepyRs.getLong("id"), hepyRs.getString("slug"), hepyRs.getString("name"),
                            hepyRs.getString("description")));
                }
                return hepyList;
            }
        });
    }

    public List<HepySport> hepyFindSports() {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT id, slug, name FROM sports ORDER BY sort_order, name");
                 ResultSet hepyRs = hepyPs.executeQuery()) {
                List<HepySport> hepyList = new ArrayList<>();
                while (hepyRs.next()) {
                    hepyList.add(new HepySport(hepyRs.getLong("id"), hepyRs.getString("slug"), hepyRs.getString("name")));
                }
                return hepyList;
            }
        });
    }

    public List<HepySize> hepyFindSizes() {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "SELECT id, code, name, size_group FROM sizes ORDER BY sort_order");
                 ResultSet hepyRs = hepyPs.executeQuery()) {
                List<HepySize> hepyList = new ArrayList<>();
                while (hepyRs.next()) {
                    hepyList.add(new HepySize(hepyRs.getLong("id"), hepyRs.getString("code"), hepyRs.getString("name"),
                            hepyRs.getString("size_group")));
                }
                return hepyList;
            }
        });
    }

    public List<HepyMaterial> hepyFindMaterials(HepyMaterial.HepyScope hepyScope) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(
                    "SELECT * FROM materials WHERE active = 1 AND scope = ? ORDER BY sort_order")) {
                hepyPs.setString(1, hepyScope.name());
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    List<HepyMaterial> hepyList = new ArrayList<>();
                    while (hepyRs.next()) {
                        hepyList.add(hepyMapMaterial(hepyRs));
                    }
                    return hepyList;
                }
            }
        });
    }

    public OptionalLong hepyFindLongSetting(String hepyKey) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT value FROM store_settings WHERE key = ?")) {
                hepyPs.setString(1, hepyKey);
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    if (!hepyRs.next()) {
                        return OptionalLong.empty();
                    }
                    try {
                        return OptionalLong.of(Long.parseLong(hepyRs.getString(1).trim()));
                    } catch (NumberFormatException hepyE) {
                        return OptionalLong.empty();
                    }
                }
            }
        });
    }

    static HepyMaterial hepyMapMaterial(ResultSet hepyRs) throws SQLException {
        Set<String> hepySuitable = Arrays.stream(hepyRs.getString("suitable_for").split(","))
                .map(String::trim)
                .filter(hepyS -> !hepyS.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return new HepyMaterial(
                hepyRs.getLong("id"),
                hepyRs.getString("code"),
                hepyRs.getString("name"),
                hepyRs.getString("composition"),
                hepyRs.getString("usage"),
                HepyMaterial.HepyScope.valueOf(hepyRs.getString("scope")),
                hepySuitable,
                hepyRs.getLong("surcharge_cents"),
                hepyRs.getDouble("roughness"),
                hepyRs.getDouble("sheen"),
                hepyRs.getDouble("metalness"),
                HepyMaterial.HepyPattern.valueOf(hepyRs.getString("preview_pattern")));
    }
}
