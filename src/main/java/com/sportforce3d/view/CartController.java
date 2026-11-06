package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.GarmentArt;
import com.sportforce3d.component.QuantityStepper;
import com.sportforce3d.component.SfBadge;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.CartLine;
import com.sportforce3d.model.CartSummary;
import com.sportforce3d.util.Money;
import com.sportforce3d.viewmodel.CartViewModel;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;

public class CartController {

    private final AppContext context;
    private final CartViewModel viewModel;

    @FXML private FlowPane layout;

    public CartController(AppContext context) {
        this.context = context;
        this.viewModel = new CartViewModel(context.cartService());
    }

    @FXML
    private void initialize() {
        viewModel.summaryProperty().addListener((obs, old, summary) -> {
            context.setCartCount(summary.itemCount());
            render(summary);
        });
        viewModel.errorProperty().addListener((obs, old, error) -> {
            if (error != null && !error.isEmpty()) {
                layout.getChildren().setAll(new EmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar tu carrito", error));
            }
        });
        viewModel.load();
    }

    private void render(CartSummary summary) {
        if (summary.isEmpty()) {
            SfButton browse = new SfButton("Ver catálogo", SfButton.Variant.PRIMARY).withIcon(Feather.GRID);
            browse.setOnAction(e -> context.router().go(Routes.CATALOG));
            EmptyState empty = new EmptyState(Feather.SHOPPING_CART, "Tu carrito está vacío",
                    "Agrega productos del catálogo y aquí verás el total con envío.", browse);
            empty.setPrefWidth(720);
            layout.getChildren().setAll(empty);
            return;
        }
        VBox lines = new VBox(12);
        lines.setPrefWidth(600);
        lines.setMinWidth(520);
        for (CartLine line : summary.lines()) {
            lines.getChildren().add(lineRow(line));
        }
        layout.getChildren().setAll(lines, summaryPanel(summary));
    }

    private Node lineRow(CartLine line) {
        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("sf-thumb");
        thumb.setMinSize(88, 110);
        thumb.setMaxSize(88, 110);
        // Las líneas personalizadas muestran la captura frontal del diseño, recortada al uniforme.
        ImageView designView = line.hasDesign()
                ? ProductImages.designCapture(context.storage(), line.designThumbnail(), 88, 110) : null;
        Image image = line.hasDesign() ? null : ProductImages.load(context.storage(), line.imagePath(), 88, 110);
        if (designView != null) {
            thumb.getChildren().add(designView);
        } else if (image != null) {
            ImageView view = new ImageView(image);
            view.setFitWidth(88);
            view.setFitHeight(110);
            view.setPreserveRatio(true);
            thumb.getChildren().add(view);
        } else {
            thumb.getChildren().add(new GarmentArt(line.garment(), line.colorHex(), 80));
        }
        thumb.setOnMouseClicked(e -> {
            if (line.hasDesign()) {
                context.router().go(Routes.DESIGNER, Routes.DesignerParams.open(line.designId()));
            } else {
                context.router().go(Routes.PRODUCT, line.productId());
            }
        });
        thumb.setCursor(javafx.scene.Cursor.HAND);

        Label name = new Label(line.productName());
        name.getStyleClass().addAll("title-s", "ink");
        name.setWrapText(true);
        Label meta = new Label("Talla " + line.sizeName() + (line.hasDesign() ? "" : " · " + line.colorName()));
        meta.getStyleClass().addAll("body-s", "ink-muted");
        Label sku = new Label(line.sku());
        sku.getStyleClass().addAll("code", "ink-subtle");
        Label unit = new Label(Money.format(line.unitPriceCents()) + " c/u");
        unit.getStyleClass().addAll("body-s", "ink-muted");
        VBox text = new VBox(4, name, meta, sku, unit);
        if (line.hasDesign()) {
            Label design = new Label("Diseño: " + (line.designName() == null ? "eliminado" : line.designName()));
            design.getStyleClass().addAll("body-s", "brand-text");
            Label surcharge = new Label("Recargo por materiales: " + Money.surcharge(line.surchargeCents())
                    + (line.surchargeCents() == 0 ? "" : " c/u"));
            surcharge.getStyleClass().addAll("body-s", "ink-muted");
            text.getChildren().add(1, new VBox(2, design, surcharge));
            if (line.designName() != null && line.purchasable()) {
                com.sportforce3d.component.SfLink edit = new com.sportforce3d.component.SfLink("Editar diseño");
                edit.setOnAction(e -> context.router().go(Routes.DESIGNER, Routes.DesignerParams.open(line.designId())));
                text.getChildren().add(edit);
            }
        }
        if (!line.purchasable()) {
            text.getChildren().add(new SfBadge(line.hasDesign() ? "El diseño o el producto ya no está disponible"
                    : "Ya no está disponible", SfBadge.Kind.DANGER).withIcon(Feather.SLASH));
        } else if (!line.enoughStock()) {
            text.getChildren().add(new SfBadge(line.available() == 0 ? "Agotado"
                    : "Solo quedan " + line.available(), SfBadge.Kind.WARNING).withIcon(Feather.ALERT_TRIANGLE));
        }
        HBox.setHgrow(text, Priority.ALWAYS);
        text.setMinWidth(0);

        QuantityStepper stepper = new QuantityStepper();
        stepper.setMax(Math.max(1, Math.min(99, Math.max(line.available(), line.quantity()))));
        stepper.setValue(line.quantity());
        stepper.setDisable(!line.purchasable());
        // Espera un instante antes de guardar para no escribir en cada clic.
        PauseTransition debounce = new PauseTransition(Duration.millis(400));
        debounce.setOnFinished(e -> viewModel.updateQuantity(line.itemId(), stepper.getValue(),
                message -> Toast.error(layout, message)));
        stepper.valueProperty().addListener((obs, old, v) -> debounce.playFromStart());

        Label total = new Label(Money.format(line.lineTotalCents()));
        total.getStyleClass().addAll("price", "ink");
        total.setMinWidth(Region.USE_PREF_SIZE);

        SfButton remove = SfButton.icon(Feather.TRASH_2, "Quitar " + line.productName());
        remove.setOnAction(e -> viewModel.remove(line.itemId(),
                () -> Toast.info(layout, line.productName() + " se quitó del carrito."),
                message -> Toast.error(layout, message)));

        VBox right = new VBox(12, total, new HBox(8, stepper, remove));
        right.setAlignment(Pos.TOP_RIGHT);
        ((HBox) right.getChildren().get(1)).setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(16, thumb, text, right);
        row.getStyleClass().add("sf-card");
        row.setPadding(new Insets(16));
        return row;
    }

    private Node summaryPanel(CartSummary s) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("sf-panel");
        panel.setPadding(new Insets(24));
        panel.setPrefWidth(340);
        panel.setMinWidth(320);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        Label title = new Label("Resumen");
        title.getStyleClass().addAll("title-s", "ink");
        panel.getChildren().addAll(title,
                row("Productos (" + s.itemCount() + ")", Money.format(s.subtotalCents())),
                row("Recargos por materiales", Money.format(s.surchargeCents())),
                row("Envío", s.shippingCents() == 0 ? "Gratis" : Money.format(s.shippingCents())));
        if (s.missingForFreeShipping() > 0) {
            Label hint = new Label("Agrega " + Money.format(s.missingForFreeShipping()) + " más y el envío es gratis.");
            hint.getStyleClass().addAll("body-s", "brand-text");
            hint.setWrapText(true);
            panel.getChildren().add(hint);
        }
        Region line = new Region();
        line.getStyleClass().add("sf-divider");
        Label totalLabel = new Label("Total");
        totalLabel.getStyleClass().addAll("title-s", "ink");
        Label total = new Label(Money.format(s.totalCents()));
        total.getStyleClass().addAll("price", "ink");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox totalRow = new HBox(totalLabel, spacer, total);
        totalRow.setAlignment(Pos.BASELINE_LEFT);

        SfButton checkout = new SfButton("Continuar al pago", SfButton.Variant.ACCENT).withIcon(Feather.ARROW_RIGHT);
        checkout.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        checkout.setMaxWidth(Double.MAX_VALUE);
        checkout.setDisable(!s.canCheckout());
        checkout.disableProperty().bind(viewModel.busyProperty().or(
                javafx.beans.binding.Bindings.createBooleanBinding(() -> !s.canCheckout())));
        checkout.setOnAction(e -> context.router().go(Routes.CHECKOUT));

        SfButton keep = new SfButton("Seguir comprando", SfButton.Variant.GHOST);
        keep.setMaxWidth(Double.MAX_VALUE);
        keep.setOnAction(e -> context.router().go(Routes.CATALOG));

        panel.getChildren().addAll(line, totalRow, checkout, keep);
        if (!s.canCheckout()) {
            Label why = new Label("Revisa los productos marcados antes de continuar.");
            why.getStyleClass().addAll("body-s", "warning-text");
            why.setWrapText(true);
            panel.getChildren().add(why);
        }
        return panel;
    }

    static HBox row(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().addAll("body-s", "ink-muted");
        Label v = new Label(value);
        v.getStyleClass().addAll("body-s", "ink");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new HBox(l, spacer, v);
    }
}
