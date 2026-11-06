package com.sportforce3d.storage;

import com.sportforce3d.service.ValidationException;
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
        Path jpeg = tempDir.resolve("foto.png"); // extensión engañosa
        Files.write(jpeg, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0});
        Path svg = tempDir.resolve("escudo.svg");
        Files.writeString(svg, "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"></svg>");
        Path pdf = tempDir.resolve("voucher.pdf");
        Files.writeString(pdf, "%PDF-1.7 ...");

        assertThat(StorageService.detect(jpeg)).isEqualTo(StorageService.Kind.JPEG);
        assertThat(StorageService.detect(svg)).isEqualTo(StorageService.Kind.SVG);
        assertThat(StorageService.detect(pdf)).isEqualTo(StorageService.Kind.PDF);

        StorageService storage = new StorageService(tempDir.resolve("storage"));
        StorageService.StoredFile stored = storage.store(jpeg, "products", StorageService.IMAGES);
        assertThat(stored.path()).endsWith(".jpg");
        assertThatThrownBy(() -> storage.store(pdf, "products", StorageService.IMAGES)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsFilesOverFiveMegabytes() throws Exception {
        Path big = tempDir.resolve("grande.png");
        Files.write(big, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        try (RandomAccessFile raf = new RandomAccessFile(big.toFile(), "rw")) {
            raf.setLength(StorageService.MAX_BYTES + 1);
        }
        StorageService storage = new StorageService(tempDir.resolve("storage"));
        assertThatThrownBy(() -> storage.store(big, "products", Set.of(StorageService.Kind.PNG)))
                .hasMessageContaining("5 MB");
    }

    @Test
    void refusesPathsOutsideTheStorageFolder() {
        StorageService storage = new StorageService(tempDir.resolve("storage"));
        assertThatThrownBy(() -> storage.resolve("../../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.store(tempDir.resolve("x"), "../fuera", StorageService.IMAGES))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
