package com.sportforce3d.report;

import com.sportforce3d.model.PpqhSalesReport;
import com.sportforce3d.model.PpqhSalesReport.PpqhGrouping;
import com.sportforce3d.service.PpqhDashboardService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PpqhReportText {

    public static final Locale PPQH_ES_PE = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter PPQH_DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy", PPQH_ES_PE);
    private static final DateTimeFormatter PPQH_MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", PPQH_ES_PE);
    private static final DateTimeFormatter PPQH_RANGE = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PPQH_ES_PE);
    private static final DateTimeFormatter PPQH_GENERATED = DateTimeFormatter.ofPattern("d/MM/yyyy HH:mm", PPQH_ES_PE);

    public static final String PPQH_DEFINITION = "Ventas = pedidos con pago aprobado por un administrador, en el día "
            + "(hora de Lima) en que se aprobó. Categorías y productos suman el precio de las prendas con sus "
            + "recargos de diseño, sin envío.";

    private PpqhReportText() {
    }

    public static String ppqhPeriod(PpqhSalesReport ppqhReport, LocalDate ppqhStart) {
        return ppqhCapitalize(ppqhReport.ppqhGrouping() == PpqhGrouping.DAY ? PPQH_DAY.format(ppqhStart) : PPQH_MONTH.format(ppqhStart));
    }

    public static String ppqhPeriodHeader(PpqhSalesReport ppqhReport) {
        return ppqhReport.ppqhGrouping() == PpqhGrouping.DAY ? "Día" : "Mes";
    }

    public static String ppqhRange(PpqhSalesReport ppqhReport) {
        return ppqhReport.ppqhFrom().equals(ppqhReport.ppqhTo()) ? PPQH_RANGE.format(ppqhReport.ppqhFrom())
                : PPQH_RANGE.format(ppqhReport.ppqhFrom()) + " al " + PPQH_RANGE.format(ppqhReport.ppqhTo());
    }

    public static String ppqhGenerated(PpqhSalesReport ppqhReport) {
        return "Generado el " + PPQH_GENERATED.format(ppqhReport.ppqhGeneratedAt().atZone(PpqhDashboardService.PPQH_LIMA))
                + " por " + ppqhReport.ppqhGeneratedBy();
    }

    public static String ppqhPercent(double ppqhValue) {
        return String.format(PPQH_ES_PE, "%.1f %%", ppqhValue);
    }

    static String ppqhCapitalize(String ppqhS) {
        return ppqhS.isEmpty() ? ppqhS : Character.toUpperCase(ppqhS.charAt(0)) + ppqhS.substring(1);
    }
}
