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
import com.sportforce3d.design.RealisticSpec;
import com.sportforce3d.design.RealisticView;
import com.sportforce3d.design.ViewSync;
import com.sportforce3d.design.TechSheet;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.OrderItem;
import com.sportforce3d.render.RealisticModel;
import com.sportforce3d.render.RealisticModelRepository;
import com.sportforce3d.render.RealisticPattern;
import com.sportforce3d.service.AdminOrderService.OrderDesign;
import com.sportforce3d.three.DesignRenderer;
import com.sportforce3d.three.ElementImages;
import com.sportforce3d.three.TextureComposer;
import com.sportforce3d.three.ViewPreset;
import com.sportforce3d.three.Viewer3D;
import com.sportforce3d.three.human.HumanModels;
import com.sportforce3d.three.human.OutfitFitter;
import com.sportforce3d.three.human.Persona;
import com.sportforce3d.three.model.ModelProviders;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Administración → Pedidos → uniforme personalizado, de solo lectura: la vista realista (fotos recoloreadas con la
 * copia del diseño) y el mismo visor 3D del diseñador, con las capturas del pedido, la ficha técnica por zona (también
 * la de la vista realista, en HEX) y la descarga de los archivos originales. Parámetro: id de la línea.
 */
public class AdminDesignController implements Router.Routable {

    private final AppContext context;
    private final Viewer3D viewer = new Viewer3D();
    private final DesignRenderer renderer;
    private final RealisticPreview preview = new RealisticPreview(150);
    private final HBox viewSwitch = new HBox(4);
    private final Region switchSep = new Region();
    private final List<Node> viewerButtons = new ArrayList<>();
    private final ToggleGroup viewGroup = new ToggleGroup();
    private final ElementImages images;
    private OrderDesign design;
    /** true si el visor muestra a la persona vestida con el corte del pedido (no el maniquí). */
    private boolean dressed;
    private long orderId;

    @FXML private StackPane stage;
    @FXML private HBox toolbar;
    @FXML private VBox loadingBox;
    @FXML private VBox panel;
    @FXML private SfLink backLink;
    @FXML private Label hint;

    public AdminDesignController(AppContext context) {
        this.context = context;
        this.images = new ElementImages(context.storage());
        this.renderer = new DesignRenderer(viewer, new TextureComposer(images));
    }

    @FXML
    private void initialize() {
        stage.getChildren().add(0, viewer);
        stage.getChildren().add(1, preview.node());
        viewer.autoRotateProperty().set(false);
        backLink.setGraphic(Icons.of(Feather.ARROW_LEFT, Icons.SM));
        backLink.setOnAction(e -> context.router().go(Routes.ADMIN_ORDER, orderId));
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setSpacing(4);
        viewSwitch.getStyleClass().add("sf-seg");
        for (boolean real : new boolean[]{true, false}) {
            ToggleButton item = new ToggleButton(real ? "Vista realista" : "Vista 3D");
            item.getStyleClass().setAll("sf-seg-item");
            item.setToggleGroup(viewGroup);
            item.setUserData(real);
            item.setMnemonicParsing(false);
            viewSwitch.getChildren().add(item);
        }
        viewGroup.selectedToggleProperty().addListener((obs, old, t) -> {
            if (t == null) {
                viewGroup.selectToggle(old);
            } else {
                showRealistic((Boolean) t.getUserData());
            }
        });
        switchSep.getStyleClass().add("sf-toolbar-sep");
        toolbar.getChildren().addAll(viewSwitch, switchSep);
        for (ViewPreset preset : ViewPreset.values()) {
            SfButton b = new SfButton(preset.label(), SfButton.Variant.GHOST, SfButton.Size.SM);
            b.getStyleClass().add("sf-toolbar-btn");
            b.setOnAction(e -> viewer.showView(preset));
            toolbar.getChildren().add(b);
            viewerButtons.add(b);
        }
        SfButton reset = SfButton.icon(Feather.MAXIMIZE, "Cuerpo completo");
        reset.setOnAction(e -> viewer.resetView());
        toolbar.getChildren().add(reset);
        viewerButtons.add(reset);
        showRealistic(false);
    }

    /** Vista realista (si el diseño la tiene) o visor 3D. */
    private void showRealistic(boolean real) {
        preview.node().setVisible(real);
        viewer.setVisible(!real);
        for (Node n : viewerButtons) {
            n.setVisible(!real);
            n.setManaged(!real);
        }
        boolean sep = !real && viewSwitch.isManaged();
        switchSep.setVisible(sep);
        switchSep.setManaged(sep);
        hint.setText(real
                ? "Solo lectura · copia del diseño tal como se compró · vista realista dibujada con sus colores y textos"
                : "Solo lectura · copia del diseño tal como se compró · arrastra para girar, rueda para acercar");
    }

    @Override
    public void onRoute(Object param) {
        if (!(param instanceof Long itemId)) {
            context.router().go(Routes.ADMIN_ORDERS);
            return;
        }
        // La persona del diseño vestida con el corte del pedido (manga, largos) y sus telas; si falta su modelo, el
        // maniquí (que solo tiene el corte de serie: la ficha lo avisa).
        Async.run(() -> {
            OrderDesign d = context.adminOrderService().design(itemId);
            var doc = d.document();
            Persona p = Persona.forDesign(doc.realisticOrDefault().modelKey(), doc.bodyType());
            Object model;
            try {
                model = HumanModels.available(p) ? HumanModels.dress(p, doc.cut()) : null;
            } catch (RuntimeException e) {
                model = null;
            }
            return new Object[]{d, model != null ? model : ModelProviders.load(doc.bodyType())};
        }, result -> {
            design = (OrderDesign) result[0];
            orderId = design.orderId();
            dressed = result[1] instanceof OutfitFitter.DressedPerson;
            if (dressed) {
                viewer.setDressedPerson((OutfitFitter.DressedPerson) result[1]);
            } else {
                viewer.setModel((com.sportforce3d.three.model.ModelData) result[1]);
            }
            renderer.applyAll(design.document(), design.materials());
            loadingBox.setVisible(false);
            showRealisticView();
            buildPanel();
        }, e -> {
            loadingBox.setVisible(false);
            panel.getChildren().setAll(backLink, new EmptyState(Feather.ALERT_TRIANGLE, "No se pudo abrir el uniforme",
                    ErrorMessages.of(e)));
        });
    }

    /**
     * Con vista realista guardada (diseños desde la Etapa 7) se abre en ella y se dibuja con la copia del pedido;
     * los diseños anteriores solo tienen el visor 3D y el selector no aparece.
     */
    private void showRealisticView() {
        // La foto se calcula del mismo diseño que el 3D (Etapa 11). Si no puede mostrarlo todo (otro corte, tela,
        // textos movidos…), se ve solo el 3D: el taller no recibe una imagen distinta a lo comprado.
        RealisticView view = ViewSync.view(design.document());
        boolean show = design.document().realistic() != null && view.notices().isEmpty();
        viewSwitch.setVisible(show);
        viewSwitch.setManaged(show);
        if (!show) {
            showRealistic(false);
            return;
        }
        viewGroup.getToggles().stream().filter(t -> Boolean.TRUE.equals(t.getUserData())).findFirst()
                .ifPresent(viewGroup::selectToggle);
        RealisticPreview.Crest crest = RealisticPreview.crest(images, view.crest());
        Async.run(() -> RealisticPreview.render(context.recolorService(), view, crest),
                imgs -> preview.show(imgs[0], imgs[1]),
                e -> preview.showMessage("No se pudo dibujar la vista realista. " + ErrorMessages.of(e)));
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
                new VBox(8, download, filesNote), realistic(sheet), cut(sheet), zones(sheet), materials(sheet),
                patterns(sheet), elements(sheet));
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

    /** Ficha de la vista realista: modelo, color HEX por zona, diseño, nombre y número. */
    private Node realistic(TechSheet sheet) {
        TechSheet.Realistic r = sheet.realistic();
        if (r == null) {
            return section("VISTA REALISTA", muted("Diseño anterior a la vista realista: solo tiene el visor 3D."));
        }
        RealisticModelRepository models = context.recolorService().models();
        RealisticModel model = models.find(r.model()).orElse(null);
        GridPane grid = grid();
        int row = 0;
        String modelLabel = model == null ? r.model()
                : RealisticDesigner.CUTS.getOrDefault(model.cut(), model.cut()) + " · "
                + RealisticDesigner.CATEGORIES.getOrDefault(model.audience(), String.valueOf(model.audience()));
        grid.addRow(row++, muted("Modelo"), new VBox(2, text(modelLabel), mono(r.model())));
        // Solo las zonas que el modelo usa, en su orden (el diseño guarda también las de otros cortes).
        List<String> order = model == null ? new ArrayList<>(r.zones().keySet()) : model.zones();
        for (String z : order) {
            String hex = r.zones().get(z);
            if (hex != null) {
                grid.addRow(row++, muted(model == null ? z : RealisticDesigner.zoneLabel(model, z)), swatchHex(hex));
            }
        }
        String pattern;
        try {
            pattern = RealisticPattern.fromCode(r.pattern()).label();
        } catch (IllegalArgumentException e) {
            pattern = r.pattern();
        }
        grid.addRow(row++, muted("Diseño"), r.patternColor() == null ? text(pattern)
                : new HBox(10, text(pattern), swatchHex(r.patternColor())));
        grid.addRow(row++, muted("Nombre"), r.name() == null ? text("—") : mono(r.name()));
        grid.addRow(row++, muted("Número"), r.number() == null ? text("—") : mono(String.valueOf(r.number())));
        if (!design.document().realisticPhotosMatch()) {
            // Otro corte u otra tela: las fotos realistas no lo muestran; el pedido se fabrica según el 3D.
            Label note = muted("Solo referencia: la foto no muestra todo este uniforme ("
                    + String.join(", ", com.sportforce3d.design.ViewSync.view(design.document()).notices()
                    .stream().map(n -> n.replace(": ver en Vista 3D", "").toLowerCase()).toList())
                    + "). Se fabrica según la Vista 3D, las capturas del pedido y las secciones de abajo.");
            note.setWrapText(true);
            return section("VISTA REALISTA", new VBox(8, note, grid));
        }
        return section("VISTA REALISTA", grid);
    }

    /** Manga y largos (Etapa 10): el visor 3D muestra a la persona con este corte. */
    private Node cut(TechSheet sheet) {
        GridPane grid = grid();
        grid.addRow(0, muted("Manga"), text(sheet.cut().sleeve().label()));
        grid.addRow(1, muted("Short"), text(sheet.cut().shorts().label() + " (" + sheet.cut().shorts().detail() + ")"));
        grid.addRow(2, muted("Medias"), text(sheet.cut().socks().label() + " (" + sheet.cut().socks().detail() + ")"));
        Label note = muted(dressed ? "El visor 3D y las capturas del pedido muestran este corte."
                : "Sin el modelo de la persona, el visor 3D muestra el maniquí con el corte de serie; vale esta ficha.");
        note.setWrapText(true);
        return section("CORTE", new VBox(8, grid, note));
    }

    private Node zones(TechSheet sheet) {
        VBox box = new VBox(14);
        for (Garment g : List.of(Garment.JERSEY, Garment.SHORTS, Garment.SOCKS, Garment.SHOES)) {
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
        return section("COLORES POR ZONA (VISOR 3D)", box);
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
