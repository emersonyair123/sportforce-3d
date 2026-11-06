package com.sportforce3d.service;

import com.sportforce3d.model.CartLine;
import com.sportforce3d.model.CartSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartServiceTest {

    @TempDir
    Path tempDir;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = new Fixture(tempDir);
    }

    @Test
    void requiresSession() {
        assertThatThrownBy(() -> f.cart.summary()).isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> f.cart.add(f.variantId("SF-CAM-001-M"), 1)).isInstanceOf(AuthenticationException.class);
        assertThat(f.cart.countUnits()).isZero();
    }

    @Test
    void addingTheSameVariantMergesQuantities() {
        f.loginClient();
        long m = f.variantId("SF-CAM-001-M");

        f.cart.add(m, 1);
        CartSummary summary = f.cart.add(m, 2);

        assertThat(summary.lines()).hasSize(1);
        assertThat(summary.lines().get(0).quantity()).isEqualTo(3);
        assertThat(summary.subtotalCents()).isEqualTo(3 * 7990);
        assertThat(f.cart.countUnits()).isEqualTo(3);
    }

    @Test
    void cannotAddMoreThanAvailableStock() {
        f.loginClient();
        long xxl = f.variantId("SF-CAM-001-XXL"); // el seed deja 2 unidades

        assertThatThrownBy(() -> f.cart.add(xxl, 3)).hasMessage("Solo quedan 2 unidades de Camiseta Pro Dry-Fit (talla XXL).");
        f.cart.add(xxl, 2);
        assertThatThrownBy(() -> f.cart.add(xxl, 1)).hasMessageContaining("Solo quedan 2");

        long soldOut = f.variantId("SF-ZAP-003-40");
        assertThatThrownBy(() -> f.cart.add(soldOut, 1)).hasMessageContaining("agotado");
    }

    @Test
    void validatesQuantityRange() {
        f.loginClient();
        long m = f.variantId("SF-CAM-001-M");
        assertThatThrownBy(() -> f.cart.add(m, 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.cart.add(m, 100)).isInstanceOf(ValidationException.class);
    }

    @Test
    void updatesAndRemovesOnlyOwnLines() {
        f.loginClient();
        CartSummary summary = f.cart.add(f.variantId("SF-CAM-001-M"), 1);
        long itemId = summary.lines().get(0).itemId();
        assertThat(f.cart.updateQuantity(itemId, 4).lines().get(0).quantity()).isEqualTo(4);

        // Otro usuario no puede tocar esa línea.
        f.loginNewClient("otro@correo.pe");
        assertThatThrownBy(() -> f.cart.updateQuantity(itemId, 1)).hasMessageContaining("ya no está en tu carrito");
        f.cart.remove(itemId);

        f.loginClient();
        assertThat(f.cart.summary().lines()).hasSize(1);
        assertThat(f.cart.remove(itemId).isEmpty()).isTrue();
    }

    @Test
    void shippingIsFlatUntilTheFreeThreshold() {
        f.loginClient();
        CartSummary cheap = f.cart.add(f.variantId("SF-ACC-002-UNICA"), 1); // S/ 19.90
        assertThat(cheap.shippingCents()).isEqualTo(1500);
        assertThat(cheap.totalCents()).isEqualTo(1990 + 1500);
        assertThat(cheap.missingForFreeShipping()).isEqualTo(29900 - 1990);

        CartSummary big = f.cart.add(f.variantId("SF-ZAP-001-40"), 2); // 2 × S/ 249.90
        assertThat(big.shippingCents()).isZero();
        assertThat(big.missingForFreeShipping()).isZero();
    }

    @Test
    void linesOfDeletedProductsBlockCheckout() {
        f.loginClient();
        f.cart.add(f.variantId("SF-CAM-002-M"), 1);
        f.products.softDelete(f.productId("SF-CAM-002"), Instant.now());

        CartSummary summary = f.cart.summary();
        CartLine line = summary.lines().get(0);
        assertThat(line.purchasable()).isFalse();
        assertThat(summary.canCheckout()).isFalse();
        assertThatThrownBy(() -> f.cart.updateQuantity(line.itemId(), 2)).hasMessageContaining("ya no está disponible");
    }

    @Test
    void cartShowsTheCurrentPrice() {
        f.loginClient();
        f.cart.add(f.variantId("SF-CAM-001-M"), 1);
        f.exec("UPDATE products SET base_price_cents = 6990 WHERE sku = 'SF-CAM-001'");
        assertThat(f.cart.summary().subtotalCents()).isEqualTo(6990);
    }
}
