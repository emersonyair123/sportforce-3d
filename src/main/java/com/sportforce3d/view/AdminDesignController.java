package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.app.Router;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.ModalDialog;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfLink;
import com.sportforce3d.component.Toast;
import com.sportforce3d.design.Area;
import com.sportforce3d.design.PatternType;
import com.sportforce3d.design.TechSheet;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.OrderItem;
import com.sportforce3d.service.AdminOrderService.OrderDesign;
import com.sportforce3d.three.DesignRenderer;
import com.sportforce3d.three.ElementImages;
import com.sportforce3d.three.TextureComposer;
import com.sportforce3d.three.ViewPreset;
import com.sportforce3d.three.Viewer3D;
import com.sportforce3d.three.model.ModelProviders;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Administración → Pedidos → uniforme personalizado: el mismo visor 3D del diseñador en modo solo lectura, con las
 * capturas del pedido, la ficha técnica por zona y la descarga de los archivos originales. Parámetro: id de la línea.
 */
public class AdminDesignController implements Router.Routable {

    private final AppContext context;
    private final Viewer3D viewer = new Viewer3D();
    private final DesignRenderer renderer;
    private OrderDesign design;
    private long orderId;

    @FXML private StackPane stage;
    @FXML private HBox toolbar;
    @FXML private VBox loadingBox;
    @FXML private VBox panel;
    @FXML private SfLink backLink;

    public AdminDesignController(AppContext context) {
        this.context = context;
        this.renderer = new DesignRenderer(viewer, new TextureComposer(new ElementImages(context.storage())));
    }

    @FXML
    private void initialize() {
        stage.getChildren().add(0, viewer);
        viewer.autoRotateProperty().set(false);
        backLink.setGraphic(Icons.of(Feather.ARROW_LEFT, Icons.SM));
        backLink.setOnAction(e -> context.router().go(Routes.ADMIN_ORDER, orderId));
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setSpacing(4);
        for (ViewPreset preset : ViewPreset.values()) {
            SfButton b = new SfButton(preset.label(), SfButton.Variant.GHOST, SfButton.Size.SM);
            b.getStyleClass().add("sf-toolbar-btn");
            b.setOnAction(e -> viewer.showView(preset));
            toolbar.getChildren().add(b);
        }
        SfButton reset = SfButton.icon(Feather.MAXIMIZE, "Cuerpo completo");
        reset.setOnAction(e -> viewer.resetView());
        toolbar.getChildren().add(reset);
    }

    @Override
    public void onRoute(Object param) {
        if (!(param instanceof Long itemId)) {
            context.router().go(Routes.ADMIN_ORDERS);
            return;
        }
        Async.run(() -> {
            OrderDesign d = context.adminOrderService().design(itemId);
            return new Object[]{d, ModelProviders.load(d.document().bodyType())};
        }, result -> {
            design = (OrderDesign) result[0];
            orderId = design.orderId();
            viewer.setModel((com.sportforce3d.three.model.ModelData) result[1]);
            renderer.applyAll(design.document(), design.materials());
            loadingBox.setVisible(false);
            buildPanel();
        }, e -> {
            loadingBox.setVisible(false);
            panel.getChildren().setAll(backLink, new EmptyState(Feather.ALERT_TRIANGLE, "No se pudo abrir el uniforme",
                    ErrorMessages.of(e)));
        });
    }

    // ---------------------------------------------------------------- Panel

    private void buildPanel() {
        OrderItem item = design.item();
        Label title = new Label("UNIFORME");
        title.getStyleClass().addAll("display-l", "ink");
        Label sub = muted(design.orderNumber() + " · " + item.productName() + " · talla " + item.sizeCode() + " · "
                + item.quantity() + (item.quantity() == 1 ? " unidad" : " unidades"));
        Label name = new Label(item.designName() + " · versión " + item.designVersion() + " · "
                + design.document().bodyType().label());
        name.getStyleClass().addAll("body-s", "brand-text");
        name.setWrapText(true);

        SfButton download = new SfButton("Descargar archivos", SfButton.Variant.PRIMARY, SfButton.Size.SM)
                .withIcon(Feather.DOWNLOAD);
        download.setMaxWidth(Double.MAX_VALUE);
        download.setOnAction(e -> download());
        Label filesNote = muted("Carpeta con diseño (JSON), capturas de frente y espalda, ficha técnica (CSV para Excel)"
                + (design.assets().isEmpty() ? "." : " y " + design.assets().size()
                + (design.assets().size() == 1 ? " logo original." : " logos originales.")));

        TechSheet sheet = design.sheet();
        panel.getChildren().setAll(backLink, new VBox(6, title, sub, name), captures(item),
                new VBox(8, download, filesNote), zones(sheet), materials(sheet), patterns(sheet), elements(sheet));
    }

    private Node captures(OrderItem item) {
        HBox row = new HBox(12, capture(item.designFrontPath(), "Frente"), capture(item.designBackPath(), "Espalda"));
        return section("CAPTURAS DEL PEDIDO", row);
    }

    private Node capture(String path, String label) {
        // Las capturas son de cuerpo completo: la miniatura muestra el centro; al hacer clic, la imagen completa.
        ImageView view = ProductImages.designCapture(context.storage(), path, 180, 220);
        StackPane box = new StackPane();
        box.getStyleClass().add("sf-proof");
        box.setPrefSize(180, 220);
        box.setMinSize(180, 220);
        if (view != null) {
            box.getChildren().add(view);
            box.setCursor(Cursor.HAND);
            box.setOnMouseClicked(e -> {
                ImageView big = new ImageView(ProductImages.load(context.storage(), path, 900, 1200));
                big.setFitWidth(430);
                big.setFitHeight(560);
                big.setPreserveRatio(true);
                ModalDialog.show(stage, label, null, new StackPane(big), "Cerrar", ModalDialog.Style.PRIMARY,
                        ModalDialog::close);
            });
        } else {
            box.getChildren().add(muted("Sin captura"));
        }
        Label l = muted(label);
        return new VBox(6, box, l);
    }

    private Node zones(TechSheet sheet) {
        VBox box = new VBox(14);
        for (Garment g : java.util.List.of(Garment.JERSEY, Garment.SHORTS, Garment.SOCKS, Garment.SHOES)) {
            GridPane grid = grid();
            int r = 0;
            for (TechSheet.ZoneRow z : sheet.zones()) {
                if (z.garment() != g) {
                    continue;
                }
                Rectangle sw = new Rectangle(14, 14, color(z.hex()));
                sw.setArcWidth(4);
                sw.setArcHeight(4);
                sw.getStyleClass().add("sf-zone-swatch");
                Label hex = new Label(z.hex());
                hex.getStyleClass().addAll("code", "ink");
                grid.addRow(r++, sw, text(z.zone().label()), hex);
            }
            Label head = new Label(g.label());
            head.getStyleClass().addAll("body-s", "ink", "sf-strong");
            box.getChildren().add(new VBox(6, head, grid));
        }
        return section("COLORES POR ZONA", box);
    }

    private Node materials(TechSheet sheet) {
        GridPane grid = grid();
        int r = 0;
        for (TechSheet.MaterialRow m : sheet.materials()) {
            Label code = new Label(m.code());
            code.getStyleClass().addAll("code", "ink-muted");
            grid.addRow(r++, text(m.slot().label()), new VBox(2, text(m.name()), code));
        }
        return section("MATERIALES", grid);
    }

    private Node patterns(TechSheet sheet) {
        GridPane grid = grid();
        int r = 0;
        for (TechSheet.PatternRow p : sheet.patterns()) {
            boolean plain = p.type() == PatternType.LISO;
            Node color = plain ? text("—") : swatchHex(p.color());
            grid.addRow(r++, text(p.garment().label()), text(p.type().label()), color,
                    text(plain || p.type() == PatternType.DEGRADADO ? "" : String.format(Locale.ROOT, "%.1f cm", p.widthCm())));
        }
        return section("PATRONES", grid);
    }

    private Node elements(TechSheet sheet) {
        VBox list = new VBox(10);
        if (sheet.elements().isEmpty()) {
            list.getChildren().add(muted("Sin nombres, números, emblemas ni logos."));
        }
        // La capa de arriba primero, como en el diseñador.
        for (int i = sheet.elements().size() - 1; i >= 0; i--) {
            TechSheet.ElementRow e = sheet.elements().get(i);
            String id = design.document().elements().get(e.layer() - 1).id();
            Label head = new Label("Capa " + e.layer() + " · " + e.type().label() + " · " + e.content());
            head.getStyleClass().addAll("body-s", "ink", "sf-strong");
            head.setWrapText(true);
            GridPane grid = grid();
            int r = 0;
            grid.addRow(r++, muted("Zona"), text(e.area().label() + (e.visible() ? "" : " (oculto)")));
            grid.addRow(r++, muted("Posición"), mono(String.format(Locale.ROOT, "u %.3f · v %.3f", e.u(), e.v())));
            grid.addRow(r++, muted("Escala / giro"), mono(String.format(Locale.ROOT, "%.0f %% · %.0f°", e.scalePercent(), e.rotationDeg())));
            grid.addRow(r++, muted("Alto"), mono(String.format(Locale.ROOT, "%.1f cm", e.heightCm())));
            if (e.font() != null) {
                grid.addRow(r++, muted("Tipografía"), text(e.font()));
            }
            if (e.color() != null) {
                grid.addRow(r++, muted(e.type().isText() ? "Color" : "Color principal"), swatchHex(e.color()));
            }
            if (e.accentColor() != null && (!e.type().isText() || e.outlineMm() > 0)) {
                grid.addRow(r++, muted(e.type().isText() ? "Contorno" : "Acento"), e.type().isText()
                        ? new HBox(8, swatchHex(e.accentColor()), mono(String.format(Locale.ROOT, "%.1f mm", e.outlineMm())))
                        : swatchHex(e.accentColor()));
            }
            if (e.assetName() != null) {
                grid.addRow(r++, muted("Archivo"), text(e.assetName()));
            }
            VBox card = new VBox(8, head, grid);
            card.getStyleClass().add("sf-layer-row");
            card.setStyle("-fx-padding: 12;");
            card.setOnMouseClicked(x -> focusElement(id, e.area()));
            list.getChildren().add(card);
        }
        return section("ELEMENTOS (clic para ubicarlo en el 3D)", list);
    }

    /** Resalta el elemento en el 3D con el contorno de foco y gira la cámara hacia él. */
    private void focusElement(String id, Area area) {
        String old = renderer.composer().highlight();
        renderer.composer().setHighlight(id);
        Set<Garment> garments = EnumSet.of(area.garment());
        design.document().element(old == null ? "" : old).ifPresent(e -> garments.add(e.area().garment()));
        renderer.applyGarments(design.document(), garments);
        viewer.jumpTo(area == Area.JERSEY_BACK ? ViewPreset.BACK : ViewPreset.FRONT, area.garment());
    }

    private void download() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Elige dónde guardar los archivos del uniforme");
        File dir = chooser.showDialog(stage.getScene().getWindow());
        if (dir == null) {
            return;
        }
        long itemId = design.item().id();
        Async.run(() -> context.adminOrderService().exportDesign(itemId, dir.toPath()),
                folder -> Toast.success(stage, "Archivos guardados en " + folder.getFileName()),
                e -> Toast.error(stage, ErrorMessages.of(e)));
    }

    // ---------------------------------------------------------------- Utilidades

    private static VBox section(String title, Node body) {
        Label l = new Label(title);
        l.getStyleClass().addAll("label-text", "ink-muted");
        return new VBox(10, l, body);
    }

    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(6);
        return g;
    }

    private static Node swatchHex(String hex) {
        Rectangle sw = new Rectangle(14, 14, color(hex));
        sw.setArcWidth(4);
        sw.setArcHeight(4);
        sw.getStyleClass().add("sf-zone-swatch");
        HBox box = new HBox(8, sw, mono(hex));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Label text(String s) {
        Label l = new Label(s);
        l.getStyleClass().addAll("body-s", "ink");
        l.setWrapText(true);
        return l;
    }

    private static Label mono(String s) {
        Label l = new Label(s);
        l.getStyleClass().addAll("code", "ink");
        return l;
    }

    private static Label muted(String s) {
        Label l = new Label(s);
        l.getStyleClass().addAll("body-s", "ink-muted");
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    private static Color color(String hex) {
        try {
            return Color.web(hex);
        } catch (RuntimeException e) {
            return Color.GRAY;
        }
    }
}
