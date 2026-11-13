package com.sportforce3d.service;

import com.sportforce3d.db.HepyDataAccessException;
import com.sportforce3d.model.HepyAdminProductRow;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyInventoryRow;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.model.HepyProductForm;
import com.sportforce3d.model.HepyProductForm.HepyVariantForm;
import com.sportforce3d.model.HepyProductImage;
import com.sportforce3d.model.HepyProductVariant;
import com.sportforce3d.model.HepyRealisticLook;
import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyCatalogRepository;
import com.sportforce3d.repository.HepyProductRepository;
import com.sportforce3d.storage.HepyStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Clock;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HepyAdminProductService {

    private static final Logger hepyLog = LoggerFactory.getLogger(HepyAdminProductService.class);
    private static final Pattern HEPY_SKU = Pattern.compile("^[A-Z0-9][A-Z0-9-]{2,39}$");
    private static final Pattern HEPY_HEX = Pattern.compile("^#[0-9A-F]{6}$");
    public static final long HEPY_MAX_PRICE_CENTS = 9_999_999;
    public static final int HEPY_MAX_STOCK = 99_999;

    private final HepyProductRepository hepyProducts;
    private final HepyCatalogRepository hepyCatalog;
    private final HepyStorageService hepyStorage;
    private final PpqhSession hepySession;
    private final Clock hepyClock;

    public HepyAdminProductService(HepyProductRepository hepyProducts, HepyCatalogRepository hepyCatalog, HepyStorageService hepyStorage,
                               PpqhSession hepySession, Clock hepyClock) {
        this.hepyProducts = hepyProducts;
        this.hepyCatalog = hepyCatalog;
        this.hepyStorage = hepyStorage;
        this.hepySession = hepySession;
        this.hepyClock = hepyClock;
    }

    public List<HepyAdminProductRow> hepyList(String hepyText, boolean hepyIncludeDeleted) {
        hepySession.ppqhRequireRole(PpqhRole.ADMIN);
        return hepyProducts.hepyFindForAdmin(hepyText, hepyIncludeDeleted);
    }

    public HepyProductDetail hepyGet(long hepyProductId) {
        hepySession.ppqhRequireRole(PpqhRole.ADMIN);
        return hepyProducts.hepyFindDetail(hepyProductId).orElseThrow(() -> new PpqhServiceException("El producto ya no existe."));
    }

    public HepyProductDetail hepySave(HepyProductForm hepyForm) {
        PpqhUser hepyAdmin = hepySession.ppqhRequireRole(PpqhRole.ADMIN);
        HepyProductDetail hepyCurrent = hepyForm.hepyId() == null ? null : hepyGet(hepyForm.hepyId());
        hepyValidate(hepyForm, hepyCurrent);
        long hepyId;
        try {
            hepyId = hepyProducts.hepySave(hepyForm);
        } catch (HepyDataAccessException hepyE) {
            String hepyCause = String.valueOf(hepyE.getCause() == null ? "" : hepyE.getCause().getMessage());
            if (hepyCause.contains("UNIQUE") && hepyCause.contains("product_variants.sku")) {
                throw PpqhValidationException.ppqhOf("variants", "Algún SKU de variante ya lo usa otro producto.");
            }
            if (hepyCause.contains("UNIQUE") && hepyCause.contains("product_variants.product_id")) {
                throw PpqhValidationException.ppqhOf("variants", "Hay dos variantes con la misma talla y color.");
            }
            if (hepyCause.contains("UNIQUE") && hepyCause.contains("products.")) {
                throw PpqhValidationException.ppqhOf("sku", "Ese SKU o nombre ya lo usa otro producto.");
            }
            throw hepyE;
        }
        hepyLog.info("ADMIN {} guardó el producto {} ({})", hepyAdmin.ppqhId(), hepyId, hepyForm.hepySku());
        return hepyProducts.hepyFindDetail(hepyId).orElseThrow();
    }

    public void hepyDelete(long hepyProductId) {
        PpqhUser hepyAdmin = hepySession.ppqhRequireRole(PpqhRole.ADMIN);
        hepyGet(hepyProductId);
        hepyProducts.hepySoftDelete(hepyProductId, hepyClock.instant());
        hepyLog.info("ADMIN {} eliminó (lógico) el producto {}", hepyAdmin.ppqhId(), hepyProductId);
    }

    public void hepyRestore(long hepyProductId) {
        hepySession.ppqhRequireRole(PpqhRole.ADMIN);
        hepyGet(hepyProductId);
        hepyProducts.hepyRestore(hepyProductId);
    }

    public java.util.Optional<HepyRealisticLook> hepyLook(long hepyProductId) {
        hepySession.ppqhRequireRole(PpqhRole.ADMIN);
        return hepyProducts.hepyFindLook(hepyProductId);
    }

    public HepyRealisticLook hepyUpdateZoneColors(long hepyProductId, Map<String, String> hepyZoneColors) {
        PpqhUser hepyAdmin = hepySession.ppqhRequireRole(PpqhRole.ADMIN);
        hepyGet(hepyProductId);
        HepyRealisticLook hepyCurrent = hepyProducts.hepyFindLook(hepyProductId)
                .orElseThrow(() -> new PpqhServiceException("Este producto no tiene foto realista."));
        Map<String, String> hepyNext = new LinkedHashMap<>(hepyCurrent.hepyZoneColors());
        for (Map.Entry<String, String> hepyE : hepyZoneColors.entrySet()) {
            if (!hepyNext.containsKey(hepyE.getKey())) {
                throw PpqhValidationException.ppqhOf("zones", "La foto de este producto no tiene la zona " + hepyE.getKey() + ".");
            }
            String hepyHex = hepyE.getValue() == null ? "" : hepyE.getValue().trim().toUpperCase(java.util.Locale.ROOT);
            if (!HEPY_HEX.matcher(hepyHex).matches()) {
                throw PpqhValidationException.ppqhOf("zones", "El color de cada zona debe ser HEX, p. ej. #F2F5F8.");
            }
            hepyNext.put(hepyE.getKey(), hepyHex.toLowerCase(java.util.Locale.ROOT));
        }
        if (!hepyNext.equals(hepyCurrent.hepyZoneColors())) {
            hepyProducts.hepyUpdateZoneColors(hepyProductId, hepyNext);
            hepyLog.info("ADMIN {} cambió los colores de la foto realista del producto {}", hepyAdmin.ppqhId(), hepyProductId);
        }
        return hepyProducts.hepyFindLook(hepyProductId).orElseThrow();
    }

    public HepyProductImage hepyAddImage(long hepyProductId, Path hepyFile) {
        hepySession.ppqhRequireRole(PpqhRole.ADMIN);
        HepyProductDetail hepyProduct = hepyGet(hepyProductId);
        if (hepyProduct.hepyImages().size() >= 8) {
            throw PpqhValidationException.ppqhOf("file", "Cada producto admite hasta 8 imágenes.");
        }
        HepyStorageService.HepyStoredFile hepyStored = hepyStorage.hepyStore(hepyFile, "products", HepyStorageService.HEPY_IMAGES);
        try {
            return hepyProducts.hepyAddImage(hepyProductId, hepyStored.hepyPath(), hepyProduct.hepyName());
        } catch (RuntimeException hepyE) {
            hepyStorage.hepyDelete(hepyStored.hepyPath());
            throw hepyE;
        }
    }

    public void hepyRemoveImage(long hepyProductId, long hepyImageId) {
        hepySession.ppqhRequireRole(PpqhRole.ADMIN);
        hepyProducts.hepyRemoveImage(hepyProductId, hepyImageId).ifPresent(hepyStorage::hepyDelete);
    }

    public List<HepyInventoryRow> hepyInventory(String hepyText, boolean hepyLowStockOnly) {
        hepySession.ppqhRequireRole(PpqhRole.ADMIN);
        return hepyProducts.hepyFindInventory(hepyText, hepyLowStockOnly);
    }

    public HepyInventoryRow hepyUpdateStock(long hepyVariantId, int hepyQuantity, int hepyLowStockThreshold) {
        PpqhUser hepyAdmin = hepySession.ppqhRequireRole(PpqhRole.ADMIN);
        HepyInventoryRow hepyRow = hepyProducts.hepyFindInventoryRow(hepyVariantId)
                .orElseThrow(() -> new PpqhServiceException("La variante ya no existe."));
        if (hepyQuantity < 0 || hepyQuantity > HEPY_MAX_STOCK) {
            throw PpqhValidationException.ppqhOf("quantity", "El stock debe estar entre 0 y " + HEPY_MAX_STOCK + ".");
        }
        if (hepyQuantity < hepyRow.hepyReserved()) {
            throw PpqhValidationException.ppqhOf("quantity", "Hay " + hepyRow.hepyReserved()
                    + " unidades reservadas por pedidos sin pagar; el stock no puede ser menor.");
        }
        if (hepyLowStockThreshold < 0 || hepyLowStockThreshold > 999) {
            throw PpqhValidationException.ppqhOf("threshold", "El aviso de stock bajo debe estar entre 0 y 999.");
        }
        hepyProducts.hepyUpdateStock(hepyVariantId, hepyQuantity, hepyLowStockThreshold);
        hepyLog.info("ADMIN {} cambió el stock de {} a {}", hepyAdmin.ppqhId(), hepyRow.hepySku(), hepyQuantity);
        return hepyProducts.hepyFindInventoryRow(hepyVariantId).orElseThrow();
    }

    private void hepyValidate(HepyProductForm hepyF, HepyProductDetail hepyCurrent) {
        Map<String, String> hepyErrors = new LinkedHashMap<>();
        if (hepyF.hepyName() == null || hepyF.hepyName().isBlank() || hepyF.hepyName().trim().length() < 3) {
            hepyErrors.put("name", "Escribe el nombre del producto (mínimo 3 caracteres).");
        } else if (hepyF.hepyName().length() > 120) {
            hepyErrors.put("name", "El nombre admite hasta 120 caracteres.");
        }
        if (hepyF.hepySku() == null || !HEPY_SKU.matcher(hepyF.hepySku()).matches()) {
            hepyErrors.put("sku", "Usa de 3 a 40 caracteres: mayúsculas, números y guiones (p. ej. SF-CAM-010).");
        } else if (hepyProducts.hepySkuExists(hepyF.hepySku(), hepyF.hepyId())) {
            hepyErrors.put("sku", "Ese SKU ya lo usa otro producto.");
        }
        if (hepyF.hepyGarment() == null) {
            hepyErrors.put("garment", "Elige el tipo de prenda.");
        } else if (hepyF.hepyCustomizable() && !hepyF.hepyGarment().hepyDesignable()) {
            hepyErrors.put("customizable", "Los buzos y accesorios no se personalizan en el diseñador 3D: se venden con "
                    + "su color y talla. Desmarca \"Personalizable\".");
        }
        if (hepyF.hepyBasePriceCents() < 0 || hepyF.hepyBasePriceCents() > HEPY_MAX_PRICE_CENTS) {
            hepyErrors.put("price", "El precio debe estar entre S/ 0.00 y S/ 99,999.99.");
        }
        if (hepyF.hepyCompareAtPriceCents() != null && hepyF.hepyCompareAtPriceCents() <= hepyF.hepyBasePriceCents()) {
            hepyErrors.put("compareAt", "El precio anterior debe ser mayor que el precio actual (o déjalo vacío).");
        }
        if (hepyF.hepyDescription() != null && hepyF.hepyDescription().length() > 1000) {
            hepyErrors.put("description", "La descripción admite hasta 1000 caracteres.");
        }
        hepyValidateMaterials(hepyF, hepyErrors);
        hepyValidateVariants(hepyF, hepyCurrent, hepyErrors);
        if (!hepyErrors.isEmpty()) {
            throw new PpqhValidationException(hepyErrors);
        }
    }

    private void hepyValidateMaterials(HepyProductForm hepyF, Map<String, String> hepyErrors) {
        if (hepyF.hepyMaterialIds().isEmpty() || hepyF.hepyGarment() == null) {
            return;
        }
        HepyMaterial.HepyScope hepyScope = hepyF.hepyGarment() == HepyGarment.SHOES ? HepyMaterial.HepyScope.CALZADO : HepyMaterial.HepyScope.TEXTIL;
        Set<Long> hepyValid = hepyCatalog.hepyFindMaterials(hepyScope).stream().map(HepyMaterial::hepyId).collect(Collectors.toSet());
        if (!hepyValid.containsAll(hepyF.hepyMaterialIds())) {
            hepyErrors.put("materials", hepyF.hepyGarment() == HepyGarment.SHOES
                    ? "Las zapatillas solo admiten materiales de calzado."
                    : "Esta prenda solo admite telas.");
        }
    }

    private static void hepyValidateVariants(HepyProductForm hepyF, HepyProductDetail hepyCurrent, Map<String, String> hepyErrors) {
        if (hepyF.hepyVariants().isEmpty()) {
            hepyErrors.put("variants", "Agrega al menos una variante (talla y color).");
            return;
        }
        Set<String> hepyCombos = new HashSet<>();
        Set<String> hepySkus = new HashSet<>();
        Map<Long, HepyProductVariant> hepyExisting = hepyCurrent == null ? Map.of()
                : hepyCurrent.hepyVariants().stream().collect(Collectors.toMap(HepyProductVariant::hepyId, hepyV -> hepyV));
        for (int hepyI = 0; hepyI < hepyF.hepyVariants().size(); hepyI++) {
            HepyVariantForm hepyV = hepyF.hepyVariants().get(hepyI);
            String hepyRow = "Variante " + (hepyI + 1) + ": ";
            if (hepyV.hepyColorName() == null || hepyV.hepyColorName().isBlank() || hepyV.hepyColorName().length() > 40) {
                hepyErrors.put("variants", hepyRow + "escribe el nombre del color.");
            } else if (hepyV.hepyColorHex() == null || !HEPY_HEX.matcher(hepyV.hepyColorHex()).matches()) {
                hepyErrors.put("variants", hepyRow + "el color debe ser HEX en mayúsculas, p. ej. #1F5FFF.");
            } else if (hepyV.hepySku() == null || !HEPY_SKU.matcher(hepyV.hepySku()).matches()) {
                hepyErrors.put("variants", hepyRow + "SKU inválido (mayúsculas, números y guiones).");
            } else if (!hepySkus.add(hepyV.hepySku())) {
                hepyErrors.put("variants", hepyRow + "el SKU " + hepyV.hepySku() + " está repetido.");
            } else if (!hepyCombos.add(hepyV.hepySizeId() + "|" + hepyV.hepyColorHex())) {
                hepyErrors.put("variants", hepyRow + "ya existe otra variante con la misma talla y color.");
            } else if (hepyV.hepyStock() < 0 || hepyV.hepyStock() > HEPY_MAX_STOCK) {
                hepyErrors.put("variants", hepyRow + "el stock debe estar entre 0 y " + HEPY_MAX_STOCK + ".");
            } else if (hepyV.hepyLowStockThreshold() < 0 || hepyV.hepyLowStockThreshold() > 999) {
                hepyErrors.put("variants", hepyRow + "el aviso de stock bajo debe estar entre 0 y 999.");
            } else if (Math.abs(hepyV.hepyPriceDeltaCents()) > HEPY_MAX_PRICE_CENTS) {
                hepyErrors.put("variants", hepyRow + "el ajuste de precio es demasiado grande.");
            } else if (hepyV.hepyId() != null && !hepyExisting.containsKey(hepyV.hepyId())) {
                hepyErrors.put("variants", hepyRow + "no pertenece a este producto.");
            } else if (hepyV.hepyId() != null && hepyV.hepyStock() < hepyExisting.get(hepyV.hepyId()).hepyReserved()) {
                hepyErrors.put("variants", hepyRow + "hay " + hepyExisting.get(hepyV.hepyId()).hepyReserved()
                        + " unidades reservadas; el stock no puede ser menor.");
            }
            if (hepyErrors.containsKey("variants")) {
                return;
            }
        }
        if (hepyF.hepyActive() && hepyF.hepyVariants().stream().noneMatch(HepyVariantForm::hepyActive)) {
            hepyErrors.put("variants", "Un producto activo necesita al menos una variante activa.");
        }
    }
}
