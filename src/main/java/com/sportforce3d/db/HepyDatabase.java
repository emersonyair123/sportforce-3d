package com.sportforce3d.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public final class HepyDatabase {

    private static final Logger hepyLog = LoggerFactory.getLogger(HepyDatabase.class);

    private final Path hepyFile;
    private final SQLiteDataSource hepyDataSource;

    public HepyDatabase(Path hepyFile) {
        this.hepyFile = hepyFile.toAbsolutePath().normalize();
        SQLiteConfig hepyConfig = new SQLiteConfig();
        hepyConfig.enforceForeignKeys(true);
        hepyConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
        hepyConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        hepyConfig.setBusyTimeout(5_000);
        this.hepyDataSource = new SQLiteDataSource(hepyConfig);
        this.hepyDataSource.setUrl("jdbc:sqlite:" + this.hepyFile);
    }

    public Path hepyFile() {
        return hepyFile;
    }

    public int hepyMigrate() {
        try {
            Path hepyParent = hepyFile.getParent();
            if (hepyParent != null) {
                Files.createDirectories(hepyParent);
            }
        } catch (IOException hepyE) {
            throw new UncheckedIOException("No se pudo crear la carpeta de la base de datos: " + hepyFile.getParent(), hepyE);
        }
        Flyway hepyFlyway = Flyway.configure()
                .dataSource(hepyDataSource)
                .locations("classpath:db/migration")
                .load();
        int hepyExecuted;
        try {
            hepyExecuted = hepyFlyway.migrate().migrationsExecuted;
        } catch (FlywayValidateException hepyE) {
            hepyLog.warn("El historial de migraciones no coincide con los scripts actuales; se repara: {}", hepyE.getMessage());
            hepyFlyway.repair();
            hepyExecuted = hepyFlyway.migrate().migrationsExecuted;
        }
        hepyLog.info("Base de datos lista en {} ({} migraciones aplicadas)", hepyFile, hepyExecuted);
        return hepyExecuted;
    }

    public Connection hepyGetConnection() throws SQLException {
        return hepyDataSource.getConnection();
    }

    public <R> R hepyWithConnection(HepySqlFunction<Connection, R> hepyWork) {
        try (Connection hepyConnection = hepyGetConnection()) {
            return hepyWork.hepyApply(hepyConnection);
        } catch (SQLException hepyE) {
            throw new HepyDataAccessException("Error al leer la base de datos", hepyE);
        }
    }

    public <R> R hepyInTransaction(HepySqlFunction<Connection, R> hepyWork) {
        try (Connection hepyConnection = hepyGetConnection()) {
            hepyConnection.setAutoCommit(false);
            try {
                R hepyResult = hepyWork.hepyApply(hepyConnection);
                hepyConnection.commit();
                return hepyResult;
            } catch (SQLException | RuntimeException hepyE) {
                hepyConnection.rollback();
                throw hepyE;
            }
        } catch (SQLException hepyE) {
            throw new HepyDataAccessException("Error al guardar en la base de datos", hepyE);
        }
    }

    public static void hepyDeleteFiles(Path hepyFile) {
        try {
            for (String hepySuffix : new String[]{"", "-wal", "-shm"}) {
                Files.deleteIfExists(Path.of(hepyFile.toAbsolutePath() + hepySuffix));
            }
        } catch (IOException hepyE) {
            throw new UncheckedIOException("No se pudo borrar " + hepyFile + ". ¿La aplicación sigue abierta?", hepyE);
        }
    }
}
