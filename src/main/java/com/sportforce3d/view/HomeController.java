package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.model.Category;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.ProductSummary;
import com.sportforce3d.util.Texts;
import com.sportforce3d.viewmodel.HomeViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import org.kordamp.ikonli.feather.Feather;

public class HomeController {

    private final AppContext context;
    private final HomeViewModel viewModel;

    @FXML private Label greeting;
    @FXML private SfButton designCta;
    @FXML private SfButton catalogButton;
    @FXML private FlowPane categoriesPane;
    @FXML private FlowPane featuredPane;

    public HomeController(AppContext context) {
        this.context = context;
        this.viewModel = new HomeViewModel(context.catalogService());
    }

    @FXML
    private void initialize() {
        context.session().current().ifPresent(user -> greeting.setText(Texts.upper("Hola, " + user.firstName())));
        designCta.withIcon(Feather.BOX);
        designCta.setOnAction(e -> context.router().go(Routes.DESIGNER, Routes.DesignerParams.focus(Garment.JERSEY)));
        catalogButton.withIcon(Feather.GRID);
        catalogButton.setOnAction(e -> context.router().go(Routes.CATALOG));

        viewModel.categories().addListener((ListChangeListener<Category>) c -> renderCategories());
        viewModel.featured().addListener((ListChangeListener<ProductSummary>) c -> renderFeatured());
        viewModel.errorProperty().addListener((obs, old, error) -> {
            if (error != null && !error.isEmpty()) {
                featuredPane.getChildren().setAll(new EmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar el catálogo", error));
            }
        });
        viewModel.load();
    }

    private void renderCategories() {
        categoriesPane.getChildren().clear();
        for (Category category : viewModel.categories()) {
            Button chip = new Button(Texts.upper(category.name()));
            chip.getStyleClass().setAll("sf-chip", "sf-chip-link");
            chip.setMnemonicParsing(false);
            if (category.description() != null) {
                chip.setTooltip(new Tooltip(category.description()));
            }
            chip.setOnAction(e -> context.router().go(Routes.CATALOG, new Routes.CatalogParams(null, category.id())));
            categoriesPane.getChildren().add(chip);
        }
    }

    private void renderFeatured() {
        featuredPane.getChildren().clear();
        if (viewModel.featured().isEmpty()) {
            featuredPane.getChildren().add(new EmptyState(Feather.SHOPPING_BAG, "Aún no hay destacados",
                    "Cuando la tienda marque productos como destacados aparecerán aquí."));
            return;
        }
        for (ProductSummary product : viewModel.featured()) {
            featuredPane.getChildren().add(ShopActions.card(context, product));
        }
    }
}
