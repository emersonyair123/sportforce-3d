package com.sportforce3d.service;

import com.sportforce3d.model.PpqhPaymentInstructions;
import com.sportforce3d.model.PpqhPaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreSettingsServiceTest {

    @TempDir
    Path tempDir;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = new Fixture(tempDir);
    }

    private static PpqhPaymentInstructions form(String yape, String plin, String account, String cci) {
        return new PpqhPaymentInstructions(yape, "  María   Quispe Huamán ", plin, "Club Deportivo Los Andes S.A.C.",
                "BCP", "SPORTFORCE 3D S.A.C.", account, cci, false);
    }

    @Test
    void migrationSplitsTheOldHolderAndStartsUnreviewed() {
        f.loginAdmin();
        PpqhPaymentInstructions p = f.settings.ppqhPaymentSettings();
        assertThat(p.ppqhYapeHolder()).isEqualTo("SPORTFORCE 3D S.A.C. — datos de prueba");
        assertThat(p.ppqhPlinHolder()).isEqualTo(p.ppqhYapeHolder());
        assertThat(p.ppqhBankHolder()).isEqualTo(p.ppqhYapeHolder());
        assertThat(p.ppqhReviewed()).isFalse();
    }

    @Test
    void adminSavesNormalizedValuesThatCustomersSeeWhenPaying() {
        f.loginAdmin();
        PpqhPaymentInstructions saved = f.settings.ppqhSavePaymentSettings(
                form("+51 912-345-678", "923456789", "191-12345678-0-12", "00219100123456780123"));

        assertThat(saved.ppqhYapePhone()).isEqualTo("912 345 678");
        assertThat(saved.ppqhPlinPhone()).isEqualTo("923 456 789");
        assertThat(saved.ppqhYapeHolder()).isEqualTo("María Quispe Huamán");
        assertThat(saved.ppqhBankAccount()).isEqualTo("191-12345678-0-12");
        assertThat(saved.ppqhBankCci()).isEqualTo("002-191-001234567801-23");
        assertThat(saved.ppqhReviewed()).isTrue();

        f.loginClient();
        PpqhPaymentInstructions seen = f.payments.ppqhInstructions();
        assertThat(seen.ppqhForMethod(PpqhPaymentMethod.YAPE)).isEqualTo("Yapea al 912 345 678 (María Quispe Huamán).");
        assertThat(seen.ppqhForMethod(PpqhPaymentMethod.PLIN)).contains("923 456 789", "Club Deportivo Los Andes S.A.C.");
        assertThat(seen.ppqhForMethod(PpqhPaymentMethod.TRANSFERENCIA)).contains("BCP", "191-12345678-0-12",
                "002-191-001234567801-23", "SPORTFORCE 3D S.A.C.");
    }

    @Test
    void savingTheSeedValuesUnchangedIsAccepted() {
        f.loginAdmin();
        PpqhPaymentInstructions seed = f.settings.ppqhPaymentSettings();
        PpqhPaymentInstructions saved = f.settings.ppqhSavePaymentSettings(seed);
        assertThat(saved.ppqhYapeHolder()).isEqualTo(seed.ppqhYapeHolder());
        assertThat(saved.ppqhBankCci()).isEqualTo("002-191-000000000000-00");
        assertThat(saved.ppqhReviewed()).isTrue();
    }

    @Test
    void rejectsInvalidDataFieldByFieldWithoutSavingAnything() {
        f.loginAdmin();
        PpqhPaymentInstructions before = f.settings.ppqhPaymentSettings();
        PpqhPaymentInstructions bad = new PpqhPaymentInstructions("812345678", "", "98765", "<script>alert(1)</script>",
                "B", "Ok Titular", "12-ab", "1234", false);

        assertThatThrownBy(() -> f.settings.ppqhSavePaymentSettings(bad))
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors())
                        .containsOnlyKeys("yapePhone", "yapeHolder", "plinPhone", "plinHolder", "bankName",
                                "bankAccount", "bankCci")
                        .containsEntry("bankCci", "El CCI tiene exactamente 20 dígitos."));
        assertThat(f.settings.ppqhPaymentSettings()).isEqualTo(before);

        assertThatThrownBy(() -> f.settings.ppqhSavePaymentSettings(form("912345678", "912345678", "1234567890123456789012",
                "00219100123456780123"))).isInstanceOfSatisfying(PpqhValidationException.class,
                e -> assertThat(e.ppqhFieldErrors()).containsOnlyKeys("bankAccount"));
        assertThatThrownBy(() -> f.settings.ppqhSavePaymentSettings(null)).isInstanceOf(PpqhValidationException.class);
    }

    @Test
    void clientCannotReadOrChangePaymentSettings() {
        f.loginClient();
        assertThatThrownBy(() -> f.settings.ppqhPaymentSettings()).isInstanceOf(PpqhAccessDeniedException.class);
        assertThatThrownBy(() -> f.settings.ppqhSavePaymentSettings(form("912345678", "923456789", "191-12345678-0-12",
                "00219100123456780123"))).isInstanceOf(PpqhAccessDeniedException.class);

        f.loginAdmin();
        assertThat(f.settings.ppqhPaymentSettings().ppqhYapePhone()).isEqualTo("987 654 321");

        f.auth.ppqhLogout();
        assertThatThrownBy(() -> f.settings.ppqhPaymentSettings()).isInstanceOf(PpqhAuthenticationException.class);
    }
}
