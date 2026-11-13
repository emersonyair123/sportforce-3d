package com.sportforce3d.db;

import java.nio.file.Path;

public final class TestDatabases {

    private TestDatabases() {
    }

    public static HepyDatabase migrated(Path tempDir) {
        HepyDatabase db = new HepyDatabase(tempDir.resolve("data").resolve("test.db"));
        db.hepyMigrate();
        return db;
    }
}
