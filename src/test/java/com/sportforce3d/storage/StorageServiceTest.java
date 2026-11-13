package com.sportforce3d.storage;

import com.sportforce3d.service.PpqhValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsTypeByContentNotByExtension() throws Exception {
        Path jpeg = tempDir.resolve("foto.png");
        Files.write(jpeg, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0});
        Path svg = tempDir.resolve("escudo.svg");
        Files.writeString(svg, "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"></svg>");
        Path pdf = tempDir.resolve("voucher.pdf");
        Files.writeString(pdf, "%PDF-1.7 ...");

        assertThat(HepyStorageService.hepyDetect(jpeg)).isEqualTo(HepyStorageService.HepyKind.JPEG);
        assertThat(HepyStorageService.hepyDetect(svg)).isEqualTo(HepyStorageService.HepyKind.SVG);
        assertThat(HepyStorageService.hepyDetect(pdf)).isEqualTo(HepyStorageService.HepyKind.PDF);

        HepyStorageService storage = new HepyStorageService(tempDir.resolve("storage"));
        HepyStorageService.HepyStoredFile stored = storage.hepyStore(jpeg, "products", HepyStorageService.HEPY_IMAGES);
        assertThat(stored.hepyPath()).endsWith(".jpg");
        assertThatThrownBy(() -> storage.hepyStore(pdf, "products", HepyStorageService.HEPY_IMAGES)).isInstanceOf(PpqhValidationException.class);
    }

    @Test
    void rejectsFilesOverFiveMegabytes() throws Exception {
        Path big = tempDir.resolve("grande.png");
        Files.write(big, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        try (RandomAccessFile raf = new RandomAccessFile(big.toFile(), "rw")) {
            raf.setLength(HepyStorageService.HEPY_MAX_BYTES + 1);
        }
        HepyStorageService storage = new HepyStorageService(tempDir.resolve("storage"));
        assertThatThrownBy(() -> storage.hepyStore(big, "products", Set.of(HepyStorageService.HepyKind.PNG)))
                .hasMessageContaining("5 MB");
    }

    @Test
    void refusesPathsOutsideTheStorageFolder() {
        HepyStorageService storage = new HepyStorageService(tempDir.resolve("storage"));
        assertThatThrownBy(() -> storage.hepyResolve("../../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.hepyStore(tempDir.resolve("x"), "../fuera", HepyStorageService.HEPY_IMAGES))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
