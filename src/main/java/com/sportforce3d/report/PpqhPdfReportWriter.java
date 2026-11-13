package com.sportforce3d.report;

import com.sportforce3d.model.PpqhSalesReport;
import com.sportforce3d.model.PpqhSalesReport.PpqhCategoryRow;
import com.sportforce3d.model.PpqhSalesReport.PpqhMethodRow;
import com.sportforce3d.model.PpqhSalesReport.PpqhPeriodRow;
import com.sportforce3d.model.PpqhSalesReport.PpqhProductRow;
import com.sportforce3d.util.HepyMoney;
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

public class PpqhPdfReportWriter {

    private static final float PPQH_MARGIN = 40;

    private final Color ppqhInk = ppqhColor("ink");
    private final Color ppqhMuted = ppqhColor("ink-muted");
    private final Color ppqhSubtle = ppqhColor("ink-subtle");
    private final Color ppqhLine = ppqhColor("line");
    private final Color ppqhBrand = ppqhColor("brand");
    private final Color ppqhAccentText = ppqhColor("accent-text");
    private final Color ppqhBand = ppqhColor("bg-300");

    private final BaseFont ppqhRegular = ppqhFont("Barlow-Regular.ttf");
    private final BaseFont ppqhSemibold = ppqhFont("Barlow-SemiBold.ttf");
    private final BaseFont ppqhCondensed = ppqhFont("BarlowCondensed-ExtraBold.ttf");
    private final BaseFont ppqhMono = ppqhFont("JetBrainsMono-Medium.ttf");

    public void ppqhWrite(PpqhSalesReport ppqhReport, OutputStream ppqhOut) throws IOException {
        Document ppqhDoc = new Document(PageSize.A4, PPQH_MARGIN, PPQH_MARGIN, PPQH_MARGIN, PPQH_MARGIN + 16);
        try {
            PdfWriter ppqhWriter = PdfWriter.getInstance(ppqhDoc, ppqhOut);
            ppqhWriter.setPageEvent(new PpqhFooter());
            ppqhDoc.addTitle("Reporte de ventas · SPORTFORCE 3D");
            ppqhDoc.addAuthor(ppqhReport.ppqhGeneratedBy());
            ppqhDoc.addCreator("SPORTFORCE 3D");
            ppqhDoc.open();

            ppqhHeader(ppqhDoc, ppqhReport);
            ppqhDoc.add(ppqhKpis(ppqhReport));
            if (ppqhReport.ppqhIsEmpty()) {
                ppqhDoc.add(ppqhParagraph("No hay ventas aprobadas en este rango.", new Font(ppqhSemibold, 12, Font.NORMAL, ppqhInk), 16));
                ppqhDoc.add(ppqhParagraph("Prueba con un rango más amplio o revisa los pagos por verificar en Administración → Pagos.",
                        new Font(ppqhRegular, 10, Font.NORMAL, ppqhMuted), 4));
            } else {
                ppqhSection(ppqhDoc, "Ventas por " + PpqhReportText.ppqhPeriodHeader(ppqhReport).toLowerCase());
                ppqhDoc.add(ppqhChart(ppqhWriter, ppqhReport));
                ppqhDoc.add(ppqhPeriodTable(ppqhReport));
                ppqhSection(ppqhDoc, "Ventas por categoría");
                ppqhDoc.add(ppqhCategoryTable(ppqhReport));
                ppqhSection(ppqhDoc, "Ventas por producto");
                ppqhDoc.add(ppqhProductTable(ppqhReport));
                ppqhSection(ppqhDoc, "Por método de pago");
                ppqhDoc.add(ppqhMethodTable(ppqhReport));
            }
            ppqhDoc.add(ppqhParagraph(PpqhReportText.PPQH_DEFINITION, new Font(ppqhRegular, 8.5f, Font.NORMAL, ppqhSubtle), 18));
            ppqhDoc.close();
        } catch (DocumentException ppqhE) {
            throw new IOException("No se pudo generar el PDF", ppqhE);
        }
    }

    private void ppqhHeader(Document ppqhDoc, PpqhSalesReport ppqhReport) throws DocumentException {
        Paragraph ppqhLogo = new Paragraph();
        ppqhLogo.add(new Chunk("SPORTFORCE ", new Font(ppqhCondensed, 20, Font.NORMAL, ppqhInk)));
        ppqhLogo.add(new Chunk("3D", new Font(ppqhCondensed, 20, Font.NORMAL, ppqhAccentText)));
        ppqhDoc.add(ppqhLogo);
        ppqhDoc.add(ppqhParagraph("REPORTE DE VENTAS", new Font(ppqhCondensed, 30, Font.NORMAL, ppqhInk), 10));
        ppqhDoc.add(ppqhParagraph(PpqhReportText.ppqhRange(ppqhReport), new Font(ppqhSemibold, 12, Font.NORMAL, ppqhInk), 2));
        ppqhDoc.add(ppqhParagraph(PpqhReportText.ppqhGenerated(ppqhReport), new Font(ppqhRegular, 9, Font.NORMAL, ppqhMuted), 2));
    }

    private PdfPTable ppqhKpis(PpqhSalesReport ppqhR) {
        PdfPTable ppqhT = new PdfPTable(4);
        ppqhT.setWidthPercentage(100);
        ppqhT.setSpacingBefore(18);
        ppqhT.addCell(ppqhKpi("VENTAS TOTALES", HepyMoney.hepyFormat(ppqhR.ppqhTotalCents()), "Incluye envío: " + HepyMoney.hepyFormat(ppqhR.ppqhShippingCents())));
        ppqhT.addCell(ppqhKpi("PEDIDOS PAGADOS", String.valueOf(ppqhR.ppqhOrders()), "Con pago aprobado"));
        ppqhT.addCell(ppqhKpi("UNIDADES", String.valueOf(ppqhR.ppqhUnits()), "Prendas vendidas"));
        ppqhT.addCell(ppqhKpi("TICKET PROMEDIO", HepyMoney.hepyFormat(ppqhR.ppqhAverageTicketCents()), "Total / pedidos"));
        return ppqhT;
    }

    private PdfPCell ppqhKpi(String ppqhTitle, String ppqhValue, String ppqhDetail) {
        PdfPCell ppqhCell = new PdfPCell();
        ppqhCell.setBorderColor(ppqhLine);
        ppqhCell.setBorderWidth(0.75f);
        ppqhCell.setPadding(10);
        ppqhCell.addElement(ppqhParagraph(ppqhTitle, new Font(ppqhSemibold, 7.5f, Font.NORMAL, ppqhMuted), 0));
        ppqhCell.addElement(ppqhParagraph(ppqhValue, new Font(ppqhCondensed, 18, Font.NORMAL, ppqhInk), 2));
        ppqhCell.addElement(ppqhParagraph(ppqhDetail, new Font(ppqhRegular, 8, Font.NORMAL, ppqhMuted), 0));
        return ppqhCell;
    }

    private void ppqhSection(Document ppqhDoc, String ppqhTitle) throws DocumentException {
        Paragraph ppqhP = ppqhParagraph(ppqhTitle, new Font(ppqhSemibold, 13, Font.NORMAL, ppqhInk), 16);
        ppqhP.setLeading(20);
        ppqhP.setSpacingAfter(4);
        ppqhP.setKeepTogether(true);
        ppqhDoc.add(ppqhP);
    }

    private Image ppqhChart(PdfWriter ppqhWriter, PpqhSalesReport ppqhReport) throws DocumentException {
        List<PpqhPeriodRow> ppqhRows = ppqhReport.ppqhPeriods();
        float ppqhW = PageSize.A4.getWidth() - 2 * PPQH_MARGIN;
        float ppqhH = 120;
        PdfTemplate ppqhTpl = ppqhWriter.getDirectContent().createTemplate(ppqhW, ppqhH);
        long ppqhMax = ppqhRows.stream().mapToLong(PpqhPeriodRow::ppqhTotalCents).max().orElse(0);
        float ppqhLeft = 4;
        float ppqhBottom = 14;
        float ppqhPlotH = ppqhH - ppqhBottom - 6;
        ppqhTpl.setColorStroke(ppqhLine);
        ppqhTpl.setLineWidth(0.5f);
        for (int ppqhI = 0; ppqhI <= 4; ppqhI++) {
            float ppqhY = ppqhBottom + ppqhPlotH * ppqhI / 4;
            ppqhTpl.moveTo(ppqhLeft, ppqhY);
            ppqhTpl.lineTo(ppqhW, ppqhY);
        }
        ppqhTpl.stroke();
        float ppqhSlot = (ppqhW - ppqhLeft) / Math.max(1, ppqhRows.size());
        float ppqhBar = Math.max(1.5f, ppqhSlot * 0.68f);
        ppqhTpl.setColorFill(ppqhBrand);
        for (int ppqhI = 0; ppqhI < ppqhRows.size(); ppqhI++) {
            long ppqhV = ppqhRows.get(ppqhI).ppqhTotalCents();
            if (ppqhV > 0 && ppqhMax > 0) {
                float ppqhBh = Math.max(1, ppqhPlotH * ppqhV / ppqhMax);
                ppqhTpl.rectangle(ppqhLeft + ppqhI * ppqhSlot + (ppqhSlot - ppqhBar) / 2, ppqhBottom, ppqhBar, ppqhBh);
            }
        }
        ppqhTpl.fill();
        Font ppqhSmall = new Font(ppqhRegular, 7, Font.NORMAL, ppqhMuted);
        ColumnText.showTextAligned(ppqhTpl, Element.ALIGN_LEFT, new Phrase(PpqhReportText.ppqhPeriod(ppqhReport, ppqhRows.get(0).start()), ppqhSmall), ppqhLeft, 3, 0);
        ColumnText.showTextAligned(ppqhTpl, Element.ALIGN_RIGHT,
                new Phrase(PpqhReportText.ppqhPeriod(ppqhReport, ppqhRows.get(ppqhRows.size() - 1).start()), ppqhSmall), ppqhW, 3, 0);
        ColumnText.showTextAligned(ppqhTpl, Element.ALIGN_LEFT, new Phrase("Máx. " + HepyMoney.hepyFormat(ppqhMax), ppqhSmall), ppqhLeft + 2, ppqhH - 7, 0);
        Image ppqhImg = Image.getInstance(ppqhTpl);
        ppqhImg.setSpacingAfter(8);
        return ppqhImg;
    }

    private PdfPTable ppqhPeriodTable(PpqhSalesReport ppqhR) {
        PdfPTable ppqhT = ppqhTable(new float[]{3.2f, 1.2f, 1.2f, 1.8f}, 1, PpqhReportText.ppqhPeriodHeader(ppqhR), "Pedidos", "Unidades", "Total");
        boolean ppqhDaily = ppqhR.ppqhGrouping() == PpqhSalesReport.PpqhGrouping.DAY;
        for (PpqhPeriodRow ppqhP : ppqhR.ppqhPeriods()) {
            if (ppqhDaily && ppqhP.ppqhOrders() == 0) {
                continue;
            }
            ppqhT.addCell(ppqhText(PpqhReportText.ppqhPeriod(ppqhR, ppqhP.start())));
            ppqhT.addCell(ppqhNumber(String.valueOf(ppqhP.ppqhOrders())));
            ppqhT.addCell(ppqhNumber(String.valueOf(ppqhP.ppqhUnits())));
            ppqhT.addCell(ppqhNumber(HepyMoney.hepyFormat(ppqhP.ppqhTotalCents())));
        }
        ppqhTotalRow(ppqhT, "Total", String.valueOf(ppqhR.ppqhOrders()), String.valueOf(ppqhR.ppqhUnits()), HepyMoney.hepyFormat(ppqhR.ppqhTotalCents()));
        return ppqhT;
    }

    private PdfPTable ppqhCategoryTable(PpqhSalesReport ppqhR) {
        PdfPTable ppqhT = ppqhTable(new float[]{3.2f, 1.1f, 1.1f, 1.7f, 1.1f}, 1, "Categoría", "Pedidos", "Unidades", "Ventas", "% del total");
        for (PpqhCategoryRow ppqhC : ppqhR.ppqhCategories()) {
            ppqhT.addCell(ppqhText(ppqhC.ppqhCategory()));
            ppqhT.addCell(ppqhNumber(String.valueOf(ppqhC.ppqhOrders())));
            ppqhT.addCell(ppqhNumber(String.valueOf(ppqhC.ppqhUnits())));
            ppqhT.addCell(ppqhNumber(HepyMoney.hepyFormat(ppqhC.ppqhRevenueCents())));
            ppqhT.addCell(ppqhNumber(PpqhReportText.ppqhPercent(ppqhR.ppqhSharePercent(ppqhC.ppqhRevenueCents()))));
        }
        ppqhTotalRow(ppqhT, "Total productos", "", String.valueOf(ppqhR.ppqhUnits()), HepyMoney.hepyFormat(ppqhR.ppqhProductsCents()), "100.0 %");
        return ppqhT;
    }

    private PdfPTable ppqhProductTable(PpqhSalesReport ppqhR) {
        PdfPTable ppqhT = ppqhTable(new float[]{1.7f, 3.4f, 1.9f, 1f, 1.6f}, 3, "SKU", "Producto", "Categoría", "Unidades", "Ventas");
        for (PpqhProductRow ppqhP : ppqhR.ppqhProducts()) {
            PdfPCell ppqhSku = ppqhText(ppqhP.ppqhSku());
            ppqhSku.setPhrase(new Phrase(ppqhP.ppqhSku(), new Font(ppqhMono, 7.5f, Font.NORMAL, ppqhMuted)));
            ppqhT.addCell(ppqhSku);
            ppqhT.addCell(ppqhText(ppqhP.ppqhName()));
            ppqhT.addCell(ppqhText(ppqhP.ppqhCategory()));
            ppqhT.addCell(ppqhNumber(String.valueOf(ppqhP.ppqhUnits())));
            ppqhT.addCell(ppqhNumber(HepyMoney.hepyFormat(ppqhP.ppqhRevenueCents())));
        }
        ppqhTotalRow(ppqhT, "Total productos", "", "", String.valueOf(ppqhR.ppqhUnits()), HepyMoney.hepyFormat(ppqhR.ppqhProductsCents()));
        return ppqhT;
    }

    private PdfPTable ppqhMethodTable(PpqhSalesReport ppqhR) {
        PdfPTable ppqhT = ppqhTable(new float[]{3.2f, 1.2f, 1.8f}, 1, "Método", "Pedidos", "Total");
        for (PpqhMethodRow ppqhM : ppqhR.ppqhMethods()) {
            ppqhT.addCell(ppqhText(ppqhM.ppqhMethod().ppqhLabel()));
            ppqhT.addCell(ppqhNumber(String.valueOf(ppqhM.ppqhOrders())));
            ppqhT.addCell(ppqhNumber(HepyMoney.hepyFormat(ppqhM.ppqhTotalCents())));
        }
        return ppqhT;
    }

    private PdfPTable ppqhTable(float[] ppqhWidths, int ppqhTextColumns, String... ppqhHeaders) {
        PdfPTable ppqhT = new PdfPTable(ppqhWidths);
        ppqhT.setWidthPercentage(100);
        ppqhT.setHeaderRows(1);
        ppqhT.setSpacingBefore(8);
        ppqhT.setSpacingAfter(4);
        for (int ppqhI = 0; ppqhI < ppqhHeaders.length; ppqhI++) {
            PdfPCell ppqhC = new PdfPCell(new Phrase(ppqhHeaders[ppqhI].toUpperCase(PpqhReportText.PPQH_ES_PE), new Font(ppqhSemibold, 7.5f, Font.NORMAL, ppqhMuted)));
            ppqhC.setBackgroundColor(ppqhBand);
            ppqhC.setBorder(Rectangle.BOTTOM);
            ppqhC.setBorderColor(ppqhLine);
            ppqhC.setPadding(6);
            ppqhC.setHorizontalAlignment(ppqhI < ppqhTextColumns ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            ppqhT.addCell(ppqhC);
        }
        return ppqhT;
    }

    private PdfPCell ppqhText(String ppqhValue) {
        PdfPCell ppqhC = new PdfPCell(new Phrase(ppqhValue == null ? "" : ppqhValue, new Font(ppqhRegular, 9, Font.NORMAL, ppqhInk)));
        ppqhC.setBorder(Rectangle.BOTTOM);
        ppqhC.setBorderColor(ppqhLine);
        ppqhC.setBorderWidth(0.5f);
        ppqhC.setPadding(5);
        return ppqhC;
    }

    private PdfPCell ppqhNumber(String ppqhValue) {
        PdfPCell ppqhC = ppqhText(ppqhValue);
        ppqhC.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return ppqhC;
    }

    private void ppqhTotalRow(PdfPTable ppqhT, String... ppqhValues) {
        for (int ppqhI = 0; ppqhI < ppqhValues.length; ppqhI++) {
            PdfPCell ppqhC = new PdfPCell(new Phrase(ppqhValues[ppqhI], new Font(ppqhSemibold, 9, Font.NORMAL, ppqhInk)));
            ppqhC.setBorder(Rectangle.TOP);
            ppqhC.setBorderColor(ppqhColor("line-strong"));
            ppqhC.setPadding(6);
            ppqhC.setHorizontalAlignment(ppqhI == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            ppqhT.addCell(ppqhC);
        }
    }

    private static Paragraph ppqhParagraph(String ppqhText, Font ppqhFont, float ppqhSpacingBefore) {
        Paragraph ppqhP = new Paragraph(ppqhText, ppqhFont);
        ppqhP.setSpacingBefore(ppqhSpacingBefore);
        return ppqhP;
    }

    private static Color ppqhColor(String ppqhToken) {
        return new Color(PpqhReportPalette.ppqhRgb(ppqhToken));
    }

    private static BaseFont ppqhFont(String ppqhFile) {
        try (InputStream ppqhIn = PpqhPdfReportWriter.class.getResourceAsStream("/fonts/" + ppqhFile)) {
            if (ppqhIn == null) {
                throw new IllegalStateException("Falta la fuente " + ppqhFile);
            }
            return BaseFont.createFont(ppqhFile, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, ppqhIn.readAllBytes(), null);
        } catch (IOException ppqhE) {
            throw new UncheckedIOException(ppqhE);
        } catch (DocumentException ppqhE) {
            throw new IllegalStateException("No se pudo cargar la fuente " + ppqhFile, ppqhE);
        }
    }

    private final class PpqhFooter extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter ppqhWriter, Document ppqhDocument) {
            PdfContentByte ppqhCb = ppqhWriter.getDirectContent();
            Font ppqhF = new Font(ppqhRegular, 8, Font.NORMAL, ppqhSubtle);
            float ppqhY = ppqhDocument.bottomMargin() - 20;
            ColumnText.showTextAligned(ppqhCb, Element.ALIGN_LEFT, new Phrase("SPORTFORCE 3D · Reporte de ventas", ppqhF),
                    ppqhDocument.left(), ppqhY, 0);
            ColumnText.showTextAligned(ppqhCb, Element.ALIGN_RIGHT, new Phrase("Página " + ppqhWriter.getPageNumber(), ppqhF),
                    ppqhDocument.right(), ppqhY, 0);
        }
    }
}
