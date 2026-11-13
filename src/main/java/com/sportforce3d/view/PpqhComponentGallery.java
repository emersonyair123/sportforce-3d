package com.sportforce3d.view;

import com.sportforce3d.app.PpqhTheme;
import com.sportforce3d.component.PpqhAppRoot;
import com.sportforce3d.component.PgeyColorSwatchGroup;
import com.sportforce3d.component.PpqhConfirmDialog;
import com.sportforce3d.component.PpqhChoiceCards;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.HepyFilterChip;
import com.sportforce3d.component.HepyGarmentArt;
import com.sportforce3d.component.PpqhOrderStatusBadge;
import com.sportforce3d.component.HepyPageNav;
import com.sportforce3d.component.PgeyGarmentPicker;
import com.sportforce3d.component.PpqhLogo;
import com.sportforce3d.component.PgeyMaterialPicker;
import com.sportforce3d.component.PpqhMotion;
import com.sportforce3d.component.HepyProductCard;
import com.sportforce3d.component.HepyQuantityStepper;
import com.sportforce3d.component.PpqhSfBadge;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhSidebarNav;
import com.sportforce3d.component.PpqhStatusTimeline;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.HepyProductSummary;
import com.sportforce3d.util.HepyMoney;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;
import java.util.Set;

public final class PpqhComponentGallery {

    private static Stage ppqhOpen;

    private PpqhComponentGallery() {
    }

    public static void ppqhOpen(Window ppqhOwner) {
        if (ppqhOpen != null) {
            ppqhOpen.toFront();
            return;
        }
        PpqhAppRoot ppqhRoot = new PpqhAppRoot();
        Scene ppqhScene = new Scene(ppqhRoot, 1180, 820);
        PpqhTheme.DARK.ppqhApply(ppqhScene);

        VBox ppqhPage = new VBox(48);
        ppqhPage.setPadding(new Insets(32));
        ppqhPage.getChildren().addAll(
                ppqhHeader(ppqhScene),
                ppqhSection("Tipografía", ppqhTypography()),
                ppqhSection("SfButton", ppqhButtons(ppqhRoot)),
                ppqhSection("SfBadge", ppqhBadges()),
                ppqhSection("ProductCard", ppqhProductCards()),
                ppqhSection("GarmentArt (sin foto)", ppqhGarmentArt()),
                ppqhSection("FilterChip y PageNav", ppqhFiltersAndPaging(ppqhRoot)),
                ppqhSection("ChoiceCards y OrderStatusBadge", ppqhChoiceCards()),
                ppqhSection("SfTextField", ppqhTextFields()),
                ppqhSection("ColorSwatch", new PgeyColorSwatchGroup()),
                ppqhSection("GarmentPicker", ppqhGarmentPicker()),
                ppqhSection("MaterialPicker", ppqhMaterialPicker()),
                ppqhSection("QuantityStepper", ppqhQuantity()),
                ppqhSection("StatusTimeline", ppqhTimelines()),
                ppqhSection("EmptyState", new PpqhEmptyState(Feather.PACKAGE, "Aún no tienes pedidos",
                        "Cuando compres algo, aquí verás su estado.",
                        new PpqhSfButton("Ver catálogo", PpqhSfButton.PpqhVariant.PRIMARY))),
                ppqhSection("Toast y ConfirmDialog", ppqhFeedback(ppqhRoot)),
                ppqhSection("SidebarNav", ppqhSidebar()));

        ScrollPane ppqhScroll = new ScrollPane(ppqhPage);
        ppqhScroll.setFitToWidth(true);
        ppqhRoot.setContent(ppqhScroll);

        Stage ppqhStage = new Stage();
        ppqhStage.initOwner(ppqhOwner);
        ppqhStage.setTitle("SPORTFORCE 3D · Galería de componentes");
        if (ppqhOwner instanceof Stage ppqhOwnerStage) {
            ppqhStage.getIcons().setAll(ppqhOwnerStage.getIcons());
        }
        ppqhStage.setScene(ppqhScene);
        ppqhStage.setOnHidden(ppqhE -> ppqhOpen = null);
        ppqhOpen = ppqhStage;
        ppqhStage.show();
    }

    private static Node ppqhHeader(Scene ppqhScene) {
        Label ppqhTitle = new Label("GALERÍA DE COMPONENTES");
        ppqhTitle.getStyleClass().addAll("display-l", "ink");

        ComboBox<PpqhTheme> ppqhTheme = new ComboBox<>();
        ppqhTheme.getItems().setAll(PpqhTheme.values());
        ppqhTheme.setValue(PpqhTheme.DARK);
        ppqhTheme.setOnAction(ppqhE -> ppqhTheme.getValue().ppqhApply(ppqhScene));
        ppqhTheme.setAccessibleText("Tema");

        CheckBox ppqhReduce = new CheckBox("Reducir animaciones");
        ppqhReduce.selectedProperty().bindBidirectional(PpqhMotion.reducedProperty());

        Region ppqhSpacer = new Region();
        HBox.setHgrow(ppqhSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox ppqhControls = new HBox(16, new PpqhLogo(28), ppqhSpacer, ppqhLabel("Tema"), ppqhTheme, ppqhReduce);
        ppqhControls.setAlignment(Pos.CENTER_LEFT);
        return new VBox(16, ppqhControls, ppqhTitle);
    }

    private static Node ppqhSection(String ppqhName, Node ppqhContent) {
        Label ppqhLabel = new Label(ppqhName);
        ppqhLabel.getStyleClass().addAll("heading-m", "ink");
        Region ppqhLine = new Region();
        ppqhLine.getStyleClass().add("sf-divider");
        return new VBox(16, ppqhLabel, ppqhLine, ppqhContent);
    }

    private static Node ppqhTypography() {
        return new VBox(12,
                ppqhStyled("FORJA TU EQUIPO", "display-xl"),
                ppqhStyled("UNIFORMES DE FÚTBOL", "display-l"),
                ppqhStyled("Nuevos lanzamientos", "heading-m"),
                ppqhStyled(HepyMoney.hepyFormat(12990), "price"),
                ppqhStyled("Camiseta Pro Dry-Fit", "title-s"),
                ppqhStyled("Tejido transpirable con secado rápido.", "body"),
                ppqhStyled("Envío a todo el Perú en 3–5 días.", "body-s"),
                ppqhStyled("CAMISETAS", "label-text"),
                ppqhStyled("PED-2026-000154 · Yape · Op. 48213077", "code"));
    }

    private static Node ppqhButtons(Node ppqhAnyNode) {
        PpqhSfButton ppqhAccent = new PpqhSfButton("Diseña tu uniforme 3D", PpqhSfButton.PpqhVariant.ACCENT, PpqhSfButton.PpqhSize.LG);
        ppqhAccent.setOnAction(ppqhE -> PpqhToast.ppqhInfo(ppqhAnyNode, "Botón accent: el único CTA principal por vista."));
        PpqhSfButton ppqhDisabled = new PpqhSfButton("Agregar al carrito", PpqhSfButton.PpqhVariant.PRIMARY).ppqhComingSoon();
        FlowPane ppqhSizes = ppqhRow(
                new PpqhSfButton("Pequeño", PpqhSfButton.PpqhVariant.PRIMARY, PpqhSfButton.PpqhSize.SM),
                new PpqhSfButton("Mediano", PpqhSfButton.PpqhVariant.PRIMARY, PpqhSfButton.PpqhSize.MD),
                new PpqhSfButton("Grande", PpqhSfButton.PpqhVariant.PRIMARY, PpqhSfButton.PpqhSize.LG));
        FlowPane ppqhVariants = ppqhRow(ppqhAccent,
                new PpqhSfButton("Confirmar pedido", PpqhSfButton.PpqhVariant.ACCENT),
                new PpqhSfButton("Agregar al carrito", PpqhSfButton.PpqhVariant.PRIMARY).ppqhWithIcon(Feather.SHOPPING_CART),
                new PpqhSfButton("Ver detalle", PpqhSfButton.PpqhVariant.GHOST),
                new PpqhSfButton("Eliminar diseño", PpqhSfButton.PpqhVariant.DANGER).ppqhWithIcon(Feather.TRASH_2),
                PpqhSfButton.ppqhIcon(Feather.ROTATE_CCW, "Deshacer"),
                ppqhDisabled);
        return new VBox(16, ppqhVariants, ppqhSizes, ppqhCaption("Tab para ver el anillo de foco. El botón deshabilitado dice \"Próximamente\"."));
    }

    private static Node ppqhBadges() {
        return ppqhRow(new PpqhSfBadge("Nuevo", PpqhSfBadge.PpqhKind.NEW),
                new PpqhSfBadge("3D", PpqhSfBadge.PpqhKind.BRAND),
                new PpqhSfBadge("Oferta", PpqhSfBadge.PpqhKind.SALE),
                new PpqhSfBadge("Pagado", PpqhSfBadge.PpqhKind.SUCCESS).ppqhWithIcon(Feather.CHECK),
                new PpqhSfBadge("Pago en verificación", PpqhSfBadge.PpqhKind.WARNING).ppqhWithIcon(Feather.CLOCK),
                new PpqhSfBadge("Agotado", PpqhSfBadge.PpqhKind.DANGER).ppqhWithIcon(Feather.SLASH),
                new PpqhSfBadge("Neutral"));
    }

    private static Node ppqhProductCards() {
        return ppqhRow(
                new HepyProductCard(new HepyProductSummary(1, "SF-CAM-001", "Camiseta Pro Dry-Fit", "Camisetas deportivas",
                        HepyGarment.JERSEY, 7990, null, true, true, 40, "#1F5FFF", 6, null, null, null), null),
                new HepyProductCard(new HepyProductSummary(2, "SF-UFU-002", "Uniforme Fútbol Clásico Franjas", "Uniformes de fútbol",
                        HepyGarment.KIT, 12990, 15990L, false, true, 12, "#D7192A", 6, null, null, null), null),
                new HepyProductCard(new HepyProductSummary(3, "SF-ZAP-003", "Zapatillas Running Pulse", "Zapatillas",
                        HepyGarment.SHOES, 27990, null, true, false, 0, "#1F5FFF", 9, null, null, null), null));
    }

    private static Node ppqhGarmentArt() {
        FlowPane ppqhPane = ppqhRow();
        String[] ppqhColors = {"#1F5FFF", "#D7192A", "#F2F5F8", "#0B0E13", "#FFD400", "#0E7C3A", "#7B2CBF"};
        HepyGarment[] ppqhGarments = {HepyGarment.JERSEY, HepyGarment.KIT, HepyGarment.SHORTS, HepyGarment.SOCKS, HepyGarment.SHOES,
                HepyGarment.HOODIE, HepyGarment.ACCESSORY};
        for (int ppqhI = 0; ppqhI < ppqhGarments.length; ppqhI++) {
            javafx.scene.layout.StackPane ppqhTile = new javafx.scene.layout.StackPane(new HepyGarmentArt(ppqhGarments[ppqhI], ppqhColors[ppqhI], 110));
            ppqhTile.getStyleClass().add("sf-product-media");
            ppqhTile.setPrefSize(120, 150);
            Label ppqhCaption = ppqhLabel(ppqhGarments[ppqhI].hepyLabel());
            ppqhPane.getChildren().add(new VBox(6, ppqhTile, ppqhCaption));
        }
        return ppqhPane;
    }

    private static Node ppqhFiltersAndPaging(Node ppqhAnyNode) {
        javafx.scene.control.ToggleGroup ppqhGroup = new javafx.scene.control.ToggleGroup();
        FlowPane ppqhChips = ppqhRow();
        for (String ppqhText : new String[]{"Todas", "Uniformes de fútbol", "Shorts", "Zapatillas"}) {
            HepyFilterChip ppqhChip = new HepyFilterChip(ppqhText);
            ppqhChip.setToggleGroup(ppqhGroup);
            ppqhChips.getChildren().add(ppqhChip);
        }
        ((HepyFilterChip) ppqhChips.getChildren().get(0)).setSelected(true);
        HepyPageNav ppqhNav = new HepyPageNav(ppqhPage -> PpqhToast.ppqhInfo(ppqhAnyNode, "Ir a la página " + ppqhPage));
        ppqhNav.hepyShow(new com.sportforce3d.model.HepyPage<>(java.util.List.of(), 2, 12, 50));
        return new VBox(16, ppqhChips, ppqhNav);
    }

    private static Node ppqhChoiceCards() {
        PpqhChoiceCards<com.sportforce3d.model.PpqhPaymentMethod> ppqhCards = new PpqhChoiceCards<>();
        for (com.sportforce3d.model.PpqhPaymentMethod ppqhM : com.sportforce3d.model.PpqhPaymentMethod.values()) {
            ppqhCards.ppqhAdd(ppqhM, ppqhM.ppqhLabel(), ppqhM.ppqhAvailable() ? "Verificación manual por un administrador." : null, null, ppqhM.ppqhAvailable());
        }
        ppqhCards.setSelected(com.sportforce3d.model.PpqhPaymentMethod.YAPE);
        ppqhCards.setMaxWidth(520);
        FlowPane ppqhBadges = ppqhRow();
        for (PpqhOrderStatus ppqhStatus : PpqhOrderStatus.values()) {
            ppqhBadges.getChildren().add(PpqhOrderStatusBadge.ppqhOf(ppqhStatus));
        }
        return new VBox(16, ppqhCards, ppqhBadges);
    }

    private static Node ppqhTextFields() {
        PpqhSfTextField ppqhNormal = new PpqhSfTextField("Nombre del jugador");
        ppqhNormal.setPromptText("Guerrero");
        ppqhNormal.setHint("Máximo 12 caracteres.");
        PpqhSfTextField ppqhError = new PpqhSfTextField("Número");
        ppqhError.setText("120");
        ppqhError.setError("El número va de 0 a 99.");
        PpqhSfTextField ppqhPassword = new PpqhSfTextField("Contraseña", true);
        PpqhSfTextField ppqhHex = new PpqhSfTextField("HEX");
        ppqhHex.setMonospace(true);
        ppqhHex.setText("#1F5FFF");
        PpqhSfTextField ppqhDisabled = new PpqhSfTextField("Correo");
        ppqhDisabled.setText("cliente@sportforce3d.pe");
        ppqhDisabled.getInput().setDisable(true);
        return ppqhRow(ppqhWidth(ppqhNormal), ppqhWidth(ppqhError), ppqhWidth(ppqhPassword), ppqhWidth(ppqhHex), ppqhWidth(ppqhDisabled));
    }

    private static Node ppqhGarmentPicker() {
        PgeyGarmentPicker ppqhPicker = new PgeyGarmentPicker();
        ppqhPicker.setPrefWrapLength(560);
        ppqhPicker.setMaxWidth(Region.USE_PREF_SIZE);
        Label ppqhSelected = ppqhLabel("");
        ppqhSelected.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> "Elegida: " + ppqhPicker.getSelected().hepyLabel(), ppqhPicker.selectedProperty()));
        return new VBox(12, ppqhPicker, ppqhSelected);
    }

    private static Node ppqhMaterialPicker() {
        PgeyMaterialPicker ppqhPicker = new PgeyMaterialPicker();
        ppqhPicker.setPrefWidth(420);
        ppqhPicker.setMinWidth(360);
        ppqhPicker.setMaterials(List.of(
                ppqhMaterial(1, "Dry-Fit", "Transpira y seca rápido", 0, HepyMaterial.HepyPattern.DOTS),
                ppqhMaterial(2, "Malla", "Ventilación máxima", 500, HepyMaterial.HepyPattern.MESH),
                ppqhMaterial(3, "Jacquard", "Relieve tono sobre tono", 1500, HepyMaterial.HepyPattern.DIAGONAL),
                ppqhMaterial(4, "Licra", "Compresión y ajuste", 1000, HepyMaterial.HepyPattern.LINES),
                ppqhMaterial(5, "Algodón", "Suave, casual", 0, HepyMaterial.HepyPattern.PLAIN)));
        PgeyColorSwatchGroup ppqhColor = new PgeyColorSwatchGroup();
        ppqhPicker.sampleColorProperty().bind(ppqhColor.valueProperty());
        Label ppqhTotal = new Label();
        ppqhTotal.getStyleClass().addAll("price", "ink");
        ppqhTotal.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> "Recargo: " + (ppqhPicker.getSelected() == null ? HepyMoney.hepyFormat(0) : HepyMoney.hepyFormat(ppqhPicker.getSelected().hepySurchargeCents())),
                ppqhPicker.selectedProperty()));
        return new HBox(32, ppqhPicker, new VBox(16, ppqhLabel("Color de la prenda"), ppqhColor, ppqhTotal));
    }

    private static HepyMaterial ppqhMaterial(long ppqhId, String ppqhName, String ppqhUsage, long ppqhSurcharge, HepyMaterial.HepyPattern ppqhPattern) {
        return new HepyMaterial(ppqhId, ppqhName.toUpperCase(), ppqhName, null, ppqhUsage, HepyMaterial.HepyScope.TEXTIL, Set.of("JERSEY"),
                ppqhSurcharge, 0.5, 0.3, 0, ppqhPattern);
    }

    private static Node ppqhQuantity() {
        HepyQuantityStepper ppqhStepper = new HepyQuantityStepper();
        ppqhStepper.setMax(10);
        Label ppqhValue = ppqhLabel("");
        ppqhValue.textProperty().bind(ppqhStepper.valueProperty().asString("Cantidad: %d (máx. 10)"));
        HBox ppqhBox = new HBox(16, ppqhStepper, ppqhValue);
        ppqhBox.setAlignment(Pos.CENTER_LEFT);
        return ppqhBox;
    }

    private static Node ppqhTimelines() {
        PpqhStatusTimeline ppqhVerifying = new PpqhStatusTimeline();
        ppqhVerifying.setStatus(PpqhOrderStatus.PAGO_EN_VERIFICACION);
        PpqhStatusTimeline ppqhProduction = new PpqhStatusTimeline();
        ppqhProduction.setStatus(PpqhOrderStatus.EN_PRODUCCION);
        PpqhStatusTimeline ppqhDelivered = new PpqhStatusTimeline();
        ppqhDelivered.setStatus(PpqhOrderStatus.ENTREGADO);
        PpqhStatusTimeline ppqhCancelled = new PpqhStatusTimeline();
        ppqhCancelled.setCancelled(PpqhOrderStatus.PAGO_EN_VERIFICACION);
        return new VBox(24, ppqhVerifying, ppqhProduction, ppqhDelivered, ppqhCancelled);
    }

    private static Node ppqhFeedback(Node ppqhAnyNode) {
        PpqhSfButton ppqhInfo = new PpqhSfButton("Toast info", PpqhSfButton.PpqhVariant.GHOST);
        ppqhInfo.setOnAction(ppqhE -> PpqhToast.ppqhInfo(ppqhAnyNode, "Diseño guardado como borrador."));
        PpqhSfButton ppqhSuccess = new PpqhSfButton("Toast éxito", PpqhSfButton.PpqhVariant.GHOST);
        ppqhSuccess.setOnAction(ppqhE -> PpqhToast.ppqhSuccess(ppqhAnyNode, "Pago aprobado · PED-2026-000154"));
        PpqhSfButton ppqhWarning = new PpqhSfButton("Toast aviso", PpqhSfButton.PpqhVariant.GHOST);
        ppqhWarning.setOnAction(ppqhE -> PpqhToast.ppqhShow(ppqhAnyNode, "Pago pendiente de verificación.", PpqhToast.PpqhKind.WARNING));
        PpqhSfButton ppqhError = new PpqhSfButton("Toast error", PpqhSfButton.PpqhVariant.GHOST);
        ppqhError.setOnAction(ppqhE -> PpqhToast.ppqhError(ppqhAnyNode, "No se pudo guardar. Inténtalo de nuevo."));
        PpqhSfButton ppqhConfirm = new PpqhSfButton("Eliminar diseño…", PpqhSfButton.PpqhVariant.DANGER);
        ppqhConfirm.setOnAction(ppqhE -> PpqhConfirmDialog.ppqhShow(ppqhAnyNode, "¿Eliminar este diseño?",
                "Se borrará “Uniforme Los Halcones”. Esta acción no se puede deshacer.", "Eliminar", true,
                () -> PpqhToast.ppqhSuccess(ppqhAnyNode, "Diseño eliminado (demostración).")));
        return ppqhRow(ppqhInfo, ppqhSuccess, ppqhWarning, ppqhError, ppqhConfirm);
    }

    private static Node ppqhSidebar() {
        PpqhSidebarNav ppqhNav = new PpqhSidebarNav();
        ppqhNav.ppqhAddSection("Tienda");
        ppqhNav.ppqhAddItem("home", "Inicio", Feather.HOME, true);
        ppqhNav.ppqhAddItem("designer", "Diseñador 3D", Feather.BOX, false);
        ppqhNav.ppqhAddItem("account", "Mi cuenta", Feather.USER, true);
        ppqhNav.ppqhAddSection("Administración");
        ppqhNav.ppqhAddItem("users", "Usuarios", Feather.USERS, true);
        ppqhNav.ppqhSelect("home");
        ppqhNav.setPrefHeight(360);
        ppqhNav.setMaxHeight(360);
        return ppqhNav;
    }

    private static FlowPane ppqhRow(Node... ppqhNodes) {
        FlowPane ppqhPane = new FlowPane(16, 16, ppqhNodes);
        ppqhPane.setAlignment(Pos.TOP_LEFT);
        return ppqhPane;
    }

    private static Node ppqhWidth(PpqhSfTextField ppqhField) {
        ppqhField.setPrefWidth(260);
        return ppqhField;
    }

    private static Label ppqhStyled(String ppqhText, String ppqhStyleClass) {
        Label ppqhLabel = new Label(ppqhText);
        ppqhLabel.getStyleClass().addAll(ppqhStyleClass, "ink");
        ppqhLabel.setWrapText(true);
        return ppqhLabel;
    }

    private static Label ppqhLabel(String ppqhText) {
        Label ppqhLabel = new Label(ppqhText);
        ppqhLabel.getStyleClass().addAll("body-s", "ink-muted");
        return ppqhLabel;
    }

    private static Label ppqhCaption(String ppqhText) {
        Label ppqhLabel = ppqhLabel(ppqhText);
        ppqhLabel.getStyleClass().add("ink-subtle");
        return ppqhLabel;
    }
}
