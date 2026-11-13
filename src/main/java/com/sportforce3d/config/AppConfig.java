package com.sportforce3d.config;

import java.nio.file.Path;
import java.util.Map;

public record AppConfig(Path databaseFile) {

    public static final String DB_PROPERTY = "sportforce.db";
    public static final String DB_ENV = "SPORTFORCE_DB";
    public static final Path DEFAULT_DB = Path.of("data", "sportforce3d.db");
    public static final String JPACKAGE_PROPERTY = "jpackage.app-path";
    public static final String INSTALLED_FOLDER = "SportForce3D";

    public Path storageDir() {
        Path parent = databaseFile.getParent();
        return (parent == null ? Path.of(".") : parent).resolve("storage");
    }

    public Path logDir() {
        Path parent = databaseFile.getParent();
        return (parent == null ? Path.of(".") : parent).resolve("logs");
    }

    public static AppConfig load() {
        return resolve(System.getProperty(DB_PROPERTY), System.getenv(), System.getProperty(JPACKAGE_PROPERTY) != null);
    }

    static AppConfig resolve(String property, Map<String, String> env) {
        return resolve(property, env, false);
    }

    static AppConfig resolve(String property, Map<String, String> env, boolean installed) {
        String configured = notBlank(property) ? property : env.get(DB_ENV);
        Path file;
        if (notBlank(configured)) {
            file = Path.of(configured.trim());
        } else if (installed) {
            String appData = env.get("APPDATA");
            Path base = notBlank(appData) ? Path.of(appData) : Path.of(System.getProperty("user.home"), "AppData", "Roaming");
            file = base.resolve(INSTALLED_FOLDER).resolve(DEFAULT_DB);
        } else {
            file = DEFAULT_DB;
        }
        return new AppConfig(file.toAbsolutePath().normalize());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
