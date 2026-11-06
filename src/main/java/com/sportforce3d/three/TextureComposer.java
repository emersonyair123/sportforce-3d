package com.sportforce3d.three;

import com.sportforce3d.design.Area;
import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.DesignElement;
import com.sportforce3d.design.DesignFonts;
import com.sportforce3d.design.ElementType;
import com.sportforce3d.design.PatternSpec;
import com.sportforce3d.design.PatternType;
import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.UvRect;
import com.sportforce3d.three.model.UvLayout;
import com.sportforce3d.three.model.Zone;
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

/**
 * Compone la textura de cada prenda y la entrega como imagen para el diffuseMap:
 * <ol>
 *     <li>color base de cada zona en sus islas UV (con un margen de "sangrado" para que el filtrado de la
 *     textura no deje costuras de otro color);</li>
 *     <li>patrón (franjas, diagonales, degradado, cuadros, sublimado) en las zonas principales;</li>
 *     <li>elementos en orden de capa: nombre, número, emblemas y logos, recortados a su zona.</li>
 * </ol>
 * Cada zona dibuja en metros reales sobre la prenda (px por metro horizontal y vertical propios), así un número
 * no sale estirado aunque la isla del short sea alta y angosta.
 * <p>
 * Se dibuja con Java2D en memoria (CPU) y solo el resultado sube a la GPU. Un Canvas de JavaFX crea en la GPU una
 * máscara del tamaño de todo el lienzo por cada recorte; con texturas de 2048 px eso agotaba la memoria de vídeo
 * de JavaFX (512 MB) en gráficos integrados. Debe usarse en el hilo de JavaFX (escribe en WritableImage).
 */
public final class TextureComposer {

    /** Zonas que reciben el patrón de su prenda. */
    static final Map<Garment, Set<Zone>> PATTERNED = Map.of(
            Garment.JERSEY, Set.of(Zone.JERSEY_BODY, Zone.JERSEY_SLEEVES),
            Garment.SHORTS, Set.of(Zone.SHORTS_BASE),
            Garment.SOCKS, Set.of(Zone.SOCKS_LEG));

    /** Tamaño físico aproximado (ancho, alto en m) de cada isla con patrón. */
    private static final Map<UvRect, double[]> PHYSICAL = physical();
    private static final Map<String, Font> FONTS = new ConcurrentHashMap<>();
    /** Mismo valor que -sf-focus en tokens-dark.css (design/tokens.json): el resaltado del elemento elegido. */
    private static final Color FOCUS = new Color(0x5C, 0x9B, 0xFF);

    private final ElementImages images;
    private final Map<Garment, BufferedImage> rasters = new EnumMap<>(Garment.class);
    private final Map<Garment, WritableImage[]> buffers = new EnumMap<>(Garment.class);
    private final Map<Garment, Integer> flip = new EnumMap<>(Garment.class);
    /** Copia en Java2D de cada imagen de emblema o logo (se libera con la imagen). */
    private final Map<Image, BufferedImage> awtImages = new WeakHashMap<>();
    /** Caja de cada elemento en UV de su prenda: [u0, v0, u1, v1]. */
    private final Map<String, double[]> bounds = new HashMap<>();
    /** Elemento elegido en el diseñador: se dibuja con un contorno sutil (nunca en capturas ni en pedidos). */
    private String highlight;

    public TextureComposer(ElementImages images) {
        this.images = images;
    }

    /** Textura de 2048 px para camiseta y short; 1024 para medias y zapatillas (islas más pequeñas). */
    public static int size(Garment g) {
        return g == Garment.JERSEY || g == Garment.SHORTS ? 2048 : 1024;
    }

    public static List<Garment> garments() {
        return List.of(Garment.JERSEY, Garment.SHORTS, Garment.SOCKS, Garment.SHOES);
    }

    /** Dibuja la prenda y devuelve la imagen (se alternan dos imágenes para que JavaFX note el cambio). */
    public Image compose(Garment garment, DesignDocument doc) {
        int s = size(garment);
        BufferedImage raster = rasters.computeIfAbsent(garment, g -> new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB_PRE));
        Graphics2D g = raster.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setComposite(AlphaComposite.Src);

            // 1. Fondo y colores de zona.
            g.setColor(awt(doc.color(mainZone(garment))));
            g.fillRect(0, 0, s, s);
            g.setComposite(AlphaComposite.SrcOver);
            double bleed = s * 0.004;
            for (Zone zone : Zone.values()) {
                if (zone.garment() != garment) {
                    continue;
                }
                g.setColor(awt(doc.color(zone)));
                for (UvRect r : UvLayout.BY_ZONE.get(zone)) {
                    g.fill(new Rectangle2D.Double(r.u0() * s - bleed, r.v0() * s - bleed,
                            (r.u1() - r.u0()) * s + 2 * bleed, (r.v1() - r.v0()) * s + 2 * bleed));
                }
            }

            // 2. Patrón.
            PatternSpec pattern = doc.pattern(garment);
            if (pattern.type() != PatternType.LISO) {
                for (Zone zone : PATTERNED.getOrDefault(garment, Set.of())) {
                    Color base = awt(doc.color(zone));
                    for (UvRect r : UvLayout.BY_ZONE.get(zone)) {
                        drawPattern(g, s, bleed, r, PHYSICAL.getOrDefault(r, new double[]{0.4, 0.4}), pattern, base);
                    }
                }
            }

            // 3. Elementos en orden de capa.
            bounds.keySet().removeIf(id -> doc.element(id).map(e -> e.area().garment() == garment).orElse(true));
            for (DesignElement e : doc.elements()) {
                if (e.area().garment() == garment && e.visible()) {
                    drawElement(g, s, e);
                }
            }
        } finally {
            g.dispose();
        }

        WritableImage[] pair = buffers.computeIfAbsent(garment, x -> new WritableImage[]{
                new WritableImage(s, s), new WritableImage(s, s)});
        int i = flip.merge(garment, 1, (a, b) -> 1 - a);
        int[] data = ((DataBufferInt) raster.getRaster().getDataBuffer()).getData();
        pair[i].getPixelWriter().setPixels(0, 0, s, s, PixelFormat.getIntArgbPreInstance(), data, 0, s);
        return pair[i];
    }

    /** Elemento visible más alto (última capa) bajo la coordenada UV, o null. */
    public String hit(Garment garment, double u, double v, DesignDocument doc) {
        List<DesignElement> list = doc.elements();
        for (int i = list.size() - 1; i >= 0; i--) {
            DesignElement e = list.get(i);
            double[] b = bounds.get(e.id());
            if (e.visible() && e.area().garment() == garment && b != null
                    && u >= b[0] && u <= b[2] && v >= b[1] && v <= b[3]) {
                return e.id();
            }
        }
        return null;
    }

    public double[] bounds(String elementId) {
        return bounds.get(elementId);
    }

    /** Elemento a resaltar en la próxima composición (null = ninguno). */
    public void setHighlight(String elementId) {
        this.highlight = elementId;
    }

    public String highlight() {
        return highlight;
    }

    // ---------------------------------------------------------------- Patrones

    private static void drawPattern(Graphics2D g0, int s, double bleed, UvRect r, double[] phys,
                                    PatternSpec p, Color base) {
        double x0 = r.u0() * s - bleed;
        double y0 = r.v0() * s - bleed;
        double w = (r.u1() - r.u0()) * s + 2 * bleed;
        double h = (r.v1() - r.v0()) * s + 2 * bleed;
        double ax = (r.u1() - r.u0()) * s / phys[0];
        double ay = (r.v1() - r.v0()) * s / phys[1];
        double cx = (r.u0() + r.u1()) / 2 * s;
        double cy = (r.v0() + r.v1()) / 2 * s;
        Color c2 = awt(p.color());
        double m = p.width();

        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.clip(new Rectangle2D.Double(x0, y0, w, h));
            g.setColor(c2);
            switch (p.type()) {
                case FRANJAS_VERTICALES -> {
                    double sw = m * ax;
                    for (double x = cx - sw / 2 - Math.ceil(w / (2 * sw) + 1) * 2 * sw; x < x0 + w; x += 2 * sw) {
                        g.fill(new Rectangle2D.Double(x, y0, sw, h));
                    }
                }
                case FRANJAS_HORIZONTALES -> {
                    double sh = m * ay;
                    for (double y = cy - sh / 2 - Math.ceil(h / (2 * sh) + 1) * 2 * sh; y < y0 + h; y += 2 * sh) {
                        g.fill(new Rectangle2D.Double(x0, y, w, sh));
                    }
                }
                case DIAGONALES -> {
                    physicalFrame(g, cx, cy, ax, ay);
                    g.rotate(Math.toRadians(-45));
                    double d = phys[0] + phys[1];
                    for (double t = -d - m / 2; t < d; t += 2 * m) {
                        g.fill(new Rectangle2D.Double(t, -d, m, 2 * d));
                    }
                }
                case CUADROS -> {
                    physicalFrame(g, cx, cy, ax, ay);
                    int nx = (int) Math.ceil(phys[0] / m) + 2;
                    int ny = (int) Math.ceil(phys[1] / m) + 2;
                    Path2D.Double squares = new Path2D.Double();
                    for (int i = -nx; i <= nx; i++) {
                        for (int j = -ny; j <= ny; j++) {
                            if (Math.floorMod(i + j, 2) == 0) {
                                squares.append(new Rectangle2D.Double(i * m - m / 2, j * m - m / 2, m, m), false);
                            }
                        }
                    }
                    g.fill(squares);
                }
                case DEGRADADO -> {
                    g.setPaint(new LinearGradientPaint((float) x0, (float) y0, (float) x0, (float) (y0 + h),
                            new float[]{0f, 0.15f, 1f}, new Color[]{base, base, c2}));
                    g.fill(new Rectangle2D.Double(x0, y0, w, h));
                }
                case SUBLIMADO -> {
                    // Triángulos facetados en dos tonos del segundo color: sublimado geométrico.
                    physicalFrame(g, cx, cy, ax, ay);
                    int nx = (int) Math.ceil(phys[0] / m) + 2;
                    int ny = (int) Math.ceil(phys[1] / m) + 2;
                    Color light = mix(c2, base, 0.35);
                    for (int i = -nx; i <= nx; i++) {
                        for (int j = -ny; j <= ny; j++) {
                            if (Math.floorMod(i * 3 + j * 5, 4) == 0) {
                                continue; // huecos que dejan ver el color base
                            }
                            double x = i * m;
                            double y = j * m;
                            boolean flipT = Math.floorMod(i * 7 + j * 3, 5) < 2;
                            Path2D.Double tri = new Path2D.Double();
                            tri.moveTo(flipT ? x : x + m, y);
                            tri.lineTo(x + m, y + m);
                            tri.lineTo(x, y + m);
                            tri.closePath();
                            g.setColor(Math.floorMod(i + j * 2, 3) == 0 ? light : c2);
                            g.fill(tri);
                        }
                    }
                }
                case LISO -> {
                }
            }
        } finally {
            g.dispose();
        }
    }

    /** Marco en metros centrado en la isla: después de esto, 1 unidad = 1 m sobre la prenda. */
    private static void physicalFrame(Graphics2D g, double cx, double cy, double ax, double ay) {
        g.translate(cx, cy);
        g.scale(ax, ay);
    }

    // ---------------------------------------------------------------- Elementos

    private void drawElement(Graphics2D g0, int s, DesignElement e) {
        Area area = e.area();
        UvRect r = area.rect();
        double ax = area.pxPerMeterX(s);
        double ay = area.pxPerMeterY(s);
        double cx = (r.u0() + e.u() * (r.u1() - r.u0())) * s;
        double cy = (r.v0() + e.v() * (r.v1() - r.v0())) * s;
        double hPx = e.height() * e.scale() * ay;
        double wPx;

        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.clip(new Rectangle2D.Double(r.u0() * s, r.v0() * s, (r.u1() - r.u0()) * s, (r.v1() - r.v0()) * s));
            g.translate(cx, cy);
            g.scale(ax / ay, 1);
            g.rotate(Math.toRadians(e.rotation()));

            if (e.type().isText()) {
                String text = e.text() == null ? "" : e.text();
                // El tamaño se refiere a la altura de las mayúsculas (~72 % del cuerpo de la fuente).
                Font font = font(e.font()).deriveFont((float) (hPx / 0.72));
                FontRenderContext frc = g.getFontRenderContext();
                double capHeight = -new TextLayout("H", font, frc).getBounds().getY();
                double stroke = e.outline() * e.scale() * ay * 2;
                if (text.isEmpty()) {
                    wPx = capHeight;
                } else {
                    TextLayout layout = new TextLayout(text, font, frc);
                    wPx = layout.getAdvance();
                    // Centrado horizontal por avance y vertical por la altura de las mayúsculas.
                    Shape outline = layout.getOutline(AffineTransform.getTranslateInstance(-wPx / 2, capHeight / 2));
                    if (stroke > 0 && e.accentColor() != null) {
                        g.setColor(awt(e.accentColor()));
                        g.setStroke(new BasicStroke((float) stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.draw(outline);
                    }
                    g.setColor(awt(e.color() == null ? "#FFFFFF" : e.color()));
                    g.fill(outline);
                }
                wPx += stroke;
                hPx = hPx * 1.15 + stroke;
            } else {
                Image img = e.type() == ElementType.EMBLEM
                        ? images.emblem(e.emblem(), e.color(), e.accentColor())
                        : images.logo(e.assetPath());
                BufferedImage awtImg = img == null ? null : toAwt(img);
                if (awtImg == null) {
                    return;
                }
                wPx = (double) awtImg.getWidth() / awtImg.getHeight() * hPx;
                AffineTransform at = new AffineTransform();
                at.translate(-wPx / 2, -hPx / 2);
                at.scale(wPx / awtImg.getWidth(), hPx / awtImg.getHeight());
                g.drawImage(awtImg, at, null);
            }
            if (e.id().equals(highlight)) {
                drawHighlight(g, wPx, hPx, s);
            }
        } finally {
            g.dispose();
        }

        // Caja alineada a los ejes del rectángulo girado y estirado, en UV.
        double rad = Math.toRadians(e.rotation());
        double cos = Math.abs(Math.cos(rad));
        double sin = Math.abs(Math.sin(rad));
        double bw = (wPx * cos + hPx * sin) * (ax / ay) / 2;
        double bh = (wPx * sin + hPx * cos) / 2;
        bounds.put(e.id(), new double[]{(cx - bw) / s, (cy - bh) / s, (cx + bw) / s, (cy + bh) / s});
    }

    /**
     * Contorno redondeado en el color de foco con un halo suave, en el mismo marco girado del elemento: sigue su
     * rotación y la curva de la prenda.
     */
    private static void drawHighlight(Graphics2D g, double w, double h, int s) {
        double pad = s * 0.006;
        java.awt.geom.RoundRectangle2D box = new java.awt.geom.RoundRectangle2D.Double(-w / 2 - pad, -h / 2 - pad,
                w + 2 * pad, h + 2 * pad, pad * 2, pad * 2);
        float line = (float) (s * 0.0018);
        g.setColor(new Color(FOCUS.getRed(), FOCUS.getGreen(), FOCUS.getBlue(), 70));
        g.setStroke(new BasicStroke(line * 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(box);
        g.setColor(FOCUS);
        g.setStroke(new BasicStroke(line, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                new float[]{line * 5f, line * 3f}, 0f));
        g.draw(box);
    }

    /** Copia los píxeles de una imagen de JavaFX (ya cargada) a una imagen de Java2D. */
    private BufferedImage toAwt(Image img) {
        BufferedImage cached = awtImages.get(img);
        if (cached != null) {
            return cached;
        }
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        if (w <= 0 || h <= 0 || img.getPixelReader() == null) {
            return null;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] data = ((DataBufferInt) out.getRaster().getDataBuffer()).getData();
        img.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbPreInstance(), data, 0, w);
        awtImages.put(img, out);
        return out;
    }

    /** Fuente de Java2D cargada desde /fonts (las mismas TTF que usa la interfaz). */
    static Font font(String key) {
        return FONTS.computeIfAbsent(DesignFonts.file(key), file -> {
            try (InputStream in = TextureComposer.class.getResourceAsStream("/fonts/" + file)) {
                if (in == null) {
                    throw new IllegalStateException("Falta la fuente " + file);
                }
                return Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (java.io.IOException | java.awt.FontFormatException ex) {
                throw new IllegalStateException("No se pudo cargar la fuente " + file, ex);
            }
        });
    }

    static Color awt(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return new Color(Integer.parseInt(h, 16));
        } catch (RuntimeException e) {
            return Color.GRAY;
        }
    }

    private static Color mix(Color a, Color b, double t) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static Zone mainZone(Garment g) {
        return switch (g) {
            case SHORTS -> Zone.SHORTS_BASE;
            case SOCKS -> Zone.SOCKS_LEG;
            case SHOES -> Zone.SHOE_UPPER;
            default -> Zone.JERSEY_BODY;
        };
    }

    private static Map<UvRect, double[]> physical() {
        Map<UvRect, double[]> m = new HashMap<>();
        m.put(UvLayout.JERSEY_FRONT, new double[]{0.47, 0.58});
        m.put(UvLayout.JERSEY_BACK, new double[]{0.47, 0.58});
        m.put(UvLayout.JERSEY_SLEEVE_LEFT, new double[]{0.40, 0.17});
        m.put(UvLayout.JERSEY_SLEEVE_RIGHT, new double[]{0.40, 0.17});
        // Antebrazo de la manga larga: el largo nominal con que lo mapea tools/blender/camiseta.py (ANTEBRAZO_H).
        m.put(UvLayout.JERSEY_FOREARM_LEFT, new double[]{0.40, 0.36});
        m.put(UvLayout.JERSEY_FOREARM_RIGHT, new double[]{0.40, 0.36});
        for (UvRect r : List.of(UvLayout.SHORTS_LEFT_FRONT, UvLayout.SHORTS_LEFT_BACK, UvLayout.SHORTS_RIGHT_FRONT,
                UvLayout.SHORTS_RIGHT_BACK)) {
            m.put(r, new double[]{0.36, 0.35});
        }
        m.put(UvLayout.SOCK_LEFT, new double[]{0.36, 0.37});
        m.put(UvLayout.SOCK_RIGHT, new double[]{0.36, 0.37});
        return m;
    }
}
