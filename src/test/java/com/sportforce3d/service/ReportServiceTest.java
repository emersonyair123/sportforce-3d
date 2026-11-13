package com.sportforce3d.service;

import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.model.PpqhSalesReport;
import com.sportforce3d.model.PpqhSalesReport.PpqhGrouping;
import com.sportforce3d.model.PpqhSalesReport.PpqhPeriodRow;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportServiceTest {

    private static final LocalDate OCT_17 = LocalDate.of(2026, 10, 17);
    private static final LocalDate OCT_18 = LocalDate.of(2026, 10, 18);

    @TempDir
    Path tempDir;

    private Fixture f;
    private Path proof;
    private int code = 10_000_000;

    @BeforeEach
    void setUp() throws Exception {
        f = new Fixture(tempDir);
        proof = tempDir.resolve("captura.png");
        Files.write(proof, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0});

        sell("SF-CAM-001-M", 2, PpqhPaymentMethod.YAPE);
        f.clock.advance(Duration.ofHours(13).plusMinutes(30));
        sell("SF-UFU-001-M", 1, PpqhPaymentMethod.PLIN);
        f.clock.advance(Duration.ofHours(9).plusMinutes(30));
        sell("SF-CAM-001-L", 1, PpqhPaymentMethod.TRANSFERENCIA);
        f.loginClient();
        f.cart.hepyAdd(f.variantId("SF-CAM-002-M"), 3);
        f.orders.ppqhPlaceOrder(f.addresses.ppqhList().get(0).ppqhId(), PpqhPaymentMethod.YAPE, null);
        f.loginAdmin();
    }

    private void sell(String sku, int quantity, PpqhPaymentMethod method) {
        f.loginClient();
        f.cart.hepyAdd(f.variantId(sku), quantity);
        PpqhOrderDetail order = f.orders.ppqhPlaceOrder(f.addresses.ppqhList().get(0).ppqhId(), method, null);
        PpqhOrderDetail registered = f.payments.ppqhRegister(order.ppqhId(), String.valueOf(code++), proof);
        f.loginAdmin();
        f.payments.ppqhApprove(registered.ppqhCurrentPayment().ppqhId(), null);
    }

    @Test
    void salesByDayCountApprovedPaymentsOnTheirLimaDay() {
        PpqhSalesReport r = f.reports.ppqhSales(OCT_17, OCT_18);

        assertThat(r.ppqhGrouping()).isEqualTo(PpqhGrouping.DAY);
        assertThat(r.ppqhOrders()).isEqualTo(3);
        assertThat(r.ppqhUnits()).isEqualTo(4);
        assertThat(r.ppqhProductsCents()).isEqualTo(2 * 7990 + 14990 + 7990);
        assertThat(r.ppqhShippingCents()).isEqualTo(3 * 1500);
        assertThat(r.ppqhTotalCents()).isEqualTo(r.ppqhProductsCents() + r.ppqhShippingCents());
        assertThat(r.ppqhAverageTicketCents()).isEqualTo(Math.round(r.ppqhTotalCents() / 3.0));
        assertThat(r.ppqhPeriods()).extracting(PpqhPeriodRow::start).containsExactly(OCT_17, OCT_18);
        assertThat(r.ppqhPeriods().get(0)).isEqualTo(new PpqhPeriodRow(OCT_17, 2, 3, 2 * 7990 + 1500 + 14990 + 1500));
        assertThat(r.ppqhPeriods().get(1)).isEqualTo(new PpqhPeriodRow(OCT_18, 1, 1, 7990 + 1500));

        assertThat(f.reports.ppqhSales(OCT_18, OCT_18).ppqhOrders()).isEqualTo(1);
        assertThat(f.reports.ppqhSales(OCT_17.minusDays(5), OCT_17.minusDays(1)).ppqhIsEmpty()).isTrue();
    }

    @Test
    void salesByCategoryAndProductExcludeShipping() {
        PpqhSalesReport r = f.reports.ppqhSales(OCT_17, OCT_18);

        assertThat(r.ppqhCategories()).hasSize(2);
        assertThat(r.ppqhCategories().get(0).ppqhCategory()).isEqualTo("Camisetas deportivas");
        assertThat(r.ppqhCategories().get(0).ppqhUnits()).isEqualTo(3);
        assertThat(r.ppqhCategories().get(0).ppqhOrders()).isEqualTo(2);
        assertThat(r.ppqhCategories().get(0).ppqhRevenueCents()).isEqualTo(3 * 7990);
        assertThat(r.ppqhCategories().get(1).ppqhCategory()).isEqualTo("Uniformes de fútbol");
        assertThat(r.ppqhCategories().stream().mapToLong(c -> c.ppqhRevenueCents()).sum()).isEqualTo(r.ppqhProductsCents());
        assertThat(r.ppqhSharePercent(3 * 7990)).isEqualTo(61.5);

        assertThat(r.ppqhProducts()).extracting(p -> p.ppqhSku()).containsExactly("SF-CAM-001", "SF-UFU-001");
        assertThat(r.ppqhProducts().get(0).ppqhUnits()).isEqualTo(3);
        assertThat(r.ppqhProducts().get(0).ppqhName()).isEqualTo("Camiseta Pro Dry-Fit");
        assertThat(r.ppqhMethods()).extracting(m -> m.ppqhMethod())
                .containsExactlyInAnyOrder(PpqhPaymentMethod.YAPE, PpqhPaymentMethod.PLIN, PpqhPaymentMethod.TRANSFERENCIA);
    }

    @Test
    void longRangesAreGroupedByMonthWithEmptyMonths() {
        PpqhSalesReport r = f.reports.ppqhSales(LocalDate.of(2026, 7, 15), OCT_18);
        assertThat(r.ppqhGrouping()).isEqualTo(PpqhGrouping.MONTH);
        assertThat(r.ppqhPeriods()).extracting(PpqhPeriodRow::start).containsExactly(LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1));
        assertThat(r.ppqhPeriods().get(3).ppqhOrders()).isEqualTo(3);
        assertThat(r.ppqhPeriods().get(0).ppqhTotalCents()).isZero();
    }

    @Test
    void validatesTheRange() {
        assertThatThrownBy(() -> f.reports.ppqhSales(OCT_18, OCT_17)).isInstanceOf(PpqhValidationException.class)
                .hasMessageContaining("posterior");
        assertThatThrownBy(() -> f.reports.ppqhSales(null, OCT_17)).isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.reports.ppqhSales(LocalDate.of(2019, 1, 1), OCT_17)).isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.reports.ppqhSales(LocalDate.of(2021, 1, 1), OCT_17)).isInstanceOf(PpqhValidationException.class)
                .hasMessageContaining("3 años");
    }

    @Test
    void exportsPdfWithEmbeddedBrandFonts() throws Exception {
        PpqhSalesReport r = f.reports.ppqhSales(OCT_17, OCT_18);
        Path pdf = f.reports.ppqhExportPdf(r, tempDir.resolve("out").resolve(PpqhReportService.ppqhSuggestedName(r, "pdf")));

        assertThat(pdf.getFileName().toString()).isEqualTo("ventas-2026-10-17-a-2026-10-18.pdf");
        byte[] bytes = Files.readAllBytes(pdf);
        assertThat(new String(bytes, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);
        assertThat(raw).contains("Barlow", "JetBrainsMono", "Reporte de ventas");
        try (var files = Files.list(pdf.getParent())) {
            assertThat(files).containsExactly(pdf);
        }
        assertThat(Files.size(f.reports.ppqhExportPdf(f.reports.ppqhSales(OCT_17.minusDays(3), OCT_17.minusDays(2)),
                tempDir.resolve("vacio.pdf")))).isPositive();
    }

    @Test
    void exportsExcelWithNumericSolesAndSumFormulas() throws Exception {
        PpqhSalesReport r = f.reports.ppqhSales(OCT_17, OCT_18);
        Path xlsx = f.reports.ppqhExportExcel(r, tempDir.resolve("ventas.xlsx"));

        try (InputStream in = Files.newInputStream(xlsx); XSSFWorkbook wb = new XSSFWorkbook(in)) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(5);
            assertThat(wb.getSheetName(1)).isEqualTo("Por día");
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();
            DataFormatter fmt = new DataFormatter();

            Sheet days = wb.getSheet("Por día");
            assertThat(days.getRow(0).getCell(3).getStringCellValue()).isEqualTo("TOTAL");
            assertThat(days.getRow(1).getCell(3).getNumericCellValue()).isEqualTo((2 * 7990 + 14990 + 3000) / 100.0);
            assertThat(eval.evaluate(days.getRow(3).getCell(3)).getNumberValue()).isEqualTo(r.ppqhTotalCents() / 100.0);
            assertThat(fmt.formatCellValue(days.getRow(2).getCell(3))).isEqualTo("S/ 94.90");

            Sheet categories = wb.getSheet("Por categoría");
            assertThat(categories.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Camisetas deportivas");
            assertThat(eval.evaluate(categories.getRow(1).getCell(4)).getNumberValue()).isCloseTo(0.615, org.assertj.core.data.Offset.offset(0.001));

            Sheet products = wb.getSheet("Por producto");
            assertThat(products.getRow(1).getCell(0).getStringCellValue()).isEqualTo("SF-CAM-001");
            assertThat(eval.evaluate(products.getRow(3).getCell(5)).getNumberValue()).isEqualTo(r.ppqhProductsCents() / 100.0);
        }
    }

    @Test
    void clientCannotSeeOrExportReports() {
        PpqhSalesReport r = f.reports.ppqhSales(OCT_17, OCT_18);
        f.loginClient();
        assertThatThrownBy(() -> f.reports.ppqhSales(OCT_17, OCT_18)).isInstanceOf(PpqhAccessDeniedException.class);
        assertThatThrownBy(() -> f.reports.ppqhExportPdf(r, tempDir.resolve("x.pdf"))).isInstanceOf(PpqhAccessDeniedException.class);
        assertThatThrownBy(() -> f.reports.ppqhExportExcel(r, tempDir.resolve("x.xlsx"))).isInstanceOf(PpqhAccessDeniedException.class);
        assertThat(tempDir.resolve("x.pdf")).doesNotExist();
    }
}
