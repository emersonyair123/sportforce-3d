package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Router;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.ColorSwatchGroup;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfLink;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.Category;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.model.ProductForm;
import com.sportforce3d.model.ProductImage;
import com.sportforce3d.model.ProductVariant;
import com.sportforce3d.model.RealisticLook;
import com.sportforce3d.model.Size;
import com.sportforce3d.model.Sport;
import com.sportforce3d.util.Money;
import com.sportforce3d.util.Texts;
import com.sportforce3d.viewmodel.AdminProductEditViewModel;
import com.sportforce3d.viewmodel.AdminProductEditViewModel.ReferenceData;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AdminProductEditController implements Router.Routable {

    /** Anchos de las columnas del editor de variantes (cabecera y filas). */
    private static final double[] COLS = {110, 150, 110, 170, 90, 80, 70, 70, 40};

    private final AppContext context;
    private final AdminProductEditViewModel viewModel;

    @FXML private SfLink backLink;
    @FXML private Label title;
    @FXML private VBox content;

    private final SfTextField name = new SfTextField("Nombre");
    private final SfTextField sku = new SfTextField("SKU");
    private final ComboBox<Category> category = new ComboBox<>();
    private final ComboBox<Sport> sport = new ComboBox<>();
    private final ComboBox<Garment> garment = new ComboBox<>();
    private final SfTextField price = new SfTextField("Precio (S/)");
    private final SfTextField compareAt = new SfTextField("Precio anterior (opcional)");
    private final TextArea description = new TextArea();
    private final CheckBox active = new CheckBox("Activo (se vende en la tienda)");
    private final CheckBox customizable = new CheckBox("Personalizable en el diseñador 3D");
    private final CheckBox isNew = new CheckBox("Marcar como nuevo");
    private final CheckBox featured = new CheckBox("Destacado en el inicio");
    private final Label customizableNote = note("Los buzos y accesorios no se personalizan en el diseñador 3D: "
            + "se compran con su color y talla.");
    private final FlowPane materialsPane = new FlowPane(16, 8);
    private final Label materialsError = errorLabel();
    private final Label generalError = errorLabel();
    private final Label categoryError = errorLabel();
    private final Label variantsError = errorLabel();
    private final VBox variantRows = new VBox(8);
    private final FlowPane imagesPane = new FlowPane(12, 12);
    /** Foto realista: miniatura y un color por zona (Etapa 12B: p. ej. la suela de la zapatilla de running). */
    private final VBox lookCard = new VBox();
    private final ImageView lookPreview = new ImageView();
    private final VBox lookZones = new VBox(16);
    private final Label lookError = errorLabel();
    private final Map<String, ColorSwatchGroup> zoneInputs = new java.util.LinkedHashMap<>();
    private final SfButton saveButton = new SfButton("Guardar producto", SfButton.Variant.ACCENT);
    private final List<VariantRow> rows = new ArrayList<>();
    private final Map<Long, CheckBox> materialChecks = new java.util.LinkedHashMap<>();
    private ReferenceData reference;

    public AdminProductEditController(AppContext context) {
        this.context = context;
        this.viewModel = new AdminProductEditViewModel(context.adminProductService(), context.catalogService());
    }

    @FXML
    private void initialize() {
        backLink.setGraphic(Icons.of(Feather.ARROW_LEFT, Icons.SM));
        backLink.setOnAction(e -> context.router().go(Routes.ADMIN_PRODUCTS));
        viewModel.referenceProperty().addListener((obs, old, data) -> {
            reference = data;
            buildForm();
        });
        viewModel.productProperty().addListener((obs, old, p) -> {
            if (reference != null) {
                fill(p);
            }
        });
    }

    @Override
    public void onRoute(Object param) {
        viewModel.load(param instanceof Long id ? id : null,
                message -> content.getChildren().setAll(new EmptyState(Feather.LOCK, "No se pudo abrir el producto", message)));
    }

    // ------------------------------------------------------------------ Construcción

    private void buildForm() {
        category.setItems(FXCollections.observableArrayList(reference.categories()));
        category.setConverter(converter(Category::name));
        category.setMaxWidth(Double.MAX_VALUE);
        category.setPromptText("Elige…");
        List<Sport> sports = new ArrayList<>();
        sports.add(null);
        sports.addAll(reference.sports());
        sport.setItems(FXCollections.observableArrayList(sports));
        sport.setConverter(converter(s -> s == null ? "Sin deporte" : s.name()));
        sport.setMaxWidth(Double.MAX_VALUE);
        garment.setItems(FXCollections.observableArrayList(Garment.values()));
        garment.setConverter(converter(Garment::label));
        garment.setMaxWidth(Double.MAX_VALUE);
        garment.valueProperty().addListener((obs, old, g) -> {
            refreshCustomizable();
            renderMaterials();
            rows.forEach(VariantRow::refreshSizes);
        });

        sku.setMonospace(true);
        sku.setPromptText("SF-CAM-010");
        sku.getInput().textProperty().addListener((obs, old, v) -> {
            if (v != null && !v.equals(v.toUpperCase())) {
                sku.getInput().setText(v.toUpperCase());
            }
        });
        price.setPromptText("79.90");
        compareAt.setPromptText("99.90");
        compareAt.setHint("Si lo llenas, la tienda muestra el descuento.");
        description.setPrefRowCount(3);
        description.setWrapText(true);
        description.getStyleClass().add("sf-textarea");
        description.setPromptText("Tejido transpirable con secado rápido.");

        VBox general = card("Datos del producto",
                grid(name, sku),
                grid(field("CATEGORÍA", category, categoryError), field("DEPORTE", sport, null),
                        field("TIPO DE PRENDA", garment, null)),
                grid(price, compareAt),
                field("DESCRIPCIÓN", description, null),
                new FlowPane(24, 8, active, customizable, isNew, featured),
                customizableNote,
                generalError);

        VBox materials = card("Telas y materiales que se ofrecen", note(
                "Solo se muestran los materiales válidos para el tipo de prenda. El recargo se suma al precio en el diseñador 3D."),
                materialsPane, materialsError);

        SfButton addVariant = new SfButton("Agregar variante", SfButton.Variant.GHOST, SfButton.Size.SM).withIcon(Feather.PLUS);
        addVariant.setOnAction(e -> addRow(null));
        SfButton allSizes = new SfButton("Agregar todas las tallas", SfButton.Variant.GHOST, SfButton.Size.SM)
                .withIcon(Feather.LAYERS);
        allSizes.setOnAction(e -> addAllSizes());
        VBox variants = card("Variantes, precio y stock",
                note("Cada variante es una talla en un color. \"Ajuste\" se suma al precio base (p. ej. 10.00 para tallas XXL). "
                        + "El stock no puede ser menor que lo reservado por pedidos sin pagar."),
                header(), variantRows, new HBox(8, addVariant, allSizes), variantsError);

        lookPreview.setFitWidth(160);
        lookPreview.setFitHeight(200);
        lookPreview.setPreserveRatio(true);
        StackPane lookFrame = new StackPane(lookPreview);
        lookFrame.getStyleClass().add("sf-thumb");
        lookFrame.setMinSize(160, 200);
        lookFrame.setMaxSize(160, 200);
        HBox.setHgrow(lookZones, Priority.ALWAYS);
        HBox lookBody = new HBox(24, lookFrame, lookZones);
        VBox lookBox = card("Foto realista", note("Color de cada zona de la foto del catálogo. Sin imágenes subidas, "
                + "la tienda muestra esta foto. Los cambios se aplican al guardar."), lookBody, lookError);
        lookCard.getChildren().setAll(lookBox);

        VBox images = card("Imágenes", note("PNG o JPG de hasta 5 MB. La primera es la portada. Sin imágenes, la tienda muestra la ilustración de la prenda."),
                imagesPane);

        saveButton.withIcon(Feather.SAVE);
        saveButton.disableProperty().bind(viewModel.savingProperty());
        saveButton.setOnAction(e -> save());
        SfButton cancel = new SfButton("Cancelar", SfButton.Variant.GHOST);
        cancel.setOnAction(e -> context.router().go(Routes.ADMIN_PRODUCTS));
        HBox actions = new HBox(12, cancel, saveButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().setAll(general, materials, variants, lookCard, images, actions);
        fill(viewModel.productProperty().get());
    }

    private void fill(ProductDetail p) {
        title.setText(p == null ? "NUEVO PRODUCTO" : Texts.upper(p.name()));
        clearErrors();
        name.setText(p == null ? "" : p.name());
        sku.setText(p == null ? "" : p.sku());
        category.setValue(p == null ? null : reference.categories().stream()
                .filter(c -> c.id() == p.categoryId()).findFirst().orElse(null));
        sport.setValue(p == null || p.sportId() == null ? null : reference.sports().stream()
                .filter(s -> s.id() == p.sportId()).findFirst().orElse(null));
        garment.setValue(p == null ? Garment.JERSEY : p.garment());
        price.setText(p == null ? "" : Money.toInput(p.basePriceCents()));
        compareAt.setText(p == null || p.compareAtPriceCents() == null ? "" : Money.toInput(p.compareAtPriceCents()));
        description.setText(p == null || p.description() == null ? "" : p.description());
        active.setSelected(p == null || p.active());
        customizable.setSelected(p != null && p.customizable());
        refreshCustomizable();
        isNew.setSelected(p == null || p.isNew());
        featured.setSelected(p != null && p.featured());

        renderMaterials();
        Set<Long> selected = new HashSet<>();
        if (p != null) {
            p.materials().forEach(m -> selected.add(m.id()));
        }
        materialChecks.forEach((id, check) -> check.setSelected(selected.contains(id)));

        rows.clear();
        variantRows.getChildren().clear();
        if (p != null) {
            p.variants().forEach(this::addRow);
        } else {
            addRow(null);
        }
        renderImages(p);
        renderLook(viewModel.lookProperty().get());
    }

    private void renderLook(RealisticLook look) {
        zoneInputs.clear();
        lookZones.getChildren().clear();
        boolean has = look != null && !look.zoneColors().isEmpty();
        lookCard.setVisible(has);
        lookCard.setManaged(has);
        if (!has) {
            return;
        }
        for (Map.Entry<String, String> e : look.zoneColors().entrySet()) {
            ColorSwatchGroup group = new ColorSwatchGroup();
            group.setValue(Color.web(e.getValue()));
            group.valueProperty().addListener((obs, old, c) -> paintLook(look));
            zoneInputs.put(e.getKey(), group);
            Label label = new Label(Texts.upper(zoneLabel(look.modelKey(), e.getKey())));
            label.getStyleClass().addAll("label-text", "ink-muted");
            lookZones.getChildren().add(new VBox(8, label, group));
        }
        paintLook(look);
    }

    /** Miniatura con los colores elegidos (aún sin guardar). */
    private void paintLook(RealisticLook saved) {
        RealisticLook shown = new RealisticLook(saved.modelKey(), zoneColors(saved), saved.pattern(), saved.patternColor());
        ProductImages.realistic(context.recolorService(), shown, lookPreview::setImage);
    }

    /** Colores por zona del formulario (las zonas sin color válido conservan el guardado). */
    private Map<String, String> zoneColors(RealisticLook saved) {
        Map<String, String> out = new java.util.LinkedHashMap<>(saved.zoneColors());
        zoneInputs.forEach((zone, group) -> {
            if (group.getValue() != null) {
                out.put(zone, com.sportforce3d.design.DesignDefaults.hex(group.getValue()).toLowerCase(java.util.Locale.ROOT));
            }
        });
        return out;
    }

    static String zoneLabel(String modelKey, String zone) {
        boolean shoe = modelKey != null && modelKey.startsWith("producto_zapatilla");
        return switch (zone) {
            case "camiseta" -> "Camiseta";
            case "short" -> "Short";
            case "medias" -> "Medias";
            case "paneles" -> shoe ? "Suela" : "Paneles";
            case "principal" -> shoe ? "Capellada" : "Color principal";
            default -> zone;
        };
    }

    /** Buzos y accesorios no se personalizan (el carrito no compra su diseño): la casilla queda apagada. */
    private void refreshCustomizable() {
        boolean allowed = garment.getValue() == null || garment.getValue().designable();
        if (!allowed) {
            customizable.setSelected(false);
        }
        customizable.setDisable(!allowed);
        customizableNote.setVisible(!allowed);
        customizableNote.setManaged(!allowed);
    }

    private void renderMaterials() {
        Set<Long> keep = new HashSet<>();
        materialChecks.forEach((id, c) -> {
            if (c.isSelected()) {
                keep.add(id);
            }
        });
        materialChecks.clear();
        materialsPane.getChildren().clear();
        List<Material> list = garment.getValue() == Garment.SHOES ? reference.shoeMaterials() : reference.textiles();
        for (Material m : list) {
            CheckBox check = new CheckBox(m.name() + " · " + Money.surcharge(m.surchargeCents()));
            check.setSelected(keep.contains(m.id()));
            materialChecks.put(m.id(), check);
            materialsPane.getChildren().add(check);
        }
    }

    private void renderImages(ProductDetail p) {
        imagesPane.getChildren().clear();
        if (p == null) {
            imagesPane.getChildren().add(note("Guarda el producto para poder agregar imágenes."));
            return;
        }
        for (ProductImage img : p.images()) {
            StackPane thumb = new StackPane();
            thumb.getStyleClass().add("sf-thumb");
            thumb.setMinSize(96, 120);
            thumb.setMaxSize(96, 120);
            Image image = ProductImages.load(context.storage(), img.path(), 96, 120);
            if (image != null) {
                ImageView view = new ImageView(image);
                view.setFitWidth(96);
                view.setFitHeight(120);
                view.setPreserveRatio(true);
                thumb.getChildren().add(view);
            }
            SfButton remove = SfButton.icon(Feather.TRASH_2, "Quitar imagen");
            remove.setOnAction(e -> viewModel.removeImage(img.id(), message -> Toast.error(content, message)));
            StackPane.setAlignment(remove, Pos.TOP_RIGHT);
            thumb.getChildren().add(remove);
            imagesPane.getChildren().add(thumb);
        }
        SfButton add = new SfButton("Agregar imagen", SfButton.Variant.GHOST).withIcon(Feather.UPLOAD);
        add.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Elegir imagen del producto");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes PNG o JPG", "*.png", "*.jpg", "*.jpeg"));
            File file = chooser.showOpenDialog(content.getScene().getWindow());
            if (file != null) {
                viewModel.addImage(file.toPath(), message -> Toast.error(content, message));
            }
        });
        imagesPane.getChildren().add(add);
    }

    // ------------------------------------------------------------------ Variantes

    private final class VariantRow extends HBox {
        final Long id;
        final ComboBox<Size> size = new ComboBox<>();
        final TextField colorName = new TextField();
        final TextField colorHex = new TextField();
        final Rectangle preview = new Rectangle(18, 18);
        final TextField variantSku = new TextField();
        final TextField delta = new TextField();
        final TextField stock = new TextField();
        final TextField threshold = new TextField();
        final CheckBox enabled = new CheckBox();
        final Label reserved = new Label();
        private boolean skuTouched;

        VariantRow(ProductVariant v, VariantRow template) {
            super(8);
            this.id = v == null ? null : v.id();
            setAlignment(Pos.CENTER_LEFT);
            size.setConverter(converter(Size::name));
            refreshSizes();
            size.setValue(v == null ? null : reference.sizes().stream().filter(s -> s.id() == v.sizeId()).findFirst().orElse(null));
            colorName.setText(v != null ? v.colorName() : template != null ? template.colorName.getText() : "Azul eléctrico");
            colorHex.setText(v != null ? v.colorHex() : template != null ? template.colorHex.getText() : "#1F5FFF");
            colorHex.getStyleClass().add("code");
            preview.setArcWidth(6);
            preview.setArcHeight(6);
            colorHex.textProperty().addListener((obs, old, hex) -> paintPreview());
            paintPreview();
            variantSku.setText(v == null ? "" : v.sku());
            variantSku.getStyleClass().add("code");
            skuTouched = v != null;
            variantSku.textProperty().addListener((obs, old, val) -> {
                if (variantSku.isFocused()) {
                    skuTouched = true;
                }
            });
            size.valueProperty().addListener((obs, old, s) -> autoSku());
            sku.textProperty().addListener((obs, old, s) -> autoSku());
            delta.setText(v == null ? "0.00" : Money.toInput(v.priceDeltaCents()));
            stock.setText(v == null ? "0" : String.valueOf(v.quantity()));
            threshold.setText(v == null ? "5" : String.valueOf(v.lowStockThreshold()));
            enabled.setSelected(v == null || v.active());
            enabled.setAccessibleText("Variante activa");
            reserved.getStyleClass().addAll("body-s", "ink-subtle");
            reserved.setText(v == null || v.reserved() == 0 ? "" : v.reserved() + " res.");

            size.setAccessibleText("Talla");
            colorName.setAccessibleText("Nombre del color");
            colorHex.setAccessibleText("Color HEX");
            variantSku.setAccessibleText("SKU de la variante");
            delta.setAccessibleText("Ajuste de precio en soles");
            stock.setAccessibleText("Stock");
            threshold.setAccessibleText("Aviso de stock bajo");

            Node last;
            if (id == null) {
                SfButton remove = SfButton.icon(Feather.X, "Quitar variante");
                remove.setOnAction(e -> {
                    rows.remove(this);
                    variantRows.getChildren().remove(this);
                });
                last = remove;
            } else {
                last = reserved;
            }
            HBox hex = new HBox(6, preview, colorHex);
            hex.setAlignment(Pos.CENTER_LEFT);
            Node[] cells = {size, colorName, hex, variantSku, delta, stock, threshold, enabled, last};
            for (int i = 0; i < cells.length; i++) {
                if (cells[i] instanceof Region r) {
                    r.setPrefWidth(COLS[i]);
                    r.setMinWidth(COLS[i]);
                    r.setMaxWidth(COLS[i]);
                }
                if (cells[i] instanceof TextField tf) {
                    tf.getStyleClass().add("sf-input-sm");
                }
            }
            colorHex.setPrefWidth(COLS[2] - 24);
            colorHex.setMinWidth(COLS[2] - 24);
            getChildren().addAll(cells);
            autoSku();
        }

        void refreshSizes() {
            String group = switch (garment.getValue() == null ? Garment.JERSEY : garment.getValue()) {
                case SHOES -> "CALZADO";
                case SOCKS -> "MEDIAS";
                case ACCESSORY -> "UNICA";
                default -> "ROPA";
            };
            Size current = size.getValue();
            List<Size> options = reference.sizes().stream().filter(s -> s.group().equals(group)).toList();
            size.setItems(FXCollections.observableArrayList(options));
            if (current != null && options.contains(current)) {
                size.setValue(current);
            } else if (current != null && id != null) {
                // Variante existente con talla de otro grupo: se conserva para no perder datos.
                size.getItems().add(current);
                size.setValue(current);
            }
        }

        private void autoSku() {
            if (skuTouched || size.getValue() == null) {
                return;
            }
            String base = sku.getText() == null ? "" : sku.getText().trim();
            variantSku.setText(base.isEmpty() ? "" : base + "-" + size.getValue().code());
        }

        private void paintPreview() {
            try {
                preview.setFill(Color.web(colorHex.getText().trim()));
            } catch (RuntimeException e) {
                preview.setFill(Color.TRANSPARENT);
            }
        }

        ProductForm.VariantForm value() {
            if (size.getValue() == null) {
                throw new IllegalArgumentException("Elige la talla de cada variante.");
            }
            return new ProductForm.VariantForm(id, size.getValue().id(), colorName.getText().trim(),
                    colorHex.getText().trim().toUpperCase(), variantSku.getText().trim().toUpperCase(),
                    parseMoneySigned(delta.getText(), "ajuste de precio"), parseInt(stock.getText(), "stock"),
                    parseInt(threshold.getText(), "aviso de stock bajo"), enabled.isSelected());
        }
    }

    private void addRow(ProductVariant v) {
        VariantRow template = rows.isEmpty() ? null : rows.get(rows.size() - 1);
        VariantRow row = new VariantRow(v, template);
        if (v == null && template != null && template.size.getValue() != null) {
            // Siguiente talla del mismo grupo, para cargar tallas rápido.
            List<Size> options = row.size.getItems();
            int next = options.indexOf(template.size.getValue()) + 1;
            if (next > 0 && next < options.size()) {
                row.size.setValue(options.get(next));
            }
        }
        rows.add(row);
        variantRows.getChildren().add(row);
    }

    private void addAllSizes() {
        VariantRow template = rows.isEmpty() ? null : rows.get(rows.size() - 1);
        String hex = template == null ? "#1F5FFF" : template.colorHex.getText().trim().toUpperCase();
        VariantRow probe = new VariantRow(null, template);
        for (Size s : probe.size.getItems()) {
            boolean exists = rows.stream().anyMatch(r -> Objects.equals(r.size.getValue(), s)
                    && r.colorHex.getText().trim().equalsIgnoreCase(hex));
            if (!exists) {
                VariantRow row = new VariantRow(null, template);
                row.size.setValue(s);
                rows.add(row);
                variantRows.getChildren().add(row);
            }
        }
    }

    private Node header() {
        String[] titles = {"TALLA", "COLOR", "HEX", "SKU", "AJUSTE S/", "STOCK", "AVISO", "ACTIVA", ""};
        HBox header = new HBox(8);
        for (int i = 0; i < titles.length; i++) {
            Label l = new Label(titles[i]);
            l.getStyleClass().add("sf-field-label");
            l.setPrefWidth(COLS[i]);
            l.setMinWidth(COLS[i]);
            header.getChildren().add(l);
        }
        return header;
    }

    // ------------------------------------------------------------------ Guardado

    private void save() {
        clearErrors();
        ProductForm form;
        try {
            form = buildForm(viewModel.productProperty().get());
        } catch (IllegalArgumentException e) {
            generalError.setText(e.getMessage());
            Toast.error(content, e.getMessage());
            return;
        }
        boolean isNewProduct = form.id() == null;
        RealisticLook look = viewModel.lookProperty().get();
        Map<String, String> zones = look == null || isNewProduct ? Map.of() : zoneColors(look);
        viewModel.save(form, zones.equals(look == null ? null : look.zoneColors()) ? Map.of() : zones,
                saved -> {
                    Toast.success(content, isNewProduct ? "Producto creado. Ahora puedes agregarle imágenes." : "Cambios guardados.");
                    context.refreshCartCount();
                },
                this::showErrors,
                message -> Toast.error(content, message));
    }

    private ProductForm buildForm(ProductDetail current) {
        if (category.getValue() == null) {
            categoryError.setText("Elige una categoría.");
            throw new IllegalArgumentException("Elige una categoría.");
        }
        long base = parseMoney(price.getText(), "precio");
        Long compare = compareAt.getText() == null || compareAt.getText().isBlank() ? null
                : parseMoney(compareAt.getText(), "precio anterior");
        List<ProductForm.VariantForm> variants = new ArrayList<>();
        for (VariantRow row : rows) {
            variants.add(row.value());
        }
        Set<Long> materials = new HashSet<>();
        materialChecks.forEach((id, c) -> {
            if (c.isSelected()) {
                materials.add(id);
            }
        });
        return new ProductForm(current == null ? null : current.id(), category.getValue().id(),
                sport.getValue() == null ? null : sport.getValue().id(), sku.getText().trim().toUpperCase(),
                name.getText() == null ? "" : name.getText().trim(), Texts.searchKey(description.getText()).isEmpty()
                ? null : description.getText().trim(), garment.getValue(), base, compare, customizable.isSelected(),
                isNew.isSelected(), featured.isSelected(), active.isSelected(), materials, variants);
    }

    private void showErrors(Map<String, String> errors) {
        name.setError(errors.get("name"));
        sku.setError(errors.get("sku"));
        price.setError(errors.get("price"));
        compareAt.setError(errors.get("compareAt"));
        materialsError.setText(errors.getOrDefault("materials", ""));
        variantsError.setText(errors.getOrDefault("variants", ""));
        generalError.setText(errors.getOrDefault("description", errors.getOrDefault("garment",
                errors.getOrDefault("customizable", ""))));
        lookError.setText(errors.getOrDefault("zones", ""));
        Toast.error(content, "Revisa los campos marcados.");
    }

    private void clearErrors() {
        for (SfTextField f : new SfTextField[]{name, sku, price, compareAt}) {
            f.setError("");
        }
        for (Label l : new Label[]{materialsError, variantsError, generalError, categoryError, lookError}) {
            l.setText("");
        }
    }

    private static long parseMoney(String text, String what) {
        try {
            return Money.parse(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Revisa el " + what + ": escribe un monto como 79.90.");
        }
    }

    private static long parseMoneySigned(String text, String what) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return 0;
        }
        boolean negative = t.startsWith("-");
        long value = parseMoney(negative ? t.substring(1) : t, what);
        return negative ? -value : value;
    }

    private static int parseInt(String text, String what) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Revisa el " + what + ": escribe un número entero.");
        }
    }

    // ------------------------------------------------------------------ Ayudas de maquetación

    private static VBox card(String title, Node... children) {
        Label label = new Label(title);
        label.getStyleClass().addAll("title-s", "ink");
        VBox card = new VBox(16, label);
        card.getChildren().addAll(children);
        card.getStyleClass().add("sf-card");
        card.setPadding(new Insets(24));
        return card;
    }

    private static HBox grid(Node... nodes) {
        HBox box = new HBox(16, nodes);
        for (Node n : nodes) {
            HBox.setHgrow(n, Priority.ALWAYS);
            if (n instanceof Region r) {
                r.setPrefWidth(260);
                r.setMaxWidth(Double.MAX_VALUE);
            }
        }
        return box;
    }

    private static VBox field(String label, Node control, Label error) {
        Label l = new Label(label);
        l.getStyleClass().add("sf-field-label");
        VBox box = new VBox(4, l, control);
        if (error != null) {
            box.getChildren().add(error);
        }
        return box;
    }

    private static Label note(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("body-s", "ink-muted");
        label.setWrapText(true);
        return label;
    }

    private static Label errorLabel() {
        Label label = new Label();
        label.getStyleClass().add("sf-field-error");
        label.setWrapText(true);
        label.managedProperty().bind(label.textProperty().isNotEmpty());
        label.visibleProperty().bind(label.managedProperty());
        return label;
    }

    private static <T> StringConverter<T> converter(java.util.function.Function<T, String> toText) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return toText.apply(value);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }
}
