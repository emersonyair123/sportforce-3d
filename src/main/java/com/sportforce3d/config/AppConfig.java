package com.sportforce3d.config;

import java.nio.file.Path;
import java.util.Map;

/**
 * Configuración de arranque. La ruta de la base de datos se resuelve en este orden:
 * <ol>
 *     <li>propiedad del sistema {@code -Dsportforce.db=...}</li>
 *     <li>variable de entorno {@code SPORTFORCE_DB}</li>
 *     <li>instalada con el instalador de Windows: {@code %APPDATA%\SportForce3D\data\sportforce3d.db}
 *     (la carpeta de Archivos de programa no se puede escribir)</li>
 *     <li>{@code data/sportforce3d.db} relativo a la carpeta de trabajo</li>
 * </ol>
 */
public record AppConfig(Path databaseFile) {

    public static final String DB_PROPERTY = "sportforce.db";
    public static final String DB_ENV = "SPORTFORCE_DB";
    public static final Path DEFAULT_DB = Path.of("data", "sportforce3d.db");
    /** El lanzador de jpackage define esta propiedad: la aplicación corre desde el instalador. */
    public static final String JPACKAGE_PROPERTY = "jpackage.app-path";
    public static final String INSTALLED_FOLDER = "SportForce3D";

    /** Archivos subidos (imágenes de producto, logos, comprobantes): carpeta "storage" junto al .db. */
    public Path storageDir() {
        Path parent = databaseFile.getParent();
        return (parent == null ? Path.of(".") : parent).resolve("storage");
    }

    /** Registros de la aplicación: carpeta "logs" junto al .db. */
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