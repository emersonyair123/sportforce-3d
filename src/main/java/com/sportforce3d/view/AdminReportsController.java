package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.FilterChip;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.SalesReport;
import com.sportforce3d.model.SalesReport.CategoryRow;
import com.sportforce3d.model.SalesReport.MethodRow;
import com.sportforce3d.model.SalesReport.PeriodRow;
import com.sportforce3d.model.SalesReport.ProductRow;
import com.sportforce3d.report.ReportText;
import com.sportforce3d.service.ReportService;
import com.sportforce3d.service.ValidationException;
import com.sportforce3d.util.Money;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

/**
 * Administración → Reportes: rango de fechas (con atajos), KPIs, ventas por día o mes, por categoría, por producto y
 * por método de pago, y exportación a PDF y Excel. Los services vuelven a exigir el rol ADMIN.
 */
public class AdminReportsController {

    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM", ReportText.ES_PE);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", ReportText.ES_PE);
    private static final double ROW = 48;

    private final AppContext context;
    private final ObjectProperty<SalesReport> report = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty busy = new SimpleBooleanProperty(false);
    private final ToggleGroup presetGroup = new ToggleGroup();

    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;
    @FXML private SfButton generateButton;
    @FXML private SfButton pdfButton;
    @FXML private SfButton excelButton;
    @FXML private FlowPane presets;
    @FXML private Label rangeError;
    @FXML private VBox content;

    public AdminReportsController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        for (DatePicker p : List.of(fromPicker, toPicker)) {
            p.setConverter(dateConverter());
            p.setPromptText("dd/mm/aaaa");
            p.getEditor().textProperty().addListener((obs, old, v) -> rangeError.setText(""));
        }
        fromPicker.setAccessibleText("Fecha inicial del reporte");
        toPicker.setAccessibleText("Fecha final del reporte");
        rangeError.managedProperty().bind(rangeError.textProperty().isNotEmpty());

        generateButton.withIcon(Feather.REFRESH_CW);
        generateButton.disableProperty().bind(busy);
        generateButton.setOnAction(e -> {
            presetGroup.selectToggle(null);
            generate();
        });
        pdfButton.withIcon(Feather.FILE_TEXT);
        excelButton.withIcon(Feather.GRID);
        pdfButton.disableProperty().bind(busy.or(Bindings.createBooleanBinding(() -> report.get() == null, report)));
        excelButton.disableProperty().bind(pdfButton.disableProperty());
        pdfButton.setOnAction(e -> export("pdf", "Documento PDF", "*.pdf"));
        excelButton.setOnAction(e -> export("xlsx", "Libro de Excel", "*.xlsx"));

        LocalDate today = context.reportService().today();
        preset("Hoy", today, today);
        preset("Últimos 7 días", today.minusDays(6), today);
        FilterChip last30 = preset("Últimos 30 días", today.minusDays(29), today);
        preset("Este mes", today.withDayOfMonth(1), today);
        LocalDate prev = today.withDayOfMonth(1).minusMonths(1);
        preset("Mes anterior", prev, prev.withDayOfMonth(prev.lengthOfMonth()));
        preset("Este año", today.withDayOfYear(1), today);

        last30.setSelected(true);
        fromPicker.setValue(today.minusDays(29));
        toPicker.setValue(today);
        generate();
    }

    private FilterChip preset(String label, LocalDate from, LocalDate to) {
        FilterChip chip = new FilterChip(label);
        chip.setToggleGroup(presetGroup);
        chip.setOnAction(e -> {
            chip.setSelected(true);
            fromPicker.setValue(from);
            toPicker.setValue(to);
            generate();
        });
        presets.getChildren().add(chip);
        return chip;
    }

    // ------------------------------------------------------------------ Generar

    private void generate() {
        LocalDate from;
        LocalDate to;
        try {
            from = readDate(fromPicker);
            to = readDate(toPicker);
        } catch (DateTimeParseException e) {
            rangeError.setText("Escribe las fechas como dd/mm/aaaa, p. ej. 01/10/2026.");
            return;
        }
        rangeError.setText("");
        busy.set(true);
        Async.run(() -> context.reportService().sales(from, to), r -> {
            busy.set(false);
            report.set(r);
            render(r);
        }, e -> {
            busy.set(false);
            if (e instanceof ValidationException v) {
                rangeError.setText(v.getMessage());
            } else {
                report.set(null);
                content.getChildren().setAll(new EmptyState(Feather.ALERT_TRIANGLE, "No se pudo generar el reporte",
                        ErrorMessages.of(e)));
            }
        });
    }

    /** Lee lo escrito aunque la persona no haya pulsado Enter (el DatePicker solo confirma al salir del campo). */
    private static LocalDate readDate(DatePicker picker) {
        String text = picker.getEditor().getText();
        if (text == null || text.isBlank()) {
            return picker.getValue(); // antes de mostrarse, el editor aún no refleja el valor
        }
        LocalDate value = LocalDate.parse(text.trim(), INPUT);
        picker.setValue(value);
        return value;
    }

    private void render(SalesReport r) {
        HBox kpis = new HBox(16,
                kpi(Feather.DOLLAR_SIGN, "VENTAS TOTALES", Money.format(r.totalCents()),
                        "Incluye envío: " + Money.format(r.shippingCents())),
                kpi(Feather.SHOPPING_BAG, "PEDIDOS PAGADOS", String.valueOf(r.orders()), "Con pago aprobado"),
                kpi(Feather.PACKAGE, "UNIDADES", String.valueOf(r.units()), "Prendas vendidas"),
                kpi(Feather.TRENDING_UP, "TICKET PROMEDIO", Money.format(r.averageTicketCents()), "Total / pedidos"));

        Label range = new Label(ReportText.range(r));
        range.getStyleClass().addAll("title-s", "ink");
        if (r.isEmpty()) {
            content.getChildren().setAll(range, kpis, new EmptyState(Feather.BAR_CHART_2, "Sin ventas en este rango",
                    "Aquí aparecen los pedidos cuyo pago ya aprobaste. Prueba con un rango más amplio o revisa "
                            + "los pagos por verificar en Pagos."));
            return;
        }
        String unit = ReportText.periodHeader(r).toLowerCase();
        content.getChildren().setAll(range, kpis,
                card("Ventas por " + unit, "Total cobrado (con envío) por " + unit + " de venta.", periodChart(r)),
                card("Ventas por categoría", "Precio de las prendas con sus recargos de diseño, sin envío.", categoryTable(r)),
                card("Ventas por producto", null, productTable(r)),
                card("Por método de pago", null, methodTable(r)));
    }

    private static Node kpi(Ikon icon, String title, String value, String detail) {
        Label t = new Label(title, Icons.of(icon, Icons.SM));
        t.getStyleClass().addAll("label-text", "ink-muted");
        Label v = new Label(value);
        v.getStyleClass().addAll("heading-m", "ink");
        Label d = new Label(detail);
        d.getStyleClass().addAll("body-s", "ink-muted");
        VBox box = new VBox(6, t, v, d);
        box.getStyleClass().addAll("sf-card", "sf-kpi");
        box.setPadding(new Insets(20));
        // Las cuatro tarjetas comparten la fila y se ajustan al ancho de la ventana.
        box.setPrefWidth(0);
        box.setMinWidth(170);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private static Node card(String title, String note, Node body) {
        Label l = new Label(title);
        l.getStyleClass().addAll("title-s", "ink");
        VBox card = new VBox(12, l);
        if (note != null) {
            Label n = new Label(note);
            n.getStyleClass().addAll("body-s", "ink-muted");
            n.setWrapText(true);
            card.getChildren().add(n);
        }
        card.getChildren().add(body);
        card.getStyleClass().addAll("sf-card", "sf-chart-card");
        card.setPadding(new Insets(20));
        return card;
    }

    // ------------------------------------------------------------------ Gráfico y tablas

    private Node periodChart(SalesReport r) {
        boolean daily = r.grouping() == SalesReport.Grouping.DAY;
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setForceZeroInRange(true);
        y.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number n) {
                return "S/ " + Math.round(n.doubleValue());
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
        BarChart<String, Number> chart = new BarChart<>(x, y);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Ventas");
        int step = Math.max(1, (int) Math.ceil(r.periods().size() / 16.0));
        for (int i = 0; i < r.periods().size(); i++) {
            PeriodRow p = r.periods().get(i);
            // Las categorías deben ser únicas: los días sin etiqueta llevan espacios invisibles distintos.
            String label = i % step == 0 ? (daily ? SHORT : MONTH).format(p.start()) : "​".repeat(i + 1);
            XYChart.Data<String, Number> d = new XYChart.Data<>(label, p.totalCents() / 100.0);
            d.setExtraValue(ReportText.period(r, p.start()) + ": " + Money.format(p.totalCents()) + " · "
                    + p.orders() + (p.orders() == 1 ? " pedido" : " pedidos"));
            series.getData().add(d);
        }
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(r.periods().size() > 20 ? 2 : 10);
        chart.setPrefHeight(280);
        chart.getStyleClass().add("sf-chart");
        for (XYChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null) {
                Tooltip.install(d.getNode(), new Tooltip(d.getExtraValue().toString()));
            }
        }
        return chart;
    }

    private Node categoryTable(SalesReport r) {
        TableView<CategoryRow> t = table(r.categories());
        t.getColumns().setAll(List.of(
                column("CATEGORÍA", 260, c -> text(c.category(), "body-s")),
                column("PEDIDOS", 100, c -> number(String.valueOf(c.orders()))),
                column("UNIDADES", 100, c -> number(String.valueOf(c.units()))),
                column("VENTAS", 140, c -> money(c.revenueCents())),
                column("% DEL TOTAL", 120, c -> number(ReportText.percent(r.sharePercent(c.revenueCents()))))));
        return t;
    }

    private Node productTable(SalesReport r) {
        TableView<ProductRow> t = table(r.products());
        t.getColumns().setAll(List.of(
                column("SKU", 140, p -> text(p.sku(), "code")),
                column("PRODUCTO", 300, p -> text(p.name(), "body-s")),
                column("CATEGORÍA", 200, p -> text(p.category(), "body-s")),
                column("UNIDADES", 100, p -> number(String.valueOf(p.units()))),
                column("VENTAS", 140, p -> money(p.revenueCents()))));
        return t;
    }

    private Node methodTable(SalesReport r) {
        TableView<MethodRow> t = table(r.methods());
        t.getColumns().setAll(List.of(
                column("MÉTODO", 260, m -> text(m.method().label(), "body-s")),
                column("PEDIDOS", 100, m -> number(String.valueOf(m.orders()))),
                column("TOTAL", 140, m -> money(m.totalCents()))));
        return t;
    }

    private static <T> TableView<T> table(List<T> rows) {
        TableView<T> t = new TableView<>();
        t.getItems().setAll(rows);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setFixedCellSize(ROW);
        t.getStyleClass().add("sf-report-table");
        // Toda la tabla a la vista: la página ya tiene su propio desplazamiento.
        double h = ROW * rows.size() + 44;
        t.setPrefHeight(h);
        t.setMinHeight(h);
        t.setMaxHeight(h);
        VBox.setVgrow(t, Priority.NEVER);
        return t;
    }

    private static <T> TableColumn<T, T> column(String title, double width, Function<T, Node> renderer) {
        TableColumn<T, T> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setMinWidth(Math.min(width, 100));
        col.setSortable(false);
        col.setReorderable(false);
        col.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(T row, boolean empty) {
                super.updateItem(row, empty);
                setText(null);
                setGraphic(empty || row == null ? null : renderer.apply(row));
            }
        });
        return col;
    }

    private static Label text(String value, String style) {
        Label l = new Label(value);
        l.getStyleClass().addAll(style, "ink");
        return l;
    }

    private static Node number(String value) {
        Label l = text(value, "body-s");
        HBox box = new HBox(l);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private static Node money(long cents) {
        Label l = text(Money.format(cents), "sf-strong");
        HBox box = new HBox(l);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    // ------------------------------------------------------------------ Exportar

    private void export(String extension, String description, String pattern) {
        SalesReport r = report.get();
        if (r == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar reporte de ventas");
        chooser.setInitialFileName(ReportService.suggestedName(r, extension));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, pattern));
        File home = new File(System.getProperty("user.home"), "Documents");
        if (home.isDirectory()) {
            chooser.setInitialDirectory(home);
        }
        File file = chooser.showSaveDialog(content.getScene().getWindow());
        if (file == null) {
            return;
        }
        Path target = file.toPath().getFileName().toString().toLowerCase().endsWith("." + extension)
                ? file.toPath() : file.toPath().resolveSibling(file.getName() + "." + extension);
        busy.set(true);
        Async.run(() -> extension.equals("pdf") ? context.reportService().exportPdf(r, target)
                        : context.reportService().exportExcel(r, target),
                path -> {
                    busy.set(false);
                    Toast.success(content, "Reporte guardado: " + path.getFileName());
                },
                e -> {
                    busy.set(false);
                    Toast.error(content, ErrorMessages.of(e));
                });
    }

    private static StringConverter<LocalDate> dateConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : INPUT.format(d);
            }

            @Override
            public LocalDate fromString(String s) {
                try {
                    return s == null || s.isBlank() ? null : LocalDate.parse(s.trim(), INPUT);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        };
    }
}
