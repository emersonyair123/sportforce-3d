package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.HepyFilterChip;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.PpqhSalesReport;
import com.sportforce3d.model.PpqhSalesReport.PpqhCategoryRow;
import com.sportforce3d.model.PpqhSalesReport.PpqhMethodRow;
import com.sportforce3d.model.PpqhSalesReport.PpqhPeriodRow;
import com.sportforce3d.model.PpqhSalesReport.PpqhProductRow;
import com.sportforce3d.report.PpqhReportText;
import com.sportforce3d.service.PpqhReportService;
import com.sportforce3d.service.PpqhValidationException;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
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

public class PpqhAdminReportsController {

    private static final DateTimeFormatter PPQH_INPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter PPQH_SHORT = DateTimeFormatter.ofPattern("d MMM", PpqhReportText.PPQH_ES_PE);
    private static final DateTimeFormatter PPQH_MONTH = DateTimeFormatter.ofPattern("MMM yyyy", PpqhReportText.PPQH_ES_PE);
    private static final double PPQH_ROW = 48;

    private final PpqhAppContext ppqhContext;
    private final ObjectProperty<PpqhSalesReport> ppqhReport = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty ppqhBusy = new SimpleBooleanProperty(false);
    private final ToggleGroup ppqhPresetGroup = new ToggleGroup();

    @FXML private DatePicker ppqhFromPicker;
    @FXML private DatePicker ppqhToPicker;
    @FXML private PpqhSfButton ppqhGenerateButton;
    @FXML private PpqhSfButton ppqhPdfButton;
    @FXML private PpqhSfButton ppqhExcelButton;
    @FXML private FlowPane ppqhPresets;
    @FXML private Label ppqhRangeError;
    @FXML private VBox ppqhContent;

    public PpqhAdminReportsController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
    }

    @FXML
    private void initialize() {
        for (DatePicker ppqhP : List.of(ppqhFromPicker, ppqhToPicker)) {
            ppqhP.setConverter(ppqhDateConverter());
            ppqhP.setPromptText("dd/mm/aaaa");
            ppqhP.getEditor().textProperty().addListener((ppqhObs, ppqhOld, ppqhV) -> ppqhRangeError.setText(""));
        }
        ppqhFromPicker.setAccessibleText("Fecha inicial del reporte");
        ppqhToPicker.setAccessibleText("Fecha final del reporte");
        ppqhRangeError.managedProperty().bind(ppqhRangeError.textProperty().isNotEmpty());

        ppqhGenerateButton.ppqhWithIcon(Feather.REFRESH_CW);
        ppqhGenerateButton.disableProperty().bind(ppqhBusy);
        ppqhGenerateButton.setOnAction(ppqhE -> {
            ppqhPresetGroup.selectToggle(null);
            ppqhGenerate();
        });
        ppqhPdfButton.ppqhWithIcon(Feather.FILE_TEXT);
        ppqhExcelButton.ppqhWithIcon(Feather.GRID);
        ppqhPdfButton.disableProperty().bind(ppqhBusy.or(Bindings.createBooleanBinding(() -> ppqhReport.get() == null, ppqhReport)));
        ppqhExcelButton.disableProperty().bind(ppqhPdfButton.disableProperty());
        ppqhPdfButton.setOnAction(ppqhE -> ppqhExport("pdf", "Documento PDF", "*.pdf"));
        ppqhExcelButton.setOnAction(ppqhE -> ppqhExport("xlsx", "Libro de Excel", "*.xlsx"));

        LocalDate ppqhToday = ppqhContext.ppqhReportService().ppqhToday();
        ppqhPreset("Hoy", ppqhToday, ppqhToday);
        ppqhPreset("Últimos 7 días", ppqhToday.minusDays(6), ppqhToday);
        HepyFilterChip ppqhLast30 = ppqhPreset("Últimos 30 días", ppqhToday.minusDays(29), ppqhToday);
        ppqhPreset("Este mes", ppqhToday.withDayOfMonth(1), ppqhToday);
        LocalDate ppqhPrev = ppqhToday.withDayOfMonth(1).minusMonths(1);
        ppqhPreset("Mes anterior", ppqhPrev, ppqhPrev.withDayOfMonth(ppqhPrev.lengthOfMonth()));
        ppqhPreset("Este año", ppqhToday.withDayOfYear(1), ppqhToday);

        ppqhLast30.setSelected(true);
        ppqhFromPicker.setValue(ppqhToday.minusDays(29));
        ppqhToPicker.setValue(ppqhToday);
        ppqhGenerate();
    }

    private HepyFilterChip ppqhPreset(String ppqhLabel, LocalDate ppqhFrom, LocalDate ppqhTo) {
        HepyFilterChip ppqhChip = new HepyFilterChip(ppqhLabel);
        ppqhChip.setToggleGroup(ppqhPresetGroup);
        ppqhChip.setOnAction(ppqhE -> {
            ppqhChip.setSelected(true);
            ppqhFromPicker.setValue(ppqhFrom);
            ppqhToPicker.setValue(ppqhTo);
            ppqhGenerate();
        });
        ppqhPresets.getChildren().add(ppqhChip);
        return ppqhChip;
    }

    private void ppqhGenerate() {
        LocalDate ppqhFrom;
        LocalDate ppqhTo;
        try {
            ppqhFrom = ppqhReadDate(ppqhFromPicker);
            ppqhTo = ppqhReadDate(ppqhToPicker);
        } catch (DateTimeParseException ppqhE) {
            ppqhRangeError.setText("Escribe las fechas como dd/mm/aaaa, p. ej. 01/10/2026.");
            return;
        }
        ppqhRangeError.setText("");
        ppqhBusy.set(true);
        PpqhAsync.ppqhRun(() -> ppqhContext.ppqhReportService().ppqhSales(ppqhFrom, ppqhTo), ppqhR -> {
            ppqhBusy.set(false);
            ppqhReport.set(ppqhR);
            ppqhRender(ppqhR);
        }, ppqhE -> {
            ppqhBusy.set(false);
            if (ppqhE instanceof PpqhValidationException ppqhV) {
                ppqhRangeError.setText(ppqhV.getMessage());
            } else {
                ppqhReport.set(null);
                ppqhContent.getChildren().setAll(new PpqhEmptyState(Feather.ALERT_TRIANGLE, "No se pudo generar el reporte",
                        PpqhErrorMessages.ppqhOf(ppqhE)));
            }
        });
    }

    private static LocalDate ppqhReadDate(DatePicker ppqhPicker) {
        String ppqhText = ppqhPicker.getEditor().getText();
        if (ppqhText == null || ppqhText.isBlank()) {
            return ppqhPicker.getValue();
        }
        LocalDate ppqhValue = LocalDate.parse(ppqhText.trim(), PPQH_INPUT);
        ppqhPicker.setValue(ppqhValue);
        return ppqhValue;
    }

    private void ppqhRender(PpqhSalesReport ppqhR) {
        HBox ppqhKpis = new HBox(16,
                ppqhKpi(Feather.DOLLAR_SIGN, "VENTAS TOTALES", HepyMoney.hepyFormat(ppqhR.ppqhTotalCents()),
                        "Incluye envío: " + HepyMoney.hepyFormat(ppqhR.ppqhShippingCents())),
                ppqhKpi(Feather.SHOPPING_BAG, "PEDIDOS PAGADOS", String.valueOf(ppqhR.ppqhOrders()), "Con pago aprobado"),
                ppqhKpi(Feather.PACKAGE, "UNIDADES", String.valueOf(ppqhR.ppqhUnits()), "Prendas vendidas"),
                ppqhKpi(Feather.TRENDING_UP, "TICKET PROMEDIO", HepyMoney.hepyFormat(ppqhR.ppqhAverageTicketCents()), "Total / pedidos"));

        Label ppqhRange = new Label(PpqhReportText.ppqhRange(ppqhR));
        ppqhRange.getStyleClass().addAll("title-s", "ink");
        if (ppqhR.ppqhIsEmpty()) {
            ppqhContent.getChildren().setAll(ppqhRange, ppqhKpis, new PpqhEmptyState(Feather.BAR_CHART_2, "Sin ventas en este rango",
                    "Aquí aparecen los pedidos cuyo pago ya aprobaste. Prueba con un rango más amplio o revisa "
                            + "los pagos por verificar en Pagos."));
            return;
        }
        String ppqhUnit = PpqhReportText.ppqhPeriodHeader(ppqhR).toLowerCase();
        ppqhContent.getChildren().setAll(ppqhRange, ppqhKpis,
                ppqhCard("Ventas por " + ppqhUnit, "Total cobrado (con envío) por " + ppqhUnit + " de venta.", ppqhPeriodChart(ppqhR)),
                ppqhCard("Ventas por categoría", "Precio de las prendas con sus recargos de diseño, sin envío.", ppqhCategoryTable(ppqhR)),
                ppqhCard("Ventas por producto", null, ppqhProductTable(ppqhR)),
                ppqhCard("Por método de pago", null, ppqhMethodTable(ppqhR)));
    }

    private static Node ppqhKpi(Ikon ppqhIcon, String ppqhTitle, String ppqhValue, String ppqhDetail) {
        Label ppqhT = new Label(ppqhTitle, PpqhIcons.ppqhOf(ppqhIcon, PpqhIcons.PPQH_SM));
        ppqhT.getStyleClass().addAll("label-text", "ink-muted");
        Label ppqhV = new Label(ppqhValue);
        ppqhV.getStyleClass().addAll("heading-m", "ink");
        Label ppqhD = new Label(ppqhDetail);
        ppqhD.getStyleClass().addAll("body-s", "ink-muted");
        VBox ppqhBox = new VBox(6, ppqhT, ppqhV, ppqhD);
        ppqhBox.getStyleClass().addAll("sf-card", "sf-kpi");
        ppqhBox.setPadding(new Insets(20));
        ppqhBox.setPrefWidth(0);
        ppqhBox.setMinWidth(170);
        ppqhBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(ppqhBox, Priority.ALWAYS);
        return ppqhBox;
    }

    private static Node ppqhCard(String ppqhTitle, String ppqhNote, Node ppqhBody) {
        Label ppqhL = new Label(ppqhTitle);
        ppqhL.getStyleClass().addAll("title-s", "ink");
        VBox ppqhCard = new VBox(12, ppqhL);
        if (ppqhNote != null) {
            Label ppqhN = new Label(ppqhNote);
            ppqhN.getStyleClass().addAll("body-s", "ink-muted");
            ppqhN.setWrapText(true);
            ppqhCard.getChildren().add(ppqhN);
        }
        ppqhCard.getChildren().add(ppqhBody);
        ppqhCard.getStyleClass().addAll("sf-card", "sf-chart-card");
        ppqhCard.setPadding(new Insets(20));
        return ppqhCard;
    }

    private Node ppqhPeriodChart(PpqhSalesReport ppqhR) {
        boolean ppqhDaily = ppqhR.ppqhGrouping() == PpqhSalesReport.PpqhGrouping.DAY;
        CategoryAxis ppqhX = new CategoryAxis();
        NumberAxis ppqhY = new NumberAxis();
        ppqhY.setForceZeroInRange(true);
        ppqhY.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number ppqhN) {
                return "S/ " + Math.round(ppqhN.doubleValue());
            }

            @Override
            public Number fromString(String ppqhS) {
                return 0;
            }
        });
        BarChart<String, Number> ppqhChart = new BarChart<>(ppqhX, ppqhY);
        XYChart.Series<String, Number> ppqhSeries = new XYChart.Series<>();
        ppqhSeries.setName("Ventas");
        int ppqhStep = Math.max(1, (int) Math.ceil(ppqhR.ppqhPeriods().size() / 16.0));
        for (int ppqhI = 0; ppqhI < ppqhR.ppqhPeriods().size(); ppqhI++) {
            PpqhPeriodRow ppqhP = ppqhR.ppqhPeriods().get(ppqhI);
            String ppqhLabel = ppqhI % ppqhStep == 0 ? (ppqhDaily ? PPQH_SHORT : PPQH_MONTH).format(ppqhP.start()) : "​".repeat(ppqhI + 1);
            XYChart.Data<String, Number> ppqhD = new XYChart.Data<>(ppqhLabel, ppqhP.ppqhTotalCents() / 100.0);
            ppqhD.setExtraValue(PpqhReportText.ppqhPeriod(ppqhR, ppqhP.start()) + ": " + HepyMoney.hepyFormat(ppqhP.ppqhTotalCents()) + " · "
                    + ppqhP.ppqhOrders() + (ppqhP.ppqhOrders() == 1 ? " pedido" : " pedidos"));
            ppqhSeries.getData().add(ppqhD);
        }
        ppqhChart.getData().add(ppqhSeries);
        ppqhChart.setLegendVisible(false);
        ppqhChart.setAnimated(false);
        ppqhChart.setCategoryGap(ppqhR.ppqhPeriods().size() > 20 ? 2 : 10);
        ppqhChart.setPrefHeight(280);
        ppqhChart.getStyleClass().add("sf-chart");
        for (XYChart.Data<String, Number> ppqhD : ppqhSeries.getData()) {
            if (ppqhD.getNode() != null) {
                Tooltip.install(ppqhD.getNode(), new Tooltip(ppqhD.getExtraValue().toString()));
            }
        }
        return ppqhChart;
    }

    private Node ppqhCategoryTable(PpqhSalesReport ppqhR) {
        TableView<PpqhCategoryRow> ppqhT = ppqhTable(ppqhR.ppqhCategories());
        ppqhT.getColumns().setAll(List.of(
                ppqhColumn("CATEGORÍA", 260, ppqhC -> ppqhText(ppqhC.ppqhCategory(), "body-s")),
                ppqhColumn("PEDIDOS", 100, ppqhC -> ppqhNumber(String.valueOf(ppqhC.ppqhOrders()))),
                ppqhColumn("UNIDADES", 100, ppqhC -> ppqhNumber(String.valueOf(ppqhC.ppqhUnits()))),
                ppqhColumn("VENTAS", 140, ppqhC -> ppqhMoney(ppqhC.ppqhRevenueCents())),
                ppqhColumn("% DEL TOTAL", 120, ppqhC -> ppqhNumber(PpqhReportText.ppqhPercent(ppqhR.ppqhSharePercent(ppqhC.ppqhRevenueCents()))))));
        return ppqhT;
    }

    private Node ppqhProductTable(PpqhSalesReport ppqhR) {
        TableView<PpqhProductRow> ppqhT = ppqhTable(ppqhR.ppqhProducts());
        ppqhT.getColumns().setAll(List.of(
                ppqhColumn("SKU", 140, ppqhP -> ppqhText(ppqhP.ppqhSku(), "code")),
                ppqhColumn("PRODUCTO", 300, ppqhP -> ppqhText(ppqhP.ppqhName(), "body-s")),
                ppqhColumn("CATEGORÍA", 200, ppqhP -> ppqhText(ppqhP.ppqhCategory(), "body-s")),
                ppqhColumn("UNIDADES", 100, ppqhP -> ppqhNumber(String.valueOf(ppqhP.ppqhUnits()))),
                ppqhColumn("VENTAS", 140, ppqhP -> ppqhMoney(ppqhP.ppqhRevenueCents()))));
        return ppqhT;
    }

    private Node ppqhMethodTable(PpqhSalesReport ppqhR) {
        TableView<PpqhMethodRow> ppqhT = ppqhTable(ppqhR.ppqhMethods());
        ppqhT.getColumns().setAll(List.of(
                ppqhColumn("MÉTODO", 260, ppqhM -> ppqhText(ppqhM.ppqhMethod().ppqhLabel(), "body-s")),
                ppqhColumn("PEDIDOS", 100, ppqhM -> ppqhNumber(String.valueOf(ppqhM.ppqhOrders()))),
                ppqhColumn("TOTAL", 140, ppqhM -> ppqhMoney(ppqhM.ppqhTotalCents()))));
        return ppqhT;
    }

    private static <T> TableView<T> ppqhTable(List<T> ppqhRows) {
        TableView<T> ppqhT = new TableView<>();
        ppqhT.getItems().setAll(ppqhRows);
        ppqhT.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ppqhT.setFixedCellSize(PPQH_ROW);
        ppqhT.getStyleClass().add("sf-report-table");
        double ppqhH = PPQH_ROW * ppqhRows.size() + 44;
        ppqhT.setPrefHeight(ppqhH);
        ppqhT.setMinHeight(ppqhH);
        ppqhT.setMaxHeight(ppqhH);
        VBox.setVgrow(ppqhT, Priority.NEVER);
        return ppqhT;
    }

    private static <T> TableColumn<T, T> ppqhColumn(String ppqhTitle, double ppqhWidth, Function<T, Node> ppqhRenderer) {
        TableColumn<T, T> ppqhCol = new TableColumn<>(ppqhTitle);
        ppqhCol.setPrefWidth(ppqhWidth);
        ppqhCol.setMinWidth(Math.min(ppqhWidth, 100));
        ppqhCol.setSortable(false);
        ppqhCol.setReorderable(false);
        ppqhCol.setCellValueFactory(ppqhData -> new ReadOnlyObjectWrapper<>(ppqhData.getValue()));
        ppqhCol.setCellFactory(ppqhC -> new TableCell<>() {
            @Override
            protected void updateItem(T ppqhRow, boolean ppqhEmpty) {
                super.updateItem(ppqhRow, ppqhEmpty);
                setText(null);
                setGraphic(ppqhEmpty || ppqhRow == null ? null : ppqhRenderer.apply(ppqhRow));
            }
        });
        return ppqhCol;
    }

    private static Label ppqhText(String ppqhValue, String ppqhStyle) {
        Label ppqhL = new Label(ppqhValue);
        ppqhL.getStyleClass().addAll(ppqhStyle, "ink");
        return ppqhL;
    }

    private static Node ppqhNumber(String ppqhValue) {
        Label ppqhL = ppqhText(ppqhValue, "body-s");
        HBox ppqhBox = new HBox(ppqhL);
        ppqhBox.setAlignment(Pos.CENTER_RIGHT);
        return ppqhBox;
    }

    private static Node ppqhMoney(long ppqhCents) {
        Label ppqhL = ppqhText(HepyMoney.hepyFormat(ppqhCents), "sf-strong");
        HBox ppqhBox = new HBox(ppqhL);
        ppqhBox.setAlignment(Pos.CENTER_RIGHT);
        return ppqhBox;
    }

    private void ppqhExport(String ppqhExtension, String ppqhDescription, String ppqhPattern) {
        PpqhSalesReport ppqhR = ppqhReport.get();
        if (ppqhR == null) {
            return;
        }
        FileChooser ppqhChooser = new FileChooser();
        ppqhChooser.setTitle("Exportar reporte de ventas");
        ppqhChooser.setInitialFileName(PpqhReportService.ppqhSuggestedName(ppqhR, ppqhExtension));
        ppqhChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(ppqhDescription, ppqhPattern));
        File ppqhHome = new File(System.getProperty("user.home"), "Documents");
        if (ppqhHome.isDirectory()) {
            ppqhChooser.setInitialDirectory(ppqhHome);
        }
        File ppqhFile = ppqhChooser.showSaveDialog(ppqhContent.getScene().getWindow());
        if (ppqhFile == null) {
            return;
        }
        Path ppqhTarget = ppqhFile.toPath().getFileName().toString().toLowerCase().endsWith("." + ppqhExtension)
                ? ppqhFile.toPath() : ppqhFile.toPath().resolveSibling(ppqhFile.getName() + "." + ppqhExtension);
        ppqhBusy.set(true);
        PpqhAsync.ppqhRun(() -> ppqhExtension.equals("pdf") ? ppqhContext.ppqhReportService().ppqhExportPdf(ppqhR, ppqhTarget)
                        : ppqhContext.ppqhReportService().ppqhExportExcel(ppqhR, ppqhTarget),
                ppqhPath -> {
                    ppqhBusy.set(false);
                    PpqhToast.ppqhSuccess(ppqhContent, "Reporte guardado: " + ppqhPath.getFileName());
                },
                ppqhE -> {
                    ppqhBusy.set(false);
                    PpqhToast.ppqhError(ppqhContent, PpqhErrorMessages.ppqhOf(ppqhE));
                });
    }

    private static StringConverter<LocalDate> ppqhDateConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate ppqhD) {
                return ppqhD == null ? "" : PPQH_INPUT.format(ppqhD);
            }

            @Override
            public LocalDate fromString(String ppqhS) {
                try {
                    return ppqhS == null || ppqhS.isBlank() ? null : LocalDate.parse(ppqhS.trim(), PPQH_INPUT);
                } catch (DateTimeParseException ppqhE) {
                    return null;
                }
            }
        };
    }
}
