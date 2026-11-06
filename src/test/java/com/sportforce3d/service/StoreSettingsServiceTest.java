package com.sportforce3d.service;

import com.sportforce3d.model.PaymentInstructions;
import com.sportforce3d.model.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Administración → Configuración: datos de pago reales, con validación y solo para ADMIN. */
class StoreSettingsServiceTest {

    @TempDir
    Path tempDir;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = new Fixture(tempDir);
    }

    private static PaymentInstructions form(String yape, String plin, String account, String cci) {
        return new PaymentInstructions(yape, "  María   Quispe Huamán ", plin, "Club Deportivo Los Andes S.A.C.",
                "BCP", "SPORTFORCE 3D S.A.C.", account, cci, false);
    }

    @Test
    void migrationSplitsTheOldHolderAndStartsUnreviewed() {
        f.loginAdmin();
        PaymentInstructions p = f.settings.paymentSettings();
        assertThat(p.yapeHolder()).isEqualTo("SPORTFORCE 3D S.A.C. — datos de prueba");
        assertThat(p.plinHolder()).isEqualTo(p.yapeHolder());
        assertThat(p.bankHolder()).isEqualTo(p.yapeHolder());
        assertThat(p.reviewed()).isFalse();
    }

    @Test
    void adminSavesNormalizedValuesThatCustomersSeeWhenPaying() {
        f.loginAdmin();
        PaymentInstructions saved = f.settings.savePaymentSettings(
                form("+51 912-345-678", "923456789", "191-12345678-0-12", "00219100123456780123"));

        assertThat(saved.yapePhone()).isEqualTo("912 345 678");
        assertThat(saved.plinPhone()).isEqualTo("923 456 789");
        assertThat(saved.yapeHolder()).isEqualTo("María Quispe Huamán");
        assertThat(saved.bankAccount()).isEqualTo("191-12345678-0-12");
        assertThat(saved.bankCci()).isEqualTo("002-191-001234567801-23");
        assertThat(saved.reviewed()).isTrue();

        // El cliente ve los datos nuevos al registrar su pago.
        f.loginClient();
        PaymentInstructions seen = f.payments.instructions();
        assertThat(seen.forMethod(PaymentMethod.YAPE)).isEqualTo("Yapea al 912 345 678 (María Quispe Huamán).");
        assertThat(seen.forMethod(PaymentMethod.PLIN)).contains("923 456 789", "Club Deportivo Los Andes S.A.C.");
        assertThat(seen.forMethod(PaymentMethod.TRANSFERENCIA)).contains("BCP", "191-12345678-0-12",
                "002-191-001234567801-23", "SPORTFORCE 3D S.A.C.");
    }

    @Test
    void savingTheSeedValuesUnchangedIsAccepted() {
        f.loginAdmin();
        PaymentInstructions seed = f.settings.paymentSettings();
        PaymentInstructions saved = f.settings.savePaymentSettings(seed);
        assertThat(saved.yapeHolder()).isEqualTo(seed.yapeHolder());
        assertThat(saved.bankCci()).isEqualTo("002-191-000000000000-00");
        assertThat(saved.reviewed()).isTrue();
    }

    @Test
    void rejectsInvalidDataFieldByFieldWithoutSavingAnything() {
        f.loginAdmin();
        PaymentInstructions before = f.settings.paymentSettings();
        PaymentInstructions bad = new PaymentInstructions("812345678", "", "98765", "<script>alert(1)</script>",
                "B", "Ok Titular", "12-ab", "1234", false);

        assertThatThrownBy(() -> f.settings.savePaymentSettings(bad))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors())
                        .containsOnlyKeys("yapePhone", "yapeHolder", "plinPhone", "plinHolder", "bankName",
                                "bankAccount", "bankCci")
                        .containsEntry("bankCci", "El CCI tiene exactamente 20 dígitos."));
        assertThat(f.settings.paymentSettings()).isEqualTo(before);

        assertThatThrownBy(() -> f.settings.savePaymentSettings(form("912345678", "912345678", "1234567890123456789012",
                "00219100123456780123"))).isInstanceOfSatisfying(ValidationException.class,
                e -> assertThat(e.fieldErrors()).containsOnlyKeys("bankAccount"));
        assertThatThrownBy(() -> f.settings.savePaymentSettings(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void clientCannotReadOrChangePaymentSettings() {
        f.loginClient();
        assertThatThrownBy(() -> f.settings.paymentSettings()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> f.settings.savePaymentSettings(form("912345678", "923456789", "191-12345678-0-12",
                "00219100123456780123"))).isInstanceOf(AccessDeniedException.class);

        f.loginAdmin();
        assertThat(f.settings.paymentSettings().yapePhone()).isEqualTo("987 654 321");

        f.auth.logout();
        assertThatThrownBy(() -> f.settings.paymentSettings()).isInstanceOf(AuthenticationException.class);
    }
}
