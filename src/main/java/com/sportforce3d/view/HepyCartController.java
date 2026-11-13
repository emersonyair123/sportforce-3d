package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.HepyGarmentArt;
import com.sportforce3d.component.HepyQuantityStepper;
import com.sportforce3d.component.PpqhSfBadge;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.HepyCartLine;
import com.sportforce3d.model.HepyCartSummary;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.viewmodel.HepyCartViewModel;
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

public class HepyCartController {

    private final PpqhAppContext hepyContext;
    private final HepyCartViewModel hepyViewModel;

    @FXML private FlowPane hepyLayout;

    public HepyCartController(PpqhAppContext hepyContext) {
        this.hepyContext = hepyContext;
        this.hepyViewModel = new HepyCartViewModel(hepyContext.ppqhCartService());
    }

    @FXML
    private void initialize() {
        hepyViewModel.summaryProperty().addListener((hepyObs, hepyOld, hepySummary) -> {
            hepyContext.setCartCount(hepySummary.hepyItemCount());
            hepyRender(hepySummary);
        });
        hepyViewModel.errorProperty().addListener((hepyObs, hepyOld, hepyError) -> {
            if (hepyError != null && !hepyError.isEmpty()) {
                hepyLayout.getChildren().setAll(new PpqhEmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar tu carrito", hepyError));
            }
        });
        hepyViewModel.hepyLoad();
    }

    private void hepyRender(HepyCartSummary hepySummary) {
        if (hepySummary.hepyIsEmpty()) {
            PpqhSfButton hepyBrowse = new PpqhSfButton("Ver catálogo", PpqhSfButton.PpqhVariant.PRIMARY).ppqhWithIcon(Feather.GRID);
            hepyBrowse.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CATALOG));
            PpqhEmptyState hepyEmpty = new PpqhEmptyState(Feather.SHOPPING_CART, "Tu carrito está vacío",
                    "Agrega productos del catálogo y aquí verás el total con envío.", hepyBrowse);
            hepyEmpty.setPrefWidth(720);
            hepyLayout.getChildren().setAll(hepyEmpty);
            return;
        }
        VBox hepyLines = new VBox(12);
        hepyLines.setPrefWidth(600);
        hepyLines.setMinWidth(520);
        for (HepyCartLine hepyLine : hepySummary.hepyLines()) {
            hepyLines.getChildren().add(hepyLineRow(hepyLine));
        }
        hepyLayout.getChildren().setAll(hepyLines, hepySummaryPanel(hepySummary));
    }

    private Node hepyLineRow(HepyCartLine hepyLine) {
        StackPane hepyThumb = new StackPane();
        hepyThumb.getStyleClass().add("sf-thumb");
        hepyThumb.setMinSize(88, 110);
        hepyThumb.setMaxSize(88, 110);
        ImageView hepyDesignView = hepyLine.hepyHasDesign()
                ? HepyProductImages.hepyDesignCapture(hepyContext.ppqhStorage(), hepyLine.hepyDesignThumbnail(), 88, 110) : null;
        Image hepyImage = hepyLine.hepyHasDesign() ? null : HepyProductImages.hepyLoad(hepyContext.ppqhStorage(), hepyLine.hepyImagePath(), 88, 110);
        if (hepyDesignView != null) {
            hepyThumb.getChildren().add(hepyDesignView);
        } else if (hepyImage != null) {
            ImageView hepyView = new ImageView(hepyImage);
            hepyView.setFitWidth(88);
            hepyView.setFitHeight(110);
            hepyView.setPreserveRatio(true);
            hepyThumb.getChildren().add(hepyView);
        } else {
            hepyThumb.getChildren().add(new HepyGarmentArt(hepyLine.hepyGarment(), hepyLine.hepyColorHex(), 80));
        }
        hepyThumb.setOnMouseClicked(hepyE -> {
            if (hepyLine.hepyHasDesign()) {
                hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_DESIGNER, PpqhRoutes.PpqhDesignerParams.ppqhOpen(hepyLine.hepyDesignId()));
            } else {
                hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_PRODUCT, hepyLine.hepyProductId());
            }
        });
        hepyThumb.setCursor(javafx.scene.Cursor.HAND);

        Label hepyName = new Label(hepyLine.hepyProductName());
        hepyName.getStyleClass().addAll("title-s", "ink");
        hepyName.setWrapText(true);
        Label hepyMeta = new Label("Talla " + hepyLine.hepySizeName() + (hepyLine.hepyHasDesign() ? "" : " · " + hepyLine.hepyColorName()));
        hepyMeta.getStyleClass().addAll("body-s", "ink-muted");
        Label hepySku = new Label(hepyLine.hepySku());
        hepySku.getStyleClass().addAll("code", "ink-subtle");
        Label hepyUnit = new Label(HepyMoney.hepyFormat(hepyLine.hepyUnitPriceCents()) + " c/u");
        hepyUnit.getStyleClass().addAll("body-s", "ink-muted");
        VBox hepyText = new VBox(4, hepyName, hepyMeta, hepySku, hepyUnit);
        if (hepyLine.hepyHasDesign()) {
            Label hepyDesign = new Label("Diseño: " + (hepyLine.hepyDesignName() == null ? "eliminado" : hepyLine.hepyDesignName()));
            hepyDesign.getStyleClass().addAll("body-s", "brand-text");
            Label hepySurcharge = new Label("Recargo por materiales: " + HepyMoney.hepySurcharge(hepyLine.hepySurchargeCents())
                    + (hepyLine.hepySurchargeCents() == 0 ? "" : " c/u"));
            hepySurcharge.getStyleClass().addAll("body-s", "ink-muted");
            hepyText.getChildren().add(1, new VBox(2, hepyDesign, hepySurcharge));
            if (hepyLine.hepyDesignName() != null && hepyLine.hepyPurchasable()) {
                com.sportforce3d.component.PpqhSfLink hepyEdit = new com.sportforce3d.component.PpqhSfLink("Editar diseño");
                hepyEdit.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_DESIGNER, PpqhRoutes.PpqhDesignerParams.ppqhOpen(hepyLine.hepyDesignId())));
                hepyText.getChildren().add(hepyEdit);
            }
        }
        if (!hepyLine.hepyPurchasable()) {
            hepyText.getChildren().add(new PpqhSfBadge(hepyLine.hepyHasDesign() ? "El diseño o el producto ya no está disponible"
                    : "Ya no está disponible", PpqhSfBadge.PpqhKind.DANGER).ppqhWithIcon(Feather.SLASH));
        } else if (!hepyLine.hepyEnoughStock()) {
            hepyText.getChildren().add(new PpqhSfBadge(hepyLine.hepyAvailable() == 0 ? "Agotado"
                    : "Solo quedan " + hepyLine.hepyAvailable(), PpqhSfBadge.PpqhKind.WARNING).ppqhWithIcon(Feather.ALERT_TRIANGLE));
        }
        HBox.setHgrow(hepyText, Priority.ALWAYS);
        hepyText.setMinWidth(0);

        HepyQuantityStepper hepyStepper = new HepyQuantityStepper();
        hepyStepper.setMax(Math.max(1, Math.min(99, Math.max(hepyLine.hepyAvailable(), hepyLine.hepyQuantity()))));
        hepyStepper.setValue(hepyLine.hepyQuantity());
        hepyStepper.setDisable(!hepyLine.hepyPurchasable());
        PauseTransition hepyDebounce = new PauseTransition(Duration.millis(400));
        hepyDebounce.setOnFinished(hepyE -> hepyViewModel.hepyUpdateQuantity(hepyLine.hepyItemId(), hepyStepper.getValue(),
                hepyMessage -> PpqhToast.ppqhError(hepyLayout, hepyMessage)));
        hepyStepper.valueProperty().addListener((hepyObs, hepyOld, hepyV) -> hepyDebounce.playFromStart());

        Label hepyTotal = new Label(HepyMoney.hepyFormat(hepyLine.hepyLineTotalCents()));
        hepyTotal.getStyleClass().addAll("price", "ink");
        hepyTotal.setMinWidth(Region.USE_PREF_SIZE);

        PpqhSfButton hepyRemove = PpqhSfButton.ppqhIcon(Feather.TRASH_2, "Quitar " + hepyLine.hepyProductName());
        hepyRemove.setOnAction(hepyE -> hepyViewModel.hepyRemove(hepyLine.hepyItemId(),
                () -> PpqhToast.ppqhInfo(hepyLayout, hepyLine.hepyProductName() + " se quitó del carrito."),
                hepyMessage -> PpqhToast.ppqhError(hepyLayout, hepyMessage)));

        VBox hepyRight = new VBox(12, hepyTotal, new HBox(8, hepyStepper, hepyRemove));
        hepyRight.setAlignment(Pos.TOP_RIGHT);
        ((HBox) hepyRight.getChildren().get(1)).setAlignment(Pos.CENTER_RIGHT);

        HBox hepyRow = new HBox(16, hepyThumb, hepyText, hepyRight);
        hepyRow.getStyleClass().add("sf-card");
        hepyRow.setPadding(new Insets(16));
        return hepyRow;
    }

    private Node hepySummaryPanel(HepyCartSummary hepyS) {
        VBox hepyPanel = new VBox(12);
        hepyPanel.getStyleClass().add("sf-panel");
        hepyPanel.setPadding(new Insets(24));
        hepyPanel.setPrefWidth(340);
        hepyPanel.setMinWidth(320);
        hepyPanel.setMaxHeight(Region.USE_PREF_SIZE);

        Label hepyTitle = new Label("Resumen");
        hepyTitle.getStyleClass().addAll("title-s", "ink");
        hepyPanel.getChildren().addAll(hepyTitle,
                hepyRow("Productos (" + hepyS.hepyItemCount() + ")", HepyMoney.hepyFormat(hepyS.hepySubtotalCents())),
                hepyRow("Recargos por materiales", HepyMoney.hepyFormat(hepyS.hepySurchargeCents())),
                hepyRow("Envío", hepyS.hepyShippingCents() == 0 ? "Gratis" : HepyMoney.hepyFormat(hepyS.hepyShippingCents())));
        if (hepyS.hepyMissingForFreeShipping() > 0) {
            Label hepyHint = new Label("Agrega " + HepyMoney.hepyFormat(hepyS.hepyMissingForFreeShipping()) + " más y el envío es gratis.");
            hepyHint.getStyleClass().addAll("body-s", "brand-text");
            hepyHint.setWrapText(true);
            hepyPanel.getChildren().add(hepyHint);
        }
        Region hepyLine = new Region();
        hepyLine.getStyleClass().add("sf-divider");
        Label hepyTotalLabel = new Label("Total");
        hepyTotalLabel.getStyleClass().addAll("title-s", "ink");
        Label hepyTotal = new Label(HepyMoney.hepyFormat(hepyS.hepyTotalCents()));
        hepyTotal.getStyleClass().addAll("price", "ink");
        Region hepySpacer = new Region();
        HBox.setHgrow(hepySpacer, Priority.ALWAYS);
        HBox hepyTotalRow = new HBox(hepyTotalLabel, hepySpacer, hepyTotal);
        hepyTotalRow.setAlignment(Pos.BASELINE_LEFT);

        PpqhSfButton hepyCheckout = new PpqhSfButton("Continuar al pago", PpqhSfButton.PpqhVariant.ACCENT).ppqhWithIcon(Feather.ARROW_RIGHT);
        hepyCheckout.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        hepyCheckout.setMaxWidth(Double.MAX_VALUE);
        hepyCheckout.setDisable(!hepyS.hepyCanCheckout());
        hepyCheckout.disableProperty().bind(hepyViewModel.busyProperty().or(
                javafx.beans.binding.Bindings.createBooleanBinding(() -> !hepyS.hepyCanCheckout())));
        hepyCheckout.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CHECKOUT));

        PpqhSfButton hepyKeep = new PpqhSfButton("Seguir comprando", PpqhSfButton.PpqhVariant.GHOST);
        hepyKeep.setMaxWidth(Double.MAX_VALUE);
        hepyKeep.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_CATALOG));

        hepyPanel.getChildren().addAll(hepyLine, hepyTotalRow, hepyCheckout, hepyKeep);
        if (!hepyS.hepyCanCheckout()) {
            Label hepyWhy = new Label("Revisa los productos marcados antes de continuar.");
            hepyWhy.getStyleClass().addAll("body-s", "warning-text");
            hepyWhy.setWrapText(true);
            hepyPanel.getChildren().add(hepyWhy);
        }
        return hepyPanel;
    }

    static HBox hepyRow(String hepyLabel, String hepyValue) {
        Label hepyL = new Label(hepyLabel);
        hepyL.getStyleClass().addAll("body-s", "ink-muted");
        Label hepyV = new Label(hepyValue);
        hepyV.getStyleClass().addAll("body-s", "ink");
        Region hepySpacer = new Region();
        HBox.setHgrow(hepySpacer, Priority.ALWAYS);
        return new HBox(hepyL, hepySpacer, hepyV);
    }
}
