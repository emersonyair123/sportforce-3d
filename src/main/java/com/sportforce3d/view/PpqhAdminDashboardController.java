package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.repository.HepyDashboardRepository.HepyTopProduct;
import com.sportforce3d.repository.HepyDashboardRepository.HepyTotal;
import com.sportforce3d.service.PpqhDashboardService;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PpqhAdminDashboardController {

    private static final DateTimeFormatter PPQH_DAY = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-PE"));
    private static final DateTimeFormatter PPQH_LONG = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy",
            Locale.forLanguageTag("es-PE"));

    private final PpqhAppContext ppqhContext;

    @FXML private Label ppqhSubtitle;
    @FXML private VBox ppqhContent;

    public PpqhAdminDashboardController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
    }

    private record PpqhLoaded(PpqhDashboardService.PpqhStats ppqhStats, boolean ppqhPaymentSettingsReviewed) {
    }

    @FXML
    private void initialize() {
        PpqhAsync.ppqhRun(() -> new PpqhLoaded(ppqhContext.ppqhDashboardService().ppqhStats(),
                        ppqhContext.ppqhStoreSettingsService().ppqhPaymentSettings().ppqhReviewed()),
                ppqhLoaded -> {
                    ppqhRender(ppqhLoaded.ppqhStats());
                    if (!ppqhLoaded.ppqhPaymentSettingsReviewed()) {
                        ppqhContent.getChildren().add(0, ppqhPaymentSettingsNotice());
                    }
                },
                ppqhE -> ppqhContent.getChildren().setAll(new PpqhEmptyState(Feather.LOCK, "No se pudo cargar el dashboard",
                        PpqhErrorMessages.ppqhOf(ppqhE))));
    }

    private Node ppqhPaymentSettingsNotice() {
        Label ppqhText = new Label("Los datos de Yape, Plin y transferencia son de prueba. Cámbialos por los reales antes "
                + "de recibir pagos.");
        ppqhText.getStyleClass().addAll("body-s", "ink");
        ppqhText.setWrapText(true);
        HBox.setHgrow(ppqhText, Priority.ALWAYS);
        PpqhSfButton ppqhGo = new PpqhSfButton("Ir a Configuración", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
        ppqhGo.setOnAction(ppqhE -> ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_SETTINGS));
        HBox ppqhBox = new HBox(12, PpqhIcons.ppqhOf(Feather.ALERT_TRIANGLE, PpqhIcons.PPQH_SM), ppqhText, ppqhGo);
        ppqhBox.getStyleClass().add("sf-alert-warning");
        return ppqhBox;
    }

    private void ppqhRender(PpqhDashboardService.PpqhStats ppqhS) {
        ppqhSubtitle.setText("Hoy es " + PPQH_LONG.format(ppqhS.ppqhToday()) + ". Las ventas cuentan los pagos aprobados, en hora de Lima.");

        int ppqhInProduction = ppqhS.ppqhOrdersByStatus().getOrDefault(PpqhOrderStatus.EN_PRODUCCION, 0);
        int ppqhPaid = ppqhS.ppqhOrdersByStatus().getOrDefault(PpqhOrderStatus.PAGADO, 0);
        HBox ppqhKpis = new HBox(16,
                ppqhKpi(Feather.DOLLAR_SIGN, "VENTAS DE HOY", HepyMoney.hepyFormat(ppqhS.ppqhTodaySales().hepyCents()),
                        ppqhCount(ppqhS.ppqhTodaySales()), null, null),
                ppqhKpi(Feather.CALENDAR, "VENTAS DEL MES", HepyMoney.hepyFormat(ppqhS.ppqhMonthSales().hepyCents()),
                        ppqhCount(ppqhS.ppqhMonthSales()), ppqhCompare(ppqhS.ppqhMonthSales(), ppqhS.ppqhPreviousMonthSales()), null),
                ppqhKpi(Feather.EYE, "PAGOS POR VERIFICAR", String.valueOf(ppqhS.ppqhPendingPayments()),
                        ppqhS.ppqhPendingPayments() == 0 ? "Todo al día" : "Revísalos en Pagos", null,
                        () -> ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_PAYMENTS)),
                ppqhKpi(Feather.SCISSORS, "POR PRODUCIR", String.valueOf(ppqhPaid + ppqhInProduction),
                        ppqhPaid + " pagados · " + ppqhInProduction + " en producción", null,
                        () -> ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_ORDERS, PpqhOrderStatus.PAGADO)));

        Node ppqhSales = ppqhCard("Ventas de los últimos 30 días", ppqhSalesChart(ppqhS), "Línea azul: últimos 30 días. "
                + "Línea verde: los 30 días anteriores.");
        Node ppqhStates = ppqhCard("Pedidos por estado", ppqhStatusChart(ppqhS.ppqhOrdersByStatus()), null);
        Node ppqhTop = ppqhCard("Más vendidos", ppqhTopChart(ppqhS.ppqhTopProducts()), "Unidades en pedidos pagados.");
        HBox.setHgrow(ppqhStates, Priority.ALWAYS);
        HBox.setHgrow(ppqhTop, Priority.ALWAYS);
        FlowPane ppqhRow = new FlowPane(24, 24, ppqhStates, ppqhTop);
        ((Region) ppqhStates).setPrefWidth(540);
        ((Region) ppqhTop).setPrefWidth(540);
        ppqhContent.getChildren().setAll(ppqhKpis, ppqhSales, ppqhRow);
    }

    private static String ppqhCount(HepyTotal ppqhT) {
        return ppqhT.hepyCount() == 1 ? "1 pago aprobado" : ppqhT.hepyCount() + " pagos aprobados";
    }

    private static String ppqhCompare(HepyTotal ppqhNow, HepyTotal ppqhBefore) {
        if (ppqhBefore.hepyCents() == 0) {
            return ppqhNow.hepyCents() == 0 ? null : "Primer mes con ventas en este tramo";
        }
        long ppqhPct = Math.round((ppqhNow.hepyCents() - ppqhBefore.hepyCents()) * 100.0 / ppqhBefore.hepyCents());
        return (ppqhPct >= 0 ? "+" : "") + ppqhPct + " % vs. el mismo tramo del mes anterior";
    }

    private Node ppqhKpi(Ikon ppqhIcon, String ppqhTitle, String ppqhValue, String ppqhDetail, String ppqhComparison, Runnable ppqhOnClick) {
        Label ppqhT = new Label(ppqhTitle, PpqhIcons.ppqhOf(ppqhIcon, PpqhIcons.PPQH_SM));
        ppqhT.getStyleClass().addAll("label-text", "ink-muted");
        Label ppqhV = new Label(ppqhValue);
        ppqhV.getStyleClass().addAll("heading-m", "ink");
        Label ppqhD = new Label(ppqhDetail);
        ppqhD.getStyleClass().addAll("body-s", "ink-muted");
        VBox ppqhBox = new VBox(6, ppqhT, ppqhV, ppqhD);
        if (ppqhComparison != null) {
            Label ppqhC = new Label(ppqhComparison);
            ppqhC.getStyleClass().addAll("body-s", ppqhComparison.startsWith("-") ? "danger-text" : "accent-text");
            ppqhC.setWrapText(true);
            ppqhBox.getChildren().add(ppqhC);
        }
        ppqhBox.getStyleClass().addAll("sf-card", "sf-kpi");
        ppqhBox.setPadding(new Insets(20));
        ppqhBox.setPrefWidth(0);
        ppqhBox.setMinWidth(170);
        ppqhBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(ppqhBox, Priority.ALWAYS);
        if (ppqhOnClick != null) {
            ppqhBox.setCursor(Cursor.HAND);
            ppqhBox.setOnMouseClicked(ppqhE -> ppqhOnClick.run());
        }
        return ppqhBox;
    }

    private static Node ppqhCard(String ppqhTitle, Node ppqhChart, String ppqhNote) {
        Label ppqhL = new Label(ppqhTitle);
        ppqhL.getStyleClass().addAll("title-s", "ink");
        VBox ppqhCard = new VBox(12, ppqhL);
        if (ppqhNote != null) {
            Label ppqhN = new Label(ppqhNote);
            ppqhN.getStyleClass().addAll("body-s", "ink-muted");
            ppqhN.setWrapText(true);
            ppqhCard.getChildren().add(ppqhN);
        }
        ppqhCard.getChildren().add(ppqhChart);
        ppqhCard.getStyleClass().addAll("sf-card", "sf-chart-card");
        ppqhCard.setPadding(new Insets(20));
        return ppqhCard;
    }

    private Node ppqhSalesChart(PpqhDashboardService.PpqhStats ppqhS) {
        List<LocalDate> ppqhDays = new ArrayList<>(ppqhS.ppqhDaily().keySet());
        List<Long> ppqhPrev = new ArrayList<>(ppqhS.ppqhPrevious().values());
        NumberAxis ppqhX = new NumberAxis(1, ppqhDays.size(), 7);
        ppqhX.setMinorTickVisible(false);
        ppqhX.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number ppqhN) {
                int ppqhI = ppqhN.intValue() - 1;
                return ppqhI >= 0 && ppqhI < ppqhDays.size() ? PPQH_DAY.format(ppqhDays.get(ppqhI)) : "";
            }

            @Override
            public Number fromString(String ppqhS) {
                return 0;
            }
        });
        NumberAxis ppqhY = new NumberAxis();
        ppqhY.setForceZeroInRange(true);
        ppqhY.setTickLabelFormatter(ppqhSoles());
        LineChart<Number, Number> ppqhChart = new LineChart<>(ppqhX, ppqhY);
        XYChart.Series<Number, Number> ppqhCurrent = new XYChart.Series<>();
        ppqhCurrent.setName("Últimos 30 días");
        XYChart.Series<Number, Number> ppqhPrevious = new XYChart.Series<>();
        ppqhPrevious.setName("30 días anteriores");
        int ppqhI = 1;
        for (Map.Entry<LocalDate, Long> ppqhE : ppqhS.ppqhDaily().entrySet()) {
            ppqhCurrent.getData().add(ppqhPoint(ppqhI, ppqhE.getValue(), PPQH_DAY.format(ppqhE.getKey())));
            long ppqhP = ppqhI - 1 < ppqhPrev.size() ? ppqhPrev.get(ppqhI - 1) : 0;
            ppqhPrevious.getData().add(ppqhPoint(ppqhI, ppqhP, "30 días antes"));
            ppqhI++;
        }
        ppqhChart.getData().add(ppqhCurrent);
        ppqhChart.getData().add(ppqhPrevious);
        ppqhChart.setCreateSymbols(true);
        ppqhChart.setAnimated(false);
        ppqhChart.setPrefHeight(300);
        ppqhChart.getStyleClass().add("sf-chart");
        ppqhAddTooltips(ppqhChart);
        return ppqhChart;
    }

    private static XYChart.Data<Number, Number> ppqhPoint(int ppqhX, long ppqhCents, String ppqhLabel) {
        XYChart.Data<Number, Number> ppqhD = new XYChart.Data<>(ppqhX, ppqhCents / 100.0);
        ppqhD.setExtraValue(ppqhLabel + ": " + HepyMoney.hepyFormat(ppqhCents));
        return ppqhD;
    }

    private static <X, Y> void ppqhAddTooltips(XYChart<X, Y> ppqhChart) {
        for (XYChart.Series<X, Y> ppqhSeries : ppqhChart.getData()) {
            for (XYChart.Data<X, Y> ppqhD : ppqhSeries.getData()) {
                if (ppqhD.getNode() != null && ppqhD.getExtraValue() != null) {
                    Tooltip.install(ppqhD.getNode(), new Tooltip(ppqhD.getExtraValue().toString()));
                }
            }
        }
    }

    private static StringConverter<Number> ppqhSoles() {
        return new StringConverter<>() {
            @Override
            public String toString(Number ppqhN) {
                return "S/ " + Math.round(ppqhN.doubleValue());
            }

            @Override
            public Number fromString(String ppqhS) {
                return 0;
            }
        };
    }

    private Node ppqhStatusChart(Map<PpqhOrderStatus, Integer> ppqhCounts) {
        NumberAxis ppqhX = new NumberAxis();
        ppqhX.setTickUnit(1);
        ppqhX.setMinorTickVisible(false);
        ppqhX.setTickLabelFormatter(ppqhIntegers());
        CategoryAxis ppqhY = new CategoryAxis();
        BarChart<Number, String> ppqhChart = new BarChart<>(ppqhX, ppqhY);
        XYChart.Series<Number, String> ppqhSeries = new XYChart.Series<>();
        ppqhSeries.setName("Pedidos");
        PpqhOrderStatus[] ppqhAll = PpqhOrderStatus.values();
        for (int ppqhI = ppqhAll.length - 1; ppqhI >= 0; ppqhI--) {
            PpqhOrderStatus ppqhS = ppqhAll[ppqhI];
            XYChart.Data<Number, String> ppqhD = new XYChart.Data<>(ppqhCounts.getOrDefault(ppqhS, 0), ppqhS.ppqhLabel());
            ppqhD.setExtraValue(ppqhS.ppqhLabel() + ": " + ppqhCounts.getOrDefault(ppqhS, 0));
            ppqhSeries.getData().add(ppqhD);
        }
        ppqhChart.getData().add(ppqhSeries);
        ppqhIntegerRange(ppqhX, ppqhCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0));
        ppqhChart.setLegendVisible(false);
        ppqhChart.setAnimated(false);
        ppqhChart.setPrefHeight(300);
        ppqhChart.setCategoryGap(12);
        ppqhChart.getStyleClass().add("sf-chart");
        ppqhAddTooltips(ppqhChart);
        return ppqhChart;
    }

    private Node ppqhTopChart(List<HepyTopProduct> ppqhTop) {
        if (ppqhTop.isEmpty()) {
            Label ppqhNone = new Label("Aún no hay pedidos pagados.");
            ppqhNone.getStyleClass().addAll("body-s", "ink-subtle");
            return ppqhNone;
        }
        NumberAxis ppqhX = new NumberAxis();
        ppqhX.setTickLabelFormatter(ppqhIntegers());
        ppqhX.setMinorTickVisible(false);
        ppqhX.setTickUnit(1);
        CategoryAxis ppqhY = new CategoryAxis();
        BarChart<Number, String> ppqhChart = new BarChart<>(ppqhX, ppqhY);
        XYChart.Series<Number, String> ppqhSeries = new XYChart.Series<>();
        ppqhSeries.setName("Unidades");
        for (int ppqhI = ppqhTop.size() - 1; ppqhI >= 0; ppqhI--) {
            HepyTopProduct ppqhT = ppqhTop.get(ppqhI);
            XYChart.Data<Number, String> ppqhD = new XYChart.Data<>(ppqhT.hepyUnits(), ppqhT.hepyName());
            ppqhD.setExtraValue(ppqhT.hepyName() + ": " + ppqhT.hepyUnits() + " u. · " + HepyMoney.hepyFormat(ppqhT.hepyRevenueCents()));
            ppqhSeries.getData().add(ppqhD);
        }
        ppqhChart.getData().add(ppqhSeries);
        ppqhIntegerRange(ppqhX, ppqhTop.stream().mapToInt(HepyTopProduct::hepyUnits).max().orElse(0));
        ppqhChart.setLegendVisible(false);
        ppqhChart.setAnimated(false);
        ppqhChart.setPrefHeight(Math.max(150, 70 + 46 * ppqhTop.size()));
        ppqhChart.setMaxHeight(Region.USE_PREF_SIZE);
        ppqhChart.setCategoryGap(14);
        ppqhChart.getStyleClass().add("sf-chart");
        ppqhAddTooltips(ppqhChart);
        HBox.setHgrow(ppqhChart, Priority.ALWAYS);
        VBox ppqhBox = new VBox(ppqhChart);
        ppqhBox.setAlignment(Pos.CENTER_LEFT);
        return ppqhBox;
    }

    private static void ppqhIntegerRange(NumberAxis ppqhAxis, int ppqhMax) {
        int ppqhUpper = Math.max(1, ppqhMax + 1);
        ppqhAxis.setAutoRanging(false);
        ppqhAxis.setLowerBound(0);
        ppqhAxis.setUpperBound(ppqhUpper);
        ppqhAxis.setTickUnit(Math.max(1, (int) Math.ceil(ppqhUpper / 6.0)));
    }

    private static StringConverter<Number> ppqhIntegers() {
        return new StringConverter<>() {
            @Override
            public String toString(Number ppqhN) {
                return ppqhN.doubleValue() == Math.rint(ppqhN.doubleValue()) ? String.valueOf(ppqhN.intValue()) : "";
            }

            @Override
            public Number fromString(String ppqhS) {
                return 0;
            }
        };
    }
}
