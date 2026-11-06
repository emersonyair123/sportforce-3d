package com.sportforce3d.db;

import org.flywaydb.core.Flyway;
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

/**
 * Archivo SQLite local con claves foráneas activas y migraciones Flyway.
 * Cada operación abre su propia conexión: en SQLite es barato y evita compartir conexiones entre hilos.
 */
public final class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);

    private final Path file;
    private final SQLiteDataSource dataSource;

    public Database(Path file) {
        this.file = file.toAbsolutePath().normalize();
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.setBusyTimeout(5_000);
        this.dataSource = new SQLiteDataSource(config);
        this.dataSource.setUrl("jdbc:sqlite:" + this.file);
    }

    public Path file() {
        return file;
    }

    /** Crea la carpeta si hace falta y aplica las migraciones pendientes. Devuelve cuántas se ejecutaron. */
    public int migrate() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de la base de datos: " + file.getParent(), e);
        }
        int executed = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate()
                .migrationsExecuted;
        log.info("Base de datos lista en {} ({} migraciones aplicadas)", file, executed);
        return executed;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Ejecuta {@code work} con una conexión de solo una operación. */
    public <R> R withConnection(SqlFunction<Connection, R> work) {
        try (Connection connection = getConnection()) {
            return work.apply(connection);
        } catch (SQLException e) {
            throw new DataAccessException("Error al leer la base de datos", e);
        }
    }

    /** Ejecuta {@code work} dentro de una transacción; si lanza una excepción se revierte todo. */
    public <R> R inTransaction(SqlFunction<Connection, R> work) {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                R result = work.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al guardar en la base de datos", e);
        }
    }

    /** Borra el archivo .db y sus archivos -wal / -shm. La siguiente llamada a {@link #migrate()} lo recrea con datos de prueba. */
    public static void deleteFiles(Path file) {
        try {
            for (String suffix : new String[]{"", "-wal", "-shm"}) {
                Files.deleteIfExists(Path.of(file.toAbsolutePath() + suffix));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo borrar " + file + ". ¿La aplicación sigue abierta?", e);
        }
    }
}
