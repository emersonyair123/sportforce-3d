package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhRouter;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PgeyColorSwatch;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.HepyFilterChip;
import com.sportforce3d.component.HepyGarmentArt;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.HepyQuantityStepper;
import com.sportforce3d.component.PpqhSfBadge;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfLink;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.model.HepyProductImage;
import com.sportforce3d.model.HepyProductVariant;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.util.HepyTexts;
import com.sportforce3d.viewmodel.HepyProductViewModel;
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

public class HepyProductController implements PpqhRouter.PpqhRoutable {

    private static final double HEPY_MEDIA_W = 400;
    private static final double HEPY_MEDIA_H = HEPY_MEDIA_W * 5 / 4;

    private final PpqhAppContext hepyContext;
    private final HepyProductViewModel hepyViewModel;

    @FXML private PpqhSfLink hepyBackLink;
    @FXML private StackPane hepyBody;
    @FXML private FlowPane hepyLayout;

    private final StackPane hepyMedia = new StackPane();
    private final HBox hepyThumbs = new HBox(8);
    private final VBox hepyInfo = new VBox(20);
    private final FlowPane hepySizePane = new FlowPane(8, 8);
    private final Label hepyStockLabel = new Label();
    private final Label hepyPriceLabel = new Label();
    private final Label hepyColorLabel = new Label();
    private final HepyQuantityStepper hepyStepper = new HepyQuantityStepper();
    private final PpqhSfButton hepyAddButton = new PpqhSfButton("Agregar al carrito", PpqhSfButton.PpqhVariant.ACCENT, PpqhSfButton.PpqhSize.MD);

    public HepyProductController(PpqhAppContext hepyContext) {
        this.hepyContext = hepyContext;
        this.hepyViewModel = new HepyProductViewModel(hepyContext.ppqhCatalogService(), hepyContext.ppqhCartService());
    }

    @FXML
    private void initialize() {
        hepyBackLink.setGraphic(PpqhIcons.ppqhOf(Feather.ARROW_LEFT, PpqhIcons.PPQH_SM));
        hepyBackLink.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhBack());

        hepyViewModel.detailProperty().addListener((hepyObs, hepyOld, hepyD) -> hepyRender(hepyD));
        hepyViewModel.errorProperty().addListener((hepyObs, hepyOld, hepyError) -> {
            if (hepyError != null && !hepyError.isEmpty()) {
                PpqhSfButton hepyCatalog = new PpqhSfButton("Ver catálogo", PpqhSfButton.PpqhVariant.PRIMARY);
                hepyCatalog.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CATALOG));
                hepyBody.getChildren().setAll(new PpqhEmptyState(Feather.SLASH, "Producto no disponible", hepyError, hepyCatalog));
            }
        });
        hepyViewModel.colorHexProperty().addListener((hepyObs, hepyOld, hepyHex) -> {
            hepyRenderSizes();
            hepyRefreshSelection();
        });
        hepyViewModel.variantProperty().addListener((hepyObs, hepyOld, hepyV) -> hepyRefreshSelection());

        hepyStepper.valueProperty().bindBidirectional(hepyViewModel.quantityProperty());
        hepyAddButton.ppqhWithIcon(Feather.SHOPPING_CART);
        hepyAddButton.disableProperty().bind(hepyViewModel.canAddProperty().not());
        hepyAddButton.setOnAction(hepyE -> hepyViewModel.hepyAddToCart(
                hepySummary -> {
                    hepyContext.setCartCount(hepySummary.hepyItemCount());
                    PpqhToast.ppqhSuccess(hepyAddButton, hepyViewModel.detailProperty().get().hepyName() + " se agregó al carrito.");
                },
                hepyMessage -> PpqhToast.ppqhError(hepyAddButton, hepyMessage)));
    }

    @Override
    public void ppqhOnRoute(Object hepyParam) {
        if (hepyParam instanceof Long hepyId) {
            hepyViewModel.hepyLoad(hepyId);
        } else {
            hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CATALOG);
        }
    }

    private void hepyRender(HepyProductDetail hepyD) {
        hepyMedia.getStyleClass().setAll("sf-product-media", "sf-product-hero");
        hepyMedia.setMinSize(HEPY_MEDIA_W, HEPY_MEDIA_H);
        hepyMedia.setPrefSize(HEPY_MEDIA_W, HEPY_MEDIA_H);
        hepyMedia.setMaxSize(HEPY_MEDIA_W, HEPY_MEDIA_H);
        Rectangle hepyClip = new Rectangle(HEPY_MEDIA_W, HEPY_MEDIA_H);
        hepyClip.setArcWidth(32);
        hepyClip.setArcHeight(32);
        hepyMedia.setClip(hepyClip);
        hepyShowImage(hepyD, hepyD.hepyImages().isEmpty() ? null : hepyD.hepyImages().get(0));
        hepyThumbs.getChildren().clear();
        if (hepyD.hepyImages().size() > 1) {
            for (HepyProductImage hepyImg : hepyD.hepyImages()) {
                Image hepySmall = HepyProductImages.hepyLoad(hepyContext.ppqhStorage(), hepyImg.hepyPath(), 72, 90);
                if (hepySmall == null) {
                    continue;
                }
                ImageView hepyView = new ImageView(hepySmall);
                hepyView.setFitWidth(72);
                hepyView.setFitHeight(90);
                hepyView.setPreserveRatio(true);
                PpqhSfButton hepyThumb = new PpqhSfButton("", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
                hepyThumb.setGraphic(hepyView);
                hepyThumb.setAccessibleText("Ver imagen " + (hepyImg.hepySortOrder() + 1));
                hepyThumb.setOnAction(hepyE -> hepyShowImage(hepyD, hepyImg));
                hepyThumbs.getChildren().add(hepyThumb);
            }
        }
        VBox hepyGallery = new VBox(12, hepyMedia, hepyThumbs);

        Label hepyCategory = new Label(HepyTexts.hepyUpper(hepyD.hepyCategoryName() + (hepyD.hepySportName() == null ? "" : " · " + hepyD.hepySportName())));
        hepyCategory.getStyleClass().addAll("label-text", "ink-muted");
        Label hepyName = new Label(hepyD.hepyName());
        hepyName.getStyleClass().addAll("heading-m", "ink");
        hepyName.setWrapText(true);
        Label hepySku = new Label(hepyD.hepySku());
        hepySku.getStyleClass().addAll("code", "ink-subtle");

        HBox hepyBadges = new HBox(6);
        if (hepyD.hepyIsNew()) {
            hepyBadges.getChildren().add(new PpqhSfBadge("Nuevo", PpqhSfBadge.PpqhKind.NEW));
        }
        if (hepyD.hepyCustomizable()) {
            hepyBadges.getChildren().add(new PpqhSfBadge("Personalizable 3D", PpqhSfBadge.PpqhKind.BRAND));
        }
        if (hepyD.hepyOnSale()) {
            hepyBadges.getChildren().add(new PpqhSfBadge("Oferta -" + hepyD.hepyDiscountPercent() + " %", PpqhSfBadge.PpqhKind.SALE));
        }

        hepyPriceLabel.getStyleClass().setAll("price", "ink");
        hepyPriceLabel.setStyle("-fx-font-size: 32px;");
        HBox hepyPriceRow = new HBox(12, hepyPriceLabel);
        hepyPriceRow.setAlignment(Pos.BASELINE_LEFT);
        if (hepyD.hepyOnSale()) {
            Text hepyWas = new Text(HepyMoney.hepyFormat(hepyD.hepyCompareAtPriceCents()));
            hepyWas.getStyleClass().add("sf-price-was");
            hepyWas.setStrikethrough(true);
            hepyPriceRow.getChildren().add(hepyWas);
        }

        Label hepyDescription = new Label(hepyD.hepyDescription() == null ? "" : hepyD.hepyDescription());
        hepyDescription.getStyleClass().addAll("body", "ink-muted");
        hepyDescription.setWrapText(true);

        HBox hepySwatches = new HBox(8);
        Map<String, String> hepyColors = hepyViewModel.hepyColors();
        hepyColors.forEach((hepyHex, hepyColorName) -> {
            PgeyColorSwatch hepySwatch = new PgeyColorSwatch(Color.web(hepyHex), hepyColorName);
            hepySwatch.selectedProperty().bind(hepyViewModel.colorHexProperty().isEqualTo(hepyHex));
            hepySwatch.setOnAction(hepyE -> hepyViewModel.colorHexProperty().set(hepyHex));
            hepySwatches.getChildren().add(hepySwatch);
        });
        hepyColorLabel.getStyleClass().setAll("body-s", "ink-muted");
        VBox hepyColorBox = hepySection("COLOR", new VBox(8, hepySwatches, hepyColorLabel));

        VBox hepySizeBox = hepySection("TALLA", new VBox(8, hepySizePane, hepyStockLabel));
        hepyStockLabel.setWrapText(true);

        Label hepyQtyLabel = new Label("CANTIDAD");
        hepyQtyLabel.getStyleClass().addAll("label-text", "ink-muted");
        HBox hepyActions = new HBox(12, hepyStepper, hepyAddButton);
        hepyActions.setAlignment(Pos.CENTER_LEFT);

        VBox hepyBuy = new VBox(8, hepyQtyLabel, hepyActions);
        if (hepyD.hepyCustomizable()) {
            PpqhSfButton hepyCustomize = new PpqhSfButton("Personalizar en 3D", PpqhSfButton.PpqhVariant.GHOST).ppqhWithIcon(Feather.BOX);
            hepyCustomize.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_DESIGNER, new PpqhRoutes.PpqhDesignerParams(hepyDesignerFocus(hepyD.hepyGarment()), null, hepyD.hepyId())));
            hepyBuy.getChildren().add(hepyCustomize);
        }

        hepyInfo.getChildren().setAll(new VBox(6, hepyCategory, hepyName, hepySku), hepyBadges, hepyPriceRow, hepyDescription, hepyColorBox, hepySizeBox, hepyBuy);
        if (!hepyD.hepyMaterials().isEmpty()) {
            Label hepyMats = new Label(hepyD.hepyMaterials().stream().map(HepyMaterial::hepyName).collect(Collectors.joining(" · ")));
            hepyMats.getStyleClass().addAll("body-s", "ink-muted");
            hepyMats.setWrapText(true);
            hepyInfo.getChildren().add(hepySection("TELAS Y MATERIALES DISPONIBLES", hepyMats));
        }
        Label hepyShipping = new Label("Envío a todo el Perú: " + HepyMoney.hepyFormat(hepyViewModel.hepyShippingFlatCents())
                + " · gratis desde " + HepyMoney.hepyFormat(hepyViewModel.hepyFreeShippingFromCents()) + ".");
        hepyShipping.getStyleClass().addAll("body-s", "ink-subtle");
        hepyShipping.setWrapText(true);
        hepyInfo.getChildren().add(hepyShipping);
        hepyInfo.setMaxWidth(500);
        hepyInfo.setPrefWidth(480);

        hepyLayout.getChildren().setAll(hepyGallery, hepyInfo);
        hepyRenderSizes();
        hepyRefreshSelection();
    }

    private void hepyShowImage(HepyProductDetail hepyD, HepyProductImage hepyImage) {
        Image hepyFull = hepyImage == null ? null : HepyProductImages.hepyLoad(hepyContext.ppqhStorage(), hepyImage.hepyPath(), HEPY_MEDIA_W, HEPY_MEDIA_H);
        if (hepyFull == null && hepyViewModel.hepyLook() != null) {
            hepyMedia.getChildren().clear();
            HepyProductImages.hepyRealistic(hepyContext.ppqhRecolorService(), hepyViewModel.hepyLook(), hepyImg -> {
                if (hepyViewModel.detailProperty().get() != hepyD || !hepyMedia.getChildren().isEmpty()) {
                    return;
                }
                if (hepyImg == null) {
                    hepyMedia.getChildren().setAll(hepyArt(hepyD));
                } else {
                    ImageView hepyView = new ImageView(hepyImg);
                    hepyView.setPreserveRatio(true);
                    hepyView.setSmooth(true);
                    hepyView.setFitWidth(HEPY_MEDIA_W);
                    hepyMedia.getChildren().setAll(hepyView);
                }
            });
        } else if (hepyFull == null) {
            hepyMedia.getChildren().setAll(hepyArt(hepyD));
        } else {
            ImageView hepyView = new ImageView(hepyFull);
            hepyView.setPreserveRatio(true);
            hepyView.setFitWidth(HEPY_MEDIA_W);
            hepyMedia.getChildren().setAll(hepyView);
        }
    }

    private HepyGarmentArt hepyArt(HepyProductDetail hepyD) {
        return new HepyGarmentArt(hepyD.hepyGarment(), hepyViewModel.colorHexProperty().get() == null
                ? hepyD.hepyPrimaryColorHex() : hepyViewModel.colorHexProperty().get(), 320);
    }

    private void hepyRenderSizes() {
        HepyProductDetail hepyD = hepyViewModel.detailProperty().get();
        if (hepyD == null) {
            return;
        }
        String hepyHex = hepyViewModel.colorHexProperty().get();
        hepyColorLabel.setText(hepyViewModel.hepyColors().getOrDefault(hepyHex, "") + (hepyHex == null ? "" : "  ·  " + hepyHex));
        if (hepyD.hepyImages().isEmpty() && hepyViewModel.hepyLook() == null) {
            hepyShowImage(hepyD, null);
        }
        ToggleGroup hepyGroup = new ToggleGroup();
        hepySizePane.getChildren().clear();
        for (HepyProductVariant hepyV : hepyViewModel.hepyVariantsFor(hepyHex)) {
            HepyFilterChip hepyChip = new HepyFilterChip(hepyV.hepySizeName());
            hepyChip.setUserData(hepyV.hepyId());
            hepyChip.setToggleGroup(hepyGroup);
            hepyChip.setSelected(hepyViewModel.variantProperty().get() != null && hepyViewModel.variantProperty().get().hepyId() == hepyV.hepyId());
            if (hepyV.hepyAvailable() == 0) {
                hepyChip.setText(hepyV.hepySizeName() + " · agotado");
                hepyChip.setDisable(true);
            }
            hepyChip.setAccessibleText("Talla " + hepyV.hepySizeName() + (hepyV.hepyAvailable() == 0 ? ", agotada" : ""));
            hepyChip.setOnAction(hepyE -> {
                hepyChip.setSelected(true);
                hepyViewModel.variantProperty().set(hepyV);
            });
            hepySizePane.getChildren().add(hepyChip);
        }
    }

    private void hepyRefreshSelection() {
        HepyProductVariant hepyV = hepyViewModel.variantProperty().get();
        hepyPriceLabel.setText(HepyMoney.hepyFormat(hepyViewModel.hepyUnitPrice()));
        hepyStockLabel.getStyleClass().setAll("body-s");
        List<HepyProductVariant> hepyForColor = hepyViewModel.hepyVariantsFor(hepyViewModel.colorHexProperty().get());
        boolean hepyAllSoldOut = !hepyForColor.isEmpty() && hepyForColor.stream().allMatch(hepyX -> hepyX.hepyAvailable() == 0);
        if (hepyV == null && hepyAllSoldOut) {
            hepyStockLabel.setText("Agotado en todas las tallas de este color.");
            hepyStockLabel.getStyleClass().add("danger-text");
            hepyStepper.setMax(1);
        } else if (hepyV == null) {
            hepyStockLabel.setText("Elige una talla para ver el stock.");
            hepyStockLabel.getStyleClass().add("ink-muted");
            hepyStepper.setMax(1);
        } else if (hepyV.hepyAvailable() == 0) {
            hepyStockLabel.setText("Agotado en esta talla.");
            hepyStockLabel.getStyleClass().add("danger-text");
            hepyStepper.setMax(1);
        } else if (hepyV.hepyLowStock()) {
            hepyStockLabel.setText(hepyV.hepyAvailable() == 1 ? "¡Última unidad!" : "¡Últimas " + hepyV.hepyAvailable() + " unidades!");
            hepyStockLabel.getStyleClass().add("warning-text");
            hepyStepper.setMax(Math.min(hepyV.hepyAvailable(), 99));
        } else {
            hepyStockLabel.setText(hepyV.hepyAvailable() + " disponibles");
            hepyStockLabel.getStyleClass().add("success-text");
            hepyStepper.setMax(Math.min(hepyV.hepyAvailable(), 99));
        }
        for (var hepyNode : hepySizePane.getChildren()) {
            if (hepyNode instanceof HepyFilterChip hepyChip) {
                hepyChip.setSelected(hepyV != null && Objects.equals(hepyChip.getUserData(), hepyV.hepyId()));
            }
        }
    }

    static com.sportforce3d.model.HepyGarment hepyDesignerFocus(com.sportforce3d.model.HepyGarment hepyG) {
        return switch (hepyG) {
            case SHORTS, SOCKS, SHOES, ACCESSORY -> hepyG;
            default -> com.sportforce3d.model.HepyGarment.JERSEY;
        };
    }

    private static VBox hepySection(String hepyTitle, javafx.scene.Node hepyContent) {
        Label hepyLabel = new Label(hepyTitle);
        hepyLabel.getStyleClass().addAll("label-text", "ink-muted");
        return new VBox(8, hepyLabel, hepyContent);
    }
}
