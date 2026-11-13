package com.sportforce3d.service;

import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyUserRepository;
import com.sportforce3d.repository.HepyUserRepository.HepyStoredUser;
import com.sportforce3d.util.PpqhValidators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class PpqhAuthService {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhAuthService.class);
    private static final String PPQH_BAD_CREDENTIALS = "Correo o contraseña incorrectos.";

    private final HepyUserRepository ppqhUsers;
    private final PpqhPasswordHasher ppqhHasher;
    private final PpqhSession ppqhSession;
    private final PpqhLoginThrottle ppqhThrottle;
    private final Clock ppqhClock;

    public PpqhAuthService(HepyUserRepository ppqhUsers, PpqhPasswordHasher ppqhHasher, PpqhSession ppqhSession, PpqhLoginThrottle ppqhThrottle, Clock ppqhClock) {
        this.ppqhUsers = ppqhUsers;
        this.ppqhHasher = ppqhHasher;
        this.ppqhSession = ppqhSession;
        this.ppqhThrottle = ppqhThrottle;
        this.ppqhClock = ppqhClock;
    }

    public PpqhUser ppqhLogin(String ppqhEmail, char[] ppqhPassword) {
        String ppqhNormalized = PpqhValidators.ppqhNormalizeEmail(ppqhEmail);
        if (ppqhNormalized.isEmpty() || ppqhPassword == null || ppqhPassword.length == 0) {
            ppqhWipe(ppqhPassword);
            throw new PpqhAuthenticationException("Ingresa tu correo y contraseña.");
        }
        try {
            ppqhThrottle.ppqhCheck(ppqhNormalized);
        } catch (PpqhAuthenticationException ppqhE) {
            ppqhWipe(ppqhPassword);
            throw ppqhE;
        }

        Optional<HepyStoredUser> ppqhStored = ppqhUsers.hepyFindByEmail(ppqhNormalized);
        boolean ppqhMatches = ppqhHasher.ppqhVerify(ppqhPassword, ppqhStored.map(HepyStoredUser::hepyPasswordHash).orElse(null));
        if (ppqhStored.isEmpty() || !ppqhMatches) {
            ppqhThrottle.ppqhRecordFailure(ppqhNormalized);
            ppqhLog.info("Inicio de sesión fallido para {}", ppqhNormalized);
            throw new PpqhAuthenticationException(PPQH_BAD_CREDENTIALS);
        }
        PpqhUser ppqhUser = ppqhStored.get().hepyUser();
        if (!ppqhUser.ppqhActive()) {
            throw new PpqhAuthenticationException("Tu cuenta está desactivada. Escribe a la tienda para reactivarla.");
        }
        ppqhThrottle.ppqhRecordSuccess(ppqhNormalized);
        ppqhUsers.hepyTouchLastLogin(ppqhUser.ppqhId(), ppqhClock.instant());
        PpqhUser ppqhFresh = ppqhUsers.hepyFindById(ppqhUser.ppqhId()).orElse(ppqhUser);
        ppqhSession.start(ppqhFresh);
        ppqhLog.info("Sesión iniciada: usuario {} {}", ppqhFresh.ppqhId(), ppqhFresh.ppqhRoles());
        return ppqhFresh;
    }

    public PpqhUser ppqhRegister(String ppqhFullName, String ppqhEmail, String ppqhPhone, char[] ppqhPassword, char[] ppqhConfirmation) {
        String ppqhNormalized = PpqhValidators.ppqhNormalizeEmail(ppqhEmail);
        Map<String, String> ppqhErrors = new LinkedHashMap<>();
        ppqhPutIfError(ppqhErrors, "fullName", PpqhValidators.ppqhFullName(ppqhFullName));
        ppqhPutIfError(ppqhErrors, "email", PpqhValidators.ppqhEmail(ppqhNormalized));
        ppqhPutIfError(ppqhErrors, "phone", PpqhValidators.ppqhPhone(ppqhPhone));
        ppqhPutIfError(ppqhErrors, "password", PpqhValidators.ppqhNewPassword(ppqhPassword));
        if (ppqhPassword != null && !Arrays.equals(ppqhPassword, ppqhConfirmation)) {
            ppqhErrors.put("confirmation", "Las contraseñas no coinciden.");
        }
        ppqhWipe(ppqhConfirmation);
        if (ppqhErrors.isEmpty() && ppqhUsers.hepyExistsByEmail(ppqhNormalized)) {
            ppqhErrors.put("email", "Ya existe una cuenta con este correo.");
        }
        if (!ppqhErrors.isEmpty()) {
            ppqhWipe(ppqhPassword);
            throw new PpqhValidationException(ppqhErrors);
        }

        PpqhUser ppqhCreated = ppqhUsers.hepyCreate(ppqhNormalized, ppqhHasher.ppqhHash(ppqhPassword), ppqhFullName.trim(),
                PpqhValidators.ppqhBlankToNull(ppqhPhone), EnumSet.of(PpqhRole.CLIENTE));
        ppqhUsers.hepyTouchLastLogin(ppqhCreated.ppqhId(), ppqhClock.instant());
        PpqhUser ppqhFresh = ppqhUsers.hepyFindById(ppqhCreated.ppqhId()).orElse(ppqhCreated);
        ppqhSession.start(ppqhFresh);
        ppqhLog.info("Cuenta CLIENTE creada: usuario {}", ppqhFresh.ppqhId());
        return ppqhFresh;
    }

    public void ppqhLogout() {
        ppqhSession.ppqhCurrent().ifPresent(ppqhU -> ppqhLog.info("Sesión cerrada: usuario {}", ppqhU.ppqhId()));
        ppqhSession.ppqhEnd();
    }

    static void ppqhPutIfError(Map<String, String> ppqhErrors, String ppqhField, String ppqhMessage) {
        if (ppqhMessage != null) {
            ppqhErrors.put(ppqhField, ppqhMessage);
        }
    }

    static void ppqhWipe(char[] ppqhValue) {
        if (ppqhValue != null) {
            Arrays.fill(ppqhValue, '\0');
        }
    }
}
