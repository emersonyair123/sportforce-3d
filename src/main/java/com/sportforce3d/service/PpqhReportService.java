package com.sportforce3d.service;

import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhSalesReport;
import com.sportforce3d.model.PpqhSalesReport.PpqhGrouping;
import com.sportforce3d.model.PpqhSalesReport.PpqhPeriodRow;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.report.PpqhExcelReportWriter;
import com.sportforce3d.report.PpqhPdfReportWriter;
import com.sportforce3d.repository.HepyReportRepository;
import com.sportforce3d.repository.HepyReportRepository.HepyPeriodTotal;
import com.sportforce3d.repository.HepyReportRepository.HepyTotals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PpqhReportService {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhReportService.class);
    public static final int PPQH_MAX_DAYS = 3 * 366;
    static final LocalDate PPQH_MIN_DATE = LocalDate.of(2020, 1, 1);

    private final HepyReportRepository ppqhReports;
    private final PpqhSession ppqhSession;
    private final Clock ppqhClock;

    public PpqhReportService(HepyReportRepository ppqhReports, PpqhSession ppqhSession, Clock ppqhClock) {
        this.ppqhReports = ppqhReports;
        this.ppqhSession = ppqhSession;
        this.ppqhClock = ppqhClock;
    }

    public LocalDate ppqhToday() {
        return LocalDate.now(ppqhClock.withZone(PpqhDashboardService.PPQH_LIMA));
    }

    public PpqhSalesReport ppqhSales(LocalDate ppqhFrom, LocalDate ppqhTo) {
        PpqhUser ppqhAdmin = ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        ppqhValidateRange(ppqhFrom, ppqhTo);
        long ppqhDays = ChronoUnit.DAYS.between(ppqhFrom, ppqhTo) + 1;
        PpqhGrouping ppqhGrouping = ppqhDays <= PpqhGrouping.PPQH_DAY_LIMIT ? PpqhGrouping.DAY : PpqhGrouping.MONTH;

        HepyTotals ppqhTotals = ppqhReports.hepyTotals(ppqhFrom, ppqhTo);
        Map<String, HepyPeriodTotal> ppqhByPeriod = ppqhReports.hepyByPeriod(ppqhFrom, ppqhTo, ppqhGrouping == PpqhGrouping.MONTH);
        List<PpqhPeriodRow> ppqhPeriods = new ArrayList<>();
        if (ppqhGrouping == PpqhGrouping.DAY) {
            for (LocalDate ppqhD = ppqhFrom; !ppqhD.isAfter(ppqhTo); ppqhD = ppqhD.plusDays(1)) {
                ppqhPeriods.add(ppqhRow(ppqhD, ppqhByPeriod.get(ppqhD.toString())));
            }
        } else {
            for (YearMonth ppqhM = YearMonth.from(ppqhFrom); !ppqhM.isAfter(YearMonth.from(ppqhTo)); ppqhM = ppqhM.plusMonths(1)) {
                ppqhPeriods.add(ppqhRow(ppqhM.atDay(1), ppqhByPeriod.get(ppqhM.toString())));
            }
        }
        return new PpqhSalesReport(ppqhFrom, ppqhTo, ppqhGrouping, ppqhClock.instant(), ppqhAdmin.ppqhFullName(), ppqhTotals.hepyOrders(), ppqhTotals.hepyUnits(),
                ppqhTotals.hepyProductsCents(), ppqhTotals.hepyShippingCents(), ppqhTotals.hepyTotalCents(), ppqhPeriods,
                ppqhReports.hepyByCategory(ppqhFrom, ppqhTo), ppqhReports.hepyByProduct(ppqhFrom, ppqhTo), ppqhReports.hepyByMethod(ppqhFrom, ppqhTo));
    }

    public Path ppqhExportPdf(PpqhSalesReport ppqhReport, Path ppqhTarget) {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        return ppqhWrite(ppqhReport, ppqhTarget, ppqhOut -> new PpqhPdfReportWriter().ppqhWrite(ppqhReport, ppqhOut));
    }

    public Path ppqhExportExcel(PpqhSalesReport ppqhReport, Path ppqhTarget) {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        return ppqhWrite(ppqhReport, ppqhTarget, ppqhOut -> new PpqhExcelReportWriter().ppqhWrite(ppqhReport, ppqhOut));
    }

    public static String ppqhSuggestedName(PpqhSalesReport ppqhReport, String ppqhExtension) {
        return "ventas-" + ppqhReport.ppqhFrom() + "-a-" + ppqhReport.ppqhTo() + "." + ppqhExtension;
    }

    static void ppqhValidateRange(LocalDate ppqhFrom, LocalDate ppqhTo) {
        if (ppqhFrom == null || ppqhTo == null) {
            throw PpqhValidationException.ppqhOf(ppqhFrom == null ? "from" : "to", "Elige la fecha inicial y la final.");
        }
        if (ppqhFrom.isAfter(ppqhTo)) {
            throw PpqhValidationException.ppqhOf("from", "La fecha inicial no puede ser posterior a la final.");
        }
        if (ppqhFrom.isBefore(PPQH_MIN_DATE)) {
            throw PpqhValidationException.ppqhOf("from", "Elige una fecha desde el 1 de enero de " + PPQH_MIN_DATE.getYear() + ".");
        }
        if (ChronoUnit.DAYS.between(ppqhFrom, ppqhTo) + 1 > PPQH_MAX_DAYS) {
            throw PpqhValidationException.ppqhOf("to", "El rango admite hasta 3 años.");
        }
    }

    private static PpqhPeriodRow ppqhRow(LocalDate ppqhStart, HepyPeriodTotal ppqhT) {
        return ppqhT == null ? new PpqhPeriodRow(ppqhStart, 0, 0, 0) : new PpqhPeriodRow(ppqhStart, ppqhT.hepyOrders(), ppqhT.hepyUnits(), ppqhT.hepyTotalCents());
    }

    private interface PpqhWriter {
        void ppqhWrite(OutputStream ppqhOut) throws IOException;
    }

    private Path ppqhWrite(PpqhSalesReport ppqhReport, Path ppqhTarget, PpqhWriter ppqhWriter) {
        if (ppqhReport == null || ppqhTarget == null) {
            throw new PpqhServiceException("Genera el reporte antes de exportarlo.");
        }
        Path ppqhDir = ppqhTarget.toAbsolutePath().getParent();
        Path ppqhTemp = null;
        try {
            Files.createDirectories(ppqhDir);
            ppqhTemp = Files.createTempFile(ppqhDir, ".sf-reporte-", ".tmp");
            try (OutputStream ppqhOut = Files.newOutputStream(ppqhTemp)) {
                ppqhWriter.ppqhWrite(ppqhOut);
            }
            Files.move(ppqhTemp, ppqhTarget, StandardCopyOption.REPLACE_EXISTING);
            ppqhLog.info("Reporte {} a {} exportado a {}", ppqhReport.ppqhFrom(), ppqhReport.ppqhTo(), ppqhTarget.getFileName());
            return ppqhTarget;
        } catch (IOException ppqhE) {
            throw new UncheckedIOException("No se pudo guardar " + ppqhTarget.getFileName(), ppqhE);
        } finally {
            if (ppqhTemp != null) {
                try {
                    Files.deleteIfExists(ppqhTemp);
                } catch (IOException ppqhIgnored) {
                }
            }
        }
    }
}
