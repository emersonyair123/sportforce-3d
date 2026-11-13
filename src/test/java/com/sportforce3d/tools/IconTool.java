package com.sportforce3d.tools;

import com.sportforce3d.app.Fonts;
import com.sportforce3d.component.Logo;
import com.sportforce3d.three.Png;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public final class IconTool {

    private static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};

    private IconTool() {
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "src/packaging/windows");
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        try {
            List<byte[]> pngs = new ArrayList<>();
            Path tmp = Files.createTempDirectory("sf-icon");
            for (int size : SIZES) {
                CompletableFuture<Image> img = new CompletableFuture<>();
                Platform.runLater(() -> {
                    Fonts.loadAll();
                    img.complete(Logo.monogram(size));
                });
                Path png = tmp.resolve(size + ".png");
                Png.write(img.get(), png);
                pngs.add(Files.readAllBytes(png));
                if (size == 256) {
                    Files.createDirectories(out);
                    Files.copy(png, out.resolve("sportforce3d-256.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Files.write(out.resolve("sportforce3d.ico"), ico(pngs));
            System.out.println("Icono: " + out.resolve("sportforce3d.ico").toAbsolutePath());
        } finally {
            Platform.exit();
        }
    }

    static byte[] ico(List<byte[]> pngs) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(6 + 16 * pngs.size()).order(ByteOrder.LITTLE_ENDIAN);
        header.putShort((short) 0).putShort((short) 1).putShort((short) pngs.size());
        int offset = header.capacity();
        for (int i = 0; i < pngs.size(); i++) {
            int size = SIZES[i];
            header.put((byte) (size >= 256 ? 0 : size)).put((byte) (size >= 256 ? 0 : size));
            header.put((byte) 0).put((byte) 0);
            header.putShort((short) 1).putShort((short) 32);
            header.putInt(pngs.get(i).length).putInt(offset);
            offset += pngs.get(i).length;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header.array());
        for (byte[] png : pngs) {
            out.write(png);
        }
        return out.toByteArray();
    }
}
