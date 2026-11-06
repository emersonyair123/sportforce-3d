package com.sportforce3d.db;

import java.nio.file.Path;

/** Base de datos SQLite en una carpeta temporal, migrada con V1 (esquema) y V2 (datos de prueba). */
public final class TestDatabases {

    private TestDatabases() {
    }

    public static Database migrated(Path tempDir) {
        Database db = new Database(tempDir.resolve("data").resolve("test.db"));
        db.migrate();
        return db;
    }
}
