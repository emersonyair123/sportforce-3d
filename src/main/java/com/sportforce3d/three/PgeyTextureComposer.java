package com.sportforce3d.three;

import com.sportforce3d.design.PgeyArea;
import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeyDesignElement;
import com.sportforce3d.design.PgeyDesignFonts;
import com.sportforce3d.design.PgeyElementType;
import com.sportforce3d.design.PgeyPatternSpec;
import com.sportforce3d.design.PgeyPatternType;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyUvRect;
import com.sportforce3d.three.model.PgeyUvLayout;
import com.sportforce3d.three.model.PgeyZone;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class PgeyTextureComposer {
    static final Map<HepyGarment, Set<PgeyZone>> PGEY_PATTERNED = Map.of(
            HepyGarment.JERSEY, Set.of(PgeyZone.JERSEY_BODY, PgeyZone.JERSEY_SLEEVES),
            HepyGarment.SHORTS, Set.of(PgeyZone.SHORTS_BASE),
            HepyGarment.SOCKS, Set.of(PgeyZone.SOCKS_LEG));

    private static final Map<PgeyUvRect, double[]> PGEY_PHYSICAL = pgeyPhysical();
    private static final Map<String, Font> PGEY_FONTS = new ConcurrentHashMap<>();
    private static final Color PGEY_FOCUS = new Color(0x5C, 0x9B, 0xFF);

    private final PgeyElementImages pgeyImages;
    private final Map<HepyGarment, BufferedImage> pgeyRasters = new EnumMap<>(HepyGarment.class);
    private final Map<HepyGarment, WritableImage[]> pgeyBuffers = new EnumMap<>(HepyGarment.class);
    private final Map<HepyGarment, Integer> pgeyFlip = new EnumMap<>(HepyGarment.class);
    private final Map<Image, BufferedImage> pgeyAwtImages = new WeakHashMap<>();
    private final Map<String, double[]> pgeyBounds = new HashMap<>();
    private String pgeyHighlight;

    public PgeyTextureComposer(PgeyElementImages pgeyImages) {
        this.pgeyImages = pgeyImages;
    }

    public static int pgeySize(HepyGarment pgeyG) {
        return pgeyG == HepyGarment.JERSEY || pgeyG == HepyGarment.SHORTS ? 2048 : 1024;
    }

    public static List<HepyGarment> pgeyGarments() {
        return List.of(HepyGarment.JERSEY, HepyGarment.SHORTS, HepyGarment.SOCKS, HepyGarment.SHOES);
    }

    public Image pgeyCompose(HepyGarment pgeyGarment, PgeyDesignDocument pgeyDoc) {
        int pgeyS = pgeySize(pgeyGarment);
        BufferedImage pgeyRaster = pgeyRasters.computeIfAbsent(pgeyGarment, pgeyG -> new BufferedImage(pgeyS, pgeyS, BufferedImage.TYPE_INT_ARGB_PRE));
        Graphics2D pgeyG = pgeyRaster.createGraphics();
        try {
            pgeyG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pgeyG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pgeyG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            pgeyG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            pgeyG.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            pgeyG.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            pgeyG.setComposite(AlphaComposite.Src);

            pgeyG.setColor(pgeyAwt(pgeyDoc.pgeyColor(pgeyMainZone(pgeyGarment))));
            pgeyG.fillRect(0, 0, pgeyS, pgeyS);
            pgeyG.setComposite(AlphaComposite.SrcOver);
            double pgeyBleed = pgeyS * 0.004;
            for (PgeyZone pgeyZone : PgeyZone.values()) {
                if (pgeyZone.pgeyGarment() != pgeyGarment) {
                    continue;
                }
                pgeyG.setColor(pgeyAwt(pgeyDoc.pgeyColor(pgeyZone)));
                for (PgeyUvRect pgeyR : PgeyUvLayout.PGEY_BY_ZONE.get(pgeyZone)) {
                    pgeyG.fill(new Rectangle2D.Double(pgeyR.pgeyU0() * pgeyS - pgeyBleed, pgeyR.pgeyV0() * pgeyS - pgeyBleed,
                            (pgeyR.pgeyU1() - pgeyR.pgeyU0()) * pgeyS + 2 * pgeyBleed, (pgeyR.pgeyV1() - pgeyR.pgeyV0()) * pgeyS + 2 * pgeyBleed));
                }
            }

            PgeyPatternSpec pgeyPattern = pgeyDoc.pgeyPattern(pgeyGarment);
            if (pgeyPattern.pgeyType() != PgeyPatternType.LISO) {
                for (PgeyZone pgeyZone : PGEY_PATTERNED.getOrDefault(pgeyGarment, Set.of())) {
                    Color pgeyBase = pgeyAwt(pgeyDoc.pgeyColor(pgeyZone));
                    for (PgeyUvRect pgeyR : PgeyUvLayout.PGEY_BY_ZONE.get(pgeyZone)) {
                        pgeyDrawPattern(pgeyG, pgeyS, pgeyBleed, pgeyR, PGEY_PHYSICAL.getOrDefault(pgeyR, new double[]{0.4, 0.4}), pgeyPattern, pgeyBase);
                    }
                }
            }

            pgeyBounds.keySet().removeIf(pgeyId -> pgeyDoc.pgeyElement(pgeyId).map(pgeyE -> pgeyE.pgeyArea().pgeyGarment() == pgeyGarment).orElse(true));
            for (PgeyDesignElement pgeyE : pgeyDoc.pgeyElements()) {
                if (pgeyE.pgeyArea().pgeyGarment() == pgeyGarment && pgeyE.pgeyVisible()) {
                    pgeyDrawElement(pgeyG, pgeyS, pgeyE);
                }
            }
        } finally {
            pgeyG.dispose();
        }

        WritableImage[] pgeyPair = pgeyBuffers.computeIfAbsent(pgeyGarment, pgeyX -> new WritableImage[]{
                new WritableImage(pgeyS, pgeyS), new WritableImage(pgeyS, pgeyS)});
        int pgeyI = pgeyFlip.merge(pgeyGarment, 1, (pgeyA, pgeyB) -> 1 - pgeyA);
        int[] pgeyData = ((DataBufferInt) pgeyRaster.getRaster().getDataBuffer()).getData();
        pgeyPair[pgeyI].getPixelWriter().setPixels(0, 0, pgeyS, pgeyS, PixelFormat.getIntArgbPreInstance(), pgeyData, 0, pgeyS);
        return pgeyPair[pgeyI];
    }

    public String pgeyHit(HepyGarment pgeyGarment, double pgeyU, double pgeyV, PgeyDesignDocument pgeyDoc) {
        List<PgeyDesignElement> pgeyList = pgeyDoc.pgeyElements();
        for (int pgeyI = pgeyList.size() - 1; pgeyI >= 0; pgeyI--) {
            PgeyDesignElement pgeyE = pgeyList.get(pgeyI);
            double[] pgeyB = pgeyBounds.get(pgeyE.pgeyId());
            if (pgeyE.pgeyVisible() && pgeyE.pgeyArea().pgeyGarment() == pgeyGarment && pgeyB != null
                    && pgeyU >= pgeyB[0] && pgeyU <= pgeyB[2] && pgeyV >= pgeyB[1] && pgeyV <= pgeyB[3]) {
                return pgeyE.pgeyId();
            }
        }
        return null;
    }

    public double[] pgeyBounds(String pgeyElementId) {
        return pgeyBounds.get(pgeyElementId);
    }

    public void pgeySetHighlight(String pgeyElementId) {
        this.pgeyHighlight = pgeyElementId;
    }

    public String pgeyHighlight() {
        return pgeyHighlight;
    }

    private static void pgeyDrawPattern(Graphics2D pgeyG0, int pgeyS, double pgeyBleed, PgeyUvRect pgeyR, double[] pgeyPhys,
                                    PgeyPatternSpec pgeyP, Color pgeyBase) {
        double pgeyX0 = pgeyR.pgeyU0() * pgeyS - pgeyBleed;
        double pgeyY0 = pgeyR.pgeyV0() * pgeyS - pgeyBleed;
        double pgeyW = (pgeyR.pgeyU1() - pgeyR.pgeyU0()) * pgeyS + 2 * pgeyBleed;
        double pgeyH = (pgeyR.pgeyV1() - pgeyR.pgeyV0()) * pgeyS + 2 * pgeyBleed;
        double pgeyAx = (pgeyR.pgeyU1() - pgeyR.pgeyU0()) * pgeyS / pgeyPhys[0];
        double pgeyAy = (pgeyR.pgeyV1() - pgeyR.pgeyV0()) * pgeyS / pgeyPhys[1];
        double pgeyCx = (pgeyR.pgeyU0() + pgeyR.pgeyU1()) / 2 * pgeyS;
        double pgeyCy = (pgeyR.pgeyV0() + pgeyR.pgeyV1()) / 2 * pgeyS;
        Color pgeyC2 = pgeyAwt(pgeyP.pgeyColor());
        double pgeyM = pgeyP.pgeyWidth();

        Graphics2D pgeyG = (Graphics2D) pgeyG0.create();
        try {
            pgeyG.clip(new Rectangle2D.Double(pgeyX0, pgeyY0, pgeyW, pgeyH));
            pgeyG.setColor(pgeyC2);
            switch (pgeyP.pgeyType()) {
                case FRANJAS_VERTICALES -> {
                    double pgeySw = pgeyM * pgeyAx;
                    for (double pgeyX = pgeyCx - pgeySw / 2 - Math.ceil(pgeyW / (2 * pgeySw) + 1) * 2 * pgeySw; pgeyX < pgeyX0 + pgeyW; pgeyX += 2 * pgeySw) {
                        pgeyG.fill(new Rectangle2D.Double(pgeyX, pgeyY0, pgeySw, pgeyH));
                    }
                }
                case FRANJAS_HORIZONTALES -> {
                    double pgeySh = pgeyM * pgeyAy;
                    for (double pgeyY = pgeyCy - pgeySh / 2 - Math.ceil(pgeyH / (2 * pgeySh) + 1) * 2 * pgeySh; pgeyY < pgeyY0 + pgeyH; pgeyY += 2 * pgeySh) {
                        pgeyG.fill(new Rectangle2D.Double(pgeyX0, pgeyY, pgeyW, pgeySh));
                    }
                }
                case DIAGONALES -> {
                    pgeyPhysicalFrame(pgeyG, pgeyCx, pgeyCy, pgeyAx, pgeyAy);
                    pgeyG.rotate(Math.toRadians(-45));
                    double pgeyD = pgeyPhys[0] + pgeyPhys[1];
                    for (double pgeyT = -pgeyD - pgeyM / 2; pgeyT < pgeyD; pgeyT += 2 * pgeyM) {
                        pgeyG.fill(new Rectangle2D.Double(pgeyT, -pgeyD, pgeyM, 2 * pgeyD));
                    }
                }
                case CUADROS -> {
                    pgeyPhysicalFrame(pgeyG, pgeyCx, pgeyCy, pgeyAx, pgeyAy);
                    int pgeyNx = (int) Math.ceil(pgeyPhys[0] / pgeyM) + 2;
                    int pgeyNy = (int) Math.ceil(pgeyPhys[1] / pgeyM) + 2;
                    Path2D.Double pgeySquares = new Path2D.Double();
                    for (int pgeyI = -pgeyNx; pgeyI <= pgeyNx; pgeyI++) {
                        for (int pgeyJ = -pgeyNy; pgeyJ <= pgeyNy; pgeyJ++) {
                            if (Math.floorMod(pgeyI + pgeyJ, 2) == 0) {
                                pgeySquares.append(new Rectangle2D.Double(pgeyI * pgeyM - pgeyM / 2, pgeyJ * pgeyM - pgeyM / 2, pgeyM, pgeyM), false);
                            }
                        }
                    }
                    pgeyG.fill(pgeySquares);
                }
                case DEGRADADO -> {
                    pgeyG.setPaint(new LinearGradientPaint((float) pgeyX0, (float) pgeyY0, (float) pgeyX0, (float) (pgeyY0 + pgeyH),
                            new float[]{0f, 0.15f, 1f}, new Color[]{pgeyBase, pgeyBase, pgeyC2}));
                    pgeyG.fill(new Rectangle2D.Double(pgeyX0, pgeyY0, pgeyW, pgeyH));
                }
                case SUBLIMADO -> {
                    pgeyPhysicalFrame(pgeyG, pgeyCx, pgeyCy, pgeyAx, pgeyAy);
                    int pgeyNx = (int) Math.ceil(pgeyPhys[0] / pgeyM) + 2;
                    int pgeyNy = (int) Math.ceil(pgeyPhys[1] / pgeyM) + 2;
                    Color pgeyLight = pgeyMix(pgeyC2, pgeyBase, 0.35);
                    for (int pgeyI = -pgeyNx; pgeyI <= pgeyNx; pgeyI++) {
                        for (int pgeyJ = -pgeyNy; pgeyJ <= pgeyNy; pgeyJ++) {
                            if (Math.floorMod(pgeyI * 3 + pgeyJ * 5, 4) == 0) {
                                continue;
                            }
                            double pgeyX = pgeyI * pgeyM;
                            double pgeyY = pgeyJ * pgeyM;
                            boolean pgeyFlipT = Math.floorMod(pgeyI * 7 + pgeyJ * 3, 5) < 2;
                            Path2D.Double pgeyTri = new Path2D.Double();
                            pgeyTri.moveTo(pgeyFlipT ? pgeyX : pgeyX + pgeyM, pgeyY);
                            pgeyTri.lineTo(pgeyX + pgeyM, pgeyY + pgeyM);
                            pgeyTri.lineTo(pgeyX, pgeyY + pgeyM);
                            pgeyTri.closePath();
                            pgeyG.setColor(Math.floorMod(pgeyI + pgeyJ * 2, 3) == 0 ? pgeyLight : pgeyC2);
                            pgeyG.fill(pgeyTri);
                        }
                    }
                }
                case LISO -> {
                }
            }
        } finally {
            pgeyG.dispose();
        }
    }

    private static void pgeyPhysicalFrame(Graphics2D pgeyG, double pgeyCx, double pgeyCy, double pgeyAx, double pgeyAy) {
        pgeyG.translate(pgeyCx, pgeyCy);
        pgeyG.scale(pgeyAx, pgeyAy);
    }

    private void pgeyDrawElement(Graphics2D pgeyG0, int pgeyS, PgeyDesignElement pgeyE) {
        PgeyArea pgeyArea = pgeyE.pgeyArea();
        PgeyUvRect pgeyR = pgeyArea.pgeyRect();
        double pgeyAx = pgeyArea.pgeyPxPerMeterX(pgeyS);
        double pgeyAy = pgeyArea.pgeyPxPerMeterY(pgeyS);
        double pgeyCx = (pgeyR.pgeyU0() + pgeyE.pgeyU() * (pgeyR.pgeyU1() - pgeyR.pgeyU0())) * pgeyS;
        double pgeyCy = (pgeyR.pgeyV0() + pgeyE.pgeyV() * (pgeyR.pgeyV1() - pgeyR.pgeyV0())) * pgeyS;
        double pgeyHPx = pgeyE.pgeyHeight() * pgeyE.pgeyScale() * pgeyAy;
        double pgeyWPx;

        Graphics2D pgeyG = (Graphics2D) pgeyG0.create();
        try {
            pgeyG.clip(new Rectangle2D.Double(pgeyR.pgeyU0() * pgeyS, pgeyR.pgeyV0() * pgeyS, (pgeyR.pgeyU1() - pgeyR.pgeyU0()) * pgeyS, (pgeyR.pgeyV1() - pgeyR.pgeyV0()) * pgeyS));
            pgeyG.translate(pgeyCx, pgeyCy);
            pgeyG.scale(pgeyAx / pgeyAy, 1);
            pgeyG.rotate(Math.toRadians(pgeyE.pgeyRotation()));

            if (pgeyE.pgeyType().pgeyIsText()) {
                String pgeyText = pgeyE.pgeyText() == null ? "" : pgeyE.pgeyText();
                Font pgeyFont = pgeyFont(pgeyE.pgeyFont()).deriveFont((float) (pgeyHPx / 0.72));
                FontRenderContext pgeyFrc = pgeyG.getFontRenderContext();
                double pgeyCapHeight = -new TextLayout("H", pgeyFont, pgeyFrc).getBounds().getY();
                double pgeyStroke = pgeyE.pgeyOutline() * pgeyE.pgeyScale() * pgeyAy * 2;
                if (pgeyText.isEmpty()) {
                    pgeyWPx = pgeyCapHeight;
                } else {
                    TextLayout pgeyLayout = new TextLayout(pgeyText, pgeyFont, pgeyFrc);
                    pgeyWPx = pgeyLayout.getAdvance();
                    Shape pgeyOutline = pgeyLayout.getOutline(AffineTransform.getTranslateInstance(-pgeyWPx / 2, pgeyCapHeight / 2));
                    if (pgeyStroke > 0 && pgeyE.pgeyAccentColor() != null) {
                        pgeyG.setColor(pgeyAwt(pgeyE.pgeyAccentColor()));
                        pgeyG.setStroke(new BasicStroke((float) pgeyStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        pgeyG.draw(pgeyOutline);
                    }
                    pgeyG.setColor(pgeyAwt(pgeyE.pgeyColor() == null ? "#FFFFFF" : pgeyE.pgeyColor()));
                    pgeyG.fill(pgeyOutline);
                }
                pgeyWPx += pgeyStroke;
                pgeyHPx = pgeyHPx * 1.15 + pgeyStroke;
            } else {
                Image pgeyImg = pgeyE.pgeyType() == PgeyElementType.EMBLEM
                        ? pgeyImages.pgeyEmblem(pgeyE.pgeyEmblem(), pgeyE.pgeyColor(), pgeyE.pgeyAccentColor())
                        : pgeyImages.pgeyLogo(pgeyE.pgeyAssetPath());
                BufferedImage pgeyAwtImg = pgeyImg == null ? null : pgeyToAwt(pgeyImg);
                if (pgeyAwtImg == null) {
                    return;
                }
                pgeyWPx = (double) pgeyAwtImg.getWidth() / pgeyAwtImg.getHeight() * pgeyHPx;
                AffineTransform pgeyAt = new AffineTransform();
                pgeyAt.translate(-pgeyWPx / 2, -pgeyHPx / 2);
                pgeyAt.scale(pgeyWPx / pgeyAwtImg.getWidth(), pgeyHPx / pgeyAwtImg.getHeight());
                pgeyG.drawImage(pgeyAwtImg, pgeyAt, null);
            }
            if (pgeyE.pgeyId().equals(pgeyHighlight)) {
                pgeyDrawHighlight(pgeyG, pgeyWPx, pgeyHPx, pgeyS);
            }
        } finally {
            pgeyG.dispose();
        }

        double pgeyRad = Math.toRadians(pgeyE.pgeyRotation());
        double pgeyCos = Math.abs(Math.cos(pgeyRad));
        double pgeySin = Math.abs(Math.sin(pgeyRad));
        double pgeyBw = (pgeyWPx * pgeyCos + pgeyHPx * pgeySin) * (pgeyAx / pgeyAy) / 2;
        double pgeyBh = (pgeyWPx * pgeySin + pgeyHPx * pgeyCos) / 2;
        pgeyBounds.put(pgeyE.pgeyId(), new double[]{(pgeyCx - pgeyBw) / pgeyS, (pgeyCy - pgeyBh) / pgeyS, (pgeyCx + pgeyBw) / pgeyS, (pgeyCy + pgeyBh) / pgeyS});
    }

    private static void pgeyDrawHighlight(Graphics2D pgeyG, double pgeyW, double pgeyH, int pgeyS) {
        double pgeyPad = pgeyS * 0.006;
        java.awt.geom.RoundRectangle2D pgeyBox = new java.awt.geom.RoundRectangle2D.Double(-pgeyW / 2 - pgeyPad, -pgeyH / 2 - pgeyPad,
                pgeyW + 2 * pgeyPad, pgeyH + 2 * pgeyPad, pgeyPad * 2, pgeyPad * 2);
        float pgeyLine = (float) (pgeyS * 0.0018);
        pgeyG.setColor(new Color(PGEY_FOCUS.getRed(), PGEY_FOCUS.getGreen(), PGEY_FOCUS.getBlue(), 70));
        pgeyG.setStroke(new BasicStroke(pgeyLine * 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        pgeyG.draw(pgeyBox);
        pgeyG.setColor(PGEY_FOCUS);
        pgeyG.setStroke(new BasicStroke(pgeyLine, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                new float[]{pgeyLine * 5f, pgeyLine * 3f}, 0f));
        pgeyG.draw(pgeyBox);
    }

    private BufferedImage pgeyToAwt(Image pgeyImg) {
        BufferedImage pgeyCached = pgeyAwtImages.get(pgeyImg);
        if (pgeyCached != null) {
            return pgeyCached;
        }
        int pgeyW = (int) pgeyImg.getWidth();
        int pgeyH = (int) pgeyImg.getHeight();
        if (pgeyW <= 0 || pgeyH <= 0 || pgeyImg.getPixelReader() == null) {
            return null;
        }
        BufferedImage pgeyOut = new BufferedImage(pgeyW, pgeyH, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] pgeyData = ((DataBufferInt) pgeyOut.getRaster().getDataBuffer()).getData();
        pgeyImg.getPixelReader().getPixels(0, 0, pgeyW, pgeyH, PixelFormat.getIntArgbPreInstance(), pgeyData, 0, pgeyW);
        pgeyAwtImages.put(pgeyImg, pgeyOut);
        return pgeyOut;
    }

    static Font pgeyFont(String pgeyKey) {
        return PGEY_FONTS.computeIfAbsent(PgeyDesignFonts.pgeyFile(pgeyKey), pgeyFile -> {
            try (InputStream pgeyIn = PgeyTextureComposer.class.getResourceAsStream("/fonts/" + pgeyFile)) {
                if (pgeyIn == null) {
                    throw new IllegalStateException("Falta la fuente " + pgeyFile);
                }
                return Font.createFont(Font.TRUETYPE_FONT, pgeyIn);
            } catch (java.io.IOException | java.awt.FontFormatException pgeyEx) {
                throw new IllegalStateException("No se pudo cargar la fuente " + pgeyFile, pgeyEx);
            }
        });
    }

    static Color pgeyAwt(String pgeyHex) {
        try {
            String pgeyH = pgeyHex.startsWith("#") ? pgeyHex.substring(1) : pgeyHex;
            return new Color(Integer.parseInt(pgeyH, 16));
        } catch (RuntimeException pgeyE) {
            return Color.GRAY;
        }
    }

    private static Color pgeyMix(Color pgeyA, Color pgeyB, double pgeyT) {
        return new Color((int) Math.round(pgeyA.getRed() + (pgeyB.getRed() - pgeyA.getRed()) * pgeyT),
                (int) Math.round(pgeyA.getGreen() + (pgeyB.getGreen() - pgeyA.getGreen()) * pgeyT),
                (int) Math.round(pgeyA.getBlue() + (pgeyB.getBlue() - pgeyA.getBlue()) * pgeyT));
    }

    private static PgeyZone pgeyMainZone(HepyGarment pgeyG) {
        return switch (pgeyG) {
            case SHORTS -> PgeyZone.SHORTS_BASE;
            case SOCKS -> PgeyZone.SOCKS_LEG;
            case SHOES -> PgeyZone.SHOE_UPPER;
            default -> PgeyZone.JERSEY_BODY;
        };
    }

    private static Map<PgeyUvRect, double[]> pgeyPhysical() {
        Map<PgeyUvRect, double[]> pgeyM = new HashMap<>();
        pgeyM.put(PgeyUvLayout.PGEY_JERSEY_FRONT, new double[]{0.47, 0.58});
        pgeyM.put(PgeyUvLayout.PGEY_JERSEY_BACK, new double[]{0.47, 0.58});
        pgeyM.put(PgeyUvLayout.PGEY_JERSEY_SLEEVE_LEFT, new double[]{0.40, 0.17});
        pgeyM.put(PgeyUvLayout.PGEY_JERSEY_SLEEVE_RIGHT, new double[]{0.40, 0.17});
        pgeyM.put(PgeyUvLayout.PGEY_JERSEY_FOREARM_LEFT, new double[]{0.40, 0.36});
        pgeyM.put(PgeyUvLayout.PGEY_JERSEY_FOREARM_RIGHT, new double[]{0.40, 0.36});
        for (PgeyUvRect pgeyR : List.of(PgeyUvLayout.PGEY_SHORTS_LEFT_FRONT, PgeyUvLayout.PGEY_SHORTS_LEFT_BACK, PgeyUvLayout.PGEY_SHORTS_RIGHT_FRONT,
                PgeyUvLayout.PGEY_SHORTS_RIGHT_BACK)) {
            pgeyM.put(pgeyR, new double[]{0.36, 0.35});
        }
        pgeyM.put(PgeyUvLayout.PGEY_SOCK_LEFT, new double[]{0.36, 0.37});
        pgeyM.put(PgeyUvLayout.PGEY_SOCK_RIGHT, new double[]{0.36, 0.37});
        return pgeyM;
    }
}
