package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.model.HepyCategory;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyProductSummary;
import com.sportforce3d.util.HepyTexts;
import com.sportforce3d.viewmodel.HepyHomeViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import org.kordamp.ikonli.feather.Feather;

public class HepyHomeController {

    private final PpqhAppContext hepyContext;
    private final HepyHomeViewModel hepyViewModel;

    @FXML private Label hepyGreeting;
    @FXML private PpqhSfButton hepyDesignCta;
    @FXML private PpqhSfButton hepyCatalogButton;
    @FXML private FlowPane hepyCategoriesPane;
    @FXML private FlowPane hepyFeaturedPane;

    public HepyHomeController(PpqhAppContext hepyContext) {
        this.hepyContext = hepyContext;
        this.hepyViewModel = new HepyHomeViewModel(hepyContext.ppqhCatalogService());
    }

    @FXML
    private void initialize() {
        hepyContext.ppqhSession().ppqhCurrent().ifPresent(hepyUser -> hepyGreeting.setText(HepyTexts.hepyUpper("Hola, " + hepyUser.ppqhFirstName())));
        hepyDesignCta.ppqhWithIcon(Feather.BOX);
        hepyDesignCta.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_DESIGNER, PpqhRoutes.PpqhDesignerParams.ppqhFocus(HepyGarment.JERSEY)));
        hepyCatalogButton.ppqhWithIcon(Feather.GRID);
        hepyCatalogButton.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CATALOG));

        hepyViewModel.hepyCategories().addListener((ListChangeListener<HepyCategory>) hepyC -> hepyRenderCategories());
        hepyViewModel.hepyFeatured().addListener((ListChangeListener<HepyProductSummary>) hepyC -> hepyRenderFeatured());
        hepyViewModel.errorProperty().addListener((hepyObs, hepyOld, hepyError) -> {
            if (hepyError != null && !hepyError.isEmpty()) {
                hepyFeaturedPane.getChildren().setAll(new PpqhEmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar el catálogo", hepyError));
            }
        });
        hepyViewModel.hepyLoad();
    }

    private void hepyRenderCategories() {
        hepyCategoriesPane.getChildren().clear();
        for (HepyCategory hepyCategory : hepyViewModel.hepyCategories()) {
            Button hepyChip = new Button(HepyTexts.hepyUpper(hepyCategory.hepyName()));
            hepyChip.getStyleClass().setAll("sf-chip", "sf-chip-link");
            hepyChip.setMnemonicParsing(false);
            if (hepyCategory.hepyDescription() != null) {
                hepyChip.setTooltip(new Tooltip(hepyCategory.hepyDescription()));
            }
            hepyChip.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CATALOG, new PpqhRoutes.PpqhCatalogParams(null, hepyCategory.hepyId())));
            hepyCategoriesPane.getChildren().add(hepyChip);
        }
    }

    private void hepyRenderFeatured() {
        hepyFeaturedPane.getChildren().clear();
        if (hepyViewModel.hepyFeatured().isEmpty()) {
            hepyFeaturedPane.getChildren().add(new PpqhEmptyState(Feather.SHOPPING_BAG, "Aún no hay destacados",
                    "Cuando la tienda marque productos como destacados aparecerán aquí."));
            return;
        }
        for (HepyProductSummary hepyProduct : hepyViewModel.hepyFeatured()) {
            hepyFeaturedPane.getChildren().add(HepyShopActions.hepyCard(hepyContext, hepyProduct));
        }
    }
}
