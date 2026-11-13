package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhChoiceCards;
import com.sportforce3d.component.PpqhConfirmDialog;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfLink;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.PpqhAddress;
import com.sportforce3d.model.HepyCartLine;
import com.sportforce3d.model.HepyCartSummary;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.service.PpqhOrderService;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.PpqhCheckoutViewModel;
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

public class PpqhCheckoutController {

    private final PpqhAppContext ppqhContext;
    private final PpqhCheckoutViewModel ppqhViewModel;

    @FXML private PpqhSfLink ppqhBackLink;
    @FXML private FlowPane ppqhLayout;

    private final PpqhChoiceCards<Long> ppqhAddressCards = new PpqhChoiceCards<>();
    private final PpqhChoiceCards<PpqhPaymentMethod> ppqhPaymentCards = new PpqhChoiceCards<>();
    private final PpqhAddressForm ppqhAddressForm = new PpqhAddressForm();
    private final PpqhSfButton ppqhAddAddress = new PpqhSfButton("Agregar dirección", PpqhSfButton.PpqhVariant.GHOST).ppqhWithIcon(Feather.PLUS);
    private final TextArea ppqhNotes = new TextArea();
    private final VBox ppqhSummaryPanel = new VBox(12);
    private final PpqhSfButton ppqhConfirm = new PpqhSfButton("Confirmar pedido", PpqhSfButton.PpqhVariant.ACCENT);

    public PpqhCheckoutController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
        this.ppqhViewModel = new PpqhCheckoutViewModel(ppqhContext.ppqhAddressService(), ppqhContext.ppqhCartService(), ppqhContext.ppqhOrderService());
    }

    @FXML
    private void initialize() {
        ppqhBackLink.setGraphic(PpqhIcons.ppqhOf(Feather.ARROW_LEFT, PpqhIcons.PPQH_SM));
        ppqhBackLink.setOnAction(ppqhE -> ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CART));

        ppqhAddressCards.selectedProperty().bindBidirectional(ppqhViewModel.addressIdProperty());
        ppqhViewModel.ppqhAddresses().addListener((ListChangeListener<PpqhAddress>) ppqhC -> ppqhRenderAddresses());
        ppqhAddressForm.setVisible(false);
        ppqhAddressForm.managedProperty().bind(ppqhAddressForm.visibleProperty());
        ppqhAddAddress.setOnAction(ppqhE -> ppqhOpenForm(null));
        ppqhAddressForm.ppqhCancel.setOnAction(ppqhE -> ppqhAddressForm.setVisible(false));
        ppqhAddressForm.ppqhSave.setOnAction(ppqhE -> ppqhViewModel.ppqhSaveAddress(ppqhAddressForm.ppqhValue(),
                () -> {
                    ppqhAddressForm.setVisible(false);
                    PpqhToast.ppqhSuccess(ppqhLayout, "Dirección guardada.");
                },
                ppqhAddressForm::ppqhShowErrors,
                ppqhMessage -> PpqhToast.ppqhError(ppqhLayout, ppqhMessage)));

        for (PpqhPaymentMethod ppqhM : PpqhPaymentMethod.values()) {
            ppqhPaymentCards.ppqhAdd(ppqhM, ppqhM.ppqhLabel(), ppqhPaymentDetail(ppqhM), null, ppqhM.ppqhAvailable());
        }
        ppqhPaymentCards.selectedProperty().bindBidirectional(ppqhViewModel.methodProperty());

        ppqhNotes.setPromptText("Ej.: entregar por la tarde, tocar el timbre 2.");
        ppqhNotes.setPrefRowCount(3);
        ppqhNotes.setWrapText(true);
        ppqhNotes.getStyleClass().add("sf-textarea");
        ppqhNotes.setAccessibleText("Notas para la tienda");
        ppqhNotes.textProperty().bindBidirectional(ppqhViewModel.notesProperty());
        Label ppqhNotesCount = new Label();
        ppqhNotesCount.getStyleClass().addAll("body-s", "ink-subtle");
        ppqhNotesCount.textProperty().bind(Bindings.createStringBinding(
                () -> (ppqhNotes.getText() == null ? 0 : ppqhNotes.getText().length()) + " / " + PpqhOrderService.PPQH_NOTES_MAX,
                ppqhNotes.textProperty()));

        VBox ppqhLeft = new VBox(32,
                ppqhSection("1. Dirección de envío", new VBox(12, ppqhAddressCards, ppqhAddAddress, ppqhAddressForm)),
                ppqhSection("2. Método de pago", new VBox(12, ppqhPaymentCards, ppqhPaymentNote())),
                ppqhSection("3. Notas para la tienda (opcional)", new VBox(6, ppqhNotes, ppqhNotesCount)));
        ppqhLeft.setPrefWidth(580);
        ppqhLeft.setMinWidth(500);

        ppqhSummaryPanel.getStyleClass().add("sf-panel");
        ppqhSummaryPanel.setPadding(new Insets(24));
        ppqhSummaryPanel.setPrefWidth(360);
        ppqhSummaryPanel.setMinWidth(320);
        ppqhSummaryPanel.setMaxHeight(Region.USE_PREF_SIZE);

        ppqhConfirm.setMaxWidth(Double.MAX_VALUE);
        ppqhConfirm.ppqhWithIcon(Feather.CHECK);
        ppqhConfirm.disableProperty().bind(ppqhViewModel.placingProperty()
                .or(ppqhViewModel.addressIdProperty().isNull())
                .or(Bindings.createBooleanBinding(() -> ppqhViewModel.summaryProperty().get() == null
                        || !ppqhViewModel.summaryProperty().get().hepyCanCheckout(), ppqhViewModel.summaryProperty())));
        ppqhConfirm.textProperty().bind(Bindings.when(ppqhViewModel.placingProperty())
                .then("Confirmando…").otherwise("Confirmar pedido"));
        ppqhConfirm.setOnAction(ppqhE -> ppqhPlaceOrder());

        ppqhViewModel.summaryProperty().addListener((ppqhObs, ppqhOld, ppqhS) -> {
            if (ppqhS != null && ppqhS.hepyIsEmpty()) {
                PpqhSfButton ppqhBrowse = new PpqhSfButton("Ver catálogo", PpqhSfButton.PpqhVariant.PRIMARY);
                ppqhBrowse.setOnAction(ppqhEv -> ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CATALOG));
                ppqhLayout.getChildren().setAll(new PpqhEmptyState(Feather.SHOPPING_CART, "Tu carrito está vacío",
                        "Agrega productos antes de finalizar la compra.", ppqhBrowse));
            } else if (ppqhS != null) {
                ppqhRenderSummary(ppqhS);
            }
        });
        ppqhViewModel.errorProperty().addListener((ppqhObs, ppqhOld, ppqhError) -> {
            if (ppqhError != null && !ppqhError.isEmpty()) {
                ppqhLayout.getChildren().setAll(new PpqhEmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar el pago", ppqhError));
            }
        });

        ppqhLayout.getChildren().setAll(ppqhLeft, ppqhSummaryPanel);
        ppqhViewModel.ppqhLoad();
    }

    private void ppqhOpenForm(PpqhAddress ppqhAddress) {
        ppqhAddressForm.ppqhEdit(ppqhAddress);
        if (ppqhAddress == null) {
            ppqhContext.ppqhSession().ppqhCurrent().ifPresent(ppqhU -> ppqhAddressForm.ppqhPrefill(ppqhU.ppqhFullName(), ppqhU.ppqhPhone()));
        }
        ppqhAddressForm.setVisible(true);
        ppqhAddressForm.requestFocus();
    }

    private void ppqhRenderAddresses() {
        ppqhAddressCards.ppqhClear();
        for (PpqhAddress ppqhA : ppqhViewModel.ppqhAddresses()) {
            PpqhSfButton ppqhEdit = PpqhSfButton.ppqhIcon(Feather.EDIT_2, "Editar " + ppqhA.ppqhLabel());
            ppqhEdit.setOnAction(ppqhE -> ppqhOpenForm(ppqhA));
            PpqhSfButton ppqhDelete = PpqhSfButton.ppqhIcon(Feather.TRASH_2, "Eliminar " + ppqhA.ppqhLabel());
            ppqhDelete.setOnAction(ppqhE -> PpqhConfirmDialog.ppqhShow(ppqhLayout, "¿Eliminar la dirección “" + ppqhA.ppqhLabel() + "”?",
                    ppqhA.ppqhOneLine(), "Eliminar", true,
                    () -> ppqhViewModel.ppqhDeleteAddress(ppqhA.ppqhId(), ppqhMessage -> PpqhToast.ppqhError(ppqhLayout, ppqhMessage))));
            HBox ppqhActions = new HBox(4, ppqhEdit, ppqhDelete);
            ppqhActions.setAlignment(Pos.CENTER_RIGHT);
            String ppqhTitle = ppqhA.ppqhLabel() + (ppqhA.ppqhIsDefault() ? " · predeterminada" : "");
            ppqhAddressCards.ppqhAdd(ppqhA.ppqhId(), ppqhTitle, ppqhA.ppqhRecipient() + " · " + ppqhA.ppqhPhone() + "\n" + ppqhA.ppqhOneLine()
                    + (ppqhA.ppqhReference() == null ? "" : "\nRef.: " + ppqhA.ppqhReference()), ppqhActions, true);
        }
        ppqhAddressCards.setSelected(ppqhViewModel.addressIdProperty().get());
        if (ppqhViewModel.ppqhAddresses().isEmpty()) {
            ppqhOpenForm(null);
        }
    }

    private void ppqhRenderSummary(HepyCartSummary ppqhS) {
        Label ppqhTitle = new Label("Tu pedido");
        ppqhTitle.getStyleClass().addAll("title-s", "ink");
        VBox ppqhLines = new VBox(8);
        for (HepyCartLine ppqhL : ppqhS.hepyLines()) {
            Label ppqhName = new Label(ppqhL.hepyQuantity() + " × " + ppqhL.hepyProductName());
            ppqhName.getStyleClass().addAll("body-s", "ink");
            ppqhName.setWrapText(true);
            Label ppqhMeta = new Label("Talla " + ppqhL.hepySizeName() + (ppqhL.hepyHasDesign()
                    ? " · Diseño: " + ppqhL.hepyDesignName() + (ppqhL.hepySurchargeCents() == 0 ? "" : " · " + HepyMoney.hepySurcharge(ppqhL.hepySurchargeCents()) + " c/u")
                    : " · " + ppqhL.hepyColorName()));
            ppqhMeta.getStyleClass().addAll("body-s", "ink-subtle");
            Label ppqhPrice = new Label(HepyMoney.hepyFormat(ppqhL.hepyLineTotalCents()));
            ppqhPrice.getStyleClass().addAll("body-s", "ink");
            ppqhPrice.setMinWidth(Region.USE_PREF_SIZE);
            VBox ppqhText = new VBox(0, ppqhName, ppqhMeta);
            HBox.setHgrow(ppqhText, Priority.ALWAYS);
            ppqhLines.getChildren().add(new HBox(8, ppqhText, ppqhPrice));
        }
        Region ppqhDivider = new Region();
        ppqhDivider.getStyleClass().add("sf-divider");
        Region ppqhDivider2 = new Region();
        ppqhDivider2.getStyleClass().add("sf-divider");
        Label ppqhTotalLabel = new Label("Total");
        ppqhTotalLabel.getStyleClass().addAll("title-s", "ink");
        Label ppqhTotal = new Label(HepyMoney.hepyFormat(ppqhS.hepyTotalCents()));
        ppqhTotal.getStyleClass().addAll("price", "ink");
        Region ppqhSpacer = new Region();
        HBox.setHgrow(ppqhSpacer, Priority.ALWAYS);
        HBox ppqhTotalRow = new HBox(ppqhTotalLabel, ppqhSpacer, ppqhTotal);
        ppqhTotalRow.setAlignment(Pos.BASELINE_LEFT);

        ppqhSummaryPanel.getChildren().setAll(ppqhTitle, ppqhLines, ppqhDivider,
                HepyCartController.hepyRow("Subtotal", HepyMoney.hepyFormat(ppqhS.hepySubtotalCents())),
                HepyCartController.hepyRow("Recargos por materiales", HepyMoney.hepyFormat(ppqhS.hepySurchargeCents())),
                HepyCartController.hepyRow("Envío", ppqhS.hepyShippingCents() == 0 ? "Gratis" : HepyMoney.hepyFormat(ppqhS.hepyShippingCents())),
                ppqhDivider2, ppqhTotalRow, ppqhConfirm);
        if (!ppqhS.hepyCanCheckout()) {
            Label ppqhWhy = new Label("Algún producto cambió de stock. Vuelve al carrito para revisarlo.");
            ppqhWhy.getStyleClass().addAll("body-s", "warning-text");
            ppqhWhy.setWrapText(true);
            ppqhSummaryPanel.getChildren().add(ppqhWhy);
        }
    }

    private void ppqhPlaceOrder() {
        ppqhViewModel.ppqhPlaceOrder(ppqhOrder -> {
            ppqhContext.setCartCount(0);
            ppqhContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ORDER, ppqhOrder.ppqhId());
            PpqhToast.ppqhSuccess(ppqhContext.ppqhNavigator().ppqhStage().getScene().getRoot(),
                    "Pedido " + ppqhOrder.ppqhOrderNumber() + " creado. Queda pendiente de pago.");
        }, ppqhMessage -> PpqhToast.ppqhError(ppqhLayout, ppqhMessage));
    }

    private static String ppqhPaymentDetail(PpqhPaymentMethod ppqhM) {
        return switch (ppqhM) {
            case YAPE, PLIN -> "Pagas desde tu app y registras el código de operación. Un administrador lo verifica.";
            case TRANSFERENCIA -> "Transfieres a la cuenta de la tienda y registras el número de operación.";
            case TARJETA -> "Requiere conexión con la pasarela de pagos.";
        };
    }

    private static Node ppqhPaymentNote() {
        Label ppqhNote = new Label("Tu pedido quedará como “Pendiente de pago” hasta que un administrador confirme tu pago. "
                + "Nunca te pediremos datos de tarjeta en esta aplicación.");
        ppqhNote.getStyleClass().addAll("body-s", "ink-muted");
        ppqhNote.setWrapText(true);
        return ppqhNote;
    }

    private static VBox ppqhSection(String ppqhTitle, Node ppqhContent) {
        Label ppqhLabel = new Label(ppqhTitle);
        ppqhLabel.getStyleClass().addAll("title-s", "ink");
        return new VBox(12, ppqhLabel, ppqhContent);
    }
}
