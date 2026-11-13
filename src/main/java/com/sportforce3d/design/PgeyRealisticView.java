package com.sportforce3d.design;

import java.util.List;
import java.util.Map;

public record PgeyRealisticView(PgeyRealisticSpec pgeySpec, PgeyText pgeyName, PgeyText pgeyBack, PgeyText pgeyChest, PgeyDesignElement pgeyCrest,
                            List<String> pgeyNotices) {
    public record PgeyText(String pgeyValue, String pgeyFill, String pgeyOutline, String pgeyFont) {
    }

    public PgeyRealisticView {
        pgeyNotices = List.copyOf(pgeyNotices);
    }

    public String pgeyModelKey() {
        return pgeySpec.pgeyModelKey();
    }

    public Map<String, String> pgeyZoneColors() {
        return pgeySpec.pgeyZoneColors();
    }
}
