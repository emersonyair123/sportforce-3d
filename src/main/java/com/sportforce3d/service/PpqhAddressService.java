package com.sportforce3d.service;

import com.sportforce3d.model.PpqhAddress;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyAddressRepository;
import com.sportforce3d.util.PpqhPeruRegions;
import com.sportforce3d.util.PpqhValidators;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PpqhAddressService {

    private final HepyAddressRepository ppqhAddresses;
    private final PpqhSession ppqhSession;

    public PpqhAddressService(HepyAddressRepository ppqhAddresses, PpqhSession ppqhSession) {
        this.ppqhAddresses = ppqhAddresses;
        this.ppqhSession = ppqhSession;
    }

    public List<PpqhAddress> ppqhList() {
        return ppqhAddresses.hepyFindByUser(ppqhSession.ppqhRequireUser().ppqhId());
    }

    public PpqhAddress ppqhSave(PpqhAddress ppqhForm) {
        PpqhUser ppqhMe = ppqhSession.ppqhRequireUser();
        PpqhAddress ppqhClean = new PpqhAddress(ppqhForm.ppqhId(), ppqhTrim(ppqhForm.ppqhLabel()), ppqhTrim(ppqhForm.ppqhRecipient()), ppqhTrim(ppqhForm.ppqhPhone()),
                ppqhTrim(ppqhForm.ppqhDepartment()), ppqhTrim(ppqhForm.ppqhProvince()), ppqhTrim(ppqhForm.ppqhDistrict()), ppqhTrim(ppqhForm.ppqhLine1()),
                PpqhValidators.ppqhBlankToNull(ppqhForm.ppqhReference()), ppqhForm.ppqhIsDefault());
        ppqhValidate(ppqhClean);
        if (ppqhClean.ppqhId() != 0 && ppqhAddresses.hepyFindById(ppqhMe.ppqhId(), ppqhClean.ppqhId()).isEmpty()) {
            throw new PpqhServiceException("Esa dirección ya no existe.");
        }
        long ppqhId = ppqhAddresses.hepySave(ppqhMe.ppqhId(), ppqhClean);
        return ppqhAddresses.hepyFindById(ppqhMe.ppqhId(), ppqhId).orElseThrow();
    }

    public void ppqhDelete(long ppqhAddressId) {
        PpqhUser ppqhMe = ppqhSession.ppqhRequireUser();
        if (!ppqhAddresses.hepyDelete(ppqhMe.ppqhId(), ppqhAddressId)) {
            throw new PpqhServiceException("Esa dirección ya no existe.");
        }
    }

    public PpqhAddress ppqhMakeDefault(long ppqhAddressId) {
        PpqhUser ppqhMe = ppqhSession.ppqhRequireUser();
        PpqhAddress ppqhA = ppqhAddresses.hepyFindById(ppqhMe.ppqhId(), ppqhAddressId)
                .orElseThrow(() -> new PpqhServiceException("Esa dirección ya no existe."));
        return ppqhSave(new PpqhAddress(ppqhA.ppqhId(), ppqhA.ppqhLabel(), ppqhA.ppqhRecipient(), ppqhA.ppqhPhone(), ppqhA.ppqhDepartment(), ppqhA.ppqhProvince(),
                ppqhA.ppqhDistrict(), ppqhA.ppqhLine1(), ppqhA.ppqhReference(), true));
    }

    static void ppqhValidate(PpqhAddress ppqhA) {
        Map<String, String> ppqhErrors = new LinkedHashMap<>();
        ppqhLength(ppqhErrors, "label", ppqhA.ppqhLabel(), 1, 40, "Ponle un nombre, p. ej. Casa u Oficina.");
        ppqhLength(ppqhErrors, "recipient", ppqhA.ppqhRecipient(), 3, 80, "Escribe el nombre de quien recibe.");
        if (ppqhA.ppqhPhone() == null || ppqhA.ppqhPhone().isEmpty()) {
            ppqhErrors.put("phone", "Escribe un celular de contacto.");
        } else if (PpqhValidators.ppqhPhone(ppqhA.ppqhPhone()) != null) {
            ppqhErrors.put("phone", PpqhValidators.ppqhPhone(ppqhA.ppqhPhone()));
        }
        if (ppqhA.ppqhDepartment() == null || !PpqhPeruRegions.PPQH_DEPARTMENTS.contains(ppqhA.ppqhDepartment())) {
            ppqhErrors.put("department", "Elige un departamento.");
        }
        ppqhLength(ppqhErrors, "province", ppqhA.ppqhProvince(), 2, 60, "Escribe la provincia.");
        ppqhLength(ppqhErrors, "district", ppqhA.ppqhDistrict(), 2, 60, "Escribe el distrito.");
        ppqhLength(ppqhErrors, "line1", ppqhA.ppqhLine1(), 5, 160, "Escribe la dirección (calle, número, interior).");
        if (ppqhA.ppqhReference() != null && ppqhA.ppqhReference().length() > 160) {
            ppqhErrors.put("reference", "La referencia admite hasta 160 caracteres.");
        } else if (PpqhValidators.ppqhPlainText(ppqhA.ppqhReference(), false) != null) {
            ppqhErrors.put("reference", PpqhValidators.ppqhPlainText(ppqhA.ppqhReference(), false));
        }
        if (!ppqhErrors.isEmpty()) {
            throw new PpqhValidationException(ppqhErrors);
        }
    }

    private static void ppqhLength(Map<String, String> ppqhErrors, String ppqhField, String ppqhValue, int ppqhMin, int ppqhMax, String ppqhMessage) {
        if (ppqhValue == null || ppqhValue.length() < ppqhMin) {
            ppqhErrors.put(ppqhField, ppqhMessage);
        } else if (ppqhValue.length() > ppqhMax) {
            ppqhErrors.put(ppqhField, "Admite hasta " + ppqhMax + " caracteres.");
        } else if (PpqhValidators.ppqhPlainText(ppqhValue, false) != null) {
            ppqhErrors.put(ppqhField, PpqhValidators.ppqhPlainText(ppqhValue, false));
        }
    }

    private static String ppqhTrim(String ppqhValue) {
        return ppqhValue == null ? null : ppqhValue.trim();
    }
}
