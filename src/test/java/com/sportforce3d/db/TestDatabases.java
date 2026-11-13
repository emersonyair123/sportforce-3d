package com.sportforce3d.db;

import java.nio.file.Path;

public final class TestDatabases {

    private TestDatabases() {
    }

    public static Database migrated(Path tempDir) {
        Database db = new Database(tempDir.resolve("data").resolve("test.db"));
        db.migrate();
        return db;
    }
}
