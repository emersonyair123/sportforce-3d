package com.sportforce3d.render;

import com.sportforce3d.design.PgeyDesignFonts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;

final class PgeyDesignOverlay {
    static final double PGEY_LOCAL = 300;
    static final int PGEY_LIGHT_TEXT = 0xF2F5F8;
    static final int PGEY_DARK_TEXT = 0x0B0E13;
    private static volatile Font pgeyFont;

    private PgeyDesignOverlay() {
    }

    static PgeyArgbImage pgeyDraw(PgeyRealisticModel pgeyModel, PgeyRecolorRequest pgeyReq) {
        PgeyRealisticModel.PgeyShirtGeometry pgeyGeo = pgeyModel.pgeyShirt();
        if (pgeyGeo == null || !pgeyModel.pgeyHasZone(PgeyRealisticModel.PGEY_CAMISETA) || !pgeyReq.pgeyHasOverlay()) {
            return null;
        }
        BufferedImage pgeyImg = new BufferedImage(pgeyModel.pgeyWidth(), pgeyModel.pgeyHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D pgeyG = pgeyImg.createGraphics();
        try {
            pgeyG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pgeyG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            pgeyG.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            if (pgeyReq.pgeyPattern() != PgeyRealisticPattern.LISO) {
                AffineTransform pgeyLocal = new AffineTransform();
                double pgeyS = pgeyGeo.pgeyHeight() / PGEY_LOCAL;
                pgeyLocal.translate(pgeyGeo.pgeyCenterX(), pgeyGeo.pgeyNeckY());
                pgeyLocal.scale(pgeyModel.pgeyView() == PgeyRealisticModel.PgeyView.ESPALDA ? -pgeyS : pgeyS, pgeyS);
                int pgeyRgb = PgeyRecolorRequest.pgeyRgb(pgeyReq.pgeyPatternColor());
                for (PgeyTinted pgeyShape : pgeyPattern(pgeyReq.pgeyPattern())) {
                    pgeyG.setColor(new Color(pgeyRgb | (Math.round(pgeyShape.pgeyAlpha() * 255) << 24), true));
                    pgeyG.fill(pgeyLocal.createTransformedShape(pgeyShape.pgeyShape()));
                }
            }
            pgeyDrawTexts(pgeyG, pgeyModel, pgeyGeo, pgeyReq);
        } finally {
            pgeyG.dispose();
        }
        int[] pgeyPx = ((DataBufferInt) pgeyImg.getRaster().getDataBuffer()).getData();
        return new PgeyArgbImage(pgeyImg.getWidth(), pgeyImg.getHeight(), pgeyPx);
    }

    record PgeyTinted(Shape pgeyShape, float pgeyAlpha) {
    }

    static java.util.List<PgeyTinted> pgeyPattern(PgeyRealisticPattern pgeyP) {
        java.util.List<PgeyTinted> pgeyOut = new java.util.ArrayList<>();
        switch (pgeyP) {
            case LISO -> {
            }
            case RAYAS -> {
                for (int pgeyK = -7; pgeyK <= 6; pgeyK++) {
                    pgeyOut.add(new PgeyTinted(new Rectangle2D.Double(pgeyK * 48 + 12, -80, 24, 420), 1f));
                }
            }
            case FRANJAS -> {
                for (int pgeyY = 40; pgeyY < 320; pgeyY += 64) {
                    pgeyOut.add(new PgeyTinted(new Rectangle2D.Double(-320, pgeyY, 640, 30), 1f));
                }
            }
            case DINAMICO -> {
                pgeyOut.add(new PgeyTinted(pgeyStroke(-250, 300, 250, 70, 46, 10), 1f));
                pgeyOut.add(new PgeyTinted(pgeyStroke(-250, 430, 250, 150, 16, 4), 1f));
                pgeyOut.add(new PgeyTinted(pgeyStroke(-250, 150, 250, 115, 9, 3), 0.85f));
                pgeyOut.add(new PgeyTinted(pgeyStroke(-250, 235, 250, -45, 6, 2), 0.7f));
            }
            case GEOMETRICO -> {
                double pgeyCell = 36;
                for (int pgeyRow = 0; pgeyRow * pgeyCell < 330; pgeyRow++) {
                    double pgeyY = 90 + pgeyRow * pgeyCell * 0.87;
                    float pgeyAlpha = (float) Math.min(1, 0.35 + pgeyRow * 0.13);
                    for (int pgeyCol = -9; pgeyCol <= 9; pgeyCol++) {
                        if (Math.floorMod(pgeyCol * 7 + pgeyRow * 3, 4) == 0 || (pgeyRow >= 4 && Math.floorMod(pgeyCol + pgeyRow, 2) == 0)) {
                            double pgeyX = pgeyCol * pgeyCell / 2;
                            boolean pgeyUp = Math.floorMod(pgeyCol + pgeyRow, 2) == 0;
                            pgeyOut.add(new PgeyTinted(pgeyTriangle(pgeyX, pgeyY, pgeyCell, pgeyCell * 0.87, pgeyUp), pgeyAlpha));
                        }
                    }
                }
            }
            case MINIMAL -> {
                Path2D.Double pgeyYoke = new Path2D.Double();
                pgeyYoke.moveTo(-320, -120);
                pgeyYoke.lineTo(320, -120);
                for (int pgeyX = 320; pgeyX >= -320; pgeyX -= 10) {
                    pgeyYoke.lineTo(pgeyX, 30 + 0.0016 * pgeyX * pgeyX);
                }
                pgeyYoke.closePath();
                pgeyOut.add(new PgeyTinted(pgeyYoke, 1f));
            }
            case BANDA -> pgeyOut.add(new PgeyTinted(pgeyBand(-190, -30, 210, 330, 64), 1f));
        }
        return pgeyOut;
    }

    private static Shape pgeyStroke(double pgeyX0, double pgeyY0, double pgeyX1, double pgeyY1, double pgeyW0, double pgeyW1) {
        double pgeyDx = pgeyX1 - pgeyX0;
        double pgeyDy = pgeyY1 - pgeyY0;
        double pgeyLen = Math.hypot(pgeyDx, pgeyDy);
        double pgeyNx = -pgeyDy / pgeyLen;
        double pgeyNy = pgeyDx / pgeyLen;
        double pgeyMx = (pgeyX0 + pgeyX1) / 2 + pgeyNx * 18;
        double pgeyMy = (pgeyY0 + pgeyY1) / 2 + pgeyNy * 18;
        double pgeyWm = (pgeyW0 + pgeyW1) / 2;
        Path2D.Double pgeyP = new Path2D.Double();
        pgeyP.moveTo(pgeyX0 - pgeyNx * pgeyW0 / 2, pgeyY0 - pgeyNy * pgeyW0 / 2);
        pgeyP.quadTo(pgeyMx - pgeyNx * pgeyWm / 2, pgeyMy - pgeyNy * pgeyWm / 2, pgeyX1 - pgeyNx * pgeyW1 / 2, pgeyY1 - pgeyNy * pgeyW1 / 2);
        pgeyP.lineTo(pgeyX1 + pgeyNx * pgeyW1 / 2, pgeyY1 + pgeyNy * pgeyW1 / 2);
        pgeyP.quadTo(pgeyMx + pgeyNx * pgeyWm / 2, pgeyMy + pgeyNy * pgeyWm / 2, pgeyX0 + pgeyNx * pgeyW0 / 2, pgeyY0 + pgeyNy * pgeyW0 / 2);
        pgeyP.closePath();
        return pgeyP;
    }

    private static Shape pgeyBand(double pgeyX0, double pgeyY0, double pgeyX1, double pgeyY1, double pgeyWidth) {
        double pgeyLen = Math.hypot(pgeyX1 - pgeyX0, pgeyY1 - pgeyY0);
        double pgeyNx = -(pgeyY1 - pgeyY0) / pgeyLen * pgeyWidth / 2;
        double pgeyNy = (pgeyX1 - pgeyX0) / pgeyLen * pgeyWidth / 2;
        Path2D.Double pgeyP = new Path2D.Double();
        pgeyP.moveTo(pgeyX0 + pgeyNx, pgeyY0 + pgeyNy);
        pgeyP.lineTo(pgeyX1 + pgeyNx, pgeyY1 + pgeyNy);
        pgeyP.lineTo(pgeyX1 - pgeyNx, pgeyY1 - pgeyNy);
        pgeyP.lineTo(pgeyX0 - pgeyNx, pgeyY0 - pgeyNy);
        pgeyP.closePath();
        return pgeyP;
    }

    private static Shape pgeyTriangle(double pgeyCx, double pgeyTop, double pgeyW, double pgeyH, boolean pgeyUp) {
        Path2D.Double pgeyP = new Path2D.Double();
        if (pgeyUp) {
            pgeyP.moveTo(pgeyCx, pgeyTop);
            pgeyP.lineTo(pgeyCx + pgeyW / 2, pgeyTop + pgeyH);
            pgeyP.lineTo(pgeyCx - pgeyW / 2, pgeyTop + pgeyH);
        } else {
            pgeyP.moveTo(pgeyCx - pgeyW / 2, pgeyTop);
            pgeyP.lineTo(pgeyCx + pgeyW / 2, pgeyTop);
            pgeyP.lineTo(pgeyCx, pgeyTop + pgeyH);
        }
        pgeyP.closePath();
        return pgeyP;
    }

    private static void pgeyDrawTexts(Graphics2D pgeyG, PgeyRealisticModel pgeyModel, PgeyRealisticModel.PgeyShirtGeometry pgeyGeo, PgeyRecolorRequest pgeyReq) {
        if (pgeyReq.pgeyOverlay() != null) {
            pgeyDrawSynced(pgeyG, pgeyModel, pgeyGeo, pgeyReq.pgeyOverlay());
            return;
        }
        if (pgeyReq.pgeyName() == null && pgeyReq.pgeyNumber() == null) {
            return;
        }
        int pgeyFill = pgeyTextColor(pgeyReq);
        double pgeyH = pgeyGeo.pgeyHeight();
        boolean pgeyBack = pgeyModel.pgeyView() == PgeyRealisticModel.PgeyView.ESPALDA;
        if (pgeyReq.pgeyNumber() != null) {
            double pgeyCy = pgeyGeo.pgeyNeckY() + (pgeyBack ? 0.80 : 0.74) * pgeyH;
            double pgeySize = (pgeyBack ? 0.46 : 0.36) * pgeyH;
            pgeyOutlined(pgeyG, pgeyText(pgeyG, String.valueOf(pgeyReq.pgeyNumber()), "0", pgeyGeo.pgeyCenterX(), pgeyCy, pgeySize, (pgeyBack ? 0.62 : 0.5) * pgeyH),
                    pgeySize * 0.035, pgeyFill);
        }
        if (pgeyBack && pgeyReq.pgeyName() != null) {
            double pgeySize = 0.10 * pgeyH;
            pgeyOutlined(pgeyG, pgeyText(pgeyG, pgeyReq.pgeyName(), "H", pgeyGeo.pgeyCenterX(), pgeyGeo.pgeyNeckY() + 0.26 * pgeyH, pgeySize, 0.66 * pgeyH), pgeySize * 0.07, pgeyFill);
        }
    }

    private static void pgeyDrawSynced(Graphics2D pgeyG, PgeyRealisticModel pgeyModel, PgeyRealisticModel.PgeyShirtGeometry pgeyGeo,
                                   PgeyRecolorRequest.PgeyOverlay pgeyO) {
        double pgeyH = pgeyGeo.pgeyHeight();
        if (pgeyModel.pgeyView() == PgeyRealisticModel.PgeyView.ESPALDA) {
            if (pgeyO.pgeyBack() != null) {
                double pgeySize = 0.46 * pgeyH;
                pgeyPaint(pgeyG, pgeyText(pgeyG, pgeyFont(pgeyO.pgeyBack().pgeyFont()), pgeyO.pgeyBack().pgeyValue(), "0", pgeyGeo.pgeyCenterX(), pgeyGeo.pgeyNeckY() + 0.80 * pgeyH,
                        pgeySize, 0.62 * pgeyH), pgeySize * 0.035, pgeyO.pgeyBack());
            }
            if (pgeyO.pgeyName() != null) {
                double pgeySize = 0.10 * pgeyH;
                pgeyPaint(pgeyG, pgeyText(pgeyG, pgeyFont(pgeyO.pgeyName().pgeyFont()), pgeyO.pgeyName().pgeyValue(), "H", pgeyGeo.pgeyCenterX(), pgeyGeo.pgeyNeckY() + 0.26 * pgeyH,
                        pgeySize, 0.66 * pgeyH), pgeySize * 0.07, pgeyO.pgeyName());
            }
            return;
        }
        if (pgeyO.pgeyChest() != null) {
            double pgeySize = 0.15 * pgeyH;
            pgeyPaint(pgeyG, pgeyText(pgeyG, pgeyFont(pgeyO.pgeyChest().pgeyFont()), pgeyO.pgeyChest().pgeyValue(), "0", pgeyGeo.pgeyCenterX() - 0.16 * pgeyH,
                    pgeyGeo.pgeyNeckY() + 0.28 * pgeyH, pgeySize, 0.22 * pgeyH), pgeySize * 0.05, pgeyO.pgeyChest());
        }
        if (pgeyO.pgeyCrest() != null) {
            PgeyArgbImage pgeyC = pgeyO.pgeyCrest();
            BufferedImage pgeyImg = new BufferedImage(pgeyC.pgeyWidth(), pgeyC.pgeyHeight(), BufferedImage.TYPE_INT_ARGB);
            pgeyImg.setRGB(0, 0, pgeyC.pgeyWidth(), pgeyC.pgeyHeight(), pgeyC.pgeyPixels(), 0, pgeyC.pgeyWidth());
            double pgeyCh = 0.15 * pgeyH;
            double pgeyCw = pgeyCh * pgeyC.pgeyWidth() / Math.max(1, pgeyC.pgeyHeight());
            pgeyG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            pgeyG.drawImage(pgeyImg, (int) Math.round(pgeyGeo.pgeyCenterX() + 0.16 * pgeyH - pgeyCw / 2),
                    (int) Math.round(pgeyGeo.pgeyNeckY() + 0.28 * pgeyH - pgeyCh / 2), (int) Math.round(pgeyCw), (int) Math.round(pgeyCh), null);
        }
    }

    private static void pgeyPaint(Graphics2D pgeyG, Shape pgeyShape, double pgeyWidth, com.sportforce3d.design.PgeyRealisticView.PgeyText pgeyT) {
        if (pgeyT.pgeyOutline() != null) {
            pgeyG.setColor(new Color(PgeyRecolorRequest.pgeyRgb(pgeyT.pgeyOutline())));
            pgeyG.setStroke(new BasicStroke((float) Math.max(1.5, pgeyWidth * 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            pgeyG.draw(pgeyShape);
        }
        pgeyG.setColor(new Color(PgeyRecolorRequest.pgeyRgb(pgeyT.pgeyFill())));
        pgeyG.fill(pgeyShape);
    }

    private static void pgeyOutlined(Graphics2D pgeyG, Shape pgeyShape, double pgeyWidth, int pgeyFill) {
        pgeyG.setColor(new Color(pgeyLuminance(pgeyFill) > 0.55 ? PGEY_DARK_TEXT : PGEY_LIGHT_TEXT));
        pgeyG.setStroke(new BasicStroke((float) Math.max(1.5, pgeyWidth * 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        pgeyG.draw(pgeyShape);
        pgeyG.setColor(new Color(pgeyFill));
        pgeyG.fill(pgeyShape);
    }

    private static Shape pgeyText(Graphics2D pgeyG, String pgeyValue, String pgeyReference, double pgeyCx, double pgeyCy, double pgeySize, double pgeyMaxWidth) {
        return pgeyText(pgeyG, pgeyFont(), pgeyValue, pgeyReference, pgeyCx, pgeyCy, pgeySize, pgeyMaxWidth);
    }

    private static Shape pgeyText(Graphics2D pgeyG, Font pgeyBase, String pgeyValue, String pgeyReference, double pgeyCx, double pgeyCy,
                              double pgeySize, double pgeyMaxWidth) {
        FontRenderContext pgeyFrc = pgeyG.getFontRenderContext();
        Font pgeyF = pgeyBase.deriveFont(100f);
        Rectangle2D pgeyRef = new TextLayout(pgeyReference, pgeyF, pgeyFrc).getOutline(null).getBounds2D();
        Shape pgeyOutline = new TextLayout(pgeyValue, pgeyF, pgeyFrc).getOutline(null);
        Rectangle2D pgeyBounds = pgeyOutline.getBounds2D();
        double pgeyScale = pgeySize / pgeyRef.getHeight();
        double pgeySx = Math.min(pgeyScale, pgeyMaxWidth / Math.max(1, pgeyBounds.getWidth()));
        AffineTransform pgeyT = new AffineTransform();
        pgeyT.translate(pgeyCx, pgeyCy);
        pgeyT.scale(pgeySx, pgeyScale);
        pgeyT.translate(-pgeyBounds.getCenterX(), -pgeyRef.getCenterY());
        return pgeyT.createTransformedShape(pgeyOutline);
    }

    static int pgeyTextColor(PgeyRecolorRequest pgeyReq) {
        if (pgeyReq.pgeyTextColor() != null) {
            return PgeyRecolorRequest.pgeyRgb(pgeyReq.pgeyTextColor());
        }
        String pgeyShirt = pgeyReq.pgeyZoneColors().get(PgeyRealisticModel.PGEY_CAMISETA);
        int pgeyC = pgeyShirt == null ? 0xFFFFFF : PgeyRecolorRequest.pgeyRgb(pgeyShirt);
        return pgeyLuminance(pgeyC) > 0.55 ? PGEY_DARK_TEXT : PGEY_LIGHT_TEXT;
    }

    private static double pgeyLuminance(int pgeyRgb) {
        return (0.2126 * ((pgeyRgb >> 16) & 0xFF) + 0.7152 * ((pgeyRgb >> 8) & 0xFF) + 0.0722 * (pgeyRgb & 0xFF)) / 255;
    }

    private static final java.util.Map<String, Font> PGEY_FONTS = new java.util.concurrent.ConcurrentHashMap<>();

    private static Font pgeyFont(String pgeyKey) {
        if (pgeyKey == null || PgeyDesignFonts.PGEY_DEFAULT.equals(pgeyKey)) {
            return pgeyFont();
        }
        return PGEY_FONTS.computeIfAbsent(pgeyKey, pgeyK -> {
            try (InputStream pgeyIn = PgeyDesignOverlay.class.getResourceAsStream("/fonts/" + PgeyDesignFonts.pgeyFile(pgeyK))) {
                return pgeyIn == null ? pgeyFont() : Font.createFont(Font.TRUETYPE_FONT, pgeyIn);
            } catch (IOException | java.awt.FontFormatException | RuntimeException pgeyE) {
                return pgeyFont();
            }
        });
    }

    private static Font pgeyFont() {
        Font pgeyF = pgeyFont;
        if (pgeyF == null) {
            String pgeyFile = PgeyDesignFonts.pgeyFile(PgeyDesignFonts.PGEY_DEFAULT);
            try (InputStream pgeyIn = PgeyDesignOverlay.class.getResourceAsStream("/fonts/" + pgeyFile)) {
                if (pgeyIn == null) {
                    throw new IllegalStateException("Falta la fuente " + pgeyFile);
                }
                pgeyF = Font.createFont(Font.TRUETYPE_FONT, pgeyIn);
                pgeyFont = pgeyF;
            } catch (IOException | java.awt.FontFormatException pgeyE) {
                throw new IllegalStateException("No se pudo cargar la fuente " + pgeyFile, pgeyE);
            }
        }
        return pgeyF;
    }
}
