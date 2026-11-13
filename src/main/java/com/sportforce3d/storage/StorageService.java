package com.sportforce3d.storage;

import com.sportforce3d.service.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    public enum Kind {
        PNG("image/png", ".png"),
        JPEG("image/jpeg", ".jpg"),
        SVG("image/svg+xml", ".svg"),
        PDF("application/pdf", ".pdf");

        private final String mimeType;
        private final String extension;

        Kind(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }

        public String mimeType() {
            return mimeType;
        }
    }

    public record StoredFile(String path, Kind kind, long sizeBytes) {
    }

    public static final Set<Kind> IMAGES = Set.of(Kind.PNG, Kind.JPEG);

    private final Path root;

    public StorageService(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public StoredFile store(Path source, String folder, Set<Kind> allowed) {
        if (!folder.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException("Carpeta inválida: " + folder);
        }
        try {
            if (!Files.isRegularFile(source)) {
                throw ValidationException.of("file", "No se encontró el archivo.");
            }
            long size = Files.size(source);
            if (size == 0) {
                throw ValidationException.of("file", "El archivo está vacío.");
            }
            if (size > MAX_BYTES) {
                throw ValidationException.of("file", "El archivo pesa más de 5 MB.");
            }
            Kind kind = detect(source);
            if (kind == null || !allowed.contains(kind)) {
                throw ValidationException.of("file", "Formato no permitido. Usa " + describe(allowed) + ".");
            }
            Path dir = root.resolve(folder);
            Files.createDirectories(dir);
            String name = UUID.randomUUID().toString().replace("-", "") + kind.extension;
            Files.copy(source, dir.resolve(name), StandardCopyOption.COPY_ATTRIBUTES);
            String relative = folder + "/" + name;
            log.info("Archivo guardado: {} ({} bytes)", relative, size);
            return new StoredFile(relative, kind, size);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
    }

    public Path resolve(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Ruta fuera del almacenamiento: " + relativePath);
        }
        return resolved;
    }

    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            log.warn("No se pudo borrar {}", relativePath, e);
        }
    }

    static Kind detect(Path file) throws IOException {
        byte[] head = new byte[512];
        int read;
        try (InputStream in = Files.newInputStream(file)) {
            read = in.readNBytes(head, 0, head.length);
        }
        byte[] h = Arrays.copyOf(head, read);
        if (startsWith(h, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Kind.PNG;
        }
        if (startsWith(h, 0xFF, 0xD8, 0xFF)) {
            return Kind.JPEG;
        }
        if (startsWith(h, '%', 'P', 'D', 'F', '-')) {
            return Kind.PDF;
        }
        String text = new String(h, java.nio.charset.StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        if (text.contains("<svg")) {
            return Kind.SVG;
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int... signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static String describe(Set<Kind> kinds) {
        return kinds.stream().map(k -> k.name()).sorted().reduce((a, b) -> a + " o " + b).orElse("");
    }
}
