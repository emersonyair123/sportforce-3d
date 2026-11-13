package com.sportforce3d.service;

import com.sportforce3d.model.PpqhAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddressServiceTest {

    @TempDir
    Path tempDir;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = new Fixture(tempDir);
    }

    private static PpqhAddress address(long id, String label, boolean isDefault) {
        return new PpqhAddress(id, label, "Lucía Quispe", "987654321", "Puno", "Puno", "Puno",
                "Av. El Sol 456", null, isDefault);
    }

    @Test
    void keepsExactlyOneDefault() {
        f.loginClient();
        PpqhAddress office = f.addresses.ppqhSave(address(0, "Oficina", true));

        List<PpqhAddress> list = f.addresses.ppqhList();
        assertThat(list).hasSize(2);
        assertThat(list.stream().filter(PpqhAddress::ppqhIsDefault)).singleElement().extracting(PpqhAddress::ppqhId).isEqualTo(office.ppqhId());

        long home = list.stream().filter(a -> a.ppqhLabel().equals("Casa")).findFirst().orElseThrow().ppqhId();
        f.addresses.ppqhMakeDefault(home);
        assertThat(f.addresses.ppqhList().get(0).ppqhId()).isEqualTo(home);

        f.addresses.ppqhDelete(home);
        assertThat(f.addresses.ppqhList()).singleElement().satisfies(a -> assertThat(a.ppqhIsDefault()).isTrue());
    }

    @Test
    void validatesFields() {
        f.loginClient();
        assertThatThrownBy(() -> f.addresses.ppqhSave(new PpqhAddress(0, "", "Lu", "abc", "Narnia", "", "", "x", null, false)))
                .isInstanceOfSatisfying(PpqhValidationException.class, e -> assertThat(e.ppqhFieldErrors())
                        .containsKeys("label", "recipient", "phone", "department", "province", "district", "line1"));
    }

    @Test
    void cannotEditOrDeleteSomeoneElsesAddress() {
        f.loginClient();
        long clientAddress = f.addresses.ppqhList().get(0).ppqhId();

        f.loginNewClient("otra@correo.pe");
        assertThat(f.addresses.ppqhList()).isEmpty();
        assertThatThrownBy(() -> f.addresses.ppqhSave(address(clientAddress, "Robada", false))).hasMessageContaining("ya no existe");
        assertThatThrownBy(() -> f.addresses.ppqhDelete(clientAddress)).hasMessageContaining("ya no existe");

        f.loginClient();
        assertThat(f.addresses.ppqhList().get(0).ppqhLabel()).isEqualTo("Casa");
    }

    @Test
    void firstAddressBecomesDefault() {
        f.loginNewClient("nueva@correo.pe");
        assertThat(f.addresses.ppqhSave(address(0, "Casa", false)).ppqhIsDefault()).isTrue();
    }
}
