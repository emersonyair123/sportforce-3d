package com.sportforce3d.service;

import com.sportforce3d.model.PpqhAddress;
import com.sportforce3d.model.PpqhOrderDetail;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.model.HepyProductQuery;
import com.sportforce3d.model.HepyProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputValidationTest {

    private static final String CONTROL = "Equipo\u0000Halcones";

    @TempDir
    Path tempDir;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = new Fixture(tempDir);
    }

    @Test
    void passwordsLongerThanBcryptLimitAreRejectedPolitely() {
        String longWithAccents = "ñ".repeat(40) + "1";
        assertThatThrownBy(() -> f.auth.ppqhRegister("Lucía Rojas", "lucia@correo.pe", null,
                longWithAccents.toCharArray(), longWithAccents.toCharArray()))
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors()).containsKey("password"));

        assertThatThrownBy(() -> f.auth.ppqhLogin("cliente@sportforce3d.pe", "x".repeat(500).toCharArray()))
                .isInstanceOf(PpqhAuthenticationException.class).hasMessage("Correo o contraseña incorrectos.");
        assertThatThrownBy(() -> f.auth.ppqhRegister("Lucía Rojas", "lucia@correo.pe", null,
                "Clave\n2026x".toCharArray(), "Clave\n2026x".toCharArray()))
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors()).containsKey("password"));
    }

    @Test
    void namesAndAddressesRejectInvisibleControlCharacters() {
        assertThatThrownBy(() -> f.auth.ppqhRegister(CONTROL, "nuevo@correo.pe", null,
                "Futbol2026".toCharArray(), "Futbol2026".toCharArray()))
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors()).containsKey("fullName"));

        f.loginClient();
        PpqhAddress bad = new PpqhAddress(0, "Casa", "Ana Torres", "987654321", "Lima", "Lima", "Miraflores",
                "Av. Larco\u0007 123", "Frente al\u001b parque", false);
        assertThatThrownBy(() -> f.addresses.ppqhSave(bad))
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors())
                        .containsOnlyKeys("line1", "reference"));
    }

    @Test
    void orderNotesAndCancelReasonsHaveLimits() {
        f.loginClient();
        f.cart.hepyAdd(f.variantId("SF-CAM-001-M"), 1);
        long address = f.addresses.ppqhList().get(0).ppqhId();
        assertThatThrownBy(() -> f.orders.ppqhPlaceOrder(address, PpqhPaymentMethod.YAPE, "x".repeat(PpqhOrderService.PPQH_NOTES_MAX + 1)))
                .isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.orders.ppqhPlaceOrder(address, PpqhPaymentMethod.YAPE, "Tocar el\u0000 timbre"))
                .isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.orders.ppqhPlaceOrder(address, PpqhPaymentMethod.TARJETA, null))
                .isInstanceOf(PpqhValidationException.class).hasMessageContaining("próximamente");
        assertThatThrownBy(() -> f.orders.ppqhPlaceOrder(address, null, null)).isInstanceOf(PpqhValidationException.class);
        assertThat(f.cart.hepyCountUnits()).isEqualTo(1);

        PpqhOrderDetail order = f.orders.ppqhPlaceOrder(address, PpqhPaymentMethod.YAPE, "Entregar por la tarde.\nGracias.");
        assertThatThrownBy(() -> f.orders.ppqhCancel(order.ppqhId(), "y".repeat(PpqhOrderService.PPQH_NOTES_MAX + 1)))
                .isInstanceOf(PpqhValidationException.class);
        assertThat(f.orders.ppqhCancel(order.ppqhId(), "Me equivoqué de talla").ppqhStatus().name()).isEqualTo("CANCELADO");
    }

    @Test
    void cartQuantitiesStayWithinBounds() {
        f.loginClient();
        long variant = f.variantId("SF-CAM-001-M");
        assertThatThrownBy(() -> f.cart.hepyAdd(variant, 0)).isInstanceOf(PpqhServiceException.class);
        assertThatThrownBy(() -> f.cart.hepyAdd(variant, -3)).isInstanceOf(PpqhServiceException.class);
        assertThatThrownBy(() -> f.cart.hepyAdd(variant, 1000)).isInstanceOf(PpqhServiceException.class);
        assertThatThrownBy(() -> f.cart.hepyAdd(999_999, 1)).isInstanceOf(PpqhServiceException.class)
                .hasMessage("Este producto ya no está disponible.");
        assertThat(f.cart.hepyCountUnits()).isZero();
    }

    @Test
    void catalogSearchRejectsAbsurdInput() {
        assertThatThrownBy(() -> f.catalog.hepySearch(new HepyProductQuery("a".repeat(HepyCatalogService.HEPY_SEARCH_MAX + 1), null, null,
                Set.of(), null, null, false, false, HepyProductSort.RELEVANCE, 1, 12))).isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.catalog.hepySearch(new HepyProductQuery(null, null, null, Set.of(), -100L, null, false, false,
                HepyProductSort.RELEVANCE, 1, 12))).isInstanceOf(PpqhValidationException.class);
        assertThat(f.catalog.hepySearch(new HepyProductQuery("' OR 1=1 --%_", null, null, Set.of(), null, null, false, false,
                HepyProductSort.RELEVANCE, 1, 12)).hepyItems()).isEmpty();
        assertThat(f.catalog.hepySearch(null).hepyItems()).isNotEmpty();
    }

    @Test
    void adminNotesAndRejectionReasonsAreChecked() throws Exception {
        f.loginClient();
        f.cart.hepyAdd(f.variantId("SF-CAM-001-M"), 1);
        PpqhOrderDetail order = f.orders.ppqhPlaceOrder(f.addresses.ppqhList().get(0).ppqhId(), PpqhPaymentMethod.YAPE, null);
        Path proof = tempDir.resolve("p.png");
        java.nio.file.Files.write(proof, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0});
        assertThatThrownBy(() -> f.payments.ppqhRegister(order.ppqhId(), "12'; DROP", proof)).isInstanceOf(PpqhServiceException.class);
        long paymentId = f.payments.ppqhRegister(order.ppqhId(), "55667788", proof).ppqhCurrentPayment().ppqhId();

        f.loginAdmin();
        assertThatThrownBy(() -> f.payments.ppqhReject(paymentId, "mal")).isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.payments.ppqhReject(paymentId, "Monto\u0000 distinto")).isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> f.adminOrders.ppqhChangeStatus(order.ppqhId(), com.sportforce3d.model.PpqhOrderStatus.CANCELADO, "x".repeat(301)))
                .isInstanceOf(PpqhValidationException.class);
        assertThat(f.payments.ppqhPayment(paymentId).ppqhStatus().name()).isEqualTo("EN_VERIFICACION");
    }
}
