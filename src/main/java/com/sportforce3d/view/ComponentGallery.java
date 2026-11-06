package com.sportforce3d.view;

import com.sportforce3d.app.Theme;
import com.sportforce3d.component.AppRoot;
import com.sportforce3d.component.ColorSwatchGroup;
import com.sportforce3d.component.ConfirmDialog;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.GarmentPicker;
import com.sportforce3d.component.Logo;
import com.sportforce3d.component.MaterialPicker;
import com.sportforce3d.component.Motion;
import com.sportforce3d.component.ProductCard;
import com.sportforce3d.component.QuantityStepper;
import com.sportforce3d.component.SfBadge;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.SidebarNav;
import com.sportforce3d.component.StatusTimeline;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.ProductSummary;
import com.sportforce3d.util.Money;
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

/**
 * Catálogo visual de los componentes del sistema de diseño en sus estados.
 * Permite alternar tema oscuro/claro y "Reducir animaciones". Ábrelo desde el menú de la cuenta
 * o con el argumento {@code --gallery}.
 */
public final class ComponentGallery {

    private static Stage open;

    private ComponentGallery() {
    }

    public static void open(Window owner) {
        if (open != null) {
            open.toFront();
            return;
        }
        AppRoot root = new AppRoot();
        Scene scene = new Scene(root, 1180, 820);
        Theme.DARK.apply(scene);

        VBox page = new VBox(48);
        page.setPadding(new Insets(32));
        page.getChildren().addAll(
                header(scene),
                section("Tipografía", typography()),
                section("SfButton", buttons(root)),
                section("SfBadge", badges()),
                section("ProductCard", productCards()),
                section("SfTextField", textFields()),
                section("ColorSwatch", new ColorSwatchGroup()),
                section("GarmentPicker", garmentPicker()),
                section("MaterialPicker", materialPicker()),
                section("QuantityStepper", quantity()),
                section("StatusTimeline", timelines()),
                section("EmptyState", new EmptyState(Feather.PACKAGE, "Aún no tienes pedidos",
                        "Cuando compres algo, aquí verás su estado.",
                        new SfButton("Ver catálogo", SfButton.Variant.PRIMARY))),
                section("Toast y ConfirmDialog", feedback(root)),
                section("SidebarNav", sidebar()));

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        root.setContent(scroll);

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("SPORTFORCE 3D · Galería de componentes");
        if (owner instanceof Stage ownerStage) {
            stage.getIcons().setAll(ownerStage.getIcons());
        }
        stage.setScene(scene);
        stage.setOnHidden(e -> open = null);
        open = stage;
        stage.show();
    }

    private static Node header(Scene scene) {
        Label title = new Label("GALERÍA DE COMPONENTES");
        title.getStyleClass().addAll("display-l", "ink");

        ComboBox<Theme> theme = new ComboBox<>();
        theme.getItems().setAll(Theme.values());
        theme.setValue(Theme.DARK);
        theme.setOnAction(e -> theme.getValue().apply(scene));
        theme.setAccessibleText("Tema");

        CheckBox reduce = new CheckBox("Reducir animaciones");
        reduce.selectedProperty().bindBidirectional(Motion.reducedProperty());

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox controls = new HBox(16, new Logo(28), spacer, label("Tema"), theme, reduce);
        controls.setAlignment(Pos.CENTER_LEFT);
        return new VBox(16, controls, title);
    }

    private static Node section(String name, Node content) {
        Label label = new Label(name);
        label.getStyleClass().addAll("heading-m", "ink");
        Region line = new Region();
        line.getStyleClass().add("sf-divider");
        return new VBox(16, label, line, content);
    }

    private static Node typography() {
        return new VBox(12,
                styled("FORJA TU EQUIPO", "display-xl"),
                styled("UNIFORMES DE FÚTBOL", "display-l"),
                styled("Nuevos lanzamientos", "heading-m"),
                styled(Money.format(12990), "price"),
                styled("Camiseta Pro Dry-Fit", "title-s"),
                styled("Tejido transpirable con secado rápido.", "body"),
                styled("Envío a todo el Perú en 3–5 días.", "body-s"),
                styled("CAMISETAS", "label-text"),
                styled("PED-2026-000154 · Yape · Op. 48213077", "code"));
    }

    private static Node buttons(Node anyNode) {
        SfButton accent = new SfButton("Diseña tu uniforme 3D", SfButton.Variant.ACCENT, SfButton.Size.LG);
        accent.setOnAction(e -> Toast.info(anyNode, "Botón accent: el único CTA principal por vista."));
        SfButton disabled = new SfButton("Agregar al carrito", SfButton.Variant.PRIMARY).comingSoon();
        FlowPane sizes = row(
                new SfButton("Pequeño", SfButton.Variant.PRIMARY, SfButton.Size.SM),
                new SfButton("Mediano", SfButton.Variant.PRIMARY, SfButton.Size.MD),
                new SfButton("Grande", SfButton.Variant.PRIMARY, SfButton.Size.LG));
        FlowPane variants = row(accent,
                new SfButton("Confirmar pedido", SfButton.Variant.ACCENT),
                new SfButton("Agregar al carrito", SfButton.Variant.PRIMARY).withIcon(Feather.SHOPPING_CART),
                new SfButton("Ver detalle", SfButton.Variant.GHOST),
                new SfButton("Eliminar diseño", SfButton.Variant.DANGER).withIcon(Feather.TRASH_2),
                SfButton.icon(Feather.ROTATE_CCW, "Deshacer"),
                disabled);
        return new VBox(16, variants, sizes, caption("Tab para ver el anillo de foco. El botón deshabilitado dice \"Próximamente\"."));
    }

    private static Node badges() {
        return row(new SfBadge("Nuevo", SfBadge.Kind.NEW),
                new SfBadge("3D", SfBadge.Kind.BRAND),
                new SfBadge("Oferta", SfBadge.Kind.SALE),
                new SfBadge("Pagado", SfBadge.Kind.SUCCESS).withIcon(Feather.CHECK),
                new SfBadge("Pago en verificación", SfBadge.Kind.WARNING).withIcon(Feather.CLOCK),
                new SfBadge("Agotado", SfBadge.Kind.DANGER).withIcon(Feather.SLASH),
                new SfBadge("Neutral"));
    }

    private static Node productCards() {
        return row(
                new ProductCard(new ProductSummary(1, "SF-CAM-001", "Camiseta Pro Dry-Fit", "Camisetas deportivas",
                        Garment.JERSEY, 7990, null, true, true, 40), null),
                new ProductCard(new ProductSummary(2, "SF-UFU-002", "Uniforme Fútbol Clásico Franjas", "Uniformes de fútbol",
                        Garment.KIT, 12990, 15990L, false, true, 12), null),
                new ProductCard(new ProductSummary(3, "SF-ZAP-003", "Zapatillas Running Pulse", "Zapatillas",
                        Garment.SHOES, 27990, null, true, false, 0), null));
    }

    private static Node textFields() {
        SfTextField normal = new SfTextField("Nombre del jugador");
        normal.setPromptText("Guerrero");
        normal.setHint("Máximo 12 caracteres.");
        SfTextField error = new SfTextField("Número");
        error.setText("120");
        error.setError("El número va de 0 a 99.");
        SfTextField password = new SfTextField("Contraseña", true);
        SfTextField hex = new SfTextField("HEX");
        hex.setMonospace(true);
        hex.setText("#1F5FFF");
        SfTextField disabled = new SfTextField("Correo");
        disabled.setText("cliente@sportforce3d.pe");
        disabled.getInput().setDisable(true);
        return row(width(normal), width(error), width(password), width(hex), width(disabled));
    }

    private static Node garmentPicker() {
        GarmentPicker picker = new GarmentPicker();
        Label selected = label("");
        selected.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> "Elegida: " + picker.getSelected().label(), picker.selectedProperty()));
        return new VBox(12, picker, selected);
    }

    private static Node materialPicker() {
        MaterialPicker picker = new MaterialPicker();
        picker.setPrefWidth(420);
        picker.setMinWidth(360);
        picker.setMaterials(List.of(
                material(1, "Dry-Fit", "Transpira y seca rápido", 0, Material.Pattern.DOTS),
                material(2, "Malla", "Ventilación máxima", 500, Material.Pattern.MESH),
                material(3, "Jacquard", "Relieve tono sobre tono", 1500, Material.Pattern.DIAGONAL),
                material(4, "Licra", "Compresión y ajuste", 1000, Material.Pattern.LINES),
                material(5, "Algodón", "Suave, casual", 0, Material.Pattern.PLAIN)));
        ColorSwatchGroup color = new ColorSwatchGroup();
        picker.sampleColorProperty().bind(color.valueProperty());
        Label total = new Label();
        total.getStyleClass().addAll("price", "ink");
        total.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> "Recargo: " + (picker.getSelected() == null ? Money.format(0) : Money.format(picker.getSelected().surchargeCents())),
                picker.selectedProperty()));
        return new HBox(32, picker, new VBox(16, label("Color de la prenda"), color, total));
    }

    private static Material material(long id, String name, String usage, long surcharge, Material.Pattern pattern) {
        return new Material(id, name.toUpperCase(), name, null, usage, Material.Scope.TEXTIL, Set.of("JERSEY"),
                surcharge, 0.5, 0.3, 0, pattern);
    }

    private static Node quantity() {
        QuantityStepper stepper = new QuantityStepper();
        stepper.setMax(10);
        Label value = label("");
        value.textProperty().bind(stepper.valueProperty().asString("Cantidad: %d (máx. 10)"));
        HBox box = new HBox(16, stepper, value);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Node timelines() {
        StatusTimeline verifying = new StatusTimeline();
        verifying.setStatus(OrderStatus.PAGO_EN_VERIFICACION);
        StatusTimeline production = new StatusTimeline();
        production.setStatus(OrderStatus.EN_PRODUCCION);
        StatusTimeline delivered = new StatusTimeline();
        delivered.setStatus(OrderStatus.ENTREGADO);
        StatusTimeline cancelled = new StatusTimeline();
        cancelled.setCancelled(OrderStatus.PAGO_EN_VERIFICACION);
        return new VBox(24, verifying, production, delivered, cancelled);
    }

    private static Node feedback(Node anyNode) {
        SfButton info = new SfButton("Toast info", SfButton.Variant.GHOST);
        info.setOnAction(e -> Toast.info(anyNode, "Diseño guardado como borrador."));
        SfButton success = new SfButton("Toast éxito", SfButton.Variant.GHOST);
        success.setOnAction(e -> Toast.success(anyNode, "Pago aprobado · PED-2026-000154"));
        SfButton warning = new SfButton("Toast aviso", SfButton.Variant.GHOST);
        warning.setOnAction(e -> Toast.show(anyNode, "Pago pendiente de verificación.", Toast.Kind.WARNING));
        SfButton error = new SfButton("Toast error", SfButton.Variant.GHOST);
        error.setOnAction(e -> Toast.error(anyNode, "No se pudo guardar. Inténtalo de nuevo."));
        SfButton confirm = new SfButton("Eliminar diseño…", SfButton.Variant.DANGER);
        confirm.setOnAction(e -> ConfirmDialog.show(anyNode, "¿Eliminar este diseño?",
                "Se borrará “Uniforme Los Halcones”. Esta acción no se puede deshacer.", "Eliminar", true,
                () -> Toast.success(anyNode, "Diseño eliminado (demostración).")));
        return row(info, success, warning, error, confirm);
    }

    private static Node sidebar() {
        SidebarNav nav = new SidebarNav();
        nav.addSection("Tienda");
        nav.addItem("home", "Inicio", Feather.HOME, true);
        nav.addItem("designer", "Diseñador 3D", Feather.BOX, false);
        nav.addItem("account", "Mi cuenta", Feather.USER, true);
        nav.addSection("Administración");
        nav.addItem("users", "Usuarios", Feather.USERS, true);
        nav.select("home");
        nav.setPrefHeight(360);
        nav.setMaxHeight(360);
        return nav;
    }

    private static FlowPane row(Node... nodes) {
        FlowPane pane = new FlowPane(16, 16, nodes);
        pane.setAlignment(Pos.TOP_LEFT);
        return pane;
    }

    private static Node width(SfTextField field) {
        field.setPrefWidth(260);
        return field;
    }

    private static Label styled(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClass, "ink");
        label.setWrapText(true);
        return label;
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("body-s", "ink-muted");
        return label;
    }

    private static Label caption(String text) {
        Label label = label(text);
        label.getStyleClass().add("ink-subtle");
        return label;
    }
}
