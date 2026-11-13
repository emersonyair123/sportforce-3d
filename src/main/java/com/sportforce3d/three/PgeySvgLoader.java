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

public final class PgeySvgLoader {
    public record PgeySvgShape(String pgeyKind, Map<String, String> pgeyAttrs, String pgeyFill, String pgeyStroke, double pgeyStrokeWidth,
                           double pgeyOpacity, String pgeyTransform, List<PgeySvgShape> pgeySpans) {
    }

    public record PgeySvgDoc(double pgeyMinX, double pgeyMinY, double pgeyWidth, double pgeyHeight, List<PgeySvgShape> pgeyShapes) {
    }

    private PgeySvgLoader() {
    }

    public static PgeySvgDoc pgeyParse(InputStream pgeyIn) {
        try {
            DocumentBuilderFactory pgeyF = DocumentBuilderFactory.newInstance();
            pgeyF.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            pgeyF.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            pgeyF.setFeature("http://xml.org/sax/features/external-general-entities", false);
            pgeyF.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            pgeyF.setXIncludeAware(false);
            pgeyF.setExpandEntityReferences(false);
            pgeyF.setNamespaceAware(false);
            DocumentBuilder pgeyBuilder = pgeyF.newDocumentBuilder();
            Document pgeyDoc = pgeyBuilder.parse(pgeyIn);
            Element pgeyRoot = pgeyDoc.getDocumentElement();
            if (!"svg".equals(pgeyLocal(pgeyRoot.getTagName()))) {
                throw new IllegalArgumentException("El archivo no es un SVG.");
            }
            double[] pgeyBox = pgeyViewBox(pgeyRoot);
            List<PgeySvgShape> pgeyShapes = new ArrayList<>();
            PgeyStyle pgeyStyle = new PgeyStyle("#000000", null, 1, 1);
            pgeyCollect(pgeyRoot, pgeyStyle, "", pgeyShapes);
            if (pgeyShapes.isEmpty()) {
                throw new IllegalArgumentException("El SVG no tiene figuras compatibles. Usa un PNG.");
            }
            return new PgeySvgDoc(pgeyBox[0], pgeyBox[1], pgeyBox[2], pgeyBox[3], pgeyShapes);
        } catch (IllegalArgumentException pgeyE) {
            throw pgeyE;
        } catch (Exception pgeyE) {
            throw new IllegalArgumentException("No se pudo leer el SVG: " + pgeyE.getMessage(), pgeyE);
        }
    }

    private record PgeyStyle(String pgeyFill, String pgeyStroke, double pgeyStrokeWidth, double pgeyOpacity) {
    }

    private static void pgeyCollect(Element pgeyEl, PgeyStyle pgeyInherited, String pgeyTransform, List<PgeySvgShape> pgeyOut) {
        NodeList pgeyChildren = pgeyEl.getChildNodes();
        for (int pgeyI = 0; pgeyI < pgeyChildren.getLength(); pgeyI++) {
            if (!(pgeyChildren.item(pgeyI) instanceof Element pgeyChild)) {
                continue;
            }
            String pgeyTag = pgeyLocal(pgeyChild.getTagName());
            PgeyStyle pgeyS = pgeyStyle(pgeyChild, pgeyInherited);
            String pgeyT = (pgeyTransform + " " + pgeyChild.getAttribute("transform")).trim();
            switch (pgeyTag) {
                case "g", "a" -> pgeyCollect(pgeyChild, pgeyS, pgeyT, pgeyOut);
                case "path", "circle", "ellipse", "rect", "polygon", "polyline", "line" ->
                        pgeyOut.add(new PgeySvgShape(pgeyTag, pgeyAttrs(pgeyChild), pgeyS.pgeyFill, pgeyS.pgeyStroke, pgeyS.pgeyStrokeWidth, pgeyS.pgeyOpacity, pgeyT, List.of()));
                case "text" -> pgeyOut.add(pgeyText(pgeyChild, pgeyS, pgeyT));
                default -> {
                }
            }
        }
    }

    private static PgeySvgShape pgeyText(Element pgeyEl, PgeyStyle pgeyS, String pgeyTransform) {
        List<PgeySvgShape> pgeySpans = new ArrayList<>();
        NodeList pgeyNodes = pgeyEl.getChildNodes();
        for (int pgeyI = 0; pgeyI < pgeyNodes.getLength(); pgeyI++) {
            org.w3c.dom.Node pgeyN = pgeyNodes.item(pgeyI);
            if (pgeyN.getNodeType() == TEXT_NODE && !pgeyN.getTextContent().isBlank()) {
                pgeySpans.add(new PgeySvgShape("span", Map.of("text", pgeyN.getTextContent().strip()), pgeyS.pgeyFill, null, 0, 1, "", List.of()));
            } else if (pgeyN instanceof Element pgeyTspan && "tspan".equals(pgeyLocal(pgeyTspan.getTagName()))) {
                PgeyStyle pgeyTs = pgeyStyle(pgeyTspan, pgeyS);
                pgeySpans.add(new PgeySvgShape("span", Map.of("text", pgeyTspan.getTextContent()), pgeyTs.pgeyFill, null, 0, 1, "", List.of()));
            }
        }
        return new PgeySvgShape("text", pgeyAttrs(pgeyEl), pgeyS.pgeyFill, pgeyS.pgeyStroke, pgeyS.pgeyStrokeWidth, pgeyS.pgeyOpacity, pgeyTransform, pgeySpans);
    }

    private static PgeyStyle pgeyStyle(Element pgeyEl, PgeyStyle pgeyParent) {
        Map<String, String> pgeyCss = new HashMap<>();
        for (String pgeyDecl : pgeyEl.getAttribute("style").split(";")) {
            int pgeyK = pgeyDecl.indexOf(':');
            if (pgeyK > 0) {
                pgeyCss.put(pgeyDecl.substring(0, pgeyK).trim(), pgeyDecl.substring(pgeyK + 1).trim());
            }
        }
        String pgeyFill = pgeyPick(pgeyEl, pgeyCss, "fill", pgeyParent.pgeyFill);
        String pgeyStroke = pgeyPick(pgeyEl, pgeyCss, "stroke", pgeyParent.pgeyStroke);
        String pgeySw = pgeyPick(pgeyEl, pgeyCss, "stroke-width", null);
        String pgeyOp = pgeyPick(pgeyEl, pgeyCss, "opacity", null);
        String pgeyFop = pgeyPick(pgeyEl, pgeyCss, "fill-opacity", null);
        double pgeyOpacity = pgeyParent.pgeyOpacity * (pgeyOp == null ? 1 : pgeyNum(pgeyOp)) * (pgeyFop == null ? 1 : pgeyNum(pgeyFop));
        return new PgeyStyle(pgeyFill, pgeyStroke, pgeySw == null ? pgeyParent.pgeyStrokeWidth : pgeyNum(pgeySw), pgeyOpacity);
    }

    private static String pgeyPick(Element pgeyEl, Map<String, String> pgeyCss, String pgeyName, String pgeyFallback) {
        if (pgeyCss.containsKey(pgeyName)) {
            return pgeyCss.get(pgeyName);
        }
        String pgeyA = pgeyEl.getAttribute(pgeyName);
        return pgeyA == null || pgeyA.isEmpty() ? pgeyFallback : pgeyA;
    }

    private static Map<String, String> pgeyAttrs(Element pgeyEl) {
        Map<String, String> pgeyM = new HashMap<>();
        var pgeyList = pgeyEl.getAttributes();
        for (int pgeyI = 0; pgeyI < pgeyList.getLength(); pgeyI++) {
            pgeyM.put(pgeyList.item(pgeyI).getNodeName(), pgeyList.item(pgeyI).getNodeValue());
        }
        return pgeyM;
    }

    private static double[] pgeyViewBox(Element pgeyRoot) {
        String pgeyVb = pgeyRoot.getAttribute("viewBox");
        if (pgeyVb != null && !pgeyVb.isBlank()) {
            String[] pgeyP = pgeyVb.trim().split("[\\s,]+");
            return new double[]{pgeyNum(pgeyP[0]), pgeyNum(pgeyP[1]), pgeyNum(pgeyP[2]), pgeyNum(pgeyP[3])};
        }
        double pgeyW = pgeyRoot.getAttribute("width").isEmpty() ? 100 : pgeyNum(pgeyRoot.getAttribute("width"));
        double pgeyH = pgeyRoot.getAttribute("height").isEmpty() ? 100 : pgeyNum(pgeyRoot.getAttribute("height"));
        return new double[]{0, 0, pgeyW, pgeyH};
    }

    private static String pgeyLocal(String pgeyTag) {
        int pgeyK = pgeyTag.indexOf(':');
        return (pgeyK >= 0 ? pgeyTag.substring(pgeyK + 1) : pgeyTag).toLowerCase(Locale.ROOT);
    }

    static double pgeyNum(String pgeyS) {
        if (pgeyS == null || pgeyS.isBlank()) {
            return 0;
        }
        return Double.parseDouble(pgeyS.trim().replaceAll("(px|pt|%)$", ""));
    }

    public static Group pgeyToNode(PgeySvgDoc pgeyDoc, Map<String, String> pgeyRecolor) {
        Group pgeyG = new Group();
        for (PgeySvgShape pgeyS : pgeyDoc.pgeyShapes()) {
            Node pgeyN = pgeyNode(pgeyS, pgeyRecolor);
            if (pgeyN != null) {
                pgeyG.getChildren().add(pgeyN);
            }
        }
        return pgeyG;
    }

    public static Image pgeyRender(PgeySvgDoc pgeyDoc, int pgeyPx, Map<String, String> pgeyRecolor) {
        Group pgeyShapes = pgeyToNode(pgeyDoc, pgeyRecolor);
        double pgeyK = pgeyPx / Math.max(pgeyDoc.pgeyWidth(), pgeyDoc.pgeyHeight());
        pgeyShapes.getTransforms().setAll(new Scale(pgeyK, pgeyK), new Translate(-pgeyDoc.pgeyMinX(), -pgeyDoc.pgeyMinY()));
        Group pgeyHolder = new Group(pgeyShapes);
        SnapshotParameters pgeyParams = new SnapshotParameters();
        pgeyParams.setFill(Color.TRANSPARENT);
        pgeyParams.setViewport(new Rectangle2D(0, 0, pgeyDoc.pgeyWidth() * pgeyK, pgeyDoc.pgeyHeight() * pgeyK));
        return pgeyHolder.snapshot(pgeyParams, null);
    }

    private static Node pgeyNode(PgeySvgShape pgeyS, Map<String, String> pgeyRecolor) {
        Map<String, String> pgeyA = pgeyS.pgeyAttrs();
        Node pgeyN = switch (pgeyS.pgeyKind()) {
            case "path" -> {
                SVGPath pgeyP = new SVGPath();
                pgeyP.setContent(pgeyA.getOrDefault("d", ""));
                if ("evenodd".equals(pgeyA.get("fill-rule"))) {
                    pgeyP.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
                }
                yield pgeyP;
            }
            case "circle" -> new Circle(pgeyNum(pgeyA.get("cx")), pgeyNum(pgeyA.get("cy")), pgeyNum(pgeyA.get("r")));
            case "ellipse" -> new Ellipse(pgeyNum(pgeyA.get("cx")), pgeyNum(pgeyA.get("cy")), pgeyNum(pgeyA.get("rx")), pgeyNum(pgeyA.get("ry")));
            case "rect" -> {
                Rectangle pgeyR = new Rectangle(pgeyNum(pgeyA.get("x")), pgeyNum(pgeyA.get("y")), pgeyNum(pgeyA.get("width")), pgeyNum(pgeyA.get("height")));
                double pgeyRx = pgeyA.containsKey("rx") ? pgeyNum(pgeyA.get("rx")) : pgeyNum(pgeyA.get("ry"));
                double pgeyRy = pgeyA.containsKey("ry") ? pgeyNum(pgeyA.get("ry")) : pgeyRx;
                pgeyR.setArcWidth(pgeyRx * 2);
                pgeyR.setArcHeight(pgeyRy * 2);
                yield pgeyR;
            }
            case "polygon" -> new Polygon(pgeyPoints(pgeyA.get("points")));
            case "polyline" -> new Polyline(pgeyPoints(pgeyA.get("points")));
            case "line" -> new Line(pgeyNum(pgeyA.get("x1")), pgeyNum(pgeyA.get("y1")), pgeyNum(pgeyA.get("x2")), pgeyNum(pgeyA.get("y2")));
            case "text" -> pgeyText(pgeyS, pgeyRecolor);
            default -> null;
        };
        if (pgeyN instanceof Shape pgeyShape) {
            pgeyShape.setFill("polyline".equals(pgeyS.pgeyKind()) || "line".equals(pgeyS.pgeyKind()) ? null : pgeyPaint(pgeyS.pgeyFill(), pgeyRecolor));
            pgeyShape.setStroke(pgeyPaint(pgeyS.pgeyStroke(), pgeyRecolor));
            pgeyShape.setStrokeWidth(pgeyS.pgeyStrokeWidth());
            pgeyShape.setStrokeLineJoin(StrokeLineJoin.ROUND);
            pgeyShape.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        if (pgeyN != null) {
            pgeyN.setOpacity(pgeyS.pgeyOpacity());
            if (!pgeyS.pgeyTransform().isBlank()) {
                pgeyN.getTransforms().add(pgeyTransform(pgeyS.pgeyTransform()));
            }
        }
        return pgeyN;
    }

    private static Node pgeyText(PgeySvgShape pgeyS, Map<String, String> pgeyRecolor) {
        Map<String, String> pgeyA = pgeyS.pgeyAttrs();
        double pgeySize = pgeyA.containsKey("font-size") ? pgeyNum(pgeyA.get("font-size")) : 16;
        boolean pgeyBold = pgeyA.getOrDefault("font-weight", "").matches("bold|[6-9]00");
        Font pgeyFont = Font.font(pgeyBold ? "Barlow Condensed ExtraBold" : "Barlow", pgeyBold ? FontWeight.BOLD : FontWeight.NORMAL, pgeySize);
        TextFlow pgeyFlow = new TextFlow();
        for (PgeySvgShape pgeySpan : pgeyS.pgeySpans()) {
            Text pgeyT = new Text(pgeySpan.pgeyAttrs().get("text"));
            pgeyT.setFont(pgeyFont);
            pgeyT.setFill(pgeyPaint(pgeySpan.pgeyFill(), pgeyRecolor));
            pgeyFlow.getChildren().add(pgeyT);
        }
        pgeyFlow.setTextAlignment(TextAlignment.LEFT);
        double pgeyWidth = pgeyFlow.prefWidth(-1);
        double pgeyX = pgeyNum(pgeyA.get("x"));
        String pgeyAnchor = pgeyA.getOrDefault("text-anchor", "start");
        if ("middle".equals(pgeyAnchor)) {
            pgeyX -= pgeyWidth / 2;
        } else if ("end".equals(pgeyAnchor)) {
            pgeyX -= pgeyWidth;
        }
        pgeyFlow.setLayoutX(pgeyX);
        pgeyFlow.setLayoutY(pgeyNum(pgeyA.get("y")) - pgeySize * 0.8);
        return pgeyFlow;
    }

    static Color pgeyPaint(String pgeyValue, Map<String, String> pgeyRecolor) {
        if (pgeyValue == null || pgeyValue.isBlank() || "none".equalsIgnoreCase(pgeyValue) || pgeyValue.startsWith("url(")) {
            return null;
        }
        try {
            Color pgeyC = Color.web(pgeyValue.trim());
            String pgeyHex = String.format("#%02X%02X%02X", (int) Math.round(pgeyC.getRed() * 255),
                    (int) Math.round(pgeyC.getGreen() * 255), (int) Math.round(pgeyC.getBlue() * 255));
            String pgeyReplacement = pgeyRecolor == null ? null : pgeyRecolor.get(pgeyHex);
            return pgeyReplacement == null ? pgeyC : Color.web(pgeyReplacement);
        } catch (IllegalArgumentException pgeyE) {
            return Color.BLACK;
        }
    }

    private static double[] pgeyPoints(String pgeyS) {
        if (pgeyS == null) {
            return new double[0];
        }
        String[] pgeyP = pgeyS.trim().split("[\\s,]+");
        double[] pgeyOut = new double[pgeyP.length - pgeyP.length % 2];
        for (int pgeyI = 0; pgeyI < pgeyOut.length; pgeyI++) {
            pgeyOut[pgeyI] = pgeyNum(pgeyP[pgeyI]);
        }
        return pgeyOut;
    }

    private static final Pattern PGEY_FN = Pattern.compile("(matrix|translate|scale|rotate)\\s*\\(([^)]*)\\)");

    static Affine pgeyTransform(String pgeySpec) {
        Affine pgeyTotal = new Affine();
        Matcher pgeyM = PGEY_FN.matcher(pgeySpec);
        while (pgeyM.find()) {
            String[] pgeyP = pgeyM.group(2).trim().split("[\\s,]+");
            double[] pgeyV = new double[pgeyP.length];
            for (int pgeyI = 0; pgeyI < pgeyP.length; pgeyI++) {
                pgeyV[pgeyI] = pgeyP[pgeyI].isEmpty() ? 0 : pgeyNum(pgeyP[pgeyI]);
            }
            switch (pgeyM.group(1)) {
                case "matrix" -> pgeyTotal.append(new Affine(pgeyV[0], pgeyV[2], pgeyV[4], pgeyV[1], pgeyV[3], pgeyV[5]));
                case "translate" -> pgeyTotal.appendTranslation(pgeyV[0], pgeyV.length > 1 ? pgeyV[1] : 0);
                case "scale" -> pgeyTotal.appendScale(pgeyV[0], pgeyV.length > 1 ? pgeyV[1] : pgeyV[0]);
                case "rotate" -> {
                    if (pgeyV.length >= 3) {
                        pgeyTotal.appendRotation(pgeyV[0], pgeyV[1], pgeyV[2]);
                    } else {
                        pgeyTotal.appendRotation(pgeyV[0]);
                    }
                }
                default -> {
                }
            }
        }
        return pgeyTotal;
    }
}
