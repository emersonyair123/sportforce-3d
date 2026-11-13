package com.sportforce3d.component;

import com.sportforce3d.model.ProductSummary;
import com.sportforce3d.util.Money;
import com.sportforce3d.util.Texts;
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

public class ProductCard extends VBox {

    public static final double WIDTH = 260;
    public static final double MEDIA_HEIGHT = WIDTH * 5 / 4;

    private final ProductSummary product;
    private final StackPane media;
    private final SfButton addButton;
    private final Label comingSoonNote;

    public ProductCard(ProductSummary product, Image image) {
        this.product = product;
        getStyleClass().add("sf-product-card");
        setPrefWidth(WIDTH);
        setMaxWidth(WIDTH);
        setMinWidth(WIDTH);

        media = new StackPane();
        media.getStyleClass().add("sf-product-media");
        media.setMinSize(WIDTH, MEDIA_HEIGHT);
        media.setPrefSize(WIDTH, MEDIA_HEIGHT);
        media.setMaxSize(WIDTH, MEDIA_HEIGHT);
        Rectangle clip = new Rectangle(WIDTH, MEDIA_HEIGHT + 20);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        media.setClip(clip);
        if (image != null) {
            media.getChildren().add(imageView(image));
        } else if (product.look() == null) {
            media.getChildren().add(art());
        }

        HBox badges = new HBox(4);
        badges.setPickOnBounds(false);
        badges.setPadding(new Insets(12));
        badges.setAlignment(Pos.TOP_LEFT);
        if (product.isNew()) {
            badges.getChildren().add(new SfBadge("Nuevo", SfBadge.Kind.NEW));
        }
        if (product.customizable()) {
            badges.getChildren().add(new SfBadge("3D", SfBadge.Kind.BRAND));
        }
        if (product.onSale()) {
            badges.getChildren().add(new SfBadge("Oferta", SfBadge.Kind.SALE));
        }
        media.getChildren().add(badges);
        StackPane.setAlignment(badges, Pos.TOP_LEFT);

        Label category = new Label(Texts.upper(product.categoryName()));
        category.getStyleClass().addAll("label-text", "ink-muted");

        Label name = new Label(product.name());
        name.getStyleClass().addAll("title-s", "ink");
        name.setWrapText(true);
        name.setMinHeight(Region.USE_PREF_SIZE);

        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.BASELINE_LEFT);
        Label price = new Label(Money.format(product.priceCents()));
        price.getStyleClass().addAll("price", "ink");
        priceRow.getChildren().add(price);
        if (product.onSale()) {
            Text was = new Text(Money.format(product.compareAtPriceCents()));
            was.getStyleClass().add("sf-price-was");
            was.setStrikethrough(true);
            Label discount = new Label("-" + product.discountPercent() + " %");
            discount.getStyleClass().addAll("label-text", "danger-text");
            priceRow.getChildren().addAll(was, discount);
        }

        VBox body = new VBox();
        body.getStyleClass().add("sf-product-body");
        body.getChildren().addAll(category, name, priceRow);

        if (product.inStock()) {
            addButton = new SfButton(product.singleVariantId() != null ? "Agregar al carrito" : "Elegir talla",
                    SfButton.Variant.PRIMARY, SfButton.Size.SM);
            addButton.withIcon(product.singleVariantId() != null ? Feather.SHOPPING_CART : Feather.CHEVRON_RIGHT);
            addButton.setMaxWidth(Double.MAX_VALUE);
            comingSoonNote = new Label("Compra disponible próximamente");
            comingSoonNote.getStyleClass().addAll("body-s", "ink-subtle");
            comingSoonNote.managedProperty().bind(comingSoonNote.visibleProperty());
            body.getChildren().addAll(addButton, comingSoonNote);
            setOnAddToCart(null);
        } else {
            addButton = null;
            comingSoonNote = null;
            body.getChildren().add(new SfBadge("Agotado", SfBadge.Kind.DANGER).withIcon(Feather.SLASH));
        }

        getChildren().addAll(media, body);
        Motion.liftOnHover(this);
        setAccessibleText(product.name() + ", " + Money.format(product.priceCents())
                + (product.inStock() ? "" : ", agotado"));
    }

    public ProductSummary getProduct() {
        return product;
    }

    public void setImage(Image image) {
        media.getChildren().removeIf(n -> n instanceof ImageView || n instanceof GarmentArt);
        Node node = image != null ? imageView(image) : art();
        media.getChildren().addFirst(node);
        if (image != null) {
            Motion.enter(node, Motion.MEDIUM, 0);
        }
    }

    public boolean hasImage() {
        return media.getChildren().stream().anyMatch(n -> n instanceof ImageView);
    }

    private GarmentArt art() {
        return new GarmentArt(product.garment(), product.colorHex(), 190);
    }

    private static ImageView imageView(Image image) {
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        Runnable fit = () -> {
            double ratio = image.getWidth() / Math.max(1, image.getHeight());
            if (ratio > WIDTH / MEDIA_HEIGHT) {
                view.setFitHeight(MEDIA_HEIGHT);
                view.setFitWidth(0);
            } else {
                view.setFitWidth(WIDTH);
                view.setFitHeight(0);
            }
        };
        fit.run();
        image.progressProperty().addListener((obs, old, p) -> {
            if (p.doubleValue() >= 1) {
                fit.run();
            }
        });
        return view;
    }

    public void setOnAddToCart(Consumer<ProductSummary> handler) {
        if (addButton == null) {
            return;
        }
        addButton.setDisable(handler == null);
        addButton.setOnAction(handler == null ? null : e -> handler.accept(product));
        comingSoonNote.setVisible(handler == null);
    }

    public void setOnOpen(Consumer<ProductSummary> handler) {
        setFocusTraversable(handler != null);
        setCursor(handler == null ? Cursor.DEFAULT : Cursor.HAND);
        setOnMouseClicked(handler == null ? null : e -> {
            if (e.getTarget() instanceof javafx.scene.Node n && isInside(n, addButton)) {
                return;
            }
            handler.accept(product);
        });
        setOnKeyPressed(handler == null ? null : e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handler.accept(product);
                e.consume();
            }
        });
    }

    private static boolean isInside(javafx.scene.Node node, javafx.scene.Node container) {
        for (javafx.scene.Node n = node; n != null; n = n.getParent()) {
            if (n == container) {
                return true;
            }
        }
        return false;
    }
}
