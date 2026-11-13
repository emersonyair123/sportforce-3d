package com.sportforce3d.service;

import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhOrderStatus;
import com.sportforce3d.model.PpqhPayment;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.model.PpqhPaymentStatus;
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
    private PpqhOrderDetail order;
    private int quantityBefore;
    private int reservedBefore;
    private Path proof;

    @BeforeEach
    void setUp() throws Exception {
        f = new Fixture(tempDir);
        quantityBefore = f.quantity(SKU);
        reservedBefore = f.reserved(SKU);
        f.loginClient();
        f.cart.hepyAdd(f.variantId(SKU), 2);
        order = f.orders.ppqhPlaceOrder(f.addresses.ppqhList().get(0).ppqhId(), PpqhPaymentMethod.YAPE, null);
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

    private PpqhPayment register(String code) {
        return f.payments.ppqhRegister(order.ppqhId(), code, proof).ppqhCurrentPayment();
    }

    @Test
    void clientRegistersCodeAndProofButOrderIsNeverMarkedPaid() throws Exception {
        PpqhOrderDetail updated = f.payments.ppqhRegister(order.ppqhId(), " 1234 5678 ", proof);

        assertThat(updated.ppqhStatus()).isEqualTo(PpqhOrderStatus.PAGO_EN_VERIFICACION);
        PpqhPayment p = updated.ppqhCurrentPayment();
        assertThat(p.ppqhStatus()).isEqualTo(PpqhPaymentStatus.EN_VERIFICACION);
        assertThat(p.ppqhOperationCode()).isEqualTo("12345678");
        assertThat(p.ppqhVerifiedBy()).isNull();
        assertThat(p.ppqhProofs()).singleElement().satisfies(pr -> {
            assertThat(pr.ppqhPath()).startsWith("payments/");
            assertThat(pr.ppqhMimeType()).isEqualTo("image/png");
            assertThat(Files.exists(f.storage.hepyResolve(pr.ppqhPath()))).isTrue();
        });
        assertThat(updated.ppqhHistory()).last().satisfies(h -> {
            assertThat(h.ppqhTo()).isEqualTo(PpqhOrderStatus.PAGO_EN_VERIFICACION);
            assertThat(h.ppqhNote()).contains("Yape", "12345678");
        });
        assertThat(f.quantity(SKU)).isEqualTo(quantityBefore);
        assertThat(f.reserved(SKU)).isEqualTo(reservedBefore + 2);
        assertThat(f.queryLong("SELECT count(*) FROM payments WHERE status = 'APROBADO'")).isZero();
    }

    @Test
    void registrationIsValidatedAndLeavesNoFilesBehind() throws Exception {
        assertThatThrownBy(() -> register("12")).isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> register("abc$%&123")).isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.payments.ppqhRegister(order.ppqhId(), "12345678", null)).isInstanceOf(PpqhValidationException.class);
        Path txt = tempDir.resolve("comprobante.png");
        Files.writeString(txt, "no es una imagen");
        assertThatThrownBy(() -> f.payments.ppqhRegister(order.ppqhId(), "12345678", txt)).isInstanceOf(PpqhValidationException.class);
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
        f.cart.hepyAdd(f.variantId("SF-ACC-002-UNICA"), 1);
        PpqhOrderDetail other = f.orders.ppqhPlaceOrder(f.addresses.ppqhList().get(0).ppqhId(), PpqhPaymentMethod.YAPE, null);
        assertThatThrownBy(() -> f.payments.ppqhRegister(other.ppqhId(), "55556666", proof))
                .hasMessageContaining("ya se registró");
        assertThat(f.orders.ppqhOrder(other.ppqhId()).ppqhStatus()).isEqualTo(PpqhOrderStatus.PENDIENTE_PAGO);
        assertThat(storedFiles("payments")).as("el comprobante del intento fallido se borra").isEqualTo(1);
    }

    @Test
    void approvingConvertsTheReservationIntoASale() {
        PpqhPayment p = register("12345678");
        assertThatThrownBy(() -> f.payments.ppqhApprove(p.ppqhId(), null)).isInstanceOf(PpqhAccessDeniedException.class);

        f.loginAdmin();
        assertThat(f.payments.ppqhQueue(PpqhPaymentStatus.EN_VERIFICACION)).extracting(PpqhPayment::ppqhId).containsExactly(p.ppqhId());
        PpqhOrderDetail paid = f.payments.ppqhApprove(p.ppqhId(), "Visto en la app de Yape");

        assertThat(paid.ppqhStatus()).isEqualTo(PpqhOrderStatus.PAGADO);
        assertThat(paid.ppqhCurrentPayment().ppqhStatus()).isEqualTo(PpqhPaymentStatus.APROBADO);
        assertThat(paid.ppqhCurrentPayment().ppqhVerifiedBy()).isEqualTo("Administrador SportForce");
        assertThat(paid.ppqhCurrentPayment().ppqhVerifiedAt()).isNotNull();
        assertThat(paid.ppqhHistory()).last().satisfies(h -> {
            assertThat(h.ppqhTo()).isEqualTo(PpqhOrderStatus.PAGADO);
            assertThat(h.ppqhNote()).contains("Pago aprobado", "12345678", "Visto en la app de Yape");
        });
        assertThat(f.quantity(SKU)).isEqualTo(quantityBefore - 2);
        assertThat(f.reserved(SKU)).isEqualTo(reservedBefore);
        assertThat(f.payments.ppqhQueue(PpqhPaymentStatus.EN_VERIFICACION)).isEmpty();

        assertThatThrownBy(() -> f.payments.ppqhApprove(p.ppqhId(), null)).hasMessageContaining("ya no está en verificación");
        assertThat(f.quantity(SKU)).as("aprobar dos veces no descuenta dos veces").isEqualTo(quantityBefore - 2);
    }

    @Test
    void approvalIsAllOrNothingWhenInventoryDoesNotMatch() {
        PpqhPayment p = register("12345678");
        f.exec("UPDATE inventory SET reserved = 0 WHERE variant_id = " + f.variantId(SKU));
        f.loginAdmin();
        assertThatThrownBy(() -> f.payments.ppqhApprove(p.ppqhId(), null)).hasMessageContaining("no coincide");
        assertThat(f.adminOrders.ppqhOrder(order.ppqhId()).ppqhStatus()).isEqualTo(PpqhOrderStatus.PAGO_EN_VERIFICACION);
        assertThat(f.payments.ppqhPayment(p.ppqhId()).ppqhStatus()).isEqualTo(PpqhPaymentStatus.EN_VERIFICACION);
        assertThat(f.quantity(SKU)).isEqualTo(quantityBefore);
    }

    @Test
    void rejectingReturnsTheOrderToPendingWithANewPaymentAndKeepsTheReservation() {
        PpqhPayment p = register("12345678");
        f.loginAdmin();
        assertThatThrownBy(() -> f.payments.ppqhReject(p.ppqhId(), "  ")).isInstanceOf(PpqhValidationException.class);

        PpqhOrderDetail back = f.payments.ppqhReject(p.ppqhId(), "El monto no coincide con la captura");
        assertThat(back.ppqhStatus()).isEqualTo(PpqhOrderStatus.PENDIENTE_PAGO);
        assertThat(back.ppqhPayments()).hasSize(2);
        assertThat(back.ppqhPayments().get(0).ppqhStatus()).isEqualTo(PpqhPaymentStatus.RECHAZADO);
        assertThat(back.ppqhPayments().get(0).ppqhRejectionReason()).isEqualTo("El monto no coincide con la captura");
        assertThat(back.ppqhCurrentPayment().ppqhStatus()).isEqualTo(PpqhPaymentStatus.PENDIENTE);
        assertThat(back.ppqhCurrentPayment().ppqhAmountCents()).isEqualTo(order.ppqhTotalCents());
        assertThat(f.reserved(SKU)).isEqualTo(reservedBefore + 2);
        assertThat(f.quantity(SKU)).isEqualTo(quantityBefore);

        f.loginClient();
        assertThat(register("12345678").ppqhStatus()).isEqualTo(PpqhPaymentStatus.EN_VERIFICACION);
    }

    @Test
    void adminMovesThePaidOrderThroughProductionButNeverMarksPaidByHand() {
        PpqhPayment p = register("12345678");
        f.loginAdmin();
        assertThatThrownBy(() -> f.adminOrders.ppqhChangeStatus(order.ppqhId(), PpqhOrderStatus.PAGADO, null))
                .hasMessageContaining("aprobar su pago");
        assertThatThrownBy(() -> f.adminOrders.ppqhChangeStatus(order.ppqhId(), PpqhOrderStatus.EN_PRODUCCION, null))
                .hasMessageContaining("No se puede pasar");

        f.payments.ppqhApprove(p.ppqhId(), null);
        PpqhOrderDetail prod = f.adminOrders.ppqhChangeStatus(order.ppqhId(), PpqhOrderStatus.EN_PRODUCCION, "Corte y sublimado");
        assertThat(prod.ppqhStatus()).isEqualTo(PpqhOrderStatus.EN_PRODUCCION);
        assertThat(prod.ppqhHistory()).last().satisfies(h -> {
            assertThat(h.ppqhFrom()).isEqualTo(PpqhOrderStatus.PAGADO);
            assertThat(h.ppqhNote()).isEqualTo("Corte y sublimado");
            assertThat(h.ppqhChangedBy()).isEqualTo("Administrador SportForce");
        });
        assertThatThrownBy(() -> f.adminOrders.ppqhChangeStatus(order.ppqhId(), PpqhOrderStatus.CANCELADO, "Sin tela"))
                .hasMessageContaining("No se puede pasar");
        assertThat(PpqhAdminOrderService.ppqhNextStates(PpqhOrderStatus.EN_PRODUCCION)).containsExactly(PpqhOrderStatus.ENVIADO);

        f.loginClient();
        assertThatThrownBy(() -> f.adminOrders.ppqhChangeStatus(order.ppqhId(), PpqhOrderStatus.ENVIADO, null))
                .isInstanceOf(PpqhAccessDeniedException.class);
    }

    @Test
    void adminCancelBeforePaymentNeedsAReasonAndReleasesStock() {
        f.loginAdmin();
        assertThatThrownBy(() -> f.adminOrders.ppqhChangeStatus(order.ppqhId(), PpqhOrderStatus.CANCELADO, null))
                .isInstanceOf(PpqhValidationException.class);
        PpqhOrderDetail cancelled = f.adminOrders.ppqhChangeStatus(order.ppqhId(), PpqhOrderStatus.CANCELADO, "Cliente pidió anular");
        assertThat(cancelled.ppqhStatus()).isEqualTo(PpqhOrderStatus.CANCELADO);
        assertThat(cancelled.ppqhCurrentPayment().ppqhStatus()).isEqualTo(PpqhPaymentStatus.RECHAZADO);
        assertThat(f.reserved(SKU)).isEqualTo(reservedBefore);
    }

    @Test
    void dashboardCountsApprovedPaymentsAsSales() {
        PpqhPayment p = register("12345678");
        f.loginAdmin();
        assertThat(f.dashboard.ppqhStats().ppqhTodaySales().hepyCents()).isZero();
        assertThat(f.dashboard.ppqhStats().ppqhPendingPayments()).isEqualTo(1);

        f.payments.ppqhApprove(p.ppqhId(), null);
        PpqhDashboardService.PpqhStats s = f.dashboard.ppqhStats();
        assertThat(s.ppqhTodaySales().hepyCents()).isEqualTo(order.ppqhTotalCents());
        assertThat(s.ppqhTodaySales().hepyCount()).isEqualTo(1);
        assertThat(s.ppqhMonthSales().hepyCents()).isEqualTo(order.ppqhTotalCents());
        assertThat(s.ppqhPendingPayments()).isZero();
        assertThat(s.ppqhOrdersByStatus().get(PpqhOrderStatus.PAGADO)).isEqualTo(1);
        assertThat(s.ppqhDaily()).hasSize(PpqhDashboardService.PPQH_DAYS).containsEntry(s.ppqhToday(), order.ppqhTotalCents());
        assertThat(s.ppqhTopProducts()).first().satisfies(t -> {
            assertThat(t.hepyName()).isEqualTo("Camiseta Pro Dry-Fit");
            assertThat(t.hepyUnits()).isEqualTo(2);
        });

        f.loginClient();
        assertThatThrownBy(() -> f.dashboard.ppqhStats()).isInstanceOf(PpqhAccessDeniedException.class);
    }
}
