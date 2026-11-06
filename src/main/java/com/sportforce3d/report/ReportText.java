package com.sportforce3d.report;

import com.sportforce3d.model.SalesReport;
import com.sportforce3d.model.SalesReport.Grouping;
import com.sportforce3d.service.DashboardService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Textos compartidos por la pantalla y los archivos exportados (fechas en español de Perú, hora de Lima). */
public final class ReportText {

    public static final Locale ES_PE = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy", ES_PE);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", ES_PE);
    private static final DateTimeFormatter RANGE = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES_PE);
    private static final DateTimeFormatter GENERATED = DateTimeFormatter.ofPattern("d/MM/yyyy HH:mm", ES_PE);

    public static final String DEFINITION = "Ventas = pedidos con pago aprobado por un administrador, en el día "
            + "(hora de Lima) en que se aprobó. Categorías y productos suman el precio de las prendas con sus "
            + "recargos de diseño, sin envío.";

    private ReportText() {
    }

    public static String period(SalesReport report, LocalDate start) {
        return capitalize(report.grouping() == Grouping.DAY ? DAY.format(start) : MONTH.format(start));
    }

    public static String periodHeader(SalesReport report) {
        return report.grouping() == Grouping.DAY ? "Día" : "Mes";
    }

    /** "1 de octubre de 2026 al 18 de octubre de 2026" (o un solo día). */
    public static String range(SalesReport report) {
        return report.from().equals(report.to()) ? RANGE.format(report.from())
                : RANGE.format(report.from()) + " al " + RANGE.format(report.to());
    }

    public static String generated(SalesReport report) {
        return "Generado el " + GENERATED.format(report.generatedAt().atZone(DashboardService.LIMA))
                + " por " + report.generatedBy();
    }

    public static String percent(double value) {
        return String.format(ES_PE, "%.1f %%", value);
    }

    static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
