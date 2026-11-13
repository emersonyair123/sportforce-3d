package com.sportforce3d.report;

import com.sportforce3d.model.SalesReport;
import com.sportforce3d.model.SalesReport.CategoryRow;
import com.sportforce3d.model.SalesReport.MethodRow;
import com.sportforce3d.model.SalesReport.PeriodRow;
import com.sportforce3d.model.SalesReport.ProductRow;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.BiConsumer;

public class ExcelReportWriter {

    private static final String SOLES = "\"S/ \"#,##0.00";

    private XSSFWorkbook wb;
    private CellStyle header;
    private CellStyle text;
    private CellStyle integer;
    private CellStyle money;
    private CellStyle percent;
    private CellStyle date;
    private CellStyle totalText;
    private CellStyle totalInteger;
    private CellStyle totalMoney;
    private CellStyle title;
    private CellStyle muted;

    public void write(SalesReport report, OutputStream out) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            wb = workbook;
            styles();
            summary(report);
            periods(report);
            categories(report);
            products(report);
            methods(report);
            wb.getProperties().getCoreProperties().setTitle("Reporte de ventas · SPORTFORCE 3D");
            wb.getProperties().getCoreProperties().setCreator(report.generatedBy());
            wb.write(out);
        }
    }

    private void summary(SalesReport r) {
        XSSFSheet s = wb.createSheet("Resumen");
        cell(s, 0, 0, "SPORTFORCE 3D · Reporte de ventas", title);
        cell(s, 1, 0, ReportText.range(r), text);
        cell(s, 2, 0, ReportText.generated(r), muted);
        String[][] rows = {
                {"Ventas totales (cobrado)", null},
                {"Ventas de productos (sin envío)", null},
                {"Envío", null},
                {"Pedidos pagados", null},
                {"Unidades vendidas", null},
                {"Ticket promedio", null},
        };
        long[] values = {r.totalCents(), r.productsCents(), r.shippingCents(), r.orders(), r.units(), r.averageTicketCents()};
        boolean[] isMoney = {true, true, true, false, false, true};
        for (int i = 0; i < rows.length; i++) {
            cell(s, 4 + i, 0, rows[i][0], text);
            Cell c = s.getRow(4 + i).createCell(1);
            if (isMoney[i]) {
                c.setCellValue(values[i] / 100.0);
                c.setCellStyle(money);
            } else {
                c.setCellValue(values[i]);
                c.setCellStyle(integer);
            }
        }
        cell(s, 11, 0, ReportText.DEFINITION, muted);
        s.addMergedRegion(new CellRangeAddress(11, 11, 0, 5));
        s.setColumnWidth(0, 36 * 256);
        s.setColumnWidth(1, 18 * 256);
    }

    private void periods(SalesReport r) {
        boolean daily = r.grouping() == SalesReport.Grouping.DAY;
        XSSFSheet s = sheet(daily ? "Por día" : "Por mes", ReportText.periodHeader(r), "Pedidos", "Unidades", "Total");
        List<PeriodRow> rows = r.periods();
        fill(s, rows, (row, p) -> {
            Cell d = row.createCell(0);
            if (daily) {
                d.setCellValue(p.start());
                d.setCellStyle(date);
            } else {
                d.setCellValue(ReportText.period(r, p.start()));
                d.setCellStyle(text);
            }
            number(row, 1, p.orders());
            number(row, 2, p.units());
            money(row, 3, p.totalCents());
        });
        totals(s, rows.size(), 1, 2, 3);
        widths(s, 22, 12, 12, 16);
    }

    private void categories(SalesReport r) {
        XSSFSheet s = sheet("Por categoría", "Categoría", "Pedidos", "Unidades", "Ventas", "% del total");
        List<CategoryRow> rows = r.categories();
        int last = rows.size() + 1;
        fill(s, rows, (row, c) -> {
            cell(row, 0, c.category());
            number(row, 1, c.orders());
            number(row, 2, c.units());
            money(row, 3, c.revenueCents());
            Cell pct = row.createCell(4);
            pct.setCellFormula("IF(SUM(D$2:D$" + last + ")=0,0,D" + (row.getRowNum() + 1) + "/SUM(D$2:D$" + last + "))");
            pct.setCellStyle(percent);
        });
        totals(s, rows.size(), 2, 3);
        widths(s, 28, 12, 12, 16, 14);
    }

    private void products(SalesReport r) {
        XSSFSheet s = sheet("Por producto", "SKU", "Producto", "Categoría", "Pedidos", "Unidades", "Ventas");
        List<ProductRow> rows = r.products();
        fill(s, rows, (row, p) -> {
            cell(row, 0, p.sku());
            cell(row, 1, p.name());
            cell(row, 2, p.category());
            number(row, 3, p.orders());
            number(row, 4, p.units());
            money(row, 5, p.revenueCents());
        });
        totals(s, rows.size(), 4, 5);
        widths(s, 18, 40, 22, 10, 10, 16);
    }

    private void methods(SalesReport r) {
        XSSFSheet s = sheet("Por método", "Método", "Pedidos", "Total");
        List<MethodRow> rows = r.methods();
        fill(s, rows, (row, m) -> {
            cell(row, 0, m.method().label());
            number(row, 1, m.orders());
            money(row, 2, m.totalCents());
        });
        totals(s, rows.size(), 1, 2);
        widths(s, 28, 12, 16);
    }

    private XSSFSheet sheet(String name, String... headers) {
        XSSFSheet s = wb.createSheet(name);
        Row row = s.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(headers[i].toUpperCase(ReportText.ES_PE));
            c.setCellStyle(header);
        }
        s.createFreezePane(0, 1);
        return s;
    }

    private static <T> void fill(XSSFSheet s, List<T> rows, BiConsumer<Row, T> writer) {
        for (int i = 0; i < rows.size(); i++) {
            writer.accept(s.createRow(i + 1), rows.get(i));
        }
    }

    private void totals(XSSFSheet s, int dataRows, int... columns) {
        Row row = s.createRow(dataRows + 1);
        Cell label = row.createCell(0);
        label.setCellValue("Total");
        label.setCellStyle(totalText);
        int lastCol = s.getRow(0).getLastCellNum() - 1;
        for (int col = 1; col <= lastCol; col++) {
            Cell c = row.createCell(col);
            c.setCellStyle(totalText);
        }
        for (int col : columns) {
            String letter = String.valueOf((char) ('A' + col));
            Cell c = row.getCell(col);
            c.setCellFormula(dataRows == 0 ? "0" : "SUM(" + letter + "2:" + letter + (dataRows + 1) + ")");
            boolean isMoney = s.getRow(0).getCell(col).getStringCellValue().matches("TOTAL|VENTAS");
            c.setCellStyle(isMoney ? totalMoney : totalInteger);
        }
    }

    private void cell(XSSFSheet s, int r, int c, String value, CellStyle style) {
        Row row = s.getRow(r) == null ? s.createRow(r) : s.getRow(r);
        Cell cell = row.createCell(c);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void cell(Row row, int c, String value) {
        Cell cell = row.createCell(c);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(text);
    }

    private void number(Row row, int c, long value) {
        Cell cell = row.createCell(c);
        cell.setCellValue(value);
        cell.setCellStyle(integer);
    }

    private void money(Row row, int c, long cents) {
        Cell cell = row.createCell(c);
        cell.setCellValue(cents / 100.0);
        cell.setCellStyle(money);
    }

    private static void widths(XSSFSheet s, int... chars) {
        for (int i = 0; i < chars.length; i++) {
            s.setColumnWidth(i, chars[i] * 256);
        }
    }

    private void styles() {
        XSSFFont bold = font(true, "ink", 11);
        XSSFFont headerFont = font(true, "on-brand", 10);
        XSSFFont titleFont = font(true, "ink", 16);
        XSSFFont mutedFont = font(false, "ink-muted", 10);

        header = style(headerFont, HorizontalAlignment.LEFT, null);
        ((XSSFCellStyle) header).setFillForegroundColor(color("brand"));
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        text = style(null, HorizontalAlignment.LEFT, null);
        integer = style(null, HorizontalAlignment.RIGHT, "#,##0");
        money = style(null, HorizontalAlignment.RIGHT, SOLES);
        percent = style(null, HorizontalAlignment.RIGHT, "0.0%");
        date = style(null, HorizontalAlignment.LEFT, "ddd dd/mm/yyyy");
        totalText = style(bold, HorizontalAlignment.LEFT, null);
        totalInteger = style(bold, HorizontalAlignment.RIGHT, "#,##0");
        totalMoney = style(bold, HorizontalAlignment.RIGHT, SOLES);
        for (CellStyle s : List.of(totalText, totalInteger, totalMoney)) {
            s.setBorderTop(BorderStyle.THIN);
            ((XSSFCellStyle) s).setTopBorderColor(color("line-strong"));
        }
        title = style(titleFont, HorizontalAlignment.LEFT, null);
        muted = style(mutedFont, HorizontalAlignment.LEFT, null);
        muted.setWrapText(false);
    }

    private CellStyle style(XSSFFont font, HorizontalAlignment align, String format) {
        XSSFCellStyle s = wb.createCellStyle();
        if (font != null) {
            s.setFont(font);
        }
        s.setAlignment(align);
        if (format != null) {
            s.setDataFormat(wb.createDataFormat().getFormat(format));
        }
        return s;
    }

    private XSSFFont font(boolean bold, String token, int points) {
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setBold(bold);
        f.setFontHeightInPoints((short) points);
        f.setColor(color(token));
        return f;
    }

    private XSSFColor color(String token) {
        int rgb = ReportPalette.rgb(token);
        return new XSSFColor(new byte[]{(byte) (rgb >> 16), (byte) (rgb >> 8), (byte) rgb}, null);
    }
}
