package com.sportforce3d.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

final class Jdbc {

    private Jdbc() {
    }

    static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);
            int index = i + 1;
            if (value == null) {
                ps.setNull(index, Types.NULL);
            } else if (value instanceof Long l) {
                ps.setLong(index, l);
            } else if (value instanceof Integer n) {
                ps.setInt(index, n);
            } else if (value instanceof Boolean b) {
                ps.setInt(index, b ? 1 : 0);
            } else {
                ps.setString(index, value.toString());
            }
        }
    }

    static Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    static String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }
}
