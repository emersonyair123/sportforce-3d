package com.sportforce3d.service;

import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongFunction;

public class PpqhSession {

    private final AtomicReference<PpqhUser> ppqhCurrent = new AtomicReference<>();
    private final LongFunction<Optional<PpqhUser>> ppqhReload;

    public PpqhSession() {
        this(null);
    }

    public PpqhSession(LongFunction<Optional<PpqhUser>> ppqhReload) {
        this.ppqhReload = ppqhReload;
    }

    public Optional<PpqhUser> ppqhCurrent() {
        return Optional.ofNullable(ppqhCurrent.get());
    }

    public boolean ppqhIsAuthenticated() {
        return ppqhCurrent.get() != null;
    }

    public PpqhUser ppqhRequireUser() {
        PpqhUser ppqhUser = ppqhCurrent.get();
        if (ppqhUser == null) {
            throw new PpqhAuthenticationException("Inicia sesión para continuar.");
        }
        return ppqhUser;
    }

    public PpqhUser ppqhRequireRole(PpqhRole ppqhRole) {
        PpqhUser ppqhUser = ppqhRequireUser();
        if (ppqhReload != null) {
            PpqhUser ppqhFresh = ppqhReload.apply(ppqhUser.ppqhId()).orElse(null);
            if (ppqhFresh == null || !ppqhFresh.ppqhActive()) {
                throw new PpqhAuthenticationException("Tu cuenta ya no está activa. Cierra sesión y escribe a la tienda.");
            }
            ppqhCurrent.compareAndSet(ppqhUser, ppqhFresh);
            ppqhUser = ppqhFresh;
        }
        if (!ppqhUser.ppqhHasRole(ppqhRole)) {
            throw new PpqhAccessDeniedException("No tienes permiso para realizar esta acción.");
        }
        return ppqhUser;
    }

    void start(PpqhUser ppqhUser) {
        ppqhCurrent.set(ppqhUser);
    }

    void ppqhRefresh(PpqhUser ppqhUser) {
        ppqhCurrent.updateAndGet(ppqhU -> ppqhU != null && ppqhU.ppqhId() == ppqhUser.ppqhId() ? ppqhUser : ppqhU);
    }

    void ppqhEnd() {
        ppqhCurrent.set(null);
    }
}
