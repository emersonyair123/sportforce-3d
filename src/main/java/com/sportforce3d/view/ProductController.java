package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Router;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.ColorSwatch;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.FilterChip;
import com.sportforce3d.component.GarmentArt;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.QuantityStepper;
import com.sportforce3d.component.SfBadge;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfLink;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.model.ProductImage;
import com.sportforce3d.model.ProductVariant;
import com.sportforce3d.util.Money;
import com.sportforce3d.util.Texts;
import com.sportforce3d.viewmodel.ProductViewModel;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProductController implements Router.Routable {

    private static final double MEDIA_W = 440;
    private static final double MEDIA_H = MEDIA_W * 5 / 4;

    private final AppContext context;
    private final ProductViewModel viewModel;

    @FXML private SfLink backLink;
    @FXML private StackPane body;
    @FXML private FlowPane layout;

    private final StackPane media = new StackPane();
    private final HBox thumbs = new HBox(8);
    private final VBox info = new VBox(20);
    private final FlowPane sizePane = new FlowPane(8, 8);
    private final Label stockLabel = new Label();
    private final Label priceLabel = new Label();
    private final Label colorLabel = new Label();
    private final QuantityStepper stepper = new QuantityStepper();
    private final SfButton addButton = new SfButton("Agregar al carrito", SfButton.Variant.ACCENT, SfButton.Size.MD);

    public ProductController(AppContext context) {
        this.context = context;
        this.viewModel = new ProductViewModel(context.catalogService(), context.cartService());
    }

    @FXML
    private void initialize() {
        backLink.setGraphic(Icons.of(Feather.ARROW_LEFT, Icons.SM));
        backLink.setOnAction(e -> context.router().back());

        viewModel.detailProperty().addListener((obs, old, d) -> render(d));
        viewModel.errorProperty().addListener((obs, old, error) -> {
            if (error != null && !error.isEmpty()) {
                SfButton catalog = new SfButton("Ver catálogo", SfButton.Variant.PRIMARY);
                catalog.setOnAction(e -> context.router().go(Routes.CATALOG));
                body.getChildren().setAll(new EmptyState(Feather.SLASH, "Producto no disponible", error, catalog));
            }
        });
        viewModel.colorHexProperty().addListener((obs, old, hex) -> renderSizes());
        viewModel.variantProperty().addListener((obs, old, v) -> refreshSelection());

        stepper.valueProperty().bindBidirectional(viewModel.quantityProperty());
        addButton.withIcon(Feather.SHOPPING_CART);
        addButton.disableProperty().bind(viewModel.canAddProperty().not());
        addButton.setOnAction(e -> viewModel.addToCart(
                summary -> {
                    context.setCartCount(summary.itemCount());
                    Toast.success(addButton, viewModel.detailProperty().get().name() + " se agregó al carrito.");
                },
                message -> Toast.error(addButton, message)));
    }

    @Override
    public void onRoute(Object param) {
        if (param instanceof Long id) {
            viewModel.load(id);
        } else {
            context.router().go(Routes.CATALOG);
        }
    }

    private void render(ProductDetail d) {
        // Galería
        media.getStyleClass().setAll("sf-product-media", "sf-product-hero");
        media.setMinSize(MEDIA_W, MEDIA_H);
        media.setPrefSize(MEDIA_W, MEDIA_H);
        media.setMaxSize(MEDIA_W, MEDIA_H);
        Rectangle clip = new Rectangle(MEDIA_W, MEDIA_H);
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        media.setClip(clip);
        showImage(d, d.images().isEmpty() ? null : d.images().get(0));
        thumbs.getChildren().clear();
        if (d.images().size() > 1) {
            for (ProductImage img : d.images()) {
                Image small = ProductImages.load(context.storage(), img.path(), 72, 90);
                if (small == null) {
                    continue;
                }
                ImageView view = new ImageView(small);
                view.setFitWidth(72);
                view.setFitHeight(90);
                view.setPreserveRatio(true);
                SfButton thumb = new SfButton("", SfButton.Variant.GHOST, SfButton.Size.SM);
                thumb.setGraphic(view);
                thumb.setAccessibleText("Ver imagen " + (img.sortOrder() + 1));
                thumb.setOnAction(e -> showImage(d, img));
                thumbs.getChildren().add(thumb);
            }
        }
        VBox gallery = new VBox(12, media, thumbs);

        // Información
        Label category = new Label(Texts.upper(d.categoryName() + (d.sportName() == null ? "" : " · " + d.sportName())));
        category.getStyleClass().addAll("label-text", "ink-muted");
        Label name = new Label(d.name());
        name.getStyleClass().addAll("heading-m", "ink");
        name.setWrapText(true);
        Label sku = new Label(d.sku());
        sku.getStyleClass().addAll("code", "ink-subtle");

        HBox badges = new HBox(6);
        if (d.isNew()) {
            badges.getChildren().add(new SfBadge("Nuevo", SfBadge.Kind.NEW));
        }
        if (d.customizable()) {
            badges.getChildren().add(new SfBadge("Personalizable 3D", SfBadge.Kind.BRAND));
        }
        if (d.onSale()) {
            badges.getChildren().add(new SfBadge("Oferta -" + d.discountPercent() + " %", SfBadge.Kind.SALE));
        }

        priceLabel.getStyleClass().setAll("price", "ink");
        priceLabel.setStyle("-fx-font-size: 32px;");
        HBox priceRow = new HBox(12, priceLabel);
        priceRow.setAlignment(Pos.BASELINE_LEFT);
        if (d.onSale()) {
            Text was = new Text(Money.format(d.compareAtPriceCents()));
            was.getStyleClass().add("sf-price-was");
            was.setStrikethrough(true);
            priceRow.getChildren().add(was);
        }

        Label description = new Label(d.description() == null ? "" : d.description());
        description.getStyleClass().addAll("body", "ink-muted");
        description.setWrapText(true);

        // Colores
        HBox swatches = new HBox(8);
        Map<String, String> colors = viewModel.colors();
        colors.forEach((hex, colorName) -> {
            ColorSwatch swatch = new ColorSwatch(Color.web(hex), colorName);
            swatch.selectedProperty().bind(viewModel.colorHexProperty().isEqualTo(hex));
            swatch.setOnAction(e -> viewModel.colorHexProperty().set(hex));
            swatches.getChildren().add(swatch);
        });
        colorLabel.getStyleClass().setAll("body-s", "ink-muted");
        VBox colorBox = section("COLOR", new VBox(8, swatches, colorLabel));

        VBox sizeBox = section("TALLA", new VBox(8, sizePane, stockLabel));
        stockLabel.setWrapText(true);

        Label qtyLabel = new Label("CANTIDAD");
        qtyLabel.getStyleClass().addAll("label-text", "ink-muted");
        HBox actions = new HBox(12, stepper, addButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox buy = new VBox(8, qtyLabel, actions);
        if (d.customizable()) {
            // El diseñador 3D llega en la Etapa 3: se dice en texto visible.
            SfButton customize = new SfButton("Personalizar en 3D", SfButton.Variant.GHOST).withIcon(Feather.BOX);
            customize.comingSoon();
            buy.getChildren().add(customize);
        }

        info.getChildren().setAll(new VBox(6, category, name, sku), badges, priceRow, description, colorBox, sizeBox, buy);
        if (!d.materials().isEmpty()) {
            Label mats = new Label(d.materials().stream().map(Material::name).collect(Collectors.joining(" · ")));
            mats.getStyleClass().addAll("body-s", "ink-muted");
            mats.setWrapText(true);
            info.getChildren().add(section("TELAS Y MATERIALES DISPONIBLES", mats));
        }
        Label shipping = new Label("Envío a todo el Perú: " + Money.format(viewModel.shippingFlatCents())
                + " · gratis desde " + Money.format(viewModel.freeShippingFromCents()) + ".");
        shipping.getStyleClass().addAll("body-s", "ink-subtle");
        shipping.setWrapText(true);
        info.getChildren().add(shipping);
        info.setMaxWidth(520);
        info.setPrefWidth(520);

        layout.getChildren().setAll(gallery, info);
        renderSizes();
        refreshSelection();
    }

    private void showImage(ProductDetail d, ProductImage image) {
        Image full = image == null ? null : ProductImages.load(context.storage(), image.path(), MEDIA_W, MEDIA_H);
        if (full == null && viewModel.look() != null) {
            // Sin foto subida: la foto realista (como en la tarjeta); si no se puede dibujar, la ilustración.
            media.getChildren().clear();
            ProductImages.realistic(context.recolorService(), viewModel.look(), img -> {
                if (viewModel.detailProperty().get() != d || !media.getChildren().isEmpty()) {
                    return;
                }
                if (img == null) {
                    media.getChildren().setAll(art(d));
                } else {
                    ImageView view = new ImageView(img);
                    view.setPreserveRatio(true);
                    view.setSmooth(true);
                    view.setFitWidth(MEDIA_W);
                    media.getChildren().setAll(view);
                }
            });
        } else if (full == null) {
            media.getChildren().setAll(art(d));
        } else {
            ImageView view = new ImageView(full);
            view.setPreserveRatio(true);
            view.setFitWidth(MEDIA_W);
            media.getChildren().setAll(view);
        }
    }

    private GarmentArt art(ProductDetail d) {
        return new GarmentArt(d.garment(), viewModel.colorHexProperty().get() == null
                ? d.primaryColorHex() : viewModel.colorHexProperty().get(), 320);
    }

    private void renderSizes() {
        ProductDetail d = viewModel.detailProperty().get();
        if (d == null) {
            return;
        }
        String hex = viewModel.colorHexProperty().get();
        colorLabel.setText(viewModel.colors().getOrDefault(hex, "") + (hex == null ? "" : "  ·  " + hex));
        if (d.images().isEmpty() && (viewModel.look() == null || media.getChildren().isEmpty())) {
            showImage(d, null); // la ilustración toma el color elegido; la foto realista no cambia
        }
        ToggleGroup group = new ToggleGroup();
        sizePane.getChildren().clear();
        for (ProductVariant v : viewModel.variantsFor(hex)) {
            FilterChip chip = new FilterChip(v.sizeName());
            chip.setUserData(v.id());
            chip.setToggleGroup(group);
            chip.setSelected(viewModel.variantProperty().get() != null && viewModel.variantProperty().get().id() == v.id());
            if (v.available() == 0) {
                // Agotado se dice con texto, no solo atenuando.
                chip.setText(v.sizeName() + " · agotado");
                chip.setDisable(true);
            }
            chip.setAccessibleText("Talla " + v.sizeName() + (v.available() == 0 ? ", agotada" : ""));
            chip.setOnAction(e -> {
                chip.setSelected(true);
                viewModel.variantProperty().set(v);
            });
            sizePane.getChildren().add(chip);
        }
    }

    private void refreshSelection() {
        ProductVariant v = viewModel.variantProperty().get();
        priceLabel.setText(Money.format(viewModel.unitPrice()));
        stockLabel.getStyleClass().setAll("body-s");
        boolean allSoldOut = viewModel.variantsFor(viewModel.colorHexProperty().get()).stream()
                .allMatch(x -> x.available() == 0);
        if (v == null && allSoldOut) {
            stockLabel.setText("Agotado en todas las tallas de este color.");
            stockLabel.getStyleClass().add("danger-text");
            stepper.setMax(1);
        } else if (v == null) {
            stockLabel.setText("Elige una talla para ver el stock.");
            stockLabel.getStyleClass().add("ink-muted");
            stepper.setMax(1);
        } else if (v.available() == 0) {
            stockLabel.setText("Agotado en esta talla.");
            stockLabel.getStyleClass().add("danger-text");
            stepper.setMax(1);
        } else if (v.lowStock()) {
            stockLabel.setText(v.available() == 1 ? "¡Última unidad!" : "¡Últimas " + v.available() + " unidades!");
            stockLabel.getStyleClass().add("warning-text");
            stepper.setMax(Math.min(v.available(), 99));
        } else {
            stockLabel.setText(v.available() + " disponibles");
            stockLabel.getStyleClass().add("success-text");
            stepper.setMax(Math.min(v.available(), 99));
        }
        // Marca el chip de la variante elegida (p. ej. tras cambiar de color).
        for (var node : sizePane.getChildren()) {
            if (node instanceof FilterChip chip) {
                chip.setSelected(v != null && Objects.equals(chip.getUserData(), v.id()));
            }
        }
    }

    private static VBox section(String title, javafx.scene.Node content) {
        Label label = new Label(title);
        label.getStyleClass().addAll("label-text", "ink-muted");
        return new VBox(8, label, content);
    }
}
