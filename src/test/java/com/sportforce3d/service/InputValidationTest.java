package com.sportforce3d.service;

import com.sportforce3d.model.Address;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.model.ProductQuery;
import com.sportforce3d.model.ProductSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Entradas fuera de rango o con caracteres invisibles: cada service responde con un mensaje amable
 * (ValidationException / AuthenticationException), nunca con un error genérico ni guardando datos a medias.
 */
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
        String longWithAccents = "ñ".repeat(40) + "1"; // 41 caracteres pero 81 bytes en UTF-8
        assertThatThrownBy(() -> f.auth.register("Lucía Rojas", "lucia@correo.pe", null,
                longWithAccents.toCharArray(), longWithAccents.toCharArray()))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors()).containsKey("password"));

        assertThatThrownBy(() -> f.auth.login("cliente@sportforce3d.pe", "x".repeat(500).toCharArray()))
                .isInstanceOf(AuthenticationException.class).hasMessage("Correo o contraseña incorrectos.");
        assertThatThrownBy(() -> f.auth.register("Lucía Rojas", "lucia@correo.pe", null,
                "Clave\n2026x".toCharArray(), "Clave\n2026x".toCharArray()))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors()).containsKey("password"));
    }

    @Test
    void namesAndAddressesRejectInvisibleControlCharacters() {
        assertThatThrownBy(() -> f.auth.register(CONTROL, "nuevo@correo.pe", null,
                "Futbol2026".toCharArray(), "Futbol2026".toCharArray()))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors()).containsKey("fullName"));

        f.loginClient();
        Address bad = new Address(0, "Casa", "Ana Torres", "987654321", "Lima", "Lima", "Miraflores",
                "Av. Larco\u0007 123", "Frente al\u001b parque", false);
        assertThatThrownBy(() -> f.addresses.save(bad))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors())
                        .containsOnlyKeys("line1", "reference"));
    }

    @Test
    void orderNotesAndCancelReasonsHaveLimits() {
        f.loginClient();
        f.cart.add(f.variantId("SF-CAM-001-M"), 1);
        long address = f.addresses.list().get(0).id();
        assertThatThrownBy(() -> f.orders.placeOrder(address, PaymentMethod.YAPE, "x".repeat(OrderService.NOTES_MAX + 1)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.orders.placeOrder(address, PaymentMethod.YAPE, "Tocar el\u0000 timbre"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.orders.placeOrder(address, PaymentMethod.TARJETA, null))
                .isInstanceOf(ValidationException.class).hasMessageContaining("próximamente");
        assertThatThrownBy(() -> f.orders.placeOrder(address, null, null)).isInstanceOf(ValidationException.class);
        assertThat(f.cart.countUnits()).isEqualTo(1); // nada se perdió

        OrderDetail order = f.orders.placeOrder(address, PaymentMethod.YAPE, "Entregar por la tarde.\nGracias.");
        assertThatThrownBy(() -> f.orders.cancel(order.id(), "y".repeat(OrderService.NOTES_MAX + 1)))
                .isInstanceOf(ValidationException.class);
        assertThat(f.orders.cancel(order.id(), "Me equivoqué de talla").status().name()).isEqualTo("CANCELADO");
    }

    @Test
    void cartQuantitiesStayWithinBounds() {
        f.loginClient();
        long variant = f.variantId("SF-CAM-001-M");
        assertThatThrownBy(() -> f.cart.add(variant, 0)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> f.cart.add(variant, -3)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> f.cart.add(variant, 1000)).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> f.cart.add(999_999, 1)).isInstanceOf(ServiceException.class)
                .hasMessage("Este producto ya no está disponible.");
        assertThat(f.cart.countUnits()).isZero();
    }

    @Test
    void catalogSearchRejectsAbsurdInput() {
        assertThatThrownBy(() -> f.catalog.search(new ProductQuery("a".repeat(CatalogService.SEARCH_MAX + 1), null, null,
                Set.of(), null, null, false, false, ProductSort.RELEVANCE, 1, 12))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.catalog.search(new ProductQuery(null, null, null, Set.of(), -100L, null, false, false,
                ProductSort.RELEVANCE, 1, 12))).isInstanceOf(ValidationException.class);
        // Comillas y comodines de SQL se buscan como texto, no rompen la consulta.
        assertThat(f.catalog.search(new ProductQuery("' OR 1=1 --%_", null, null, Set.of(), null, null, false, false,
                ProductSort.RELEVANCE, 1, 12)).items()).isEmpty();
        assertThat(f.catalog.search(null).items()).isNotEmpty();
    }

    @Test
    void adminNotesAndRejectionReasonsAreChecked() throws Exception {
        f.loginClient();
        f.cart.add(f.variantId("SF-CAM-001-M"), 1);
        OrderDetail order = f.orders.placeOrder(f.addresses.list().get(0).id(), PaymentMethod.YAPE, null);
        Path proof = tempDir.resolve("p.png");
        java.nio.file.Files.write(proof, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0});
        assertThatThrownBy(() -> f.payments.register(order.id(), "12'; DROP", proof)).isInstanceOf(ServiceException.class);
        long paymentId = f.payments.register(order.id(), "55667788", proof).currentPayment().id();

        f.loginAdmin();
        assertThatThrownBy(() -> f.payments.reject(paymentId, "mal")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.payments.reject(paymentId, "Monto\u0000 distinto")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> f.adminOrders.changeStatus(order.id(), com.sportforce3d.model.OrderStatus.CANCELADO, "x".repeat(301)))
                .isInstanceOf(ValidationException.class);
        assertThat(f.payments.payment(paymentId).status().name()).isEqualTo("EN_VERIFICACION");
    }
}
