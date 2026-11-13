package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.ChoiceCards;
import com.sportforce3d.component.ConfirmDialog;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfLink;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.Address;
import com.sportforce3d.model.CartLine;
import com.sportforce3d.model.CartSummary;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.service.OrderService;
import com.sportforce3d.util.Money;
import com.sportforce3d.viewmodel.CheckoutViewModel;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

public class CheckoutController {

    private final AppContext context;
    private final CheckoutViewModel viewModel;

    @FXML private SfLink backLink;
    @FXML private FlowPane layout;

    private final ChoiceCards<Long> addressCards = new ChoiceCards<>();
    private final ChoiceCards<PaymentMethod> paymentCards = new ChoiceCards<>();
    private final AddressForm addressForm = new AddressForm();
    private final SfButton addAddress = new SfButton("Agregar dirección", SfButton.Variant.GHOST).withIcon(Feather.PLUS);
    private final TextArea notes = new TextArea();
    private final VBox summaryPanel = new VBox(12);
    private final SfButton confirm = new SfButton("Confirmar pedido", SfButton.Variant.ACCENT);

    public CheckoutController(AppContext context) {
        this.context = context;
        this.viewModel = new CheckoutViewModel(context.addressService(), context.cartService(), context.orderService());
    }

    @FXML
    private void initialize() {
        backLink.setGraphic(Icons.of(Feather.ARROW_LEFT, Icons.SM));
        backLink.setOnAction(e -> context.router().go(Routes.CART));

        addressCards.selectedProperty().bindBidirectional(viewModel.addressIdProperty());
        viewModel.addresses().addListener((ListChangeListener<Address>) c -> renderAddresses());
        addressForm.setVisible(false);
        addressForm.managedProperty().bind(addressForm.visibleProperty());
        addAddress.setOnAction(e -> openForm(null));
        addressForm.cancel.setOnAction(e -> addressForm.setVisible(false));
        addressForm.save.setOnAction(e -> viewModel.saveAddress(addressForm.value(),
                () -> {
                    addressForm.setVisible(false);
                    Toast.success(layout, "Dirección guardada.");
                },
                addressForm::showErrors,
                message -> Toast.error(layout, message)));

        for (PaymentMethod m : PaymentMethod.values()) {
            paymentCards.add(m, m.label(), paymentDetail(m), null, m.available());
        }
        paymentCards.selectedProperty().bindBidirectional(viewModel.methodProperty());

        notes.setPromptText("Ej.: entregar por la tarde, tocar el timbre 2.");
        notes.setPrefRowCount(3);
        notes.setWrapText(true);
        notes.getStyleClass().add("sf-textarea");
        notes.setAccessibleText("Notas para la tienda");
        notes.textProperty().bindBidirectional(viewModel.notesProperty());
        Label notesCount = new Label();
        notesCount.getStyleClass().addAll("body-s", "ink-subtle");
        notesCount.textProperty().bind(Bindings.createStringBinding(
                () -> (notes.getText() == null ? 0 : notes.getText().length()) + " / " + OrderService.NOTES_MAX,
                notes.textProperty()));

        VBox left = new VBox(32,
                section("1. Dirección de envío", new VBox(12, addressCards, addAddress, addressForm)),
                section("2. Método de pago", new VBox(12, paymentCards, paymentNote())),
                section("3. Notas para la tienda (opcional)", new VBox(6, notes, notesCount)));
        left.setPrefWidth(580);
        left.setMinWidth(500);

        summaryPanel.getStyleClass().add("sf-panel");
        summaryPanel.setPadding(new Insets(24));
        summaryPanel.setPrefWidth(360);
        summaryPanel.setMinWidth(320);
        summaryPanel.setMaxHeight(Region.USE_PREF_SIZE);

        confirm.setMaxWidth(Double.MAX_VALUE);
        confirm.withIcon(Feather.CHECK);
        confirm.disableProperty().bind(viewModel.placingProperty()
                .or(viewModel.addressIdProperty().isNull())
                .or(Bindings.createBooleanBinding(() -> viewModel.summaryProperty().get() == null
                        || !viewModel.summaryProperty().get().canCheckout(), viewModel.summaryProperty())));
        confirm.textProperty().bind(Bindings.when(viewModel.placingProperty())
                .then("Confirmando…").otherwise("Confirmar pedido"));
        confirm.setOnAction(e -> placeOrder());

        viewModel.summaryProperty().addListener((obs, old, s) -> {
            if (s != null && s.isEmpty()) {
                SfButton browse = new SfButton("Ver catálogo", SfButton.Variant.PRIMARY);
                browse.setOnAction(ev -> context.router().go(Routes.CATALOG));
                layout.getChildren().setAll(new EmptyState(Feather.SHOPPING_CART, "Tu carrito está vacío",
                        "Agrega productos antes de finalizar la compra.", browse));
            } else if (s != null) {
                renderSummary(s);
            }
        });
        viewModel.errorProperty().addListener((obs, old, error) -> {
            if (error != null && !error.isEmpty()) {
                layout.getChildren().setAll(new EmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar el pago", error));
            }
        });

        layout.getChildren().setAll(left, summaryPanel);
        viewModel.load();
    }

    private void openForm(Address address) {
        addressForm.edit(address);
        if (address == null) {
            context.session().current().ifPresent(u -> addressForm.prefill(u.fullName(), u.phone()));
        }
        addressForm.setVisible(true);
        addressForm.requestFocus();
    }

    private void renderAddresses() {
        addressCards.clear();
        for (Address a : viewModel.addresses()) {
            SfButton edit = SfButton.icon(Feather.EDIT_2, "Editar " + a.label());
            edit.setOnAction(e -> openForm(a));
            SfButton delete = SfButton.icon(Feather.TRASH_2, "Eliminar " + a.label());
            delete.setOnAction(e -> ConfirmDialog.show(layout, "¿Eliminar la dirección “" + a.label() + "”?",
                    a.oneLine(), "Eliminar", true,
                    () -> viewModel.deleteAddress(a.id(), message -> Toast.error(layout, message))));
            HBox actions = new HBox(4, edit, delete);
            actions.setAlignment(Pos.CENTER_RIGHT);
            String title = a.label() + (a.isDefault() ? " · predeterminada" : "");
            addressCards.add(a.id(), title, a.recipient() + " · " + a.phone() + "\n" + a.oneLine()
                    + (a.reference() == null ? "" : "\nRef.: " + a.reference()), actions, true);
        }
        addressCards.setSelected(viewModel.addressIdProperty().get());
        if (viewModel.addresses().isEmpty()) {
            openForm(null);
        }
    }

    private void renderSummary(CartSummary s) {
        Label title = new Label("Tu pedido");
        title.getStyleClass().addAll("title-s", "ink");
        VBox lines = new VBox(8);
        for (CartLine l : s.lines()) {
            Label name = new Label(l.quantity() + " × " + l.productName());
            name.getStyleClass().addAll("body-s", "ink");
            name.setWrapText(true);
            Label meta = new Label("Talla " + l.sizeName() + (l.hasDesign()
                    ? " · Diseño: " + l.designName() + (l.surchargeCents() == 0 ? "" : " · " + Money.surcharge(l.surchargeCents()) + " c/u")
                    : " · " + l.colorName()));
            meta.getStyleClass().addAll("body-s", "ink-subtle");
            Label price = new Label(Money.format(l.lineTotalCents()));
            price.getStyleClass().addAll("body-s", "ink");
            price.setMinWidth(Region.USE_PREF_SIZE);
            VBox text = new VBox(0, name, meta);
            HBox.setHgrow(text, Priority.ALWAYS);
            lines.getChildren().add(new HBox(8, text, price));
        }
        Region divider = new Region();
        divider.getStyleClass().add("sf-divider");
        Region divider2 = new Region();
        divider2.getStyleClass().add("sf-divider");
        Label totalLabel = new Label("Total");
        totalLabel.getStyleClass().addAll("title-s", "ink");
        Label total = new Label(Money.format(s.totalCents()));
        total.getStyleClass().addAll("price", "ink");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox totalRow = new HBox(totalLabel, spacer, total);
        totalRow.setAlignment(Pos.BASELINE_LEFT);

        summaryPanel.getChildren().setAll(title, lines, divider,
                CartController.row("Subtotal", Money.format(s.subtotalCents())),
                CartController.row("Recargos por materiales", Money.format(s.surchargeCents())),
                CartController.row("Envío", s.shippingCents() == 0 ? "Gratis" : Money.format(s.shippingCents())),
                divider2, totalRow, confirm);
        if (!s.canCheckout()) {
            Label why = new Label("Algún producto cambió de stock. Vuelve al carrito para revisarlo.");
            why.getStyleClass().addAll("body-s", "warning-text");
            why.setWrapText(true);
            summaryPanel.getChildren().add(why);
        }
    }

    private void placeOrder() {
        viewModel.placeOrder(order -> {
            context.setCartCount(0);
            context.router().go(Routes.ORDER, order.id());
            Toast.success(context.navigator().stage().getScene().getRoot(),
                    "Pedido " + order.orderNumber() + " creado. Queda pendiente de pago.");
        }, message -> Toast.error(layout, message));
    }

    private static String paymentDetail(PaymentMethod m) {
        return switch (m) {
            case YAPE, PLIN -> "Pagas desde tu app y registras el código de operación. Un administrador lo verifica.";
            case TRANSFERENCIA -> "Transfieres a la cuenta de la tienda y registras el número de operación.";
            case TARJETA -> "Requiere conexión con la pasarela de pagos.";
        };
    }

    private static Node paymentNote() {
        Label note = new Label("Tu pedido quedará como “Pendiente de pago” hasta que un administrador confirme tu pago. "
                + "Nunca te pediremos datos de tarjeta en esta aplicación.");
        note.getStyleClass().addAll("body-s", "ink-muted");
        note.setWrapText(true);
        return note;
    }

    private static VBox section(String title, Node content) {
        Label label = new Label(title);
        label.getStyleClass().addAll("title-s", "ink");
        return new VBox(12, label, content);
    }
}
