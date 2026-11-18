package com.sportforce3d.tools;

import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.StylesDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ManualDocx {

    private static final int TEXT_TWIPS = 9026;
    private static final double TEXT_WIDTH_PT = TEXT_TWIPS / 20.0;
    private static final double MAX_IMG_HEIGHT_PT = 590;
    private static final int BAND = 62;
    private static final int DIAM = 40;
    private static final int GAP = 46;
    private static final Color RED = new Color(0xD9, 0x10, 0x10);

    private record Row(String text, int x, int y, int w, int h) {
    }

    private record Mark(String alias, String selector, String name, String desc) {
    }

    private record Placed(Mark mark, int[] box, int side, double pos) {
    }

    private final Path capturas;
    private final Path outJpg;
    private final XWPFDocument doc = new XWPFDocument();
    private final Map<String, String> refs = new HashMap<>();
    private final Map<String, Integer> figNumbers = new HashMap<>();
    private final List<String> report = new ArrayList<>();
    private int figure;
    private int tableNo;

    private ManualDocx(Path capturas, Path outJpg) {
        this.capturas = capturas;
        this.outJpg = outJpg;
    }

    public static void main(String[] args) throws Exception {
        Path spec = Path.of(args[0]);
        Path capturas = Path.of(args[1]);
        Path out = Path.of(args[2]);
        Path jpg = Path.of(args.length > 3 ? args[3] : "target/manual-jpg");
        Files.createDirectories(jpg);
        ManualDocx m = new ManualDocx(capturas, jpg);
        m.build(Files.readAllLines(spec, StandardCharsets.UTF_8));
        try (OutputStream os = Files.newOutputStream(out)) {
            m.doc.write(os);
        }
        Files.write(jpg.resolve("informe.txt"), m.report, StandardCharsets.UTF_8);
        System.out.println("Figuras: " + m.figure + " · tablas: " + m.tableNo);
        m.report.stream().filter(s -> s.startsWith("FALTA")).forEach(System.out::println);
    }

    private void build(List<String> lines) throws Exception {
        setup();
        List<String[]> parsed = new ArrayList<>();
        for (String l : lines) {
            if (!l.isBlank()) {
                parsed.add(l.split("\\|", -1));
            }
        }
        preNumber(parsed);
        int i = 0;
        while (i < parsed.size()) {
            String[] p = parsed.get(i);
            switch (p[0]) {
                case "TITLE" -> title(p[1]);
                case "SUB" -> centered(p[1]);
                case "TOC" -> toc();
                case "PB" -> pageBreak();
                case "H1" -> heading(p[1], "Heading1");
                case "H2" -> heading(p[1], "Heading2");
                case "P" -> para(p[1], false);
                case "L" -> listItem(p[1], p.length > 2 ? p[2] : "");
                case "FIG" -> {
                    List<Mark> marks = new ArrayList<>();
                    int j = i + 1;
                    while (!parsed.get(j)[0].equals("END")) {
                        String[] q = parsed.get(j);
                        marks.add(new Mark(q[1], q[2], q[3], q[4]));
                        j++;
                    }
                    figureBlock(p[1], p[2], marks);
                    i = j;
                }
                case "TABLE" -> {
                    String[] widths = p[1].split(",");
                    List<String[]> rows = new ArrayList<>();
                    int j = i + 1;
                    while (!parsed.get(j)[0].equals("ENDT")) {
                        String[] q = parsed.get(j);
                        String[] cells = new String[q.length - 1];
                        System.arraycopy(q, 1, cells, 0, cells.length);
                        rows.add(cells);
                        j++;
                    }
                    table(widths, rows, 22);
                    i = j;
                }
                default -> throw new IllegalArgumentException("Línea desconocida: " + String.join("|", p));
            }
            i++;
        }
    }

    private void preNumber(List<String[]> parsed) throws IOException {
        int n = 0;
        for (int i = 0; i < parsed.size(); i++) {
            String[] p = parsed.get(i);
            if (!p[0].equals("FIG")) {
                continue;
            }
            n++;
            figNumbers.put(p[1], n);
            List<Mark> marks = new ArrayList<>();
            int j = i + 1;
            while (!parsed.get(j)[0].equals("END")) {
                String[] q = parsed.get(j);
                marks.add(new Mark(q[1], q[2], q[3], q[4]));
                j++;
            }
            List<Placed> placed = layout(p[1], marks, readImage(p[1]));
            for (int k = 0; k < placed.size(); k++) {
                String alias = placed.get(k).mark().alias();
                if (!alias.isEmpty()) {
                    refs.put(alias, "botón " + (k + 1) + " de la Figura " + n);
                }
            }
        }
    }

    private BufferedImage readImage(String id) throws IOException {
        return ImageIO.read(capturas.resolve(id + ".png").toFile());
    }

    private List<Row> rows(String id) throws IOException {
        List<Row> list = new ArrayList<>();
        for (String l : Files.readAllLines(capturas.resolve(id + ".tsv"), StandardCharsets.UTF_8)) {
            String[] c = l.split("\t", -1);
            list.add(new Row(String.join(" ", c), Integer.parseInt(c[3]), Integer.parseInt(c[4]),
                    Integer.parseInt(c[5]), Integer.parseInt(c[6])));
        }
        return list;
    }

    private int[] resolve(String id, String selector, List<Row> rows) {
        selector = selector.replaceAll("\\^[TRBL]$", "");
        if (selector.startsWith("BOX:")) {
            String[] v = selector.substring(4).split(",");
            return new int[]{Integer.parseInt(v[0].trim()), Integer.parseInt(v[1].trim()),
                    Integer.parseInt(v[2].trim()), Integer.parseInt(v[3].trim())};
        }
        selector = selector.replaceAll("\\^[TRBL]$", "");
        boolean union = selector.startsWith("U:");
        String sel = union ? selector.substring(2) : selector;
        int occurrence = 1;
        Matcher m = Pattern.compile("~(\\d+)$").matcher(sel);
        if (m.find()) {
            occurrence = Integer.parseInt(m.group(1));
            sel = sel.substring(0, m.start());
        }
        String[] alternatives = sel.split(" OR ");
        List<Row> found = new ArrayList<>();
        for (Row r : rows) {
            for (String alt : alternatives) {
                boolean ok = true;
                for (String t : alt.split(" && ")) {
                    if (!r.text().contains(t)) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    found.add(r);
                    break;
                }
            }
        }
        if (found.isEmpty() || !union && found.size() < occurrence) {
            report.add("FALTA " + id + ": " + selector);
            return null;
        }
        if (!union) {
            Row r = found.get(occurrence - 1);
            return new int[]{r.x(), r.y(), r.w(), r.h()};
        }
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y1 = Integer.MIN_VALUE;
        for (Row r : found) {
            x0 = Math.min(x0, r.x());
            y0 = Math.min(y0, r.y());
            x1 = Math.max(x1, r.x() + r.w());
            y1 = Math.max(y1, r.y() + r.h());
        }
        return new int[]{x0, y0, x1 - x0, y1 - y0};
    }

    private List<Placed> layout(String id, List<Mark> marks, BufferedImage img) throws IOException {
        int w = img.getWidth();
        int h = img.getHeight();
        List<Row> rows = rows(id);
        List<Mark> ok = new ArrayList<>();
        List<int[]> boxes = new ArrayList<>();
        for (Mark mk : marks) {
            int[] b = resolve(id, mk.selector(), rows);
            if (b != null) {
                ok.add(mk);
                boxes.add(b);
            }
        }
        int n = ok.size();
        int[] side = new int[n];
        double[][] dist = new double[n][4];
        for (int i = 0; i < n; i++) {
            int[] b = boxes.get(i);
            double cx = b[0] + b[2] / 2.0;
            double cy = b[1] + b[3] / 2.0;
            dist[i] = new double[]{cy, w - cx, h - cy, cx};
            for (int sd = 0; sd < 4; sd++) {
                dist[i][sd] += 260 * crossings(i, sd, boxes, w, h);
            }
            side[i] = argMin(dist[i], new boolean[4]);
        }
        int[] cap = {w / GAP, h / GAP, w / GAP, h / GAP};
        for (int pass = 0; pass < 8; pass++) {
            int[] count = new int[4];
            for (int sd : side) {
                count[sd]++;
            }
            boolean changed = false;
            for (int sd = 0; sd < 4; sd++) {
                while (count[sd] > cap[sd]) {
                    int worst = -1;
                    double cost = Double.MAX_VALUE;
                    boolean[] banned = new boolean[4];
                    banned[sd] = true;
                    for (int i = 0; i < n; i++) {
                        if (side[i] == sd) {
                            double c = dist[i][argMin(dist[i], banned)] - dist[i][sd];
                            if (c < cost) {
                                cost = c;
                                worst = i;
                            }
                        }
                    }
                    side[worst] = argMin(dist[worst], banned);
                    count[sd]--;
                    count[side[worst]]++;
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        int[] forced = new int[n];
        for (int i = 0; i < n; i++) {
            String sl = ok.get(i).selector();
            forced[i] = sl.matches(".*\\^[TRBL]$") ? "TRBL".indexOf(sl.charAt(sl.length() - 1)) : -1;
            if (forced[i] >= 0) {
                side[i] = forced[i];
            }
        }
        double best = totalCost(side, boxes, w, h);
        for (int pass = 0; pass < 8; pass++) {
            boolean improved = false;
            for (int i = 0; i < n; i++) {
                if (forced[i] >= 0) {
                    continue;
                }
                int keep = side[i];
                for (int sd = 0; sd < 4; sd++) {
                    if (sd == keep) {
                        continue;
                    }
                    int count = 0;
                    for (int k = 0; k < n; k++) {
                        if (k != i && side[k] == sd) {
                            count++;
                        }
                    }
                    if (count + 1 > cap[sd]) {
                        continue;
                    }
                    side[i] = sd;
                    double c = totalCost(side, boxes, w, h);
                    if (c < best - 0.5) {
                        best = c;
                        keep = sd;
                        improved = true;
                    }
                    side[i] = keep;
                }
            }
            if (!improved) {
                break;
            }
        }
        double[] pos = positions(side, boxes, w, h);
        List<Placed> placed = new ArrayList<>();
        int[] order = {0, 3, 1, 2};
        for (int s : order) {
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (side[i] == s) {
                    idx.add(i);
                }
            }
            idx.sort(Comparator.comparingDouble(i -> pos[i]));
            for (int i : idx) {
                placed.add(new Placed(ok.get(i), boxes.get(i), s, pos[i]));
            }
        }
        return placed;
    }

    private static double[] positions(int[] side, List<int[]> boxes, int w, int h) {
        int n = side.length;
        double[] pos = new double[n];
        for (int sd = 0; sd < 4; sd++) {
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (side[i] == sd) {
                    idx.add(i);
                }
            }
            boolean horizontal = sd == 0 || sd == 2;
            int len = horizontal ? w : h;
            idx.sort(Comparator.comparingDouble(i -> center(boxes.get(i), horizontal)));
            double[] p = new double[idx.size()];
            for (int k = 0; k < p.length; k++) {
                p[k] = center(boxes.get(idx.get(k)), horizontal);
            }
            spread(p, GAP / 2.0, len - GAP / 2.0, GAP);
            for (int k = 0; k < p.length; k++) {
                pos[idx.get(k)] = p[k];
            }
        }
        return pos;
    }

    private static double[] segment(int sd, double pos, int[] b, int w, int h) {
        double cx;
        double cy;
        switch (sd) {
            case 0 -> {
                cx = pos;
                cy = -BAND / 2.0;
            }
            case 1 -> {
                cx = w + BAND / 2.0;
                cy = pos;
            }
            case 2 -> {
                cx = pos;
                cy = h + BAND / 2.0;
            }
            default -> {
                cx = -BAND / 2.0;
                cy = pos;
            }
        }
        double bx0 = b[0] - 3;
        double by0 = b[1] - 3;
        double bx1 = b[0] + b[2] + 3;
        double by1 = b[1] + b[3] + 3;
        double tx;
        double ty;
        if (sd == 0 || sd == 2) {
            tx = Math.max(bx0 + 4, Math.min(bx1 - 4, cx));
            ty = sd == 0 ? by0 : by1;
        } else {
            ty = Math.max(by0 + 4, Math.min(by1 - 4, cy));
            tx = sd == 3 ? bx0 : bx1;
        }
        return new double[]{cx, cy, tx, ty};
    }

    private static double totalCost(int[] side, List<int[]> boxes, int w, int h) {
        int n = side.length;
        double[] pos = positions(side, boxes, w, h);
        double[][] seg = new double[n][];
        for (int i = 0; i < n; i++) {
            seg[i] = segment(side[i], pos[i], boxes.get(i), w, h);
        }
        double total = 0;
        for (int i = 0; i < n; i++) {
            double[] sg = seg[i];
            double len = Math.hypot(sg[2] - sg[0], sg[3] - sg[1]);
            double straight = Math.abs(side[i] == 0 || side[i] == 2 ? sg[2] - sg[0] : sg[3] - sg[1]);
            total += len + 1.5 * straight;
            int[] b = boxes.get(i);
            java.awt.geom.Rectangle2D self = new java.awt.geom.Rectangle2D.Double(b[0], b[1], b[2], b[3]);
            for (int j = 0; j < n; j++) {
                if (j == i) {
                    continue;
                }
                int[] o = boxes.get(j);
                java.awt.geom.Rectangle2D r = new java.awt.geom.Rectangle2D.Double(o[0] + 2, o[1] + 2, o[2] - 4, o[3] - 4);
                if (!r.isEmpty() && !r.intersects(self) && r.intersectsLine(sg[0], sg[1], sg[2], sg[3])) {
                    total += 220;
                }
                if (j > i && Line2D.linesIntersect(sg[0], sg[1], sg[2], sg[3], seg[j][0], seg[j][1], seg[j][2], seg[j][3])) {
                    total += 300;
                }
            }
        }
        return total;
    }

    private static int crossings(int i, int sd, List<int[]> boxes, int w, int h) {
        int[] b = boxes.get(i);
        double cx = b[0] + b[2] / 2.0;
        double cy = b[1] + b[3] / 2.0;
        double x1;
        double y1;
        double x2;
        double y2;
        switch (sd) {
            case 0 -> {
                x1 = cx;
                y1 = -10;
                x2 = cx;
                y2 = b[1];
            }
            case 1 -> {
                x1 = w + 10;
                y1 = cy;
                x2 = b[0] + b[2];
                y2 = cy;
            }
            case 2 -> {
                x1 = cx;
                y1 = h + 10;
                x2 = cx;
                y2 = b[1] + b[3];
            }
            default -> {
                x1 = -10;
                y1 = cy;
                x2 = b[0];
                y2 = cy;
            }
        }
        java.awt.geom.Rectangle2D self = new java.awt.geom.Rectangle2D.Double(b[0], b[1], b[2], b[3]);
        int n = 0;
        for (int j = 0; j < boxes.size(); j++) {
            if (j == i) {
                continue;
            }
            int[] o = boxes.get(j);
            java.awt.geom.Rectangle2D r = new java.awt.geom.Rectangle2D.Double(o[0] + 1, o[1] + 1, o[2] - 2, o[3] - 2);
            if (r.intersects(self) || r.isEmpty()) {
                continue;
            }
            if (r.intersectsLine(x1, y1, x2, y2)) {
                n++;
            }
        }
        return n;
    }

    private static double center(int[] b, boolean horizontal) {
        return horizontal ? b[0] + b[2] / 2.0 : b[1] + b[3] / 2.0;
    }

    private static int argMin(double[] d, boolean[] banned) {
        int best = -1;
        for (int i = 0; i < d.length; i++) {
            if (!banned[i] && (best < 0 || d[i] < d[best])) {
                best = i;
            }
        }
        return best;
    }

    private static void spread(double[] p, double min, double max, double gap) {
        if (p.length == 0) {
            return;
        }
        for (int k = 0; k < p.length; k++) {
            p[k] = Math.max(min, Math.min(max, p[k]));
        }
        for (int iter = 0; iter < 200; iter++) {
            boolean moved = false;
            for (int k = 1; k < p.length; k++) {
                double overlap = p[k - 1] + gap - p[k];
                if (overlap > 0.01) {
                    p[k - 1] -= overlap / 2;
                    p[k] += overlap / 2;
                    moved = true;
                }
            }
            if (p[0] < min) {
                double d = min - p[0];
                for (int k = 0; k < p.length; k++) {
                    p[k] += d;
                }
            }
            if (p[p.length - 1] > max) {
                double d = p[p.length - 1] - max;
                for (int k = 0; k < p.length; k++) {
                    p[k] -= d;
                }
            }
            if (!moved) {
                break;
            }
        }
    }

    private BufferedImage annotate(BufferedImage img, List<Placed> placed) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage out = new BufferedImage(w + 2 * BAND, h + 2 * BAND, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.drawImage(img, BAND, BAND, null);
        g.setColor(new Color(0x80, 0x80, 0x80));
        g.drawRect(BAND - 1, BAND - 1, w + 1, h + 1);
        double[][] circles = new double[placed.size()][];
        for (int k = 0; k < placed.size(); k++) {
            Placed p = placed.get(k);
            double cx;
            double cy;
            switch (p.side()) {
                case 0 -> {
                    cx = BAND + p.pos();
                    cy = BAND / 2.0;
                }
                case 1 -> {
                    cx = BAND + w + BAND / 2.0;
                    cy = BAND + p.pos();
                }
                case 2 -> {
                    cx = BAND + p.pos();
                    cy = BAND + h + BAND / 2.0;
                }
                default -> {
                    cx = BAND / 2.0;
                    cy = BAND + p.pos();
                }
            }
            circles[k] = new double[]{cx, cy};
        }
        for (int k = 0; k < placed.size(); k++) {
            int[] b = placed.get(k).box();
            g.setStroke(new BasicStroke(2.2f));
            g.setColor(RED);
            g.drawRoundRect(BAND + b[0] - 3, BAND + b[1] - 3, b[2] + 6, b[3] + 6, 10, 10);
        }
        for (int k = 0; k < placed.size(); k++) {
            int[] b = placed.get(k).box();
            double cx = circles[k][0];
            double cy = circles[k][1];
            double bx0 = BAND + b[0] - 3;
            double by0 = BAND + b[1] - 3;
            double bx1 = BAND + b[0] + b[2] + 3;
            double by1 = BAND + b[1] + b[3] + 3;
            double tx = Math.max(bx0, Math.min(bx1, cx));
            double ty = Math.max(by0, Math.min(by1, cy));
            if (tx > bx0 && tx < bx1 && ty > by0 && ty < by1) {
                tx = (bx0 + bx1) / 2;
                ty = by0;
            }
            int side = placed.get(k).side();
            if (side == 0 || side == 2) {
                double mx = Math.max(bx0 + 4, Math.min(bx1 - 4, cx));
                tx = mx;
                ty = side == 0 ? by0 : by1;
            } else {
                double my = Math.max(by0 + 4, Math.min(by1 - 4, cy));
                ty = my;
                tx = side == 3 ? bx0 : bx1;
            }
            double dx = tx - cx;
            double dy = ty - cy;
            double len = Math.hypot(dx, dy);
            if (len < DIAM / 2.0 + 4) {
                continue;
            }
            double ux = dx / len;
            double uy = dy / len;
            double sx = cx + ux * (DIAM / 2.0);
            double sy = cy + uy * (DIAM / 2.0);
            double head = 14;
            double ex = tx - ux * head;
            double ey = ty - uy * head;
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(sx, sy, ex, ey));
            g.setColor(RED);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(sx, sy, ex, ey));
            Polygon tri = new Polygon();
            tri.addPoint((int) Math.round(tx), (int) Math.round(ty));
            tri.addPoint((int) Math.round(ex - uy * 8), (int) Math.round(ey + ux * 8));
            tri.addPoint((int) Math.round(ex + uy * 8), (int) Math.round(ey - ux * 8));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.5f));
            g.drawPolygon(tri);
            g.setColor(RED);
            g.fillPolygon(tri);
        }
        Font font = new Font("Arial", Font.BOLD, 22);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        for (int k = 0; k < placed.size(); k++) {
            double cx = circles[k][0];
            double cy = circles[k][1];
            g.setColor(RED);
            g.fillOval((int) Math.round(cx - DIAM / 2.0), (int) Math.round(cy - DIAM / 2.0), DIAM, DIAM);
            g.setColor(Color.WHITE);
            String s = String.valueOf(k + 1);
            g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) (cy + fm.getAscent() / 2.0 - 3));
        }
        g.dispose();
        return out;
    }

    private void figureBlock(String id, String caption, List<Mark> marks) throws IOException {
        figure++;
        BufferedImage img = readImage(id);
        List<Placed> placed = layout(id, marks, img);
        BufferedImage annotated = annotate(img, placed);
        byte[] jpg = jpeg(annotated);
        Files.write(outJpg.resolve(id + ".jpg"), jpg);
        double wPt = TEXT_WIDTH_PT;
        double hPt = wPt * annotated.getHeight() / annotated.getWidth();
        if (hPt > MAX_IMG_HEIGHT_PT) {
            hPt = MAX_IMG_HEIGHT_PT;
            wPt = hPt * annotated.getWidth() / annotated.getHeight();
        }
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBefore(60);
        p.setSpacingAfter(40);
        p.setKeepNext(true);
        XWPFRun r = p.createRun();
        try {
            r.addPicture(new ByteArrayInputStream(jpg), Document.PICTURE_TYPE_JPEG, id + ".jpg",
                    Units.toEMU(wPt), Units.toEMU(hPt));
        } catch (Exception e) {
            throw new IOException(e);
        }
        XWPFParagraph c = doc.createParagraph();
        c.setAlignment(ParagraphAlignment.CENTER);
        c.setSpacingAfter(100);
        c.setKeepNext(true);
        XWPFRun cr = c.createRun();
        cr.setItalic(true);
        cr.setFontSize(10);
        cr.setText("Figura " + figure + ". " + caption);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"N°", "Botón", "Para qué sirve"});
        for (int k = 0; k < placed.size(); k++) {
            Mark mk = placed.get(k).mark();
            rows.add(new String[]{String.valueOf(k + 1), mk.name(), mk.desc()});
            report.add(id + "\t" + (k + 1) + "\t" + mk.name() + "\t" + mk.selector());
        }
        table(new String[]{"560", "2500", "5966"}, rows, 19);
    }

    private static byte[] jpeg(BufferedImage img) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.85f);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
        return bos.toByteArray();
    }

    private String expand(String text) {
        Matcher m = Pattern.compile("\\[\\[([^\\]]+)]]").matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String value;
            if (key.startsWith("fig:")) {
                Integer n = figNumbers.get(key.substring(4));
                value = n == null ? null : "Figura " + n;
            } else {
                value = refs.get(key);
            }
            if (value == null) {
                report.add("FALTA referencia " + key);
                value = "??";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void addRich(XWPFParagraph p, String text, int size) {
        String t = expand(text);
        String[] parts = t.split("\\*\\*", -1);
        for (int k = 0; k < parts.length; k++) {
            if (parts[k].isEmpty()) {
                continue;
            }
            XWPFRun r = p.createRun();
            r.setText(parts[k]);
            r.setBold(k % 2 == 1);
            if (size > 0) {
                r.setFontSize(size / 2.0);
            }
        }
    }

    private void table(String[] widths, List<String[]> rows, int size) {
        XWPFTable t = doc.createTable(rows.size(), widths.length);
        CTTblPr pr = t.getCTTbl().getTblPr();
        pr.addNewTblW().setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
        pr.getTblW().setW(BigInteger.valueOf(TEXT_TWIPS));
        pr.addNewTblLayout().setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType.FIXED);
        var grid = t.getCTTbl().getTblGrid() != null ? t.getCTTbl().getTblGrid() : t.getCTTbl().addNewTblGrid();
        while (grid.sizeOfGridColArray() > 0) {
            grid.removeGridCol(0);
        }
        for (String wd : widths) {
            grid.addNewGridCol().setW(BigInteger.valueOf(Integer.parseInt(wd)));
        }
        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = t.getRow(r);
            row.setCantSplitRow(true);
            if (r == 0) {
                row.setRepeatHeader(true);
            }
            for (int c = 0; c < widths.length; c++) {
                XWPFTableCell cell = row.getCell(c);
                CTTcPr tc = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
                tc.addNewTcW().setW(BigInteger.valueOf(Integer.parseInt(widths[c])));
                tc.getTcW().setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
                var borders = tc.addNewTcBorders();
                for (var b : new org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder[]{
                        borders.addNewTop(), borders.addNewLeft(), borders.addNewBottom(), borders.addNewRight()}) {
                    b.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
                    b.setSz(BigInteger.valueOf(4));
                    b.setColor("000000");
                }
                var mar = tc.addNewTcMar();
                mar.addNewTop().setW(BigInteger.valueOf(30));
                mar.getTop().setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
                mar.addNewBottom().setW(BigInteger.valueOf(30));
                mar.getBottom().setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
                mar.addNewLeft().setW(BigInteger.valueOf(80));
                mar.getLeft().setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
                mar.addNewRight().setW(BigInteger.valueOf(80));
                mar.getRight().setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
                if (r == 0) {
                    var shd = tc.addNewShd();
                    shd.setFill("D9D9D9");
                    shd.setColor("auto");
                    shd.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd.CLEAR);
                }
                XWPFParagraph p = cell.getParagraphs().get(0);
                p.setSpacingAfter(0);
                p.setSpacingBefore(0);
                p.setSpacingBetween(1.0);
                if (r < rows.size() - 1) {
                    p.setKeepNext(true);
                }
                if (c == 0 && widths.length == 3 && widths[0].equals("560")) {
                    p.setAlignment(ParagraphAlignment.CENTER);
                }
                if (r == 0) {
                    XWPFRun run = p.createRun();
                    run.setBold(true);
                    run.setColor("000000");
                    run.setFontSize(size / 2.0);
                    run.setText(rows.get(r)[c]);
                } else {
                    addRich(p, rows.get(r)[c], size);
                }
            }
        }
        tableNo++;
        XWPFParagraph after = doc.createParagraph();
        after.setSpacingAfter(60);
        after.setSpacingBefore(0);
        var rpr = after.getCTP().addNewPPr().addNewRPr();
        rpr.addNewSz().setVal(BigInteger.valueOf(8));
    }

    private void title(String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("Title");
        p.createRun().setText(text);
    }

    private void centered(String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingAfter(120);
        p.setSpacingBetween(1.15);
        addRich(p, text, 0);
    }

    private void heading(String text, String style) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle(style);
        p.setKeepNext(true);
        p.createRun().setText(text);
    }

    private void para(String text, boolean keep) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        p.setSpacingAfter(100);
        p.setSpacingBetween(1.15);
        p.setKeepNext(keep);
        addRich(p, text, 0);
    }

    private void listItem(String number, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        p.setSpacingAfter(60);
        p.setSpacingBetween(1.15);
        p.setIndentationLeft(360);
        p.setIndentationHanging(360);
        XWPFRun r = p.createRun();
        r.setText(number + "\t");
        addRich(p, text, 0);
        var tabs = p.getCTP().getPPr().addNewTabs();
        var tab = tabs.addNewTab();
        tab.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc.LEFT);
        tab.setPos(BigInteger.valueOf(360));
    }

    private void pageBreak() {
        XWPFParagraph p = doc.createParagraph();
        p.setPageBreak(true);
    }

    private void toc() {
        XWPFParagraph h = doc.createParagraph();
        h.setStyle("TOCHeading");
        h.createRun().setText("Índice");
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r1 = p.createRun();
        r1.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        XWPFRun r2 = p.createRun();
        var instr = r2.getCTR().addNewInstrText();
        instr.setStringValue(" TOC \\o \"1-2\" \\h \\z \\u ");
        instr.setSpace(org.apache.xmlbeans.impl.xb.xmlschema.SpaceAttribute.Space.PRESERVE);
        XWPFRun r3 = p.createRun();
        r3.getCTR().addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        XWPFRun r4 = p.createRun();
        r4.setText("Actualice el índice (clic derecho › Actualizar campo).");
        XWPFRun r5 = p.createRun();
        r5.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);
    }

    private void setup() throws Exception {
        String styles = """
                <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="Times New Roman" w:cs="Times New Roman"/><w:sz w:val="24"/><w:szCs w:val="24"/><w:lang w:val="es-PE"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="0" w:line="240" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>
                <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:qFormat/></w:style>
                <w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:spacing w:before="600" w:after="200"/><w:jc w:val="center"/></w:pPr><w:rPr><w:b/><w:bCs/><w:color w:val="000000"/><w:sz w:val="40"/><w:szCs w:val="40"/></w:rPr></w:style>
                <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:uiPriority w:val="9"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="300" w:after="120"/><w:outlineLvl w:val="0"/></w:pPr><w:rPr><w:b/><w:bCs/><w:color w:val="000000"/><w:sz w:val="28"/><w:szCs w:val="28"/></w:rPr></w:style>
                <w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:uiPriority w:val="9"/><w:unhideWhenUsed/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="200" w:after="80"/><w:outlineLvl w:val="1"/></w:pPr><w:rPr><w:b/><w:bCs/><w:color w:val="000000"/><w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr></w:style>
                <w:style w:type="paragraph" w:styleId="TOCHeading"><w:name w:val="TOC Heading"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:pPr><w:spacing w:before="240" w:after="160"/></w:pPr><w:rPr><w:b/><w:bCs/><w:color w:val="000000"/><w:sz w:val="28"/><w:szCs w:val="28"/></w:rPr></w:style>
                <w:style w:type="paragraph" w:styleId="TOC1"><w:name w:val="toc 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:uiPriority w:val="39"/><w:unhideWhenUsed/><w:pPr><w:tabs><w:tab w:val="right" w:leader="dot" w:pos="9016"/></w:tabs><w:spacing w:before="30" w:after="0"/></w:pPr><w:rPr><w:b/><w:bCs/><w:sz w:val="22"/><w:szCs w:val="22"/></w:rPr></w:style>
                <w:style w:type="paragraph" w:styleId="TOC2"><w:name w:val="toc 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:uiPriority w:val="39"/><w:unhideWhenUsed/><w:pPr><w:tabs><w:tab w:val="right" w:leader="dot" w:pos="9016"/></w:tabs><w:spacing w:after="0"/><w:ind w:left="240"/></w:pPr><w:rPr><w:sz w:val="22"/><w:szCs w:val="22"/></w:rPr></w:style>
                <w:style w:type="character" w:styleId="Hyperlink"><w:name w:val="Hyperlink"/><w:rPr><w:color w:val="000000"/></w:rPr></w:style>
                <w:style w:type="table" w:default="1" w:styleId="TableNormal"><w:name w:val="Normal Table"/><w:tblPr><w:tblInd w:w="0" w:type="dxa"/><w:tblCellMar><w:top w:w="0" w:type="dxa"/><w:left w:w="108" w:type="dxa"/><w:bottom w:w="0" w:type="dxa"/><w:right w:w="108" w:type="dxa"/></w:tblCellMar></w:tblPr></w:style>
                </w:styles>""";
        doc.createStyles().setStyles(StylesDocument.Factory.parse(styles).getStyles());
        CTSectPr sect = doc.getDocument().getBody().isSetSectPr() ? doc.getDocument().getBody().getSectPr()
                : doc.getDocument().getBody().addNewSectPr();
        CTPageSz sz = sect.addNewPgSz();
        sz.setW(BigInteger.valueOf(11906));
        sz.setH(BigInteger.valueOf(16838));
        CTPageMar mar = sect.addNewPgMar();
        mar.setTop(BigInteger.valueOf(1440));
        mar.setBottom(BigInteger.valueOf(1440));
        mar.setLeft(BigInteger.valueOf(1440));
        mar.setRight(BigInteger.valueOf(1440));
        mar.setHeader(BigInteger.valueOf(708));
        mar.setFooter(BigInteger.valueOf(708));
        mar.setGutter(BigInteger.ZERO);
        XWPFFooter footer = doc.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph fp = footer.createParagraph();
        fp.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun f1 = fp.createRun();
        f1.setFontSize(9);
        f1.setText("pág. ");
        XWPFRun f2 = fp.createRun();
        f2.setFontSize(9);
        f2.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        XWPFRun f3 = fp.createRun();
        f3.setFontSize(9);
        f3.getCTR().addNewInstrText().setStringValue("PAGE");
        XWPFRun f4 = fp.createRun();
        f4.setFontSize(9);
        f4.getCTR().addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        XWPFRun f5 = fp.createRun();
        f5.setFontSize(9);
        f5.setText("1");
        XWPFRun f6 = fp.createRun();
        f6.setFontSize(9);
        f6.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);
        doc.getProperties().getCoreProperties().setTitle("SportForce 3D - Manual de Uso");
        doc.getProperties().getCoreProperties().setCreator("PGEY, HEPY y PPQH");
    }
}
