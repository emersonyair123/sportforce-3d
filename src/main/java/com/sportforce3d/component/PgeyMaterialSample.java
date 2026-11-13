package com.sportforce3d.component;

import com.sportforce3d.model.HepyMaterial;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

public class PgeyMaterialSample extends Canvas {

    private final HepyMaterial.HepyPattern pgeyPattern;

    public PgeyMaterialSample(HepyMaterial.HepyPattern pgeyPattern, Color pgeyBase, double pgeySize) {
        super(pgeySize, pgeySize);
        this.pgeyPattern = pgeyPattern;
        getStyleClass().add("sf-mat-sample");
        pgeyPaint(pgeyBase);
    }

    public void pgeyPaint(Color pgeyBase) {
        GraphicsContext pgeyG = getGraphicsContext2D();
        double pgeyS = getWidth();
        pgeyG.clearRect(0, 0, pgeyS, pgeyS);
        pgeyG.save();
        pgeyG.beginPath();
        pgeyG.moveTo(6, 0);
        pgeyG.arcTo(pgeyS, 0, pgeyS, pgeyS, 6);
        pgeyG.arcTo(pgeyS, pgeyS, 0, pgeyS, 6);
        pgeyG.arcTo(0, pgeyS, 0, 0, 6);
        pgeyG.arcTo(0, 0, pgeyS, 0, 6);
        pgeyG.closePath();
        pgeyG.clip();

        pgeyG.setFill(pgeyBase);
        pgeyG.fillRect(0, 0, pgeyS, pgeyS);
        Color pgeyLight = pgeyBase.deriveColor(0, 1, 1.35, 0.55);
        Color pgeyDark = pgeyBase.deriveColor(0, 1, 0.6, 0.55);
        pgeyG.setLineCap(StrokeLineCap.ROUND);

        switch (pgeyPattern) {
            case DOTS -> pgeyDots(pgeyG, pgeyS, 5, 1.0, pgeyDark);
            case MESH -> pgeyDots(pgeyG, pgeyS, 7, 2.2, pgeyDark);
            case DIAGONAL -> {
                pgeyG.setStroke(pgeyLight);
                pgeyG.setLineWidth(1.5);
                for (double pgeyX = -pgeyS; pgeyX < pgeyS; pgeyX += 6) {
                    pgeyG.strokeLine(pgeyX, pgeyS, pgeyX + pgeyS, 0);
                }
            }
            case LINES -> {
                pgeyG.setStroke(pgeyLight);
                pgeyG.setLineWidth(1);
                for (double pgeyY = 3; pgeyY < pgeyS; pgeyY += 4) {
                    pgeyG.strokeLine(0, pgeyY, pgeyS, pgeyY);
                }
            }
            case GRID -> {
                pgeyG.setStroke(pgeyDark);
                pgeyG.setLineWidth(0.8);
                for (double pgeyV = 3; pgeyV < pgeyS; pgeyV += 5) {
                    pgeyG.strokeLine(0, pgeyV, pgeyS, pgeyV);
                    pgeyG.strokeLine(pgeyV, 0, pgeyV, pgeyS);
                }
            }
            case KNIT -> {
                pgeyG.setStroke(pgeyLight);
                pgeyG.setLineWidth(1.2);
                for (double pgeyY = 2; pgeyY < pgeyS; pgeyY += 5) {
                    for (double pgeyX = 0; pgeyX < pgeyS; pgeyX += 6) {
                        pgeyG.strokeLine(pgeyX, pgeyY, pgeyX + 3, pgeyY + 3);
                        pgeyG.strokeLine(pgeyX + 3, pgeyY + 3, pgeyX + 6, pgeyY);
                    }
                }
            }
            case GRAIN -> {
                pgeyG.setFill(pgeyDark);
                long pgeySeed = 7;
                for (int pgeyI = 0; pgeyI < 90; pgeyI++) {
                    pgeySeed = pgeySeed * 6364136223846793005L + 1442695040888963407L;
                    double pgeyX = ((pgeySeed >>> 33) % 1000) / 1000.0 * pgeyS;
                    double pgeyY = ((pgeySeed >>> 17) % 1000) / 1000.0 * pgeyS;
                    pgeyG.fillOval(pgeyX, pgeyY, 1.4, 1.4);
                }
            }
            case RUBBER -> {
                pgeyG.setStroke(pgeyDark);
                pgeyG.setLineWidth(2);
                for (double pgeyY = 4; pgeyY < pgeyS; pgeyY += 6) {
                    for (double pgeyX = 0; pgeyX < pgeyS; pgeyX += 8) {
                        pgeyG.strokeLine(pgeyX, pgeyY, pgeyX + 4, pgeyY + 3);
                    }
                }
            }
            case STUDS -> {
                pgeyG.setFill(pgeyDark);
                for (double pgeyY = 6; pgeyY < pgeyS; pgeyY += 12) {
                    for (double pgeyX = 6; pgeyX < pgeyS; pgeyX += 12) {
                        pgeyG.fillOval(pgeyX - 3, pgeyY - 3, 6, 6);
                    }
                }
            }
            case REFLECTIVE -> {
                pgeyG.setStroke(Color.color(1, 1, 1, 0.55));
                pgeyG.setLineWidth(3);
                pgeyG.strokeLine(0, pgeyS * 0.75, pgeyS, pgeyS * 0.25);
                pgeyG.setLineWidth(1);
                pgeyG.strokeLine(0, pgeyS, pgeyS, pgeyS * 0.5);
            }
            case SMOOTH -> {
                pgeyG.setStroke(pgeyLight);
                pgeyG.setLineWidth(4);
                pgeyG.strokeLine(pgeyS * 0.15, pgeyS * 0.35, pgeyS * 0.55, pgeyS * 0.15);
            }
            case PLAIN -> {
                pgeyG.setStroke(pgeyDark);
                pgeyG.setLineWidth(0.7);
                for (double pgeyY = 1.5; pgeyY < pgeyS; pgeyY += 3) {
                    for (double pgeyX = 0; pgeyX < pgeyS; pgeyX += 4) {
                        pgeyG.strokeLine(pgeyX, pgeyY, pgeyX + 2, pgeyY + 1.5);
                        pgeyG.strokeLine(pgeyX + 2, pgeyY + 1.5, pgeyX + 4, pgeyY);
                    }
                }
            }
        }
        pgeyG.restore();
    }

    private static void pgeyDots(GraphicsContext pgeyG, double pgeyS, double pgeyStep, double pgeyR, Color pgeyColor) {
        pgeyG.setFill(pgeyColor);
        for (double pgeyY = pgeyStep / 2; pgeyY < pgeyS; pgeyY += pgeyStep) {
            for (double pgeyX = pgeyStep / 2; pgeyX < pgeyS; pgeyX += pgeyStep) {
                pgeyG.fillOval(pgeyX - pgeyR, pgeyY - pgeyR, pgeyR * 2, pgeyR * 2);
            }
        }
    }
}
