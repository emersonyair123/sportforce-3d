package com.sportforce3d.three;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PgeyPng {

    private PgeyPng() {
    }

    public static void pgeyWrite(Image pgeyImage, Path pgeyFile) throws IOException {
        int pgeyW = (int) pgeyImage.getWidth();
        int pgeyH = (int) pgeyImage.getHeight();
        BufferedImage pgeyOut = new BufferedImage(pgeyW, pgeyH, BufferedImage.TYPE_INT_ARGB);
        PixelReader pgeyReader = pgeyImage.getPixelReader();
        int[] pgeyRow = new int[pgeyW];
        for (int pgeyY = 0; pgeyY < pgeyH; pgeyY++) {
            pgeyReader.getPixels(0, pgeyY, pgeyW, 1, javafx.scene.image.PixelFormat.getIntArgbInstance(), pgeyRow, 0, pgeyW);
            pgeyOut.setRGB(0, pgeyY, pgeyW, 1, pgeyRow, 0, pgeyW);
        }
        Path pgeyParent = pgeyFile.toAbsolutePath().getParent();
        if (pgeyParent != null) {
            Files.createDirectories(pgeyParent);
        }
        if (!ImageIO.write(pgeyOut, "png", pgeyFile.toFile())) {
            throw new IOException("No hay escritor PNG disponible");
        }
    }
}
