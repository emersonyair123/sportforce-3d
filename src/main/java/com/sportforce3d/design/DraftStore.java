package com.sportforce3d.design;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class DraftStore {

    private static final Logger log = LoggerFactory.getLogger(DraftStore.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record Draft(Long designId, String name, String documentJson, String savedAt) {
    }

    private final Path dir;

    public DraftStore(Path dir) {
        this.dir = dir;
    }

    public synchronized void save(long userId, Draft draft) {
        try {
            Files.createDirectories(dir);
            Path tmp = dir.resolve("user-" + userId + ".json.tmp");
            MAPPER.writeValue(tmp.toFile(), draft);
            Files.move(tmp, file(userId), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.warn("No se pudo guardar el borrador del usuario {}", userId, e);
        }
    }

    public synchronized Optional<Draft> load(long userId) {
        Path f = file(userId);
        if (!Files.isRegularFile(f)) {
            return Optional.empty();
        }
        try {
            return Optional.of(MAPPER.readValue(f.toFile(), Draft.class));
        } catch (IOException e) {
            log.warn("Borrador ilegible, se descarta: {}", f, e);
            delete(userId);
            return Optional.empty();
        }
    }

    public synchronized void delete(long userId) {
        try {
            Files.deleteIfExists(file(userId));
        } catch (IOException e) {
            log.warn("No se pudo borrar el borrador del usuario {}", userId, e);
        }
    }

    private Path file(long userId) {
        return dir.resolve("user-" + userId + ".json");
    }
}
