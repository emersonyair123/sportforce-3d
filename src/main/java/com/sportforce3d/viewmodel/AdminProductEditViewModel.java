package com.sportforce3d.viewmodel;

import com.sportforce3d.app.Async;
import com.sportforce3d.model.Category;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.model.ProductForm;
import com.sportforce3d.model.RealisticLook;
import com.sportforce3d.model.Size;
import com.sportforce3d.model.Sport;
import com.sportforce3d.service.AdminProductService;
import com.sportforce3d.service.CatalogService;
import com.sportforce3d.service.ValidationException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Editor de producto del ADMIN: datos de referencia, carga, guardado e imágenes. */
public class AdminProductEditViewModel {

    /** Listas para los selectores del formulario. */
    public record ReferenceData(List<Category> categories, List<Sport> sports, List<Size> sizes,
                                List<Material> textiles, List<Material> shoeMaterials) {
    }

    private final AdminProductService admin;
    private final CatalogService catalog;
    private final ObjectProperty<ReferenceData> reference = new SimpleObjectProperty<>();
    private final ObjectProperty<ProductDetail> product = new SimpleObjectProperty<>();
    /** Foto realista del producto (null si no tiene). Se fija antes que {@link #product}. */
    private final ObjectProperty<RealisticLook> look = new SimpleObjectProperty<>();
    private final BooleanProperty saving = new SimpleBooleanProperty(false);

    public AdminProductEditViewModel(AdminProductService admin, CatalogService catalog) {
        this.admin = admin;
        this.catalog = catalog;
    }

    /** Carga los datos de referencia y, si {@code productId} no es null, el producto. */
    public void load(Long productId, Consumer<String> onError) {
        Async.run(() -> {
                    ReferenceData data = new ReferenceData(catalog.categories(), catalog.sports(), catalog.sizes(),
                            catalog.materials(Material.Scope.TEXTIL), catalog.materials(Material.Scope.CALZADO));
                    ProductDetail detail = productId == null ? null : admin.get(productId);
                    RealisticLook realistic = productId == null ? null : admin.look(productId).orElse(null);
                    return new Loaded(data, detail, realistic);
                },
                result -> {
                    look.set(result.look());
                    reference.set(result.data());
                    product.set(result.detail());
                },
                e -> onError.accept(ErrorMessages.of(e)));
    }

    private record Loaded(ReferenceData data, ProductDetail detail, RealisticLook look) {
    }

    private record Saved(ProductDetail detail, RealisticLook look) {
    }

    /** Guarda el producto y, si cambiaron, los colores por zona de su foto realista ({@code zoneColors} vacío = no). */
    public void save(ProductForm form, Map<String, String> zoneColors, Consumer<ProductDetail> onSuccess, Consumer<Map<String, String>> onFieldErrors,
                     Consumer<String> onError) {
        saving.set(true);
        Async.run(() -> {
                    ProductDetail saved = admin.save(form);
                    RealisticLook realistic = zoneColors.isEmpty() ? admin.look(saved.id()).orElse(null)
                            : admin.updateZoneColors(saved.id(), zoneColors);
                    return new Saved(saved, realistic);
                },
                saved -> {
                    saving.set(false);
                    look.set(saved.look());
                    product.set(saved.detail());
                    onSuccess.accept(saved.detail());
                },
                e -> {
                    saving.set(false);
                    if (e instanceof ValidationException v) {
                        onFieldErrors.accept(v.fieldErrors());
                    } else {
                        onError.accept(ErrorMessages.of(e));
                    }
                });
    }

    public void addImage(Path file, Consumer<String> onError) {
        ProductDetail p = product.get();
        if (p == null) {
            return;
        }
        Async.run(() -> {
                    admin.addImage(p.id(), file);
                    return admin.get(p.id());
                },
                product::set,
                e -> onError.accept(ErrorMessages.of(e)));
    }

    public void removeImage(long imageId, Consumer<String> onError) {
        ProductDetail p = product.get();
        if (p == null) {
            return;
        }
        Async.run(() -> {
                    admin.removeImage(p.id(), imageId);
                    return admin.get(p.id());
                },
                product::set,
                e -> onError.accept(ErrorMessages.of(e)));
    }

    public ObjectProperty<ReferenceData> referenceProperty() { return reference; }
    public ObjectProperty<ProductDetail> productProperty() { return product; }
    public ObjectProperty<RealisticLook> lookProperty() { return look; }
    public BooleanProperty savingProperty() { return saving; }
}