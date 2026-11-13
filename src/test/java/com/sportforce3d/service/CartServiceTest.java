package com.sportforce3d.service;

import com.sportforce3d.model.HepyCartLine;
import com.sportforce3d.model.HepyCartSummary;
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
        assertThatThrownBy(() -> f.cart.hepySummary()).isInstanceOf(PpqhAuthenticationException.class);
        assertThatThrownBy(() -> f.cart.hepyAdd(f.variantId("SF-CAM-001-M"), 1)).isInstanceOf(PpqhAuthenticationException.class);
        assertThat(f.cart.hepyCountUnits()).isZero();
    }

    @Test
    void addingTheSameVariantMergesQuantities() {
        f.loginClient();
        long m = f.variantId("SF-CAM-001-M");

        f.cart.hepyAdd(m, 1);
        HepyCartSummary summary = f.cart.hepyAdd(m, 2);

        assertThat(summary.hepyLines()).hasSize(1);
        assertThat(summary.hepyLines().get(0).hepyQuantity()).isEqualTo(3);
        assertThat(summary.hepySubtotalCents()).isEqualTo(3 * 7990);
        assertThat(f.cart.hepyCountUnits()).isEqualTo(3);
    }

    @Test
    void cannotAddMoreThanAvailableStock() {
        f.loginClient();
        long xxl = f.variantId("SF-CAM-001-XXL");

        assertThatThrownBy(() -> f.cart.hepyAdd(xxl, 3)).hasMessage("Solo quedan 2 unidades de Camiseta Pro Dry-Fit (talla XXL).");
        f.cart.hepyAdd(xxl, 2);
        assertThatThrownBy(() -> f.cart.hepyAdd(xxl, 1)).hasMessageContaining("Solo quedan 2");

        long soldOut = f.variantId("SF-ZAP-003-40");
        assertThatThrownBy(() -> f.cart.hepyAdd(soldOut, 1)).hasMessageContaining("agotado");
    }

    @Test
    void validatesQuantityRange() {
        f.loginClient();
        long m = f.variantId("SF-CAM-001-M");
        assertThatThrownBy(() -> f.cart.hepyAdd(m, 0)).isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.cart.hepyAdd(m, 100)).isInstanceOf(PpqhValidationException.class);
    }

    @Test
    void updatesAndRemovesOnlyOwnLines() {
        f.loginClient();
        HepyCartSummary summary = f.cart.hepyAdd(f.variantId("SF-CAM-001-M"), 1);
        long itemId = summary.hepyLines().get(0).hepyItemId();
        assertThat(f.cart.hepyUpdateQuantity(itemId, 4).hepyLines().get(0).hepyQuantity()).isEqualTo(4);

        f.loginNewClient("otro@correo.pe");
        assertThatThrownBy(() -> f.cart.hepyUpdateQuantity(itemId, 1)).hasMessageContaining("ya no está en tu carrito");
        f.cart.hepyRemove(itemId);

        f.loginClient();
        assertThat(f.cart.hepySummary().hepyLines()).hasSize(1);
        assertThat(f.cart.hepyRemove(itemId).hepyIsEmpty()).isTrue();
    }

    @Test
    void shippingIsFlatUntilTheFreeThreshold() {
        f.loginClient();
        HepyCartSummary cheap = f.cart.hepyAdd(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThat(cheap.hepyShippingCents()).isEqualTo(1500);
        assertThat(cheap.hepyTotalCents()).isEqualTo(1990 + 1500);
        assertThat(cheap.hepyMissingForFreeShipping()).isEqualTo(29900 - 1990);

        HepyCartSummary big = f.cart.hepyAdd(f.variantId("SF-ZAP-001-40"), 2);
        assertThat(big.hepyShippingCents()).isZero();
        assertThat(big.hepyMissingForFreeShipping()).isZero();
    }

    @Test
    void linesOfDeletedProductsBlockCheckout() {
        f.loginClient();
        f.cart.hepyAdd(f.variantId("SF-CAM-002-M"), 1);
        f.products.hepySoftDelete(f.productId("SF-CAM-002"), Instant.now());

        HepyCartSummary summary = f.cart.hepySummary();
        HepyCartLine line = summary.hepyLines().get(0);
        assertThat(line.hepyPurchasable()).isFalse();
        assertThat(summary.hepyCanCheckout()).isFalse();
        assertThatThrownBy(() -> f.cart.hepyUpdateQuantity(line.hepyItemId(), 2)).hasMessageContaining("ya no está disponible");
    }

    @Test
    void cartShowsTheCurrentPrice() {
        f.loginClient();
        f.cart.hepyAdd(f.variantId("SF-CAM-001-M"), 1);
        f.exec("UPDATE products SET base_price_cents = 6990 WHERE sku = 'SF-CAM-001'");
        assertThat(f.cart.hepySummary().hepySubtotalCents()).isEqualTo(6990);
    }
}
