package com.sportforce3d.service;

import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PpqhUserAdminService {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhUserAdminService.class);

    private final HepyUserRepository ppqhUsers;
    private final PpqhSession ppqhSession;

    public PpqhUserAdminService(HepyUserRepository ppqhUsers, PpqhSession ppqhSession) {
        this.ppqhUsers = ppqhUsers;
        this.ppqhSession = ppqhSession;
    }

    public List<PpqhUser> ppqhListUsers() {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        return ppqhUsers.hepyFindAll();
    }

    public PpqhUser ppqhSetActive(long ppqhUserId, boolean ppqhActive) {
        PpqhUser ppqhAdmin = ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        PpqhUser ppqhTarget = ppqhFind(ppqhUserId);
        if (!ppqhActive && ppqhTarget.ppqhId() == ppqhAdmin.ppqhId()) {
            throw new PpqhServiceException("No puedes desactivar tu propia cuenta.");
        }
        if (!ppqhActive && ppqhTarget.ppqhIsAdmin() && ppqhTarget.ppqhActive() && ppqhUsers.hepyCountActiveWithRole(PpqhRole.ADMIN) <= 1) {
            throw new PpqhServiceException("Debe quedar al menos un administrador activo.");
        }
        ppqhUsers.hepySetActive(ppqhUserId, ppqhActive);
        ppqhLog.info("ADMIN {} cambió active={} del usuario {}", ppqhAdmin.ppqhId(), ppqhActive, ppqhUserId);
        return ppqhFind(ppqhUserId);
    }

    public PpqhUser ppqhSetAdmin(long ppqhUserId, boolean ppqhAdmin) {
        PpqhUser ppqhMe = ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        PpqhUser ppqhTarget = ppqhFind(ppqhUserId);
        if (ppqhTarget.ppqhIsAdmin() == ppqhAdmin) {
            return ppqhTarget;
        }
        if (!ppqhAdmin && ppqhTarget.ppqhId() == ppqhMe.ppqhId()) {
            throw new PpqhServiceException("No puedes quitarte tu propio rol de administrador.");
        }
        if (!ppqhAdmin && ppqhTarget.ppqhActive() && ppqhUsers.hepyCountActiveWithRole(PpqhRole.ADMIN) <= 1) {
            throw new PpqhServiceException("Debe quedar al menos un administrador activo.");
        }
        Set<PpqhRole> ppqhRoles = ppqhTarget.ppqhRoles().isEmpty() ? EnumSet.noneOf(PpqhRole.class) : EnumSet.copyOf(ppqhTarget.ppqhRoles());
        if (ppqhAdmin) {
            ppqhRoles.add(PpqhRole.ADMIN);
        } else {
            ppqhRoles.remove(PpqhRole.ADMIN);
            ppqhRoles.add(PpqhRole.CLIENTE);
        }
        ppqhUsers.hepyReplaceRoles(ppqhUserId, ppqhRoles);
        ppqhLog.info("ADMIN {} cambió roles del usuario {} a {}", ppqhMe.ppqhId(), ppqhUserId, ppqhRoles);
        return ppqhFind(ppqhUserId);
    }

    private PpqhUser ppqhFind(long ppqhUserId) {
        return ppqhUsers.hepyFindById(ppqhUserId).orElseThrow(() -> new PpqhServiceException("El usuario ya no existe."));
    }
}
