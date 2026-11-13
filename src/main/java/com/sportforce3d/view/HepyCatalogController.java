package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhRouter;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.HepyFilterChip;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.HepyPageNav;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.model.HepyCategory;
import com.sportforce3d.model.HepyPage;
import com.sportforce3d.model.HepyProductSort;
import com.sportforce3d.model.HepyProductSummary;
import com.sportforce3d.model.HepySize;
import com.sportforce3d.model.HepySport;
import com.sportforce3d.util.HepyTexts;
import com.sportforce3d.viewmodel.HepyCatalogViewModel;
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

public class HepyCatalogController implements PpqhRouter.PpqhRoutable {

    private static final Map<String, String> HEPY_SIZE_GROUPS = new LinkedHashMap<>();

    static {
        HEPY_SIZE_GROUPS.put("ROPA", "Ropa");
        HEPY_SIZE_GROUPS.put("CALZADO", "Calzado");
        HEPY_SIZE_GROUPS.put("MEDIAS", "Medias");
        HEPY_SIZE_GROUPS.put("UNICA", "Accesorios");
    }

    private final PpqhAppContext hepyContext;
    private final HepyCatalogViewModel hepyViewModel;
    private final HepyPageNav hepyPageNav;

    @FXML private PpqhSfButton hepyClearButton;
    @FXML private FlowPane hepyCategoryChips;
    @FXML private FlowPane hepySportChips;
    @FXML private VBox hepySizeGroups;
    @FXML private TextField hepyMinPriceField;
    @FXML private TextField hepyMaxPriceField;
    @FXML private PpqhSfButton hepyApplyPriceButton;
    @FXML private Label hepyPriceError;
    @FXML private CheckBox hepyInStockCheck;
    @FXML private CheckBox hepyCustomizableCheck;
    @FXML private ScrollPane hepyResultsScroll;
    @FXML private Label hepyTitle;
    @FXML private HBox hepySearchBox;
    @FXML private TextField hepySearchField;
    @FXML private Label hepyCountLabel;
    @FXML private ComboBox<HepyProductSort> hepySortBox;
    @FXML private FlowPane hepyGrid;
    @FXML private StackPane hepyPagerSlot;

    public HepyCatalogController(PpqhAppContext hepyContext) {
        this.hepyContext = hepyContext;
        this.hepyViewModel = new HepyCatalogViewModel(hepyContext.ppqhCatalogService());
        this.hepyPageNav = new HepyPageNav(hepyPage -> {
            hepyViewModel.hepySearch(hepyPage);
            hepyResultsScroll.setVvalue(0);
        });
    }

    @FXML
    private void initialize() {
        hepyPagerSlot.getChildren().add(hepyPageNav);
        hepySearchBox.getChildren().add(0, PpqhIcons.ppqhOf(Feather.SEARCH, PpqhIcons.PPQH_SM));
        hepySearchField.textProperty().bindBidirectional(hepyViewModel.textProperty());
        hepySearchField.setOnKeyPressed(hepyE -> {
            if (hepyE.getCode() == KeyCode.ENTER) {
                hepyViewModel.hepySearch(1);
            }
        });

        hepySortBox.getItems().setAll(HepyProductSort.values());
        hepySortBox.valueProperty().bindBidirectional(hepyViewModel.sortProperty());
        hepySortBox.setOnAction(hepyE -> hepyViewModel.hepySearch(1));
        hepySortBox.setAccessibleText("Ordenar por");

        hepyMinPriceField.textProperty().bindBidirectional(hepyViewModel.minPriceProperty());
        hepyMaxPriceField.textProperty().bindBidirectional(hepyViewModel.maxPriceProperty());
        hepyMinPriceField.setOnAction(hepyE -> hepyViewModel.hepySearch(1));
        hepyMaxPriceField.setOnAction(hepyE -> hepyViewModel.hepySearch(1));
        hepyApplyPriceButton.setOnAction(hepyE -> hepyViewModel.hepySearch(1));
        hepyMinPriceField.setAccessibleText("Precio mínimo en soles");
        hepyMaxPriceField.setAccessibleText("Precio máximo en soles");
        hepyPriceError.textProperty().bind(hepyViewModel.priceErrorProperty());
        hepyPriceError.managedProperty().bind(hepyViewModel.priceErrorProperty().isNotEmpty());
        hepyPriceError.visibleProperty().bind(hepyPriceError.managedProperty());

        hepyInStockCheck.selectedProperty().bindBidirectional(hepyViewModel.inStockOnlyProperty());
        hepyCustomizableCheck.selectedProperty().bindBidirectional(hepyViewModel.customizableOnlyProperty());
        hepyInStockCheck.setOnAction(hepyE -> hepyViewModel.hepySearch(1));
        hepyCustomizableCheck.setOnAction(hepyE -> hepyViewModel.hepySearch(1));

        hepyClearButton.setOnAction(hepyE -> {
            hepyViewModel.hepyClearFilters();
            hepyRenderFilters();
        });

        hepyViewModel.resultProperty().addListener((hepyObs, hepyOld, hepyPage) -> hepyRender(hepyPage));
        hepyViewModel.errorProperty().addListener((hepyObs, hepyOld, hepyError) -> {
            if (hepyError != null && !hepyError.isEmpty()) {
                hepyGrid.getChildren().setAll(new PpqhEmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar el catálogo", hepyError));
            }
        });
    }

    @Override
    public void ppqhOnRoute(Object hepyParam) {
        if (hepyParam instanceof PpqhRoutes.PpqhCatalogParams hepyP) {
            hepyViewModel.textProperty().set(hepyP.ppqhText() == null ? "" : hepyP.ppqhText());
            hepyViewModel.categoryIdProperty().set(hepyP.ppqhCategoryId());
        }
        hepyViewModel.start(this::hepyRenderFilters);
    }

    private void hepyRenderFilters() {
        ToggleGroup hepyCategories = new ToggleGroup();
        hepyCategoryChips.getChildren().setAll(hepyChip("Todas", null, hepyViewModel.categoryIdProperty().get(), hepyCategories,
                hepyId -> hepyViewModel.categoryIdProperty().set(hepyId)));
        for (HepyCategory hepyC : hepyViewModel.hepyCategories()) {
            hepyCategoryChips.getChildren().add(hepyChip(hepyC.hepyName(), hepyC.hepyId(), hepyViewModel.categoryIdProperty().get(), hepyCategories,
                    hepyId -> hepyViewModel.categoryIdProperty().set(hepyId)));
        }

        ToggleGroup hepySports = new ToggleGroup();
        hepySportChips.getChildren().setAll(hepyChip("Todos", null, hepyViewModel.sportIdProperty().get(), hepySports,
                hepyId -> hepyViewModel.sportIdProperty().set(hepyId)));
        for (HepySport hepyS : hepyViewModel.hepySports()) {
            hepySportChips.getChildren().add(hepyChip(hepyS.hepyName(), hepyS.hepyId(), hepyViewModel.sportIdProperty().get(), hepySports,
                    hepyId -> hepyViewModel.sportIdProperty().set(hepyId)));
        }

        hepySizeGroups.getChildren().clear();
        HEPY_SIZE_GROUPS.forEach((hepyGroup, hepyLabel) -> {
            List<HepySize> hepyInGroup = hepyViewModel.hepySizes().stream().filter(hepyS -> hepyS.hepyGroup().equals(hepyGroup)).toList();
            if (hepyInGroup.isEmpty()) {
                return;
            }
            FlowPane hepyPane = new FlowPane(6, 6);
            for (HepySize hepySize : hepyInGroup) {
                HepyFilterChip hepyChip = new HepyFilterChip(hepySize.hepyName());
                hepyChip.setSelected(hepyViewModel.hepySizeCodes().contains(hepySize.hepyCode()));
                hepyChip.setAccessibleText("Talla " + hepySize.hepyName());
                hepyChip.setOnAction(hepyE -> {
                    if (hepyChip.isSelected()) {
                        hepyViewModel.hepySizeCodes().add(hepySize.hepyCode());
                    } else {
                        hepyViewModel.hepySizeCodes().remove(hepySize.hepyCode());
                    }
                    hepyViewModel.hepySearch(1);
                });
                hepyPane.getChildren().add(hepyChip);
            }
            Label hepyCaption = new Label(hepyLabel);
            hepyCaption.getStyleClass().addAll("body-s", "ink-subtle");
            hepySizeGroups.getChildren().add(new VBox(4, hepyCaption, hepyPane));
        });
    }

    private HepyFilterChip hepyChip(String hepyText, Long hepyId, Long hepyCurrent, ToggleGroup hepyGroup, Consumer<Long> hepyOnSelect) {
        HepyFilterChip hepyChip = new HepyFilterChip(hepyText);
        hepyChip.setToggleGroup(hepyGroup);
        hepyChip.setSelected(Objects.equals(hepyId, hepyCurrent));
        hepyChip.setOnAction(hepyE -> {
            hepyChip.setSelected(true);
            hepyOnSelect.accept(hepyId);
            hepyViewModel.hepySearch(1);
        });
        return hepyChip;
    }

    private void hepyRender(HepyPage<HepyProductSummary> hepyPage) {
        String hepyCategory = hepyViewModel.hepyCategoryName();
        hepyTitle.setText(hepyCategory == null ? "CATÁLOGO" : HepyTexts.hepyUpper(hepyCategory));
        hepyCountLabel.setText(hepyPage.hepyTotal() == 1 ? "1 producto" : hepyPage.hepyTotal() + " productos");
        hepyPageNav.hepyShow(hepyPage);

        hepyGrid.getChildren().clear();
        if (hepyPage.hepyItems().isEmpty()) {
            PpqhSfButton hepyClear = new PpqhSfButton("Limpiar filtros", PpqhSfButton.PpqhVariant.GHOST);
            hepyClear.setOnAction(hepyE -> {
                hepyViewModel.hepyClearFilters();
                hepyRenderFilters();
            });
            hepyGrid.getChildren().add(new PpqhEmptyState(Feather.SEARCH, "No encontramos productos",
                    "Prueba con otra búsqueda o quita algunos filtros.", hepyClear));
            return;
        }
        for (HepyProductSummary hepyProduct : hepyPage.hepyItems()) {
            hepyGrid.getChildren().add(HepyShopActions.hepyCard(hepyContext, hepyProduct));
        }
    }
}
