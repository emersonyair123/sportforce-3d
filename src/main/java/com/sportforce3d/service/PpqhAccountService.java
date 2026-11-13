package com.sportforce3d.service;

import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyUserRepository;
import com.sportforce3d.util.PpqhValidators;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.sportforce3d.service.PpqhAuthService.ppqhPutIfError;
import static com.sportforce3d.service.PpqhAuthService.ppqhWipe;

public class PpqhAccountService {

    private final HepyUserRepository ppqhUsers;
    private final PpqhPasswordHasher ppqhHasher;
    private final PpqhSession ppqhSession;

    public PpqhAccountService(HepyUserRepository ppqhUsers, PpqhPasswordHasher ppqhHasher, PpqhSession ppqhSession) {
        this.ppqhUsers = ppqhUsers;
        this.ppqhHasher = ppqhHasher;
        this.ppqhSession = ppqhSession;
    }

    public PpqhUser ppqhUpdateProfile(String ppqhFullName, String ppqhPhone) {
        PpqhUser ppqhMe = ppqhSession.ppqhRequireUser();
        Map<String, String> ppqhErrors = new LinkedHashMap<>();
        ppqhPutIfError(ppqhErrors, "fullName", PpqhValidators.ppqhFullName(ppqhFullName));
        ppqhPutIfError(ppqhErrors, "phone", PpqhValidators.ppqhPhone(ppqhPhone));
        if (!ppqhErrors.isEmpty()) {
            throw new PpqhValidationException(ppqhErrors);
        }
        ppqhUsers.hepyUpdateProfile(ppqhMe.ppqhId(), ppqhFullName.trim(), PpqhValidators.ppqhBlankToNull(ppqhPhone));
        PpqhUser ppqhFresh = ppqhUsers.hepyFindById(ppqhMe.ppqhId()).orElseThrow();
        ppqhSession.ppqhRefresh(ppqhFresh);
        return ppqhFresh;
    }

    public void ppqhChangePassword(char[] ppqhCurrent, char[] ppqhNext, char[] ppqhConfirmation) {
        try {
            PpqhUser ppqhMe = ppqhSession.ppqhRequireUser();
            Map<String, String> ppqhErrors = new LinkedHashMap<>();
            if (ppqhCurrent == null || ppqhCurrent.length == 0) {
                ppqhErrors.put("current", "Ingresa tu contraseña actual.");
            }
            ppqhPutIfError(ppqhErrors, "next", PpqhValidators.ppqhNewPassword(ppqhNext));
            if (ppqhNext != null && !Arrays.equals(ppqhNext, ppqhConfirmation)) {
                ppqhErrors.put("confirmation", "Las contraseñas no coinciden.");
            }
            if (!ppqhErrors.isEmpty()) {
                throw new PpqhValidationException(ppqhErrors);
            }
            String ppqhStoredHash = ppqhUsers.hepyFindStoredById(ppqhMe.ppqhId())
                    .orElseThrow(() -> new PpqhAuthenticationException("Tu cuenta ya no existe."))
                    .hepyPasswordHash();
            if (!ppqhHasher.ppqhVerify(ppqhCurrent.clone(), ppqhStoredHash)) {
                throw PpqhValidationException.ppqhOf("current", "La contraseña actual no es correcta.");
            }
            if (Arrays.equals(ppqhCurrent, ppqhNext)) {
                throw PpqhValidationException.ppqhOf("next", "La nueva contraseña debe ser distinta de la actual.");
            }
            ppqhUsers.hepyUpdatePassword(ppqhMe.ppqhId(), ppqhHasher.ppqhHash(ppqhNext.clone()));
        } finally {
            ppqhWipe(ppqhCurrent);
            ppqhWipe(ppqhNext);
            ppqhWipe(ppqhConfirmation);
        }
    }
}
