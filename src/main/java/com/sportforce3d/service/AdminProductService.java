package com.sportforce3d.service;

import com.sportforce3d.db.DataAccessException;
import com.sportforce3d.model.AdminProductRow;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.InventoryRow;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.model.ProductForm;
import com.sportforce3d.model.ProductForm.VariantForm;
import com.sportforce3d.model.ProductImage;
import com.sportforce3d.model.ProductVariant;
import com.sportforce3d.model.RealisticLook;
import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.CatalogRepository;
import com.sportforce3d.repository.ProductRepository;
import com.sportforce3d.storage.StorageService;
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

/** Gestión de productos, imágenes e inventario. Todos los métodos exigen rol ADMIN. */
public class AdminProductService {

    private static final Logger log = LoggerFactory.getLogger(AdminProductService.class);
    private static final Pattern SKU = Pattern.compile("^[A-Z0-9][A-Z0-9-]{2,39}$");
    private static final Pattern HEX = Pattern.compile("^#[0-9A-F]{6}$");
    public static final long MAX_PRICE_CENTS = 9_999_999; // S/ 99,999.99
    public static final int MAX_STOCK = 99_999;

    private final ProductRepository products;
    private final CatalogRepository catalog;
    private final StorageService storage;
    private final Session session;
    private final Clock clock;

    public AdminProductService(ProductRepository products, CatalogRepository catalog, StorageService storage,
                               Session session, Clock clock) {
        this.products = products;
        this.catalog = catalog;
        this.storage = storage;
        this.session = session;
        this.clock = clock;
    }

    public List<AdminProductRow> list(String text, boolean includeDeleted) {
        session.requireRole(Role.ADMIN);
        return products.findForAdmin(text, includeDeleted);
    }

    public ProductDetail get(long productId) {
        session.requireRole(Role.ADMIN);
        return products.findDetail(productId).orElseThrow(() -> new ServiceException("El producto ya no existe."));
    }

    /** Crea o actualiza. Devuelve el producto guardado. */
    public ProductDetail save(ProductForm form) {
        User admin = session.requireRole(Role.ADMIN);
        ProductDetail current = form.id() == null ? null : get(form.id());
        validate(form, current);
        long id;
        try {
            id = products.save(form);
        } catch (DataAccessException e) {
            String cause = String.valueOf(e.getCause() == null ? "" : e.getCause().getMessage());
            if (cause.contains("UNIQUE") && cause.contains("product_variants.sku")) {
                throw ValidationException.of("variants", "Algún SKU de variante ya lo usa otro producto.");
            }
            if (cause.contains("UNIQUE") && cause.contains("product_variants.product_id")) {
                throw ValidationException.of("variants", "Hay dos variantes con la misma talla y color.");
            }
            if (cause.contains("UNIQUE") && cause.contains("products.")) {
                throw ValidationException.of("sku", "Ese SKU o nombre ya lo usa otro producto.");
            }
            throw e;
        }
        log.info("ADMIN {} guardó el producto {} ({})", admin.id(), id, form.sku());
        return products.findDetail(id).orElseThrow();
    }

    public void delete(long productId) {
        User admin = session.requireRole(Role.ADMIN);
        get(productId);
        products.softDelete(productId, clock.instant());
        log.info("ADMIN {} eliminó (lógico) el producto {}", admin.id(), productId);
    }

    public void restore(long productId) {
        session.requireRole(Role.ADMIN);
        get(productId);
        products.restore(productId);
    }

    /** Foto realista del producto (modelo y colores por zona), si tiene. */
    public java.util.Optional<RealisticLook> look(long productId) {
        session.requireRole(Role.ADMIN);
        return products.findLook(productId);
    }

    /**
     * Cambia los colores por zona de la foto realista (p. ej. la suela de la zapatilla de running, zona
     * {@code paneles}). Solo zonas que el producto ya tiene; el modelo no cambia.
     */
    public RealisticLook updateZoneColors(long productId, Map<String, String> zoneColors) {
        User admin = session.requireRole(Role.ADMIN);
        get(productId);
        RealisticLook current = products.findLook(productId)
                .orElseThrow(() -> new ServiceException("Este producto no tiene foto realista."));
        Map<String, String> next = new LinkedHashMap<>(current.zoneColors());
        for (Map.Entry<String, String> e : zoneColors.entrySet()) {
            if (!next.containsKey(e.getKey())) {
                throw ValidationException.of("zones", "La foto de este producto no tiene la zona " + e.getKey() + ".");
            }
            String hex = e.getValue() == null ? "" : e.getValue().trim().toUpperCase(java.util.Locale.ROOT);
            if (!HEX.matcher(hex).matches()) {
                throw ValidationException.of("zones", "El color de cada zona debe ser HEX, p. ej. #F2F5F8.");
            }
            next.put(e.getKey(), hex.toLowerCase(java.util.Locale.ROOT));
        }
        if (!next.equals(current.zoneColors())) {
            products.updateZoneColors(productId, next);
            log.info("ADMIN {} cambió los colores de la foto realista del producto {}", admin.id(), productId);
        }
        return products.findLook(productId).orElseThrow();
    }

    public ProductImage addImage(long productId, Path file) {
        session.requireRole(Role.ADMIN);
        ProductDetail product = get(productId);
        if (product.images().size() >= 8) {
            throw ValidationException.of("file", "Cada producto admite hasta 8 imágenes.");
        }
        StorageService.StoredFile stored = storage.store(file, "products", StorageService.IMAGES);
        try {
            return products.addImage(productId, stored.path(), product.name());
        } catch (RuntimeException e) {
            storage.delete(stored.path());
            throw e;
        }
    }

    public void removeImage(long productId, long imageId) {
        session.requireRole(Role.ADMIN);
        products.removeImage(productId, imageId).ifPresent(storage::delete);
    }

    public List<InventoryRow> inventory(String text, boolean lowStockOnly) {
        session.requireRole(Role.ADMIN);
        return products.findInventory(text, lowStockOnly);
    }

    public InventoryRow updateStock(long variantId, int quantity, int lowStockThreshold) {
        User admin = session.requireRole(Role.ADMIN);
        InventoryRow row = products.findInventoryRow(variantId)
                .orElseThrow(() -> new ServiceException("La variante ya no existe."));
        if (quantity < 0 || quantity > MAX_STOCK) {
            throw ValidationException.of("quantity", "El stock debe estar entre 0 y " + MAX_STOCK + ".");
        }
        if (quantity < row.reserved()) {
            throw ValidationException.of("quantity", "Hay " + row.reserved()
                    + " unidades reservadas por pedidos sin pagar; el stock no puede ser menor.");
        }
        if (lowStockThreshold < 0 || lowStockThreshold > 999) {
            throw ValidationException.of("threshold", "El aviso de stock bajo debe estar entre 0 y 999.");
        }
        products.updateStock(variantId, quantity, lowStockThreshold);
        log.info("ADMIN {} cambió el stock de {} a {}", admin.id(), row.sku(), quantity);
        return products.findInventoryRow(variantId).orElseThrow();
    }

    private void validate(ProductForm f, ProductDetail current) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (f.name() == null || f.name().isBlank() || f.name().trim().length() < 3) {
            errors.put("name", "Escribe el nombre del producto (mínimo 3 caracteres).");
        } else if (f.name().length() > 120) {
            errors.put("name", "El nombre admite hasta 120 caracteres.");
        }
        if (f.sku() == null || !SKU.matcher(f.sku()).matches()) {
            errors.put("sku", "Usa de 3 a 40 caracteres: mayúsculas, números y guiones (p. ej. SF-CAM-010).");
        } else if (products.skuExists(f.sku(), f.id())) {
            errors.put("sku", "Ese SKU ya lo usa otro producto.");
        }
        if (f.garment() == null) {
            errors.put("garment", "Elige el tipo de prenda.");
        } else if (f.customizable() && !f.garment().designable()) {
            errors.put("customizable", "Los buzos y accesorios no se personalizan en el diseñador 3D: se venden con "
                    + "su color y talla. Desmarca \"Personalizable\".");
        }
        if (f.basePriceCents() < 0 || f.basePriceCents() > MAX_PRICE_CENTS) {
            errors.put("price", "El precio debe estar entre S/ 0.00 y S/ 99,999.99.");
        }
        if (f.compareAtPriceCents() != null && f.compareAtPriceCents() <= f.basePriceCents()) {
            errors.put("compareAt", "El precio anterior debe ser mayor que el precio actual (o déjalo vacío).");
        }
        if (f.description() != null && f.description().length() > 1000) {
            errors.put("description", "La descripción admite hasta 1000 caracteres.");
        }
        validateMaterials(f, errors);
        validateVariants(f, current, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private void validateMaterials(ProductForm f, Map<String, String> errors) {
        if (f.materialIds().isEmpty() || f.garment() == null) {
            return;
        }
        Material.Scope scope = f.garment() == Garment.SHOES ? Material.Scope.CALZADO : Material.Scope.TEXTIL;
        Set<Long> valid = catalog.findMaterials(scope).stream().map(Material::id).collect(Collectors.toSet());
        if (!valid.containsAll(f.materialIds())) {
            errors.put("materials", f.garment() == Garment.SHOES
                    ? "Las zapatillas solo admiten materiales de calzado."
                    : "Esta prenda solo admite telas.");
        }
    }

    private static void validateVariants(ProductForm f, ProductDetail current, Map<String, String> errors) {
        if (f.variants().isEmpty()) {
            errors.put("variants", "Agrega al menos una variante (talla y color).");
            return;
        }
        Set<String> combos = new HashSet<>();
        Set<String> skus = new HashSet<>();
        Map<Long, ProductVariant> existing = current == null ? Map.of()
                : current.variants().stream().collect(Collectors.toMap(ProductVariant::id, v -> v));
        for (int i = 0; i < f.variants().size(); i++) {
            VariantForm v = f.variants().get(i);
            String row = "Variante " + (i + 1) + ": ";
            if (v.colorName() == null || v.colorName().isBlank() || v.colorName().length() > 40) {
                errors.put("variants", row + "escribe el nombre del color.");
            } else if (v.colorHex() == null || !HEX.matcher(v.colorHex()).matches()) {
                errors.put("variants", row + "el color debe ser HEX en mayúsculas, p. ej. #1F5FFF.");
            } else if (v.sku() == null || !SKU.matcher(v.sku()).matches()) {
                errors.put("variants", row + "SKU inválido (mayúsculas, números y guiones).");
            } else if (!skus.add(v.sku())) {
                errors.put("variants", row + "el SKU " + v.sku() + " está repetido.");
            } else if (!combos.add(v.sizeId() + "|" + v.colorHex())) {
                errors.put("variants", row + "ya existe otra variante con la misma talla y color.");
            } else if (v.stock() < 0 || v.stock() > MAX_STOCK) {
                errors.put("variants", row + "el stock debe estar entre 0 y " + MAX_STOCK + ".");
            } else if (v.lowStockThreshold() < 0 || v.lowStockThreshold() > 999) {
                errors.put("variants", row + "el aviso de stock bajo debe estar entre 0 y 999.");
            } else if (Math.abs(v.priceDeltaCents()) > MAX_PRICE_CENTS) {
                errors.put("variants", row + "el ajuste de precio es demasiado grande.");
            } else if (v.id() != null && !existing.containsKey(v.id())) {
                errors.put("variants", row + "no pertenece a este producto.");
            } else if (v.id() != null && v.stock() < existing.get(v.id()).reserved()) {
                errors.put("variants", row + "hay " + existing.get(v.id()).reserved()
                        + " unidades reservadas; el stock no puede ser menor.");
            }
            if (errors.containsKey("variants")) {
                return;
            }
        }
        if (f.active() && f.variants().stream().noneMatch(VariantForm::active)) {
            errors.put("variants", "Un producto activo necesita al menos una variante activa.");
        }
    }
}
