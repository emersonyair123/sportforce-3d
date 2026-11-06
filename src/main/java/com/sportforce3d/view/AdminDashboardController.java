package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.repository.DashboardRepository.TopProduct;
import com.sportforce3d.repository.DashboardRepository.Total;
import com.sportforce3d.service.DashboardService;
import com.sportforce3d.util.Money;
import com.sportforce3d.viewmodel.ErrorMessages;
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

/**
 * Administración → Dashboard: ventas del día y del mes (pagos aprobados), pagos por verificar, pedidos por estado,
 * ventas de 30 días contra los 30 anteriores y productos más vendidos. Colores de las series en components.css
 * (principal brand, comparación accent, rejilla line).
 */
public class AdminDashboardController {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-PE"));
    private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy",
            Locale.forLanguageTag("es-PE"));

    private final AppContext context;

    @FXML private Label subtitle;
    @FXML private VBox content;

    public AdminDashboardController(AppContext context) {
        this.context = context;
    }

    /** Números del dashboard y si los datos de pago siguen siendo los de prueba. */
    private record Loaded(DashboardService.Stats stats, boolean paymentSettingsReviewed) {
    }

    @FXML
    private void initialize() {
        Async.run(() -> new Loaded(context.dashboardService().stats(),
                        context.storeSettingsService().paymentSettings().reviewed()),
                loaded -> {
                    render(loaded.stats());
                    if (!loaded.paymentSettingsReviewed()) {
                        content.getChildren().add(0, paymentSettingsNotice());
                    }
                },
                e -> content.getChildren().setAll(new EmptyState(Feather.LOCK, "No se pudo cargar el dashboard",
                        ErrorMessages.of(e))));
    }

    private Node paymentSettingsNotice() {
        Label text = new Label("Los datos de Yape, Plin y transferencia son de prueba. Cámbialos por los reales antes "
                + "de recibir pagos.");
        text.getStyleClass().addAll("body-s", "ink");
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);
        SfButton go = new SfButton("Ir a Configuración", SfButton.Variant.GHOST, SfButton.Size.SM);
        go.setOnAction(e -> context.router().go(Routes.ADMIN_SETTINGS));
        HBox box = new HBox(12, Icons.of(Feather.ALERT_TRIANGLE, Icons.SM), text, go);
        box.getStyleClass().add("sf-alert-warning");
        return box;
    }

    private void render(DashboardService.Stats s) {
        subtitle.setText("Hoy es " + LONG.format(s.today()) + ". Las ventas cuentan los pagos aprobados, en hora de Lima.");

        int inProduction = s.ordersByStatus().getOrDefault(OrderStatus.EN_PRODUCCION, 0);
        int paid = s.ordersByStatus().getOrDefault(OrderStatus.PAGADO, 0);
        HBox kpis = new HBox(16,
                kpi(Feather.DOLLAR_SIGN, "VENTAS DE HOY", Money.format(s.todaySales().cents()),
                        count(s.todaySales()), null, null),
                kpi(Feather.CALENDAR, "VENTAS DEL MES", Money.format(s.monthSales().cents()),
                        count(s.monthSales()), compare(s.monthSales(), s.previousMonthSales()), null),
                kpi(Feather.EYE, "PAGOS POR VERIFICAR", String.valueOf(s.pendingPayments()),
                        s.pendingPayments() == 0 ? "Todo al día" : "Revísalos en Pagos", null,
                        () -> context.router().go(Routes.ADMIN_PAYMENTS)),
                kpi(Feather.SCISSORS, "POR PRODUCIR", String.valueOf(paid + inProduction),
                        paid + " pagados · " + inProduction + " en producción", null,
                        () -> context.router().go(Routes.ADMIN_ORDERS, OrderStatus.PAGADO)));

        Node sales = card("Ventas de los últimos 30 días", salesChart(s), "Línea azul: últimos 30 días. "
                + "Línea verde: los 30 días anteriores.");
        Node states = card("Pedidos por estado", statusChart(s.ordersByStatus()), null);
        Node top = card("Más vendidos", topChart(s.topProducts()), "Unidades en pedidos pagados.");
        HBox.setHgrow(states, Priority.ALWAYS);
        HBox.setHgrow(top, Priority.ALWAYS);
        FlowPane row = new FlowPane(24, 24, states, top);
        ((Region) states).setPrefWidth(540);
        ((Region) top).setPrefWidth(540);
        content.getChildren().setAll(kpis, sales, row);
    }

    private static String count(Total t) {
        return t.count() == 1 ? "1 pago aprobado" : t.count() + " pagos aprobados";
    }

    /** "+12 % vs. el mismo tramo del mes anterior" o null si no hay base de comparación. */
    private static String compare(Total now, Total before) {
        if (before.cents() == 0) {
            return now.cents() == 0 ? null : "Primer mes con ventas en este tramo";
        }
        long pct = Math.round((now.cents() - before.cents()) * 100.0 / before.cents());
        return (pct >= 0 ? "+" : "") + pct + " % vs. el mismo tramo del mes anterior";
    }

    private Node kpi(Ikon icon, String title, String value, String detail, String comparison, Runnable onClick) {
        Label t = new Label(title, Icons.of(icon, Icons.SM));
        t.getStyleClass().addAll("label-text", "ink-muted");
        Label v = new Label(value);
        v.getStyleClass().addAll("heading-m", "ink");
        Label d = new Label(detail);
        d.getStyleClass().addAll("body-s", "ink-muted");
        VBox box = new VBox(6, t, v, d);
        if (comparison != null) {
            Label c = new Label(comparison);
            c.getStyleClass().addAll("body-s", comparison.startsWith("-") ? "danger-text" : "accent-text");
            c.setWrapText(true);
            box.getChildren().add(c);
        }
        box.getStyleClass().addAll("sf-card", "sf-kpi");
        box.setPadding(new Insets(20));
        // Las cuatro tarjetas comparten la fila y se ajustan al ancho de la ventana.
        box.setPrefWidth(0);
        box.setMinWidth(170);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        if (onClick != null) {
            box.setCursor(Cursor.HAND);
            box.setOnMouseClicked(e -> onClick.run());
        }
        return box;
    }

    private static Node card(String title, Node chart, String note) {
        Label l = new Label(title);
        l.getStyleClass().addAll("title-s", "ink");
        VBox card = new VBox(12, l);
        if (note != null) {
            Label n = new Label(note);
            n.getStyleClass().addAll("body-s", "ink-muted");
            n.setWrapText(true);
            card.getChildren().add(n);
        }
        card.getChildren().add(chart);
        card.getStyleClass().addAll("sf-card", "sf-chart-card");
        card.setPadding(new Insets(20));
        return card;
    }

    // ---------------------------------------------------------------- Gráficos

    private Node salesChart(DashboardService.Stats s) {
        List<LocalDate> days = new ArrayList<>(s.daily().keySet());
        List<Long> prev = new ArrayList<>(s.previous().values());
        NumberAxis x = new NumberAxis(1, days.size(), 7);
        x.setMinorTickVisible(false);
        x.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number n) {
                int i = n.intValue() - 1;
                return i >= 0 && i < days.size() ? DAY.format(days.get(i)) : "";
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
        NumberAxis y = new NumberAxis();
        y.setForceZeroInRange(true);
        y.setTickLabelFormatter(soles());
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        XYChart.Series<Number, Number> current = new XYChart.Series<>();
        current.setName("Últimos 30 días");
        XYChart.Series<Number, Number> previous = new XYChart.Series<>();
        previous.setName("30 días anteriores");
        int i = 1;
        for (Map.Entry<LocalDate, Long> e : s.daily().entrySet()) {
            current.getData().add(point(i, e.getValue(), DAY.format(e.getKey())));
            long p = i - 1 < prev.size() ? prev.get(i - 1) : 0;
            previous.getData().add(point(i, p, "30 días antes"));
            i++;
        }
        chart.getData().add(current);
        chart.getData().add(previous);
        chart.setCreateSymbols(true);
        chart.setAnimated(false);
        chart.setPrefHeight(300);
        chart.getStyleClass().add("sf-chart");
        addTooltips(chart);
        return chart;
    }

    private static XYChart.Data<Number, Number> point(int x, long cents, String label) {
        XYChart.Data<Number, Number> d = new XYChart.Data<>(x, cents / 100.0);
        d.setExtraValue(label + ": " + Money.format(cents));
        return d;
    }

    private static <X, Y> void addTooltips(XYChart<X, Y> chart) {
        for (XYChart.Series<X, Y> series : chart.getData()) {
            for (XYChart.Data<X, Y> d : series.getData()) {
                if (d.getNode() != null && d.getExtraValue() != null) {
                    Tooltip.install(d.getNode(), new Tooltip(d.getExtraValue().toString()));
                }
            }
        }
    }

    private static StringConverter<Number> soles() {
        return new StringConverter<>() {
            @Override
            public String toString(Number n) {
                return "S/ " + Math.round(n.doubleValue());
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        };
    }

    /** Barras horizontales: los nombres de estado se leen completos (en vertical se giraban 90°). */
    private Node statusChart(Map<OrderStatus, Integer> counts) {
        NumberAxis x = new NumberAxis();
        x.setTickUnit(1);
        x.setMinorTickVisible(false);
        x.setTickLabelFormatter(integers());
        CategoryAxis y = new CategoryAxis();
        BarChart<Number, String> chart = new BarChart<>(x, y);
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Pedidos");
        OrderStatus[] all = OrderStatus.values();
        // El eje de categorías dibuja de abajo hacia arriba: se agregan al revés para leer el flujo de arriba abajo.
        for (int i = all.length - 1; i >= 0; i--) {
            OrderStatus s = all[i];
            XYChart.Data<Number, String> d = new XYChart.Data<>(counts.getOrDefault(s, 0), s.label());
            d.setExtraValue(s.label() + ": " + counts.getOrDefault(s, 0));
            series.getData().add(d);
        }
        chart.getData().add(series);
        integerRange(x, counts.values().stream().mapToInt(Integer::intValue).max().orElse(0));
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(300);
        chart.setCategoryGap(12);
        chart.getStyleClass().add("sf-chart");
        addTooltips(chart);
        return chart;
    }

    private Node topChart(List<TopProduct> top) {
        if (top.isEmpty()) {
            Label none = new Label("Aún no hay pedidos pagados.");
            none.getStyleClass().addAll("body-s", "ink-subtle");
            return none;
        }
        NumberAxis x = new NumberAxis();
        x.setTickLabelFormatter(integers());
        x.setMinorTickVisible(false);
        x.setTickUnit(1);
        CategoryAxis y = new CategoryAxis();
        BarChart<Number, String> chart = new BarChart<>(x, y);
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Unidades");
        // El más vendido arriba: el eje de categorías dibuja de abajo hacia arriba.
        for (int i = top.size() - 1; i >= 0; i--) {
            TopProduct t = top.get(i);
            XYChart.Data<Number, String> d = new XYChart.Data<>(t.units(), t.name());
            d.setExtraValue(t.name() + ": " + t.units() + " u. · " + Money.format(t.revenueCents()));
            series.getData().add(d);
        }
        chart.getData().add(series);
        integerRange(x, top.stream().mapToInt(TopProduct::units).max().orElse(0));
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        // Alto según la cantidad de productos: con uno solo, la barra no ocupa todo el gráfico.
        chart.setPrefHeight(Math.max(150, 70 + 46 * top.size()));
        chart.setMaxHeight(Region.USE_PREF_SIZE);
        chart.setCategoryGap(14);
        chart.getStyleClass().add("sf-chart");
        addTooltips(chart);
        HBox.setHgrow(chart, Priority.ALWAYS);
        VBox box = new VBox(chart);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** Eje de conteos: de 0 a máx + 1 con marcas enteras (unas 6 como mucho). */
    private static void integerRange(NumberAxis axis, int max) {
        int upper = Math.max(1, max + 1);
        axis.setAutoRanging(false);
        axis.setLowerBound(0);
        axis.setUpperBound(upper);
        axis.setTickUnit(Math.max(1, (int) Math.ceil(upper / 6.0)));
    }

    private static StringConverter<Number> integers() {
        return new StringConverter<>() {
            @Override
            public String toString(Number n) {
                return n.doubleValue() == Math.rint(n.doubleValue()) ? String.valueOf(n.intValue()) : "";
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        };
    }
}
