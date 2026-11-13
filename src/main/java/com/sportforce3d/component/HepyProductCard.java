package com.sportforce3d.component;

import com.sportforce3d.model.HepyProductSummary;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.util.HepyTexts;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.kordamp.ikonli.feather.Feather;

import java.util.function.Consumer;

public class HepyProductCard extends VBox {

    public static final double HEPY_WIDTH = 260;
    public static final double HEPY_MEDIA_HEIGHT = HEPY_WIDTH * 5 / 4;

    private final HepyProductSummary hepyProduct;
    private final StackPane hepyMedia;
    private final PpqhSfButton hepyAddButton;
    private final Label hepyComingSoonNote;

    public HepyProductCard(HepyProductSummary hepyProduct, Image hepyImage) {
        this.hepyProduct = hepyProduct;
        getStyleClass().add("sf-product-card");
        setPrefWidth(HEPY_WIDTH);
        setMaxWidth(HEPY_WIDTH);
        setMinWidth(HEPY_WIDTH);

        hepyMedia = new StackPane();
        hepyMedia.getStyleClass().add("sf-product-media");
        hepyMedia.setMinSize(HEPY_WIDTH, HEPY_MEDIA_HEIGHT);
        hepyMedia.setPrefSize(HEPY_WIDTH, HEPY_MEDIA_HEIGHT);
        hepyMedia.setMaxSize(HEPY_WIDTH, HEPY_MEDIA_HEIGHT);
        Rectangle hepyClip = new Rectangle(HEPY_WIDTH, HEPY_MEDIA_HEIGHT + 20);
        hepyClip.setArcWidth(30);
        hepyClip.setArcHeight(30);
        hepyMedia.setClip(hepyClip);
        if (hepyImage != null) {
            hepyMedia.getChildren().add(hepyImageView(hepyImage));
        } else if (hepyProduct.hepyLook() == null) {
            hepyMedia.getChildren().add(hepyArt());
        }

        HBox hepyBadges = new HBox(4);
        hepyBadges.setPickOnBounds(false);
        hepyBadges.setPadding(new Insets(12));
        hepyBadges.setAlignment(Pos.TOP_LEFT);
        if (hepyProduct.hepyIsNew()) {
            hepyBadges.getChildren().add(new PpqhSfBadge("Nuevo", PpqhSfBadge.PpqhKind.NEW));
        }
        if (hepyProduct.hepyCustomizable()) {
            hepyBadges.getChildren().add(new PpqhSfBadge("3D", PpqhSfBadge.PpqhKind.BRAND));
        }
        if (hepyProduct.hepyOnSale()) {
            hepyBadges.getChildren().add(new PpqhSfBadge("Oferta", PpqhSfBadge.PpqhKind.SALE));
        }
        hepyMedia.getChildren().add(hepyBadges);
        StackPane.setAlignment(hepyBadges, Pos.TOP_LEFT);

        Label hepyCategory = new Label(HepyTexts.hepyUpper(hepyProduct.hepyCategoryName()));
        hepyCategory.getStyleClass().addAll("label-text", "ink-muted");

        Label hepyName = new Label(hepyProduct.hepyName());
        hepyName.getStyleClass().addAll("title-s", "ink");
        hepyName.setWrapText(true);
        hepyName.setMinHeight(Region.USE_PREF_SIZE);

        HBox hepyPriceRow = new HBox(8);
        hepyPriceRow.setAlignment(Pos.BASELINE_LEFT);
        Label hepyPrice = new Label(HepyMoney.hepyFormat(hepyProduct.hepyPriceCents()));
        hepyPrice.getStyleClass().addAll("price", "ink");
        hepyPriceRow.getChildren().add(hepyPrice);
        if (hepyProduct.hepyOnSale()) {
            Text hepyWas = new Text(HepyMoney.hepyFormat(hepyProduct.hepyCompareAtPriceCents()));
            hepyWas.getStyleClass().add("sf-price-was");
            hepyWas.setStrikethrough(true);
            Label hepyDiscount = new Label("-" + hepyProduct.hepyDiscountPercent() + " %");
            hepyDiscount.getStyleClass().addAll("label-text", "danger-text");
            hepyPriceRow.getChildren().addAll(hepyWas, hepyDiscount);
        }

        VBox hepyBody = new VBox();
        hepyBody.getStyleClass().add("sf-product-body");
        hepyBody.getChildren().addAll(hepyCategory, hepyName, hepyPriceRow);

        if (hepyProduct.hepyInStock()) {
            hepyAddButton = new PpqhSfButton(hepyProduct.hepySingleVariantId() != null ? "Agregar al carrito" : "Elegir talla",
                    PpqhSfButton.PpqhVariant.PRIMARY, PpqhSfButton.PpqhSize.SM);
            hepyAddButton.ppqhWithIcon(hepyProduct.hepySingleVariantId() != null ? Feather.SHOPPING_CART : Feather.CHEVRON_RIGHT);
            hepyAddButton.setMaxWidth(Double.MAX_VALUE);
            hepyComingSoonNote = new Label("Compra disponible próximamente");
            hepyComingSoonNote.getStyleClass().addAll("body-s", "ink-subtle");
            hepyComingSoonNote.managedProperty().bind(hepyComingSoonNote.visibleProperty());
            hepyBody.getChildren().addAll(hepyAddButton, hepyComingSoonNote);
            setOnAddToCart(null);
        } else {
            hepyAddButton = null;
            hepyComingSoonNote = null;
            hepyBody.getChildren().add(new PpqhSfBadge("Agotado", PpqhSfBadge.PpqhKind.DANGER).ppqhWithIcon(Feather.SLASH));
        }

        getChildren().addAll(hepyMedia, hepyBody);
        PpqhMotion.ppqhLiftOnHover(this);
        setAccessibleText(hepyProduct.hepyName() + ", " + HepyMoney.hepyFormat(hepyProduct.hepyPriceCents())
                + (hepyProduct.hepyInStock() ? "" : ", agotado"));
    }

    public HepyProductSummary getProduct() {
        return hepyProduct;
    }

    public void setImage(Image hepyImage) {
        hepyMedia.getChildren().removeIf(hepyN -> hepyN instanceof ImageView || hepyN instanceof HepyGarmentArt);
        Node hepyNode = hepyImage != null ? hepyImageView(hepyImage) : hepyArt();
        hepyMedia.getChildren().addFirst(hepyNode);
        if (hepyImage != null) {
            PpqhMotion.ppqhEnter(hepyNode, PpqhMotion.PPQH_MEDIUM, 0);
        }
    }

    public boolean hepyHasImage() {
        return hepyMedia.getChildren().stream().anyMatch(hepyN -> hepyN instanceof ImageView);
    }

    private HepyGarmentArt hepyArt() {
        return new HepyGarmentArt(hepyProduct.hepyGarment(), hepyProduct.hepyColorHex(), 190);
    }

    private static ImageView hepyImageView(Image hepyImage) {
        ImageView hepyView = new ImageView(hepyImage);
        hepyView.setPreserveRatio(true);
        hepyView.setSmooth(true);
        Runnable hepyFit = () -> {
            double hepyRatio = hepyImage.getWidth() / Math.max(1, hepyImage.getHeight());
            if (hepyRatio > HEPY_WIDTH / HEPY_MEDIA_HEIGHT) {
                hepyView.setFitHeight(HEPY_MEDIA_HEIGHT);
                hepyView.setFitWidth(0);
            } else {
                hepyView.setFitWidth(HEPY_WIDTH);
                hepyView.setFitHeight(0);
            }
        };
        hepyFit.run();
        hepyImage.progressProperty().addListener((hepyObs, hepyOld, hepyP) -> {
            if (hepyP.doubleValue() >= 1) {
                hepyFit.run();
            }
        });
        return hepyView;
    }

    public void setOnAddToCart(Consumer<HepyProductSummary> hepyHandler) {
        if (hepyAddButton == null) {
            return;
        }
        hepyAddButton.setDisable(hepyHandler == null);
        hepyAddButton.setOnAction(hepyHandler == null ? null : hepyE -> hepyHandler.accept(hepyProduct));
        hepyComingSoonNote.setVisible(hepyHandler == null);
    }

    public void setOnOpen(Consumer<HepyProductSummary> hepyHandler) {
        setFocusTraversable(hepyHandler != null);
        setCursor(hepyHandler == null ? Cursor.DEFAULT : Cursor.HAND);
        setOnMouseClicked(hepyHandler == null ? null : hepyE -> {
            if (hepyE.getTarget() instanceof javafx.scene.Node hepyN && hepyIsInside(hepyN, hepyAddButton)) {
                return;
            }
            hepyHandler.accept(hepyProduct);
        });
        setOnKeyPressed(hepyHandler == null ? null : hepyE -> {
            if (hepyE.getCode() == KeyCode.ENTER) {
                hepyHandler.accept(hepyProduct);
                hepyE.consume();
            }
        });
    }

    private static boolean hepyIsInside(javafx.scene.Node hepyNode, javafx.scene.Node hepyContainer) {
        for (javafx.scene.Node hepyN = hepyNode; hepyN != null; hepyN = hepyN.getParent()) {
            if (hepyN == hepyContainer) {
                return true;
            }
        }
        return false;
    }
}
