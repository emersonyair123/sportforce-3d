package com.sportforce3d.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    @Test
    void defaultsToDataFolderInWorkingDirectory() {
        AppConfig config = AppConfig.resolve(null, Map.of());
        assertThat(config.databaseFile()).isEqualTo(Path.of("data", "sportforce3d.db").toAbsolutePath().normalize());
    }

    @Test
    void systemPropertyWinsOverEnvironment() {
        AppConfig config = AppConfig.resolve("C:/tmp/prop.db", Map.of(AppConfig.DB_ENV, "C:/tmp/env.db"));
        assertThat(config.databaseFile()).isEqualTo(Path.of("C:/tmp/prop.db").toAbsolutePath().normalize());
    }

    @Test
    void installedAppUsesAppDataBecauseProgramFilesIsReadOnly() {
        AppConfig config = AppConfig.resolve(null, Map.of("APPDATA", "C:/Users/ana/AppData/Roaming"), true);
        assertThat(config.databaseFile()).isEqualTo(
                Path.of("C:/Users/ana/AppData/Roaming/SportForce3D/data/sportforce3d.db").toAbsolutePath().normalize());
        assertThat(config.storageDir()).endsWithRaw(Path.of("SportForce3D", "data", "storage"));
        assertThat(config.logDir()).endsWithRaw(Path.of("SportForce3D", "data", "logs"));

        // Una ruta explícita sigue mandando aunque esté instalada.
        assertThat(AppConfig.resolve(null, Map.of("APPDATA", "C:/x", AppConfig.DB_ENV, "D:/tienda.db"), true)
                .databaseFile()).isEqualTo(Path.of("D:/tienda.db").toAbsolutePath().normalize());
    }

    @Test
    void environmentVariableIsUsedWhenNoProperty() {
        AppConfig config = AppConfig.resolve(" ", Map.of(AppConfig.DB_ENV, "otra/carpeta/tienda.db"));
        assertThat(config.databaseFile()).isEqualTo(Path.of("otra/carpeta/tienda.db").toAbsolutePath().normalize());
    }
}
