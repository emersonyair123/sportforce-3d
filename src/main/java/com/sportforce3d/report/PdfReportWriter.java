package com.sportforce3d.report;

import com.sportforce3d.model.SalesReport;
import com.sportforce3d.model.SalesReport.CategoryRow;
import com.sportforce3d.model.SalesReport.MethodRow;
import com.sportforce3d.model.SalesReport.PeriodRow;
import com.sportforce3d.model.SalesReport.ProductRow;
import com.sportforce3d.util.Money;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Reporte de ventas en PDF (A4) con OpenPDF, en el tema claro de la guía de marca: fuentes Barlow incrustadas,
 * tablas con cabecera {@code label} en {@code ink-muted}, bordes {@code line} y el gráfico en {@code brand}.
 */
public class PdfReportWriter {

    private static final float MARGIN = 40;

    private final Color ink = color("ink");
    private final Color muted = color("ink-muted");
    private final Color subtle = color("ink-subtle");
    private final Color line = color("line");
    private final Color brand = color("brand");
    private final Color accentText = color("accent-text");
    private final Color band = color("bg-300");

    private final BaseFont regular = font("Barlow-Regular.ttf");
    private final BaseFont semibold = font("Barlow-SemiBold.ttf");
    private final BaseFont condensed = font("BarlowCondensed-ExtraBold.ttf");
    private final BaseFont mono = font("JetBrainsMono-Medium.ttf");

    public void write(SalesReport report, OutputStream out) throws IOException {
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN + 16);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new Footer());
            doc.addTitle("Reporte de ventas · SPORTFORCE 3D");
            doc.addAuthor(report.generatedBy());
            doc.addCreator("SPORTFORCE 3D");
            doc.open();

            header(doc, report);
            doc.add(kpis(report));
            if (report.isEmpty()) {
                doc.add(paragraph("No hay ventas aprobadas en este rango.", new Font(semibold, 12, Font.NORMAL, ink), 16));
                doc.add(paragraph("Prueba con un rango más amplio o revisa los pagos por verificar en Administración → Pagos.",
                        new Font(regular, 10, Font.NORMAL, muted), 4));
            } else {
                section(doc, "Ventas por " + ReportText.periodHeader(report).toLowerCase());
                doc.add(chart(writer, report));
                doc.add(periodTable(report));
                section(doc, "Ventas por categoría");
                doc.add(categoryTable(report));
                section(doc, "Ventas por producto");
                doc.add(productTable(report));
                section(doc, "Por método de pago");
                doc.add(methodTable(report));
            }
            doc.add(paragraph(ReportText.DEFINITION, new Font(regular, 8.5f, Font.NORMAL, subtle), 18));
            doc.close();
        } catch (DocumentException e) {
            throw new IOException("No se pudo generar el PDF", e);
        }
    }

    // ------------------------------------------------------------------ Bloques

    private void header(Document doc, SalesReport report) throws DocumentException {
        Paragraph logo = new Paragraph();
        logo.add(new Chunk("SPORTFORCE ", new Font(condensed, 20, Font.NORMAL, ink)));
        logo.add(new Chunk("3D", new Font(condensed, 20, Font.NORMAL, accentText)));
        doc.add(logo);
        doc.add(paragraph("REPORTE DE VENTAS", new Font(condensed, 30, Font.NORMAL, ink), 10));
        doc.add(paragraph(ReportText.range(report), new Font(semibold, 12, Font.NORMAL, ink), 2));
        doc.add(paragraph(ReportText.generated(report), new Font(regular, 9, Font.NORMAL, muted), 2));
    }

    private PdfPTable kpis(SalesReport r) {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setSpacingBefore(18);
        t.addCell(kpi("VENTAS TOTALES", Money.format(r.totalCents()), "Incluye envío: " + Money.format(r.shippingCents())));
        t.addCell(kpi("PEDIDOS PAGADOS", String.valueOf(r.orders()), "Con pago aprobado"));
        t.addCell(kpi("UNIDADES", String.valueOf(r.units()), "Prendas vendidas"));
        t.addCell(kpi("TICKET PROMEDIO", Money.format(r.averageTicketCents()), "Total / pedidos"));
        return t;
    }

    private PdfPCell kpi(String title, String value, String detail) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(line);
        cell.setBorderWidth(0.75f);
        cell.setPadding(10);
        cell.addElement(paragraph(title, new Font(semibold, 7.5f, Font.NORMAL, muted), 0));
        cell.addElement(paragraph(value, new Font(condensed, 18, Font.NORMAL, ink), 2));
        cell.addElement(paragraph(detail, new Font(regular, 8, Font.NORMAL, muted), 0));
        return cell;
    }

    private void section(Document doc, String title) throws DocumentException {
        Paragraph p = paragraph(title, new Font(semibold, 13, Font.NORMAL, ink), 16);
        // Interlineado mayor que el cuerpo para que los descendentes (g, p) no toquen lo que sigue.
        p.setLeading(20);
        p.setSpacingAfter(4);
        p.setKeepTogether(true);
        doc.add(p);
    }

    private Image chart(PdfWriter writer, SalesReport report) throws DocumentException {
        List<PeriodRow> rows = report.periods();
        float w = PageSize.A4.getWidth() - 2 * MARGIN;
        float h = 120;
        PdfTemplate tpl = writer.getDirectContent().createTemplate(w, h);
        long max = rows.stream().mapToLong(PeriodRow::totalCents).max().orElse(0);
        float left = 4;
        float bottom = 14;
        float plotH = h - bottom - 6;
        tpl.setColorStroke(line);
        tpl.setLineWidth(0.5f);
        for (int i = 0; i <= 4; i++) {
            float y = bottom + plotH * i / 4;
            tpl.moveTo(left, y);
            tpl.lineTo(w, y);
        }
        tpl.stroke();
        float slot = (w - left) / Math.max(1, rows.size());
        float bar = Math.max(1.5f, slot * 0.68f);
        tpl.setColorFill(brand);
        for (int i = 0; i < rows.size(); i++) {
            long v = rows.get(i).totalCents();
            if (v > 0 && max > 0) {
                float bh = Math.max(1, plotH * v / max);
                tpl.rectangle(left + i * slot + (slot - bar) / 2, bottom, bar, bh);
            }
        }
        tpl.fill();
        // Etiquetas: primer y último periodo, y el máximo arriba a la izquierda.
        Font small = new Font(regular, 7, Font.NORMAL, muted);
        ColumnText.showTextAligned(tpl, Element.ALIGN_LEFT, new Phrase(ReportText.period(report, rows.get(0).start()), small), left, 3, 0);
        ColumnText.showTextAligned(tpl, Element.ALIGN_RIGHT,
                new Phrase(ReportText.period(report, rows.get(rows.size() - 1).start()), small), w, 3, 0);
        ColumnText.showTextAligned(tpl, Element.ALIGN_LEFT, new Phrase("Máx. " + Money.format(max), small), left + 2, h - 7, 0);
        Image img = Image.getInstance(tpl);
        img.setSpacingAfter(8);
        return img;
    }

    private PdfPTable periodTable(SalesReport r) {
        PdfPTable t = table(new float[]{3.2f, 1.2f, 1.2f, 1.8f}, 1, ReportText.periodHeader(r), "Pedidos", "Unidades", "Total");
        boolean daily = r.grouping() == SalesReport.Grouping.DAY;
        for (PeriodRow p : r.periods()) {
            if (daily && p.orders() == 0) {
                continue; // por día solo se listan los días con ventas; el gráfico muestra todos
            }
            t.addCell(text(ReportText.period(r, p.start())));
            t.addCell(number(String.valueOf(p.orders())));
            t.addCell(number(String.valueOf(p.units())));
            t.addCell(number(Money.format(p.totalCents())));
        }
        totalRow(t, "Total", String.valueOf(r.orders()), String.valueOf(r.units()), Money.format(r.totalCents()));
        return t;
    }

    private PdfPTable categoryTable(SalesReport r) {
        PdfPTable t = table(new float[]{3.2f, 1.1f, 1.1f, 1.7f, 1.1f}, 1, "Categoría", "Pedidos", "Unidades", "Ventas", "% del total");
        for (CategoryRow c : r.categories()) {
            t.addCell(text(c.category()));
            t.addCell(number(String.valueOf(c.orders())));
            t.addCell(number(String.valueOf(c.units())));
            t.addCell(number(Money.format(c.revenueCents())));
            t.addCell(number(ReportText.percent(r.sharePercent(c.revenueCents()))));
        }
        totalRow(t, "Total productos", "", String.valueOf(r.units()), Money.format(r.productsCents()), "100.0 %");
        return t;
    }

    private PdfPTable productTable(SalesReport r) {
        PdfPTable t = table(new float[]{1.7f, 3.4f, 1.9f, 1f, 1.6f}, 3, "SKU", "Producto", "Categoría", "Unidades", "Ventas");
        for (ProductRow p : r.products()) {
            PdfPCell sku = text(p.sku());
            sku.setPhrase(new Phrase(p.sku(), new Font(mono, 7.5f, Font.NORMAL, muted)));
            t.addCell(sku);
            t.addCell(text(p.name()));
            t.addCell(text(p.category()));
            t.addCell(number(String.valueOf(p.units())));
            t.addCell(number(Money.format(p.revenueCents())));
        }
        totalRow(t, "Total productos", "", "", String.valueOf(r.units()), Money.format(r.productsCents()));
        return t;
    }

    private PdfPTable methodTable(SalesReport r) {
        PdfPTable t = table(new float[]{3.2f, 1.2f, 1.8f}, 1, "Método", "Pedidos", "Total");
        for (MethodRow m : r.methods()) {
            t.addCell(text(m.method().label()));
            t.addCell(number(String.valueOf(m.orders())));
            t.addCell(number(Money.format(m.totalCents())));
        }
        return t;
    }

    // ------------------------------------------------------------------ Celdas

    /** Las primeras {@code textColumns} columnas son texto (a la izquierda); las demás, números (a la derecha). */
    private PdfPTable table(float[] widths, int textColumns, String... headers) {
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        // El espacio va en la tabla: OpenPDF no aplica el spacingAfter del título antes de una tabla.
        t.setSpacingBefore(8);
        t.setSpacingAfter(4);
        for (int i = 0; i < headers.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(headers[i].toUpperCase(ReportText.ES_PE), new Font(semibold, 7.5f, Font.NORMAL, muted)));
            c.setBackgroundColor(band);
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(line);
            c.setPadding(6);
            c.setHorizontalAlignment(i < textColumns ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            t.addCell(c);
        }
        return t;
    }

    private PdfPCell text(String value) {
        PdfPCell c = new PdfPCell(new Phrase(value == null ? "" : value, new Font(regular, 9, Font.NORMAL, ink)));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(line);
        c.setBorderWidth(0.5f);
        c.setPadding(5);
        return c;
    }

    private PdfPCell number(String value) {
        PdfPCell c = text(value);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private void totalRow(PdfPTable t, String... values) {
        for (int i = 0; i < values.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(values[i], new Font(semibold, 9, Font.NORMAL, ink)));
            c.setBorder(Rectangle.TOP);
            c.setBorderColor(color("line-strong"));
            c.setPadding(6);
            c.setHorizontalAlignment(i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            t.addCell(c);
        }
    }

    private static Paragraph paragraph(String text, Font font, float spacingBefore) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(spacingBefore);
        return p;
    }

    private static Color color(String token) {
        return new Color(ReportPalette.rgb(token));
    }

    private static BaseFont font(String file) {
        try (InputStream in = PdfReportWriter.class.getResourceAsStream("/fonts/" + file)) {
            if (in == null) {
                throw new IllegalStateException("Falta la fuente " + file);
            }
            return BaseFont.createFont(file, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo cargar la fuente " + file, e);
        }
    }

    /** Pie de cada página: marca y número de página. */
    private final class Footer extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Font f = new Font(regular, 8, Font.NORMAL, subtle);
            float y = document.bottomMargin() - 20;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase("SPORTFORCE 3D · Reporte de ventas", f),
                    document.left(), y, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase("Página " + writer.getPageNumber(), f),
                    document.right(), y, 0);
        }
    }
}
