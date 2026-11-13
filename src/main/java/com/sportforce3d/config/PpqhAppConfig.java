package com.sportforce3d.config;

import java.nio.file.Path;
import java.util.Map;

public record PpqhAppConfig(Path ppqhDatabaseFile) {

    public static final String PPQH_DB_PROPERTY = "sportforce.db";
    public static final String PPQH_DB_ENV = "SPORTFORCE_DB";
    public static final Path PPQH_DEFAULT_DB = Path.of("data", "sportforce3d.db");
    public static final String PPQH_JPACKAGE_PROPERTY = "jpackage.app-path";
    public static final String PPQH_INSTALLED_FOLDER = "SportForce3D";

    public Path ppqhStorageDir() {
        Path ppqhParent = ppqhDatabaseFile.getParent();
        return (ppqhParent == null ? Path.of(".") : ppqhParent).resolve("storage");
    }

    public Path ppqhLogDir() {
        Path ppqhParent = ppqhDatabaseFile.getParent();
        return (ppqhParent == null ? Path.of(".") : ppqhParent).resolve("logs");
    }

    public static PpqhAppConfig ppqhLoad() {
        return ppqhResolve(System.getProperty(PPQH_DB_PROPERTY), System.getenv(), System.getProperty(PPQH_JPACKAGE_PROPERTY) != null);
    }

    static PpqhAppConfig ppqhResolve(String ppqhProperty, Map<String, String> ppqhEnv) {
        return ppqhResolve(ppqhProperty, ppqhEnv, false);
    }

    static PpqhAppConfig ppqhResolve(String ppqhProperty, Map<String, String> ppqhEnv, boolean ppqhInstalled) {
        String ppqhConfigured = ppqhNotBlank(ppqhProperty) ? ppqhProperty : ppqhEnv.get(PPQH_DB_ENV);
        Path ppqhFile;
        if (ppqhNotBlank(ppqhConfigured)) {
            ppqhFile = Path.of(ppqhConfigured.trim());
        } else if (ppqhInstalled) {
            String ppqhAppData = ppqhEnv.get("APPDATA");
            Path ppqhBase = ppqhNotBlank(ppqhAppData) ? Path.of(ppqhAppData) : Path.of(System.getProperty("user.home"), "AppData", "Roaming");
            ppqhFile = ppqhBase.resolve(PPQH_INSTALLED_FOLDER).resolve(PPQH_DEFAULT_DB);
        } else {
            ppqhFile = PPQH_DEFAULT_DB;
        }
        return new PpqhAppConfig(ppqhFile.toAbsolutePath().normalize());
    }

    private static boolean ppqhNotBlank(String ppqhValue) {
        return ppqhValue != null && !ppqhValue.isBlank();
    }
}
