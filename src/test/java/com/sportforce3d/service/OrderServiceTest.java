package com.sportforce3d.service;

import com.sportforce3d.model.PpqhAddress;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.PpqhPaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceTest {

    @TempDir
    Path tempDir;

    private Fixture f;
    private long clientAddress;

    @BeforeEach
    void setUp() {
        f = new Fixture(tempDir);
        f.loginClient();
        clientAddress = f.addresses.ppqhList().get(0).ppqhId();
    }

    @Test
    void placesOrderFreezingPricesReservingStockAndClearingCart() {
        int availableBefore = f.quantity("SF-CAM-001-M") - f.reserved("SF-CAM-001-M");
        f.cart.hepyAdd(f.variantId("SF-CAM-001-M"), 2);
        f.cart.hepyAdd(f.variantId("SF-ACC-002-UNICA"), 1);

        PpqhOrderDetail order = f.orders.ppqhPlaceOrder(clientAddress, PpqhPaymentMethod.YAPE, "  Tocar el timbre  ");

        assertThat(order.ppqhOrderNumber()).isEqualTo("PED-2026-000001");
        assertThat(order.ppqhStatus()).isEqualTo(PpqhOrderStatus.PENDIENTE_PAGO);
        assertThat(order.ppqhSubtotalCents()).isEqualTo(2 * 7990 + 1990);
        assertThat(order.ppqhShippingCents()).isEqualTo(1500);
        assertThat(order.ppqhTotalCents()).isEqualTo(2 * 7990 + 1990 + 1500);
        assertThat(order.ppqhItems()).hasSize(2);
        assertThat(order.ppqhItems().get(0).ppqhUnitPriceCents()).isEqualTo(7990);
        assertThat(order.ppqhShipLine1()).isEqualTo("Jr. Mariano Núñez 245");
        assertThat(order.ppqhNotes()).isEqualTo("Tocar el timbre");
        assertThat(order.ppqhHistory()).singleElement().satisfies(h -> {
            assertThat(h.ppqhFrom()).isNull();
            assertThat(h.ppqhTo()).isEqualTo(PpqhOrderStatus.PENDIENTE_PAGO);
        });
        assertThat(f.queryLong("SELECT count(*) FROM payments WHERE order_id = " + order.ppqhId()
                + " AND status = 'PENDIENTE' AND amount_cents = " + order.ppqhTotalCents())).isEqualTo(1);

        assertThat(f.cart.hepySummary().hepyIsEmpty()).isTrue();
        assertThat(f.quantity("SF-CAM-001-M") - f.reserved("SF-CAM-001-M")).isEqualTo(availableBefore - 2);

        f.exec("UPDATE products SET base_price_cents = 1000 WHERE sku = 'SF-CAM-001'");
        assertThat(f.orders.ppqhOrder(order.ppqhId()).ppqhItems().get(0).ppqhUnitPriceCents()).isEqualTo(7990);
    }

    @Test
    void orderNumbersAreCorrelativePerYear() {
        f.cart.hepyAdd(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThat(f.orders.ppqhPlaceOrder(clientAddress, PpqhPaymentMethod.PLIN, null).ppqhOrderNumber()).isEqualTo("PED-2026-000001");
        f.cart.hepyAdd(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThat(f.orders.ppqhPlaceOrder(clientAddress, PpqhPaymentMethod.TRANSFERENCIA, null).ppqhOrderNumber()).isEqualTo("PED-2026-000002");

        f.clock.advance(Duration.ofDays(120));
        f.cart.hepyAdd(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThat(f.orders.ppqhPlaceOrder(clientAddress, PpqhPaymentMethod.YAPE, null).ppqhOrderNumber()).isEqualTo("PED-2027-000001");
    }

    @Test
    void cardPaymentIsNotAvailableAndNothingIsSimulated() {
        f.cart.hepyAdd(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThatThrownBy(() -> f.orders.ppqhPlaceOrder(clientAddress, PpqhPaymentMethod.TARJETA, null))
                .isInstanceOf(PpqhValidationException.class)
                .hasMessageContaining("próximamente");
        assertThat(f.queryLong("SELECT count(*) FROM orders")).isZero();
    }

    @Test
    void rejectsEmptyCartAndForeignAddress() {
        assertThatThrownBy(() -> f.orders.ppqhPlaceOrder(clientAddress, PpqhPaymentMethod.YAPE, null)).hasMessageContaining("vacío");

        f.loginAdmin();
        f.cart.hepyAdd(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThatThrownBy(() -> f.orders.ppqhPlaceOrder(clientAddress, PpqhPaymentMethod.YAPE, null))
                .as("la dirección es de otro usuario")
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors()).containsKey("address"));
    }

    @Test
    void failsAtomicallyWhenStockRanOutAfterAddingToCart() {
        f.cart.hepyAdd(f.variantId("SF-CAM-001-XXL"), 2);
        f.cart.hepyAdd(f.variantId("SF-ACC-002-UNICA"), 1);
        f.exec("UPDATE inventory SET quantity = 1 WHERE variant_id = " + f.variantId("SF-CAM-001-XXL"));
        int accReservedBefore = f.reserved("SF-ACC-002-UNICA");

        assertThatThrownBy(() -> f.orders.ppqhPlaceOrder(clientAddress, PpqhPaymentMethod.YAPE, null))
                .hasMessageContaining("Solo queda 1 unidad");

        assertThat(f.queryLong("SELECT count(*) FROM orders")).isZero();
        assertThat(f.queryLong("SELECT count(*) FROM order_counters")).as("el correlativo también se revierte").isZero();
        assertThat(f.reserved("SF-ACC-002-UNICA")).isEqualTo(accReservedBefore);
        assertThat(f.cart.hepySummary().hepyLines()).hasSize(2);
    }

    @Test
    void cancellingReleasesReservedStockAndRejectsThePendingPayment() {
        f.cart.hepyAdd(f.variantId("SF-CAM-001-L"), 3);
        int reservedBefore = f.reserved("SF-CAM-001-L");
        PpqhOrderDetail order = f.orders.ppqhPlaceOrder(clientAddress, PpqhPaymentMethod.YAPE, null);
        assertThat(f.reserved("SF-CAM-001-L")).isEqualTo(reservedBefore + 3);

        PpqhOrderDetail cancelled = f.orders.ppqhCancel(order.ppqhId(), null);

        assertThat(cancelled.ppqhStatus()).isEqualTo(PpqhOrderStatus.CANCELADO);
        assertThat(cancelled.ppqhLastStatusBeforeCancel()).isEqualTo(PpqhOrderStatus.PENDIENTE_PAGO);
        assertThat(cancelled.ppqhHistory()).hasSize(2);
        assertThat(f.reserved("SF-CAM-001-L")).isEqualTo(reservedBefore);
        assertThat(f.queryLong("SELECT count(*) FROM payments WHERE order_id = " + order.ppqhId() + " AND status = 'RECHAZADO'"))
                .isEqualTo(1);
        assertThatThrownBy(() -> f.orders.ppqhCancel(order.ppqhId(), null)).hasMessageContaining("ya no se puede cancelar");
    }

    @Test
    void onlyTheOwnerOrAnAdminCanSeeAnOrder() {
        f.cart.hepyAdd(f.variantId("SF-ACC-002-UNICA"), 1);
        PpqhOrderDetail order = f.orders.ppqhPlaceOrder(clientAddress, PpqhPaymentMethod.YAPE, null);
        assertThat(f.orders.ppqhMyOrders()).hasSize(1);

        f.loginNewClient("curioso@correo.pe");
        assertThat(f.orders.ppqhMyOrders()).isEmpty();
        assertThatThrownBy(() -> f.orders.ppqhOrder(order.ppqhId())).hasMessage("No encontramos ese pedido.");
        assertThatThrownBy(() -> f.orders.ppqhCancel(order.ppqhId(), null)).hasMessage("No encontramos ese pedido.");

        f.loginAdmin();
        assertThat(f.orders.ppqhOrder(order.ppqhId()).ppqhCustomerEmail()).isEqualTo(Fixture.CLIENT);
    }

    @Test
    void adminWithoutCustomerProfileCanBuyToo() {
        f.loginAdmin();
        PpqhAddress saved = f.addresses.ppqhSave(new PpqhAddress(0, "Tienda", "Administrador SportForce", "951111222", "Lima",
                "Lima", "Miraflores", "Av. Larco 1234", null, false));
        f.cart.hepyAdd(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThat(f.orders.ppqhPlaceOrder(saved.ppqhId(), PpqhPaymentMethod.PLIN, null).ppqhStatus()).isEqualTo(PpqhOrderStatus.PENDIENTE_PAGO);
    }
}
