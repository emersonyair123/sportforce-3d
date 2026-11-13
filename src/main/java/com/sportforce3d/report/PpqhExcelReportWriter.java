package com.sportforce3d.report;

import com.sportforce3d.model.PpqhSalesReport;
import com.sportforce3d.model.PpqhSalesReport.PpqhCategoryRow;
import com.sportforce3d.model.PpqhSalesReport.PpqhMethodRow;
import com.sportforce3d.model.PpqhSalesReport.PpqhPeriodRow;
import com.sportforce3d.model.PpqhSalesReport.PpqhProductRow;
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

public class PpqhExcelReportWriter {

    private static final String PPQH_SOLES = "\"S/ \"#,##0.00";

    private XSSFWorkbook ppqhWb;
    private CellStyle ppqhHeader;
    private CellStyle ppqhText;
    private CellStyle ppqhInteger;
    private CellStyle ppqhMoney;
    private CellStyle ppqhPercent;
    private CellStyle ppqhDate;
    private CellStyle ppqhTotalText;
    private CellStyle ppqhTotalInteger;
    private CellStyle ppqhTotalMoney;
    private CellStyle ppqhTitle;
    private CellStyle ppqhMuted;

    public void ppqhWrite(PpqhSalesReport ppqhReport, OutputStream ppqhOut) throws IOException {
        try (XSSFWorkbook ppqhWorkbook = new XSSFWorkbook()) {
            ppqhWb = ppqhWorkbook;
            ppqhStyles();
            ppqhSummary(ppqhReport);
            ppqhPeriods(ppqhReport);
            ppqhCategories(ppqhReport);
            ppqhProducts(ppqhReport);
            ppqhMethods(ppqhReport);
            ppqhWb.getProperties().getCoreProperties().setTitle("Reporte de ventas · SPORTFORCE 3D");
            ppqhWb.getProperties().getCoreProperties().setCreator(ppqhReport.ppqhGeneratedBy());
            ppqhWb.write(ppqhOut);
        }
    }

    private void ppqhSummary(PpqhSalesReport ppqhR) {
        XSSFSheet ppqhS = ppqhWb.createSheet("Resumen");
        ppqhCell(ppqhS, 0, 0, "SPORTFORCE 3D · Reporte de ventas", ppqhTitle);
        ppqhCell(ppqhS, 1, 0, PpqhReportText.ppqhRange(ppqhR), ppqhText);
        ppqhCell(ppqhS, 2, 0, PpqhReportText.ppqhGenerated(ppqhR), ppqhMuted);
        String[][] ppqhRows = {
                {"Ventas totales (cobrado)", null},
                {"Ventas de productos (sin envío)", null},
                {"Envío", null},
                {"Pedidos pagados", null},
                {"Unidades vendidas", null},
                {"Ticket promedio", null},
        };
        long[] ppqhValues = {ppqhR.ppqhTotalCents(), ppqhR.ppqhProductsCents(), ppqhR.ppqhShippingCents(), ppqhR.ppqhOrders(), ppqhR.ppqhUnits(), ppqhR.ppqhAverageTicketCents()};
        boolean[] ppqhIsMoney = {true, true, true, false, false, true};
        for (int ppqhI = 0; ppqhI < ppqhRows.length; ppqhI++) {
            ppqhCell(ppqhS, 4 + ppqhI, 0, ppqhRows[ppqhI][0], ppqhText);
            Cell ppqhC = ppqhS.getRow(4 + ppqhI).createCell(1);
            if (ppqhIsMoney[ppqhI]) {
                ppqhC.setCellValue(ppqhValues[ppqhI] / 100.0);
                ppqhC.setCellStyle(ppqhMoney);
            } else {
                ppqhC.setCellValue(ppqhValues[ppqhI]);
                ppqhC.setCellStyle(ppqhInteger);
            }
        }
        ppqhCell(ppqhS, 11, 0, PpqhReportText.PPQH_DEFINITION, ppqhMuted);
        ppqhS.addMergedRegion(new CellRangeAddress(11, 11, 0, 5));
        ppqhS.setColumnWidth(0, 36 * 256);
        ppqhS.setColumnWidth(1, 18 * 256);
    }

    private void ppqhPeriods(PpqhSalesReport ppqhR) {
        boolean ppqhDaily = ppqhR.ppqhGrouping() == PpqhSalesReport.PpqhGrouping.DAY;
        XSSFSheet ppqhS = ppqhSheet(ppqhDaily ? "Por día" : "Por mes", PpqhReportText.ppqhPeriodHeader(ppqhR), "Pedidos", "Unidades", "Total");
        List<PpqhPeriodRow> ppqhRows = ppqhR.ppqhPeriods();
        ppqhFill(ppqhS, ppqhRows, (ppqhRow, ppqhP) -> {
            Cell ppqhD = ppqhRow.createCell(0);
            if (ppqhDaily) {
                ppqhD.setCellValue(ppqhP.start());
                ppqhD.setCellStyle(ppqhDate);
            } else {
                ppqhD.setCellValue(PpqhReportText.ppqhPeriod(ppqhR, ppqhP.start()));
                ppqhD.setCellStyle(ppqhText);
            }
            ppqhNumber(ppqhRow, 1, ppqhP.ppqhOrders());
            ppqhNumber(ppqhRow, 2, ppqhP.ppqhUnits());
            ppqhMoney(ppqhRow, 3, ppqhP.ppqhTotalCents());
        });
        ppqhTotals(ppqhS, ppqhRows.size(), 1, 2, 3);
        ppqhWidths(ppqhS, 22, 12, 12, 16);
    }

    private void ppqhCategories(PpqhSalesReport ppqhR) {
        XSSFSheet ppqhS = ppqhSheet("Por categoría", "Categoría", "Pedidos", "Unidades", "Ventas", "% del total");
        List<PpqhCategoryRow> ppqhRows = ppqhR.ppqhCategories();
        int ppqhLast = ppqhRows.size() + 1;
        ppqhFill(ppqhS, ppqhRows, (ppqhRow, ppqhC) -> {
            ppqhCell(ppqhRow, 0, ppqhC.ppqhCategory());
            ppqhNumber(ppqhRow, 1, ppqhC.ppqhOrders());
            ppqhNumber(ppqhRow, 2, ppqhC.ppqhUnits());
            ppqhMoney(ppqhRow, 3, ppqhC.ppqhRevenueCents());
            Cell ppqhPct = ppqhRow.createCell(4);
            ppqhPct.setCellFormula("IF(SUM(D$2:D$" + ppqhLast + ")=0,0,D" + (ppqhRow.getRowNum() + 1) + "/SUM(D$2:D$" + ppqhLast + "))");
            ppqhPct.setCellStyle(ppqhPercent);
        });
        ppqhTotals(ppqhS, ppqhRows.size(), 2, 3);
        ppqhWidths(ppqhS, 28, 12, 12, 16, 14);
    }

    private void ppqhProducts(PpqhSalesReport ppqhR) {
        XSSFSheet ppqhS = ppqhSheet("Por producto", "SKU", "Producto", "Categoría", "Pedidos", "Unidades", "Ventas");
        List<PpqhProductRow> ppqhRows = ppqhR.ppqhProducts();
        ppqhFill(ppqhS, ppqhRows, (ppqhRow, ppqhP) -> {
            ppqhCell(ppqhRow, 0, ppqhP.ppqhSku());
            ppqhCell(ppqhRow, 1, ppqhP.ppqhName());
            ppqhCell(ppqhRow, 2, ppqhP.ppqhCategory());
            ppqhNumber(ppqhRow, 3, ppqhP.ppqhOrders());
            ppqhNumber(ppqhRow, 4, ppqhP.ppqhUnits());
            ppqhMoney(ppqhRow, 5, ppqhP.ppqhRevenueCents());
        });
        ppqhTotals(ppqhS, ppqhRows.size(), 4, 5);
        ppqhWidths(ppqhS, 18, 40, 22, 10, 10, 16);
    }

    private void ppqhMethods(PpqhSalesReport ppqhR) {
        XSSFSheet ppqhS = ppqhSheet("Por método", "Método", "Pedidos", "Total");
        List<PpqhMethodRow> ppqhRows = ppqhR.ppqhMethods();
        ppqhFill(ppqhS, ppqhRows, (ppqhRow, ppqhM) -> {
            ppqhCell(ppqhRow, 0, ppqhM.ppqhMethod().ppqhLabel());
            ppqhNumber(ppqhRow, 1, ppqhM.ppqhOrders());
            ppqhMoney(ppqhRow, 2, ppqhM.ppqhTotalCents());
        });
        ppqhTotals(ppqhS, ppqhRows.size(), 1, 2);
        ppqhWidths(ppqhS, 28, 12, 16);
    }

    private XSSFSheet ppqhSheet(String ppqhName, String... ppqhHeaders) {
        XSSFSheet ppqhS = ppqhWb.createSheet(ppqhName);
        Row ppqhRow = ppqhS.createRow(0);
        for (int ppqhI = 0; ppqhI < ppqhHeaders.length; ppqhI++) {
            Cell ppqhC = ppqhRow.createCell(ppqhI);
            ppqhC.setCellValue(ppqhHeaders[ppqhI].toUpperCase(PpqhReportText.PPQH_ES_PE));
            ppqhC.setCellStyle(ppqhHeader);
        }
        ppqhS.createFreezePane(0, 1);
        return ppqhS;
    }

    private static <T> void ppqhFill(XSSFSheet ppqhS, List<T> ppqhRows, BiConsumer<Row, T> ppqhWriter) {
        for (int ppqhI = 0; ppqhI < ppqhRows.size(); ppqhI++) {
            ppqhWriter.accept(ppqhS.createRow(ppqhI + 1), ppqhRows.get(ppqhI));
        }
    }

    private void ppqhTotals(XSSFSheet ppqhS, int ppqhDataRows, int... ppqhColumns) {
        Row ppqhRow = ppqhS.createRow(ppqhDataRows + 1);
        Cell ppqhLabel = ppqhRow.createCell(0);
        ppqhLabel.setCellValue("Total");
        ppqhLabel.setCellStyle(ppqhTotalText);
        int ppqhLastCol = ppqhS.getRow(0).getLastCellNum() - 1;
        for (int ppqhCol = 1; ppqhCol <= ppqhLastCol; ppqhCol++) {
            Cell ppqhC = ppqhRow.createCell(ppqhCol);
            ppqhC.setCellStyle(ppqhTotalText);
        }
        for (int ppqhCol : ppqhColumns) {
            String ppqhLetter = String.valueOf((char) ('A' + ppqhCol));
            Cell ppqhC = ppqhRow.getCell(ppqhCol);
            ppqhC.setCellFormula(ppqhDataRows == 0 ? "0" : "SUM(" + ppqhLetter + "2:" + ppqhLetter + (ppqhDataRows + 1) + ")");
            boolean ppqhIsMoney = ppqhS.getRow(0).getCell(ppqhCol).getStringCellValue().matches("TOTAL|VENTAS");
            ppqhC.setCellStyle(ppqhIsMoney ? ppqhTotalMoney : ppqhTotalInteger);
        }
    }

    private void ppqhCell(XSSFSheet ppqhS, int ppqhR, int ppqhC, String ppqhValue, CellStyle ppqhStyle) {
        Row ppqhRow = ppqhS.getRow(ppqhR) == null ? ppqhS.createRow(ppqhR) : ppqhS.getRow(ppqhR);
        Cell ppqhCell = ppqhRow.createCell(ppqhC);
        ppqhCell.setCellValue(ppqhValue);
        ppqhCell.setCellStyle(ppqhStyle);
    }

    private void ppqhCell(Row ppqhRow, int ppqhC, String ppqhValue) {
        Cell ppqhCell = ppqhRow.createCell(ppqhC);
        ppqhCell.setCellValue(ppqhValue == null ? "" : ppqhValue);
        ppqhCell.setCellStyle(ppqhText);
    }

    private void ppqhNumber(Row ppqhRow, int ppqhC, long ppqhValue) {
        Cell ppqhCell = ppqhRow.createCell(ppqhC);
        ppqhCell.setCellValue(ppqhValue);
        ppqhCell.setCellStyle(ppqhInteger);
    }

    private void ppqhMoney(Row ppqhRow, int ppqhC, long ppqhCents) {
        Cell ppqhCell = ppqhRow.createCell(ppqhC);
        ppqhCell.setCellValue(ppqhCents / 100.0);
        ppqhCell.setCellStyle(ppqhMoney);
    }

    private static void ppqhWidths(XSSFSheet ppqhS, int... ppqhChars) {
        for (int ppqhI = 0; ppqhI < ppqhChars.length; ppqhI++) {
            ppqhS.setColumnWidth(ppqhI, ppqhChars[ppqhI] * 256);
        }
    }

    private void ppqhStyles() {
        XSSFFont ppqhBold = ppqhFont(true, "ink", 11);
        XSSFFont ppqhHeaderFont = ppqhFont(true, "on-brand", 10);
        XSSFFont ppqhTitleFont = ppqhFont(true, "ink", 16);
        XSSFFont ppqhMutedFont = ppqhFont(false, "ink-muted", 10);

        ppqhHeader = ppqhStyle(ppqhHeaderFont, HorizontalAlignment.LEFT, null);
        ((XSSFCellStyle) ppqhHeader).setFillForegroundColor(ppqhColor("brand"));
        ppqhHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        ppqhText = ppqhStyle(null, HorizontalAlignment.LEFT, null);
        ppqhInteger = ppqhStyle(null, HorizontalAlignment.RIGHT, "#,##0");
        ppqhMoney = ppqhStyle(null, HorizontalAlignment.RIGHT, PPQH_SOLES);
        ppqhPercent = ppqhStyle(null, HorizontalAlignment.RIGHT, "0.0%");
        ppqhDate = ppqhStyle(null, HorizontalAlignment.LEFT, "ddd dd/mm/yyyy");
        ppqhTotalText = ppqhStyle(ppqhBold, HorizontalAlignment.LEFT, null);
        ppqhTotalInteger = ppqhStyle(ppqhBold, HorizontalAlignment.RIGHT, "#,##0");
        ppqhTotalMoney = ppqhStyle(ppqhBold, HorizontalAlignment.RIGHT, PPQH_SOLES);
        for (CellStyle ppqhS : List.of(ppqhTotalText, ppqhTotalInteger, ppqhTotalMoney)) {
            ppqhS.setBorderTop(BorderStyle.THIN);
            ((XSSFCellStyle) ppqhS).setTopBorderColor(ppqhColor("line-strong"));
        }
        ppqhTitle = ppqhStyle(ppqhTitleFont, HorizontalAlignment.LEFT, null);
        ppqhMuted = ppqhStyle(ppqhMutedFont, HorizontalAlignment.LEFT, null);
        ppqhMuted.setWrapText(false);
    }

    private CellStyle ppqhStyle(XSSFFont ppqhFont, HorizontalAlignment ppqhAlign, String ppqhFormat) {
        XSSFCellStyle ppqhS = ppqhWb.createCellStyle();
        if (ppqhFont != null) {
            ppqhS.setFont(ppqhFont);
        }
        ppqhS.setAlignment(ppqhAlign);
        if (ppqhFormat != null) {
            ppqhS.setDataFormat(ppqhWb.createDataFormat().getFormat(ppqhFormat));
        }
        return ppqhS;
    }

    private XSSFFont ppqhFont(boolean ppqhBold, String ppqhToken, int ppqhPoints) {
        XSSFFont ppqhF = ppqhWb.createFont();
        ppqhF.setFontName("Calibri");
        ppqhF.setBold(ppqhBold);
        ppqhF.setFontHeightInPoints((short) ppqhPoints);
        ppqhF.setColor(ppqhColor(ppqhToken));
        return ppqhF;
    }

    private XSSFColor ppqhColor(String ppqhToken) {
        int ppqhRgb = PpqhReportPalette.ppqhRgb(ppqhToken);
        return new XSSFColor(new byte[]{(byte) (ppqhRgb >> 16), (byte) (ppqhRgb >> 8), (byte) ppqhRgb}, null);
    }
}
