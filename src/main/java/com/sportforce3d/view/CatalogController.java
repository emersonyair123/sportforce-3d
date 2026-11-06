package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Router;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.FilterChip;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.PageNav;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.model.Category;
import com.sportforce3d.model.Page;
import com.sportforce3d.model.ProductSort;
import com.sportforce3d.model.ProductSummary;
import com.sportforce3d.model.Size;
import com.sportforce3d.model.Sport;
import com.sportforce3d.util.Texts;
import com.sportforce3d.viewmodel.CatalogViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class CatalogController implements Router.Routable {

    private static final Map<String, String> SIZE_GROUPS = new LinkedHashMap<>();

    static {
        SIZE_GROUPS.put("ROPA", "Ropa");
        SIZE_GROUPS.put("CALZADO", "Calzado");
        SIZE_GROUPS.put("MEDIAS", "Medias");
        SIZE_GROUPS.put("UNICA", "Accesorios");
    }

    private final AppContext context;
    private final CatalogViewModel viewModel;
    private final PageNav pageNav;

    @FXML private SfButton clearButton;
    @FXML private FlowPane categoryChips;
    @FXML private FlowPane sportChips;
    @FXML private VBox sizeGroups;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;
    @FXML private SfButton applyPriceButton;
    @FXML private Label priceError;
    @FXML private CheckBox inStockCheck;
    @FXML private CheckBox customizableCheck;
    @FXML private ScrollPane resultsScroll;
    @FXML private Label title;
    @FXML private HBox searchBox;
    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private ComboBox<ProductSort> sortBox;
    @FXML private FlowPane grid;
    @FXML private StackPane pagerSlot;

    public CatalogController(AppContext context) {
        this.context = context;
        this.viewModel = new CatalogViewModel(context.catalogService());
        this.pageNav = new PageNav(page -> {
            viewModel.search(page);
            resultsScroll.setVvalue(0);
        });
    }

    @FXML
    private void initialize() {
        pagerSlot.getChildren().add(pageNav);
        searchBox.getChildren().add(0, Icons.of(Feather.SEARCH, Icons.SM));
        searchField.textProperty().bindBidirectional(viewModel.textProperty());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                viewModel.search(1);
            }
        });

        sortBox.getItems().setAll(ProductSort.values());
        sortBox.valueProperty().bindBidirectional(viewModel.sortProperty());
        sortBox.setOnAction(e -> viewModel.search(1));
        sortBox.setAccessibleText("Ordenar por");

        minPriceField.textProperty().bindBidirectional(viewModel.minPriceProperty());
        maxPriceField.textProperty().bindBidirectional(viewModel.maxPriceProperty());
        minPriceField.setOnAction(e -> viewModel.search(1));
        maxPriceField.setOnAction(e -> viewModel.search(1));
        applyPriceButton.setOnAction(e -> viewModel.search(1));
        minPriceField.setAccessibleText("Precio mínimo en soles");
        maxPriceField.setAccessibleText("Precio máximo en soles");
        priceError.textProperty().bind(viewModel.priceErrorProperty());
        priceError.managedProperty().bind(viewModel.priceErrorProperty().isNotEmpty());
        priceError.visibleProperty().bind(priceError.managedProperty());

        inStockCheck.selectedProperty().bindBidirectional(viewModel.inStockOnlyProperty());
        customizableCheck.selectedProperty().bindBidirectional(viewModel.customizableOnlyProperty());
        inStockCheck.setOnAction(e -> viewModel.search(1));
        customizableCheck.setOnAction(e -> viewModel.search(1));

        clearButton.setOnAction(e -> {
            viewModel.clearFilters();
            renderFilters();
        });

        viewModel.resultProperty().addListener((obs, old, page) -> render(page));
        viewModel.errorProperty().addListener((obs, old, error) -> {
            if (error != null && !error.isEmpty()) {
                grid.getChildren().setAll(new EmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar el catálogo", error));
            }
        });
    }

    @Override
    public void onRoute(Object param) {
        if (param instanceof Routes.CatalogParams p) {
            viewModel.textProperty().set(p.text() == null ? "" : p.text());
            viewModel.categoryIdProperty().set(p.categoryId());
        }
        viewModel.start(this::renderFilters);
    }

    private void renderFilters() {
        ToggleGroup categories = new ToggleGroup();
        categoryChips.getChildren().setAll(chip("Todas", null, viewModel.categoryIdProperty().get(), categories,
                id -> viewModel.categoryIdProperty().set(id)));
        for (Category c : viewModel.categories()) {
            categoryChips.getChildren().add(chip(c.name(), c.id(), viewModel.categoryIdProperty().get(), categories,
                    id -> viewModel.categoryIdProperty().set(id)));
        }

        ToggleGroup sports = new ToggleGroup();
        sportChips.getChildren().setAll(chip("Todos", null, viewModel.sportIdProperty().get(), sports,
                id -> viewModel.sportIdProperty().set(id)));
        for (Sport s : viewModel.sports()) {
            sportChips.getChildren().add(chip(s.name(), s.id(), viewModel.sportIdProperty().get(), sports,
                    id -> viewModel.sportIdProperty().set(id)));
        }

        sizeGroups.getChildren().clear();
        SIZE_GROUPS.forEach((group, label) -> {
            List<Size> inGroup = viewModel.sizes().stream().filter(s -> s.group().equals(group)).toList();
            if (inGroup.isEmpty()) {
                return;
            }
            FlowPane pane = new FlowPane(6, 6);
            for (Size size : inGroup) {
                FilterChip chip = new FilterChip(size.name());
                chip.setSelected(viewModel.sizeCodes().contains(size.code()));
                chip.setAccessibleText("Talla " + size.name());
                chip.setOnAction(e -> {
                    if (chip.isSelected()) {
                        viewModel.sizeCodes().add(size.code());
                    } else {
                        viewModel.sizeCodes().remove(size.code());
                    }
                    viewModel.search(1);
                });
                pane.getChildren().add(chip);
            }
            Label caption = new Label(label);
            caption.getStyleClass().addAll("body-s", "ink-subtle");
            sizeGroups.getChildren().add(new VBox(4, caption, pane));
        });
    }

    /** Chip de selección única: volver a pulsar el elegido no lo deja vacío ("Todas" sigue siendo una opción). */
    private FilterChip chip(String text, Long id, Long current, ToggleGroup group, Consumer<Long> onSelect) {
        FilterChip chip = new FilterChip(text);
        chip.setToggleGroup(group);
        chip.setSelected(Objects.equals(id, current));
        chip.setOnAction(e -> {
            chip.setSelected(true);
            onSelect.accept(id);
            viewModel.search(1);
        });
        return chip;
    }

    private void render(Page<ProductSummary> page) {
        String category = viewModel.categoryName();
        title.setText(category == null ? "CATÁLOGO" : Texts.upper(category));
        countLabel.setText(page.total() == 1 ? "1 producto" : page.total() + " productos");
        pageNav.show(page);

        grid.getChildren().clear();
        if (page.items().isEmpty()) {
            SfButton clear = new SfButton("Limpiar filtros", SfButton.Variant.GHOST);
            clear.setOnAction(e -> {
                viewModel.clearFilters();
                renderFilters();
            });
            grid.getChildren().add(new EmptyState(Feather.SEARCH, "No encontramos productos",
                    "Prueba con otra búsqueda o quita algunos filtros.", clear));
            return;
        }
        for (ProductSummary product : page.items()) {
            grid.getChildren().add(ShopActions.card(context, product));
        }
    }
}
