package com.sportforce3d.service;

import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.DesignJson;
import com.sportforce3d.design.DesignPricing;
import com.sportforce3d.design.MaterialSlot;
import com.sportforce3d.model.CartLine;
import com.sportforce3d.model.CartSummary;
import com.sportforce3d.model.Garment;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.ProductDetail;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.CartRepository;
import com.sportforce3d.repository.DesignRepository;
import com.sportforce3d.repository.DesignRepository.StoredDesign;
import com.sportforce3d.repository.ProductRepository;
import com.sportforce3d.repository.ProductRepository.VariantInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Carrito del usuario conectado (cualquier rol). Valida stock y cantidades; los totales se calculan aquí.
 * Las líneas personalizadas llevan un diseño propio del usuario y el recargo de sus materiales
 * ({@link DesignPricing}), que se recalcula si el diseño cambia antes de comprar.
 */
public class CartService {

    public static final int MAX_PER_LINE = 99;

    /** Prendas que el diseñador 3D sabe dibujar ({@link Garment#DESIGNABLE}). */
    public static final Set<Garment> DESIGNABLE = Garment.DESIGNABLE;

    private final CartRepository carts;
    private final ProductRepository products;
    private final CatalogService catalog;
    private final DesignRepository designs;
    private final Session session;

    public CartService(CartRepository carts, ProductRepository products, CatalogService catalog,
                       DesignRepository designs, Session session) {
        this.carts = carts;
        this.products = products;
        this.catalog = catalog;
        this.designs = designs;
        this.session = session;
    }

    public CartSummary summary() {
        User me = session.requireUser();
        return summarize(refreshDesignSurcharges(me.id(), carts.findLines(me.id())));
    }

    CartSummary summarize(List<CartLine> lines) {
        long subtotal = lines.stream().mapToLong(l -> l.unitPriceCents() * l.quantity()).sum();
        long surcharge = lines.stream().mapToLong(l -> l.surchargeCents() * l.quantity()).sum();
        return new CartSummary(lines, subtotal, surcharge, catalog.shippingFor(subtotal + surcharge),
                catalog.freeShippingFromCents());
    }

    public int countUnits() {
        return session.current().map(u -> carts.countUnits(u.id())).orElse(0);
    }

    /** Agrega unidades de una variante; si ya está en el carrito, suma la cantidad. */
    public CartSummary add(long variantId, int quantity) {
        User me = session.requireUser();
        checkQuantity(quantity);
        VariantInfo variant = products.findVariantInfo(variantId)
                .filter(VariantInfo::purchasable)
                .orElseThrow(() -> new ServiceException("Este producto ya no está disponible."));

        var existing = carts.findStandardItem(me.id(), variantId);
        int total = quantity + existing.map(CartRepository.ExistingItem::quantity).orElse(0);
        if (total > MAX_PER_LINE) {
            throw new ServiceException("Puedes llevar hasta " + MAX_PER_LINE + " unidades de un mismo producto.");
        }
        checkStock(variant.productName() + " (talla " + variant.sizeName() + ")", total, variant.available());

        if (existing.isPresent()) {
            carts.updateQuantity(me.id(), existing.get().itemId(), total, variant.unitPriceCents());
        } else {
            carts.addItem(me.id(), variantId, quantity, variant.unitPriceCents());
        }
        return summary();
    }

    /**
     * Agrega un producto personalizable con uno de tus diseños guardados. La talla es la variante; los colores y
     * materiales salen del diseño. Si la misma talla con el mismo diseño ya está en el carrito, suma la cantidad.
     */
    public CartSummary addDesign(long designId, long variantId, int quantity) {
        User me = session.requireUser();
        checkQuantity(quantity);
        StoredDesign design = designs.find(designId)
                .filter(d -> d.userId() == me.id() && !d.deleted())
                .orElseThrow(() -> new ServiceException("No encontramos ese diseño."));
        VariantInfo variant = products.findVariantInfo(variantId)
                .filter(VariantInfo::purchasable)
                .orElseThrow(() -> new ServiceException("Este producto ya no está disponible."));
        ProductDetail product = catalog.detail(variant.productId());
        if (!product.customizable() || !DESIGNABLE.contains(product.garment())) {
            throw new ServiceException(product.name() + " no se puede personalizar en el diseñador 3D.");
        }
        long surcharge = surchargeFor(DesignJson.read(design.documentJson()), product);

        var existing = carts.findDesignItem(me.id(), variantId, designId);
        int total = quantity + existing.map(CartRepository.ExistingItem::quantity).orElse(0);
        if (total > MAX_PER_LINE) {
            throw new ServiceException("Puedes llevar hasta " + MAX_PER_LINE + " unidades de un mismo diseño y talla.");
        }
        checkStock(variant.productName() + " (talla " + variant.sizeName() + ")", total, variant.available());
        if (existing.isPresent()) {
            carts.updateQuantity(me.id(), existing.get().itemId(), total, variant.unitPriceCents());
            carts.updateSurcharge(me.id(), existing.get().itemId(), surcharge);
        } else {
            carts.addDesignItem(me.id(), variantId, designId, quantity, variant.unitPriceCents(), surcharge);
        }
        return summary();
    }

    /**
     * Recargo por unidad del diseño para ese producto. Exige que cada material elegido esté permitido para el
     * producto (product_materials); si no, el cliente debe cambiarlo en el diseñador.
     */
    long surchargeFor(DesignDocument doc, ProductDetail product) {
        Set<String> allowed = product.materials().stream().map(Material::code).collect(Collectors.toSet());
        Map<String, Material> all = catalog.materialsByCode();
        for (MaterialSlot slot : DesignPricing.slotsFor(product.garment())) {
            String code = doc.material(slot);
            if (!allowed.contains(code)) {
                Material m = all.get(code);
                throw new ServiceException("El material " + (m == null ? code : m.name()) + " (" + slot.label().toLowerCase()
                        + ") no está disponible para " + product.name() + ". Cámbialo en el diseñador.");
            }
        }
        return DesignPricing.surchargeCents(doc, all, product.garment());
    }

    /**
     * Las líneas con diseño toman el recargo de la versión actual del diseño (el cliente pudo cambiar de tela
     * después de agregarlo). Si cambió, se guarda el nuevo valor.
     */
    List<CartLine> refreshDesignSurcharges(long userId, List<CartLine> lines) {
        if (lines.stream().noneMatch(CartLine::hasDesign)) {
            return lines;
        }
        Map<String, Material> all = catalog.materialsByCode();
        List<CartLine> out = new ArrayList<>(lines.size());
        for (CartLine line : lines) {
            if (!line.hasDesign() || !line.purchasable()) {
                out.add(line);
                continue;
            }
            long current = designs.find(line.designId())
                    .map(d -> DesignPricing.surchargeCents(DesignJson.read(d.documentJson()), all, line.garment()))
                    .orElse(line.surchargeCents());
            if (current != line.surchargeCents()) {
                carts.updateSurcharge(userId, line.itemId(), current);
                out.add(line.withSurcharge(current));
            } else {
                out.add(line);
            }
        }
        return out;
    }

    public CartSummary updateQuantity(long itemId, int quantity) {
        User me = session.requireUser();
        checkQuantity(quantity);
        CartLine line = carts.findLines(me.id()).stream()
                .filter(l -> l.itemId() == itemId)
                .findFirst()
                .orElseThrow(() -> new ServiceException("Ese producto ya no está en tu carrito."));
        if (!line.purchasable()) {
            throw new ServiceException(line.productName() + " ya no está disponible. Quítalo del carrito.");
        }
        checkStock(line.productName() + " (talla " + line.sizeName() + ")", quantity, line.available());
        carts.updateQuantity(me.id(), itemId, quantity, line.unitPriceCents());
        return summary();
    }

    public CartSummary remove(long itemId) {
        User me = session.requireUser();
        carts.removeItem(me.id(), itemId);
        return summary();
    }

    private static void checkQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_PER_LINE) {
            throw ValidationException.of("quantity", "La cantidad debe estar entre 1 y " + MAX_PER_LINE + ".");
        }
    }

    static void checkStock(String what, int wanted, int available) {
        if (available <= 0) {
            throw new ServiceException(what + " está agotado.");
        }
        if (wanted > available) {
            throw new ServiceException("Solo " + (available == 1 ? "queda 1 unidad" : "quedan " + available + " unidades")
                    + " de " + what + ".");
        }
    }
}
