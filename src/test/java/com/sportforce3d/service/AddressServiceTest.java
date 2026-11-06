package com.sportforce3d.service;

import com.sportforce3d.model.Address;
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

    private static Address address(long id, String label, boolean isDefault) {
        return new Address(id, label, "Lucía Quispe", "987654321", "Puno", "Puno", "Puno",
                "Av. El Sol 456", null, isDefault);
    }

    @Test
    void keepsExactlyOneDefault() {
        f.loginClient();
        Address office = f.addresses.save(address(0, "Oficina", true));

        List<Address> list = f.addresses.list();
        assertThat(list).hasSize(2);
        assertThat(list.stream().filter(Address::isDefault)).singleElement().extracting(Address::id).isEqualTo(office.id());

        long home = list.stream().filter(a -> a.label().equals("Casa")).findFirst().orElseThrow().id();
        f.addresses.makeDefault(home);
        assertThat(f.addresses.list().get(0).id()).isEqualTo(home);

        f.addresses.delete(home);
        assertThat(f.addresses.list()).singleElement().satisfies(a -> assertThat(a.isDefault()).isTrue());
    }

    @Test
    void validatesFields() {
        f.loginClient();
        assertThatThrownBy(() -> f.addresses.save(new Address(0, "", "Lu", "abc", "Narnia", "", "", "x", null, false)))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors())
                        .containsKeys("label", "recipient", "phone", "department", "province", "district", "line1"));
    }

    @Test
    void cannotEditOrDeleteSomeoneElsesAddress() {
        f.loginClient();
        long clientAddress = f.addresses.list().get(0).id();

        f.loginNewClient("otra@correo.pe");
        assertThat(f.addresses.list()).isEmpty();
        assertThatThrownBy(() -> f.addresses.save(address(clientAddress, "Robada", false))).hasMessageContaining("ya no existe");
        assertThatThrownBy(() -> f.addresses.delete(clientAddress)).hasMessageContaining("ya no existe");

        f.loginClient();
        assertThat(f.addresses.list().get(0).label()).isEqualTo("Casa");
    }

    @Test
    void firstAddressBecomesDefault() {
        f.loginNewClient("nueva@correo.pe");
        assertThat(f.addresses.save(address(0, "Casa", false)).isDefault()).isTrue();
    }
}
