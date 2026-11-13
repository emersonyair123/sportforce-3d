package com.sportforce3d.render;

import com.sportforce3d.design.DesignFonts;

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

final class DesignOverlay {
    static final double LOCAL = 300;
    static final int LIGHT_TEXT = 0xF2F5F8;
    static final int DARK_TEXT = 0x0B0E13;
    private static volatile Font font;

    private DesignOverlay() {
    }

    static ArgbImage draw(RealisticModel model, RecolorRequest req) {
        RealisticModel.ShirtGeometry geo = model.shirt();
        if (geo == null || !model.hasZone(RealisticModel.CAMISETA) || !req.hasOverlay()) {
            return null;
        }
        BufferedImage img = new BufferedImage(model.width(), model.height(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            if (req.pattern() != RealisticPattern.LISO) {
                AffineTransform local = new AffineTransform();
                double s = geo.height() / LOCAL;
                local.translate(geo.centerX(), geo.neckY());
                local.scale(model.view() == RealisticModel.View.ESPALDA ? -s : s, s);
                int rgb = RecolorRequest.rgb(req.patternColor());
                for (Tinted shape : pattern(req.pattern())) {
                    g.setColor(new Color(rgb | (Math.round(shape.alpha() * 255) << 24), true));
                    g.fill(local.createTransformedShape(shape.shape()));
                }
            }
            drawTexts(g, model, geo, req);
        } finally {
            g.dispose();
        }
        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        return new ArgbImage(img.getWidth(), img.getHeight(), px);
    }

    record Tinted(Shape shape, float alpha) {
    }

    static java.util.List<Tinted> pattern(RealisticPattern p) {
        java.util.List<Tinted> out = new java.util.ArrayList<>();
        switch (p) {
            case LISO -> {
            }
            case RAYAS -> {
                for (int k = -7; k <= 6; k++) {
                    out.add(new Tinted(new Rectangle2D.Double(k * 48 + 12, -80, 24, 420), 1f));
                }
            }
            case FRANJAS -> {
                for (int y = 40; y < 320; y += 64) {
                    out.add(new Tinted(new Rectangle2D.Double(-320, y, 640, 30), 1f));
                }
            }
            case DINAMICO -> {
                out.add(new Tinted(stroke(-250, 300, 250, 70, 46, 10), 1f));
                out.add(new Tinted(stroke(-250, 430, 250, 150, 16, 4), 1f));
                out.add(new Tinted(stroke(-250, 150, 250, 115, 9, 3), 0.85f));
                out.add(new Tinted(stroke(-250, 235, 250, -45, 6, 2), 0.7f));
            }
            case GEOMETRICO -> {
                double cell = 36;
                for (int row = 0; row * cell < 330; row++) {
                    double y = 90 + row * cell * 0.87;
                    float alpha = (float) Math.min(1, 0.35 + row * 0.13);
                    for (int col = -9; col <= 9; col++) {
                        if (Math.floorMod(col * 7 + row * 3, 4) == 0 || (row >= 4 && Math.floorMod(col + row, 2) == 0)) {
                            double x = col * cell / 2;
                            boolean up = Math.floorMod(col + row, 2) == 0;
                            out.add(new Tinted(triangle(x, y, cell, cell * 0.87, up), alpha));
                        }
                    }
                }
            }
            case MINIMAL -> {
                Path2D.Double yoke = new Path2D.Double();
                yoke.moveTo(-320, -120);
                yoke.lineTo(320, -120);
                for (int x = 320; x >= -320; x -= 10) {
                    yoke.lineTo(x, 30 + 0.0016 * x * x);
                }
                yoke.closePath();
                out.add(new Tinted(yoke, 1f));
            }
            case BANDA -> out.add(new Tinted(band(-190, -30, 210, 330, 64), 1f));
        }
        return out;
    }

    private static Shape stroke(double x0, double y0, double x1, double y1, double w0, double w1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double len = Math.hypot(dx, dy);
        double nx = -dy / len;
        double ny = dx / len;
        double mx = (x0 + x1) / 2 + nx * 18;
        double my = (y0 + y1) / 2 + ny * 18;
        double wm = (w0 + w1) / 2;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x0 - nx * w0 / 2, y0 - ny * w0 / 2);
        p.quadTo(mx - nx * wm / 2, my - ny * wm / 2, x1 - nx * w1 / 2, y1 - ny * w1 / 2);
        p.lineTo(x1 + nx * w1 / 2, y1 + ny * w1 / 2);
        p.quadTo(mx + nx * wm / 2, my + ny * wm / 2, x0 + nx * w0 / 2, y0 + ny * w0 / 2);
        p.closePath();
        return p;
    }

    private static Shape band(double x0, double y0, double x1, double y1, double width) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        double nx = -(y1 - y0) / len * width / 2;
        double ny = (x1 - x0) / len * width / 2;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x0 + nx, y0 + ny);
        p.lineTo(x1 + nx, y1 + ny);
        p.lineTo(x1 - nx, y1 - ny);
        p.lineTo(x0 - nx, y0 - ny);
        p.closePath();
        return p;
    }

    private static Shape triangle(double cx, double top, double w, double h, boolean up) {
        Path2D.Double p = new Path2D.Double();
        if (up) {
            p.moveTo(cx, top);
            p.lineTo(cx + w / 2, top + h);
            p.lineTo(cx - w / 2, top + h);
        } else {
            p.moveTo(cx - w / 2, top);
            p.lineTo(cx + w / 2, top);
            p.lineTo(cx, top + h);
        }
        p.closePath();
        return p;
    }

    private static void drawTexts(Graphics2D g, RealisticModel model, RealisticModel.ShirtGeometry geo, RecolorRequest req) {
        if (req.overlay() != null) {
            drawSynced(g, model, geo, req.overlay());
            return;
        }
        if (req.name() == null && req.number() == null) {
            return;
        }
        int fill = textColor(req);
        double h = geo.height();
        boolean back = model.view() == RealisticModel.View.ESPALDA;
        if (req.number() != null) {
            double cy = geo.neckY() + (back ? 0.80 : 0.74) * h;
            double size = (back ? 0.46 : 0.36) * h;
            outlined(g, text(g, String.valueOf(req.number()), "0", geo.centerX(), cy, size, (back ? 0.62 : 0.5) * h),
                    size * 0.035, fill);
        }
        if (back && req.name() != null) {
            double size = 0.10 * h;
            outlined(g, text(g, req.name(), "H", geo.centerX(), geo.neckY() + 0.26 * h, size, 0.66 * h), size * 0.07, fill);
        }
    }

    private static void drawSynced(Graphics2D g, RealisticModel model, RealisticModel.ShirtGeometry geo,
                                   RecolorRequest.Overlay o) {
        double h = geo.height();
        if (model.view() == RealisticModel.View.ESPALDA) {
            if (o.back() != null) {
                double size = 0.46 * h;
                paint(g, text(g, font(o.back().font()), o.back().value(), "0", geo.centerX(), geo.neckY() + 0.80 * h,
                        size, 0.62 * h), size * 0.035, o.back());
            }
            if (o.name() != null) {
                double size = 0.10 * h;
                paint(g, text(g, font(o.name().font()), o.name().value(), "H", geo.centerX(), geo.neckY() + 0.26 * h,
                        size, 0.66 * h), size * 0.07, o.name());
            }
            return;
        }
        if (o.chest() != null) {
            double size = 0.15 * h;
            paint(g, text(g, font(o.chest().font()), o.chest().value(), "0", geo.centerX() - 0.16 * h,
                    geo.neckY() + 0.28 * h, size, 0.22 * h), size * 0.05, o.chest());
        }
        if (o.crest() != null) {
            ArgbImage c = o.crest();
            BufferedImage img = new BufferedImage(c.width(), c.height(), BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, c.width(), c.height(), c.pixels(), 0, c.width());
            double ch = 0.15 * h;
            double cw = ch * c.width() / Math.max(1, c.height());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(img, (int) Math.round(geo.centerX() + 0.16 * h - cw / 2),
                    (int) Math.round(geo.neckY() + 0.28 * h - ch / 2), (int) Math.round(cw), (int) Math.round(ch), null);
        }
    }

    private static void paint(Graphics2D g, Shape shape, double width, com.sportforce3d.design.RealisticView.Text t) {
        if (t.outline() != null) {
            g.setColor(new Color(RecolorRequest.rgb(t.outline())));
            g.setStroke(new BasicStroke((float) Math.max(1.5, width * 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(shape);
        }
        g.setColor(new Color(RecolorRequest.rgb(t.fill())));
        g.fill(shape);
    }

    private static void outlined(Graphics2D g, Shape shape, double width, int fill) {
        g.setColor(new Color(luminance(fill) > 0.55 ? DARK_TEXT : LIGHT_TEXT));
        g.setStroke(new BasicStroke((float) Math.max(1.5, width * 2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
        g.setColor(new Color(fill));
        g.fill(shape);
    }

    private static Shape text(Graphics2D g, String value, String reference, double cx, double cy, double size, double maxWidth) {
        return text(g, font(), value, reference, cx, cy, size, maxWidth);
    }

    private static Shape text(Graphics2D g, Font base, String value, String reference, double cx, double cy,
                              double size, double maxWidth) {
        FontRenderContext frc = g.getFontRenderContext();
        Font f = base.deriveFont(100f);
        Rectangle2D ref = new TextLayout(reference, f, frc).getOutline(null).getBounds2D();
        Shape outline = new TextLayout(value, f, frc).getOutline(null);
        Rectangle2D bounds = outline.getBounds2D();
        double scale = size / ref.getHeight();
        double sx = Math.min(scale, maxWidth / Math.max(1, bounds.getWidth()));
        AffineTransform t = new AffineTransform();
        t.translate(cx, cy);
        t.scale(sx, scale);
        t.translate(-bounds.getCenterX(), -ref.getCenterY());
        return t.createTransformedShape(outline);
    }

    static int textColor(RecolorRequest req) {
        if (req.textColor() != null) {
            return RecolorRequest.rgb(req.textColor());
        }
        String shirt = req.zoneColors().get(RealisticModel.CAMISETA);
        int c = shirt == null ? 0xFFFFFF : RecolorRequest.rgb(shirt);
        return luminance(c) > 0.55 ? DARK_TEXT : LIGHT_TEXT;
    }

    private static double luminance(int rgb) {
        return (0.2126 * ((rgb >> 16) & 0xFF) + 0.7152 * ((rgb >> 8) & 0xFF) + 0.0722 * (rgb & 0xFF)) / 255;
    }

    private static final java.util.Map<String, Font> FONTS = new java.util.concurrent.ConcurrentHashMap<>();

    private static Font font(String key) {
        if (key == null || DesignFonts.DEFAULT.equals(key)) {
            return font();
        }
        return FONTS.computeIfAbsent(key, k -> {
            try (InputStream in = DesignOverlay.class.getResourceAsStream("/fonts/" + DesignFonts.file(k))) {
                return in == null ? font() : Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (IOException | java.awt.FontFormatException | RuntimeException e) {
                return font();
            }
        });
    }

    private static Font font() {
        Font f = font;
        if (f == null) {
            String file = DesignFonts.file(DesignFonts.DEFAULT);
            try (InputStream in = DesignOverlay.class.getResourceAsStream("/fonts/" + file)) {
                if (in == null) {
                    throw new IllegalStateException("Falta la fuente " + file);
                }
                f = Font.createFont(Font.TRUETYPE_FONT, in);
                font = f;
            } catch (IOException | java.awt.FontFormatException e) {
                throw new IllegalStateException("No se pudo cargar la fuente " + file, e);
            }
        }
        return f;
    }
}
