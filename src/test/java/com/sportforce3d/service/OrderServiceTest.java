package com.sportforce3d.service;

import com.sportforce3d.model.Address;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.PaymentMethod;
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
        clientAddress = f.addresses.list().get(0).id();
    }

    @Test
    void placesOrderFreezingPricesReservingStockAndClearingCart() {
        int availableBefore = f.quantity("SF-CAM-001-M") - f.reserved("SF-CAM-001-M");
        f.cart.add(f.variantId("SF-CAM-001-M"), 2);           // 2 × 79.90
        f.cart.add(f.variantId("SF-ACC-002-UNICA"), 1);       // 19.90

        OrderDetail order = f.orders.placeOrder(clientAddress, PaymentMethod.YAPE, "  Tocar el timbre  ");

        assertThat(order.orderNumber()).isEqualTo("PED-2026-000001");
        assertThat(order.status()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
        assertThat(order.subtotalCents()).isEqualTo(2 * 7990 + 1990);
        assertThat(order.shippingCents()).isEqualTo(1500);
        assertThat(order.totalCents()).isEqualTo(2 * 7990 + 1990 + 1500);
        assertThat(order.items()).hasSize(2);
        assertThat(order.items().get(0).unitPriceCents()).isEqualTo(7990);
        assertThat(order.shipLine1()).isEqualTo("Jr. Mariano Núñez 245");
        assertThat(order.notes()).isEqualTo("Tocar el timbre");
        assertThat(order.history()).singleElement().satisfies(h -> {
            assertThat(h.from()).isNull();
            assertThat(h.to()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
        });
        assertThat(f.queryLong("SELECT count(*) FROM payments WHERE order_id = " + order.id()
                + " AND status = 'PENDIENTE' AND amount_cents = " + order.totalCents())).isEqualTo(1);

        assertThat(f.cart.summary().isEmpty()).isTrue();
        assertThat(f.quantity("SF-CAM-001-M") - f.reserved("SF-CAM-001-M")).isEqualTo(availableBefore - 2);

        // Un cambio de precio posterior no afecta al pedido.
        f.exec("UPDATE products SET base_price_cents = 1000 WHERE sku = 'SF-CAM-001'");
        assertThat(f.orders.order(order.id()).items().get(0).unitPriceCents()).isEqualTo(7990);
    }

    @Test
    void orderNumbersAreCorrelativePerYear() {
        f.cart.add(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThat(f.orders.placeOrder(clientAddress, PaymentMethod.PLIN, null).orderNumber()).isEqualTo("PED-2026-000001");
        f.cart.add(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThat(f.orders.placeOrder(clientAddress, PaymentMethod.TRANSFERENCIA, null).orderNumber()).isEqualTo("PED-2026-000002");

        f.clock.advance(Duration.ofDays(120)); // 2027
        f.cart.add(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThat(f.orders.placeOrder(clientAddress, PaymentMethod.YAPE, null).orderNumber()).isEqualTo("PED-2027-000001");
    }

    @Test
    void cardPaymentIsNotAvailableAndNothingIsSimulated() {
        f.cart.add(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThatThrownBy(() -> f.orders.placeOrder(clientAddress, PaymentMethod.TARJETA, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("próximamente");
        assertThat(f.queryLong("SELECT count(*) FROM orders")).isZero();
    }

    @Test
    void rejectsEmptyCartAndForeignAddress() {
        assertThatThrownBy(() -> f.orders.placeOrder(clientAddress, PaymentMethod.YAPE, null)).hasMessageContaining("vacío");

        f.loginAdmin();
        f.cart.add(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThatThrownBy(() -> f.orders.placeOrder(clientAddress, PaymentMethod.YAPE, null))
                .as("la dirección es de otro usuario")
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors()).containsKey("address"));
    }

    @Test
    void failsAtomicallyWhenStockRanOutAfterAddingToCart() {
        f.cart.add(f.variantId("SF-CAM-001-XXL"), 2);
        f.cart.add(f.variantId("SF-ACC-002-UNICA"), 1);
        f.exec("UPDATE inventory SET quantity = 1 WHERE variant_id = " + f.variantId("SF-CAM-001-XXL"));
        int accReservedBefore = f.reserved("SF-ACC-002-UNICA");

        assertThatThrownBy(() -> f.orders.placeOrder(clientAddress, PaymentMethod.YAPE, null))
                .hasMessageContaining("Solo queda 1 unidad");

        assertThat(f.queryLong("SELECT count(*) FROM orders")).isZero();
        assertThat(f.queryLong("SELECT count(*) FROM order_counters")).as("el correlativo también se revierte").isZero();
        assertThat(f.reserved("SF-ACC-002-UNICA")).isEqualTo(accReservedBefore);
        assertThat(f.cart.summary().lines()).hasSize(2);
    }

    @Test
    void cancellingReleasesReservedStockAndRejectsThePendingPayment() {
        f.cart.add(f.variantId("SF-CAM-001-L"), 3);
        int reservedBefore = f.reserved("SF-CAM-001-L");
        OrderDetail order = f.orders.placeOrder(clientAddress, PaymentMethod.YAPE, null);
        assertThat(f.reserved("SF-CAM-001-L")).isEqualTo(reservedBefore + 3);

        OrderDetail cancelled = f.orders.cancel(order.id(), null);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELADO);
        assertThat(cancelled.lastStatusBeforeCancel()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
        assertThat(cancelled.history()).hasSize(2);
        assertThat(f.reserved("SF-CAM-001-L")).isEqualTo(reservedBefore);
        assertThat(f.queryLong("SELECT count(*) FROM payments WHERE order_id = " + order.id() + " AND status = 'RECHAZADO'"))
                .isEqualTo(1);
        assertThatThrownBy(() -> f.orders.cancel(order.id(), null)).hasMessageContaining("ya no se puede cancelar");
    }

    @Test
    void onlyTheOwnerOrAnAdminCanSeeAnOrder() {
        f.cart.add(f.variantId("SF-ACC-002-UNICA"), 1);
        OrderDetail order = f.orders.placeOrder(clientAddress, PaymentMethod.YAPE, null);
        assertThat(f.orders.myOrders()).hasSize(1);

        f.loginNewClient("curioso@correo.pe");
        assertThat(f.orders.myOrders()).isEmpty();
        assertThatThrownBy(() -> f.orders.order(order.id())).hasMessage("No encontramos ese pedido.");
        assertThatThrownBy(() -> f.orders.cancel(order.id(), null)).hasMessage("No encontramos ese pedido.");

        f.loginAdmin();
        assertThat(f.orders.order(order.id()).customerEmail()).isEqualTo(Fixture.CLIENT);
    }

    @Test
    void adminWithoutCustomerProfileCanBuyToo() {
        f.loginAdmin();
        Address saved = f.addresses.save(new Address(0, "Tienda", "Administrador SportForce", "951111222", "Lima",
                "Lima", "Miraflores", "Av. Larco 1234", null, false));
        f.cart.add(f.variantId("SF-ACC-002-UNICA"), 1);
        assertThat(f.orders.placeOrder(saved.id(), PaymentMethod.PLIN, null).status()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
    }
}
