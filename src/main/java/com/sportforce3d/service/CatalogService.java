package com.sportforce3d.service;

import com.sportforce3d.model.Category;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.Page;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.model.ProductQuery;
import com.sportforce3d.model.ProductSummary;
import com.sportforce3d.model.ProductVariant;
import com.sportforce3d.model.RealisticLook;
import com.sportforce3d.model.Size;
import com.sportforce3d.model.Sport;
import com.sportforce3d.repository.CatalogRepository;
import com.sportforce3d.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

public class CatalogService {

    public static final long DEFAULT_SHIPPING_CENTS = 1500;
    public static final long DEFAULT_FREE_SHIPPING_FROM_CENTS = 29900;

    private final CatalogRepository catalog;
    private final ProductRepository products;

    public static final int SEARCH_MAX = 100;

    public CatalogService(CatalogRepository catalog, ProductRepository products) {
        this.catalog = catalog;
        this.products = products;
    }

    public List<Category> categories() {
        return catalog.findActiveCategories();
    }

    public List<Sport> sports() {
        return catalog.findSports();
    }

    public List<Size> sizes() {
        return catalog.findSizes();
    }

    public List<ProductSummary> featured(int limit) {
        return products.findFeatured(Math.max(1, Math.min(limit, 24)));
    }

    public Page<ProductSummary> search(ProductQuery query) {
        if (query == null) {
            query = ProductQuery.all();
        }
        if (query.text() != null && query.text().length() > SEARCH_MAX) {
            throw ValidationException.of("text", "La búsqueda admite hasta " + SEARCH_MAX + " caracteres.");
        }
        if ((query.minPriceCents() != null && query.minPriceCents() < 0)
                || (query.maxPriceCents() != null && query.maxPriceCents() < 0)) {
            throw ValidationException.of("price", "Los precios no pueden ser negativos.");
        }
        if (query.minPriceCents() != null && query.maxPriceCents() != null
                && query.minPriceCents() > query.maxPriceCents()) {
            throw ValidationException.of("price", "El precio mínimo no puede ser mayor que el máximo.");
        }
        return products.search(query);
    }

    public ProductDetail detail(long productId) {
        ProductDetail d = products.findDetail(productId)
                .filter(p -> p.active() && !p.deleted())
                .orElseThrow(() -> new ServiceException("Este producto ya no está disponible."));
        List<ProductVariant> active = d.activeVariants();
        return new ProductDetail(d.id(), d.categoryId(), d.categoryName(), d.sportId(), d.sportName(), d.sku(),
                d.name(), d.description(), d.garment(), d.basePriceCents(), d.compareAtPriceCents(),
                d.customizable(), d.isNew(), d.featured(), d.active(), d.deleted(), active, d.images(), d.materials());
    }

    public List<Material> materials(Material.Scope scope) {
        return catalog.findMaterials(scope);
    }

    public List<ProductSummary> designableProducts() {
        List<ProductSummary> out = new java.util.ArrayList<>();
        for (int page = 1; ; page++) {
            Page<ProductSummary> result = search(new ProductQuery(null, null, null, java.util.Set.of(), null, null,
                    false, true, com.sportforce3d.model.ProductSort.RELEVANCE, page, 60));
            result.items().stream().filter(p -> CartService.DESIGNABLE.contains(p.garment())).forEach(out::add);
            if (!result.hasNext()) {
                return out;
            }
        }
    }

    public Optional<RealisticLook> realisticLook(long productId) {
        return products.findLook(productId);
    }

    public java.util.Map<String, Material> materialsByCode() {
        java.util.Map<String, Material> map = new java.util.HashMap<>();
        for (Material.Scope scope : Material.Scope.values()) {
            catalog.findMaterials(scope).forEach(m -> map.put(m.code(), m));
        }
        return java.util.Map.copyOf(map);
    }

    public long shippingFlatCents() {
        return catalog.findLongSetting("shipping_flat_cents").orElse(DEFAULT_SHIPPING_CENTS);
    }

    public long freeShippingFromCents() {
        return catalog.findLongSetting("free_shipping_from_cents").orElse(DEFAULT_FREE_SHIPPING_FROM_CENTS);
    }

    public long shippingFor(long goodsCents) {
        if (goodsCents <= 0) {
            return 0;
        }
        return goodsCents >= freeShippingFromCents() ? 0 : shippingFlatCents();
    }
}
