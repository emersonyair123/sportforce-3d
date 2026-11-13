package com.sportforce3d.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

final class HepyJdbc {

    private HepyJdbc() {
    }

    static void hepyBind(PreparedStatement hepyPs, List<Object> hepyParams) throws SQLException {
        for (int hepyI = 0; hepyI < hepyParams.size(); hepyI++) {
            Object hepyValue = hepyParams.get(hepyI);
            int hepyIndex = hepyI + 1;
            if (hepyValue == null) {
                hepyPs.setNull(hepyIndex, Types.NULL);
            } else if (hepyValue instanceof Long hepyL) {
                hepyPs.setLong(hepyIndex, hepyL);
            } else if (hepyValue instanceof Integer hepyN) {
                hepyPs.setInt(hepyIndex, hepyN);
            } else if (hepyValue instanceof Boolean hepyB) {
                hepyPs.setInt(hepyIndex, hepyB ? 1 : 0);
            } else {
                hepyPs.setString(hepyIndex, hepyValue.toString());
            }
        }
    }

    static Long hepyGetLongOrNull(ResultSet hepyRs, String hepyColumn) throws SQLException {
        long hepyValue = hepyRs.getLong(hepyColumn);
        return hepyRs.wasNull() ? null : hepyValue;
    }

    static String hepyPlaceholders(int hepyCount) {
        return String.join(", ", java.util.Collections.nCopies(hepyCount, "?"));
    }
}
