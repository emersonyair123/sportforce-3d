package com.sportforce3d.component;

import com.sportforce3d.model.Material;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/** Muestra cuadrada de una tela dibujada por código sobre el color de la prenda. */
public class MaterialSample extends Canvas {

    private final Material.Pattern pattern;

    public MaterialSample(Material.Pattern pattern, Color base, double size) {
        super(size, size);
        this.pattern = pattern;
        getStyleClass().add("sf-mat-sample");
        paint(base);
    }

    public void paint(Color base) {
        GraphicsContext g = getGraphicsContext2D();
        double s = getWidth();
        g.clearRect(0, 0, s, s);
        g.save();
        // Esquinas de 6 px (radius-sm).
        g.beginPath();
        g.moveTo(6, 0);
        g.arcTo(s, 0, s, s, 6);
        g.arcTo(s, s, 0, s, 6);
        g.arcTo(0, s, 0, 0, 6);
        g.arcTo(0, 0, s, 0, 6);
        g.closePath();
        g.clip();

        g.setFill(base);
        g.fillRect(0, 0, s, s);
        Color light = base.deriveColor(0, 1, 1.35, 0.55);
        Color dark = base.deriveColor(0, 1, 0.6, 0.55);
        g.setLineCap(StrokeLineCap.ROUND);

        switch (pattern) {
            case DOTS -> dots(g, s, 5, 1.0, dark);
            case MESH -> dots(g, s, 7, 2.2, dark);
            case DIAGONAL -> {
                g.setStroke(light);
                g.setLineWidth(1.5);
                for (double x = -s; x < s; x += 6) {
                    g.strokeLine(x, s, x + s, 0);
                }
            }
            case LINES -> {
                g.setStroke(light);
                g.setLineWidth(1);
                for (double y = 3; y < s; y += 4) {
                    g.strokeLine(0, y, s, y);
                }
            }
            case GRID -> {
                g.setStroke(dark);
                g.setLineWidth(0.8);
                for (double v = 3; v < s; v += 5) {
                    g.strokeLine(0, v, s, v);
                    g.strokeLine(v, 0, v, s);
                }
            }
            case KNIT -> {
                g.setStroke(light);
                g.setLineWidth(1.2);
                for (double y = 2; y < s; y += 5) {
                    for (double x = 0; x < s; x += 6) {
                        g.strokeLine(x, y, x + 3, y + 3);
                        g.strokeLine(x + 3, y + 3, x + 6, y);
                    }
                }
            }
            case GRAIN -> {
                g.setFill(dark);
                long seed = 7;
                for (int i = 0; i < 90; i++) {
                    seed = seed * 6364136223846793005L + 1442695040888963407L;
                    double x = ((seed >>> 33) % 1000) / 1000.0 * s;
                    double y = ((seed >>> 17) % 1000) / 1000.0 * s;
                    g.fillOval(x, y, 1.4, 1.4);
                }
            }
            case RUBBER -> {
                g.setStroke(dark);
                g.setLineWidth(2);
                for (double y = 4; y < s; y += 6) {
                    for (double x = 0; x < s; x += 8) {
                        g.strokeLine(x, y, x + 4, y + 3);
                    }
                }
            }
            case STUDS -> {
                g.setFill(dark);
                for (double y = 6; y < s; y += 12) {
                    for (double x = 6; x < s; x += 12) {
                        g.fillOval(x - 3, y - 3, 6, 6);
                    }
                }
            }
            case REFLECTIVE -> {
                g.setStroke(Color.color(1, 1, 1, 0.55));
                g.setLineWidth(3);
                g.strokeLine(0, s * 0.75, s, s * 0.25);
                g.setLineWidth(1);
                g.strokeLine(0, s, s, s * 0.5);
            }
            case SMOOTH -> {
                g.setStroke(light);
                g.setLineWidth(4);
                g.strokeLine(s * 0.15, s * 0.35, s * 0.55, s * 0.15);
            }
            case PLAIN -> {
                // Algodón: punto mate de hilos finos (sin brillo).
                g.setStroke(dark);
                g.setLineWidth(0.7);
                for (double y = 1.5; y < s; y += 3) {
                    for (double x = 0; x < s; x += 4) {
                        g.strokeLine(x, y, x + 2, y + 1.5);
                        g.strokeLine(x + 2, y + 1.5, x + 4, y);
                    }
                }
            }
        }
        g.restore();
    }

    private static void dots(GraphicsContext g, double s, double step, double r, Color color) {
        g.setFill(color);
        for (double y = step / 2; y < s; y += step) {
            for (double x = step / 2; x < s; x += step) {
                g.fillOval(x - r, y - r, r * 2, r * 2);
            }
        }
    }
}
