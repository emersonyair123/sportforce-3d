package com.sportforce3d.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyAndValidatorsTest {

    @Test
    void formatsSolesLikeTheBrandGuide() {
        assertThat(Money.format(12990)).isEqualTo("S/ 129.90");
        assertThat(Money.format(0)).isEqualTo("S/ 0.00");
        assertThat(Money.format(5)).isEqualTo("S/ 0.05");
        assertThat(Money.format(123456789)).isEqualTo("S/ 1,234,567.89");
        assertThat(Money.format(-1500)).isEqualTo("-S/ 15.00");
    }

    @Test
    void parsesAmountsTypedByPeople() {
        assertThat(Money.parse("129.90")).isEqualTo(12990);
        assertThat(Money.parse("129,90")).isEqualTo(12990);
        assertThat(Money.parse("S/ 129.9")).isEqualTo(12990);
        assertThat(Money.parse("129")).isEqualTo(12900);
        assertThat(Money.parse("1,299.00")).isEqualTo(129900);
        assertThat(Money.parse("0.05")).isEqualTo(5);
        assertThat(Money.toInput(12990)).isEqualTo("129.90");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Money.parse("doce"))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Money.parse("-5"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchKeyRemovesAccents() {
        assertThat(Texts.searchKey("Uniforme FÚTBOL Vóley Ñandú")).isEqualTo("uniforme futbol voley nandu");
        assertThat(Texts.skuPart("Camiseta Básica 2")).isEqualTo("CAMISETABASICA2");
    }

    @Test
    void formatsMaterialSurcharge() {
        assertThat(Money.surcharge(0)).isEqualTo("Incluido");
        assertThat(Money.surcharge(1500)).isEqualTo("+ S/ 15.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"lucia@correo.pe", "  Admin@SportForce3D.pe ", "a.b+c@dominio.com.pe"})
    void acceptsValidEmails(String email) {
        assertThat(Validators.email(email)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sin-arroba", "a@b", "a@b.c", "a b@c.pe"})
    void rejectsInvalidEmails(String email) {
        assertThat(Validators.email(email)).isNotNull();
    }

    @Test
    void passwordPolicyNeedsLengthLettersAndDigits() {
        assertThat(Validators.newPassword("Futbol2026".toCharArray())).isNull();
        assertThat(Validators.newPassword("corta1".toCharArray())).contains("8");
        assertThat(Validators.newPassword("solamenteletras".toCharArray())).contains("letras y números");
        assertThat(Validators.newPassword("12345678".toCharArray())).contains("letras y números");
    }

    @Test
    void phoneIsOptional() {
        assertThat(Validators.phone(null)).isNull();
        assertThat(Validators.phone("  ")).isNull();
        assertThat(Validators.phone("+51 987 654 321")).isNull();
        assertThat(Validators.phone("98-76")).isNotNull();
    }
}
