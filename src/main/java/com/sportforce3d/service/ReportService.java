package com.sportforce3d.service;

import com.sportforce3d.model.Role;
import com.sportforce3d.model.SalesReport;
import com.sportforce3d.model.SalesReport.Grouping;
import com.sportforce3d.model.SalesReport.PeriodRow;
import com.sportforce3d.model.User;
import com.sportforce3d.report.ExcelReportWriter;
import com.sportforce3d.report.PdfReportWriter;
import com.sportforce3d.repository.ReportRepository;
import com.sportforce3d.repository.ReportRepository.PeriodTotal;
import com.sportforce3d.repository.ReportRepository.Totals;
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

/**
 * Administración → Reportes: ventas por rango de fechas, por categoría y por producto, y su exportación a PDF
 * (OpenPDF) y Excel (Apache POI). Solo ADMIN. Las fechas son días de Lima.
 */
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    /** Rango máximo: tres años. */
    public static final int MAX_DAYS = 3 * 366;
    static final LocalDate MIN_DATE = LocalDate.of(2020, 1, 1);

    private final ReportRepository reports;
    private final Session session;
    private final Clock clock;

    public ReportService(ReportRepository reports, Session session, Clock clock) {
        this.reports = reports;
        this.session = session;
        this.clock = clock;
    }

    /** Hoy en Lima: para los atajos de rango de la pantalla. */
    public LocalDate today() {
        return LocalDate.now(clock.withZone(DashboardService.LIMA));
    }

    public SalesReport sales(LocalDate from, LocalDate to) {
        User admin = session.requireRole(Role.ADMIN);
        validateRange(from, to);
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        Grouping grouping = days <= Grouping.DAY_LIMIT ? Grouping.DAY : Grouping.MONTH;

        Totals totals = reports.totals(from, to);
        Map<String, PeriodTotal> byPeriod = reports.byPeriod(from, to, grouping == Grouping.MONTH);
        List<PeriodRow> periods = new ArrayList<>();
        if (grouping == Grouping.DAY) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                periods.add(row(d, byPeriod.get(d.toString())));
            }
        } else {
            for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
                periods.add(row(m.atDay(1), byPeriod.get(m.toString())));
            }
        }
        return new SalesReport(from, to, grouping, clock.instant(), admin.fullName(), totals.orders(), totals.units(),
                totals.productsCents(), totals.shippingCents(), totals.totalCents(), periods,
                reports.byCategory(from, to), reports.byProduct(from, to), reports.byMethod(from, to));
    }

    /** Escribe el reporte en PDF. Si falla, no deja un archivo a medias. */
    public Path exportPdf(SalesReport report, Path target) {
        session.requireRole(Role.ADMIN);
        return write(report, target, out -> new PdfReportWriter().write(report, out));
    }

    /** Escribe el reporte en Excel (.xlsx) con una hoja por vista. */
    public Path exportExcel(SalesReport report, Path target) {
        session.requireRole(Role.ADMIN);
        return write(report, target, out -> new ExcelReportWriter().write(report, out));
    }

    /** Nombre sugerido: ventas-2026-10-01-a-2026-10-18.pdf */
    public static String suggestedName(SalesReport report, String extension) {
        return "ventas-" + report.from() + "-a-" + report.to() + "." + extension;
    }

    static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw ValidationException.of(from == null ? "from" : "to", "Elige la fecha inicial y la final.");
        }
        if (from.isAfter(to)) {
            throw ValidationException.of("from", "La fecha inicial no puede ser posterior a la final.");
        }
        if (from.isBefore(MIN_DATE)) {
            throw ValidationException.of("from", "Elige una fecha desde el 1 de enero de " + MIN_DATE.getYear() + ".");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw ValidationException.of("to", "El rango admite hasta 3 años.");
        }
    }

    private static PeriodRow row(LocalDate start, PeriodTotal t) {
        return t == null ? new PeriodRow(start, 0, 0, 0) : new PeriodRow(start, t.orders(), t.units(), t.totalCents());
    }

    private interface Writer {
        void write(OutputStream out) throws IOException;
    }

    private Path write(SalesReport report, Path target, Writer writer) {
        if (report == null || target == null) {
            throw new ServiceException("Genera el reporte antes de exportarlo.");
        }
        Path dir = target.toAbsolutePath().getParent();
        Path temp = null;
        try {
            Files.createDirectories(dir);
            temp = Files.createTempFile(dir, ".sf-reporte-", ".tmp");
            try (OutputStream out = Files.newOutputStream(temp)) {
                writer.write(out);
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Reporte {} a {} exportado a {}", report.from(), report.to(), target.getFileName());
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + target.getFileName(), e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // Se limpia en el próximo intento.
                }
            }
        }
    }
}
