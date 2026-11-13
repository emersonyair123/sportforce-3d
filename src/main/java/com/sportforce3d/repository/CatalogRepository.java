package com.sportforce3d.repository;

import com.sportforce3d.db.Database;
import com.sportforce3d.model.Category;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.Size;
import com.sportforce3d.model.Sport;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

public class CatalogRepository {

    private final Database db;

    public CatalogRepository(Database db) {
        this.db = db;
    }

    public List<Category> findActiveCategories() {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, slug, name, description FROM categories WHERE active = 1 ORDER BY sort_order, name");
                 ResultSet rs = ps.executeQuery()) {
                List<Category> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Category(rs.getLong("id"), rs.getString("slug"), rs.getString("name"),
                            rs.getString("description")));
                }
                return list;
            }
        });
    }

    public List<Sport> findSports() {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT id, slug, name FROM sports ORDER BY sort_order, name");
                 ResultSet rs = ps.executeQuery()) {
                List<Sport> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Sport(rs.getLong("id"), rs.getString("slug"), rs.getString("name")));
                }
                return list;
            }
        });
    }

    public List<Size> findSizes() {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, code, name, size_group FROM sizes ORDER BY sort_order");
                 ResultSet rs = ps.executeQuery()) {
                List<Size> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Size(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                            rs.getString("size_group")));
                }
                return list;
            }
        });
    }

    public List<Material> findMaterials(Material.Scope scope) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM materials WHERE active = 1 AND scope = ? ORDER BY sort_order")) {
                ps.setString(1, scope.name());
                try (ResultSet rs = ps.executeQuery()) {
                    List<Material> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(mapMaterial(rs));
                    }
                    return list;
                }
            }
        });
    }

    public OptionalLong findLongSetting(String key) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT value FROM store_settings WHERE key = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return OptionalLong.empty();
                    }
                    try {
                        return OptionalLong.of(Long.parseLong(rs.getString(1).trim()));
                    } catch (NumberFormatException e) {
                        return OptionalLong.empty();
                    }
                }
            }
        });
    }

    static Material mapMaterial(ResultSet rs) throws SQLException {
        Set<String> suitable = Arrays.stream(rs.getString("suitable_for").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return new Material(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("composition"),
                rs.getString("usage"),
                Material.Scope.valueOf(rs.getString("scope")),
                suitable,
                rs.getLong("surcharge_cents"),
                rs.getDouble("roughness"),
                rs.getDouble("sheen"),
                rs.getDouble("metalness"),
                Material.Pattern.valueOf(rs.getString("preview_pattern")));
    }
}
