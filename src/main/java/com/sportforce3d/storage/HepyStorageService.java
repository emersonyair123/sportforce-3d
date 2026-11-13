package com.sportforce3d.storage;

import com.sportforce3d.service.PpqhValidationException;
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

public class HepyStorageService {

    private static final Logger hepyLog = LoggerFactory.getLogger(HepyStorageService.class);

    public static final long HEPY_MAX_BYTES = 5L * 1024 * 1024;

    public enum HepyKind {
        PNG("image/png", ".png"),
        JPEG("image/jpeg", ".jpg"),
        SVG("image/svg+xml", ".svg"),
        PDF("application/pdf", ".pdf");

        private final String hepyMimeType;
        private final String hepyExtension;

        HepyKind(String hepyMimeType, String hepyExtension) {
            this.hepyMimeType = hepyMimeType;
            this.hepyExtension = hepyExtension;
        }

        public String hepyMimeType() {
            return hepyMimeType;
        }
    }

    public record HepyStoredFile(String hepyPath, HepyKind hepyKind, long hepySizeBytes) {
    }

    public static final Set<HepyKind> HEPY_IMAGES = Set.of(HepyKind.PNG, HepyKind.JPEG);

    private final Path hepyRoot;

    public HepyStorageService(Path hepyRoot) {
        this.hepyRoot = hepyRoot.toAbsolutePath().normalize();
    }

    public Path hepyRoot() {
        return hepyRoot;
    }

    public HepyStoredFile hepyStore(Path hepySource, String hepyFolder, Set<HepyKind> hepyAllowed) {
        if (!hepyFolder.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException("Carpeta inválida: " + hepyFolder);
        }
        try {
            if (!Files.isRegularFile(hepySource)) {
                throw PpqhValidationException.ppqhOf("file", "No se encontró el archivo.");
            }
            long hepySize = Files.size(hepySource);
            if (hepySize == 0) {
                throw PpqhValidationException.ppqhOf("file", "El archivo está vacío.");
            }
            if (hepySize > HEPY_MAX_BYTES) {
                throw PpqhValidationException.ppqhOf("file", "El archivo pesa más de 5 MB.");
            }
            HepyKind hepyKind = hepyDetect(hepySource);
            if (hepyKind == null || !hepyAllowed.contains(hepyKind)) {
                throw PpqhValidationException.ppqhOf("file", "Formato no permitido. Usa " + hepyDescribe(hepyAllowed) + ".");
            }
            Path hepyDir = hepyRoot.resolve(hepyFolder);
            Files.createDirectories(hepyDir);
            String hepyName = UUID.randomUUID().toString().replace("-", "") + hepyKind.hepyExtension;
            Files.copy(hepySource, hepyDir.resolve(hepyName), StandardCopyOption.COPY_ATTRIBUTES);
            String hepyRelative = hepyFolder + "/" + hepyName;
            hepyLog.info("Archivo guardado: {} ({} bytes)", hepyRelative, hepySize);
            return new HepyStoredFile(hepyRelative, hepyKind, hepySize);
        } catch (IOException hepyE) {
            throw new UncheckedIOException("No se pudo guardar el archivo", hepyE);
        }
    }

    public Path hepyResolve(String hepyRelativePath) {
        Path hepyResolved = hepyRoot.resolve(hepyRelativePath).normalize();
        if (!hepyResolved.startsWith(hepyRoot)) {
            throw new IllegalArgumentException("Ruta fuera del almacenamiento: " + hepyRelativePath);
        }
        return hepyResolved;
    }

    public void hepyDelete(String hepyRelativePath) {
        try {
            Files.deleteIfExists(hepyResolve(hepyRelativePath));
        } catch (IOException hepyE) {
            hepyLog.warn("No se pudo borrar {}", hepyRelativePath, hepyE);
        }
    }

    static HepyKind hepyDetect(Path hepyFile) throws IOException {
        byte[] hepyHead = new byte[512];
        int hepyRead;
        try (InputStream hepyIn = Files.newInputStream(hepyFile)) {
            hepyRead = hepyIn.readNBytes(hepyHead, 0, hepyHead.length);
        }
        byte[] hepyH = Arrays.copyOf(hepyHead, hepyRead);
        if (hepyStartsWith(hepyH, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return HepyKind.PNG;
        }
        if (hepyStartsWith(hepyH, 0xFF, 0xD8, 0xFF)) {
            return HepyKind.JPEG;
        }
        if (hepyStartsWith(hepyH, '%', 'P', 'D', 'F', '-')) {
            return HepyKind.PDF;
        }
        String hepyText = new String(hepyH, java.nio.charset.StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        if (hepyText.contains("<svg")) {
            return HepyKind.SVG;
        }
        return null;
    }

    private static boolean hepyStartsWith(byte[] hepyData, int... hepySignature) {
        if (hepyData.length < hepySignature.length) {
            return false;
        }
        for (int hepyI = 0; hepyI < hepySignature.length; hepyI++) {
            if ((hepyData[hepyI] & 0xFF) != hepySignature[hepyI]) {
                return false;
            }
        }
        return true;
    }

    private static String hepyDescribe(Set<HepyKind> hepyKinds) {
        return hepyKinds.stream().map(hepyK -> hepyK.name()).sorted().reduce((hepyA, hepyB) -> hepyA + " o " + hepyB).orElse("");
    }
}
