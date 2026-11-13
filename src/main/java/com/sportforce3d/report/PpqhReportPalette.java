package com.sportforce3d.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PpqhReportPalette {

    private static final Pattern PPQH_TOKEN = Pattern.compile("-sf-([a-z0-9-]+):\\s*#([0-9a-fA-F]{6})\\s*;");
    private static final Map<String, Integer> PPQH_LIGHT = ppqhLoad("/css/tokens-light.css");

    private PpqhReportPalette() {
    }

    public static int ppqhRgb(String ppqhToken) {
        return Objects.requireNonNull(PPQH_LIGHT.get(ppqhToken), "Token sin color HEX en tokens-light.css: " + ppqhToken);
    }

    public static String ppqhHex(String ppqhToken) {
        return String.format("%06X", ppqhRgb(ppqhToken));
    }

    static Map<String, Integer> ppqhLoad(String ppqhResource) {
        try (InputStream ppqhIn = PpqhReportPalette.class.getResourceAsStream(ppqhResource)) {
            if (ppqhIn == null) {
                throw new IllegalStateException("No existe " + ppqhResource);
            }
            String ppqhCss = new String(ppqhIn.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Integer> ppqhMap = new HashMap<>();
            Matcher ppqhM = PPQH_TOKEN.matcher(ppqhCss);
            while (ppqhM.find()) {
                ppqhMap.put(ppqhM.group(1), Integer.parseInt(ppqhM.group(2), 16));
            }
            return Map.copyOf(ppqhMap);
        } catch (IOException ppqhE) {
            throw new UncheckedIOException(ppqhE);
        }
    }
}
