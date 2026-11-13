package com.sportforce3d.service;

import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.model.SalesReport;
import com.sportforce3d.model.SalesReport.Grouping;
import com.sportforce3d.model.SalesReport.PeriodRow;
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

        sell("SF-CAM-001-M", 2, PaymentMethod.YAPE);
        f.clock.advance(Duration.ofHours(13).plusMinutes(30));
        sell("SF-UFU-001-M", 1, PaymentMethod.PLIN);
        f.clock.advance(Duration.ofHours(9).plusMinutes(30));
        sell("SF-CAM-001-L", 1, PaymentMethod.TRANSFERENCIA);
        f.loginClient();
        f.cart.add(f.variantId("SF-CAM-002-M"), 3);
        f.orders.placeOrder(f.addresses.list().get(0).id(), PaymentMethod.YAPE, null);
        f.loginAdmin();
    }

    private void sell(String sku, int quantity, PaymentMethod method) {
        f.loginClient();
        f.cart.add(f.variantId(sku), quantity);
        OrderDetail order = f.orders.placeOrder(f.addresses.list().get(0).id(), method, null);
        OrderDetail registered = f.payments.register(order.id(), String.valueOf(code++), proof);
        f.loginAdmin();
        f.payments.approve(registered.currentPayment().id(), null);
    }

    @Test
    void salesByDayCountApprovedPaymentsOnTheirLimaDay() {
        SalesReport r = f.reports.sales(OCT_17, OCT_18);

        assertThat(r.grouping()).isEqualTo(Grouping.DAY);
        assertThat(r.orders()).isEqualTo(3);
        assertThat(r.units()).isEqualTo(4);
        assertThat(r.productsCents()).isEqualTo(2 * 7990 + 14990 + 7990);
        assertThat(r.shippingCents()).isEqualTo(3 * 1500);
        assertThat(r.totalCents()).isEqualTo(r.productsCents() + r.shippingCents());
        assertThat(r.averageTicketCents()).isEqualTo(Math.round(r.totalCents() / 3.0));
        assertThat(r.periods()).extracting(PeriodRow::start).containsExactly(OCT_17, OCT_18);
        assertThat(r.periods().get(0)).isEqualTo(new PeriodRow(OCT_17, 2, 3, 2 * 7990 + 1500 + 14990 + 1500));
        assertThat(r.periods().get(1)).isEqualTo(new PeriodRow(OCT_18, 1, 1, 7990 + 1500));

        assertThat(f.reports.sales(OCT_18, OCT_18).orders()).isEqualTo(1);
        assertThat(f.reports.sales(OCT_17.minusDays(5), OCT_17.minusDays(1)).isEmpty()).isTrue();
    }

    @Test
    void salesByCategoryAndProductExcludeShipping() {
        SalesReport r = f.reports.sales(OCT_17, OCT_18);

        assertThat(r.categories()).hasSize(2);
        assertThat(r.categories().get(0).category()).isEqualTo("Camisetas deportivas");
        assertThat(r.categories().get(0).units()).isEqualTo(3);
        assertThat(r.categories().get(0).orders()).isEqualTo(2);
        assertThat(r.categories().get(0).revenueCents()).isEqualTo(3 * 7990);
        assertThat(r.categories().get(1).category()).isEqualTo("Uniformes de fútbol");
        assertThat(r.categories().stream().mapToLong(c -> c.revenueCents()).sum()).isEqualTo(r.productsCents());
        assertThat(r.sharePercent(3 * 7990)).isEqualTo(61.5);

        assertThat(r.products()).extracting(p -> p.sku()).containsExactly("SF-CAM-001", "SF-UFU-001");
        assertThat(r.products().get(0).units()).isEqualTo(3);
        assertThat(r.products().get(0).name()).isEqualTo("Camiseta Pro Dry-Fit");
        assertThat(r.methods()).extracting(m -> m.method())
                .containsExactlyInAnyOrder(PaymentMethod.YAPE, PaymentMethod.PLIN, PaymentMethod.TRANSFERENCIA);
    }

    @Test
    void longRangesAreGroupedByMonthWithEmptyMonths() {
        SalesReport r = f.reports.sales(LocalDate.of(2026, 7, 15), OCT_18);
        assertThat(r.grouping()).isEqualTo(Grouping.MONTH);
        assertThat(r.periods()).extracting(PeriodRow::start).containsExactly(LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1));
        assertThat(r.periods().get(3).orders()).isEqualTo(3);
        assertThat(r.periods().get(0).totalCents()).isZero();
    }

    @Test
    void validatesTheRange() {
        assertThatThrownBy(() -> f.reports.sales(OCT_18, OCT_17)).isInstanceOf(ValidationException.class)
                .hasMessageContaining("posterior");
        assertThatThrownBy(() -> f.reports.sales(null, OCT_17)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.reports.sales(LocalDate.of(2019, 1, 1), OCT_17)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.reports.sales(LocalDate.of(2021, 1, 1), OCT_17)).isInstanceOf(ValidationException.class)
                .hasMessageContaining("3 años");
    }

    @Test
    void exportsPdfWithEmbeddedBrandFonts() throws Exception {
        SalesReport r = f.reports.sales(OCT_17, OCT_18);
        Path pdf = f.reports.exportPdf(r, tempDir.resolve("out").resolve(ReportService.suggestedName(r, "pdf")));

        assertThat(pdf.getFileName().toString()).isEqualTo("ventas-2026-10-17-a-2026-10-18.pdf");
        byte[] bytes = Files.readAllBytes(pdf);
        assertThat(new String(bytes, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);
        assertThat(raw).contains("Barlow", "JetBrainsMono", "Reporte de ventas");
        try (var files = Files.list(pdf.getParent())) {
            assertThat(files).containsExactly(pdf);
        }
        assertThat(Files.size(f.reports.exportPdf(f.reports.sales(OCT_17.minusDays(3), OCT_17.minusDays(2)),
                tempDir.resolve("vacio.pdf")))).isPositive();
    }

    @Test
    void exportsExcelWithNumericSolesAndSumFormulas() throws Exception {
        SalesReport r = f.reports.sales(OCT_17, OCT_18);
        Path xlsx = f.reports.exportExcel(r, tempDir.resolve("ventas.xlsx"));

        try (InputStream in = Files.newInputStream(xlsx); XSSFWorkbook wb = new XSSFWorkbook(in)) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(5);
            assertThat(wb.getSheetName(1)).isEqualTo("Por día");
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();
            DataFormatter fmt = new DataFormatter();

            Sheet days = wb.getSheet("Por día");
            assertThat(days.getRow(0).getCell(3).getStringCellValue()).isEqualTo("TOTAL");
            assertThat(days.getRow(1).getCell(3).getNumericCellValue()).isEqualTo((2 * 7990 + 14990 + 3000) / 100.0);
            assertThat(eval.evaluate(days.getRow(3).getCell(3)).getNumberValue()).isEqualTo(r.totalCents() / 100.0);
            assertThat(fmt.formatCellValue(days.getRow(2).getCell(3))).isEqualTo("S/ 94.90");

            Sheet categories = wb.getSheet("Por categoría");
            assertThat(categories.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Camisetas deportivas");
            assertThat(eval.evaluate(categories.getRow(1).getCell(4)).getNumberValue()).isCloseTo(0.615, org.assertj.core.data.Offset.offset(0.001));

            Sheet products = wb.getSheet("Por producto");
            assertThat(products.getRow(1).getCell(0).getStringCellValue()).isEqualTo("SF-CAM-001");
            assertThat(eval.evaluate(products.getRow(3).getCell(5)).getNumberValue()).isEqualTo(r.productsCents() / 100.0);
        }
    }

    @Test
    void clientCannotSeeOrExportReports() {
        SalesReport r = f.reports.sales(OCT_17, OCT_18);
        f.loginClient();
        assertThatThrownBy(() -> f.reports.sales(OCT_17, OCT_18)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> f.reports.exportPdf(r, tempDir.resolve("x.pdf"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> f.reports.exportExcel(r, tempDir.resolve("x.xlsx"))).isInstanceOf(AccessDeniedException.class);
        assertThat(tempDir.resolve("x.pdf")).doesNotExist();
    }
}
