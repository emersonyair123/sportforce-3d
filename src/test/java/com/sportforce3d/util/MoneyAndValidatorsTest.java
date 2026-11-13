package com.sportforce3d.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyAndValidatorsTest {

    @Test
    void formatsSolesLikeTheBrandGuide() {
        assertThat(HepyMoney.hepyFormat(12990)).isEqualTo("S/ 129.90");
        assertThat(HepyMoney.hepyFormat(0)).isEqualTo("S/ 0.00");
        assertThat(HepyMoney.hepyFormat(5)).isEqualTo("S/ 0.05");
        assertThat(HepyMoney.hepyFormat(123456789)).isEqualTo("S/ 1,234,567.89");
        assertThat(HepyMoney.hepyFormat(-1500)).isEqualTo("-S/ 15.00");
    }

    @Test
    void parsesAmountsTypedByPeople() {
        assertThat(HepyMoney.hepyParse("129.90")).isEqualTo(12990);
        assertThat(HepyMoney.hepyParse("129,90")).isEqualTo(12990);
        assertThat(HepyMoney.hepyParse("S/ 129.9")).isEqualTo(12990);
        assertThat(HepyMoney.hepyParse("129")).isEqualTo(12900);
        assertThat(HepyMoney.hepyParse("1,299.00")).isEqualTo(129900);
        assertThat(HepyMoney.hepyParse("0.05")).isEqualTo(5);
        assertThat(HepyMoney.hepyToInput(12990)).isEqualTo("129.90");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> HepyMoney.hepyParse("doce"))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> HepyMoney.hepyParse("-5"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchKeyRemovesAccents() {
        assertThat(HepyTexts.hepySearchKey("Uniforme FÚTBOL Vóley Ñandú")).isEqualTo("uniforme futbol voley nandu");
        assertThat(HepyTexts.hepySkuPart("Camiseta Básica 2")).isEqualTo("CAMISETABASICA2");
    }

    @Test
    void formatsMaterialSurcharge() {
        assertThat(HepyMoney.hepySurcharge(0)).isEqualTo("Incluido");
        assertThat(HepyMoney.hepySurcharge(1500)).isEqualTo("+ S/ 15.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"lucia@correo.pe", "  Admin@SportForce3D.pe ", "a.b+c@dominio.com.pe"})
    void acceptsValidEmails(String email) {
        assertThat(PpqhValidators.ppqhEmail(email)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sin-arroba", "a@b", "a@b.c", "a b@c.pe"})
    void rejectsInvalidEmails(String email) {
        assertThat(PpqhValidators.ppqhEmail(email)).isNotNull();
    }

    @Test
    void passwordPolicyNeedsLengthLettersAndDigits() {
        assertThat(PpqhValidators.ppqhNewPassword("Futbol2026".toCharArray())).isNull();
        assertThat(PpqhValidators.ppqhNewPassword("corta1".toCharArray())).contains("8");
        assertThat(PpqhValidators.ppqhNewPassword("solamenteletras".toCharArray())).contains("letras y números");
        assertThat(PpqhValidators.ppqhNewPassword("12345678".toCharArray())).contains("letras y números");
    }

    @Test
    void phoneIsOptional() {
        assertThat(PpqhValidators.ppqhPhone(null)).isNull();
        assertThat(PpqhValidators.ppqhPhone("  ")).isNull();
        assertThat(PpqhValidators.ppqhPhone("+51 987 654 321")).isNull();
        assertThat(PpqhValidators.ppqhPhone("98-76")).isNotNull();
    }
}
