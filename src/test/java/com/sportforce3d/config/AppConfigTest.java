package com.sportforce3d.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    @Test
    void defaultsToDataFolderInWorkingDirectory() {
        PpqhAppConfig config = PpqhAppConfig.ppqhResolve(null, Map.of());
        assertThat(config.ppqhDatabaseFile()).isEqualTo(Path.of("data", "sportforce3d.db").toAbsolutePath().normalize());
    }

    @Test
    void systemPropertyWinsOverEnvironment() {
        PpqhAppConfig config = PpqhAppConfig.ppqhResolve("C:/tmp/prop.db", Map.of(PpqhAppConfig.PPQH_DB_ENV, "C:/tmp/env.db"));
        assertThat(config.ppqhDatabaseFile()).isEqualTo(Path.of("C:/tmp/prop.db").toAbsolutePath().normalize());
    }

    @Test
    void installedAppUsesAppDataBecauseProgramFilesIsReadOnly() {
        PpqhAppConfig config = PpqhAppConfig.ppqhResolve(null, Map.of("APPDATA", "C:/Users/ana/AppData/Roaming"), true);
        assertThat(config.ppqhDatabaseFile()).isEqualTo(
                Path.of("C:/Users/ana/AppData/Roaming/SportForce3D/data/sportforce3d.db").toAbsolutePath().normalize());
        assertThat(config.ppqhStorageDir()).endsWithRaw(Path.of("SportForce3D", "data", "storage"));
        assertThat(config.ppqhLogDir()).endsWithRaw(Path.of("SportForce3D", "data", "logs"));

        assertThat(PpqhAppConfig.ppqhResolve(null, Map.of("APPDATA", "C:/x", PpqhAppConfig.PPQH_DB_ENV, "D:/tienda.db"), true)
                .ppqhDatabaseFile()).isEqualTo(Path.of("D:/tienda.db").toAbsolutePath().normalize());
    }

    @Test
    void environmentVariableIsUsedWhenNoProperty() {
        PpqhAppConfig config = PpqhAppConfig.ppqhResolve(" ", Map.of(PpqhAppConfig.PPQH_DB_ENV, "otra/carpeta/tienda.db"));
        assertThat(config.ppqhDatabaseFile()).isEqualTo(Path.of("otra/carpeta/tienda.db").toAbsolutePath().normalize());
    }
}
