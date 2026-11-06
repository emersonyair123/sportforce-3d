package com.sportforce3d.three;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.w3c.dom.Node.TEXT_NODE;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lector SVG mínimo para emblemas y logos simples: path, circle, ellipse, rect, polygon, polyline, line, text
 * (con tspan) y g, con fill, stroke, opacidad y transformaciones básicas. No ejecuta scripts ni carga recursos
 * externos (XXE desactivado). Si un logo usa algo no soportado (degradados, filtros), se ignora esa parte;
 * para logos complejos conviene subir un PNG.
 */
public final class SvgLoader {

    /** Una figura ya resuelta (estilos heredados aplicados). */
    public record SvgShape(String kind, Map<String, String> attrs, String fill, String stroke, double strokeWidth,
                           double opacity, String transform, List<SvgShape> spans) {
    }

    /** Documento: caja de vista y figuras en orden de dibujo. */
    public record SvgDoc(double minX, double minY, double width, double height, List<SvgShape> shapes) {
    }

    private SvgLoader() {
    }

    // ---------------------------------------------------------------- Lectura (sin JavaFX)

    public static SvgDoc parse(InputStream in) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            f.setNamespaceAware(false);
            DocumentBuilder builder = f.newDocumentBuilder();
            Document doc = builder.parse(in);
            Element root = doc.getDocumentElement();
            if (!"svg".equals(local(root.getTagName()))) {
                throw new IllegalArgumentException("El archivo no es un SVG.");
            }
            double[] box = viewBox(root);
            List<SvgShape> shapes = new ArrayList<>();
            Style style = new Style("#000000", null, 1, 1);
            collect(root, style, "", shapes);
            if (shapes.isEmpty()) {
                throw new IllegalArgumentException("El SVG no tiene figuras compatibles. Usa un PNG.");
            }
            return new SvgDoc(box[0], box[1], box[2], box[3], shapes);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el SVG: " + e.getMessage(), e);
        }
    }

    private record Style(String fill, String stroke, double strokeWidth, double opacity) {
    }

    private static void collect(Element el, Style inherited, String transform, List<SvgShape> out) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element child)) {
                continue;
            }
            String tag = local(child.getTagName());
            Style s = style(child, inherited);
            String t = (transform + " " + child.getAttribute("transform")).trim();
            switch (tag) {
                case "g", "a" -> collect(child, s, t, out);
                case "path", "circle", "ellipse", "rect", "polygon", "polyline", "line" ->
                        out.add(new SvgShape(tag, attrs(child), s.fill, s.stroke, s.strokeWidth, s.opacity, t, List.of()));
                case "text" -> out.add(text(child, s, t));
                default -> {
                    // defs, style, title, metadata, linearGradient…: no se dibujan.
                }
            }
        }
    }

    private static SvgShape text(Element el, Style s, String transform) {
        List<SvgShape> spans = new ArrayList<>();
        NodeList nodes = el.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node n = nodes.item(i);
            if (n.getNodeType() == TEXT_NODE && !n.getTextContent().isBlank()) {
                spans.add(new SvgShape("span", Map.of("text", n.getTextContent().strip()), s.fill, null, 0, 1, "", List.of()));
            } else if (n instanceof Element tspan && "tspan".equals(local(tspan.getTagName()))) {
                Style ts = style(tspan, s);
                spans.add(new SvgShape("span", Map.of("text", tspan.getTextContent()), ts.fill, null, 0, 1, "", List.of()));
            }
        }
        return new SvgShape("text", attrs(el), s.fill, s.stroke, s.strokeWidth, s.opacity, transform, spans);
    }

    private static Style style(Element el, Style parent) {
        Map<String, String> css = new HashMap<>();
        for (String decl : el.getAttribute("style").split(";")) {
            int k = decl.indexOf(':');
            if (k > 0) {
                css.put(decl.substring(0, k).trim(), decl.substring(k + 1).trim());
            }
        }
        String fill = pick(el, css, "fill", parent.fill);
        String stroke = pick(el, css, "stroke", parent.stroke);
        String sw = pick(el, css, "stroke-width", null);
        String op = pick(el, css, "opacity", null);
        String fop = pick(el, css, "fill-opacity", null);
        double opacity = parent.opacity * (op == null ? 1 : num(op)) * (fop == null ? 1 : num(fop));
        return new Style(fill, stroke, sw == null ? parent.strokeWidth : num(sw), opacity);
    }

    private static String pick(Element el, Map<String, String> css, String name, String fallback) {
        if (css.containsKey(name)) {
            return css.get(name);
        }
        String a = el.getAttribute(name);
        return a == null || a.isEmpty() ? fallback : a;
    }

    private static Map<String, String> attrs(Element el) {
        Map<String, String> m = new HashMap<>();
        var list = el.getAttributes();
        for (int i = 0; i < list.getLength(); i++) {
            m.put(list.item(i).getNodeName(), list.item(i).getNodeValue());
        }
        return m;
    }

    private static double[] viewBox(Element root) {
        String vb = root.getAttribute("viewBox");
        if (vb != null && !vb.isBlank()) {
            String[] p = vb.trim().split("[\\s,]+");
            return new double[]{num(p[0]), num(p[1]), num(p[2]), num(p[3])};
        }
        double w = root.getAttribute("width").isEmpty() ? 100 : num(root.getAttribute("width"));
        double h = root.getAttribute("height").isEmpty() ? 100 : num(root.getAttribute("height"));
        return new double[]{0, 0, w, h};
    }

    private static String local(String tag) {
        int k = tag.indexOf(':');
        return (k >= 0 ? tag.substring(k + 1) : tag).toLowerCase(Locale.ROOT);
    }

    static double num(String s) {
        if (s == null || s.isBlank()) {
            return 0;
        }
        return Double.parseDouble(s.trim().replaceAll("(px|pt|%)$", ""));
    }

    // ---------------------------------------------------------------- Dibujo (hilo de JavaFX)

    /**
     * Figuras de JavaFX del SVG. {@code recolor} reemplaza colores exactos (HEX en mayúsculas), p. ej. para pintar
     * un emblema con los colores del uniforme.
     */
    public static Group toNode(SvgDoc doc, Map<String, String> recolor) {
        Group g = new Group();
        for (SvgShape s : doc.shapes()) {
            Node n = node(s, recolor);
            if (n != null) {
                g.getChildren().add(n);
            }
        }
        return g;
    }

    /** Imagen del SVG con fondo transparente; el lado mayor mide {@code px}. */
    public static Image render(SvgDoc doc, int px, Map<String, String> recolor) {
        Group shapes = toNode(doc, recolor);
        double k = px / Math.max(doc.width(), doc.height());
        shapes.getTransforms().setAll(new Scale(k, k), new Translate(-doc.minX(), -doc.minY()));
        Group holder = new Group(shapes);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(0, 0, doc.width() * k, doc.height() * k));
        return holder.snapshot(params, null);
    }

    private static Node node(SvgShape s, Map<String, String> recolor) {
        Map<String, String> a = s.attrs();
        Node n = switch (s.kind()) {
            case "path" -> {
                SVGPath p = new SVGPath();
                p.setContent(a.getOrDefault("d", ""));
                if ("evenodd".equals(a.get("fill-rule"))) {
                    p.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
                }
                yield p;
            }
            case "circle" -> new Circle(num(a.get("cx")), num(a.get("cy")), num(a.get("r")));
            case "ellipse" -> new Ellipse(num(a.get("cx")), num(a.get("cy")), num(a.get("rx")), num(a.get("ry")));
            case "rect" -> {
                Rectangle r = new Rectangle(num(a.get("x")), num(a.get("y")), num(a.get("width")), num(a.get("height")));
                double rx = a.containsKey("rx") ? num(a.get("rx")) : num(a.get("ry"));
                double ry = a.containsKey("ry") ? num(a.get("ry")) : rx;
                r.setArcWidth(rx * 2);
                r.setArcHeight(ry * 2);
                yield r;
            }
            case "polygon" -> new Polygon(points(a.get("points")));
            case "polyline" -> new Polyline(points(a.get("points")));
            case "line" -> new Line(num(a.get("x1")), num(a.get("y1")), num(a.get("x2")), num(a.get("y2")));
            case "text" -> text(s, recolor);
            default -> null;
        };
        if (n instanceof Shape shape) {
            shape.setFill("polyline".equals(s.kind()) || "line".equals(s.kind()) ? null : paint(s.fill(), recolor));
            shape.setStroke(paint(s.stroke(), recolor));
            shape.setStrokeWidth(s.strokeWidth());
            shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
            shape.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        if (n != null) {
            n.setOpacity(s.opacity());
            if (!s.transform().isBlank()) {
                n.getTransforms().add(transform(s.transform()));
            }
        }
        return n;
    }

    private static Node text(SvgShape s, Map<String, String> recolor) {
        Map<String, String> a = s.attrs();
        double size = a.containsKey("font-size") ? num(a.get("font-size")) : 16;
        boolean bold = a.getOrDefault("font-weight", "").matches("bold|[6-9]00");
        Font font = Font.font(bold ? "Barlow Condensed ExtraBold" : "Barlow", bold ? FontWeight.BOLD : FontWeight.NORMAL, size);
        TextFlow flow = new TextFlow();
        for (SvgShape span : s.spans()) {
            Text t = new Text(span.attrs().get("text"));
            t.setFont(font);
            t.setFill(paint(span.fill(), recolor));
            flow.getChildren().add(t);
        }
        flow.setTextAlignment(TextAlignment.LEFT);
        double width = flow.prefWidth(-1);
        double x = num(a.get("x"));
        String anchor = a.getOrDefault("text-anchor", "start");
        if ("middle".equals(anchor)) {
            x -= width / 2;
        } else if ("end".equals(anchor)) {
            x -= width;
        }
        flow.setLayoutX(x);
        flow.setLayoutY(num(a.get("y")) - size * 0.8);
        return flow;
    }

    static Color paint(String value, Map<String, String> recolor) {
        if (value == null || value.isBlank() || "none".equalsIgnoreCase(value) || value.startsWith("url(")) {
            return null;
        }
        try {
            Color c = Color.web(value.trim());
            String hex = String.format("#%02X%02X%02X", (int) Math.round(c.getRed() * 255),
                    (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
            String replacement = recolor == null ? null : recolor.get(hex);
            return replacement == null ? c : Color.web(replacement);
        } catch (IllegalArgumentException e) {
            return Color.BLACK;
        }
    }

    private static double[] points(String s) {
        if (s == null) {
            return new double[0];
        }
        String[] p = s.trim().split("[\\s,]+");
        double[] out = new double[p.length - p.length % 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = num(p[i]);
        }
        return out;
    }

    private static final Pattern FN = Pattern.compile("(matrix|translate|scale|rotate)\\s*\\(([^)]*)\\)");

    static Affine transform(String spec) {
        Affine total = new Affine();
        Matcher m = FN.matcher(spec);
        while (m.find()) {
            String[] p = m.group(2).trim().split("[\\s,]+");
            double[] v = new double[p.length];
            for (int i = 0; i < p.length; i++) {
                v[i] = p[i].isEmpty() ? 0 : num(p[i]);
            }
            switch (m.group(1)) {
                case "matrix" -> total.append(new Affine(v[0], v[2], v[4], v[1], v[3], v[5]));
                case "translate" -> total.appendTranslation(v[0], v.length > 1 ? v[1] : 0);
                case "scale" -> total.appendScale(v[0], v.length > 1 ? v[1] : v[0]);
                case "rotate" -> {
                    if (v.length >= 3) {
                        total.appendRotation(v[0], v[1], v[2]);
                    } else {
                        total.appendRotation(v[0]);
                    }
                }
                default -> {
                }
            }
        }
        return total;
    }
}
