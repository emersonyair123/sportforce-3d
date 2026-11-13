package com.sportforce3d.service;

import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.Payment;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private static final String SKU = "SF-CAM-001-M";

    @TempDir
    Path tempDir;

    private Fixture f;
    private OrderDetail order;
    private int quantityBefore;
    private int reservedBefore;
    private Path proof;

    @BeforeEach
    void setUp() throws Exception {
        f = new Fixture(tempDir);
        quantityBefore = f.quantity(SKU);
        reservedBefore = f.reserved(SKU);
        f.loginClient();
        f.cart.add(f.variantId(SKU), 2);
        order = f.orders.placeOrder(f.addresses.list().get(0).id(), PaymentMethod.YAPE, null);
        proof = png(tempDir.resolve("captura-yape.png"));
    }

    private static Path png(Path p) throws Exception {
        Files.write(p, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0});
        return p;
    }

    private long storedFiles(String folder) throws Exception {
        Path dir = tempDir.resolve("storage").resolve(folder);
        if (!Files.exists(dir)) {
            return 0;
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.count();
        }
    }

    private Payment register(String code) {
        return f.payments.register(order.id(), code, proof).currentPayment();
    }

    @Test
    void clientRegistersCodeAndProofButOrderIsNeverMarkedPaid() throws Exception {
        OrderDetail updated = f.payments.register(order.id(), " 1234 5678 ", proof);

        assertThat(updated.status()).isEqualTo(OrderStatus.PAGO_EN_VERIFICACION);
        Payment p = updated.currentPayment();
        assertThat(p.status()).isEqualTo(PaymentStatus.EN_VERIFICACION);
        assertThat(p.operationCode()).isEqualTo("12345678");
        assertThat(p.verifiedBy()).isNull();
        assertThat(p.proofs()).singleElement().satisfies(pr -> {
            assertThat(pr.path()).startsWith("payments/");
            assertThat(pr.mimeType()).isEqualTo("image/png");
            assertThat(Files.exists(f.storage.resolve(pr.path()))).isTrue();
        });
        assertThat(updated.history()).last().satisfies(h -> {
            assertThat(h.to()).isEqualTo(OrderStatus.PAGO_EN_VERIFICACION);
            assertThat(h.note()).contains("Yape", "12345678");
        });
        assertThat(f.quantity(SKU)).isEqualTo(quantityBefore);
        assertThat(f.reserved(SKU)).isEqualTo(reservedBefore + 2);
        assertThat(f.queryLong("SELECT count(*) FROM payments WHERE status = 'APROBADO'")).isZero();
    }

    @Test
    void registrationIsValidatedAndLeavesNoFilesBehind() throws Exception {
        assertThatThrownBy(() -> register("12")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> register("abc$%&123")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.payments.register(order.id(), "12345678", null)).isInstanceOf(ValidationException.class);
        Path txt = tempDir.resolve("comprobante.png");
        Files.writeString(txt, "no es una imagen");
        assertThatThrownBy(() -> f.payments.register(order.id(), "12345678", txt)).isInstanceOf(ValidationException.class);
        assertThat(storedFiles("payments")).isZero();

        f.loginNewClient("intruso@correo.pe");
        assertThatThrownBy(() -> register("12345678")).hasMessage("No encontramos ese pedido.");

        f.loginClient();
        register("12345678");
        assertThatThrownBy(() -> register("87654321")).hasMessageContaining("no espera un pago");
    }

    @Test
    void sameOperationCodeCannotPayTwoOrders() throws Exception {
        register("55556666");
        f.cart.add(f.variantId("SF-ACC-002-UNICA"), 1);
        OrderDetail other = f.orders.placeOrder(f.addresses.list().get(0).id(), PaymentMethod.YAPE, null);
        assertThatThrownBy(() -> f.payments.register(other.id(), "55556666", proof))
                .hasMessageContaining("ya se registró");
        assertThat(f.orders.order(other.id()).status()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
        assertThat(storedFiles("payments")).as("el comprobante del intento fallido se borra").isEqualTo(1);
    }

    @Test
    void approvingConvertsTheReservationIntoASale() {
        Payment p = register("12345678");
        assertThatThrownBy(() -> f.payments.approve(p.id(), null)).isInstanceOf(AccessDeniedException.class);

        f.loginAdmin();
        assertThat(f.payments.queue(PaymentStatus.EN_VERIFICACION)).extracting(Payment::id).containsExactly(p.id());
        OrderDetail paid = f.payments.approve(p.id(), "Visto en la app de Yape");

        assertThat(paid.status()).isEqualTo(OrderStatus.PAGADO);
        assertThat(paid.currentPayment().status()).isEqualTo(PaymentStatus.APROBADO);
        assertThat(paid.currentPayment().verifiedBy()).isEqualTo("Administrador SportForce");
        assertThat(paid.currentPayment().verifiedAt()).isNotNull();
        assertThat(paid.history()).last().satisfies(h -> {
            assertThat(h.to()).isEqualTo(OrderStatus.PAGADO);
            assertThat(h.note()).contains("Pago aprobado", "12345678", "Visto en la app de Yape");
        });
        assertThat(f.quantity(SKU)).isEqualTo(quantityBefore - 2);
        assertThat(f.reserved(SKU)).isEqualTo(reservedBefore);
        assertThat(f.payments.queue(PaymentStatus.EN_VERIFICACION)).isEmpty();

        assertThatThrownBy(() -> f.payments.approve(p.id(), null)).hasMessageContaining("ya no está en verificación");
        assertThat(f.quantity(SKU)).as("aprobar dos veces no descuenta dos veces").isEqualTo(quantityBefore - 2);
    }

    @Test
    void approvalIsAllOrNothingWhenInventoryDoesNotMatch() {
        Payment p = register("12345678");
        f.exec("UPDATE inventory SET reserved = 0 WHERE variant_id = " + f.variantId(SKU));
        f.loginAdmin();
        assertThatThrownBy(() -> f.payments.approve(p.id(), null)).hasMessageContaining("no coincide");
        assertThat(f.adminOrders.order(order.id()).status()).isEqualTo(OrderStatus.PAGO_EN_VERIFICACION);
        assertThat(f.payments.payment(p.id()).status()).isEqualTo(PaymentStatus.EN_VERIFICACION);
        assertThat(f.quantity(SKU)).isEqualTo(quantityBefore);
    }

    @Test
    void rejectingReturnsTheOrderToPendingWithANewPaymentAndKeepsTheReservation() {
        Payment p = register("12345678");
        f.loginAdmin();
        assertThatThrownBy(() -> f.payments.reject(p.id(), "  ")).isInstanceOf(ValidationException.class);

        OrderDetail back = f.payments.reject(p.id(), "El monto no coincide con la captura");
        assertThat(back.status()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
        assertThat(back.payments()).hasSize(2);
        assertThat(back.payments().get(0).status()).isEqualTo(PaymentStatus.RECHAZADO);
        assertThat(back.payments().get(0).rejectionReason()).isEqualTo("El monto no coincide con la captura");
        assertThat(back.currentPayment().status()).isEqualTo(PaymentStatus.PENDIENTE);
        assertThat(back.currentPayment().amountCents()).isEqualTo(order.totalCents());
        assertThat(f.reserved(SKU)).isEqualTo(reservedBefore + 2);
        assertThat(f.quantity(SKU)).isEqualTo(quantityBefore);

        f.loginClient();
        assertThat(register("12345678").status()).isEqualTo(PaymentStatus.EN_VERIFICACION);
    }

    @Test
    void adminMovesThePaidOrderThroughProductionButNeverMarksPaidByHand() {
        Payment p = register("12345678");
        f.loginAdmin();
        assertThatThrownBy(() -> f.adminOrders.changeStatus(order.id(), OrderStatus.PAGADO, null))
                .hasMessageContaining("aprobar su pago");
        assertThatThrownBy(() -> f.adminOrders.changeStatus(order.id(), OrderStatus.EN_PRODUCCION, null))
                .hasMessageContaining("No se puede pasar");

        f.payments.approve(p.id(), null);
        OrderDetail prod = f.adminOrders.changeStatus(order.id(), OrderStatus.EN_PRODUCCION, "Corte y sublimado");
        assertThat(prod.status()).isEqualTo(OrderStatus.EN_PRODUCCION);
        assertThat(prod.history()).last().satisfies(h -> {
            assertThat(h.from()).isEqualTo(OrderStatus.PAGADO);
            assertThat(h.note()).isEqualTo("Corte y sublimado");
            assertThat(h.changedBy()).isEqualTo("Administrador SportForce");
        });
        assertThatThrownBy(() -> f.adminOrders.changeStatus(order.id(), OrderStatus.CANCELADO, "Sin tela"))
                .hasMessageContaining("No se puede pasar");
        assertThat(AdminOrderService.nextStates(OrderStatus.EN_PRODUCCION)).containsExactly(OrderStatus.ENVIADO);

        f.loginClient();
        assertThatThrownBy(() -> f.adminOrders.changeStatus(order.id(), OrderStatus.ENVIADO, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCancelBeforePaymentNeedsAReasonAndReleasesStock() {
        f.loginAdmin();
        assertThatThrownBy(() -> f.adminOrders.changeStatus(order.id(), OrderStatus.CANCELADO, null))
                .isInstanceOf(ValidationException.class);
        OrderDetail cancelled = f.adminOrders.changeStatus(order.id(), OrderStatus.CANCELADO, "Cliente pidió anular");
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELADO);
        assertThat(cancelled.currentPayment().status()).isEqualTo(PaymentStatus.RECHAZADO);
        assertThat(f.reserved(SKU)).isEqualTo(reservedBefore);
    }

    @Test
    void dashboardCountsApprovedPaymentsAsSales() {
        Payment p = register("12345678");
        f.loginAdmin();
        assertThat(f.dashboard.stats().todaySales().cents()).isZero();
        assertThat(f.dashboard.stats().pendingPayments()).isEqualTo(1);

        f.payments.approve(p.id(), null);
        DashboardService.Stats s = f.dashboard.stats();
        assertThat(s.todaySales().cents()).isEqualTo(order.totalCents());
        assertThat(s.todaySales().count()).isEqualTo(1);
        assertThat(s.monthSales().cents()).isEqualTo(order.totalCents());
        assertThat(s.pendingPayments()).isZero();
        assertThat(s.ordersByStatus().get(OrderStatus.PAGADO)).isEqualTo(1);
        assertThat(s.daily()).hasSize(DashboardService.DAYS).containsEntry(s.today(), order.totalCents());
        assertThat(s.topProducts()).first().satisfies(t -> {
            assertThat(t.name()).isEqualTo("Camiseta Pro Dry-Fit");
            assertThat(t.units()).isEqualTo(2);
        });

        f.loginClient();
        assertThatThrownBy(() -> f.dashboard.stats()).isInstanceOf(AccessDeniedException.class);
    }
}
