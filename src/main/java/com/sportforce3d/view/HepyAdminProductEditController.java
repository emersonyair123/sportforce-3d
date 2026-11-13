package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhRouter;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PgeyColorSwatchGroup;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfLink;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.HepyCategory;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.model.HepyProductForm;
import com.sportforce3d.model.HepyProductImage;
import com.sportforce3d.model.HepyProductVariant;
import com.sportforce3d.model.HepyRealisticLook;
import com.sportforce3d.model.HepySize;
import com.sportforce3d.model.HepySport;
import com.sportforce3d.util.HepyMoney;
import com.sportforce3d.util.HepyTexts;
import com.sportforce3d.viewmodel.HepyAdminProductEditViewModel;
import com.sportforce3d.viewmodel.HepyAdminProductEditViewModel.HepyReferenceData;
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

public class HepyAdminProductEditController implements PpqhRouter.PpqhRoutable {
    private static final double[] HEPY_COLS = {100, 140, 104, 160, 80, 70, 64, 56, 40};
    private static final List<String> HEPY_ZONE_ORDER = List.of("camiseta", "principal", "short", "paneles", "medias");

    private final PpqhAppContext hepyContext;
    private final HepyAdminProductEditViewModel hepyViewModel;

    @FXML private PpqhSfLink hepyBackLink;
    @FXML private Label hepyTitle;
    @FXML private VBox hepyContent;

    private final PpqhSfTextField hepyName = new PpqhSfTextField("Nombre");
    private final PpqhSfTextField hepySku = new PpqhSfTextField("SKU");
    private final ComboBox<HepyCategory> hepyCategory = new ComboBox<>();
    private final ComboBox<HepySport> hepySport = new ComboBox<>();
    private final ComboBox<HepyGarment> hepyGarment = new ComboBox<>();
    private final PpqhSfTextField hepyPrice = new PpqhSfTextField("Precio (S/)");
    private final PpqhSfTextField hepyCompareAt = new PpqhSfTextField("Precio anterior (opcional)");
    private final TextArea hepyDescription = new TextArea();
    private final CheckBox hepyActive = new CheckBox("Activo (se vende en la tienda)");
    private final CheckBox hepyCustomizable = new CheckBox("Personalizable en el diseñador 3D");
    private final CheckBox hepyIsNew = new CheckBox("Marcar como nuevo");
    private final CheckBox hepyFeatured = new CheckBox("Destacado en el inicio");
    private final Label hepyCustomizableNote = hepyNote("Los buzos y accesorios no se personalizan en el diseñador 3D: "
            + "se compran con su color y talla.");
    private final FlowPane hepyMaterialsPane = new FlowPane(16, 8);
    private final Label hepyMaterialsError = hepyErrorLabel();
    private final Label hepyGeneralError = hepyErrorLabel();
    private final Label hepyCategoryError = hepyErrorLabel();
    private final Label hepyVariantsError = hepyErrorLabel();
    private final VBox hepyVariantRows = new VBox(8);
    private final FlowPane hepyImagesPane = new FlowPane(12, 12);
    private final VBox hepyLookCard = new VBox();
    private final ImageView hepyLookPreview = new ImageView();
    private final VBox hepyLookZones = new VBox(16);
    private final Label hepyLookError = hepyErrorLabel();
    private final Map<String, PgeyColorSwatchGroup> hepyZoneInputs = new java.util.LinkedHashMap<>();
    private final PpqhSfButton hepySaveButton = new PpqhSfButton("Guardar producto", PpqhSfButton.PpqhVariant.ACCENT);
    private final List<HepyVariantRow> hepyRows = new ArrayList<>();
    private final Map<Long, CheckBox> hepyMaterialChecks = new java.util.LinkedHashMap<>();
    private HepyReferenceData hepyReference;

    public HepyAdminProductEditController(PpqhAppContext hepyContext) {
        this.hepyContext = hepyContext;
        this.hepyViewModel = new HepyAdminProductEditViewModel(hepyContext.ppqhAdminProductService(), hepyContext.ppqhCatalogService());
    }

    @FXML
    private void initialize() {
        hepyBackLink.setGraphic(PpqhIcons.ppqhOf(Feather.ARROW_LEFT, PpqhIcons.PPQH_SM));
        hepyBackLink.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_PRODUCTS));
        hepyViewModel.referenceProperty().addListener((hepyObs, hepyOld, hepyData) -> {
            hepyReference = hepyData;
            hepyBuildForm();
        });
        hepyViewModel.productProperty().addListener((hepyObs, hepyOld, hepyP) -> {
            if (hepyReference != null) {
                hepyFill(hepyP);
            }
        });
    }

    @Override
    public void ppqhOnRoute(Object hepyParam) {
        hepyViewModel.hepyLoad(hepyParam instanceof Long hepyId ? hepyId : null,
                hepyMessage -> hepyContent.getChildren().setAll(new PpqhEmptyState(Feather.LOCK, "No se pudo abrir el producto", hepyMessage)));
    }

    private void hepyBuildForm() {
        hepyCategory.setItems(FXCollections.observableArrayList(hepyReference.hepyCategories()));
        hepyCategory.setConverter(hepyConverter(HepyCategory::hepyName, ""));
        hepyCategory.setMaxWidth(Double.MAX_VALUE);
        hepyCategory.setPromptText("Elige…");
        List<HepySport> hepySports = new ArrayList<>();
        hepySports.add(null);
        hepySports.addAll(hepyReference.hepySports());
        hepySport.setItems(FXCollections.observableArrayList(hepySports));
        hepySport.setConverter(hepyConverter(HepySport::hepyName, "Sin deporte"));
        hepySport.setMaxWidth(Double.MAX_VALUE);
        hepyGarment.setItems(FXCollections.observableArrayList(HepyGarment.values()));
        hepyGarment.setConverter(hepyConverter(HepyGarment::hepyLabel, ""));
        hepyGarment.setMaxWidth(Double.MAX_VALUE);
        hepyGarment.valueProperty().addListener((hepyObs, hepyOld, hepyG) -> {
            hepyRefreshCustomizable();
            hepyRenderMaterials();
            hepyRows.forEach(HepyVariantRow::hepyRefreshSizes);
        });

        hepySku.setMonospace(true);
        hepySku.setPromptText("SF-CAM-010");
        hepySku.getInput().textProperty().addListener((hepyObs, hepyOld, hepyV) -> {
            if (hepyV != null && !hepyV.equals(hepyV.toUpperCase())) {
                hepySku.getInput().setText(hepyV.toUpperCase());
            }
        });
        hepyPrice.setPromptText("79.90");
        hepyCompareAt.setPromptText("99.90");
        hepyCompareAt.setHint("Si lo llenas, la tienda muestra el descuento.");
        hepyDescription.setPrefRowCount(3);
        hepyDescription.setWrapText(true);
        hepyDescription.getStyleClass().add("sf-textarea");
        hepyDescription.setPromptText("Tejido transpirable con secado rápido.");

        VBox hepyGeneral = hepyCard("Datos del producto",
                hepyGrid(hepyName, hepySku),
                hepyGrid(hepyField("CATEGORÍA", hepyCategory, hepyCategoryError), hepyField("DEPORTE", hepySport, null),
                        hepyField("TIPO DE PRENDA", hepyGarment, null)),
                hepyGrid(hepyPrice, hepyCompareAt),
                hepyField("DESCRIPCIÓN", hepyDescription, null),
                new FlowPane(24, 8, hepyActive, hepyCustomizable, hepyIsNew, hepyFeatured),
                hepyCustomizableNote,
                hepyGeneralError);

        VBox hepyMaterials = hepyCard("Telas y materiales que se ofrecen", hepyNote(
                "Solo se muestran los materiales válidos para el tipo de prenda. El recargo se suma al precio en el diseñador 3D."),
                hepyMaterialsPane, hepyMaterialsError);

        PpqhSfButton hepyAddVariant = new PpqhSfButton("Agregar variante", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM).ppqhWithIcon(Feather.PLUS);
        hepyAddVariant.setOnAction(hepyE -> hepyAddRow(null));
        PpqhSfButton hepyAllSizes = new PpqhSfButton("Agregar todas las tallas", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM)
                .ppqhWithIcon(Feather.LAYERS);
        hepyAllSizes.setOnAction(hepyE -> hepyAddAllSizes());
        VBox hepyVariants = hepyCard("Variantes, precio y stock",
                hepyNote("Cada variante es una talla en un color. \"Ajuste\" se suma al precio base (p. ej. 10.00 para tallas XXL). "
                        + "El stock no puede ser menor que lo reservado por pedidos sin pagar."),
                hepyHeader(), hepyVariantRows, new HBox(8, hepyAddVariant, hepyAllSizes), hepyVariantsError);

        hepyLookPreview.setFitWidth(160);
        hepyLookPreview.setFitHeight(200);
        hepyLookPreview.setPreserveRatio(true);
        StackPane hepyLookFrame = new StackPane(hepyLookPreview);
        hepyLookFrame.getStyleClass().add("sf-thumb");
        hepyLookFrame.setMinSize(160, 200);
        hepyLookFrame.setMaxSize(160, 200);
        HBox.setHgrow(hepyLookZones, Priority.ALWAYS);
        HBox hepyLookBody = new HBox(24, hepyLookFrame, hepyLookZones);
        VBox hepyLookBox = hepyCard("Foto realista", hepyNote("Color de cada zona de la foto del catálogo. Sin imágenes subidas, "
                + "la tienda muestra esta foto. Los cambios se aplican al guardar."), hepyLookBody, hepyLookError);
        hepyLookCard.getChildren().setAll(hepyLookBox);

        VBox hepyImages = hepyCard("Imágenes", hepyNote("PNG o JPG de hasta 5 MB. La primera es la portada. Sin imágenes, la tienda muestra la foto realista (o, si no tiene, la ilustración de la prenda)."),
                hepyImagesPane);

        hepySaveButton.ppqhWithIcon(Feather.SAVE);
        hepySaveButton.disableProperty().bind(hepyViewModel.savingProperty());
        hepySaveButton.setOnAction(hepyE -> hepySave());
        PpqhSfButton hepyCancel = new PpqhSfButton("Cancelar", PpqhSfButton.PpqhVariant.GHOST);
        hepyCancel.setOnAction(hepyE -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_ADMIN_PRODUCTS));
        HBox hepyActions = new HBox(12, hepyCancel, hepySaveButton);
        hepyActions.setAlignment(Pos.CENTER_RIGHT);

        hepyContent.getChildren().setAll(hepyGeneral, hepyMaterials, hepyVariants, hepyLookCard, hepyImages, hepyActions);
        hepyFill(hepyViewModel.productProperty().get());
    }

    private void hepyFill(HepyProductDetail hepyP) {
        hepyTitle.setText(hepyP == null ? "NUEVO PRODUCTO" : HepyTexts.hepyUpper(hepyP.hepyName()));
        hepyClearErrors();
        hepyName.setText(hepyP == null ? "" : hepyP.hepyName());
        hepySku.setText(hepyP == null ? "" : hepyP.hepySku());
        hepyCategory.setValue(hepyP == null ? null : hepyReference.hepyCategories().stream()
                .filter(hepyC -> hepyC.hepyId() == hepyP.hepyCategoryId()).findFirst().orElse(null));
        hepySport.setValue(hepyP == null || hepyP.hepySportId() == null ? null : hepyReference.hepySports().stream()
                .filter(hepyS -> hepyS.hepyId() == hepyP.hepySportId()).findFirst().orElse(null));
        hepyGarment.setValue(hepyP == null ? HepyGarment.JERSEY : hepyP.hepyGarment());
        hepyPrice.setText(hepyP == null ? "" : HepyMoney.hepyToInput(hepyP.hepyBasePriceCents()));
        hepyCompareAt.setText(hepyP == null || hepyP.hepyCompareAtPriceCents() == null ? "" : HepyMoney.hepyToInput(hepyP.hepyCompareAtPriceCents()));
        hepyDescription.setText(hepyP == null || hepyP.hepyDescription() == null ? "" : hepyP.hepyDescription());
        hepyActive.setSelected(hepyP == null || hepyP.hepyActive());
        hepyCustomizable.setSelected(hepyP != null && hepyP.hepyCustomizable());
        hepyRefreshCustomizable();
        hepyIsNew.setSelected(hepyP == null || hepyP.hepyIsNew());
        hepyFeatured.setSelected(hepyP != null && hepyP.hepyFeatured());

        hepyRenderMaterials();
        Set<Long> hepySelected = new HashSet<>();
        if (hepyP != null) {
            hepyP.hepyMaterials().forEach(hepyM -> hepySelected.add(hepyM.hepyId()));
        }
        hepyMaterialChecks.forEach((hepyId, hepyCheck) -> hepyCheck.setSelected(hepySelected.contains(hepyId)));

        hepyRows.clear();
        hepyVariantRows.getChildren().clear();
        if (hepyP != null) {
            hepyP.hepyVariants().forEach(this::hepyAddRow);
        } else {
            hepyAddRow(null);
        }
        hepyRenderImages(hepyP);
        hepyRenderLook(hepyViewModel.lookProperty().get());
    }

    private void hepyRenderLook(HepyRealisticLook hepyLook) {
        hepyZoneInputs.clear();
        hepyLookZones.getChildren().clear();
        boolean hepyHas = hepyLook != null && !hepyLook.hepyZoneColors().isEmpty();
        hepyLookCard.setVisible(hepyHas);
        hepyLookCard.setManaged(hepyHas);
        if (!hepyHas) {
            return;
        }
        List<Map.Entry<String, String>> hepyEntries = new ArrayList<>(hepyLook.hepyZoneColors().entrySet());
        hepyEntries.sort(java.util.Comparator.comparingInt(hepyE -> {
            int hepyI = HEPY_ZONE_ORDER.indexOf(hepyE.getKey());
            return hepyI < 0 ? HEPY_ZONE_ORDER.size() : hepyI;
        }));
        for (Map.Entry<String, String> hepyE : hepyEntries) {
            PgeyColorSwatchGroup hepyGroup = new PgeyColorSwatchGroup();
            hepyGroup.setValue(Color.web(hepyE.getValue()));
            hepyGroup.valueProperty().addListener((hepyObs, hepyOld, hepyC) -> hepyPaintLook(hepyLook));
            hepyZoneInputs.put(hepyE.getKey(), hepyGroup);
            Label hepyLabel = new Label(HepyTexts.hepyUpper(hepyZoneLabel(hepyLook.hepyModelKey(), hepyE.getKey())));
            hepyLabel.getStyleClass().addAll("label-text", "ink-muted");
            hepyLookZones.getChildren().add(new VBox(8, hepyLabel, hepyGroup));
        }
        hepyPaintLook(hepyLook);
    }

    private void hepyPaintLook(HepyRealisticLook hepySaved) {
        HepyRealisticLook hepyShown = new HepyRealisticLook(hepySaved.hepyModelKey(), hepyZoneColors(hepySaved), hepySaved.hepyPattern(), hepySaved.hepyPatternColor());
        HepyProductImages.hepyRealistic(hepyContext.ppqhRecolorService(), hepyShown, hepyLookPreview::setImage);
    }

    private Map<String, String> hepyZoneColors(HepyRealisticLook hepySaved) {
        Map<String, String> hepyOut = new java.util.LinkedHashMap<>(hepySaved.hepyZoneColors());
        hepyZoneInputs.forEach((hepyZone, hepyGroup) -> {
            if (hepyGroup.getValue() != null) {
                hepyOut.put(hepyZone, com.sportforce3d.design.PgeyDesignDefaults.pgeyHex(hepyGroup.getValue()).toLowerCase(java.util.Locale.ROOT));
            }
        });
        return hepyOut;
    }

    static String hepyZoneLabel(String hepyModelKey, String hepyZone) {
        boolean hepyShoe = hepyModelKey != null && hepyModelKey.startsWith("producto_zapatilla");
        return switch (hepyZone) {
            case "camiseta" -> "Camiseta";
            case "short" -> "Short";
            case "medias" -> "Medias";
            case "paneles" -> hepyShoe ? "Suela" : "Paneles";
            case "principal" -> hepyShoe ? "Capellada" : "Color principal";
            default -> hepyZone;
        };
    }

    private void hepyRefreshCustomizable() {
        boolean hepyAllowed = hepyGarment.getValue() == null || hepyGarment.getValue().hepyDesignable();
        if (!hepyAllowed) {
            hepyCustomizable.setSelected(false);
        }
        hepyCustomizable.setDisable(!hepyAllowed);
        hepyCustomizableNote.setVisible(!hepyAllowed);
        hepyCustomizableNote.setManaged(!hepyAllowed);
    }

    private void hepyRenderMaterials() {
        Set<Long> hepyKeep = new HashSet<>();
        hepyMaterialChecks.forEach((hepyId, hepyC) -> {
            if (hepyC.isSelected()) {
                hepyKeep.add(hepyId);
            }
        });
        hepyMaterialChecks.clear();
        hepyMaterialsPane.getChildren().clear();
        List<HepyMaterial> hepyList = hepyGarment.getValue() == HepyGarment.SHOES ? hepyReference.hepyShoeMaterials() : hepyReference.hepyTextiles();
        for (HepyMaterial hepyM : hepyList) {
            CheckBox hepyCheck = new CheckBox(hepyM.hepyName() + " · " + HepyMoney.hepySurcharge(hepyM.hepySurchargeCents()));
            hepyCheck.setSelected(hepyKeep.contains(hepyM.hepyId()));
            hepyMaterialChecks.put(hepyM.hepyId(), hepyCheck);
            hepyMaterialsPane.getChildren().add(hepyCheck);
        }
    }

    private void hepyRenderImages(HepyProductDetail hepyP) {
        hepyImagesPane.getChildren().clear();
        if (hepyP == null) {
            hepyImagesPane.getChildren().add(hepyNote("Guarda el producto para poder agregar imágenes."));
            return;
        }
        for (HepyProductImage hepyImg : hepyP.hepyImages()) {
            StackPane hepyThumb = new StackPane();
            hepyThumb.getStyleClass().add("sf-thumb");
            hepyThumb.setMinSize(96, 120);
            hepyThumb.setMaxSize(96, 120);
            Image hepyImage = HepyProductImages.hepyLoad(hepyContext.ppqhStorage(), hepyImg.hepyPath(), 96, 120);
            if (hepyImage != null) {
                ImageView hepyView = new ImageView(hepyImage);
                hepyView.setFitWidth(96);
                hepyView.setFitHeight(120);
                hepyView.setPreserveRatio(true);
                hepyThumb.getChildren().add(hepyView);
            }
            PpqhSfButton hepyRemove = PpqhSfButton.ppqhIcon(Feather.TRASH_2, "Quitar imagen");
            hepyRemove.setOnAction(hepyE -> hepyViewModel.hepyRemoveImage(hepyImg.hepyId(), hepyMessage -> PpqhToast.ppqhError(hepyContent, hepyMessage)));
            StackPane.setAlignment(hepyRemove, Pos.TOP_RIGHT);
            hepyThumb.getChildren().add(hepyRemove);
            hepyImagesPane.getChildren().add(hepyThumb);
        }
        PpqhSfButton hepyAdd = new PpqhSfButton("Agregar imagen", PpqhSfButton.PpqhVariant.GHOST).ppqhWithIcon(Feather.UPLOAD);
        hepyAdd.setOnAction(hepyE -> {
            FileChooser hepyChooser = new FileChooser();
            hepyChooser.setTitle("Elegir imagen del producto");
            hepyChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes PNG o JPG", "*.png", "*.jpg", "*.jpeg"));
            File hepyFile = hepyChooser.showOpenDialog(hepyContent.getScene().getWindow());
            if (hepyFile != null) {
                hepyViewModel.hepyAddImage(hepyFile.toPath(), hepyMessage -> PpqhToast.ppqhError(hepyContent, hepyMessage));
            }
        });
        hepyImagesPane.getChildren().add(hepyAdd);
    }

    private final class HepyVariantRow extends HBox {
        final Long hepyId;
        final ComboBox<HepySize> hepySize = new ComboBox<>();
        final TextField hepyColorName = new TextField();
        final TextField hepyColorHex = new TextField();
        final Rectangle hepyPreview = new Rectangle(18, 18);
        final TextField hepyVariantSku = new TextField();
        final TextField hepyDelta = new TextField();
        final TextField hepyStock = new TextField();
        final TextField hepyThreshold = new TextField();
        final CheckBox hepyEnabled = new CheckBox();
        final Label hepyReserved = new Label();
        private boolean hepySkuTouched;

        HepyVariantRow(HepyProductVariant hepyV, HepyVariantRow hepyTemplate) {
            super(8);
            this.hepyId = hepyV == null ? null : hepyV.hepyId();
            setAlignment(Pos.CENTER_LEFT);
            hepySize.setConverter(hepyConverter(HepySize::hepyName, "Elige"));
            hepyRefreshSizes();
            hepySize.setValue(hepyV == null ? null : hepyReference.hepySizes().stream().filter(hepyS -> hepyS.hepyId() == hepyV.hepySizeId()).findFirst().orElse(null));
            hepyColorName.setText(hepyV != null ? hepyV.hepyColorName() : hepyTemplate != null ? hepyTemplate.hepyColorName.getText() : "Azul eléctrico");
            hepyColorHex.setText(hepyV != null ? hepyV.hepyColorHex() : hepyTemplate != null ? hepyTemplate.hepyColorHex.getText() : "#1F5FFF");
            hepyColorHex.getStyleClass().add("code");
            hepyPreview.setArcWidth(6);
            hepyPreview.setArcHeight(6);
            hepyColorHex.textProperty().addListener((hepyObs, hepyOld, hepyHex) -> hepyPaintPreview());
            hepyPaintPreview();
            hepyVariantSku.setText(hepyV == null ? "" : hepyV.hepySku());
            hepyVariantSku.getStyleClass().add("code");
            hepySkuTouched = hepyV != null;
            hepyVariantSku.textProperty().addListener((hepyObs, hepyOld, hepyVal) -> {
                if (hepyVariantSku.isFocused()) {
                    hepySkuTouched = true;
                }
            });
            hepySize.valueProperty().addListener((hepyObs, hepyOld, hepyS) -> hepyAutoSku());
            hepySku.textProperty().addListener((hepyObs, hepyOld, hepyS) -> hepyAutoSku());
            hepyDelta.setText(hepyV == null ? "0.00" : HepyMoney.hepyToInput(hepyV.hepyPriceDeltaCents()));
            hepyStock.setText(hepyV == null ? "0" : String.valueOf(hepyV.hepyQuantity()));
            hepyThreshold.setText(hepyV == null ? "5" : String.valueOf(hepyV.hepyLowStockThreshold()));
            hepyEnabled.setSelected(hepyV == null || hepyV.hepyActive());
            hepyEnabled.setAccessibleText("Variante activa");
            hepyReserved.getStyleClass().addAll("body-s", "ink-subtle");
            hepyReserved.setText(hepyV == null || hepyV.hepyReserved() == 0 ? "" : hepyV.hepyReserved() + " res.");

            hepySize.setAccessibleText("Talla");
            hepyColorName.setAccessibleText("Nombre del color");
            hepyColorHex.setAccessibleText("Color HEX");
            hepyVariantSku.setAccessibleText("SKU de la variante");
            hepyDelta.setAccessibleText("Ajuste de precio en soles");
            hepyStock.setAccessibleText("Stock");
            hepyThreshold.setAccessibleText("Aviso de stock bajo");

            Node hepyLast;
            if (hepyId == null) {
                PpqhSfButton hepyRemove = PpqhSfButton.ppqhIcon(Feather.X, "Quitar variante");
                hepyRemove.setOnAction(hepyE -> {
                    hepyRows.remove(this);
                    hepyVariantRows.getChildren().remove(this);
                });
                hepyLast = hepyRemove;
            } else {
                hepyLast = hepyReserved;
            }
            HBox hepyHex = new HBox(6, hepyPreview, hepyColorHex);
            hepyHex.setAlignment(Pos.CENTER_LEFT);
            Node[] hepyCells = {hepySize, hepyColorName, hepyHex, hepyVariantSku, hepyDelta, hepyStock, hepyThreshold, hepyEnabled, hepyLast};
            for (int hepyI = 0; hepyI < hepyCells.length; hepyI++) {
                if (hepyCells[hepyI] instanceof Region hepyR) {
                    hepyR.setPrefWidth(HEPY_COLS[hepyI]);
                    hepyR.setMinWidth(HEPY_COLS[hepyI]);
                    hepyR.setMaxWidth(HEPY_COLS[hepyI]);
                }
                if (hepyCells[hepyI] instanceof TextField hepyTf) {
                    hepyTf.getStyleClass().add("sf-input-sm");
                }
            }
            hepyColorHex.setPrefWidth(HEPY_COLS[2] - 24);
            hepyColorHex.setMinWidth(HEPY_COLS[2] - 24);
            getChildren().addAll(hepyCells);
            hepyAutoSku();
        }

        void hepyRefreshSizes() {
            HepySize hepyCurrent = hepySize.getValue();
            List<HepySize> hepyOptions = hepySizeOptions();
            hepySize.setItems(FXCollections.observableArrayList(hepyOptions));
            if (hepyCurrent != null && hepyOptions.contains(hepyCurrent)) {
                hepySize.setValue(hepyCurrent);
            } else if (hepyCurrent != null && hepyId != null) {
                hepySize.getItems().add(hepyCurrent);
                hepySize.setValue(hepyCurrent);
            }
        }

        private void hepyAutoSku() {
            if (hepySkuTouched || hepySize.getValue() == null) {
                return;
            }
            String hepyBase = hepySku.getText() == null ? "" : hepySku.getText().trim();
            hepyVariantSku.setText(hepyBase.isEmpty() ? "" : hepyBase + "-" + hepySize.getValue().hepyCode());
        }

        private void hepyPaintPreview() {
            try {
                hepyPreview.setFill(Color.web(hepyColorHex.getText().trim()));
            } catch (RuntimeException hepyE) {
                hepyPreview.setFill(Color.TRANSPARENT);
            }
        }

        HepyProductForm.HepyVariantForm hepyValue() {
            if (hepySize.getValue() == null) {
                throw new IllegalArgumentException("Elige la talla de cada variante.");
            }
            return new HepyProductForm.HepyVariantForm(hepyId, hepySize.getValue().hepyId(), hepyColorName.getText().trim(),
                    hepyColorHex.getText().trim().toUpperCase(), hepyVariantSku.getText().trim().toUpperCase(),
                    hepyParseMoneySigned(hepyDelta.getText(), "ajuste de precio"), hepyParseInt(hepyStock.getText(), "stock"),
                    hepyParseInt(hepyThreshold.getText(), "aviso de stock bajo"), hepyEnabled.isSelected());
        }
    }

    private void hepyAddRow(HepyProductVariant hepyV) {
        HepyVariantRow hepyTemplate = hepyRows.isEmpty() ? null : hepyRows.get(hepyRows.size() - 1);
        HepyVariantRow hepyRow = new HepyVariantRow(hepyV, hepyTemplate);
        if (hepyV == null && hepyTemplate != null && hepyTemplate.hepySize.getValue() != null) {
            List<HepySize> hepyOptions = hepyRow.hepySize.getItems();
            int hepyNext = hepyOptions.indexOf(hepyTemplate.hepySize.getValue()) + 1;
            if (hepyNext > 0 && hepyNext < hepyOptions.size()) {
                hepyRow.hepySize.setValue(hepyOptions.get(hepyNext));
            }
        }
        hepyRows.add(hepyRow);
        hepyVariantRows.getChildren().add(hepyRow);
    }

    private List<HepySize> hepySizeOptions() {
        String hepyGroup = switch (hepyGarment.getValue() == null ? HepyGarment.JERSEY : hepyGarment.getValue()) {
            case SHOES -> "CALZADO";
            case SOCKS -> "MEDIAS";
            case ACCESSORY -> "UNICA";
            default -> "ROPA";
        };
        return hepyReference.hepySizes().stream().filter(hepyS -> hepyS.hepyGroup().equals(hepyGroup)).toList();
    }

    private void hepyAddAllSizes() {
        HepyVariantRow hepyTemplate = hepyRows.isEmpty() ? null : hepyRows.get(hepyRows.size() - 1);
        String hepyHex = hepyTemplate == null ? "#1F5FFF" : hepyTemplate.hepyColorHex.getText().trim().toUpperCase();
        for (HepySize hepyS : hepySizeOptions()) {
            boolean hepyExists = hepyRows.stream().anyMatch(hepyR -> Objects.equals(hepyR.hepySize.getValue(), hepyS)
                    && hepyR.hepyColorHex.getText().trim().equalsIgnoreCase(hepyHex));
            if (!hepyExists) {
                HepyVariantRow hepyRow = new HepyVariantRow(null, hepyTemplate);
                hepyRow.hepySize.setValue(hepyS);
                hepyRows.add(hepyRow);
                hepyVariantRows.getChildren().add(hepyRow);
            }
        }
    }

    private Node hepyHeader() {
        String[] hepyTitles = {"TALLA", "COLOR", "HEX", "SKU", "AJUSTE S/", "STOCK", "AVISO", "ACTIVA", ""};
        HBox hepyHeader = new HBox(8);
        for (int hepyI = 0; hepyI < hepyTitles.length; hepyI++) {
            Label hepyL = new Label(hepyTitles[hepyI]);
            hepyL.getStyleClass().add("sf-field-label");
            hepyL.setPrefWidth(HEPY_COLS[hepyI]);
            hepyL.setMinWidth(HEPY_COLS[hepyI]);
            hepyHeader.getChildren().add(hepyL);
        }
        return hepyHeader;
    }

    private void hepySave() {
        hepyClearErrors();
        HepyProductForm hepyForm;
        try {
            hepyForm = hepyBuildForm(hepyViewModel.productProperty().get());
        } catch (IllegalArgumentException hepyE) {
            hepyGeneralError.setText(hepyE.getMessage());
            PpqhToast.ppqhError(hepyContent, hepyE.getMessage());
            return;
        }
        boolean hepyIsNewProduct = hepyForm.hepyId() == null;
        HepyRealisticLook hepyLook = hepyViewModel.lookProperty().get();
        Map<String, String> hepyZones = hepyLook == null || hepyIsNewProduct ? Map.of() : hepyZoneColors(hepyLook);
        hepyViewModel.hepySave(hepyForm, hepyZones.equals(hepyLook == null ? null : hepyLook.hepyZoneColors()) ? Map.of() : hepyZones,
                hepySaved -> {
                    PpqhToast.ppqhSuccess(hepyContent, hepyIsNewProduct ? "Producto creado. Ahora puedes agregarle imágenes." : "Cambios guardados.");
                    hepyContext.ppqhRefreshCartCount();
                },
                this::hepyShowErrors,
                hepyMessage -> PpqhToast.ppqhError(hepyContent, hepyMessage));
    }

    private HepyProductForm hepyBuildForm(HepyProductDetail hepyCurrent) {
        if (hepyCategory.getValue() == null) {
            hepyCategoryError.setText("Elige una categoría.");
            throw new IllegalArgumentException("Elige una categoría.");
        }
        long hepyBase = hepyParseMoney(hepyPrice.getText(), "precio");
        Long hepyCompare = hepyCompareAt.getText() == null || hepyCompareAt.getText().isBlank() ? null
                : hepyParseMoney(hepyCompareAt.getText(), "precio anterior");
        List<HepyProductForm.HepyVariantForm> hepyVariants = new ArrayList<>();
        for (HepyVariantRow hepyRow : hepyRows) {
            hepyVariants.add(hepyRow.hepyValue());
        }
        Set<Long> hepyMaterials = new HashSet<>();
        hepyMaterialChecks.forEach((hepyId, hepyC) -> {
            if (hepyC.isSelected()) {
                hepyMaterials.add(hepyId);
            }
        });
        return new HepyProductForm(hepyCurrent == null ? null : hepyCurrent.hepyId(), hepyCategory.getValue().hepyId(),
                hepySport.getValue() == null ? null : hepySport.getValue().hepyId(), hepySku.getText().trim().toUpperCase(),
                hepyName.getText() == null ? "" : hepyName.getText().trim(),
                hepyDescription.getText() == null || hepyDescription.getText().isBlank() ? null : hepyDescription.getText().trim(), hepyGarment.getValue(), hepyBase, hepyCompare, hepyCustomizable.isSelected(),
                hepyIsNew.isSelected(), hepyFeatured.isSelected(), hepyActive.isSelected(), hepyMaterials, hepyVariants);
    }

    private void hepyShowErrors(Map<String, String> hepyErrors) {
        hepyName.setError(hepyErrors.get("name"));
        hepySku.setError(hepyErrors.get("sku"));
        hepyPrice.setError(hepyErrors.get("price"));
        hepyCompareAt.setError(hepyErrors.get("compareAt"));
        hepyMaterialsError.setText(hepyErrors.getOrDefault("materials", ""));
        hepyVariantsError.setText(hepyErrors.getOrDefault("variants", ""));
        hepyGeneralError.setText(hepyErrors.getOrDefault("description", hepyErrors.getOrDefault("garment",
                hepyErrors.getOrDefault("customizable", ""))));
        hepyLookError.setText(hepyErrors.getOrDefault("zones", ""));
        PpqhToast.ppqhError(hepyContent, "Revisa los campos marcados.");
    }

    private void hepyClearErrors() {
        for (PpqhSfTextField hepyF : new PpqhSfTextField[]{hepyName, hepySku, hepyPrice, hepyCompareAt}) {
            hepyF.setError("");
        }
        for (Label hepyL : new Label[]{hepyMaterialsError, hepyVariantsError, hepyGeneralError, hepyCategoryError, hepyLookError}) {
            hepyL.setText("");
        }
    }

    private static long hepyParseMoney(String hepyText, String hepyWhat) {
        try {
            return HepyMoney.hepyParse(hepyText);
        } catch (IllegalArgumentException hepyE) {
            throw new IllegalArgumentException("Revisa el " + hepyWhat + ": escribe un monto como 79.90.");
        }
    }

    private static long hepyParseMoneySigned(String hepyText, String hepyWhat) {
        String hepyT = hepyText == null ? "" : hepyText.trim();
        if (hepyT.isEmpty()) {
            return 0;
        }
        boolean hepyNegative = hepyT.startsWith("-");
        long hepyValue = hepyParseMoney(hepyNegative ? hepyT.substring(1) : hepyT, hepyWhat);
        return hepyNegative ? -hepyValue : hepyValue;
    }

    private static int hepyParseInt(String hepyText, String hepyWhat) {
        try {
            return Integer.parseInt(hepyText == null ? "" : hepyText.trim());
        } catch (NumberFormatException hepyE) {
            throw new IllegalArgumentException("Revisa el " + hepyWhat + ": escribe un número entero.");
        }
    }

    private static VBox hepyCard(String hepyTitle, Node... hepyChildren) {
        Label hepyLabel = new Label(hepyTitle);
        hepyLabel.getStyleClass().addAll("title-s", "ink");
        VBox hepyCard = new VBox(16, hepyLabel);
        hepyCard.getChildren().addAll(hepyChildren);
        hepyCard.getStyleClass().add("sf-card");
        hepyCard.setPadding(new Insets(24));
        return hepyCard;
    }

    private static HBox hepyGrid(Node... hepyNodes) {
        HBox hepyBox = new HBox(16, hepyNodes);
        for (Node hepyN : hepyNodes) {
            HBox.setHgrow(hepyN, Priority.ALWAYS);
            if (hepyN instanceof Region hepyR) {
                hepyR.setPrefWidth(260);
                hepyR.setMaxWidth(Double.MAX_VALUE);
            }
        }
        return hepyBox;
    }

    private static VBox hepyField(String hepyLabel, Node hepyControl, Label hepyError) {
        Label hepyL = new Label(hepyLabel);
        hepyL.getStyleClass().add("sf-field-label");
        VBox hepyBox = new VBox(4, hepyL, hepyControl);
        if (hepyError != null) {
            hepyBox.getChildren().add(hepyError);
        }
        return hepyBox;
    }

    private static Label hepyNote(String hepyText) {
        Label hepyLabel = new Label(hepyText);
        hepyLabel.getStyleClass().addAll("body-s", "ink-muted");
        hepyLabel.setWrapText(true);
        return hepyLabel;
    }

    private static Label hepyErrorLabel() {
        Label hepyLabel = new Label();
        hepyLabel.getStyleClass().add("sf-field-error");
        hepyLabel.setWrapText(true);
        hepyLabel.managedProperty().bind(hepyLabel.textProperty().isNotEmpty());
        hepyLabel.visibleProperty().bind(hepyLabel.managedProperty());
        return hepyLabel;
    }

    private static <T> StringConverter<T> hepyConverter(java.util.function.Function<T, String> hepyToText, String hepyNullText) {
        return new StringConverter<>() {
            @Override
            public String toString(T hepyValue) {
                return hepyValue == null ? hepyNullText : hepyToText.apply(hepyValue);
            }

            @Override
            public T fromString(String hepyS) {
                return null;
            }
        };
    }
}
