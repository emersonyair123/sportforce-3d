package com.sportforce3d.design;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class PgeyDraftStore {

    private static final Logger pgeyLog = LoggerFactory.getLogger(PgeyDraftStore.class);
    private static final ObjectMapper PGEY_MAPPER = new ObjectMapper();

    public record PgeyDraft(@JsonProperty("designId") Long pgeyDesignId, @JsonProperty("name") String pgeyName, @JsonProperty("documentJson") String pgeyDocumentJson, @JsonProperty("savedAt") String pgeySavedAt) {
    }

    private final Path pgeyDir;

    public PgeyDraftStore(Path pgeyDir) {
        this.pgeyDir = pgeyDir;
    }

    public synchronized void pgeySave(long pgeyUserId, PgeyDraft pgeyDraft) {
        try {
            Files.createDirectories(pgeyDir);
            Path pgeyTmp = pgeyDir.resolve("user-" + pgeyUserId + ".json.tmp");
            PGEY_MAPPER.writeValue(pgeyTmp.toFile(), pgeyDraft);
            Files.move(pgeyTmp, pgeyFile(pgeyUserId), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException pgeyE) {
            pgeyLog.warn("No se pudo guardar el borrador del usuario {}", pgeyUserId, pgeyE);
        }
    }

    public synchronized Optional<PgeyDraft> pgeyLoad(long pgeyUserId) {
        Path pgeyF = pgeyFile(pgeyUserId);
        if (!Files.isRegularFile(pgeyF)) {
            return Optional.empty();
        }
        try {
            return Optional.of(PGEY_MAPPER.readValue(pgeyF.toFile(), PgeyDraft.class));
        } catch (IOException pgeyE) {
            pgeyLog.warn("Borrador ilegible, se descarta: {}", pgeyF, pgeyE);
            pgeyDelete(pgeyUserId);
            return Optional.empty();
        }
    }

    public synchronized void pgeyDelete(long pgeyUserId) {
        try {
            Files.deleteIfExists(pgeyFile(pgeyUserId));
        } catch (IOException pgeyE) {
            pgeyLog.warn("No se pudo borrar el borrador del usuario {}", pgeyUserId, pgeyE);
        }
    }

    private Path pgeyFile(long pgeyUserId) {
        return pgeyDir.resolve("user-" + pgeyUserId + ".json");
    }
}
