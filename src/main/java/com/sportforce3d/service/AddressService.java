package com.sportforce3d.service;

import com.sportforce3d.model.Address;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.AddressRepository;
import com.sportforce3d.util.PeruRegions;
import com.sportforce3d.util.Validators;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Direcciones de envío del usuario conectado. Nadie puede ver ni editar direcciones ajenas. */
public class AddressService {

    private final AddressRepository addresses;
    private final Session session;

    public AddressService(AddressRepository addresses, Session session) {
        this.addresses = addresses;
        this.session = session;
    }

    public List<Address> list() {
        return addresses.findByUser(session.requireUser().id());
    }

    /** Crea (id = 0) o actualiza. Devuelve la dirección guardada. */
    public Address save(Address form) {
        User me = session.requireUser();
        Address clean = new Address(form.id(), trim(form.label()), trim(form.recipient()), trim(form.phone()),
                trim(form.department()), trim(form.province()), trim(form.district()), trim(form.line1()),
                Validators.blankToNull(form.reference()), form.isDefault());
        validate(clean);
        if (clean.id() != 0 && addresses.findById(me.id(), clean.id()).isEmpty()) {
            throw new ServiceException("Esa dirección ya no existe.");
        }
        long id = addresses.save(me.id(), clean);
        return addresses.findById(me.id(), id).orElseThrow();
    }

    public void delete(long addressId) {
        User me = session.requireUser();
        if (!addresses.delete(me.id(), addressId)) {
            throw new ServiceException("Esa dirección ya no existe.");
        }
    }

    public Address makeDefault(long addressId) {
        User me = session.requireUser();
        Address a = addresses.findById(me.id(), addressId)
                .orElseThrow(() -> new ServiceException("Esa dirección ya no existe."));
        return save(new Address(a.id(), a.label(), a.recipient(), a.phone(), a.department(), a.province(),
                a.district(), a.line1(), a.reference(), true));
    }

    static void validate(Address a) {
        Map<String, String> errors = new LinkedHashMap<>();
        length(errors, "label", a.label(), 1, 40, "Ponle un nombre, p. ej. Casa u Oficina.");
        length(errors, "recipient", a.recipient(), 3, 80, "Escribe el nombre de quien recibe.");
        if (a.phone() == null || a.phone().isEmpty()) {
            errors.put("phone", "Escribe un celular de contacto.");
        } else if (Validators.phone(a.phone()) != null) {
            errors.put("phone", Validators.phone(a.phone()));
        }
        if (a.department() == null || !PeruRegions.DEPARTMENTS.contains(a.department())) {
            errors.put("department", "Elige un departamento.");
        }
        length(errors, "province", a.province(), 2, 60, "Escribe la provincia.");
        length(errors, "district", a.district(), 2, 60, "Escribe el distrito.");
        length(errors, "line1", a.line1(), 5, 160, "Escribe la dirección (calle, número, interior).");
        if (a.reference() != null && a.reference().length() > 160) {
            errors.put("reference", "La referencia admite hasta 160 caracteres.");
        } else if (Validators.plainText(a.reference(), false) != null) {
            errors.put("reference", Validators.plainText(a.reference(), false));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void length(Map<String, String> errors, String field, String value, int min, int max, String message) {
        if (value == null || value.length() < min) {
            errors.put(field, message);
        } else if (value.length() > max) {
            errors.put(field, "Admite hasta " + max + " caracteres.");
        } else if (Validators.plainText(value, false) != null) {
            errors.put(field, Validators.plainText(value, false));
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}